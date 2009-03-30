package org.mitre.giscore.test.output;

import junit.framework.TestCase;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;
import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipFile;
import java.text.SimpleDateFormat;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 20, 2009 11:54:04 AM
 */
public class TestKmlWriter extends TestCase {

	@Test
    public void test_read_write_Kml() {
        checkDir(new File("data/kml"));
    }

    private void checkDir(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) checkDir(file);
			else {
				String name = file.getName().toLowerCase();
				if (name.endsWith(".kml") || name.endsWith(".kmz"))
					try {
						checkKmlFile(file);
					} catch (IOException e) {
						System.out.println("Failed to read/write: " + file + " " + e);
					}
			}
		}
    }

    private void checkKmlFile(File file) throws IOException {
        System.out.println("Testing " + file);
        KmlReader reader = new KmlReader(file);
		List<IGISObject> objs = reader.readAll();
		List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
        List<URI> links = reader.getNetworkLinks();
        if (links.size() != 0)
            assertTrue(linkedFeatures.size() != 0);
		//File temp = File.createTempFile("test", reader.isCompressed() ? ".kmz" : ".kml");
		/*
		String suff = file.getName();
		int ind = suff.lastIndexOf('.');
		if (ind != -1) suff = suff.substring(0, ind);
		if (suff.length() < 3) suff = "x" + suff;
		File temp = File.createTempFile(suff + "-", reader.isCompressed() ? ".kmz" : ".kml", new File("testOutput/kml"));
		*/
		File temp = new File("testOutput/test." + (reader.isCompressed() ? "kmz" : "kml"));
		try {
			System.out.println(">create " + temp);
			KmlWriter writer = new KmlWriter(temp);
			for (IGISObject o : objs) {
				writer.write(o);
			}
			writer.close();
			// Filter original list such that it will match the re-imported list
			List<IGISObject> objs2 = new ArrayList<IGISObject>();
			for (int i = 0; i < objs.size(); i++) {
				IGISObject o = objs.get(i);
				// KmlReader may introduce Comment Objects for skipped elements
				// so need to remove these since reading them back in will not preserve them
				if (o instanceof Comment) continue;
				// KmlWriter ignores any empty containers so any ContainerStart
				// followed by a ContainerEnd will be discarded.
				// need to remove any of these from the list from which
				// to compare to original list.
				if (o instanceof ContainerStart && i + 1 < objs.size()) {
					IGISObject next = objs.get(i + 1);
					if (next instanceof ContainerEnd) {
						if (i > 0) {
							IGISObject prev = objs.get(i - 1);
							// ignore unless previous elements are Style and StyleMaps
							// which are added to an empty container...
							if (prev instanceof Style || prev instanceof StyleMap) {
								objs2.add(o);
								continue;
							}
						}
						i++; // skip current and next items
						continue;
					}
				}
				objs2.add(o);
			}
			objs = objs2;
			KmlReader reader2 = new KmlReader(temp);
			List<IGISObject> elements = reader2.readAll();
			/*
			if (objs.size() != elements.size()) {
					for(Object o : objs) {
						System.out.println(" >" + o.getClass().getName());
					}
					System.out.println();
					for(Object o : elements) {
						System.out.println(" <" + o.getClass().getName());
					}
					//System.out.println("\nelts1=" + elements);
					//System.out.println("\nelts2=" + elements2);
					//System.out.println();
			}
			*/
			assertEquals(objs.size(), elements.size());
		} finally {
			// delete temp file
			if (temp != null && temp.exists()) temp.delete();
		}
	}

	public void test_NetworkLink_Kmz() throws IOException, XMLStreamException {
		File temp = File.createTempFile("test", ".kmz");
		//File temp = new File("test.kmz");
		ZipFile zf = null;
		try {
			KmlWriter writer = new KmlWriter(temp);

			NetworkLink nl = new NetworkLink();
			TaggedMap link = new TaggedMap(IKml.LINK);
			link.put(IKml.HREF, "kml/link.kml");
			nl.setName("NetworkLink Test");
			nl.setLink(link);
			writer.write(nl);

			// add linked KML entry to KMZ file as "kml/link.kml"
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			KmlOutputStream kos = new KmlOutputStream(bos);
			kos.write(new DocumentStart(DocumentType.KML));
			/*
			 could fill out completed GroundOverlay with icon href to image here
			 (see data/kml/groundoverlay/etna.kml) but doesn't change the test
			 results so just write out a simple Placemark.
			*/
			// GroundOverlay o = new GroundOverlay();
			Feature f = new Feature();
			f.setGeometry(new Point(42.504733587704, -71.238861602674));
			f.setName("test");
			f.setDescription("this is a test placemark");
			kos.write(f);
			kos.close();
			writer.write(new ByteArrayInputStream(bos.toByteArray()), "kml/link.kml");

			// added image entry to KMZ file
			File file = new File("data/kml/GroundOverlay/etna.jpg");
			writer.write(file, "images/etna.jpg");

			writer.close();

			KmlReader reader = new KmlReader(temp);
			List<IGISObject> objs = reader.readAll();
			// System.out.println(objs);
			/*
			for(Object o : objs) {
				System.out.println(" >" + o.getClass().getName());
			}
			System.out.println();
			*/

			assertTrue(objs.size() == 2);
			TestKmlOutputStream.checkApproximatelyEquals(nl, objs.get(1));

			List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
			List<URI> links =  reader.getNetworkLinks();
			//System.out.println("linkedFeature=" + linkedFeatures);
			//System.out.println("links=" + links);
			assertEquals(2, linkedFeatures.size());
			assertEquals(1, links.size());
			TestKmlOutputStream.checkApproximatelyEquals(f, linkedFeatures.get(1));

			zf = new ZipFile(temp);
			assertEquals(3, zf.size());
		} finally {
			if (zf != null) zf.close();
			// delete temp file
			if (temp != null && temp.exists()) temp.delete();
		}
	}

	/**
	 * Using TimeTest.kml example test 6 variations of timeStamps or timeSpans.  Verify 
	 * read and write various time start and end time combinations.
	 *
	 * @throws Exception
	 */
	public void test_Timestamp_Feature() throws Exception {
		File input = new File("data/kml/time/TimeTest.kml");
		TimeZone gmt = TimeZone.getTimeZone("GMT");
		File temp = File.createTempFile("test1", ".kml");
		try {
			KmlReader reader = new KmlReader(input);
			List<IGISObject> objs = reader.readAll();

			//System.out.println(features);
			//System.out.println("# features=" + features.size());
			assertEquals(9, objs.size());

			/*
			 Structure of KML objects:
			  org.mitre.giscore.events.DocumentStart
			  org.mitre.giscore.events.ContainerStart
			  org.mitre.giscore.events.Feature - feature 0 timeStamp placemark - start marker marks earlier time in dataset
			  org.mitre.giscore.events.Feature - feature 1 TimeSpan only end time
			  org.mitre.giscore.events.Feature - feature 2 TimeSpan both start and end
			  org.mitre.giscore.events.Feature - feature 3 TimeSpan with only begin time
			  org.mitre.giscore.events.Feature - feature 4 timeStamp placemark - end marker marks latest time in dataset
			  org.mitre.giscore.events.Feature - feature 5 no time -> static placemark
			  org.mitre.giscore.events.ContainerEnd
			 */

			List<Feature> features = new ArrayList<Feature>(6);
			for (IGISObject o : objs) {
				if (o instanceof Feature)
					features.add((Feature)o);
			}
			assertEquals(6, features.size());

			// feature 0 timeStamp placemark - start marker marks earlier time in dataset
			Feature f = features.get(0);
			DatatypeFactory fact = DatatypeFactory.newInstance();
			XMLGregorianCalendar xmlCal = fact.newXMLGregorianCalendar("2008-08-12T20:16:00Z");
			GregorianCalendar cal = xmlCal.toGregorianCalendar();
			cal.setTimeZone(gmt);
			Date firstTime = cal.getTime();
			assertEquals(firstTime, f.getStartTime());
			assertEquals(firstTime, f.getEndTime());
			Geometry geom = f.getGeometry();
			Geodetic2DPoint center = geom.getCenter();
			assertEquals(new Latitude(Math.toRadians(39.104144789924)).inDegrees(), center.getLatitude().inDegrees(), 1e-5);
			assertEquals(new Longitude(Math.toRadians(-76.72894181350101)).inDegrees(), center.getLongitude().inDegrees(), 1e-5);

			// feature 1 TimeSpan only end time
			assertNull(features.get(1).getStartTime());
			assertNotNull(features.get(1).getEndTime());

			// feature 2 TimeSpan both start and end
			assertNotNull(features.get(2).getStartTime());
			assertNotNull(features.get(2).getEndTime());

			// feature 3 TimeSpan with only begin time
			assertNotNull(features.get(3).getStartTime());
			assertNull(features.get(3).getEndTime());

			// feature 4 timeStamp placemark - end marker marks latest time in dataset
			Date lastEndTime = features.get(4).getEndTime();
			assertNotNull(lastEndTime);

			// feature 5 no time -> static placemark
			assertNull(features.get(5).getStartTime());
			assertNull(features.get(5).getEndTime());

			for (Feature feat : features) {
				Date starTime = feat.getStartTime();
				// all begin times will be greater or equal to the time of the first feature
				if (starTime != null)
					assertTrue(starTime.compareTo(firstTime) >= 0);
				Date endTime = feat.getEndTime();
				// all end times will be less or equal to the end time of the last feature
				if (endTime != null)
					assertTrue(endTime.compareTo(lastEndTime) <= 0);
			}

			KmlWriter writer = new KmlWriter(temp);
			for (IGISObject o : objs) {
				writer.write(o);
			}
			writer.close();

			reader = new KmlReader(temp);
			List<IGISObject> objs2 = reader.readAll();
			assertEquals(objs.size(), objs2.size());
			for (int i = 0; i < objs.size(); i++) {
				TestKmlOutputStream.checkApproximatelyEquals(objs.get(i), objs2.get(i));
			}
		} finally {
			if (temp.exists()) temp.delete();
		}
	}

	private final String[] timestamps = {
			"2009-01-01T00:00:00.000Z	2009-01-01T00:00:00.000Z", // when 2007
			"2009-01-01T00:00:00.000Z	2009-01-01T00:00:00.000Z", // span 2007
			"2009-03-01T00:00:00.000Z	2009-03-01T00:00:00.000Z", // when 2009-03
			"2009-03-01T00:00:00.000Z	2009-03-01T00:00:00.000Z", // span 2009-03
			"2009-03-14T00:00:00.000Z	2009-03-14T00:00:00.000Z", // when 2009-03-14
			"2009-03-14T00:00:00.000Z	2009-03-14T00:00:00.000Z", // span 2009-03-14
			"2009-03-14T21:06:30.000Z	2009-03-14T21:06:30.000Z", // when 2009-03-14T21:06:30Z
			"2009-03-14T21:06:00.000Z	2009-03-14T21:06:59.000Z", // span 2009-03-14T21:06Z
			"2009-03-14T18:10:46.000Z	2009-03-14T18:10:46.000Z", // when 2009-03-14T21:10:46+03:00
			"2009-03-14T21:10:50.000Z	2009-03-14T21:10:50.000Z", // when 2009-03-14T16:10:50-05:00
			"2009-03-14T21:10:50.000Z	2009-03-14T21:10:50.000Z"  // when 2009-03-14T16:10:50 (no timezone assumes UTC)
	};

	public void test_Time_Feature() throws Exception {
		File input = new File("data/kml/time/timestamps.kml");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		File temp = File.createTempFile("test1", ".kml");
		try {
			KmlReader reader = new KmlReader(input);
			List<IGISObject> objs = reader.readAll();

			//System.out.println(objs);
			//System.out.println("# features=" + objs.size());
			// assertEquals(9, objs.size());

			List<Feature> features = new ArrayList<Feature>(11);
			for (IGISObject o : objs) {
				if (o instanceof Feature)
					features.add((Feature)o);
			}
			assertEquals(11, features.size());

			SimpleDateFormat df = new SimpleDateFormat(IKml.ISO_DATE_FMT);
        	df.setTimeZone(tz);

			for (int i = 0; i < features.size(); i++) {
				Feature f = features.get(i);
				Date start = f.getStartTime();
				Date end = f.getEndTime();
				String startFmt = start == null ? null : df.format(start);
				String endFmt = end == null ? null : df.format(end);
				System.out.println("\n >" + f.getClass().getName());
				System.out.format("\t%s\t%s%n", startFmt, endFmt);
				String[] startEnd = timestamps[i].split("\t");
				String expStartTime = startEnd[0];
				String expEndTime = startEnd[1];
				System.out.println("\t" + expStartTime + "\t"+ expEndTime );
				assertEquals("startTime compare @" + i, expStartTime, startFmt);
				assertEquals("endTime compare @" + i, expEndTime, endFmt);
			}

			KmlWriter writer = new KmlWriter(temp);
			for (IGISObject o : objs) {
				writer.write(o);
			}
			writer.close();

			reader = new KmlReader(temp);
			List<IGISObject> objs2 = reader.readAll();
			assertEquals(14, objs2.size());
			for (int i = 0; i < objs.size(); i++) {
				TestKmlOutputStream.checkApproximatelyEquals(objs.get(i), objs2.get(i));
			}
		} finally {
			if (temp.exists()) temp.delete();
		}
	}
}
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

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 20, 2009 11:54:04 AM
 */
public class TestKmlWriter extends TestCase {
	
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

			// added KML entry to KMZ file
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
			List<IGISObject> objs = reader.getFeatures();
			// System.out.println(objs);
			/*
			for(Object o : objs) {
				System.out.println(" >" + o.getClass().getName());
			}
			System.out.println();
			*/

			assertTrue(objs.size() == 2);
			TestKmlOutputStream.checkApproximatelyEquals(nl, objs.get(1));

			List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
			List<URI> links = reader.importFromNetworkLinks(linkedFeatures);
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

	public void test_Timestamp_Feature() throws Exception {
		File input = new File("data/kml/time/TimeTest.kml");
		TimeZone gmt = TimeZone.getTimeZone("GMT");
		File temp = File.createTempFile("test1", ".kml");
		try {
			KmlReader reader = new KmlReader(input);
			List<IGISObject> objs = reader.getFeatures();

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
			List<IGISObject> objs2 = reader.getFeatures();
			assertEquals(objs.size(), objs2.size());
			for (int i = 0; i < objs.size(); i++) {
				TestKmlOutputStream.checkApproximatelyEquals(objs.get(i), objs2.get(i));
			}
		} finally {
			if (temp.exists()) temp.delete();
		}
	}
}

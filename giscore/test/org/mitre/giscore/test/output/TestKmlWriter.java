package org.mitre.giscore.test.output;

import junit.framework.TestCase;
import org.mitre.giscore.input.kml.KmlWriter;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.DocumentType;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.net.URI;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 20, 2009 11:54:04 AM
 */
public class TestKmlWriter extends TestCase {
	
	public void testNetworkLinkKmz() throws IOException, XMLStreamException {
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
	
}

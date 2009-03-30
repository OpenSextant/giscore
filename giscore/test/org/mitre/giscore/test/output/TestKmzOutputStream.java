/****************************************************************************************
 *  $Id$
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.Assert;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.TaggedMap;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.output.kml.KmzOutputStream;

/**
 * This is basically just a subset of {@code TestKmlWriter} pointed at a
 * KmzOutputStream instead.
 *
 * @author jgibson
 */
public class TestKmzOutputStream {

	@Test
	public void test_NetworkLink_Kmz() throws Exception {
		File temp = File.createTempFile("test", ".kmz");
		//File temp = new File("test.kmz");
		ZipFile zf = null;
		try {
			KmzOutputStream kmzos = new KmzOutputStream(new FileOutputStream(temp));

			NetworkLink nl = new NetworkLink();
			TaggedMap link = new TaggedMap(IKml.LINK);
			link.put(IKml.HREF, "kml/link.kml");
			nl.setName("NetworkLink Test");
			nl.setLink(link);
			kmzos.write(nl);

			// added KML entry to KMZ file
			KmlOutputStream kos = new KmlOutputStream(kmzos.addEntry("kml/link.kml"));
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

			// added image entry to KMZ file
			File file = new File("data/kml/GroundOverlay/etna.jpg");
			kmzos.addEntry(new FileInputStream(file), "images/etna.jpg");

			kmzos.close();

			KmlReader reader = new KmlReader(temp);
			List<IGISObject> objs = reader.readAll();
			// System.out.println(objs);
			/*
			for(Object o : objs) {
				System.out.println(" >" + o.getClass().getName());
			}
			System.out.println();
			*/

			Assert.assertEquals(2, objs.size());
			TestKmlOutputStream.checkApproximatelyEquals(nl, objs.get(1));

			List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
			List<URI> links = reader.getNetworkLinks();
			//System.out.println("linkedFeature=" + linkedFeatures);
			//System.out.println("links=" + links);
			Assert.assertEquals(2, linkedFeatures.size());
			Assert.assertEquals(1, links.size());
			TestKmlOutputStream.checkApproximatelyEquals(f, linkedFeatures.get(1));

			zf = new ZipFile(temp);
			Assert.assertEquals(3, zf.size());
		} finally {
			if (zf != null) zf.close();
			// delete temp file
			if (temp != null && temp.exists()) temp.delete();
		}
	}
}

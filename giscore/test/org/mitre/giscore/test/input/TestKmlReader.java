package org.mitre.giscore.test.input;

import org.junit.Test;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.giscore.events.*;
import org.mitre.giscore.test.output.TestKmlOutputStream;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Point;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URL;
import java.awt.image.BufferedImage;

import junit.framework.TestCase;

import javax.imageio.ImageIO;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 17, 2009 3:23:00 PM
 */
public class TestKmlReader extends TestCase {

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
        List<IGISObject> objs = reader.getFeatures();
        List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
        List<URI> links = reader.importFromNetworkLinks(linkedFeatures);
        if (links.size() != 0)
            assertTrue(linkedFeatures.size() != 0);
		File temp = File.createTempFile("test", reader.isCompressed() ? ".kmz" : ".kml");
		//File temp = new File("test." + (reader.isCompressed() ? "kmz" : "kml"));
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
			reader = new KmlReader(temp);
			List<IGISObject> elements = reader.getFeatures();
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

	/**
     * Test loading KMZ file with network link contining embedded KML
	 * then load the content from the networkLink.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testKmzNetworkLinks() throws IOException {
		File file = new File("data/kml/kmz/dir/content.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> features = reader.getFeatures();
		List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
		List<URI> networkLinks = reader.importFromNetworkLinks(linkedFeatures);
		assertEquals(5, features.size());
		assertEquals(1, networkLinks.size());
		assertEquals(2, linkedFeatures.size());
		IGISObject o = linkedFeatures.get(1); 
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);

		// import same KMZ file as URL
		URL url = file.toURI().toURL();
		KmlReader reader2 = new KmlReader(url);
		List<IGISObject> features2 = reader2.getFeatures();
		List<IGISObject> linkedFeatures2 = new ArrayList<IGISObject>();
		List<URI> networkLinks2 = reader2.importFromNetworkLinks(linkedFeatures2);
		assertEquals(5, features2.size());
		assertEquals(1, networkLinks2.size());
		assertEquals(2, linkedFeatures2.size());
		// NetworkLinked Feature -> DocumentStart + Feature
		TestKmlOutputStream.checkApproximatelyEquals(ptFeat, linkedFeatures2.get(1));
	}

	/**
     * Test ground overlays with KML from URL and KMZ from file targets
     */
    @Test
	public void testOverlay() throws Exception {
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kmz")));
		// target overlay URI -> kmzfile:/C:/projects/giscore/data/kml/GroundOverlay/etna.kmz?file=etna.jpg
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kml").toURI().toURL()));
		// target overlay URI -> file:/C:/projects/giscore/data/kml/GroundOverlay/etna.jpg
	}

	private void checkGroundOverlay(KmlReader reader) throws Exception {
		List<IGISObject> features = reader.getFeatures();
		assertEquals(2, features.size());
		IGISObject obj = features.get(1);
		assertTrue(obj instanceof GroundOverlay);
		GroundOverlay o = (GroundOverlay)obj;
		TaggedMap icon = o.getIcon();
		String href = icon != null ? icon.get(IKml.HREF) : null;
		assertNotNull(href);
		//System.out.println(href);
		UrlRef urlRef = new UrlRef(new URI(href));
		//System.out.println(urlRef);
		InputStream is = null;
		try {
			is = urlRef.getInputStream();
			BufferedImage img = ImageIO.read(is);
			assertNotNull(img);
			assertEquals(418, img.getHeight());
			assertEquals(558, img.getWidth());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}

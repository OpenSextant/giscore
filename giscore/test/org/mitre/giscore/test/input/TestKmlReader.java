package org.mitre.giscore.test.input;

import junit.framework.TestCase;
import org.junit.Test;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.test.output.TestKmlOutputStream;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URL;
import java.awt.image.BufferedImage;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 30, 2009 1:12:51 PM
 */
public class TestKmlReader extends TestCase {

	/**
     * Test loading KMZ file with network link containing embedded KML
	 * then load the content from the NetworkLink.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testKmzNetworkLinks() throws IOException {
		File file = new File("data/kml/kmz/dir/content.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> features = reader.readAll(); // implicit close
		assertEquals(5, features.size());

        IGISObject o = features.get(2);
		assertTrue(o instanceof NetworkLink);
        NetworkLink link = (NetworkLink)o;
        final URI linkUri = KmlReader.getLinkUri(link);
        assertNotNull(linkUri);
        // href = kmzfile:/C:/giscoreHome/data/kml/kmz/dir/content.kmz?file=kml/hi.kml
        assertTrue(linkUri.toString().endsWith("content.kmz?file=kml/hi.kml"));
        
		List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
		List<URI> networkLinks = reader.getNetworkLinks();
		assertEquals(1, networkLinks.size());
		assertEquals(2, linkedFeatures.size());
		o = linkedFeatures.get(1);
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);

		// import same KMZ file as URL
		URL url = file.toURI().toURL();
		KmlReader reader2 = new KmlReader(url);
		List<IGISObject> features2 = reader2.readAll();
		List<IGISObject> linkedFeatures2 = reader2.importFromNetworkLinks();
		List<URI> networkLinks2 = reader2.getNetworkLinks();
		assertEquals(5, features2.size());
		assertEquals(1, networkLinks2.size());
		assertEquals(2, linkedFeatures2.size());
		// NetworkLinked Feature -> DocumentStart + Feature
		TestKmlOutputStream.checkApproximatelyEquals(ptFeat, linkedFeatures2.get(1));
	}

    @Test
	public void testUrlNetworkLink() throws IOException {
        // test NetworkLink that contains viewFormat + httpQuery elements which get populated in URL
        // via KmlBaseReader.getLinkHref()
        KmlReader reader = new KmlReader(new URL("http://jason-stage.mitre.org:8080/kmlWeb/youAreHere.gsp"));
        // encooded as URL: http://xxx/youAreHere.gsp?clientVersion=4.3.7284.3916&kmlVersion=2.2&clientName=Google+Earth&lang=en&BBOX=0,0,0,0&CAMERA=0,0,0,0,0&Fov=0,0&width=0&height=0&terrain=0]
        List<IGISObject> features = reader.readAll(); // implicit close
        //System.out.println("features=" + features);
        assertFalse(features.isEmpty());
        List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
        //System.out.println("XXX: linkedFeatures=" + linkedFeatures);
        assertFalse(linkedFeatures.isEmpty());
		List<URI> networkLinks = reader.getNetworkLinks();
        //System.out.println("links=" + networkLinks);
        assertEquals(1, networkLinks.size());
		//assertEquals(2, linkedFeatures.size());
    }

    /**
     * Targets of NetworkLinks may exist inside a KMZ as well as outside at
     * the same context as the KMZ resource itself so test such a KMZ file.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
	public void testKmzOutsideNetworkLinks() throws IOException {
        File file = new File("data/kml/kmz/networklink/hier.kmz");
        // e.g. http://kml-samples.googlecode.com/svn/trunk/kml/kmz/networklink/hier.kmz
        KmlReader reader = new KmlReader(file);
        List<IGISObject> features = reader.readAll(); // implicit close
        /*
        for(IGISObject obj : features) {
            if (obj instanceof NetworkLink) {
                NetworkLink nl = (NetworkLink)obj;
                URI linkUri = KmlReader.getLinkUri(nl);
                assertNotNull(linkUri);
                UrlRef urlRef = new UrlRef(linkUri);
                InputStream is = null;
                try {
                    is = urlRef.getInputStream();
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        */
        List<URI> networkLinks = reader.getNetworkLinks();
        assertEquals(2, networkLinks.size());

        List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
        assertEquals(4, linkedFeatures.size());
        
        // System.out.println("linkedFeatures=" + linkedFeatures);
        // within.kml ->  <name>within.kml</name>
        // outside.kml -> name>outside.kml</name>
        IGISObject o1 = linkedFeatures.get(1);
        assertTrue(o1 instanceof Feature && "within.kml".equals(((Feature)o1).getName()));
        IGISObject o3 = linkedFeatures.get(3);
        assertTrue(o3 instanceof Feature && "outside.kml".equals(((Feature)o3).getName()));
    }

	/**
     * Test loading KMZ file with 2 levels of network links
	 * recursively loading each NetworkLink.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testMultiLevelNetworkLinks() throws IOException {
		File file = new File("data/kml/NetworkLink/multiLevelNetworkLinks2.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> objs = reader.readAll(); // implicit close
		assertEquals(6, objs.size());
		List<IGISObject> linkedFeatures = reader.importFromNetworkLinks();
		List<URI> networkLinks = reader.getNetworkLinks();

		assertEquals(2, networkLinks.size());
		assertEquals(9, linkedFeatures.size());
		IGISObject o = linkedFeatures.get(8);
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);
	}

    /**
     * Test loading KMZ file with 2 levels of network links
	 * recursively loading each NetworkLink using callback to handle
     * objects found in network links.
     *
	 * @throws IOException if an I/O error occurs
     */
    @Test
	public void testMultiLevelNetworkLinksWithCallback() throws IOException {
		File file = new File("data/kml/NetworkLink/multiLevelNetworkLinks2.kmz");
		KmlReader reader = new KmlReader(file);
		List<IGISObject> objs = reader.readAll(); // implicit close
		assertEquals(6, objs.size());
		final List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
            reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
            public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
                linkedFeatures.add(gisObj);
                return true;
            }
        });
		List<URI> networkLinks = reader.getNetworkLinks();

		assertEquals(2, networkLinks.size());
		assertEquals(9, linkedFeatures.size());
		IGISObject o = linkedFeatures.get(8);
		assertTrue(o instanceof Feature);
		Feature ptFeat = (Feature)o;
		Geometry geom = ptFeat.getGeometry();
		assertTrue(geom instanceof Point);
	}

	/**
     * Test ground overlay from KMZ file target
     */
    @Test
	public void testKmzFileOverlay() throws Exception {
		// target overlay URI -> kmzfile:/C:/projects/giscore/data/kml/GroundOverlay/etna.kmz?file=etna.jpg
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kmz")));
	}

	/**
     * Test ground overlays with KML from URL target
     */
    @Test
	public void testUrlOverlay() throws Exception {
		// target overlay URI -> file:/C:/projects/giscore/data/kml/GroundOverlay/etna.jpg
		checkGroundOverlay(new KmlReader(new File("data/kml/GroundOverlay/etna.kml").toURI().toURL()));
	}

	private void checkGroundOverlay(KmlReader reader) throws Exception {
		List<IGISObject> features = reader.readAll(); // implicit close
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
    
	/**
     * Test IconStyle with KML from URL target with relative URL to icon
	 * @throws Exception
	 */
    @Test
	public void testIconStyle() throws Exception {
		checkIconStyle(new KmlReader(new File("data/kml/Style/styled_placemark.kml").toURI().toURL()));
	}

	/**
     * Test IconStyle from KMZ file target with icon inside KMZ
	 * @throws Exception
	 */
    @Test
	public void testKmzIconStyle() throws Exception {
		checkIconStyle(new KmlReader(new File("data/kml/kmz/iconStyle/styled_placemark.kmz")));
	}

	private void checkIconStyle(KmlReader reader) throws Exception {
		List<IGISObject> features = new ArrayList<IGISObject>();
		try {
			IGISObject gisObj;
			while ((gisObj = reader.read()) != null) {
				features.add(gisObj);
			}
		} finally {
			reader.close();
		}
		/*
		for(Object o : features) {
			System.out.println(" >" + o.getClass().getName());
		}
		System.out.println();
        */
		assertEquals(3, features.size());
		IGISObject obj = features.get(1);
		assertTrue(obj instanceof Style);
		Style style = (Style)obj;
		assertTrue(style.hasIconStyle());
		String href = style.getIconUrl();
		assertNotNull(href);
		UrlRef urlRef = new UrlRef(new URI(href));
		InputStream is = null;
		try {
			is = urlRef.getInputStream();
			BufferedImage img = ImageIO.read(is);
			assertNotNull(img);
			assertEquals(80, img.getHeight());
			assertEquals(80, img.getWidth());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

}
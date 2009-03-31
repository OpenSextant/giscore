/*
 *  TestUrlRef.java
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
 */
package org.mitre.giscore.test.input;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.File;
import java.io.StringWriter;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.input.kml.UrlRef;

/**
 * @author Jason Mathews, MITRE Corp.
 * Created: Mar 23, 2009 4:17:15 PM
 */
public class TestUrlRef extends TestCase {

	/**
	 * Test UrlRef with normal non-KMZ URLs
	 */
	public void testSimpleURL() {
        try {
			// uri=file:/C:/projects/giscore/data/kml/Placemark/placemark.kml
			// url=file:/C:/projects/giscore/data/kml/Placemark/placemark.kml
			File file = new File("data/kml/Placemark/placemark.kml");
            URL url = file.toURI().toURL();
            UrlRef ref = new UrlRef(url, null);
			URI uri = ref.getURI();

			assertFalse(ref.isKmz());
            assertNull(ref.getKmzRelPath());
			assertNotNull(uri);
			assertEquals(url, ref.getURL());

			// now construct UrlRef from URI to validate every field gets set correctly
			UrlRef ref2 = new UrlRef(uri);
			assertFalse(ref.isKmz());
			assertEquals(url, ref2.getURL());
			assertEquals(uri, ref2.getURI());
			assertNull(ref2.getKmzRelPath());
		} catch (MalformedURLException e) {
            fail("Failed to construct URL");
        } catch (URISyntaxException e) {
            fail("Failed to construct UrlRef");
        }
    }

	/**
	 * Test UrlRef with KMZ resources for links to entries inside the KMZ
	 */
	public void testKmzURL() {
		InputStream is = null;
		try {
			File file = new File("data/kml/kmz/dir/content.kmz");
			URL url = file.toURI().toURL();
			UrlRef ref = new UrlRef(url, "kml/hi.kml");
			URI uri = ref.getURI();
			assertTrue(ref.isKmz());
			assertEquals("kml/hi.kml", ref.getKmzRelPath());
			// URL: file:/C:/projects/giscore/data/kml/kmz/dir/content.kmz
			assertEquals(url, ref.getURL());

			//System.out.println(ref);
			// file:/C:/projects/transfusion/trunk/mediate/data/kml/kmz/dir/content.kmz/kml/hi.kml
			assertTrue(ref.toString().endsWith("kml/kmz/dir/content.kmz/kml/hi.kml"));
			// URI: kmzfile:/C:/projects/giscore/data/kml/kmz/dir/content.kmz?file=kml/hi.kml
			assertNotNull(uri);
			assertTrue(uri.toString().startsWith("kmz"));
			assertTrue(uri.toString().endsWith("kml/kmz/dir/content.kmz?file=kml/hi.kml"));

			// now construct UrlRef from special URI to validate every field gets set correctly
			UrlRef ref2 = new UrlRef(uri);
			assertTrue(ref.isKmz());
			assertEquals(url, ref2.getURL());
			assertEquals(uri, ref2.getURI());
			assertEquals(ref.getKmzRelPath(), ref2.getKmzRelPath());

			is = ref.getInputStream();
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer);
			// check for expected contents within KML document
			assertTrue(writer.toString().indexOf("This is the location of my office.") != -1);
		} catch (MalformedURLException e) {
			fail("Failed to construct URL");
		} catch (URISyntaxException e) {
			fail("Failed to construct UrlRef");
		} catch (IOException e) {
			fail("Failed to get KMZ InputStream");
		} finally {
			IOUtils.closeQuietly(is);
		}
    }

	public void testDynamicURL() {
        try {
            URL url = new URL("http://localhost:8081/genxml.php?year=2008");
            UrlRef ref = new UrlRef(url, null);
            //System.out.println(ref);
            assertFalse(ref.isKmz());
            assertNull(ref.getKmzRelPath());
            assertEquals(url, ref.getURL());

            ref = new UrlRef(url, "kml/other.kml");
            // http://localhost:8081/genxml.php?year=2008&file=kml/other.kml
            // URI: kmzhttp://localhost:8081/genxml.php?year=2008&file=kml/other.kml
            //System.out.println(ref);
            assertTrue(ref.isKmz());
            assertEquals("kml/other.kml", ref.getKmzRelPath());
            assertEquals("kmzhttp://localhost:8081/genxml.php?year=2008&file=kml/other.kml", ref.getURI().toString());
            assertTrue(ref.toString().endsWith("file=kml/other.kml"));
            assertEquals(url, ref.getURL());
        } catch (MalformedURLException e) {
            fail("Failed to construct URL");
        } catch (URISyntaxException e) {
            fail("Failed to construct UrlRef");
        }
    }

}

/****************************************************************************************
 *  TestGeoAtomStream.java
 *
 *  Created: Jul 22, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.opensextant.giscore.test.input;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.GISFactory;
import org.opensextant.giscore.Namespace;
import org.opensextant.giscore.events.AtomHeader;
import org.opensextant.giscore.events.AtomLink;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Element;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.input.IGISInputStream;
import org.opensextant.giscore.utils.IDataSerializable;
import org.opensextant.giscore.utils.SimpleObjectInputStream;
import org.opensextant.giscore.utils.SimpleObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test against some known atom streams
 * 
 * @author DRAND
 */
public class TestGeoAtomStream {

	@Test
	public void testInput() throws Exception {
		// testObjectDataSerialization("http://www.theregister.co.uk/software/headlines.atom");
	}
	
	@Test
	public void testInput2() throws Exception {
		testObjectDataSerialization("http://www.microsoft.com/australia/presspass/theme/feed/Windows-7?format=Atom");
	}

	@Test
	public void testInput3() throws Exception {
		testObjectDataSerialization("https://www.us-cert.gov/ncas/alerts.xml");
	}
	
	private void testObjectDataSerialization(String feedurl) throws Exception {
		URL url = new URL(feedurl);
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();
		IGISInputStream gis = GISFactory.getInputStream(DocumentType.GeoAtom, is);
		IGISObject ob = gis.read();
		List<IGISObject> read = new ArrayList<IGISObject>();
		List<IDataSerializable> serializableList = new ArrayList<IDataSerializable>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100);
		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(bos);
		while(ob != null) {
			read.add(ob);
			if (ob instanceof IDataSerializable) {
				final IDataSerializable serializable = (IDataSerializable) ob;
				serializableList.add(serializable);
				soos.writeObject(serializable);
			}
			ob = gis.read();
		}
		soos.close();

		assertFalse(read.isEmpty());

		if (!serializableList.isEmpty()) {
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			SimpleObjectInputStream sois = new SimpleObjectInputStream(bis);
			for (IDataSerializable s1 : serializableList) {
				IDataSerializable s2 = (IDataSerializable) sois.readObject();
				assertEquals(s1, s2);
			}
			sois.close();
		}
	}

	@Test
	public void testEquals() throws MalformedURLException {
		final URL url = new URL("http://www.fake.mitre.org/atomfakefeed/id=xyzzy/123");
		AtomLink link1 = new AtomLink(url, "self");
		AtomLink link2 = new AtomLink(url, "self");
		assertEquals(link1, link2);
		assertEquals(link1.hashCode(), link2.hashCode());

		final Date date = new Date();
		AtomHeader header1 = new AtomHeader("http://www.fake.mitre.org/12412412412512123123", link1, "title", date);
		AtomHeader header2 = new AtomHeader("http://www.fake.mitre.org/12412412412512123123", link2, "title", date);
		assertEquals(header1, header2);
		assertEquals(header1.hashCode(), header2.hashCode());

		link2.setHref(null);
		assertFalse(link1.equals(link2));
		assertFalse(header1.equals(header2));
		AtomLink link3 = new AtomLink(url, "other");
		assertFalse(link1.equals(link3));
	}

	@Test
	public void testForeignElements() throws Exception {
		File file = new File("data/atom/techalerts.xml");
		checkTechAlerts(GISFactory.getInputStream(DocumentType.GeoAtom, file));
	}
	
	public static void checkTechAlerts(final IGISInputStream gis) throws Exception {
		try {
			IGISObject feed = gis.read();
			assertEquals(AtomHeader.class, feed.getClass());
			final AtomHeader header = (AtomHeader) feed;
			assertEquals(1, header.getElements().size());
			Element elt = header.getElements().get(0);
			assertEquals("urn:x:extension", elt.getNamespaceURI());
			assertEquals("foo", elt.getNamespace().getPrefix());
			assertEquals("A simple element at the top level.", elt.getText());
			IGISObject next = gis.read();
			assertTrue("Element wasn't a document start? " + next, next instanceof DocumentStart);
			next = gis.read();
			assertTrue("Element wasn't a feature? " + next, next instanceof Feature);
			Feature feat = (Feature) next;
			assertEquals(1, feat.getElements().size());
			elt = feat.getElements().get(0);
			assertEquals("urn:x:extension", elt.getNamespaceURI());
			assertEquals("foo", elt.getNamespace().getPrefix());
			assertEquals("A complex element at the top level.", elt.getAttributes().get("description"));
			elt = elt.getChild("child", Namespace.getNamespace("foo", "urn:x:extension"));
			assertNotNull("Child element wasn't found.", elt);
			while(gis.read() != null);
		} finally {
			gis.close();
		}
	}
}

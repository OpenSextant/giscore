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
package org.mitre.giscore.test.input;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.AtomHeader;
import org.mitre.giscore.events.AtomLink;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Test against some known atom streams
 * 
 * @author DRAND
 */
public class TestGeoAtomStream {

	@Test
	public void testInput() throws Exception {
		testObjectDataSerialization("http://www.theregister.co.uk/software/headlines.atom");
	}
	
	@Test
	public void testInput2() throws Exception {
		testObjectDataSerialization("http://www.microsoft.com/australia/presspass/theme/feed/Windows-7?format=Atom");
	}

	@Test
	public void testInput3() throws Exception {
		testObjectDataSerialization("http://www.us-cert.gov/channels/techalerts.atom");
	}
	
	private void testObjectDataSerialization(String feedurl) throws Exception {
		System.setProperty("http.proxyHost", "gatekeeper.mitre.org");
		System.setProperty("http.proxyPort", "80");
		URL url = new URL(feedurl);
		URLConnection connection = url.openConnection();
		IGISInputStream gis = GISFactory.getInputStream(DocumentType.GeoAtom, connection.getInputStream());
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
}

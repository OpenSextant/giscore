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
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.input;

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;

/**
 * Test against some known atom streams
 * 
 * @author DRAND
 */
public class TestGeoAtomStream {
	@Test
	public void testInput() throws Exception {
		System.setProperty("http.proxyHost", "gatekeeper.mitre.org");
		System.setProperty("http.proxyPort", "80");
		String feedurl = "http://www.theregister.co.uk/software/headlines.atom";
		URL url = new URL(feedurl);
		URLConnection connection = url.openConnection();
		IGISInputStream gis = GISFactory.getInputStream(DocumentType.GeoAtom, connection.getInputStream());
		IGISObject ob = gis.read();
		List<IGISObject> read = new ArrayList<IGISObject>();
		while(ob != null) {
			read.add(ob);
			ob = gis.read();
		}
		
		assertTrue(read.size() > 1);
		
	}
}

/****************************************************************************************
 *  TestKmlInputStream.java
 *
 *  Created: Jan 27, 2009
 *
 *  @author DRAND
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
package org.mitre.giscore.test.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.input.IGISInputStream;

/**
 * @author DRAND
 * 
 */
public class TestKmlInputStream {
	@Test
	public void testTinySample() throws Exception {
		InputStream stream = getClass().getResourceAsStream("7084.kml");
		IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);

		IGISObject firstN[] = new IGISObject[10];
		for(int i = 0; i < firstN.length; i++) {
			firstN[i] = kis.read();
		}
		assertNotNull(firstN[7]);
		assertNull(firstN[8]);
		assertTrue(firstN[0] instanceof DocumentStart);
		DocumentStart ds = (DocumentStart) firstN[0];
		assertEquals(DocumentType.KML, ds.getType());
		assertTrue(firstN[1] instanceof Style);
		assertTrue(firstN[2] instanceof Style);
		assertTrue(firstN[3] instanceof StyleMap);
		assertTrue(firstN[4] instanceof ContainerStart);
		assertTrue(firstN[5] instanceof Feature);
		assertTrue(firstN[6] instanceof Feature);
		assertTrue(firstN[7] instanceof ContainerEnd);
	}
	
	@Test public void testLargerSample() throws Exception {
		InputStream stream = getClass().getResourceAsStream("KML_sample1.kml");
		IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);
		
		IGISObject firstN[] = new IGISObject[100];
		for(int i = 0; i < firstN.length; i++) {
			firstN[i] = kis.read();
		}
		System.out.println(firstN);
	}
	
	@Test public void testSchemaSample() throws Exception {
		InputStream stream = getClass().getResourceAsStream("schema_example.kml");
		IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);
		
		IGISObject firstN[] = new IGISObject[10];
		for(int i = 0; i < firstN.length; i++) {
			firstN[i] = kis.read();
		}
		System.out.println(firstN);
	}
	
	
}


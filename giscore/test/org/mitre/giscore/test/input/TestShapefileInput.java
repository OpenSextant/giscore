/****************************************************************************************
 *  TestShapefileInput.java
 *
 *  Created: Jul 28, 2009
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.input.shapefile.SingleShapefileInputHandler;

/**
 * Test single shapefile reader
 * 
 * @author DRAND
 */
public class TestShapefileInput {
	public static File shpdir = new File("data/shape");
	
	@Test public void testErrorcase1() throws Exception {
		doTest("Point File Test_Point File Test");
	}

	@Test public void testPoints() throws Exception {
		doTest("points");
	}

	@Test public void testPointz() throws Exception {
		doTest("pointz");
	}
	
	@Test public void testLines() throws Exception {
		doTest("lines");
	}

	@Test public void testMultilines() throws Exception {
		doTest("multilines");
	}
	
	@Test public void testMultipoint() throws Exception {
		doTest("multipoint");
	}

	@Test public void testMultipolys() throws Exception {
		doTest("multipolys");
	}
	
	@Test public void testMultipolyz() throws Exception {
		doTest("multipolyz");
	}

	@Test public void testMultirings() throws Exception {
		doTest("multirings");
	}
	
	@Test public void testMultiringz() throws Exception {
		doTest("multiringz");
	}

	@Test public void testPolys() throws Exception {
		doTest("polys");
	}
	
	@Test public void testPolyz() throws Exception {
		doTest("polyz");
	}
   
	@Test public void testRings() throws Exception {
		doTest("rings");
	}

	@Test public void testRingz() throws Exception {
		doTest("ringz");
	}

	private void doTest(String file) throws URISyntaxException, IOException {
		System.out.println("Test " + file);
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(shpdir, file);
		try {
			IGISObject ob = handler.read();
			assertTrue(ob instanceof Schema);
			int count = 0;
			while((ob = handler.read()) != null) {
				assertTrue(ob instanceof Feature);
				count++;
			}
			assertTrue(count > 0);
			System.out.println(" count=" + count);
		} finally {
			handler.close();
		}
	}
}

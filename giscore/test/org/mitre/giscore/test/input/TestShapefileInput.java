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
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.shapefile.SingleShapefileInputHandler;

/**
 * Test single shapefile reader
 * 
 * @author DRAND
 */
public class TestShapefileInput {
	public static File shpdir = new File("data/shape");
	
	@Test public void testErrorcase1() throws Exception {
		doTest("Point File Test_Point File Test", Point.class);
	}

	@Test public void testPoints() throws Exception {
		doTest("points", Point.class);
	}

	@Test public void testPointz() throws Exception {
		doTest("pointz", Point.class);
	}
	
	@Test public void testLines() throws Exception {
		doTest("lines", MultiLine.class);
	}

	@Test public void testMultilines() throws Exception {
		doTest("multilines", MultiLine.class);
	}
	
	@Test public void testMultipoint() throws Exception {
		doTest("multipoint", MultiPoint.class);
	}

	@Test public void testMultipolys() throws Exception {
		doTest("multipolys", MultiPolygons.class);                                                              
	}
	
	@Test public void testMultipolyz() throws Exception {
		doTest("multipolyz", MultiPolygons.class);
	}

	@Test public void testMultirings() throws Exception {
		doTest("multirings", MultiPolygons.class);
	}
	
	@Test public void testMultiringz() throws Exception {
		doTest("multiringz", MultiPolygons.class);
	}

	@Test public void testPolys() throws Exception {
		doTest("polys", MultiPolygons.class);
	}
	
	@Test public void testPolyz() throws Exception {
		doTest("polyz", MultiPolygons.class);
	}
   
	@Test public void testRings() throws Exception {
		doTest("rings", MultiPolygons.class);
	}

	@Test public void testRingz() throws Exception {
		doTest("ringz", MultiPolygons.class);
	}

	private void doTest(String file, Class geoclass) throws URISyntaxException, IOException {
		System.out.println("Test " + file);
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(shpdir, file);
		try {
			IGISObject ob = handler.read();
			assertTrue(ob instanceof Schema);
			int count = 0;
			while((ob = handler.read()) != null) {
				assertTrue(ob instanceof Feature);
				Feature feat = (Feature) ob;
				assertNotNull(feat.getGeometry());
				assertTrue(geoclass.isAssignableFrom(feat.getGeometry().getClass()));
				count++;
			}
			assertTrue(count > 0);
			System.out.println(" count=" + count);
		} finally {
			handler.close();
		}
	}
}

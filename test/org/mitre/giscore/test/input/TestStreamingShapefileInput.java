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

import java.io.*;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.EnumMap;

import org.junit.Test;
import static org.junit.Assert.*;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.shapefile.ShapefileComponent;
import org.mitre.giscore.input.shapefile.SingleShapefileInputHandler;

/**
 * Test single shapefile reader
 * 
 * @author DRAND
 * @author jgibson
 */
public class TestStreamingShapefileInput {
	public static File shpdir = new File("data/shape");
	
	@Test public void testReadShpDirectly() throws Exception {
		FileInputStream is = new FileInputStream(new File(shpdir, "afghanistan.shp"));
		SingleShapefileInputHandler sis = new SingleShapefileInputHandler(is, null, "afghanistan");
		IGISObject ob;
		while((ob = sis.read()) != null) {
			if (ob instanceof Feature) {
				Feature f = (Feature) ob;
				Geometry geo = f.getGeometry();
				assertTrue(geo instanceof MultiPolygons);
			}
		}
	}

	@Test public void testReadShpDirectly2() throws Exception {
		FileInputStream is = new FileInputStream(new File(shpdir, "linez.shp"));
		SingleShapefileInputHandler sis = new SingleShapefileInputHandler(is, null, "linez");
		IGISObject ob;
		while((ob = sis.read()) != null) {
			if (ob instanceof Feature) {
				Feature f = (Feature) ob;
				Geometry geo = f.getGeometry();
				assertTrue(geo instanceof Line);
			}
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testBadStream() throws Exception {
		new SingleShapefileInputHandler(new ByteArrayInputStream(new byte[0]), null, null);
	}
	
	@Test(expected=IOException.class)
	public void testBadStream2() throws Exception {
		new SingleShapefileInputHandler(new ByteArrayInputStream(new byte[0]), null, "foo");
	}

	@Test(expected=IOException.class)
	public void testBadStream3() throws Exception {
		new SingleShapefileInputHandler(new ByteArrayInputStream("not a shape file".getBytes()), null, "foo");
	}

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
		doTest("lines", Line.class);
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
		doTest("polys", Polygon.class);
	}
	
	@Test public void testPolyz() throws Exception {
		doTest("polyz", MultiPolygons.class);
	}
   
	@Test public void testRings() throws Exception {
		doTest("rings", LinearRing.class);
	}

	@Test public void testRingz() throws Exception {
		doTest("ringz", LinearRing.class);
	}
	
	@Test public void testAfghanistan() throws Exception {
		FileInputStream shp_is = new FileInputStream(new File(shpdir, "afghanistan.shp"));
		InputStream dbf_is = new FileInputStream(new File(shpdir, "afghanistan.dbf"));
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(shp_is, Collections.singletonMap(ShapefileComponent.DBF, dbf_is), "afghanistan");
		Schema sh = (Schema) handler.read();
		Feature shape = (Feature) handler.read();
		assertNotNull(shape);
		assertTrue(shape.getGeometry() instanceof MultiPolygons);
		IGISObject next = handler.read();
		assertNull(next);
	}
	
    // TODO? Replicate this test for the single shapefile input handler
//	@Test public void testShapefileInputStream3() throws Exception {
//		FileInputStream fis = new FileInputStream(new File(shpdir, "testLayersShp.zip"));
//		ZipInputStream zis = new ZipInputStream(fis);
//		IGISInputStream stream = GISFactory.getInputStream(DocumentType.Shapefile, zis);
//		
//		IGISObject ob;
//		while((ob = stream.read()) != null) {
//			System.out.println("(Zip) read: " + ob);
//		}
//	}

	private void doTest(String file, Class geoclass) throws URISyntaxException, IOException {
		System.out.println("Test " + file);
		File shp = new File(shpdir, file + ".shp");
		File dbf = new File(shpdir, file + ".dbf");
		File prj = new File(shpdir, file + ".prj");
        final EnumMap<ShapefileComponent, InputStream> map = new EnumMap<ShapefileComponent, InputStream>(ShapefileComponent.class);
        if(dbf.exists()) {
            map.put(ShapefileComponent.DBF, new FileInputStream(dbf));
        }
        if(prj.exists()) {
            map.put(ShapefileComponent.PRJ, new FileInputStream(prj));
        }
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(new FileInputStream(shp), map, file);
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

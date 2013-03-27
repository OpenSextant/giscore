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
import java.util.zip.ZipInputStream;

import org.junit.Test;
import static org.junit.Assert.*;

import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.IAcceptSchema;
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
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.shapefile.ShapefileInputStream;
import org.mitre.giscore.input.shapefile.SingleShapefileInputHandler;

/**
 * Test single shapefile reader
 * 
 * @author DRAND
 */
public class TestShapefileInput {
	public static final File shpdir = new File("data/shape");
	
	@Test public void testReadShpDirectly() throws Exception {
		FileInputStream is = new FileInputStream(new File(shpdir, "afghanistan.shp"));
		ShapefileInputStream sis = new ShapefileInputStream(is, null);
		assertNotNull(sis);
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
		ShapefileInputStream sis = new ShapefileInputStream(is, null);
		assertNotNull(sis);
		IGISObject ob;
		while((ob = sis.read()) != null) {
			if (ob instanceof Feature) {
				Feature f = (Feature) ob;
				Geometry geo = f.getGeometry();
				assertTrue(geo instanceof Line);
			}
		}
	}

	@Test(expected=IOException.class)
	public void testBadStream() throws Exception {
		new ShapefileInputStream(new ByteArrayInputStream(new byte[0]), null);
	}
	
	@Test(expected=IOException.class)
	public void testBadStream2() throws Exception {
		new ShapefileInputStream(new ByteArrayInputStream("not a shape file".getBytes()), null);
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
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(shpdir, "afghanistan");
		Schema sh = (Schema) handler.read();
		Feature shape = (Feature) handler.read();
		assertNotNull(shape);
		assertTrue(shape.getGeometry() instanceof MultiPolygons);
		IGISObject next = handler.read();
		assertNull(next);
	}
	
	@Test public void testShapefileInputStream() throws Exception {
		IAcceptSchema test = new IAcceptSchema() {
			@Override
			public boolean accept(Schema schema) {
				return schema.get("today") != null;
			}
		};
		
		IGISInputStream stream = GISFactory.getInputStream(DocumentType.Shapefile, shpdir, test);
		while(stream.read() != null) {
			// No body
		}
	}
	
	@Test public void testShapefileInputStream2() throws Exception {
		IGISInputStream stream = GISFactory.getInputStream(DocumentType.Shapefile, shpdir);
		while(stream.read() != null) {
			// No body
		}
	}
	
	@Test public void testShapefileInputStream3() throws Exception {
		FileInputStream fis = new FileInputStream(new File(shpdir, "testLayersShp.zip"));
		ZipInputStream zis = new ZipInputStream(fis);
		IGISInputStream stream = GISFactory.getInputStream(DocumentType.Shapefile, zis);
		
		IGISObject ob;
		while((ob = stream.read()) != null) {
			System.out.println("(Zip) read: " + ob);
		}
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

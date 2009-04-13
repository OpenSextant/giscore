/****************************************************************************************
 *  TestFileGdbSupport.java
 *
 *  Created: Feb 16, 2009
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
package org.mitre.giscore.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.test.input.TestKmlInputStream;

/**
 * Test file gdb output. Currently tests 5 and 6 are not working
 * 
 * @author DRAND
 * 
 */
public class TestGdbSupport extends TestGISBase {	
	/**
	 * Base path to test directories
	 */
	public static final String base_path = "data/kml/";

	@Test
	public void testMultiPointWithDate() throws Exception {
		File test = createTemp("t", ".zip");
		FileOutputStream fos = new FileOutputStream(test);
		IGISOutputStream os = null;
		ZipOutputStream zos = null;
		zos = new ZipOutputStream(fos);
		os = GISFactory.getOutputStream(DocumentType.FileGDB, zos, createTemp("t",".gdb"));
		
		SimpleField nameid = new SimpleField("nameid");
		nameid.setType(SimpleField.Type.INT);
		SimpleField dtm = new SimpleField("dtm");
		dtm.setType(SimpleField.Type.DATE);
		
		Schema s = new Schema();
		s.put(nameid);
		s.put(dtm);
		os.write(s);
		
		Feature f = new Feature();
		f.setSchema(s.getId());
		List<Point> pnts = new ArrayList<Point>();
		pnts.add(new Point(44.0, 33.0));
		pnts.add(new Point(44.1, 33.4));
		pnts.add(new Point(44.3, 33.3));
		pnts.add(new Point(44.2, 33.1));
		pnts.add(new Point(44.6, 33.2));
		MultiPoint mp = new MultiPoint(pnts);
		f.setGeometry(mp);
		f.putData(nameid, null);
		f.putData(dtm, new Date());
		os.write(f);
		
		f = new Feature();
		f.setSchema(s.getId());
		pnts = new ArrayList<Point>();
		pnts.add(new Point(44.5, 33.3));
		pnts.add(new Point(44.6, 33.1));
		pnts.add(new Point(44.7, 33.0));
		pnts.add(new Point(44.4, 33.4));
		pnts.add(new Point(44.2, 33.6));
		mp = new MultiPoint(pnts);
		f.setGeometry(mp);
		f.putData(nameid, 2);
		f.putData(dtm, new Date());
		os.write(f);
		
		os.close();
		zos.close();
		fos.close();
	}
	
	@Test
	public void test1f() throws Exception {
		InputStream s = TestKmlInputStream.class
				.getResourceAsStream("7084.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}

	@Test
	public void test1s() throws Exception {
		InputStream s = TestKmlInputStream.class
				.getResourceAsStream("7084.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test2af() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/straight.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}

	@Test
	public void test2as() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/straight.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test2bf() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/extruded.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}

	@Test
	public void test2bs() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/extruded.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test3a() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LinearRing/polygon-lr-all-modes.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}

	@Test
	public void test3as() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LinearRing/polygon-lr-all-modes.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test3bf() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Polygon/treasureIsland.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}

	@Test
	public void test3bs() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Polygon/treasureIsland.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test4() throws Exception {
		InputStream s = TestKmlInputStream.class
				.getResourceAsStream("KML_sample1.kml");
		doKmlTest(s, ".gdb", true, DocumentType.FileGDB);
	}
	
	@Test
	public void test4s() throws Exception {
		InputStream s = TestKmlInputStream.class
				.getResourceAsStream("KML_sample1.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	@Test
	public void test5() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "MultiGeometry/polygon-point.kml");
		doKmlTest(s, "", true, DocumentType.FileGDB);
	}

	@Test
	public void test6f() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "MultiGeometry/multi-linestrings.kml");
		doKmlTest(s, "", true, DocumentType.FileGDB);
	}
	@Test
	public void test6s() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "MultiGeometry/multi-linestrings.kml");
		doKmlTest(s, "", true, DocumentType.Shapefile);
	}

	
	public void doKmlTest(InputStream is, String suffix, boolean usezip,
			DocumentType type) throws IOException {
		IGISInputStream gisis = GISFactory.getInputStream(DocumentType.KML, is);
		doTest(gisis, suffix, usezip, type);
	}

	public void doTest(IGISInputStream is, String suffix, boolean usezip,
			DocumentType type) throws IOException {
		File outputdir = createTemp("test", suffix);
		File outputfile = null;
		if (usezip) {
			outputfile = new File(tempdir, "testout"
					+ System.currentTimeMillis() + ".zip");
		} else {
			outputfile = new File(tempdir, "testout"
					+ System.currentTimeMillis() + suffix);
		}
		OutputStream fos = new FileOutputStream(outputfile);
		IGISOutputStream os = null;
		ZipOutputStream zos = null;
		if (usezip) {
			zos = new ZipOutputStream(fos);
			os = GISFactory.getOutputStream(type, zos, outputdir);
		} else {
			os = GISFactory.getOutputStream(type, fos, outputdir);
		}
		for (IGISObject object = is.read(); object != null; object = is.read()) {
			os.write(object);
		}
		os.close();
		if (usezip) {
			zos.close();
		}
		fos.close();
	}
}

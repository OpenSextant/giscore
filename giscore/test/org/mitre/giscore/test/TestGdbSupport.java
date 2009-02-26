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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.test.input.TestKmlInputStream;

/**
 * Test file gdb output
 * 
 * @author DRAND
 * 
 */
public class TestGdbSupport {
	public static File tempdir = null;
	public static SimpleDateFormat FMT = new SimpleDateFormat("D-HH-mm-ss");
	
	static {
		// String dir = System.getProperty("java.io.tmpdir");
		tempdir = new File("c:/temp/", "t" + FMT.format(new Date()));
		tempdir.mkdirs();
	}
	
	public static final AtomicInteger count = new AtomicInteger();
	/**
	 * Base path to test directories
	 */
	public static final String base_path = "data/kml/";

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
		File outputdir = new File(tempdir, "test" + count.incrementAndGet()
				+ suffix);
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

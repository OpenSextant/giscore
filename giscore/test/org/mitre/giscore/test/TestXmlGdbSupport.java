/****************************************************************************************
 *  TestXmlGdbSupport.java
 *
 *  Created: Feb 11, 2009
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.test.input.TestKmlInputStream;


/**
 * Test inputting data from some KML sources and outputting to Gdb. 
 * 
 * Add input tests for Gdb once support is done for that format.
 * 
 * @author DRAND
 *
 */
public class TestXmlGdbSupport extends TestGISBase  {
	/**
	 * Base path to test directories
	 */
	public static final String base_path = "data/kml/";

	@Test public void test1() throws Exception {
		InputStream s = TestKmlInputStream.class.getResourceAsStream("7084.kml");
		doKmlTest(s);
	}

	 @Test
	public void test2() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/straight.kml");
		doKmlTest(s);
	}

	@Test
	public void test3() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LineString/extruded.kml");
		doKmlTest(s);
	}

	@Test
	public void test4() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Placemark/LinearRing/polygon-lr-all-modes.kml");
		doKmlTest(s);
	}

	@Test
	public void test5() throws Exception {
		InputStream s = new FileInputStream(base_path
				+ "Polygon/treasureIsland.kml");
		doKmlTest(s);
	}

	@Test
	public void test6() throws Exception {
		InputStream s = TestKmlInputStream.class
				.getResourceAsStream("KML_sample1.kml");
		doKmlTest(s);
	}
	
	public void doKmlTest(InputStream is) throws IOException {
		IGISInputStream gisis = GISFactory.getInputStream(DocumentType.KML, is);
		doTest(gisis);
	}
	
	public void doTest(IGISInputStream is) throws IOException {
		File test = createTemp("t", ".xml");
		FileOutputStream fos = new FileOutputStream(test);
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.XmlGDB, fos);
		for(IGISObject object = is.read(); object != null; object = is.read()) {
			os.write(object);
		}
		os.close();
		IOUtils.closeQuietly(fos);
	}
}

/****************************************************************************************
 *  TestKmlSupport.java
 *
 *  Created: Feb 5, 2009
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;


/**
 * @author DRAND
 *
 */
public class TestKmlSupport {
	/**
	 * Base path to test directories
	 */
	public static final String base_path = "mediate/data/kml/";
	
	@Test public void testAtom() throws Exception {
		runTestsOnDir("atom");
	}
	
	@Test public void testBalloon() throws Exception {
		runTestsOnDir("balloon");
	}
	
	@Test public void testBalloonStyle() throws Exception {
		runTestsOnDir("BalloonStyle");
	}

	@Test public void testCamera() throws Exception {
		runTestsOnDir("Camera");
	}
	
	@Test public void testExtendedData() throws Exception {
		runTestsOnDir("ExtendedData");
	}
	
	@Test public void testFeatureAnchor() throws Exception {
		runTestsOnDir("feature-anchor");
	}

	@Test public void testFeatureType() throws Exception {
		runTestsOnDir("FeatureType");
	}

	@Test public void testGroundOverlay() throws Exception {
		runTestsOnDir("GroundOverlay");
	}
	
	@Test public void testItemIcon() throws Exception {
		runTestsOnDir("ItemIcon");
	}
	
	@Test public void testLinkType() throws Exception {
		runTestsOnDir("LinkType");
	}

	@Test public void testLinkStyle() throws Exception {
		runTestsOnDir("LinkStyle");
	}
	
	@Test public void testlistview() throws Exception {
		runTestsOnDir("listview");
	}
	
	@Test public void tesMetadata() throws Exception {
		runTestsOnDir("Metadata");
	}
	
	@Test public void testMultiGeometry() throws Exception {
		runTestsOnDir("MultiGeometry");
	}
	
	@Test public void testNetworkLink() throws Exception {
		runTestsOnDir("NetworkLink");
	}
	
	@Test public void testPlacemark() throws Exception {
		runTestsOnDir("Placemark");
	}
	
	@Test public void testPolygon() throws Exception {
		runTestsOnDir("Polygon");
	}
	
	@Test public void testRegion() throws Exception {
		runTestsOnDir("Region");
	}	
	
	@Test public void testSchema() throws Exception {
		runTestsOnDir("Schema");
	}	
	
	@Test public void testsky() throws Exception {
		runTestsOnDir("sky");
	}	
	
	@Test public void testStyle() throws Exception {
		runTestsOnDir("Style");
	}	

	@Test public void testTime() throws Exception {
		runTestsOnDir("time");
	}	

	@Test public void testXmlns() throws Exception {
		runTestsOnDir("Xmlns");
	}	
	
	/**
	 * Iterate over the files in the directory
	 * @param dirname
	 * @throws IOException 
	 */
	private void runTestsOnDir(String dirname) throws IOException {
		File dir = new File(base_path, dirname);
		File contents[] = dir.listFiles();
		if (contents != null) {
			for(File testcase : contents) {
				if (testcase.isFile() && testcase.getName().endsWith(".kml")) {
					doTest(testcase);
				}
			}
		}
	}

	/**
	 * Do the actual test. The actual test reads in the original file, writes
	 * out the data, then compares the data for essential equality. This means
	 * that for features, the geometry is not compared precisely
	 * 
	 * @param testcase the file being checked
	 * @throws IOException 
	 */
	private void doTest(File testcase) throws IOException {
		System.out.println("Testing " + testcase);
		FileInputStream fs = new FileInputStream(testcase);
		IGISInputStream is = GISFactory.getInputStream(DocumentType.KML, fs);
		File temp = File.createTempFile("test", ".kml");
		OutputStream fos = new FileOutputStream(temp);
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos);
		List<IGISObject> elements = new ArrayList<IGISObject>();
		IGISObject current;
		while ((current = is.read()) != null) {
			os.write(current);
			elements.add(current);
		}

		is.close();
		fs.close();

		os.close();
		fos.close();

		// Test for equivalence
		fs = new FileInputStream(temp);
		is = GISFactory.getInputStream(DocumentType.KML, fs);
		int index = 0;
		while ((current = is.read()) != null) {
			if (index >= elements.size()) {
				assertTrue("Found at least one extra element " + current, false);
			}
			checkApproximatelyEquals(elements.get(index), current);
			index++;
		}
		is.close();
		fs.close();
		 
	}


	/**
	 * For most objects they need to be exactly the same, but for some we can 
	 * approximate equality
	 * 
	 * @param source
	 * @param test
	 */
	private void checkApproximatelyEquals(IGISObject source, IGISObject test) {
		if (Feature.class.isAssignableFrom(source.getClass()) && 
				Feature.class.isAssignableFrom(test.getClass())) {
			Feature sf = (Feature) source;
			Feature tf = (Feature) test;
			
			assertTrue(sf.approximatelyEquals(tf));			
		} else {
			assertEquals(source, test);
		}
		
	}
}

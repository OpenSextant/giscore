/****************************************************************************************
 *  TestKmlOutputStream.java
 *
 *  Created: Feb 4, 2009
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
package org.mitre.giscore.test.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.test.input.TestKmlInputStream;

/**
 * Test the output stream
 * 
 * @author DRAND
 * 
 */
public class TestKmlOutputStream {
	@Test
	public void testSimpleCase() throws Exception {
		doTest(TestKmlInputStream.class.getResourceAsStream("7084.kml"));
	}
	
	/**
	 * Note, this test fails due to some sort of issue with geodesy, but the
	 * actual output kml is fine.
	 * @throws Exception
	 */
	@Test
	public void testCase2() throws Exception {
		doTest(TestKmlInputStream.class.getResourceAsStream("KML_sample1.kml"));
	}
	
	@Test
	public void testCase3() throws Exception {
		doTest(TestKmlInputStream.class.getResourceAsStream("schema_example.kml"));
	}	

	public void doTest(InputStream fs) throws Exception {
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
			checkApproximatelyEquals(elements.get(index++), current);
		}
		is.close();
		fs.close();
		temp.delete();
	}
	
	/**
	 * For most objects they need to be exactly the same, but for some we can 
	 * approximate equality
	 * 
	 * @param source
	 * @param test
	 */
	private void checkApproximatelyEquals(IGISObject source, IGISObject test) {
		if (source instanceof Feature && test instanceof Feature) {
			Feature sf = (Feature) source;
			Feature tf = (Feature) test;
			
			boolean ae = sf.approximatelyEquals(tf);
			
			if (! ae) {		
				System.err.println("Source: " + source);
				System.err.println("Test: " + test);
				assertTrue("Found unequal objects", false);
			}
		} else {
			assertEquals(source, test);
		}
		
	}
}

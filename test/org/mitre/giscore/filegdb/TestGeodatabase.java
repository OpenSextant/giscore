/****************************************************************************************
 *  TestOpenCloseGeodatabase.java
 *
 *  Created: Oct 3, 2012
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2012
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
package org.mitre.giscore.filegdb;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestGeodatabase extends FileGDBTest {
	private static final String FEATURE_CLASS = "Feature Class";
	public static final String DB = "data/gdb/Shapes.gdb";
	
	@Test
	public void testGeodatabaseOpenAndClose() {
		// Open the database
		Geodatabase db = new Geodatabase(new File(DB));
		
		assertTrue(db.isValid());
		
		try {
			db.close();
		} catch(Exception e) {
			fail();
		}
	}
	
	@Test
	public void testGeoCreateAndDelete() throws IOException {
		File path = createTempDir(new File("c:/temp"), "temp", "gdb");
		Geodatabase db = new Geodatabase(path);
		assertTrue(db.isValid());
		db.delete();
	}

	@Test 
	public void testGetDatasetChildren() throws Exception {
		Geodatabase db = new Geodatabase(new File(DB));
		
		String children[] = db.getChildDatasets("\\", FEATURE_CLASS);
		assertTrue(children.length > 0);
		db.close();
	}
	
	@Test 
	public void testGetDatabaseTypes() throws Exception {
		Geodatabase db = new Geodatabase(new File(DB));
		
		String types[] = db.getDatasetTypes();
		assertTrue(types.length > 0);
		db.close();
	}

	@Test
	public void testGetDatasetDef() throws Exception {
		Geodatabase db = new Geodatabase(new File(DB));
		
		String docs[] = db.getChildDatasetDefinitions("\\", FEATURE_CLASS);
		assertTrue(docs.length > 0);
		db.close();
	}
	
	Random rand = new Random(System.currentTimeMillis());
	
	public File createTempDir(File parent, String prefix, String suffix) {
		while(true) {
			File test = new File(parent, prefix + rand.nextInt() + "." + suffix);
			if (test.exists()) continue;
			return test;
		}
	}
}

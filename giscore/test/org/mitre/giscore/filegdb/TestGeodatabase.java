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

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestGeodatabase extends FileGDBTest {
	@Test
	public void testGeodatabaseOpenAndClose() {
		// Open the database
		Geodatabase db = new Geodatabase(new File("data/gdb/EH_20090331144528.gdb"));
		
		assertTrue(db.isValid());
		
		try {
			db.close();
		} catch(Exception e) {
			fail();
		}
	}

}

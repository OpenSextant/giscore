/****************************************************************************************
 *  TestDbfInputStream.java
 *
 *  Created: Jun 23, 2009
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

import org.junit.Test;
import static org.junit.Assert.*;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.input.dbf.DbfInputStream;

public class TestDbfInputStream {
	@Test public void testDbfInputStream1() throws Exception {
		DbfInputStream dbfs = new DbfInputStream(new File("data/shape/Iraq.dbf"), new Object[0]);
		IGISObject obj = dbfs.read();
		assertNotNull(obj);
		assertTrue(obj instanceof Schema);
		Schema s = (Schema) obj;
		assertNotNull(s.getKeys());
		assertTrue(s.getKeys().size() > 1);
		obj = dbfs.read();
		while(obj != null) {
			assertNotNull(obj);
			assertTrue(obj instanceof Row);
			obj = dbfs.read();
		}
	}
	
	@Test public void testDbfInputStream2() throws Exception {
		DbfInputStream dbfs = new DbfInputStream(new File("data/shape/tl_2008_us_metdiv.dbf"), new Object[0]);
		IGISObject obj = dbfs.read();
		assertNotNull(obj);
		assertTrue(obj instanceof Schema);
		Schema s = (Schema) obj;
		assertNotNull(s.getKeys());
		assertTrue(s.getKeys().size() > 1);
		obj = dbfs.read();
		while(obj != null) {
			assertNotNull(obj);
			assertTrue(obj instanceof Row);
			obj = dbfs.read();
		}

	}
	
	@Test public void testDbfInputStream3() throws Exception {
		DbfInputStream dbfs = new DbfInputStream(new File("data/shape/AlleghenyCounty_Floodplain2000.dbf"), new Object[0]);
		IGISObject obj = dbfs.read();
		assertNotNull(obj);
		assertTrue(obj instanceof Schema);
		Schema s = (Schema) obj;
		assertNotNull(s.getKeys());
		assertTrue(s.getKeys().size() > 1);
		obj = dbfs.read();
		while(obj != null) {
			assertNotNull(obj);
			assertTrue(obj instanceof Row);
			obj = dbfs.read();
		}

	}	
}

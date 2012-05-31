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
        final File file = new File("data/shape/Iraq.dbf");
        if (!file.exists()) {
            System.err.println("file not found: " + file);
            return;
        }
        DbfInputStream dbfs = new DbfInputStream(file, new Object[0]);
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

    @Test public void testDbfInputStreamShort() throws Exception {
        final File file = new File("data/shape/MBTA.dbf");
        if (!file.exists()) {
            System.err.println("file not found: " + file);
            return;
        }
        DbfInputStream dbfs = new DbfInputStream(file, new Object[0]);
        IGISObject obj = dbfs.read();
        assertNotNull(obj);
        assertTrue(obj instanceof Schema);
        Schema s = (Schema) obj;
        /*
        <Schema name='schema_2' id='s_2'>
          <SimpleField name='OBJECTID' type='LONG'/>
          <SimpleField name='SOURCE' type='STRING'/>
          <SimpleField name='LINE' type='STRING'/>
          <SimpleField name='GRADE' type='SHORT'/>
          <SimpleField name='SHAPE_LEN' type='DOUBLE'/>
        </Schema>
         */
        assertNotNull(s.getKeys());
        assertTrue(s.getKeys().size() > 1);
        obj = dbfs.read();
        while (obj != null) {
            /*
            Row data=
            OBJECTID (LONG) = '1'
            SOURCE (STRING) = 'DLG'
            LINE (STRING) = 'SILVER'
            GRADE (SHORT) = '3'
            SHAPE_LEN (DOUBLE) = '4575.18028634'
             */
            assertNotNull(obj);
            assertTrue(obj instanceof Row);
            obj = dbfs.read();
        }
        dbfs.close();
    }

	@Test public void testDbfInputStream2() throws Exception {
        final File file = new File("data/shape/tl_2008_us_metdiv.dbf");
        if (!file.exists()) {
            System.err.println("file not found: " + file);
            return;
        }
        DbfInputStream dbfs = new DbfInputStream(file, new Object[0]);
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
        dbfs.close();
    }
	
	@Test public void testDbfInputStream3() throws Exception {
        final File file = new File("data/shape/AlleghenyCounty_Floodplain2000.dbf");
        if (!file.exists()) {
            System.err.println("file not found: " + file);
            return;
        }
        DbfInputStream dbfs = new DbfInputStream(file, new Object[0]);
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

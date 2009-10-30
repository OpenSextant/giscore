/****************************************************************************************
 *  TestObjectDataSerialization.java
 *
 *  Created: Oct 28, 2009
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
package org.mitre.giscore.test.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Date;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.events.Common;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import static org.junit.Assert.*;

/**
 * Test individual object data serialization 
 * @author DRAND
 *
 */
public class TestObjectDataSerialization {
	@Test public void testRowAndTypes() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100);
		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(bos);
		Row r = new Row();
		SimpleField f1 = new SimpleField("a", Type.BOOL);
		SimpleField f2 = new SimpleField("b", Type.DATE);
		SimpleField f3 = new SimpleField("c", Type.DOUBLE);
		SimpleField f4 = new SimpleField("d", Type.FLOAT);
		SimpleField f5 = new SimpleField("e", Type.INT);
		SimpleField f6 = new SimpleField("f", Type.SHORT);
		SimpleField f7 = new SimpleField("g", Type.STRING);
		SimpleField f8 = new SimpleField("h", Type.UINT);
		SimpleField f9 = new SimpleField("i", Type.USHORT);
		r.putData(f1, true);
		r.putData(f2, new Date());
		r.putData(f3, RandomUtils.nextDouble());
		r.putData(f4, RandomUtils.nextFloat());
		r.putData(f5, RandomUtils.nextInt(100));
		r.putData(f6, RandomUtils.nextInt(100));
		r.putData(f7, "str" + RandomUtils.nextInt(100));
		r.putData(f8, RandomUtils.nextInt(100));
		r.putData(f9, RandomUtils.nextInt(100));
		soos.writeObject(r);
		soos.close();
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		SimpleObjectInputStream sois = new SimpleObjectInputStream(bis);
		Row r2 = (Row) sois.readObject();
		assertEquals(r, r2);
	}
	
	@Test public void testContainerStart() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100);
		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(bos);
		ContainerStart c = new ContainerStart();
		c.setName("cs1");
		c.setDescription("desc1");
		c.setStartTime(new Date(1));
		c.setEndTime(new Date(2));
		c.setSchema(new URI("urn:xyz"));
		c.setStyleUrl("#style1");
		c.setVisibility(true);
		c.setType("foo");
		soos.writeObject(c);
		soos.close();
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		SimpleObjectInputStream sois = new SimpleObjectInputStream(bis);
		ContainerStart c2 = (ContainerStart) sois.readObject();
		assertEquals(c, c2);
	}
	
	@Test public void testX() throws Exception {
//		ByteArrayOutputStream bos = new ByteArrayOutputStream(100);
//		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(bos);
//		
//		soos.writeObject(r);
//		soos.close();
//		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
//		SimpleObjectInputStream sois = new SimpleObjectInputStream(bis);
//		Row r2 = (Row) sois.readObject();
//		assertEquals(r, r2);
	}
}

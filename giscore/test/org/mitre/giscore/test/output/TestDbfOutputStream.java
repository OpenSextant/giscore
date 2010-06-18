/****************************************************************************************
 *  TestDbfOutputStream.java
 *
 *  Created: Jul 16, 2009
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.input.dbf.DbfInputStream;
import org.mitre.giscore.output.dbf.DbfOutputStream;

/**
 * 
 */
public class TestDbfOutputStream {
	Random rand = new Random();
	
	@Test public void testDbfOutputStreamString() throws Exception {
		Schema s = new Schema();
		SimpleField s1 = new SimpleField("s1");
		s1.setLength(5);
		SimpleField s2 = new SimpleField("s2");
		s2.setLength(10);
		SimpleField s3 = new SimpleField("s3");
		s3.setLength(253);
		
		s.put(s1);
		s.put(s2);
		s.put(s3);
		
		File temp = File.createTempFile("test", ".dbf");
		FileOutputStream os = new FileOutputStream(temp);
		DbfOutputStream dbfos = new DbfOutputStream(os, null);
		dbfos.write(s);
		List<Row> data = new ArrayList<Row>();
		for(int i = 0; i < 50; i++) {
			Row r = new Row();
			r.putData(s1, randomString(s1));
			r.putData(s2, randomString(s2));
			r.putData(s3, randomString(s3));
			data.add(r);
			dbfos.write(r);
		}
		dbfos.close();
		os.close();
		
		FileInputStream is = new FileInputStream(temp);
		DbfInputStream dbfis = new DbfInputStream(is, null);
		Schema readschema = (Schema) dbfis.read();
		assertNotNull(readschema);
		assertEquals(3, readschema.getKeys().size());
		compare(s1, readschema.get("s1"));
		compare(s2, readschema.get("s2"));
		compare(s3, readschema.get("s3"));
		for(int i = 0; i < 50; i++) {
			Row readrow = (Row) dbfis.read();
			Row origrow = data.get(i);
			compare(s, readschema, origrow, readrow);
		}
	}
	
	@Test public void testDbfOutputStreamNumeric() throws Exception {
		Schema s = new Schema();
		SimpleField b = new SimpleField("b", Type.BOOL);
		SimpleField f = new SimpleField("f", Type.FLOAT);
		SimpleField db = new SimpleField("db", Type.DOUBLE); 
		SimpleField it = new SimpleField("it", Type.INT);
		SimpleField sh = new SimpleField("sh", Type.SHORT);
		SimpleField ui = new SimpleField("ui", Type.UINT);
		SimpleField us = new SimpleField("us", Type.USHORT);
		SimpleField oid = new SimpleField("oid", Type.OID);
		
		s.put(b);
		s.put(f);
		s.put(db);
		s.put(it);
		s.put(sh);
		s.put(ui);
		s.put(us);
		s.put(oid);
		
		File temp = File.createTempFile("test", ".dbf");
		FileOutputStream os = new FileOutputStream(temp);
		DbfOutputStream dbfos = new DbfOutputStream(os, null);
		dbfos.write(s);
		List<Row> data = new ArrayList<Row>();
		for(int i = 0; i < 50; i++) {
			Row r = new Row();
			r.putData(b, RandomUtils.nextBoolean());
			r.putData(f, RandomUtils.nextFloat());
			r.putData(db, RandomUtils.nextFloat());
			r.putData(it, RandomUtils.nextInt(10000000));
			r.putData(sh, (short) RandomUtils.nextInt(10000));
			r.putData(ui, Math.abs(RandomUtils.nextInt(10000000)));
			r.putData(us, (short) Math.abs(RandomUtils.nextInt(10000)));
			r.putData(oid, Math.abs(RandomUtils.nextInt(10000000)));
			data.add(r);
			dbfos.write(r);
		}
		dbfos.close();
		os.close();
		
		FileInputStream is = new FileInputStream(temp);
		DbfInputStream dbfis = new DbfInputStream(is, null);
		Schema readschema = (Schema) dbfis.read();
		assertNotNull(readschema);
		assertEquals(8, readschema.getKeys().size());
		compare(b, readschema.get("b"));
		compare(f, readschema.get("f"));
		compare(db, readschema.get("db"));
		compare(it, readschema.get("it"));
		compare(sh, readschema.get("sh"));
		compare(ui, readschema.get("ui"));
		compare(us, readschema.get("us"));
		compare(oid, readschema.get("oid"));

		for(int i = 0; i < 50; i++) {
			Row readrow = (Row) dbfis.read();
			Row origrow = data.get(i);
			compare(s, readschema, origrow, readrow);
		}
	}
	
	@Test public void testDbfOutputStreamDate() throws Exception {
		Schema s = new Schema();
		SimpleField date = new SimpleField("date", Type.DATE);
		s.put(date);
		
		File temp = File.createTempFile("test", ".dbf");
		FileOutputStream os = new FileOutputStream(temp);
		DbfOutputStream dbfos = new DbfOutputStream(os, null);
		dbfos.write(s);
		List<Row> data = new ArrayList<Row>();
		for(int i = 0; i < 50; i++) {
			Row r = new Row();
			Date d = new Date(System.currentTimeMillis() 
					- (200L * RandomUtils.nextInt()));
			// System.out.println("Date: " + d);
			r.putData(date, d);
			data.add(r);
			dbfos.write(r);
		}
		dbfos.close();
		os.close();
		
		FileInputStream is = new FileInputStream(temp);
		DbfInputStream dbfis = new DbfInputStream(is, null);
		Schema readschema = (Schema) dbfis.read();
		assertNotNull(readschema);
		assertEquals(1, readschema.getKeys().size());
		compare(date, readschema.get("date"));
		for(int i = 0; i < 50; i++) {
			Row readrow = (Row) dbfis.read();
			Row origrow = data.get(i);
			compare(s, readschema, origrow, readrow);
		}
	}

	private void compare(SimpleField orig, SimpleField read) {
		assertEquals(orig.getName(), read.getName());
		// Not really correct as the lengths from the dbf file are generally longer
		// than the normal lengths. Still correct for strings
		if (orig.getType().equals(Type.STRING))
			assertEquals(orig.getLength(), read.getLength());
		// Types won't always match because there are destructive mappings
		// assertEquals(orig.getType(), read.getType());
	}

	private void compare(Schema s, Schema readschema, Row origrow, Row readrow) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		assertNotNull(readrow);
		assertEquals(origrow.getFields().size(), readrow.getFields().size());
		for(String key : s.getKeys()) {
			SimpleField field = s.get(key);
			Object v1 = origrow.getData(field);
			SimpleField readfield = readschema.get(key);
			Object v2 = readrow.getData(readfield);
			if (v1 instanceof Number) { 
				assertEquals(((Number) v1).doubleValue(), ((Number) v2).doubleValue(), 1e-6);
			} else if (v1 instanceof Date) {
				int y1, y2, m1, m2, d1, d2;
				cal.setTimeInMillis(((Date) v1).getTime());
				y1 = cal.get(Calendar.YEAR);
				m1 = cal.get(Calendar.MONTH);
				d1 = cal.get(Calendar.DAY_OF_MONTH);
				cal.setTimeInMillis(((Date) v2).getTime());
				y2 = cal.get(Calendar.YEAR);
				m2 = cal.get(Calendar.MONTH);
				d2 = cal.get(Calendar.DAY_OF_MONTH);
				assertEquals("YEAR field", y1, y2);
				assertEquals("MONTH field", m1, m2);
				assertEquals("DAY_OF_MONTH field", d1, d2);
			} else {
				assertEquals(v1, v2);
			}
		}
	}

	private String randomString(SimpleField s1) {
		int len = s1.getLength();
		if (rand.nextFloat() < .3) {
			len -= rand.nextInt(len / 2);
		}
		StringBuilder b = new StringBuilder(len);
		for(int i = 0; i < len; i++) {
			int ch;
			int m = i % 10;
			if (m == 0) 
				ch = 'A' + (i / 10);
			else
				ch = '0' + m;
			b.append((char) ch);
		}
		return b.toString();
	}
}

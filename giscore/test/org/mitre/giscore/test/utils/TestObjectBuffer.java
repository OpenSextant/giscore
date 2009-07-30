/****************************************************************************************
 *  TestObjectBuffer.java
 *
 *  Created: Jul 15, 2009
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.ObjectBuffer;

/**
 * Test object buffers for several different scenarios
 * 
 * @author DRAND
 * 
 */
public class TestObjectBuffer {
	public static final int max = 10;
	
	@Test
	public void test1() throws Exception {
		int count = max - 1;
		ObjectBuffer buffer = new ObjectBuffer(max);
		IDataSerializable objects[] = setupTest(count, buffer);
		doTest(objects, buffer);
	}
	
	@Test
	public void test2() throws Exception {
		int count = max;
		ObjectBuffer buffer = new ObjectBuffer(max);
		IDataSerializable objects[] = setupTest(count, buffer);
		doTest(objects, buffer);
	}
	
	@Test
	public void test3() throws Exception {
		int count = max + 1;
		ObjectBuffer buffer = new ObjectBuffer(max);
		IDataSerializable objects[] = setupTest(count, buffer);
		doTest(objects, buffer);
	}
	
	@Test
	public void test4() throws Exception {
		int count = max * 2;
		ObjectBuffer buffer = new ObjectBuffer(max);
		IDataSerializable objects[] = setupTest(count, buffer);
		doTest(objects, buffer);
	}	
	
	@Test
	public void testTimed() throws Exception {
		ObjectBuffer buffer = new ObjectBuffer(10000);
		
		long start = System.nanoTime();
		setupTest(10000, buffer);
		long end = System.nanoTime();
		long millis = (end - start) / 1000000;
		System.out.println("Storing the first 10000 elements to memory took " + millis + " ms");
		
		start = System.nanoTime();
		setupTest(10000, buffer);
		end = System.nanoTime();
		millis = (end - start) / 1000000;
		System.out.println("Storing the next 10000 elements to file took " + millis + " ms");
		
		start = System.nanoTime();
		for(int i = 0; i < 10000; i++) {
			buffer.read();
		}
		end = System.nanoTime();
		millis = (end - start) / 1000000;
		System.out.println("Reading the first 10000 elements from memory took " + millis + " ms");
		
		start = System.nanoTime();
		for(int i = 0; i < 10000; i++) {
			buffer.read();
		}
		end = System.nanoTime();
		millis = (end - start) / 1000000;
		System.out.println("Reading the next 10000 elements from file took " + millis + " ms");
	}

	private IDataSerializable[] setupTest(int count, ObjectBuffer buffer)
			throws URISyntaxException, IOException {
		URI suri = new URI("urn:mitre:test:uri1");
		Schema s = new Schema(suri);
		SimpleField text = new SimpleField("text");
		text.setLength(100);
		s.put(text);

		IDataSerializable objects[] = new IDataSerializable[count];
		for (int i = 0; i < count; i++) {
			Row r = new Row();
			r.setSchema(suri);
			r.putData(text, "test text " + i);
			objects[i] = r;
			buffer.write(r);
		}

		return objects;
	}

	private void doTest(IDataSerializable[] objects, ObjectBuffer buffer)
			throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		assertEquals(objects.length, buffer.count());

		// Check each object against the buffer
		for (int i = 0; i < objects.length; i++) {
			IDataSerializable retrieved = buffer.read();
			assertNotNull(retrieved);
			assertEquals(objects[i], retrieved);
		}
		assertNull(buffer.read());

		buffer.close();
	}

}

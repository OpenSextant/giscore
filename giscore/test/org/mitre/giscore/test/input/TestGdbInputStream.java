/****************************************************************************************
 *  TestGdbInputStream.java
 *
 *  Created: Mar 18, 2009
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.IAcceptSchema;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.input.IGISInputStream;


/**
 * @author DRAND
 *
 */
public class TestGdbInputStream {
	static class TestAccept implements IAcceptSchema {
		private URI accept = null;
		
		public TestAccept(URI accept) {
			this.accept = accept;
		}
		
		/* (non-Javadoc)
		 * @see org.mitre.giscore.IAcceptSchema#accept(org.mitre.giscore.events.Schema)
		 */
		public boolean accept(Schema schema) {
			System.err.println(schema.getId());
			return accept.equals(schema.getId());
		}
		
	}
	
	class TestInThread implements Runnable {

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				System.err.println("Begin thread " + Thread.currentThread());
				testFileGdbInput();
			} catch (Exception e) {
				fail("Thread failed: " + e.getLocalizedMessage());
			} finally {
				System.err.println("End thread " + Thread.currentThread());
			}
		}
	}
	
	
	@Test public void testFileGdbInput() throws Exception {
		IGISInputStream gis = GISFactory.getInputStream(DocumentType.FileGDB, 
				new File("data/gdb/EH_20090331144528.gdb"),
				new TestAccept(new URI("urn:EHFC_20090331144528")));
		int schema_count = 0;
		int total = 0;
		IGISObject gisobject = null;
		SimpleField lpath = null;
		while((gisobject = gis.read()) != null) {
			total++;
			if (gisobject instanceof Schema) {
				Schema s = (Schema) gisobject;
				if (lpath == null) {
					lpath = s.get("lpath");
				}
				schema_count++;
				assertTrue(s.getKeys() != null && s.getKeys().size() > 0);
				assertNotNull(s.getOidField());
				assertNotNull(lpath);
			} else if (gisobject instanceof Feature) {
				Feature f = (Feature) gisobject;
				String path = (String) f.getData(lpath);
				assertNotNull(path);
			}
		}
		assertEquals(1, schema_count);
	}
	
	@Test public void testFileGdbZipStream() throws Exception {
		File temp = File.createTempFile("test", ".zip");
		FileOutputStream fos = new FileOutputStream(temp);
		File input = new File("data/gdb/EH_20090331144528.gdb");
		ZipOutputStream zos = new ZipOutputStream(fos);
		for(File file : input.listFiles()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				ZipEntry entry = new ZipEntry(file.getCanonicalPath());
				zos.putNextEntry(entry);
				IOUtils.copy(fis, zos);
				IOUtils.closeQuietly(fis);
			} catch(Exception e) {
				// Ignore
			}
		}
		IOUtils.closeQuietly(zos);
		IOUtils.closeQuietly(fos);
		
		FileInputStream fis = new FileInputStream(temp);
		IGISInputStream is = GISFactory.getInputStream(DocumentType.FileGDB, fis);
		int count = 0;
		while(is.read() != null) {
			count++;
		}
		assertTrue(count > 0);
		IOUtils.closeQuietly(fis);
		is.close();
		temp.delete();
	}
	
	@Test public void testMultiThread() throws Exception {
		Thread t1 = new Thread(new TestInThread());
		Thread t2 = new Thread(new TestInThread());
		t1.start();
		t2.start();
		t1.join();
		t2.join();
	}
	
//	/** 
//	 * Present to test specific file from EH, but the file is not checked 
//	 * into the tree. Uncomment to use.
//	 * 
//	 * @throws Exception
//	 */
//	@Test public void testFileGdbInput2() throws Exception {
//		IGISInputStream gis = GISFactory.getInputStream(DocumentType.FileGDB, 
//				new File("data/gdb/eh_fgdb92_20090314153313.gdb"),
//				new TestAccept(new URI("urn:EHFC_20090314153313")));
//		int schema_count = 0;
//		int total = 0;
//		IGISObject gisobject = null;
//		SimpleField lpath = null;
//		String lastpath = null;
//		while((gisobject = gis.read()) != null) {
//			total++;
//			if (gisobject instanceof Schema) {
//				Schema s = (Schema) gisobject;
//				lpath = s.get("lpath");
//				schema_count++;
//				assertTrue(s.getKeys() != null && s.getKeys().size() > 0);
//				assertNotNull(s.getOidField());
//				assertNotNull(lpath);
//			} else if (gisobject instanceof Feature) {
//				Feature f = (Feature) gisobject;
//				String path = (String) f.getData(lpath);
//				assertNotNull(path);
//			}
//		}
//		System.err.println("Schema count is " + schema_count);
//		System.err.println("Total objects is " + total);
//	}
}

/****************************************************************************************
 *  TestBlockDataTransfer.java
 *
 *  Created: May 4, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
package org.mitre.giscore.test.impl;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.impl.BlockInputStream;
import org.mitre.giscore.impl.BlockOutputStream;

/**
 * Test block transfer streams. Note - you can't test cases that are large
 * enough to trigger the blocking queue without creating a separate thread or
 * you'll block execution.
 * 
 * @author DRAND
 */
public class TestBlockDataTransfer {
	@Test public void testSmallSingle() throws Exception {
		callableSingleByteCase(1000);
	}
	
	@Test public void testSmall() throws Exception {
		callableBlockCase(1000);
	}
	
	@Test public void testOffsetSmall() throws Exception {
		int size = 1000;
		byte[] testdata1 = randomBytes(size);
		BlockOutputStream bos = new BlockOutputStream();
		bos.write(testdata1, 500, 500);
		bos.close();
		byte[] testcorrect = new byte[500];
		for(int i = 0; i < 500; i++) {
			testcorrect[i] = testdata1[i + 500];
		}

		BlockInputStream bis = new BlockInputStream(bos);
		byte[] testoutput1 = new byte[size];
		bis.read(testoutput1);
		bis.close();		
		compareBytes(testcorrect, testoutput1);
	}
	
	@Test public void testOffsetLarge() throws Exception {
		int size = 100000;
		byte[] testdata1 = randomBytes(size);
		BlockOutputStream bos = new BlockOutputStream();
		int extent = size - 500;
		bos.write(testdata1, 500, extent);
		bos.close();
		byte[] testcorrect = new byte[extent];
		for(int i = 0; i < extent; i++) {
			testcorrect[i] = testdata1[i + 500];
		}

		BlockInputStream bis = new BlockInputStream(bos);
		byte[] testoutput1 = new byte[extent];
		bis.read(testoutput1);
		bis.close();		
		compareBytes(testcorrect, testoutput1);
	}
	
	@Test public void testBlockSingleSize() throws Exception {
		callableSingleByteCase(65536);
	}
	
	@Test public void testBlockSize() throws Exception {
		callableBlockCase(65536);
	}
	
	@Test public void testBlockSizePlus() throws Exception {
		callableBlockCase(65537);
	}
	
	@Test public void testBigger() throws Exception {
		callableBlockCase(100000);
	}
	
	@Test public void testReallyBigger() throws Exception {
		callableBlockCase(100000);
	}
	
	
	public void callableBlockCase(int size) throws Exception {
		byte[] testdata1 = randomBytes(size);
		BlockOutputStream bos = new BlockOutputStream();
		bos.write(testdata1);
		bos.close();
		
		BlockInputStream bis = new BlockInputStream(bos);
		byte[] testoutput1 = new byte[size];
		bis.read(testoutput1);
		bis.close();
		
		compareBytes(testdata1, testoutput1);
	}	
	
	public void callableSingleByteCase(int size) throws Exception {
		byte[] testdata1 = randomBytes(size);
		BlockOutputStream bos = new BlockOutputStream();
		for(int i = 0; i < size; i++) {
			bos.write(testdata1[i]);
		}
		bos.close();
		
		BlockInputStream bis = new BlockInputStream(bos);
		byte[] testoutput1 = new byte[size];
		bis.read(testoutput1);
		bis.close();
		
		compareBytes(testdata1, testoutput1);
	}	

	private boolean compareBytes(byte a[], byte b[]) {
		if (a == null && b == null) {
			return true;
		} else if (a == null) {
			return false;
		} else if (b == null) {
			return false;
		} else if (a.length != b.length) {
			return false;
		} else {
			for(int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	private byte[] randomBytes(int len) {
		byte data[] = new byte[len];
		for(int i = 0; i < len; i++) {
			data[i] = (byte) RandomUtils.nextInt(255);
		}
		return data;
	}
}

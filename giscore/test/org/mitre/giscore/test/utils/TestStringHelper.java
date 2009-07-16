/****************************************************************************************
 *  TestStringHelper.java
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
package org.mitre.giscore.test.utils;

import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.utils.StringHelper;


public class TestStringHelper {
	@Test public void testFull() throws Exception {
		doTest("abcdefghijk", "abcdefghijk");
	}
	
	@Test public void testRemovedVowels() throws Exception {
		doTest("abcdefghijkl", "abcdfghjkl");
	}
	
	@Test public void testTruncated() throws Exception {
		doTest("abcdefghijklmnopqrst", "abcdfghjklm");
	}

	private void doTest(String input, String expected) {
		byte result[] = StringHelper.esriFieldName(input);
		assertEquals(expected.length(), result.length);
		for(int i = 0; i < expected.length(); i++) {
			byte b = result[i];
			char ch = expected.charAt(i);
			assertEquals((byte) ch, b);
		}
	}
}

/****************************************************************************************
 *  TestPair.java
 *
 *  Created: Oct 3, 2011
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2011
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

import static org.junit.Assert.*;

import org.junit.Test;
import org.mitre.giscore.utils.Pair;

public class TestPair {

	@Test
	public void test() {
		Pair<Integer,Long> simple = new Pair<Integer,Long>(101, 123L);
		assertNotNull(simple.getFirst());
		assertNotNull(simple.getSecond());
		assertEquals(101L, simple.getFirst().longValue());
		assertEquals(123L, simple.getSecond().longValue());
		
		Pair<Integer,Long> dup = new Pair<Integer,Long>(101, 123L);
		assertEquals(simple, dup);
		assertEquals(simple.hashCode(), dup.hashCode());
	}

}

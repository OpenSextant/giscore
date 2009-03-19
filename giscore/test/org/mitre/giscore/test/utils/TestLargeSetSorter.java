/****************************************************************************************
 *  TestLargeSetSorter.java
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
package org.mitre.giscore.test.utils;

import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.utils.SortMerge;

/**
 * 
 * @author DRAND
 * 
 */
public class TestLargeSetSorter {
	@Test
	public void testOneBufferSet() throws Exception {
		SortMerge sorter = new SortMerge(100);
		doTest(sorter, 50);
	}

	@Test
	public void testTwoBufferSet() throws Exception {
		SortMerge sorter = new SortMerge(100);
		doTest(sorter, 150);
	}

	@Test
	public void testThreeBufferSet() throws Exception {
		SortMerge sorter = new SortMerge(100);
		doTest(sorter, 250);
	}
	
	@Test
	public void testLargerBufferSet() throws Exception {
		SortMerge sorter = new SortMerge(40000);
		doTest(sorter, 200000);
		System.gc();
	}	

	@Test
	public void testTimingLargeSet() throws Exception {
		SortMerge bigmodel = new SortMerge(40000);
		int count = bigmodel.getMaxInMemory() * 100;
		long start = System.nanoTime();
		doTest(bigmodel, count);
		long end = System.nanoTime();

		long millis = (end - start) / 1000000;
		System.out.println("Took " + millis + "ms to insert and sort " + 
				fmt.format(count) + " rows");

	}

	public void doTest(SortMerge sorter, int total) throws Exception {
//		printCurrentMemory("before test for " + total + " count");
		for (int i = 0; i < total; i++) {
			Object tuple[] = new Object[2];
			tuple[0] = "a" + RandomUtils.nextInt(1000) + "b"
					+ RandomUtils.nextInt(500);
			tuple[1] = i;
			sorter.add(tuple);
		}
		System.gc();
//		printCurrentMemory("after load");
		Object last[] = null;
		Object current[] = null;
		Iterator<Object[]> iter = sorter.iterator();
		int count = 0;
		while (iter.hasNext()) {
			current = iter.next();
			count++;
			if (last != null) {
				Comparable a = (Comparable) current[0];
				assertTrue(a.compareTo(last[0]) >= 0);
			}
			last = current;
		}
		sorter.dispose();
		assertEquals(total, count);
		System.gc();
		printCurrentMemory("after test");
	}
	
	static final DecimalFormat fmt = new DecimalFormat("###,###");
	
	public void printCurrentMemory(String label) {
		long max = Runtime.getRuntime().maxMemory();
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		long inuse = total - free;
		System.err.println(label);
//		System.err.println("Max    " + fmt.format(max));
//		System.err.println("Total  " + fmt.format(total));
//		System.err.println("Free   " + fmt.format(free));
		System.err.println("In use " + fmt.format(inuse));
		System.err.println("---");
	}
}

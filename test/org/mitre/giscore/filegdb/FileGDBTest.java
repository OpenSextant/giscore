/****************************************************************************************
 *  FileGDBTest.java
 *
 *  Created: Oct 3, 2012
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2012
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
package org.mitre.giscore.filegdb;

import org.junit.BeforeClass;

public class FileGDBTest {
	@BeforeClass public static void openLib() {
		Runtime rt = Runtime.getRuntime();	
		rt.loadLibrary("filegdb");
	}
}

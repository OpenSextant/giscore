/****************************************************************************************
 *  Geodatabase.java
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

import java.io.File;
import java.util.List;

/**
 * Wraps the FileGDB API Geodatabase Object and presents the 
 * needed operations on the Geodatabase Object so they can be executed
 * from Java.
 * 
 * @author DRAND
 */
public class Geodatabase extends GDB {	
	/**
	 * Ctor
	 * 
	 * @param pathToGeodatabase the geodatabase, must not be <code>null</code>
	 * and must exist
	 */
	public Geodatabase(File pathToGeodatabase) {
		if (pathToGeodatabase == null || ! pathToGeodatabase.exists()) {
			throw new IllegalArgumentException("Bad path");
		}
		ptr = open(pathToGeodatabase.getAbsolutePath());
	}
	
	/**
	 * Close the geodatabase, will do nothing if the database is already 
	 * closed
	 */
	public void close() {
		if (ptr != 0L) {
			close_db(ptr);
		}
	}

	public native String[] getChildDatasets(String datasettype);
		
	public native String[] getDatasetTypes();
	
	public native String[] getRelatedDatasets();
	
	public native String getDatasetDef(String datasettype);
	
	public native String getDatasetDocumentation(String datasettype);
	
	/**
	 * Open the geodatabase
	 * 
	 * @param absolutePath path to the geodatabase, assumed to be valid
	 * @return
	 */
	private native long open(String absolutePath);
	
	/**
	 * Close the geodatabase (internal)
	 * @param ptr 
	 */
	private native void close_db(long ptr);  
	

}

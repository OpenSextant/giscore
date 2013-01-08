/****************************************************************************************
 *  Geodatabase.java
 *
 *  Created: Oct 30, 2012
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

/**
 * Wraps the FileGDB API Geodatabase Object and presents the 
 * needed operations on the Geodatabase Object so they can be executed
 * from Java.
 * 
 * @author DRAND
 */
public class Geodatabase extends GDB {

	private File path;
	
	static {
		System.loadLibrary("filegdb");
	}
	
	/**
	 * Ctor
	 * 
	 * @param pathToGeodatabase the geodatabase, must not be <code>null</code>
	 * and must exist
	 */
	public Geodatabase(File pathToGeodatabase) {
		if (pathToGeodatabase == null) {
			throw new IllegalArgumentException("Bad path");
		}
		if (pathToGeodatabase.exists()) {
			ptr = open(pathToGeodatabase.getAbsolutePath());
		} else {
			ptr = create(pathToGeodatabase.getAbsolutePath());
		}
		path = pathToGeodatabase;
	}
	
	/**
	 * Delete the current Geodatabase, closing it if it is currently 
	 * open.
	 */
	public void delete() {
		close();
		delete(path.getAbsolutePath());
	}
	
	/**
	 * Close the geodatabase. If already closed do nothing.
	 */
	public void close() {
		if (isValid()) close1();
		ptr = 0L;
	}
	
	private native long open(String path);
	
	private native long create(String path);
	
	private native void close1();
		
	public static native void delete(String path);
	
	public native String[] getDatasetTypes();
	
	public native String[] getChildDatasets(String parentPath, String child);
	
	public native String[] getChildDatasetDefinitions(String parentPath, String child);
}

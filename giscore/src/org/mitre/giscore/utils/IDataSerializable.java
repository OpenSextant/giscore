/****************************************************************************************
 *  IDataSerializable.java
 *
 *  Created: Mar 23, 2009
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
package org.mitre.giscore.utils;

import java.io.IOException;

/**
 * @author DRAND
 *
 */
public interface IDataSerializable {

	/**
	 * Read object from the data stream.
	 * 
	 * @param in
	 *            the input stream, never <code>null</code>
	 * 
	 * @throws IOException  if an I/O error occurs.
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException;

	/**
	 * Write the object to the data stream
	 * 
	 * @param out
	 *            the output stream, never <code>null</code>
	 * @throws  IOException  if an I/O error occurs.
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException;
}
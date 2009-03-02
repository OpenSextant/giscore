/****************************************************************************************
 *  IGISOutputStream.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.output;

import java.io.IOException;

import org.mitre.giscore.events.IGISObject;

/**
 * A stream that accepts GIS objects to be consumed. Generally it consumes
 * GIS objects to be written to a given sink.
 * 
 * @author DRAND
 */
public interface IGISOutputStream {
	/**
	 * Write the given object.
	 * 
	 * @param object the object to be written, never <code>null</code>.
	 */
	void write(IGISObject object);
	
	/**
	 * Close the stream and release any resources.
	 * @throws IOException 
	 */
	void close() throws IOException;
}
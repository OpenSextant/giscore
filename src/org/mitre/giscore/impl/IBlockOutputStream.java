/****************************************************************************************
 *  IBlockOutputStream.java
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
package org.mitre.giscore.impl;

import java.io.InputStream;

/**
 * A blocking output interface.
 * 
 * @author DRAND
 */
public interface IBlockOutputStream {
	/**
	 * Get buffer, used to retrieve all the buffers allocated while 
	 * writing data using this stream. These buffers are turned back into 
	 * an {@link InputStream} using {@link BlockInputStream}.
	 * 
	 * @return the next buffer
	 */
	public byte[] getNextBuffer();
}

/****************************************************************************************
 *  BlockInputStream.java
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
package org.opensextant.giscore.impl;

import java.io.IOException;
import java.io.InputStream;

/**
 * Block stream that matches the block output stream to read from the remote
 * stream and reconstitutes the data on the client side. This stream does not 
 * need to be serializable.
 * 
 * @author DRAND
 */
public class BlockInputStream extends InputStream {
	/**
	 * Position in the input buffer
	 */
	private int pos = 0;
	
	/**
	 * The input buffer
	 */
	private byte[] buffer;
	
	private final IBlockOutputStream blockOutputStream;
	
	public BlockInputStream(IBlockOutputStream blockOutputStream) {
		if (blockOutputStream == null) {
			throw new IllegalArgumentException(
					"blockOutputStream should never be null");
		}
		this.blockOutputStream = blockOutputStream;
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (checkBuffer())
			return (0xFF & buffer[pos++]);
		else
			return -1;
	}
	
	

	/**
	 * Check and make sure that we have a valid buffer with at least one
	 * byte available. If the buffer is not available this method will try
	 * to fetch the next buffer and make it available. The next buffer must
	 * still have at least a byte available for this method to return <code>true</code>.
	 * 
	 * @return <code>true</code> if the buffer is available and has at least
	 * a byte available
	 */
	
	private boolean checkBuffer() {
		if (buffer == null || buffer.length - pos <= 0) {
			buffer = blockOutputStream.getNextBuffer();
			pos = 0;
		}
		return buffer != null && buffer.length > 0;
	}

}

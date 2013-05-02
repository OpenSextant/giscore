/****************************************************************************************
 *  RemoteAsyncRetriever.java
 *
 *  Created: May 5, 2010
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
package org.opensextant.giscore.output.remote;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.opensextant.giscore.impl.BlockInputStream;
import org.opensextant.giscore.impl.IBlockOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data must be retrieved from the remote stream asynchronously as the data is 
 * written to the stream and may block if the memory stream blocks at capacity.
 * @author DRAND
 */
public class RemoteAsyncRetriever implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(RemoteAsyncRetriever.class);
	
	private IBlockOutputStream realStream;
	private OutputStream outputStream;

	/**
	 * Ctor
	 * @param blockStream
	 * @param os
	 */
	RemoteAsyncRetriever(IBlockOutputStream blockStream, OutputStream os) {
		if (blockStream == null) {
			throw new IllegalArgumentException("blockStream should never be null");
		}
		if (os == null) {
			throw new IllegalArgumentException("os should never be null");
		}
		realStream = blockStream;
		outputStream = os;
	}

	@Override
	public void run() {
		// Copy the remote results to the local filesystem
		BlockInputStream blockInputStream = null;
		try {
			blockInputStream = new BlockInputStream(realStream); 
			IOUtils.copy(blockInputStream, outputStream);
			outputStream.flush();
		} catch (IOException e) {
			logger.error("Problem while copying data from remote source", e);
		} finally {
			IOUtils.closeQuietly(blockInputStream);
			IOUtils.closeQuietly(outputStream);
		}
		
	}

}

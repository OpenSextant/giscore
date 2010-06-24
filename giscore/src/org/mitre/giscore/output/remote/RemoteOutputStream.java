/****************************************************************************************
 *  RemoteOutputStream.java
 *
 *  Created: May 3, 2010
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
package org.mitre.giscore.output.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.IRemoteableGISOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy stream that buffers content between a proxy stream user an a remote
 * implementation. 
 * 
 * @author DRAND
 *
 */
public class RemoteOutputStream implements IGISOutputStream {
	private static final Logger logger = LoggerFactory.getLogger(RemoteOutputStream.class);  
	private final int BUFFER_COUNT = 100;
	private static final AtomicInteger threadCount = new AtomicInteger();
	
	private List<IGISObject> buffer = new ArrayList<IGISObject>();	
	private IRemoteableGISOutputStream realStream;
	private OutputStream outputStream;
	private Thread retriever = null;
	
	public RemoteOutputStream(IRemoteableGISOutputStream realStream, OutputStream outputStream) {
		if (realStream == null) {
			throw new IllegalArgumentException("realStream should never be null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException("outputStream should never be null");
		}
		this.realStream = realStream;
		this.outputStream = outputStream;
	}

	public void write(IGISObject object) {
		if (retriever == null) {
			// Start the async retriever, which will run until we're done
			RemoteAsyncRetriever ar = new RemoteAsyncRetriever(realStream, outputStream);
			retriever = new Thread(ar, "asyncRetriever-" + threadCount.incrementAndGet());
			retriever.start();
		}
		try {
			buffer.add(object);
			if (buffer.size() > BUFFER_COUNT) {
				realStream.write(buffer);
				buffer.clear();
			}
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() throws IOException {
		// Force completion of the actual work on the remote end
		if (buffer.size() > 0) {
			realStream.write(buffer);
			buffer.clear();
		}
		realStream.close(); 
		// Wait for the thread to complete as long as the write method
		// has been called at least once
		if (retriever != null) {
			try {
				retriever.join();
			} catch (InterruptedException e) {
				logger.warn("Retriever was interrupted");
			}
		}
	}
}

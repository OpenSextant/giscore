/****************************************************************************************
 *  ProxyOutputStream.java
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
package org.mitre.giscore.output.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.impl.BlockOutputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.IRemoteableGISOutputStream;

/**
 * The proxy output stream implements the remoteable aspect of the stream. The
 * stream will have been given a special blocking output stream that holds the data and 
 * allows the client to pull the data in sections back to the client. The proxy
 * is never returned back to the client - the client is given a reference to
 * the proxy which is used in subsequent calls to a proxy handler service,
 * which prevents issues with the proxy having to be serialized back.
 * 
 * @author DRAND
 */
public class ProxyOutputStream implements IRemoteableGISOutputStream {
	private IGISOutputStream stream = null;
	private BlockOutputStream blockOutputStream = null;
	private OutputStream inuseStream = null;
	
	public ProxyOutputStream(IGISOutputStream streamToProxy, BlockOutputStream blockOutputStream, OutputStream inuseStream) {
		if (streamToProxy == null) {
			throw new IllegalArgumentException(
					"streamToProxy should never be null");
		}
		if (blockOutputStream == null) {
			throw new IllegalArgumentException("blockOutputStream should never be null");
		}
		if (inuseStream == null) {
			throw new IllegalArgumentException(
					"inuseStream should never be null");
		}
		stream = streamToProxy;
		this.blockOutputStream = blockOutputStream;
		this.inuseStream = inuseStream;
	}
	
	public void write(List<IGISObject> objectList) throws RemoteException {
		for(IGISObject object : objectList) {
			stream.write(object);
		}
	}

	public void close() throws IOException {
		stream.close();
		IOUtils.closeQuietly(inuseStream);
		IOUtils.closeQuietly(blockOutputStream);
	}

	@Override
	public byte[] getNextBuffer() { 
		return blockOutputStream.getNextBuffer();
	}

}

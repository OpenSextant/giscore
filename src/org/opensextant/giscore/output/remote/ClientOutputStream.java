/****************************************************************************************
 *  ClientOutputStream.java
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
import java.rmi.RemoteException;
import java.util.List;

import org.opensextant.giscore.IRemoteGISService;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.output.IRemoteableGISOutputStream;

/**
 * Local implementation that handles the client stub side. It makes the calls
 * to the remote service transparent.
 * 
 * @author DRAND
 *
 */
public class ClientOutputStream implements IRemoteableGISOutputStream {
	private IRemoteGISService service;
	private Long streamId;
	
	
	public ClientOutputStream(IRemoteGISService service, Long streamId) {
		if (service == null) {
			throw new IllegalArgumentException(
					"service should never be null");
		}
		if (streamId == null) {
			throw new IllegalArgumentException(
					"streamId should never be null");
		}
		this.service = service;
		this.streamId = streamId;
	}
	
	@Override
	public void write(List<IGISObject> objectList) throws RemoteException {
		service.write(streamId, objectList);
	}

	@Override
	public byte[] getNextBuffer() {
		try {
			byte[] rval = service.getNextBuffer(streamId);
			return rval;
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		service.close(streamId);
	}

	@Override
	protected void finalize() {
		try {
			service.dispose(streamId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

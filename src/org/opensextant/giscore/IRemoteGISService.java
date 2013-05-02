/****************************************************************************************
 *  IRemoteGISFactory.java
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
package org.opensextant.giscore;

import java.rmi.RemoteException;
import java.util.List;

import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.output.IRemoteableGISOutputStream;
import org.opensextant.giscore.output.remote.ClientOutputStream;

/**
 * A remote interface that allows access to a set of methods to create and use
 * remote streams.
 * 
 * @author DRAND
 */
public interface IRemoteGISService {
	/**
	 * Create a remote output stream
	 * 
	 * @param type
	 *            the type of the remote output stream. Only types that
	 *            implement {@link IRemoteableGISOutputStream} can be returned
	 *            by this method.
	 * @param arguments
	 *            the arguments expected by the specific stream implementation
	 * @return a reference to the remote stream, never <code>null</code>
	 * @throws RemoteException
	 *             if there's an error, one specific error would be trying to
	 *             create a document type that doesn't support a remote stream
	 */
	Long makeGISOutputStream(DocumentType type, Object... arguments)
			throws RemoteException;

	/**
	 * Write a set of objects to the remote output stream. 
	 * 
	 * @param streamId
	 *            the stream id, never <code>null</code>, returned by
	 *            {@link #makeGISOutputStream(DocumentType, Object...)}, never
	 *            <code>null</code>
	 * @param objectList a list of objects to be written, never <code>null</code>
	 * @throws RemoteException
	 */
	void write(Long streamId, List<IGISObject> objectList)
			throws RemoteException;

	/**
	 * Close the given stream. 
	 * 
	 * @param streamId the stream id, never <code>null</code>
	 * @throws RemoteException
	 */
	void close(Long streamId) throws RemoteException;

	/**
	 * Retrieve the results from the given stream. This should be called until
	 * it returns <code>null</code> to indicate that there are no more 
	 * results. This can be called in parallel with {@link #write(Long, List)}.
	 * 
	 * @param streamId the stream id, never <code>null</code>.
	 * @return the next block of result data or <code>null</code> if there
	 * is no more data to return. The call will block if there is no more data
	 * ready.
	 * @throws RemoteException
	 */
	byte[] getNextBuffer(Long streamId) throws RemoteException;
	
	/**
	 * Dispose the underlying stream. This is called automatically by 
	 * {@link ClientOutputStream} when the object is finalized. You should
	 * not call it explicitly.
	 * 
	 * @param streamId the stream id, never <code>null</code>.
	 */
	void dispose(Long streamId);
}

package org.opensextant.giscore.output;

import java.io.Closeable;
import java.rmi.RemoteException;
import java.util.List;

import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.impl.IBlockOutputStream;

/****************************************************************************************
 *  IRemoteableGISOutputStream.java
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

/**
 * We need to have some output streams implemented on a remote server in order
 * to use ESRI's libraries on a Windows platform while implementing our actual
 * implementation on a non-Windows platform. This interface will be implemented
 * by those streams to make them amenable to become RMI callable. The method
 * is the same as the method in {@link IGISOutputStream} except that it can
 * throw {@link RemoteException}. A wrapper makes this stream compatible with
 * {@link IGISOutputStream} for callers that are not aware of the remoting.
 */
public interface IRemoteableGISOutputStream extends IBlockOutputStream, java.rmi.Remote, Closeable {
	/**
	 * Write the given objects.
	 * 
	 * @param objectList the objects to be written, never <code>null</code>.
	 */
	void write(List<IGISObject> objectList) throws RemoteException;
	

}

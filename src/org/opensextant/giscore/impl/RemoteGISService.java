/****************************************************************************************
 *  RemoteGISFactory.java
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
package org.opensextant.giscore.impl;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipOutputStream;

import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.GISFactory;
import org.opensextant.giscore.IRemoteGISService;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.output.IContainerNameStrategy;
import org.opensextant.giscore.output.esri.GdbOutputStream;
import org.opensextant.giscore.output.remote.ProxyOutputStream;

/**
 * A remote factory to create RMI callable streams
 * 
 * @author DRAND
 */
public class RemoteGISService extends GISFactory implements IRemoteGISService  {
	private static final AtomicLong streamId = new AtomicLong(0);
	private static final Map<Long, ProxyOutputStream> activeStreams =
		new ConcurrentHashMap<Long, ProxyOutputStream>();
	
	public Long makeGISOutputStream(DocumentType type, Object... arguments)
			throws RemoteException {
		if (type == null) {
			throw new RemoteException("type should never be null");
		}
		try {
			IContainerNameStrategy strategy;
			if(type == DocumentType.FileGDB) {
				checkArguments(new Class[] { File.class,
						IContainerNameStrategy.class }, arguments,
						new boolean[] { false, false });
				strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
						: null);
				BlockOutputStream bos = new BlockOutputStream();
				ZipOutputStream zos = new ZipOutputStream(bos);
				Long id = streamId.incrementAndGet();
				ProxyOutputStream stream = new ProxyOutputStream(new GdbOutputStream(type, zos,
						(File) null, strategy), bos, zos);
				activeStreams.put(id, stream);
				return id;
            } else {
				throw new RemoteException("Unsupported document type " + type.name());
			}
		} catch (IOException e) {
			throw new RemoteException("Problem in making GISStream", e);
		}
	}

	public void close(Long streamId) throws RemoteException {
		if (streamId == null) {
			throw new RemoteException(
					"streamId should never be null");
		}
		ProxyOutputStream stream = activeStreams.get(streamId);
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				throw new RemoteException(e.getLocalizedMessage(), e);
			}
		}
	}

	public byte[] getNextBuffer(Long streamId) throws RemoteException {
		if (streamId == null) {
			throw new RemoteException(
					"streamId should never be null");
		}
		ProxyOutputStream stream = activeStreams.get(streamId);
		if (stream != null) {
			return stream.getNextBuffer();
		} else {
			throw new RemoteException("Unknown stream " + streamId);
		}
	}

	public void write(Long streamId, List<IGISObject> objectList)
			throws RemoteException {
		if (streamId == null) {
			throw new RemoteException(
					"streamId should never be null");
		}
		ProxyOutputStream stream = activeStreams.get(streamId);
		if (stream != null) {
			stream.write(objectList);
		} else {
			throw new RemoteException("Unknown stream " + streamId);
		}
	}

	public void dispose(Long streamId) {
		if (streamId == null) {
			return;
		}
		ProxyOutputStream stream = activeStreams.get(streamId);
		activeStreams.remove(streamId);
	}
}

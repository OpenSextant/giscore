/****************************************************************************************
 *  BufferedObjectStream.java
 *
 *  Created: Mar 18, 2009
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
package org.mitre.giscore.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.commons.io.IOUtils;

/**
 * An object stream reader that manages an in-memory buffer to allow more
 * efficient disk usage. It will always try to refill the in-memory queue after
 * dropping below the threshold.
 * 
 * @author DRAND
 */
public class BufferedObjectStream {
	int lowCount;
	int highCount;
	Queue<Object> buffer;
	File bufferFile;
	FileInputStream istream;
	ObjectInputStream stream;
	boolean streamIsOpen;

	/**
	 * ctor
	 * 
	 * @param file the file containing objects to be buffered, never
	 *            <code>null</code> and must exist
	 * @param low
	 *            the low count on which to refill the queue, must be smaller
	 *            than the high count
	 * @param high
	 *            the high count, which is the maximum count of in memory
	 *            elements to hold
	 * @throws IOException 
	 */
	public BufferedObjectStream(File file, int low, int high) throws IOException {
		if (file == null || ! file.exists()) {
			throw new IllegalArgumentException("file should never be null and must exist");
		}
		if (high <= low) {
			throw new IllegalArgumentException("high must be larger than low");
		}
		bufferFile = file;
		istream = new FileInputStream(bufferFile);
		stream = new ObjectInputStream(istream);
		lowCount = low;
		highCount = high;
		streamIsOpen = true;
		buffer = new ArrayDeque<Object>(highCount);
	}

	/**
	 * @return the next element from the buffer, or <code>null</code> if there
	 *         are no more elements in the buffer and if the object input stream
	 *         is empty
	 */
	public Object read() {
		ensureQueue();
		if (buffer.isEmpty())
			return null;
		else
			return buffer.remove();
	}

	/**
	 * @return the next element from the buffer without removing it, or
	 *         <code>null</code> if there are no more elements in the buffer and
	 *         if the object input stream is empty
	 */
	public Object peek() {
		ensureQueue();
		return buffer.peek();
	}
	
	/**
	 * Close the buffer stream, closing the contained object stream and clearing
	 * any in memory data.
	 */
	public void close() {
		IOUtils.closeQuietly(stream);
		IOUtils.closeQuietly(istream);
		stream = null;
		istream = null;
		streamIsOpen = false;
		buffer.clear();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * 
	 */
	private void ensureQueue() {
		if (buffer.size() <= lowCount && streamIsOpen) {
			while(buffer.size() <= highCount) {
				try {
					Object next = stream.readObject();
					buffer.add(next);
				} catch (IOException e) {
					streamIsOpen = false;
					return;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Problem reading from stream", e);
				}
			}
		}
	}

}

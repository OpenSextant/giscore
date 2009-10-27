/****************************************************************************************
 *  ObjectBuffer.java
 *
 *  Created: Jul 15, 2009
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
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A buffer that will hold a fixed amount of data in memory, 
 * and overflows into secondary storage if there's too much data 
 * to hold in memory (i.e. it overflows into a file). 
 * 
 * @author DRAND
 *
 */
public class ObjectBuffer {	
	/**
	 * Default number of elements to allow in memory.
	 */
	private static final int DEFAULT_SIZE = 2000;

	/**
	 * The maximum number of buffered elements to hold in
	 * memory before overflowing into secondary storage.
	 */
	private int maxElements;
	
	/**
	 * A file pointer to the secondary store, remains 
	 * <code>null</code> until there is data in the secondary 
	 * store.
	 */
	private File secondaryStore;
	
	/**
	 * The simple object output stream used to write
	 * object to the secondary store.
	 */
	private SimpleObjectOutputStream outputStream;
	
	/**
	 * The simple object input stream used to read objects
	 * from the secondary store.
	 */
	private SimpleObjectInputStream inputStream;
	
	/**
	 * The actual data buffer, allocated on the
	 * first store operation.
	 */
	private IDataSerializable buffer[];
	
	/**
	 * The store index, which is the total count of elements
	 * stored into the virtual buffer. Values .ge. to maxElements
	 * indicate elements that are stored in the file.
	 */
	private int storeIndex = 0;
	
	/**
	 * The read pointer into the buffer or the file.
	 */
	private int readIndex = 0;
	
	public ObjectBuffer() {
		this(DEFAULT_SIZE);
	}
	
	/**
	 * Ctor
	 * @param size the maximum count of elements held in memory, must
	 * be a positive integer.
	 */
	public ObjectBuffer(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("size must be positive");
		}
		maxElements = size;
		buffer = new IDataSerializable[size];
	}
	
	/**
	 * Close any and all streams, delete any temporary file 
	 * and dispose of buffered data.
	 * @throws IOException 
	 */
	public void close() throws IOException {
		try {
			closeOutputStream();
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		} finally {
			buffer = null;
			readIndex = 0;
			storeIndex = 0;
			if (secondaryStore != null) {
				secondaryStore.delete();
			}
		}
	}
	
	/**
	 * Close any open output streams but leave the data alone
	 * @throws IOException 
	 */
	public void closeOutputStream() throws IOException {
		if (outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
	}
	
	/**
	 * Either put the given object in memory, or into secondary storage
	 * if memory already has used the available storage.
	 * @param object the non-<code>null</code> value to store in 
	 * the buffer.
	 * @throws IOException 
	 */
	public void write(IDataSerializable object) throws IOException {
		if (object == null) {
			throw new IllegalArgumentException("object should never be null");
		}
		if (storeIndex < maxElements) {
			buffer[storeIndex] = object;
		} else {
			if (secondaryStore == null) {
				secondaryStore = File.createTempFile("obj", ".buffer");
				outputStream = new SimpleObjectOutputStream(new FileOutputStream(secondaryStore));
			}
			outputStream.writeObject(object);
		}
		storeIndex++;
	}
	
	/**
	 * Read objects from memory or the secondary storage.
	 * @return the object or <code>null</code> when the objects
	 * have been exhausted.
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public IDataSerializable read() throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		try {
			if (readIndex >= storeIndex) {
				return null;
			} else if (readIndex < maxElements) {
				return buffer[readIndex];
			} else {
				if (inputStream == null && secondaryStore != null) {
					inputStream = new SimpleObjectInputStream(new FileInputStream(secondaryStore));
				}
				if (inputStream != null)
					return (IDataSerializable) inputStream.readObject();
				else
					throw new IllegalStateException("Beyond the buffer with no secondaryStore");
			}
		} finally {
			readIndex++;
		}
	}
	
	/**
	 * @return the count of objects that have been stored in the buffer to this point.
	 */
	public int count() {
		return storeIndex;
	}

	/**
	 * Reset the read pointer to the start of the buffer
	 */
	public void resetReadIndex() {
		readIndex = 0;
	}

}

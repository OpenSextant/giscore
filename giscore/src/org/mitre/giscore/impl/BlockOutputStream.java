/****************************************************************************************
 *  BlockOutputStream.java
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
package org.mitre.giscore.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An output stream that allows the remote implementation to provide output
 * to the client via data blocks. The output stream receives data and stores
 * it in records that the RMI infrastructure can transfer via method calls
 * in an efficient manner. The records are then removed.
 * 
 * @author DRAND
 *
 */
public class BlockOutputStream extends OutputStream implements Serializable, IBlockOutputStream {
	
	private static final long serialVersionUID = 1L;
	private static final int MAX_BUFFER_SIZE = 65536;
	private static final int CAPACITY = 10;
	
	/**
	 * Each buffer holds up to MAX_BUFFER_SIZE bytes of data. The buffers are
	 * transferred as a block to the client. Buffers are allocated all at once
	 * and the current buffer is not held in this queue. 
	 * 
	 * The queue only holds CAPACITY count of buffers before it blocks the output
	 * stream. This will cause the output stream to wait for the corresponding 
	 * input stream to start reading data before allowing the local process to 
	 * continue to output more data.
	 */
	private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(CAPACITY);
	
	/**
	 * The current buffer
	 */
	private byte[] current;
	
	/**
	 * The current insert position into the current buffer. After the buffer
	 * is closed the position will be the size of the final buffer since it 
	 * will point to the position immediately after the last byte inserted.
	 */
	private int pos = 0;
	
	/**
	 * Set to true when the buffer is closed
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	public BlockOutputStream() {
		newBuffer();
	}

	public void close() throws IOException {
		if (closed.get()) return;
		closed.set(true);
		// Resize the final buffer to just the used size
		byte finalbuffer[] = new byte[pos];
		for(int i = 0; i < pos; i++) {
			finalbuffer[i] = current[i];
		}
		try {
			queue.put(finalbuffer);
		} catch (InterruptedException e) {
			throw new IOException("Queue interrupted while recording current buffer");
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if (b == null) {
		    throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) ||
			   ((off + len) > b.length) || ((off + len) < 0)) {
		    throw new IndexOutOfBoundsException();
		} else if (len == 0) {
		    return;
		}
		
		int end = off + len;
		int start = off;
		int rem = MAX_BUFFER_SIZE - pos;
		while(len > rem) { 
			for(int i = 0; i < rem; i++) {
				current[pos + i] = b[start + i];
			}
			try {
				queue.put(current);
			} catch (InterruptedException e) {
				throw new IOException("Queue interrupted while recording current buffer");
			}
			newBuffer();
			start += rem;
			rem = MAX_BUFFER_SIZE;
			len = end - start;
		}
		if (len > 0) {
			for(int i = 0; i < len; i++) {
				current[pos + i] = b[start + i];
			}
			pos += len;
		}
	}

	/**
	 * Create a new buffer
	 */
	private void newBuffer() {
		current = new byte[MAX_BUFFER_SIZE];
		pos = 0;
	}

	public void write(int b) throws IOException {
		byte thebyte = (byte) b;
		int rem = MAX_BUFFER_SIZE - pos;
		if (rem == 0) {
			try {
				queue.put(current);
			} catch (InterruptedException e) {
				throw new IOException("Queue interrupted while recording current buffer");
			}
			newBuffer();
		}
		current[pos++] = thebyte;
	}
	
	/**
	 * Get buffer, used to retrieve all the buffers allocated while 
	 * writing data using this stream. 
	 * @return the next buffer
	 */
	public byte[] getNextBuffer() {
		while(closed.get() == false && queue.peek() == null) {
			// Block while open and no data in the queue
			synchronized(this) {
				try {
					wait(500);
				} catch (InterruptedException e) {
					throw new RuntimeException("getNextBuffer interrupted while waiting to close", e);
				}
			}
		}
		// If the queue has no data now, it must be closed and therefore we
		// have no data to return. Otherwise we'll have data since we waited
		// until peek returned something
		while(true) {
			try {
				return queue.poll(0, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException("getNextBuffer interrupted while polling", e);
			}
		}
	}
}

/****************************************************************************************
 *  LargeSetSorter.java
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * The sorter takes tuples of one or more objects. The tuples are grouped into
 * segments that are manageable in memory and these segments are first sorted
 * and then either saved directly into the initial buffer file, or merged with
 * the current buffer file after being sorted in memory.
 * <p>
 * Sorting is performed by comparing those elements of the array that implement
 * the <code>Comparable</code> interface. It is assumed that the tuples are of
 * uniform length and composition. A null element is considered less than all
 * others.
 * <p>
 * The data in the buffer file can be retrieved in sort order.
 * <p>
 * The user must call the dispose method when done with the large set to remove
 * the buffer file that remains.
 * <p>
 * This is not a thread safe class, nor is the returned iterator.
 * 
 * @author DRAND
 */
public class SortMerge {
	public static class TupleSorter implements Comparator<Object[]> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public int compare(Object[] o1, Object[] o2) {
			if (o1 == null) {
				throw new IllegalArgumentException("o1 should never be null");
			}
			if (o2 == null) {
				throw new IllegalArgumentException("o2 should never be null");
			}
			if (o1.length != o2.length) {
				throw new IllegalArgumentException(
						"tuples must have the same length");
			}
			for (int i = 0; i < o1.length; i++) {
				Object a = o1[i];
				Object b = o2[i];
				if (a instanceof Comparable) {
					int c = ((Comparable) a).compareTo(b);
					if (c < 0 || c > 0) {
						return c;
					}
				}
			}
			return 0;
		}

	}

	static TupleSorter sorter = new TupleSorter();

	static Comparator<BufferedObjectStream> scomparator = new Comparator<BufferedObjectStream>() {
		@Override
		public int compare(BufferedObjectStream o1, BufferedObjectStream o2) {
			Object a[] = (Object[]) o1.peek();
			Object b[] = (Object[]) o2.peek();

			if (a == null)
				return 1; // Push empty buffers to end
			else if (b == null)
				return -1;
			else
				return sorter.compare(a, b);
		}

	};

	public class LargeSetIterator implements Iterator<Object[]> {
		/**
		 * The streams that provide access to the buffer files.
		 */
		private BufferedObjectStream stream;

		/**
		 * Ctor - initialize the streams, placing them so the peek() returns
		 * data in sort order.
		 * 
		 * @throws IOException
		 */
		public LargeSetIterator() throws IOException {
			stream = new BufferedObjectStream(buffer, 50, 100);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			if (stream == null) {
				throw new IllegalStateException("Stream is closed");
			}
			return stream.peek() != null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Object[] next() {
			if (stream == null) {
				throw new IllegalStateException("Stream is closed");
			}
			return (Object[]) stream.read();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Cannot remove");
		}

		/**
		 * Close all associated streams
		 */
		public void close() {
			if (stream != null) {
				stream.close();
			}
			stream = null;
		}

	}

	/**
	 * The maximum number of objects held in memory before sorting and writing
	 * to a temporary file buffer.
	 */
	private int maxInMemory;

	/**
	 * In memory storage used to hold up to {@link #maxInMemory} objects. Once
	 * more than this count is held, the first {@link #maxInMemory} objects are
	 * sorted and saved to a buffer file.
	 */
	private List<Object[]> inMemory;

	/**
	 * We keep a list around so we can make sure the streams are closed before
	 * deleting the files. Weak references are used to ensure that we don't keep
	 * these in memory.
	 */
	private List<WeakReference<LargeSetIterator>> iterators = new ArrayList<WeakReference<LargeSetIterator>>();

	/**
	 * Buffer file that contains the set.
	 */
	File buffer;

	public SortMerge(int maxInMemory) throws IOException {
		if (maxInMemory < 1) {
			throw new IllegalArgumentException("max in memory must be positive");
		}
		setMaxInMemory(maxInMemory);
		inMemory = new ArrayList<Object[]>();
		buffer = null;
	}

	/**
	 * Add a single tuple to the collection
	 * 
	 * @param tuple
	 *            the tuple, never <code>null</code>
	 * @throws IOException
	 */
	public void add(Object[] tuple) throws IOException {
		if (tuple == null) {
			throw new IllegalArgumentException("tuple should never be null");
		}
		add(Collections.singletonList(tuple));
	}

	/**
	 * Add the tuples to the collection.
	 * 
	 * @param tuples
	 *            the tuples to be added, never <code>null</code>, may be
	 *            modified as part of the call
	 * @throws IOException
	 */
	public void add(Collection<Object[]> tuples) throws IOException {
		if (tuples == null) {
			throw new IllegalArgumentException("tuples should never be null");
		}
		int total = inMemory.size() + tuples.size();
		if (total > maxInMemory) {
			int delta = maxInMemory - inMemory.size();
			Iterator<Object[]> titer = tuples.iterator();
			for (int i = 0; i < delta && titer.hasNext(); i++) {
				inMemory.add(titer.next());
				titer.remove();
			}
			merge();
			add(tuples); // Add the remainder
		} else {
			inMemory.addAll(tuples);
		}
	}

	/**
	 * Merge the current in memory data with the current buffer, writing to a
	 * new buffer, clearing the collection and removing the old buffer.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void merge() throws IOException, FileNotFoundException {
		if (inMemory.isEmpty())
			return;
		Collections.sort(inMemory, sorter);
		File temp = File.createTempFile("sorter", ".idx");
		FileOutputStream fos = new FileOutputStream(temp);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		BufferedObjectStream stream = null;
		Iterator<Object[]> titer = inMemory.iterator();
		try {
			if (buffer == null) {
				while (titer.hasNext()) {
					oos.writeObject(titer.next());
				}
			} else {
				stream = new BufferedObjectStream(buffer, 50, 100);
				while (titer.hasNext()) {
					Object[] nextInMemory = titer.next();
					Object[] nextInStream = (Object[]) stream.peek();
					while (nextInStream != null
							&& sorter.compare(nextInStream, nextInMemory) < 0) {
						nextInStream = (Object[]) stream.read();
						oos.writeObject(nextInStream);
						nextInStream = (Object[]) stream.peek();
					}
					oos.writeObject(nextInMemory);
				}
				Object[] nextInStream = (Object[]) stream.read();
				// Finish copying old file
				while (nextInStream != null) {
					oos.writeObject(nextInStream);
					nextInStream = (Object[]) stream.read();
				}
			}
		} finally {
			inMemory.clear();
			IOUtils.closeQuietly(oos);
			IOUtils.closeQuietly(fos);
			if (stream != null) stream.close();
			if (buffer != null) buffer.delete();
			buffer = temp;
		}
	}

	/**
	 * Clean up
	 */
	public void dispose() {
		inMemory.clear();
		for (WeakReference<LargeSetIterator> ref : iterators) {
			LargeSetIterator iter = ref.get();
			if (iter != null) {
				iter.close();
			}
		}
		if (buffer != null) {
			buffer.delete();
			buffer = null;
		}
	}

	/**
	 * @return an iterator to the sorted collection, never <code>null</code>
	 * @throws IOException
	 */
	public Iterator<Object[]> iterator() throws IOException {
		merge();
		LargeSetIterator rval = new LargeSetIterator();
		iterators.add(new WeakReference<LargeSetIterator>(rval));
		return rval;
	}

	/**
	 * @return the maxInMemory
	 */
	public int getMaxInMemory() {
		return maxInMemory;
	}

	/**
	 * @param maxInMemory
	 *            the maxInMemory to set
	 */
	public void setMaxInMemory(int maxInMemory) {
		this.maxInMemory = maxInMemory;
	}
}

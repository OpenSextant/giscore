/****************************************************************************************
 *  SimpleObjectInputStream.java
 *
 *  Created: Mar 23, 2009
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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.io.IOUtils;

/**
 * Simplified stream that doesn't hold object references on input
 * 
 * @author DRAND
 * 
 */
public class SimpleObjectInputStream {
	static final int NULL = 0;
	static final int BOOL = 1;
	static final int SHORT = 2;
	static final int INT = 3;
	static final int LONG = 4;
	static final int FLOAT = 5;
	static final int DOUBLE = 6;
	static final int STRING = 7;
	static final int OBJECT_NULL = 8;
	static final int DATE = 9;
	
	private DataInputStream stream;
	@SuppressWarnings("unchecked")
	private final Map<Integer, Class> classMap = new HashMap<Integer, Class>();
	
	/**
	 * Ctor
	 * 
	 * @param s InputStream, never null
	 * @throws IllegalArgumentException if s is null  
	 */
	public SimpleObjectInputStream(InputStream s) {
		if (s == null) {
			throw new IllegalArgumentException("s should never be null");
		}
		stream = new DataInputStream(s);
	}

	/**
	 * Close the stream
	 */
	public void close() {
		IOUtils.closeQuietly(stream);
	}

	/**
	 * Read the next object from the stream
	 * 
	 * @return the next object, or <code>null</code> if the stream is empty
	 * @throws ClassNotFoundException
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	public Object readObject() throws ClassNotFoundException, IOException,
			InstantiationException, IllegalAccessException {
		try {
			boolean classref = readBoolean();
			Class clazz = null;
			if (classref) {
				int refid = readInt();
				if (refid == 0) {
					return null;
				} else {
					clazz = classMap.get(refid);
				}
			} else {
				String className = readString();
				int refid = readInt();
				clazz = (Class<IDataSerializable>) Class.forName(className);
				classMap.put(refid, clazz);
			}
			IDataSerializable rval = (IDataSerializable) clazz.newInstance();
			rval.readData(this);
			return rval;
		} catch(EOFException e) {
			return null;
		}
	}

	/**
	 * Read a collection of objects from the stream
	 * @return the collection of object, may be <code>null</code>
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public List<? extends IDataSerializable> readObjectCollection()
			throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		int count = readInt();
		if (count == 0) {
			return null;
		}
		List rval = new ArrayList(count);
		for (int i = 0; i < count; i++) {
			rval.add(readObject());
		}
		return rval;
	}

	/**
	 * @return the next scalar object from the stream
	 * @throws IOException if an I/O error occurs
	 */
	public Object readScalar() throws IOException {
		int type = stream.readShort();
		switch (type) {
		case NULL:
			return null;
		case OBJECT_NULL:
			return ObjectUtils.NULL;
		case SHORT:
			return stream.readShort();
		case INT:
			return stream.readInt();
		case LONG:
			return stream.readLong();
		case DOUBLE:
			return stream.readDouble();
		case FLOAT:
			return stream.readFloat();
		case STRING:
			return readString();
		case BOOL:
			return stream.readBoolean();
		case DATE:
			return new Date(stream.readLong());
		default:
			throw new UnsupportedOperationException(
					"Found unsupported scalar enum " + type);
		}
	}

	/**
	 * Read a string from the data stream
	 * 
	 * @return
	 * @throws IOException if an I/O error occurs
	 */
	public String readString() throws IOException {
		int strlen = stream.readInt();
		if (strlen > 0) {
			byte strbytes[] = new byte[strlen];
			stream.read(strbytes);
			return new String(strbytes, "UTF8");
		} else {
			return null;
		}
	}

	/**
	 * @return the next long value
	 * @throws IOException if an I/O error occurs
	 */
	public long readLong() throws IOException {
		return stream.readLong();
	}

	/**
	 * @return the next int value
	 * @throws IOException if an I/O error occurs
	 */
	public int readInt() throws IOException {
		return stream.readInt();
	}

	/**
	 * @return the next boolean value
	 * @throws IOException if an I/O error occurs
	 */
	public boolean readBoolean() throws IOException {
		return stream.readBoolean();
	}

	/**
	 * @return the next double value
	 * @throws IOException if an I/O error occurs
	 */
	public double readDouble() throws IOException {
		return stream.readDouble();
	}

	/**
	 * @return the next short value
	 * @throws IOException if an I/O error occurs
	 */
	public short readShort() throws IOException {
		return stream.readShort();
	}
}

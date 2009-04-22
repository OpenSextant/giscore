/****************************************************************************************
 *  SimpleObjectOutputStream.java
 *
 *  Created: Mar 24, 2009
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Simplified Object Output Stream that saves data object
 * 
 * @author DRAND
 */
public class SimpleObjectOutputStream {
	private DataOutputStream stream;
	/**
	 * Tracks the correspondence between a generated id and the class
	 */
	@SuppressWarnings("unchecked")
	private Map<Class, Integer> classMap = new HashMap<Class, Integer>();
	/**
	 * Current class id value, incremented for each additional class. Zero
	 * is reserved for <code>null</code> objects.
	 */
	private int cid = 1;
	
	/**
	 * Ctor
	 * @param s
	 */
	public SimpleObjectOutputStream(OutputStream s) {
		if (s == null) {
			throw new IllegalArgumentException("s should never be null");
		}
		stream = new DataOutputStream(s);
	}
	
	/**
	 * Close the stream
	 * @throws IOException
	 */
	public void close() throws IOException {
		stream.close();
	}
	
	/**
	 * Write an object to the stream
	 * @param object
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void writeObject(IDataSerializable object) throws IOException {
		if (object == null) {
			writeBoolean(true);
			writeInt(0);
		} else {
			Class clazz = object.getClass();
			Integer classid = classMap.get(clazz);
			if (classid != null) {
				writeBoolean(true);
				writeInt(classid);
			} else {
				writeBoolean(false);
				writeString(object.getClass().getName());
				writeInt(cid);
				classMap.put(clazz, cid);
				cid++;
			}
			object.writeData(this);
		}
		
	}
	
	/**
	 * Write a collection of object
	 * @param objects the object collection
	 * @throws IOException 
	 */
	public void writeObjectCollection(Collection<? extends IDataSerializable> objects) throws IOException {
		if (objects == null) {
			writeInt(0);
		} else {
			writeInt(objects.size());
			for(IDataSerializable ser : objects) {
				writeObject(ser);
			}
		}
	}
	
	/**
	 * Write primitive non-array value, i.e. a string, integer, float, etc. The
	 * value is written with a prior marker to allow reading later
	 * @param value the value, must be a supported type
	 * @throws IOException 
	 */
	public void writeScalar(Object value) throws IOException {
		if (value == null) {
			stream.writeShort(SimpleObjectInputStream.NULL);
		} else if (ObjectUtils.NULL.equals(value)) {
			stream.writeShort(SimpleObjectInputStream.OBJECT_NULL);
		} else if (value instanceof Short) {
			stream.writeShort(SimpleObjectInputStream.SHORT);
			stream.writeShort(((Short)value).shortValue()); 
		} else if (value instanceof Integer) {
			stream.writeShort(SimpleObjectInputStream.INT);
			stream.writeInt(((Integer)value).intValue()); 
		} else if (value instanceof Long) {
			stream.writeShort(SimpleObjectInputStream.LONG);
			stream.writeLong(((Long)value).longValue()); 
		} else if (value instanceof Double) {
			stream.writeShort(SimpleObjectInputStream.DOUBLE);
			stream.writeDouble(((Double) value).doubleValue());
		} else if (value instanceof Float) {
			stream.writeShort(SimpleObjectInputStream.FLOAT);
			stream.writeFloat(((Float) value).floatValue());
		} else if (value instanceof String) {
			stream.writeShort(SimpleObjectInputStream.STRING);
			writeString((String) value);
		} else if (value instanceof Boolean) {
			stream.writeShort(SimpleObjectInputStream.BOOL);
			writeBoolean((Boolean) value);
		} else if (value instanceof Date) {
			stream.writeShort(SimpleObjectInputStream.DATE);
			writeLong(((Date) value).getTime());
		} else {
			throw new UnsupportedOperationException("Found unsupported type " + value.getClass());
		}		
	}
	

	/**
	 * Helper method that aids in writing a string to the data stream
	 * @param str the string to write.
	 * @throws IOException
	 */
    // REVIEW! : Doug should an empty string write null ?
	public void writeString(String str) throws IOException {
		if (StringUtils.isBlank(str)) {
			stream.writeInt(0);
		} else {
			byte arr[] = str.getBytes("UTF8");
			stream.writeInt(arr.length);
			stream.write(arr);
		}
	}

	/**
	 * Write a long value
	 * @param lval
	 * @throws IOException 
	 */
	public void writeLong(long lval) throws IOException {
		stream.writeLong(lval);
	}

	/**
	 * Write an int value
	 * @param ival
	 * @throws IOException 
	 */
	public void writeInt(int ival) throws IOException {
		stream.writeInt(ival);
	}
	
	/**
	 * Write a boolean value
	 * @param bval
	 * @throws IOException
	 */
	public void writeBoolean(boolean bval) throws IOException {
		stream.writeBoolean(bval);
	}

	/**
	 * Write a double value
	 * @param dval
	 * @throws IOException 
	 */
	public void writeDouble(double dval) throws IOException {
		stream.writeDouble(dval);
	}

	/**
	 * Write a short value
	 * @param sval
	 * @throws IOException 
	 */
	public void writeShort(short sval) throws IOException {
		stream.writeShort(sval);
	}

	/**
	 * Flush
	 * @throws IOException 
	 */
	public void flush() throws IOException {
		stream.flush();
	}
}

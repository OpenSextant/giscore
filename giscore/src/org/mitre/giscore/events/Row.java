/****************************************************************************************
 *  Row.java
 *
 *  Created: Apr 10, 2009
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
package org.mitre.giscore.events;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Represents the most basic tabular data. 
 * 
 * @author DRAND
 */
public class Row implements IGISObject, IDataSerializable {
	protected URI schema;
	protected final Map<SimpleField, Object> extendedData = new LinkedHashMap<SimpleField, Object>();

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.VisitableGeometry#accept(org.mitre.giscore.output.StreamVisitorBase)
	 */
	@Override
	public void accept(StreamVisitorBase visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		String schemaStr = in.readString(); 
		try {
			schema = schemaStr != null ? new URI(schemaStr) : null;
		} catch (URISyntaxException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
		int cnt = in.readInt();
		for(int i = 0; i < cnt; i++) {
			SimpleField field = (SimpleField) in.readObject();
			Object val = in.readScalar();
			extendedData.put(field, val);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(schema != null ? schema.toString() : null);
		int cnt = extendedData.size();
		out.writeInt(cnt);
		for(Map.Entry<SimpleField, Object> entry : extendedData.entrySet()) {
			out.writeObject(entry.getKey());
			out.writeScalar(entry.getValue());
		}
	}

	/**
	 * @return the schema, may be <code>null</code> if there's no reference to a
	 *         schema
	 */
	public URI getSchema() {
		return schema;
	}

	/**
	 * @param schema
	 *            the schema to set
	 */
	public void setSchema(URI schema) {
		this.schema = schema;
	}

	/**
	 * Put an attribute value
	 * 
	 * @param key
	 *            the key, never <code>null</code>
	 * @param value
	 *            the value, never <code>null</code>
	 */
	public void putData(SimpleField key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("key should never be null");
		}
		if (value == null) {
			extendedData.put(key, ObjectUtils.NULL);
		} else {
			extendedData.put(key, value);
		}
	}

	/**
	 * @return the fields
	 */
	public Collection<SimpleField> getFields() {
		return extendedData.keySet();
	}

	/**
	 * @return
	 */
	public boolean hasExtendedData() {
		return extendedData.size() > 0;
	}

	/**
	 * Get the value of a field
	 * 
	 * @param field
	 *            the fieldname, never <code>null</code> or empty.
	 * 
	 * @return the value of the field
	 */
	public Object getData(SimpleField field) {
		if (field == null) {
			throw new IllegalArgumentException(
					"field should never be null or empty");
		}
		return extendedData.get(field);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(80);
		b.append("<Row schemaUri='");
		b.append(schema);
		b.append("'");
		for(SimpleField key : extendedData.keySet()) {
			b.append(" ");
			b.append(key.getName());
			b.append("='");
			b.append(extendedData.get(key));
			b.append("'");
		}
		b.append("/>");
		return b.toString();
	}
}

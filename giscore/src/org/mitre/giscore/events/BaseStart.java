/****************************************************************************************
 *  BaseStart.java
 *
 *  Created: Jan 27, 2009
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
import java.util.*;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Base class for start.
 * 
 * @author DRAND
 * 
 */
public abstract class BaseStart implements IGISObject, IDataSerializable {
	
	protected String name;
	protected String description;
	protected URI schema;
	protected Date startTime;
	protected Date endTime;
	protected String styleUrl;
	protected final Map<SimpleField, Object> extendedData = new LinkedHashMap<SimpleField, Object>();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the styleUrl
	 */
	public String getStyleUrl() {
		return styleUrl;
	}

	/**
	 * @param styleUrl
	 *            the styleUrl to set
	 */
	public void setStyleUrl(String styleUrl) {
		this.styleUrl = styleUrl;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
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
     * Returns a {@link Set} view of the extended data mappings contained in the GISObject. 
     * @return a set view of the extended data mappings contained in this GISObject;
     *   an empty set if no extended data is defined.
     */
    public Set<Map.Entry<SimpleField, Object>> getEntrySet() {
        return extendedData.entrySet();
    }

	/**
	 * @return true if this GISObject has ExtendedData, false otherwise
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

	/**
	 * Read object from the data stream.
	 * 
	 * @param in
	 *            the input stream, never <code>null</code>
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		try {
			name = in.readString();
			description = in.readString();
			String schemaStr = in.readString(); 
			schema = schemaStr != null ? new URI(schemaStr) : null;
			styleUrl = in.readString();
			long val = in.readLong();
			if (val > -1) {
				startTime = new Date(val);
			} else {
				startTime = null;
			}
			val = in.readLong();
			if (val > -1) {
				endTime = new Date(val);
			} else {
				endTime = null;
			}
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

	/**
	 * Write the object to the data stream
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(name);
		out.writeString(description);
		out.writeString(schema != null ? schema.toString() : null);
		out.writeString(styleUrl);
		if (startTime != null)
			out.writeLong(startTime.getTime());
		else 
			out.writeLong(-1);
		if (endTime != null)
			out.writeLong(endTime.getTime());
		else 
			out.writeLong(-1);
		int cnt = extendedData.size();
		out.writeInt(cnt);
		for(Map.Entry<SimpleField, Object> entry : extendedData.entrySet()) {
			out.writeObject(entry.getKey());
			out.writeScalar(entry.getValue());
		}
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
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}
}

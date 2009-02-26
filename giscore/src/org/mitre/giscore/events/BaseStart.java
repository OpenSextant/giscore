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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Base class for start.
 * 
 * @author DRAND
 * 
 */
public abstract class BaseStart implements IGISObject, Serializable {
	private static final long serialVersionUID = 1L;
	protected String name;
	protected String description;
	protected String schema;
	protected Map<String, Object> elementAttributes = new HashMap<String, Object>();
	protected Map<SimpleField, Object> extendedData = new HashMap<SimpleField, Object>();
	
	/**
	 * Get element attribute value. Element attributes are used to store data
	 * for the element that can be interpreted by the output code.
	 * 
	 * @param key
	 *            the attribute key, never <code>null</code>
	 * @return the value, may be <code>null</code>.
	 */
	public Object getElementAttribute(String key) {
		if (key == null) {
			throw new IllegalArgumentException("key should never be null");
		}
		return elementAttributes.get(key);
	}

	/**
	 * Put an element attribute value.
	 * 
	 * @param key
	 *            the key, never <code>null</code>
	 * @param value
	 *            the value, never <code>null</code>
	 */
	public void put(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("key should never be null");
		}
		if (value == null) {
			elementAttributes.remove(key);
		} else {
			elementAttributes.put(key, value);
		}
	}

	/**
	 * @return an iterator over the element attribute keys, never
	 *         <code>null</code>.
	 */
	public Iterator<String> keys() {
		return elementAttributes.keySet().iterator();
	}

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
	 * @return the schema, may be <code>null</code> if there's no reference
	 * to a schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * @param schema the schema to set
	 */
	public void setSchema(String schema) {
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
			extendedData.remove(key);
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
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}
}

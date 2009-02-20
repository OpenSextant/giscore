/****************************************************************************************
 *  Field.java
 *
 *  Created: Jan 29, 2009
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This is the key for the extended data in the features. Fields with no 
 * schema are used for anonymous data, otherwise the schema name/url is
 * in the schema member.
 * 
 * @author DRAND
 *
 */
public class Field {
	String schema;
	String name;
	
	/**
	 * Ctor for simple data
	 * @param name
	 */
	public Field(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.name = name;
	}

	/**
	 * Ctor for simple data
	 * @param name
	 */
	public Field(String schema, String name) {
		if (schema == null || schema.trim().length() == 0) {
			throw new IllegalArgumentException(
					"schema should never be null or empty");
		}
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.schema = schema;
		this.name = name;
	}
	
	/**
	 * @return the schema
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}

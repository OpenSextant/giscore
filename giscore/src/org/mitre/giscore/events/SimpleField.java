/****************************************************************************************
 *  SimpleField.java
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

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A descriptor for a given schema field. A given field is described by its
 * type, name and display name. The display name dictates (for some formats) how
 * the field should be shown to the user.
 * 
 * @author DRAND
 */
public class SimpleField implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum Type {
		STRING("esriFieldTypeString", "xs:string", 255, 0), 
		INT("esriFieldTypeInteger", "xs:int", 4, 0), 
		UINT("esriFieldTypeInteger", "xs:int", 4, 0), 
		SHORT("esriFieldTypeSmallInteger", "xs:int", 2, 0), 
		USHORT("esriFieldTypeSmallInteger", "xs:int", 2, 0), 
		FLOAT("esriFieldTypeSingle", "xs:float", 4, 0), 
		DOUBLE("esriFieldTypeDouble", "xs:double", 8, 0), 
		GEOMETRY("esriFieldTypeGeometry", null, 0, 0),
		DATE("esriFieldTypeDate", "xs:dateTime", 4, 0),
		OID("esriFieldTypeOID", "xs:int", 4, 0),
		BOOL(null, "xs:boolean", 1, 0);

		private Type(String gdbxml, String xmlschematype, int def_len, int def_pre) {
			gdbXmlType = gdbxml;
			xmlSchemaType = xmlschematype;
			default_length = def_len;
			default_precision = def_pre;
		}
		
		String gdbXmlType, xmlSchemaType;
		int default_length;
		int default_precision;

		/**
		 * @return the default_length
		 */
		public int getDefaultLength() {
			return default_length;
		}

		/**
		 * @return the default_precision
		 */
		public int getDefaultPrecision() {
			return default_precision;
		}

		/**
		 * @return a type string for an esri xml interchange file
		 */
		public Object getGdbXmlType() {
			return gdbXmlType;
		}
		
		/**
		 * @return the xmlSchemaType
		 */
		public String getXmlSchemaType() {
			return xmlSchemaType;
		}

		/**
		 * @return <code>true</code> if this type is the geometry type
		 */
		public boolean isGeometry() {
			return GEOMETRY.equals(this);
		}
		
		/**
		 * @return <code>true</code> if this type is compatible with KML schemata
		 */
		public boolean isKmlCompatible() {
			return !(isGeometry() || DATE.equals(this));
		}
	
	}
	
	Type type;
	String name;
	String displayName;
	String aliasName;
	String modelName;
	boolean required = false;
	Integer length;
	Integer precision;
	Integer scale;
	boolean editable = true;
	
	/**
	 * Ctor - for a simple default field type
	 * @param name
	 */
	public SimpleField(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.name = name;
		displayName = name;
		type = Type.STRING;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(Type type) {
		this.type = type;
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
		if (name == null) {
			throw new IllegalArgumentException(
					"name should never be null");
		}
		this.name = name;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 *            the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return the required
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}
	
	/**
	 * @return the length
	 */
	public Integer getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(Integer length) {
		this.length = length;
	}

	/**
	 * @return the precision
	 */
	public Integer getPrecision() {
		return precision;
	}

	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	/**
	 * @return the scale
	 */
	public Integer getScale() {
		return scale;
	}

	/**
	 * @param scale the scale to set
	 */
	public void setScale(Integer scale) {
		this.scale = scale;
	}

	/**
	 * @return the nullable
	 */
	public boolean isNullable() {
		return ! required;
	}

	/**
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * @param editable the editable to set
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * @return the aliasName
	 */
	public String getAliasName() {
		return aliasName;
	}

	/**
	 * @param aliasName the aliasName to set
	 */
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	/**
	 * @return the modelName
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * @param modelName the modelName to set
	 */
	public void setModelName(String modelName) {
		this.modelName = modelName;
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

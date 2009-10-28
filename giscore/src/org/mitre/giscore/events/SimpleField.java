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

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * A descriptor for a given schema field.  A given field is described by its
 * type, name and display name.  The display name dictates (for some formats) how
 * the field should be shown to the user.  A SimpleField only exists in a Schema context.
 * @see Schema 
 * 
 * @author DRAND
 */
public class SimpleField implements IDataSerializable {
	private static final long serialVersionUID = 1L;

	public static enum Type {
		STRING("esriFieldTypeString", "xs:string", 255, 0, ""), 
		INT("esriFieldTypeInteger", "xs:int", 4, 0, 0), 
		UINT("esriFieldTypeInteger", "xs:int", 4, 0, 0), 
		SHORT("esriFieldTypeSmallInteger", "xs:int", 2, 0, 0), 
		USHORT("esriFieldTypeSmallInteger", "xs:int", 2, 0, 0), 
		FLOAT("esriFieldTypeSingle", "xs:float", 4, 0, 0.0), 
		DOUBLE("esriFieldTypeDouble", "xs:double", 8, 0, 0.0), 
		GEOMETRY("esriFieldTypeGeometry", null, 0, 0, null),
		DATE("esriFieldTypeDate", "xs:dateTime", 4, 0, new Date(0)),
		OID("esriFieldTypeOID", "xs:int", 4, 0, null),
		BOOL(null, "xs:boolean", 1, 0, 0);

		private Type(String gdbxml, String xmlschematype, int def_len, int def_pre, Object gdbEmptyValue) {
			gdbXmlType = gdbxml;
			xmlSchemaType = xmlschematype;
			default_length = def_len;
			default_precision = def_pre;
			this.gdbEmptyValue = gdbEmptyValue;
		}
		
		String gdbXmlType, xmlSchemaType;
		int default_length;
		int default_precision;
		Object gdbEmptyValue;

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
		 * (string, int, uint, short, ushort, float, double, bool).
		 * @see http://code.google.com/apis/kml/documentation/kmlreference.html#schema
		 */
		public boolean isKmlCompatible() {
			return !(isGeometry() || OID.equals(this) || DATE.equals(this));
		}

		/**
		 * @return the value to use in the Xml Workspace document when there's
		 * no actual value, will be <code>null</code> if the type cannot support
		 * a missing value.
		 */
		public Object getGdbEmptyValue() {
			return gdbEmptyValue;
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
	 * The index into the feature class, just used for ESRI
	 */
	transient Integer index;
	
	/**
	 * Constructor for a simple default field type (String).
	 * @param name Proper name of this field. Must be a non-null/non-empty string
	 * and unique if used in a Schema.
	 */
	public SimpleField(String name) {
		setName(name);
		displayName = name;
		type = Type.STRING;
	}
	
	/**
	 * Ctor
	 * @param name 
	 * @param type
	 */
	public SimpleField(String name, Type type) {
		this(name);
		setType(type);
	}

	/**
	 * No args ctor use for serialization. Caller must set name and type fields,
	 * which are required non-null fields.
	 */
	public SimpleField() {

	}

	/**
	 * @return the type never null
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
     * @throws IllegalArgumentException if <code>type</code> is null
	 */
	public void setType(Type type) {
        if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		this.type = type;
		// setLength(type.getDefaultLength()); already returns default Length in getLength()
		setPrecision(type.getDefaultPrecision());
	}

	/**
	 * @return the name, never null
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
     * @throws IllegalArgumentException if <code>name</code> is null 
	 */
	public void setName(String name) {
		if (name != null) {
			name = name.trim(); // normalize name (strip whitespace)
			if (name.length() == 0) name = null;
		}
		if (name == null) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
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
	 * @return the length, never null
	 */
	public Integer getLength() {
		if (length != null)
			return length;
		else 
			return type.getDefaultLength();
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

	/**
	 * @return the index
	 */
	public Integer getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(Integer index) {
		this.index = index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// note: if use EqualsBuilder.reflectionEquals then Feature.approximatelyEquals() gets messed up
		// when comparing fields from two different Features so just test the name field.
		if (obj instanceof SimpleField) {
			SimpleField other = (SimpleField)obj;
			if (name == null) return other.name == null;
			return name.equals(other.name);
		}
		return false;
		//return EqualsBuilder.reflectionEquals(this, obj);
	}

	/*
	 * Simple hash of the name field which is used as single key to lookup these object in Hash maps
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(80);
		b.append("<SimpleField name='");
		b.append(getName());
		b.append("' type='");
		b.append(getType());
		b.append("'/>");
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		setAliasName(in.readString());
		setDisplayName(in.readString());
		setModelName(in.readString());
		setName(in.readString());
		String type = in.readString();
		setType(Type.valueOf(type));
		setLength((Integer) in.readScalar());
		setPrecision((Integer) in.readScalar());
		setScale((Integer) in.readScalar());
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(getAliasName());
		out.writeString(getDisplayName());
		out.writeString(getModelName());
		out.writeString(getName());
		out.writeString(getType().name());
		out.writeScalar(getLength());
		out.writeScalar(getPrecision());
		out.writeScalar(getScale());
	}
}

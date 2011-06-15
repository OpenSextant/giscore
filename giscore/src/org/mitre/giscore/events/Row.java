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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the most basic tabular data. 
 * 
 * @author DRAND
 */
public class Row extends AbstractObject implements IDataSerializable {
    private static final long serialVersionUID = 1L;
    
	protected URI schema;
	protected final Map<SimpleField, Object> extendedData = new LinkedHashMap<SimpleField, Object>();

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.VisitableGeometry#accept(org.mitre.giscore.output.StreamVisitorBase)
	 */
	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		super.readData(in);
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
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
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
    @CheckForNull
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
     * Returns a {@link Set} view of the Extended data mappings associated with this row.
     * Note values will be {@code ObjectUtils.NULL} if original value was @{code null} so
     * this needs to be tested accordingly by the caller. If, however, {@link #getData(SimpleField)}
	 * is used then {@code ObjectUtils.NULL} values are returned as {@code null} values.
	 *
     * @return a set view of the Extended data mappings contained in this row
     */
    @NonNull
	public Set<Map.Entry<SimpleField,Object>> getEntrySet() {
		return extendedData.entrySet();
	}

	/**
	 * @return the fields
	 */
    @NonNull
	public Collection<SimpleField> getFields() {
		return extendedData.keySet();
	}

	/**
	 * @return true of Row has ExtendedData, false otherwise.
	 */
	public boolean hasExtendedData() {
		return !extendedData.isEmpty();
	}

	/**
	 * Get the value of a field
	 * 
	 * @param field
	 *            the fieldname, never @{code null} or empty.
	 * 
	 * @return the value of the field, value can be @{code null} if defined as such.
	 */
    @CheckForNull
	public Object getData(SimpleField field) {
		if (field == null) {
			throw new IllegalArgumentException(
					"field should never be null or empty");
		}
		final Object value = extendedData.get(field);
		return ObjectUtils.NULL.equals(value) ? null : value;
	}

    /**
     * Removes the extended data associated with this SimpleField if it is present.
     *  
     * @param field field whose mapping is to be removed from the extended data collection
     * @return the previous value associated with <tt>field</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>field</tt>.
     */
    @Nullable
    public Object removeData(SimpleField field) {
		if (field == null) {
			throw new IllegalArgumentException(
					"field should never be null or empty");
		}
        final Object value = extendedData.remove(field);
        return ObjectUtils.NULL.equals(value) ? null : value;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		if (!super.equals(obj))
			return false;
		Row other = (Row) obj;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (extendedData == null) {
			if (other.extendedData != null)
				return false;
		} else if (!extendedData.equals(other.extendedData))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((extendedData == null) ? 0 : extendedData.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(80);
		b.append(getClass().getSimpleName());
        if (getId() != null)
            b.append(" id=").append(getId()).append('\n');
		b.append(" data=\n");
        for(Map.Entry<SimpleField, Object> entry : extendedData.entrySet()) {
        	SimpleField field = entry.getKey();
			b.append("    ");
			b.append(field.getName());
			b.append(" (");
			b.append(field.getType().name());
			b.append(')');
			b.append(" = ");
			if (ObjectUtils.NULL.equals(entry.getValue())) {
				b.append("null");
			} else {
				b.append('\'');
				b.append(entry.getValue());
				b.append('\'');
			}
			b.append('\n');
		}
        if (schema != null) {
            b.append(" schemaUri=");
		    b.append(schema);
            b.append('\n');
        }
		return b.toString();
	}
}

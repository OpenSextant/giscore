/****************************************************************************************
 *  Schema.java
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.output.StreamVisitorBase;

/**
 * Defines a data schema. Data schemata are important because they allow us to 
 * transmit information about the structure of the data from the input to the
 * output side, and even to the processing.
 * <p>
 * While this concept comes from KML, it definitely is present in Shapefiles
 * in the form of the header and type information from the dbf file.
 * <p>
 * A Schema element contains one or more SimpleField elements. In the SimpleField,
 * the Schema declares the type and name of the custom field. 
 * 
 * @author DRAND
 */
public class Schema implements IGISObject {
	// Numeric id, used for GDB XML and to create an initial name
	private final static AtomicInteger ms_idgen = new AtomicInteger();
	
	private String name;
	private URI id;
	private transient int nid; 
	private final Map<String, SimpleField> fields = new LinkedHashMap<String, SimpleField>();
    private String parent;

    /**
	 * Default Ctor
	 */
	public Schema() {
		try {
			id = new URI("s_" + ms_idgen.incrementAndGet());
			name = "schema_" + ms_idgen.get();
			nid = ms_idgen.get();
		} catch (URISyntaxException e) {
			// Impossible
		}
	}
	
	public Schema(URI urn) {
		this();
		if (urn == null) {
			throw new IllegalArgumentException(
					"urn should never be null");
		}
		id = urn;
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
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public URI getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(URI id) {
		if (id == null) {
			throw new IllegalArgumentException(
					"id should never be null");
		}
		this.id = id;
	}
	
	/**
	 * @return the nid, a numeric id used for gdb interactions, ignored in KML
	 */
	public int getNid() {
		return nid;
	}

    /**
     * Return parent. Used only in old-style KML 2.0 documents.
	 * This should normally be null.
     * @return parent
     */
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
	 * Add a new field to the schema
	 * @param name the name of the field, never <code>null</code> or empty
	 * @param field the field, never <code>null</code>
	 */
	public void put(String name, SimpleField field) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		if (field == null) {
			throw new IllegalArgumentException(
					"field should never be null");
		}
		fields.put(name, field);
	}
	
	/**
	 * Shortcut that adds a field and uses the name in the field
	 * @param field the field, never <code>null</code>
	 */
	public void put(SimpleField field) {
		if (field == null) {
			throw new IllegalArgumentException(
					"field should never be null");
		}
		put(field.getName(), field);
	}
	
	/**
	 * Get the field description for a given field
	 * @param name the name of the field, never <code>null</code> or empty
	 * @return the field data, may be <code>null</code> if the field is
	 * not found.
	 */
	public SimpleField get(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		return fields.get(name);
	}
	
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
	
	/**
	 * @return the read-only collection of field names, never <code>null</code>.
	 */
	public Collection<String> getKeys() {
		return Collections.unmodifiableSet(fields.keySet());
	}
	
	/**
	 * @return the name of the geometry field or <code>null</code> if one 
	 * doesn't exist
	 */
	public String getGeometryField() {
        for (SimpleField field : fields.values()) {
            if (field.getType().isGeometry()) return field.getName();
        }
		return null;
	}
	
	/**
	 * @return the contained field that has a geometry, or <code>null</code>
	 * if there is no such field. Note that if there is, for some reason, 
	 * multiples, then the first will be returned.
	 */
	public SimpleField getShapeField() {
		return getFieldOfType(SimpleField.Type.GEOMETRY);
	}
	
	/**
	 * @return the contained field that is the OID, or <code>null</code>
	 * if there is no such field. Note that if there is, for some reason, 
	 * multiples, then the first will be returned.
	 */
	public SimpleField getOidField() {
		return getFieldOfType(SimpleField.Type.OID);
	}
	
	/**
	 * @param geometry
	 * @return
	 */
	private SimpleField getFieldOfType(Type type) {
		for(SimpleField field : fields.values()) {
			if (type.equals(field.getType())) {
				return field;
			}
		}
		return null;
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
		StringBuilder b = new StringBuilder(fields.size() * 80);
		b.append("<Schema name='");
		b.append(getName());
		b.append("' id='");
		b.append(getId());
		b.append("'>\n");
		for(SimpleField field : fields.values()) {
			b.append("  ");
			b.append(field);
			b.append("\n");
		}
		b.append("</Schema>\n");
		
		return b.toString();
	}

}

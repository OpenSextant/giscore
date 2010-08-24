/****************************************************************************************
 *  Element.java
 *
 *  Created: Jul 15, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
import java.io.Serializable;
import java.util.*;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * An element represents an XML element found in an XML type document such
 * as KML. This is a limited representation that does not allow for nested
 * element structures, although such a thing could be added in the future.
 * 
 * @author DRAND
 */
public class Element implements IGISObject, IDataSerializable, Serializable {
    
	private static final long serialVersionUID = 1L;

    private transient Namespace namespace;

	/**
	 * The name of the element
	 */
	private String name;
	
	/**
	 * Attribute/value pairs found on the element
	 */
	private final Map<String, String> attributes = new HashMap<String, String>();
	
	/**
	 * Child elements contained within the element
	 */
	private final List<Element> children = new ArrayList<Element>();
	
	/**
	 * Text content 
	 */
	private String text;

    /**
	 * Empty ctor
	 */
	public Element() {
		
	}
	
	/**
	 * Create XML Element object.
     * 
	 * @param namespace Namespace of this <code>Element</code>,
     *      may be <code>null</code>.
	 * @param name
     *
     * @throws IllegalArgumentException if name is blank string or <tt>null</tt>. 
	 */
	public Element(Namespace namespace, String name) {
		super();
		setName(name);
		this.namespace = namespace;
	}

    /**
     * Get namespace that this element belongs to,
	 * may be <code>null</code>.
     */
    @CheckForNull
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * Set the Namespace of this XML <code>Element</code>.
     * @param namespace Namespace of this <code>Element</code>,
     *      may be <code>null</code>. 
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

	/**
	 * @return the prefix
	 */
    @CheckForNull
	public String getPrefix() {
		return namespace != null ? namespace.getPrefix() : null;
	}

     /**
     * Get the Namespace URI of this XML <code>Element</code>.
     *
     * @return Namespace URI of this <code>Element</code>
     */
    @CheckForNull
    public String getNamespaceURI() {
        return namespace != null ? namespace.getURI() : null;
    }

	/**
	 * @return the name
	 */
	public String getName() {
        assert name != null;
		return name;
	}

	/**
	 * @param name the name to set
     * 
     * @throws IllegalArgumentException if name is blank string or <tt>null</tt>.
	 */
	public void setName(String name) {
        if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.name = name;
	}

	/**
	 * @return the text
	 */
    @CheckForNull
	public String getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the attributes
	 */
    @NonNull
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @return the children, never null
	 */
    @NonNull
	public List<Element> getChildren() {
        assert children != null;
		return children;
	}

    /**
     * This returns the first child element within this element with the
     * given local name and belonging to the given namespace.
     * If no elements exist for the specified name and namespace, null is
     * returned.
     *
     * @param name local name of child element to match
     * @param ns <code>Namespace</code> to search within. If ns is null
     *      then only the name field is checked.
     * @return the first matching child element, or null if not found
     */
    @CheckForNull
    public Element getChild(final String name, final Namespace ns) {
        if (name == null || children.size() == 0) return null;
        for (Element child : children) {
            if (name.equals(child.getName())) {
                if (ns == null || ns.equals(child.getNamespace()))
                    return child;
            }
        }
        return null;
    }

    /**
     * This returns the first child element within this element with the
     * given local name regardless of the namespace.
     * If no elements exist for the specified name, null is
     * returned.
     *
     * @param name local name of child element to match
     * @return the first matching child element, or null if not found
     */
    @CheckForNull
    public Element getChild(final String name) {
        return getChild(name, null);
    }

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);	
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Element [");
        if (namespace != null)
            b.append("Namespace=").append(namespace.getPrefix()).append(":").append(namespace.getURI()).append(", ");
        b.append("name=").append(name);
        if (!attributes.isEmpty())
            b.append(", attributes=").append(attributes);
        if (text != null)
            b.append(", text=").append(text);
        if (!children.isEmpty())
            b.append('\n').append("  children=").append(children);
        b.append(']');
        return b.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Element other = (Element) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
        if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		return true;
	}

	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
        String prefix = in.readString();
        if (prefix != null) {
            String nsURI = in.readString();
            namespace = Namespace.getNamespace(prefix, nsURI);
        } else
            namespace = null;
		name = in.readString();
		text = in.readString();
		int count = in.readInt();
		for(int i = 0; i < count; i++) {
			String attr = in.readString();
			String val = in.readString();
			attributes.put(attr, val);
		}
        List<Element> collection = (List<Element>) in.readObjectCollection();
        children.clear();
        if (collection != null)
            children.addAll(collection);
	}

	public void writeData(SimpleObjectOutputStream out) throws IOException {
        if (namespace == null || namespace.getPrefix() == null)
            out.writeString(null);
        else {
		    out.writeString(namespace.getPrefix());
            out.writeString(namespace.getURI());
        }
		out.writeString(name);
		out.writeString(text);
		out.writeInt(attributes.size());
		for(Map.Entry<String,String>entry : attributes.entrySet()) {
			out.writeString(entry.getKey());
			out.writeString(entry.getValue());
		}
		out.writeObjectCollection(children);
	}

}

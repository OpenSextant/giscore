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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An element represents an XML element found in an XML type document such
 * as KML. This is a limited representation that does not allow for nested
 * element structures, although such a thing could be added in the future.
 * 
 * @author DRAND
 */
public class Element implements IGISObject, IDataSerializable, Serializable {
    
	private static final long serialVersionUID = 1L;

	@NonNull
    private transient Namespace namespace;

	/**
	 * The name of the element
	 */
	@NonNull
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
	 * Empty constructor requires caller to call setName() and setNamespace()
     * directly or deserialize with {@link #readData}.
	 */
	public Element() {
        namespace = Namespace.NO_NAMESPACE;
	}
	
	/**
	 * Create XML Element object.
     * 
	 * @param namespace Namespace of this <code>Element</code>. If
     *      the namespace is <code>null</code>, the element will have no namespace.
      @param  name                 local name of the element
     *
     * @throws IllegalArgumentException if name is blank string or <tt>null</tt>. 
	 */
	public Element(Namespace namespace, String name) {
		setName(name);
		setNamespace(namespace);
	}

    /**
     * Get namespace that this element belongs to.
	 * Never <code>null</code>.
     * @return                     the element's namespace
     */
    @NonNull
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * Set the Namespace of this XML <code>Element</code>. If the provided namespace is null,
     * the element will have no namespace.
     * @param namespace Namespace of this <code>Element</code>,
     *      may be <code>null</code>. 
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace == null ? Namespace.NO_NAMESPACE : namespace;
    }

	/**
	 * @return the prefix
	 */
    @NonNull
	public String getPrefix() {
		return namespace.getPrefix();
	}

     /**
     * Get the Namespace URI of this XML <code>Element</code>.
     *
     * @return Namespace URI of this <code>Element</code>
     */
    @NonNull
    public String getNamespaceURI() {
        return namespace.getURI();
    }

	/**
	 * @return the name
	 */
    @NonNull
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set. Names must conform to name construction
	 * rules in the XML 1.0 specification as it applies to an XML element.
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
        if (name == null || children.isEmpty()) return null;
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
		b.append("Namespace=").append(namespace.getPrefix()).append(':').append(namespace.getURI()).append(", ");
        b.append("name=").append(name);
        if (!attributes.isEmpty())
            b.append(", attributes=").append(attributes);
        if (text != null) {
            String txtout = text.trim();
            if (txtout.length() != 0)
                b.append(", text=").append(text);
        }
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
		int result = attributes.hashCode(); // never null
		result = prime * result + children.hashCode(); // never null
		result = prime * result + name.hashCode(); // never null
		result = prime * result + namespace.hashCode(); // never null
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
		if (!attributes.equals(other.attributes))
			return false;
		if (!name.equals(other.name))
			return false;
		if (!namespace.equals(other.namespace))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
        if (!children.equals(other.children))
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
            namespace = Namespace.NO_NAMESPACE;
		name = in.readString();
		if (name == null) throw new IOException("name field cannot be null");
		text = in.readString();
		int count = in.readInt();
		attributes.clear();
		for(int i = 0; i < count; i++) {
			String attr = in.readString();
			String val = in.readString();
			attributes.put(attr, val);
		}
        List<Element> collection = (List<Element>) in.readObjectCollection();
        children.clear();
        if (collection != null && !collection.isEmpty())
            children.addAll(collection);
	}

	public void writeData(SimpleObjectOutputStream out) throws IOException {
        if (namespace.getURI().length() == 0)
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

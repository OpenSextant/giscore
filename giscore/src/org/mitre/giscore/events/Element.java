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

import org.mitre.giscore.IStreamVisitor;
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

	/**
	 * The namespace prefix for the namespace that this element belongs to,
	 * may be <code>null</code> or empty.
	 */
	private String prefix;
	
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
	 * Ctor
	 * @param prefix
	 * @param name
	 */
	public Element(String prefix, String name) {
		super();
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.prefix = prefix;
		this.name = name;
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
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

	/**
	 * @return the text
	 */
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
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @return the children, never null
	 */
	public List<Element> getChildren() {
        assert children != null;
		return children;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);	
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Element [prefix=" + prefix + ", name=" + name + ", attributes="
				+ attributes + ", text=" + text
				+ ", children=" + children + "]";
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
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		prefix = in.readString();
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
		out.writeString(prefix);
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

/****************************************************************************************
 *  AtomHeader.java
 *
 *  Created: Jul 19, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Represents the data sent in the header of an Atom 1.0 document. This is used
 * for the GeoAtom input and output streams and will probably be ignored by the
 * other streams. This is not an attempt to perfectly represent the data found
 * in an atom feed (see the ROME library for that), this is rather a good faith
 * attempt to consume and produce atom data primarily aimed at opensearch
 * applications.
 *
 * @author DRAND
 */
public class AtomHeader implements IGISObject, IDataSerializable, Serializable {

    private static final long serialVersionUID = 1L;

	@NonNull private String id;
	@NonNull private AtomLink selflink;
	@NonNull private List<AtomLink> relatedlinks = new ArrayList<AtomLink>();
	@NonNull private List<AtomAuthor> authors = new ArrayList<AtomAuthor>();
	@NonNull private List<Namespace> namespaces = new ArrayList<Namespace>();
	@NonNull private List<Element> elements = new ArrayList<Element>();
	private String title;
	private Date updated;
	private String generator;

	/**
	 * Empty ctor. Caller must set id, selflink, updated otherwise object is invalid.
	 */
	public AtomHeader() {
        // caller set required fields
	}

	/**
	 * Ctor
	 * @param id
	 * @param selflink
	 * @param title
	 * @param updated
     *
     * @throws IllegalArgumentException if id, selflink or updated arguments are <tt>null</tt>
	 */
	public AtomHeader(String id, AtomLink selflink, String title, Date updated) {
		super();
		if (id == null) {
			throw new IllegalArgumentException(
					"id should never be null");
		}
		if (selflink == null) {
			throw new IllegalArgumentException(
					"selflink should never be null");
		}
		if (updated == null) {
			throw new IllegalArgumentException(
					"updated should never be null");
		}
		this.id = id;
		this.selflink = selflink;
		this.title = title;
		this.updated = updated;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the selflink
	 */
	public AtomLink getSelflink() {
		return selflink;
	}

	/**
	 * @param selflink the selflink to set
	 */
	public void setSelflink(AtomLink selflink) {
		this.selflink = selflink;
	}

	/**
	 * @return the relatedlinks
	 */
	@NonNull
	public List<AtomLink> getRelatedlinks() {
		return relatedlinks;
	}

	/**
	 * @param relatedlinks the relatedlinks to set
	 */
	public void setRelatedlinks(List<AtomLink> relatedlinks) {
		this.relatedlinks = relatedlinks == null ? new ArrayList<AtomLink>() : relatedlinks;
	}

	/**
	 * @return the authors
	 */
	@NonNull
	public List<AtomAuthor> getAuthors() {
		return authors;
	}

	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(List<AtomAuthor> authors) {
		this.authors = authors == null ? new ArrayList<AtomAuthor>() : authors;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the updated
	 */
	@Nullable
	public Date getUpdated() {
		return updated;
	}

	/**
	 * @param updated the updated to set
	 */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/**
	 * @return the elements
	 */
	@NonNull
	public List<Element> getElements() {
		return elements;
	}

	/**
	 * @param elements the elements to set
	 */
	public void setElements(List<Element> elements) {
		this.elements = elements == null ? new ArrayList<Element>() : elements;
	}

	/**
	 * @return the namespaces
	 */
	@NonNull
	public List<Namespace> getNamespaces() {
		return namespaces;
	}

	/**
	 * @param namespaces the namespaces to set
	 */
	public void setNamespaces(List<Namespace> namespaces) {
		this.namespaces = namespaces == null ? new ArrayList<Namespace>() : namespaces;
	}

	/**
	 * @return the generator
	 */
	public String getGenerator() {
		return generator;
	}

	/**
	 * @param generator the generator to set
	 */
	public void setGenerator(String generator) {
		this.generator = generator;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = ((id == null) ? 0 : id.hashCode());
		result = prime * result + authors.hashCode();
		result = prime * result + elements.hashCode();
		result = prime * result
				+ ((generator == null) ? 0 : generator.hashCode());
		result = prime * result + namespaces.hashCode();
		result = prime * result + relatedlinks.hashCode();
		result = prime * result + selflink.hashCode();
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((updated == null) ? 0 : updated.hashCode());
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
		AtomHeader other = (AtomHeader) obj;
		if (!authors.equals(other.authors))
			return false;
		if (!elements.equals(other.elements))
			return false;
		if (generator == null) {
			if (other.generator != null)
				return false;
		} else if (!generator.equals(other.generator))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!namespaces.equals(other.namespaces))
			return false;
		if (!relatedlinks.equals(other.relatedlinks))
			return false;
		if (!selflink.equals(other.selflink))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (updated == null) {
			if (other.updated != null)
				return false;
		} else if (!updated.equals(other.updated))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		id = in.readString();
		selflink = (AtomLink) in.readObject();
		relatedlinks = in.readNonNullObjectCollection();
		authors = in.readNonNullObjectCollection();
		elements = in.readNonNullObjectCollection();
		title = in.readString();
		generator = in.readString();
		updated = (Date) in.readScalar();
		int nscount = in.readInt();
		namespaces.clear();
		for(int i = 0; i < nscount; i++) {
			String pre = in.readString();
			String uri = in.readString();
			namespaces.add(Namespace.getNamespace(pre, uri));
		}
	}

	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(id);
		out.writeObject(selflink);
		out.writeObjectCollection(relatedlinks);
		out.writeObjectCollection(authors);
		out.writeObjectCollection(elements);
		out.writeString(title);
		out.writeString(generator);
		out.writeScalar(updated);
		int nscount = namespaces.size();
		out.writeInt(nscount);
		for (Namespace ns : namespaces) {
			out.writeString(ns.getPrefix());
			out.writeString(ns.getURI());
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}
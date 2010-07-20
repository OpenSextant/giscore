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
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.IStreamVisitor;
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
	private URL id;
	private AtomLink selflink;
	private List<AtomLink> relatedlinks = new ArrayList<AtomLink>();
	private List<AtomAuthor> authors = new ArrayList<AtomAuthor>();
	private String title;
	private Date updated;

	/**
	 * Ctor
	 * @param id
	 * @param selflink
	 * @param title
	 * @param updated
	 */
	public AtomHeader(URL id, AtomLink selflink, String title, Date updated) {
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

	@Override
	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
		
	}
	
	/**
	 * @return the id
	 */
	public URL getId() {
		return id;
	}



	/**
	 * @param id the id to set
	 */
	public void setId(URL id) {
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
	public List<AtomLink> getRelatedlinks() {
		return relatedlinks;
	}



	/**
	 * @param relatedlinks the relatedlinks to set
	 */
	public void setRelatedlinks(List<AtomLink> relatedlinks) {
		this.relatedlinks = relatedlinks;
	}



	/**
	 * @return the authors
	 */
	public List<AtomAuthor> getAuthors() {
		return authors;
	}



	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(List<AtomAuthor> authors) {
		this.authors = authors;
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
	public Date getUpdated() {
		return updated;
	}



	/**
	 * @param updated the updated to set
	 */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authors == null) ? 0 : authors.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((relatedlinks == null) ? 0 : relatedlinks.hashCode());
		result = prime * result
				+ ((selflink == null) ? 0 : selflink.hashCode());
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
		if (authors == null) {
			if (other.authors != null)
				return false;
		} else if (!authors.equals(other.authors))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (relatedlinks == null) {
			if (other.relatedlinks != null)
				return false;
		} else if (!relatedlinks.equals(other.relatedlinks))
			return false;
		if (selflink == null) {
			if (other.selflink != null)
				return false;
		} else if (!selflink.equals(other.selflink))
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
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		String idstr = in.readString();
		id = StringUtils.isNotBlank(idstr) ? new URL(idstr) : null;
		selflink = (AtomLink) in.readObject();
		relatedlinks = (List<AtomLink>) in.readObjectCollection();
		authors = (List<AtomAuthor>) in.readObjectCollection();
		title = in.readString();
		updated = (Date) in.readScalar();
	}

	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(id != null ? id.toExternalForm() : null);
		out.writeObject(selflink);
		out.writeObjectCollection(relatedlinks);
		out.writeObjectCollection(authors);
		out.writeString(title);
		out.writeScalar(updated);
		
	}

}

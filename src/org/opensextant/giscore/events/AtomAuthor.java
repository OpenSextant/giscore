/****************************************************************************************
 *  AtomAuthor.java
 *
 *  Created: Jul 20, 2010
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
package org.opensextant.giscore.events;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.opensextant.giscore.utils.IDataSerializable;
import org.opensextant.giscore.utils.SimpleObjectInputStream;
import org.opensextant.giscore.utils.SimpleObjectOutputStream;

/**
 * Represents an atom author's information
 *
 * @author DRAND
 */
public class AtomAuthor implements IDataSerializable, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
	private URI uri;
	private String email;

	/**
	 * Empty ctor
	 */
	public AtomAuthor() {
        // caller must set name otherwise object is not valid
	}

	public AtomAuthor(String name, String email) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		this.name = name;
		this.email = email;
	}

	/**
	 * @return the name
	 */
    @CheckForNull
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
	 * @return the uri
	 */
    @CheckForNull
	public URI getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * @return the email
	 */
    @CheckForNull
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		AtomAuthor other = (AtomAuthor) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		name = in.readString();
		email = in.readString();
		String uristr = in.readString();
		if (StringUtils.isNotBlank(uristr)) {
			try {
				uri = new URI(uristr);
			} catch (URISyntaxException e) {
				throw new IOException("Bad URI", e);
			}
		} else {
			uri = null;
		}
	}

	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(name);
		out.writeString(email);
		out.writeString(uri != null ? uri.toASCIIString() : null);
	}

}
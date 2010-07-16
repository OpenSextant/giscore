/****************************************************************************************
 *  Namespace.java
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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Represents an XML namespace. This is used for XML based data formats to 
 * map a prefix to a naming URI.
 * 
 * @author DRAND
 */
public class Namespace implements IDataSerializable, Serializable {
	private static final long serialVersionUID = 1L;

	private String prefix;
	private URI name;
	
	public Namespace(String prefix, URI name) {
		if (prefix == null) {
			throw new IllegalArgumentException(
					"prefix should never be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("name should never be null");
		}
		this.prefix = prefix;
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public URI getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public String toString() {
		return name.toASCIIString() + "{{" + prefix + "}}";
	}
	
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		prefix = in.readString();
		try {
			name = new URI(in.readString());
		} catch (URISyntaxException e) {
			throw new InstantiationException("Bad data found: " + e.getLocalizedMessage());
		}
	}

	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(prefix);
		out.writeString(name.toURL().toExternalForm());
	}
}

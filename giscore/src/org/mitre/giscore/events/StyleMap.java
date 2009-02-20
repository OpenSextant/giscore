/****************************************************************************************
 *  StyleMap.java
 *
 *  Created: Jan 28, 2009
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.output.StreamVisitorBase;

/**
 * Represents a style map. A style map goes from a 
 * 
 * @author DRAND
 */
public class StyleMap implements IGISObject {
	private String id;
	private Map<String, String> mappings = new HashMap<String, String>();
	
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

	public void put(String key, String url) {
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException(
					"key should never be null or empty");
		}
		if (url == null || url.trim().length() == 0) {
			throw new IllegalArgumentException(
					"url should never be null or empty");
		}
		mappings.put(key, url);
	}
	
	public String get(String key) {
		return mappings.get(key);
	}

	public Iterator<String> keys() {
		return mappings.keySet().iterator();
	}
	
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
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
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}

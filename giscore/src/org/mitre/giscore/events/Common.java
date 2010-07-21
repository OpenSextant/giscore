/****************************************************************************************
 *  BaseStart.java
 *
 *  Created: Jan 27, 2009
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Common abstract superclass for features of various kinds.
 * 
 * @author DRAND
 * 
 */
public abstract class Common extends Row {
	
	private static final long serialVersionUID = 1L;
	
	protected String name;
	protected String description;
	private Boolean visibility;
	protected Date startTime;
	protected Date endTime;
	protected String styleUrl;
	private TaggedMap viewGroup;
	private TaggedMap region;
	private List<Element> elements = new ArrayList<Element>();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the styleUrl
	 */
	public String getStyleUrl() {
		return styleUrl;
	}

	/**
	 * @param styleUrl
	 *            the styleUrl to set
	 */
	public void setStyleUrl(String styleUrl) {
		this.styleUrl = styleUrl;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
        // note this exposes the internal representation by returning reference to mutable object
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime == null ? null : (Date)startTime.clone();
	}

	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
        // note this exposes the internal representation by returning reference to mutable object
		return endTime;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(Date endTime) {
        this.endTime = endTime == null ? null : (Date)endTime.clone();
	}

	public TaggedMap getViewGroup() {
		return viewGroup;
	}

    /**
     * Set ViewGroup on feature (e.g. Camera or LookAt element)
     * @param viewGroup
     */
	public void setViewGroup(TaggedMap viewGroup) {
		this.viewGroup = viewGroup;
	}

	public TaggedMap getRegion() {
		return region;
	}

    /**
     * Set Region on feature
     * @param region
     */
	public void setRegion(TaggedMap region) {
		this.region = region;
	}

	public Boolean getVisibility() {
		return visibility;
	}

    /**
     * Specifies whether the feature is "visible" when it is initially loaded
     * @param visibility Flag whether feature is visible or not.
     *      default value is null (or undefined).
     */
    public void setVisibility(Boolean visibility) {
        this.visibility = visibility;
    }
    
	/**
	 * @return the elements
	 */
	public List<Element> getElements() {
		return elements;
	}

	/**
	 * @param elements the elements to set
	 */
	public void setElements(List<Element> elements) {
		this.elements = elements;
	}

	/**
	 * Read object from the data stream.
	 * 
	 * @param in
	 *            the input stream, never <code>null</code>
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		super.readData(in);
		name = in.readString();
		description = in.readString();
		styleUrl = in.readString();
		long val = in.readLong();
		if (val > -1) {
			startTime = new Date(val);
		} else {
			startTime = null;
		}
		val = in.readLong();
		if (val > -1) {
			endTime = new Date(val);
		} else {
			endTime = null;
		}
		viewGroup = (TaggedMap) in.readObject();
		region = (TaggedMap) in.readObject();
		visibility = (Boolean) in.readScalar();
		elements = (List<Element>) in.readObjectCollection();
		if (elements == null) {
			elements = new ArrayList<Element>();
		}
	}

	/**
	 * Write the object to the data stream
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeString(name);
		out.writeString(description);
		out.writeString(styleUrl);
		if (startTime != null)
			out.writeLong(startTime.getTime());
		else 
			out.writeLong(-1);
		if (endTime != null)
			out.writeLong(endTime.getTime());
		else 
			out.writeLong(-1);
		out.writeObject(viewGroup);
		out.writeObject(region);
		out.writeScalar(visibility);
		out.writeObjectCollection(elements);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((region == null) ? 0 : region.hashCode());
		result = prime * result
				+ ((startTime == null) ? 0 : startTime.hashCode());
		result = prime * result
				+ ((styleUrl == null) ? 0 : styleUrl.hashCode());
		result = prime * result
				+ ((viewGroup == null) ? 0 : viewGroup.hashCode());
		result = prime * result
				+ ((visibility == null) ? 0 : visibility.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Common other = (Common) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		if (endTime == null) {
			if (other.endTime != null)
				return false;
		} else if (!endTime.equals(other.endTime))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (region == null) {
			if (other.region != null)
				return false;
		} else if (!region.equals(other.region))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		if (styleUrl == null) {
			if (other.styleUrl != null)
				return false;
		} else if (!styleUrl.equals(other.styleUrl))
			return false;
		if (viewGroup == null) {
			if (other.viewGroup != null)
				return false;
		} else if (!viewGroup.equals(other.viewGroup))
			return false;
		if (visibility == null) {
			if (other.visibility != null)
				return false;
		} else if (!visibility.equals(other.visibility))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
        StringBuilder b = new StringBuilder(super.toString());
        if (name != null) {
            b.append(" name = ");
            b.append(name);
            b.append('\n');
        }
        if (description != null) {
            b.append(" description = ");
            b.append(description);
            b.append('\n');
        }
        if (startTime != null) {
            b.append(" startTime = ");
            b.append(startTime);
            b.append('\n');
        }
        if (endTime != null) {
            b.append(" endTime = ");
            b.append(endTime);
            b.append('\n');
        }
        if (styleUrl != null) {
            b.append(" styleUrl = ");
            b.append(styleUrl);
            b.append('\n');
        }
        if (viewGroup != null && !viewGroup.isEmpty()) {
            b.append(" viewGroup = ");
            b.append(viewGroup);
            b.append('\n');
        }
        if (region != null && !region.isEmpty()) {
            b.append(" region = ");
            b.append(region);
            b.append('\n');
        }
        if (elements != null && !elements.isEmpty()) {
            b.append(" elements = ");
            b.append(elements);
            b.append('\n');
        }
        return b.toString();
	}

}

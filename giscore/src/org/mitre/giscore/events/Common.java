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
import java.util.Date;

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
	protected String name;
	protected String description;
	private Boolean visibility;
	protected Date startTime;
	protected Date endTime;
	protected String styleUrl;
	private TaggedMap viewGroup;
	private TaggedMap region;

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/*
	 * (non-Javadoc)
	 * 
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
        return b.toString();
	}

}

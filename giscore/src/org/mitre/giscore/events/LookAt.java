package org.mitre.giscore.events;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Simple bean that mirrors the LookAt element in KML.

 * @author Jason Mathews, MITRE Corp.
 * Date: May 28, 2009 2:03:57 PM
 */
public class LookAt {
    
    public Double longitude;			// default = 0.0
    public Double latitude;			// default = 0.0
    public Double altitude;			// default = 0.0
    public Double heading;			// default = 0.0
    public Double tilt;				// default = 0.0
    public Double range;			// default = 0.0
    public AltitudeModeEnumType altitudeMode;	// default = clampToGround

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
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.MULTI_LINE_STYLE);
	}
}

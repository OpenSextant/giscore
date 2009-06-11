package org.mitre.giscore.geometry;

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Basic Model object that represents a 3D object described in a COLLADA file as used in a KML context.  
 * <p>
 * Notes/Limitations:
 * <p>
 *  Only contains AltitudeMode and Location for now until more is supported.
 *  TODO: add other elements (e.g. Link, Orientation, Scale, ResourceMap) 
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 5, 2009 12:32:41 PM
 */
public class Model extends Geometry {

    private static final long serialVersionUID = 1L;

    private AltitudeModeEnumType altitudeMode; // default (clampToGround)

	private Geodetic2DPoint location;

	/**
	 * Construct a Model Geometry object.
	 */
	public Model() {
	}

    public Geodetic2DPoint getLocation() {
        return location;
    }

    public void setLocation(Geodetic2DPoint gp) {
        this.location = gp;
        if (gp != null) {
            if (gp instanceof Geodetic3DPoint) {
                is3D = true;
                bbox = new Geodetic3DBounds((Geodetic3DPoint) gp);
            } else {
                is3D = false;
                bbox = new Geodetic2DBounds(gp);
            }
            numPoints = 1;
            numParts = 1;
        } else {
            bbox = null;
            numPoints = 0;
            numParts = 0;
            is3D = false;
        }
    }

    public AltitudeModeEnumType getAltitudeMode() {
        return altitudeMode;
    }

    /**
	 * @param altitudeMode
	 *            the altitudeMode to set ([clampToGround], relativeToGround, absolute)
	 */
    public void setAltitudeMode(AltitudeModeEnumType altitudeMode) {
        this.altitudeMode = altitudeMode;
    }

    /**
	 * @param altitudeMode
	 *            the altitudeMode to set ([clampToGround], relativeToGround, absolute)
	 */
    public void setAltitudeMode(String altitudeMode) {
        this.altitudeMode = AltitudeModeEnumType.getNormalizedMode(altitudeMode);
    }

    /**
	 * This method returns a Geodetic2DPoint that is at the center of this
	 * Model's Bounding Box, or null if the bounding box (location) is not
	 * defined.
	 *
	 * @return Geodetic2DPoint or Geodetic3DPoint at the center of this Model
	 */
	public Geodetic2DPoint getCenter() {
		// for point feature just return the point
		return location;
	}

    public void accept(IStreamVisitor visitor) {
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

	/**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

}

package org.mitre.giscore.geometry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Basic Model object that represents a 3D object described in a COLLADA file as used in a KML context.  
 * <p>
 * <h4>Notes/Limitations:</h4>
 *  - Only contains AltitudeMode and Location for now until more is supported. <br/>
 *  - TODO: add other elements (e.g. Orientation, Scale, Link, ResourceMap) 
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
        // empty constructor
	}

    @Nullable
    public Geodetic2DPoint getLocation() {
        return location;
    }    

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#computeBoundingBox()
	 */
	protected void computeBoundingBox() {
		bbox = new Geodetic2DBounds(location);
	}

	public void setLocation(Geodetic2DPoint gp) {
        this.location = gp;
        if (gp != null) {
            if (gp instanceof Geodetic3DPoint) {
                is3D = true;
                bbox = new UnmodifiableGeodetic3DBounds((Geodetic3DPoint) gp);
            } else {
                is3D = false;
                bbox = new UnmodifiableGeodetic2DBounds(gp);
            }
        } else {
            bbox = null;
            is3D = false;
        }
    }

	@Override
	public int getNumParts() {
		return location != null ? 1 : 0;
	}

	@Override
	public int getNumPoints() {
		return getNumParts(); // Happily they happen to be the same here
	}

    @Nullable
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
    @Nullable
	public Geodetic2DPoint getCenter() {
		// for point feature just return the point
		return location;
	}

    @Override
    @NonNull
	public List<Point> getPoints() {
		return Collections.singletonList(new Point(location));
	}

	public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.
	 * SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
        boolean isNull = in.readBoolean();
        if (!isNull) {
            boolean is3d = in.readBoolean();
            double elevation = 0.0;
            if (is3d) {
                elevation = in.readDouble();
            }
            Angle lat = readAngle(in);
            Angle lon = readAngle(in);
            if (is3d)
                location = new Geodetic3DPoint(new Longitude(lon), new Latitude(lat), elevation);
            else
                location = new Geodetic2DPoint(new Longitude(lon), new Latitude(lat));
        }
        String s = in.readString();
        altitudeMode = s == null ? null : AltitudeModeEnumType.getNormalizedMode(s);  
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils
	 * .SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
        out.writeBoolean(location == null);
        if (location != null) {
            boolean is3d = location instanceof Geodetic3DPoint;
            out.writeBoolean(is3d);
            if (is3d) {
                out.writeDouble(((Geodetic3DPoint) location).getElevation());
            }
            writeAngle(out, location.getLatitude());
            writeAngle(out, location.getLongitude());
        }
        out.writeString(altitudeMode == null ? null : altitudeMode.toString());
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

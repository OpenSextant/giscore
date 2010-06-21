package org.mitre.giscore.geometry;

import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.*;
import org.mitre.giscore.IStreamVisitor;

import java.io.IOException;

/**
 * The Circle class represents a circular region containing three coordinates (center-point
 * latitude, center-point longitude, circle radius) with latitude and longitude
 * in the WGS84 coordinate reference system and radius in meters.
 *
 * For reference see gml:CircleByCenterPoint or georss:circle definitions.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 7, 2009 6:06:27 PM
 */
public class Circle extends Point {
    
	private static final long serialVersionUID = 1L;

    // delta in comparing radius for equality
    private static final double DELTA = 1e-6;

    private double radius;

    /**
     * gives hint to which shape should be used if native circle isn't available (e.g. KML)
     * in which case circle must be converted to another form.
     */
    public static enum HintType {
        LINE,
        RING,
        POLYGON
    }

    private HintType hint;

    /**
     * The Constructor takes a GeoPoint (either a Geodetic2DPoint or a
     * Geodetic3DPoint) and a radius then initializes a Geometry object for it.
     *
     * @param center
     *            Center GeoPoint to initialize this Circle with (must be Geodetic form)
     * @param radius
     *          Radius in meters from the center point (in meters) 
     * @throws IllegalArgumentException
     *             error if object is null or not valid.
     */
    public Circle(GeoPoint center, double radius) throws IllegalArgumentException {
        super(center);
        this.radius = radius;
    }

    /**
	 * Empty ctor only for object IO.  Constructor must be followed by call to {@code readData()}
     * to initialize the object instance otherwise object is invalid.
	 */
	public Circle() {
		//
	}

    public double getRadius() {
        return radius;
    }
    
	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#computeBoundingBox()
	 */
	protected void computeBoundingBox() {
        final Geodetic2DBounds bounds = new Geodetic2DBounds(getCenter(), radius);
        if (is3D) {
            double elev = ((Geodetic3DPoint)getCenter()).getElevation();
            Geodetic3DPoint westCoord = new Geodetic3DPoint(bounds.getWestLon(), bounds.getNorthLat(), elev);
            Geodetic3DPoint eastCoord = new Geodetic3DPoint(bounds.getEastLon(), bounds.getSouthLat(), elev);
            bbox = new UnmodifiableGeodetic3DBounds(new Geodetic3DBounds(westCoord, eastCoord));
        } else {
            bbox = new UnmodifiableGeodetic2DBounds(bounds);
        }
	}

    /**
     * Get hint of how circle should be handled
     * if circe does not exist in target format (e.g. KML) in which
     * the circle must be converted.
     * @return hint { LINE, RING, or POLYGON }
     */
    public HintType getHint() {
        return hint;
    }

    public void setHint(HintType hint) {
        this.hint = hint;
    }

	/**
	 * This method returns a hash code for this Point object.
	 *
	 * @return a hash code value for this object.
	 */
	@Override
	public int hashCode() {
        // Note we're using approximate equals vs absolute equals on floating point number
        // so must ignore beyond ~6 decimal places in computing the hashCode, otherwise
        // we break the equals-hashCode contract. ChangingDELTA or equals(Circle)
        // may require changing the logic used here also.
		return super.hashCode() ^ ((int) (radius * 10e+6));
	}

	/**
	 * This method is used to test whether two Circle are equal in the sense
	 * that have the same coordinate value and radius.
	 *
	 * @param that
	 *            Circle to compare against this one.
	 * @return true if specified Circle is equal in value to this Circle.
	 */
	public boolean equals(Circle that) {
		return this.getCenter().equals(that.getCenter()) && (Double.compare(this.radius, that.radius) == 0 ||
                        Math.abs(this.radius - that.radius) <= DELTA);
	}

	/**
	 * This method is used to test whether two Circle are equal in the sense
	 * that have the same coordinate value.
	 *
	 * @param that
	 *            Point to compare against this one.
	 * @return true if specified Point is equal in value to this Point.
	 */
	@Override
	public boolean equals(Object that) {
		return (that instanceof Circle) && this.equals((Circle) that);
	}

	/**
	 * The toString method returns a String representation of this Object
	 * suitable for debugging
	 *
	 * @return String containing Geometry Object type, bounding coordintates,
	 *         and number of parts.
	 */
	public String toString() {
		return "Circle at " + getCenter();
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
        radius = in.readDouble();
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
        out.writeDouble(radius);
    }
}

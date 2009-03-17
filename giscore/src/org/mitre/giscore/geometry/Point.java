/***************************************************************************
 * $Id$
 *
 * (C) Copyright MITRE Corporation 2006-2007
 *
 * The program is provided "as is" without any warranty express or implied,
 * including the warranty of non-infringement and the implied warranties of
 * merchantibility and fitness for a particular purpose.  The Copyright
 * owner will not be liable for any damages suffered by you as a result of
 * using the Program.  In no event will the Copyright owner be liable for
 * any special, indirect or consequential damages or lost profits even if
 * the Copyright owner has been advised of the possibility of their
 * occurrence.
 *
 ***************************************************************************/
package org.mitre.giscore.geometry;

import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.GeoPoint;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * The Point class represents a single Geodetic point (Geodetic2DPoint or
 * Geodetic3DPoint) for input and output in GIS formats such as ESRI Shapefiles
 * or Google Earth KML files. In ESRI Shapefiles, this object corresponds to a
 * ShapeType of Point or PointZ. In Google KML files, this object corresponds to
 * a Geometry object of type Point.
 * 
 * @author Paul Silvey
 */
public class Point extends Geometry {
    
    private static final long serialVersionUID = 1L;

	private final Geodetic2DPoint pt; // or extended Geodetic3DPoint

	/**
	 * Ctor, create a point given a lat and lon value in a WGS84 spatial
	 * reference system.
	 * 
	 * @param lat
	 *            the latitude in degrees
	 * @param lon
	 *            the longitude in degrees
	 */
	public Point(double lat, double lon) {
        this(new Geodetic2DPoint(new Longitude(lon,
				Angle.DEGREES), new Latitude(lat, Angle.DEGREES)));
	}

	/**
	 * The Constructor takes a GeoPoint that is either a Geodetic2DPoint or a
	 * Geodetic3DPoint and initializes a Geometry object for it.
	 * 
	 * @param gp
	 *            GeoPoint to initialize this Point with (must be Geodetic form)
	 * @throws IllegalArgumentException
	 *             error if object is not valid.
	 */
	public Point(GeoPoint gp) throws IllegalArgumentException {
		if (gp == null)
			throw new IllegalArgumentException("Point must not be null");
		if (gp instanceof Geodetic3DPoint) {
			is3D = true;
			bbox = new Geodetic3DBounds((Geodetic3DPoint) gp);
		} else if (gp instanceof Geodetic2DPoint) {
			is3D = false;
			bbox = new Geodetic2DBounds((Geodetic2DPoint) gp);
		} else
			throw new IllegalArgumentException("Point must be in Geodetic form");
		pt = (Geodetic2DPoint) gp;
		numParts = 1;
		numPoints = 1;
	}

	/**
	 * This method returns a Geodetic2DPoint that is at the center of this
	 * Geometry object's Bounding Box, or null if the bounding box is not
	 * defined.
	 * 
	 * @return Geodetic2DPoint at the center of this Geometry object
	 */
	public Geodetic2DPoint getCenter() {
		// for point feature just return the point
		return pt;
	}

	/**
	 * This method simply returns the Geodetic point object (modeled like a cast
	 * operation).
	 * 
	 * @return the Geodetic2DPoint object that defines this Point.
	 */
	public Geodetic2DPoint asGeodetic2DPoint() {
		return pt;
	}

	/**
	 * This method returns a hash code for this Point object.
	 * 
	 * @return a hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return pt.hashCode();
	}

	/**
	 * This method is used to test whether two Points are equal in the sense
	 * that have the same coordinate value.
	 * 
	 * @param that
	 *            Point to compare against this one.
	 * @return true if specified Point is equal in value to this Point.
	 */
	public boolean equals(Point that) {
		return this.pt.equals(that.pt);
	}

	/**
	 * This method is used to test whether two Points are equal in the sense
	 * that have the same coordinate value.
	 * 
	 * @param that
	 *            Point to compare against this one.
	 * @return true if specified Point is equal in value to this Point.
	 */
	@Override
	public boolean equals(Object that) {
		return (that instanceof Point) && this.equals((Point) that);
	}

	/**
	 * The toString method returns a String representation of this Object
	 * suitable for debugging
	 * 
	 * @return String containing Geometry Object type, bounding coordintates,
	 *         and number of parts.
	 */
	public String toString() {
		return "Point at (" + pt.getLongitude() + ", " + pt.getLatitude() + ")";
	}

	public void accept(StreamVisitorBase visitor) {
		visitor.visit(this);
	}
}

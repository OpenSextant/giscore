/****************************************************************************************
 *  Geometry.java
 *
 *  Created: Feb 20, 2007
 *
 *  @author Paul Silvey
 *
 *  (C) Copyright MITRE Corporation 2006
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
package org.mitre.giscore.geometry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * The Geometry abstract class is the basis for all geometric objects in the
 * mediate package.
 */
public abstract class Geometry implements VisitableGeometry, IGISObject,
		IDataSerializable {
	private static final long serialVersionUID = 1L;

	// Instance variables are the responsibility of extending class to
	// initialize
	boolean is3D;
	transient Geodetic2DBounds bbox;

	/**
	 * This method returns a boolean indicating if this geometry object is
	 * 3-dimensional (false means it is based on only Geodetic surface
	 * coordinates of Longitude and Latitude, while true means the points also
	 * contain elevation values in meters).
	 * 
	 * @return true if elevation values are included in the point data, false
	 *         otherwise.
	 */
	public boolean is3D() {
		return is3D;
	}

	/**
	 * This method returns the Geodetic2DBounds that encloses this Geometry
	 * object.
	 * 
	 * @return Geodetic2DBounds object enclosing this Geometry object.
	 */
	public Geodetic2DBounds getBoundingBox() {
		return bbox;
	}

	/**
	 * This method returns the number of separate parts of the Geometry object.
	 * The count of parts corresponds to the count of parts for use in 
	 * creating ESRI Shapefiles. 
	 * 
	 * @return integer number of separate parts that make up this Geometry
	 *         object.
	 */
	public abstract int getNumParts();

	/**
	 * This method returns the total number of points in all the parts of this
	 * Geometry object.
	 * 
	 * @return integer number of total points in all the parts of this Geometry
	 *         object.
	 */
	public abstract int getNumPoints();

	/**
	 * This method returns a <code>Geodetic2DPoint</code> or <code>Geodetic3DPoint</code>
     * that is at the center of this Geometry object's Bounding Box or null if the
     * bounding box is not defined.
	 * 
	 * @return <code>Geodetic2DPoint</code> or <code>Geodetic3DPoint</code> at the center of this Geometry object
	 */
	public Geodetic2DPoint getCenter() {
		if (bbox == null)
			return null;
		double wLonRad = bbox.westLon.inRadians();
		double eLonRad = bbox.eastLon.inRadians();
		if (eLonRad < wLonRad)
			eLonRad += (2.0 * Math.PI);
		double cLonRad = wLonRad + ((eLonRad - wLonRad) / 2.0);
		double sLatRad = bbox.southLat.inRadians();
		double nLatRad = bbox.northLat.inRadians();
		double cLatRad = sLatRad + ((nLatRad - sLatRad) / 2.0);
        // can't assume if is3D == true that bbox == Geodetic3DBounds 
        if (bbox instanceof Geodetic3DBounds) {
            Geodetic3DBounds bbox3d = (Geodetic3DBounds)bbox;
            double cElev = (bbox3d.maxElev + bbox3d.minElev) / 2.0;
            return new Geodetic3DPoint(new Longitude(cLonRad),
				    new Latitude(cLatRad), cElev);
        }

		return new Geodetic2DPoint(new Longitude(cLonRad),
				new Latitude(cLatRad));
	}

	/**
	 * Tests whether this geometry is a container of otherGeom such as
	 * MultiPolygon is a container of Polygons, MultiPoint is a container of
	 * Point, etc.
	 * 
	 * @param otherGeom
	 *            the geometry from which to test if this is a container for
	 * @return true if the geometry of this object is a "proper" container for
	 *         otherGeom features Point, Line, LinearRing, Polygon are treated
	 *         as primitive features and not considered "proper" containers
	 *         though Lines are contain points, polygons contain LinearRings,
	 *         etc.
	 * 
	 *         By default return false and override only for container
	 *         Geometries (e.g. MultiPolygon, MultiPoint, etc.)
	 */
	public boolean containerOf(Geometry otherGeom) {
		// primitive objects { e.g. Point, Line, Polygon, etc. } return false
		// since
		// they are treated as core elements not containers.
		return false;
	}

	/**
	 * The toString method returns a String representation of this object
	 * suitable for debugging
	 * 
	 * @return String containing Geometry object type, bounding coordintates,
	 *         and number of parts.
	 */
	public abstract String toString();

	/**
	 * This method using reflection to determine whether two Geometries are equal.
	 *
	 * @param obj
	 *            Geometry to compare against this one.
	 * @return true if specified Geometry is equal in value to this Geometry.
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/**
	 * This method returns a hash code for this Geometry object
	 * using reflection to compute aggregate hashCode of its
	 * non-static and non-transient fields and those of it superclasses. 
	 *
	 * @return a hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.events.IGISObject#writeData(java.io.DataOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		if (bbox == null) {
			out.writeBoolean(false);
		} else {
			out.writeBoolean(true);
            if (bbox instanceof Geodetic3DBounds) {
                out.writeBoolean(true);
                Geodetic3DBounds bbox3d = (Geodetic3DBounds)bbox;
                out.writeDouble(bbox3d.minElev);
                out.writeDouble(bbox3d.maxElev);
            }
            else out.writeBoolean(false);
			writeAngle(out, bbox.getNorthLat());
			writeAngle(out, bbox.getSouthLat());
			writeAngle(out, bbox.getEastLon());
			writeAngle(out, bbox.getWestLon());
            // fails to write out min/max elevation
		}
		out.writeBoolean(is3D);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.events.IGISObject#readData(java.io.DataInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		boolean hasBbox = in.readBoolean();
		if (hasBbox) {
            boolean isBbox3d = in.readBoolean();
            if (isBbox3d) {
                Geodetic3DBounds bbox3d = new Geodetic3DBounds();
                bbox3d.minElev = in.readDouble();
                bbox3d.maxElev = in.readDouble();
                bbox = bbox3d;
            } else{
			    bbox = new Geodetic2DBounds();
            }
			bbox.setNorthLat(new Latitude(readAngle(in)));
			bbox.setSouthLat(new Latitude(readAngle(in)));
			bbox.setEastLon(new Longitude(readAngle(in)));
			bbox.setWestLon(new Longitude(readAngle(in)));
		}
		is3D = in.readBoolean();
	}

	/**
	 * @param in
	 * @return
	 * @throws IOException
	 */
	protected Angle readAngle(SimpleObjectInputStream in) throws IOException {
		double angleInDegrees = in.readDouble();
		return new Angle(angleInDegrees, Angle.DEGREES);
	}

	/**
	 * Write an angle
	 * 
	 * @param out
	 * @param angle
	 * @throws IOException
	 */
	protected void writeAngle(SimpleObjectOutputStream out, Angle angle)
			throws IOException {
		if (angle != null) {
			out.writeDouble(angle.inDegrees());
		} else {
			out.writeDouble(0.0);
		}
	}

	/**
	 * @return the component points for the given geometry
	 */
	public abstract List<Point> getPoints();
	
	/**
	 * @param i the desired part, 0 origin
	 * @return the referenced part
	 */
	public Geometry getPart(int i) {
		if (i == 0)
			return this;
		else
			return null;
	}
}

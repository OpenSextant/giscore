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

import java.io.Serializable;

import org.mitre.giscore.events.IGISObject;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * The Geometry abstract class is the basis for all geometric objects in the mediate package.
 */
public abstract class Geometry implements VisitableGeometry, IGISObject, Serializable {
	private static final long serialVersionUID = 1L;
	
	// Instance variables are the responsibility of extending class to initialize
    boolean is3D;
    Geodetic2DBounds bbox;
    int numParts;
    int numPoints;

    /**
     * This method returns a boolean indicating if this geometry object is 3-dimensional (false
     * means it is based on only Geodetic surface coordinates of Longitude and Latitude, while
     * true means the points also contain elevation values in meters).
     *
     * @return true if elevation values are included in the point data, false otherwise.
     */
    public boolean is3D() {
        return is3D;
    }

    /**
     * This method returns the Geodetic2DBounds that encloses this Geometry object.
     *
     * @return Geodetic2DBounds object enclosing this Geometry object.
     */
    public Geodetic2DBounds getBoundingBox() {
        return bbox;
    }

    /**
     * This method returns the number of separate parts of the Geometry object.  Note that this
     * is used by ESRI Shapefiles, so MultiNestedRings geometry objects treat each Ring within
     * their NestedRings as a separate part, instead of counting the number of NestedRings as
     * parts. This flattening of the hierarchy is because ESRI polygons can have multiple outer
     * rings, whereas KML polygons can not.
     *
     * @return integer number of separate parts that make up this Geometry object.
     */
    public int getNumParts() {
        return numParts;
    }

    /**
     * This method returns the total number of points in all the parts of this Geometry object.
     *
     * @return integer number of total points in all the parts of this Geometry object.
     */
    public int getNumPoints() {
        return numPoints;
    }

    /**
     * This method returns a Geodetic2DPoint that is at the center of this Geometry object's
     * Bounding Box, or null if the bounding box is not defined.
     *
     * @return Geodetic2DPoint at the center of this Geometry object
     */
    public Geodetic2DPoint getCenter() {
        if (bbox == null) return null;
        double wLonRad = bbox.westLon.inRadians();
        double eLonRad = bbox.eastLon.inRadians();
        if (eLonRad < wLonRad) eLonRad += (2.0 * Math.PI);
        double cLonRad = wLonRad + ((eLonRad - wLonRad) / 2.0);
        double sLatRad = bbox.southLat.inRadians();
        double nLatRad = bbox.northLat.inRadians();
        double cLatRad = sLatRad + ((nLatRad - sLatRad) / 2.0);
        return new Geodetic2DPoint(new Longitude(cLonRad), new Latitude(cLatRad));
    }

    /**
     * Tests whether this geometry is a container of otherGeom such as
     * MultiPolygon is a container of Polygons, MultiPoint is a container of Point, etc.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *         Point, Line, LinearRing, Polygon are treated as primitive features and not
     *         considered "proper" containers though Lines are contain points, polygons
     *         contain LinearRings, etc.
     * 
     *         By default return false and override only for container Geometries (e.g. MultiPolygon, MultiPoint, etc.)
     */
    public boolean containerOf(Geometry otherGeom) {
        // primitive objects { e.g. Point, Line, Polygon, etc. } return false since
        // they are treated as core elements not containers.
        return false;
    }

    /**
     * The toString method returns a String representation of this object suitable for debugging
     *
     * @return String containing Geometry object type, bounding coordintates, and number of parts.
     */
    public abstract String toString();
}
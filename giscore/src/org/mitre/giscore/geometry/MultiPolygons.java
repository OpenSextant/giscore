/***************************************************************************
 * $Id$
 *
 * (C) Copyright MITRE Corporation 2006-2008
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MultiPolygons class represents an ordered list of Polygon objects for input
 * and output in GIS formats such as ESRI Shapefiles or Google Earth KML files. In ESRI
 * Shapefiles, this type of object corresponds to a ShapeType of Polygon, but this class
 * imposes some additional constraints on the order of the base rings. In ESRI Shapefiles,
 * outer and inner rings of polygons can be included in any order, but here they are grouped
 * such that each outer is followed by its list of inners. In Shapefiles, this object
 * corresponds to a ShapeType of Polygon. This type of object does not exist as a primitive
 * in Google KML files, but it is a simple list of KML Polygon types represented as
 * as a MultiGeometry container with Polygon children.
 * <p/>
 * Note if have polygons of mixed dimensions then MultiPolygons container is downgraded to 2d.
 *
 * @author Paul Silvey
 */
public class MultiPolygons extends Geometry implements Iterable<Polygon> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MultiPolygons.class);

    private final List<Polygon> polygonList, publicPolygonList;

    /**
     * This method returns an iterator for cycling through the Polygons in this Object.
     * This class supports use of Java 'for each' syntax to cycle through the Polygons.
     *
     * @return Iterator over Polygons objects.
     */
    public Iterator<Polygon> iterator() {
        return publicPolygonList.iterator();
    }

	/**
	 * This method returns the {@code Polygon}s in this {@code MultiPolygons}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code Polygon} objects.
	 */
	public Collection<Polygon> getPolygons() {
		return publicPolygonList;
	}

    /**
     * The Constructor takes a list of Polygon Objects to initialize this MultiPolygons object.
     *
     * @param polygonList List of Polygons objects which define the parts of this MultiPolygons.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiPolygons(List<Polygon> polygonList) throws IllegalArgumentException {
        if (polygonList == null || polygonList.size() < 1)
            throw new IllegalArgumentException("MultiPolygons must contain " +
                    "at least 1 Polygons object");
        // Make sure all the polygons have the same number of dimensions (2D or 3D)
        is3D = polygonList.get(0).is3D();
        numParts = 0;
        numPoints = 0;
        boolean mixedDims = false;
        for (Polygon nr : polygonList) {
            if (is3D != nr.is3D()) mixedDims = true;
            numParts += nr.getNumParts();
            numPoints += nr.getNumPoints();
        }
        if (mixedDims) {
            log.info("Polygons have mixed dimensionality: downgrading MultiPolygon to 2d");
            is3D = false;
        }
        bbox = null;
        if (is3D) {
            for (Polygon nr : polygonList) {
                Geodetic3DBounds bbox3 = (Geodetic3DBounds) nr.getBoundingBox();
                if (bbox == null) bbox = new Geodetic3DBounds(bbox3);
                else bbox.include(bbox3);
            }
        } else {
            for (Polygon nr : polygonList) {
                Geodetic2DBounds bbox2 = nr.getBoundingBox();
                if (bbox == null) bbox = new Geodetic2DBounds(bbox2);
                else bbox.include(bbox2);
            }
        }
        this.polygonList = polygonList;
		this.publicPolygonList = Collections.unmodifiableList(polygonList);
    }

    /**
     * Tests whether this MultiPolygons geometry is a container for otherGeom's type.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *          which in this case is a Polygon.
     */
    public boolean containerOf(Geometry otherGeom) {
        return otherGeom instanceof Polygon;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "Polygons within " + bbox + " consists of " +
                polygonList.size() + " Polygons";

    }
    
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
}
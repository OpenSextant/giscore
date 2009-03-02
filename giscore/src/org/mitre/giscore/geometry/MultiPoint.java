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
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MultiPoint class represents a list of geodetic Points for input and output in
 * GIS formats such as ESRI Shapefiles or Google Earth KML files.  In ESRI Shapefiles,
 * this object corresponds to a ShapeType of MultiPoint. This type of object does not
 * exist as a primitive in Google KML files, so it is just written as a list of Points.
 * <p/>
 * Note if have points of mixed dimensions then MultiPoint container is downgraded to 2d.
 *
 * @author Paul Silvey
 */
public class MultiPoint extends Geometry implements Iterable<Point> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MultiPoint.class);

    private final List<Point> pointList, publicPointList;

    /**
     * This method returns an iterator for cycling through the geodetic Points in this MultiPoint.
     * This class supports use of Java 'for each' syntax to cycle through the geodetic Points.
     *
     * @return Iterator over geodetic Point objects.
     */
    public Iterator<Point> iterator() {
        return publicPointList.iterator();
    }

	/**
	 * This method returns the {@code Point}s in this {@code MultiPoint}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code Point} objects.
	 */
	public Collection<Point> getPoints() {
		return publicPointList;
	}

    /**
     * The Constructor takes a list of points and initializes a Geometry Object for this MultiPoint.
     *
     * @param pts List of Geodetic2DPoint point objects to use for the parts of this MultiPoint.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiPoint(List<Point> pts) throws IllegalArgumentException {
        if (pts == null || pts.size() < 1)
            throw new IllegalArgumentException("MultiPoint must contain at least 1 Point");
        // Make sure all the points have the same number of dimensions (2D or 3D)
        is3D = pts.get(0).is3D();
        for (Point p : pts) {
            if (is3D != p.is3D()) {
                log.info("MultiPoint points have mixed dimensionality: downgrading to 2d");
                is3D = false;
                break;
            }
        }
        Geodetic2DPoint gp = pts.get(0).asGeodetic2DPoint();
        bbox = is3D ? new Geodetic3DBounds((Geodetic3DPoint)gp) : new Geodetic2DBounds(gp);
        for (Point p : pts) bbox.include(p.asGeodetic2DPoint());
        pointList = pts;
		publicPointList = Collections.unmodifiableList(pointList);
        numPoints = pts.size();
        numParts = numPoints;      // We might choose to make this 1 instead of numPoints
    }

    /**
     * Tests whether this MultiPoint geometry is a container for otherGeom's type.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *          which in this case is a Point.
     */
    public boolean containerOf(Geometry otherGeom) {
        return otherGeom instanceof Point;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "MultiPoint within " + bbox + " consists of " + pointList.size() + " Points";
    }
    
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
}
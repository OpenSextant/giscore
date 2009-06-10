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

import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LinearRing class represents an ordered list of Geodetic2DPoint points for input
 * and output in GIS formats such as ESRI Shapefiles or Google Earth KML files.
 * <p/>
 * A LinearRing is a closed-loop, double-digitized list, which means the first and
 * last point must be the same. This type of object does not exist as a primitive
 * in ESRI Shapefiles. In Google KML files, this object corresponds to a Geometry
 * object of type LinearRing. Note that the topological predicates assume that
 * LinearRings do not wrap around the international date line.
 * <p/>
 * A LinearRing object in itself may be a closed-line (i.e. polyline) shape or the inner/outer
 * boundary of a polygon so depends on the context of the Ring. The first ring in a Polygon
 * container is considered the outer boundary of that Polygon and any other Rings are used
 * as the inner boundary of a Polygon to create holes in the Polygon.
 * <p/>
 * Notes/Restrictions: <br/>
 *   -LinearRings can have mixed dimensionality but such rings are downgraded to 2d. <br/>
 *   -LinearRing must contain at least 4 Points. <br/>
 *   -If validateTopology is selected then constructor enforces that the ring must start and end
 *    with the same point and the ring does not self-intersect.
 *
 * @author Paul Silvey
 */
public class LinearRing extends Geometry implements Iterable<Point> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LinearRing.class);

    private List<Point> pointList, publicPointList;
    private boolean idlWrap;  // International Date Line Wrap

    /**
     * This method returns an iterator for cycling through the Points in this Ring.
     * This class supports use of Java 'for each' syntax to cycle through the Points.
     *
     * @return Iterator over Geodetic2DPoint point objects
     */
    public Iterator<Point> iterator() {
        return publicPointList.iterator();
    }

	/**
	 * This method returns the points in this ring.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the point objects.
	 */
	public List<Point> getPoints() {
		return publicPointList;
	}

    // This method will check that this LinearRing is closed and non-self-intersecting.
    private void validateTopology(List<Point> pts) throws IllegalArgumentException {
        int n = pts.size();
        // Verify that ring is closed, i.e. that beginning and ending point are equal
        if (!pts.get(0).equals(pts.get(n - 1)))
            throw new IllegalArgumentException("LinearRing must start and end with the same point.");

        // Look at each line segment in turn, and compare to every other non-neighbor
        // For neighbor segments, make sure distance to non-shared endpoint is positive.
        // This requires (n*(n-1)/2) comparisons
        Geodetic2DPoint gp1, gp2, gp3, gp4;
        for (int i = 0; i < n - 2; i++) {
            gp1 = pts.get(i).asGeodetic2DPoint();
            double x1 = gp1.getLongitude().inRadians();
            double y1 = gp1.getLatitude().inRadians();
            gp2 = pts.get(i + 1).asGeodetic2DPoint();
            double x2 = gp2.getLongitude().inRadians();
            double y2 = gp2.getLatitude().inRadians();
            for (int j = i + 1; j < n - 1; j++) {
                gp3 = pts.get(j).asGeodetic2DPoint();
                double x3 = gp3.getLongitude().inRadians();
                double y3 = gp3.getLatitude().inRadians();
                gp4 = pts.get(j + 1).asGeodetic2DPoint();
                double x4 = gp4.getLongitude().inRadians();
                double y4 = gp4.getLatitude().inRadians();
                boolean inv;
                if ((j - i) == 1) {
                    // make sure non-zero distance from (x1, y1)->(x2, y2) to (x4, y4)
                    inv = (Line2D.Double.ptLineDist(x1, y1, x2, y2, x4, y4) == 0.0);
                } else if ((i == 0) && (j == (n - 2))) {
                    // make sure non-zero distance from (x1, y1)->(x2, y2) to (x3, y3)
                    inv = (Line2D.Double.ptLineDist(x1, y1, x2, y2, x3, y3) == 0.0);
                } else {
                    // make sure non-adjacent segments do not intersect
                    inv = Line2D.Double.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4);
                }
                if (inv) throw new IllegalArgumentException("LinearRing can not self-intersect.");
            }
        }
    }

    /**
     * Private init method shared by Constructors
     * @param pts
     * @param validateTopology
     * @throws IllegalArgumentException error if point list is empty
     *          or number of points is less than 4
     */
    private void init(List<Point> pts, boolean validateTopology) throws IllegalArgumentException {
        if (pts == null || pts.size() < 4)
            throw new IllegalArgumentException("LinearRing must contain at least 4 Points");
        if (validateTopology) validateTopology(pts);
        // Make sure all the points have the same number of dimensions (2D or 3D)
        is3D = pts.get(0).is3D();
        for (Point p : pts) {
            if (is3D != p.is3D()) {
                log.info("LinearRing points have mixed dimensionality: downgrading ring to 2d");
                is3D = false;
                break;
            }
        }
        Geodetic2DPoint gp1 = pts.get(0).asGeodetic2DPoint();
        bbox = is3D ? new Geodetic3DBounds((Geodetic3DPoint) gp1) : new Geodetic2DBounds(gp1);

        Geodetic2DPoint gp2;
        double lonRad1, lonRad2;
        idlWrap = false;
        for (Point p : pts) {
            gp2 = p.asGeodetic2DPoint();
            bbox.include(gp2);
            // Test for Longitude wrap at International Date Line (IDL)
            // Line segments always connect following the shortest path around the globe,
            // and we assume lines are clipped at the IDL crossing, so if there is a sign
            // change between points and one of the points is -180.0, we classify this LinearRing
            // as having wrapped. This allows the -180 to be written as +180 on export,
            // to satisfy GIS tools that expect this.
            lonRad1 = gp1.getLongitude().inRadians();
            lonRad2 = gp2.getLongitude().inRadians();
            // It is a wrap if any segment that changes lon sign has an endpoint on the line
            if (((lonRad1 < 0.0 && lonRad2 >= 0.0) || (lonRad2 < 0.0 && lonRad1 >= 0.0)) &&
                    (lonRad1 == -Math.PI || lonRad2 == -Math.PI)) idlWrap = true;
            gp1 = gp2;
        }
        pointList = pts;
		publicPointList = Collections.unmodifiableList(pointList);
        numParts = 1;
        numPoints = pts.size();
    }

    /**
     * This Constructor takes a list of points and initializes a Geometry Object for this Ring. By
     * default, it does not do topology validation.  To do validation, use alternate constructor.
     *
     * @param pts List of Geodetic2DPoint point objects to use for the vertices of this Ring
     * @throws IllegalArgumentException error if point list is empty
     *          or number of points is less than 4
     */
    public LinearRing(List<Point> pts) throws IllegalArgumentException {
        init(pts, false);
    }

    /**
     * This Constructor takes a list of points and initializes a Geometry Object for this Ring,
     * performing topology validation if requested.
     *
     * @param pts              List of Geodetic2DPoint objects to use as Ring vertices
     * @param validateTopology boolean flag indicating that Ring topology should be validated
     * @throws IllegalArgumentException error if point list is empty
     *          or number of points is less than 4
     */
    public LinearRing(List<Point> pts, boolean validateTopology) throws IllegalArgumentException {
        init(pts, validateTopology);
    }

    /**
     * Empty ctor for object io
     */
    public LinearRing() {
    	// 
    }
    
    /**
     * This Constructor takes a bounding box and initializes a Geometry Object
     * for it. This ring will be clockwise.
     *
     * @param box the bounding box to represent as a ring
     * @throws IllegalArgumentException if the bounding box is a point or a line
     */
    public LinearRing(Geodetic2DBounds box) {
        if (box.getEastLon().inRadians() == box.getWestLon().inRadians())
            log.warn("Bounding box not a polygon - east and west points are the same.");
        if (box.getNorthLat().inRadians() == box.getSouthLat().inRadians())
            log.warn("Bounding box not a polygon - north and south points are the same.");

        final List<Point> points = new ArrayList<Point>(5);
        points.add(new Point(new Geodetic2DPoint(box.getWestLon(), box.getSouthLat())));
        points.add(new Point(new Geodetic2DPoint(box.getWestLon(), box.getNorthLat())));
        points.add(new Point(new Geodetic2DPoint(box.getEastLon(), box.getNorthLat())));
        points.add(new Point(new Geodetic2DPoint(box.getEastLon(), box.getSouthLat())));
        points.add(new Point(new Geodetic2DPoint(box.getWestLon(), box.getSouthLat())));
        init(points, false);
    }

    /**
     * This predicate method is used to tell if this Ring has positive Longitude points
     * that are part of segments which are clipped at the International Date Line (IDL)
     * (+/- 180 Longitude). If so, -180 values may need to be written as +180 by
     * export methods to satisfy GIS tools that expect this.
     *
     * @return boolean value indicating whether this ring is clipped at the IDL
     */
    public boolean clippedAtDateLine() {
        return idlWrap;
    }

    /**
     * This predicate method tests whether this Ring object is in clockwise point order or not.
     *
     * @return true if this Ring's points are in clockwise order, false otherwise
     */
    public boolean clockwise() {
        Geodetic2DPoint gp1, gp2;
        double doubleArea = 0.0;
        for (int i = 0; i < pointList.size() - 1; i++) {
            gp1 = pointList.get(i).asGeodetic2DPoint();
            gp2 = pointList.get(i + 1).asGeodetic2DPoint();
            doubleArea += gp1.getLongitude().inRadians() * gp2.getLatitude().inRadians();
            doubleArea -= gp1.getLatitude().inRadians() * gp2.getLongitude().inRadians();
        }
        return (doubleArea < 0);
    }

    /**
     * This predicate method determines whether this Ring contains a test point, by counting
     * line crossings and determining their parity.  This version of the algorithm adapted from
     * code written by W. Randolph Franklin.
     *
     * @param p Geodetic2DPoint test point
     * @return true if the test point is inside of this ring of points
     */
    public boolean contains(Geodetic2DPoint p) {
        boolean in = false;
        double x = p.getLongitude().inRadians();
        double y = p.getLatitude().inRadians();
        Geodetic2DPoint gp1, gp2;
        for (int i = 0; i < pointList.size() - 1; i++) {
            gp1 = pointList.get(i).asGeodetic2DPoint();
            gp2 = pointList.get(i + 1).asGeodetic2DPoint();
            double xi = gp1.getLongitude().inRadians();
            double yi = gp1.getLatitude().inRadians();
            double xj = gp2.getLongitude().inRadians();
            double yj = gp2.getLatitude().inRadians();
            if ((((yi <= y) && (y < yj)) || ((yj <= y) && (y < yi))) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi))
                in = !in;
        }
        return in;
    }

    /**
     * This predicate method determines whether this Ring overlaps another Ring (that).
     *
     * @param that Ring object to test for overlapping with this Ring
     * @return true if this Ring overlaps the specified Ring 'that'
     */
    public boolean overlaps(LinearRing that) {
        // Compare each segment in this ring to every segment in that ring to see if they cross.
        // Short-circuit exit as soon as any pair of segments being compared cross.
        Geodetic2DPoint gp1, gp2, gp3, gp4;
        int n1 = this.pointList.size();
        int n2 = that.pointList.size();
        for (int i = 0; i < n1 - 1; i++) {
            gp1 = this.pointList.get(i).asGeodetic2DPoint();
            double x1 = gp1.getLongitude().inRadians();
            double y1 = gp1.getLatitude().inRadians();
            gp2 = this.pointList.get(i + 1).asGeodetic2DPoint();
            double x2 = gp2.getLongitude().inRadians();
            double y2 = gp2.getLatitude().inRadians();
            for (int j = 0; j < n2 - 1; j++) {
                gp3 = that.pointList.get(j).asGeodetic2DPoint();
                double x3 = gp3.getLongitude().inRadians();
                double y3 = gp3.getLatitude().inRadians();
                gp4 = that.pointList.get(j + 1).asGeodetic2DPoint();
                double x4 = gp4.getLongitude().inRadians();
                double y4 = gp4.getLatitude().inRadians();
                if (Line2D.Double.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4))
                    return true;
            }
        }
        return false;
    }

    /**
     * This predicate method determines whether this Ring completely contains another Ring
     * (that).  It first makes sure that no pair of line segments between the two rings
     * intersect, and if that is true, it then checks to see if a single point of the
     * proposed inner Ring is in fact inside this Ring.
     *
     * @param that Ring object to test for containment within this Ring
     * @return true if this Ring completely contains the specified Ring 'that'
     */
    public boolean contains(LinearRing that) {
        // If not overlapping, then all points are either in or they're out, so only test one
        return (!this.overlaps(that) &&
                this.contains(that.pointList.get(0).asGeodetic2DPoint()));
    }

    /**
     * This predicate method determines whether the specified Ring (that) has any area in common with
     * this Ring, that is, whether they intersect or not.
     *
     * @param that Ring to test for intersection with this Ring
     * @return true if the specified Ring intersects this Ring
     */
    public boolean intersects(LinearRing that) {
        // If not overlapping, then see if a point from this is in that, or vice versa
        return (this.overlaps(that) ||
                this.contains(that.pointList.get(0).asGeodetic2DPoint()) ||
                that.contains(this.pointList.get(0).asGeodetic2DPoint()));
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts
     */
    public String toString() {
        return "LinearRing within " + bbox + " consists of " + pointList.size() + " Points";
    }
    
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
    
	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
    @SuppressWarnings("unchecked")
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException {
		super.readData(in);
		idlWrap = in.readBoolean();
		List<Point> plist = (List<Point>) in.readObjectCollection();
		init(plist, false);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeBoolean(idlWrap);
		out.writeObjectCollection(pointList);
	}
}

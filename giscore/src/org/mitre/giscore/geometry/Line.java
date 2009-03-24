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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Line class represents an ordered list of Geodetic2DPoint points for input and output in
 * GIS formats such as ESRI Shapefiles or Google Earth KML files.  This type of object does
 * not exist as a primitive in ESRI Shapefiles, but it can be represented as an ESRI Polyline
 * with a single part.  In Google KML files, this object corresponds to a Geometry object of
 * type LineString. <p/>
 *
 * Note if have points of mixed dimensions then line is downgraded to 2d.
 *
 * @author Paul Silvey
 */
public class Line extends Geometry implements Iterable<Point> {
	private static final long serialVersionUID = 1L;
	
    private static final Logger log = LoggerFactory.getLogger(Line.class);

    private List<Point> pointList, publicPointList;
    private boolean idlWrap;  // International Date Line Wrap

    /**
     * This method returns an iterator for cycling through the Points in this Line.
     * This class supports use of Java 'for each' syntax to cycle through the Points.
     *
     * @return Iterator over Geodetic2DPoint point objects.
     */
    public Iterator<Point> iterator() {
        return publicPointList.iterator();
    }

	/**
	 * This method returns the points in this line.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the point objects.
	 */
	public Collection<Point> getPoints() {
		return publicPointList;
	}

	/**
	 * Empty ctor for object io
	 */
	public Line() {
		//
	}
	
    /**
     * The Constructor takes a list of points and initializes a Geometry Object for this Line.
     *
     * @param pts List of Geodetic2DPoint point objects to use for the parts of this Line.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public Line(List<Point> pts) throws IllegalArgumentException {
        if (pts == null || pts.size() < 2)
            throw new IllegalArgumentException("Line must contain at least 2 Points");
        init(pts);
    }

    /**
     * Initialize given a set of points
     * @param pts
     */
	private void init(List<Point> pts) {
		// Make sure all the points have the same number of dimensions (2D or 3D)
        is3D = pts.get(0).is3D();
        for (Point p : pts) {
            if (is3D != p.is3D()) {
                log.info("Line points have mixed dimensionality: downgrading line to 2d");
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
            // change between points and one of the points is -180.0, we classify this Line
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
     * The toString method returns a String representation of this Object suitable for debugging.
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "Line within " + bbox + " consists of " + pointList.size() + " Points";
    }

    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
    @SuppressWarnings("unchecked")
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		idlWrap = in.readBoolean();
		List<Point> plist = (List<Point>) in.readObjectCollection();
		init(plist);
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

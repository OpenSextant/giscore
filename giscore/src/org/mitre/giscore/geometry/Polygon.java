/***************************************************************************
 * $Id$
 *
 * (C) Copyright MITRE Corporation 2008
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Polygon class represents an ordered list of LinearRing objects for input and output in
 * GIS formats such as ESRI Shapefiles or Google Earth KML files. A polygon begins with
 * an outer ring, and is followed by zero or more inner rings to create holes in the
 * Polygon.  For direct compatibility with ESRI Shapefile Polygons, the points in the
 * outer ring must be in clockwise order, and any inner rings must have their points in
 * counter-clockwise order and be properly inside the outer ring (only vertex sharing
 * is allowed, not segment intersection). This type of object does not exist as a
 * primitive in input ESRI Shapefiles, since it is more restrictive than a ShapeType
 * of Polygon (see MultiPolygons). However, it may be written as an ESRI Shapefile
 * Polygon. In Google KML files, this object corresponds to a Geometry object of
 * type Polygon.
 * <p/>
 * Notes/restrictions: <br/>
 *   Polygon rings may have mixed dimensionality but such polygons will be downgraded to 2d. <br/>
 *   If validateTopology is selected then constructor enforces that the outer ring lists its points
 *   in the clockwise direction, and one or more fully and properly contained inner rings that
 *   list their points counter-clockwise.
 *
 * @author Jason Mathews
 */
public class Polygon extends Geometry implements Iterable<LinearRing> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Polygon.class);

    private LinearRing outerRing;
    private List<LinearRing> ringList, publicRingList;

    /**
     * This Constructor takes an outer boundary ring and initializes a Geometry object for this Polygon
     * object. By default, it does not do topology validation. To do validation, use alternate
     * constructor.
     *
     * @param outerRing required outer boundary ring of polygon
     *
     * @throws IllegalArgumentException error if object is not valid.
     */
    public Polygon(LinearRing outerRing) throws IllegalArgumentException {
        this.outerRing = outerRing;
        init(null, false);
    }

    /**
     * This Constructor takes an outer boundary ring and initializes a Geometry object for this Polygon
     * object, performing validation checking if requested.
     *
     * @param outerRing required outer boundary ring of polygon
     * @param validateTopology boolean flag indicating whether validation should be performed.
     *
     * @throws IllegalArgumentException error if object is not valid.
     */
    public Polygon(LinearRing outerRing, boolean validateTopology) throws IllegalArgumentException {
        this.outerRing = outerRing;
        init(null, validateTopology);
    }

    /**
     * This Constructor takes an outer boundary ring with a list of rings and initializes
     * a Geometry object for this Polygon object. By default, it does not do topology
     * validation. To do validation, use alternate constructor.
     *
     * @param outerRing required outer boundary ring of polygon
     * @param innerRings List of Ring objects to use for the parts of this Polygon.
     *
     * @throws IllegalArgumentException error if object is not valid.
     */
    public Polygon(LinearRing outerRing, List<LinearRing> innerRings) throws IllegalArgumentException {
        this.outerRing = outerRing;
        init(innerRings, false);
    }

    /**
     * This Constructor takes an outer boundary ring with a list of rings and initializes
     * a Geometry Object for this Polygon object, performing validation checking if requested.
     *
     * @param outerRing required outer boundary ring of polygon
     * @param innerRings List of inner ring objects to use for the parts of this Polygon.
     * @param validateTopology boolean flag indicating whether validation should be performed.
     * 
     * @throws IllegalArgumentException error if object is not valid.
     */
    public Polygon(LinearRing outerRing, List<LinearRing> innerRings, boolean validateTopology) throws IllegalArgumentException {
        this.outerRing = outerRing;
        init(innerRings, validateTopology);
    }

    /**
     * This method returns an iterator for cycling through the inner rings in this Object.
     * This class supports use of Java 'for each' syntax to cycle through the inner rings.
     * The outer ring must be accessed via the getOuterRing() method.
     *
     * @return Iterator over LinearRing objects.
     */
    public Iterator<LinearRing> iterator() {
        return publicRingList.iterator();
    }

	/**
	 * This method returns the inner {@code LinearRing}s in this {@code Polygon}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code LinearRing} objects.
	 */
	public Collection<LinearRing> getLinearRings() {
		return publicRingList;
	}

    // This method will check that this Polygon Object has a single outer ring that
    // lists its points in the clockwise direction, and one or more fully and properly
    // contained inner rings that list their points counter-clockwise.
    private void validateTopology(List<LinearRing> rings) throws IllegalArgumentException {
        int n = rings.size();
        // Verify that outer ring is in clockwise point order
        if (!outerRing.clockwise())
            throw new IllegalArgumentException("Outer LinearRing in Polygon must be " +
                    "in clockwise point order");

        // Verify that all the inner rings are in counter-clockwise point order, are fully
        // contained in the outer ring, and are non-intersecting with each other.
        for (int i = 0; i < n; i++) {
            LinearRing inner = rings.get(i);
            if (inner.clockwise())
                throw new IllegalArgumentException("All inner rings in Polygon must be " +
                    "in counter-clockwise point order");
            // Verify that inner rings are properly contained inside outer ring 
            if (!outerRing.contains(inner))
                throw new IllegalArgumentException("All inner rings in Polygon must be " +
                    "properly contained in outer ring");
            // Verify that inner rings don't overlap with each other
            if (i < n -1)
                for (int j = i + 1; j < n; j++) {
                    if (inner.overlaps(rings.get(j)))
                        throw new IllegalArgumentException("Inner rings in Polygon must not " +
                                "overlap with each other");
                }
        }
    }

    // Private init method shared by Constructors
    private void init(List<LinearRing> rings, boolean validateTopology) throws IllegalArgumentException {
        if (outerRing == null)
            throw new IllegalArgumentException("Polygon must contain an outer boundary ring");
        if (rings == null)
            rings = new ArrayList<LinearRing>(0);
        if (validateTopology) validateTopology(rings);
        // Make sure all the rings have the same number of dimensions (2D or 3D)
        is3D = outerRing.is3D();
        numParts = 1 + rings.size();
        numPoints = outerRing.getNumPoints();
        boolean mixedDims = false;
        for (LinearRing rg : rings) {
            if (is3D != rg.is3D()) mixedDims = true;
            numPoints += rg.getNumPoints();
        }
        if (mixedDims) {
            log.info("Rings have mixed dimensionality: downgrading Polygon to 2d");
            is3D = false;
        }
        if (is3D) {
            Geodetic3DBounds bbox3 = (Geodetic3DBounds) outerRing.getBoundingBox();
            bbox = new Geodetic3DBounds(bbox3);
            for (LinearRing rg : rings) {
                bbox3 = (Geodetic3DBounds) rg.getBoundingBox();
                bbox.include(bbox3);
            }
        } else {
            Geodetic2DBounds bbox2 = outerRing.getBoundingBox();
            bbox = new Geodetic2DBounds(bbox2);
            for (LinearRing rg : rings) {
                bbox2 = rg.getBoundingBox();
                bbox.include(bbox2);
            }
        }
        ringList = rings;
		publicRingList = Collections.unmodifiableList(ringList);
    }

    /**
     * Get outer ring of the polygon
     * 
     * @return outer ring.  This value cannot be null. 
     */
    public LinearRing getOuterRing() {
        return outerRing;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "Polygon within " + bbox + " consists of " + ringList.size() + " inner rings";
    }

    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
    
	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		super.readData(in);
		outerRing = (LinearRing) in.readObject();
		List<LinearRing> lrlist = (List<LinearRing>) in.readObjectCollection();
		init(lrlist, false);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObject(outerRing);
		out.writeObjectCollection(ringList);
	}
}

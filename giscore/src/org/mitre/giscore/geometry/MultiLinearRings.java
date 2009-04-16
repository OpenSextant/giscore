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

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MultiLinearRings class represents an ordered list of LinearRing objects for input
 * and output in GIS formats such as ESRI Shapefiles or Google Earth KML files.
 * <p/>
 * A MultiLinearRings object is a container for LinearRings, which represents a collection
 * of closed-line strings as opposed to inner and outer boundaries of a Polygon represented
 * by a Polygon object.
 * <p/>
 * In Google KML files, this object corresponds to a list of Geometry objects of type
 * LinearRing within a MultiGeometry container.
 * <p/>
 * LinearRings contained with a MultiLinearRings should typically have same dimensionality, but
 * mixed dimensions are allowed but MultiLinearRings dimensionality will be downgraded to 2d.   
 * <p/>
 * If selected the constructor will enforce that all points be in clockwise order for direct
 * compatibility with ESRI Shapefile Polygons. This type of object does not exist as a
 * primitive in input ESRI Shapefiles, since it is more restrictive than a ShapeType of
 * Polygon (see Polygon). However, it may be written as an ESRI Shapefile Polygon.
 *
 * @author Paul Silvey
 */
public class MultiLinearRings extends Geometry implements Iterable<LinearRing> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MultiLinearRings.class);

    private List<LinearRing> ringList, publicRingList;

    /**
     * Empty ctor for object io
     */
    public MultiLinearRings() {
    	//
    }
    
    /**
     * This Constructor takes a list of LinearRings and initializes a Geometry object for this MultiLinearRings
     * object. By default, it does not do topology validation. To do validation, use alternate
     * constructor.
     *
     * @param rings List of LinearRing objects to use for the parts this object.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiLinearRings(List<LinearRing> rings) throws IllegalArgumentException {
        init(rings, false);
    }

    /**
     * This Constructor takes a list of LinearRings and initializes a Geometry Object for this MultiLinearRings
     * object, performing validation checking if requested.
     *
     * @param rings List of LinearRing objects to use for the parts this object.
     * @param validateTopology boolean flag indicating whether validation should be performed.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiLinearRings(List<LinearRing> rings, boolean validateTopology) throws IllegalArgumentException {
        init(rings, validateTopology);
    }

    /**
     * This method returns an iterator for cycling through the Rings in this Object.
     * This class supports use of Java 'for each' syntax to cycle through the Rings.
     *
     * @return Iterator over Line objects.
     */
    public Iterator<LinearRing> iterator() {
        return publicRingList.iterator();
    }

	/**
	 * This method returns the {@code LinearRing}s in this {@code MultiLinearRings}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code LinearRing} objects.
	 */
	public Collection<LinearRing> getLinearRings() {
		return publicRingList;
	}

    // This method will check that this MultiLinearRings Object has rings that
    // lists its points in the clockwise direction.
    private void validateTopology(List<LinearRing> rings) throws IllegalArgumentException {
        // Verify that rings are in clockwise point order.
        for (LinearRing ring : rings) {
            if (!ring.clockwise())
                throw new IllegalArgumentException("LinearRing in MultiLinearRings must be " +
                        "in clockwise point order");
        }
    }

    // Private init method shared by Constructors
    private void init(List<LinearRing> rings, boolean validateTopology) throws IllegalArgumentException {
        if (rings == null || rings.size() < 1)
            throw new IllegalArgumentException("MultiLinearRings must contain at least 1 LinearRing");
        if (validateTopology) validateTopology(rings);
        // Make sure all the rings have the same number of dimensions (2D or 3D)
        is3D = rings.get(0).is3D();
        numParts = rings.size();
        numPoints = 0;
        boolean mixedDims = false;
        for (LinearRing rg : rings) {
            if (is3D != rg.is3D()) mixedDims = true;
            numPoints += rg.getNumPoints();
        }
        if (mixedDims) {
            log.info("LinearRings have mixed dimensionality: downgrading to 2d");
            is3D = false;
        }            
        bbox = null;
        if (is3D) {
            for (LinearRing rg : rings) {
                Geodetic3DBounds bbox3 = (Geodetic3DBounds) rg.getBoundingBox();
                if (bbox == null) bbox = new Geodetic3DBounds(bbox3);
                else bbox.include(bbox3);
            }
        } else {
            for (LinearRing rg : rings) {
                Geodetic2DBounds bbox2 = rg.getBoundingBox();
                if (bbox == null) bbox = new Geodetic2DBounds(bbox2);
                else bbox.include(bbox2);
            }
        }
        ringList = rings;
		publicRingList = Collections.unmodifiableList(ringList);
    }

    /**
     * Tests whether this MultiLinearRings geometry is a container for otherGeom's type.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *          which in this case is a LinearRing.
     */
    public boolean containerOf(Geometry otherGeom) {
        return otherGeom instanceof LinearRing;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "MultiLinearRings within " + bbox + " consists of " + ringList.size() + " Rings";
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
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		super.readData(in);
		List<LinearRing> lrlist = (List<LinearRing>) in.readObjectCollection();
		init(lrlist, false);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObjectCollection(ringList);
	}
}

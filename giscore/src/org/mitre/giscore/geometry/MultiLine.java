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
 * The MultiLine class represents an ordered list of Line objects for input and output in
 * GIS formats such as ESRI Shapefiles or Google Earth KML files.  In ESRI Shapefiles, this
 * object corresponds to a ShapeType of PolyLine.  This type of object does not exist as a
 * primitive in Google KML files.
 * <p/> 
 * Note if have lines of mixed dimensions then MultiLine container is downgraded to 2d.
 *
 * @author Paul Silvey
 */
public class MultiLine extends Geometry implements Iterable<Line> {
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MultiLine.class);

    private final List<Line> lineList, publicLineList;

    /**
     * This method returns an iterator for cycling through Lines in this MultiLine.
     * This class supports use of Java 'for each' syntax to cycle through the Lines.
     *
     * @return Iterator over Line objects.
     */
    public Iterator<Line> iterator() {
        return publicLineList.iterator();
    }

	/**
	 * This method returns the {@code Line}s in this {@code MultiLine}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code Line} objects.
	 */
	public Collection<Line> getLines() {
		return publicLineList;
	}

    /**
     * The Constructor takes a list of points and initializes a Geometry Object for this MultiPoint.
     *
     * @param lines List of Line objects to use for the parts of this MultiLine.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiLine(List<Line> lines) throws IllegalArgumentException {
        if (lines == null || lines.size() < 1)
            throw new IllegalArgumentException("MultiLine must contain at least 1 Line");
        // Make sure all the lines have the same number of dimensions (2D or 3D)
        is3D = lines.get(0).is3D();
        numParts = lines.size();
        numPoints = 0;
        boolean mixedDims = false;
        for (Line ln : lines) {
            if (is3D != ln.is3D()) {
                mixedDims = true;
                is3D = false;
            }
            numPoints += ln.getNumPoints();
        }
        if (mixedDims)
            log.info("MultiLine lines have mixed dimensionality: downgrading to 2d");
        bbox = null;
        if (is3D) {
            for (Line ln : lines) {
                Geodetic3DBounds bbox3 = (Geodetic3DBounds) ln.getBoundingBox();
                if (bbox == null) bbox = new Geodetic3DBounds(bbox3);
                else bbox.include(bbox3);
            }
        } else {
            for (Line ln : lines) {
                Geodetic2DBounds bbox2 = ln.getBoundingBox();
                if (bbox == null) bbox = new Geodetic2DBounds(bbox2);
                else bbox.include(bbox2);
            }
        }
        lineList = lines;
		publicLineList = Collections.unmodifiableList(lineList);
    }

    /**
     * Tests whether this MultiLine geometry is a container for otherGeom's type.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *          which in this case is a Line.
     */
    public boolean containerOf(Geometry otherGeom) {
        return otherGeom instanceof Line;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "MultiLine within " + bbox + " consists of " + lineList.size() + " Lines";
    }
    
    public void accept(StreamVisitorBase visitor) {
    	visitor.visit(this);
    }
}

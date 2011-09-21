/****************************************************************************************
 *  GeometryBag.java
 *
 *  Created: Feb 23, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;

/**
 * This is a simple typed collection of geometries. The methods aggregate answers
 * from the child geometries.
 *
 * @author DRAND
 */
public class GeometryBag extends Geometry implements Collection<Geometry> {
	private static final long serialVersionUID = 1L;

	private final List<Geometry> geometries = new ArrayList<Geometry>();

    private static final Geodetic2DBounds EMPTY_BOUNDS = new Geodetic2DBounds();

	/**
	 * Empty ctor for object io.
	 */
	public GeometryBag() {
		//
	}

	/**
	 * Ctor
	 * @param geometries List of Geometry objects to include in the bag,
	 * 		if null then an empty list is created.
	 */
	public GeometryBag(List<Geometry> geometries) {
		if (geometries != null && !geometries.isEmpty()) {
			this.geometries.addAll(geometries);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#containerOf(org.mitre.giscore.geometry.Geometry)
	 */
	@Override
	public boolean containerOf(Geometry otherGeom) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#computeBoundingBox()
	 */
	protected void computeBoundingBox() {
		Geodetic2DBounds rval = null;
		for(Geometry geo : geometries) {
			Geodetic2DBounds bounds = geo.getBoundingBox();
            if (bounds != null) {
                if (rval == null) {
                    rval = new Geodetic2DBounds(bounds);
                } else {
                    rval.include(bounds);
                }
            }
		}
		if (rval != null)
			bbox = rval;
		else
			bbox = EMPTY_BOUNDS;
	}

    /**
	 * This method returns the Geodetic2DBounds that encloses this Geometry
	 * object.
	 *
	 * @return Geodetic2DBounds object enclosing this Geometry object.
     *          <tt>null</tt> if no contained geometry has a bounding box.
	 */
    @CheckForNull
    @Override
	public Geodetic2DBounds getBoundingBox() {
		if (bbox == null) {
			computeBoundingBox();
		}
		return bbox == EMPTY_BOUNDS ? null : bbox;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getNumParts()
	 */
	@Override
	public int getNumParts() {
		int total = 0;
		for(Geometry geo : geometries) {
			total += geo.getNumParts();
		}
		return total;
	}

    /**
	 * @param i the desired part, 0 origin
	 * @return the referenced part, <tt>null</tt> if index out of range
	 */
	@Override
	@Nullable
	public Geometry getPart(int i) {
		return i >= 0 && i < geometries.size() ? geometries.get(i) : null;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getNumPoints()
	 */
	@Override
	public int getNumPoints() {
		int total = 0;
		for(Geometry geo : geometries) {
			total += geo.getNumPoints();
		}
		return total;
	}

	@Override
	@NonNull
	public List<Point> getPoints() {
		List<Point> rval = new ArrayList<Point>();
		for(Geometry geo : geometries) {
			rval.addAll(geo.getPoints());
		}
		return Collections.unmodifiableList(rval);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#is3D()
	 */
	@Override
	public boolean is3D() {
        // returns true if any child geometry is 3d
		for(Geometry geo : geometries) {
			if (geo.is3D) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#toString()
	 */
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.VisitableGeometry#accept(org.mitre.giscore.output.StreamVisitorBase)
	 */
	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Geometry geom) {
        // if non-null then need to recompute bbox
        if (geom != null) bbox = null;
		return geometries.add(geom);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends Geometry> c) {
        // if non-null then need to recompute bbox
        if (c != null) bbox = null;
		return geometries.addAll(c);
	}

	/**
	 * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
	 */
	public void clear() {
        bbox = null; // need to recompute bbox
		geometries.clear();
	}

	/**
	 * Returns <tt>true</tt> if this list contains the specified element.
	 */
	public boolean contains(Object o) {
		return geometries.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return geometries.containsAll(c);
	}

	/**
	 * Returns <tt>true</tt> if this collection contains no elements.
	 *
	 * @return <tt>true</tt> if this collection contains no elements
	 */
	public boolean isEmpty() {
		return geometries.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	@NonNull
	public Iterator<Geometry> iterator() {
		return geometries.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
        final boolean b = geometries.remove(o);
        if (b) bbox = null; // need to recompute bbox if list changed
        return b;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
        final boolean b = geometries.removeAll(c);
        if (b) bbox = null; // need to recompute bbox if list changed
        return b;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
        final boolean b = geometries.retainAll(c);
        if (b) bbox = null; // need to recompute bbox if list changed
        return b;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return geometries.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	@NonNull
	public Object[] toArray() {
		return geometries.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(T[])
	 */
	@NonNull    
	public <T> T[] toArray(T[] a) {
		return geometries.toArray(a);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException, IllegalAccessException {
		super.readData(in);
		List<Geometry> list = (List<Geometry>) in.readObjectCollection();
		if (!geometries.isEmpty()) geometries.clear();
		if (list != null && !list.isEmpty())
			geometries.addAll(list);
        bbox = null; // need to recompute bbox
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObjectCollection(geometries);
	}
}

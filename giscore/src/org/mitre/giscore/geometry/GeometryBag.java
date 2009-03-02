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
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * This is a simple typed collection of geometries. The methods aggregate answers
 * from the child geometries.
 * 
 * @author DRAND
 */
public class GeometryBag extends Geometry implements Collection<Geometry> {
	private static final long serialVersionUID = 1L;
	
	Collection<Geometry> geometries = new ArrayList<Geometry>();

	/**
	 * Ctor
	 * @param geometries
	 */
	public GeometryBag(List<Geometry> geometries) {
		if (geometries == null) {
			throw new IllegalArgumentException(
					"geometries should never be null");
		}
		this.geometries = geometries;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#containerOf(org.mitre.giscore.geometry.Geometry)
	 */
	@Override
	public boolean containerOf(Geometry otherGeom) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getBoundingBox()
	 */
	@Override
	public Geodetic2DBounds getBoundingBox() {	
		Geodetic2DBounds rval = null;
		for(Geometry geo : geometries) {
			Geodetic2DBounds bounds = geo.getBoundingBox();
			if (rval == null) {
				rval = new Geodetic2DBounds(bounds);
			} else {
				rval.include(bounds);
			}
		}
		return rval;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#getCenter()
	 */
	@Override
	public Geodetic2DPoint getCenter() {
		double lat = 0.0;
		double lon = 0.0;
		double count = 0;
		for(Geometry geo : geometries) {
			Geodetic2DPoint cen = geo.getCenter();
			lat += cen.getLatitude().inDegrees();
			lon += cen.getLongitude().inDegrees();
			count++;
		}
		// Compute Averages
		lat = lat / count;
		lon = lon / count;
		return new Geodetic2DPoint(new Longitude(lon, Angle.DEGREES),
				new Latitude(lat, Angle.DEGREES));
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

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#is3D()
	 */
	@Override
	public boolean is3D() {
		for(Geometry geo : geometries) {
			if (geo.is3D) return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.VisitableGeometry#accept(org.mitre.giscore.output.StreamVisitorBase)
	 */
	@Override
	public void accept(StreamVisitorBase visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	@Override
	public boolean add(Geometry e) {
		return geometries.add(e);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends Geometry> c) {
		return geometries.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	@Override
	public void clear() {
		geometries.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		return geometries.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return geometries.containsAll(c); 
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return geometries.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	@Override
	public Iterator<Geometry> iterator() {
		return geometries.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		return geometries.remove(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return geometries.removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		return geometries.retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	@Override
	public int size() {
		return geometries.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	@Override
	public Object[] toArray() {
		return geometries.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return geometries.toArray(a);
	}

}
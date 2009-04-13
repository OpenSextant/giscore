/****************************************************************************************
 *  FeatureStart.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.events;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;

/**
 * We've seen the start of a feature set. The start of the feature set has all
 * the available information about the feature. After the start of a feature,
 * there will be zero or more geometry objects seen.
 * 
 * @author DRAND
 */
public class Feature extends Common {
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mitre.giscore.events.BaseStart#readData(org.mitre.giscore.utils.
	 * SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		geometry = (Geometry) in.readObject();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.events.BaseStart#writeData(java.io.DataOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObject(geometry);
	}

	private Geometry geometry;

	/**
	 * Ctor
	 */
	public Feature() {
	}

	/**
	 * @return the geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry
	 *            the geometry to set
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return IKml.PLACEMARK;
	}

	public void accept(StreamVisitorBase visitor) {
		visitor.visit(this);
	}

	/**
	 * The approximately equals method checks all the fields for equality with
	 * the exception of the geometry.
	 * 
	 * @param other
	 */
	public boolean approximatelyEquals(Feature other) {
		EqualsBuilder eb = new EqualsBuilder();
		boolean fields = eb.append(description, other.description) //
				.append(name, other.name) //
				.append(schema, other.schema) //
				.append(styleUrl, other.styleUrl) // 
				.append(endTime, other.endTime) //
				.append(startTime, other.startTime).isEquals();

		if (!fields)
			return false;

		// Check extended data
		Set<SimpleField> maximalSet = new HashSet<SimpleField>();
		maximalSet.addAll(extendedData.keySet());
		maximalSet.addAll(other.extendedData.keySet());
		for (SimpleField fieldname : maximalSet) {
			Object val1 = extendedData.get(fieldname);
			Object val2 = other.extendedData.get(fieldname);
			if (val1 != null && val2 != null) {
				if (val1 instanceof Number && val2 instanceof Number) {
					double dv1 = ((Number) val1).doubleValue();
					double dv2 = ((Number) val2).doubleValue();
					return Math.abs(dv1 - dv2) < 1e-5;
				} else {
					return val1.equals(val2);
				}
			} else if (val1 == null || val2 == null) {
				return false;
			}
		}

		// Check geometry for equivalence
		if (geometry == null && other.geometry == null) {
			return true;
		} else if (geometry != null && other.geometry != null) {
			Geodetic2DBounds bb1 = geometry.getBoundingBox();
			Geodetic2DBounds bb2 = other.geometry.getBoundingBox();
			if (bb1 == null) {
				if (bb2 != null)
					return false;
			} else if (!bb1.equals(bb2, 1e-5))
				return false;
			if (geometry.getNumPoints() != other.geometry.getNumPoints())
				return false;
			return true;
		} else {
			return false;
		}

	}
}

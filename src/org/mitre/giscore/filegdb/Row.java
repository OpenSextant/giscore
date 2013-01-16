/****************************************************************************************
 *  Row.java
 *
 *  Created: Nov 9, 2012
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2012
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
package org.mitre.giscore.filegdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;

/**
 * The Row class encapsulates the FileGDB row class to efficiently provide the
 * functionality. It caches data to avoid crossing the JNI boundary more than
 * necessary, and uses simple Java constructs to move data across while 
 * reorganizing the data in a; more java native fashion locally.
 * 
 * @author DRAND
 */
public class Row extends GDB {
	private Map<String, Object> attrs = null;
	private Geometry geo = null;
	protected Table table;
	
	/**
	 * Ctor
	 * @param table
	 */
	protected Row(Table t) {
		this.table = t;
	}
	
	/**
	 * @return the OID for the row
	 */
	public native Integer getOID();
	
	/**
	 * @return the geometry associated with the row if the row is part of a
	 * feature class or <code>null</code> if no geometry is set on the row.
	 */
	public Geometry getGeometry() {
		if (geo == null) {
			Object shapeInfo[] = getGeo();
			// Decode
			Short type = (Short) shapeInfo[0];
			Boolean hasz = (Boolean) shapeInfo[1];
			int ptr = 4;
			int npoints = (Integer) shapeInfo[2];
			int nparts = (Integer) shapeInfo[3];
			int part_start;
			int point_start;
			int last;
			int incr = hasz ? 3 : 2;
			List<Point> larr;
			switch(type) {
			case 0: // Point
				geo = getPoint(ptr, shapeInfo, hasz);
				break;
			case 1: // Multipoint
				larr = new ArrayList<Point>();
				for(int i = 0; i < npoints; i++) {
					larr.add(getPoint(ptr, shapeInfo, hasz));
					ptr += incr;
				}
				geo = new MultiPoint(larr);
				break;
			case 2: // Polyline
				List<Line> lines = new ArrayList<Line>();
				part_start = ptr;
				ptr = part_start + nparts;
				last = (Integer) shapeInfo[part_start];
				larr = new ArrayList<Point>();
				for(int j = 0; j < nparts; j++) {
					int jpo = j + 1;
					int next = jpo == nparts ? npoints : (Integer) shapeInfo[part_start + j];
					for(int i = last; i < next; i++) {
						larr.add(getPoint(ptr, shapeInfo, hasz));
						ptr += incr;
					}
					Line l = new Line(larr);
					lines.add(l);
				}
				geo = new MultiLine(lines);
				break;
			case 3: // Polygon
				LinearRing outerRing = null;
				List<LinearRing> innerRings = new ArrayList<LinearRing>();
				part_start = ptr;
				ptr = part_start + nparts;
				last = (Integer) shapeInfo[part_start];
				larr = new ArrayList<Point>();
				for(int j = 0; j < nparts; j++) {
					int jpo = j + 1;
					int next = jpo == nparts ? npoints : (Integer) shapeInfo[part_start + j];
					for(int i = last; i < next; i++) {
						larr.add(getPoint(ptr, shapeInfo, hasz));
						ptr += incr;
					}
					if (! larr.get(0).equals(larr.get(larr.size() - 1))) {
						// Add the first point to the end as the ctor expects this
						larr.add(larr.get(0));
					}
					if (outerRing == null) {
						outerRing = new LinearRing(larr);
						if (! outerRing.clockwise()) {
							Collections.reverse(larr);
						}
					} else {
						innerRings.add(new LinearRing(larr));
					}
				}
				geo = new Polygon(outerRing, innerRings, true);
				break;
			case 4: // General Polyline, Polygon
				break;
			case 5: // Patches
				// Unsupported
			default:
				// Ignore
			}
		}
		return geo;
	}
	
	private Point getPoint(int ptr, Object[] shapeInfo, boolean hasz) {
		Double lon = (Double) shapeInfo[ptr++];
		Double lat = (Double) shapeInfo[ptr++];
		if (hasz) {
			Double elev = (Double) shapeInfo[ptr++];
			return new Point(lat, lon, elev);
		} else {
			return new Point(lat, lon, 0.0);
		}
		
	}

	/**
	 * @return the geometry associated with the row if the row is part of a
	 * feature class or <code>null</code> if no geometry is set on the row. The
	 * geometry information is directly derived from the shape buffer returned
	 * from the row. The information is serialized into a series of java 
	 * primitives. 
	 * 
	 * The first object returned is a Short representing the Shape Type. The
	 * rest of the objects are dependent on the type.
	 * 
	 * M is ignored for all types since giscore has no representation
	 */
	 
	//	  Line:
	//	  Integer: npoints	
	//	  point array
	//	  
	//	  PolyLine:
	//	  Integer: npoints
	//	  Integer: nparts
	//    part array
	//	  point array
	//	  
	//	  Polygon:
	//	  Integer: npoints
	//	  Integer: nparts
	//	  part array 
	//	  point array
	// 
	// part arrays are filled with ints
	//	   
	//	  Other shapes are not supported at this time
	 
	private native Object[] getGeo();
	
	public native void setGeometry(Object[] buffer);
	
	/**
	 * @return get the attributes as a map where the key is the field name
	 * and the value is the field value
	 */
	public Map<String, Object> getAttributes() {
		if (attrs == null) {
			Object[] data = getAttrArray();
			attrs = new HashMap<String, Object>(data.length / 2);
			for(int i = 0; i < data.length; i += 2) {
				String name = (String) data[i];
				Object datum = data[i+1];
				attrs.put(name, datum);
			}
		}
		return attrs;
	}
	
	/**
	 * Set new attribute data on the row
	 * @param data the new data
	 */
	public void setAttributes(Map<String, Object> data) {
		attrs = data; // Replace old data
		Object[] darray = new Object[attrs.size() * 2];
		int i = 0;
		for(String field : data.keySet()) {
			Object val = data.get(field);
			darray[i++] = field;
			if (val == GDB.NULL_OBJECT) {
				darray[i++] = null;
			} else {
				darray[i++] = val;
			}
		}
		setAttrArray(darray);
	}
	
	/**
	 * @return the attribute values as an alternating vector of field names
	 * and values.
	 */
	private native Object[] getAttrArray();
	
	/**
	 * Set the attribute values
	 * @param attrs the new values as an alternating vector of field names
	 * and values
	 */
	private native void setAttrArray(Object[] attrs);
}

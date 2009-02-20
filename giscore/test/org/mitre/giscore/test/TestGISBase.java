/****************************************************************************************
 *  TestXmlGdbBase.java
 *
 *  Created: Feb 10, 2009
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
package org.mitre.giscore.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.itf.geodesy.Geodetic2DPoint;

/**
 * The base class provides a series of features of various kinds, used to feed
 * the test cases.
 * 
 * @author DRAND
 */
public class TestGISBase {
	protected int count = 0;
	protected Random random = new Random(1000);
	
	/**
	 * Create a feature with a number of data elements in the extended data.
	 * This method will not use a schema.
	 * 
	 * @param geoclass
	 *            the class of the geometry objects to create
	 * @param names
	 *            the names of the attributes
	 * @param values
	 *            the values, the length must match the length of names
	 * @return the new instance
	 */
	protected Feature createFeature(Class<? extends Geometry> geoclass,
			String names[], Object values[]) {
		if (names == null) {
			throw new IllegalArgumentException("names should never be null");
		}
		if (values == null) {
			throw new IllegalArgumentException("values should never be null");
		}
		if (names.length != values.length) {
			throw new IllegalArgumentException("the count of names and values must match");
		}
		Feature f = createBasicFeature(geoclass);
		for(int i = 0; i < names.length; i++) {
			f.putData(names[i], values[i]);
		}
		return f;
	}
	
	/**
	 * Create a feature with a number of data elements in the extended data.
	 * This method will not use a schema.
	 * 
	 * @param geoclass
	 *            the class of the geometry objects to create
	 * @param schema
	 *            the schema
	 * @param valuemap
	 *            the valuemap, not <code>null</code>
	 * @return the new instance
	 */
	protected Feature createFeature(Class<? extends Geometry> geoclass,
			Schema schema, Map<String,Object> valuemap) {
		if (schema == null) {
			throw new IllegalArgumentException("schema should never be null");
		}
		if (valuemap == null) {
			throw new IllegalArgumentException("valuemap should never be null");
		}
		Feature f = createBasicFeature(geoclass);
		for(String key : schema.getKeys()) {
			if (valuemap.get(key) != null)
				f.putData(key, valuemap.get(key));
		}
		return f;
	}	
	
	/**
	 * Create a schema
	 * @param names the names for the fields
	 * @param types the types for the fields
	 * @return
	 */
	protected Schema createSchema(String names[], SimpleField.Type types[]) {
		Schema s = new Schema();
		byte arr[] = new byte[6];
		random.nextBytes(arr);
		String id = new String(arr);
		s.setId("id" + id);
		s.setName("name" + arr);
		for(int i = 0; i < names.length; i++) {
			SimpleField field = new SimpleField();
			field.setName(names[i]);
			field.setType(types[i]);
			s.put(names[i], field);
		}
		return s;
	}

	/**
	 * @param geoclass
	 * @return
	 */
	protected Feature createBasicFeature(Class<? extends Geometry> geoclass) {
		Feature f = new Feature();
		count++;
		f.setName("feature" + count);
		f.setDescription("feature description " + count);
		if (geoclass.isAssignableFrom(Point.class)) {
			Point p = new Point(new Geodetic2DPoint(random));
			f.setGeometry(p);
		} else if (geoclass.isAssignableFrom(Line.class)) {
			List<Point> pts = new ArrayList<Point>();
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			f.setGeometry(new Line(pts));
		} else if (geoclass.isAssignableFrom(LinearRing.class)) {
			List<Point> pts = new ArrayList<Point>();
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			f.setGeometry(new LinearRing(pts));
		} else if (geoclass.isAssignableFrom(Polygon.class)) {
			List<Point> pts = new ArrayList<Point>();
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			pts.add(new Point(new Geodetic2DPoint(random)));
			f.setGeometry(new Polygon(new LinearRing(pts)));
		}
		return f;
	}
	
	/**
	 * @param f
	 * @return
	 */
	protected int countFeatures(File f) {
		int count = 0;
		InputStream is = null;
		ObjectInputStream ois = null;
		try {
			is = new FileInputStream(f);
			ois = new ObjectInputStream(is);
			Object next = null;
			while ((next = ois.readObject()) != null) {
				Feature feature = (Feature) next;
				Assert.assertNotNull(feature);
				count++;
			}
		} catch (IOException e) {
			return count;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(ois);
			IOUtils.closeQuietly(is);
		}
		return count;
	}

}

/****************************************************************************************
 *  TestObjectPersistence.java
 *
 *  Created: Mar 24, 2009
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
package org.mitre.giscore.test.input;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import static org.junit.Assert.*;

/**
 * Test the geometry and the feature objects
 * 
 * @author DRAND
 * 
 */
public class TestObjectPersistence {
	@Test
	public void testSimpleGeometries() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2000);
		DataOutputStream dos = new DataOutputStream(bos);
		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(dos);

		Point p = new Point(.30, .42);
		soos.writeObject(p);

		List<Point> pts = new ArrayList<Point>();
		for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .01, i * .01));
		}
		Line l = new Line(pts);
		soos.writeObject(l);

		pts = new ArrayList<Point>();
		pts.add(new Point(.10, .10));
		pts.add(new Point(.10, -.10));
		pts.add(new Point(-.10, -.10));
		pts.add(new Point(-.10, .10));
		LinearRing r = new LinearRing(pts);
		soos.writeObject(r);

		soos.close();

		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		DataInputStream dis = new DataInputStream(bis);
		SimpleObjectInputStream sois = new SimpleObjectInputStream(dis);

		Geometry g = (Geometry) sois.readObject();
		assertEquals(p, g);

		g = (Geometry) sois.readObject();
		assertEquals(l.getNumPoints(), g.getNumPoints());
		assertEquals(l.getBoundingBox(), g.getBoundingBox());

		g = (Geometry) sois.readObject();
		assertEquals(r.getNumPoints(), g.getNumPoints());
		assertEquals(r.getBoundingBox(), g.getBoundingBox());

		sois.close();
	}

	@Test
	public void testMultiGeometries() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2000);
		DataOutputStream dos = new DataOutputStream(bos);
		SimpleObjectOutputStream soos = new SimpleObjectOutputStream(dos);

		List<Line> lines = new ArrayList<Line>();
		List<Point> pts = new ArrayList<Point>();
		for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .01, i * .01));
		}
		Line l = new Line(pts);
		lines.add(l);
		Geometry g = new MultiLine(lines );
		soos.writeObject(g);
		
		List<LinearRing> rings = new ArrayList<LinearRing>();
		pts = new ArrayList<Point>();
		pts.add(new Point(.10, .20));
		pts.add(new Point(.10, .10));
		pts.add(new Point(.20,.10));
		pts.add(new Point(.20, .20));
		LinearRing r1 = new LinearRing(pts);
		rings.add(r1);
		g = new MultiLinearRings(rings);
		soos.writeObject(g);
		
		pts = new ArrayList<Point>();
		pts.add(new Point(.10, .10));
		pts.add(new Point(.10, -.10));
		pts.add(new Point(-.10, -.10));
		pts.add(new Point(-.10, .10));
		LinearRing outer = new LinearRing(pts);
		pts = new ArrayList<Point>();
		pts.add(new Point(.05, .05));
		pts.add(new Point(.05, -.05));
		pts.add(new Point(-.05, -.05));
		pts.add(new Point(-.05, .05));
		List<LinearRing> innerRings = new ArrayList<LinearRing>();
		innerRings.add(new LinearRing(pts));
		Polygon p = new Polygon(outer, innerRings);
		soos.writeObject(p);
		
		g = new MultiPolygons(Collections.singletonList(p));
		soos.writeObject(g);
		
		soos.close();

		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		DataInputStream dis = new DataInputStream(bis);
		SimpleObjectInputStream sois = new SimpleObjectInputStream(dis);

		MultiLine ml = (MultiLine) sois.readObject();
		assertEquals(1, ml.getNumParts());
		assertEquals(10, ml.getNumPoints());
		
		MultiLinearRings mlr = (MultiLinearRings) sois.readObject();
		assertEquals(1, ml.getNumParts());
		
		Polygon p2 = (Polygon) sois.readObject();
		assertEquals(1, p2.getLinearRings().size());
		assertNotNull(p2.getOuterRing());

		MultiPolygons mp = (MultiPolygons) sois.readObject();
		assertEquals(2, mp.getNumParts());
		
		sois.close();
	}
}

/****************************************************************************************
 *  TestWKTOutputStream.java
 *
 *  Created: Jan 11, 2012
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
package org.mitre.giscore.test.output;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.IGISOutputStream;

public class TestWKTOutputStream {
	private static final File tempWKTDir = new File("testOutput/wkt");
	

	static {
		if (tempWKTDir.mkdirs())
			System.out.println("Created temp output directory: " + tempWKTDir);
	}

	@Test
	public void testSimpleOne() throws IOException {
		File temp = new File(tempWKTDir, "output" + System.currentTimeMillis() + ".wkt");
		OutputStream os = new FileOutputStream(temp);
		IGISOutputStream gisos = GISFactory.getOutputStream(DocumentType.WKT, os);
		
		Point pnt = new Point(42.0, 72.0);
		gisos.write(pnt);
		
		List<Point> pnts = new ArrayList<Point>();
		pnts.add(new Point(40.0, 70.0));
		pnts.add(new Point(41.0, 71.0));
		pnts.add(new Point(40.0, 72.0));
		Line line = new Line(pnts);
		gisos.write(line);
		
		pnts = new ArrayList<Point>();
		pnts.add(new Point(41.0, 69.0));
		pnts.add(new Point(41.4, 69.0));
		pnts.add(new Point(41.4, 69.4));
		pnts.add(new Point(41.0, 69.4));
		pnts.add(new Point(41.0, 69.0));
		LinearRing ring = new LinearRing(pnts);
		gisos.write(ring);
		
		pnts = new ArrayList<Point>();
		pnts.add(new Point(40.0, 70.0));
		pnts.add(new Point(41.0, 71.0));
		pnts.add(new Point(40.0, 72.0));
		MultiPoint mp = new MultiPoint(pnts);
		gisos.write(mp);
		
		List<Line> lines = new ArrayList<Line>();
		pnts = new ArrayList<Point>();
		pnts.add(new Point(40.0, 70.0));
		pnts.add(new Point(41.0, 71.0));
		Line line1 = new Line(pnts);
		pnts = new ArrayList<Point>();
		pnts.add(new Point(50.0, 80.0));
		pnts.add(new Point(51.0, 81.0));
		Line line2 = new Line(pnts);
		lines.add(line1);
		lines.add(line2);
		MultiLine ml = new MultiLine(lines);
		gisos.write(ml);
		
		List<LinearRing> rings = new ArrayList<LinearRing>();
		rings.add(ring);
		pnts = new ArrayList<Point>();
		pnts.add(new Point(42.0, 59.0));
		pnts.add(new Point(42.4, 59.0));
		pnts.add(new Point(42.4, 59.4));
		pnts.add(new Point(42.0, 59.4));
		pnts.add(new Point(42.0, 59.0));
		LinearRing ring2 = new LinearRing(pnts);
		rings.add(ring2);
		MultiLinearRings mlr = new MultiLinearRings(rings);
		gisos.write(mlr);
		
		
		Polygon p = new Polygon(ring);
		gisos.write(p);
		
		p = new Polygon(ring, rings);
		gisos.write(p);
		
		List<Polygon> polys = new ArrayList<Polygon>();
		polys.add(p);
		polys.add(new Polygon(ring));
		MultiPolygons mpoly = new MultiPolygons(polys);
		gisos.write(mpoly);
		
		List<Geometry> geos = new ArrayList<Geometry>();
		geos.add(pnt);
		geos.add(line);
		geos.add(ring);
		geos.add(p);
		GeometryBag bag = new GeometryBag(geos);
		gisos.write(bag);
		
		gisos.close();
	}

}

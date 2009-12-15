/****************************************************************************************
 *  TestShapefileBase.java
 *
 *  Created: Dec 10, 2009
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
package org.mitre.giscore.test.output;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.math.RandomUtils;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.shapefile.SingleShapefileInputHandler;
import org.mitre.giscore.output.shapefile.SingleShapefileOutputHandler;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * 
 * @author DRAND
 * 
 */
public abstract class TestShapefileBase extends TestGISBase {
	protected static final File shapeOutputDir = new File("testOutput/shptest");

	static {
		shapeOutputDir.mkdirs();
	}

	public TestShapefileBase() {
		super();
	}

	protected void writeShapefile(Schema schema, ObjectBuffer buffer,
			List<? extends Geometry> geometries, String file)
			throws IOException, URISyntaxException, XMLStreamException,
			ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		// now read shape file back in and test against what we wrote
		System.out.println("Test " + file);

		SingleShapefileOutputHandler soh = new SingleShapefileOutputHandler(
				schema, null, buffer, shapeOutputDir, file, null);
		soh.process();

		// System.out.println("Verify " + file);
		SingleShapefileInputHandler handler = new SingleShapefileInputHandler(
				shapeOutputDir, file);
		try {
			IGISObject ob = handler.read();
			assertTrue(ob instanceof Schema);
			int count = 0;
			while ((ob = handler.read()) != null) {
				assertTrue(ob instanceof Feature);
				if (geometries == null) {
					count++;
					continue;
				}
				Feature f = (Feature) ob;
				Geometry geom = f.getGeometry();
				Geometry expectedGeom = geometries.get(count++);
				// flatten geometry
				if (geom instanceof MultiLine) {
					MultiLine ml = (MultiLine) geom;
					if (ml.getNumParts() == 1)
						geom = ml.getPart(0);
				} else if (geom instanceof MultiPolygons) {
					MultiPolygons mp = (MultiPolygons) geom;
					if (mp.getNumParts() == 1) {
						Polygon poly = (Polygon) mp.getPart(0);
						// reader turns rings into MultiPolygons with single
						// Polygon having single outer ring
						if (expectedGeom instanceof LinearRing) {
							LinearRing ring = poly.getOuterRing();
							/*
							 * for (Point pt : ring) {
							 * System.out.format("%f %f%n",
							 * pt.getCenter().getLongitude().inDegrees(),
							 * pt.getCenter().getLatitude().inDegrees()); }
							 */
							if (!ring.clockwise())
								System.out
										.println("imported rings must be in clockwise point order");
							geom = ring;
						} else
							geom = poly;
					}
				}
				assertEquals(expectedGeom, geom);
			}
			if (geometries != null) {
				assertEquals(geometries.size(), count);
			}
			System.out.println("  count=" + count);
		} finally {
			handler.close();
		}
	}

	protected Point getRingPointZ(Point cp, int n, int total, double size,
			double min) {
		double lat = cp.getCenter().getLatitude().inDegrees();
		double lon = cp.getCenter().getLongitude().inDegrees();
		double theta = Math.toRadians(360.0 * n / total);
		double magnitude = min + RandomUtils.nextDouble() * size;
		double dy = magnitude * Math.sin(theta);
		double dx = magnitude * Math.cos(theta);
		Point pt = new Point(lat + dy, lon + dx, true);
		pt.setAltitudeMode(AltitudeModeEnumType.absolute);
		return pt;
	}

	protected Point getRandomPointZ() {
		double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
		double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
		double elt = RandomUtils.nextInt(200);
		return new Point(new Geodetic3DPoint(new Longitude(lon, Angle.DEGREES),
				new Latitude(lat, Angle.DEGREES), elt));
	}

}
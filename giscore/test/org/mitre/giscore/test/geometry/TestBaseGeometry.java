package org.mitre.giscore.test.geometry;

import org.junit.*;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.itf.geodesy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test base geometry classes with geometry creation and various
 * implementations of the Geometry base class.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 16, 2010 Time: 10:50:19 AM
 */
public class TestBaseGeometry extends TestGISBase {

    private static final double EPSILON = 1E-5;

	@Test
    public void testNullPointCompare() throws Exception {
		Point pt = getRandomPoint();
		Point other = null;
		assertFalse(pt.equals(other));
	}

	@Test
    public void testNullCircleCompare() throws Exception {
		Circle circle = new Circle(random3dGeoPoint(), 1000.0);
		Circle other = null;
		assertFalse(circle.equals(other));
	}

	@Test
    public void testNullLineCompare() throws Exception {
		Point cp = getRandomPoint();
		List<Point> pts = new ArrayList<Point>();
		for (int i=0; i < 5; i++) {
			Point pt = getRingPoint(cp, i, 5, .3, .4);
            assertEquals(1, pt.getNumParts());
            assertEquals(1, pt.getNumPoints());
            assertEquals(pt.asGeodetic2DPoint(), pt.getCenter());
			pts.add(pt);
        }
		Line line = new Line(pts);
		Line other = null;
		assertFalse(line.equals(other));
	}

    @Test
	public void testPointLineCreation() throws Exception {
		Point cp = getRandomPoint();
		List<Point> pts = new ArrayList<Point>();
		for (int i=0; i < 5; i++) {
			Point pt = getRingPoint(cp, i, 5, .3, .4);
            assertEquals(1, pt.getNumParts());
            assertEquals(1, pt.getNumPoints());
            assertEquals(pt.asGeodetic2DPoint(), pt.getCenter());
			pts.add(pt);
        }

        // construct MultiPoint
        MultiPoint mp = new MultiPoint(pts);
        assertEquals(pts.size(), mp.getNumParts());
        assertEquals(pts.size(), mp.getNumPoints());
        assertFalse(mp.is3D());

        // construct Line
        Line line = new Line(new ArrayList<Point>(pts));
        assertEquals(1, line.getNumParts());
        assertEquals(pts.size(), line.getNumPoints());
        assertFalse(line.is3D());

        Iterator<Point> it1 = line.iterator();
        Iterator<Point> it2 = mp.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            assertEquals(it1.next(), it2.next());
        }
        assertFalse(it1.hasNext());
        assertFalse(it2.hasNext());

        List<Point> linePts = line.getPoints();
        List<Point> multiPts = mp.getPoints();
        assertEquals(linePts.size(), multiPts.size());
        for (int i=0; i < linePts.size(); i++) {
           assertEquals(linePts.get(i), multiPts.get(i));
        }

        assertEquals(mp.getCenter(), line.getCenter());
    }

	/**
	 * Create mixed dimension (2d + 3d pts) MultiPoint which downgrades to 2d
	 */
	@Test
	public void testMixedMultiPoint() {
		Point pt2d = getRandomPoint();
		Point pt3d = new Point(random3dGeoPoint());
		List<Point> pts = new ArrayList<Point>();
		pts.add(pt2d);
		pts.add(pt3d);

        MultiPoint mp = new MultiPoint(pts);
        assertEquals(pts.size(), mp.getNumParts());
        assertEquals(pts.size(), mp.getNumPoints());
        assertFalse(mp.is3D());
	}

    @Test
    public void testCircle() throws Exception {
        Point pt = getRandomPoint();
		Circle c = new Circle(pt.getCenter(), 10000.0);
        assertEquals(pt.asGeodetic2DPoint(), c.getCenter());
        assertFalse(c.is3D());
        Geodetic2DBounds bounds = c.getBoundingBox();
        Assert.assertNotNull(bounds);

        pt = new Point(random3dGeoPoint());
		c = new Circle(pt.getCenter(), 10000.0);
        assertEquals(pt.asGeodetic2DPoint(), c.getCenter());
        assertTrue(c.is3D());
        bounds = c.getBoundingBox();
        assertTrue(bounds instanceof Geodetic3DBounds);
        assertTrue(bounds.contains(pt.asGeodetic2DPoint()));
    }

    @Test
    public void testRing() throws Exception {
		List<Point> pts = new ArrayList<Point>();
		pts.add(new Point(0.0, 0.0));
		pts.add(new Point(0.0, 1.0));
		pts.add(new Point(1.0, 2.0));
		pts.add(new Point(2.0, 1.0));
		pts.add(new Point(1.0, 0.0));
		pts.add(new Point(0.0, 0.0));
		LinearRing geo = new LinearRing(pts, true);
        assertEquals(1, geo.getNumParts());
        assertEquals(pts.size(), geo.getNumPoints());
        assertFalse(geo.is3D());
        // center: (1° 0' 0" E, 1° 0' 0" N)
        Geodetic2DPoint center = geo.getCenter();
        assertEquals(1.0, center.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.0, center.getLongitudeAsDegrees(), EPSILON);

        geo = new LinearRing(geo.getBoundingBox());
        assertEquals(1, geo.getNumParts());
        assertEquals(5, geo.getNumPoints());
        // center: (1° 0' 0" E, 1° 0' 0" N)
        center = geo.getCenter();
        assertEquals(1.0, center.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.0, center.getLongitudeAsDegrees(), EPSILON);
    }

    @Test
    public void testPolygon() throws Exception {
		List<Point> pts = new ArrayList<Point>();
        // Outer LinearRing in Polygon must be in clockwise point order
		pts.add(new Point(0.0, 0.0));
        pts.add(new Point(1.0, 0.0));
        pts.add(new Point(2.0, 1.0));
        pts.add(new Point(1.0, 2.0));
        pts.add(new Point(0.0, 1.0));
        pts.add(new Point(0.0, 0.0));
        final LinearRing ring = new LinearRing(pts, true);
        Polygon geo = new Polygon(ring, true);
        assertEquals(1, geo.getNumParts());
        assertEquals(pts.size(), geo.getNumPoints());
        assertFalse(geo.is3D());
        Geodetic2DPoint cp = geo.getCenter();
        // center: (1° 0' 0" E, 1° 0' 0" N)
        assertEquals(1.0, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.0, cp.getLongitudeAsDegrees(), EPSILON);

        // create new polygon with outer and inner ring
        pts = new ArrayList<Point>();
		pts.add(new Point(0.2, 0.2));
		pts.add(new Point(0.2, 0.8));
		pts.add(new Point(0.8, 0.8));
		pts.add(new Point(0.8, 0.2));
		pts.add(new Point(0.2, 0.2));
		LinearRing ir = new LinearRing(pts);
		geo = new Polygon(ring, Collections.singletonList(ir));
        assertEquals(2, geo.getNumParts());
        assertEquals(ring.getNumPoints() + ir.getNumPoints(), geo.getNumPoints());
        cp = geo.getCenter();
        // center: (1° 0' 0" E, 1° 0' 0" N)
        assertEquals(1.0, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.0, cp.getLongitudeAsDegrees(), EPSILON);
    }

    /*
    @Test
    public void testModPoint() {
        double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
		double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
        Geodetic2DPoint pt = new Geodetic2DPoint(new Longitude(lon, Angle.DEGREES),
                new Latitude(lat, Angle.DEGREES));
        Point geo = new Point(pt);
        Geodetic2DPoint cp = geo.getCenter();
        assertEquals(lat, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(lon, cp.getLongitudeAsDegrees(), EPSILON);

        // changing Geodetic2DPoint after constructing Point should not change internal state of Point
        // but Point is doing copy-by-reference so side effects such as this do exist.
        pt.setLongitude(new Longitude(lon + 1, Angle.DEGREES));
        pt.setLatitude(new Latitude(lat + 1, Angle.DEGREES));

        assertEquals(lat, cp.getLatitudeAsDegrees(), EPSILON); // fails
        assertEquals(lon, cp.getLongitudeAsDegrees(), EPSILON); // fails

        // likewise if we add/remove points after bounding box is calculated then line/ring state
        // will not be consistent.
    }
    */

    @Test
    public void testGeometryBag() throws Exception {
		List<Geometry> geometries = new ArrayList<Geometry>();
		geometries.add(new Point(2.0, 2.0));
		List<Point> points = new ArrayList<Point>();
		points.add(new Point(0.0, 0.0));
		points.add(new Point(0.0, 1.0));
		points.add(new Point(1.0, 0.0));
		geometries.add(new Line(points));
		GeometryBag geo = new GeometryBag(geometries);
        assertEquals(2, geo.size()); // number of geometries
        assertEquals(2, geo.getNumParts()); // aggregate parts of all geometries
        assertEquals(1 + points.size(), geo.getNumPoints());
        assertFalse(geo.is3D());

        // center = (1° 15' 0" E, 1° 15' 0" N)
        final Geodetic2DPoint cp = geo.getCenter();
        assertEquals(1.25, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.25, cp.getLongitudeAsDegrees(), EPSILON);
    }

    @Test
    public void testMultiLine() throws Exception {
        List<Line> lines = new ArrayList<Line>();
        List<Point> pts = new ArrayList<Point>();
        for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .01 + 0.1, i * .01 + 0.1, true)); // sets 0.0 elevation
		}
        Line line = new Line(pts);
        line.setTessellate(false);
        line.setAltitudeMode(AltitudeModeEnumType.clampToGround);
        lines.add(line);
        pts = new ArrayList<Point>();
		for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .02 + 0.2, i * .02 + 0.2, 100));
		}
        line = new Line(pts);
        line.setTessellate(true);
        lines.add(line);
		Geometry geo = new MultiLine(lines);
        assertEquals(2, geo.getNumParts());
        assertEquals(20, geo.getNumPoints());
        assertTrue(geo.is3D());
        Geodetic2DBounds bounds = geo.getBoundingBox();
        assertTrue(bounds instanceof Geodetic3DBounds);
        // bounding box of MultiLine must contain bounding box for each of its lines
        assertTrue(bounds.contains(line.getBoundingBox()));

        // (0° 14' 24" E, 0° 14' 24" N) @ 0m
        final Geodetic2DPoint cp = geo.getCenter();
        System.out.println(cp);
        assertEquals(0.24, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(0.24, cp.getLongitudeAsDegrees(), EPSILON);

        List<Point> points = geo.getPoints(); // all 20 points
        assertEquals(20, points.size());
        for (int i=0; i < 10; i++) {
            assertEquals(pts.get(i), points.get(i+10));
        }

        List<Geometry> geometries = new ArrayList<Geometry>();
        geometries.add(pts.get(0));
        geometries.add(line);
		geo = new GeometryBag(geometries);
        assertEquals(2, geo.getNumParts());
        assertTrue(geo.is3D());
    }

	/**
	 * Construct mixed dimension MultiLine (2d + 3d Lines) which downgrades to 2d.
	 */
	@Test
    public void testMixedMultiLine() {
        List<Line> lines = new ArrayList<Line>();
        List<Point> pts = new ArrayList<Point>();
        for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .01 + 0.1, i * .01 + 0.1, 500));
		}
        Line line = new Line(pts);
		line.setAltitudeMode(AltitudeModeEnumType.absolute);
		line.setTessellate(true);
		assertTrue(line.is3D());
		lines.add(line);

		pts = new ArrayList<Point>();
        for (int i = 0; i < 10; i++) {
			pts.add(new Point(i * .03 + 0.3, i * .03 + 0.3)); // 2-d points
		}
		line = new Line(pts);
        line.setTessellate(false);
        lines.add(line);
		MultiLine geo = new MultiLine(lines);
        assertEquals(2, geo.getNumParts());
        assertEquals(20, geo.getNumPoints());
        assertFalse(geo.is3D());
	}

    @Test
    public void testModel() throws Exception {
         Model model = new Model();
         final Geodetic2DPoint pt = random3dGeoPoint();
         model.setLocation(pt);
         model.setAltitudeMode(AltitudeModeEnumType.absolute);
         assertEquals(pt, model.getCenter());
         assertEquals(1, model.getNumParts());
         assertEquals(1, model.getNumPoints());
         assertTrue(model.is3D());
         Geodetic2DBounds bounds = model.getBoundingBox();
         assertTrue(bounds.contains(pt));
         assertEquals(pt, bounds.getCenter());
     }

    @Test
    public void testClippedAtDateLine() throws Exception {
        // create outline of Fiji islands which wrap international date line
		List<Point> pts = new ArrayList<Point>();
        final Point firstPt = new Point(-16.68226928264316, 179.900033693558);
        pts.add(firstPt);
        pts.add(new Point(-16.68226928264316, -180));
		pts.add(new Point(-17.01144405215603, -180));
		pts.add(new Point(-17.01144405215603, 179.900033693558));
		pts.add(firstPt);
        Line line = new Line(pts);
        assertTrue(line.clippedAtDateLine());

        LinearRing ring = new LinearRing(pts, true);
        assertTrue(ring.clippedAtDateLine());
     }
}
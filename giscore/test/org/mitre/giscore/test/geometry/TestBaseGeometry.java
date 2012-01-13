package org.mitre.giscore.test.geometry;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.*;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.itf.geodesy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

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
    public void testNullPointCompare() {
		Point pt = getRandomPoint();
		Point other = null;
		assertFalse(pt.equals(other));
	}

	@Test
    public void testNullCircleCompare() {
		Circle circle = new Circle(random3dGeoPoint(), 1000.0);
		Circle other = null;
		assertFalse(circle.equals(other));
	}

	@Test
    public void testNullLineCompare() {
		List<Point> pts = createPoints();
		Line line = new Line(pts);
		Line other = null;
		assertFalse(line.equals(other));
	}

	private static List<Point> createPoints() {
		Point cp = getRandomPoint();
		List<Point> pts = new ArrayList<Point>(5);
		for (int i=0; i < 5; i++) {
			Point pt = getRingPoint(cp, i, 5, .3, .4);
            assertEquals(1, pt.getNumParts());
            assertEquals(1, pt.getNumPoints());
            assertEquals(pt.asGeodetic2DPoint(), pt.getCenter());
			pts.add(pt);
        }
		return pts;
	}

	@Test
	public void testPointLineCreation() {
		List<Point> pts = createPoints();

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

	@Test
	public void testLineBBox() {
		double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
		double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
        Geodetic2DPoint pt1 = new Geodetic2DPoint(new Longitude(lon, Angle.DEGREES),
                new Latitude(lat, Angle.DEGREES));
		Geodetic2DBounds bbox = new Geodetic2DBounds(pt1);
		try {
			// single point bbox - line requires at least 2 points
			new Line(bbox);
			fail("Expected to throw Exception");
		} catch (IllegalArgumentException iae) {
			// expected
		}
		try {
			new LinearRing(bbox); // ring requires at least 4 points
			fail("Expected to throw Exception");
		} catch (IllegalArgumentException iae) {
			// expected
		}

		Geodetic2DPoint pt2 = new Geodetic2DPoint(new Longitude(lon + 10, Angle.DEGREES),
				pt1.getLatitude());
		bbox = new Geodetic2DBounds(pt1, pt2);
		Line line = new Line(bbox);
		assertEquals(2, line.getNumPoints());
		try {
			// 2-point line bbox - ring requires at least 4 points
			new LinearRing(bbox);
			fail("Expected to throw Exception");
		} catch (IllegalArgumentException iae) {
			// expected
		}

		Geodetic2DPoint pt3 = new Geodetic2DPoint(pt1.getLongitude(),
                new Latitude(lat + 10, Angle.DEGREES));
		bbox = new Geodetic2DBounds(pt1, pt3);
		line = new Line(bbox);
		assertEquals(2, line.getNumPoints());
		try {
			// 2-point line bbox - ring requires at least 4 points
			new LinearRing(bbox);
			fail("Expected to throw Exception");
		} catch (IllegalArgumentException iae) {
			// expected
		}

		Geodetic2DPoint pt4 = new Geodetic2DPoint(pt2.getLongitude(),
				pt3.getLatitude());
		line = new Line(new Geodetic2DBounds(pt1, pt4));
		assertEquals(5, line.getNumPoints());
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
    public void testCircle() {
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
    public void testRing() {
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
    public void testPolygon() {
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
        assertNotNull(geo.getPart(0));
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
    public void testGeometryBag() {
		List<Geometry> geometries = new ArrayList<Geometry>();
		geometries.add(new Point(2.0, 2.0));
		List<Point> points = new ArrayList<Point>();
		points.add(new Point(0.0, 0.0));
		points.add(new Point(0.0, 1.0));
		points.add(new Point(1.0, 0.0));
		final Line line = new Line(points);
		geometries.add(line);
		GeometryBag geo = new GeometryBag(geometries);
        assertEquals(2, geo.size()); // number of geometries
        assertEquals(2, geo.getNumParts()); // aggregate parts of all geometries
        assertNotNull(geo.getPart(0));
        assertNull(geo.getPart(2));
        assertEquals(1 + points.size(), geo.getNumPoints());
        assertFalse(geo.is3D());
		assertTrue(geo.contains(line));
		assertFalse(geo.isEmpty());

        // center = (1° 15' 0" E, 1° 15' 0" N)
        final Geodetic2DPoint cp = geo.getCenter();
        assertEquals(1.0, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(1.0, cp.getLongitudeAsDegrees(), EPSILON);

		geo.clear();
		assertEquals(0, geo.size());
        assertEquals(0, geo.getNumParts());
		assertTrue(geo.isEmpty());
		assertFalse(geo.is3D());

		geometries.clear();
		geometries.add(new Point(30.0, 40.0, 400));
		geo = new GeometryBag(geometries);
		assertTrue(geo.is3D());
    }

    @Test
    public void testMultiLine() {
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
        System.out.println("multiline center=" + cp);
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
	public void testModel() {
		Model model = new Model();
		final Geodetic2DPoint pt = random3dGeoPoint();
		model.setLocation(pt);
		model.setAltitudeMode(AltitudeModeEnumType.absolute);
		assertEquals(pt, model.getCenter());
		assertEquals(1, model.getNumParts());
		assertEquals(1, model.getNumPoints());
		assertTrue(model.is3D());
		Geodetic2DBounds bounds = model.getBoundingBox();
		assertNotNull(bounds);
		assertTrue(bounds.contains(pt));
		assertEquals(pt, bounds.getCenter());
	}

    @Test
    public void testClippedAtDateLine() {
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

		// (179° 57' 0" E, 16° 50' 49" S)
		Geodetic2DPoint cp = line.getCenter();
		// System.out.println("Fctr=" + cp.getLatitudeAsDegrees() + " " + cp.getLongitudeAsDegrees());
		assertEquals(-16.846856667399592, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(179.950016846779, cp.getLongitudeAsDegrees(), EPSILON);

        LinearRing ring = new LinearRing(pts, true);
        assertTrue(ring.clippedAtDateLine());
		assertEquals(cp, ring.getCenter());
     }

	@Test
    public void testWrapDateLine() {
        // create outline of Fiji islands which wrap international date line
		List<Point> pts = new ArrayList<Point>();
		final Point firstPt = new Point(-16.68226928264316, 179.900033693558);
        pts.add(firstPt);
        pts.add(new Point(-16.68226928264316, -179.65));
		pts.add(new Point(-17.01144405215603, -180));
		pts.add(new Point(-17.01144405215603, 179.900033693558));
		pts.add(firstPt);
        Line line = new Line(pts);
        assertTrue(line.clippedAtDateLine());

		// (179° 52' 30" W, 16° 50' 49" S)
		Geodetic2DPoint cp = line.getCenter();
		// System.out.println("Fctr=" + cp + " " + cp.getLatitudeAsDegrees() + " " + cp.getLongitudeAsDegrees());
		assertEquals(-16.846856667399592, cp.getLatitudeAsDegrees(), EPSILON);
        assertEquals(-179.874983153221, cp.getLongitudeAsDegrees(), EPSILON);

        LinearRing ring = new LinearRing(pts, true);
        assertTrue(ring.clippedAtDateLine());
		assertEquals(cp, ring.getCenter());
     }
}

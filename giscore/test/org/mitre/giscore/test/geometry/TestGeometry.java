package org.mitre.giscore.test.geometry;

import org.junit.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.itf.geodesy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * Test base geometry classes.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 16, 2010 Time: 10:50:19 AM
 */
public class TestGeometry extends TestGISBase {

    private static final double EPSILON = 1E-5;

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

        // construct Line
        Line line = new Line(new ArrayList<Point>(pts));
        assertEquals(1, line.getNumParts());
        assertEquals(pts.size(), line.getNumPoints());

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
    }

    @Test
    public void testCircle() throws Exception {
        Point pt = getRandomPoint();
		Circle c = new Circle(pt.getCenter(), 10000.0);
        assertEquals(pt.asGeodetic2DPoint(), c.getCenter());
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
        // center: (1° 0' 0" E, 1° 0' 0" N)
        Geodetic2DPoint center = geo.getCenter();
        assertEquals(1.0, center.getLatitude().inDegrees(), EPSILON);
        assertEquals(1.0, center.getLongitude().inDegrees(), EPSILON);

        geo = new LinearRing(geo.getBoundingBox());
        assertEquals(1, geo.getNumParts());
        assertEquals(5, geo.getNumPoints());
        // center: (1° 0' 0" E, 1° 0' 0" N)
        center = geo.getCenter();
        assertEquals(1.0, center.getLatitude().inDegrees(), EPSILON);
        assertEquals(1.0, center.getLongitude().inDegrees(), EPSILON);
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
        Geodetic2DPoint cp = geo.getCenter();
        // center: (1° 0' 0" E, 1° 0' 0" N)
        assertEquals(1.0, cp.getLatitude().inDegrees(), EPSILON);
        assertEquals(1.0, cp.getLongitude().inDegrees(), EPSILON);

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
        assertEquals(1.0, cp.getLatitude().inDegrees(), EPSILON);
        assertEquals(1.0, cp.getLongitude().inDegrees(), EPSILON);
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
        assertEquals(lat, cp.getLatitude().inDegrees(), EPSILON);
        assertEquals(lon, cp.getLongitude().inDegrees(), EPSILON);
        // changing Geodetic2DPoint after constructing Point doesn't change internal state of Point
        // but Point is doing copy-by-reference.
        pt.setLongitude(new Longitude(lon + 1, Angle.DEGREES));
        pt.setLatitude(new Latitude(lat + 1, Angle.DEGREES));
        assertEquals(lat, cp.getLatitude().inDegrees(), EPSILON);
        assertEquals(lon, cp.getLongitude().inDegrees(), EPSILON);
    }
    */

    @Test
    public void testClippedAtDateLine() throws Exception {
        // create outline of Fiji which wraps international date line
		List<Point> pts = new ArrayList<Point>();
        final Point firstPt = new Point(-16.68226928264316, 179.900033693558);
        pts.add(firstPt);
        pts.add(new Point(-16.68226928264316, -180));
		pts.add(new Point(-17.01144405215603, -180));
		pts.add(new Point(-17.01144405215603, 179.900033693558));
		pts.add(firstPt);
        Line line = new Line(pts);
        Assert.assertTrue(line.clippedAtDateLine());

        LinearRing ring = new LinearRing(pts, true);
        Assert.assertTrue(ring.clippedAtDateLine());
     }

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
        assertEquals(2, geo.getNumParts());
        assertEquals(1 + points.size(), geo.getNumPoints());
    }
}

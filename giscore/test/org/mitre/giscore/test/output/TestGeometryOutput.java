package org.mitre.giscore.test.output;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.test.TestGISBase;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import static junit.framework.Assert.assertEquals;

/**
 * Test reading/writing core geometry classes.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Oct 5, 2009 3:12:57 PM
 */
public class TestGeometryOutput extends TestGISBase {

	@Test
	public void testPointCreation() throws Exception {
		Point cp = getRandomPoint();
		List<Point> pts = new ArrayList<Point>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SimpleObjectOutputStream os = new SimpleObjectOutputStream(bos);
		for (int i=0; i < 5; i++) {
			Point pt = getRingPoint(cp, i, 5, .3, .4);
			pts.add(pt);
			pt.writeData(os);
		}
		Point pt2 = new Point();
		SimpleObjectInputStream is = new SimpleObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		for (Point pt : pts) {
			pt2.readData(is);
			assertEquals(pt, pt2);
		}
	}

	@Test
	public void testRingCreation() throws Exception {
		Point cp = getRandomPoint();
		List<Point> pts = new ArrayList<Point>();
		pts.add(getRingPoint(cp, 4, 5, .3, .4));
		pts.add(getRingPoint(cp, 3, 5, .3, .4));
		pts.add(getRingPoint(cp, 2, 5, .3, .4));
		pts.add(getRingPoint(cp, 1, 5, .3, .4));
		pts.add(getRingPoint(cp, 0, 5, .3, .4));
		pts.add(pts.get(0)); // ring should start and end with the same point
		LinearRing ring = new LinearRing(pts, true);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SimpleObjectOutputStream os = new SimpleObjectOutputStream(bos);
		ring.writeData(os);
		LinearRing ring2 = new LinearRing();
		SimpleObjectInputStream is = new SimpleObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		ring2.readData(is);
		assertEquals(ring, ring2);
	}

    @Test
	public void testGeometryBagCreation() throws Exception {
        List<Feature> features = getMultiGeometries();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SimpleObjectOutputStream os = new SimpleObjectOutputStream(bos);
        GeometryBag geo = new GeometryBag();
        for (Feature f : features) {
            final Geometry geom = f.getGeometry();
            if (geom != null) geo.add(geom);
        }
        geo.writeData(os);

        SimpleObjectInputStream is = new SimpleObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        GeometryBag geo2 = new GeometryBag();
        geo2.readData(is);
        assertEquals(geo, geo2);
    }
}

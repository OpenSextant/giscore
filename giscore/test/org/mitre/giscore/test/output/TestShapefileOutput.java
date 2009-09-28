/****************************************************************************************
 *  TestShapefileOutput.java
 *
 *  Created: Jul 23, 2009
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

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.shapefile.SingleShapefileOutputHandler;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.DocumentType;

public class TestShapefileOutput {

    private static final File shapeOutputDir = new File("testOutput/shptest");

    static {
        shapeOutputDir.mkdirs();
    }

    @Test public void testWriteReferencePointOutput() throws Exception {
		FileOutputStream zip = new FileOutputStream(new File(shapeOutputDir, "reference.zip"));
		ZipOutputStream zos = new ZipOutputStream(zip);
        File outDir = new File("testOutput/shptest/buf");
        outDir.mkdirs();
        IGISOutputStream shpos = GISFactory.getOutputStream(DocumentType.Shapefile, zos, outDir);
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		DocumentStart ds = new DocumentStart(DocumentType.Shapefile);
		shpos.write(ds);
		ContainerStart cs = new ContainerStart("Folder");
		cs.setName("aaa");
		shpos.write(cs);
		shpos.write(schema);
		for(int i = 0; i < 5; i++) {
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.setSchema(schema.getId());
			double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
			double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
			Point point = new Point(lat, lon);
			f.setGeometry(point);
			shpos.write(f);
		}
		shpos.close();
		zos.flush();
		zos.close();		
	}

	@Test public void testPointOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.setSchema(schema.getId());
			Point point = getRandomPoint();
			f.setGeometry(point);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "points", null);
		soh.process();
	}
	
	@Test public void testMultiPointOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multipoint");
		f.setSchema(schema.getId());
		List<Point> pts = new ArrayList<Point>();
		for(int i = 0; i < 5; i++) {
			Point point = getRandomPoint();
			pts.add(point);
		}
		f.setGeometry(new MultiPoint(pts));
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multipoint", null);
		soh.process();
	}
	
	@Test public void testLineOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.putData(date, new Date());
			f.setSchema(schema.getId());
			List<Point> pts = new ArrayList<Point>();
			pts.add(getRandomPoint());
			pts.add(getRandomPoint());
			Line line = new Line(pts);
			f.setGeometry(line);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "lines", null);
		soh.process();
	}
	
	@Test public void testMultiLineOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multiline");
		f.putData(date, new Date());
		f.setSchema(schema.getId());
		List<Line> lines = new ArrayList<Line>();
		for(int i = 0; i < 5; i++) {
			List<Point> pts = new ArrayList<Point>();
			pts.add(getRandomPoint());
			pts.add(getRandomPoint());
			Line line = new Line(pts);
			lines.add(line);
		}
		MultiLine mline = new MultiLine(lines);
		f.setGeometry(mline);
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multilines", null);
		soh.process();
	}	
	
	@Test public void testRingOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint();
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.putData(date, new Date());
			f.setSchema(schema.getId());
			List<Point> pts = new ArrayList<Point>();
			pts.add(getRingPoint(cp, 0, 5, .3, .4));
			pts.add(getRingPoint(cp, 1, 5, .3, .4));
			pts.add(getRingPoint(cp, 2, 5, .3, .4));
			pts.add(getRingPoint(cp, 3, 5, .3, .4));
			pts.add(getRingPoint(cp, 4, 5, .3, .4));
            pts.add(pts.get(0)); // should start and end with the same point
			LinearRing ring = new LinearRing(pts, true);
			f.setGeometry(ring);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "rings", null);
		soh.process();
	}
	
	@Test public void testRingZOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint();
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.putData(date, new Date());
			f.setSchema(schema.getId());
			List<Point> pts = new ArrayList<Point>();
			pts.add(getRingPointZ(cp, 0, 5, .3, .4));
			pts.add(getRingPointZ(cp, 1, 5, .3, .4));
			pts.add(getRingPointZ(cp, 2, 5, .3, .4));
			pts.add(getRingPointZ(cp, 3, 5, .3, .4));
			pts.add(getRingPointZ(cp, 4, 5, .3, .4));
            pts.add(pts.get(0)); // should start and end with the same point
			// First (outer) ring should be in clockwise point order
			LinearRing ring = new LinearRing(pts, true);
			f.setGeometry(ring);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "ringz", null);
		soh.process();
	}
	
	@Test public void testPolyOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.putData(date, new Date());
			f.setSchema(schema.getId());
			List<Point> pts = new ArrayList<Point>();
			for(int k = 0; k < 5; k++) {
				pts.add(getRingPoint(cp, k, 5, 1.0, 2.0));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			// First (outer) ring should be in clockwise point order
			LinearRing outerRing = new LinearRing(pts, true);
			List<LinearRing> innerRings = new ArrayList<LinearRing>();
			for(int j = 0; j < 4; j++) {
				pts = new ArrayList<Point>();
				Point ircp = getRingPoint(cp, j, 4, .5, 1.0);
				for(int k = 0; k < 5; k++) {
					pts.add(getRingPoint(ircp, k, 5, .24, .2));
				}
                pts.add(pts.get(0)); // should start and end with the same point
				innerRings.add(new LinearRing(pts, true));
			}
			Polygon p = new Polygon(outerRing, innerRings, true);
			f.setGeometry(p);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "polys", null);
		soh.process();
	}
	
	@Test public void testPolyZOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			Feature f = new Feature();
			f.putData(id, "id polyz " + i);
			f.putData(date, new Date());
			f.setSchema(schema.getId());
			List<Point> pts = new ArrayList<Point>();
			for(int k = 0; k < 5; k++) {
				pts.add(getRingPointZ(cp, k, 5, 1.0, 2.0));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			// First (outer) ring should be in clockwise point order
			LinearRing outerRing = new LinearRing(pts);
			List<LinearRing> innerRings = new ArrayList<LinearRing>();
			for(int j = 0; j < 4; j++) {
				pts = new ArrayList<Point>();
				Point ircp = getRingPointZ(cp, j, 4, .5, 1.0);
				for(int k = 0; k < 5; k++) {
					pts.add(getRingPointZ(ircp, k, 5, .24, .2));
				}
                pts.add(pts.get(0)); // should start and end with the same point
				innerRings.add(new LinearRing(pts));
			}
			Polygon p = new Polygon(outerRing, innerRings);
			f.setGeometry(p);
			buffer.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "polyz", null);
		soh.process();
	}	
	
	
	@Test public void testMultiRingOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multiring");
		f.putData(date, new Date());
		f.setSchema(schema.getId());
		List<LinearRing> rings = new ArrayList<LinearRing>();
		// First (outer) ring should be in clockwise point order
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			List<Point> pts = new ArrayList<Point>();
			for(int k = 0; k < 5; k++) {
				pts.add(getRingPoint(cp, k, 5, .2, .5));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			LinearRing outerRing = new LinearRing(pts);
			rings.add(outerRing);
		}
		MultiLinearRings mring = new MultiLinearRings(rings);
		f.setGeometry(mring);
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multirings", null);
		soh.process();
	}
		
	@Test public void testMultiRingZOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multiringz");
		f.putData(date, new Date());
		f.setSchema(schema.getId());
		List<LinearRing> rings = new ArrayList<LinearRing>();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			List<Point> pts = new ArrayList<Point>();
			for(int k = 0; k < 5; k++) {
				pts.add(getRingPointZ(cp, k, 5, 1.0, 2.0));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			LinearRing outerRing = new LinearRing(pts);
			rings.add(outerRing);
		}
		MultiLinearRings mring = new MultiLinearRings(rings);
		f.setGeometry(mring);
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multiringz", null);
		soh.process();
	}
	
	@Test public void testMultiPolyOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multipoly");
		f.putData(date, new Date());
		f.setSchema(schema.getId());
		List<Polygon> polys = new ArrayList<Polygon>();
		for(int i = 0; i < 4; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			List<Point> pts = new ArrayList<Point>();
			int sides = RandomUtils.nextInt(4) + 4;
			// First (outer) ring should be in clockwise point order
			for(int k = 0; k < sides; k++) {
				pts.add(getRingPoint(cp, k, sides, 1.0, 2.0));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			LinearRing outerRing = new LinearRing(pts);
			List<LinearRing> innerRings = new ArrayList<LinearRing>();
			int inners = RandomUtils.nextInt(4) + 1;
			for(int j = 0; j < inners; j++) {
				pts = new ArrayList<Point>();
				Point ircp = getRingPoint(cp, j, inners, .5, 1.0);
				for(int k = 0; k < 5; k++) {
					pts.add(getRingPoint(ircp, k, 5, .24, .2));
				}
                pts.add(pts.get(0)); // should start and end with the same point
				innerRings.add(new LinearRing(pts));
			}
			Polygon p = new Polygon(outerRing, innerRings);
			polys.add(p);
		}
		MultiPolygons mp = new MultiPolygons(polys);
		f.setGeometry(mp);
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multipolys", null);
		soh.process();
	}
	
	
	@Test public void testMultiPolyZOutput() throws Exception {
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField date = new SimpleField("today", SimpleField.Type.DATE);
		schema.put(date);
		ObjectBuffer buffer = new ObjectBuffer();
		Feature f = new Feature();
		f.putData(id, "id multipolyz");
		f.putData(date, new Date());
		f.setSchema(schema.getId());
		List<Polygon> polys = new ArrayList<Polygon>();
		for(int i = 0; i < 5; i++) {
			Point cp = getRandomPoint(25.0); // Center of outer poly
			List<Point> pts = new ArrayList<Point>();
			for(int k = 0; k < 5; k++) {
				pts.add(getRingPointZ(cp, k, 5, 2, 1.5));
			}
            pts.add(pts.get(0)); // should start and end with the same point
			LinearRing outerRing = new LinearRing(pts);
			List<LinearRing> innerRings = new ArrayList<LinearRing>();
			for(int j = 0; j < 4; j++) {
				pts = new ArrayList<Point>();
				Point ircp = getRingPointZ(cp, j, 4, .5, 1.0);
				for(int k = 0; k < 5; k++) {
					pts.add(getRingPointZ(ircp, k, 5, .24, .2));
				}
                pts.add(pts.get(0)); // should start and end with the same point
				innerRings.add(new LinearRing(pts));
			}
			Polygon p = new Polygon(outerRing, innerRings);
			polys.add(p);
		}
		MultiPolygons mp = new MultiPolygons(polys);
		f.setGeometry(mp);
		buffer.write(f);
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer,
                    shapeOutputDir, "multipolyz", null);
		soh.process();
	}	

	private Point getRingPointZ(Point cp, int n, int total, double size, double min) {
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

	private Point getRingPoint(Point cp, int n, int total, double size, double min) {
		double lat = cp.getCenter().getLatitude().inDegrees();
		double lon = cp.getCenter().getLongitude().inDegrees();
		double theta = Math.toRadians(360.0 * n / total);
		double magnitude = min + RandomUtils.nextDouble() * size;
		double dy = magnitude * Math.sin(theta);
		double dx = magnitude * Math.cos(theta);
		return new Point(lat + dy, lon + dx);
	}
	
	private Point getRandomPoint(double radius) {
		double lat = 40.0 + (radius * RandomUtils.nextDouble());
		double lon = 40.0 + (radius * RandomUtils.nextDouble());
		return new Point(lat, lon);
	}
	
	private Point getRandomPoint() {
		double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
		double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
        return new Point(lat, lon);
	}

}

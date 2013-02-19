/****************************************************************************************
 *  TestFileGDBSupport.java
 *
 *  Created: Dec 18, 2012
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.gdb.FileGdbInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.esri.FileGdbOutputStream;

import static org.junit.Assert.assertTrue;

public class TestFileGDBSupport {

	@Test
	public void testCreateFeatureAndRow() throws XMLStreamException, IOException, URISyntaxException {
		File temp = new File(System.getProperty("java.io.tmpdir"));
		File db = new File(temp, "ftest1.gdb");
		if (db.exists()) {
			for(File f : db.listFiles()) {
				f.delete();
			}
			db.delete();
		}
		FileOutputStream fos = new FileOutputStream(new File(temp, "ftest" + System.currentTimeMillis() + ".zip"));
		ZipOutputStream zos = new ZipOutputStream(fos);
		IGISOutputStream os = new FileGdbOutputStream(zos, db, null);
		Schema schema = new Schema();
		SimpleField field = new SimpleField("speedLimit", SimpleField.Type.DOUBLE);
		schema.put(field);
		schema.setId(new URI("urn:org:mitre:112"));
		os.write(schema);
		
		Schema s2 = new Schema();
		
		SimpleField field2 = new SimpleField("temp", SimpleField.Type.FLOAT);
		SimpleField field3 = new SimpleField("volume", SimpleField.Type.DOUBLE);
		SimpleField field4 = new SimpleField("pressure", SimpleField.Type.FLOAT);
		s2.put(field2);
		s2.put(field3);
		s2.put(field4);
		s2.setId(new URI("urn:org:mitre:110"));
		os.write(s2);

		ContainerStart x = new ContainerStart("Folder");
		x.setName("data");
		os.write(x);

		Row r = new Row();
		r.setSchema(s2.getId());
		r.putData(field2, 32.0);
		r.putData(field3, 1000.0);
		r.putData(field4, 2.0);
		os.write(r);
		
		ContainerEnd y = new ContainerEnd();
		os.write(y);
		
		Feature f;
		
		/* Points */
		x = new ContainerStart("Folder");
		x.setName("individual dots");
		os.write(x);
		
		Random rand = new Random();
		for(int i = 0; i < 20; i++) {
			f = new Feature();
			f.setName("pos" + i);
			f.setSchema(schema.getId());
			f.setGeometry(new Point(rand.nextDouble(), rand.nextDouble()));
			f.putData(field, 50.0 + (5.0 * rand.nextDouble()));
			os.write(f);
		}

		y = new ContainerEnd();
		os.write(y);

		List<Point> pts = new ArrayList<Point>();
		
		/* Multi Points */
		x = new ContainerStart("Folder");
		x.setName("multipoint");
		os.write(x);
		
		for(int i = 0; i < 10; i++) {
			pts.add(new Point(-5.0 + rand.nextDouble(), -15.0 + rand.nextDouble()));
		}
		
		f = new Feature();
		f.setName("mp");
		f.setSchema(schema.getId());
		f.setGeometry(new MultiPoint(pts));
		f.putData(field, 10.0 + (5.0 * rand.nextDouble()));
		os.write(f);

		y = new ContainerEnd();
		os.write(y);

		/* Lines */
		x = new ContainerStart("Folder");
		x.setName("geo_lines");
		os.write(x);

		for(int i = 0; i < 40; i++) {
			double angle = 2.0 * Math.PI * ((double) i / 40.0);
			double s = Math.sin(angle);
			double c = Math.cos(angle);
			pts = new ArrayList<Point>(2);
			pts.add(new Point(s * .2, c * .2));
			pts.add(new Point(s * .4, c * .4));
			Line l = new Line(pts);			
			f = new Feature();
			f.setName("line" + i);
			f.setSchema(schema.getId());
			f.setGeometry(l);
			f.putData(field, 0.0);
			os.write(f);	
		}
		
		y = new ContainerEnd();
		os.write(y);

		/* Ring */
		x = new ContainerStart("Folder");
		x.setName("ring");
		os.write(x);
		
		f = new Feature();
		f.setName("ring");
		f.setSchema(schema.getId());
		f.setGeometry(makeRing(6, 1.0, 0.0, 0.0));
		f.putData(field, 0.0);
		os.write(f);	
		
		y = new ContainerEnd();
		os.write(y);
		
		/* Poly */
		x = new ContainerStart("Folder");
		x.setName("poly");
		os.write(x);

		pts = new ArrayList<Point>(2);
		for(int i = 0; i < 10; i++) {
			double angle = -(2.0 * Math.PI * ((double) i / 9.0));
			double s = Math.sin(angle);
			double c = Math.cos(angle);
			pts.add(new Point(s * .2, c * .2));
		}
		
		f = new Feature();
		f.setName("poly");
		f.setSchema(schema.getId());
		f.setGeometry(new Polygon(makeRing(6, 1.0, 10.0, 10.0)));
		f.putData(field, 0.0);
		os.write(f);		

		y = new ContainerEnd();
		os.write(y);
		
		/* Poly */
		x = new ContainerStart("Folder");
		x.setName("poly2");
		os.write(x);
		
		f = new Feature();
		f.setName("poly2");
		f.setSchema(schema.getId());
		List<LinearRing> inner = new ArrayList<LinearRing>();
		inner.add(makeRing(5, .5, -10.0, -10.0));
		f.setGeometry(new Polygon(makeRing(9, 1.2, -10.0, -10.0), inner));
		f.putData(field, 0.0);
		os.write(f);
		
		y = new ContainerEnd();
		os.write(y);
		
		os.close();
	}

	private LinearRing makeRing(int count, double radius, double xoffset, double yoffset) {
		List<Point> pts = new ArrayList<Point>(count);
		double denominator = count;
		for(int i = 0; i <= count; i++) {
			double angle = -(2.0 * i * Math.PI) / denominator;
			double s = Math.sin(angle);
			double c = Math.cos(angle);
			pts.add(new Point(yoffset + s * radius, xoffset + c * radius));
		}
		return new LinearRing(pts);
	}
	
	@Test
	public void testReadFeatureGdb() throws IOException {
		File path = new File("data/gdb/ftest1.gdb");
		IGISInputStream os = new FileGdbInputStream(path, null);
		IGISObject ob = os.read();
		int featurecount = 0;
		while(ob != null) {		
			assertTrue(ob instanceof Schema);
			ob = os.read(); // CS
			assertTrue(ob instanceof ContainerStart);
			String cname = ((ContainerStart) ob).getName();
			System.err.println("Open container " + cname);
			ob = os.read(); // Feature or Row
			while(ob != null && (ob instanceof Row)) {
				ob = os.read(); // Feature or Row
				featurecount++;
			}
			assertTrue(ob instanceof ContainerEnd);
			System.err.println("Close container - " + featurecount + " features");
			featurecount = 0;
			ob = os.read();
		}
		os.close();
	}

}



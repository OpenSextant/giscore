/****************************************************************************************
 *  TestGeoAtomOutputStream.java
 *
 *  Created: Jul 19, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.AtomAuthor;
import org.mitre.giscore.events.AtomHeader;
import org.mitre.giscore.events.AtomLink;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Element;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.atom.IAtomConstants;

import static org.junit.Assert.*;

public class TestGeoAtomOutputStream {
	private static final String OPENSEARCH = "opensearch";
	private static final SimpleField X = new SimpleField("X",
			SimpleField.Type.DOUBLE);
	private static final SimpleField Y = new SimpleField("Y",
			SimpleField.Type.DOUBLE);

	@Test
	public void testBasicOutput() throws Exception {
		File temp = File.createTempFile("test", ".xml");
		FileOutputStream os = new FileOutputStream(temp);
		IGISOutputStream gisos = GISFactory.getOutputStream(
				DocumentType.GeoAtom, os);
		AtomHeader header = new AtomHeader(new URL(
				"http://www.fake.mitre.org/12412412412512123123"),
				new AtomLink(new URL(
						"http://www.fake.mitre.org/atomfakefeed/id=xyzzy/123"),
						"self"), "dummy title", new Date());
		header.getAuthors().add(new AtomAuthor("Joe Shmoe","joe@mitre.org"));
		header.getRelatedlinks().add(new AtomLink(new URL("http://www.yahoo.com"), "related"));
		
		header.getNamespaces().add(
				Namespace.getNamespace(OPENSEARCH, "http://a9.com/-/spec/opensearch/1.1/"));
		Element results = new Element(OPENSEARCH, "totalResults");
		results.setText("1000");
		Element startIndex = new Element(OPENSEARCH, "startIndex");
		startIndex.setText("1");
		
		header.getElements().add(results);
		header.getElements().add(startIndex);
		gisos.write(header);
		
		List<IGISObject> written = new ArrayList<IGISObject>();
		for (int i = 0; i < 25; i++) {
			IGISObject ob = randomFeature();
			gisos.write(ob);
			written.add(ob);
		}
		for (int i = 0; i < 10; i++) {
			IGISObject ob = randomRow();
			gisos.write(ob);
			written.add(ob);
		}

		gisos.close();
		
		FileInputStream is = new FileInputStream(temp);
		IGISInputStream gisis = GISFactory.getInputStream(DocumentType.GeoAtom, is);
		IGISObject first = gisis.read();
		assertNotNull(first);
		assertTrue(first instanceof AtomHeader);
		AtomHeader readheader = (AtomHeader) first;
		assertEquals(header, readheader);
		
		List<IGISObject> read = new ArrayList<IGISObject>();
		while(true) {
			IGISObject ob = gisis.read();
			if (ob == null) break;
			if (ob instanceof DocumentStart) continue;
			read.add(ob);
		}
		assertEquals(written.size(), read.size());
		
		for(int i = 0; i < written.size(); i++) {
			System.err.println("Compare #" + i);
			compare(written.get(i), read.get(i));
		}
	}

	private void compare(IGISObject ob, IGISObject ob2) {
		if (ob instanceof Feature) {
			compareFeatures(ob, ob2);
		} else {
			compareRows((Row) ob, (Row) ob2);
		}
	}

	private void compareRows(Row r1, Row r2) {
		assertEquals(r1.getId(), r2.getId());
		// Compare data by named fields
		Map<String,SimpleField> r1fieldmap = new HashMap<String, SimpleField>();
		Map<String,SimpleField> r2fieldmap = new HashMap<String, SimpleField>();
		
		for(SimpleField f : r1.getFields()) {
			r1fieldmap.put(f.getName(), f);
		}
		for(SimpleField f : r2.getFields()) {
			r2fieldmap.put(f.getName(), f);
		}
		assertEquals(r1fieldmap.keySet(), r2fieldmap.keySet());
		// Compare data
		for(String name : r1fieldmap.keySet()) {
			SimpleField r1field = r1fieldmap.get(name);
			SimpleField r2field = r2fieldmap.get(name);
			Object data1 = r1.getData(r1field);
			Object data2 = r2.getData(r2field);
			if (data1 instanceof Double) {
				assertEquals((Double) data1, (Double) data2, 0.0001);
			} else {
				assertEquals(data1, data2);
			}
		}
	}

	private void compareFeatures(IGISObject ob, IGISObject ob2) {
		if (ob instanceof Feature && ob2 instanceof Feature) {
			// 
		} else {
			fail("Not both features");
		}
		Feature f1 = (Feature) ob;
		Feature f2 = (Feature) ob2;
		assertEquals(f1.getName(), f2.getName());
		assertEquals(f1.getDescription(), f2.getDescription());
		assertEquals(f1.getStartTime(), f2.getStartTime());
		// Hard to compare the geometry objects due to comparing double values
		// just compare type and counts and call it a day
		assertNotNull(f1.getGeometry());
		assertNotNull(f2.getGeometry());
		assertEquals(f1.getGeometry().getClass(), f2.getGeometry().getClass());
		assertEquals(f1.getGeometry().getNumPoints(), f2.getGeometry().getNumPoints());
		compareRows((Row) ob, (Row) ob2);
	}

	private IGISObject randomFeature() {
		Feature rval = new Feature();
		rval.setStartTime(new Date());
		rval.setName("Random Name " + RandomUtils.nextInt(100));
		fillData((Row) rval);
		int i = RandomUtils.nextInt(3);
		double centerlat = 40.0 + RandomUtils.nextDouble() * 2.0;
		double centerlon = 40.0 + RandomUtils.nextDouble() * 2.0;
		Point p1 = new Point(centerlat, centerlon);
		switch (i) {
		case 0:
			rval.setGeometry(p1);
			break;
		case 1: {
			List<Point> pts = new ArrayList<Point>();
			pts.add(p1);
			double dx = RandomUtils.nextDouble() * 4.0;
			double dy = RandomUtils.nextDouble() * 4.0;
			Point p2 = new Point(centerlat + dy, centerlon + dx);
			pts.add(p2);
			rval.setGeometry(new Line(pts));
			break;
		}
		default: {
			List<Point> pts = new ArrayList<Point>();
			pts.add(p1);
			pts.add(new Point(centerlat + 0.0, centerlon + 0.5));
			pts.add(new Point(centerlat + 0.5, centerlon + 0.5));
			pts.add(new Point(centerlat + 0.8, centerlon + 0.3));
			pts.add(new Point(centerlat + 0.5, centerlon + 0.0));
			pts.add(new Point(centerlat + 0.0, centerlon + 0.0));
			rval.setGeometry(new LinearRing(pts));
		}
		}
		rval.setDescription("Random desc " + RandomUtils.nextInt());
		return rval;
	}

	private void fillData(Row rval) {
		rval.setId("urn:mitre:test:" + System.nanoTime());
		rval.putData(IAtomConstants.LINK_ATTR,
				"http://asite.mitre.org/myservice/fetch=" + rval.getId());
		if (!(rval instanceof Feature)) {
			rval.putData(IAtomConstants.UPDATED_ATTR, new Date());
			rval.putData(IAtomConstants.TITLE_ATTR,
					"Random Title " + RandomUtils.nextInt(100));
		}
		rval.putData(X, RandomUtils.nextDouble());
		rval.putData(Y, RandomUtils.nextDouble());
		rval.putData(IAtomConstants.AUTHOR_ATTR,
				"author a" + RandomUtils.nextInt(5));
	}

	private IGISObject randomRow() {
		Row rval = new Row();
		fillData(rval);
		rval.putData(IAtomConstants.CONTENT_ATTR, "Content " + RandomUtils.nextInt());
		return rval;
	}

}

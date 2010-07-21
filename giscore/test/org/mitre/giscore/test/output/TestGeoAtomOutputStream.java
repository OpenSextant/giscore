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
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.AtomAuthor;
import org.mitre.giscore.events.AtomHeader;
import org.mitre.giscore.events.AtomLink;
import org.mitre.giscore.events.Element;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
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
		
		for (int i = 0; i < 25; i++) {
			gisos.write(randomFeature());
		}
		for (int i = 0; i < 10; i++) {
			gisos.write(randomRow());
		}

		gisos.close();
	}

	private IGISObject randomFeature() {
		Feature rval = new Feature();
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
		rval.putData(IAtomConstants.UPDATED_ATTR, new Date());
		rval.putData(IAtomConstants.TITLE_ATTR,
				"Random Title " + RandomUtils.nextInt(100));
		rval.putData(X, RandomUtils.nextDouble());
		rval.putData(Y, RandomUtils.nextDouble());
		rval.putData(IAtomConstants.AUTHOR_ATTR,
				"author a" + RandomUtils.nextInt(5));
	}

	private IGISObject randomRow() {
		Row rval = new Row();
		fillData(rval);
		return rval;
	}

}

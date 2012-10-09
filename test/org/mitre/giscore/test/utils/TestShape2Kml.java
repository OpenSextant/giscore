package org.mitre.giscore.test.utils;

import junit.framework.TestCase;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.utils.Shape2Kml;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/27/11 12:05 PM
 */
public class TestShape2Kml extends TestCase {

	public void testConversion() throws XMLStreamException, IOException {
		Shape2Kml app = new Shape2Kml();
		final File baseDir = new File("testOutput/kml");
		baseDir.mkdir();
		app.setBaseDir(baseDir);
		testOutput(app, 5, new File("data/shape/points.shp"), new File(baseDir, "points.kmz"));
		app.setLabelName("LONG_NAME");
		testOutput(app, 1, new File("data/shape/Iraq.shp"), new File(baseDir, "Iraq.kmz"));
	}

	private void testOutput(Shape2Kml app, int count, File input, File target)
			throws XMLStreamException, IOException {
		app.outputKml(input);
		assertTrue(target.exists());
		KmlReader reader = new KmlReader(target);
		try {
			IGISObject o;
			int features = 0;
			while ((o = reader.read()) != null) {
				if (o instanceof Feature) features++;
			}
			assertEquals(count, features);
		} finally {
			reader.close();
		}
	}

}

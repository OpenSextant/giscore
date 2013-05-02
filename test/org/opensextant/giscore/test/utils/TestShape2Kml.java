package org.opensextant.giscore.test.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.input.kml.KmlReader;
import org.opensextant.giscore.utils.Shape2Kml;

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
		app.outputKml(input, false);
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

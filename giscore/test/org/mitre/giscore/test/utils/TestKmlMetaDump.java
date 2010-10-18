package org.mitre.giscore.test.utils;

import junit.framework.TestCase;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.utils.KmlMetaDump;

import java.io.File;
import java.io.IOException;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Oct 18, 2010 1:49:01 PM
 */
public class TestKmlMetaDump extends TestCase {

	public void testMain() {
		KmlMetaDump.main(new String[] { "data/kml/Placemark" }); 
	}

	public void testKmlTime() throws IOException {
		realTest(new File("data/kml/time"));		
	}

	public void testKmlGx() throws IOException {
		realTest(new File("data/kml/gx"));
	}

	public void testKmlSloppy() throws IOException {
		realTest(new File("data/kml/sloppy"));
	}

	public void testKmlOutput() throws IOException {
		File file = new File("data/kml/MultiGeometry/testLayers.kml");
		if (file.isFile()) {
			KmlMetaDump app = new KmlMetaDump();
			app.setOutPath(new File("testOutput"));
			app.checkSource(file);
			assertTrue(app.getTagSet().containsKey(IKml.MULTI_GEOMETRY));
		}
	}

	public void testKmlSchema() throws IOException {
		File dir = new File("data/kml/Schema");
		if (dir.isDirectory()) {
			KmlMetaDump app = new KmlMetaDump();
			app.useSimpleFieldSet();
			app.checkSource(dir);
			assertFalse(app.getSimpleFieldSet().isEmpty());
			assertTrue(app.getTagSet().containsKey(IKml.SCHEMA));
		}
	}

	public void testKmlMetaDump() throws IOException {
		File dir = new File("data/kml/kmz");
		if (dir.isDirectory()) {
			KmlMetaDump app = new KmlMetaDump();
			app.setFollowLinks(true);
			app.checkSource(dir);
			assertTrue(app.getTagSet().containsKey(IKml.NETWORK_LINK));
		}
	}

	private void realTest(File file) throws IOException {
		if (file.isDirectory()) {
			KmlMetaDump app = new KmlMetaDump();
			app.checkSource(file);
			assertFalse(app.getTagSet().isEmpty());
		}
	}
	
}

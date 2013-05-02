package org.opensextant.giscore.test.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.opensextant.giscore.utils.KmlRegionBox;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Oct 18, 2010 1:49:01 PM
 */
public class TestKmlRegionBox extends TestCase {

	public void testMain() {
        // test remote NetworkLink with Region definition
        String url = "http://jason-stage.mitre.org:8080/kmlWeb/liveFeed.gsp"; 
        KmlRegionBox.main(new String[] {
            url, "-otestOutput/bbox.kml", "-f"
        });
	}

	public void testKmlMetaDump() throws XMLStreamException, IOException {
		File dir = new File("data/kml/Region");
		if (dir.isDirectory()) {
			KmlRegionBox app = new KmlRegionBox();
            app.setOutFile(new File("testOutput/bbox.kml"));
            try {
			    app.checkSource(dir);
			    assertTrue(! app.getRegions().isEmpty());
            } finally {
                app.close();
            }
		}
	}

}

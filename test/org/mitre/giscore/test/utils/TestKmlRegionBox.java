package org.mitre.giscore.test.utils;

import junit.framework.TestCase;
import org.mitre.giscore.utils.KmlRegionBox;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

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

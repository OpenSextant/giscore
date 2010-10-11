/****************************************************************************************
 *  TestKmlSupport.java
 *
 *  Created: Feb 5, 2009
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
package org.mitre.giscore.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.apache.commons.io.IOUtils;
import org.mitre.giscore.output.kml.KmlOutputStream;

import javax.xml.stream.XMLStreamException;

/**
 * @author DRAND
 *
 */
public class TestKmlSupport extends TestGISBase {

	/**
	 * Base path to test directories
	 */
	public static final String base_path = "data/kml/";

	@Test public void testAtom() throws Exception {
		runTestsOnDir("atom");
	}
	
	@Test public void testBalloon() throws Exception {
		runTestsOnDir("balloon");
	}
	
	@Test public void testBalloonStyle() throws Exception {
		runTestsOnDir("BalloonStyle");
	}

	@Test public void testCamera() throws Exception {
		runTestsOnDir("Camera");
	}
	
	@Test public void testExtendedData() throws Exception {
		runTestsOnDir("ExtendedData");
	}
	
	@Test public void testFeatureAnchor() throws Exception {
		runTestsOnDir("feature-anchor");
	}

	@Test public void testFeatureType() throws Exception {
		runTestsOnDir("FeatureType");
	}

	@Test public void testGroundOverlay() throws Exception {
		runTestsOnDir("GroundOverlay");
	}
	
	@Test public void testItemIcon() throws Exception {
		runTestsOnDir("ItemIcon");
	}
	
	@Test public void testLinkType() throws Exception {
		runTestsOnDir("LinkType");
	}

	@Test public void testLinkStyle() throws Exception {
		runTestsOnDir("LinkStyle");
	}
	
	@Test public void testlistview() throws Exception {
		runTestsOnDir("listview");
	}
	
	@Test public void testMetadata() throws Exception {
		runTestsOnDir("Metadata");
	}
	
	@Test public void testMultiGeometry() throws Exception {
		runTestsOnDir("MultiGeometry");
	}
	
	@Test public void testNetworkLink() throws Exception {
		runTestsOnDir("NetworkLink");
	}

	@Test public void testPhotoOverlay() throws Exception {
		runTestsOnDir("PhotoOverlay");
	}
	
	@Test public void testPlacemark() throws Exception {
		runTestsOnDir("Placemark");
	}
	
	@Test public void testPolygon() throws Exception {
		runTestsOnDir("Polygon");
	}
	
	@Test public void testRegion() throws Exception {
		runTestsOnDir("Region");
	}
	
	@Test public void testSchema() throws Exception {
		runTestsOnDir("Schema");
	}

	@Test public void testScreenOverlay() throws Exception {
		runTestsOnDir("ScreenOverlay");
	}
	
	@Test public void testsky() throws Exception {
		runTestsOnDir("sky");
	}	
	
	@Test public void testStyle() throws Exception {
		runTestsOnDir("Style");
	}	

	@Test public void testTime() throws Exception {
		runTestsOnDir("time");
	}	

	@Test public void testXmlns() throws Exception {
		runTestsOnDir("xmlns");
	}	
	
	/**
	 * Iterate over the files in the directory
	 * @param dirname
	 * @throws IOException 
	 */
	private void runTestsOnDir(String dirname) throws IOException {
		File dir = new File(base_path, dirname);
        File contents[] = dir.listFiles();
		if (contents != null) {
			for(File testcase : contents) {
				if (testcase.isFile() && testcase.getName().endsWith(".kml")) {
					doTest(testcase);
				}
			}
		}
	}

	/**
	 * Do the actual test. The actual test reads in the original file, writes
	 * out the data, then compares the data for essential equality. This means
	 * that for features, the geometry is not compared precisely
	 * 
	 * @param testcase the file being checked
	 * @throws IOException 
	 */
	private void doTest(File testcase) throws IOException {
		System.out.println("Testing " + testcase);
        File temp = null;
        FileInputStream fs = new FileInputStream(testcase);
        List<IGISObject> elements = new ArrayList<IGISObject>();
        try {
            KmlInputStream kis = (KmlInputStream) GISFactory.getInputStream(DocumentType.KML, fs);
		    temp = createTemp(testcase.getName(), ".kml");
			OutputStream fos = new FileOutputStream(temp);
            String encoding = kis.getEncoding();
            assertNotNull(encoding);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos, encoding);
            IGISObject current;
            while ((current = kis.read()) != null) {
                os.write(current);
                elements.add(current);
            }

            kis.close();
            fs.close();

            os.close();
            fos.close();

            //System.out.println("Testing rewritten file: " + testcase.getName());

            // Test for equivalence
            fs = new FileInputStream(temp);
            IGISInputStream is = GISFactory.getInputStream(DocumentType.KML, fs);
            int index = 0;
            while ((current = is.read()) != null) {
                //elements2.add(current);
                if (index >= elements.size()) {
                    fail("Found at least one extra element " + current);
                }
                IGISObject prev = elements.get(index++);
                if (prev instanceof Schema) {
                    Schema schema = (Schema)prev;
                    // if schema aliasing is used by presence of parent element or attribute
                    // (old-style KML schema feature) then skip approx test for this element
                    if (schema.getParent() != null)
                        continue;
                }
                checkApproximatelyEquals(prev, current);
            }
            is.close();
        } catch (IOException e) {
            System.out.println(" *Failed to parse KML for testcase: " + testcase.getName());
            //String msg = e.getMessage();
            //if (msg == null || !msg.contains("Message: Invalid byte 1 of 1-byte UTF-8 sequence"))
            throw e;
            // otherwise we wrote a KML source with wrong XML encoding
            // this is an error in the tester not the giscore framework
        } catch (AssertionError e) {
            System.out.println(" *Failed in testcase: " + testcase.getName());
            // System.out.println(" *temp=" + temp);
            /*
                for(Object o : elements) {
                    System.out.println(" >" + o.getClass().getName());
                }
                System.out.println();
                for(Object o : elements2) {
                    System.out.println(" <" + o.getClass().getName());
                }
                System.out.println("\nelts1=" + elements);
                System.out.println("\nelts2=" + elements2);
                System.out.println();
                */
            throw e;
        } finally {
            IOUtils.closeQuietly(fs);
			if (autoDelete && temp != null && temp.exists()) {
                temp.delete();
            }
        }
	}

    @Test public void testStyleUrl()  {
        Feature f = new Feature();
        f.setStyleUrl("myStyle");
        // test auto anchor prefix '#' prepend to feature style url
        assertEquals("#myStyle", f.getStyleUrl());

        f.setStyleUrl("#my_Style123");
        assertEquals("#my_Style123", f.getStyleUrl());
    }

    @Test public void testStyleMapUrl()  {
        StyleMap sm = new StyleMap("myStyle");
        sm.put(StyleMap.NORMAL, "sn_myStyle");
        sm.put(StyleMap.HIGHLIGHT, "sh_myStyle");
        // test auto anchor prefix '#' prepend to styleUrls
        assertEquals("#sn_myStyle", sm.get(StyleMap.NORMAL));
        assertEquals("#sh_myStyle", sm.get(StyleMap.HIGHLIGHT));
    }

	@Test
	public void test_Style() throws XMLStreamException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
		KmlOutputStream kos = new KmlOutputStream(bos);
		Style s = new Style("myStyle");
		s.setIconStyle(Color.red, 1.4, "http://maps.google.com/mapfiles/kml/shapes/airports.png");
		kos.write(s);
		kos.write(new ContainerStart(IKml.DOCUMENT));
		Feature f = createBasicFeature(Point.class);
		f.setStyleUrl("myStyle");
		// test auto anchor prefix '#' prepend to feature style url
		assertEquals("#myStyle", f.getStyleUrl());
		kos.write(f);
		kos.write(new ContainerEnd());
		kos.close();

		//String kml = bos.toString("UTF-8");
		//System.out.println(kml);
		KmlInputStream kis = new KmlInputStream(new ByteArrayInputStream(bos.toByteArray()));
		int count = 0;
		for (IGISObject o; (o = kis.read()) != null; ) {
			if (o instanceof Feature) {
				Feature f2 = (Feature)o;
				assertEquals("#myStyle", f2.getStyleUrl());
			}
			count++;
			// System.out.println(o);
		}
		assertEquals(5, count);
		kis.close();
    }

	/**
	 * For most objects they need to be exactly the same, but for some we can 
	 * approximate equality
	 * 
	 * @param source
	 * @param test
	 */
	private void checkApproximatelyEquals(IGISObject source, IGISObject test) {
		if (Feature.class.isAssignableFrom(source.getClass()) && 
				Feature.class.isAssignableFrom(test.getClass())) {
			Feature sf = (Feature) source;
			Feature tf = (Feature) test;

            if (!sf.approximatelyEquals(tf)) {
                System.out.format(" *Failed approximatelyEquals\n\tsrc=%s\n\ttest=%s%n",
                  sf.getClass().getName(), tf.getClass().getName());
				/*
                System.out.println(" source=" + sf);
                System.out.println("--");
                System.out.println(" test=" + tf);
                System.out.println("--");
                */
                fail("approximatelyEquals");
            }
        } else {
			assertEquals(source, test);
		}
	}
}


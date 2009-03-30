/****************************************************************************************
 *  TestKmlOutputStream.java
 *
 *  Created: Feb 4, 2009
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.test.TestGISBase;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLStreamException;

/**
 * Test the output stream
 * 
 * @author DRAND
 * 
 */
public class TestKmlOutputStream extends TestGISBase {

    @Test
	public void testSimpleCase() throws Exception {
		doTest(getStream("7084.kml"));
	}
	
	/**
	 * Note, this test fails due to some sort of issue with geodesy, but the
	 * actual output kml is fine.
	 * @throws Exception
	 */
	@Test
	public void testCase2() throws Exception {
		doTest(getStream("KML_sample1.kml"));
	}
	
	@Test
	public void testCase3() throws Exception {
		doTest(getStream("schema_example.kml"));
	}

    @Test
    public void testKmz() throws IOException, XMLStreamException {
        File file = createTemp("test", ".kmz");
        ZipOutputStream zoS = null;
        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream boS = new BufferedOutputStream(os);
            // Create the doc.kml file inside of a zip entry
            zoS = new ZipOutputStream(boS);
            ZipEntry zEnt = new ZipEntry("doc.kml");
            zoS.putNextEntry(zEnt);
            KmlOutputStream kos = new KmlOutputStream(zoS);
            kos.write(new DocumentStart(DocumentType.KML));
            Feature f = new Feature();
            f.setGeometry(new Point(42.504733587704, -71.238861602674));
            f.setName("test");
            f.setDescription("this is a test placemark");
            kos.write(f);
            try {
                kos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(zoS);
            zoS = null;
            KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll();
            // imported features should be DocumentStart followed by Feature 
            assertEquals(2, objs.size());
            checkApproximatelyEquals(f, objs.get(1));
        } finally {
            IOUtils.closeQuietly(zoS);
            if (file != null && file.exists()) file.delete();
        }
    }

    public void doTest(InputStream fs) throws Exception {
        File temp = null;
        try {
            IGISInputStream is = GISFactory.getInputStream(DocumentType.KML, fs);
		    temp = createTemp("test", ".kml");
            OutputStream fos = new FileOutputStream(temp);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos);
            List<IGISObject> elements = new ArrayList<IGISObject>();
            IGISObject current;
            while ((current = is.read()) != null) {
                os.write(current);
                elements.add(current);
            }

            is.close();
            fs.close();

            os.close();
            fos.close();

            // Test for equivalence
            fs = new FileInputStream(temp);
            is = GISFactory.getInputStream(DocumentType.KML, fs);
            int index = 0;
            while ((current = is.read()) != null) {
                checkApproximatelyEquals(elements.get(index++), current);
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(fs);
            if (temp != null && temp.exists()) temp.delete();
        }
    }
	
	/**
	 * For most objects they need to be exactly the same, but for some we can 
	 * approximate equality
	 * 
	 * @param source
	 * @param test
	 */
	public static void checkApproximatelyEquals(IGISObject source, IGISObject test) {
		if (source instanceof Feature && test instanceof Feature) {
			Feature sf = (Feature) source;
			Feature tf = (Feature) test;
			
			boolean ae = sf.approximatelyEquals(tf);
			
			if (! ae) {		
				System.err.println("Source: " + source);
				System.err.println("Test: " + test);
				fail("Found unequal objects");
			}
		} else {
			assertEquals(source, test);
		}
	}

    private InputStream getStream(String filename) throws FileNotFoundException {
        File file = new File("test/org/mitre/giscore/test/input/" + filename);
        if (file.exists()) return new FileInputStream(file);
        System.out.println("File does not exist: " + file);
        return getClass().getResourceAsStream(filename);
    }
}

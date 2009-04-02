/****************************************************************************************
 *  TestKmlInputStream.java
 *
 *  Created: Jan 27, 2009
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
package org.mitre.giscore.test.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.input.IGISInputStream;
import org.apache.commons.io.IOUtils;

/**
 * @author DRAND
 * 
 */
public class TestKmlInputStream {
	@Test
	public void testTinySample() throws Exception {
		InputStream stream = getStream("7084.kml");
		try {
			IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);

			IGISObject firstN[] = new IGISObject[10];
			for(int i = 0; i < firstN.length; i++) {
				firstN[i] = kis.read();
			}
			assertNotNull(firstN[7]);
			assertNull(firstN[8]);
			assertTrue(firstN[0] instanceof DocumentStart);
			DocumentStart ds = (DocumentStart) firstN[0];
			assertEquals(DocumentType.KML, ds.getType());
			assertTrue(firstN[1] instanceof Style);
			assertTrue(firstN[2] instanceof Style);
			assertTrue(firstN[3] instanceof StyleMap);
			assertTrue(firstN[4] instanceof ContainerStart);
			assertTrue(firstN[5] instanceof Feature);
			assertTrue(firstN[6] instanceof Feature);
			assertTrue(firstN[7] instanceof ContainerEnd);
		} finally {
		    IOUtils.closeQuietly(stream);
		}
	}

	/**
	 * Test calling close() multiple times does not throw an exception
	 * @throws Exception
	 */
	@Test
	public void testClose() throws Exception {
		InputStream stream = getStream("7084.kml");
		try {
			IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);
			while (kis.read() != null) {
				// nothing
			}
			assertNull(kis.read());
			kis.close();
			assertNull(kis.read());
			kis.close();
		} finally {
		    IOUtils.closeQuietly(stream);
		}
	}

	@Test public void testLargerSample() throws Exception {
		InputStream stream = getStream("KML_sample1.kml");
		try {
			IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);

			IGISObject firstN[] = new IGISObject[100];
			for(int i = 0; i < firstN.length; i++) {
				firstN[i] = kis.read();
			}
			System.out.println(firstN);
		} finally {
		    IOUtils.closeQuietly(stream);
		}
	}
	
	@Test public void testSchemaSample() throws Exception {
		InputStream stream = getStream("schema_example.kml");
		try {
			IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, stream);

			IGISObject firstN[] = new IGISObject[10];
			Schema s = null;
			Feature f = null;
			for(int i = 0; i < firstN.length; i++) {
				IGISObject obj = kis.read();
				if (s == null && obj instanceof Schema) {
					s = (Schema) obj;
				} else if (f == null && s != null && obj instanceof Feature) {
					f = (Feature) obj;
				}
				firstN[i] = obj;
			}
			Collection<SimpleField> fields = f.getFields();
			assertNotNull(fields);
			for(SimpleField field : fields) {
				assertEquals(field, s.get(field.getName()));
			}
			System.out.println(firstN);
		} finally {
		    IOUtils.closeQuietly(stream);
		}
	}

    private InputStream getStream(String filename) throws FileNotFoundException {
        File file = new File("test/org/mitre/giscore/test/input/" + filename);
        if (file.exists()) return new FileInputStream(file);
        System.out.println("File does not exist: " + file);
        return getClass().getResourceAsStream(filename);
    }

}


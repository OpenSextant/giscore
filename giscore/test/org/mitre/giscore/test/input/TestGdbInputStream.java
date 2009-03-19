/****************************************************************************************
 *  TestGdbInputStream.java
 *
 *  Created: Mar 18, 2009
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

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.input.IGISInputStream;


/**
 * @author DRAND
 *
 */
public class TestGdbInputStream {
	@Test public void testFileGdbInput() throws Exception {
		IGISInputStream gis = GISFactory.getInputStream(DocumentType.FileGDB, 
				new File("data/gdb/test20090312163935.gdb"), "LPATH A");
		int schema_count = 0;
		int total = 0;
		IGISObject gisobject = null;
		SimpleField lpath = null;
		String lastpath = null;
		while((gisobject = gis.read()) != null) {
			total++;
			if (gisobject instanceof Schema) {
				Schema s = (Schema) gisobject;
				if (lpath == null) {
					lpath = s.get("lpath");
				}
				schema_count++;
				assertTrue(s.getKeys() != null && s.getKeys().size() > 0);
				assertNotNull(s.getOidField());
				assertNotNull(lpath);
			} else if (gisobject instanceof Feature) {
				Feature f = (Feature) gisobject;
				String path = (String) f.getData(lpath);
				if (lastpath != null) {
					System.err.println("Path: " + path);
				}
				lastpath = path;
			}
		}
		assertEquals(1, schema_count);
	}
}

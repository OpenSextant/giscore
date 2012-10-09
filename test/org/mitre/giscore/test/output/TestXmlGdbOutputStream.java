/****************************************************************************************
 *  TestXmlGdbOutputStream.java
 *
 *  Created: Feb 10, 2009
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.test.TestGISBase;


/**
 * Test output for XmlGdb stream.
 * 
 * @author DRAND
 */
public class TestXmlGdbOutputStream extends TestGISBase {
	@Test public void testSimple() throws Exception {
		Schema s1 = new Schema();
		SimpleField field = new SimpleField("category");
		field.setDisplayName("Category");
		field.setType(SimpleField.Type.STRING);
		s1.put(field);
		field = new SimpleField("subcategory");
		field.setDisplayName("Sub Category");
		field.setType(SimpleField.Type.STRING);
		s1.put(field);
		
		File test = createTemp("testxmlgdb", ".xml");
		FileOutputStream fos = new FileOutputStream(test);
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.XmlGDB, fos);
		os.write(s1);
		
		String names[] = {"hole", "distance"};
		Object values[];
		ContainerStart cs = new ContainerStart("Folder");
		cs.setName("One");
		os.write(cs);
		for(int i = 0; i < 3; i++) {
			values = new Object[2];
			values[0] = random.nextInt(18) + 1;
			values[1] = random.nextInt(40) * 10;
			Feature f = createFeature(Point.class, names, values);
			os.write(f);
		}
		os.write(new ContainerEnd());
		Map<String,Object> vmap = new HashMap<String, Object>();
		vmap.put("category", "building");
		vmap.put("subcategory", "house");

		cs = new ContainerStart("Folder");
		cs.setName("Two");
		os.write(cs);
		for(int i = 0; i < 10; i++) {
			Feature f = createFeature(Point.class, s1, vmap);
			os.write(f);
		}
		os.write(new ContainerEnd());
		cs = new ContainerStart("Folder");
		cs.setName("Three");
		os.write(cs);
		for(int i = 0; i < 10; i++) {
			Feature f = createFeature(Line.class, s1, vmap);
			os.write(f);
		}
		os.write(new ContainerEnd());
		cs = new ContainerStart("Folder");
		cs.setName("Four");
		os.write(cs);
		for(int i = 0; i < 10; i++) {
			Feature f = createFeature(LinearRing.class, s1, vmap);
			os.write(f);
		}
		os.write(new ContainerEnd());
		cs = new ContainerStart("Folder");
		cs.setName("Five");
		os.write(cs);
		for(int i = 0; i < 10; i++) {
			Feature f = createFeature(Polygon.class, s1, vmap);
			os.write(f);
		}
		os.write(new ContainerEnd());
		os.close();
		IOUtils.closeQuietly(fos);
	}
}

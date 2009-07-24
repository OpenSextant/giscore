/****************************************************************************************
 *  TestShapefileOutput.java
 *
 *  Created: Jul 23, 2009
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
import java.net.URI;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.shapefile.SingleShapefileOutputHandler;
import org.mitre.giscore.utils.ObjectBuffer;

public class TestShapefileOutput {
	@Test public void testPointOutput() throws Exception {
		FileOutputStream zip = new FileOutputStream("c:/temp/shptest/reference.zip");
		ZipOutputStream zos = new ZipOutputStream(zip);
		IGISOutputStream shpos = GISFactory.getOutputStream(DocumentType.Shapefile, zos, new File("c:/temp/shptest/buf/"));
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		DocumentStart ds = new DocumentStart(DocumentType.Shapefile);
		shpos.write(ds);
		ContainerStart cs = new ContainerStart("Folder");
		cs.setName("aaa");
		shpos.write(cs);
		shpos.write(schema);
		ObjectBuffer buffer = new ObjectBuffer();
		for(int i = 0; i < 5; i++) {
			Feature f = new Feature();
			f.putData(id, "id " + i);
			f.setSchema(schema.getId());
			double lat = 40.0 + (5.0 * RandomUtils.nextDouble());
			double lon = 40.0 + (5.0 * RandomUtils.nextDouble());
			Point point = new Point(lat, lon);
			f.setGeometry(point);
			buffer.write(f);
			shpos.write(f);
		}
		SingleShapefileOutputHandler soh =
			new SingleShapefileOutputHandler(schema, null, buffer, 
					new File("c:/temp/shptest/"), "points", null);
		soh.process();
		shpos.close();
		zos.flush();
		zos.close();
	}
}

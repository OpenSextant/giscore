/****************************************************************************************
 *  TestLargeShapefileOutput.java
 *
 *  Created: Dec 10, 2009
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
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.shapefile.SingleShapefileOutputHandler;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.giscore.utils.ObjectBuffer;

/**
 * Create a large shapefile with comp
 * 
 * @author DRAND
 */
public class TestLargeShapefileOutput extends TestGISBase {
	public static final int memsize = 10000;
	public static final int totsize = 200000;
	
	@Test public void createLargeShapefile() throws Exception {
		GISFactory.inMemoryBufferSize.set(memsize);
		Schema schema = new Schema(new URI("urn:test"));
		SimpleField id = new SimpleField("testid");
		id.setLength(10);
		schema.put(id);
		SimpleField dtm = new SimpleField("dtm", Type.DATE);
		schema.put(dtm);
		SimpleField lat = new SimpleField("lat", Type.DOUBLE);
		schema.put(lat);
		SimpleField lon = new SimpleField("lon", Type.DOUBLE);
		schema.put(lon);
		SimpleField charFields[] = new SimpleField[26];
		for(int i = 0; i < charFields.length; i++) {
			charFields[i] = new SimpleField(getRandomFieldName());
			charFields[i].setLength(RandomUtils.nextInt(100) + 120);
			schema.put(charFields[i]);
		}
		SimpleField intFields[] = new SimpleField[10];
		for(int i = 0; i < intFields.length; i++) {
			intFields[i] = new SimpleField(getRandomFieldName(), Type.INT);
			schema.put(intFields[i]);
		}
		
		File tempcsv = new File(tempdir, "large.csv");
		OutputStream csvos = new FileOutputStream(tempcsv);
		IGISOutputStream csvout = GISFactory.getOutputStream(DocumentType.CSV, csvos);
		
		for(int i = 0; i < totsize; i++) {
			Row r = new Row();
			r.putData(id, "id " + i);
			r.putData(dtm, new Date(System.currentTimeMillis()));
			r.putData(lat, RandomUtils.nextDouble() * 5.0 + 30.0);
			r.putData(lon, RandomUtils.nextDouble() * 5.0 + 30.0);
			r.setSchema(schema.getId());
			for(int j = 0; j < charFields.length; j++) {
				if (RandomUtils.nextInt(3) == 1) continue; // null value
				r.putData(charFields[j], getRandomText(charFields[j]));
			}
			for(int j = 0; j < intFields.length; j++) {
				if (RandomUtils.nextInt(3) == 1) continue; // null value
				r.putData(intFields[j], RandomUtils.nextInt());
			}
			csvout.write(r);
		}
		csvout.close();
		IOUtils.closeQuietly(csvos);
		
		IGISInputStream csvin = GISFactory.getInputStream(DocumentType.CSV, tempcsv, schema);
		ObjectBuffer buffer = new ObjectBuffer();
		while(true) {
			IGISObject ob = csvin.read();
			if (ob == null) {
				csvin.close();
				break;
			}
			if (ob instanceof Row) {
				Row row = (Row) ob;
				Feature f = new Feature();
				Double latVal = new Double((String) row.getData(lat));
				Double lonVal = new Double((String) row.getData(lon));
				f.setGeometry(new Point(latVal, lonVal));
				f.setSchema(schema.getId());
				for(SimpleField field : row.getFields()) {
					if (lat.equals(field) || lon.equals(field)) continue;
					Object val = row.getData(field);
					f.putData(field, val);
				}
				buffer.write(f);
			}
		}
		SingleShapefileOutputHandler handler = new SingleShapefileOutputHandler(schema, null, buffer, tempdir, "largepoints", null);
		handler.process();
		
		System.out.println("Done");
	}

	private Object getRandomText(SimpleField simpleTextField) {
		int len = simpleTextField.getLength();
		StringBuilder sb = new StringBuilder(20);
		for(int i = 0; i < len; i++) {
			if (RandomUtils.nextInt(10) == 1) 
				sb.append(' ');
			else
				sb.append((char) ('a' + RandomUtils.nextInt(25)));
		}
		return sb.toString();
	}

	private String getRandomFieldName() {
		StringBuilder sb = new StringBuilder(20);
		for(int i = 0; i < (3 + RandomUtils.nextInt(10)); i++) {
			int n = RandomUtils.nextInt(20);
			switch(n) {
			case 1:
				sb.append(' ');
				break;
			case 2:
				sb.append(':');
				break;
			case 3:
				sb.append('(');
				break;
			case 4:
				sb.append(')');
				break;
			case 5:
				sb.append('_');
				break;
			case 6:
				sb.append('/');
				break;
			case 7:
				sb.append((char) '0' + RandomUtils.nextInt(9));
				break;
			default:
				sb.append((char) ('a' + RandomUtils.nextInt(26)));
			}
		}
		return sb.toString();
	}
}

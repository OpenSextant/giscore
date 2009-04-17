package org.mitre.giscore.test;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.ICategoryNameExtractor;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.SortingOutputStream;

/****************************************************************************************
 *  TestSorterOutputStream.java
 *
 *  Created: Apr 16, 2009
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

/**
 * @author DRAND
 * 
 */
public class TestSorterOutputStream extends TestGISBase {
	static SimpleField ms_type = new SimpleField("reportType", Type.STRING);

	/**
	 * @author DRAND
	 * 
	 */
	public class TestCNS implements IContainerNameStrategy,
			ICategoryNameExtractor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mitre.giscore.output.IContainerNameStrategy#deriveContainerName
		 * (java.util.List, org.mitre.giscore.output.FeatureKey)
		 */
		@Override
		public String deriveContainerName(List<String> path, FeatureKey feature) {
			String fullpath = StringUtils.join(path, '-');
			if (feature.getGeoclass() != null) {
				fullpath += "-" + feature.getGeoclass().getSimpleName();
			}
			return fullpath;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mitre.giscore.ICategoryNameExtractor#extractCategoryName(org.
		 * mitre.giscore.events.Row)
		 */
		@Override
		public String extractCategoryName(Row row) {
			return row.getData(ms_type).toString();
		}

	}

	@Test
	public void testKml() throws Exception {
		File temp = createTemp("test", ".kml");
		OutputStream fos = new FileOutputStream(temp);
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos);
		TestCNS strategy = new TestCNS();
		SortingOutputStream sos = new SortingOutputStream(os, strategy,
				strategy);
		outputTestData(sos);

		sos.close();
		fos.close();
	}

	@Test
	public void testFileGDB() throws Exception {
		File temp = createTemp("test", ".zip");
		File gdbDir = new File(temp.getParentFile(), "test.gdb");
		OutputStream fos = new FileOutputStream(temp);
		ZipOutputStream zos = new ZipOutputStream(fos);
		zos.putNextEntry(new ZipEntry("Results"));
		TestCNS strategy = new TestCNS();
		IGISOutputStream os = GISFactory.getOutputStream(DocumentType.FileGDB, zos, gdbDir, strategy);
		SortingOutputStream sos = new SortingOutputStream(os, strategy,
				strategy);
		outputTestData(sos);

		sos.close();
		zos.close();
	}
	
	private void outputTestData(SortingOutputStream sos)
			throws URISyntaxException {
		Schema ts = new Schema(new URI("#testSchema"));
		ts.put(new SimpleField("height", SimpleField.Type.DOUBLE));
		ts.put(new SimpleField("reportType", SimpleField.Type.STRING));
		ts.put(new SimpleField("count", SimpleField.Type.INT));
		ContainerStart cs = new ContainerStart("results");
		Style teststyle = new Style();
		teststyle.setLineStyle(Color.red, 1.4);
		sos.write(teststyle);
		sos.write(cs);
		sos.write(ts);
		
		for (int i = 0; i < 100; i++) {
			sos.write(getRandomFeature(i));
		}
	}

	private static String types[] = { "orange", "cherry", "apple" };

	/**
	 * @param i
	 * @return
	 */
	private IGISObject getRandomFeature(int i) {
		Feature feature = new Feature();
		feature.putData(ms_type, types[RandomUtils.nextInt(types.length)]);
		feature.setGeometry(new Point(RandomUtils.nextDouble() + 30.0,
				RandomUtils.nextDouble() + 40.0));
		return feature;
	}

}


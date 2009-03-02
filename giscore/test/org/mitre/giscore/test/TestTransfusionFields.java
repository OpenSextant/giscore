/****************************************************************************************
 *  TestTransfusionFields.java
 *
 *  Created: Feb 27, 2009
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.itf.geodesy.Geodetic2DPoint;


/**
 * Test to localize issues with specific fields used for transfusion. Just tries
 * to create a file gdb with these fields.
 * 
 * @author DRAND
 *
 */
public class TestTransfusionFields {
	public static File tempdir = null;
	
	static {
		String dir = System.getProperty("java.io.tmpdir");
		tempdir = new File(dir);
	}
	
	private static final String XFUSION_SCHEMA_PT = "xfusion_schema_pt";
	private static final String XFUSION_SCHEMA_RING = "xfusion_schema_ring";
	
	private static List<SimpleField> ms_fields = new ArrayList<SimpleField>();
	private static SimpleField ms_field_cacheUrl;
	private static SimpleField ms_field_localUrl;	
	private static SimpleField ms_field_origURL;
	private static SimpleField ms_field_itfid;
	private static SimpleField ms_field_classification;
	private static SimpleField ms_field_earliestReportDate;
	private static SimpleField ms_field_latestReportDate;
	private static SimpleField ms_field_creationDate;	
	private static SimpleField ms_field_postedDate;
	private static SimpleField ms_field_validUntil;
	private static SimpleField ms_field_infoCutoff;	
	private static SimpleField ms_field_lat;
	private static SimpleField ms_field_lon;
	private static SimpleField ms_field_mgrs;
	private static SimpleField ms_field_rpt;
	private static SimpleField ms_field_desc;
	private static SimpleField ms_field_ctx;

	static {
		ms_field_cacheUrl = makeSimpleField("cacheURL", "Cache_URL",
				SimpleField.Type.STRING);
		ms_field_localUrl = makeSimpleField("localURL", "Local_URL",
				SimpleField.Type.STRING);				
		ms_field_origURL = makeSimpleField("origURL", "Source_URL",
				SimpleField.Type.STRING);
		ms_field_itfid = makeSimpleField("itfid", "ITFID", SimpleField.Type.INT);
		ms_field_classification = makeSimpleField("clsfctn", "Classification",
				SimpleField.Type.STRING);
		ms_field_earliestReportDate = makeSimpleField("startRepDt",
				"Earliest_Report_Date", SimpleField.Type.DATE);
		ms_field_latestReportDate = makeSimpleField("endRepDt",
				"Latest_Report_Date", SimpleField.Type.DATE);
		ms_field_creationDate = makeSimpleField("createDt", "Creation_Date",
				SimpleField.Type.DATE);				
		ms_field_postedDate = makeSimpleField("postedDt", "Posted_Date",
				SimpleField.Type.DATE);
		ms_field_validUntil = makeSimpleField("validTil", "Valid_Until",
				SimpleField.Type.DATE);
		ms_field_infoCutoff = makeSimpleField("infoCutoff", "Info_Cutoff",
				SimpleField.Type.STRING);				
		ms_field_lat = makeSimpleField("lat", "Lat", SimpleField.Type.DOUBLE);
		ms_field_lon = makeSimpleField("lon", "Long", SimpleField.Type.DOUBLE);
		ms_field_mgrs = makeSimpleField("mgrs", "MGRS", SimpleField.Type.STRING);
		ms_field_rpt = makeSimpleField("reportType", "Report_Type", SimpleField.Type.STRING);
		ms_field_desc = makeSimpleField("desc", "Description", SimpleField.Type.STRING);
		ms_field_ctx = makeSimpleField("context", "Context",
				SimpleField.Type.STRING);
	}
	
	/**
	 * Create a simple field and set the information
	 * 
	 * @param name
	 * @param displayName
	 * @param type
	 * @return
	 */
	private static SimpleField makeSimpleField(String name, String displayName,
			Type type) {
		SimpleField rval = new SimpleField(name);
		rval.setType(type);
		rval.setDisplayName(StringUtils.isBlank(displayName) ? name
				: displayName);
		ms_fields.add(rval);
		return rval;
	}
	
	@Test public void createFileGDB() throws Exception {
		File temp = File.createTempFile("test", ".zip");
		OutputStream os = new FileOutputStream(temp);
		ZipOutputStream zos = new ZipOutputStream(os);
		File gdb = new File(tempdir, "test" + System.currentTimeMillis() + ".gdb");
		IGISOutputStream gos = GISFactory.getOutputStream(DocumentType.FileGDB,
				zos, gdb);
		Schema s = new Schema();
		s.setId(XFUSION_SCHEMA_PT);
		s.setName(XFUSION_SCHEMA_PT);
		for(SimpleField field : ms_fields) {
			s.put(field);
		}
		gos.write(s);
		
		Schema s2 = new Schema();
		s2.setId(XFUSION_SCHEMA_RING);
		s2.setName(XFUSION_SCHEMA_RING);
		gos.write(s2);

		for(int i = 0; i < 10; i++) {
			gos.write(getFeatureT1());
			gos.write(getFeatureT2());
		}
		
		gos.close();
		zos.close();
		os.close();
	}
	
	@Test public void createShapefile() throws Exception {
		File temp = File.createTempFile("test", ".zip");
		OutputStream os = new FileOutputStream(temp);
		ZipOutputStream zos = new ZipOutputStream(os);
		File sf = new File(tempdir, "test" + System.currentTimeMillis());
		IGISOutputStream gos = GISFactory.getOutputStream(DocumentType.Shapefile,
				zos, sf);
		Schema s = new Schema();
		s.setId(XFUSION_SCHEMA_PT);
		s.setName(XFUSION_SCHEMA_PT);
		for(SimpleField field : ms_fields) {
			s.put(field);
		}
		gos.write(s);

		for(int i = 0; i < 10; i++) {
			gos.write(getFeatureT1());
			gos.write(getFeatureT2());
		}
		
		gos.close();
		zos.close();
		os.close();
	}
	
	public Feature getFeatureT1() {
		Feature f = new Feature();
		f.setName("f1");
		f.setSchema(XFUSION_SCHEMA_PT);
		List<Point> pts = new ArrayList<Point>();
		pts.add(new Point(new Geodetic2DPoint(RandomUtils.JVM_RANDOM)));
		f.setGeometry(new MultiPoint(pts));
		f.putData(ms_field_earliestReportDate, new Date());
		f.putData(ms_field_latestReportDate, new Date());
		f.putData(ms_field_desc, "A random feature");
		f.putData(ms_field_lat, RandomUtils.nextDouble() * 10.0);
		f.putData(ms_field_lon, RandomUtils.nextDouble() * 10.0);
		return f;
	}
	
	public Feature getFeatureT2() {
		Feature f = new Feature();
		f.setName("f1");
		f.setSchema(XFUSION_SCHEMA_PT);
		List<Point> pts = new ArrayList<Point>();
		pts.add(new Point(new Geodetic2DPoint(RandomUtils.JVM_RANDOM)));
		pts.add(new Point(new Geodetic2DPoint(RandomUtils.JVM_RANDOM)));
		pts.add(new Point(new Geodetic2DPoint(RandomUtils.JVM_RANDOM)));
		pts.add(new Point(new Geodetic2DPoint(RandomUtils.JVM_RANDOM)));
		f.putData(ms_field_lat, RandomUtils.nextDouble() * 10.0);
		f.putData(ms_field_lon, RandomUtils.nextDouble() * 10.0);
		Polygon p = new Polygon(new LinearRing(pts));
		f.setGeometry(p);
		f.putData(ms_field_earliestReportDate, new Date());
		f.putData(ms_field_latestReportDate, new Date());
		f.putData(ms_field_desc, "A random feature");
		return f;
	}
}

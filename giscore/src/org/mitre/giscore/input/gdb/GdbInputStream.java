/****************************************************************************************
 *  GdbInputStream.java
 *
 *  Created: Mar 17, 2009
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
package org.mitre.giscore.input.gdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.IAcceptSchema;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.output.esri.ESRIInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.arcgis.datasourcesGDB.AccessWorkspaceFactory;
import com.esri.arcgis.datasourcesGDB.FileGDBWorkspaceFactory;
import com.esri.arcgis.datasourcesfile.ShapefileWorkspaceFactory;
import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.geodatabase.IDataset;
import com.esri.arcgis.geodatabase.IEnumDataset;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IField;
import com.esri.arcgis.geodatabase.IWorkspace;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.QueryFilter;
import com.esri.arcgis.geodatabase.Workspace;
import com.esri.arcgis.geodatabase.esriDatasetType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.Multipoint;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.Ring;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.interop.AutomationException;

/**
 * Opens the GDB or shapefile to return the contained information. The
 * information returned will be the schema and the features from each schema.
 * The output ordering is schema(name) followed by feature(name).
 * <p>
 * The features will be ordered by any passed fields that were specified as part
 * of the ctor, filtered by those actually present in the schema. Only ascending
 * order is supported.
 * 
 * @author DRAND
 */
public class GdbInputStream extends GISInputStreamBase {
	private static Logger logger = LoggerFactory
			.getLogger(GdbInputStream.class);

	private static final String ms_tempDir = System.getProperty("java.io.tmpdir");
	
	private static final AtomicInteger ms_tempDirCounter = new AtomicInteger();
	
	static {
		if(!ESRIInitializer.initialize(false, true)) {
			throw new UnsatisfiedLinkError("Could not initialize ESRI environment.");
		}
	}

	/**
	 * The ESRI workspace factory
	 */
	private IWorkspaceFactory factory = null;

	/**
	 * The workspace
	 */
	private IWorkspace workspace;
	/**
	 * The enumeration of data sets
	 */
	private IEnumDataset datasetenum;
	/**
	 * The current dataset being traversed
	 */
	private IDataset currentDataset = null;
	/**
	 * The current row cursor being fetched or <code>null</code> if we haven't
	 * yet opened a dataset.
	 */
	private IFeatureCursor cursor = null;
	/**
	 * The current schema name, set the in the
	 * {@link #makeSchema(IFeatureClass)} method
	 */
	private URI currentSchemaURI = null;
	/**
	 * The current schema, set in the {@link #makeSchema(IFeatureClass)} method
	 */
	private Schema currentSchema = null;
	/**
	 * Features are named with sequential numbers
	 */
	private long fnumber = 0;

	/**
	 * The accepter, may be null, used to determine if a given schema is wanted
	 */
	private IAcceptSchema accepter;
	
	/**
	 * Temporary directory to hold shapefile or gdb data until close is called.
	 */
	private File tempDir = null;

	/**
	 * Ctor
	 * 
	 * @param type
	 *            the type used
	 * @param stream
	 *            the stream containing a zip archive of the file gdb
	 * @param accepter
	 * 				a function that determines if a schema should be used, may be <code>null</code>
	 * @throws IOException 
	 */
	public GdbInputStream(DocumentType type, InputStream stream, IAcceptSchema accepter) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException(
					"type should never be null");
		}
		if (stream == null) {
			throw new IllegalArgumentException(
					"stream should never be null");
		}
		
		// The stream better point to zip data
		ZipInputStream zipstream = null;
		if (! (stream instanceof ZipInputStream)) {
			zipstream = new ZipInputStream(stream);
		} else {
			zipstream = (ZipInputStream) stream;
		}
		
		tempDir = new File(ms_tempDir, 
				"temp" + ms_tempDirCounter.incrementAndGet() + 
				(DocumentType.FileGDB.equals(type) ? ".gdb" : ""));
		tempDir.mkdirs();
		ZipEntry entry = zipstream.getNextEntry();
		while(entry != null) {
			String name = entry.getName().replace('\\', '/');
			String parts[] = name.split("/");
			File file = new File(tempDir, parts[parts.length - 1]);
			FileOutputStream fos = new FileOutputStream(file);
			IOUtils.copy(zipstream, fos);
			IOUtils.closeQuietly(fos);
			entry = zipstream.getNextEntry();
		}
		initialize(type, tempDir, accepter);
	}

	/**
	 * Ctor
	 * 
	 * @param type
	 *            the type used
	 * @param file
	 *            the location of the file GDB or of the shapefile
	 * @param accepter
	 * 				a function that determines if a schema should be used,
	 * may be <code>null</code>
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public GdbInputStream(DocumentType type, File file, IAcceptSchema accepter)
			throws UnknownHostException, IOException {
		if (type == null) {
			throw new IllegalArgumentException(
					"type should never be null");
		}
		if (file == null) {
			throw new IllegalArgumentException(
					"file should never be null");
		}
		initialize(type, file, accepter);
	}

	/**
	 * Initialize the input stream
	 * @param type
	 * @param file
	 * @param accepter
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws AutomationException
	 */
	private void initialize(DocumentType type, File file, IAcceptSchema accepter)
			throws IOException, UnknownHostException, AutomationException {
		if (type.equals(DocumentType.FileGDB)) {
			factory = new FileGDBWorkspaceFactory();
		} else if (type.equals(DocumentType.PersonalGDB)) {
			factory = new AccessWorkspaceFactory();
		} else if (type.equals(DocumentType.Shapefile)) {
			factory = new ShapefileWorkspaceFactory();
		} else {
			throw new IllegalArgumentException("Unhandled format " + type);
		}
		
		this.accepter = accepter;

		workspace = factory.openFromFile(file.getAbsolutePath(), 0);

		datasetenum = workspace.getDatasets(esriDatasetType.esriDTFeatureClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.input.IGISInputStream#close()
	 */
	public void close() {
		((Workspace) workspace).release();
		if (tempDir != null && tempDir.exists()) {
			tempDir.delete();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.input.IGISInputStream#read()
	 */
	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return super.readSaved();
		} else if (datasetenum == null) {
			return null;
		}
		try {
			if (currentDataset == null) {
				currentDataset = datasetenum.next();
				if (currentDataset == null) {
					datasetenum = null;
					return read();
				} else {
					FeatureClass fclass = new FeatureClass(currentDataset);
					QueryFilter filter = new QueryFilter();
					cursor = fclass.search(filter, true);
					Schema s = makeSchema(fclass);
					if (accepter == null || accepter.accept(s)) {
						return s;
					} else {
						currentDataset = null;
						return read();
					}
				}
			} else {
				IFeature feature = cursor.nextFeature();
				if (feature == null) {
					currentDataset = null;
					currentSchema = null;
					cursor = null;
					return read();
				} else {
					return makeFeature(feature);
				}
			}
		} catch (Exception e) {
			final IOException e2 = new IOException("Problem reading data from database");
			e2.initCause(e);
			throw e2;
		}
	}

	/**
	 * @param ifeature
	 * @return
	 * @throws IOException
	 * @throws AutomationException
	 */
	private IGISObject makeFeature(IFeature ifeature)
			throws AutomationException, IOException {
		Feature feature = new Feature();
		feature.setSchema(currentSchemaURI);
		IGeometry geo = ifeature.getShape();
		int gtype = geo.getGeometryType();
		feature.setGeometry(makeGeometry(gtype, geo));
		for (String fieldname : currentSchema.getKeys()) {
			SimpleField field = currentSchema.get(fieldname);
			// Geometry handled above
			if (field != null && !field.getType().equals(Type.GEOMETRY)) {
				Object javaobject = ifeature.getValue(field.getIndex());
				if (javaobject != null) {
					feature.putData(field, javaobject);
				}
			}
		}
		feature.setName("f" + fnumber++);
		return feature;
	}

	/**
	 * @param gtype
	 * @param geo
	 * @return
	 * @throws IOException
	 * @throws AutomationException
	 */
	private Geometry makeGeometry(int gtype, IGeometry geo)
			throws AutomationException, IOException {
		List<Point> points = null;
		IPoint point = null;
		switch (gtype) {
		case esriGeometryType.esriGeometryPoint:
			return makePoint((IPoint) geo);
		case esriGeometryType.esriGeometryPolyline:
			Polyline line = (Polyline) geo;
			points = new ArrayList<Point>();
			for (int i = 0; i < line.getPointCount(); i++) {
				point = line.getPoint(i);
				points.add(makePoint(point));
			}
			return new Line(points);
		case esriGeometryType.esriGeometryRing:
			return makeRing((Ring) geo);
		case esriGeometryType.esriGeometryPolygon:
			return makePolygon((com.esri.arcgis.geometry.Polygon) geo);
		case esriGeometryType.esriGeometryBag:
			return makeGeometryBag((com.esri.arcgis.geometry.GeometryBag) geo);
		case esriGeometryType.esriGeometryMultipoint:
			Multipoint mp = (Multipoint) geo;
			List<Point> pts = new ArrayList<Point>();
			for (int i = 0; i < mp.getGeometryCount(); i++) {
				pts.add(makePoint((IPoint) mp.getGeometry(i)));
			}
			return new MultiPoint(pts);
		default:
			throw new UnsupportedOperationException("Geometry type " + gtype
					+ " unknown");
		}
	}

	/**
	 * Make a geometry bag from the esri geometry bag
	 * 
	 * @param geo
	 *            the esri bag, assumed not <code>null</code>
	 * @return a giscore geometry bag, never <code>null</code>
	 * @throws IOException
	 * @throws AutomationException
	 */
	private GeometryBag makeGeometryBag(com.esri.arcgis.geometry.GeometryBag bag)
			throws IOException, AutomationException {
		GeometryBag rval = new GeometryBag(new ArrayList<Geometry>());
		for (int i = 0; i < bag.getGeometryCount(); i++) {
			IGeometry bgeo = bag.getGeometry(i);
			rval.add(makeGeometry(bgeo.getGeometryType(), bgeo));
		}
		return rval;
	}

	/**
	 * Make a point from a point geometry object
	 * 
	 * @param point
	 *            the point, assumed not <code>null</code>
	 * @return a giscore point object, never <code>null</code>
	 * @throws IOException
	 * @throws AutomationException
	 */
	private Point makePoint(IPoint point) throws IOException,
			AutomationException {
		return new Point(point.getY(), point.getX());
	}

	/**
	 * Make a simple ring from an esri ring
	 * 
	 * @param ring
	 *            the esri ring, assumed not <code>null</code>
	 * @return a giscore ring, never <code>null</code>
	 * @throws IOException
	 * @throws AutomationException
	 */
	private LinearRing makeRing(Ring ring) throws IOException,
			AutomationException {
		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < ring.getPointCount(); i++) {
			IPoint point = ring.getPoint(i);
			points.add(new Point(point.getY(), point.getX()));
		}
		LinearRing rval = new LinearRing(points);
		return rval;
	}

	/**
	 * Make a complete poly. Each poly may have multiple outer rings, and each
	 * of those may have multiple inner rings
	 * 
	 * @param poly
	 * @return
	 * @throws IOException
	 * @throws AutomationException
	 */
	private Geometry makePolygon(com.esri.arcgis.geometry.Polygon poly)
			throws IOException, AutomationException {
		com.esri.arcgis.geometry.GeometryBag bag = (com.esri.arcgis.geometry.GeometryBag) poly
				.getOutermostComponentBag();
		if (bag.getGeometryCount() == 1) {
			Ring gouter = (Ring) bag.getGeometry(0);
			return makeSimplePoly(poly, gouter);
		} else if (bag.getGeometryCount() == 0) {
			throw new IllegalStateException(
					"Poly must have at least one outer ring");
		} else {
			List<Polygon> polyList = new ArrayList<Polygon>();
			for (int i = 0; i < bag.getGeometryCount(); i++) {
				Ring gouter = (Ring) bag.getGeometry(i);
				polyList.add(makeSimplePoly(poly, gouter));
			}
			return new MultiPolygons(polyList);
		}
	}

	/**
	 * Make a simple poly with a single outer ring
	 * 
	 * @param poly
	 * @param outerRing
	 * @return
	 * @throws IOException
	 * @throws AutomationException
	 */
	private Polygon makeSimplePoly(com.esri.arcgis.geometry.Polygon poly,
			Ring outerRing) throws IOException, AutomationException {
		LinearRing outer = makeRing(outerRing);
		List<LinearRing> inners = new ArrayList<LinearRing>();
		com.esri.arcgis.geometry.GeometryBag ibag = (com.esri.arcgis.geometry.GeometryBag) poly
				.getInteriorRingBag(outerRing);
		for (int i = 0; i < ibag.getGeometryCount(); i++) {
			Ring ginner = (Ring) ibag.getGeometry(i);
			inners.add(makeRing(ginner));
		}
		return new Polygon(outer, inners);
	}

	/**
	 * Make a schema from the feature class's description. The schema attempts
	 * to capture all the relevant information about the feature class. Some
	 * information, such as the OID and Shape fields are captured implicitly in
	 * the typing of the schema's simple field objects.
	 * <p>
	 * If a feature class field has an unknown type it will be omitted from the
	 * schema
	 * 
	 * @return a schema object, never <code>null</code>
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws AutomationException
	 */
	private Schema makeSchema(FeatureClass fclass)
			throws AutomationException, URISyntaxException, IOException {
		currentSchemaURI = new URI("urn:" + fclass.getAliasName());
		currentSchema = new Schema(currentSchemaURI);
		currentSchema.setName(fclass.getAliasName());
		int fieldCount = fclass.getFields().getFieldCount();
		for (int i = 0; i < fieldCount; i++) {
			IField field = fclass.getFields().getField(i);
			String name = field.getName();
			Type type = decodeType(field.getType());
			if (type != null) {
				SimpleField sfield = new SimpleField(name);
				sfield.setType(type);
				sfield.setAliasName(field.getAliasName());
				sfield.setLength(field.getLength());
				sfield.setPrecision(field.getPrecision());
				sfield.setIndex(i);
				currentSchema.put(name, sfield);
			} else {
				logger.warn("Could not add field " + name + " of type "
						+ field.getType());
			}
		}
		return currentSchema;
	}

	/**
	 * Map the numeric esri type back to the type enum in simple field.
	 * 
	 * @param type
	 *            the type
	 * @return the simple field type or <code>null</code> if no type matches
	 */
	private Type decodeType(int type) {
		switch (type) {
		case esriFieldType.esriFieldTypeSingle:
			return Type.FLOAT;
		case esriFieldType.esriFieldTypeDate:
			return Type.DATE;
		case esriFieldType.esriFieldTypeDouble:
			return Type.DOUBLE;
		case esriFieldType.esriFieldTypeGeometry:
			return Type.GEOMETRY;
		case esriFieldType.esriFieldTypeInteger:
			return Type.INT;
		case esriFieldType.esriFieldTypeOID:
			return Type.OID;
		case esriFieldType.esriFieldTypeSmallInteger:
			return Type.SHORT;
		case esriFieldType.esriFieldTypeString:
			return Type.STRING;
		default:
			return null;
		}
	}
}

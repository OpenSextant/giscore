/****************************************************************************************
 *  EsriXmlGdbOutputStream.java
 *
 *  Created: Feb 16, 2009
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
package org.mitre.giscore.output.esri;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
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
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.FeatureSorter;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.giscore.utils.SafeDateFormat;
import org.mitre.giscore.utils.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.arcgis.datasourcesGDB.AccessWorkspaceFactory;
import com.esri.arcgis.datasourcesGDB.FileGDBWorkspaceFactory;
import com.esri.arcgis.datasourcesGDB.SdeWorkspaceFactory;
import com.esri.arcgis.datasourcesfile.ShapefileWorkspaceFactory;
import com.esri.arcgis.geodatabase.Field;
import com.esri.arcgis.geodatabase.FieldChecker;
import com.esri.arcgis.geodatabase.Fields;
import com.esri.arcgis.geodatabase.GeometryDef;
import com.esri.arcgis.geodatabase.IEnumFieldError;
import com.esri.arcgis.geodatabase.IFeatureBuffer;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IFeatureWorkspace;
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IField;
import com.esri.arcgis.geodatabase.IFieldError;
import com.esri.arcgis.geodatabase.IFields;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.IWorkspaceName;
import com.esri.arcgis.geodatabase.Workspace;
import com.esri.arcgis.geodatabase.WorkspaceFactory;
import com.esri.arcgis.geodatabase.esriFeatureType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.ISpatialReference;
import com.esri.arcgis.geometry.Multipoint;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.Ring;
import com.esri.arcgis.geometry.SpatialReferenceEnvironment;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.geometry.esriSRGeoCSType;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.Cleaner;
import com.esri.arcgis.interop.Variant;
import com.esri.arcgis.system.IName;
import com.esri.arcgis.system.IUID;
import com.esri.arcgis.system.PropertySet;

/**
 * Output the GIS information using the ArcObjects Java API. This results in a
 * file GDB, which is stored in a directory containing multiple individual
 * files. The result is packaged into the output stream using ZIP encoding.
 * 
 * @author DRAND
 */
public class GdbOutputStream extends StreamVisitorBase implements
		IGISOutputStream {
	private static final Logger logger = LoggerFactory
			.getLogger(GdbOutputStream.class);

	private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private SafeDateFormat dateFormatter;

	/**
	 * Checker to use for dataset names
	 */
	private FieldChecker checker = null;

	/**
	 * Reference environment to create geospatial references
	 */
	private static SpatialReferenceEnvironment spatialRefEnv = null;

	private static SimpleField shape = null;

	static {
		shape = new SimpleField("INT_SHAPE");
		shape.setType(SimpleField.Type.GEOMETRY);
		shape.setLength(0);
		shape.setRequired(true);

		ESRIInitializer.initialize(false, true);

		try {
			spatialRefEnv = new SpatialReferenceEnvironment();
		} catch (Exception e) {
			logger.error("Problem creating spatial reference environment", e);
		}
	}

	/**
	 * The feature sorter takes care of the details of storing features for
	 * later retrieval by schema.
	 */
	private FeatureSorter sorter = new FeatureSorter();

	/**
	 * Tracks the path - useful for naming collections
	 */
	private Stack<String> path = new Stack<String>();

	/**
	 * The first time we find a particular feature key, we store away the path
	 * and geometry type as a name. Not perfect, but at least it will be
	 * somewhat meaningful.
	 */
	private Map<FeatureKey, String> datasets = new HashMap<FeatureKey, String>();

	/**
	 * The workspace factory, never <code>null</code> after successful
	 * construction.
	 */
	private IWorkspaceFactory factory = null;

	/**
	 * Spatial reference, initialized in the ctor and unchanging afterward
	 */
	private ISpatialReference spatialRef = null;

	/**
	 * The workspace, initialized in the ctor and unchanging afterward.
	 */
	private Workspace workspace;

	/**
	 * The feature workspace, used to create types and store data, initialized
	 * in the ctor and unchanging afterward.
	 */
	private IFeatureWorkspace featureWorkspace;

	/**
	 * The output stream, used at the end to store away the generated file or
	 * directory, initialized in the ctor and unchanging afterward.
	 */
	private OutputStream outputStream;

	/**
	 * This stores whether the result of the output is a file or a directory,
	 * which affects how the eventual output is rendered to the stream.
	 * Directory output implies having some sort of archive or ZIP output.
	 */
	private boolean isdir = false;

	/**
	 * Set to <code>true</code> for shapefiles since they require special
	 * handling of fieldnames
	 */
	private boolean isshapefile = false;

	/**
	 * The path to the file or directory being created by this stream.
	 */
	private File outputPath = null;

	/**
	 * The container naming strategy. If not supplied in the ctor then
	 * {@link BasicContainerNameStrategy} will be used.
	 */
	private IContainerNameStrategy containerNameStrategy = null;

	/**
	 * Map that allows the software to go from esri field objects back to the
	 * {@link SimpleField} object needed to look up data for the extended data
	 * in the features.
	 */
	private Map<FeatureKey, Map<String, SimpleField>> fieldmap = new HashMap<FeatureKey, Map<String, SimpleField>>();

	/**
	 * Ctor
	 * 
	 * @param type
	 *            the format to use, never <code>null</code>. ESRI has a number
	 *            of workspace types, which can be varied to output the
	 *            different formats of GDB.
	 * @param stream
	 *            the output stream to write the resulting GDB into, never
	 *            <code>null</code>.
	 * @param path
	 *            the directory and file that should hold the file gdb, never
	 *            <code>null</code>.
	 * @param containerNameStrategy
	 *            a name strategy to override the default, may be
	 *            <code>null</code>.
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public GdbOutputStream(DocumentType type, OutputStream stream, File path,
			IContainerNameStrategy containerNameStrategy) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		if (path == null || !path.getParentFile().exists()) {
			throw new IllegalArgumentException(
					"path should never be null and parent must exist");
		}
		if (containerNameStrategy == null) {
			this.containerNameStrategy = new BasicContainerNameStrategy();
		} else {
			this.containerNameStrategy = containerNameStrategy;
		}
		outputStream = stream;
		outputPath = path;
		if (type.equals(DocumentType.FileGDB)) {
			factory = new FileGDBWorkspaceFactory();
			isdir = true;
		} else if (type.equals(DocumentType.PersonalGDB)) {
			factory = new AccessWorkspaceFactory();
			isdir = false;
		} else if (type.equals(DocumentType.Shapefile)) {
			factory = new ShapefileWorkspaceFactory();
			isdir = true;
			isshapefile = true;
		} else {
			throw new IllegalArgumentException("Unhandled format " + type);
		}

		factory = new WorkspaceFactory(factory);
		PropertySet pset = new PropertySet();
		IWorkspaceName workspaceName = factory.create(path.getParent(), path.getName(), pset,
				0);
		IName name = (IName) workspaceName;
		workspace = new Workspace(name.open());
		setupWorkspace();
	}

	private void setupWorkspace() throws IOException, AutomationException {
		featureWorkspace = new IFeatureWorkspaceProxy(workspace);
		spatialRef = spatialRefEnv
				.createGeographicCoordinateSystem(esriSRGeoCSType.esriSRGeoCS_WGS1984);
		spatialRef.setDomain(-180.0, 180.0, -90.0, 90.0);
	}

	/**
	 * Ctor for creating an output stream for an SDE
	 * 
	 * <blockquote> <em>From the ESRI javadoc</em>
	 * <p>
	 * List of acceptable connection property names and a brief description of
	 * each:
	 * <table>
	 * <tr><th align="left">SERVER</th><td>SDE server name you are connecting to.</td></tr>
	 * <tr><th align="left">INSTANCE</th><td>Instance you are connection to.</td></tr>
	 * <tr><th align="left">DATABASE</th><td>Database connected to.</td></tr>
	 * <tr><th align="left">USER</th><td>Connected user.</td></tr>
	 * <tr><th align="left">PASSWORD</th><td>Connected password.</td></tr>
	 * <tr><th align="left">AUTHENTICATION_MODE</th><td>Credential authentication mode of the
	 * connection. Acceptable values are "OSA" and "DBMS".</td></tr>
	 * <tr><th align="left">VERSION</th><td>Transactional version to connect to. Acceptable value is
	 * a string that represents a transaction version name.</td></tr>
	 * <tr><th align="left">HISTORICAL_NAME</th><td>Historical version to connect to. Acceptable
	 * value is a string type that represents a historical marker name.</td></tr>
	 * <tr><th align="left">HISTORICAL_TIMESTAMP</th><td>Moment in history to establish an historical
	 * version connection. Acceptable value is a date time that represents a
	 * moment timestamp.</td></tr>
	 * </table>
	 * Notes:
	 * <p>
	 * The <Q>DATABASE</Q> property is optional and is required for ArcSDE instances
	 * that manage multiple databases (for example, SQL Server).
	 * <p>
	 * If <Q>AUTHENTICATION_MODE</Q> is <Q>OSA</Q> then <Q>USER</Q> and <Q>PASSWORD</Q> are not
	 * required. <Q>OSA</Q> represents operating system authentication and uses the
	 * operating system credentials to establish a connection with the database.
	 * <p>
	 * Since the workspace connection can only represent one version only 1 of
	 * the 3 version properties (<Q>VERSION</Q> or <Q>HISTORICAL_NAME</Q> or
	 * <Q>HISTORICAL_TIMESTAMP</Q>) should be used. </blockquote>
	 * 
	 * @param properties
	 *            the properties required by the open call
	 * @param containerNameStrategy a name strategy to override the default, may be
	 *            <code>null</code>.  
	 * @throws IOException 
	 */
	public GdbOutputStream(Properties properties,
			IContainerNameStrategy containerNameStrategy) throws IOException {
		if (containerNameStrategy == null) {
			this.containerNameStrategy = new BasicContainerNameStrategy();
		} else {
			this.containerNameStrategy = containerNameStrategy;
		}
		factory = new SdeWorkspaceFactory();
		
		PropertySet set = new PropertySet();
		for(Object key : properties.keySet()) {
			String keystr = (String) key;
			set.setProperty(keystr, properties.getProperty(keystr));
		}
		
		workspace = new Workspace(factory.open(set, 0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.output.IGISOutputStream#close()
	 */
	public void close() throws IOException {
		sorter.close();
		for (FeatureKey key : sorter.keys()) {
			IFeatureClass fc = addSchema(key);
			if (fc != null) {
				addData(key, fc);
			}
		}
		sorter.cleanup();
		workspace.release();

		// Output file or directory to output stream
		if (isdir) {
			if (!(outputStream instanceof ZipOutputStream)) {
				throw new IllegalStateException(
						"Can't output multi-component output without a zip output stream");
			}
			ZipUtils.outputZipComponents(outputPath.getName(), outputPath,
					(ZipOutputStream) outputStream);
		} else {
			InputStream is = null;
			try {
				is = new FileInputStream(outputPath);
				IOUtils.copy(is, outputStream);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
	}

	/**
	 * This gets the feature data and stores it to the database.
	 * 
	 * @param key
	 *            the key being handled
	 * @param fc
	 *            the feature class, never <code>null</code>
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private void addData(FeatureKey key, IFeatureClass fc) throws IOException {
		ObjectBuffer obuffer = sorter.getBuffer(key);
		Map<String, SimpleField> etosf = fieldmap.get(key);
		IFeatureBuffer buffer = fc.createFeatureBuffer();
		IFeatureCursor cursor = fc.IFeatureClass_insert(true);
		try {
			Feature current;
			int oid = 0;
			while ((current = (Feature) obuffer.read()) != null) {
				IFields fields = fc.getFields();
				for (int i = 0; i < fields.getFieldCount(); i++) {
					IField field = fields.getField(i);
					if (!field.isEditable())
						continue;
					if (fc.getShapeFieldName().equals(field.getName())) {
						if (current.getGeometry() != null) {
							IGeometry geo = makeFeatureGeometry(current
									.getGeometry());
							buffer.setShapeByRef(geo);
						}
					} else if (fc.getOIDFieldName().equals(field.getName())) {
						Variant oval = createVariant(field.getType(), field
								.getLength(), field.getName(), oid++);
						buffer.setValue(i, oval);
					} else {
						if (etosf == null) {
							throw new IllegalStateException(
									"Found fields without map");
						}
						SimpleField gisfield = etosf.get(field.getName());
						if (gisfield != null) {
							Object value = current.getData(gisfield);
							Variant vval = createVariant(field.getType(), field
									.getLength(), field.getName(), value);
							buffer.setValue(i, vval);
						} else {
							logger
									.debug("Found field without corresponding schema info: "
											+ field.getName());
						}
					}
				}
				cursor.insertFeature(buffer);
			}
		} catch (RuntimeException e) {
			logger.error("Problem creating data record", e);
			throw e;
		} catch (EOFException e) {
			// Ignore
		} catch (ClassNotFoundException e) {
			logger.error("Referenced class not available", e);
		} catch (InstantiationException e) {
			logger.error("Referenced class not available (instantiation)", e);
		} catch (IllegalAccessException e) {
			logger.error("Referenced class not available (access)", e);
		} finally {
			cursor.flush();
			Cleaner.release(cursor);
		}

	}

	/**
	 * @param geo
	 *            Geometry to be made into a Feature
	 * @return resulting IGeometry object
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private IGeometry makeFeatureGeometry(Geometry geo) throws IOException {
		IGeometry rval;
		if (geo instanceof Point) {
			rval = makePoint((Point) geo);
		} else if (geo instanceof Line) {
			rval = makeLine((Line) geo);
		} else if (geo instanceof LinearRing) {
			rval = makeRing((LinearRing) geo);
		} else if (geo instanceof Polygon) {
			Polygon poly = (Polygon) geo;
			com.esri.arcgis.geometry.Polygon epoly = new com.esri.arcgis.geometry.Polygon();
			if (poly.getOuterRing() != null) {
				epoly.addGeometry(makeRing(poly.getOuterRing()), null, null);
			}
			for (LinearRing ir : poly.getLinearRings()) {
				epoly.addGeometry(makeRing(ir), null, null);
			}
			rval = epoly;
		} else if (geo instanceof GeometryBag) {
			throw new UnsupportedOperationException(
					"Geometry bags are currently not working with ESRI output formats");
		} else if (geo instanceof MultiPoint) {
			MultiPoint gismp = (MultiPoint) geo;
			Multipoint mp = new Multipoint();
			for (Point p : gismp.getPoints()) {
				mp.addPoint(makePoint(p), 0, null);
			}
			rval = mp;
		} else {
			throw new UnsupportedOperationException(
					"Found unknown type of geometry: " + geo.getClass());
		}
		return rval;
	}

	/**
	 * @param ring
	 *            LinearRing to be output as an ESRI object
	 * @return resulting ESRI Ring object
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private Ring makeRing(LinearRing ring) throws IOException {
		Ring rval = new Ring();
		for (Point p : ring.getPoints()) {
			rval.addPoint(makePoint(p), null, null);
		}
		return rval;
	}

	/**
	 * @param line
	 *            Line to be output as an ESRI object
	 * @return resulting Polyline object
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private Polyline makeLine(Line line) throws IOException {
		Polyline pline = new Polyline();
		for (Point p : line.getPoints()) {
			pline.addPoint(makePoint(p), null, null);
		}
		return pline;
	}

	/**
	 * @param point
	 *            Point to be output as an ESRI object
	 * @return resulting ESRI Point object
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private com.esri.arcgis.geometry.Point makePoint(Point point)
			throws IOException {
		com.esri.arcgis.geometry.Point cpoint = new com.esri.arcgis.geometry.Point();
		cpoint.setX(point.getCenter().getLongitude().inDegrees());
		cpoint.setY(point.getCenter().getLatitude().inDegrees());
		return cpoint;
	}

	// Thread-safe date formatter helper method
	private SafeDateFormat getDateFormatter() {
		if (dateFormatter == null) {
			SafeDateFormat thisDateFormatter = new SafeDateFormat(ISO_DATE_FMT);
			thisDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormatter = thisDateFormatter;
		}
		return dateFormatter;
	}

	/**
	 * Create a variant of the given object datum
	 * 
	 * @param type
	 *            integer type of Variant
	 * @param size
	 *            integer size of Variant
	 * @param name
	 *            the name, never <code>null</code> or empty
	 * @param value
	 *            the object datum
	 * @return the appropriate variant
	 */
	private Variant createVariant(int type, int size, String name, Object value) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException(
					"name should never be null or empty");
		}
		if (value == null || ObjectUtils.NULL.equals(value)) {
			return new Variant(name);
		}

		switch (type) {
		case esriFieldType.esriFieldTypeSmallInteger:
			return makeIntegerVariant(name, 2, Variant.VT_I2, value);
		case esriFieldType.esriFieldTypeInteger:
			return makeIntegerVariant(name, 4, Variant.VT_I4, value);
		case esriFieldType.esriFieldTypeSingle:
			return makeFloatVariant(name, true, Variant.VT_R4, value);
		case esriFieldType.esriFieldTypeDouble:
			return makeFloatVariant(name, false, Variant.VT_R8, value);
		case esriFieldType.esriFieldTypeDate:
			if (value instanceof Date) {
				return new Variant(name, Variant.VT_DATE, (Date) value);
			} else if (value instanceof String) {
				throw new IllegalArgumentException("No date format available");
			} else {
				throw new IllegalArgumentException("Uncoercible type found "
						+ value.getClass());
			}
		case esriFieldType.esriFieldTypeOID:
			return makeIntegerVariant(name, 4, Variant.VT_INT, value);
		case esriFieldType.esriFieldTypeString:
			if (value instanceof Date) {
				return new Variant(name, Variant.VT_BSTR, getDateFormatter()
						.format((Date) value));
			} else {
				String str = value.toString();
				if (str.length() > size) {
					str = str.substring(0, size - 1);
				}
				return new Variant(name, Variant.VT_BSTR, str);
			}
		default:
			throw new IllegalStateException("Found unsupported type: " + type);
		}
	}

	private Variant makeFloatVariant(String name, boolean single, int ft,
			Object value) {
		if (value instanceof Number) {
			if (single)
				return new Variant(name, ft, ((Number) value).floatValue());
			else
				return new Variant(name, ft, ((Number) value).doubleValue());
		} else if (value instanceof String) {
			if (single)
				return new Variant(name, ft, new Float((String) value)
						.floatValue());
			else
				return new Variant(name, ft, new Double((String) value)
						.doubleValue());
		} else {
			throw new IllegalArgumentException("Uncoercible type found "
					+ value.getClass());
		}
	}

	private Variant makeIntegerVariant(String name, int bytes, int ft,
			Object value) {
		if (value instanceof Number) {
			if (bytes == 2)
				return new Variant(name, ft, ((Number) value).shortValue());
			else
				return new Variant(name, ft, ((Number) value).intValue());
		} else if (value instanceof Boolean) {
			return new Variant(name, ft, (short) (((Boolean) value) ? 1 : 0));
		} else if (value instanceof String) {
			if (bytes == 2)
				return new Variant(name, ft, new Short((String) value)
						.shortValue());
			else
				return new Variant(name, ft, new Integer((String) value)
						.intValue());
		} else {
			throw new IllegalArgumentException("Uncoercible type found "
					+ value.getClass());
		}
	}

	/**
	 * @param key
	 *            FeatureKey used to get schema
	 * @throws IOException
	 *             if an IO error occurs
	 * @return resulting IFeatureClass object
	 */
	private IFeatureClass addSchema(FeatureKey key) throws IOException {
		if (key.getGeoclass() == null) {
			return null; // Probably an overlay feature, skip
		}
		Schema schema = key.getSchema();
		if (schema == null) {
			throw new IllegalArgumentException("Missing schema from key");
		}
		SimpleField shapeField = schema.getShapeField();
		String dsname = datasets.get(key);
		IUID clsid = null;
		IUID extclsid = null;
		int featureType = esriFeatureType.esriFTSimple;
		String shapeFieldName = shape != null ? shape.getName() : null;
		String configKeyword = null;
		Set<String> fieldnames = new HashSet<String>();

		Fields fields = new Fields();
		for (String fieldname : schema.getKeys()) {
			fields
					.addField(createField(key, schema.get(fieldname),
							fieldnames));
		}
		if (shapeField == null) {
			fields.addField(createField(key, shape, fieldnames));
		}
		loadChecker();
		IEnumFieldError[] errors = new IEnumFieldError[1];
		IFields[] fixedFields = new IFields[1];
		checker.validate(fields, errors, fixedFields);

		if (errors[0] != null) {
			IFieldError err = errors[0].next();
			logger.error("Schema fields: " + schema.getKeys());
			while(err != null) {
				logger.error("Field problem for field index " + err.getFieldIndex()
						+ " error " + err.getFieldError());
				err = errors[0].next();
			}
			throw new RuntimeException(
					"There are one or more invalid field names for data set "
							+ dsname);
		}

		return featureWorkspace.createFeatureClass(dsname, fields, clsid,
				extclsid, featureType, shapeFieldName, configKeyword);
	}

	/**
	 * Method to check the dataset name for validity with ESRI's checker
	 * 
	 * @param dsname
	 *            input dataset name String
	 * @return fixed dataset name String
	 */
	private String fixDatasetName(String dsname) {
		loadChecker();
		String fixed[] = new String[1];
		try {
			if (checker.validateTableName(dsname, fixed) != 0) {
				return fixed[0];
			}
		} catch (Exception e) {
			logger.error("Validation failed badly", e);
		}
		return dsname;
	}

	// Helper method to load the field checker into local instance variable
	private void loadChecker() {
		if (checker == null) {
			try {
				checker = new FieldChecker();
				checker.setInputWorkspace(workspace);
			} catch (Exception e) {
				throw new RuntimeException(
						"Failed to instantiate the field checker", e);
			}
		}
	}

	/**
	 * Create an ESRI field from a simple field and store the relationship for
	 * later use.
	 * 
	 * @param key
	 *            FeatureKey needed for field context
	 * @param simpleField
	 *            field to be converted
	 * @param fieldnames
	 *            the fieldnames already in use
	 * @return resulting IField object
	 * @throws IOException
	 *             if an IO error occurs
	 */
	private IField createField(FeatureKey key, SimpleField simpleField,
			Set<String> fieldnames) throws IOException {
		Field field = new Field();
		String fieldname = makeUniqueFieldname(simpleField, fieldnames);
		field.setName(fieldname);
		if (simpleField.getDisplayName() != null) {
			field.setAliasName(simpleField.getDisplayName());
		}
		if (isshapefile && Type.DATE.equals(simpleField.getType())) {
			field.setType(esriFieldType.esriFieldTypeString);
			field.setLength(26);
		} else {
			field.setType(getEsriFieldType(simpleField.getType()));
			if (simpleField.getLength() != null)
				field.setLength(simpleField.getLength());
			else
				field.setLength(simpleField.getType().getDefaultLength());
		}

		field.setIsNullable(simpleField.isNullable());
		field.setRequired(simpleField.isRequired());
		field.setEditable(simpleField.isEditable());
		if (simpleField.getPrecision() != null)
			field.setPrecision(simpleField.getPrecision());
		else
			field.setPrecision(simpleField.getType().getDefaultPrecision());
		if (simpleField.getScale() != null)
			field.setScale(simpleField.getScale());
		if (Type.GEOMETRY.equals(simpleField.getType())) {
			// Add geometry definition
			GeometryDef def = new GeometryDef();
			def.setAvgNumPoints(1);
			def.setGeometryType(getEsriGeoType(key.getGeoclass()));
			def.setSpatialReferenceByRef(spatialRef);
			def.setHasM(false);
			def.setHasZ(false);
			field.setGeometryDefByRef(def);
		}
		Map<String, SimpleField> etosf = fieldmap.get(key);
		if (etosf == null) {
			etosf = new HashMap<String, SimpleField>();
			fieldmap.put(key, etosf);
		}
		etosf.put(fieldname, simpleField);
		return field;
	}

	/**
	 * Make a unique fieldname considering whether we're outputting a shapefile.
	 * For non-shapefiles the fieldnames must be unique already.
	 * 
	 * @param simpleField
	 *            simpleField whose name is being made unique
	 * @param fieldnames
	 *            other field names already in use
	 * @return String unique form of input field name
	 */
	private String makeUniqueFieldname(SimpleField simpleField,
			Set<String> fieldnames) {
		String fieldname = simpleField.getName();
		if (isshapefile) {
			int i = 0;
			String basefieldname;
			if (fieldname.length() > 10) {
				basefieldname = fieldname.substring(0, 10);
			} else {
				basefieldname = fieldname;
			}
			while (fieldnames.contains(basefieldname)) {
				if (i > 99) {
					throw new IllegalStateException(
							"Couldn't form unique fieldname for " + fieldname);
				}
				if (fieldname.length() > 8) {
					basefieldname = fieldname.substring(0, 8) + i++;
				} else {
					basefieldname = fieldname + i++;
				}
			}
			fieldname = basefieldname;
		} else if (fieldnames.contains(fieldname)) {
			throw new IllegalStateException("Fieldname " + fieldname
					+ " isn't unique");
		}
		fieldnames.add(fieldname);
		return fieldname;
	}

	/**
	 * @param type
	 *            Type to be converted to ESRI field type
	 * @return equivalent ESRI field type
	 */
	private int getEsriFieldType(Type type) {
		if (Type.BOOL.equals(type)) {
			return esriFieldType.esriFieldTypeSmallInteger;
		} else if (Type.DATE.equals(type)) {
			return esriFieldType.esriFieldTypeDate;
		} else if (Type.DOUBLE.equals(type)) {
			return esriFieldType.esriFieldTypeDouble;
		} else if (Type.FLOAT.equals(type)) {
			return esriFieldType.esriFieldTypeSingle;
		} else if (Type.GEOMETRY.equals(type)) {
			return esriFieldType.esriFieldTypeGeometry;
		} else if (Type.INT.equals(type)) {
			return esriFieldType.esriFieldTypeInteger;
		} else if (Type.OID.equals(type)) {
			return esriFieldType.esriFieldTypeOID;
		} else if (Type.SHORT.equals(type)) {
			return esriFieldType.esriFieldTypeSmallInteger;
		} else if (Type.STRING.equals(type)) {
			return esriFieldType.esriFieldTypeString;
		} else if (Type.UINT.equals(type)) {
			return esriFieldType.esriFieldTypeInteger;
		} else if (Type.USHORT.equals(type)) {
			return esriFieldType.esriFieldTypeSmallInteger;
		} else {
			throw new IllegalArgumentException("Found unknown type " + type);
		}
	}

	/**
	 * @param geoclass
	 *            Geometry class to be converted to ESRI geometry type
	 * @return equivalent ESRI geometry type
	 */
	private int getEsriGeoType(Class<? extends Geometry> geoclass) {
		if (geoclass == null)
			return 0;

		if (geoclass.isAssignableFrom(Point.class)) {
			return esriGeometryType.esriGeometryPoint;
		} else if (geoclass.isAssignableFrom(Line.class)) {
			return esriGeometryType.esriGeometryPolyline;
		} else if (geoclass.isAssignableFrom(LinearRing.class)) {
			return esriGeometryType.esriGeometryRing;
		} else if (geoclass.isAssignableFrom(Polygon.class)) {
			return esriGeometryType.esriGeometryPolygon;
		} else if (geoclass.isAssignableFrom(GeometryBag.class)) {
			return esriGeometryType.esriGeometryBag;
		} else if (geoclass.isAssignableFrom(MultiPoint.class)) {
			return esriGeometryType.esriGeometryMultipoint;
		} else {
			throw new UnsupportedOperationException(
					"Found unknown type of geometry: " + geoclass.getClass());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerEnd
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		path.remove(path.size() - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerStart
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		path.add(containerStart.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Feature
	 */
	@Override
	public void visit(Feature feature) {
		// Skip non-geometry features
		if (feature.getGeometry() == null)
			return;

		String fullpath = StringUtils.join(path, '_');
		FeatureKey key = sorter.add(feature, fullpath);
		if (datasets.get(key) == null) {
			String datasetname = containerNameStrategy.deriveContainerName(
					path, key);
			datasetname = fixDatasetName(datasetname);
			datasets.put(key, datasetname);
			writeDataSetDef(key, datasetname);
		}
	}

	/**
	 * @param key
	 *            FeatureKey for dataset context
	 * @param datasetname
	 *            name of dataset to be written
	 */
	private void writeDataSetDef(FeatureKey key, String datasetname) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Schema
	 */
	@Override
	public void visit(Schema schema) {
		sorter.add(schema);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events
	 * .IGISObject
	 */
	public void write(IGISObject object) {
		object.accept(this);
	}
}
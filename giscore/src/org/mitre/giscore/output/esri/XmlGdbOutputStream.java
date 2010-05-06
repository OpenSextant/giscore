/****************************************************************************************
 *  XmlGdbOutputStream.java
 *
 *  Created: Feb 6, 2009
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
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.gdb.IXmlGdb;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.FeatureSorter;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.itf.geodesy.Geodetic2DBounds;

/**
 * The organization of the gdb xml exchange document is divided in two sections.
 * The first section defines the schemata for all of the data sets. The second
 * contains the data sets themselves.
 * <p>
 * Since the elements coming into this may be coming in a relatively arbitrary
 * order, i.e. the schema elements may be intermixed with feature elements, the
 * implementation first takes all the features and saves them on a by schema
 * basis to temp files.
 * <p>
 * While it does this, it defines all the schemata, including creating schemata
 * for features that have no explicit schema references, or for schemata
 * referenced but not defined in the input stream.
 * <p>
 * Once phase one completes and the {@link #close()} method is called, the
 * features are read in, set by set and output. Finally the temp files are
 * deleted.
 * <p>
 * The initial implementation is aimed at being readable by a corresponding
 * input stream implementation. The Xml Gdb schema is quite complex and includes
 * many elements that are irrelevant for our usage, those elements are going to
 * be skipped for the time being.
 * <h2>Warning Warning Warning</h2>
 * This code is derived from many sources. It is necessary to be extremely
 * careful when making changes as error reporting from ESRI tools is, to say
 * the least, non-specific. The tendency is to show a simple error dialog with
 * a missleading error text or worse yet to just crash.
 * <ul>
 * <li>XML Schema - accurate enough, but gives you no semantic information about
 * what elements are needed. An optional element may not be optional given the
 * presence of other elements.
 * <li>Examples exported via ArcCatalog of different data sets
 * <li>Written documentation about the schema - very sketchy and avoids the
 * details
 * <li>Trial and error, i.e. reverse engineering by running things through
 * ArcCatalog and testing for what elements can be removed and what values 
 * work correctly in some cases.
 * </ul> 
 * <p>
 * Note that the visitor pattern is somewhat split here. On the feature and 
 * schema visitor methods are really exercised in the "front-end" process where
 * the data is pulled in and sorted into the feature sorter. The "back-end" 
 * process handles one feature set at a time and the visitor patter is once more
 * asserted to handle the geometry information.
 * 
 * @author DRAND
 */
public class XmlGdbOutputStream extends XmlOutputStreamBase implements IXmlGdb {
//	/**
//	 * An escaping writer to handle the odd text rules required for ESRI's brand
//	 * of XML file.
//	 */
//	static class ESRIEscapingWriter extends Writer {
//		private Writer inner = null;
//		
//		public ESRIEscapingWriter(Writer inner) {
//			if (inner == null) {
//				throw new IllegalArgumentException(
//						"inner should never be null");
//			}
//			this.inner = inner;
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.io.Writer#close()
//		 */
//		@Override
//		public void close() throws IOException {
//			inner.close(); 
//		}
//
//		/* (non-Javadoc)
//		 * @see java.io.Writer#flush()
//		 */
//		@Override
//		public void flush() throws IOException {
//			inner.flush();
//		}
//
//		/* (non-Javadoc)
//		 * @see java.io.Writer#write(char[], int, int)
//		 */
//		@Override
//		public void write(char[] cbuf, int off, int len) throws IOException {
//			for(int i = 0; i < len; i++) {
//				char ch = cbuf[off + i];
//				if (ch == '"') {
//					inner.write("&quot;");
//				} else if (ch == '<') {
//					inner.write("&lt;");
//				} else if (ch == '>') {
//					inner.write("&gt;");
//				} else {
//					inner.write((int) ch);
//				}
//			}
//		}
//		
//	}
//	
//	static class ESRIEscapingWriterFactory implements EscapingWriterFactory {
//		/* (non-Javadoc)
//		 * @see org.codehaus.stax2.io.EscapingWriterFactory#createEscapingWriterFor(java.io.Writer, java.lang.String)
//		 */
//		@Override
//		public Writer createEscapingWriterFor(Writer innerwriter, String enc)
//				throws UnsupportedEncodingException {
//			return new ESRIEscapingWriter(innerwriter);
//		}
//
//		/* (non-Javadoc)
//		 * @see org.codehaus.stax2.io.EscapingWriterFactory#createEscapingWriterFor(java.io.OutputStream, java.lang.String)
//		 */
//		@Override
//		public Writer createEscapingWriterFor(OutputStream innerstream, String enc)
//				throws UnsupportedEncodingException {
//			OutputStreamWriter osw = new OutputStreamWriter(innerstream, enc);
//			return createEscapingWriterFor(osw, enc);
//		} 
//		
//	}
	
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
	 * The first time we find a particular feature key, we store away the 
	 * path and geometry type as a name. Not perfect, but at least it will
	 * be somewhat meaningful.
	 */
	private final Map<FeatureKey, String> datasets = new HashMap<FeatureKey, String>();

	/*
	 * WGS wkid
	 */
	private static final int WGS_84 = 4326;
	private static String WKT_WGS_84 = "GEOGCS[\"GCS_WGS_1984\"," +
			"DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," +
			"PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]," +
			"AUTHORITY[\"EPSG\",4326]]";
	
	/*
	 * Formats
	 */
	private static final DecimalFormat DEC = new DecimalFormat("###############.#");
	private static final DecimalFormat LOC = new DecimalFormat("###.#####");
	private static final SimpleDateFormat ISO_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
	
	/*
	 * The following three fields are reused for each feature class that
	 * contains geometry.
	 */
	private static SimpleField shape;
	private static SimpleField shapeArea;
	private static SimpleField shapeLength;

    private static AtomicInteger ms_id = new AtomicInteger();

	static {		
		shape = new SimpleField("INT_SHAPE");
		shape.setType(SimpleField.Type.GEOMETRY);
		shape.setLength(0);
		shape.setRequired(true);
		shape.setAliasName("INT_SHAPE");
		shape.setModelName("INT_SHAPE");
		
		shapeArea = new SimpleField("INT_SHAPE_AREA");
		shapeArea.setType(SimpleField.Type.DOUBLE);
		shapeArea.setLength(8);
		shapeArea.setRequired(true);
		shapeArea.setAliasName("INT_SHAPE_AREA");
		shapeArea.setModelName("INT_SHAPE_AREA");
		
		shapeLength = new SimpleField("INT_SHAPE_LENGTH");
		shapeLength.setType(SimpleField.Type.DOUBLE);
		shapeLength.setLength(8);
		shapeLength.setRequired(true);
		shapeLength.setAliasName("INT_SHAPE_LENGTH");
		shapeLength.setModelName("INT_SHAPE_LENGTH");

        ISO_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
    /**
     * Ctor
     *
     * @param stream the underlying input stream.
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    public XmlGdbOutputStream(OutputStream stream) throws XMLStreamException {
        super(stream);
    }

//	/* (non-Javadoc)
//	 * @see org.mitre.giscore.output.XmlOutputStreamBase#createFactory()
//	 */
//	@Override
//	protected XMLOutputFactory createFactory() {
//		XMLOutputFactory factory = super.createFactory();
//		WstxOutputFactory wstxfact = (WstxOutputFactory) factory;
//		wstxfact.getConfig().setTextEscaperFactory(new ESRIEscapingWriterFactory());
//		return factory;
//	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.ContainerEnd)
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		path.pop();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		path.push(containerStart.getName());
	}

	/**
	 * The esri elements are usually marked with a specific type. This method
	 * handles adding that attribute
	 * @param type the type, never <code>null</code> or empty.
	 * @throws XMLStreamException 
	 */
	private void writeEsriType(String type) throws XMLStreamException {
		if (type == null || type.trim().length() == 0) {
			throw new IllegalArgumentException(
					"type should never be null or empty");
		}
		writer.writeAttribute(XSI_NS, TYPE_ATTR, "esri:" + type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.output.IGISOutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			writer.writeStartDocument();
			writer.writeStartElement(ESRI, WORKSPACE, ESRI_NS);
			writer.writeNamespace(ESRI, ESRI_NS);
			writer.writeNamespace(XSI, XSI_NS);
			writer.writeNamespace(XS, XS_NS);
			writer.writeStartElement(WORKSPACE_DEF);
			writeEsriType("WorkspaceDefinition");
			handleSimpleElement(WORKSPACE_TYPE, "esriLocalDatabaseWorkspace");
			writer.writeStartElement(VERSION);
			writer.writeEndElement();
			writer.writeStartElement(DOMAINS);
			writeEsriType("ArrayOfDomain");
			writer.writeEndElement();
			writer.writeStartElement(DATASET_DEFS);
			writeEsriType("ArrayOfDataElement");			
			for(FeatureKey key : sorter.keys()) {
				String datasetname = datasets.get(key);
				try {
					writeDataSetDef(key, datasetname);
				} catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}
			}
			sorter.close();
			writer.writeEndElement(); // Dataset defs
			writer.writeEndElement(); // Workspace def
			writeDataSets();
			writer.writeEndElement(); // Workspace
			writer.writeEndDocument();
			super.close();
			sorter.cleanup();
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
	}

	/**
	 * 
	 */
	private void writeDataSets() throws XMLStreamException {
		writer.writeStartElement(WORKSPACE_DATA);
		writeEsriType("WorkspaceData");
		// Handle all the schemata + geo class data collections
		for (FeatureKey key : sorter.keys()) {
			if (key.getFeatureClass().equals(Feature.class))
				writeDataSet(key);
		}
		writer.writeEndElement(); // Workspace data
	}

	/**
	 * Write a data set. To write the data set we output a particular set of
	 * container elements, then the data set's definition, and finally the
	 * records from the data set.
	 * 
	 * @param featureKey
	 * @throws XMLStreamException
	 */
	private void writeDataSet(FeatureKey featureKey)
			throws XMLStreamException {
		String geometryField = null;
		writer.writeStartElement(DATASET_DATA);
		writeEsriType("TableData");
		handleSimpleElement(DATASET_NAME, datasets.get(featureKey));
		handleSimpleElement(DATASET_TYPE, "esriDTFeatureClass");
		writer.writeStartElement(DATA);
		writeEsriType("RecordSet");
		geometryField = writeFields(featureKey);
		writeRecords(featureKey, geometryField);
		writer.writeEndElement(); // DATA
		writer.writeEndElement(); // DATASET_DATA
	}

	/**
	 * Write the records. To write the records, we open the temp file to
	 * retrieve the data and write each feature into its own output record
	 * element.
	 * 
	 * @param featureKey
	 * @param geometryField 
	 * @throws XMLStreamException
	 */
	private void writeRecords(FeatureKey featureKey, String geometryField)
			throws XMLStreamException {
		writer.writeStartElement(RECORDS);
		writeEsriType("ArrayOfRecord");
		ObjectBuffer buffer = sorter.getBuffer(featureKey);
		Schema schema = featureKey.getSchema();
		if (buffer == null) {
			throw new RuntimeException("Couldn't find temp file for schema "
					+ schema.getName());
		}
		try {
			Object next = null;
			SimpleField geofielddef = new SimpleField(geometryField);
			geofielddef.setType(SimpleField.Type.GEOMETRY);
			int index = 0;
			while ((next = buffer.read()) != null) {
				Feature feature = (Feature) next;
				writeRecord(featureKey, feature, geofielddef, index++);
			}
		} catch (EOFException e) {
			// Done reading, just ignore
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		writer.writeEndElement(); // RECORDS
	}

	/**
	 * Write a single record. The geometry field will be present for some
	 * formats, where other formats will use a generated field name.
	 * 
	 * @param featureKey
	 * @param feature
	 *            the feature to be written
	 * @param geometryField
	 *            the field that holds the geometry
	 * @param index
	 * @throws XMLStreamException
	 */
	private void writeRecord(FeatureKey featureKey, Feature feature,
			SimpleField geometryField, int index) throws XMLStreamException {
		Schema schema = featureKey.getSchema();
		writer.writeStartElement(RECORD);
		writeEsriType("Record");
		writer.writeStartElement(VALUES);
		writeEsriType("ArrayOfValue");
		boolean geohandled = false;
		SimpleField oidfield = schema.getOidField();
		for (String fieldname : schema.getKeys()) {
			SimpleField field = schema.get(fieldname);
			if (oidfield.equals(field)) {
				writeValue(oidfield, index + 1);
			} else if (field.equals(geometryField)) {
				writeValue(field, feature.getGeometry());
				geohandled = true;
			} else {
				writeValue(field, feature.getData(field));
			}
		}
		if (geohandled == false) {
			writeValue(geometryField, feature.getGeometry());
		}
		if (geoClassNeedsArea(featureKey.getGeoclass()))
			writeValue(shapeArea, null);
		if (geoClassNeedsLength(featureKey.getGeoclass()))
			writeValue(shapeLength, null);
		writer.writeEndElement(); // VALUES
		writer.writeEndElement(); // RECORD
	}

	/**
	 * This writes a single value as part of the output record, dispatching for
	 * geometry data.
	 * 
	 * @param field
	 *            the field, assumed not <code>null</code>
	 * @param datum
	 *            the datum
	 * @throws XMLStreamException
	 */
	private void writeValue(SimpleField field, Object datum)
			throws XMLStreamException {
		writer.writeStartElement(VALUE);
		if (field.getType().getXmlSchemaType() != null) {
			writer.writeAttribute(XSI_NS, TYPE_ATTR, 
					field.getType().getXmlSchemaType());
		}
		SimpleField.Type type = field.getType();
		if (datum == null) {
			// Can't put out nothing for the value - ESRI really don't seem to
			// have that as a concept
			if (type.getGdbEmptyValue() != null) {
				datum = type.getGdbEmptyValue();
			} else {
				throw new RuntimeException("Missing required value for type " + type.name());
			}
		}
		if (SimpleField.Type.GEOMETRY.equals(type)) {
			Geometry geo = (Geometry) datum;
			geo.accept(this);
		} else if (SimpleField.Type.DATE.equals(type)) {
			Date dtm = null;
			try {
				if (datum instanceof String) {
					dtm = ISO_DATE_FMT.parse((String) datum);
				} else if (datum instanceof Date) {
					dtm = (Date) datum;
				}
                if (dtm != null)
				    handleCharacters(ISO_DATE_FMT.format(dtm));
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} else {
			String val = datum.toString();
			int size = field.getLength() != null ? field.getLength() : 0;
			if (SimpleField.Type.STRING.equals(field.getType()) && size > 0 &&
					val.length() > size) {
				val = val.substring(0, size - 1);
			}
			handleCharacters(val);
		}
		writer.writeEndElement();
	}
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.GeometryBag)
	 */
	@Override
	public void visit(GeometryBag geobag) {
		throw new UnsupportedOperationException("Geometry Bag is not supported by XML GDB");
	}
	
	

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.Line)
	 */
	@Override
	public void visit(Line line) {
		try {
			writeEsriType("PolylineN");
			handleSimpleElement(HAS_ID, "false");
			handleSimpleElement(HAS_Z, "false");
			handleSimpleElement(HAS_M, "false");
			writeExtent(line.getBoundingBox(), false);
			writer.writeStartElement(PATH_ARRAY);
			writeEsriType("ArrayOfPath");
			writer.writeStartElement(PATH);
			writeEsriType("Path");
			writePointArray(line.getPoints());
			writer.writeEndElement();
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.LinearRing)
	 */
	@Override
	public void visit(LinearRing ring) {
		try {
			writeEsriType("PolygonN");
			handleSimpleElement(HAS_ID, "false");
			handleSimpleElement(HAS_Z, "false");
			handleSimpleElement(HAS_M, "false");
			writeExtent(ring.getBoundingBox(), false);
			writer.writeStartElement(RING_ARRAY);
			writeEsriType("ArrayOfRing");
			if (ring.getBoundingBox() != null) {
				writeRing(RING, ring);
			}
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.Point)
	 */
	@Override
	public void visit(Point point) {
		try {
			writeEsriType(POINT_N);
			writePoint(point);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.MultiPoint)
	 */
	@Override
	public void visit(MultiPoint mp) {
		try {
			writeEsriType(MULTIPOINT_N);
			handleSimpleElement(HAS_ID, "false");
			handleSimpleElement(HAS_Z, "false");
			handleSimpleElement(HAS_M, "false");
			writeExtent(mp.getBoundingBox(), false);
			writer.writeStartElement(POINT_ARRAY);
			writeEsriType(ARRAY_OF_POINT);
			for(Point p : mp.getPoints()) {
				writer.writeStartElement(POINT);
				p.accept(this);
				writer.writeEndElement();
			}
			writer.writeEndElement(); // Point array
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}		
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.Polygon)
	 */
	@Override
	public void visit(Polygon polygon) {
		try {
			writeEsriType("PolygonN");
			handleSimpleElement(HAS_ID, "false");
			handleSimpleElement(HAS_Z, "false");
			handleSimpleElement(HAS_M, "false");
			writeExtent(polygon.getBoundingBox(), false);
			writer.writeStartElement(RING_ARRAY);
			writeEsriType("ArrayOfRing");
			if (polygon.getBoundingBox() != null) {
				writeRing(RING, polygon.getOuterRing());
			}
			for(LinearRing ring : polygon.getLinearRings()) {
				writeRing(RING, ring);				
			}
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Output the bounding box as an extent
	 * @param bbox the bounding box, assumed not <code>null</code>.
	 * @param includeReference if <code>true</code>, include a spatial reference
	 * @throws XMLStreamException 
	 */
	private void writeExtent(Geodetic2DBounds bbox, boolean includeReference)
		throws XMLStreamException {
		writer.writeStartElement(EXTENT);
		writeEsriType("EnvelopeN");
		handleSimpleElement(XMIN, LOC.format(bbox.getWestLon().inDegrees()));
		handleSimpleElement(YMIN, LOC.format(bbox.getSouthLat().inDegrees()));
		handleSimpleElement(XMAX, LOC.format(bbox.getEastLon().inDegrees()));
		handleSimpleElement(YMAX, LOC.format(bbox.getNorthLat().inDegrees()));
		if (includeReference) writeSpatialReference(WKT_WGS_84, WGS_84);
		writer.writeEndElement();
	}

	/**
	 * Write an array of points
	 * @param points the points, assumed not <code>null</code>
	 * @throws XMLStreamException
	 */
	private void writePointArray(Collection<Point> points) throws XMLStreamException {
		writer.writeStartElement(POINT_ARRAY);
		writeEsriType("ArrayOfPoint");
		writePoints(points);
		writer.writeEndElement();
	}

	/**
	 * Write a ring element
	 * @param tag the name of the containing element, if <code>null</code>
	 * skip the containing element
	 * @param ring the ring, assumed not <code>null</code>
	 * @throws XMLStreamException
	 */
	private void writeRing(String tag, LinearRing ring) throws XMLStreamException {
		if (tag != null) writer.writeStartElement(tag);
		writeEsriType("Ring");
		writePointArray(ring.getPoints());
		if (tag != null) writer.writeEndElement();
	}

	/**
	 * Write one or more points
	 * 
	 * @param points
	 * @throws XMLStreamException
	 */
	private void writePoints(Collection<Point> points)
			throws XMLStreamException {
		for (Point point : points) {
			writer.writeStartElement(POINT);
			writeEsriType("PointN");
			writePoint(point);
			writer.writeEndElement();
		}
	}

	/**
	 * Write point xy data
	 * 
	 * @param point
	 * @throws XMLStreamException
	 */
	private void writePoint(Point point) throws XMLStreamException {
		handleSimpleElement(X, LOC.format(point.getCenter().getLongitude()
				.inDegrees()));
		handleSimpleElement(Y, LOC.format(point.getCenter().getLatitude()
				.inDegrees()));
	}

	/**
	 * Write out the field definitions for the schema
	 * 
	 * @param featureKey
	 * @return the shape field name if present, <code>null</code> if there's
	 * no geometry
	 * @throws XMLStreamException
	 */
	private String writeFields(FeatureKey featureKey)
			throws XMLStreamException {
		Class<? extends Geometry> geoclass = featureKey.getGeoclass();
		Schema schema = featureKey.getSchema();
		String geo_field = schema.getGeometryField();
		Set<String> fieldnames = new HashSet<String>();
		writer.writeStartElement(FIELDS);
		writeEsriType("Fields");
		writer.writeStartElement(FIELD_ARRAY);
		writeEsriType("ArrayOfField");
		for(String name : schema.getKeys()) {
			fieldnames.add(name);
			SimpleField field = schema.get(name);
			writeField(field, featureKey.getGeoclass());
		}
		if (geo_field == null && featureKey.getGeoclass() != null) {
			SimpleField field = shape;
			geo_field = shape.getName();
			writeField(field, geoclass);
			
		}
		if (featureKey.getGeoclass() != null) {
			if (geoClassNeedsArea(featureKey.getGeoclass()))
				writeField(shapeArea, null);
			if (geoClassNeedsLength(featureKey.getGeoclass()))
				writeField(shapeLength, null);
		}
		writer.writeEndElement(); // Fields array
		writer.writeEndElement(); // Fields
		return geo_field;
	}

	/**
	 * @param geoclass
	 * @return
	 */
	private boolean geoClassNeedsLength(Class<? extends Geometry> geoclass) {
		return geoClassNeedsArea(geoclass) || geoclass.isAssignableFrom(Line.class);
	}

	/**
	 * @param geoclass
	 * @return
	 */
	private boolean geoClassNeedsArea(Class<? extends Geometry> geoclass) {
		return geoclass.isAssignableFrom(LinearRing.class) || 
			geoclass.isAssignableFrom(Polygon.class);
	}

	/**
	 * Handle one field from a schema
	 * 
	 * @param field
	 *            the schema field
	 * @param geoclass
	 * @throws XMLStreamException
	 */
	private void writeField(SimpleField field,
			Class<? extends Geometry> geoclass) throws XMLStreamException {
		writer.writeStartElement(FIELD);
		writeEsriType("Field");
		handleSimpleElement(NAME, field.getName());
		handleSimpleElement("Type", field.getType().getGdbXmlType());
		handleSimpleElement(IS_NULLABLE, handleBoolean(field.isNullable()));
		if (field.getLength() != null)
			handleSimpleElement(LENGTH, field.getLength().toString());
		else
			handleSimpleElement(LENGTH, field.getType().getDefaultLength());
		if (field.getPrecision() != null)
			handleSimpleElement(PRECISION, field.getPrecision().toString());
		else
			handleSimpleElement(PRECISION, field.getType().getDefaultPrecision());
		if (field.getScale() != null)
			handleSimpleElement(SCALE, field.getScale().toString());
		else
			handleSimpleElement(SCALE, 0);
		handleSimpleElement(REQUIRED, field.isRequired());
		handleSimpleElement(EDITABLE, field.isEditable());
		if (field.getType().equals(SimpleField.Type.GEOMETRY)) {
			writer.writeStartElement(GEOMETRY_DEF);
			writeEsriType("GeometryDef");
			handleSimpleElement(AVG_NUM_POINTS, "0");
			handleSimpleElement(GEOMETRY_TYPE, getEsriGeoType(geoclass));
			handleSimpleElement(HAS_M, "false");
			handleSimpleElement(HAS_Z, "false");
			writeSpatialReference(WKT_WGS_84, WGS_84);
			handleSimpleElement(GRID_SIZE + "0", 0);
			writer.writeEndElement(); // GEO DEF
		}
		if (field.getAliasName() != null) {
			handleSimpleElement(ALIAS_NAME, field.getAliasName());
		}
		if (field.getModelName() != null) {
			handleSimpleElement(MODEL_NAME, field.getModelName());
			
		}
		writer.writeEndElement();
	}
	
	/**
	 * Write the given spatial reference
	 * @param wkt
	 * @param wkid
	 * @throws XMLStreamException
	 */
	private void writeSpatialReference(String wkt, int wkid) throws XMLStreamException {
		writer.writeStartElement(SPATIAL_REFERENCE);
		writeEsriType("GeographicCoordinateSystem");
		writer.writeStartElement(WKT);
		writer.writeCharacters(wkt);
		writer.writeEndElement();
		handleSimpleElement(X_ORIGIN, -180);
		handleSimpleElement(Y_ORIGIN, -90);
		handleSimpleElement(XY_SCALE, DEC.format(25019997929836.1));
		handleSimpleElement(XY_TOLERANCE, 7.99360577730113E-14);
		handleSimpleElement(HIGH_PRECISION, "true");
		handleSimpleElement(LEFT_LONGITUDE, -180);
		handleSimpleElement(WKID, wkid);
		writer.writeEndElement();		
	}

	/**
	 * @param geoclass
	 * @return the esri type from the schema
	 */
	private String getEsriGeoType(Class<? extends Geometry> geoclass) {
		if (geoclass == null) return "";
		
		if (geoclass.isAssignableFrom(Point.class)) {
			return "esriGeometryPoint";
		} else if (geoclass.isAssignableFrom(Line.class)) {
			return "esriGeometryPolyline";
		} else if (geoclass.isAssignableFrom(LinearRing.class)) {
			return "esriGeometryPolygon";
		} else if (geoclass.isAssignableFrom(Polygon.class)) {
			return "esriGeometryPolygon";
		} else if (geoclass.isAssignableFrom(MultiPoint.class)) {
			return "esriGeometryMultipoint";
		} else {
			throw new UnsupportedOperationException(
					"Found unknown type of geometry: " + geoclass.getClass());
		}
	}
	
	private String getEsriClsid(Class<? extends Geometry> geoclass) {
		return "{52353152-891A-11D0-BEC6-00805F7C4268}";
	}
	
	private String getEsriFeatureType(Class<? extends Geometry> geoclass) {
		return "esriFTSimple";
	}	
	
	private String getEsriExtId(Class<? extends Geometry> geoclass) {
		return "";
	}

	/**
	 * @param value
	 *            the value
	 * @return the boolean as a string
	 */
	private String handleBoolean(boolean value) {
		return value ? "true" : "false";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Feature)
	 */
	@Override
	public void visit(Feature feature) {
		// Skip non-geometry features
		if (feature.getGeometry() == null) return;
		
		String fullpath = path != null ? StringUtils.join(path, '_') : null;
		FeatureKey key = sorter.add(feature, fullpath);
		if (datasets.get(key) == null) {
			StringBuilder setname = new StringBuilder();
			setname.append(fullpath);
			if (key.getGeoclass() != null) {
				setname.append("_");
				setname.append(key.getGeoclass().getSimpleName());
			}
			String datasetname = setname.toString();
			datasetname = datasetname.replaceAll("\\s", "_");
			datasets.put(key, datasetname);
		}
	}

	/**
	 * Write the dataset into the defs portion. This will add the appropriate
	 * extra elements required by ESRI
	 * 
	 * @param key
	 * @param datasetname
	 * @throws XMLStreamException 
	 */
	private void writeDataSetDef(FeatureKey key, String datasetname) throws XMLStreamException {
		if (key.getGeoclass() == null) {
			throw new IllegalArgumentException("Must have a geo class");
		}
		Schema schema = key.getSchema();
		writer.writeStartElement(DATA_ELEMENT);
		writeEsriType("DEFeatureClass");
		handleSimpleElement(CATALOG_PATH, "/FC=" + datasetname);
		handleSimpleElement(NAME, datasetname);
		handleSimpleElement(DATASET_TYPE, "esriDTFeatureClass");
		handleSimpleElement(DSID, ms_id.incrementAndGet());
		handleSimpleElement(VERSIONED, "false");
		handleSimpleElement(CAN_VERSION, "false");
		handleSimpleElement(HAS_OID, "true");
		handleSimpleElement(OID_FIELD_NAME, schema.getOidField().getName());
		writeFields(key);
		writer.writeStartElement(INDEXES);
		writeEsriType("Indexes");
		writer.writeStartElement(INDEX_ARRAY);
		writeEsriType("ArrayOfIndex");
		writeIndex(schema.getOidField(), true, true, key);
		if (key.getGeoclass() != null) {
			writeIndex(shape, false, false, key);
		}
		writer.writeEndElement(); // IndexArray
		writer.writeEndElement(); // Indexes
		handleSimpleElement(CLSID, getEsriClsid(key.getGeoclass()));
		handleSimpleElement(EXTCLSID, getEsriExtId(key.getGeoclass()));
		writer.writeStartElement(REL_CLASS_NAMES);
		writeEsriType("Names");
		writer.writeEndElement();
		handleSimpleElement(ALIAS_NAME, datasetname);
		handleSimpleElement(MODEL_NAME, "");
		handleSimpleElement(HAS_GLOBAL_ID, "false");
		handleSimpleElement(GLOBAL_ID_FIELD, "");
		handleSimpleElement(RASTER_FIELD_NAME, "");
		writer.writeStartElement(EXT_PROPS);
		writeEsriType("PropertySet");
		writer.writeStartElement(PROPERTY_ARRAY);
		writeEsriType("ArrayOfPropertySetProperty");
		writer.writeEndElement();
		writer.writeEndElement();
		writer.writeStartElement(CONTROLLER_MEMBERSHIPS);
		writeEsriType("ArrayOfControllerMembership");
		writer.writeEndElement();
		handleSimpleElement(FEATURE_TYPE, getEsriFeatureType(key.getGeoclass()));
		handleSimpleElement(SHAPE_TYPE, getEsriGeoType(key.getGeoclass()));
		handleSimpleElement(SHAPE_FIELD_NAME, shape.getName());
		handleSimpleElement(HAS_M, "false");
		handleSimpleElement(HAS_Z, "false");
		handleSimpleElement(HAS_SPATIAL_INDEX, "true");
		if (key.getGeoclass() != null) {
			if (geoClassNeedsArea(key.getGeoclass()))
				handleSimpleElement(AREA_FIELD_NAME, shapeArea.getName());
			else
				handleSimpleElement(AREA_FIELD_NAME, "");
			if (geoClassNeedsLength(key.getGeoclass()))
				handleSimpleElement(LENGTH_FIELD_NAME, shapeLength.getName());
			else
				handleSimpleElement(LENGTH_FIELD_NAME, "");
			Geodetic2DBounds bounds = sorter.getBounds(key);
			if (bounds != null) {
				writeExtent(bounds, true);
			}
		}
		writeSpatialReference(WKT_WGS_84, WGS_84);
		writer.writeEndElement(); // DataElement
	}

	/**
	 * @param field
	 * @param ascending
	 * @param unique
	 * @param key 
	 * @throws XMLStreamException 
	 */
	private void writeIndex(SimpleField field, boolean ascending, boolean unique, FeatureKey key)
	throws XMLStreamException {
		writer.writeStartElement(INDEX);
		writeEsriType(INDEX);
		handleSimpleElement(NAME, "FDO_" + field.getName());
		handleSimpleElement(IS_UNIQUE, handleBoolean(unique));
		handleSimpleElement(IS_ASCENDING, handleBoolean(ascending));
		writer.writeStartElement(FIELDS);
		writeEsriType("Fields");
		writer.writeStartElement(FIELD_ARRAY);
		writeEsriType("ArrayOfField");
		writeField(field, key.getGeoclass());
		writer.writeEndElement(); // FieldArray
		writer.writeEndElement(); // Field
		writer.writeEndElement(); // Index
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Schema)
	 */
	@Override
	public void visit(Schema schema) {
		sorter.add(schema);
	}
}


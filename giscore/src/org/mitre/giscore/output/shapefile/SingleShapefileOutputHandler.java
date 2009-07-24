/****************************************************************************************
 *  SingleShapefileOutputHandler.java
 *
 *  Created: Jul 21, 2009
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
package org.mitre.giscore.output.shapefile;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Formatter;
import java.util.Locale;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.dbf.DbfOutputStream;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;

/**
 * Coordinate the output of a single buffer for writing the four shapefile
 * component files. The four files are:
 * <ul>
 * <li>.shp - the shapefile itself which contains a single kind of geometry
 * information.
 * <li>.shx - the index file, which contains records pointing to the locations
 * in the .shp file for each geometry.
 * <li>.dbf - the attribute file, which is written by a separate handler
 * <li>.prj - a plaintext projection file that identifies the projection in use
 * within the shapefile. Always WGS84 for our classes.
 * </ul>
 * This is a helper class that will not be used standalone.
 * <p>
 * The majority of the shapefile code is lifted directly from the old mediate
 * <code>ShpHandler</code>
 * 
 * @author DRAND
 * @author Paul Silvey for the original Mediate
 */
public class SingleShapefileOutputHandler extends ShapefileBaseClass {
	private static final int VERSION = 1000;
	private static final String WGS84prj = "PROJCS[\"WGS_1984_UTM_Zone_35S\"," + //
			"GEOGCS[\"GCS_WGS_1984\"," + //
			"DATUM[\"D_WGS_1984\"," + //
			"SPHEROID[\"WGS_1984\",6378137,298.257223563]]," + //
			"PRIMEM[\"Greenwich\",0]," + //
			"UNIT[\"Degree\",0.017453292519943295]]," + //
			"PROJECTION[\"Transverse_Mercator\"]," + //
			"PARAMETER[\"Central_Meridian\",27]," + //
			"PARAMETER[\"Latitude_Of_Origin\",0]," + //
			"PARAMETER[\"Scale_Factor\",0.9996]," + //
			"PARAMETER[\"False_Easting\",500000]," + //
			"PARAMETER[\"False_Northing\",10000000]," + //
			"UNIT[\"Meter\",1]";

	/**
	 * The schema being used will never be <code>null</code>
	 */
	private Schema schema;

	/**
	 * The optional style information
	 */
	private Style style = null;

	/*
	 * Pointers to the four required files. Setup in the ctor and never modified
	 * afterward.
	 */
	private File shpFile;
	private File shxFile;
	private File prjFile;
	private File dbfFile;
	/*
	 * Optional shm file, only used if style != null and there is an icon url.
	 */
	private File shmFile;

	/**
	 * The buffer that holds the data to be output.
	 */
	private ObjectBuffer buffer;

	/**
	 * The mapper, which maps from urls used in the style to integer values used
	 * for ESRI.
	 */
	private PointShapeMapper mapper;

	/**
	 * Ctor
	 * 
	 * @param schema
	 *            the schema, never <code>null</code>.
	 * @param style
	 *            the optional style, may be <code>null</code>
	 * @param buffer
	 *            the output buffer, never <code>null</code> or empty
	 * @param outputDirectory
	 *            the output directory, will be created if it does not exist,
	 *            never <code>null</code>.
	 * @param shapefilename
	 *            the name of the shapefile to be created. This name will be
	 *            modified with the standard suffixes to create the actual
	 *            output shapefile. never <code>null</code> or empty
	 * @param mapper
	 *            a mapper to go from the url based icons in the Style to a
	 *            short value for use with ESRI, must not be <code>null</code>
	 *            if style is not <code>null</code>.
	 */
	public SingleShapefileOutputHandler(Schema schema, Style style,
			ObjectBuffer buffer, File outputDirectory, String shapefilename,
			PointShapeMapper mapper) {
		if (schema == null) {
			throw new IllegalArgumentException("schema should never be null");
		}
		if (buffer == null || buffer.count() == 0) {
			throw new IllegalArgumentException("buffer should never be null and must contain at least one geometry element");
		}
		if (outputDirectory == null) {
			throw new IllegalArgumentException(
					"outputDirectory should never be null");
		}
		if (shapefilename == null || shapefilename.trim().length() == 0) {
			throw new IllegalArgumentException(
					"shapefilename should never be null or empty");
		}
		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new IllegalArgumentException(
						"Couldn't create output directory");
			}
		}
		if (style != null && mapper == null) {
			throw new IllegalArgumentException(
					"mapper should never be null if style is provided");
		}
		this.schema = schema;
		this.buffer = buffer;
		this.style = style;
		this.mapper = mapper;

		shpFile = new File(outputDirectory, shapefilename + ".shp");
		shxFile = new File(outputDirectory, shapefilename + ".shx");
		prjFile = new File(outputDirectory, shapefilename + ".prj");
		dbfFile = new File(outputDirectory, shapefilename + ".dbf");
		shmFile = new File(outputDirectory, shapefilename + ".shm");
	}

	/**
	 * Output the data.
	 * 
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws XMLStreamException
	 */
	public void process() throws IOException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, XMLStreamException {
		// Write prj
		FileOutputStream prjos = new FileOutputStream(prjFile);
		prjos.write(WGS84prj.getBytes("US-ASCII"));
		prjos.close();
		// Write shp and shx
		outputFeatures();
		// Write dbf
		FileOutputStream dbfos = new FileOutputStream(dbfFile);
		buffer.resetReadIndex();
		DbfOutputStream dbf = new DbfOutputStream(dbfos, schema, buffer);
		dbf.close();
		dbfos.close();
		// Write shm
		writeShm();
	}

	/**
	 * Write shm if the style is given. This writes a minimal shm file that
	 * provides shape and color information for the points.
	 * 
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 * @throws MalformedURLException
	 */
	private void writeShm() throws FileNotFoundException, XMLStreamException,
			MalformedURLException {
		if (style == null || style.getIconUrl() == null)
			return;

		OutputStream stream = new FileOutputStream(shmFile);
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stream);
		writer.writeStartDocument();
		writer.writeStartElement("org.mitre.forensics.util.BaseLayerMetaData");
		writer.writeStartElement("renderer__info");

		writer.writeStartElement("symbol__class");
		writer.writeCharacters("point");
		writer.writeEndElement();

		writer.writeStartElement("symbol__type");
		writer.writeCharacters(Short.toString(mapper.getMarker(new URL(style
				.getIconUrl()))));
		writer.writeEndElement();
		
		if (style.getIconColor() != null) {
			writer.writeStartElement("symbol__color");
	        StringBuilder sb = new StringBuilder(8);
	        Formatter formatter = new Formatter(sb, Locale.US);
	        Color color = style.getIconColor();
	        formatter.format("0x%02x%02x%02x%02x", color.getAlpha(), color
	                .getBlue(), color.getGreen(), color.getRed());
			writer.writeCharacters(sb.toString());
			writer.writeEndElement();
		}
		
		writer.writeStartElement("has__labelling");
		writer.writeCharacters("false");
		writer.writeEndElement();

		writer.writeStartElement("point__size");
		int size = (int) (style.getIconScale() * 15.0);
		writer.writeCharacters(Integer.toString(size));
		writer.writeEndElement();

		writer.writeStartElement("outer-class");
		writer.writeAttribute("reference", "../..");
		writer.writeEndElement();
		
		writer.writeEndElement();
		writer.writeEndDocument();
	}

	/**
	 * Output the features. As the features are output, track the bounding box
	 * information and check for consistent geometry usage. After all the
	 * features are written we reopen the shapefile to output the header.
	 * 
	 * @param shpbos
	 * @param shxbos
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	private void outputFeatures() throws IOException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		BinaryOutputStream shpbos = null, shxbos = null;
		int recordNumber = 1;
		Geodetic2DBounds bbox = null;
		int shapeAll = NULL_TYPE;
		boolean is3D = false;
		// offset is in 16 bit words from the start of the file
		// always contains the offset into the file for the current
		// geo. After all the geo is written contains the length
		// of the file
		int offset = 50;
		try {
			FileOutputStream shpos = new FileOutputStream(shpFile);
			shpbos = new BinaryOutputStream(shpos);
			FileOutputStream shxos = new FileOutputStream(shxFile);
			shxbos = new BinaryOutputStream(shxos);

			// Skip enough bytes to write the header later in the
			// shp and shx files
			for (int i = 0; i < offset; i++) {
				shpbos.writeShort(0);
				shxbos.writeShort(0);
			}
			IDataSerializable ser = buffer.read();
			while (ser != null) {
				Feature feat = (Feature) ser;
				Geometry geo = feat.getGeometry();
				int shape = getShapeType(geo);
				if (shape != NULL_TYPE) {
					// Make sure the type is the same as others in the feature
					// list
					if (shapeAll == NULL_TYPE) {
						shapeAll = shape;
						is3D = is3D(shape);
					} else if (shape != shapeAll)
						throw new IllegalArgumentException(
								"Feature list must contain"
										+ " geometry objects of same type");
				}
				if (bbox == null) {
					bbox = geo.getBoundingBox(); // 3d or not depending on the geo
				} else {
					bbox.include(geo.getBoundingBox());
				}
				int len = getRecLen(geo);
				outputGeometry(shpbos, geo, is3D, shape, recordNumber, len);
				outputIndex(shxbos, offset, len);
				// Records have additional 4 words of info at the start of the
				// record
				offset += len + 4;
				recordNumber++;
				ser = buffer.read();
			}
			// Write header
			putShapeHeader(shpbos, offset, shapeAll, is3D, bbox);
			putShapeHeader(shxbos, offset, shapeAll, is3D, bbox);
		} finally {
			if (shxbos != null)
				shxbos.close();
			if (shpbos != null)
				shpbos.close();
		}

		
	}

	private void outputIndex(BinaryOutputStream bos, int offset, int length)
			throws IOException {
		bos.writeInt(offset, ByteOrder.BIG_ENDIAN);
		bos.writeInt(length, ByteOrder.BIG_ENDIAN);
	}

	private void outputGeometry(BinaryOutputStream bos, Geometry geom,
			boolean is3D, int shape, int rnumber, int rlength)
			throws IOException {
		bos.writeInt(rnumber, ByteOrder.BIG_ENDIAN);
		bos.writeInt(rlength, ByteOrder.BIG_ENDIAN);
		bos.writeInt(shape, ByteOrder.LITTLE_ENDIAN);
		if (shape != NULL_TYPE) {
			// Now, write the record contents, corresponding to shapeType
			// (adjusted if 3D)
			int st = (is3D) ? shape - 10 : shape;
			if (st == POINT_TYPE)
				putPoint(bos, is3D, (Point) geom);
			else {
				// Write X-Y Bounding Box coordinates for Feature's geometry
				putBBox(bos, geom.getBoundingBox());
				if (st == MULTILINE_TYPE)
					putPolyLine(bos, is3D, geom);
				else if (st == MULTINESTEDRINGS_TYPE)
					putPolygon(bos, is3D, geom);
				else if (st == MULTIPOINT_TYPE)
					putMultipoint(bos, is3D, (MultiPoint) geom);
			}
		}
	}

	/**
	 * Count required words to output the geometry information for a given
	 * geometry object
	 * 
	 * @param geom
	 *            the geo object
	 * @return the count in words (16 bit words) not including the record header
	 *         (4 words, record number and length)
	 */
	private int getRecLen(Geometry geom) {
		int nParts, nPoints, ptSize;
		int recLen = 2; // 2 word shape type
		int shapeOne = getShapeType(geom);
		if (shapeOne != NULL_TYPE) {
			nParts = geom.getNumParts();
			nPoints = geom.getNumPoints();
			// 2D points (x, y) are 8 words each, 3D points (x, y, z, m) are 16
			// words each
			ptSize = 8;
			if (is3D(shapeOne)) {
				ptSize += 8; // Add 8 to account for extra fields in 3D points
				shapeOne -= 10; // Subtract 10 to enable 2D type matching in if
				// stmt below
			}
			// Point type: ptSize words
			// MultiPoint type: 18 words + (ptSize * nPoints) words
			// PolyLine & Polygon type: 20 words + (2 * nParts) words + (ptSize
			// * nPoints) words
			if (shapeOne == POINT_TYPE)
				recLen += ptSize;
			else if (shapeOne == MULTIPOINT_TYPE)
				recLen += 18 + (ptSize * nPoints);
			else
				recLen += 20 + (2 * nParts) + (ptSize * nPoints);
		}
		return recLen;
	}

	/**
	 * private helper method to load Geodetic Point coordinates into arrays of
	 * doubles
	 * 
	 * @param i
	 * @param lleDeg
	 * @param is3D
	 * @param pt
	 * @param wrap
	 */
	private void loadPtData(int i, double[][] lleDeg, boolean is3D,
			Geodetic2DPoint pt, boolean wrap) {
		double lonDeg = pt.getLongitude().inDegrees();
		if (wrap && (lonDeg == -180.0))
			lonDeg = 180.0;
		lleDeg[0][i] = lonDeg;
		lleDeg[1][i] = pt.getLatitude().inDegrees();
		if (is3D)
			lleDeg[2][i] = ((Geodetic3DPoint) pt).getElevation();
	}

	/**
	 * private helper method to write out X-Y bounding box
	 * 
	 * @param boS
	 * @param bbox
	 * @throws IOException
	 */
	private void putBBox(BinaryOutputStream boS, Geodetic2DBounds bbox)
			throws IOException {
		double westLonDeg = bbox.westLon.inDegrees();
		double southLatDeg = bbox.southLat.inDegrees();
		double eastLonDeg = bbox.eastLon.inDegrees();
		double northLatDeg = bbox.northLat.inDegrees();
		// Correct for ESRI tools dislike for bounding wrapping bounding boxes
		if (eastLonDeg == -180.0)
			eastLonDeg = +180.0;
		else if (eastLonDeg < westLonDeg) {
			westLonDeg = -180.0;
			eastLonDeg = +180.0;
		}
		boS.writeDouble(westLonDeg, ByteOrder.LITTLE_ENDIAN); // X min
		boS.writeDouble(southLatDeg, ByteOrder.LITTLE_ENDIAN); // Y min
		boS.writeDouble(eastLonDeg, ByteOrder.LITTLE_ENDIAN); // X max
		boS.writeDouble(northLatDeg, ByteOrder.LITTLE_ENDIAN); // Y max
	}

	/**
	 * Write the shapefile header, containing fileLen, shapeType and Bounding
	 * Box
	 * 
	 * @param boS
	 * @param fileLen
	 * @param shapeType
	 * @param is3D
	 * @param bbox
	 * @throws IOException
	 */
	private void putShapeHeader(BinaryOutputStream bos, int fileLen, int shapeType,
			boolean is3D, Geodetic2DBounds bbox) throws IOException {
		bos.flush(); // Make sure everything's written
		FileOutputStream os = (FileOutputStream) bos.getWrappedStream();
		os.getChannel().position(0); // Reposition to the start of the file
		try {
			// Write the shapefile signature (should be 9994)
			bos.writeInt(SIGNATURE, ByteOrder.BIG_ENDIAN);
			// fill unused bytes in header with zeros
			for (int i = 0; i < 5; i++)
				bos.writeInt(0, ByteOrder.BIG_ENDIAN);
			// Write the file length (total number of 2-byte words, including
			// header)
			bos.writeInt(fileLen, ByteOrder.BIG_ENDIAN);
			// Write the shapefile version (should be 1000)
			bos.writeInt(VERSION, ByteOrder.LITTLE_ENDIAN);
			// Write the shapeType
			bos.writeInt(shapeType, ByteOrder.LITTLE_ENDIAN);
			// Write the overall X-Y bounding box to the shapefile header
			putBBox(bos, bbox);
			// In Shapefiles, Z and M bounds are usually separated from X-Y
			// bounds
			// (and instead grouped with their arrays of data), except for in
			// the
			// header
			double zMin = (is3D) ? ((Geodetic3DBounds) bbox).minElev : 0.0;
			double zMax = (is3D) ? ((Geodetic3DBounds) bbox).maxElev : 0.0;
			bos.writeDouble(zMin, ByteOrder.LITTLE_ENDIAN); // Z min
			bos.writeDouble(zMax, ByteOrder.LITTLE_ENDIAN); // Z max
			bos.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M min (not
			// supported)
			bos.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M max (not
			// supported)
		} finally {
			bos.close();
		}
	}

	/**
	 * Write next Point (ESRI Point or PointZ) record
	 * 
	 * @param boS
	 * @param is3D
	 * @param pt
	 * @throws IOException
	 */
	private void putPoint(BinaryOutputStream boS, boolean is3D, Point pt)
			throws IOException {
		Geodetic2DPoint gp = pt.asGeodetic2DPoint();
		boS.writeDouble(gp.getLongitude().inDegrees(), ByteOrder.LITTLE_ENDIAN);
		boS.writeDouble(gp.getLatitude().inDegrees(), ByteOrder.LITTLE_ENDIAN);
		if (is3D) {
			boS.writeDouble(((Geodetic3DPoint) gp).getElevation(),
					ByteOrder.LITTLE_ENDIAN);
			boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // measure value M
			// not used
		}
	}

	/**
	 * Write PolyLine and Polygon point values from array of Geodetic points
	 * 
	 * @param boS
	 * @param bbox
	 * @param lleDeg
	 * @throws IOException
	 */
	private void putPolyPoints(BinaryOutputStream boS, Geodetic2DBounds bbox,
			double[][] lleDeg) throws IOException {
		// lleDeg contains the Lon, Lat, and (optionally) Elevation values in
		// decimal degrees
		// Write the x and y points
		int nPoints = lleDeg[0].length;
		for (int i = 0; i < nPoints; i++) {
			boS.writeDouble(lleDeg[0][i], ByteOrder.LITTLE_ENDIAN); // Longitude
			boS.writeDouble(lleDeg[1][i], ByteOrder.LITTLE_ENDIAN); // Latitude
		}
		// If 3D, write the Z bounds + values and zeroed out (unused) M bounds
		// and values
		if (lleDeg.length == 3) {
			boS.writeDouble(((Geodetic3DBounds) bbox).minElev,
					ByteOrder.LITTLE_ENDIAN); // Z min
			boS.writeDouble(((Geodetic3DBounds) bbox).maxElev,
					ByteOrder.LITTLE_ENDIAN); // Z max
			for (int i = 0; i < nPoints; i++)
				boS.writeDouble(lleDeg[2][i], ByteOrder.LITTLE_ENDIAN); // Elevation
			boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M min (not
			// supported)
			boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M max (not
			// supported)
			for (int i = 0; i < nPoints; i++)
				boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN);
		}
	}

	// Write next MultiLine (ESRI Polyline or PolylineZ) record
	// Take part hierarchy and flatten in file structure
	private void putPolyLine(BinaryOutputStream boS, boolean is3D, Geometry geom)
			throws IllegalArgumentException, IOException {
		boS.writeInt(geom.getNumParts(), ByteOrder.LITTLE_ENDIAN);
		int nPoints = geom.getNumPoints();
		boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN);
		double[][] lleDeg = (is3D) ? new double[3][nPoints]
				: new double[2][nPoints];
		int pointIndex = 0;
		boolean wrap;
		// Note that Line and MultiLine objects are valid Geometry inputs to
		// this method
		if (geom instanceof Line) {
			boS.writeInt(0, ByteOrder.LITTLE_ENDIAN);
			wrap = ((Line) geom).clippedAtDateLine();
			for (Point pt : (Line) geom)
				loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(),
						wrap);
		} else if (geom instanceof MultiLine) {
			int partOffset = 0;
			for (Line ln : (MultiLine) geom) {
				boS.writeInt(partOffset, ByteOrder.LITTLE_ENDIAN);
				partOffset += ln.getNumPoints();
				wrap = ln.clippedAtDateLine();
				for (Point pt : ln)
					loadPtData(pointIndex++, lleDeg, is3D, pt
							.asGeodetic2DPoint(), wrap);
			}
		} else
			throw new IllegalArgumentException("Invalid Geometry object "
					+ geom);
		putPolyPoints(boS, geom.getBoundingBox(), lleDeg);
	}

	// Write next MultiPolygons (ESRI Polygon or PolygonZ) record
	// Take nested part hierarchy and flatten in file structure
	private void putPolygon(BinaryOutputStream boS, boolean is3D, Geometry geom)
			throws IOException, IllegalArgumentException {
		int nParts = geom.getNumParts();
		int nPoints = geom.getNumPoints();
		// Determine the part offsets, and collect the Points into a flat array
		int[] parts = new int[nParts];
		double[][] lleDeg = (is3D) ? new double[3][nPoints]
				: new double[2][nPoints];
		// LinearRing, MultiLinearRings and MultiPolygons objects are valid
		// Geometry types here
		int partOffset = 0;
		int partIndex = 0;
		int pointIndex = 0;
		boolean wrap;

		if (geom instanceof LinearRing) {
			parts[partIndex] = partOffset;
			wrap = ((LinearRing) geom).clippedAtDateLine();
			for (Point pt : (LinearRing) geom)
				loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(),
						wrap);
		} else if (geom instanceof MultiLinearRings) {
			for (LinearRing rg : (MultiLinearRings) geom) {
				parts[partIndex++] = partOffset;
				wrap = rg.clippedAtDateLine();
				for (Point pt : rg)
					loadPtData(pointIndex++, lleDeg, is3D, pt
							.asGeodetic2DPoint(), wrap);
				partOffset += rg.getNumPoints();
			}
		} else if (geom instanceof Polygon) {
			Polygon poly = (Polygon) geom;
			// handle outer ring
			LinearRing ring = poly.getOuterRing();
			parts[partIndex++] = partOffset;
			wrap = ring.clippedAtDateLine();
			for (Point pt : ring)
				loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(),
						wrap);
			partOffset += ring.getNumPoints();
			// handle inner rings
			for (LinearRing rg : poly) {
				parts[partIndex++] = partOffset;
				wrap = rg.clippedAtDateLine();
				for (Point pt : rg)
					loadPtData(pointIndex++, lleDeg, is3D, pt
							.asGeodetic2DPoint(), wrap);
				partOffset += rg.getNumPoints();
			}
		} else if (geom instanceof MultiPolygons) {
			for (Polygon nr : (MultiPolygons) geom) {
				// handle outer ring
				LinearRing ring = nr.getOuterRing();
				parts[partIndex++] = partOffset;
				wrap = ring.clippedAtDateLine();
				for (Point pt : ring)
					loadPtData(pointIndex++, lleDeg, is3D, pt
							.asGeodetic2DPoint(), wrap);
				partOffset += ring.getNumPoints();
				// handle inner rings
				for (LinearRing rg : nr) {
					parts[partIndex++] = partOffset;
					wrap = rg.clippedAtDateLine();
					for (Point pt : rg)
						loadPtData(pointIndex++, lleDeg, is3D, pt
								.asGeodetic2DPoint(), wrap);
					partOffset += rg.getNumPoints();
				}
			}
		} else
			throw new IllegalArgumentException("Invalid Geometry object "
					+ geom);

		// Write out the counts, part offsets, and points
		boS.writeInt(nParts, ByteOrder.LITTLE_ENDIAN);
		boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < nParts; i++)
			boS.writeInt(parts[i], ByteOrder.LITTLE_ENDIAN);
		putPolyPoints(boS, geom.getBoundingBox(), lleDeg);
	}

	/**
	 * Write next MultiPoint (ESRI MultiPoint or MultiPointZ) record
	 * 
	 * @param boS
	 * @param is3D
	 * @param mp
	 * @throws IOException
	 */
	private void putMultipoint(BinaryOutputStream boS, boolean is3D,
			MultiPoint mp) throws IOException {
		int nPoints = mp.getNumPoints();
		boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN); // total numPoints
		double[][] lleDeg = (is3D) ? new double[3][nPoints]
				: new double[2][nPoints];
		int pointIndex = 0;
		for (Point pt : mp)
			loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(),
					false);
		putPolyPoints(boS, mp.getBoundingBox(), lleDeg);
	}

}

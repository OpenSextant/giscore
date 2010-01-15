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
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.dbf.DbfOutputStream;
import org.mitre.giscore.utils.ICancelable;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.arcgis.geoprocessing.tools.analysistools.MultipleRingBuffer;

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
 * The shapefile code is inspired by the old mediate code.
 * <code>ShpHandler</code>
 * 
 * @author DRAND
 * @author Paul Silvey for the original Mediate
 */
public class SingleShapefileOutputHandler extends ShapefileBaseClass {
	private static final Logger logger = LoggerFactory
			.getLogger(SingleShapefileOutputHandler.class);
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
			"UNIT[\"Meter\",1]]";

	/**
	 * The schema being used will never be <code>null</code>
	 */
	private final Schema schema;

	/**
	 * The optional style information
	 */
	private final Style style;

	/**
	 * The current buffer being used to output geometry. The buffer is created
	 * just before the current geometry's accept method is called and is set
	 * back to <code>null</code> just afterward.
	 */
	private ByteBuffer obuf = null;

	/*
	 * Pointers to the four required files. Setup in the ctor and never modified
	 * afterward.
	 */
	private final File shpFile;
	private final File shxFile;
	private final File prjFile;
	private final File dbfFile;
	/*
	 * Optional shm file, only used if style != null and there is an icon url.
	 */
	private final File shmFile;

	/**
	 * The buffer that holds the data to be output.
	 */
	private final ObjectBuffer buffer;

	/**
	 * The mapper, which maps from urls used in the style to integer values used
	 * for ESRI.
	 */
	private final PointShapeMapper mapper;

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
	 * @throws IllegalArgumentException
	 *             if couldn't create output directory or any of the required
	 *             arguments are invalid
	 */
	public SingleShapefileOutputHandler(Schema schema, Style style,
			ObjectBuffer buffer, File outputDirectory, String shapefilename,
			PointShapeMapper mapper) {
		if (schema == null) {
			throw new IllegalArgumentException("schema should never be null");
		}
		if (buffer == null || buffer.count() == 0) {
			throw new IllegalArgumentException(
					"buffer should never be null and must contain at least one geometry element");
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
	 *             if an error occurs
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws XMLStreamException
	 */
	public void process() throws IOException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, XMLStreamException {
		process(null);
	}

	/**
	 * Output the data.
	 * 
	 * @param callback
	 *            Provide {@code ICancelable} callback which if {@code
	 *            isCanceled()} returns true then processing is aborted and
	 *            CancellationException is thrown. If {@code null} then no
	 *            cancellation checks are done.
	 * 
	 * @throws IOException
	 *             if an error occurs
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws XMLStreamException
	 * @throws CancellationException
	 *             if callback is provided and forces a cancellation
	 */
	public void process(ICancelable callback) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, XMLStreamException {
		// Write prj
		FileOutputStream prjos = new FileOutputStream(prjFile);
		try {
			prjos.write(WGS84prj.getBytes("US-ASCII"));
		} finally {
			prjos.close();
		}
		// Write shp and shx
		outputFeatures(callback);
		// Write dbf
		FileOutputStream dbfos = new FileOutputStream(dbfFile);
		try {
			buffer.resetReadIndex();
			// Modify the schema for the dbf if we have dates since shapefile's
			// output of dates is entirely useless. Substitute string for date
			Schema dbfschema = dbfModify(schema);
			DbfOutputStream dbf = new DbfOutputStream(dbfos, dbfschema, buffer);
			dbf.close();
		} finally {
			dbfos.close();
		}
		// Write shm
		writeShm();
	}

	/**
	 * Find and replace and simple fields of type date with type string
	 * 
	 * @param orign
	 * @return
	 */
	private Schema dbfModify(Schema orign) {
		boolean foundDate = false;
		for (SimpleField field : orign.getFields()) {
			if (field.getType().equals(SimpleField.Type.DATE)) {
				foundDate = true;
				break;
			}
		}
		if (!foundDate) {
			return orign;
		}
		Schema rval = new Schema();
		for (SimpleField field : orign.getFields()) {
			if (field.getType().equals(SimpleField.Type.DATE)) {
				SimpleField copy = new SimpleField(field.getName());
				copy.setAliasName(field.getAliasName());
				copy.setDisplayName(field.getDisplayName());
				rval.put(copy);
			} else {
				rval.put(field);
			}
		}
		return rval;
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
		try {
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = factory.createXMLStreamWriter(stream);
			writer.writeStartDocument();
			writer
					.writeStartElement("org.mitre.forensics.util.BaseLayerMetaData");
			writer.writeStartElement("renderer__info");

			writer.writeStartElement("symbol__class");
			writer.writeCharacters("point");
			writer.writeEndElement();

			writer.writeStartElement("symbol__type");
			writer.writeCharacters(Short.toString(mapper.getMarker(new URL(
					style.getIconUrl()))));
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
			writer.close();
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}

	/**
	 * Output the features. As the features are output, track the bounding box
	 * information and check for consistent geometry usage. After all the
	 * features are written we reopen the shapefile to output the header.
	 * 
	 * @param callback
	 * 
	 * @throws IOException
	 *             if an error occurs
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws CancellationException
	 *             if callback is provided and forces a cancellation
	 */
	private void outputFeatures(ICancelable callback) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		int recordNumber = 1;
		Geodetic2DBounds bbox = null;
		int shapeAll = NULL_TYPE;
		boolean is3D = false;
		// offset is in 16 bit words from the start of the file
		// always contains the offset into the file for the current
		// geo. After all the geo is written contains the length
		// of the file
		int offset = 50;
		int ioffset = 50;
		FileOutputStream shfos = null;
		FileOutputStream shxfos = null;

		try {
			shfos = new FileOutputStream(shpFile);
			shxfos = new FileOutputStream(shxFile);
			FileChannel channel = shfos.getChannel();
			FileChannel xchannel = shxfos.getChannel();

			IDataSerializable ser = buffer.read();
			while (ser != null) {
				if (callback != null && callback.isCanceled()) {
					throw new CancellationException();
				}
				Feature feat = (Feature) ser;
				Geometry geo = feat.getGeometry();
				int shape = getEsriShapeType(geo);
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
					bbox = geo.getBoundingBox(); // 3d or not depending on the
					// must make copy of the bounding box
					bbox = bbox instanceof Geodetic3DBounds ? new Geodetic3DBounds(
							(Geodetic3DBounds) bbox)
							: new Geodetic2DBounds(bbox);
					// geo
				} else {
					bbox.include(geo.getBoundingBox());
				}
				int len = getRecLen(geo);
				outputGeometry(channel, offset, geo, is3D, shape, recordNumber,
						len);
				outputIndex(xchannel, ioffset, offset, len);
				// Records have additional 4 words of info at the start of the
				// record
				offset += len + 4;
				ioffset += 4;
				recordNumber++;
				ser = buffer.read();
			}
			// Write header
			putShapeHeader(channel, offset, shapeAll, is3D, bbox);
			int shxlen = 50 + (recordNumber - 1) * 4;
			putShapeHeader(xchannel, shxlen, shapeAll, is3D, bbox);
		} finally {
			IOUtils.closeQuietly(shfos);
			IOUtils.closeQuietly(shxfos);
		}
	}

	/**
	 * Calculate the esri type
	 * 
	 * @param geo
	 * @return
	 */
	private int getEsriShapeType(Geometry geo) {
		EsriTypeVisitor tv = new EsriTypeVisitor();
		geo.accept(tv);
		return tv.getType();
	}

	private void outputIndex(FileChannel xchannel, int offset, int recoffset,
			int length) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		writeInt(buffer, recoffset, ByteOrder.BIG_ENDIAN);
		writeInt(buffer, length, ByteOrder.BIG_ENDIAN);
		buffer.flip();
		xchannel.write(buffer, offset * 2);
	}

	@Override
	public void visit(GeometryBag geobag) {
		throw new IllegalStateException(
				"Cannot output geometry bags to a shapefile");
	}

	@Override
	public void visit(Line line) {
		outputLines(line);
	}

	@Override
	public void visit(MultiLine multiLine) {
		outputLines(multiLine);
	}

	/**
	 * Output either a single or a group of lines
	 * 
	 * @param geo
	 */
	private void outputLines(Geometry geo) {
		/*
		 * A PolyLineZ consists of one or more parts. A part is a connected
		 * sequence of two or more points. Parts may or may not be connected to
		 * one another. Parts may or may not intersect one another. PolyLineZ {
		 * Double[4] Box // Bounding Box Integer NumParts // Number of Parts
		 * Integer NumPoints // Total Number of Points Integer[NumParts] Parts
		 * // Index to First Point in Part Point[NumPoints] Points // Points for
		 * All Parts Double[2] Z Range // Bounding Z Range Double[NumPoints] Z
		 * Array // Z Values for All Points Double[2] M Range // Bounding
		 * Measure Range Double[NumPoints] M Array // Measures }
		 */
		try {
			putBBox(geo.getBoundingBox());
			putPartCount(geo);
			putPartsVector(geo);
			putPointsXY(geo.getPoints());
			if (geo.is3D()) {
				putPointsZ(geo.getPoints());
				// todo: M Array // Measures ??
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(LinearRing ring) {
		outputPolygon(ring, 1);
	}

	@Override
	public void visit(MultiLinearRings rings) {
		try {
			PointOffsetVisitor pv = new PointOffsetVisitor();
			rings.accept(pv);
			putBBox(rings.getBoundingBox());
			writeInt(obuf, pv.getPartCount(), ByteOrder.LITTLE_ENDIAN);
			writeInt(obuf, pv.getTotal(), ByteOrder.LITTLE_ENDIAN);
			for (Integer offset : pv.getOffsets()) {
				writeInt(obuf, offset, ByteOrder.LITTLE_ENDIAN);
			}
			for (LinearRing ring : rings.getLinearRings()) {
				putPolyPointsXY(ring);
			}
			if (rings.is3D()) {
				writeDouble(obuf, pv.getZmin(), ByteOrder.LITTLE_ENDIAN);
				writeDouble(obuf, pv.getZmax(), ByteOrder.LITTLE_ENDIAN);
				for (LinearRing ring : rings.getLinearRings()) {
					putPartPointsZ(ring);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(MultiPolygons polygons) {
		try {
			PointOffsetVisitor pv = new PointOffsetVisitor();
			polygons.accept(pv);
			putBBox(polygons.getBoundingBox());
			writeInt(obuf, pv.getPartCount(), ByteOrder.LITTLE_ENDIAN);
			writeInt(obuf, pv.getTotal(), ByteOrder.LITTLE_ENDIAN);
			for (Integer offset : pv.getOffsets()) {
				writeInt(obuf, offset, ByteOrder.LITTLE_ENDIAN);
			}
			for (Polygon poly : polygons.getPolygons()) {
				putPolyPointsXY(poly.getOuterRing());
				for (LinearRing ring : poly.getLinearRings()) {
					putPolyPointsXY(ring);
				}
			}
			if (polygons.is3D()) {
				writeDouble(obuf, pv.getZmin(), ByteOrder.LITTLE_ENDIAN);
				writeDouble(obuf, pv.getZmax(), ByteOrder.LITTLE_ENDIAN);
				for (Polygon poly : polygons.getPolygons()) {
					putPartPointsZ(poly.getOuterRing());
					for (LinearRing ring : poly.getLinearRings()) {
						putPartPointsZ(ring);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(Polygon polygon) {
		outputPolygon(polygon, 1 + polygon.getLinearRings().size());
	}

	private void outputPolygon(Geometry geo, int partcount) {
		try {
			putBBox(geo.getBoundingBox());
			writeInt(obuf, partcount, ByteOrder.LITTLE_ENDIAN);
			// This call writes out the point count and the parts vector
			putPartsVector(geo);
			putPolyPointsXY(geo);
			if (geo.is3D()) {
				putPolyPointsZ(geo);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(MultiPoint multiPoint) {
		/*
		 * A MultiPointZ represents a set of PointZs, as follows: MultiPointZ {
		 * Double[4] Box // Bounding Box Integer NumPoints // Number of Points
		 * Point[NumPoints] Points // The Points in the Set Double[2] Z Range //
		 * Bounding Z Range Double[NumPoints] Z Array // Z Values Double[2] M
		 * Range // Bounding Measure Range Double[NumPoints] M Array // Measures
		 * } The Bounding Box is stored in the order Xmin, Ymin, Xmax, Ymax. The
		 * bounding Z Range is stored in the order Zmin, Zmax. Bounding M Range
		 * is stored in the order Mmin, Mmax
		 */
		try {
			putBBox(multiPoint.getBoundingBox());
			putPointCount(multiPoint);
			putPointsXY(multiPoint.getPoints());
			if (multiPoint.is3D()) {
				putPointsZ(multiPoint.getPoints());
				// TODO: not outputting M Range + M Array values...
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(Point point) {
		try {
			putPoint(point);
			if (point.is3D()) {
				putPointZ(point, true);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void outputGeometry(FileChannel channel, int offset, Geometry geom,
			boolean is3D, int shape, int rnumber, int rlength)
			throws IOException {
		ByteBuffer hbuffer = ByteBuffer.allocate(12);
		writeInt(hbuffer, rnumber, ByteOrder.BIG_ENDIAN);
		writeInt(hbuffer, rlength, ByteOrder.BIG_ENDIAN);
		writeInt(hbuffer, shape, ByteOrder.LITTLE_ENDIAN);
		hbuffer.flip();
		channel.write(hbuffer, offset * 2);
		obuf = ByteBuffer.allocate(rlength * 2);
		try {
			geom.accept(this);
		} catch (BufferOverflowException bfe) {
			logger.error("Overflow at having allocated " + rlength * 2
					+ " bytes for geometry " + geom + " having "
					+ geom.getNumPoints() + " points and " + geom.getNumParts() + " parts" );
			throw bfe;
		}
		obuf.flip();
		channel.write(obuf, offset * 2 + 12);
		obuf = null;
	}

	/**
	 * Output the z point range as well as the z points. If the geometry isn't a
	 * 3d geometry then output zero z values.
	 * 
	 * @param points
	 * @throws IOException
	 */
	private void putPointsZ(Collection<Point> points) throws IOException {
		if (points == null || points.size() == 0) {
			writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
			writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
		} else {
			Point first = points.iterator().next();
			if (first.is3D()) {
				Geodetic3DPoint pt = (Geodetic3DPoint) first.getCenter();
				double min = pt.getElevation();
				double max = pt.getElevation();
				for (Point point : points) {
					pt = (Geodetic3DPoint) point.getCenter();
					min = Math.min(pt.getElevation(), min);
					max = Math.min(pt.getElevation(), max);
				}
				writeDouble(obuf, min, ByteOrder.LITTLE_ENDIAN);
				writeDouble(obuf, max, ByteOrder.LITTLE_ENDIAN);
				for (Point point : points) {
					pt = (Geodetic3DPoint) point.getCenter();
					writeDouble(obuf, pt.getElevation(),
							ByteOrder.LITTLE_ENDIAN);
				}
			} else {
				writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
				writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < points.size(); i++) {
					writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
				}
			}
		}
	}

	/**
	 * Output point count and indices to each part in the point vector.
	 * 
	 * @param geom
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPartsVector(Geometry geom) throws IOException {
		PointOffsetVisitor pov = new PointOffsetVisitor();
		geom.accept(pov);
		writeInt(obuf, pov.getTotal(), ByteOrder.LITTLE_ENDIAN);
		for (Integer offset : pov.getOffsets()) {
			writeInt(obuf, offset, ByteOrder.LITTLE_ENDIAN);
		}
	}

	/**
	 * Count the total points for a polygon
	 * 
	 * @param geom
	 * @return
	 */
	private int getPolyPntCount(Geometry geom) {
		PolygonCountingVisitor pv = new PolygonCountingVisitor();
		geom.accept(pv);
		return pv.getPointCount();
	}

	/**
	 * Output each poly point's XY paying attention to the need to close the
	 * figure.
	 * 
	 * @param geom
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPolyPointsXY(Geometry geom) throws IOException {
		for (int j = 0; j < geom.getNumParts(); j++) {
			Geometry part = geom.getPart(j);
			int count = part.getNumPoints();
			if (count > 0) {
				putPointsXY(part.getPoints());
				Point first = part.getPoints().get(0);
				Point last = part.getPoints().get(count - 1);
				if (!first.equals(last)) {
					putPoint(first); // Dupl the first point to close the figure
				}
			}
		}
	}

	/**
	 * Output each poly point's Z paying attention to the need to close the
	 * figure. If the last and first point are not the same then we add the
	 * first point into the list again.
	 * 
	 * @param geom
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPolyPointsZ(Geometry geom) throws IOException {
		double zmax = 0.0, zmin = 0.0;
		for (int j = 0; j < geom.getNumParts(); j++) {
			Geometry part = geom.getPart(j);
			int count = part.getNumPoints();
			if (count > 0) {
				List<Point> pts = part.getPoints();
				Geodetic2DPoint fpt = pts.get(0).getCenter();
				if (fpt instanceof Geodetic3DPoint) {
					zmax = zmin = ((Geodetic3DPoint) fpt).getElevation();
					for (Point pt : pts) {
						Geodetic3DPoint geopt = (Geodetic3DPoint) pt
								.getCenter();
						zmax = Math.max(zmax, geopt.getElevation());
						zmin = Math.min(zmin, geopt.getElevation());
					}
				}
			}
		}
		writeDouble(obuf, zmin, ByteOrder.LITTLE_ENDIAN);
		writeDouble(obuf, zmax, ByteOrder.LITTLE_ENDIAN);

		putPartPointsZ(geom);
	}

	/**
	 * Output Z points for a given geometry for each of the geometry's parts
	 * in order. This is really just used for a ring or poly's geometry
	 * @param geom the geometry
	 * @throws IOException
	 */
	private void putPartPointsZ(Geometry geom) throws IOException {
		for (int j = 0; j < geom.getNumParts(); j++) {
			Geometry part = geom.getPart(j);
			int count = part.getNumPoints();
			if (count > 0) {
				List<Point> pts = part.getPoints();
				for (Point pt : pts) {
					putPointZ(pt, false);
				}
				Point first = pts.get(0);
				Point last = pts.get(count - 1);
				if (!first.equals(last)) {
					putPointZ(first, false);
				}
			}
		}
	}

	/**
	 * Output point count for the given geometry
	 * 
	 * @param geom
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPointCount(Geometry geom) throws IOException {
		writeInt(obuf, geom.getNumPoints(), ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Output part count for the given geometry
	 * 
	 * @param geom
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPartCount(Geometry geom) throws IOException {
		writeInt(obuf, geom.getNumParts(), ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Output points
	 * 
	 * @param points
	 *            the points
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPointsXY(Collection<Point> points) throws IOException {
		if (points != null && points.size() > 0) {
			for (Point p : points) {
				writeDouble(obuf, p.getCenter().getLongitude().inDegrees(),
						ByteOrder.LITTLE_ENDIAN);
				writeDouble(obuf, p.getCenter().getLatitude().inDegrees(),
						ByteOrder.LITTLE_ENDIAN);
			}
		}

	}

	/**
	 * Count required words to output the geometry information for a given
	 * geometry object. For any given type there are three components to sum:
	 * <ul>
	 * <li>base record size in words, which is the minimum length for the record
	 * that does not include the 4 words that every record has in the record
	 * header.
	 * <li>the count of words used per part
	 * <li>the count of words used per point
	 * </ul>
	 * <em>Note, we do not use the M formats at all. They are included
	 * here for completeness' sake.</em>
	 * 
	 * @param geom
	 *            the geo object
	 * @return the count in words (16 bit words) not including the record header
	 *         (4 words, record number and length)
	 */
	private int getRecLen(Geometry geom) {
		int nParts, nPoints;
		int type = getEsriShapeType(geom);
		int base;
		if (geom instanceof MultiPolygons) {
			MultiPolygons mp = (MultiPolygons) geom;
			nParts = 0;
			for(Polygon p : mp.getPolygons()) {
				nParts += p.getNumParts();
			}
		} else if (geom instanceof MultiLinearRings) {
			MultiLinearRings mp = (MultiLinearRings) geom;
			nParts = 0;
			for(LinearRing r : mp.getLinearRings()) {
				nParts += r.getNumParts();
			}
		} else {
			nParts = geom.getNumParts();
		}
		nPoints = geom.getNumPoints();
		int bytesPerPnt = 16;
		int bytesPerPart = 0;

		switch (type) {
		case 0: // Null shape
			base = 4;
			break; // Defaults are ok
		case 1: // Point
			base = 4;
			break; // Defaults are ok
		case 8: // Multipoint
		case 28: // MultiPointM
			base = 40;
			break;
		case 11: // PointZ
			base = 4;
			bytesPerPnt = 32;
			break;
		case 21: // PointM
			base = 4;
			bytesPerPnt = 24;
			break;
		case 18: // MultipointZ
			base = 56;
			bytesPerPnt = 24;
			break;
		case 5: // Polygon
		case 25: // PolygonM
			nPoints = getPolyPntCount(geom);
		case 3: // Polyline
		case 23: // PolyLineM
			base = 44;
			bytesPerPart = 4;
			break;
		case 15: // PolygonZ
			nPoints = getPolyPntCount(geom);
		case 13: // PolyLineZ
			base = 60;
			bytesPerPart = 4;
			bytesPerPnt = 24;
			break;
		case 31: // Multipatch
			base = 56;
			bytesPerPart = 8;
			bytesPerPnt = 32;
			break;
		default:
			throw new UnsupportedOperationException("Unsupported type " + type);
		}
		return (base + nParts * bytesPerPart + nPoints * bytesPerPnt) / 2;
	}

	/**
	 * private helper method to write out X-Y bounding box
	 * 
	 * @param
	 * @param bbox
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putBBox(Geodetic2DBounds bbox) throws IOException {
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
		writeDouble(obuf, westLonDeg, ByteOrder.LITTLE_ENDIAN); // X min
		writeDouble(obuf, southLatDeg, ByteOrder.LITTLE_ENDIAN); // Y min
		writeDouble(obuf, eastLonDeg, ByteOrder.LITTLE_ENDIAN); // X max
		writeDouble(obuf, northLatDeg, ByteOrder.LITTLE_ENDIAN); // Y max
	}

	/**
	 * Write the shapefile header, containing fileLen, shapeType and Bounding
	 * Box
	 * 
	 * @param channel
	 * @param fileLen
	 * @param shapeType
	 * @param is3D
	 * @param bbox
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putShapeHeader(FileChannel channel, int fileLen,
			int shapeType, boolean is3D, Geodetic2DBounds bbox)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(100);
		// Write the shapefile signature (should be 9994)
		writeInt(buffer, SIGNATURE, ByteOrder.BIG_ENDIAN);
		// fill unused bytes in header with zeros
		for (int i = 0; i < 5; i++)
			writeInt(buffer, 0, ByteOrder.BIG_ENDIAN);
		// Write the file length (total number of 2-byte words, including
		// header)
		writeInt(buffer, fileLen, ByteOrder.BIG_ENDIAN);
		// Write the shapefile version (should be 1000)
		writeInt(buffer, VERSION, ByteOrder.LITTLE_ENDIAN);
		// Write the shapeType
		writeInt(buffer, shapeType, ByteOrder.LITTLE_ENDIAN);
		// Write the overall X-Y bounding box to the shapefile header
		obuf = buffer;
		putBBox(bbox);
		obuf = null;
		// In Shapefiles, Z and M bounds are usually separated from X-Y
		// bounds
		// (and instead grouped with their arrays of data), except for in
		// the
		// header
		double zMin = (is3D) ? ((Geodetic3DBounds) bbox).minElev : 0.0;
		double zMax = (is3D) ? ((Geodetic3DBounds) bbox).maxElev : 0.0;
		writeDouble(buffer, zMin, ByteOrder.LITTLE_ENDIAN); // Z min
		writeDouble(buffer, zMax, ByteOrder.LITTLE_ENDIAN); // Z max
		writeDouble(buffer, 0.0, ByteOrder.LITTLE_ENDIAN); // M min (not
		// supported)
		writeDouble(buffer, 0.0, ByteOrder.LITTLE_ENDIAN); // M max (not
		// supported)
		buffer.flip();
		channel.write(buffer, 0);
	}

	/**
	 * Write a single X/Y point
	 * 
	 * @param pt
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPoint(Point pt) throws IOException {
		Geodetic2DPoint gp = pt.asGeodetic2DPoint();
		writeDouble(obuf, gp.getLongitude().inDegrees(),
				ByteOrder.LITTLE_ENDIAN);
		writeDouble(obuf, gp.getLatitude().inDegrees(), ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Put out the elevation value
	 * 
	 * @param pt
	 * @param writeM <code>true</code> if we should write the measure value
	 * @throws IOException
	 *             if an error occurs
	 */
	private void putPointZ(Point pt, boolean writeM) throws IOException {
		Geodetic2DPoint gp = pt.asGeodetic2DPoint();
		if (gp instanceof Geodetic3DPoint) {
			writeDouble(obuf, ((Geodetic3DPoint) gp).getElevation(),
					ByteOrder.LITTLE_ENDIAN);
		} else {
			writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
		}
		if (writeM) {
			// measure value M not used
			writeDouble(obuf, 0.0, ByteOrder.LITTLE_ENDIAN);
		}
	}

	/**
	 * Helper which sets the byte order and then writes the datum to the byte
	 * buffer
	 * 
	 * @param buffer
	 * @param datum
	 * @param order
	 */
	private void writeInt(ByteBuffer buffer, int datum, ByteOrder order) {
		buffer.order(order);
		buffer.putInt(datum);
	}

	/**
	 * Helper which sets the byte order and then writes the datum to the byte
	 * buffer
	 * 
	 * @param buffer
	 * @param datum
	 * @param order
	 */
	private void writeDouble(ByteBuffer buffer, double datum, ByteOrder order) {
		buffer.order(order);
		buffer.putDouble(datum);
	}
}

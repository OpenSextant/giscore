/****************************************************************************************
 *  SingleShapefileInputHandler.java
 *
 *  Created: Jul 22, 2009
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
package org.mitre.giscore.input.shapefile;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.dbf.DbfInputStream;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read a single shapefile (.prj, .shp, .dbf, etc) to an object buffer for later
 * processing.
 * 
 * @author DRAND
 */
public class SingleShapefileInputHandler extends GISInputStreamBase implements
		IGISInputStream {
	private static final Logger logger = LoggerFactory.getLogger(SingleShapefileInputHandler.class);
	
    // Constants
    protected static final int SIGNATURE = 9994;
    protected static final int VERSION = 1000;

    // These are the 2D ESRI shape types. Add 10 to get the equivalent 3D types.
    protected static final int NULL_TYPE = 0;
    protected static final int POINT_TYPE = 1;             // 1 is Point,     11 is PointZ
    protected static final int MULTILINE_TYPE = 3;         // 3 is MultiLine  13 is MultiLineZ
    protected static final int MULTINESTEDRINGS_TYPE = 5;  // 5 is Polygon    15 is PolygonZ
    protected static final int MULTIPOINT_TYPE = 8;        // 8 is MultiPoint 18 is MultiPointZ
    
	/**
	 * Schema, derived from the read dbf file
	 */
	private Schema schema;

	/**
	 * Style derived from the shm file if present
	 */
	private Style style;

	/*
	 * Files that hold the essential information for the shapefile.
	 */
	private File dbfFile;
	private File shpFile;
	private File prjFile;

	/**
	 * Generates to be returned features, which are decorated with the geo data
	 * from the shp file and returned.
	 */
	private DbfInputStream dbf;

	/**
	 * Open shp file as a binary input stream
	 */
	private BinaryInputStream stream;
	
	/**
	 * Geometry type for this shapefile
	 */
	private int shpType;

	public SingleShapefileInputHandler(File inputDirectory, String shapefilename)
			throws URISyntaxException, IOException {
		if (inputDirectory == null || !inputDirectory.exists()) {
			throw new IllegalArgumentException(
					"Input directory must exist and be non-null");
		}
		if (shapefilename == null) {
			throw new IllegalArgumentException(
					"shapefilename should never be null");
		}
		URI uri = new URI("urn:org:mitre:giscore:schema:"
				+ UUID.randomUUID().toString());
		schema = new Schema(uri);
		dbfFile = new File(inputDirectory, shapefilename + ".dbf");
		shpFile = new File(inputDirectory, shapefilename + ".shp");
		prjFile = new File(inputDirectory, shapefilename + ".prj");

		if (!dbfFile.exists()) {
			throw new IllegalArgumentException(
					"DBF file missing for shapefile " + shapefilename);
		}
		if (!shpFile.exists()) {
			throw new IllegalArgumentException(
					"SHP file missing for shapefile " + shapefilename);
		}
		if (prjFile.exists()) {
			checkPrj(prjFile);
		}

		dbf = new DbfInputStream(dbfFile, null);
		dbf.setRowClass(Feature.class);

		// First thing in the dbf should be a schema
		IGISObject ob = dbf.read();
		if (ob instanceof Schema) {
			schema = (Schema) ob;
			addFirst(ob);
		} else {
			throw new IllegalStateException(
					"Schema not the first thing returned from dbf");
		}

		FileInputStream fis = new FileInputStream(shpFile);
		stream = new BinaryInputStream(fis);
		readHeader();
	}

	/**
	 * Check contents of the prj file to
	 * 
	 * @param prjFile
	 * @throws IOException if an I/O error occurs 
	 * @throws UnsupportedEncodingException 
	 */
	private void checkPrj(File prjFile) throws IOException {
		FileInputStream fis = new FileInputStream(prjFile);
		
		try {
			Reader reader = new InputStreamReader(fis, "UTF8");
			StringWriter writer = new StringWriter();
			IOUtils.copy(reader, writer);
			WKTReader wkt = new WKTReader(writer.toString());
			WKTReader.Entry geogcs = wkt.getEntry("GEOGCS");
			if (geogcs != null && geogcs.getValues().size() > 0) {
				Object v1 = geogcs.getValues().get(0);
				if (v1 instanceof String) {
					String datum = (String) v1;
					if (! datum.equals("GCS_WGS_1984")) {
						logger.warn("Shapefile is not a WGS 84 datum");
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	/**
	 * Closes this input stream and releases any system resources 
     * associated with the stream.
	 * @throws RuntimeException if an I/O error occurs closing shp stream  
	 */
	public void close() {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				throw new RuntimeException("Problem closing shp stream", e);
			}
			stream = null;
		}
		if (dbf != null) {
			dbf.close();
			dbf = null;
		}
	}

	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return readSaved();
		} else {
			return readNext();
		}
	}
	
	/**
	 * Read the next feature from the shapefile. A shapefile will contain a 
	 * uniform set of geometry features. 
	 * @return the next feature or <code>null</code> if we are done.
	 * @throws IOException if an I/O error occurs 
	 */
    private IGISObject readNext() throws IOException {
    	Feature f = (Feature) dbf.read();
    	boolean is3D = is3D(shpType);
    	if (f != null) {
    		Geometry geo = getGeometry(is3D);
    		f.setGeometry(geo);
    	}
		return f;
	}
    
    // Read the next Geometry Object and validate type. Return null at valid EOF.
    private Geometry getGeometry(boolean is3D)
            throws IOException, IllegalArgumentException {
        // EOF is OK if it occurs here, otherwise we'll throw the exception to caller
        try {
            stream.readInt(ByteOrder.BIG_ENDIAN);
        } catch (EOFException eof) {
            return null;
        }
        int contentLen = stream.readInt(ByteOrder.BIG_ENDIAN);
        if (contentLen <= 4)
            throw new IOException("Shapefile contains badly formatted record");
        Geometry geomObj = null;
        int recShapeType = stream.readInt(ByteOrder.LITTLE_ENDIAN);
        if (recShapeType != NULL_TYPE) {
            if (recShapeType != shpType)
                throw new IOException("Shapefile contains record with " +
                        "unexpected shape type " + recShapeType + ", expecting " + shpType);
            // Now read the record content, corresponding to specified shapeType (adjusted if 3D)
            int st = (is3D) ? shpType - 10 : shpType;
            if (st == POINT_TYPE) geomObj = getPoint(is3D);
            else {
                // Skip over Bounding Box coordinates (we'll reconstruct directly from points)
                for (int i = 0; i < 4; i++) stream.readDouble(ByteOrder.LITTLE_ENDIAN);
                if (st == MULTILINE_TYPE) geomObj = getPolyLine(is3D);
                else if (st == MULTINESTEDRINGS_TYPE) geomObj = getPolygon(is3D);
                else if (st == MULTIPOINT_TYPE) geomObj = getMultipoint(is3D);
                else throw new IOException("Shapefile contains shape type (" +
                        shpType + ") that is currently unsupported");
            }
        }
        return geomObj;
    }
    
    /**
     * Utility method to test for 3D geometry based on shapeType code
     * 
     * @param shapeType
     * @return
     */
    protected boolean is3D(int shapeType) {
        return (shapeType > 10);
    }

	/**
	 * Read the header from the stream
	 * @throws IOException
	 * @throws IllegalArgumentException 
	 */
	private void readHeader() throws IllegalArgumentException, IOException {
		shpType = getShapeTypeFromHeader();
		Geodetic2DBounds bbox = getBoundingBoxFromHeader(is3D(shpType));
	}
	
	// Read first part of shapefile header and get shapeType if possible
    private int getShapeTypeFromHeader()
            throws IOException, IllegalArgumentException {
        // Read and validate the shapefile signature (should be 9994)
        int fileSig = stream.readInt(ByteOrder.BIG_ENDIAN);
        if (fileSig != SIGNATURE)
            throw new IllegalArgumentException("Invalid Shapefile signature");
        // Skip over unused bytes in header
        for (int i = 0; i < 5; i++) stream.readInt(ByteOrder.BIG_ENDIAN);
        // Read the file length (total number of 2-byte words, including header)
        stream.readInt(ByteOrder.BIG_ENDIAN);  // fileLen (not used)
        // Read and validate the shapefile version (should be 1000)
        int version = stream.readInt(ByteOrder.LITTLE_ENDIAN);
        if (version != VERSION)
            throw new IllegalArgumentException("Invalid Shapefile version " + version);
        // Read return the shapeType (validation is done later)
        return stream.readInt(ByteOrder.LITTLE_ENDIAN);
    }

    // Read the remainder of shapefile header and get bounding box if possible
    private Geodetic2DBounds getBoundingBoxFromHeader(boolean is3D)
            throws IOException {
        // Read Bounding Box coordinates (assume WGS-84 decimal degrees, elevation in meters)
        double xMin = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        double yMin = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        double xMax = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        double yMax = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        double zMin = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        double zMax = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
        // Read remaining header fields (not currently used)
        stream.readDouble(ByteOrder.LITTLE_ENDIAN);  // mMin (not used)
        stream.readDouble(ByteOrder.LITTLE_ENDIAN);  // mMax (not used)
        // See if the shapeType is valid and whether the points are 3D
        Geodetic2DBounds bbox;
        if (is3D) {
            Geodetic3DPoint sw = new Geodetic3DPoint(new Longitude(xMin, Angle.DEGREES),
                    new Latitude(yMin, Angle.DEGREES), zMin);
            Geodetic3DPoint ne = new Geodetic3DPoint(new Longitude(xMax, Angle.DEGREES),
                    new Latitude(yMax, Angle.DEGREES), zMax);
            bbox = new Geodetic3DBounds(sw);
            bbox.include(ne);
        } else {
            Geodetic2DPoint sw = new Geodetic2DPoint(new Longitude(xMin, Angle.DEGREES),
                    new Latitude(yMin, Angle.DEGREES));
            Geodetic2DPoint ne = new Geodetic2DPoint(new Longitude(xMax, Angle.DEGREES),
                    new Latitude(yMax, Angle.DEGREES));
            bbox = new Geodetic2DBounds(sw);
            bbox.include(ne);
        }
        return bbox;
    }

    // Read next Point (ESRI Point or PointZ) record
    private Point getPoint(boolean is3D)
            throws IOException {
        Geodetic2DPoint gp;
        Longitude lon = new Longitude(stream.readDouble(ByteOrder.LITTLE_ENDIAN), Angle.DEGREES);
        Latitude lat = new Latitude(stream.readDouble(ByteOrder.LITTLE_ENDIAN), Angle.DEGREES);
        if (!is3D)
            gp = new Geodetic2DPoint(lon, lat); // ESRI Point type (X and Y fields)
        else {
            // ESRI PointZ type (x, y, z, and m fields)
            gp = new Geodetic3DPoint(lon, lat, stream.readDouble(ByteOrder.LITTLE_ENDIAN));
            stream.readDouble(ByteOrder.LITTLE_ENDIAN); // measure value M not used
        }
        return new Point(gp);
    }

    // Read PolyLine and Polygon parts offset array
    private int[] getPartOffsets(int nParts, int nPoints)
            throws IOException {
        // read index array of starting positions for each part, put total numPoints at end
        int[] parts = new int[nParts + 1];
        parts[nParts] = nPoints;
        for (int i = 0; i < nParts; i++) parts[i] = stream.readInt(ByteOrder.LITTLE_ENDIAN);
        return parts;
    }
    
    // Read PolyLine and Polygon point values and return Geodetic point array
    private Geodetic2DPoint[] getPolyPoints(int nPoints, boolean is3D)
            throws IOException {
        Geodetic2DPoint[] pts;
        // Read the X and Y points into arrays
        double[] x = new double[nPoints];
        double[] y = new double[nPoints];
        for (int i = 0; i < nPoints; i++) {
            x[i] = stream.readDouble(ByteOrder.LITTLE_ENDIAN); // Longitude
            y[i] = stream.readDouble(ByteOrder.LITTLE_ENDIAN); // Latitude
        }
        // If 3D, read the Z bounds + values and skip over rest of record (M bounds and values)
        if (is3D) {
            stream.readDouble(ByteOrder.LITTLE_ENDIAN); // skip Zmin
            stream.readDouble(ByteOrder.LITTLE_ENDIAN); // skip Zmax
            double[] z = new double[nPoints];
            for (int i = 0; i < nPoints; i++)
                z[i] = stream.readDouble(ByteOrder.LITTLE_ENDIAN);
            stream.readDouble(ByteOrder.LITTLE_ENDIAN); // skip Mmin
            stream.readDouble(ByteOrder.LITTLE_ENDIAN); // skip Mmax
            for (int i = 0; i < nPoints; i++)
                stream.readDouble(ByteOrder.LITTLE_ENDIAN); // skip measured vals
            // Convert x, y, and z values into Geodetic points
            pts = new Geodetic3DPoint[nPoints];
            for (int i = 0; i < nPoints; i++)
                pts[i] = new Geodetic3DPoint(new Longitude(x[i], Angle.DEGREES),
                        new Latitude(y[i], Angle.DEGREES), z[i]);
        } else {
            // Convert x and y values into Geodetic points
            pts = new Geodetic2DPoint[nPoints];
            for (int i = 0; i < nPoints; i++)
                pts[i] = new Geodetic2DPoint(new Longitude(x[i], Angle.DEGREES),
                        new Latitude(y[i], Angle.DEGREES));
        }
        return pts;
    }
    
    // Read next MultiLine (ESRI Polyline or PolylineZ) record
    // Take flattened file structure and construct the part hierarchy
    private MultiLine getPolyLine(boolean is3D)
            throws IOException {
        int nParts = stream.readInt(ByteOrder.LITTLE_ENDIAN);
        int nPoints = stream.readInt(ByteOrder.LITTLE_ENDIAN);  // total numPoints
        int[] parts = getPartOffsets(nParts, nPoints);
        Geodetic2DPoint[] pts = getPolyPoints(nPoints, is3D);
        ArrayList<Line> lnList = new ArrayList<Line>();
        // Collect up the Geodetic points into the line parts
        int k = 0; // point index
        for (int j = 1; j <= nParts; j++) {
            ArrayList<Point> ptList = new ArrayList<Point>();
            int n = parts[j] - parts[j - 1];
            for (int i = 0; i < n; i++) ptList.add(new Point(pts[k++]));
            lnList.add(new Line(ptList));
        }
        return new MultiLine(lnList);
    }
    

    // TODO - we need some topology checks and repairs, including international date line
    // TODO - polygon splitting, and checking for non-containment of inner polygons
    // TODO - which should be elevated to their own outer rings (and reverse the point
    // TODO - order.

    // TODO - note: all cntry02 inner rings intersect with their outer rings
    // TODO - but they go counter-clockwise (we could make them separate outers, but
    // TODO - we need to reverse the point order) - write LinearRing reverse method to help
    // Read next MultiPolygons (ESRI Polygon or PolygonZ) record
    // Take flattened file structure and construct the nested part hierarchy
    private MultiPolygons getPolygon(boolean is3D)
            throws IOException, IllegalArgumentException {
        int nParts = stream.readInt(ByteOrder.LITTLE_ENDIAN);
        int nPoints = stream.readInt(ByteOrder.LITTLE_ENDIAN);  // total numPoints
        int[] parts = getPartOffsets(nParts, nPoints);
        Geodetic2DPoint[] pts = getPolyPoints(nPoints, is3D);
        // Shapefiles allow multiple outer rings intermixed with multiple inner rings
        // Our MultiLinearRings Object requires 1 outer and 0 or more inner.  We'll assume
        // inner rings follow their outer ring, and use direction as a list delimiter.
        ArrayList<Polygon> polyList = new ArrayList<Polygon>();
        ArrayList<LinearRing> rgList = null;
        LinearRing outerRing = null;
        int k = 0; // point index
        for (int j = 1; j <= nParts; j++) {
            ArrayList<Point> ptList = new ArrayList<Point>();
            int n = parts[j] - parts[j - 1];
            for (int i = 0; i < n; i++) ptList.add(new Point(pts[k++]));
            LinearRing r = new LinearRing(ptList);
            if (r.clockwise()) {
                // If ring is clockwise, assume it is an outer ring and start a new list
                if (outerRing != null) polyList.add(new Polygon(outerRing, rgList));
                rgList = new ArrayList<LinearRing>();
                outerRing = r;
            } else if (rgList == null || rgList.size() == 1) {
                // otherwise, verify that it is not the first in list
                throw new IllegalArgumentException("First (outer) ring should be " +
                        "in clockwise point order");
            } else {
                rgList.add(r);
            }
        }
        if (outerRing != null) polyList.add(new Polygon(outerRing, rgList));
        return new MultiPolygons(polyList);
    }
    
    // Read next MultiPoint (ESRI MultiPoint or MultiPointZ) record
    private MultiPoint getMultipoint(boolean is3D)
            throws IOException {
        int nPoints = stream.readInt(ByteOrder.LITTLE_ENDIAN);  // total numPoints
        Geodetic2DPoint[] pts = getPolyPoints(nPoints, is3D);
        ArrayList<Point> ptList = new ArrayList<Point>();
        for (int i = 0; i < nPoints; i++) ptList.add(new Point(pts[i]));
        return new MultiPoint(ptList);
    }
}

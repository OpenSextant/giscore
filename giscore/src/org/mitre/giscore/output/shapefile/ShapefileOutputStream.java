package org.mitre.giscore.output.shapefile;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.FeatureSorter;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DBounds;
import org.mitre.itf.geodesy.Geodetic3DPoint;

/**
 * Output stream for shapefile creation. The basic output routines are lifted
 * from the transfusion mediate package.
 * 
 * @author DRAND
 *
 */
public class ShapefileOutputStream extends ShapefileBaseClass implements IGISOutputStream {
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
	private Map<FeatureKey, String> datasets = new HashMap<FeatureKey, String>();
	
	@Override
	public void write(IGISObject object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}


    /**
     * private helper method to load Geodetic Point coordinates into arrays of doubles
     * @param i
     * @param lleDeg
     * @param is3D
     * @param pt
     * @param wrap
     */
    private void loadPtData(int i, double[][] lleDeg, boolean is3D,
                                   Geodetic2DPoint pt, boolean wrap) {
        double lonDeg = pt.getLongitude().inDegrees();
        if (wrap && (lonDeg == -180.0)) lonDeg = 180.0;
        lleDeg[0][i] = lonDeg;
        lleDeg[1][i] = pt.getLatitude().inDegrees();
        if (is3D) lleDeg[2][i] = ((Geodetic3DPoint) pt).getElevation();
    }

    /**
     * private helper method to write out X-Y bounding box
     * @param boS
     * @param bbox
     * @throws IOException
     */
    private void putBBox(BinaryOutputStream boS, Geodetic2DBounds bbox) throws IOException {
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
        boS.writeDouble(westLonDeg, ByteOrder.LITTLE_ENDIAN);     // X min
        boS.writeDouble(southLatDeg, ByteOrder.LITTLE_ENDIAN);    // Y min
        boS.writeDouble(eastLonDeg, ByteOrder.LITTLE_ENDIAN);     // X max
        boS.writeDouble(northLatDeg, ByteOrder.LITTLE_ENDIAN);    // Y max
    }
    

    /**
     * Write the shapefile header, containing fileLen, shapeType and Bounding Box
     * @param boS
     * @param fileLen
     * @param shapeType
     * @param is3D
     * @param bbox
     * @throws IOException
     */
    private void putShapeHeader(BinaryOutputStream boS, int fileLen, int shapeType,
                               boolean is3D, Geodetic2DBounds bbox) throws IOException {
        // Write the shapefile signature (should be 9994)
        boS.writeInt(SIGNATURE, ByteOrder.BIG_ENDIAN);
        // fill unused bytes in header with zeros
        for (int i = 0; i < 5; i++) boS.writeInt(0, ByteOrder.BIG_ENDIAN);
        // Write the file length (total number of 2-byte words, including header)
        boS.writeInt(fileLen, ByteOrder.BIG_ENDIAN);
        // Write the shapefile version (should be 1000)
        boS.writeInt(VERSION, ByteOrder.LITTLE_ENDIAN);
        // Write the shapeType
        boS.writeInt(shapeType, ByteOrder.LITTLE_ENDIAN);
        // Write the overall X-Y bounding box to the shapefile header
        putBBox(boS, bbox);
        // In Shapefiles, Z and M bounds are usually separated from X-Y bounds
        // (and instead grouped with their arrays of data), except for in the header
        double zMin = (is3D) ? ((Geodetic3DBounds) bbox).minElev : 0.0;
        double zMax = (is3D) ? ((Geodetic3DBounds) bbox).maxElev : 0.0;
        boS.writeDouble(zMin, ByteOrder.LITTLE_ENDIAN);     // Z min
        boS.writeDouble(zMax, ByteOrder.LITTLE_ENDIAN);     // Z max
        boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN);      // M min (not supported)
        boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN);      // M max (not supported)
    }
    
    /**
     * Write next Point (ESRI Point or PointZ) record
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
            boS.writeDouble(((Geodetic3DPoint) gp).getElevation(), ByteOrder.LITTLE_ENDIAN);
            boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN);  // measure value M not used
        }
    }

    /**
     * Write PolyLine and Polygon point values from array of Geodetic points
     * @param boS
     * @param bbox
     * @param lleDeg
     * @throws IOException
     */
    private void putPolyPoints(BinaryOutputStream boS, Geodetic2DBounds bbox,
                                      double[][] lleDeg) throws IOException {
        // lleDeg contains the Lon, Lat, and (optionally) Elevation values in decimal degrees
        // Write the x and y points
        int nPoints = lleDeg[0].length;
        for (int i = 0; i < nPoints; i++) {
            boS.writeDouble(lleDeg[0][i], ByteOrder.LITTLE_ENDIAN); // Longitude
            boS.writeDouble(lleDeg[1][i], ByteOrder.LITTLE_ENDIAN); // Latitude
        }
        // If 3D, write the Z bounds + values and zeroed out (unused) M bounds and values
        if (lleDeg.length == 3) {
            boS.writeDouble(((Geodetic3DBounds) bbox).minElev, ByteOrder.LITTLE_ENDIAN); // Z min
            boS.writeDouble(((Geodetic3DBounds) bbox).maxElev, ByteOrder.LITTLE_ENDIAN); // Z max
            for (int i = 0; i < nPoints; i++)
                boS.writeDouble(lleDeg[2][i], ByteOrder.LITTLE_ENDIAN); // Elevation
            boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M min (not supported)
            boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN); // M max (not supported)
            for (int i = 0; i < nPoints; i++) boS.writeDouble(0.0, ByteOrder.LITTLE_ENDIAN);
        }
    }
    

    // Write next MultiLine (ESRI Polyline or PolylineZ) record
    // Take part hierarchy and flatten in file structure
    private void putPolyLine(BinaryOutputStream boS, boolean is3D, Geometry geom)
            throws IllegalArgumentException, IOException {
        boS.writeInt(geom.getNumParts(), ByteOrder.LITTLE_ENDIAN);
        int nPoints = geom.getNumPoints();
        boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN);
        double[][] lleDeg = (is3D) ? new double[3][nPoints] : new double[2][nPoints];
        int pointIndex = 0;
        boolean wrap;
        // Note that Line and MultiLine objects are valid Geometry inputs to this method
        if (geom instanceof Line) {
            boS.writeInt(0, ByteOrder.LITTLE_ENDIAN);
            wrap = ((Line) geom).clippedAtDateLine();
            for (Point pt : (Line) geom)
                loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
        } else if (geom instanceof MultiLine) {
            int partOffset = 0;
            for (Line ln : (MultiLine) geom) {
                boS.writeInt(partOffset, ByteOrder.LITTLE_ENDIAN);
                partOffset += ln.getNumPoints();
                wrap = ln.clippedAtDateLine();
                for (Point pt : ln)
                    loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
            }
        }
        else
            throw new IllegalArgumentException("Invalid Geometry object " + geom);
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
        double[][] lleDeg = (is3D) ? new double[3][nPoints] : new double[2][nPoints];
        // LinearRing, MultiLinearRings and MultiPolygons objects are valid Geometry types here
        int partOffset = 0;
        int partIndex = 0;
        int pointIndex = 0;
        boolean wrap;
        
        if (geom instanceof LinearRing) {
            parts[partIndex] = partOffset;
            wrap = ((LinearRing) geom).clippedAtDateLine();
            for (Point pt : (LinearRing) geom)
                loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
        } else if (geom instanceof MultiLinearRings) {
            for (LinearRing rg : (MultiLinearRings) geom) {
                parts[partIndex++] = partOffset;
                wrap = rg.clippedAtDateLine();
                for (Point pt : rg)
                    loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
                partOffset += rg.getNumPoints();
            }
        } else if (geom instanceof Polygon) {
                Polygon poly = (Polygon)geom;
                // handle outer ring
                LinearRing ring = poly.getOuterRing();
                parts[partIndex++] = partOffset;
                wrap = ring.clippedAtDateLine();
                for (Point pt : ring)
                    loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
                partOffset += ring.getNumPoints();
                // handle inner rings
                for (LinearRing rg : poly) {
                    parts[partIndex++] = partOffset;
                    wrap = rg.clippedAtDateLine();
                    for (Point pt : rg)
                        loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
                    partOffset += rg.getNumPoints();
                }            
        } else if (geom instanceof MultiPolygons) {
            for (Polygon nr : (MultiPolygons) geom) {
                // handle outer ring
                LinearRing ring = nr.getOuterRing();
                parts[partIndex++] = partOffset;
                wrap = ring.clippedAtDateLine();
                for (Point pt : ring)
                    loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
                partOffset += ring.getNumPoints();
                // handle inner rings
                for (LinearRing rg : nr) {
                    parts[partIndex++] = partOffset;
                    wrap = rg.clippedAtDateLine();
                    for (Point pt : rg)
                        loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), wrap);
                    partOffset += rg.getNumPoints();
                }
            }
        } else
            throw new IllegalArgumentException("Invalid Geometry object " + geom);

        // Write out the counts, part offsets, and points
        boS.writeInt(nParts, ByteOrder.LITTLE_ENDIAN);
        boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < nParts; i++)
            boS.writeInt(parts[i], ByteOrder.LITTLE_ENDIAN);
        putPolyPoints(boS, geom.getBoundingBox(), lleDeg);
    }
    
    /**
     * Write next MultiPoint (ESRI MultiPoint or MultiPointZ) record
     * @param boS
     * @param is3D
     * @param mp
     * @throws IOException
     */
    private void putMultipoint(BinaryOutputStream boS, boolean is3D, MultiPoint mp)
            throws IOException {
        int nPoints = mp.getNumPoints();
        boS.writeInt(nPoints, ByteOrder.LITTLE_ENDIAN);  // total numPoints
        double[][] lleDeg = (is3D) ? new double[3][nPoints] : new double[2][nPoints];
        int pointIndex = 0;
        for (Point pt : mp) loadPtData(pointIndex++, lleDeg, is3D, pt.asGeodetic2DPoint(), false);
        putPolyPoints(boS, mp.getBoundingBox(), lleDeg);
    }
    
    
}

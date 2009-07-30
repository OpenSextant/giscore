package org.mitre.giscore.output.shapefile;

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.events.Comment;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.NetworkLinkControl;
import org.mitre.giscore.events.PhotoOverlay;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.ScreenOverlay;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.geometry.Circle;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Model;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;

/**
 * Common methods and constants for use with both input and output of shapefiles
 * as GIS objects. Values copied from <code>ShpHandler</code> in transfusion's mediate
 * package.
 * 
 * @author DRAND
 * 
 */
public abstract class ShapefileBaseClass implements IStreamVisitor {
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
     * Utility method to test for 3D geometry based on shapeType code
     * 
     * @param shapeType
     * @return
     */
    protected boolean is3D(int shapeType) {
        return (shapeType > 10);
    }
    
    /**
     * This predicate method determines if the specified ESRI shapeType is valid, and then
     * if it is a 2D or a 3D type. If invalid, an IllegalArgumentException is thrown.
     * @param esriShapeType
     * @return
     * @throws IllegalArgumentException
     */
    protected boolean is3DType(int esriShapeType) throws IllegalArgumentException {
        // Loop thru the type comparisons twice, once for ESRI 2D types and then for
        // 3D types. Short-circuit exit if match is found.
        int st = esriShapeType;
        boolean valid3D = false;
        for (int i = 0; i < 2; i++) {
            if (st == POINT_TYPE) return valid3D;
            else if (st == MULTIPOINT_TYPE) return valid3D;
            else if (st == MULTILINE_TYPE) return valid3D;
            else if (st == MULTINESTEDRINGS_TYPE) return valid3D;
            st -= 10;   // 3D (Z Types) are exactly 10 more than 2D type ints
            valid3D = true;
        }
        // If we are still here, no type matches, so throw exception
        throw new IllegalArgumentException("Invalid ESRI Shape Type " + esriShapeType);
    }
    

    /**
     * Visting a row causes an error for non-row oriented output streams
     * @param row
     */
    public void visit(Row row) {
    	throw new UnsupportedOperationException("Can't output a tabular row");
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.IStreamVisitor#visit(org.mitre.giscore.events.NetworkLink)
	 */
	public void visit(NetworkLink link) {
		visit((Feature) link);
	}

	/**
	 * Visit NetworkLinkControl.
	 * Default behavior ignores NetworkLinkControls 
	 * @param networkLinkControl
	 */
	public void visit(NetworkLinkControl networkLinkControl) {
		// Ignored by default
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.IStreamVisitor#visit(org.mitre.giscore.events.PhotoOverlay)
	 */
	public void visit(PhotoOverlay overlay) {
		visit((Feature) overlay);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.IStreamVisitor#visit(org.mitre.giscore.events.ScreenOverlay)
	 */
	public void visit(ScreenOverlay overlay) {
		visit((Feature) overlay);
	}

	/**
     * @param overlay
     */
    public void visit(GroundOverlay overlay) {
    	visit((Feature) overlay);
    } 

	
    /**
	 * @param point
	 */
	public void visit(Point point) {
		// do nothing
	}

    /**
     * @param multiPoint
     */
    public void visit(MultiPoint multiPoint) {
        for (Point point : multiPoint) {
            point.accept(this);
        }
    }

    /**
     * @param line
     */
    public void visit(Line line) {
        for (Point pt : line) {
            pt.accept(this);
        }
	}

    /**
     * @param geobag a geometry bag
     */
    public void visit(GeometryBag geobag) {
    	for(Geometry geo : geobag) {
    		geo.accept(this);
    	}
    }
    
	/**
	 * @param multiLine
	 */
	public void visit(MultiLine multiLine) {
        for (Line line : multiLine) {
            line.accept(this);
        }
	}

	/**
	 * @param ring
	 */
	public void visit(LinearRing ring) {
        for (Point pt : ring) {
            pt.accept(this);
        }
	}

    /**
     * @param rings
     */
    public void visit(MultiLinearRings rings) {
        for (LinearRing ring : rings) {
            ring.accept(this);
        }
    }

    /**
	 * @param polygon
	 */
	public void visit(Polygon polygon) {
        polygon.getOuterRing().accept(this);
        for (LinearRing ring : polygon.getLinearRings()) {
            ring.accept(this);
        }
	}

    /**
     * @param polygons
     */
    public void visit(MultiPolygons polygons) {
        for (Polygon polygon : polygons) {
            polygon.accept(this);
        }
    }

	public void visit(Comment comment) {
		// Ignored by default
	}

    public void visit(Model model) {
        // Ignored by default
    }

	/**
     * Handle the output of a Circle
     *
     * @param circle the circle
     */
    public void visit(Circle circle) {
        // treat as Point by default
        visit((Point)circle);
    }
    
	@Override
	public void visit(DocumentStart documentStart) {
		// Ignore
	}

	@Override
	public void visit(ContainerStart containerStart) {
		// Ignore		
	}

	@Override
	public void visit(StyleMap styleMap) {
		// Ignore		
	}

	@Override
	public void visit(Style style) {
		// Ignore
	}

	@Override
	public void visit(Schema schema) {
		// Ignore
	}

	@Override
	public void visit(Feature feature) {
		// Ignore
	}

	@Override
	public void visit(ContainerEnd containerEnd) {
		// Ignore
	}
}

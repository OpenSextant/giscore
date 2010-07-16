/****************************************************************************************
 *  StreamVisitorBase.java
 *
 *  Created: Jan 30, 2009
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
package org.mitre.giscore.output;

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;

/**
 * The stream visitor base extends the original visitor base and changes the
 * default behaviors to be compatible with the new stream elements. It hides
 * the elements that should no longer be used (org.mitre.itf.Feature and 
 * org.mitre.itf.ThematicLayer). 
 * 
 * @author DRAND
 *
 */
public class StreamVisitorBase implements IStreamVisitor {

    /**
	 * Default behavior ignores containers
	 * @param containerStart 
	 */
	public void visit(ContainerStart containerStart) {
		// Ignored by default
	}

	/**
	 * @param styleMap
	 */
	public void visit(StyleMap styleMap) {
		// Ignored by default
	}

	/**
	 * @param style
	 */
	public void visit(Style style) {
		// Ignored by default		
	}

	/**
	 * @param schema
	 */
	public void visit(Schema schema) {
		// Ignored by default	
	}

    /**
     * Visting a row causes an error for non-row oriented output streams
     * @param row
     */
    public void visit(Row row) {
    	throw new UnsupportedOperationException("Can't output a tabular row");
    }
    
	/**
	 * @param feature
	 */
	public void visit(Feature feature) {
		if (feature.getGeometry() != null) {
			feature.getGeometry().accept(this);
		}
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
	 * @param documentStart
	 */
	public void visit(DocumentStart documentStart) {
		// Ignored by default
	}

	/**
	 * @param containerEnd
	 */
	public void visit(ContainerEnd containerEnd) {
		// Ignored by default	
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

    public void visit(Element element) {
    	// Ignored by default
    }
}

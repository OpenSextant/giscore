/****************************************************************************************
 *  IStreamVisitor.java
 *
 *  Created: Apr 16, 2009
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
package org.opensextant.giscore;

import org.opensextant.giscore.events.AtomHeader;
import org.opensextant.giscore.events.Comment;
import org.opensextant.giscore.events.ContainerEnd;
import org.opensextant.giscore.events.ContainerStart;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Element;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.GroundOverlay;
import org.opensextant.giscore.events.NetworkLink;
import org.opensextant.giscore.events.NetworkLinkControl;
import org.opensextant.giscore.events.PhotoOverlay;
import org.opensextant.giscore.events.Row;
import org.opensextant.giscore.events.Schema;
import org.opensextant.giscore.events.ScreenOverlay;
import org.opensextant.giscore.events.Style;
import org.opensextant.giscore.events.StyleMap;
import org.opensextant.giscore.geometry.Circle;
import org.opensextant.giscore.geometry.GeometryBag;
import org.opensextant.giscore.geometry.Line;
import org.opensextant.giscore.geometry.LinearRing;
import org.opensextant.giscore.geometry.Model;
import org.opensextant.giscore.geometry.MultiLine;
import org.opensextant.giscore.geometry.MultiLinearRings;
import org.opensextant.giscore.geometry.MultiPoint;
import org.opensextant.giscore.geometry.MultiPolygons;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.geometry.Polygon;

/**
 * @author DRAND
 *
 */
public interface IStreamVisitor {

	/**
	 * Default behavior ignores containers
	 * @param containerStart 
	 */
	public void visit(ContainerStart containerStart);

	/**
	 * @param styleMap
	 */
	public void visit(StyleMap styleMap);

	/**
	 * @param style
	 */
	public void visit(Style style);

	/**
	 * @param schema
	 */
	public void visit(Schema schema);

	/**
	 * Visting a row causes an error for non-row oriented output streams
	 * @param row
	 */
	public void visit(Row row);
	
	/**
	 * @param feature
	 */
	public void visit(Feature feature);

	/**
	 * @param link
	 */
	public void visit(NetworkLink link);

	/**
	 * @param networkLinkControl
	 */
	public void visit(NetworkLinkControl networkLinkControl);	
	
	/**
	 * @param overlay
	 */
	public void visit(GroundOverlay overlay);	
	
	/**
	 * @param overlay
	 */
	public void visit(PhotoOverlay overlay);
	
	/**
	 * @param overlay
	 */
	public void visit(ScreenOverlay overlay);

	/**
	 * @param documentStart
	 */
	public void visit(DocumentStart documentStart);

	/**
	 * @param containerEnd
	 */
	public void visit(ContainerEnd containerEnd);

	/**
	 * @param point
	 */
	public void visit(Point point);

	/**
	 * @param multiPoint
	 */
	public void visit(MultiPoint multiPoint);

	/**
	 * @param line
	 */
	public void visit(Line line);

	/**
	 * @param geobag a geometry bag
	 */
	public void visit(GeometryBag geobag);

	/**
	 * @param multiLine
	 */
	public void visit(MultiLine multiLine);

	/**
	 * @param ring
	 */
	public void visit(LinearRing ring);

	/**
	 * @param rings
	 */
	public void visit(MultiLinearRings rings);

	/**
	 * @param polygon
	 */
	public void visit(Polygon polygon);

	/**
	 * @param polygons
	 */
	public void visit(MultiPolygons polygons);

	/**
	 * @param comment
	 */
	public void visit(Comment comment);

    /**
     * @param model
     */
    public void visit(Model model);

    /**
     * @param circle
     */
    public void visit(Circle circle);
    
    /**
     * @param element
     */
    public void visit(Element element);
    
    /**
     * @param header
     */
    public void visit(AtomHeader header);
}

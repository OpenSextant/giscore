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
package org.mitre.giscore;

import org.mitre.giscore.events.Comment;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.PhotoOverlay;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.ScreenLocation;
import org.mitre.giscore.events.ScreenOverlay;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
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
}

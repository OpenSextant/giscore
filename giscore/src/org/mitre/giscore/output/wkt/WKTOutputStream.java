/****************************************************************************************
 *  WKTOutputStream.java
 *
 *  Created: Jan 11, 2012
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2012
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
package org.mitre.giscore.output.wkt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Outputs an OGC Well-known text (WKT) format of a series of geometry objects. This only deals
 * with WGS 84 right now.
 * 
 * @author DRAND
 * 
 */
public class WKTOutputStream extends StreamVisitorBase implements
		IGISOutputStream {
	private static final Logger log = LoggerFactory.getLogger(WKTOutputStream.class);
	private Writer writer;

	/**
	 * Compatible ctor
	 * 
	 * @param outputStream
	 * @param arguments
	 * @throws IOException if an error occurs
	 * @throws IllegalArgumentException if stream is null
	 */
	public WKTOutputStream(OutputStream outputStream, Object[] arguments)
			throws IOException {
		this(outputStream);
	}

	/**
	 * Ctor
	 * 
	 * @param stream
	 *            output stream to send the WKT output to
	 * @throws IOException if an error occurs
	 * @throws IllegalArgumentException if stream is null
	 */
	public WKTOutputStream(OutputStream stream) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		writer = new OutputStreamWriter(stream, "UTF-8");
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Point)
	 */
	@Override
	public void visit(Point point) {
		try {
			writer.append("POINT (");
			Geodetic2DPoint center = point.asGeodetic2DPoint();
			handlePoint(center);
			writer.append(')');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Output a point
	 *  
	 * @param pnt
	 * @throws IOException
	 */
	public void handlePoint(Geodetic2DPoint pnt) throws IOException {
		writer.append(Double.toString(pnt.getLongitude().inDegrees()));
		writer.append(" ");
		writer.append(Double.toString(pnt.getLatitude().inDegrees()));
	}

	/**
	 * Handle polygon
	 * 
	 * @param polygon
	 * @throws IOException if an error occurs
	 */
	private void handlePoly(Polygon polygon) throws IOException {
		writer.append("(");
		handlePointList(polygon.getOuterRing().getPoints());
		for (LinearRing ring : polygon.getLinearRings()) {
			writer.append(",\n  ");
			handlePointList(ring.getPoints());
		}
		writer.append(')');
	}

	/**
	 * Output a list of points
	 * 
	 * @param points
	 * @throws IOException if an error occurs
	 */
	public void handlePointList(List<Point> points) throws IOException {
		int count = points.size();
		writer.append("(");
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				writer.append(", ");
			}
			Geodetic2DPoint pnt = points.get(i).getCenter();
			handlePoint(pnt);
		}
		writer.append(')');
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Line)
	 */
	@Override
	public void visit(Line line) {
		try {
			writer.append("LINESTRING ");
			handlePointList(line.getPoints());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .LinearRing)
	 */
	@Override
	public void visit(LinearRing ring) {
		try {
			writer.append("POLYGON (");
			handlePointList(ring.getPoints());
			writer.append(')');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Polygon)
	 */
	@Override
	public void visit(Polygon polygon) {
		try {
			writer.append("POLYGON ");
			handlePoly(polygon);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiPoint)
	 */
	@Override
	public void visit(MultiPoint multiPoint) {
		try {
			writer.append("MULTIPOINT ");
			List<Point> points = multiPoint.getPoints();
			handlePointList(points);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiLine)
	 */
	@Override
	public void visit(MultiLine multiLine) {
		try {
			writer.append("MULTILINESTRING (");
			Collection<Line> lines = multiLine.getLines();
			int count = lines.size();
			Iterator<Line> iter = lines.iterator();
			for (int i = 0; i < count; i++) {
				if (i > 0) {
					writer.append(",\n  ");
				}
				Line line = iter.next();
				handlePointList(line.getPoints());
			}
			writer.append(')');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiLinearRings)
	 */
	@Override
	public void visit(MultiLinearRings rings) {
		try {
			writer.append("MULTIPOLYGON (");
			int count = rings.getLinearRings().size();
			Iterator<LinearRing> iter = rings.getLinearRings().iterator();
			for (int i = 0; i < count; i++) {
				if (i > 0) {
					writer.append(",\n  ");
				}
				LinearRing ring = iter.next();
				writer.append("(");
				handlePointList(ring.getPoints());
				writer.append(")");
			}
			writer.append(')');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiPolygons)
	 */
	@Override
	public void visit(MultiPolygons multi) {
		try {
			writer.append("MULTIPOLYGON (");
			int count = multi.getPolygons().size();
			Iterator<Polygon> iter = multi.getPolygons().iterator();
			for (int i = 0; i < count; i++) {
				if (i > 0) {
					writer.append(",\n  ");
				}
				Polygon poly = iter.next();
				handlePoly(poly);
			}
			writer.append(')');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .GeometryBag)
	 */
	@Override
	public void visit(GeometryBag geobag) {
		if (geobag != null && !geobag.isEmpty()) {
			try {
				writer.append("GEOMETRYCOLLECTION (");
				int count = geobag.size();
				for (int i = 0; i < count; i++) {
					if (i > 0) {
						writer.append(",\n  ");
					}
					geobag.getPart(i).accept(this);
				}

				writer.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @throws IllegalArgumentException if object is null
	 * @throws RuntimeException if an I/O error occurs
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events.IGISObject)
	 */
	public void write(IGISObject object) {
		if (object == null) {
			throw new IllegalArgumentException("object should never be null");
		}
		object.accept(this);
		try {
			writer.append("\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

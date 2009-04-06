/****************************************************************************************
 *  KmlOutputStream.java
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
package org.mitre.giscore.output.kml;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.events.BaseStart;
import org.mitre.giscore.events.Comment;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.Overlay;
import org.mitre.giscore.events.PhotoOverlay;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.ScreenLocation;
import org.mitre.giscore.events.ScreenOverlay;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.events.TaggedMap;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;

/**
 * The kml output stream creates a result KML file using the given output
 * stream. It uses STaX methods for writing the XML elements to avoid building
 * an in-memory DOM, which reduces the memory overhead of creating the document.
 * <p>
 * For KML, each incoming element generally adds another full element to the
 * output document. There are a couple of distinct exceptions. These are the
 * Style selectors. The style selectors instead appear before the matched
 * feature, and the KML output stream buffers these until the next feature is
 * seen. At that point the styles are output after the element's attributes and
 * before any content.
 * <p>
 * The geometry visitors are invoked by the feature vistor via the Geometry 
 * accept method.
 * 
 * @author DRAND
 * 
 */
public class KmlOutputStream extends XmlOutputStreamBase implements IKml {

    private final List<IGISObject> waitingElements = new ArrayList<IGISObject>();
	private static final DecimalFormat ms_float_fmt =
		new DecimalFormat("##0.####");
	private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss'Z'";	
	private DateFormat dateFormatter;
    
    /**
	 * Ctor
	 * 
	 * @param stream
	 * @throws XMLStreamException
	 */
	public KmlOutputStream(OutputStream stream) throws XMLStreamException {
		super(stream);
		
		writer.writeStartDocument();
        writer.writeCharacters("\n");
        writer.writeStartElement(KML);
		writer.writeDefaultNamespace(KML_NS);
        writer.writeCharacters("\n");
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.output.XmlOutputStreamBase#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		} finally {
            super.close();
        }
	}

	/**
	 * Flush and close XMLStreamWriter but not the outputStream
	 *
	 * @throws IOException
	 * @see org.mitre.giscore.output.IGISOutputStream#close()
	 */
	public void closeWriter() throws IOException {
		try {
			try {
				writer.writeEndElement();
				writer.writeCharacters("\n");
				writer.writeEndDocument();
			} finally {
				writer.flush();
				writer.close();
				// don't call super.close() which closes the outputStream
			}
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerEnd)
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		try {
			String tag = containerStart.getType();
			writer.writeStartElement(tag);
			handleAttributes(containerStart);
			handleWaitingElements();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

    private DateFormat getDateFormatter() {
        if (dateFormatter == null) {
			DateFormat thisDateFormatter = new SimpleDateFormat(ISO_DATE_FMT);
			thisDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormatter = thisDateFormatter;
		}
        return dateFormatter;
    }

    /**
	 * Common code for outputting feature data that is held for both containers
	 * and other features like Placemarks and Overlays.
	 * 
	 * @param feature
	 */
	private void handleAttributes(BaseStart feature) {
		try {
			if (feature.getName() != null) {
				handleSimpleElement(NAME, feature.getName());
			}
			if (feature.getDescription() != null) {
				handleSimpleElement(DESCRIPTION, feature.getDescription());
			}

            Date startTime = feature.getStartTime();
            Date endTime = feature.getEndTime();
            if (startTime != null) {
				if (endTime == null) {
                    // start time with no end time
                    writer.writeStartElement(TIME_SPAN);
                    handleSimpleElement(BEGIN, getDateFormatter().format(startTime.getTime()));
                } else if (endTime.equals(startTime)) {
                    writer.writeStartElement(TIME_STAMP);
                    handleSimpleElement(WHEN, getDateFormatter().format(startTime.getTime()));
                } else {
                    writer.writeStartElement(TIME_SPAN);
                    handleSimpleElement(BEGIN, getDateFormatter().format(startTime.getTime()));
                    handleSimpleElement(END, getDateFormatter().format(endTime.getTime()));
                }
                writer.writeEndElement();
            } else if (endTime != null) {
                    // end time with no start time
                    writer.writeStartElement(TIME_SPAN);
					handleSimpleElement(END, getDateFormatter().format(endTime.getTime()));
					writer.writeEndElement();
            }

            if (feature.getStyleUrl() != null) {
				handleSimpleElement(STYLE_URL, feature.getStyleUrl());
			}
			if (feature.hasExtendedData()) {
				URI schema = feature.getSchema();
				writer.writeStartElement(EXTENDED_DATA);
				if (schema == null) {
					for (SimpleField field : feature.getFields()) {
						Object value = feature.getData(field);
						writer.writeStartElement(DATA);
						writer.writeAttribute(NAME, field.getName());
						handleSimpleElement(VALUE, formatValue(field.getType(),
								value));
						writer.writeEndElement();
					}
				} else {
					writer.writeStartElement(SCHEMA_DATA);
					writer.writeAttribute(SCHEMA_URL, schema.toString());
					for (SimpleField field : feature.getFields()) {
						Object value = feature.getData(field);
						writer.writeStartElement(SIMPLE_DATA);
						writer.writeAttribute(NAME, field.getName());
						handleCharacters(formatValue(field.getType(),
								value));
						writer.writeEndElement();
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Format a value according to the type, defaults to using toString.
	 * 
	 * @param type the type, assumed not <code>null</code>
	 * @param data the data, may be a number of types, but must be coercible to
	 * the given type 
	 * @return a formatted value
	 */
	private String formatValue(Type type, Object data) {
		if (data == null || ObjectUtils.NULL.equals(data)) {
			return "";
		} else if (Type.DATE.equals(type)) {
			Object val = data;
			if (val instanceof String) {
				try {
					// Try converting to ISO?
                    val = KmlInputStream.parseDate((String) data);
				} catch(Exception e) {
					// Fall through
				}
			}
			if (val instanceof Date) {
                return getDateFormatter().format(((Date)val).getTime());
			} else {
				return val.toString();
			}
		} else if (Type.DOUBLE.equals(type) || Type.FLOAT.equals(type)) {
			if (data instanceof String) {
				data = new Double((String) data);
			} 
			
			if (data instanceof Number) {
				return ms_float_fmt.format(((Number) data).doubleValue());
			} else {
				throw new IllegalArgumentException("Data that cannot be coerced to float: " + data);
			}
		} else if (Type.INT.equals(type) || Type.SHORT.equals(type) 
				|| Type.UINT.equals(type) || Type.USHORT.equals(type)) {
			if (data instanceof String) {
				data = new Long((String) data);
			}
			
			if (data instanceof Number) {
				return String.valueOf(data); // ms_int_fmt.format(((Number) data).longValue());
			} else {
				throw new IllegalArgumentException("Data that cannot be coerced to int: " + data);
			}
		} else {
			return data.toString();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .DocumentStart)
	 */
	@Override
	public void visit(DocumentStart documentStart) {
		// Ignore
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
		try {
			String tag = feature.getType();
			writer.writeStartElement(tag);
			handleAttributes(feature);
			handleWaitingElements();
			if (feature instanceof Overlay) {
				handleOverlay((Overlay) feature);
			} else if (feature.getGeometry() != null) {
				feature.getGeometry().accept(this);
			} else if (feature instanceof NetworkLink) {
				handleNetworkLink((NetworkLink) feature);
			}
			writer.writeEndElement();
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle elements specific to a network link feature.
	 * 
	 * @param link
     * @throws javax.xml.stream.XMLStreamException
	 */
	private void handleNetworkLink(NetworkLink link) throws XMLStreamException {
        handleLinkElement(LINK, link.getLink());
	}

	/**
	 * Handle elements specific to an overlay feature
	 * 
	 * @param overlay
	 * @throws XMLStreamException
	 */
	private void handleOverlay(Overlay overlay) throws XMLStreamException {
		handleColor(COLOR, overlay.getColor());
		handleSimpleElement(DRAW_ORDER, Integer
				.toString(overlay.getDrawOrder()));
        handleLinkElement(ICON, overlay.getIcon());

		if (overlay instanceof GroundOverlay) {
			GroundOverlay go = (GroundOverlay) overlay;
			handleNonNullSimpleElement(ALTITUDE, go.getAltitude());
			handleNonNullSimpleElement(ALTITUDE_MODE, go.getAltitudeMode());
			writer.writeStartElement(LAT_LON_BOX);
			handleNonNullSimpleElement(NORTH, go.getNorth());
			handleNonNullSimpleElement(SOUTH, go.getSouth());
			handleNonNullSimpleElement(EAST, go.getEast());
			handleNonNullSimpleElement(WEST, go.getWest());
			handleNonNullSimpleElement(ROTATION, go.getRotation());
            writer.writeEndElement();
		} else if (overlay instanceof PhotoOverlay) {
			// PhotoOverlay po = (PhotoOverlay) overlay;
			// TODO: Fill in sometime
		} else if (overlay instanceof ScreenOverlay) {
			ScreenOverlay so = (ScreenOverlay) overlay;
			handleXY(OVERLAY_XY, so.getOverlay());
			handleXY(SCREEN_XY, so.getScreen());
			handleXY(ROTATION_XY, so.getRotation());
			handleXY(SIZE, so.getSize());
			handleNonNullSimpleElement(ROTATION, so.getRotationAngle());
		}
	}

	private void handleNonNullSimpleElement(String tag, Object content) throws XMLStreamException {
		if (content != null) handleSimpleElement(tag, content);
	}

	// elements associated with Kml22 LinkType in sequence order
    // for Icon, Link, and Url elements
    private static final String[] LINK_TYPE_TAGS = {
          "href",
          "refreshMode",
          "refreshInterval",
          "viewRefreshMode",
          "viewRefreshTime",
          "viewBoundScale",
          "viewFormat",
          "httpQuery"
    };

    private void handleLinkElement(String elementName, TaggedMap map) throws XMLStreamException {
        if (map == null || map.isEmpty())
            return;
        writer.writeStartElement(elementName);
        for (String tag : LINK_TYPE_TAGS) {
            String val = map.get(tag);
            if (val != null && val.length() != 0) {
                if (tag.equals(HREF) && val.startsWith("kmz") && val.indexOf("file=") > 0) {
                    // replace internal URI (which is used to associate link with parent KMZ file)
                    // with the relative target URL from original KMZ file.
                    try {
                        UrlRef urlRef = new UrlRef(new URI(val));
                        val = urlRef.getKmzRelPath();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                handleSimpleElement(tag, val);
            }
        }
        writer.writeEndElement();
    }

    /**
	 * Handle the screen location information
	 * 
	 * @param tag
	 * @param loc
	 * @throws XMLStreamException
	 */
	private void handleXY(String tag, ScreenLocation loc)
			throws XMLStreamException {
		if (loc != null) {
			writer.writeStartElement(tag);
			writer.writeAttribute("x", Double.toString(loc.x));
			writer.writeAttribute("y", Double.toString(loc.y));
			writer.writeAttribute("xunits", loc.xunit.kmlValue);
			writer.writeAttribute("yunits", loc.yunit.kmlValue);
			writer.writeEndElement();
		}
	}

	/*
	 * Output a tagged element.
	 * 
	 * @param data
	 */
    /*
    private void handleTagElement(TaggedMap data) throws XMLStreamException {
		if (data == null)
			return;
		writer.writeStartElement(data.getTag());
        // TODO: this writes elements in tag order which DOES NOT match order in KML XML schema
        // KML is well-formed and should correctly display in Google Earth but is not valid KML wrt spec.
        for (Map.Entry<String,String> entry : data.entrySet()) {
			handleSimpleElement(entry.getKey(), entry.getValue());
		}
		writer.writeEndElement();
	}
    */
    
    /**
	 * Handle elements that have been deferred. Style information is stored as
	 * found and output on the next feature or container.
	 * 
	 * @throws XMLStreamException
	 */
	private void handleWaitingElements() throws XMLStreamException {
		for (int i = waitingElements.size() - 1; i >= 0; i--) {
			IGISObject element = waitingElements.get(i);
			if (element instanceof Style) {
				handle((Style) element);
			} else if (element instanceof StyleMap) {
				handle((StyleMap) element);
			} else {
				throw new RuntimeException("Unknown kind of deferred element: "
						+ element.getClass());
			}
		}
		waitingElements.clear();
	}

	/**
	 * Output a multigeometry, represented by a geometry bag
	 * @param bag the geometry bag, never <code>null</code>
	 */
	@Override
	public void visit(GeometryBag bag) {
		if (bag == null) {
			throw new IllegalArgumentException("bag should never be null");
		}
		try {
			writer.writeStartElement(MULTI_GEOMETRY);
			super.visit(bag);
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry.MultiPoint)
	 */
	@Override
	public void visit(MultiPoint multiPoint) {
		if (multiPoint == null) {
			throw new IllegalArgumentException("bag should never be null");
		}
		try {
			writer.writeStartElement(MULTI_GEOMETRY);
			for(Point point : multiPoint.getPoints()) {
				point.accept(this);
			}
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Handle the output of a polygon
	 * 
	 * @param poly
	 *            the polygon, never <code>null</code>
	 */
	@Override
	public void visit(Polygon poly) {
		if (poly == null) {
			throw new IllegalArgumentException("poly should never be null");
		}
		try {
			writer.writeStartElement(POLYGON);
			if (poly.getOuterRing() != null) {
				writer.writeStartElement(OUTER_BOUNDARY_IS);
				writer.writeStartElement(LINEAR_RING);
				handleSimpleElement(COORDINATES, handleCoordinates(poly
						.getOuterRing().iterator()));
				writer.writeEndElement();
				writer.writeEndElement();
			}
			if (poly.getLinearRings() != null) {
				for (LinearRing lr : poly.getLinearRings()) {
					writer.writeStartElement(INNER_BOUNDARY_IS);
					writer.writeStartElement(LINEAR_RING);
					handleSimpleElement(COORDINATES, handleCoordinates(lr
							.getPoints()));
					writer.writeEndElement();
					writer.writeEndElement();
				}
			}
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle the output of a ring
	 * 
	 * @param r
	 *            the ring, never <code>null</code>
	 */
	@Override
	public void visit(LinearRing r) {
		if (r == null) {
			throw new IllegalArgumentException("r should never be null");
		}
		try {
			writer.writeStartElement(LINEAR_RING);
			handleSimpleElement(COORDINATES, handleCoordinates(r.iterator()));
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle the output of a line
	 * 
	 * @param l
	 *            the line, never <code>null</code>
	 */
	@Override
	public void visit(Line l) {
		if (l == null) {
			throw new IllegalArgumentException("l should never be null");
		}
		try {
			writer.writeStartElement(LINE_STRING);
			handleSimpleElement(COORDINATES, handleCoordinates(l.getPoints()));
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle the output of a point
	 * 
	 * @param p
	 *            the point, never <code>null</code>
	 */
	@Override
	public void visit(Point p) {
		if (p == null) {
			throw new IllegalArgumentException("p should never be null");
		}
		try {
			writer.writeStartElement(POINT);
			handleSimpleElement(COORDINATES, handleCoordinates(Collections
					.singletonList(p)));
			writer.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * output the coordinates. The coordinates are output as lon,lat[,altitude]
	 * and are separated by spaces
	 * 
	 * @param coordinates
	 *            an iterator over the points, never <code>null</code>
	 * @throws XMLStreamException
	 * @return the coordinates as a string
	 */
	private String handleCoordinates(Iterator<Point> coordinates)
			throws XMLStreamException {
		StringBuilder b = new StringBuilder();
		while (coordinates.hasNext()) {
			Point point = coordinates.next();
			handleSingleCoordinate(b, point);
		}
		return b.toString();
	}

	/**
	 * output the coordinates. The coordinates are output as lon,lat[,altitude]
	 * and are separated by spaces
	 * 
	 * @param coordinateList
	 *            the list of coordinates, never <code>null</code>
	 * @throws XMLStreamException
	 */
	private String handleCoordinates(Collection<Point> coordinateList)
			throws XMLStreamException {
		StringBuilder b = new StringBuilder();
		for (Point point : coordinateList) {
			handleSingleCoordinate(b, point);
		}
		return b.toString();
	}

	/**
	 * Output a single coordinate
	 * 
	 * @param b
	 * @param point
	 */
	private void handleSingleCoordinate(StringBuilder b, Point point) {
		if (b.length() > 0) {
			b.append(' ');
		}
		Geodetic2DPoint p2d = point.getCenter();
		b.append(p2d.getLongitude().inDegrees());
		b.append(',');
		b.append(p2d.getLatitude().inDegrees());
		if (point.getCenter() instanceof Geodetic3DPoint) {
			Geodetic3DPoint p3d = (Geodetic3DPoint) point.getCenter();
			b.append(',');
			b.append(p3d.getElevation());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Comment)
	 */
	@Override
	public void visit(Comment comment) {
		String text = comment.getText();
		if (StringUtils.isNotEmpty(text))
			try {
				// string "--" (double-hyphen) MUST NOT occur within comments.
				text = text.replace("--", "&#x2D;&#x2D;");
				StringBuilder buf = new StringBuilder();
				if (!Character.isWhitespace(text.charAt(0)))
					buf.append(' ');
				buf.append(text);
				if (text.length() == 1 || !Character.isWhitespace(text.charAt(text.length() - 1)))
					buf.append(' ');
				writer.writeComment(buf.toString());
				writer.writeCharacters("\n");
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
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
		try {
			writer.writeStartElement(SCHEMA);
			writer.writeAttribute(NAME, schema.getName());
			writer.writeAttribute(ID, schema.getId().toString());
            for (String name : schema.getKeys()) {
				SimpleField field = schema.get(name);
				if (field.getType().isGeometry()) {
					continue; // Skip geometry elements, no equivalent in Kml
				}
				writer.writeStartElement(SIMPLE_FIELD);
				if (field.getType().isKmlCompatible())
					writer.writeAttribute(TYPE, field.getType().toString()
							.toLowerCase());
				else
					writer.writeAttribute(TYPE, "string");
				writer.writeAttribute(NAME, field.getName());
				if (StringUtils.isNotEmpty(field.getDisplayName())) {
					handleSimpleElement(DISPLAY_NAME, field.getDisplayName());
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Style)
	 */
	@Override
	public void visit(Style style) {
		waitingElements.add(style);
	}

	/**
	 * Actually output the style
	 * 
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handle(Style style) throws XMLStreamException {
		writer.writeStartElement(STYLE);
		if (style.getId() != null) {
			writer.writeAttribute(ID, style.getId());
		}
		if (style.hasIconStyle()) {
			handleIconStyleElement(style);
		}
        if (style.hasLabelStyle()) {
			handleLabelStyleElement(style);
		}
        if (style.hasLineStyle()) {
			handleLineStyleElement(style);
		}
        if (style.hasPolyStyle()) {
			handlePolyStyleElement(style);
		}
        if (style.hasBalloonStyle()) {
			handleBalloonStyleElement(style);
		}
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handlePolyStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(POLY_STYLE);
		handleColor(COLOR, style.getPolyColor());
		handleSimpleElement(FILL, style.isPolyfill() ? "1" : "0");
		handleSimpleElement(OUTLINE, style.isPolyoutline() ? "1" : "0");
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleLabelStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(LABEL_STYLE);
		handleColor(COLOR, style.getLabelColor());
		handleSimpleElement(SCALE, style.getLabelScale());
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleBalloonStyleElement(Style style)
			throws XMLStreamException {
		writer.writeStartElement(BALLOON_STYLE);
		handleColor(BG_COLOR, style.getBalloonBgColor());
        handleColor(TEXT_COLOR, style.getBalloonTextColor());
        handleSimpleElement(TEXT, style.getBalloonText());
        handleSimpleElement(DISPLAY_MODE, style.getBalloonDisplayMode());
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @return
	 * @throws XMLStreamException
	 */
	protected void handleLineStyleElement(Style style)
			throws XMLStreamException {
		writer.writeStartElement(LINE_STYLE);
        handleColor(COLOR, style.getLineColor());
        handleSimpleElement(WIDTH, Double.toString(style.getLineWidth()));
		writer.writeEndElement();
	}

	/**
	 * @return
	 * @throws XMLStreamException
	 */
	protected void handleIconStyleElement(Style style)
			throws XMLStreamException {
		writer.writeStartElement(ICON_STYLE);
		handleColor(COLOR, style.getIconColor());
		handleSimpleElement(SCALE, Double.toString(style.getIconScale()));
		if (style.getIconUrl() != null) {
			writer.writeStartElement(ICON);
			if (style.getIconUrl() != null)
				handleSimpleElement(HREF, style.getIconUrl());
			writer.writeEndElement();
		}
        /*
        // hotSpot optional. skip it
        writer.writeStartElement(HOT_SPOT);
		writer.writeAttribute("x", "0");
		writer.writeAttribute("y", "0");
		//writer.writeAttribute("xunits", "fraction"); // default
		//writer.writeAttribute("yunits", "fraction"); // default
		writer.writeEndElement();
		*/

		writer.writeEndElement();
	}

	/**
	 * Get the kml compliant color translation
	 * 
	 * @return kml color element
	 * @throws XMLStreamException
	 */
	protected void handleColor(String tag, Color color)
			throws XMLStreamException {
		if (color != null) {
			StringBuilder sb = new StringBuilder(8);
			Formatter formatter = new Formatter(sb, Locale.US);
			formatter.format("%02x%02x%02x%02x", color.getAlpha(), color
					.getBlue(), color.getGreen(), color.getRed());
			handleSimpleElement(tag, sb.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .StyleMap)
	 */
	@Override
	public void visit(StyleMap styleMap) {
		waitingElements.add(styleMap);
	}

	/**
	 * Actually handle style map
	 * 
	 * @param styleMap
	 * @throws XMLStreamException
	 */
	private void handle(StyleMap styleMap) throws XMLStreamException {
		writer.writeStartElement(STYLE_MAP);
		if (styleMap.getId() != null) {
			writer.writeAttribute(ID, styleMap.getId());
		}
		Iterator<String> kiter = styleMap.keys();
		while (kiter.hasNext()) {
			String key = kiter.next();
			String value = styleMap.get(key);
			writer.writeStartElement(PAIR);
			handleSimpleElement(KEY, key);
			handleSimpleElement(STYLE_URL, value);
			writer.writeEndElement();
		}
		writer.writeEndElement();
	}

    /**
     *
     * @return true if there are any elements on the waitingElements list
     */
    public boolean isWaiting() {
        return waitingElements.size() != 0;
    }
}

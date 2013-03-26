/****************************************************************************************
 *  GeoAtomOutputStream.java
 *
 *  Created: Jul 16, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
package org.mitre.giscore.output.atom;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.AtomAuthor;
import org.mitre.giscore.events.AtomHeader;
import org.mitre.giscore.events.AtomLink;
import org.mitre.giscore.events.Element;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.Circle;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.giscore.utils.SafeDateFormat;
import org.mitre.itf.geodesy.Geodetic2DPoint;

/**
 * Output ATOM 1.0 format with OGC and other extensions to contain information
 * appropriate for GIS and extended data.
 *
 * @author DRAND
 */
public class GeoAtomOutputStream extends XmlOutputStreamBase implements
		IAtomConstants {
	private static final SafeDateFormat fmt = new SafeDateFormat(
			IKml.ISO_DATE_FMT);
	private static final Namespace ATOM_NS = Namespace.getNamespace(ATOM_URI_NS);
	private final DecimalFormat dfmt;

	private final static Set<SimpleField> ms_builtinFields = new HashSet<SimpleField>(4);
	static {
		ms_builtinFields.add(LINK_ATTR);
		ms_builtinFields.add(TITLE_ATTR);
		ms_builtinFields.add(UPDATED_ATTR);
		ms_builtinFields.add(AUTHOR_ATTR);
	}
	private static final Namespace gns = Namespace.getNamespace("geo", GIS_NS);
	private boolean headerwritten = false;

	public GeoAtomOutputStream(OutputStream outputStream, Object[] arguments)
	throws XMLStreamException {
	    /* GeoAtomOutputStream(OutputStream stream, Date updateDTM,
			String title, String link, List<String> authors) */
		super(outputStream);
		writer.writeStartDocument();
		dfmt = new DecimalFormat("##.######");
	}



	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.XmlOutputStreamBase#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			writer.writeEndElement();
			writer.writeEndDocument();
			super.close();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}



	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.AtomHeader)
	 */
	@Override
	public void visit(AtomHeader header) {
		try {
			writer.writeStartElement("feed");
			writer.writeDefaultNamespace(ATOM_URI_NS);
			writer.writeNamespace("ext", EXT_DATA_NS);
			writer.writeNamespace("geo", GIS_NS);
			namespaces.put("ext", EXT_DATA_NS);
			namespaces.put("geo", GIS_NS);
			if (header.getNamespaces() != null) {
				for(Namespace ns : header.getNamespaces()) {
					writer.writeNamespace(ns.getPrefix(), ns.getURI());
					namespaces.put(ns.getPrefix(), ns.getURI());
				}
			}
			handleSimpleElement("generator", header.getGenerator());
			handleSimpleElement("id", header.getId());
			handleSimpleElement("title", header.getTitle());
			handleSimpleElement("updated", fmt.format(header.getUpdated()));
			handleLink(header.getSelflink());
			if (header.getRelatedlinks() != null) {
				for(AtomLink rel : header.getRelatedlinks()) {
					handleLink(rel);
				}
			}
			if (header.getAuthors() != null && header.getAuthors().size() > 0) {
				for (AtomAuthor author : header.getAuthors()) {
					handleAuthor(author);
				}
			}
			if (header.getElements() != null && header.getElements().size() > 0) {
				for(Element el : header.getElements()) {
					visit(el);
				}
			}
			headerwritten = true;
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle an author
	 * @param author
	 * @throws XMLStreamException
	 */
	private void handleAuthor(AtomAuthor author) throws XMLStreamException {
		writer.writeStartElement("author");
		handleSimpleElement("name", author.getName());
		if (author.getUri() != null) {
			handleSimpleElement("uri", author.getUri());
		}
		handleSimpleElement("email", author.getEmail());
		writer.writeEndElement();
	}


	/**
	 * Handle a link
	 * @param link link data
	 * @throws XMLStreamException
	 */
	private void handleLink(AtomLink link) throws XMLStreamException {
		writer.writeStartElement("link");
		writer.writeAttribute("href", link.getHref().toExternalForm());
		if (link.getHreflang() != null) {
			writer.writeAttribute("hreflang", link.getHreflang());
		}
		if (link.getType() != null) {
			writer.writeAttribute("type", link.getType().toString());
		}
		if (link.getRel() != null) {
			writer.writeAttribute("rel", link.getRel());
		}
		writer.writeEndElement();
	}



	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Element)
	 */
	@Override
	public void visit(Element element) {
		try {
			handleXmlElement(element, ATOM_NS);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Row)
	 */
	@Override
	public void visit(Row row) {
		Object u = row.getData(UPDATED_ATTR);
		Date updated = null;
		if (u != null && u instanceof Date) {
			updated = (Date) u;
		}
		Object title = row.getData(TITLE_ATTR);
		outputRow(row, updated, title != null ? title.toString() : "", null, null, Collections.<Element>emptyList());
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
		Object u = feature.getData(UPDATED_ATTR);
		Date updated = feature.getStartTime();
		if (updated == null && u != null && u instanceof Date) {
			updated = (Date) u;
		}
		outputRow(feature, updated, feature.getName(), feature.getDescription(), feature.getGeometry(), feature.getElements());
	}

	/**
	 * Output a row plus other info, a refactoring of outputting the information
	 * contained in a feature to allow the same code to output either a feature
	 * or a row.
	 * @param row
	 * @param updated
	 * @param description
	 * @param geo
	 */
	private void outputRow(Row row, Date updated, String title,
			String description, Geometry geo, List<Element> extraElements) {
		if (! headerwritten) {
			throw new IllegalStateException("Must output atom header before any feature or row");
		}
		String id = row.getId();
		Object a = row.getData(AUTHOR_ATTR);
		List<String> alist = new ArrayList<String>();

		if (a != null) {
			String authors = a.toString();
			String parts[] = authors.split(",");
			for(String part : parts) {
				alist.add(part);
			}
		}
		Object link = row.getData(LINK_ATTR);
		Map<SimpleField, String> data = new HashMap<SimpleField, String>();
		for (SimpleField field : row.getFields()) {
			if (ms_builtinFields.contains(field)) continue;
			String name = field.getName();
			Object val = row.getData(field);
			if (val != null) {
				data.put(field, writeValue(field.getType(), val));
			}
		}
		outputEntry(id, title,
				description, updated,
				link != null ? link.toString() : "", data,
				geo, alist, extraElements);
	}

	/**
	 * Process the string value according to the type. By default just store the
	 * string.
	 * @param type
	 * @param val
	 * @return
	 */
	private String writeValue(Type type, Object val)  {
		switch(type) {
		case DOUBLE:
		case FLOAT:
			if (val instanceof Number) {
				return dfmt.format((Number) val);
			}
			break;
        case LONG:
            if (val instanceof Number) {
                return Long.toString(((Number) val).longValue());
            }
            break;
		case INT:
		case UINT:
			if (val instanceof Number) {
                return Integer.toString(((Number) val).intValue());
			}
			break;
		case SHORT:
		case USHORT:
            if (val instanceof Number) {
                return Short.toString(((Number) val).shortValue());
            }
			break;
		case BOOL:
			if (val instanceof Boolean) {
				return Boolean.toString((Boolean) val);
			}
			break;
		case DATE:
			if (val instanceof Date) {
				return fmt.format((Date) val);
			}
		}
		return val.toString();
	}

	/**
	 * Output the data in an entry
	 *
	 * @param id
	 *            the atom id for the entry
	 * @param title
	 * @param description
	 * @param updated
	 * @param link
	 * @param data
	 *            extended data for the entry, extracted, filtered and formatted
	 *            from the original's extended data. The atom id does not have
	 *            to be removed as it will be skipped.
	 * @param geo
	 * @param alist
	 */
	public void outputEntry(String id, String title, String description,
			Date updated, String link, Map<SimpleField, String> data, Geometry geo, List<String> alist) {
		outputEntry(id, title, description, updated, link, data, geo, alist, Collections.<Element>emptyList());
	}

	/**
	 * Output the data in an entry
	 *
	 * @param id
	 *            the atom id for the entry
	 * @param title
	 * @param description
	 * @param updated
	 * @param link
	 * @param data
	 *            extended data for the entry, extracted, filtered and formatted
	 *            from the original's extended data. The atom id does not have
	 *            to be removed as it will be skipped.
	 * @param geo
	 * @param alist
	 * @param children
	 */
	public void outputEntry(String id, String title, String description,
			Date updated, String link, Map<SimpleField, String> data, Geometry geo, List<String> alist, List<Element> children) {
		try {
			writer.writeStartElement("entry");
			for(String author : alist) {
				writer.writeStartElement("author");
				handleSimpleElement("name", author);
				writer.writeEndElement();
			}
			handleSimpleElement("id", id);
			handleSimpleElement("title", title);
			handleSimpleElement("content", description);
			handleSimpleElement("updated", fmt.format(updated));
			if (StringUtils.isNotBlank(link)) {
				writer.writeStartElement("link");
				writer.writeAttribute("href", link);
				writer.writeEndElement();
			}
			for(Map.Entry<SimpleField, String> entry : data.entrySet()) {
				SimpleField key = entry.getKey();
				writer.writeStartElement("ext", "data", EXT_DATA_NS);
				writer.writeAttribute("name", key.getName());
				writer.writeAttribute("type", key.getType().name());
				writer.writeCharacters(entry.getValue());
				writer.writeEndElement();
			}
			if (geo != null) {
				geo.accept(this);
			}
			if (children != null) {
				for(Element el : children) {
					visit(el);
				}
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
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Point)
	 */
	@Override
	public void visit(Point point) {
		Geodetic2DPoint center = point.getCenter();
		try {
			handleSimpleElement(
					gns,
					"point",
          dfmt.format(center.getLatitudeAsDegrees()) + " "
              + dfmt.format(center.getLongitudeAsDegrees()));
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
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
			handleSimpleElement(gns, "line", getCoords(line.getPoints()));
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	private String getCoords(List<Point> points) {
		StringBuilder coords = new StringBuilder();
		for (Point p : points) {
			Geodetic2DPoint center = p.getCenter();
			coords.append(" ");
      coords.append(dfmt.format(center.getLatitudeAsDegrees()));
			coords.append(" ");
      coords.append(dfmt.format(center.getLongitudeAsDegrees()));
		}
		return coords.toString().trim();
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
			handleSimpleElement(gns, "polygon", getCoords(ring.getPoints()));
		} catch (XMLStreamException e) {
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
			handleSimpleElement(gns, "polygon", getCoords(polygon.getPoints()));
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void visit(Circle circle) {
		final Geodetic2DPoint center = circle.getCenter();
		try {
			handleSimpleElement(gns, "circle", dfmt.format(center.getLatitudeAsDegrees()) + " "
					+ dfmt.format(center.getLongitudeAsDegrees()) + " "
					+ dfmt.format(circle.getRadius()));
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
}

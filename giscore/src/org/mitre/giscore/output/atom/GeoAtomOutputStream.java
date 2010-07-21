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
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.stream.XMLStreamException;

import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.AtomAuthor;
import org.mitre.giscore.events.AtomHeader;
import org.mitre.giscore.events.AtomLink;
import org.mitre.giscore.events.Element;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.SimpleField;
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
	private DecimalFormat dfmt;

	private final static Set<SimpleField> ms_builtinFields = new HashSet<SimpleField>();
	static {
		ms_builtinFields.add(LINK_ATTR);
		ms_builtinFields.add(TITLE_ATTR);
		ms_builtinFields.add(UPDATED_ATTR);
		ms_builtinFields.add(AUTHOR_ATTR);
	}
	private static Namespace gns = Namespace.getNamespace("geo", GIS_NS);
	private boolean headerwritten = false;

	public GeoAtomOutputStream(OutputStream outputStream, Object[] arguments)  
	throws XMLStreamException {
	    /* GeoAtomOutputStream(OutputStream stream, Date updateDTM,
			String title, String link, List<String> authors) */
		super(outputStream);	
		writer.writeStartDocument();
		dfmt = new DecimalFormat("##.#####");
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
			handleSimpleElement("generator", "ITF giscore library");
			handleSimpleElement("id", header.getId().toExternalForm());
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
			handleXmlElement(element);
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
		outputRow(row, null, null);
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
		outputRow(feature, feature.getDescription(), feature.getGeometry());
	}

	/**
	 * Output a row plus other info, a refactoring of outputting the information
	 * contained in a feature to allow the same code to output either a feature
	 * or a row.
	 * @param row
	 * @param description
	 * @param geo
	 */
	private void outputRow(Row row, String description, Geometry geo) {
		if (! headerwritten) {
			throw new IllegalStateException("Must output atom header before any feature or row");
		}
		String id = row.getId();
		Object title = row.getData(TITLE_ATTR);
		Object u = row.getData(UPDATED_ATTR);
		Object a = row.getData(AUTHOR_ATTR);
		List<String> alist = new ArrayList<String>();
		Date updated = null;
		if (u != null && u instanceof Date) {
			updated = (Date) u;
		}
		if (a != null) {
			String authors = a.toString();
			String parts[] = authors.split(",");
			for(String part : parts) {
				alist.add(part);
			}
		}
		Object link = (Object) row.getData(LINK_ATTR);
		Map<String, String> data = new HashMap<String, String>();
		for (SimpleField field : row.getFields()) {
			if (ms_builtinFields.contains(field)) continue;
			String name = field.getName();
			Object val = row.getData(field);
			if (val != null) {
				data.put(name, val.toString());
			}
		}
		outputEntry(id, title != null ? title.toString() : "",
				description, updated,
				link != null ? link.toString() : "", data,
				geo, alist);
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
			Date updated, String link, Map<String, String> data, Geometry geo, List<String> alist) {
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
			writer.writeStartElement("link");
			writer.writeAttribute("href", link);
			writer.writeEndElement();
			for(String key : data.keySet()) {
				writer.writeStartElement("ext", "data", EXT_DATA_NS);
				writer.writeAttribute("name", key);
				writer.writeCharacters(data.get(key));
				writer.writeEndElement();
			}
			if (geo != null) {
				geo.accept(this);
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
					dfmt.format(center.getLatitude().inDegrees()) + " "
							+ dfmt.format(center.getLongitude().inDegrees()));
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
			coords.append(dfmt.format(center.getLatitude().inDegrees()));
			coords.append(" ");
			coords.append(dfmt.format(center.getLongitude().inDegrees()));
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
}

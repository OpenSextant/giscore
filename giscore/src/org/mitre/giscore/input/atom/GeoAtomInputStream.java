/****************************************************************************************
 *  GeoAtomInputStream.java
 *
 *  Created: Jul 20, 2010
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
package org.mitre.giscore.input.atom;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.*;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.XmlInputStream;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.output.atom.IAtomConstants;
import org.mitre.giscore.utils.SafeDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoAtomInputStream extends XmlInputStream {

	private static final Logger logger = LoggerFactory.getLogger(GeoAtomInputStream.class);
	
	private static final SafeDateFormat fmt = new SafeDateFormat(IKml.ISO_DATE_FMT);
	private static final SafeDateFormat inputFormats[] = {
		fmt,
		new SafeDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
		new SafeDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
		new SafeDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
	};
	private static final Pattern problemTZ = Pattern.compile("([-+]\\p{Digit}{2}):(\\p{Digit}{2})$");
	
	private final Map<String, String> namespaceMap = new HashMap<String, String>();
	private String defaultNamespace;

	/**
	 * Ctor
	 * 
	 * @param stream
	 * @param arguments
     * @throws IOException if an I/O or parsing error occurs
	 */
	public GeoAtomInputStream(InputStream stream, Object arguments[])
			throws IOException {
		super(stream, DocumentType.GeoAtom);
		try {
			readRootElement();
			readHeader();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Read the root element and record any namespaces and prefixes used for the
	 * document. If the first element found isn't an atom:feed element then
	 * there's an error.
	 * <p>
	 * A valid document will not cause a stream exception to be thrown.
	 * 
     * @throws XMLStreamException if there is an error with the underlying XML.
	 */
	private void readRootElement() throws XMLStreamException {
		XMLEvent ev = stream.nextEvent();
		while (ev != null && !ev.isStartElement()) {
			ev = stream.nextEvent();
		}
		if (ev != null) {
			StartElement el = ev.asStartElement();
			if (!IAtomConstants.ATOM_URI_NS.equals(el.getName()
					.getNamespaceURI())) {

			}
			Iterator<Namespace> niter = el.getNamespaces();
			while (niter.hasNext()) {
				Namespace n = niter.next();
				if (StringUtils.isBlank(n.getPrefix()))
					defaultNamespace = el.getName().getNamespaceURI();
				else
					namespaceMap.put(n.getPrefix(),
							el.getNamespaceURI(n.getPrefix()));
			}
		}
	}

	/**
	 * Read elements from the XML stream until we find the first entry. At that
	 * point we need to stop reading and queue the atom header.
	 * 
	 * @throws IOException if an I/O or parsing error occurs
	 */
	private void readHeader() throws IOException {
		try {
			AtomHeader header = new AtomHeader();
			XMLEvent ev = stream.peek();
			while (ev != null) {
				if (ev.isStartElement()) {
					StartElement se = ev.asStartElement();
					if (se.getName().getNamespaceURI()
							.equals(IAtomConstants.ATOM_URI_NS)
							&& se.getName().getLocalPart().equals("entry")) {
						break;
					}
				}
				ev = stream.nextEvent();
				if (ev.isStartElement()) {
					StartElement se = ev.asStartElement();
					String ns = se.getName().getNamespaceURI();
					String name = se.getName().getLocalPart();
					if (ns.equals(IAtomConstants.ATOM_URI_NS)) {
						if ("generator".equals(name)) {
							header.setGenerator(getElementText(se.getName()));
						} else if ("id".equals(name)) {
							header.setId(getElementText(se.getName()));
						} else if ("title".equals(name)) {
							header.setTitle(getElementText(se.getName()));
						} else if ("updated".equals(name)) {
							header.setUpdated(parseDate(getElementText(se
									.getName())));
						} else if ("link".equals(name)) {
							AtomLink link = parseLink(se);
							if ("self".equals(link.getRel())) {
								header.setSelflink(link);
							} else {
								header.getRelatedlinks().add(link);
							}
						} else if ("author".equals(name)) {
							header.getAuthors().add(readAuthor(se));
						}
					} else {
						header.getElements().add(
								(Element) getForeignElement(se));
					}
				}
				ev = stream.peek();
			}
			for (String prefix : namespaceMap.keySet()) {
				String uri = namespaceMap.get(prefix);
				if (IAtomConstants.GIS_NS.equals(uri)
						|| IAtomConstants.EXT_DATA_NS.equals(uri))
					continue;
				header.getNamespaces().add(
						org.mitre.giscore.Namespace.getNamespace(prefix, uri));
			}
			addFirst(header);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Read author
	 * 
	 * @param se
	 * @throws XMLStreamException
	 * @throws URISyntaxException
	 */
	private AtomAuthor readAuthor(StartElement se) throws XMLStreamException,
			URISyntaxException {
		XMLEvent ev = stream.nextEvent();
		AtomAuthor a = new AtomAuthor();
		while (ev != null) {
			if (foundEndTag(ev, se.getName()))
				break; // Finished authors
			if (ev.isStartElement()) {
				StartElement start = ev.asStartElement();
				String tag = start.getName().getLocalPart();
				if ("name".equals(tag)) {
					a.setName(getElementText(start.getName()));
				} else if ("email".equals(tag)) {
					a.setEmail(getElementText(start.getName()));
				} else if ("uri".equals(tag)) {
					a.setUri(new URI(getElementText(start.getName())));
				}
			}
			ev = stream.nextEvent();
		}
		return a;
	}

	/**
	 * Extract a link from the input stream. This should be an empty element
	 * 
	 * @param se
	 *            the start element
	 * @return an atom link element
	 * @throws MalformedURLException
	 * @throws XMLStreamException
	 */
	private AtomLink parseLink(StartElement se) throws MalformedURLException,
			XMLStreamException {
		Attribute rel = se.getAttributeByName(new QName("rel"));
		Attribute href = se.getAttributeByName(new QName("href"));
		XMLEvent ev = stream.nextEvent();
		while (ev != null) {
			if (foundEndTag(ev, se.getName()))
				break;
			ev = stream.nextEvent();
		}
		return new AtomLink(new URL(href.getValue()),
				rel != null ? rel.getValue() : null);
	}

	/**
	 * Parse the date in a standard fashion
	 * 
	 * @param elementText
	 * @return
	 * @throws IOException
	 */
	private Date parseDate(String elementText) throws IOException {
		Matcher m = problemTZ.matcher(elementText);
		if (m.find()) {
			elementText = elementText.substring(0,elementText.length()-6);
			elementText += m.group(1) + m.group(2);
		}
		for(SafeDateFormat format : inputFormats) {
			try {
				return format.parse(elementText);
			} catch (ParseException e) {
				//
			}
		}
		throw new IOException("Could not parse date and time from " + elementText);
	}

    /**
     * Reads the next <code>IGISObject</code> from the InputStream.
     * 
     * @return next object, or <code>null</code> if the end of the
     *             stream is reached.
     * @throws IOException if an I/O or parsing error occurs
     */
    @CheckForNull
	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return readSaved();
		} else {
			try {
				while (true) {
					XMLEvent e = stream.nextEvent();
					if (e == null) {
						return null;
					}
					int type = e.getEventType();
					if (XMLStreamReader.START_ELEMENT == type) {
						StartElement se = e.asStartElement();
						QName name = se.getName();
						if ("entry".equals(name.getLocalPart())
								&& IAtomConstants.ATOM_URI_NS.equals(name
										.getNamespaceURI())) {
							return readEntry(se);
						} else {
							// Read but discard non entries, which shouldn't
							// ever be found
							getForeignElement(se);
							logger.warn("Found non-entry at top level of atom feed: "
									+ name.getLocalPart());
						}
					}
				}
			} catch (NoSuchElementException e) {
				return null;
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
		}
	}

	/**
	 * Read an entry and return a gis object
	 * 
	 * @param start
	 * @throws IOException if there is an error with the underlying XML.
	 */
	private IGISObject readEntry(StartElement start) throws IOException {
		try {
			List<AtomAuthor> authors = new ArrayList<AtomAuthor>();
			String title = null;
			String content = null;
			String summary = null;
			String id = null;
			Date updated = null;
			AtomLink link = null;
			Map<SimpleField, Object> data = new HashMap<SimpleField, Object>();
			Geometry geo = null;
			List<Element> elements = new ArrayList<Element>();
			XMLEvent ev = stream.nextEvent();
			while (ev != null) {
				if (foundEndTag(ev, start.getName()))
					break; // Finished entry
				if (ev.isStartElement()) {
					StartElement se = ev.asStartElement();
					String ns = se.getName().getNamespaceURI();
					String name = se.getName().getLocalPart();
					if (ns.equals(IAtomConstants.ATOM_URI_NS)) {
						if ("title".equals(name)) {
							title = getElementText(se.getName());
						} else if ("link".equals(name)) {
							link = parseLink(se);
						} else if ("content".equals(name)) {
							content = getSerializedElement(se);
						} else if ("summary".equals(name)) {
							summary = getSerializedElement(se);
						} else if ("id".equals(name)) {
							id = getElementText(se.getName());
						} else if ("updated".equals(name)) {
							String u = getElementText(se.getName());
							updated = parseDate(u);
						} else if ("author".equals(name)) {
							authors.add(readAuthor(se));
						}
					} else if (ns.equals(IAtomConstants.EXT_DATA_NS)) {
						if ("data".equals(name)) {
							Attribute aname = se.getAttributeByName(new QName(
									"name"));
							Attribute atype = se.getAttributeByName(new QName(
									"type"));
							String val = getElementText(se.getName());
							SimpleField.Type type = SimpleField.Type
									.valueOf(atype.getValue());
							data.put(new SimpleField(aname.getValue(), type),
									readValue(type, val));
						} else {
							elements.add((Element) getForeignElement(se));
						}
					} else if (ns.equals(IAtomConstants.GIS_NS)) {
						if ("point".equals(name)) {
							geo = readPoint(getElementText(se.getName()));
						} else if ("line".equals(name)) {
							geo = readLine(getElementText(se.getName()));
						} else if ("polygon".equals(name)) {
							geo = readPoly(getElementText(se.getName()));
						}
					} else {
						elements.add((Element) getForeignElement(se));
					}
				}
				ev = stream.nextEvent();
			}
			if (geo == null) {
				Row rval = new Row();
				rval.setId(id);
				rval.putData(IAtomConstants.TITLE_ATTR, title);
				rval.putData(IAtomConstants.UPDATED_ATTR, updated);
				if (summary != null) {
					rval.putData(IAtomConstants.CONTENT_ATTR, summary);
				} else if (content != null) {
					rval.putData(IAtomConstants.CONTENT_ATTR, content);
				} 
				if (link != null) {
					rval.putData(IAtomConstants.LINK_ATTR, link.getHref().toExternalForm());
				}
				for (Map.Entry<SimpleField, Object> entry : data.entrySet()) {
					rval.putData(entry.getKey(), entry.getValue());
				}
				for(AtomAuthor author : authors) {
					rval.putData(IAtomConstants.AUTHOR_ATTR, author.getName());
				}
				return rval;
			} else {
				Feature rval = new Feature();
				rval.setId(id);
				rval.setName(title);
				if (summary != null) {
					rval.setDescription(summary);
				} else if (content != null) {
					rval.setDescription(content);
				}
				rval.setStartTime(updated);
				if (link != null) {
					rval.putData(IAtomConstants.LINK_ATTR, link.getHref().toExternalForm());
				}
				for (Map.Entry<SimpleField, Object> entry : data.entrySet()) {
					rval.putData(entry.getKey(), entry.getValue());
				}
				for(AtomAuthor author : authors) {
					rval.putData(IAtomConstants.AUTHOR_ATTR, author.getName());
				}
				rval.setGeometry(geo);
				return rval;
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Process the string value according to the type. By default just store the
	 * string.
	 * @param type
	 * @param val
	 * @return value of appropriate type
	 * @throws ParseException if the string does not contain
     *            a parsable value of that type. 
	 */
	private Object readValue(Type type, String val) throws ParseException {
		switch(type) {
		case DOUBLE:
			return Double.parseDouble(val);
		case FLOAT:
			return Float.parseFloat(val);
		case INT:
		case UINT:
			return Integer.parseInt(val);
		case SHORT:
		case USHORT:
			return Short.parseShort(val);
		case BOOL:
			return Boolean.getBoolean(val);
		case DATE:
			return fmt.parse(val);
		default:
			return val;
		}
	}

	private Geometry readPoly(String elementText) {
		List<Point> pts = parsePoints(elementText);
		return new LinearRing(pts);
	}

	private Geometry readLine(String elementText) {
		List<Point> pts = parsePoints(elementText);
		return new Line(pts);
	}

	private Geometry readPoint(String elementText) {
		List<Point> pts = parsePoints(elementText);
		return pts.get(0);
	}

	private List<Point> parsePoints(String text) {
		double coords[] = parseCoords(text);
		List<Point> pts = new ArrayList<Point>();
		for (int i = 0; i < coords.length; i += 2) {
			double lat = coords[i];
			double lon = coords[i + 1];
			pts.add(new Point(lat, lon));
		}
		return pts;
	}

	private double[] parseCoords(String text) {
		String parts[] = text.trim().split("\\s+");
		double rval[] = new double[parts.length];
		for (int i = 0; i < rval.length; i++) {
			rval[i] = Double.parseDouble(parts[i]);
		}
		return rval;
	}
}

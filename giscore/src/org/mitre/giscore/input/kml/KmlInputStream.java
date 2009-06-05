/****************************************************************************************
 *  KmlInputStream.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.input.kml;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.utils.NumberStreamTokenizer;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read a kml file in as an input stream. Each time the read method is called,
 * the code tries to read a single event's worth of data. Generally that is one
 * of the following events:
 * <ul>
 * <li>A new container
 * <li>Exiting a container
 * <li>A new features
 * <li>Existing the feature
 * </ul>
 * <p>
 * There are some interesting behaviors needed to make this work properly. The
 * stream needs to buffer data before returning. For example, KML contains
 * elements like Style and StyleMap that are part of the feature. To handle this
 * correctly on output, these elements need to be earlier in the output stream
 * than the container itself.
 * <p>
 * This is handled in the KML stream by buffering those elements before the
 * container. The container (and feature) handlers return the top element in the
 * stack, not necessarily the container or feature. Additionally, the read
 * method always empties the stack before looking for another element to return.
 * <p>
 * The actual handling of containers and other features has some uniform
 * methods. Every feature in KML can have a set of common attributes and
 * additional elements. The
 * {@link #handleProperties(Common, XMLEvent, String)} method takes care of
 * these. This returns <code>false</code> if the current element isn't a common
 * element, which allows the caller to handle the code.
 * <p>
 * Geometry is handled by common code as well. All coordinates in KML are
 * transmitted as tuples of two or three elements. The formatting of these is
 * consistent and is handled by {@link #parseCoordinates(String)}.
 * <p>
 * Notes/Limitations:
 * <p> 
 * Note only a single Data/SchemaData/Schema ExtendedData mapping is assumed
 * per Feature but Collections can reference among several Schemas. Features
 * with mixed Data and/or Multiple SchemaData elements will be associated only
 * with the last Schema referenced.
 * <p> 
 * Unsupported tags include the following:
 *  atom:author, atom:link, address, xal:AddressDetails, Camera, LookAt,
 *  Metadata, open, phoneNumber, Region, Snippet, snippet, visibility.
 * <p>
 * While these tags don't break anything if present they are ignored.
 * <p> 
 * Limited support for PhotoOverlay which creates an basic overlay object
 * without retaining PhotoOverlay-specific properties (rotation, ViewVolume,
 * ImagePyramid, Point, shape, etc).
 * <p>
 * Limited support for Model geometry.
 * <p> 
 * Limited support for NetworkLinkControl which creates a wrapper for the link
 * with the top-level info but the update details (i.e. Create, Delete, and Change) are discarded.
 *
 * @author DRAND
 * @author J.Mathews
 * 
 */
public class KmlInputStream extends GISInputStreamBase implements IKml {

    private static final Logger log = LoggerFactory.getLogger(KmlInputStream.class);

    private InputStream is;
    private XMLEventReader stream;

	private static final XMLInputFactory ms_fact;
	private static final Set<String> ms_features = new HashSet<String>();
	private static final Set<String> ms_containers = new HashSet<String>();
	private static final Set<String> ms_attributes = new HashSet<String>();
	private static final Set<String> ms_geometries = new HashSet<String>();
	private Map<String, String> schemaAliases;
	private Map<String, Schema> schemata = new HashMap<String, Schema>();

	private static final List<DateFormat> ms_dateFormats = new ArrayList<DateFormat>();
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static DatatypeFactory fact;
	private static final Longitude COORD_ERROR = new Longitude();

	static {
		ms_fact = XMLInputFactory.newInstance();
		ms_features.add(PLACEMARK);
		ms_features.add(NETWORK_LINK);
		ms_features.add(GROUND_OVERLAY);
		ms_features.add(PHOTO_OVERLAY);
		ms_features.add(SCREEN_OVERLAY);

		ms_containers.add(FOLDER);
		ms_containers.add(DOCUMENT);

		ms_attributes.add(VISIBILITY);
		ms_attributes.add(OPEN);
		ms_attributes.add(ADDRESS);
		ms_attributes.add(PHONE_NUMBER);

		ms_geometries.add(POINT);
		ms_geometries.add(LINE_STRING);
		ms_geometries.add(LINEAR_RING);
		ms_geometries.add(POLYGON);
		ms_geometries.add(MULTI_GEOMETRY);
		ms_geometries.add(MODEL);

		ms_dateFormats.add(new SimpleDateFormat(ISO_DATE_FMT)); // default: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy"));
		for (DateFormat fmt : ms_dateFormats) {
			fmt.setTimeZone(UTC);
		}
	}

	/**
	 * Creates a <code>KmlInputStream</code>
     * and saves its argument, the input stream
     * <code>input</code>, for later use. 
	 * 
	 * @param input
	 *            input stream for the kml file, never <code>null</code>
	 * @throws IOException if an I/O or parsing error occurs
	 * @throws IllegalArgumentException if input is null
	 */
	public KmlInputStream(InputStream input) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input should never be null");
		}
		is = input;
		addLast(new DocumentStart(DocumentType.KML));
		try {
			stream = ms_fact.createXMLEventReader(is);
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
	}

	/**
	 * Closes this input stream and releases any system resources 
     * associated with the stream.
	 * Once the stream has been closed, further read() invocations may throw an IOException.
     * Closing a previously closed stream has no effect.
	 */
	public void close() {
		if (stream != null)
			try {
				stream.close();
			} catch (XMLStreamException e) {
				log.warn("Failed to close reader", e);
			}
		if (is != null) {
			IOUtils.closeQuietly(is);
			is = null;
		}
	}

	/**
	 * Push an object back into the read queue
	 * 
	 * @param o object to push onto queue
	 */
	public void pushback(IGISObject o) {
		addFirst(o);
	}

	/**
	 * Reads the next <code>IGISObject</code> from the InputStream.
	 *
	 * @return next <code>IGISObject</code>, null if end of stream reached
	 * @throws IOException if an I/O error occurs
	 */
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
					switch (e.getEventType()) {
					case XMLStreamReader.START_ELEMENT:
						IGISObject se = handleStartElement(e);
						if (se == NullObject.getInstance())
							break;
						return se; // start element is GISObject or null (indicating EOF)
					case XMLStreamReader.END_ELEMENT:
						IGISObject rval = handleEndElement(e);
						if (rval != null)
							return rval;
                    /*
                    // saving comments messes up the junit tests so comment out for now
                    break;
                    case XMLStreamReader.COMMENT:
                        IGISObject comment = handleComment(e);
						if (comment != null)
							return comment;
					*/
                    }
				}
			} catch (NoSuchElementException e) {
				return null;
			} catch (XMLStreamException e) {
				final IOException e2 = new IOException();
				e2.initCause(e);
				throw e2;
			}
		}
	}

    private IGISObject handleComment(XMLEvent e) throws XMLStreamException {
        if (e instanceof javax.xml.stream.events.Comment) {
            String text = ((javax.xml.stream.events.Comment)e).getText();
            if (StringUtils.isNotBlank(text))
                return new Comment(text);
        }
        return null;
    }

    /**
	 * Read elements until we find a feature or a schema element. Use the name
	 * and description data to set the equivalent data on the container start.
	 * 
	 * @param e
	 * @return
	 * @throws XMLStreamException
	 */
	private IGISObject handleContainer(XMLEvent e) throws XMLStreamException {
		StartElement se = e.asStartElement();
		String containerTag = se.getName().getLocalPart();
		ContainerStart cs = new ContainerStart(containerTag);
		addFirst(cs);

		while (true) {
			XMLEvent ne = stream.peek();
			// Found end tag, sometimes a container has no other content
			if (foundEndTag(ne, containerTag)) {
				break;
			} else if (ne != null
					&& ne.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement nextel = ne.asStartElement();
				String tag = nextel.getName().getLocalPart();
				// check if element has been aliased in Schema
				// only used for old-style KML 2.0 Schema defs with "parent" attribute/element.
				if (schemaAliases != null) {
					String newName = schemaAliases.get(tag);
					if (newName != null) {
						// log.info("Alias " + tag +" -> " + newName);
						tag = newName;
					}
				}
				if (ms_containers.contains(tag) || ms_features.contains(tag)
						|| SCHEMA.equals(tag)) {
					break;
				}
			}

			XMLEvent ee = stream.nextEvent();
			if (ee == null) {
				break;
			} else if (ee.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement sl = ee.asStartElement();
				QName name = sl.getName();
				String localname = name.getLocalPart();
				//System.out.println(localname);//debug
				if (!handleProperties(cs, ee, localname)) {
					// Ignore other attributes
				}
            }
		}

		return readSaved();
	}

	/**
	 * Handle the elements found in all features
	 * 
	 * @param feature
	 * @param ee
	 * @param localname
	 * @return <code>true</code> if the event has been handled
	 */
	private boolean handleProperties(Common feature, XMLEvent ee,
			String localname) {
        try {
            if (localname.equals(NAME)) {
                feature.setName(getNonEmptyElementText());
                return true;
            } else if (localname.equals(DESCRIPTION)) {
                feature.setDescription(getElementText(localname));
                return true;
            } else if (localname.equals(STYLE)) {
                handleStyle(feature, ee);
                return true;
            } else if (ms_attributes.contains(localname)) {
                // Skip, but consume
                return true;
			} else if (localname.equals(STYLE_URL)) {
                feature.setStyleUrl(stream.getElementText());
                return true;
			} else if (localname.equals(REGION)) {
                handleRegion(feature, ee);
                return true;
            } else if (localname.equals(TIME_SPAN) || localname.equals(TIME_STAMP)) {
                handleTimePrimitive(feature, ee);
                return true;
            } else if (localname.equals(STYLE_MAP)) {
                handleStyleMap(feature, ee);
                return true;
            } else if (localname.equals(LOOK_AT) || localname.equals(CAMERA)) {
                handleAbstractView(feature, ee);
                return true;
			} else if (localname.equals(EXTENDED_DATA)) {
                handleExtendedData(feature, ee);
                return true;
			} else if (localname.equals(METADATA)) {
                handleMetadata(feature, ee);
                return true;
			} else if (localname.equals(SNIPPET)) {
                handleSnippet(feature, ee); // Snippet
                return true;
			} else if (localname.equals("snippet")) {
				handleSnippet(feature, ee); // snippet (deprecated in 2.2)
				// Note: schema shows Snippet is deprecated but Google documentation and examples
				// suggestion snippet (lower case 's') is deprecated instead...
				return true;
            } else if (localname.equals("AddressDetails")) {
                // skip AddressDetails (namespace: urn:oasis:names:tc:ciq:xsdschema:xAL:2.0)
                log.debug("skip " + localname);
                skipNextElement(stream, localname);
                return true;
			} else {
                StartElement sl = ee.asStartElement();
				QName name = sl.getName();
                // skip atom:link and atom:author elements
                if ("http://www.w3.org/2005/Atom".equals(name.getNamespaceURI())) {
                    log.debug("skip atom:" + localname);
                    skipNextElement(stream, localname);
                    return true;
                }
                //System.out.println("*** skip other: " + localname + " sl=" + sl + " name=" + name.getNamespaceURI());
            }
		} catch (XMLStreamException e) {
            log.error("Failed to handle: " + localname, e);
            // TODO: do we have any situation where need to skip over failed localname element??
            // skipNextElement(stream, localname);
        }
        return false;
	}

    private String getElementText(String localname) throws XMLStreamException {
        /*
         * some elements such as description may have HTML elements as child elements rather than
         * within required CDATA block.
         */
        try {
            return getNonEmptyElementText();
        } catch (XMLStreamException e) {
            log.warn("Unable to parse " + localname + " as text element: " + e);
            skipNextElement(stream, localname);
            return null;
        }
    }

    /**
	 * @param cs
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleExtendedData(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				String tag = se.getName().getLocalPart();
				/*
				 * TODO: Add xmlns:prefix handling
				 */
				if (tag.equals(DATA)) {
					Attribute name = se.getAttributeByName(new QName(NAME));
					if (name != null) {
						String value = parseValue(DATA);
                        if (value != null)
    						cs.putData(new SimpleField(name.getValue()), value);
                        // NOTE: if feature has mixed Data and SchemaData then Data fields will be associated with last SchemaData schema processed
					}
				} else if (tag.equals(SCHEMA_DATA)) {
					Attribute url = se
							.getAttributeByName(new QName(SCHEMA_URL));
					if (url != null) {
						String uri = url.getValue();
						handleSchemaData(uri, cs);
						try {
							cs.setSchema(new URI(uri));
						} catch (URISyntaxException e) {
							log.error("Failed to handle SchemaData schemaUrl=" + uri , e);
						}
					}
				}
				// Note external namespace contents not in ExtendedData supported
				// http://code.google.com/apis/kml/documentation/extendeddata.html
				/*
					<ExtendedData xmlns:camp="http://campsites.com">
					  <camp:number>14</camp:number>
					  <camp:parkingSpaces>2</camp:parkingSpaces>
					  <camp:tentSites>4</camp:tentSites>
					</ExtendedData>
				 */
			} else if (foundEndTag(next, EXTENDED_DATA)) {
				return;
			}
		}
	}

	/**
	 * Is this event a matching end tag?
	 * 
	 * @param event
	 *            the event
	 * @param tag
	 *            the tag
	 * @return <code>true</code> if this is an end element event for the
	 *         matching tag
	 */
	private boolean foundEndTag(XMLEvent event, String tag) {
        if (event == null) {
           return true;
        }
        if (event.getEventType() == XMLEvent.END_ELEMENT) {
			if (event.asEndElement().getName().getLocalPart().equals(tag))
				return true;
		}
		return false;
	}

	/**
	 * Found a specific start tag
	 * 
	 * @param se
	 * @param tag
	 * @return
	 */
	private boolean foundStartTag(StartElement se, String tag) {
		return se.getName().getLocalPart().equals(tag);
	}

	/**
	 * @param uri a reference to a schema, if local then use that schema's
	 * simple field objects instead of creating ones on the fly
	 * @param cs Feature/Container for ExtendedData tag
	 * @throws XMLStreamException
	 */
	private void handleSchemaData(String uri, Common cs)
			throws XMLStreamException {
		XMLEvent next;
		if (uri.startsWith("#")) uri = uri.substring(1);
		Schema schema = schemata.get(uri);
		
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				if (foundStartTag(se, SIMPLE_DATA)) {
					Attribute name = se.getAttributeByName(new QName(NAME));
					if (name != null) {
						String value = stream.getElementText();
						SimpleField field = null;
						if (schema != null) {
							field = schema.get(name.getValue());
						}
						if (field == null) {
							// Either we don't know the schema or it isn't local
							field = new SimpleField(name.getValue());
						}
                        // NOTE: if feature has multiple SchemaData elements (multi-schemas) then fields will be associated with last SchemaData schema processed
						cs.putData(field, value);
					}
				}
			} else if (foundEndTag(next, SCHEMA_DATA)) {
				return;
			}
		}
	}

	/**
	 * @param tag
	 * @return the value associated with the element
	 * @throws XMLStreamException
	 */
	private String parseValue(String tag) throws XMLStreamException {
		XMLEvent next;
		String rval = null;
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return rval;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				if (foundStartTag(se, VALUE)) {
					rval = stream.getElementText();
				}
			} else if (foundEndTag(next, tag)) {
				return rval;
			}
		}
	}

	/**
	 * @param cs
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleMetadata(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, METADATA))
				return;
		}
	}

	/**
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleAbstractView(Common cs, XMLEvent ee)
			throws XMLStreamException {
		StartElement se = ee.asStartElement();
		XMLEvent next;
        String localname = se.getName().getLocalPart();

        while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, localname))
				return;
		}
	}

	/**
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleStyleMap(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;
		StyleMap sm = new StyleMap();
		addFirst(sm);
		StartElement sl = ee.asStartElement();
		Attribute id = sl.getAttributeByName(new QName(ID));
		if (id != null) {
			sm.setId(id.getValue());
		}

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement ie = next.asStartElement();
				if (foundStartTag(ie, PAIR)) {
					handleStyleMapPair(sm);
				}
			}
			if (foundEndTag(next, STYLE_MAP)) {
				return;
			}
		}
	}

	/**
	 * @param sm
	 * @throws XMLStreamException
	 */
	private void handleStyleMapPair(StyleMap sm) throws XMLStreamException {
		String key = null, value = null;
		while (true) {
			XMLEvent ce = stream.nextEvent();
			if (ce.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = ce.asStartElement();
				if (foundStartTag(se, KEY)) {
					key = stream.getElementText();
				} else if (foundStartTag(se, STYLE_URL)) {
					value = stream.getElementText();
				}
			}
			XMLEvent ne = stream.peek();
			if (foundEndTag(ne, STYLE_MAP)) {
				if (key != null && value != null) {
					sm.put(key, value);
				}
				return;
			}
		}
	}

	/**
	 * @param cs
     * @param ee
	 * @throws XMLStreamException
	 */
	private void handleTimePrimitive(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;
		StartElement sl = ee.asStartElement();
		QName tag = sl.getName();
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				String time = null;
				try {
                    if (foundStartTag(se, WHEN)) {
                        time = stream.getElementText();
                        Date date = parseDate(time.trim());
                        cs.setStartTime(date);
                        cs.setEndTime(date);
                    } else if (foundStartTag(se, BEGIN)) {
						time = stream.getElementText();
						cs.setStartTime(parseDate(time.trim()));
					} else if (foundStartTag(se, END)) {
						time = stream.getElementText();
						cs.setEndTime(parseDate(time.trim()));
					}
				} catch (IllegalArgumentException e) {
					log.warn("Ignoring bad time: " + time + ": " + e);
				} catch (ParseException e) {
					log.warn("Ignoring bad time: " + time + ": " + e);
				}
			}
			if (foundEndTag(next, tag.getLocalPart())) {
				return;
			}
		}
	}

	/**
	 * Parse kml:dateTimeType XML date/time field and convert to Date object.
	 *
	 * @param datestr  Lexical representation for one of XML Schema date/time datatypes.
	 * @return <code>Date</code> created from the <code>lexicalRepresentation</code>.
	 * @throws ParseException If the <code>lexicalRepresentation</code> is not a valid <code>Date</code>.
	 */
	public static Date parseDate(String datestr) throws ParseException {
		try {
			if (fact == null) fact = DatatypeFactory.newInstance();
			XMLGregorianCalendar o = fact.newXMLGregorianCalendar(datestr);
			GregorianCalendar cal = o.toGregorianCalendar();
			String type = o.getXMLSchemaType().getLocalPart();
			boolean setTimeZone = true;
			if ("dateTime".equals(type)) {
				// dateTime (YYYY-MM-DDThh:mm:ssZ)
				// dateTime (YYYY-MM-DDThh:mm:sszzzzzz)
				// Second form gives the local time and then the +/- conversion to UTC.
				// Set timezone to UTC if other than dateTime formats with explicit timezones
				// e.g. 2009-03-14T18:10:46+03:00, 2009-03-14T18:10:46-05:00
				int ind = datestr.lastIndexOf('T') + 1; // index should never be -1 if type is dateTime
				if (ind > 0 && (datestr.indexOf('+', ind) > 0 || datestr.indexOf('-', ind) > 0))
					setTimeZone = false;
				// if timeZone is missing (e.g. 2009-03-14T21:10:50) then 'Z' is assumed and UTC is used
			}
			if (setTimeZone) cal.setTimeZone(UTC);
			//else datestr += "*";
			//System.out.format("%-10s\t%s%n", type, datestr);
			/*
			  possible dateTime types: { dateTime, date, gYearMonth, gYear }
			  if other than dateTime then must adjust the time to 0

			  1997                      gYear        (YYYY)						1997-01-01T00:00:00.000Z
			  1997-07                   gYearMonth   (YYYY-MM)					1997-07-01T00:00:00.000Z
			  1997-07-16                date         (YYYY-MM-DD)				1997-07-16T00:00:00.000Z
			  1997-07-16T07:30:15Z      dateTime (YYYY-MM-DDThh:mm:ssZ)			1997-07-16T07:30:15.000Z
			  1997-07-16T07:30:15.30Z   dateTime     							1997-07-16T07:30:15.300Z
			  1997-07-16T10:30:15+03:00 dateTime (YYYY-MM-DDThh:mm:sszzzzzz)	1997-07-16T07:30:15.000Z
			 */
			if (!"dateTime".equals(type)) {
				cal.set(Calendar.HOUR_OF_DAY, 0);
			}
			return cal.getTime();
		} catch (IllegalArgumentException iae) {
			final ParseException e2 = new ParseException(iae.getMessage(), 0);
			e2.initCause(iae);
			throw e2;
		} catch (DatatypeConfigurationException ce) {
			// if unable to create factory then try brute force
			log.error("Failed to get DatatypeFactory", ce);
			// note this does not correctly handle dateTime (YYYY-MM-DDThh:mm:sszzzzzz) format 
			ParseException e = null;
			for (DateFormat fmt : ms_dateFormats) {
				try {
					return fmt.parse(datestr);
				} catch (ParseException pe) {
					e = pe;
				}
			}
			throw e;
		}
	}

	/**
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleRegion(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, REGION)) {
				return;
			}
		}
	}

	/**
	 * @param cs
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleSnippet(Common cs, XMLEvent ee)
			throws XMLStreamException {
        StartElement se = ee.asStartElement();
		XMLEvent next;
        String localname = se.getName().getLocalPart();

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, localname)) {
				return;
			}
		}
	}

	/**
	 * Get the style data and push the style onto the buffer so it is returned
	 * first, before its container or placemark
	 * 
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleStyle(Common cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;

		Style style = new Style();
		StartElement sse = ee.asStartElement();
		Attribute id = sse.getAttributeByName(new QName(ID));
		if (id != null) {
			style.setId(id.getValue());
		}
		addFirst(style);
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;

			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(ICON_STYLE)) {
					handleIconStyle(style);
				} else if (name.equals(LINE_STYLE)) {
					handleLineStyle(style);
				} else if (name.equals(BALLOON_STYLE)) {
					handleBalloonStyle(style);
				} else if (name.equals(LABEL_STYLE)) {
					handleLabelStyle(style);
				} else if (name.equals(POLY_STYLE)) {
					handlePolyStyle(style);
				}
			}

			if (foundEndTag(next, STYLE)) {
				return;
			}
		}
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handlePolyStyle(Style style) throws XMLStreamException {
		Color color = Color.white;
		boolean fill = true;
		boolean outline = true;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(FILL)) {
					fill = isTrue(stream.getElementText());
				} else if (name.equals(OUTLINE)) {
					outline = isTrue(stream.getElementText());
				} else if (name.equals(COLOR)) {
					color = parseColor(stream.getElementText());
				}
			}
			if (foundEndTag(e, POLY_STYLE)) {
				style.setPolyStyle(color, fill, outline);
				return;
			}
		}
	}

	/**
	 * Determine if an element value is true or false
	 * 
	 * @param val
	 *            the value, may be <code>null</code>
	 * @return <code>false</code> if the value is not the single character "1".
	 */
	private boolean isTrue(String val) {
		return val != null && val.trim().equals("1");
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleLabelStyle(Style style) throws XMLStreamException {
		double scale = 1;
		Color color = Color.black;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(SCALE)) {
                    String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            scale = Double.parseDouble(value);
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid scale value: " + value);
                        }
				} else if (name.equals(COLOR)) {
					color = parseColor(stream.getElementText());
				}
			}
			else if (foundEndTag(e, LABEL_STYLE)) {
				style.setLabelStyle(color, scale);
				return;
			}
		}
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleLineStyle(Style style) throws XMLStreamException {
		double width = 1;
		Color color = Color.white;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(WIDTH)) {
					String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            width = Double.parseDouble(value);
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid width value: " + value);
                        }
				} else if (name.equals(COLOR)) {
					String value = stream.getElementText();
					color = parseColor(value);
					if (color == null) {
						//log.warn("Invalid LineStyle color: " + value);
						color = Color.white; // use default
					}
				}
			}
			else if (foundEndTag(e, LINE_STYLE)) {
				style.setLineStyle(color, width);
				return;
			}
		}
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleBalloonStyle(Style style) throws XMLStreamException {
		String text = "";
		Color color = Color.white;
		Color textColor = Color.black;
		String displayMode = "default"; // [default] | hide
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(TEXT)) {
					text = stream.getElementText();
				} else if (name.equals(BG_COLOR)) {
					color = parseColor(stream.getElementText());
				} else if (name.equals(DISPLAY_MODE)) {
					displayMode = getNonEmptyElementText();
				} else if (name.equals(TEXT_COLOR)) {
					textColor = parseColor(stream.getElementText());
				}
			}
			if (foundEndTag(e, BALLOON_STYLE)) {
				style.setBalloonStyle(color, text, textColor, displayMode);
				return;
			}
		}
	}

    /**
	 * Get the href subelement from the Icon element.
	 * 
	 * @return the href, <code>null</code> if not found.
	 * @throws XMLStreamException
	 */
	private String parseIconHref() throws XMLStreamException {
		String href = null;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(HREF)) {
					href = getNonEmptyElementText();
				}
			}
			if (foundEndTag(e, ICON)) {
				return href;
			}
		}
	}

	/**
	 * Parse the color from a kml file, in AABBGGRR order.
	 * 
	 * @param cstr
	 *            a hex encoded string, must be exactly 8 characters long.
	 * @return the color value, null if value is null, empty or invalid
	 */
	private Color parseColor(String cstr) {
		if (cstr == null) return null;
        cstr = cstr.trim();
        if (cstr.length() == 8)
            try {
                int alpha = Integer.parseInt(cstr.substring(0, 2), 16);
                int blue = Integer.parseInt(cstr.substring(2, 4), 16);
                int green = Integer.parseInt(cstr.substring(4, 6), 16);
                int red = Integer.parseInt(cstr.substring(6, 8), 16);
                return new Color(red, green, blue, alpha);
            } catch (IllegalArgumentException ex) {
                // fall through and log bad value
            }
        
        log.warn("Invalid color value: " + cstr);
        return null;
	}

    /**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleIconStyle(Style style) throws XMLStreamException {
		String url = null;
		double scale = 1.0;		// default value
		Color color = Color.white;	// default="ffffffff"
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(SCALE)) {
                    String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            scale = Double.parseDouble(value);
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid scale value: " + value);
                        }
				} else if (name.equals(COLOR)) {
					String value = stream.getElementText();
					color = parseColor(value);
					if (color == null) {
						//log.warn("Invalid IconStyle color: " + value);
						color = Color.white; // use default="ffffffff"
					}
				} else if (name.equals(ICON)) {
					url = parseIconHref();
				}
			}
			if (foundEndTag(e, ICON_STYLE)) {
				try {
					style.setIconStyle(color, scale, url);
				} catch (IllegalArgumentException iae) {
					log.warn("Invalid style: " + iae);
				}
				return;
			}
		}
	}

	/**
	 * @param e current XML element
	 * @return IGISObject representing current element,
	 * 			NullObject if failed to parse and unable to skip to end tag for that element
	 * @throws XMLStreamException 
	 * @throws IOException if encountered NetworkLinkControl or out of order Style element
	 * 			and failed to skip to end tag for that element.
	 */
	@SuppressWarnings("unchecked")
	private IGISObject handleStartElement(XMLEvent e) throws XMLStreamException, IOException {
		StartElement se = e.asStartElement();
		String localname = se.getName().getLocalPart();
		String elementName = localname; // differs from localname if aliased by Schema mapping
		//System.out.println(localname); //debug
		// check if element has been aliased in Schema
		// only used for old-style KML 2.0/2.1 Schema defs with "parent" attribute.
		// generally only Placemarks are aliased. Not much use to alias Document or Folder elements, etc.
		if (schemaAliases != null) {
			String newName = schemaAliases.get(elementName);
			if (newName != null) {
				// log.info("Alias " + elementName + " -> " + newName);
				// Note: does not support multiple levels of aliases (e.g. Person <- Placemark; VipPerson <- Person, etc.)
				// To-date have only seen aliases for Placemarks so don't bother checking.
				elementName = newName;
			}
		}
		try {
			if (ms_features.contains(elementName)) {
				return handleFeature(e, elementName);
			} else if (ms_containers.contains(elementName)) {
				//System.out.println("** handle container: " + elementName);
				return handleContainer(se);
			} else if (SCHEMA.equals(localname)) {
				return handleSchema(se, localname);
			} else if (NETWORK_LINK_CONTROL.equals(localname)) {
				return handleNetworkLinkControl(stream, localname);
			} else if (STYLE.equals(localname)) {
                StringBuilder sb = new StringBuilder();
                sb.append("placeholder for style");
                int count = 0;
                for(Iterator it = se.getAttributes(); it.hasNext(); ) {
                    Object o = it.next();
                    if (o instanceof Attribute) {
                        Attribute a = (Attribute)o;
                        count++;
                        sb.append("\n\t").append(a.getName()).append("=").append(a.getValue());
                    }
                }

                //System.out.println("XXX: skipping found style out of order");
				//handleStyle(null, e);
				//return buffered.removeFirst();
                try {
					skipNextElement(stream, localname);
				} catch (XMLStreamException xe) {
					final IOException e2 = new IOException();
					e2.initCause(xe);
					throw e2;
				}
                if (count != 0) sb.append(" \n");
                return new Comment(sb.toString());
			} else {
				// Look for next start element and recurse
				e = stream.nextTag();
				if (e != null && e.getEventType() == XMLEvent.START_ELEMENT) {
					return handleStartElement(e);
				}                
			}
		} catch (XMLStreamException e1) {
			log.warn("Failed at element: " + localname);
			skipNextElement(stream, localname);
		}

		// return non-null NullObject to skip but not end parsing...
		return NullObject.getInstance();
	}

	private void skipNextElement(XMLEventReader element, String localname) throws XMLStreamException {
		while (true) {
			XMLEvent next = element.nextEvent();
			if (next == null || foundEndTag(next, localname)) {
				break;
			}
		}
	}

	private IGISObject handleNetworkLinkControl(XMLEventReader stream, String localname) throws XMLStreamException {
		NetworkLinkControl c = new NetworkLinkControl();
		boolean updateFound = false;
		String updateType = null;
		while (true) {
			XMLEvent next = stream.nextEvent();
			if (next == null)
				break;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				if (updateFound) {
					if (foundStartTag(se, "targetHref")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setTargetHref(val);
						// TODO: NetworkLinkControl can have 1 or more Update controls
						// ... TODO: handle Update details
					} else if (foundStartTag(se, "Create")) {
						updateType = "Create";
					} else if (foundStartTag(se, "Delete")) {
						updateType = "Delete";
					} else if (foundStartTag(se, "Change")) {
						updateType = "Change";
					}
					if (updateType != null) {
						// log.info("XXX: updatetupe=" + updateType);
						c.setUpdateType(updateType);
						skipNextElement(stream, NETWORK_LINK_CONTROL);
						break;
					}
				} else {
					if (foundStartTag(se, "minRefreshPeriod")) {
						Double val = getDoubleElementValue("minRefreshPeriod");
						if (val != null) c.setMinRefreshPeriod(val);
					} else if (foundStartTag(se, "maxSessionLength")) {
						Double val = getDoubleElementValue("maxSessionLength");
						if (val != null) c.setMaxSessionLength(val);
					} else if (foundStartTag(se, "cookie")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setCookie(val);
					} else if (foundStartTag(se, "message")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setMessage(val);
					} else if (foundStartTag(se, "linkName")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setLinkName(val);
					} else if (foundStartTag(se, "linkDescription")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setLinkDescription(val);
					} else if (foundStartTag(se, "linkSnippet")) {
						String val = getNonEmptyElementText();
						if (val != null) c.setLinkSnippet(val);
					} else if (foundStartTag(se, "expires")) {
						String expires = getNonEmptyElementText();
						if (expires != null)
							try {
								c.setExpires(parseDate(expires));
							} catch (ParseException e) {
								log.warn("Ignoring bad expires value: " + expires + ": " + e);
							}
					} else if (foundStartTag(se, "Update")) {
						updateFound = true; // start phase 2 and parse inside Update element
					}
				}
			} else if (foundEndTag(next, NETWORK_LINK_CONTROL)) {
				break;
			}
		}
		return c;
	}

	/**
	 * @param element
	 * @param localname
	 * @return
	 * @throws XMLStreamException
	 */
	private IGISObject handleSchema(StartElement element, String localname)
			throws XMLStreamException {
		Schema s = new Schema();
		addLast(s);
		Attribute attr = element.getAttributeByName(new QName(NAME));
		String name = getNonEmptyAttrValue(attr);

		// get parent attribute for old-style KML 2.0/2.1 which aliases KML elements
		// (e.g. Placemarks) with user-defined ones.
/*
        <Schema name="S_FOBS_USA_ISAF_NATO_DSSSSSSDDDD" parent="Placemark">
            <SimpleField name="NAME" type="wstring"/>
            <SimpleField name="DATE" type="wstring"/>
            <SimpleField name="MGRS" type="wstring"/>
        </Schema>
*/
		attr = element.getAttributeByName(new QName(PARENT));
		String parent = getNonEmptyAttrValue(attr);
		Attribute id = element.getAttributeByName(new QName(ID));
		if (id != null) {
			String uri = id.getValue();
			// remember the schema for later references
			schemata.put(uri, s);
			try {
				s.setId(new URI(uri));
			} catch (URISyntaxException e) {
				log.warn("Invalid schema id " + uri, e);
			}
		}

		int gen = 0;
		while (true) {
			XMLEvent next = stream.nextEvent();
			if (next == null)
				break;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				if (foundStartTag(se, SIMPLE_FIELD)) {
					Attribute fname = se.getAttributeByName(new QName(NAME));
					String fieldname = fname != null ? fname.getValue() : "gen" + gen++;
					// http://code.google.com/apis/kml/documentation/kmlreference.html#simplefield
					// If either the type or the name is omitted, the field is ignored. 
					try {
						SimpleField field = new SimpleField(fieldname);
						Attribute type = se.getAttributeByName(new QName(TYPE));
						SimpleField.Type ttype = SimpleField.Type.STRING; // default
						if (type != null) {
							String typeValue = type.getValue();
							// old-style "wstring" is just a string type
							if (StringUtils.isNotBlank(typeValue) && !"wstring".equalsIgnoreCase(typeValue))
								ttype = SimpleField.Type.valueOf(typeValue.toUpperCase());
						}
						field.setType(ttype);
						String displayName = parseDisplayName(SIMPLE_FIELD);
						field.setDisplayName(displayName);
						s.put(fieldname, field);
					} catch (IllegalArgumentException e) {
						log.warn("Invalid schema field " + fieldname + ": " + e.getMessage());
					}
				} else if (foundStartTag(se, PARENT)) {
					 // parent should only appear as Schema child element in KML 2.0 or 2.1
/*
        <Schema>
            <name>S_FOBS_USA_ISAF_NATO_DSSSSSSDDDD</name>
            <parent>Placemark</parent>
            <SimpleField name="NAME" type="string"/>
            <SimpleField name="DATE" type="string"/>
            <SimpleField name="MGRS" type="string"/>
        </Schema>
*/
					String parentVal = getNonEmptyElementText();
					if (parentVal != null) parent = parentVal;
				} else if (foundStartTag(se, NAME)) {
					 // name should only appear as Schema child element in KML 2.0 or 2.1
					String nameVal = getNonEmptyElementText();
					if (nameVal != null) name = nameVal;
				}
			} else if (foundEndTag(next, SCHEMA)) {
				break;
			}
		}

		if (name != null) s.setName(name);

		// define old-style parent association
		if (parent != null) {
			s.setParent(parent);
			if (name != null) {
				// add alias to schema alias list
				if (schemaAliases == null)
					schemaAliases = new HashMap<String, String>();
				schemaAliases.put(name, parent);
			}
		}
		
		return readSaved();
	}

	/**
	 * Returns non-empty text value from attribute
	 * @param attr Attribute
	 * @return non-empty text value trimmed from attribute,
	 * 			null if empty
	 */
	private static String getNonEmptyAttrValue(Attribute attr) {
		if (attr != null) {
			String value = attr.getValue();
			if (value != null) {
				value = value.trim();
				if (value.length() != 0) return value;
			}
		}
		return null;
	}

	/**
	 * @param tag
	 * @return
	 * @throws XMLStreamException
	 */
	private String parseDisplayName(String tag) throws XMLStreamException {
		String rval = null;
		while (true) {
			XMLEvent ee = stream.nextEvent();
			if (ee == null) {
				break;
			}
			if (ee.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement sl = ee.asStartElement();
				QName name = sl.getName();
				String localname = name.getLocalPart();
				if (localname.equals(DISPLAY_NAME)) {
					rval = getNonEmptyElementText();
				}
			} else if (foundEndTag(ee, tag)) {
				break;
			}
		}
		return rval;
	}

	/**
	 * @param e
	 * @param type
	 * @return
	 * @throws XMLStreamException
	 */
	private IGISObject handleFeature(XMLEvent e, String type) throws XMLStreamException {
		StartElement se = e.asStartElement();
		boolean placemark = PLACEMARK.equals(type);
		boolean screen = SCREEN_OVERLAY.equals(type);
		boolean photo = PHOTO_OVERLAY.equals(type);
		boolean ground = GROUND_OVERLAY.equals(type);
		boolean network = NETWORK_LINK.equals(type);
		boolean isOverlay = screen || photo || ground;
		Feature fs;
		if (placemark) {
			fs = new Feature();
		} else if (screen) {
			fs = new ScreenOverlay();
		} else if (photo) {
			fs = new PhotoOverlay();
		} else if (ground) {
			fs = new GroundOverlay();
		} else if (network) {
			fs = new NetworkLink();
		} else {
			String localname = se.getName().getLocalPart();
			if (!localname.equals(type))
				log.error(String.format("Found new unhandled feature type: %s [%s]", type, localname));
			else
				log.error("Found new unhandled feature type: " + type);
			return NullObject.getInstance();
		}

		addFirst(fs);
		while (true) {
			XMLEvent ee = stream.nextEvent();
			if (ee == null) {
				break;
			}
			if (ee.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement sl = ee.asStartElement();
				QName name = sl.getName();
				String localname = name.getLocalPart();
				// Note: if element is aliased Placemark then metadata fields won't be saved
				// could treat as ExtendedData if want to preserve this data.
				if (!handleProperties(fs, ee, localname)) {
					// Deal with specific feature elements
					if (ms_geometries.contains(localname)) {
						// Point, LineString, Polygon, Model, etc.
                        try {
                            Geometry geo = handleGeometry(sl);
                            if (geo != null) {
                                fs.setGeometry(geo);
                            }
                        } catch (RuntimeException iae) {
                            log.warn("Failed geometry: " + fs, iae);
                        }
					} else if (isOverlay) {
						if (COLOR.equals(localname)) {
							((Overlay) fs).setColor(parseColor(stream
									.getElementText()));
						} else if (DRAW_ORDER.equals(localname)) {
							((Overlay) fs).setDrawOrder(Integer.parseInt(stream
									.getElementText()));
						} else if (ICON.equals(localname)) {
							((Overlay) fs).setIcon(handleTaggedData(localname));
						}
						if (ground) {
							if (LAT_LON_BOX.equals(localname)) {
								handleLatLonBox((GroundOverlay) fs, sl);
							} else if (ALTITUDE.equals(localname)) {
								String text = getNonEmptyElementText();
								if (text != null) {
									((GroundOverlay) fs).setAltitude(new Double(text));
								}
							} else if (ALTITUDE_MODE.equals(localname)) {
								((GroundOverlay) fs).setAltitudeMode(
                                        getNonEmptyElementText());
							}
						} else if (screen) {
							if (OVERLAY_XY.equals(localname)) {
								ScreenLocation val = handleScreenLocation(sl);
								((ScreenOverlay) fs).setOverlay(val);
							} else if (SCREEN_XY.equals(localname)) {
								ScreenLocation val = handleScreenLocation(sl);
								((ScreenOverlay) fs).setScreen(val);
							} else if (ROTATION_XY.equals(localname)) {
								ScreenLocation val = handleScreenLocation(sl);
								((ScreenOverlay) fs).setRotation(val);
							} else if (SIZE.equals(localname)) {
								ScreenLocation val = handleScreenLocation(sl);
								((ScreenOverlay) fs).setSize(val);
							} else if (ROTATION.equals(localname)) {
								String val = getNonEmptyElementText();
                                if (val != null)
                                    try {
                                        double rot = Double.parseDouble(val);
                                        if (Math.abs(rot) <= 180)
                                            ((ScreenOverlay) fs).setRotationAngle(rot);
                                        else
                                            log.warn("Invalid ScreenOverlay rotation value " + val);
                                    } catch (NumberFormatException nfe) {
                                        log.warn("Invalid ScreenOverlay rotation " + val + ": " + nfe);
                                    }
							}
						}
					} else if (network) {
						if (REFRESH_VISIBILITY.equals(localname)) {
							((NetworkLink) fs).setRefreshVisibility(isTrue(stream
									.getElementText()));
						} else if (FLY_TO_VIEW.equals(localname)) {
							((NetworkLink) fs).setFlyToView(isTrue(stream
									.getElementText()));
						} else if (LINK.equals(localname)) {
							((NetworkLink) fs).setLink(handleTaggedData(localname));
						} else if (URL.equals(localname)) {
							((NetworkLink) fs).setLink(handleTaggedData(localname));
						}
					}
				}
			} else if (foundEndTag(ee, se.getName().getLocalPart())) {
				break; // End of feature
			}
		}
		return readSaved();
	}

	/**
	 * Process the attributes from the start element to create a screen location
	 * 
	 * @param sl
	 *            the start element
	 * @return the location, never <code>null</code>.
	 */
	private ScreenLocation handleScreenLocation(StartElement sl) {
        try {
            ScreenLocation loc = new ScreenLocation();
            Attribute x = sl.getAttributeByName(new QName("x"));
            Attribute y = sl.getAttributeByName(new QName("y"));
            Attribute xunits = sl.getAttributeByName(new QName("xunits"));
            Attribute yunits = sl.getAttributeByName(new QName("yunits"));
            if (x != null) {
                loc.x = new Double(x.getValue());
            }
            if (y != null) {
                loc.y = new Double(y.getValue());
            }
            if (xunits != null) {
                String val = xunits.getValue();
                loc.xunit = ScreenLocation.UNIT.valueOf(val.toUpperCase());
            }
            if (yunits != null) {
                String val = yunits.getValue();
                loc.yunit = ScreenLocation.UNIT.valueOf(val.toUpperCase());
            }
            return loc;
        } catch (IllegalArgumentException iae) {
            log.error("Invalid screenLocation", iae);
            return null;
        }
    }

	/**
	 * Handle a set of elements with character values. The block has been found
	 * that starts with a &lt;localname&gt; tag, and it will end with a matching
	 * tag. All other elements found will be added to a greated map object.
	 * 
	 * @param localname
	 *            the localname, assumed not <code>null</code>.
	 * @return the map, null if no non-empty values are found
	 * @throws XMLStreamException
	 */
	private TaggedMap handleTaggedData(String localname)
			throws XMLStreamException {
		TaggedMap rval = new TaggedMap(localname);
		while (true) {
			XMLEvent event = stream.nextEvent();
			if (foundEndTag(event, localname)) {
				break;
			}
			if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement se = event.asStartElement();
				String sename = se.getName().getLocalPart();
				String value = getNonEmptyElementText();
                // ignore empty elements; e.g. <Icon><href /></Icon>
                if (value != null)
                    rval.put(sename, value);
			}
		}
		return rval.size() == 0 ? null : rval;
	}

	/**
	 * Handle a lat lon box with north, south, east and west elements
	 * 
	 * @param overlay
	 * @param sl
     * @throws javax.xml.stream.XMLStreamException
	 */
	private void handleLatLonBox(GroundOverlay overlay, StartElement sl)
			throws XMLStreamException {
		QName name = sl.getName();
		String localname = name.getLocalPart();
		while (true) {
			XMLEvent event = stream.nextEvent();
			if (foundEndTag(event, localname)) {
				break;
			}
			if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement se = event.asStartElement();
				String sename = se.getName().getLocalPart();
				String value = getNonEmptyElementText();
				if (value != null) {
                    try {
                        Double angle = Double.valueOf(value);
                        if (NORTH.equals(sename)) {
                            overlay.setNorth(angle);
                        } else if (SOUTH.equals(sename)) {
                            overlay.setSouth(angle);
                        } else if (EAST.equals(sename)) {
                            overlay.setEast(angle);
                        } else if (WEST.equals(sename)) {
                            overlay.setWest(angle);
                        } else if (ROTATION.equals(sename)) {
                            overlay.setRotation(angle);
                        }
                    } catch (NumberFormatException nfe) {
                        log.error("Invalid GroundOverlay angle " + value + " in " + sename);
                    } catch (IllegalArgumentException nfe) {
                        log.error("Invalid GroundOverlay value in " + sename + ": " + nfe);
                    }
                }
			}
		}
	}

	/**
	 * Parse and process the geometry for the feature and store in the feature
	 * 
	 * @param sl StartElement
     * @return Geometry associated with this element
     *          otherwise null if no valid Geometry can be constructed
     * @throws XMLStreamException if there is an error with the underlying XML
     * @throws IllegalStateException if geometry is invalid
     * @throws IllegalArgumentException if geometry is invalid
	 */
	@SuppressWarnings("unchecked")
	private Geometry handleGeometry(StartElement sl) throws XMLStreamException {
		QName name = sl.getName();
		String localname = name.getLocalPart();
		if (localname.equals(POINT)) {
			return parseCoordinate(localname);
		} else if (localname.equals(LINE_STRING)) {
			List<Point> coords = parseCoordinates(localname);
            if (coords.size() == 1) {
                Point pt = coords.get(0); 
                log.warn("line with single coordinate converted to point: " + pt);
                return pt;
            }
			else return new Line(coords);
		} else if (localname.equals(LINEAR_RING)) {
			List<Point> coords = parseCoordinates(localname);
            if (coords.size() == 1) {
                Point pt = coords.get(0);
                log.warn("ring with single coordinate converted to point: " + pt);
                return pt;
            } else if (coords.size() != 0 && coords.size() < 4) {
                log.warn("ring with " + coords.size()+ " coordinates converted to line: " + coords);
                return new Line(coords);
            }
			else return new LinearRing(coords);
		} else if (localname.equals(POLYGON)) {
			// Contains two linear rings
			LinearRing outer = null;
			List<LinearRing> inners = new ArrayList<LinearRing>();
			while (true) {
				XMLEvent event = stream.nextEvent();
				if (foundEndTag(event, localname)) {
					break;
				}
				if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
					StartElement se = event.asStartElement();
					String sename = se.getName().getLocalPart();
					if (sename.equals(OUTER_BOUNDARY_IS)) {
						List<Point> coords = parseCoordinates(sename);
                        if (coords.size() == 1) {
                            Point pt = coords.get(0);
                            log.warn("polygon with single coordinate converted to point: " + pt);
                            return pt;
                        } else if (coords.size() != 0 && coords.size() < 4) {
                            log.warn("polygon with " + coords.size()+ " coordinates converted to line: " + coords);
                            return new Line(coords);
                        }
						outer = new LinearRing(coords);
					} else if (sename.equals(INNER_BOUNDARY_IS)) {
						List<Point> coords = parseCoordinates(sename);
						inners.add(new LinearRing(coords));
					}
				}
			}
			if (outer == null) {
				throw new IllegalStateException("Bad poly found, no outer ring");
			}

			return new Polygon(outer, inners);
		} else if (localname.equals(MULTI_GEOMETRY)) {
			List<Geometry> geometries = new ArrayList<Geometry>();
			while (true) {
				XMLEvent event = stream.nextEvent();
				if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
					StartElement el = (StartElement) event;
					String tag = el.getName().getLocalPart();
					if (ms_geometries.contains(tag)) {
                        Geometry geom = handleGeometry(el);
                        if (geom != null) geometries.add(geom);
					}
				}
				if (foundEndTag(event, localname)) {
					break;
				}
			}
			boolean allpoints = true;
			for(Geometry geo : geometries) {
				if (!(geo instanceof Point)) {
					allpoints = false;
					break;
				}
			}                             
			if (allpoints) {
				return new MultiPoint((List) geometries);
			} else {
				return new GeometryBag(geometries);
			}
		} else if (localname.equals(MODEL)) {
			// we don't really have a way to represent this yet, look for end
			// element and continue
            Model model = new Model();
            while (true) {
				XMLEvent event = stream.nextEvent();
				if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
					StartElement se = event.asStartElement();
					String sename = se.getName().getLocalPart();
					if (sename.equals(LOCATION)) {
                        Geodetic2DPoint point = parseLocation(sename);
                        if (point != null)
                            model.setLocation(point);
                    } else if (sename.equals(ALTITUDE_MODE)) {
                        model.setAltitudeMode(getNonEmptyElementText());
                    }
                } else if (foundEndTag(event, localname)) {
					break;
				}                        
            }
            return model;
		}
		return null; // Default
	}

    private Geodetic2DPoint parseLocation(String localname) throws XMLStreamException {
        Latitude latitude = null;
        Longitude longitude = null;
        Double altitude = null;
        while (true) {
            XMLEvent event = stream.nextEvent();
            if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
                StartElement se = event.asStartElement();
                String name = se.getName().getLocalPart();
                if (name.equals(LATITUDE)) {
                    String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            latitude = new Latitude(Double.parseDouble(value), Angle.DEGREES);
                        } catch (IllegalArgumentException nfe) {
                            log.warn("Invalid latitude value: " + value);
                        }
                } else if (name.equals(LONGITUDE)) {
                    String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            longitude = new Longitude(Double.parseDouble(value), Angle.DEGREES);
                        } catch (IllegalArgumentException nfe) {
                            log.warn("Invalid longitude value: " + value);
                        }
                } else if (name.equals(ALTITUDE)) {
                    String value = getNonEmptyElementText();
                    if (value != null)
                        try {
                            altitude = Double.valueOf(value);
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid altitude value: " + value);
                        }
                }
            } else if (foundEndTag(event, localname)) {
                break;
            }
        }
        
        if (longitude == null && latitude == null) return null;
        if (longitude == null) longitude = new Longitude();
        else if (latitude == null) latitude = new Latitude();
        return altitude == null ? new Geodetic2DPoint(longitude, latitude)
                : new Geodetic3DPoint(longitude, latitude, altitude);
    }

    /**
	 * Find the coordinates element and extract the fractional lat/lons/alts
	 * into an array. The element name is used to spot if we leave the "end" of
	 * the block. The stream will be positioned after the element when this
	 * returns.
	 * 
	 * @param localname
	 *            the tag name of the containing element
	 * @return the coordinates
     * @throws XMLStreamException if there is an error with the underlying XML.
	 */
	private List<Point> parseCoordinates(String localname)
			throws XMLStreamException {
		List<Point> rval = null;
		while (true) {
			XMLEvent event = stream.nextEvent();
			if (foundEndTag(event, localname)) {
				break;
			}
			if (event.getEventType() == XMLStreamReader.START_ELEMENT &&
					COORDINATES.equals(event.asStartElement().getName().getLocalPart())) {
				String text = getNonEmptyElementText();
				if (text != null) rval = parseCoord(text);
				skipNextElement(stream, localname);
				break;
			}
		}
		return rval == null ? new ArrayList<Point>() : rval;
	}

	/**
	 * Find the coordinates element for Point and extract the fractional
	 * lat/lons/alts. The element name is used to spot if we leave the "end" of
	 * the block. The stream will be positioned after the element when this
	 * returns.
	 *
	 * @param localname
	 *            the tag name of the containing element
	 * @return the coordinate
	 * @throws XMLStreamException
	 */
	private Point parseCoordinate(String localname) throws XMLStreamException {
		Point rval = null;
		while (true) {
			XMLEvent event = stream.nextEvent();
			if (foundEndTag(event, localname)) {
				break;
			}
			if (event.getEventType() == XMLStreamReader.START_ELEMENT &&
					COORDINATES.equals(event.asStartElement().getName().getLocalPart())) {
				String text = getNonEmptyElementText();
				// allow sloppy KML with whitespace appearing before/after
				// lat and lon values; e.g. <coordinates>-121.9921875, 37.265625</coordinates>
				// http://kml-samples.googlecode.com/svn/trunk/kml/ListStyle/radio-folder-vis.kml
				if (text != null) rval = parsePointCoord(text);
				skipNextElement(stream, localname);
				break;
			}
		}
		return rval;
	}

	private static Point parsePointCoord(String coord) {
		List<Point> list = parseCoord(coord);
		return list.size() == 0 ? null : list.get(0);
	}

	/**
	 * Coordinate parser that matches the loose parsing of coordinates in Google Earth.
	 * KML reference states "Do not include spaces within a [coordinate] tuple" yet
	 * it still allows whitespace to appear anywhere in the input. 
	 * State machine-like parsing keeps track of what part of the coordinate
	 * had been found so far.
	 * Extra whitespace is allowed anywhere in the string.
	 * Invalid text in input is ignored.
	 *
	 * @param coord
	 * @return list of coordinates
	 */
	public static List<Point> parseCoord(String coord) {
		List<Point> list = new ArrayList<Point>();
		NumberStreamTokenizer st = new NumberStreamTokenizer(coord);
		st.ordinaryChar(',');
		boolean seenComma = false;
		int numparts = 0;
		double elev = 0;
		Longitude lon = null;
		Latitude lat = null;
		try {
			while (st.nextToken() != NumberStreamTokenizer.TT_EOF) {
				switch (st.ttype) {
					case NumberStreamTokenizer.TT_WORD:
						//s = "STRING:" + st.sval; // Already a String
						log.warn("ignore invalid string in coordinate: \"" + st.sval + "\"");
						//if (seenComma) System.out.println("\tXXX: WORD: seenComma");
						//if (numparts != 0) System.out.println("\tXXX: WORD: numparts=" + numparts);
						break;

					case NumberStreamTokenizer.TT_NUMBER:
						if (numparts == 3) {
							if (seenComma) {
								// skip over extra values
								//System.out.format("\tWarn: ignore extra values in coordinate: \"%f\"%n", st.nval);
								seenComma = false;
								continue;
							}
							// add last coord to list and reset counter
							if (lon != COORD_ERROR)
								list.add(new Point(new Geodetic3DPoint(lon, lat, elev)));
							//else System.out.println("\tERROR: drop bad coord");
							numparts = 0;
						}
						try {
							switch (++numparts) {
								case 1:
									if (seenComma) {
										lat = new Latitude(st.nval, Angle.DEGREES);
										lon = new Longitude(); // skipped longitude (use 0 degrees)
										numparts = 2;
									} else {
										// starting new coordinate
										lon = new Longitude(st.nval, Angle.DEGREES);
									}
									break;

								case 2:
									if (seenComma) {
										//System.out.println("lat=" + st.nval);
										lat = new Latitude(st.nval, Angle.DEGREES);
									} else {
										if (lon != COORD_ERROR)
											list.add(new Point(new Geodetic2DPoint(
													lon, new Latitude())));
										//else System.out.println("\tERROR: drop bad coord");
										// start new tuple
										lon = new Longitude(st.nval, Angle.DEGREES);
										numparts = 1;
									}
									break;

								case 3:
									if (seenComma) {
										elev = st.nval;
									} else {
										if (lon != COORD_ERROR)
											list.add(new Point(new Geodetic2DPoint(lon, lat)));
										//else System.out.println("\tERROR: drop bad coord");
										// start new tuple
										numparts = 1;
										lon = new Longitude(st.nval, Angle.DEGREES);
									}
									break;
							}

							//s = "NUM:" + Double.toString(st.nval);
							/*
							 double nval = st.nval;
							 if (st.nextToken() == StreamTokenizer.TT_WORD && expPattern.matcher(st.sval).matches()) {
								 s = "ENUM:" + Double.valueOf(Double.toString(nval) + st.sval).toString();
							 } else {
								 s = "NUM:" + Double.toString(nval);
								 st.pushBack();
							 }
							 */
						} catch (IllegalArgumentException e) {
							// bad lat/longitude; e.g. out of valid range
							log.error("Invalid coordinate: " + st.nval, e);
							if (numparts != 0) lon = COORD_ERROR;
						}
						seenComma = false; // reset flag
						break;

					default: // single character in ttype
						if (st.ttype == ',') {
							if (!seenComma) {
								// start of next coordinate component
								seenComma = true;
								if (numparts == 0) {
									//System.out.println("\tXXX: WARN: COMMA0: seenComma w/numparts=" + numparts);
									lon = new Longitude(); // skipped longitude (use 0 degrees)
									numparts = 1;
								}
							} else {
								switch (numparts) {
									case 0:
										//System.out.println("\tXXX: WARN: COMMA1: seenComma w/numparts=" + numparts);
										lon = new Longitude(); // skipped longitude (use 0 degrees)
										numparts = 1;
										break;
									case 1:
										//System.out.println("\tXXX: WARN: COMMA2: seenComma w/numparts=" + numparts);
										lat = new Latitude();  // skipped Latitude (use 0 degrees)
										numparts = 2;
										break;
									//default:
										//System.out.println("\tXXX: ** ERROR: COMMA3: seenComma w/numparts=" + numparts);
								}
							}
						} else
							log.warn("ignore invalid character in coordinate string: (" + (char) st.ttype + ")");
						//s = "CHAR:" + String.valueOf((char) st.ttype);
				}
				//System.out.println("\t" + s);
			} // while
		} catch (IOException e) {
			// we're using StringReader. this should never happen
			log.error("Failed to parse coord string: " + coord == null || coord.length() <= 20
                    ? coord : coord.substring(0,20) + "...", e);
		}

		// add last coord if valid
		if (numparts != 0 && lon != COORD_ERROR)
			switch (numparts) {
				case 1:
					list.add(new Point(new Geodetic2DPoint(lon, new Latitude())));
					break;
				case 2:
					list.add(new Point(new Geodetic2DPoint(lon, lat)));
					break;
				case 3:
					list.add(new Point(new Geodetic3DPoint(lon, lat, elev)));
			}

		return list;
	}

	/**
	 * @param e
	 * @return
	 */
	private IGISObject handleEndElement(XMLEvent e) {
		EndElement ee = e.asEndElement();
		String localname = ee.getName().getLocalPart();

		if (ms_containers.contains(localname)) {
			return new ContainerEnd();
		}

		return null;
	}

	private Double getDoubleElementValue(String localName) throws XMLStreamException {
		String elementText = stream.getElementText();
		if (elementText != null && StringUtils.isNotBlank(elementText))
			try {
				return Double.parseDouble(elementText);
			} catch (NumberFormatException nfe) {
				log.warn("Ignoring bad value for " + localName + ": " + nfe);
			}
		return null;
	}

    /**
     * Returns non-empty trimmed elementText from stream otherwise null
     * @return non-empty trimmed string, otherwise null
     * @throws XMLStreamException if the current event is not a START_ELEMENT
     * or if a non text element is encountered
     */
    private String getNonEmptyElementText() throws XMLStreamException {
        String elementText = stream.getElementText();
        if (elementText == null || elementText.length() == 0) return null;
        elementText = elementText.trim();
        return elementText.length() == 0 ? null : elementText;
    }

}

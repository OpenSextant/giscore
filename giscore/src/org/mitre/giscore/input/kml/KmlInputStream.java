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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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
import org.mitre.giscore.events.BaseStart;
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
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

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
 * {@link #handleProperties(BaseStart, XMLEvent, String)} method takes care of
 * these. This returns <code>false</code> if the current element isn't a common
 * element, which allows the caller to handle the code.
 * <p>
 * Geometry is handled by common code as well. All coordinates in KML are
 * transmitted as tuples of two or three elements. The formatting of these is
 * consistent and is handled by {@link #parseCoordinates(String)}.
 * 
 * @author DRAND
 * 
 */
public class KmlInputStream extends GISInputStreamBase implements IKml {
	private InputStream is = null;
	private XMLEventReader stream = null;
	private LinkedList<IGISObject> buffered = new LinkedList<IGISObject>();

	private static XMLInputFactory ms_fact;
	private static Set<String> ms_features = new HashSet<String>();
	private static Set<String> ms_containers = new HashSet<String>();
	private static Set<String> ms_attributes = new HashSet<String>();
	private static Set<String> ms_geometries = new HashSet<String>();
	
	private static List<DateFormat> ms_dateFormats = new ArrayList<DateFormat>(); 

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
		
		ms_dateFormats.add(ISO_DATE_FMT);
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd hh:mm"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd'T'hh"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd hh"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy-MM"));
		ms_dateFormats.add(new SimpleDateFormat("yyyy"));
		
	}

	/**
	 * Ctor
	 * 
	 * @param input
	 *            input stream for the kml file, never <code>null</code>
	 * @throws XMLStreamException
	 */
	public KmlInputStream(InputStream input) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input should never be null");
		}
		is = input;
		buffered.add(new DocumentStart(DocumentType.KML));
		try {
			stream = ms_fact.createXMLEventReader(is);
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.input.IGISInputStream#close()
	 */
	public void close() {
		IOUtils.closeQuietly(is);
	}

	/**
	 * Push an object back into the read queue
	 * 
	 * @param o
	 */
	public void pushback(IGISObject o) {
		buffered.addFirst(o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.input.IGISInputStream#read()
	 */
	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return super.readSaved();
		} else if (buffered.size() > 0) {
			return buffered.removeFirst();
		} else {
			try {
				while (true) {
					XMLEvent e = stream.nextEvent();
					if (e == null) {
						return null;
					}
					switch (e.getEventType()) {
					case XMLStreamReader.START_ELEMENT:
						return handleStartElement(e);
					case XMLStreamReader.END_ELEMENT:
						IGISObject rval = handleEndElement(e);
						if (rval != null)
							return rval;
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
		buffered.addFirst(cs);

		while (true) {
			XMLEvent ne = stream.peek();
			// Found end tag, sometimes a container has no other content
			if (foundEndTag(ne, containerTag)) {
				break;
			} else if (ne != null
					&& ne.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement nextel = ne.asStartElement();
				String tag = nextel.getName().getLocalPart();
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
				if (!handleProperties(cs, ee, localname)) {
					// Ignore other attributes
				}
			}
		}

		return buffered.removeFirst();
	}

	/**
	 * Handle the elements found in all features
	 * 
	 * @param feature
	 * @param ee
	 * @param localname
	 * @return <code>true</code> if the event has been handled
	 * @throws XMLStreamException
	 */
	private boolean handleProperties(BaseStart feature, XMLEvent ee,
			String localname) throws XMLStreamException {
		if (localname.equals(NAME)) {
			feature.setName(stream.getElementText());
			return true;
		} else if (localname.equals(DESCRIPTION)) {
			feature.setDescription(stream.getElementText());
			return true;
		} else if (localname.equals(STYLE)) {
			handleStyle(feature, ee);
			return true;
		} else if (ms_attributes.contains(localname)) {
			// Skip, but consume
			return true;
		} else if (localname.equals(SNIPPET)) {
			handleSnippet(feature, ee);
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
		} else if (localname.equals(METADATA)) {
			handleMetadata(feature, ee);
			return true;
		} else if (localname.equals(EXTENDED_DATA)) {
			handleExtendedData(feature, ee);
			return true;
		} else if (localname.equals(STYLE_URL)) {
			feature.setStyleUrl(stream.getElementText());
			return true;
		}
		return false;
	}

	/**
	 * @param cs
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleExtendedData(BaseStart cs, XMLEvent ee)
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
						cs.putData(new SimpleField(name.getValue()), value);
					}
				} else if (tag.equals(SCHEMA_DATA)) {
					Attribute url = se
							.getAttributeByName(new QName(SCHEMA_URL));
					if (url != null) {
						handleSchemaData(cs, url);
						try {
							cs.setSchema(new URI(url.getValue()));
						} catch (URISyntaxException e) {
							throw new XMLStreamException(e);
						}
					}
				}
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
		if (event == null || event.getEventType() == XMLEvent.END_ELEMENT) {
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
	 * @param cs
	 * @param url
	 * @throws XMLStreamException
	 */
	private void handleSchemaData(BaseStart cs, Attribute url)
			throws XMLStreamException {
		XMLEvent next;
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
						cs.putData(new SimpleField(name.getValue()), value);
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
	private void handleMetadata(BaseStart cs, XMLEvent ee)
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
	private void handleAbstractView(BaseStart cs, XMLEvent ee)
			throws XMLStreamException {
		StartElement se = ee.asStartElement();
		XMLEvent next;

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, se.getName().getLocalPart()))
				return;
		}
	}

	/**
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleStyleMap(BaseStart cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;
		StyleMap sm = new StyleMap();
		buffered.addFirst(sm);
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
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleTimePrimitive(BaseStart cs, XMLEvent ee)
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
				String time;
				try {
					if (foundStartTag(se, BEGIN) || foundStartTag(se, WHEN)) {
						time = stream.getElementText();
						cs.setStartTime(parseDate(time.trim()));
					} else if (foundStartTag(se, END)) {
						time = stream.getElementText();
						cs.setEndTime(parseDate(time.trim()));
					}
				} catch (ParseException e) {
					throw new XMLStreamException("Found bad time", e);
				}
			}
			if (foundEndTag(next, tag.getLocalPart())) {
				return;
			}
		}
	}

	/**
	 * Try all available formats to parse the passed string. The method will
	 * return if the parse succeeds, otherwise it remembers the last exception
	 * and throws that if we exhaust the list of formats.
	 * @param datestr
	 * @return
	 * @throws ParseException 
	 */
	private Date parseDate(String datestr) throws ParseException {
		ParseException e = null;
		for(DateFormat fmt : ms_dateFormats) {
			try {
				return fmt.parse(datestr);
			} catch(ParseException pe) {
				e = pe;
			}
		}
		throw e;
	}

	/**
	 * @param ee
	 * @throws XMLStreamException
	 */
	private void handleRegion(BaseStart cs, XMLEvent ee)
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
	private void handleSnippet(BaseStart cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;

		while (true) {
			next = stream.nextEvent();
			if (next == null)
				return;
			if (foundEndTag(next, SNIPPET)) {
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
	private void handleStyle(BaseStart cs, XMLEvent ee)
			throws XMLStreamException {
		XMLEvent next;

		Style style = new Style();
		StartElement sse = ee.asStartElement();
		Attribute id = sse.getAttributeByName(new QName(ID));
		if (id != null) {
			style.setId(id.getValue());
		}
		buffered.addFirst(style);
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
		Color color = Color.black;
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
					scale = Double.parseDouble(stream.getElementText());
				} else if (name.equals(COLOR)) {
					color = parseColor(stream.getElementText());
				}
			}
			if (foundEndTag(e, LABEL_STYLE)) {
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
		Color color = Color.black;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(WIDTH)) {
					width = Double.parseDouble(stream.getElementText());
				} else if (name.equals(COLOR)) {
					color = parseColor(stream.getElementText());
				}
			}
			if (foundEndTag(e, LINE_STYLE)) {
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
		String displayMode = "default";
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
					displayMode = stream.getElementText();
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
					href = stream.getElementText();
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
	 *            a hex encoded string, never <code>null</code> or empty and
	 *            must be exactly 8 characters long.
	 * @return the color value
	 */
	private Color parseColor(String cstr) {
		if (cstr == null || cstr.length() != 8) {
			return null;
		}
		int alpha = Integer.parseInt(cstr.substring(0, 2), 16);
		int blue = Integer.parseInt(cstr.substring(2, 4), 16);
		int green = Integer.parseInt(cstr.substring(4, 6), 16);
		int red = Integer.parseInt(cstr.substring(6, 8), 16);
		return new Color(red, green, blue, alpha);
	}

	/**
	 * @param style
	 * @throws XMLStreamException
	 */
	private void handleIconStyle(Style style) throws XMLStreamException {
		String url = null;
		double scale = 1.0;
		Color color = Color.black;
		while (true) {
			XMLEvent e = stream.nextEvent();
			if (e.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = e.asStartElement();
				String name = se.getName().getLocalPart();
				if (name.equals(SCALE)) {
					scale = Double.parseDouble(stream.getElementText());
				} else if (name.equals(COLOR)) {
					color = parseColor(stream.getElementText());
				} else if (name.equals(ICON)) {
					url = parseIconHref();
				}
			}
			if (foundEndTag(e, ICON_STYLE)) {
				style.setIconStyle(color, scale, url);
				return;
			}
		}
	}

	/**
	 * @param e
	 * @return
	 */
	private IGISObject handleStartElement(XMLEvent e) {
		StartElement se = e.asStartElement();
		String localname = se.getName().getLocalPart();

		try {
			if (ms_features.contains(localname)) {
				return handleFeature(e);
			} else if (SCHEMA.equals(localname)) {
				return handleSchema(se, localname);
			} else if (ms_containers.contains(localname)) {
				return handleContainer(se);
			} else {
				// Look for next start element and recurse
				e = stream.nextTag();
				if (e != null && e.getEventType() == XMLEvent.START_ELEMENT) {
					return handleStartElement(e);
				}
			}
		} catch (XMLStreamException e1) {
			throw new RuntimeException(e1);
		}

		return null;
	}

	/**
	 * @param element
	 * @param localname
	 * @return
	 * @throws XMLStreamException
	 */
	private IGISObject handleSchema(StartElement element, String localname)
			throws XMLStreamException {
		XMLEvent next;
		Schema s = new Schema();
		buffered.add(s);
		Attribute id = element.getAttributeByName(new QName(ID));
		Attribute name = element.getAttributeByName(new QName(NAME));
		if (id != null)
			s.setId(id.getValue());
		try {
			if (name != null)
				s.setName(new URI(name.getValue()));
		} catch (URISyntaxException e) {
			throw new XMLStreamException(e);
		}

		int gen = 0;
		while (true) {
			next = stream.nextEvent();
			if (next == null)
				break;
			if (next.getEventType() == XMLEvent.START_ELEMENT) {
				StartElement se = next.asStartElement();
				if (foundStartTag(se, SIMPLE_FIELD)) {
					Attribute fname = se.getAttributeByName(new QName(NAME));
					String fieldname = null;
					if (fname != null) {
						fieldname = fname.getValue();
					} else {
						fieldname = "gen" + gen++;
					}
					SimpleField field = new SimpleField(fieldname);
					s.put(fieldname, field);
					Attribute type = se.getAttributeByName(new QName(TYPE));
					
					if (type != null) {
						SimpleField.Type ttype = SimpleField.Type.valueOf(type
								.getValue().toUpperCase());
						field.setType(ttype);
					}
					
					String displayName = parseDisplayName(SIMPLE_FIELD);
					field.setDisplayName(displayName);
				}
			} else if (foundEndTag(next, SCHEMA)) {
				break;
			}
		}
		return buffered.removeFirst();
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
					rval = stream.getElementText();
				}
			} else if (foundEndTag(ee, tag)) {
				break;
			}
		}
		return rval;
	}

	/**
	 * @param e
	 * @return
	 * @throws XMLStreamException
	 */
	private IGISObject handleFeature(XMLEvent e) throws XMLStreamException {
		StartElement se = e.asStartElement();
		String type = se.getName().getLocalPart();
		boolean placemark = PLACEMARK.equals(type);
		boolean screen = SCREEN_OVERLAY.equals(type);
		boolean photo = PHOTO_OVERLAY.equals(type);
		boolean ground = GROUND_OVERLAY.equals(type);
		boolean network = NETWORK_LINK.equals(type);
		boolean isOverlay = screen || photo || ground;
		Feature fs = null;
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
			throw new RuntimeException("Found new unhandled feature type "
					+ type);
		}

		buffered.addFirst(fs);
		while (true) {
			XMLEvent ee = stream.nextEvent();
			if (ee == null) {
				break;
			}
			if (ee.getEventType() == XMLStreamReader.START_ELEMENT) {
				StartElement sl = ee.asStartElement();
				QName name = sl.getName();
				String localname = name.getLocalPart();
				if (!handleProperties(fs, ee, localname)) {
					// Deal with specific feature elements
					if (ms_geometries.contains(localname)) {
						Geometry geo = handleGeometry(sl);
						if (geo != null) {
							fs.setGeometry(geo);
						}
					} else if (ground && LAT_LON_BOX.equals(localname)) {
						handleLatLonBox((GroundOverlay) fs, sl);
					} else if (screen && OVERLAY_XY.equals(localname)) {
						ScreenLocation val = handleScreenLocation(sl);
						((ScreenOverlay) fs).setOverlay(val);
					} else if (screen && SCREEN_XY.equals(localname)) {
						ScreenLocation val = handleScreenLocation(sl);
						((ScreenOverlay) fs).setScreen(val);
					} else if (screen && ROTATION_XY.equals(localname)) {
						ScreenLocation val = handleScreenLocation(sl);
						((ScreenOverlay) fs).setRotation(val);
					} else if (screen && SIZE.equals(localname)) {
						ScreenLocation val = handleScreenLocation(sl);
						((ScreenOverlay) fs).setSize(val);
					} else if (screen && ROTATION.equals(localname)) {
						Double rot = new Double(stream.getElementText());
						((ScreenOverlay) fs).setRotationAngle(rot);
					} else if (isOverlay && COLOR.equals(localname)) {
						((Overlay) fs).setColor(parseColor(stream
								.getElementText()));
					} else if (isOverlay && DRAW_ORDER.equals(localname)) {
						((Overlay) fs).setDrawOrder(Integer.parseInt(stream
								.getElementText()));
					} else if (isOverlay && ICON.equals(localname)) {
						((Overlay) fs).setIcon(handleTaggedData(localname));
					} else if (ground && ALTITUDE.equals(localname)) {
						String text = stream.getElementText();
						if (StringUtils.isNotBlank(text)) {
							((GroundOverlay) fs).setAltitude(new Double(text));
						}
					} else if (ground && ALTITUDE_MODE.equals(localname)) {
						((GroundOverlay) fs).setAltitudeMode(stream
								.getElementText());
					} else if (network && REFRESH_VISIBILITY.equals(localname)) {
						((NetworkLink) fs).setRefreshVisibility(isTrue(stream
								.getElementText()));
					} else if (network && FLY_TO_VIEW.equals(localname)) {
						((NetworkLink) fs).setFlyToView(isTrue(stream
								.getElementText()));
					} else if (network && LINK.equals(localname)) {
						((NetworkLink) fs).setLink(handleTaggedData(localname));
					}
				}
			} else if (foundEndTag(ee, se.getName().getLocalPart())) {
				break; // End of feature
			}
		}
		return buffered.removeFirst();
	}

	/**
	 * Process the attributes from the start element to create a screen location
	 * 
	 * @param sl
	 *            the start element
	 * @return the location, never <code>null</code>.
	 */
	private ScreenLocation handleScreenLocation(StartElement sl) {
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
	}

	/**
	 * Handle a set of elements with character values. The block has been found
	 * that starts with a &lt;localname&gt; tag, and it will end with a matching
	 * tag. All other elements found will be added to a greated map object.
	 * 
	 * @param localname
	 *            the localname, assumed not <code>null</code>.
	 * @return the map, never <code>null</code>
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
				String value = stream.getElementText();
				rval.put(sename, value);
			}
		}
		return rval;
	}

	/**
	 * Handle a lat lon box with north, south, east and west elements
	 * 
	 * @param overlay
	 * @param sl
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
				String value = stream.getElementText();
				if (StringUtils.isNotBlank(value)) {
					Double angle = new Double(value.trim());
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
				}
			}
		}
	}

	/**
	 * Parse and process the geometry for the feature and store in the feature
	 * 
	 * @param sl
	 * @throws XMLStreamException
	 */
	private Geometry handleGeometry(StartElement sl) throws XMLStreamException {
		QName name = sl.getName();
		String localname = name.getLocalPart();
		if (localname.equals(POINT)) {
			return parseCoordinate(localname);
		} else if (localname.equals(LINE_STRING)) {
			List<Point> coords = parseCoordinates(localname);
			Line line = new Line(coords);
			return line;
		} else if (localname.equals(LINEAR_RING)) {
			ArrayList<Point> coords = parseCoordinates(localname);
			LinearRing ring = new LinearRing(coords);
			return ring;
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
						geometries.add(handleGeometry(el));
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
			while (true) {
				XMLEvent event = stream.nextEvent();
				if (foundEndTag(event, localname)) {
					break;
				}
			}
		}
		return null; // Default
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
	 * @throws XMLStreamException
	 */
	private ArrayList<Point> parseCoordinates(String localname)
			throws XMLStreamException {
		ArrayList<Point> rval = new ArrayList<Point>();
		while (true) {
			XMLEvent event = stream.nextEvent();
			if (foundEndTag(event, localname)) {
				break;
			}
			if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
				if (event.asStartElement().getName().getLocalPart().equals(
						COORDINATES)) {
					String text = stream.getElementText().trim();
					String tuples[] = text.split("\\s");
					for (int i = 0; i < tuples.length; i++) {
						if (!StringUtils.isEmpty(tuples[i])) {
							String parts[] = tuples[i].trim().split(",");
							double dlon = Double.parseDouble(parts[0]);
							double dlat = Double.parseDouble(parts[1]);
							Longitude lon = new Longitude(dlon, Angle.DEGREES);
							Latitude lat = new Latitude(dlat, Angle.DEGREES);
							if (parts.length < 3) {
								rval.add(new Point(
										new Geodetic2DPoint(lon, lat)));
							} else {
								double elev = Double.parseDouble(parts[2]);
								rval.add(new Point(new Geodetic3DPoint(lon,
										lat, elev)));
							}
						}
					}
				}
			}
		}
		return rval;
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
			if (event.getEventType() == XMLStreamReader.START_ELEMENT) {
				if (event.asStartElement().getName().getLocalPart().equals(
						COORDINATES)) {
					String text = stream.getElementText().trim();
					// allow sloppy KML with whitespace appearing before/after
					// lat and lon values
					// e.g. <coordinates>-121.9921875, 37.265625</coordinates>
					// http://kml-samples.googlecode.com/svn/trunk/kml/ListStyle/radio-folder-vis.kml
					if (!StringUtils.isEmpty(text)) {
						String parts[] = text.split(",");
						if (parts.length > 1) {
							double dlon = Double.parseDouble(parts[0]);
							double dlat = Double.parseDouble(parts[1]);
							Longitude lon = new Longitude(dlon, Angle.DEGREES);
							Latitude lat = new Latitude(dlat, Angle.DEGREES);
							if (parts.length < 3) {
								rval = new Point(new Geodetic2DPoint(lon, lat));
							} else {
								double elev = Double.parseDouble(parts[2]);
								rval = new Point(new Geodetic3DPoint(lon, lat,
										elev));
							}
						}
					}
				}
			}
		}
		return rval;
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

}

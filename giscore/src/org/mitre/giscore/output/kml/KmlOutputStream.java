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
import java.util.*;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.*;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.giscore.output.atom.IAtomConstants;
import org.mitre.giscore.utils.SafeDateFormat;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KML output stream creates a result KML file using the given output
 * stream. It uses STaX methods for writing the XML elements to avoid building
 * an in-memory DOM, which reduces the memory overhead of creating the document.
 * <p/>
 * KmlOutputStream produces a valid KML Document wrt the KML 2.2 specification.
 * <p/>
 * For KML, each incoming element generally adds another full element to the
 * output document. There are a couple of distinct exceptions. These are the
 * Style selectors. The style selectors instead appear before the matched
 * feature, and the KML output stream buffers these until the next feature is
 * seen. At that point the styles are output after the element's attributes and
 * before any content.
 * <p/>
 * The geometry visitors are invoked by the feature vistor via the Geometry
 * accept method.
 * <p/>
 * Elements such as atom:author, atom:link, xal:AddressDetails, and gx: extensions
 * must be added to the Feature object as {@link Element} objects.
 *
 * <h4>Notes/Limitations:</h4>
 *  -A few tags are not yet supported on features so are omitted from output:
 *  {@code address, ListStyle, Metadata, open, phoneNumber, Snippet, and snippet}.<br/>
 * -Warns if shared styles appear in Folders. According to OGC KML specification
 *  shared styles shall only appear within a Document [OGC 07-147r2 section 6.4].
 *
 * @author DRAND
 * @author J.Mathews
 */
public class KmlOutputStream extends XmlOutputStreamBase implements IKml {
	private static final Logger log = LoggerFactory.getLogger(KmlOutputStream.class);

    private final List<IGISObject> waitingElements = new ArrayList<IGISObject>();

    private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private transient SafeDateFormat dateFormatter;

    /**
     * prefix associated with gx extension namespace if such namespace is provided
     * in root Document declarations
     */
    private Namespace gxNamespace;

    /**
     * Ctor
     *
     * @param stream OutputStream to decorate as a KmlOutputStream
     * @param encoding the encoding to use, if null default encoding (UTF-8) is assumed
     * @throws XMLStreamException if error occurs creating output stream
     */
    public KmlOutputStream(OutputStream stream, String encoding) throws XMLStreamException {
        super(stream, encoding);
        if (StringUtils.isBlank(encoding))
            writer.writeStartDocument();
        else
            writer.writeStartDocument(encoding, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement(KML);
        writer.writeDefaultNamespace(KML_NS);
    }

    /**
     * Ctor
     *
     * @param stream OutputStream to decorate as a KmlOutputStream
     * @throws XMLStreamException if error occurs creating output stream
     */
    public KmlOutputStream(OutputStream stream) throws XMLStreamException {
        this(stream, null);
    }

    /**
     * Close this writer and free any resources associated with the
     * writer.  This also closes the underlying output stream.
     *
     * @throws IOException if an error occurs
     */
    @Override
    public void close() throws IOException {
        try {
            if (writerOpen) {
                writer.writeEndElement();
                writer.writeCharacters("\n");
                writer.writeEndDocument();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            super.close();            
        }
    }

    /**
     * Flush and close XMLStreamWriter but not the outputStream
     *
     * @throws IOException if an error occurs
     */
    public void closeWriter() throws IOException {
        if (writerOpen)
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
                throw new IOException(e);
            } finally {
                writerOpen = false;
            }
    }

    /**
     * Closes the underlying stream typically done after calling closeWriter
     */
    public void closeStream()  {
        IOUtils.closeQuietly(stream);
    }

    /**
     * Visit a DocumentStart object
     *
     * @param documentStart
     * @throws RuntimeException if there is an error with the underlying XML
     */
	@Override
	public void visit(DocumentStart documentStart) {
		try {
            boolean needNewline = false;
			// Add any additional namespaces to the most proximate containing element
			for(Namespace ns : documentStart.getNamespaces()) {
                String prefix = ns.getPrefix();
                if (StringUtils.isNotBlank(prefix)) {
                    writer.writeNamespace(prefix, ns.getURI());
                    needNewline = true;
                    namespaces.put(prefix, ns.getURI());
                    if (gxNamespace == null && ns.getURI() != null && ns.getURI().startsWith(NS_GOOGLE_KML_EXT_PREFIX)) {
                        gxNamespace = ns;
                    }
                }
			}
            if (needNewline) writer.writeCharacters("\n");
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * Visit a ContainerEnd object
     *
     * @param containerEnd
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(ContainerEnd containerEnd) {
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Visit a ContainerStart object
     *
     * @param containerStart
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(ContainerStart containerStart) {
        try {
            String tag = containerStart.getType();
            if (!IKml.DOCUMENT.equals(tag) && !IKml.FOLDER.equals(tag)) {
                // Folder has more restrictions than Document in KML (e.g. shared styles cannot appear in Folders)
                // so if container is unknown then use Document type.
                tag = IKml.FOLDER.equalsIgnoreCase(tag) ? IKml.FOLDER : IKml.DOCUMENT;
            }
            writer.writeStartElement(tag);
            List<Element> elements = handleAttributes(containerStart, tag);
            for(Element el : elements) {
                if (el.getNamespaceURI().startsWith(NS_GOOGLE_KML_EXT_PREFIX))
            	    handleXmlElement(el);
                else {
                    // what non-kml namespaces can we support without creating invalid KML other than gx: and atom: ??
                    // suppress atom:attributes in post-xml element dump
                    // atoms handled in handleAttributes
                    log.debug("Handle XML element " + el.getName() + " as comment");
                    writeAsComment(el);
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

	 private void handleRegion(TaggedMap region) {
		 if (region != null && !region.isEmpty()) {
			 try {
				 // postpone writing out LAT_LON_BOX element until there is a child element
				 // likewise don't write Region element unless we have LAT_LON_BOX or Lod
				 List<String> waitingList = new java.util.LinkedList<String>();
				 waitingList.add(REGION);
				 waitingList.add(LAT_LON_ALT_BOX);
				 handleTaggedElement(NORTH, region, waitingList);
				 handleTaggedElement(SOUTH, region, waitingList);
				 handleTaggedElement(EAST, region, waitingList);
				 handleTaggedElement(WEST, region, waitingList);
				 handleTaggedElement(MIN_ALTITUDE, region, waitingList);
				 handleTaggedElement(MAX_ALTITUDE, region, waitingList);
				 // if altitudeMode is invalid then it will be omitted
				 AltitudeModeEnumType altMode = AltitudeModeEnumType.getNormalizedMode(region.get(ALTITUDE_MODE));
				 if (altMode != null) {
					 /*
					 if (!waitingList.isEmpty()) {
					 	writer.writeStartElement(REGION);
						writer.writeStartElement(LAT_LON_ALT_BOX);
						waitingList.clear();
					 }
					 handleAltitudeMode(altMode);
					 */
					 if (waitingList.isEmpty()) {
					 	handleAltitudeMode(altMode);
					 }
					 // otherwise don't have LatLonAltBox so AltitudeMode has no meaning
				 }
				 if (waitingList.isEmpty()) {
					 writer.writeEndElement(); // end LatLonAltBox
				 } else {
					 waitingList.remove(1); // remove LatLonAltBox from consideration
					 // we still have Region in waiting list
				 }
				 // next check Lod
				 waitingList.add(LOD);
				 handleTaggedElement(MIN_LOD_PIXELS, region, waitingList);
				 handleTaggedElement(MAX_LOD_PIXELS, region, waitingList);
				 handleTaggedElement(MIN_FADE_EXTENT, region, waitingList);
				 handleTaggedElement(MAX_FADE_EXTENT, region, waitingList);
				 if (waitingList.isEmpty())
					 writer.writeEndElement(); // end Lod
				 //if (!waitingList.isEmpty()) System.out.println("XXX: *NO* LOD in region..."); // debug
				 //else System.out.println("XXX: got LOD in region..."); // debug
				 // if have 2 elements in map then have neither Lod nor Region to end
				 // if have 0 or 1 {Lod} elements in list then we need to end of Region
				 if (waitingList.size() < 2)
					 writer.writeEndElement(); // end Region
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		 }
	 }

	private void handleAbstractView(TaggedMap viewGroup) {
        if (viewGroup != null && !viewGroup.isEmpty()) {
			String tag = viewGroup.getTag();
			if (!CAMERA.equals(tag) && !LOOK_AT.equals(tag)) {
				log.error("Invalid AbstractView type: " + viewGroup);
				return;
			}
			try {
				writer.writeStartElement(tag); // LookAt or Camera
				handleTaggedElement(LONGITUDE, viewGroup);
				handleTaggedElement(LATITUDE, viewGroup);
				handleTaggedElement(ALTITUDE, viewGroup);
				handleTaggedElement(HEADING, viewGroup);
				handleTaggedElement(TILT, viewGroup);
				handleTaggedElement(RANGE, viewGroup);
				// if altitudeMode is invalid then it will be omitted
				AltitudeModeEnumType altMode = AltitudeModeEnumType.getNormalizedMode(viewGroup.get(ALTITUDE_MODE));
				handleAltitudeMode(altMode);
				writer.writeEndElement();
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
        }
    }

	// Thread-safe date formatter helper method
    private SafeDateFormat getDateFormatter() {
        if (dateFormatter == null) {
            dateFormatter = new SafeDateFormat(ISO_DATE_FMT);
        }
        return dateFormatter;
    }

    /**
     * Common code for outputting feature data that is held for both containers
     * and other features like Placemarks and Overlays.
     *
     * @param feature Common feature object for whom attributes will be written
     * @param containerType type of Container were visiting if (Feature is a Document or Folder) otherwise null
     * @return list of elements initialized awith getElement() and removed those elements that were processed, empty list
     *          if no non-kml elements left.
     */
    private List<Element> handleAttributes(Common feature, String containerType) {
        try {
            List<Element> elements = feature.getElements().isEmpty() ?
                    Collections.<Element>emptyList() : new LinkedList<Element>(feature.getElements());

            String id = feature.getId();
            if (id != null) writer.writeAttribute(ID, id);
            handleNonNullSimpleElement(NAME, feature.getName());
            Boolean visibility = feature.getVisibility();
            if (visibility != null && !visibility)
                handleSimpleElement(VISIBILITY, "0"); // default=1

            // handle atom attributes if defined and remove from list
            Element author = null;
            Element link = null;
            Element addressDetails = null;
            for(Iterator<Element>it = elements.iterator(); it.hasNext(); ) {
                Element el = it.next();
                // remove atom:attributes in post-xml element dump
                if (IAtomConstants.ATOM_URI_NS.equals(el.getNamespaceURI())) {
                    if ("author".equals(el.getName())) {
                        author = el;
                        it.remove(); // remove from list - marked as processed
                    } else if ("link".equals(el.getName())) {
                        link = el;
                        it.remove(); // remove from list - marked as processed
                    }
                } else if (NS_OASIS_XAL.equals(el.getNamespaceURI()) &&
                        ADDRESS_DETAILS.equals(el.getName())) {
                    addressDetails = el;
                    it.remove(); // remove from list - marked as processed
                }
            }
            if (author != null) handleXmlElement(author);
            if (link != null) handleXmlElement(link);
            // todo: handle kml:address
            if (addressDetails != null) handleXmlElement(addressDetails);
            // todo: handle kml:Snippet
            handleNonNullSimpleElement(DESCRIPTION, feature.getDescription());
            handleAbstractView(feature.getViewGroup()); // LookAt or Camera AbstractViewGroup
            Date startTime = feature.getStartTime();
            Date endTime = feature.getEndTime();
            if (startTime != null) {
                if (endTime == null) {
                    // start time with no end time
                    writer.writeStartElement(TIME_SPAN);
                    handleSimpleElement(BEGIN, formatDate(startTime));
                } else if (endTime.equals(startTime)) {
                    // start == end represents a Timestamp
                    // note that having feature with a timeSpan with same begin and end time
                    // is identical to one with a timestamp of same time in Google Earth client.
                    writer.writeStartElement(TIME_STAMP);
                    handleSimpleElement(WHEN, formatDate(startTime));
                } else {
                    // start != end represents a TimeSpan
                    writer.writeStartElement(TIME_SPAN);
                    handleSimpleElement(BEGIN, formatDate(startTime));
                    handleSimpleElement(END, formatDate(endTime));
                }
                writer.writeEndElement();
            } else if (endTime != null) {
                // end time with no start time
                writer.writeStartElement(TIME_SPAN);
                handleSimpleElement(END, formatDate(endTime));
                writer.writeEndElement();
            }

            handleNonNullSimpleElement(STYLE_URL, feature.getStyleUrl());
            // if feature has inline style needs to write here
            handleWaitingElements(containerType);

			handleRegion(feature.getRegion());
            handleExtendedData(feature);

            return elements;
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleExtendedData(Row feature) throws XMLStreamException {
        if (feature.hasExtendedData()) {
            URI schema = feature.getSchema();
            writer.writeStartElement(EXTENDED_DATA);
            if (schema == null) {
                for (SimpleField field : feature.getFields()) {
                    Object value = feature.getData(field);
                    if (value != null) {
                        writer.writeStartElement(DATA);
                        writer.writeAttribute(NAME, field.getName());
                        handleSimpleElement(VALUE, formatValue(field.getType(),
                                value));
                        writer.writeEndElement();
                    }
                }
            } else {
                writer.writeStartElement(SCHEMA_DATA);
                writer.writeAttribute(SCHEMA_URL, schema.toString());
                for (SimpleField field : feature.getFields()) {
                    Object value = feature.getData(field);
                    if (value != null) {
                        writer.writeStartElement(SIMPLE_DATA);
                        writer.writeAttribute(NAME, field.getName());
                        handleCharacters(formatValue(field.getType(),
                                value));
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }


    /**
     * Format date in ISO format and trim milliseconds field if 0
     * @param date
     * @return formatted date (e.g. 2003-09-30T00:00:06.930Z)
     */
    private String formatDate(Date date) {
        String d = getDateFormatter().format(date);
        if (d.endsWith(".000Z")) {
            // trim milliseconds field
            d = d.substring(0, d.length() - 5) + "Z";
        }
        return d;
    }

    /**
     * Format a value according to the type, defaults to using toString.
     *
     * @param type the type, assumed not <code>null</code>
     * @param data the data, may be a number of types, but must be coercible to
     *             the given type
     * @return a formatted value
     * @throws IllegalArgumentException if values cannot be formatted
     *                                  using specified data type.
     */
    private String formatValue(Type type, Object data) {
        if (data == null) {
            return "";
        } else if (Type.DATE.equals(type)) {
            Object val = data;
            if (val instanceof String) {
                try {
                    // Try converting to ISO?
                    val = KmlInputStream.parseDate((String) data);
                } catch (ParseException e) {
                    // Fall through
                } catch (RuntimeException e) {
                    // Fall through
                }
            }
            if (val instanceof Date) {
                return formatDate((Date) val);
            } else {
                return val.toString();
            }
        } else if (Type.DOUBLE.equals(type) || Type.FLOAT.equals(type)) {
            if (data instanceof String) {
                return (String) data;
            }

            if (data instanceof Number) {
                return String.valueOf(data);
            } else {
                throw new IllegalArgumentException("Data that cannot be coerced to float: " + data);
            }
        } else if (Type.INT.equals(type) || Type.SHORT.equals(type)
                || Type.UINT.equals(type) || Type.USHORT.equals(type)) {
            if (data instanceof String) {
                return (String) data;
            }

            if (data instanceof Number) {
                return String.valueOf(data);
            } else {
                throw new IllegalArgumentException("Data that cannot be coerced to int: " + data);
            }
        } else {
            return data.toString();
        }
    }

    /**
     * Visit a Feature including its geometry and any child XML Elements.
     *
     * @param feature
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(Feature feature) {
        try {
            String tag = feature.getType();
            writer.writeStartElement(tag);
            List<Element> elements = handleAttributes(feature, tag);
            if (feature instanceof Overlay) {
                handleOverlay((Overlay) feature);
            } else if (feature.getGeometry() != null) {
                feature.getGeometry().accept(this);
            } else if (feature instanceof NetworkLink) {
                handleNetworkLink((NetworkLink) feature);
            }
            for(Element el : elements) {
                if (el.getNamespaceURI() != null && el.getNamespaceURI().startsWith(NS_GOOGLE_KML_EXT_PREFIX))
            	    handleXmlElement(el);
                else {
                    // what non-kml namespaces can we support without creating invalid KML other than gx: and atom: ??
                    // suppress atom:attributes in post-xml element dump
                    // atoms handled in handleAttributes
                    log.debug("Handle XML element " + el.getName() + " as comment");
                    writeAsComment(el);
                }
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Visit a row. Output as a Placemark with ExtendedData without geometry
     * @param row Row to visit
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(Row row) {
        if (row != null && row.hasExtendedData()) {
            try {
                writer.writeStartElement(PLACEMARK);
                handleExtendedData(row);
                writer.writeEndElement();
                writer.writeCharacters("\n");
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Visit an XML Element.
     *
     * @param element Element to visit, never null
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(Element element) {
        try {
            if (gxNamespace != null && gxNamespace.getPrefix().equals(element.getPrefix())
                    || element.getNamespaceURI().startsWith(NS_GOOGLE_KML_EXT_PREFIX)) {
                handleXmlElement(element);
            } else {
                // REVIEW: handle non-kml element as comment for now .. any other namespaces to support??
                if (log.isDebugEnabled()) {
                    String prefix = element.getPrefix();
                    String name = element.getName();
                    if (StringUtils.isNotEmpty(prefix)) name += " " + element.getNamespace();
                    log.debug("Handle XML element " + name  + " as comment");
                }
                writeAsComment(element);
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

	/**
     * Handle elements specific to a network link feature.
     *
     * @param link NetworkLink to be handled
	 * @throws XMLStreamException if there is an error with the underlying XML.
     */
    private void handleNetworkLink(NetworkLink link) throws XMLStreamException {
        handleLinkElement(LINK, link.getLink());
    }

    /**
     * Handle elements specific to an overlay feature
     *
     * @param overlay Overlay to be handled
     * @throws XMLStreamException if an error occurs
     */
    private void handleOverlay(Overlay overlay) throws XMLStreamException {
        handleColor(COLOR, overlay.getColor());
        int order = overlay.getDrawOrder();
        // don't bother to output drawOrder element if is the default value (0)
        if (order != 0) handleSimpleElement(DRAW_ORDER, Integer.toString(order));
        handleLinkElement(ICON, overlay.getIcon());

        if (overlay instanceof GroundOverlay) {
            GroundOverlay go = (GroundOverlay) overlay;
            handleNonNullSimpleElement(ALTITUDE, go.getAltitude());
			// if null or default clampToGround then ignore
			handleAltitudeMode(go.getAltitudeMode());
            // postpone writing out LAT_LON_BOX element until there is a child element
            Queue<String> waitingList = new java.util.LinkedList<String>();
            waitingList.add(LAT_LON_BOX);
            handleNonNullSimpleElement(NORTH, go.getNorth(), waitingList);
            handleNonNullSimpleElement(SOUTH, go.getSouth(), waitingList);
            handleNonNullSimpleElement(EAST, go.getEast(), waitingList);
            handleNonNullSimpleElement(WEST, go.getWest(), waitingList);
            handleNonNullSimpleElement(ROTATION, go.getRotation(), waitingList);
            if (waitingList.isEmpty()) writer.writeEndElement();
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

    private void handleNonNullSimpleElement(String tag, Object content, Queue<String> waitingList) throws XMLStreamException {
        if (content != null) {
            if (waitingList != null && !waitingList.isEmpty())
                writer.writeStartElement(waitingList.remove());
            handleSimpleElement(tag, content);
        }
    }

    // elements associated with Kml22 LinkType in sequence order for Icon, Link, and Url elements
    private static final String[] LINK_TYPE_TAGS = {
            HREF,
            REFRESH_MODE,
            REFRESH_INTERVAL,
            VIEW_REFRESH_MODE,
            VIEW_REFRESH_TIME,
            VIEW_BOUND_SCALE,
            VIEW_FORMAT,
            HTTP_QUERY
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
     * @param tag String tag
     * @param loc ScreenLocation of tag
     * @throws XMLStreamException if there is an error with the underlying XML
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
        // Note: this writes elements in hash order which DOES NOT match order in KML XML schema
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
     * @param containerType type of Container were visiting if (Feature is a Document or Folder) otherwise null
     * @throws XMLStreamException if there is an error with the underlying XML
     * @throws IllegalStateException if invalid element is found in waitingElements list
     */
    private void handleWaitingElements(String containerType) throws XMLStreamException {
        for (int i = waitingElements.size() - 1; i >= 0; i--) {
            IGISObject element = waitingElements.get(i);
			if (element instanceof StyleSelector) {
				StyleSelector style = (StyleSelector)element;
				if (containerType != null && StringUtils.isNotBlank(style.getId()) && FOLDER.equals(containerType)) {
					// http://code.google.com/apis/kml/documentation/kmlreference.html#document
					// see definition of Shared Styles in OGC KML specification [OGC 07-147r2 section 6.4]
					// shared styles should only appear in Documents
					log.warn("Do not put shared styles within a Folder. Fails OGC constraint.");
				}
				if (element instanceof Style) {
					handle((Style) element);
				} else if (element instanceof StyleMap) {
					handle((StyleMap) element);
				}
			}
			else {
                throw new IllegalStateException("Unknown kind of deferred element: "
                        + element.getClass());
            }
        }
        waitingElements.clear();
    }

    /**
     * Handle the output of a polygon
     *
     * @param poly the polygon, never <code>null</code>
     */
    @Override
    public void visit(Polygon poly) {
        if (poly != null)
            try {
                writer.writeStartElement(POLYGON);
                handleGeometryAttributes(poly);
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
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle the output of a ring
     *
     * @param r the ring, never <code>null</code>
     */
    @Override
    public void visit(LinearRing r) {
        if (r != null)
            try {
                writer.writeStartElement(LINEAR_RING);
                handleGeometryAttributes(r);
                handleSimpleElement(COORDINATES, handleCoordinates(r.iterator()));
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle the output of a line
     *
     * @param l the line, never <code>null</code>
     */
    @Override
    public void visit(Line l) {
        if (l != null)
            try {
                writer.writeStartElement(LINE_STRING);
                handleGeometryAttributes(l);
                handleSimpleElement(COORDINATES, handleCoordinates(l.getPoints()));
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle the output of a point
     *
     * @param p the point, never <code>null</code>
     */
    @Override
    public void visit(Point p) {
        if (p != null)
            try {
                writer.writeStartElement(POINT);
                handleGeometryAttributes(p);
                //<extrude>0</extrude> <!-- boolean -->
                //<altitudeMode>clampToGround</altitudeMode>
                handleSimpleElement(COORDINATES, handleCoordinates(Collections
                        .singletonList(p)));
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /*
     * Handle the output of a circle
     *
     * @param circle the circle, never <code>null</code>
     */
	/*
    @Override
    public void visit(Circle circle) {
        // todo: use circle hints to output as linearRing, Polygon, etc.
    }
    */

	/**
     * Output a multigeometry, represented by a geometry bag
     *
     * @param bag the geometry bag, never <code>null</code>
     */
    @Override
    public void visit(GeometryBag bag) {
        if (bag != null && bag.getNumParts() != 0)
            try {
                writer.writeStartElement(MULTI_GEOMETRY);
                super.visit(bag);
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle the output of a MultiPoint geometry.
     *
     * @param multiPoint the MultiPoint, never <code>null</code>
     */
    @Override
    public void visit(MultiPoint multiPoint) {
        if (multiPoint != null && multiPoint.getPoints().size() != 0)
            try {
                writer.writeStartElement(MULTI_GEOMETRY);
                for (Point point : multiPoint.getPoints()) {
                    point.accept(this);
                }
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle the output of a MultiLine geometry.
     *
     * @param multiLine the MultiLine, never <code>null</code>
     */
    @Override
    public void visit(MultiLine multiLine) {
        if (multiLine != null && multiLine.getLines().size() != 0)
         try {
            writer.writeStartElement(MULTI_GEOMETRY);
            super.visit(multiLine);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handle the output of a MultiLinearRings geometry.
     *
     * @param rings the MultiLinearRings, never <code>null</code>
     */
    @Override
    public void visit(MultiLinearRings rings) {
        if (rings != null && rings.getLinearRings().size() != 0)
         try {
            writer.writeStartElement(MULTI_GEOMETRY);
            super.visit(rings);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handle the output of a MultiPolygons geometry.
     *
     * @param polygons the MultiPolygons, never <code>null</code>
     */
    @Override
    public void visit(MultiPolygons polygons) {
        if (polygons != null && polygons.getPolygons().size() != 0)
            try {
                writer.writeStartElement(MULTI_GEOMETRY);
                super.visit(polygons);
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Handle Geometry attributes common to Point, Line, LinearRing, and Polygon namely
     * extrude, tessellate, and altitudeMode.  Note tessellate tag is not applicable to Point
     * geometry and will be ignored is set on Points.
     *
     * @param geom
     * @throws XMLStreamException if there is an error with the underlying XML.
     */
    private void handleGeometryAttributes(GeometryBase geom) throws XMLStreamException {
        /*
            <element ref="kml:extrude" minOccurs="0"/>
            <element ref="kml:tessellate" minOccurs="0"/>
            <element ref="kml:altitudeModeGroup" minOccurs="0"/>
         */
        //<extrude>0</extrude>                   <!-- boolean -->
        // <tessellate>0</tessellate>             <!-- boolean -->
        // To enable tessellation, the value for <altitudeMode> must be clampToGround or clampToSeaFloor.
        //<altitudeMode>clampToGround</altitudeMode>
        // tessellate not applicable to Point
        Boolean extrude = geom.getExtrude();
        if (extrude != null)
            handleSimpleElement(EXTRUDE, extrude ? "1" : "0");
		Boolean tessellate = geom.getTessellate();
        if (tessellate != null && !(geom instanceof Point))
            handleSimpleElement(TESSELLATE, tessellate ? "1" : "0");
		handleAltitudeMode(geom.getAltitudeMode());
    }

	private void handleAltitudeMode(AltitudeModeEnumType altitudeMode) throws XMLStreamException {
		// if null or default (clampToGround) then ignore
		// if gx:AltitudeMode extension (clampToSeaFloor, relativeToSeaFloor) then output a with gx namespace
		if (altitudeMode != null) {
			if (altitudeMode == AltitudeModeEnumType.relativeToGround || altitudeMode == AltitudeModeEnumType.absolute) {
				handleSimpleElement(ALTITUDE_MODE, altitudeMode);
			} else if (altitudeMode == AltitudeModeEnumType.clampToSeaFloor || altitudeMode == AltitudeModeEnumType.relativeToSeaFloor) {
                if (gxNamespace != null)
                    writer.writeStartElement(gxNamespace.getPrefix(), ALTITUDE_MODE, gxNamespace.getURI());
                else
                    writer.writeStartElement("gx", ALTITUDE_MODE, NS_GOOGLE_KML_EXT);
                handleCharacters(altitudeMode.toString());
                writer.writeEndElement();
				//log.warn("gx:altitudeMode values not supported in KML output: " + altitudeMode);
				//writer.writeComment("gx:altitudeMode>" + altitudeMode + "</gx:altitudeMode");
        		writer.writeCharacters("\n");
			}
		}
	}

	/**
     * output the coordinates. The coordinates are output as lon,lat[,altitude]
     * and are separated by spaces
     *
     * @param coordinates an iterator over the points, never <code>null</code>
     * @return the coordinates as a string
     */
    private String handleCoordinates(Iterator<Point> coordinates) {
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
     * @param coordinateList the list of coordinates, never <code>null</code>
     * @return String formatted list of coordinate points
     * @throws XMLStreamException if there is an error with the underlying XML
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
     * @param b     StringBuilder to write coordinate to
     * @param point Point to be formatted for output
     */
    private void handleSingleCoordinate(StringBuilder b, Point point) {
        if (b.length() > 0) {
            b.append(' ');
        }
        Geodetic2DPoint p2d = point.getCenter();
        b.append(formatDouble(p2d.getLongitudeAsDegrees()));
        b.append(',');
        b.append(formatDouble(p2d.getLatitudeAsDegrees()));
        if (point.getCenter() instanceof Geodetic3DPoint) {
            Geodetic3DPoint p3d = (Geodetic3DPoint) point.getCenter();
            b.append(',');
            b.append(formatDouble(p3d.getElevation()));
        }
    }

    /**
     * Visit a Schema object
     *
     * @param schema Schema to visit, never null
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(Schema schema) {
        try {
            writer.writeStartElement(SCHEMA);
            writer.writeAttribute(NAME, schema.getName());
            String schemaid = schema.getId().toString();
            if (schemaid.startsWith("#")) {
                schemaid = schemaid.substring(1);
            }
            writer.writeAttribute(ID, schemaid);
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

    /**
     * Visit a Style object
     * @param style Style to visit, never null
     */
    @Override
    public void visit(Style style) {
        waitingElements.add(style);
    }

    /**
     * Handle the output of a NetworkLinkControl feature
     *
     * @param networkLinkControl
     * @throws RuntimeException if there is an error with the underlying XML
     */
    public void visit(NetworkLinkControl networkLinkControl) {
        /*
        <element name="NetworkLinkControl" type="kml:NetworkLinkControlType"/>
        <complexType name="NetworkLinkControlType" final="#all">
          <sequence>
            <element ref="kml:minRefreshPeriod" minOccurs="0"/>
            <element ref="kml:maxSessionLength" minOccurs="0"/>
            <element ref="kml:cookie" minOccurs="0"/>
            <element ref="kml:message" minOccurs="0"/>
            <element ref="kml:linkName" minOccurs="0"/>
            <element ref="kml:linkDescription" minOccurs="0"/>
            <element ref="kml:linkSnippet" minOccurs="0"/>
            <element ref="kml:expires" minOccurs="0"/>
            <element ref="kml:Update" minOccurs="0"/>
            <element ref="kml:AbstractViewGroup" minOccurs="0"/>
            <element ref="kml:NetworkLinkControlSimpleExtensionGroup" minOccurs="0"
              maxOccurs="unbounded"/>
            <element ref="kml:NetworkLinkControlObjectExtensionGroup" minOccurs="0"
              maxOccurs="unbounded"/>
          </sequence>
        </complexType>
       */
        try {
            writer.writeStartElement(NETWORK_LINK_CONTROL);
            handleNonNullSimpleElement("minRefreshPeriod", networkLinkControl.getMinRefreshPeriod());
            handleNonNullSimpleElement("maxSessionLength", networkLinkControl.getMaxSessionLength());
            handleNonEmptySimpleElement("cookie", networkLinkControl.getCookie());
            handleNonEmptySimpleElement("message", networkLinkControl.getMessage());
            handleNonEmptySimpleElement("linkName", networkLinkControl.getLinkName());
            handleNonEmptySimpleElement("linkDescription", networkLinkControl.getLinkDescription());
            handleNonEmptySimpleElement("linkSnippet", networkLinkControl.getLinkSnippet());
            Date expires = networkLinkControl.getExpires();
            if (expires != null) handleSimpleElement("expires", formatDate(expires));
            String targetHref = networkLinkControl.getTargetHref();
			String type = networkLinkControl.getUpdateType();
			if (targetHref != null && type != null) {
				writer.writeStartElement("Update");
				handleSimpleElement("targetHref", targetHref);
				// create elements -> Create | Delete | Change
				writer.writeEmptyElement(type);//TODO: handle multiple update objects
				writer.writeEndElement(); // end Update
            }
			handleAbstractView(networkLinkControl.getViewGroup()); // LookAt or Camera AbstractViewGroup
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNonEmptySimpleElement(String tag, String content) throws XMLStreamException {
        if (content != null) {
            content = content.trim();
            if (content.length() != 0) handleSimpleElement(tag, content);
        }
    }

	private void handleTaggedElement(String tag, TaggedMap map) throws XMLStreamException {
		handleNonNullSimpleElement(tag, map.get(tag));
	}

	private void handleTaggedElement(String tag, TaggedMap map, List<String> waitingList) throws XMLStreamException {
		String content = map.get(tag);
		if (content != null) {
            if (waitingList != null) {
				while (!waitingList.isEmpty()) {
					writer.writeStartElement(waitingList.remove(0));
				}
			}
            handleSimpleElement(tag, content);
        }
	}

    /*
     private void writeNonEmptyAttribute(String localName, String value) throws XMLStreamException {
         if (value != null) {
             value = value.trim();
             if (value.length() != 0) writer.writeAttribute(localName, value);
         }
     }

     private void writeNonNullAttribute(String localName, Object value) throws XMLStreamException {
         if (value != null)
             writer.writeAttribute(localName, value.toString());
     }
     */

    /**
     * Actually output the style
     *
     * @param style Style object to be written
     * @throws XMLStreamException if an error occurs
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
        if (style.hasListStyle()) {
            handleListStyleElement(style);
        }
        writer.writeEndElement();
    }

    /**
     * @param style polygon Style element to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    private void handlePolyStyleElement(Style style) throws XMLStreamException {
        writer.writeStartElement(POLY_STYLE);
        handleColor(COLOR, style.getPolyColor());
        if (style.getPolyfill() != null)
            handleSimpleElement(FILL, style.getPolyfill() ? "1" : "0"); // default 1
        if (style.getPolyoutline() != null)
            handleSimpleElement(OUTLINE, style.getPolyoutline() ? "1" : "0"); // default 1
        writer.writeEndElement();
    }

    /**
     * @param style Style element with label Style to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    private void handleLabelStyleElement(Style style) throws XMLStreamException {
        writer.writeStartElement(LABEL_STYLE);
        handleColor(COLOR, style.getLabelColor());
        handleDouble(SCALE, style.getLabelScale());
        writer.writeEndElement();
    }

    /**
     * @param style Style element with List style to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    private void handleListStyleElement(Style style) throws XMLStreamException {
        writer.writeStartElement(LIST_STYLE);
        handleColor(BG_COLOR, style.getListBgColor());
        Style.ListItemType listItemType = style.getListItemType();
        if (listItemType != null)
            handleSimpleElement(LIST_ITEM_TYPE, listItemType.toString());
        writer.writeEndElement();
    }

    /**
     * @param style balloon Style element to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    private void handleBalloonStyleElement(Style style)
            throws XMLStreamException {
        writer.writeStartElement(BALLOON_STYLE);
        handleColor(BG_COLOR, style.getBalloonBgColor());
        handleColor(TEXT_COLOR, style.getBalloonTextColor());
        handleSimpleElement(TEXT, style.getBalloonText());
        String displayMode = style.getBalloonDisplayMode();
        // ignore default displayMode value (default)
        if (displayMode != null && "hide".equals(displayMode))
            handleSimpleElement(DISPLAY_MODE, displayMode);
        writer.writeEndElement();
    }

    /**
     * @param style line Style element to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    protected void handleLineStyleElement(Style style)
            throws XMLStreamException {
        writer.writeStartElement(LINE_STYLE);
        handleColor(COLOR, style.getLineColor());
        handleDouble(WIDTH, style.getLineWidth());
        writer.writeEndElement();
    }

    /**
     * @param style icon Style element to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    protected void handleIconStyleElement(Style style)
            throws XMLStreamException {
        writer.writeStartElement(ICON_STYLE);
        handleColor(COLOR, style.getIconColor());
        handleDouble(SCALE, style.getIconScale());
        Double heading = style.getIconHeading();
        if (heading != null && Math.abs(heading) > 0.1 && heading < 360)
            handleSimpleElement(HEADING, formatDouble(heading));
        String iconUrl = style.getIconUrl();
        if (iconUrl != null) {
            // if want empty Icon tag then include a blank href
            if (iconUrl.length() == 0)
                writer.writeEmptyElement(ICON);
            else {
                writer.writeStartElement(ICON);
                handleSimpleElement(HREF, iconUrl);
                writer.writeEndElement();
            }
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

    private void handleDouble(String tag, Double value) throws XMLStreamException {
        if (value != null) {
            handleSimpleElement(tag, formatDouble(value));
        }
    }

    /**
     * Get the KML compliant color translation
     *
     * @param tag   String tag color element
     * @param color the Color of the tag to be written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    protected void handleColor(String tag, Color color)
            throws XMLStreamException {
        if (color != null) {
            handleSimpleElement(tag, String.format("%02x%02x%02x%02x",
                color.getAlpha(), color.getBlue(),
                color.getGreen(), color.getRed()));
        }
    }

    /**
     * Visit a StyleMap object
     * @param styleMap StyleMap to visit, never null
     */
    @Override
    public void visit(StyleMap styleMap) {
        waitingElements.add(styleMap);
    }

    /**
     * Actually handle style map
     *
     * @param styleMap StyleMap to be written
     * @throws XMLStreamException if there is an error with the underlying XML
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
            // key and url will never be null or empty
            handleSimpleElement(KEY, key);
            handleSimpleElement(STYLE_URL, value);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

  /**
     * Visit a Model object
     *
     * @param model Model to visit, never null
     * @throws RuntimeException if there is an error with the underlying XML
     */
    @Override
    public void visit(Model model) {
        try {
            Geodetic2DPoint point = model.getLocation();
            if (point == null && model.getAltitudeMode() == null)
                writer.writeEmptyElement(MODEL);
            else {
                writer.writeStartElement(MODEL);
                // if altitudeMode is invalid/null then it will be omitted (handles gx:AltitudeMode extensions)
                handleAltitudeMode(model.getAltitudeMode());
                if (point != null) {
                    writer.writeStartElement(LOCATION);
                    handleSimpleElement(LONGITUDE, formatDouble(point.getLongitudeAsDegrees()));
                    handleSimpleElement(LATITUDE, formatDouble(point.getLatitudeAsDegrees()));
                    if (model.is3D())
                        handleSimpleElement(ALTITUDE, formatDouble(((Geodetic3DPoint)point).getElevation()));
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if there are any elements on the waitingElements list
     */
    public boolean isWaiting() {
        return !waitingElements.isEmpty();
    }

}
/*
 *  KmlWriter.java
 *
 *  @author Jason Mathews
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
 */
package org.mitre.giscore.output.kml;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * Wrapper to KmlOutputStream for handling the common steps needed to create
 * basic KML or KMZ files.
 * <p/>
 * Handles the following tasks:
 * 
 * <ul>
 * <li>write to KMZ/KML files transparently. If file has a .kmz file extension then a KMZ (ZIP)
 *    file is created with that file name.
 * <li>discards empty containers if ContainerStart is followed by a ContainerEnd element
 *    in a successive write() call.
 * <li>write Files or contents from inputStream to entries in KMZ for networkLinked content,
 *    overlay images, etc.
 * </ul>
 * 
 * Complements the KmlReader class. Advanced KML support should use the <code>KmlOutputStream</code> class directly.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Created: Mar 13, 2009 10:06:17 AM
 */
public class KmlWriter {

    private static final Logger log = LoggerFactory.getLogger(KmlWriter.class);

    private KmlOutputStream kos;
    private ZipOutputStream zoS;
    private ContainerStart waiting;
	private boolean compressed;

	/**
	 * Construct a KmlWriter whiuch starts writing a KML document into
	 * the specified KML or KMZ file.  If filename ends with .kmz extension
	 * then a compressed KMZ (ZIP) file is produced with the main KML document
	 * stored as "doc.kml" in the root directory. <p/>
	 *
	 * For details on .KMZ files see "Creating a .kmz Archive" section
	 * of http://code.google.com/apis/kml/documentation/kml_21tutorial.html
	 * 
	 * @param file the file to be opened for writing.
	 * @throws IOException if an I/O error occurs
	 */
	public KmlWriter(File file) throws IOException {
        compressed = (file.getName().toLowerCase().endsWith(".kmz"));
        OutputStream os = new FileOutputStream(file);
        try {
            if (compressed) {
                BufferedOutputStream boS = new BufferedOutputStream(os);
                // Create the doc.kml file inside of a zip entry
                zoS = new ZipOutputStream(boS);
                ZipEntry zEnt = new ZipEntry("doc.kml");
                zoS.putNextEntry(zEnt);
                kos = new KmlOutputStream(zoS);
            } else {
                kos = new KmlOutputStream(os);
            }
        } catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
        }
        kos.write(new DocumentStart(DocumentType.KML));
		// TODO: consider adding KmlWriter(InoutStream is, boolean compress) constructor
    }

    /**
     * Tests whether the output file is a compressed KMZ file.
     *
     * @return <code>true</code> if the output file is a compressed KMZ file;
     *          <code>false</code> otherwise*
     *
     * @return
     */
    public boolean isCompressed() {
        return compressed;
    }

	/**
	 * Write file contents into entry of compressed KMZ file.  File can itself be
	 * KML, image, model or other file.  Contents are not parsed or validated.
	 * This must be called after entire KML for main document "doc.kml" is written.
	 *
	 * @param file file to write into the KMZ
	 * @param entryName the entry name for file as it will appear in the KMZ
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if arguments are null or KmlWriter is not writing
	 * 			a compressed KMZ file
	 */
	public void write(File file, String entryName) throws IOException {
		write(new FileInputStream(file), entryName);
    }

	/**
	 * Write contents from InputStream into file named localName in compressed KMZ file.
	 * This must be called after entire KML for main document doc.kml is written.
	 *
	 * @param is InputStream to write into the KMZ 
	 * @param entryName the entry name for file as it will appear in the KMZ.
	 * 					This should be a root-level or relative file path (e.g. myOtherData.kml or images/image.png).
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if arguments are null or KmlWriter is not writing
	 * 			a compressed KMZ file 
	 */
	public void write(InputStream is, String entryName) throws IOException {
		if (is == null) throw new IllegalArgumentException("InputStream cannot be null"); 
        try {
			if (!compressed)
            	throw new IllegalArgumentException("Not a compressed KMZ file. Cannot add arbitrary content to non-KMZ output.");
        	if (StringUtils.isBlank(entryName))
            	throw new IllegalArgumentException("localName must be non-blank file name");
			if (zoS == null) throw new IOException("stream is alreay closed");
			if (kos != null) {
				kos.closeWriter();
				kos = null;
				zoS.closeEntry();
			}
			ZipEntry zEnt = new ZipEntry(entryName.trim());
			zoS.putNextEntry(zEnt);
			// copy input to output
			// write contents to entry within compressed KMZ file
			IOUtils.copy(is, zoS);
			zoS.closeEntry();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * Write GISObject into KML output stream.
	 * 
	 * @param object IGISObject object to write
	 * 
	 * @throws RuntimeException if failed with XMLStreamException
	 * @throws IllegalArgumentException if KmlOutputStream is closed
	 */
	public void write(IGISObject object) {
		if (kos == null) throw new IllegalArgumentException("cannot write after stream is closed");
		// log.info("> Write: " + object.getClass().getName());
        if (object instanceof ContainerStart) {
            if (waiting != null) {
                kos.write(waiting);
            }
            waiting = (ContainerStart)object;
        } else {
            if (waiting != null) {
                if (object instanceof ContainerEnd && !kos.isWaiting()) {
					// if have ContainerStart followed by ContainerEnd then ignore empty container
                    // unless have waiting elements to flush (e.g. Styles)
                    waiting = null;
                    return;
                }
                /*
                if (object instanceof Style) {
                    // write style first so becomes attached to container
                    log.info("XXX: print style before container...");
                    kos.write(object);
                    object = null;
                }
                */
                kos.write(waiting);
                waiting = null;
            }
            //if (object != null)
            kos.write(object);
        }
    }

    /**
     * Close this KmlWriter and free any resources associated with the
     * writer.
     */
    public void close() {
        // If we have any waiting element (waiting != null) then
        // we have a ContainerStart with no matching ContainerEnd so ignore it
		if (kos != null)
			try {
				kos.closeWriter();
				kos = null;
			} catch (IOException e) {
				log.warn("Failed to close writer", e);
			}

        if (zoS != null) {
            try {
                zoS.closeEntry();
            } catch (IOException e) {
                log.error("Failed to close Zip Entry", e);
            }
			IOUtils.closeQuietly(zoS);
			zoS = null;
		}

        waiting = null;
    }

	/**
	 * @param href href URI to normalize
	 * @return Return normalized href, null if normal or failed to normalize
	 */
	private static String fixHref(String href) {
		if (href != null && href.startsWith("kmz")) {
			try {
				return new UrlRef(new URI(href)).getKmzRelPath();
			} catch (MalformedURLException e) {
				// ignore
			} catch (URISyntaxException e) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Normalize URLs from internal URIs. Only IGISObjects that haves URLs
	 * may be affected (i.e., NetworkLink, Overlay, and Style) and only if original
	 * href had a relative URL which gets rewritten to include the parent KML/KMZ document.
	 * 
	 * @param o IGISObject to normalize 
	 */
	public static void normalizeUrls(IGISObject o) {
		if (o instanceof NetworkLink) {
			NetworkLink nl = (NetworkLink) o;
			TaggedMap link = nl.getLink();
			if (link != null) {
				String href = fixHref(link.get(IKml.HREF));
				// check for treated URLs and normalized them so they work outside
				// this package (e.g. with Google Earth client).
				if (href != null) link.put(IKml.HREF, href);
			}
		} else if (o instanceof Overlay) {
			// handle GroundOverlay or ScreenOverlay href
			Overlay ov = (Overlay) o;
			TaggedMap icon = ov.getIcon();
			if (icon != null) {
				String href = fixHref(icon.get(IKml.HREF));
				if (href != null) icon.put(IKml.HREF, href);
			}
		} else if (o instanceof Style) {
			Style style = (Style) o;
			if (style.hasIconStyle()) {
				String href = fixHref(style.getIconUrl());
				if (href != null)
					style.setIconStyle(style.getIconColor(), style.getIconScale(), href);
			}
		}
	}
}

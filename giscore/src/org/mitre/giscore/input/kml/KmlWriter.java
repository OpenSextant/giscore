package org.mitre.giscore.input.kml;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Wrapper to KmlOutputStream that handles the common steps needed to create
 * basic KML or KMZ files.
 * 
 * Advanced KML support should use the KmlOutputStream class directly. 
 *
 * Handles the following tasks:
 *
 * - write to KMZ/KML files transparently. If file has a .kmz file extension then a KMZ file
 *    is created with that file name.
 * - discards empty containers with ContainerStart is followed by a ContainerEnd element
 *    in a successive write() call.
 * - write Files or contents from inputStream to entries in KMZ for networkLinked content,
 *    overlay images, etc.
 * 
 * Complements the KmlReader class.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 13, 2009 10:06:17 AM
 */
public class KmlWriter {

    private static final Logger log = LoggerFactory.getLogger(KmlWriter.class);

    private KmlOutputStream kos;
    private ZipOutputStream zoS;
    private ContainerStart waiting;

    public KmlWriter(File file) throws IOException {
        boolean compressed = (file.getName().toLowerCase().endsWith(".kmz"));
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
    }

    public void write(File file, String localName) throws IOException {
		write(new FileInputStream(file), localName);
    }

	/**
	 * Write contents from InputStream into file named localName in compressed KMZ file.
	 * This must be done after entire KML for main document doc.kml is written.
	 *
	 * @param is InputStream 
	 * @param localName
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if arguments are null or KmlWriter is not writing
	 * 			a compressed KMZ file 
	 */
	public void write(InputStream is, String localName) throws IOException {
		if (is == null) throw new IllegalArgumentException("InputStream cannot be null"); 
        try {
			if (zoS == null)
            	throw new IllegalArgumentException("Not a compressed KMZ file");
        	if (StringUtils.isBlank(localName))
            	throw new IllegalArgumentException("localName must be non-blank file name");
			if (kos != null) {
				kos.closeWriter();
				kos = null;
			}
			zoS.closeEntry();
			ZipEntry zEnt = new ZipEntry(localName.trim());
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
				log.warn("Failed to close", e);
			}

        if (zoS != null) {
            try {
                zoS.closeEntry();
            } catch (IOException e) {
                log.error("Failed to closeEntry", e);
            }
			IOUtils.closeQuietly(zoS);
		}

        waiting = null;
    }

}

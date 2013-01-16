/****************************************************************************************
 *  $Id: KmzOutputStream.java 1426 2011-09-27 13:08:37Z "MATHEWS" $
 *
 *  Created: Feb 6, 2009
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ProxyOutputStream;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.output.IGISOutputStream;

/**
 * The kmz output stream creates a result KMZ file using the given output
 * stream. It delegates the GIS output to a {@link KmlOutputStream}.
 * <p>
 * After all of the GIS objects have been written, additional zip entries can be
 * added with the {@code addEntry()} methods.
 * <p>
 * TODO: Add special handling for the COLLADA models:
 * http://code.google.com/apis/kml/documentation/kml_21tutorial.html
 * <p> 
 * See also {@link KmlWriter} which wraps KmlOutputStream and handles KML or KMZ
 * depending on file extension.
 *
 * @author jgibson
 */
public class KmzOutputStream implements IGISOutputStream {
	
	private KmlOutputStream kmlStream;
	final ZipOutputStream zipStream;

    /**
     * Creates a <code>KmzOutputStream</code> by opening a ZipOutputStream on the output stream.
     * @param stream OutputStream to decorate as a KmzOutputStream
     * @throws XMLStreamException if error occurs creating output stream
     */
    public KmzOutputStream(final OutputStream stream) throws XMLStreamException {
        this(stream, null);
    }

    /**
     * Creates a <code>KmzOutputStream</code> by opening a ZipOutputStream on the output stream.
     * @param stream OutputStream to decorate as a KmzOutputStream
     * @param encoding the encoding to use, if null default encoding (UTF-8) is assumed
     * @throws XMLStreamException if error occurs creating output stream
     */
	public KmzOutputStream(final OutputStream stream, String encoding) throws XMLStreamException {
		zipStream = new ZipOutputStream(stream);
		try {
			zipStream.putNextEntry(new ZipEntry("doc.kml"));
		} catch (IOException e) {
			throw new XMLStreamException("Could not add doc.kml entry to the zip file", e);
		}
		kmlStream = new KmlOutputStream(zipStream, encoding);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the underlying stream has already
	 *  been closed. (By adding additional file entries, for example).
	 */
	public void write(IGISObject object) throws IllegalStateException {
		if(kmlStream == null) {
			throw new IllegalStateException("GISStream has already been closed, no further GIS output is possible.");
		}
		kmlStream.write(object);
	}

	/** {@inheritDoc} */
	public void close() throws IOException {
		if(kmlStream != null) {
			kmlStream.close();
			kmlStream = null;
		} else {
			zipStream.close();
		}
	}

	/**
	 * Flush and close the KML file, but not the outputStream.
	 * <br/>
	 * This is useful if you want to write additional objects into the KMZ.
	 *
	 * @throws IOException if an error occurs
	 * @see KmlOutputStream#closeWriter()
	 */
	public void closeWriter() throws IOException {
		if(kmlStream != null) {
			kmlStream.closeWriter();
			kmlStream = null;
			zipStream.closeEntry();
		}
	}

	/**
	 * Add a new entry to the KMZ file. As in any other zipfile, entry names
	 * must be unique.
	 *
	 * @param entryName the name of the entry to add
	 * @return An {@code OutputStream} to receive the entry data. Closing it
	 *  will finish the current entry and will not close the underlying output
	 *  stream.
	 * @throws java.io.IOException if there is an error creating a new entry.
	 */
	public OutputStream addEntry(final String entryName) throws IOException {
		if(kmlStream != null) {
			closeWriter();
		} else {
			zipStream.closeEntry();
		}

		zipStream.putNextEntry(new ZipEntry(entryName));

		return new ProxyOutputStream(zipStream) {
			private boolean closed = false;

			@Override
			public void close() throws IOException {
				if(!closed) {
					flush();
					zipStream.closeEntry();
					closed = true;
				}
			}
		};
	}

	/**
	 * Add a new entry to the KMZ file. As in any other zipfile, entry names
	 * must be unique.
	 *
	 * @param source the data to add to the KMZ file.
	 * @param entryName the name of the entry to add
	 * @throws java.io.IOException if there is an error creating a new entry.
	 */
	public void addEntry(final InputStream source, final String entryName) throws IOException {
		final OutputStream out = addEntry(entryName);
		IOUtils.copy(source, out);
		out.close();
	}
}

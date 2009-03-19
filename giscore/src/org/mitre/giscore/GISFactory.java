/****************************************************************************************
 *  GISFactory.java
 *
 *  Created: Jan 28, 2009
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
package org.mitre.giscore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.gdb.GdbInputStream;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.esri.GdbOutputStream;
import org.mitre.giscore.output.esri.XmlGdbOutputStream;
import org.mitre.giscore.output.kml.KmlOutputStream;

/**
 * Factory class which creates concrete instantiations of input and output
 * streams for various formats.
 * 
 * @author DRAND
 */
public class GISFactory {
	/**
	 * Input stream factory
	 * 
	 * @param type
	 *            the type of the document, never <code>null</code>.
	 * @param stream
	 *            an input stream to the document or document contents, never
	 *            <code>null</code>.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis input stream, never <code>null</code>
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static IGISInputStream getInputStream(DocumentType type,
			InputStream stream, Object... arguments) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}

		if (DocumentType.KML.equals(type)) {
			return new KmlInputStream(stream);
		} else if (DocumentType.Shapefile.equals(type)) {
			return new GdbInputStream(type, stream);
		} else if (DocumentType.FileGDB.equals(type)) {
			return new GdbInputStream(type, stream);
		} else {
			throw new UnsupportedOperationException(
					"Cannot create an input stream for type " + type);
		}
	}
	
	/**
	 * Input stream factory
	 * 
	 * @param type
	 *            the type of the document, never <code>null</code>.
	 * @param file
	 *            a file containing the document, never <code>null</code>
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis input stream, never <code>null</code>
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static IGISInputStream getInputStream(DocumentType type,
			File file, Object... arguments) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("file should never be null and must exist");
		}

		if (DocumentType.KML.equals(type)) {
			return new KmlInputStream(new FileInputStream(file));
		} else if (DocumentType.Shapefile.equals(type)) {
			return new GdbInputStream(type, file);
		} else if (DocumentType.FileGDB.equals(type)) {
			return new GdbInputStream(type, file);
		} else {
			throw new UnsupportedOperationException(
					"Cannot create an input stream for type " + type);
		}
	}	

	/**
	 * Output stream factory
	 * 
	 * @param type
	 *            the type of the document to be written, never
	 *            <code>null</code>.
	 * @param outputStream
	 *            the output stream used to save the generated GIS file or
	 *            files.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis output stream, never <code>null</code>.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static IGISOutputStream getOutputStream(DocumentType type,
			OutputStream outputStream, Object... arguments) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException(
					"outputStream should never be null");
		}

		try {
			if (DocumentType.KML.equals(type)) {
				checkArguments(new Class[] {}, arguments,
						new boolean[] {});
				return new KmlOutputStream(outputStream);
			} else if (DocumentType.Shapefile.equals(type)) {
				checkArguments(new Class[] { File.class,
						IContainerNameStrategy.class }, arguments,
						new boolean[] { true, false });
				IContainerNameStrategy strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
						: null);
				return new GdbOutputStream(type, outputStream,
						(File) arguments[0], strategy);
			} else if (DocumentType.FileGDB.equals(type)) {
				checkArguments(new Class[] { File.class,
						IContainerNameStrategy.class }, arguments,
						new boolean[] { true, false });
				IContainerNameStrategy strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
						: null);
				return new GdbOutputStream(type, outputStream,
						(File) arguments[0], strategy);
			} else if (DocumentType.XmlGDB.equals(type)) {
				checkArguments(new Class[] {}, arguments,
						new boolean[] { });
				return new XmlGdbOutputStream(outputStream);
			} else if (DocumentType.PersonalGDB.equals(type)) {
				checkArguments(new Class[] { File.class,
						IContainerNameStrategy.class }, arguments,
						new boolean[] { true, false });
				IContainerNameStrategy strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
						: null);
				return new GdbOutputStream(type, outputStream,
						(File) arguments[0], strategy);
			} else {
				throw new UnsupportedOperationException(
						"Cannot create an output stream for type " + type);
			}
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		}
	}

	/**
	 * Check the count and types of the arguments against the required types
	 * 
	 * @param types
	 *            the required types, one per required argument
	 * @param arguments
	 *            the arguments
	 * @param required
	 *            for each argument, is it required? If required has fewer
	 *            elements than types or arguments the remainder is treated as
	 *            false. If a <code>false</code> element is found, any further
	 *            <code>true</code> value is ignored, i.e. only leading
	 *            arguments are considered required.
	 */
	private static void checkArguments(Class<? extends Object> types[],
			Object arguments[], boolean required[]) {
		int nreq = 0;
		for (int i = 0; i < required.length; i++) {
			if (!required[i])
				break;
			nreq++;
		}
		if (arguments.length < nreq) {
			throw new IllegalArgumentException(
					"There are insufficient arguments, there should be at least "
							+ types.length);
		}
		for (int i = 0; i < arguments.length; i++) {
			boolean argreq = false;
			Class<? extends Object> type = Object.class;
			Object arg = arguments[i];
			if (i < required.length) {
				argreq = required[i];
			}
			if (i < types.length) {
				type = types[i];
			}
			if (arg == null && argreq) {
				throw new IllegalArgumentException("Missing argument " + i);
			}
			if (arg != null) {
				Class<? extends Object> argtype = arg.getClass();
				if (!type.isAssignableFrom(argtype)) {
					throw new IllegalArgumentException("Argument #" + i
							+ " should be of a class derived from " + type);
				}
			}
		}
	}
}

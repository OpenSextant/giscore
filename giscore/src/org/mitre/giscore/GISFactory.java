/****************************************************************************************
 *  GISFactory.java
 *
 *  Created: Jan 28, 2009
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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLStreamException;

import org.mitre.giscore.events.Schema;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.csv.CsvInputStream;
import org.mitre.giscore.input.gdb.GdbInputStream;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.input.shapefile.ShapefileInputStream;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.csv.CsvOutputStream;
import org.mitre.giscore.output.esri.GdbOutputStream;
import org.mitre.giscore.output.esri.XmlGdbOutputStream;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.output.kml.KmzOutputStream;
import org.mitre.giscore.output.remote.ClientOutputStream;
import org.mitre.giscore.output.remote.RemoteOutputStream;
import org.mitre.giscore.output.shapefile.PointShapeMapper;
import org.mitre.giscore.output.shapefile.ShapefileOutputStream;

/**
 * Factory class which creates concrete instantiations of input and output
 * streams for various formats.
 * 
 * @author DRAND
 */
public class GISFactory {
	/**
	 * The size of the buffer that should be used when buffering content
	 * in memory. Right now this is a per feature class buffer size.
	 */
	public final static AtomicInteger inMemoryBufferSize = new AtomicInteger(2000);
	
	private static IRemoteGISService remoteService = null;
	
	/**
	 * Input stream factory
	 * 
	 * @param type
	 *            the type of the document, never <code>null</code>.
	 * @param stream
	 *            an input stream to the document or document contents, never
	 *            <code>null</code>. If stream is for a KMZ (ZIP) source then
	 *            <code>KmlReader</code> should used instead.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis input stream, never <code>null</code>
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalArgumentException
	 *             if type or stream are null
	 * @see org.mitre.giscore.input.kml.KmlReader
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
			checkArguments(new Class[] { IAcceptSchema.class }, arguments,
					new boolean[] { false });
			IAcceptSchema accepter = (IAcceptSchema) (arguments != null
					&& arguments.length > 0 ? arguments[0] : null);
			return new ShapefileInputStream(stream, accepter);
		} else if (DocumentType.FileGDB.equals(type)) {
			checkArguments(new Class[] { IAcceptSchema.class }, arguments,
					new boolean[] { false });
			IAcceptSchema accepter = (IAcceptSchema) (arguments != null
					&& arguments.length > 0 ? arguments[0] : null);
			return new GdbInputStream(type, stream, accepter);
		} else if (DocumentType.CSV.equals(type)) {
			checkArguments(new Class[] { Schema.class, String.class, 
					Character.class, Character.class },
					arguments,
					new boolean[] { false, false, false, false });
			return new CsvInputStream(stream, arguments);
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
	 *            a file containing the document, never <code>null</code>. If
	 *            file is a KMZ (ZIP) file then <code>KmlReader</code> should
	 *            used instead.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis input stream, never <code>null</code>
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalArgumentException
	 *             if type or file are null
	 * @see org.mitre.giscore.input.kml.KmlReader
	 */
	@SuppressWarnings("unchecked")
	public static IGISInputStream getInputStream(DocumentType type, File file,
			Object... arguments) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException(
					"file should never be null and must exist");
		}

		if (DocumentType.KML.equals(type)) {
			return new KmlInputStream(new FileInputStream(file));
		} else if (DocumentType.Shapefile.equals(type)) {
			checkArguments(new Class[] { IAcceptSchema.class }, arguments,
					new boolean[] { false });
			IAcceptSchema accepter = (IAcceptSchema) (arguments != null
					&& arguments.length > 0 ? arguments[0] : null);
			return new ShapefileInputStream(file, accepter);
		} else if (DocumentType.FileGDB.equals(type)) {
			checkArguments(new Class[] { IAcceptSchema.class }, arguments,
					new boolean[] { false });
			IAcceptSchema accepter = (IAcceptSchema) (arguments != null
					&& arguments.length > 0 ? arguments[0] : null);
			return new GdbInputStream(type, file, accepter);
		} else if (DocumentType.CSV.equals(type)) {
			checkArguments(new Class[] { Schema.class, String.class, Character.class, Character.class },
					arguments,
					new boolean[] { false, false, false, false });
			return new CsvInputStream(file, arguments);
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
	 *            files. If target output is KMZ then <code>KmlWriter</code>
	 *            should be used instead.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis output stream, never <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see org.mitre.giscore.output.kml.KmlWriter
	 */
	@SuppressWarnings("unchecked")
	public static IGISOutputStream getOutputStream(DocumentType type,
			OutputStream outputStream, Object... arguments) throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (outputStream == null && !DocumentType.Shapefile.equals(type)) {
			throw new IllegalArgumentException(
					"outputStream should never be null");
		}

		try {
			IContainerNameStrategy strategy;
			switch(type) {
				case KML:
					checkArguments(new Class[] {}, arguments, new boolean[] {});
					return new KmlOutputStream(outputStream);
				case KMZ:
					checkArguments(new Class[] {}, arguments, new boolean[] {});
					return new KmzOutputStream(outputStream);
				case Shapefile:
					checkArguments(new Class[] { File.class,
							IContainerNameStrategy.class, PointShapeMapper.class },
							arguments, new boolean[] { true, false, false });
					strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
							: null);
					PointShapeMapper mapper = (PointShapeMapper) (arguments.length > 2 ? arguments[2]
							: null);
					return new ShapefileOutputStream(outputStream,
							(File) arguments[0], strategy, mapper);
				case FileGDB:
					checkArguments(new Class[] { File.class,
							IContainerNameStrategy.class }, arguments,
							new boolean[] { false, false });
					strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
							: null);
					return new GdbOutputStream(type, outputStream,
							(File) arguments[0], strategy);
				case XmlGDB:
					checkArguments(new Class[] {}, arguments, new boolean[] {});
					return new XmlGdbOutputStream(outputStream);
				case PersonalGDB:
					checkArguments(new Class[] { File.class,
							IContainerNameStrategy.class }, arguments,
							new boolean[] { true, false });
					strategy = (IContainerNameStrategy) (arguments.length > 1 ? arguments[1]
							: null);
					return new GdbOutputStream(type, outputStream,
							(File) arguments[0], strategy);
				case CSV:
					checkArguments(new Class[] { String.class, Character.class, 
							Character.class, Boolean.class },
							arguments,
							new boolean[] { false, false, false, false });
					return new CsvOutputStream(outputStream, arguments);
				default:
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
	 * Output stream factory
	 * 
	 * @param type
	 *            the type of the document to be written, never
	 *            <code>null</code>.
	 * @param outputStream
	 *            the output stream used to save the generated GIS file or
	 *            files. If target output is KMZ then <code>KmlWriter</code>
	 *            should be used instead.
	 * @param arguments
	 *            the additional arguments needed by the constructor, the type
	 *            or types depend on the constructor
	 * @return a gis output stream, never <code>null</code>.
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see org.mitre.giscore.output.kml.KmlWriter
	 */
	public static IGISOutputStream getClientOutputStream(DocumentType type,
			OutputStream outputStream, Object... arguments) 
		throws IOException {
		if (type == null) {
			throw new IllegalArgumentException("type should never be null");
		}
		if (outputStream == null) {
			throw new IllegalArgumentException(
					"outputStream should never be null");
		}
		Long streamId = remoteService.makeGISOutputStream(type, arguments);
		ClientOutputStream client = new ClientOutputStream(remoteService, streamId);
		return new RemoteOutputStream(client, outputStream);
	}	
	
	/**
	 * Create an enterprise GDB output stream
	 * 
	 * <blockquote> <em>From the ESRI javadoc</em>
	 * <p>
	 * List of acceptable connection property names and a brief description of
	 * each:
	 * <table>
	 * <tr><th align="left">SERVER</th><td>SDE server name you are connecting to.</td></tr>
	 * <tr><th align="left">INSTANCE</th><td>Instance you are connection to.</td></tr>
	 * <tr><th align="left">DATABASE</th><td>Database connected to.</td></tr>
	 * <tr><th align="left">USER</th><td>Connected user.</td></tr>
	 * <tr><th align="left">PASSWORD</th><td>Connected password.</td></tr>
	 * <tr><th align="left">AUTHENTICATION_MODE</th><td>Credential authentication mode of the
	 * connection. Acceptable values are "OSA" and "DBMS".</td></tr>
	 * <tr><th align="left">VERSION</th><td>Transactional version to connect to. Acceptable value is
	 * a string that represents a transaction version name.</td></tr>
	 * <tr><th align="left">HISTORICAL_NAME</th><td>Historical version to connect to. Acceptable
	 * value is a string type that represents a historical marker name.</td></tr>
	 * <tr><th align="left">HISTORICAL_TIMESTAMP</th><td>Moment in history to establish an historical
	 * version connection. Acceptable value is a date time that represents a
	 * moment timestamp.</td></tr>
	 * </table>
	 * Notes:
	 * <p>
	 * The <Q>DATABASE</Q> property is optional and is required for ArcSDE instances
	 * that manage multiple databases (for example, SQL Server).
	 * <p>
	 * If <Q>AUTHENTICATION_MODE</Q> is <Q>OSA</Q> then <Q>USER</Q> and <Q>PASSWORD</Q> are not
	 * required. <Q>OSA</Q> represents operating system authentication and uses the
	 * operating system credentials to establish a connection with the database.
	 * <p>
	 * Since the workspace connection can only represent one version only 1 of
	 * the 3 version properties (<Q>VERSION</Q> or <Q>HISTORICAL_NAME</Q> or
	 * <Q>HISTORICAL_TIMESTAMP</Q>) should be used. </blockquote>
	 * 
	 * @param properties
	 * @param strategy
	 * @return
	 * @throws IOException 
	 */
	public static IGISOutputStream getSdeOutputStream(Properties properties, IContainerNameStrategy strategy) throws IOException {
		if (properties == null) {
			throw new IllegalArgumentException("properties should never be null");
		}
		return new GdbOutputStream(properties, strategy);
	}
	
	/**
	 * Create an enterprise GDB input stream
	 * 
	 * <blockquote> <em>From the ESRI javadoc</em>
	 * <p>
	 * List of acceptable connection property names and a brief description of
	 * each:
	 * <table>
	 * <tr><th align="left">SERVER</th><td>SDE server name you are connecting to.</td></tr>
	 * <tr><th align="left">INSTANCE</th><td>Instance you are connection to.</td></tr>
	 * <tr><th align="left">DATABASE</th><td>Database connected to.</td></tr>
	 * <tr><th align="left">USER</th><td>Connected user.</td></tr>
	 * <tr><th align="left">PASSWORD</th><td>Connected password.</td></tr>
	 * <tr><th align="left">AUTHENTICATION_MODE</th><td>Credential authentication mode of the
	 * connection. Acceptable values are "OSA" and "DBMS".</td></tr>
	 * <tr><th align="left">VERSION</th><td>Transactional version to connect to. Acceptable value is
	 * a string that represents a transaction version name.</td></tr>
	 * <tr><th align="left">HISTORICAL_NAME</th><td>Historical version to connect to. Acceptable
	 * value is a string type that represents a historical marker name.</td></tr>
	 * <tr><th align="left">HISTORICAL_TIMESTAMP</th><td>Moment in history to establish an historical
	 * version connection. Acceptable value is a date time that represents a
	 * moment timestamp.</td></tr>
	 * </table>
	 * Notes:
	 * <p>
	 * The <Q>DATABASE</Q> property is optional and is required for ArcSDE instances
	 * that manage multiple databases (for example, SQL Server).
	 * <p>
	 * If <Q>AUTHENTICATION_MODE</Q> is <Q>OSA</Q> then <Q>USER</Q> and <Q>PASSWORD</Q> are not
	 * required. <Q>OSA</Q> represents operating system authentication and uses the
	 * operating system credentials to establish a connection with the database.
	 * <p>
	 * Since the workspace connection can only represent one version only 1 of
	 * the 3 version properties (<Q>VERSION</Q> or <Q>HISTORICAL_NAME</Q> or
	 * <Q>HISTORICAL_TIMESTAMP</Q>) should be used. </blockquote>
	 * 
	 * @param properties
	 * @param accepter
	 * @return
	 * @throws IOException 
	 */
	public static IGISInputStream getSdeInputStream(Properties properties, IAcceptSchema accepter) throws IOException {
		if (properties == null) {
			throw new IllegalArgumentException("properties should never be null");
		}
		return new GdbInputStream(properties, accepter);
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
	protected static void checkArguments(Class<? extends Object> types[],
			Object arguments[], boolean required[]) {
		int nreq = 0;
		for (boolean aRequired : required) {
			if (!aRequired)
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

	/**
	 * @return the remoteService
	 */
	public IRemoteGISService getRemoteService() {
		return remoteService;
	}

	/**
	 * @param remoteService the remoteService to set
	 */
	public void setRemoteService(IRemoteGISService remoteService) {
		GISFactory.remoteService = remoteService;
	}
}

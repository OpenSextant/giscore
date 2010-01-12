/****************************************************************************************
 *  ShapefileInputStream.java
 *
 *  Created: Jan 12, 2010
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
package org.mitre.giscore.input.shapefile;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.IAcceptSchema;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.dbf.DbfInputStream;

import com.esri.arcgis.interop.AutomationException;

/**
 * Read one or more shapefiles in from a directory or from a zip input stream.
 * 
 * @author DRAND
 *
 */
public class ShapefileInputStream extends GISInputStreamBase {
	private static final String ms_tempDir = System.getProperty("java.io.tmpdir");
	
	private static final AtomicInteger ms_tempDirCounter = new AtomicInteger();

	/**
	 * The accepter, may be null, used to determine if a given schema is wanted
	 */
	private IAcceptSchema accepter;
	
	/**
	 * Working directory to hold shapefile data. If we are using a temp directory
	 * then this will be removed when close is called. If not it belongs to the
	 * caller and will be left alone.
	 */
	private File workingDir = null;
	
	/**
	 * Shapefiles found in the working directory. This will have all the found
	 * shapefiles. Shapefiles will actually be read based on the accepter if
	 * one is defined.
	 */
	private File shapefiles[] = null;
	
	/**
	 * The current shapefile being read. When we are done this will be set
	 * to the length of the shapefiles array.
	 * is yet been selected.
	 */
	private int currentShapefile = 0;
	
	/**
	 * The handler. The current handler is held here. If the handler returns
	 * <code>null</code> then the current shapefile's stream is empty. The first
	 * item returned from a new shapefile is the schema, and we can use that
	 * against the accepter to decide if the shapefile will be used or not. If
	 * it won't then we will move onto the next shapefile.
	 */
	private SingleShapefileInputHandler handler = null;
	
	/**
	 * This tracks if we're using a temp directory
	 */
	private boolean usingTemp;

	/**
	 * Ctor
	 * 
	 * @param type
	 *            the type used
	 * @param stream
	 *            the stream containing a zip archive of the file gdb
	 * @param accepter
	 * 				a function that determines if a schema should be used, may be <code>null</code>
	 * @throws IOException 
	 */
	public ShapefileInputStream(InputStream stream, IAcceptSchema accepter) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException(
					"stream should never be null");
		}
		
		// The stream better point to zip data
		ZipInputStream zipstream = null;
		if (! (stream instanceof ZipInputStream)) {
			zipstream = new ZipInputStream(stream);
		} else {
			zipstream = (ZipInputStream) stream;
		}
		
		File dir = new File(ms_tempDir, 
				"temp" + ms_tempDirCounter.incrementAndGet());
		dir.mkdirs();
		usingTemp = true;
		ZipEntry entry = zipstream.getNextEntry();
		while(entry != null) {
			String name = entry.getName().replace('\\', '/');
			String parts[] = name.split("/");
			File file = new File(dir, parts[parts.length - 1]);
			FileOutputStream fos = new FileOutputStream(file);
			IOUtils.copy(zipstream, fos);
			IOUtils.closeQuietly(fos);
			entry = zipstream.getNextEntry();
		}
		initialize(dir, accepter);
	}

	/**
	 * Ctor
	 * 
	 * @param file
	 *            the location of the the shapefile
	 * @param accepter
	 * 				a function that determines if a schema should be used,
	 * may be <code>null</code>
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public ShapefileInputStream(File file, IAcceptSchema accepter)
			throws UnknownHostException, IOException {
		if (file == null) {
			throw new IllegalArgumentException(
					"file should never be null");
		}
		usingTemp = false;
		initialize(file, accepter);
	}
	
	/**
	 * Initialize the input stream
	 * @param dir
	 * @param accepter
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws AutomationException
	 */
	private void initialize(File dir, IAcceptSchema accepter)
			throws IOException, UnknownHostException, AutomationException {	
		workingDir = dir;
		this.accepter = accepter;
		
		shapefiles = workingDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".shp");
			}
		});
	}

	@Override
	public void close() {
		if (handler != null) {
			handler.close();
		}
		if (usingTemp) {
			workingDir.delete();
		}
	}

	@Override
	public Iterator<Schema> enumerateSchemata() throws IOException {
		File[] dbfs = workingDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".dbf");
			}
		});
		List<Schema> schemata = new ArrayList<Schema>();
		for(File dbf : dbfs) {
			DbfInputStream dbfis = new DbfInputStream(dbf, null);
			Schema schema = (Schema) dbfis.read();
			if (schema != null) schemata.add(schema);
		}
		return schemata.iterator();
	}

	@Override
	public IGISObject read() throws IOException {
		IGISObject rval = null;
		while(rval == null && currentShapefile < shapefiles.length) {
			if (handler == null) {
				handleNewShapefile(); // It will iterate
			} else {
				rval = handler.read();
				if (rval == null) {
					handler.close();
					currentShapefile++;
					handler = null;
				} else if ((rval instanceof Schema) && accepter != null) {
					if (! accepter.accept((Schema) rval)) { 
						currentShapefile++; // Next
						handler.close();
						handler = null;
						rval = null; // null to force iteration
					}
				}
			}
		}
		return rval;
	}

	/**
	 * Calculate the shapefile basename and open the single handler to the 
	 * new shapefile.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private void handleNewShapefile() throws IOException {
		try {
			File shapefile = shapefiles[currentShapefile];
			String basename = shapefile.getName();
			int i = basename.indexOf(".shp");
			basename = basename.substring(0, i);
			handler = new SingleShapefileInputHandler(workingDir, basename);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}
}

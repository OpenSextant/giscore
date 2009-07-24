/****************************************************************************************
 *  SingleShapefileInputHandler.java
 *
 *  Created: Jul 22, 2009
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
package org.mitre.giscore.input.shapefile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.dbf.DbfInputStream;
import org.mitre.giscore.utils.ObjectBuffer;

/**
 * Read a single shapefile (.prj, .shp, .dbf, etc) to an object buffer for later
 * processing.
 * 
 * @author DRAND
 */
public class SingleShapefileInputHandler extends GISInputStreamBase implements
		IGISInputStream {
	/**
	 * Schema, derived from the read dbf file
	 */
	private Schema schema = null;

	/**
	 * Style derived from the shm file if present
	 */
	private Style style = null;

	/*
	 * Files that hold the essential information for the shapefile.
	 */
	private File dbfFile;
	private File shpFile;
	private File prjFile;

	/**
	 * Generates to be returned features, which are decorated with the geo data
	 * from the shp file and returned.
	 */
	private DbfInputStream dbf;

	/**
	 * Open shp file as a binary input stream
	 */
	private BinaryInputStream stream;

	/**
	 * The count of records
	 */
	private int recordCount;

	public SingleShapefileInputHandler(File inputDirectory, String shapefilename)
			throws URISyntaxException, IOException {
		if (inputDirectory == null || inputDirectory.exists() == false) {
			throw new IllegalArgumentException(
					"Input directory must exist and be non-null");
		}
		if (shapefilename == null) {
			throw new IllegalArgumentException(
					"shapefilename should never be null");
		}
		URI uri = new URI("urn:org:mitre:giscore:schema:"
				+ UUID.randomUUID().toString());
		schema = new Schema(uri);
		dbfFile = new File(inputDirectory, shapefilename + ".dbf");
		shpFile = new File(inputDirectory, shapefilename + ".shp");
		prjFile = new File(inputDirectory, shapefilename + ".prj");

		if (dbfFile.exists() == false) {
			throw new IllegalArgumentException(
					"DBF file missing for shapefile " + shapefilename);
		}
		if (shpFile.exists() == false) {
			throw new IllegalArgumentException(
					"SHP file missing for shapefile " + shapefilename);
		}
		if (prjFile.exists()) {
			checkPrj(prjFile);
		}

		dbf = new DbfInputStream(dbfFile, null);
		dbf.setRowClass(Feature.class);

		// First thing in the dbf should be a schema
		IGISObject ob = dbf.read();
		if (ob instanceof Schema) {
			schema = (Schema) ob;
			addFirst(ob);
		} else {
			throw new IllegalStateException(
					"Schema not the first thing returned from dbf");
		}

		FileInputStream fis = new FileInputStream(shpFile);
		stream = new BinaryInputStream(fis);
	}

	/**
	 * Check contents of the prj file to
	 * 
	 * @param prjFile
	 */
	private void checkPrj(File prjFile) {
		// FIXME: Do some sort of checking. We ought to either reject shapefiles
		// if the prj indicates an alternate datum or better yet we ought to
		// translate into the specified datum
	}

	@Override
	public void close() {
		if (stream != null)
			try {
				stream.close();
			} catch (IOException e) {
				throw new RuntimeException("Problem closing shp stream");
			}
		if (dbf != null)
			dbf.close();

		dbf = null;
		stream = null;
	}

	@Override
	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return readSaved();
		} else {
			return null;
		}
	}

}

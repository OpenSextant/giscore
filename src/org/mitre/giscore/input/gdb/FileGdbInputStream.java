/****************************************************************************************
 *  FileGdbInputStream.java
 *
 *  Created: Jan 3, 2013
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2013
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
package org.mitre.giscore.input.gdb;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.IAcceptSchema;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.filegdb.EnumRows;
import org.mitre.giscore.filegdb.Geodatabase;
import org.mitre.giscore.filegdb.Row;
import org.mitre.giscore.filegdb.Table;
import org.mitre.giscore.filegdb.Table.FieldInfo;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.output.esri.FileGdbConstants;

public class FileGdbInputStream extends GISInputStreamBase implements FileGdbConstants {
	
	private class TableState {
		private boolean hasGeo;
		private int index = -1;
		private List<String> paths = new ArrayList<String>();
		/**
		 * If currentTable is null we're writing the schema for the table or 
		 * feature class. Otherwise we're enumerating the rows or features for
		 * that particular table or feature class. When we've reached the end
		 * of the table we'll set it back to null to trigger the next schema or
		 * to move on to the next set.
		 */
		private Table currentTable = null;
		private EnumRows rows = null;
		private Schema currentSchema;
		
		public TableState(boolean hasGeo, List<String> paths) {
			this.hasGeo = hasGeo;
			this.paths = paths;
		}

		/**
		 * We're ready if:
		 * <ul>
		 * <li>We've never touched this table set so the index is negative
		 * <li>There's a row ready for the current path
		 * <li>The incremented index is in the range [0..size-1] so it can 
		 * index one of the paths and we can hand back the schema and rows
		 * </ul>
		 * 
		 * We're definitely not ever ready if there aren't any paths.
		 * 
		 * @return
		 */
		public boolean ready() {
			if (paths.size() == 0) return false;
			int ipo = index + 1;
			
			return index < 0 
					|| (rows != null && rows.hasNext())
					|| (ipo < paths.size() && currentTable == null);	
		}

		private IGISObject next() {
			String fcPath;
			if (currentTable == null) {
				index++;
				fcPath = paths.get(index);
				currentSchema = getSchema(fcPath);
				if (acceptor != null && !acceptor.accept(currentSchema)) {
					currentSchema = null;
					return next();
				} 
				currentTable = database.openTable(fcPath);
				rows = currentTable.enumerate();
				ContainerStart cs = new ContainerStart("Folder");
				cs.setSchema(currentSchema.getId());
				cs.setName(fcPath);
				addLast(cs);
				return currentSchema;
			} else {
				Row row = rows.next();
				if (row != null) {
					Map<String, Object> data = row.getAttributes();
					org.mitre.giscore.events.Row gval;
					if (hasGeo) {
						gval = new Feature();
					} else {
						gval = new org.mitre.giscore.events.Row();
					}
					// Map values to row/feature
					for(String name : currentSchema.getKeys()) {
						Object value = data.get(name);
						gval.putData(currentSchema.get(name), value);
					}
					if (hasGeo) {
						((Feature) gval).setGeometry(row.getGeometry());
					}
					gval.setSchema(currentSchema.getId());
					if (! rows.hasNext()) {
						currentTable = null;
						currentSchema = null;
						rows = null;
						addLast(new ContainerEnd());
					}
					return gval;
				} else {
					// If no rows at all. Does this ever happen?
					currentTable = null;
					currentSchema = null;
					rows = null;
					return new ContainerEnd();
				}
			}
		}
		
		private Schema getSchema(String path) {
			currentTable = database.openTable(path);
			Map<String, FieldInfo> fieldInfo = currentTable.getFieldTypes();
			Schema schema;
			try {
				String name = path.replaceAll("\\\\", "/");
				schema = new Schema(new URI("uri:" + name));
			} catch (URISyntaxException e) {
				throw new RuntimeException("Unexpected failure due to URI exception", e);
			}
			for(String name : fieldInfo.keySet()) {
				SimpleField field = new SimpleField(name);
				FieldInfo info = fieldInfo.get(name);
				if (info.type == 7) continue; // Geometry
				field.setLength(info.length);
				field.setType(convertFieldTypeToSQLType(info.type));
				field.setRequired(! info.nullable);
				schema.put(name, field);
			}
			schema.setName(path.substring(1));
			return schema;
		}
	}
	
	private Geodatabase database;
	private File inputPath;
	private boolean deleteOnClose = false;
	private TableState table;
	private TableState feature;
	private IAcceptSchema acceptor;

	public FileGdbInputStream(InputStream stream, IAcceptSchema acceptor) {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		if (! (stream instanceof InputStream)) {
			throw new IllegalArgumentException("stream must be an input stream");
		}
		deleteOnClose = true;
		File temp = new File(System.getProperty("java.io.tmpdir"));
		long t = System.currentTimeMillis();
		String name = "input" + t + ".gdb";
		inputPath = new File(temp, name);
		inputPath.mkdirs();
		
		// The stream better point to zip data
		ZipInputStream zis = null;
		if (!(stream instanceof ZipInputStream)) {
			zis = new ZipInputStream(stream);
		} else {
			zis = (ZipInputStream) stream;
		}
		
		ZipEntry entry;
		try {
			while((entry = zis.getNextEntry()) != null) {
				File entryPath = new File(entry.getName()); 
				File path = new File(inputPath, entryPath.getName());
				OutputStream os = new FileOutputStream(path);
				IOUtils.copy(zis, os);
				os.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		database = new Geodatabase(inputPath);
		init(acceptor);
	}
	
	public FileGdbInputStream(File path, IAcceptSchema acceptor) {
		if (path == null) {
			throw new IllegalArgumentException("path should never be null");
		}
		if (path.exists() == false) {
			throw new IllegalArgumentException("path must exist");
		}
		inputPath = path;
		database = new Geodatabase(inputPath);
		
		init(acceptor);
	}
	
	/**
	 * Initialize scanning information for the database by creating the
	 * two table states.
	 * @param acceptor 
	 */
	private void init(IAcceptSchema acceptor) {
		this.acceptor = acceptor;
		feature = new TableState(true, findAllChildren("\\", Geodatabase.FEATURE_CLASS));
		table = new TableState(false, findAllChildren("\\", Geodatabase.TABLE));
	}
	
	
	/**
	 * Walk the hierarchy and add all the found paths to the returned
	 * list
	 * 
	 * @param path
	 * @param type
	 * @return
	 */
	private List<String> findAllChildren(String path, String type) {
		String children[] = database.getChildDatasets(path, type);
		List<String> rval = new ArrayList<String>();
		for(String child : children) {
			rval.add(child);
			rval.addAll(findAllChildren(path + "\\" + child, type));
		}
		return rval;
	}
	
	@Override
	@CheckForNull
	public IGISObject read() throws IOException {
		if (hasSaved()) {
			return readSaved();
		}
		if (table.ready()) {
			return table.next();
		} else if (feature.ready()) {
			return feature.next();
		} else {
			return null;
		}
	}
	
	/**
	 * Convert esri types
	 * @param ft
	 * @return
	 */
	private SimpleField.Type convertFieldTypeToSQLType(int ft) {
		switch(ft) {
		case 0: // fieldTypeSmallInteger:
			return SimpleField.Type.SHORT;
		case 1: // fieldTypeInteger:
			return SimpleField.Type.INT;
		case 2: // fieldTypeSingle:
			return SimpleField.Type.FLOAT;
		case 3: // fieldTypeDouble:
			return SimpleField.Type.DOUBLE;
		case 4: // fieldTypeString:
			return SimpleField.Type.STRING;
		case 5: // fieldTypeDate:
			return SimpleField.Type.DATE;
		case 6: // fieldTypeOID:
			return SimpleField.Type.OID;
		case 7: // fieldTypeGeometry:
			return SimpleField.Type.GEOMETRY;
		case 8: // fieldTypeBlob:
			return SimpleField.Type.BLOB;
		case 9: // fieldTypeRaster:
			return SimpleField.Type.IMAGE;
		case 10: // fieldTypeGUID:
			return SimpleField.Type.GUID;
		case 11: // fieldTypeGlobalID:
			return SimpleField.Type.ID;
		case 12: // fieldTypeXML:
			return SimpleField.Type.CLOB;
		default:
			return null;
		}
	}

	@Override
	@NonNull
	public Iterator<Schema> enumerateSchemata() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		if (deleteOnClose) {
			inputPath.delete();
		}
		
	}

}

/****************************************************************************************
 *  EsriBaseOutputStream.java
 *
 *  Created: Feb 10, 2009
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
package org.mitre.giscore.output;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.Geodetic2DBounds;

/**
 * The esri formats require that the features be sorted into uniform bins where
 * the attributes and the type of geometry involved is homogeneous. This class
 * takes care of the sorting of features into temporary files that hold a
 * uniform set of features, and which allows the consumer to then get the
 * features back out by category.
 * 
 * @author DRAND
 */
public class FeatureSorter {
	private static final String OID = "OID";
	
	private static SimpleField oid = null;
	
	static {
		oid = new SimpleField(OID);
		oid.setType(SimpleField.Type.OID);
		oid.setLength(4);
		oid.setRequired(true);
		oid.setEditable(false);
		oid.setAliasName(OID);
		oid.setModelName(OID);
	}
	
	/**
	 * Maps the schema name to the schema. The schemata included are both
	 * defined schemata as well as implied or inline schemata that are defined
	 * with their data.
	 */
	private Map<URI, Schema> schemata = null;
	/**
	 * Maps a set of simple fields, derived from inline data declarations to a
	 * schema. This is used to gather like features together. THe assumption is
	 * that we will see consistent elements between features.
	 */
	private Map<Set<SimpleField>, Schema> internalSchema = null;
	/**
	 * Each schema's data is stored in a temporary file since the actual record
	 * sets need to be written for one type at a time.
	 */
	private Map<FeatureKey, File> dataFileMap = null;
	/**
	 * The output streams must be held open while features are being sorted. 
	 * Otherwise the output stream becomes corrupted, at least sometimes.
	 */
	private Map<FeatureKey, SimpleObjectOutputStream> dataStreamMap = null;
	/**
	 * The class keeps track of the overall extent of the features in a
	 * particular collection.
	 */
	private Map<FeatureKey, Geodetic2DBounds> boundingBoxes = null;
	/**
	 * To avoid all the juggling, we track the current feature key and current
	 * stream. When we hit a new piece of data for a different key we flip both
	 * of these pieces of information.
	 */
	private transient FeatureKey currentKey = null;

	/**
	 * Empty ctor
	 */
	public FeatureSorter() {
		try {
			cleanup();
		} catch (IOException e) {
			// Ignore, can't happen since no stream is open
		}
	}

	/**
	 * @return the known keys to the files
	 */
	public Collection<FeatureKey> keys() {
		return dataFileMap.keySet();
	}

	/**
	 * Get the file corresponding to the given feature key.
	 * 
	 * @param featureKey
	 *            the key, never <code>null</code>
	 * @return the corresponding file, or <code>null</code> if the key is
	 *         unknown, which cannot occur if the {@link #keys()} method was
	 *         used to obtain the key set.
	 */
	public File getFeatureFile(FeatureKey featureKey) {
		if (featureKey == null) {
			throw new IllegalArgumentException(
					"featureKey should never be null");
		}
		return dataFileMap.get(featureKey);
	}

	/**
	 * Add a feature to the appropriate file
	 * 
	 * @param feature
	 */
	public FeatureKey add(Feature feature) {
		if (feature == null) {
			throw new IllegalArgumentException(
					"feature should never be null");
		}
		try {
			Schema s = getSchema(feature);
			if (s.getOidField() == null) {
				s.put(oid);
			}
			Class<? extends Geometry> geoclass = null;
			if (feature.getGeometry() != null)
				geoclass = feature.getGeometry().getClass();
			FeatureKey key = new FeatureKey(s, geoclass, feature.getClass());
			SimpleObjectOutputStream stream = null;
			if (!key.equals(currentKey)) {
				currentKey = key;
				File stemp = dataFileMap.get(key);
				if (stemp == null) {
					stemp = File.createTempFile("gdbrecordset", ".data");
					dataFileMap.put(key, stemp);
					FileOutputStream os = new FileOutputStream(stemp, true);
					DataOutputStream dos = new DataOutputStream(os);
					SimpleObjectOutputStream oos = new SimpleObjectOutputStream(dos);
					dataStreamMap.put(key, oos);
				}
			}
			stream = dataStreamMap.get(key);
			stream.writeObject(feature);
			Geometry g = feature.getGeometry();
			if (g != null) {
				Geodetic2DBounds bounds = boundingBoxes.get(key);
				if (bounds == null) {
					bounds = new Geodetic2DBounds(g.getBoundingBox());
					boundingBoxes.put(key, bounds);
				} else {
					bounds.include(g.getBoundingBox());
				}
			}
			return key;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * A feature may either have a reference to a known schema or data that is
	 * not governed by a schema. The assumption here is that the extended data
	 * will only belong to a single schema, either ad hoc or explicit.
	 * <p>
	 * This code assumes that local references to a schema involve schemata that
	 * have already been read. External references will result in the creation
	 * of a dummy schema object with an appropriate name.
	 * <p>
	 * Names of the schema are urls, either fragments, which reference local
	 * schemata, or full urls, which reference non-resident schemata.
	 * 
	 * @param feature
	 *            the feature
	 * @return the referenced schema, never <code>null</code> but keep in mind
	 *         that internal schemata may be returned.
	 * @throws MalformedURLException
	 */
	private Schema getSchema(Feature feature) throws MalformedURLException {
		URI schema = feature.getSchema();
		if (schema != null) {
			Schema rval = schemata.get(schema);
			if (rval == null) {
				rval = new Schema(schema);
				schemata.put(schema, rval);
			}
			return rval;
		}
		// No schema case
		Set<SimpleField> fields = getFields(feature);
		Schema rval = internalSchema.get(fields);
		if (rval == null) {
			rval = new Schema();
			for (SimpleField field : fields) {
				rval.put(field.getName(), field);
			}
			internalSchema.put(fields, rval);
		}
		return rval;
	}

	/**
	 * This feature has inline data in the extended data, so extract the field
	 * names and create a set of such fields.
	 * 
	 * @param feature
	 *            the feature
	 * @return the fields, may be empty
	 */
	private Set<SimpleField> getFields(Feature feature) {
		Set<SimpleField> rval = new HashSet<SimpleField>();
		for (SimpleField field : feature.getFields()) {
			rval.add(field);
		}
		return rval;
	}

	/**
	 * @param schema
	 */
	public void add(Schema schema) {
		if (schema == null) {
			throw new IllegalArgumentException(
					"schema should never be null");
		}
		if (schema.getOidField() == null) {
			schema.put(oid);
		}
		schemata.put(schema.getId(), schema);
	}
	
	/**
	 * Get bounding area if the features are geometry
	 * @param key the key, assumed not <code>null</code>
	 * @return the bounding area, or <code>null</code> if the features aren't
	 * geometric.
	 */
	public Geodetic2DBounds getBounds(FeatureKey key) {
		return boundingBoxes.get(key);
	}

	/**
	 * Close any open streams.
	 * @throws IOException 
	 */
	public void close() throws IOException {
		if (dataStreamMap != null) {
			for(SimpleObjectOutputStream os : dataStreamMap.values()) {
				os.flush();
				os.close();
				dataStreamMap = null;
			}
		}
	}

	/**
	 * Cleanup deletes the temporary files and resets all the data structures.
	 * @throws IOException 
	 */
	public void cleanup() throws IOException {
		close();
		if (dataFileMap != null) {
			for (File tfile : dataFileMap.values()) {
				int count = 0;
				while (!tfile.delete() && count < 10) {
					synchronized (this) {
						// Wait a second and try again
						try {
							wait(1000L);
						} catch (InterruptedException e) {
							// ignore
						}
						count++;
					}
				}
			}
		}
		schemata = new HashMap<URI, Schema>();
		internalSchema = new HashMap<Set<SimpleField>, Schema>();
		dataFileMap = new HashMap<FeatureKey, File>();
		dataStreamMap = new HashMap<FeatureKey, SimpleObjectOutputStream>();
		boundingBoxes = new HashMap<FeatureKey, Geodetic2DBounds>();
		currentKey = null;
	}
}

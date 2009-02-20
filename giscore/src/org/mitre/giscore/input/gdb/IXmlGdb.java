/****************************************************************************************
 *  IXmlGdb.java
 *
 *  Created: Feb 6, 2009
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
package org.mitre.giscore.input.gdb;

/**
 * This interface contains all the values and element tags for creating the ESRI
 * Gdb Xml interchange document format.
 * 
 * @author DRAND
 */
public interface IXmlGdb {
	// Values
	static final String ESRI = "esri";
	static final String ESRI_NS = "http://www.esri.com/schemas/ArcGIS/9.3";
	static final String XS = "xs";
	static final String XS_NS = "http://www.w3.org/2001/XMLSchema";
	static final String XSI = "xsi";
	static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
	
	// XSD Schema attributes
	static final String TYPE_ATTR = "type";
	
	// Generated Field Names
	static final String GEO_FIELD = "geometry";

	// Elements 
	static final String ALIAS_NAME = "AliasName";
	static final String AREA_FIELD_NAME = "AreaFieldName";
	static final String AVG_NUM_POINTS = "AvgNumPoints";
	static final String CAN_VERSION = "CanVersion";
	static final String CATALOG_PATH = "CatalogPath";
	static final String CONTROLLER_MEMBERSHIPS = "ControllerMemberships";
	static final String CLSID = "CLSID";
	static final String DATA = "Data";
	static final String DATA_ELEMENT = "DataElement";
	static final String DATASET_DATA = "DatasetData";
	static final String DATASET_DEFS = "DatasetDefinitions";
	static final String DATASET_NAME = "DatasetName";
	static final String DATASET_TYPE = "DatasetType";
	static final String DOMAIN = "Domain";
	static final String DOMAINS = "Domains";
	static final String DSID = "DSID";
	static final String EDITABLE = "Editable";
	static final String EXTCLSID = "EXTCLSID";
	static final String EXTENT = "Extent";
	static final String EXT_PROPS = "ExtensionProperties";
	static final String FEATURE_TYPE = "FeatureType";
	static final String FIELD = "Field";
	static final String FIELD_ARRAY = "FieldArray";
	static final String FIELDS = "Fields";
	static final String FROM_POINT = "FromPoint";
	static final String GEOMETRY_DEF = "GeometryDef";
	static final String GEOMETRY_TYPE = "GeometryType";
	static final String GLOBAL_ID_FIELD = "GlobalIDFieldName";
	static final String GRID_SIZE = "GridSize"; 
	static final String HAS_GLOBAL_ID = "HasGlobalID";
	static final String HAS_ID = "HasID";
	static final String HAS_M = "HasM";
	static final String HAS_OID = "HasOID";
	static final String HAS_SPATIAL_INDEX = "HasSpatialIndex";
	static final String HAS_Z = "HasZ";
	static final String HIGH_PRECISION = "HighPrecision";
	static final String INDEX = "Index";
	static final String INDEXES = "Indexes";
	static final String INDEX_ARRAY = "IndexArray";
	static final String IS_ASCENDING = "IsAscending";
	static final String IS_NULLABLE = "IsNullable";
	static final String IS_UNIQUE = "IsUnique";
	static final String LEFT_LONGITUDE = "LeftLongitude";
	static final String LENGTH = "Length";
	static final String LENGTH_FIELD_NAME = "LengthFieldName";
	static final String METADATA = "Metadata";
	static final String MODEL_NAME = "ModelName";
	static final String M_ORIGIN = "MOrigin";
	static final String M_SCALE = "MScale";
	static final String M_TOLERANCE = "MTolerance";
	static final String NAME = "Name";
	static final String OID_FIELD_NAME = "OIDFieldName";
	static final String PATH = "Path";
	static final String PATH_ARRAY = "PathArray";
	static final String POINT = "Point";
	static final String POINT_ARRAY = "PointArray";
	static final String POINT_N = "PointN";
	static final String PRECISION = "Precision";
	static final String PROPERTY_ARRAY = "PropertyArray";
	static final String RASTER_FIELD_NAME = "RasterFieldName";
	static final String RECORD = "Record";
	static final String RECORDS = "Records";
	static final String REL_CLASS_NAMES = "RelationshipClassNames";
	static final String REQUIRED = "Required";
	static final String RING = "Ring";
	static final String RING_ARRAY = "RingArray";
	static final String SCALE = "Scale";
	static final String SHAPE_FIELD_NAME = "ShapeFieldName";
	static final String SHAPE_TYPE = "ShapeType";
	static final String SPATIAL_REFERENCE = "SpatialReference";
	static final String TABLE_DATA = "TableData";
	static final String TABLE_ROLE = "TableRole";
	static final String TO_POINT = "ToPoint";
	static final String VALUE = "Value";
	static final String VALUES = "Values";
	static final String VERSION = "Version";
	static final String VERSIONED = "Versioned";
	static final String WKID = "WKID";
	static final String WKT = "WKT";
	static final String WORKSPACE = "Workspace";
	static final String WORKSPACE_DATA = "WorkspaceData";
	static final String WORKSPACE_DEF = "WorkspaceDefinition";
	static final String WORKSPACE_TYPE = "WorkspaceType";
	static final String X = "X";
	static final String X_ORIGIN = "XOrigin";
	static final String XMAX = "XMax";
	static final String XMIN = "XMin";
	static final String XMLDOC = "XmlDoc";
	static final String XY_SCALE = "XYScale";
	static final String XY_TOLERANCE = "XYTolerance";
	static final String Y = "Y";
	static final String YMAX = "YMax";
	static final String YMIN = "YMin";
	static final String Y_ORIGIN = "YOrigin";
	static final String Z_ORIGIN = "ZOrigin";
	static final String Z_SCALE = "ZScale";
	static final String Z_TOLERANCE = "ZTolerance";
}

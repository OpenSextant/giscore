/****************************************************************************************
 *  IKml.java
 *
 *  Created: Feb 2, 2009
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
package org.mitre.giscore.input.kml;

import org.apache.log4j.helpers.ISO8601DateFormat;

/**
 * Container for element and attribute names used in KML
 * 
 * @author DRAND
 */
public interface IKml {
	static final String KML_NS = "http://earth.google.com/kml/2.2";
	static final ISO8601DateFormat ISO_DATE_FMT = 
		new ISO8601DateFormat();
	static final String ADDRESS = "address";
	static final String ALTITUDE = "altitude";
	static final String ALTITUDE_MODE = "altitudeMode";
	static final String BALLOON_STYLE = "BalloonStyle";
	static final String BEGIN = "begin";
	static final String BG_COLOR = "bgColor";
	static final String CAMERA = "Camera";
	static final String COLOR = "color";
	static final String COORDINATES = "coordinates";
	static final String DATA = "Data";
	static final String DESCRIPTION = "description";
	static final String DISPLAY_MODE = "displayMode";
	static final String DISPLAY_NAME = "displayName";
	static final String DOCUMENT = "Document";
	static final String DRAW_ORDER = "drawOrder";
	static final String EAST = "east";
	static final String END = "end";
	static final String EXTENDED_DATA = "ExtendedData";
	static final String FILL = "fill";
	static final String FLY_TO_VIEW = "flyToView";
	static final String FOLDER = "Folder";
	static final String GROUND_OVERLAY = "GroundOverlay";
	static final String HOT_SPOT = "hotSpot";
	static final String HREF = "href";
    static final String HTTP_QUERY = "httpQuery";
    static final String ICON = "Icon";
	static final String ICON_STYLE = "IconStyle";
	static final String ID = "id";
	static final String INNER_BOUNDARY_IS = "innerBoundaryIs";
	static final String KEY = "key";
	static final String KML = "kml";
	static final String LABEL_STYLE = "LabelStyle";
	static final String LAT_LON_BOX = "LatLonBox";
	static final String LINEAR_RING = "LinearRing";
	static final String LINE_STRING = "LineString";
	static final String LINE_STYLE = "LineStyle";
	static final String LINK = "Link";
	static final String LOOK_AT = "LookAt";
	static final String METADATA = "Metadata";
	static final String MODEL = "Model";
	static final String MULTI_GEOMETRY = "MultiGeometry";
	static final String NAME = "name";
	static final String NETWORK_LINK = "NetworkLink";
    static final String NETWORK_LINK_CONTROL = "NetworkLinkControl";
    static final String NORTH = "north";
	static final String OPEN = "open";
	static final String OUTER_BOUNDARY_IS = "outerBoundaryIs";
	static final String OUTLINE = "outline";
	static final String OVERLAY_XY = "overlayXY";
	static final String PAIR = "Pair";
    static final String PHONE_NUMBER = "phoneNumber";
	static final String PHOTO_OVERLAY = "PhotoOverlay"; // new in KML 2.2
	static final String PLACEMARK = "Placemark";
	static final String POINT = "Point";
	static final String POLY_STYLE = "PolyStyle";
	static final String POLYGON = "Polygon";
	static final String REFRESH_VISIBILITY = "refreshVisibility";
	static final String REGION = "Region";
	static final String ROTATION = "rotation";
	static final String ROTATION_XY = "rotationXY";
	static final String SCALE = "scale";
	static final String SCHEMA = "Schema";
	static final String SCHEMA_DATA = "SchemaData";
	static final String SCHEMA_URL = "schemaUrl";
	static final String SCREEN_OVERLAY = "ScreenOverlay";
	static final String SCREEN_XY = "screenXY";
	static final String SIMPLE_DATA = "SimpleData";
	static final String SIMPLE_FIELD = "SimpleField";
	static final String SIZE = "size";
	static final String SNIPPET = "Snippet";
	static final String SOUTH = "south";
	static final String STYLE = "Style";
	static final String STYLE_MAP = "StyleMap";
	static final String STYLE_URL = "styleUrl";
	static final String TEXT = "text";
	static final String TEXT_COLOR = "textColor";
	static final String TIME_SPAN = "TimeSpan";
	static final String TIME_STAMP = "TimeStamp";
	static final String TYPE = "type";
	static final String URL = "Url"; // (*) attribute deprecated in KML 2.1
	static final String VALUE = "value";
    static final String VIEW_FORMAT = "viewFormat";
    static final String VISIBILITY = "visibility";
	static final String WEST = "west";
	static final String WHEN = "when";
	static final String WIDTH = "width";
}

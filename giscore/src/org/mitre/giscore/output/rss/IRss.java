package org.mitre.giscore.output.rss;

import org.mitre.giscore.Namespace;

/**
 * Container for element and attribute names used in GeoRSS.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 6, 2009 6:07:28 PM
 */
public interface IRss {

    static final Namespace GEORSS_NS = Namespace.getNamespace("georss", "http://www.georss.org/georss");
    static final Namespace GML_NS = Namespace.getNamespace("gml", "http://www.opengis.net/gml");

    static final String AUTHOR = "author";
    static final String CATEGORY = "category";
    static final String CHANNEL = "channel";
    static final String COMMENTS = "comments";
    static final String DESCRIPTION = "description";
    static final String GUID = "guid";
    static final String IMAGE = "image";
    static final String ITEM = "item";
    static final String LANGUAGE = "language";
    static final String LINK = "link";
    static final String PUB_DATE = "pubDate";
    static final String RSS = "rss";
    static final String SOURCE = "source";
    static final String TITLE = "title";

    // GeoRSS elements namespace="http://www.georss.org/georss"
    // see http://www.georss.org/xml/1.1/georss.xsd

    static final String CIRCLE = "circle";
    static final String ELEV = "elev";
    static final String FEATURE_NAME = "featureName";
    static final String FEATURE_TYPE_TAG = "featureTypeTag";
    static final String LINE = "line";
    static final String POINT = "point";
    static final String POLYGON = "polygon";
    static final String RADIUS = "radius";
    static final String RELATIONSHIP_TAG = "relationshipTag";

}
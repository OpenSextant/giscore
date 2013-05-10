/***************************************************************************
 * (C) Copyright MITRE Corporation 2012
 *
 * The program is provided "as is" without any warranty express or implied,
 * including the warranty of non-infringement and the implied warranties of
 * merchantibility and fitness for a particular purpose.  The Copyright
 * owner will not be liable for any damages suffered by you as a result of
 * using the Program.  In no event will the Copyright owner be liable for
 * any special, indirect or consequential damages or lost profits even if
 * the Copyright owner has been advised of the possibility of their
 * occurrence.
 ***************************************************************************/
package org.opensextant.giscore.input.shapefile;

/**
 * An enumeration of the supported components of a Shapefile.
 * <br>
 * A "Shapefile" actually consists of several different files, most of which
 * are optional.
 *
 * @author jgibson
 */
public enum ShapefileComponent {
    /**
     * The main component of a shapefile that contains geometry.
     */
    SHP,
    /**
     * The main component of a shapefile that contains fields and shape metadata.
     */
    DBF,
    /**
     * Defines the coordinate projection of the shapefile.
     * <br>
     * Note that currently the only supported projection is WGS-84.
     */
    PRJ,
    // Unsupported components
    /**
     * Specifies the character encoding of the DBF component.
     */
    //CPG,
    /**
     * An index of the SHP component of a shapefile.
     */
    //SHX,
    /** Spatial index? */
    //SBN,
    /** Spatial index? */
    //SBX,
    /** Read-only spatial index? */
    //FBN,
    /** Read-only spatial index? */
    //FBX,
    /** Attribute index? */
    //AIN,
    /** Attribute index? */
    //AIX,
    /** Geocoding index for read-write shapefiles. */
    //IXS,
    /** Geocoding index for read-write shapefiles (ODB format?). */
    //MXS,
    /** An attribute index for the dbf? (ArcGIS 8 and later). */
    //ATX,
    /** Geospatial metadata in XML format, possibly in the ISO 19115 schema? */
    //SHP_XML
    ;
}

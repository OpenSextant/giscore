package org.mitre.giscore.events;

/**
 * Maps to altitudeModeEnumType in KML.
 * see http://code.google.com/apis/kml/documentation/kmlreference.html#altitudemode
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: May 28, 2009 2:07:37 PM
 */
public enum AltitudeModeEnumType {
    clampToGround, // default
    relativeToGround,
    absolute
}

package org.mitre.giscore.events;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps to altitudeModeEnumType in KML.
 * see http://code.google.com/apis/kml/documentation/kmlreference.html#altitudemode
 *
 * Also includes altitude types from gx:altitudeMode KML extension schema xmlns:gx=http://www.google.com/kml/ext/2.2
 * see http://code.google.com/apis/kml/schema/kml22gx.xsd
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: May 28, 2009 2:07:37 PM
 */
public enum AltitudeModeEnumType {

    clampToGround, // default
    relativeToGround,
    absolute,
	clampToSeaFloor,
	relativeToSeaFloor;

    private static final Logger log = LoggerFactory.getLogger(AltitudeModeEnumType.class);

	/**
	 * Get normalized altitude Mode such that if altitudeMode value is valid
	 * then associated AltitudeModeEnumType enumeration is returned otherwise
	 * null. Null or empty string also returns null. 
	 * @param altitudeMode ([clampToGround], relativeToGround, absolute)
	 * 		also including gx:extensions (clampToSeaFloor, relativeToSeaFloor)
	 * @return Enumerated type instance if valid otherwise null
	 */
    public static AltitudeModeEnumType getNormalizedMode(String altitudeMode) {
        if (StringUtils.isNotBlank(altitudeMode))
            try {
                return AltitudeModeEnumType.valueOf(altitudeMode);
            } catch (IllegalArgumentException e) {
                log.info("Ignoring invalid altitudeMode value: " + altitudeMode); // use default value
            }
        return null;
    }
}

package org.mitre.giscore.events;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    absolute;

    private static final Logger log = LoggerFactory.getLogger(AltitudeModeEnumType.class);

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

/****************************************************************************************
 *  GroundOverlay.java
 *
 *  Created: Feb 4, 2009
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
package org.mitre.giscore.events;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.input.kml.IKml;

/**
 * This element draws an image overlay draped onto the terrain. The <href> child
 * of <Icon> specifies the image to be used as the overlay. This file can be
 * either on a local file system or on a web server. If this element is omitted
 * or contains no <href>, a rectangle is drawn using the color and LatLonBox
 * bounds defined by the ground overlay.
 * 
 * @author DRAND
 * 
 */
public class GroundOverlay extends Overlay {
	private static final long serialVersionUID = 1L;
	
	private Double north, south, east, west, rotation, altitude;
	private String altitudeMode;

	/**
	 * @return the type
	 */
	public String getType() {
		return IKml.GROUND_OVERLAY;
	}

	/**
	 * @return the north
	 */
	public Double getNorth() {
		return north;
	}

	/**
	 * @param north
	 *            the north to set
	 */
	public void setNorth(Double north) {
		this.north = north;
	}

	/**
	 * @return the south
	 */
	public Double getSouth() {
		return south;
	}

	/**
	 * @param south
	 *            the south to set
	 */
	public void setSouth(Double south) {
		this.south = south;
	}

	/**
	 * @return the east
	 */
	public Double getEast() {
		return east;
	}

	/**
	 * @param east
	 *            the east to set
	 */
	public void setEast(Double east) {
		this.east = east;
	}

	/**
	 * @return the west
	 */
	public Double getWest() {
		return west;
	}

	/**
	 * @param west
	 *            the west to set
	 */
	public void setWest(Double west) {
		this.west = west;
	}

	/**
	 * @return the rotation
	 */
	public Double getRotation() {
		return rotation;
	}

	/**
     * Set angle of rotation of the overlay image about its center, in degrees
     * counterclockwise starting from north. The default is 0 (north).
     *
     * @param rotation angle of rotation
     * @throws IllegalArgumentException if rotation angle is out of range [-180,+180]
	 */
	public void setRotation(Double rotation) {
        if (rotation != null && (Double.isNaN(rotation) || rotation < -180 || rotation > 180))
            throw new IllegalArgumentException("Rotation out of range [-180,+180]: " + rotation);        
        this.rotation = rotation;
	}

	/**
	 * @return the altitude
	 */
	public Double getAltitude() {
		return altitude;
	}

	/**
	 * @param altitude
	 *            the altitude to set
	 */
	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	/**
	 * @return the altitudeMode
	 */
	public String getAltitudeMode() {
		return altitudeMode;
	}

	/**
	 * @param altitudeMode
	 *            the altitudeMode to set
	 */
	public void setAltitudeMode(String altitudeMode) {
		this.altitudeMode = altitudeMode;
	}

	/**
	 * The approximately equals method checks all the fields for equality with
	 * the exception of the geometry.
	 * 
	 * @param tf
	 */
	public boolean approximatelyEquals(Feature tf) {
		if (!(tf instanceof GroundOverlay))
			return false;
		if (!super.approximatelyEquals(tf))
			return false;

		GroundOverlay gother = (GroundOverlay) tf;
		boolean equals = closeFloat(altitude, gother.altitude)
				&& closeFloat(east, gother.east)
				&& closeFloat(north, gother.north)
				&& closeFloat(west, gother.west)
				&& closeFloat(south, gother.south)
				&& closeFloat(rotation, gother.rotation);

		if (!equals)
			return false;

		if (StringUtils.isBlank(altitudeMode) && 
				StringUtils.isBlank(gother.altitudeMode))
			return true;
		else if (altitudeMode != null)
			return altitudeMode.equals(gother.altitudeMode);
		else
			return false;
	}

	private boolean closeFloat(Double a, Double b) {
		if (a == null && b == null)
			return true;
		else if (a != null && b != null) {
			double delta = Math.abs(a - b);
			return delta < 1e-5; // delta < epsilon
		} else {
			return false;
		}
	}
}

/****************************************************************************************
 *  ScreenOverlay.java
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.mitre.giscore.input.kml.IKml;

/**
 * A kind of feature that contains an image overlayed on the screen.
 * 
 * @author DRAND
 */
public class ScreenOverlay extends Overlay {
	private static final long serialVersionUID = 1L;
	
	public ScreenLocation overlay;
	public ScreenLocation screen;
	public ScreenLocation rotation;
	public ScreenLocation size;
	public double rotationAngle;
	
	/**
	 * @return the type
	 */
	public String getType() {
		return IKml.SCREEN_OVERLAY;
	}
	
	/**
	 * @return the overlay
	 */
	public ScreenLocation getOverlay() {
		return overlay;
	}

	/**
	 * @param overlay the overlay to set
	 */
	public void setOverlay(ScreenLocation overlay) {
		this.overlay = overlay;
	}

	/**
	 * @return the screen
	 */
	public ScreenLocation getScreen() {
		return screen;
	}

	/**
	 * @param screen the screen to set
	 */
	public void setScreen(ScreenLocation screen) {
		this.screen = screen;
	}

	/**
	 * @return the rotation
	 */
	public ScreenLocation getRotation() {
		return rotation;
	}

	/**
	 * @param rotation the rotation to set
	 */
	public void setRotation(ScreenLocation rotation) {
		this.rotation = rotation;
	}

	/**
	 * @return the size
	 */
	public ScreenLocation getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(ScreenLocation size) {
		this.size = size;
	}

	/**
	 * @return the rotationAngle
	 */
	public double getRotationAngle() {
		return rotationAngle;
	}

	/**
	 * @param rotationAngle the rotationAngle to set
	 */
	public void setRotationAngle(double rotationAngle) {
		this.rotationAngle = rotationAngle;
	}
	
	/**
	 * The approximately equals method checks all the fields for equality with
	 * the exception of the geometry.
	 * 
	 * @param tf
	 */
	public boolean approximatelyEquals(Feature tf) {
		if (! (tf instanceof ScreenOverlay)) return false;
		if (! super.approximatelyEquals(tf)) return false;
		
		ScreenOverlay sother = (ScreenOverlay) tf;
		EqualsBuilder eb = new EqualsBuilder();
		return eb.append(overlay, sother.overlay) //
			.append(rotationAngle, sother.rotationAngle) //
			.append(screen, sother.screen) //
			.append(size, sother.size) //
			.append(rotation, sother.rotation) //
			.isEquals();
	}
}

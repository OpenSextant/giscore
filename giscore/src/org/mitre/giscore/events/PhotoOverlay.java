/****************************************************************************************
 *  PhotoOverlay.java
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

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.input.kml.IKml;

/**
 * @author DRAND
 *
 */
public class PhotoOverlay extends Overlay {

    private static final long serialVersionUID = 1L;

	/**
	 * @return the type
	 */
	public String getType() {
		return IKml.PHOTO_OVERLAY;
	}
	
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
}

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

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * A kind of feature that contains an image overlayed on the screen.
 * 
 * @author DRAND
 */
public class ScreenOverlay extends Overlay {
    
    private static final long serialVersionUID = 1L;

	private ScreenLocation overlay;      // overlayXY
	private ScreenLocation screen;       // screenXY
	private ScreenLocation rotation;     // rotationXY
	private ScreenLocation size;         // type="kml:vec2Type"

    private Double rotationAngle;
	
	/**
	 * @return the type
	 */
	public String getType() {
		return IKml.SCREEN_OVERLAY;
	}
	
	/**
	 * @return the overlay
	 */
    @Nullable
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
    @Nullable
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
    @Nullable
	public ScreenLocation getRotation() {
		return rotation;
	}

    /**
	 * @param rotation the rotationXY to set
	 */
	public void setRotation(ScreenLocation rotation) {
       this.rotation = rotation;
	}

	/**
	 * @return the size
	 */
    @Nullable
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
    @Nullable
	public Double getRotationAngle() {
		return rotationAngle;
	}

    /**
     * Set angle of rotation of the overlay image about its center, in degrees
     * counterclockwise starting from north. The default is 0 (north).
     *
     * @param rotationAngle angle of rotation
     * @throws IllegalArgumentException if rotation angle is out of range [-180,+180]
	 */
    public void setRotationAngle(Double rotationAngle) {
		if (rotationAngle != null) {
            double rotation = rotationAngle;
            if (Double.isNaN(rotation) || rotation < -180 || rotation > 180)
                throw new IllegalArgumentException("Rotation out of range [-180,+180]: " + rotation);
        }
        this.rotationAngle = rotationAngle;
	}
	
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
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

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Overlay#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		overlay = (ScreenLocation) in.readObject();
		rotation = (ScreenLocation) in.readObject();
		screen = (ScreenLocation) in.readObject();
		size = (ScreenLocation) in.readObject();
		rotationAngle = in.readDouble();
		
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.Overlay#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeObject(overlay);
		out.writeObject(rotation);
		out.writeObject(screen);
		out.writeObject(size);
		out.writeDouble(rotationAngle != null ? rotationAngle : 0.0);
	}
	
	
}

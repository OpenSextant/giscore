package org.mitre.giscore.geometry;

import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import java.io.IOException;

/**
 * Abstract Geometry class with common altitudeMode and extrude
 * fields associated with Points, Lines, Polygons, etc.
 *  
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 15, 2009 2:02:35 PM
 */
public abstract class GeometryBase extends Geometry {

    private static final long serialVersionUID = 1L;
    
    private AltitudeModeEnumType altitudeMode; // default (clampToGround)

    private Boolean extrude; // default (false)

	private Boolean tessellate; // default (false)

    /**
     * Altitude Mode ([clampToGround], relativeToGround, absolute). If value is null
     * then the default clampToGround is assumed and altitude can be ignored.
	 * @return the altitudeMode
	 */
	public AltitudeModeEnumType getAltitudeMode() {
		return altitudeMode;
	}

	/**
     * Set altitudeMode
	 * @param altitudeMode
	 *            the altitudeMode to set ([clampToGround], relativeToGround, absolute)
	 */
	public void setAltitudeMode(AltitudeModeEnumType altitudeMode) {
		this.altitudeMode = altitudeMode;
	}

    /**
     * Set altitudeMode to normalized AltitudeModeEnumType value or null if invalid. 
	 * @param altitudeMode
	 *            the altitudeMode to set ([clampToGround], relativeToGround, absolute)
	 * 				also includes gx:extensions (clampToSeaFloor and relativeToSeaFloor)
	 */
    public void setAltitudeMode(String altitudeMode) {
        this.altitudeMode = AltitudeModeEnumType.getNormalizedMode(altitudeMode);
	}

    public Boolean getExtrude() {
        return extrude;
    }

    public void setExtrude(Boolean extrude) {
        this.extrude = extrude;
    }

	public Boolean getTessellate() {
        return tessellate;
    }

    public void setTessellate(Boolean tessellate) {
        this.tessellate = tessellate;
    }

    /**
     * Read data from SimpleObjectInputStream
     * 
     * @param in SimpleObjectInputStream
     * 
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @exception IllegalArgumentException if enumerated AltitudeMode value is invalid
     */
    @SuppressWarnings("unchecked")
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		String val = in.readString();
		altitudeMode = val != null && val.length() != 0 ? AltitudeModeEnumType.valueOf(val) : null;
        int ch = in.readByte();
        extrude = (ch  == 0 || ch == 1) ? Boolean.valueOf(ch == 1) : null;
		ch = in.readByte();
		tessellate = (ch  == 0 || ch == 1) ? Boolean.valueOf(ch == 1) : null;
	}

    /**
     * Writes data to SimpleObjectOutputStream
     * @param out
     * @throws IOException if an I/O error occurs
     */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
        out.writeString(altitudeMode == null ? "" : altitudeMode.toString());
		out.writeByte(extrude == null ? 0xff : extrude ? 1 : 0);
		out.writeByte(tessellate == null ? 0xff : tessellate ? 1 : 0);
	}
}

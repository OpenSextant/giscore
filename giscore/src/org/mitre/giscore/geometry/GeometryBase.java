package org.mitre.giscore.geometry;

import edu.umd.cs.findbugs.annotations.CheckForNull;
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
    @CheckForNull
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

    @CheckForNull
    public Boolean getExtrude() {
        return extrude;
    }

    public void setExtrude(Boolean extrude) {
        this.extrude = extrude;
    }

    @CheckForNull
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
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		String val = in.readString();
		altitudeMode = val != null && val.length() != 0 ? AltitudeModeEnumType.valueOf(val) : null;
        int mask = in.readByte();
		int exMask = mask & 0xf;
        extrude = (exMask == 0 || exMask == 0x1) ? Boolean.valueOf(exMask == 0x1) : null;
		int tessMask = mask & 0xf0;
		tessellate = (tessMask == 0 || tessMask == 0x10) ? Boolean.valueOf(tessMask == 0x10) : null;
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
		// write out extrude and tessellate into same byte mask field since both are Boolean fields
		// with values: 0,1,null
		int mask = extrude == null ? 0x2 : extrude ? 0x1 : 0;
		if (tessellate == null) mask |= 0x20;
		else if (tessellate) mask |= 0x10;
		out.writeByte(mask);
	}
}

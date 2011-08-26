/****************************************************************************************
 *  Style.java
 *
 *  Created: Jan 28, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.events;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import java.awt.Color;
import java.io.IOException;

/**
 * Represents style information for points and lines. This information is used
 * by the rendering code to emit the correct information for the output format.
 * <p>
 * Generic information from a KML reference.
 * <p>
 * Color and opacity (alpha) values are expressed in hexadecimal notation. The
 * range of values for any one color is 0 to 255 (00 to ff). For alpha, 00 is
 * fully transparent and ff is fully opaque. The order of expression is
 * aabbggrr, where aa=alpha (00 to ff); bb=blue (00 to ff); gg=green (00 to ff);
 * rr=red (00 to ff). For example, if you want to apply a blue color with 50
 *
 * percent opacity to an overlay, you would specify the following:
 * <color>7fff0000</color>, where alpha=0x7f, blue=0xff, green=0x00, and
 * red=0x00.
 * <p>
 * Values for <colorMode> are normal (no effect) and random. A value of random
 * applies a random linear scale to the base <color> as follows.
 * <p>
 * <h4>Notes/Limitations:</h4>
 * <p>
 *  TODO: {@code ListStyle} supported except for ItemIcon
 * <br>
 *  Some less common tags (e.g. hotSpot in IconStyle) are not preserved.
 * 
 * @author DRAND
 * @author J.Mathews
 */
public class Style extends StyleSelector {

    private static final long serialVersionUID = 1L;

    public enum ColorMode { NORMAL, RANDOM }

    public enum ListItemType { check, checkOffOnly,
        checkHideChildren, radioFolder }

	private boolean hasIconStyle; // false
	private Color iconColor;
	private Double iconScale;
	private Double iconHeading;
	private String iconUrl;

	private boolean hasLineStyle; // false
	private Color lineColor;
	private Double lineWidth;

	private boolean hasListStyle; // false
	private Color listBgColor;
	private ListItemType listItemType;

	private boolean hasBalloonStyle; // false
	private Color balloonBgColor;
	private Color balloonTextColor;
	private String balloonText;
	private String balloonDisplayMode;

	private boolean hasLabelStyle; // false;
	private Color labelColor;
	private Double labelScale;

	private boolean hasPolyStyle; // false
	private Color polyColor;
	private Boolean polyfill;
	private Boolean polyoutline;

	/**
	 * Default Ctor
	 */
	public Style() {
        // default constructor only calls super()
	}

    /**
	 * Constructor Style with id
     * @param id
     */
	public Style(String id) {
        setId(id);
	}

	/**
	 * @return <code>true</code> if this style contains an icon style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasIconStyle() {
		return hasIconStyle;
	}

	/**
	 * @return <code>true</code> if this style contains a line style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasLineStyle() {
		return hasLineStyle;
	}

	/**
	 * @return <code>true</code> if this style contains a list style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasListStyle() {
		return hasListStyle;
	}

	/**
	 * @return <code>true</code> if this style contains a balloon style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasBalloonStyle() {
		return hasBalloonStyle;
	}

	/**
	 * @return <code>true</code> if this style contains a poly rendering style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasPolyStyle() {
		return hasPolyStyle;
	}

	/**
	 * @return <code>true</code> if this style contains a label style,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasLabelStyle() {
		return hasLabelStyle;
	}

	/**
	 * Set the icon style information
	 * 
	 * @param color
	 *            the color for the icon, can be null if want to use default color.
	 * @param scale
	 *            the scale of the icon, nullable (1.0=normal size of icon, 2.0=twice normal size, etc.)
	 * @param url
	 *            the url of the icon, nullable. If url is empty string or blank
	 *            then an empty <Icon/> element would appear in KML output.
	 *            If {@code null} then no <Icon> will appear in IconStyle (using default icon).
	 */
	public void setIconStyle(Color color, Double scale, String url) {
		setIconStyle(color, scale, null, url);
	}

	/**
	 * Set the icon style information
	 * 
	 * @param color
	 *            the color for the icon, can be null if want to use default color.
	 * @param scale
	 *            the scale of the icon, nullable (1.0=normal size of icon, 2.0=twice normal size, etc.)
	 * @param heading
	 *            heading (i.e. icon rotation) in degrees. Default=0 (North).
	 *            Values range from 0 to 360 degrees, nullable. 
	 * @param url
	 *            the url of the icon, nullable. If url is blank or empty string
	 *            then an empty <Icon/> element would appear in corresponding KML output.
	 *            If {@code null} then no <Icon> will appear in IconStyle (using default icon).
	 *
	 * @see org.mitre.giscore.output.kml.KmlOutputStream#handleIconStyleElement(Style)
	 */
	public void setIconStyle(Color color, Double scale, Double heading, String url) {
		iconColor = color;
		iconScale = scale == null || scale < 0.0 ? null : scale;
		iconHeading = heading == null || Math.abs(heading - 360) < 0.1 ? null : heading; // default heading = 0.0
		setIconUrl(url);
	}

	/**
	 * Set Icon Style url
	 *
	 * @param url
	 *            the url of the icon, nullable. If url is blank or empty string
	 *            then an empty <Icon/> element would appear in corresponding KML output.
	 *            If {@code null} then no <Icon> will appear in IconStyle (using default icon).
	 */
	public void setIconUrl(String url) {
		iconUrl = url == null ? null : url.trim();
		hasIconStyle = iconColor != null | iconScale != null || iconHeading != null || iconUrl != null;
	}

	/**
	 * @return the iconColor, the color to apply to the icon in the display.
     *      Value may be <code>null</code> in which the default color should be used.
	 */
    @CheckForNull
	public Color getIconColor() {
		return iconColor;
	}

	/**
	 * @return the iconScale, the fraction to increase or decrease the size of
	 *         the icon from it's native size.
     *      Value may be <code>null</code> in which the default scale (1.0) may be used.
	 */
    @CheckForNull
	public Double getIconScale() {
		return iconScale;
	}

    /**
	 * @return the iconHeading.
     *      Value may be <code>null</code> in which the default heading (0) may be used.
	 */
    @CheckForNull
	public Double getIconHeading() {
		return iconHeading;
	}

	/**
     * Get URL associated with IconStyle.
	 * @return the url of the icon (non-empty or null value)
     *      If null then IconStyle did not have Icon element. If value is
     *      non-null then IconStyle should have an associated Icon element
     *      present.<P>
     *      If icon URL is empty string then this indicates the href
     *      element was omitted, an empty element or value was an empty string.<BR>
     *      All 3 of these cases are handled the same in Google Earth which suppresses
     *      showing an icon.
     *      <pre>
                1. &lt;IconStyle&gt;
                    &lt;Icon/&gt;
                   &lt;/IconStyle&gt;

                2. &lt;Icon&gt;
                        &lt;href/&gt;
                   &lt;/Icon&gt;

                3. &lt;Icon&gt;
                        &lt;href&gt;&lt;/href&gt;
                   &lt;/Icon&gt;
     *      </pre>
	 */
    @CheckForNull
	public String getIconUrl() {
		return iconUrl;
	}

	/**
	 * Set the line style
	 * 
	 * @param color
	 *            the color of the line(s), can be null if want to use default color.
	 * @param width
	 *            the width of the line(s).
	 *            Note non-positive width suppresses display of lines in Google Earth
	 */
	public void setLineStyle(Color color, Double width) {
		lineColor = color;
		lineWidth = width == null ? null : (width <= 0.0) ? 0.0 : width;
        hasLineStyle = lineColor != null || lineWidth != null;
	}

	/**
	 * @return the lineColor, the color to use when rendering the line. May
	 *         be <code>null</code> in which the default color should be used.
	 */
    @CheckForNull
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * @return the lineWidth, the width of the line when rendered or <tt>null</tt> if not defined.
	 * Valid if {@link #hasLineStyle} is <code>true</code>.
	 * 
	 */
    @CheckForNull
	public Double getLineWidth() {
		return lineWidth;
	}

	public void setListStyle(Color listBgColor, ListItemType listItemType) {
		this.listBgColor = listBgColor;
		this.listItemType = listItemType;
        hasListStyle = listBgColor != null || listItemType != null;
	}

	/**
	 * Valid if {@link #hasListStyle} returns <code>true</code>.
	 * 
	 * @return the list background color, <tt>null</tt> if not defined.
	 */
    @CheckForNull
    public Color getListBgColor() {
        return listBgColor;
    }

    @CheckForNull
    public ListItemType getListItemType() {
        return listItemType;
    }

	/**
	 * Set the balloon style
	 * 
	 * @param bgColor
	 *            the color for the balloon background, if <code>null</code>
     *            will use default color: opaque white (ffffffff). 
	 * @param text
	 *            the textual template for the balloon content
	 * @param textColor
	 *            the color for the text in the balloon.
     *            The default is black (ff000000). 
	 * @param displayMode
     *            If <displayMode> is default, Google Earth uses the information
     *            supplied in <text> to create a balloon . If <displayMode> is hide,
     *            Google Earth does not display the balloon. "default" is the default value
     *            if null value is supplied.     
	 */
	public void setBalloonStyle(Color bgColor, String text, Color textColor,
			String displayMode) {
        hasBalloonStyle = text != null || bgColor != null;
		this.balloonBgColor = bgColor;
		this.balloonText = text;
		this.balloonTextColor = textColor;
		this.balloonDisplayMode = displayMode;
	}

	/**
	 * Valid if {@link #hasBalloonStyle} returns <code>true</code>.
	 * 
	 * @return the bgColor, background color of the balloon (optional). Color
	 *         and opacity (alpha) values are expressed in hexadecimal notation.
	 *         The range of values for any one color is 0 to 255 (00 to ff). The
	 *         order of expression is aabbggrr, where aa=alpha (00 to ff);
	 *         bb=blue (00 to ff); gg=green (00 to ff); rr=red (00 to ff). For
	 *         alpha, 00 is fully transparent and ff is fully opaque. For
	 *         example, if you want to apply a blue color with 50 percent
	 *         opacity to an overlay, you would specify the following:
	 *         <bgColor>7fff0000</bgColor>, where alpha=0x7f, blue=0xff,
	 *         green=0x00, and red=0x00. The default is opaque white (ffffffff).
	 */
    @CheckForNull
	public Color getBalloonBgColor() {
		return balloonBgColor;
	}

	/**
	 * Valid if {@link #hasBalloonStyle} returns <code>true</code>.
	 * 
	 * @return the text, Text displayed in the balloon. If no text is specified,
	 *         Google Earth draws the default balloon (with the Feature <name>
	 *         in boldface, the Feature <description>, links for driving
	 *         directions, a white background, and a tail that is attached to
	 *         the point coordinates of the Feature, if specified).
	 */
    @CheckForNull 
	public String getBalloonText() {
		return balloonText;
	}

	/**
	 * Valid if {@link #hasBalloonStyle} returns <code>true</code>.
	 * 
	 * @return the balloonTextColor, foreground color for text. The default is
	 *         black (ff000000).
	 */
    @CheckForNull
	public Color getBalloonTextColor() {
		return balloonTextColor;
	}

	/**
	 * Valid if {@link #hasBalloonStyle} returns <code>true</code>.
	 * 
	 * @return the balloonDisplayMode, If <displayMode> is default, Google Earth
	 *         uses the information supplied in <text> to create a balloon . If
	 *         <displayMode> is hide, Google Earth does not display the balloon.
	 *         In Google Earth, clicking the List View icon for a Placemark
	 *         whose balloon's <displayMode> is hide causes Google Earth to fly
	 *         to the Placemark.
	 */
    @CheckForNull
	public String getBalloonDisplayMode() {
		return balloonDisplayMode;
	}

	/**
	 * Set the label style
	 * 
	 * @param color
	 * @param scale
	 *            the scale of the labels, nullable (1.0=normal size, 2.0=twice normal size, etc.)
	 */
	public void setLabelStyle(Color color, Double scale) {
		labelColor = color;
		labelScale = scale;
		hasLabelStyle = color != null || scale != null;
	}

	/**
	 * Valid if {@link #hasLabelStyle} returns <code>true</code>.
	 * 
	 * @return the labelColor, <tt>null</tt> if not defined.
	 */
    @CheckForNull
	public Color getLabelColor() {
		return labelColor;
	}

	/**
	 * Valid if {@link #hasLabelStyle} returns <code>true</code>.
	 * 
	 * @return the labelScale, <tt>null</tt> if not defined.
	 */
    @CheckForNull
	public Double getLabelScale() {
		return labelScale;
	}

	/**
	 * Set the poly style
	 * 
	 * @param color Polygon color
	 * @param fill Specifies whether to fill the polygon
	 * @param outline Specifies whether to outline the polygon. Polygon outlines use the current LineStyle. 
	 */
	public void setPolyStyle(Color color, Boolean fill, Boolean outline) {
		polyColor = color;
		polyfill = fill;
		polyoutline = outline;
		hasPolyStyle = color != null || fill != null || outline != null;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyColor
	 */
    @CheckForNull
	public Color getPolyColor() {
		return polyColor;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyfill, specifies whether to fill the polygon.
	 */
    @CheckForNull
	public Boolean getPolyfill() {
		return polyfill;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyoutline, specifies whether to outline the polygon.
	 *         Polygon outlines use the current LineStyle.
	 */
    @CheckForNull
	public Boolean getPolyoutline() {
		return polyoutline;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		hasIconStyle = in.readBoolean();
		if (hasIconStyle) {
			iconColor = (Color)in.readScalar();
			iconUrl = in.readString();
			iconScale = (Double)in.readScalar();
			iconHeading = (Double)in.readScalar();
		}
		hasLineStyle = in.readBoolean();
		if (hasLineStyle) {
			lineColor = (Color)in.readScalar();
			lineWidth = (Double)in.readScalar();
		}

		hasListStyle = in.readBoolean();
		if (hasListStyle) {
			listBgColor = (Color)in.readScalar();
			listItemType = (ListItemType)in.readEnum(ListItemType.class);
		}

		hasBalloonStyle = in.readBoolean();
		if (hasBalloonStyle) {
			balloonBgColor = (Color) in.readScalar();
			balloonTextColor = (Color) in.readScalar();
			balloonText = in.readString();
			balloonDisplayMode = in.readString();
		}

		hasLabelStyle = in.readBoolean();
		if (hasLabelStyle) {
			labelColor = (Color) in.readScalar();
			labelScale = (Double) in.readScalar();
		}

		hasPolyStyle = in.readBoolean();
		if (hasPolyStyle) {
			polyColor = (Color) in.readScalar();
			polyfill = (Boolean)in.readScalar();
			polyoutline = (Boolean)in.readScalar();
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeBoolean(hasIconStyle);
		if (hasIconStyle) {
			out.writeScalar(iconColor);
			out.writeString(iconUrl);
			out.writeScalar(iconScale);
			out.writeScalar(iconHeading);
		}

		out.writeBoolean(hasLineStyle);
		if (hasLineStyle) {
			out.writeScalar(lineColor);
			out.writeScalar(lineWidth);
		}

		out.writeBoolean(hasListStyle);
		if (hasListStyle) {
			out.writeScalar(listBgColor);
			out.writeEnum(listItemType);
		}

		out.writeBoolean(hasBalloonStyle);
		if (hasBalloonStyle) {
			out.writeScalar(balloonBgColor);
			out.writeScalar(balloonTextColor);
			out.writeString(balloonText);
			out.writeString(balloonDisplayMode);
		}

		out.writeBoolean(hasLabelStyle);
		if (hasLabelStyle()) {
			out.writeScalar(labelColor);
			out.writeScalar(labelScale);
		}

		out.writeBoolean(hasPolyStyle);
		if (hasPolyStyle()) {
			out.writeScalar(polyColor);
			out.writeScalar(polyfill);
			out.writeScalar(polyoutline);
		}
	}

}

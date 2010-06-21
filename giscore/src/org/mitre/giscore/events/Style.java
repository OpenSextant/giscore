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
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.events;

import java.awt.Color;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.IStreamVisitor;

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
 * percent opacity to an overlay, you would specify the following:
 * <color>7fff0000</color>, where alpha=0x7f, blue=0xff, green=0x00, and
 * red=0x00.
 * <p>
 * Values for <colorMode> are normal (no effect) and random. A value of random
 * applies a random linear scale to the base <color> as follows.
 * <p>
 * <h4>Notes/Limitations:</h4>
 * <p>
 *  TODO: {@code ListStyle} not supported
 * <br>
 *  Some less common tags (e.g. hotSpot in IconStyle) are not preserved.
 * 
 * @author DRAND
 */
public class Style extends StyleSelector {

    private static final long serialVersionUID = 1L;

	public enum ColorMode { NORMAL, RANDOM }
	
	private boolean hasIconStyle = false;
	private Color iconColor;
	private double iconScale;
	private double iconHeading;
	private String iconUrl;

	private boolean hasLineStyle = false;
	private Color lineColor;
	private double lineWidth;

	private boolean hasBalloonStyle = false;
	private Color balloonBgColor;
	private Color balloonTextColor;
	private String balloonText;
	private String balloonDisplayMode;

	private boolean hasLabelStyle = false;
	private Color labelColor;
	private double labelScale;

	private boolean hasPolyStyle = false;
	private Color polyColor;
	private boolean polyfill;
	private boolean polyoutline;

	/**
	 * Ctor
	 */
	public Style() {
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
	 *            the color for the icon, never <code>null</code>
	 * @param scale
	 *            the scale of the icon
	 * @param url
	 *            the url of the icon
	 * @throws IllegalArgumentException if color is null. 
	 */
	public void setIconStyle(Color color, double scale, String url) {
		setIconStyle(color, scale, 0.0, url);
	}

	/**
	 * Set the icon style information
	 * 
	 * @param color
	 *            the color for the icon, never <code>null</code>
	 * @param scale
	 *            the scale of the icon
	 * @param heading
	 *            heading (i.e. icon rotation) in degrees. Default=0 (North).
	 *            Values range from 0 to 360 degrees. 
	 * @param url
	 *            the url of the icon
	 * @throws IllegalArgumentException if color is null. 
	 */
	public void setIconStyle(Color color, double scale, double heading, String url) {
		if (color == null) {
			throw new IllegalArgumentException("color should never be null");
		}
		hasIconStyle = true;
		iconColor = color;
		iconScale = scale <= 0.0 ? 0 : scale;
		iconHeading = Math.abs(heading - 360) < 0.1 ? 0 : heading;
		iconUrl = StringUtils.isBlank(url) ? null : url;
	}

	/**
	 * @return the iconColor, the color to apply to the icon in the display. May
	 *         be <code>null</code> if {@link #hasIconStyle} returns
	 *         <code>false</code>, but otherwise will be not <code>null</code>.
	 */
	public Color getIconColor() {
		return iconColor;
	}

	/**
	 * @return the iconScale, the fraction to increase or decrease the size of
	 *         the icon from it's native size. Valid if {@link #hasIconStyle}
	 *         returns <code>true</code>.
	 */
	public double getIconScale() {
		return iconScale;
	}

	public double getIconHeading() {
		return iconHeading;
	}

	/**
	 * @return the url of the icon. If null then href was either missing or an empty string.
	 */
	public String getIconUrl() {
		return iconUrl;
	}

	/**
	 * Set the line style
	 * 
	 * @param color
	 *            the color of the line(s), never <code>null</code>
	 * @param width
	 *            the width of the line(s).
	 *            Note non-positive width suppresses display of lines in Google Earth
	 * @throws IllegalArgumentException if color is null.
	 */
	public void setLineStyle(Color color, double width) {
		if (color == null) {
			throw new IllegalArgumentException("color should never be null");
		}
		hasLineStyle = true;
		lineColor = color;
		lineWidth = (width <= 0.0) ? 0.0 : width;
	}

	/**
	 * @return the lineColor, the color to use when rendering the line. Valid if
	 *         {@link #hasLineStyle} is <code>true</code>.
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * @return the lineWidth, the width of the line when rendered. Valid if
	 *         {@link #hasLineStyle} is <code>true</code>.
	 */
	public double getLineWidth() {
		return lineWidth;
	}

	/**
	 * Set the balloon style
	 * 
	 * @param bgColor
	 *            the color for the balloon background, if <code>null</code>
     *            will use default color: opaque white (ffffffff). 
	 * @param text
	 *            the textual template for the balloon content, never
	 *            <code>null</code>
	 * @param textColor
	 *            the color for the text in the balloon.
     *            The default is black (ff000000). 
	 * @param displayMode
     *            If <displayMode> is default, Google Earth uses the information
     *            supplied in <text> to create a balloon . If <displayMode> is hide,
     *            Google Earth does not display the balloon. "default" is the default value
     *            if null value is supplied.
     * @throws IllegalArgumentException if text is null.
	 */
	public void setBalloonStyle(Color bgColor, String text, Color textColor,
			String displayMode) {
		if (text == null) {
			throw new IllegalArgumentException("text should never be null");
		}
		hasBalloonStyle = true;
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
	public String getBalloonText() {
		return balloonText;
	}

	/**
	 * Valid if {@link #hasBalloonStyle} returns <code>true</code>.
	 * 
	 * @return the balloonTextColor, foreground color for text. The default is
	 *         black (ff000000).
	 */
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
	public String getBalloonDisplayMode() {
		return balloonDisplayMode;
	}

	/**
	 * Set the label style
	 * 
	 * @param color
	 * @param scale
	 */
	public void setLabelStyle(Color color, double scale) {
		labelColor = color;
		labelScale = scale;
		hasLabelStyle = true;
	}

	/**
	 * Valid if {@link #hasLabelStyle} returns <code>true</code>.
	 * 
	 * @return the labelColor
	 */
	public Color getLabelColor() {
		return labelColor;
	}

	/**
	 * Valid if {@link #hasLabelStyle} returns <code>true</code>.
	 * 
	 * @return the labelScale
	 */
	public double getLabelScale() {
		return labelScale;
	}

	/**
	 * Set the poly style
	 * 
	 * @param color Polygon color
	 * @param fill Specifies whether to fill the polygon
	 * @param outline Specifies whether to outline the polygon. Polygon outlines use the current LineStyle. 
	 */
	public void setPolyStyle(Color color, boolean fill,
			boolean outline) {
		polyColor = color;
		polyfill = fill;
		polyoutline = outline;
		hasPolyStyle = true;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyColor
	 */
	public Color getPolyColor() {
		return polyColor;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyfill, specifies whether to fill the polygon.
	 */
	public boolean isPolyfill() {
		return polyfill;
	}

	/**
	 * Valid if {@link #hasPolyStyle} returns <code>true</code>.
	 * 
	 * @return the polyoutline, specifies whether to outline the polygon.
	 *         Polygon outlines use the current LineStyle.
	 */
	public boolean isPolyoutline() {
		return polyoutline;
	}

	public void accept(IStreamVisitor visitor) {
		visitor.visit(this);
	}

}

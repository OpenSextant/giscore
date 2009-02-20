/****************************************************************************************
 *  Style.java
 *
 *  Created: Oct 28, 2008
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2008
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
package org.mitre.giscore.geometry;

import java.awt.Color;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents style information for points and lines. This information is used
 * by the rendering code to emit the correct information for the output format.
 * 
 * @author DRAND
 */
public class Style {
	private int		id;
	
	private boolean	hasIconStyle = false;
	private Color	iconColor;
	private double	iconScale;
	private int		iconIndex;
	
	private boolean hasLineStyle = false;
	private Color	lineColor;
	private int		lineWidth;
	
	/**
	 * Ctor
	 */
	public Style() {
	}

	/**
	 * @return <code>true</code> if this style contains an icon style, 
	 * <code>false</code> otherwise.
	 */
	public boolean hasIconStyle() {
		return hasIconStyle;
	}
	
	/**
	 * @return <code>true</code> if this style contains a line style, 
	 * <code>false</code> otherwise.
	 */
	public boolean hasLineStyle() {
		return hasLineStyle;
	}
	
	/**
	 * Set the icon style information
	 * @param color the color for the icon, never <code>null</code>
	 * @param scale the scale of the icon, must be positive
	 * @param index the index of the icon, may not be negative
	 */
	public void setIconStyle(Color color, double scale, int index) {
		if (color == null) {
			throw new IllegalArgumentException("color should never be null");
		}
		if (scale <= 0.0) {
			throw new IllegalArgumentException("scale must be positive");
		}
		if (index < 0) {
			throw new IllegalArgumentException("index cannot be negative");
		}
		hasIconStyle = true;
		iconColor = color;
		iconScale = scale;
		iconIndex = index;
	}
	
	/**
	 * @return the iconColor, the color to apply to the icon in the display. 
	 * May be <code>null</code> if {@link #hasIconStyle} returns <code>false</code>,
	 * but otherwise will be not <code>null</code>.
	 */
	public Color getIconColor() {
		return iconColor;
	}

	/**
	 * @return the iconScale, the fraction to increase or decrease the size of
	 * the icon from it's native size. Valid if {@link #hasIconStyle} returns
	 * <code>true</code>.
	 */
	public double getIconScale() {
		return iconScale;
	}

	/**
	 * @return the iconIndex, a numeric identifier that is translated by the 
	 * rendering code to a specific icon. There are no semantics associated
	 * with a particular value, that is determined by the rendering code. 
	 * Valid if {@link #hasIconStyle} returns <code>true</code>.
	 */
	public int getIconIndex() {
		return iconIndex;
	}
	
	/**
	 * Set the line style
	 * @param color the color of the line(s), never <code>null</code>
	 * @param width the width of the line(s), must be positive
	 */
	public void setLineStyle(Color color, int width) {
		if (color == null) {
			throw new IllegalArgumentException("color should never be null");
		}
		if (width <= 0) {
			throw new IllegalArgumentException("width must be positive");
		}
		hasLineStyle = true;
		lineColor = color;
		lineWidth = width;
	}

	/**
	 * @return the lineColor, the color to use when rendering the line. Valid
	 * if {@link #hasLineStyle} is <code>true</code>.
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * @return the lineWidth, the width of the line when rendered. Valid
	 * if {@link #hasLineStyle} is <code>true</code>.
	 */
	public int getLineWidth() {
		return lineWidth;
	}

	/**
	 * @return the id to use to distinguish this style from another
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}

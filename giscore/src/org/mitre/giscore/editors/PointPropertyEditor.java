/****************************************************************************************
 *  PointPropertyEditor.java
 *
 *  Created: Jul 9, 2009
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
package org.mitre.giscore.editors;

import java.beans.PropertyEditorSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mitre.giscore.geometry.Point;
import org.mitre.itf.geodesy.Angle;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;

/**
 * Convert a string to a geodetic point. The point must be a 
 * pair of floating point numbers. The first is taken to be 
 * the latitude and the second is the longitude.
 * <p>
 * Example: -24.33/+123.11
 * 
 * @author DRAND
 */
public class PointPropertyEditor extends PropertyEditorSupport {
	/**
	 * Pattern that matches a decimal lat/lon using +/- for the 
	 * sign
	 */
	protected static final Pattern coordpattern = 
		Pattern.compile("\\s*([+-]?\\d{0,2}\\.\\d*)\\s*[ ,/]\\s*([+-]?[01]?\\d{0,2}\\.\\d*)\\s*");
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#getAsText()
	 */
	@Override
	public String getAsText() {
		if (getValue() == null)
			return null;
		else {
			Point point = (Point) getValue();
			StringBuilder sb = new StringBuilder(8);
			sb.append(point.getCenter().getLatitude().inDegrees());
			sb.append("/");
			sb.append(point.getCenter().getLongitude().inDegrees());
			return sb.toString();
		}
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null || text.trim().length() == 0) {
			throw new IllegalArgumentException(
					"text should never be null or empty");
		}
		Matcher m = coordpattern.matcher(text);
		if (m.matches()) {
			Double lat = new Double(m.group(1));
			Double lon = new Double(m.group(2));
			Geodetic2DPoint point = new Geodetic2DPoint(new Longitude(lon, Angle.DEGREES),
					new Latitude(lat, Angle.DEGREES));
			setValue(new Point(point));
		} else {
			throw new IllegalArgumentException("Value " + text + " did not parse to a point");
		}
	}
}

/****************************************************************************************
 *  SafeDateFormat.java
 *
 *  Created: Apr 10, 2009
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
package org.mitre.giscore.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * SafeDateFormat is not a thread safe class and therefore needs to be 
 * instantiated on a per-thread basis. This class is a simplistic wrapper that
 * wraps a per-thread formatter and creates them on the fly, exposing only 
 * needed functionality.
 * 
 * @author DRAND
 */
public class SafeDateFormat {
	private ThreadLocal<SimpleDateFormat> ms_dateFormatter =
		new ThreadLocal<SimpleDateFormat>();
	
	private String pattern = null;
	
	public SafeDateFormat(String pattern) {
		if (pattern == null || pattern.trim().length() == 0) {
			throw new IllegalArgumentException(
					"pattern should never be null or empty");
		}
		this.pattern = pattern;
	}
	
	private SimpleDateFormat getInstance() {
		if (ms_dateFormatter.get() == null) {
			ms_dateFormatter.set(new SimpleDateFormat(pattern));
		}
		return ms_dateFormatter.get();
	}
	
	/**
	 * Format the value
	 * @param value the value, never <code>null</code>
	 * @return the formatted value, never <code>null</code> or empty
	 */
	public String format(Date value) {
		return getInstance().format(value);
	}
	
	/**
	 * Parse the value
	 * @param value the value, never <code>null</code>
	 * @return the parsed value, never <code>null</code>
	 * @throws ParseException 
	 */
	public Date parse(String value) throws ParseException {
		return getInstance().parse(value);
	}

	/**
	 * @param timeZone
	 */
	public void setTimeZone(TimeZone timeZone) {
		getInstance().setTimeZone(timeZone);
	}
}

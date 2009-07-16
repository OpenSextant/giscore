/****************************************************************************************
 *  StringHelper.java
 *
 *  Created: Jul 16, 2009
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

import java.io.UnsupportedEncodingException;

/**
 * A set of useful string utility functions
 * 
 * @author DRAND
 */
public class StringHelper {
	private static char vowels[] = new char[] {'a', 'A', 'e', 'E', 'i', 'I', 'o', 'O', 'u', 'U'};
	
	/**
	 * String as an ascii string. If the field name is too long, first try 
	 * removing vowels (after the first character). If the remainder is 
	 * still too long, truncate to no more than 11 characters.
	 * @param fieldname the fieldname, never <code>null</code> or empty
	 * @return a byte array with the fieldname in ascii
	 */
	public static byte[] esriFieldName(String fieldname) {
		if (fieldname == null || fieldname.trim().length() == 0) {
			throw new IllegalArgumentException(
					"fieldname should never be null or empty");
		}
		if (fieldname.length() > 11) {
			StringBuilder nfieldname = new StringBuilder(11);
			for(int i = 0; i < fieldname.length(); i++) {
				char ch = fieldname.charAt(i);
				boolean found = false;
				if (i > 0) {
					for(int j = 0; j < vowels.length; j++) {
						if (vowels[j] == ch) {
							found = true;
							break;
						}
					}
				}
				if (!found)	nfieldname.append(ch);
			}
			fieldname = nfieldname.toString();
		}
		if (fieldname.length() > 11) {
			fieldname = fieldname.substring(0, 11);
		}
		try {
			return fieldname.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Why is ASCII not supported?");
		}
	}
}

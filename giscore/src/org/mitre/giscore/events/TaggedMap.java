/****************************************************************************************
 *  TaggedMap.java
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

import java.util.HashMap;

/**
 * There are a number of elements in KML that simply need their data 
 * carried through the pipe. This class holds a named set of AV pairs.
 * 
 * @author DRAND
 *
 */
public class TaggedMap extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The tag of the element being held
	 */
	private String tag;
	
	/**
	 * Ctor
	 * @param tag the tag for the collection, never <code>null</code> or empty
	 */
	public TaggedMap(String tag) {
		if (tag == null || tag.trim().length() == 0) {
			throw new IllegalArgumentException(
					"tag should never be null or empty");
		}
		this.tag = tag;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}
}

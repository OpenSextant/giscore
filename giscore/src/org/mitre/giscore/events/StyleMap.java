/****************************************************************************************
 *  StyleMap.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mitre.giscore.IStreamVisitor;

/**
 * A <code>StyleMap></code> maps between two different Styles.
 *
 * Typically a <code>StyleMap</code> element is used to provide separate
 * normal and highlighted styles for a placemark, so that the highlighted
 * version appears when the user mouses over the icon in Google Earth client.
 *
 * @author DRAND
 */
public class StyleMap extends StyleSelector {
	
	private final Map<String, String> mappings = new HashMap<String, String>();

	public void put(String key, String url) {
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException(
					"key should never be null or empty");
		}
		if (url == null || url.trim().length() == 0) {
			throw new IllegalArgumentException(
					"url should never be null or empty");
		}
		mappings.put(key, url);
	}
	
	public String get(String key) {
		return mappings.get(key);
	}

	public Iterator<String> keys() {
		return mappings.keySet().iterator();
	}
	
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
		
}

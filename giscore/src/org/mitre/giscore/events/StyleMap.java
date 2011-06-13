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

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.input.kml.UrlRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>StyleMap></code> maps between two different Styles.
 *
 * Typically a <code>StyleMap</code> element is used to provide separate
 * normal and highlighted styles for a placemark, so that the highlighted
 * version appears when the user mouses over the icon in Google Earth client.
 *
 * <h4>Notes/Limitations:</h4>
 *  Does not support inline Styles in StyleMaps only referenced styles via URLs
 *  as typically used in all generated KML.
 *
 * @author DRAND
 */
public class StyleMap extends StyleSelector {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(StyleMap.class);

	private final Map<String, String> mappings = new HashMap<String, String>();

    public final static String NORMAL = "normal";
    public final static String HIGHLIGHT = "highlight";

    /**
     * Default Ctor
     */
    public StyleMap() {
        super();
    }

    /**
     * Constructor StyleMap with id
     * @param id
     */
    public StyleMap(String id) {
        setId(id);
    }

    /**
     *
     * @param key
     * @param url
     * throws IllegalArgumentException if key or url is null or empty string 
     */
	public void put(String key, String url) {
        key = StringUtils.trimToNull(key);
		if (key == null) {
			throw new IllegalArgumentException(
					"key should never be null or empty");
		}
        url = StringUtils.trimToNull(url);
		if (url == null) {
			throw new IllegalArgumentException(
					"url should never be null or empty");
		}
		// try to auto-fix bad KML. test if url relative identifier not starting
		// with '#' then prepend '#' to url
        if (!url.startsWith("#") && UrlRef.isIdentifier(url, true)) {
            url = "#" + url;
            log.debug("fix StyleMap url identifier as local reference: " + url);
        }
		mappings.put(key, url);
	}

    public boolean containsKey(String key) {
        return mappings.containsKey(key);
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

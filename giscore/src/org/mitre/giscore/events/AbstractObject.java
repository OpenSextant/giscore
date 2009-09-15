package org.mitre.giscore.events;

import org.apache.commons.lang.StringUtils;

/**
 * AbstractObject is a base IGISObject that simply has a unique id field.
 * Conceptually same as the KML AbstractObjectGroup element. 
 *  
 * @author Jason Mathews, MITRE Corp.
 * Date: Sep 15, 2009 10:50:00 AM
 */
public abstract class AbstractObject implements IGISObject {

    private String id;

    /**
     * @return the id to use to distinguish this object from another.
     * Value is either a non-empty string or <code>null</code>.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set, if blank or empty string then null is assigned
     */
    public void setId(String id) {
        this.id = StringUtils.isBlank(id) ? null : id;
    }
    
}

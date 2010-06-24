package org.mitre.giscore.events;

import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.utils.SimpleObjectInputStream;

import java.io.IOException;

/**
 * AbstractObject is a base IGISObject that simply has a unique id field.
 * Conceptually same as the KML AbstractObjectGroup element. 
 *  
 * @author Jason Mathews, MITRE Corp.
 * Date: Sep 15, 2009 10:50:00 AM
 */
public abstract class AbstractObject implements IDataSerializable, IGISObject {

	private static final long serialVersionUID = 1L;

    private String id;

    /**
     * @return the id to use to distinguish this object from another.
     * Value is either a non-empty string or <code>null</code>.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set, if blank or empty string then null is assigned.
     * Whitespace is stripped from start and end of the string if present.
     */
    public void setId(String id) {
        if (id != null) {
            id = id.trim();
            if (id.length() == 0) id = null;
        }
        this.id = id;
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		id = in.readString();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(id);
	}
    
}

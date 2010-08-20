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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;

/**
 * There are a number of elements in KML that simply need their data 
 * carried through the pipe. This class holds a named set of AV pairs.
 * 
 * @author DRAND
 *
 */
public class TaggedMap extends HashMap<String, String> implements IDataSerializable {
	private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TaggedMap.class);

	/**
	 * The tag of the element being held
	 */
    @NonNull private String tag;
	
	/**
	 * Empty ctor for IO only.  Constructor must be followed by call to {@code readData()}
     * to initialize the object instance otherwise object is invalid.
	 */
	public TaggedMap() {
		// must set tag otherwise object is invalid 
	}
	
	/**
	 * Ctor
	 * @param tag the tag for the collection, never <code>null</code> or empty
     * @throws IllegalArgumentException if tag is null or empty 
	 */
	public TaggedMap(String tag) {
		if (StringUtils.isBlank(tag)) {
			throw new IllegalArgumentException(
					"tag should never be null or empty");
		}
		this.tag = tag;
	}

	/**
	 * @return the tag
	 */
    @NonNull
	public String getTag() {
		return tag;
	}

    /**
     * Searches for the property with the specified key in this Map.
     * The method returns the default value argument if the property is not found.
     * @param   key            the key.
     * @param   defaultValue   a default value.
     * @return  the value in this Map with the specified key value
     *          or defaultValue if not found.  Not <code>null</code> unless defaultValue is null.
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		tag = in.readString();
		int count = in.readInt();
		for(int i = 0; i < count; i++) {
			String key = in.readString();
			String value = in.readString();
			put(key, value);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(tag);
		out.writeInt(size());
		for(Map.Entry<String,String> entry : entrySet()) {
			out.writeString(entry.getKey());
			out.writeString(entry.getValue());
		}
	}

    /**
     * Converts property to Integer if found.
     * @param key
     * @return Integer if property is found and a valid Integer value, otherwise null.
     */
    @CheckForNull
    public Integer getIntegerValue(String key) {
        String val = get(key);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                log.error("Error in " + key + " with value: " + val, nfe);
            }
        }
        return null;
    }

    /**
     * Converts property to Double if found.
     * @param key
     * @return Double if property is found and a valid Double value, otherwise null.
     */
    @CheckForNull
    public Double getDoubleValue(String key) {
        String val = get(key);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException nfe) {
                log.error("Error in " + key + " with value: " + val, nfe);
            }
        }
        return null;
    }
}

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

import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

/**
 * There are a number of elements in KML that simply need their data 
 * carried through the pipe. This class holds a named set of AV pairs.
 * 
 * @author DRAND
 *
 */
public class TaggedMap extends HashMap<String, String> implements IDataSerializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The tag of the element being held
	 */
	private String tag;
	
	/**
	 * Empty ctor for IO only
	 */
	public TaggedMap() {
		// 
	}
	
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
}

/****************************************************************************************
 *  GISInputStreamBase.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.input;

import java.util.ArrayList;
import java.util.List;

import org.mitre.giscore.events.IGISObject;

/**
 * Base class that handles the mark and reset behavior.
 * 
 * @author DRAND
 *
 */
public abstract class GISInputStreamBase implements IGISInputStream {
	private int capacity = 0;
	private int readpos = 0;
	private List<IGISObject> savebuffer = null;
	private boolean isreset = false;
	
	
	/* (non-Javadoc)
	 * @see org.mitre.giscore.input.IGISInputStream#mark(int)
	 */
	public void mark(int readlimit) {
		capacity = readlimit;
		readpos = 0;
		savebuffer = new ArrayList<IGISObject>(readlimit);
		isreset = false;
	}

	/**
	 * Save an object if appropriate for later reset
	 * @param obj the object to be saved
	 */
	public void save(IGISObject obj) {
		if (savebuffer != null && savebuffer.size() < capacity) {
			savebuffer.add(obj);
		}
	}
	
	/**
	 * @return <code>true</code> if there is data saved to be played back.
	 */
	public boolean hasSaved() {
		return isreset && savebuffer != null && readpos < savebuffer.size();
	}
	
	/**
	 * @return the saved data and manage the saved data
	 */
	public IGISObject readSaved() {
		if (hasSaved()) {
			IGISObject rval = savebuffer.get(readpos++);
			if (readpos == savebuffer.size()) {
				isreset = false;
				savebuffer = null;
			}
			return rval;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.input.IGISInputStream#reset()
	 */
	public void reset() {
		isreset = true;
	}

}

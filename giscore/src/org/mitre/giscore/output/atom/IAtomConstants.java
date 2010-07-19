/****************************************************************************************
 *  IAtomConstants.java
 *
 *  Created: Jul 16, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
package org.mitre.giscore.output.atom;

import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;

public interface IAtomConstants {	
	/**
	 * The name of the extended data element that holds the link information for
	 * atom features and rows.
	 */
	static final SimpleField LINK_ATTR = new SimpleField("ATOM_LINK");
	
	/**
	 * The name of the extended data element that holds the title information 
	 * for atom features and rows.
	 */
	static final SimpleField TITLE_ATTR = new SimpleField("ATOM_TITLE");
	
	/**
	 * The name of the extended data element that holds the updated date 
	 * information for atom features and rows. The type of the data should be
	 * a date.
	 */
	static final SimpleField UPDATED_ATTR = new SimpleField("ATOM_UPDATED", Type.DATE);
	
	/**
	 * One or more names, comma separated that can be passed as the author of a
	 * given atom entry. Required by the atom standard this may be filled in by 
	 * something fake for programatic purposes for internal use. Note that this
	 * may want better handling for real atom feed usage, i.e. it might require
	 * a type that can handle compound data for authors, or encoding of author
	 * data to allow author email and other data to be passed.
	 */
	static final SimpleField AUTHOR_ATTR = new SimpleField("ATOM_AUTHOR");
	
	/**
	 * A namespace for the extended data elements
	 */
	static final String EXT_DATA_NS = "http://mitre.org/itf/ext_data/1.0";
	
	/**
	 * Geo rss extensions
	 */
	static final String GIS_NS = "http://www.georss.org/georss";
}

/****************************************************************************************
 *  IGISObject.java
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
package org.mitre.giscore.events;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;

import org.mitre.giscore.geometry.VisitableGeometry;

/**
 * GISObject is primarily a marker interface for all the objects that are part
 * of the GIS core manipulation package.
 * 
 * @author DRAND
 */
public interface IGISObject extends VisitableGeometry {
	/**
	 * The id of the style referenced by the given feature or container
	 */
	public static final String STYLE_PROP = "styleUrl";
}

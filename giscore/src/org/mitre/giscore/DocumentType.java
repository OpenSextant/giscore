/****************************************************************************************
 *  DocumentType.java
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
package org.mitre.giscore;

/**
 * Values to identify the various formats to the framework. Extend this list
 * as new formats are added.
 * 
 * @author DRAND
 */
public enum DocumentType {
	KML /* Includes KML and KMZ */,
	Shapefile /* An ESRI format */,
	PersonalGDB /* Access GDB output, an ESRI format */,
	FileGDB /* File GDB output, an ESRI format */,
	XmlGDB /* An xml interchange format for ESRI Geodatabase data */
}

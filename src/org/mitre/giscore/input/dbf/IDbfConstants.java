/****************************************************************************************
 *  IDbfConstants.java
 *
 *  Created: Jun 24, 2009
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
package org.mitre.giscore.input.dbf;

/**
 * Common constants for Dbf files
 * 
 * @author DRAND
 */
public interface IDbfConstants {
	static final byte SIGNATURE = 0x03; // DBase III file signature
	static final byte EOH = 0x0D; // End of Header marker
	static final byte EOF = 0x1A; // End of File marker
	static final byte ROK = 0x20; // Record OK (not deleted) marker
	static final byte NUL = 0x00; // Null byte (filler or ignored)
	static final int MAX_CHARLEN = 253; // Maximum length for character
	static final String DATEFMT = "yyyyMMdd";
}

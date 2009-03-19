/****************************************************************************************
 *  ESRIInitializer.java
 *
 *  Created: Mar 18, 2009
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
package org.mitre.giscore.output.esri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.arcgis.system.AoInitialize;
import com.esri.arcgis.system.EngineInitializer;
import com.esri.arcgis.system.esriLicenseProductCode;

/**
 * Encapsulate initializing the ESRI environment
 * 
 * @author DRAND
 *
 */
public class ESRIInitializer {
	private static Logger logger = LoggerFactory.getLogger(ESRIInitializer.class);
	
	private static ESRIInitializer esri = null;
	
	public static synchronized void initialize() {
		if (esri == null) {
			esri = new ESRIInitializer();
		}
	}
	
	/**
	 * Private ctor
	 */
	private ESRIInitializer() {
		try {
			// Step 1: Initialize the Java Componet Object Model (COM) Interop.
			EngineInitializer.initializeEngine();
	
			// Step 2: Initialize a valid license.
			// new AoInitialize().initialize
			// (esriLicenseProductCode.esriLicenseProductCodeEngineGeoDB);
			new AoInitialize()
					.initialize(esriLicenseProductCode.esriLicenseProductCodeArcEditor);
		} catch (Exception e) {
			logger.error("Problem initializing the ESRI interop system", e);
		}
	}
}

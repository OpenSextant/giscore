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

import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.system.AoInitialize;
import com.esri.arcgis.system.EngineInitializer;
import com.esri.arcgis.system.esriLicenseProductCode;
import com.esri.arcgis.system.esriLicenseStatus;

/**
 * Encapsulate initializing the ESRI environment
 * 
 * @author DRAND
 *
 */
public class ESRIInitializer {
	private static Logger logger = LoggerFactory.getLogger(ESRIInitializer.class);
	
	private static ESRIInitializer esri = null;

	private static boolean attempted = false;

	/**
	 * Initialize the ESRI environment.
	 *
	 * @param force {@code true} skip our homemade checks and force the
	 *  initialization to happen. If these checks fail then ESRI's code will
	 *  invoke {@code System.exit()} (which can't be stopped).
	 * @param error {@code true} to throw an error if initialization fails
	 *  (instead of returning {@code false}).
	 * @return {@code true} iff ESRI was initialized properly.
	 * @throws LinkageError if there is an error initializing the ESRI codebase.
	 */
	public static synchronized boolean initialize(final boolean force, final boolean error) throws LinkageError {
		if(attempted && (!force) && (!error)) {
			return esri != null;
		}
		if (esri == null) {
			try {
				esri = new ESRIInitializer(force);
			} catch(LinkageError e) {
				attempted = true;
				if(error) {
					throw e;
				}
				return false;
			}
		}
		return true;
	}
	
	private static int LicensesToTry[] = new int[] {
		esriLicenseProductCode.esriLicenseProductCodeArcEditor,
		esriLicenseProductCode.esriLicenseProductCodeEngineGeoDB,
		esriLicenseProductCode.esriLicenseProductCodeArcView
	};
	
	/**
	 * Private ctor
	 */
	private ESRIInitializer(final boolean force) throws LinkageError {
		if(!force) {
			try {
				// We can't use the ESRIInitializer because esri shuts down the
				// JVM if the libraries can't be found.
				System.loadLibrary("ntvinv");
			} catch(UnsatisfiedLinkError e) {
				logger.warn("Could not initialize ESRI libraries, ArcGIS output formats will not work.", e);
				throw e;
			}
		}

		try {
			// Step 1: Initialize the Java Componet Object Model (COM) Interop.
			EngineInitializer.initializeEngine();
			for(int i = 0; i < LicensesToTry.length; i++) {
				// Step 2: Initialize a valid license. 
				try {
					int code = new AoInitialize().initialize(LicensesToTry[i]);
					if (code == esriLicenseStatus.esriLicenseAvailable ||
							code == esriLicenseStatus.esriLicenseAlreadyInitialized)
						break; // Worked!
				} catch(AutomationException e) {
					// Ignore
				}
			}
		} catch (Throwable t) {
			logger.error("Problem initializing the ESRI interop system", t);
			final LinkageError error = new LinkageError("Problem initializing the ESRI interop system");
			error.initCause(t);
			throw error;
		}
	}
}

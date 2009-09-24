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

import java.io.File;
import java.io.IOException;

import org.mitre.javautil.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.NativeLoader;
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
	private static final String TEST_DLL = "ntvinv";

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

	@SuppressWarnings("unchecked")
	private static Pair<Integer, String> LicensesToTry[] = new Pair[] {
		new Pair<Integer, String>(esriLicenseProductCode.esriLicenseProductCodeArcEditor, "Arc Editor"),
		new Pair<Integer, String>(esriLicenseProductCode.esriLicenseProductCodeEngineGeoDB, "Arc Engine GeoDB"),
		new Pair<Integer, String>(esriLicenseProductCode.esriLicenseProductCodeArcView, "Arc View"),
		new Pair<Integer, String>(esriLicenseProductCode.esriLicenseProductCodeArcServer, "Arc Server"),
		new Pair<Integer, String>(esriLicenseProductCode.esriLicenseProductCodeArcInfo, "Arc Info")
	};
	
	/**
	 * Private ctor
	 */
	private ESRIInitializer(final boolean force) throws LinkageError {
		if(!force) {
			logger.debug("Attempting to load libraries ourselves.");
			try {
				// We can't use the ESRIInitializer because esri shuts down the
				// JVM if the libraries can't be found.
				String arcHome = System.getenv("ARCGISHOME");
				if(arcHome == null || arcHome.length() < 1) {
					throw new UnsatisfiedLinkError();
				}
				logger.debug("ARCGISHOME found at " + arcHome);
				String libName = System.mapLibraryName(TEST_DLL);
				String parent = arcHome + "bin";
				File f = new File(parent, libName);
				if(!f.exists() || !f.isFile()) {
					throw new UnsatisfiedLinkError();
				}
				logger.debug("Test DLL found at " + f.getAbsolutePath());

				// This should only be attempted with version 9.3.0.B or with
				//  a version that has a patched loader.
				NativeLoader.loadLibrary(TEST_DLL);
			} catch(UnsatisfiedLinkError e) {
				logger.warn("Could not initialize ESRI libraries, ArcGIS output formats will not work.", e);
				throw e;
			}
		}

		try {
			// Step 1: Initialize the Java Component Object Model (COM) Interop.
			logger.debug("Initializing ESRI engine");
			EngineInitializer.initializeEngine();
			logger.debug("Initializing licenses");
			boolean worked = false;
			for(int i = 0; i < LicensesToTry.length; i++) {
				// Step 2: Initialize a valid license. 
				try {
					int code = new AoInitialize().initialize(LicensesToTry[i].a);
					if (code == esriLicenseStatus.esriLicenseAvailable ||
							code == esriLicenseStatus.esriLicenseAlreadyInitialized) {
						logger.info("Successfully initialized ESRI using license: {}", LicensesToTry[i].b);
						worked = true;
						break; // Worked!
					}
					if(logger.isDebugEnabled()) {
						switch(code) {
							case esriLicenseStatus.esriLicenseCheckedIn:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported license checked in.");
								break;
							case esriLicenseStatus.esriLicenseCheckedOut:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported license checked out.");
								break;
							case esriLicenseStatus.esriLicenseFailure:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported license failure.");
								break;
							case esriLicenseStatus.esriLicenseNotInitialized:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported license not initialized.");
								break;
							case esriLicenseStatus.esriLicenseNotLicensed:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported not licensed.");
								break;
							case esriLicenseStatus.esriLicenseUnavailable:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported license unavailable.");
								break;
							default:
								logger.debug("Initialization of " + LicensesToTry[i].b + " reported strange license response: " + code);
						}
					}
				} catch(AutomationException e) {
					// Ignore
					logger.debug("Automation exception initializing ESRI using license: {} : {}", LicensesToTry[i].b, e.getMessage());
				} catch(IOException e) {
					// Ignore
					logger.debug("IO exception initializing ESRI using license: {} : {}", LicensesToTry[i].b, e.getMessage());
				}
			}
			if(!worked) {
				logger.warn("All attempts at license initialization failed. ESRI operations may fail randomly.");
			}
		} catch (Throwable t) {
			logger.error("Problem initializing the ESRI interop system", t);
			final LinkageError error = new LinkageError("Problem initializing the ESRI interop system");
			error.initCause(t);
			throw error;
		}
	}
}

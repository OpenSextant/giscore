package org.mitre.giscore.utils;

import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.itf.geodesy.*;

import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * Simple Debugging Tool to import KML/KMZ documents by File or URL and dump statistics
 * on number of feature elements (Placemarks, Points, Polygons, LineStrings, NetworkLinks, etc.)
 * and optionally export the same KML to a file to verify all content has been properly parsed.
 * This will uncover any issues in reading and writing target KML files. Some KML files fail
 * to parse and those cases fall within the cateogy of those that don't conform to the
 * appropriate KML XML Schema or follow the KML Reference Spec (see
 * http://code.google.com/apis/kml/documentation/kmlreference.html) such "Do not
 * include spaces between the three values that describe a coordinate", etc.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Created: May 20, 2009 12:05:04 PM
 */
public class KmlMetaDump implements IKml {

	private final Map<String,Integer> tagSet = new java.util.TreeMap<String,Integer>();
	private boolean followLinks;
	private File outPath;
	private boolean outPathCheck;
	private int features;


	public void checkSource(URL url) throws IOException {
		System.out.println(url);
		checkReader(new KmlReader(url), url.getFile());
	}

	public void checkSource(File file) throws IOException {
		if (file.isDirectory()) {
			for (File f : file.listFiles())
				if (f.isDirectory())
					checkSource(f);
				else {
					String name = f.getName().toLowerCase();
					if (name.endsWith(".kml") || name.endsWith(".kmz"))
						checkSource(f);
				}
		} else {
			System.out.println(file.getAbsolutePath());
			checkReader(new KmlReader(file), file.getName());
		}
	}

	public void setFollowLinks(boolean followLinks) {
		this.followLinks = followLinks;
	}

	public void setOutPath(File outPath) {
		System.err.println("set output dir=" + outPath);
		this.outPath = outPath;
	}

	private void addTag(String tag) {
		if (tag != null) {
			Integer val = tagSet.get(tag);
			if (val == null)
				val = Integer.valueOf(1);
			else
				val = Integer.valueOf(val.intValue() + 1);
			tagSet.put(tag, val);
		}
	}

	private void dumpTags() {
		System.out.flush();
		Integer docCnt = tagSet.get(DOCUMENT); Integer fldCnt = tagSet.get(FOLDER);
		if ((docCnt == null || docCnt == 1) && (fldCnt == null || fldCnt == 1)) {
			// if have only one document and/or folder then omit these
			tagSet.remove(DOCUMENT); tagSet.remove(FOLDER);
		}
		for (Map.Entry<String,Integer> entry: tagSet.entrySet()) {
			System.out.format("\t%-20s %d%n", entry.getKey(), entry.getValue());
		}
		System.out.flush();
	}

	private void checkReader(KmlReader reader, String name) {
		KmlWriter writer = getWriter(name);
		features = 0;
		try {
			IGISObject gisObj;
			while ((gisObj = reader.read()) != null) {
				checkObject(gisObj);
				if (writer != null) {
					KmlWriter.normalizeUrls(gisObj);
					writer.write(gisObj);
				}
			}
		} catch (IOException e) {
			dump(e);
		} finally {
			reader.close();
			if (writer != null)
				writer.close();
		}

		if (followLinks) {
			List<URI> networkLinks = reader.getNetworkLinks();
			if (networkLinks.size() != 0)
				reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
					public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
						checkObject(gisObj);
						return true;
					}
				});
		}
		
		dumpTags();
		System.out.println("\t# features=" + features);
		System.out.println();
		tagSet.clear();
	}

	private KmlWriter getWriter(String name) {
		if (outPath != null) {
			if (!outPathCheck) {
				if (!outPath.exists() && !outPath.mkdirs()) {
					System.err.println("*** ERROR: Failed to create outputPath: " + outPath);
					outPath = null; // don't bother with the output
					return null;
				}
				outPathCheck = true; // don't need to check again				
			}
			try {
				String lowerCaseName = name.toLowerCase();
				if (!lowerCaseName.endsWith(".kml") && !lowerCaseName.endsWith(".kmz"))
					name += ".kml";
				return new KmlWriter(new File(outPath, name));
			} catch (IOException e) {
				System.err.println("*** ERROR: Failed to create output: " + name);
				e.printStackTrace();
			}
		}
		return null;
	}

	private void checkObject(IGISObject gisObj) {
		features++;
		if (gisObj instanceof NetworkLink) {
			addTag(NETWORK_LINK);
			checkFeature((Feature) gisObj);
		} else if (gisObj instanceof Overlay) {
			checkFeature((Feature) gisObj);
			addTag(gisObj.getClass());
		} else if (gisObj instanceof ContainerStart) {
			addTag(((ContainerStart) gisObj).getType()); // Documemnt | Folder
		} else {
			Class cl = gisObj.getClass();
			if (cl == Feature.class) {
				Feature f = (Feature) gisObj;
				Geometry geom = f.getGeometry();
				addTag(PLACEMARK);
				checkFeature(f);
				if (geom != null) {
					Class geomClass = geom.getClass();
					if (geomClass == GeometryBag.class) {
						addTag(MULTI_GEOMETRY);
						for (Geometry g : (GeometryBag) geom) {
							if (g != null) addTag(g.getClass());
						}
					} else addTag(geomClass);
				}
			} else if (cl == Style.class ||
					cl == Schema.class ||
					cl == StyleMap.class ||
					cl == NetworkLinkControl.class) {
				addTag(cl);
				// ignore: DocumentStart + ContainerEnd + Comment
			} else if (cl != ContainerEnd.class && cl != DocumentStart.class && cl != Comment.class)
				System.err.println("*** other: " + gisObj.getClass().getName()); // note unhandled types for debugging
		}
	}

	private void checkFeature(Feature f) {
		Date startTime = f.getStartTime();
		Date endTime = f.getEndTime();
		if (startTime != null || endTime != null) {
			if (startTime != null && startTime.equals(endTime)) {
				 // if start == stop then assume timestamp/when -- no way to determine if TimeSpan was used with start=end=timestamp
				addTag(TIME_STAMP);
			} else {
				// otherwise timespan used with start and/or end dates
				addTag(TIME_SPAN);
			}
		}
		// otherwise don't have timestamp or timeSpans
	}

	private void addTag(Class aClass) {
		if (aClass != null) {
			String name = aClass.getName();
			int ind = name.lastIndexOf('.');
			if (ind > 0) {
				name = name.substring(ind + 1);
				addTag(name);
			}
		}
	}

	private static void dump(IOException e) {
		String msg = e.getMessage();
		if (msg != null)
			System.out.println("\t*** " + e.getClass().getName() + ": " + msg);
		else {
			System.out.println("\t*** " + e.getClass().getName());
			e.printStackTrace();
		}
	}

	public static void usage() {
		System.out.println("Usage: java KmlMetaDump [options] <file, directory, or URL..>");
		System.out.println("\nOptions:");
		System.out.println("\t-o<path-to-output-directory>");
		System.out.println("\t-f Follow networkLinks and loads networkLinks and add features to stats");
		System.exit(1);
	}	

	public static void main (String args[]) {
		KmlMetaDump app = new KmlMetaDump();

		List<String> sources = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (arg.startsWith("-o") && arg.length() > 2)
					app.setOutPath(new File(arg.substring(2)));
				else if (arg.equals("-f"))
					app.setFollowLinks(true);
				else usage();
			} else
				sources.add(arg);
		}

		if (sources.size() == 0) usage();

		for (String arg : sources) {
			try {
				if (arg.startsWith("http:") || arg.startsWith("file:")) {
					URL url = new URL(arg);
					app.checkSource(url);
				} else {
					File f = new File(arg);
					if (f.exists())
						app.checkSource(f);
					else
						app.checkSource(new URL(arg));
				}
			} catch (MalformedURLException e) {
				System.out.println(arg);
				System.out.println("\t*** " + e.getMessage());
				System.out.println();
			} catch (IOException e) {
				dump(e);
				System.out.println();
			}
		}

	  }

}

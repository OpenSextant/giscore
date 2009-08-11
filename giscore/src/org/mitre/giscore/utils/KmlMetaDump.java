package org.mitre.giscore.utils;

import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.output.kml.KmlWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple KML Debugging Tool to import KML/KMZ documents by File or URL and dump statistics
 * on number of feature elements (Placemarks, Points, Polygons, LineStrings, NetworkLinks, etc.)
 * and optionally export the same KML to a file to verify all content has been correctly
 * parsed. <p/>
 *
 * Notes following conditions if found:
 * <ul>
 *  <li> Features inherit time from parent container
 *  <li> NetworkLink has missing or empty HREF
 * </ul>
 * 
 * This tool helps to uncover issues in reading and writing target KML files.
 * Some KML files fail to parse and those cases are almost always those that don't
 * conform to the appropriate KML XML Schema or follow the KML Reference Spec (see
 * http://code.google.com/apis/kml/documentation/kmlreference.html) such
 * as in coordinates element which states "Do not include spaces between the
 * three values that describe a coordinate", etc. <p/>
 *
 * If logger at at debug level then all info, warnings and parsing messages will be logged. 
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
    private boolean verbose;
    private Class lastObjClass;
    private Date containerStartDate;
    private Date containerEndDate;

    public void checkSource(URL url) throws IOException {
		System.out.println(url);
		processKmlSource(new KmlReader(url), null, url.getFile());
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
			processKmlSource(new KmlReader(file), file, file.getName());
		}
	}

	public void setFollowLinks(boolean followLinks) {
		this.followLinks = followLinks;
	}

	public void setOutPath(File outPath) {
		System.err.println("set output dir=" + outPath);
		this.outPath = outPath;
	}

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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
        System.out.println();
		Integer docCnt = tagSet.get(DOCUMENT); Integer fldCnt = tagSet.get(FOLDER);
		if ((docCnt == null || docCnt == 1) && (fldCnt == null || fldCnt == 1)) {
			// if have only one document and/or folder then omit these
			tagSet.remove(DOCUMENT); tagSet.remove(FOLDER);
		}
		for (Map.Entry<String,Integer> entry: tagSet.entrySet()) {
				String key = entry.getKey();
				// message/warnings start with : prefix, otherwise show key + count
				if (key.startsWith(":"))
					System.out.println("\t" + key.substring(1));
				else
					System.out.format("\t%-20s %d%n", key, entry.getValue());
		}
		System.out.flush();
	}

    /**
     * Process KML Source reading each feature and dump out stats when done
     * @param reader KmlReader
     * @param file File if checking File source, other null if URL source
     * @param name Name part of KML file or URL
     */
	private void processKmlSource(KmlReader reader, File file, String name) {
		KmlWriter writer = getWriter(file, name);
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
			dumpException(e);
		} finally {
			reader.close();
			if (writer != null)
				writer.close();
		}

		if (followLinks) {
			List<URI> networkLinks = reader.getNetworkLinks();
			if (networkLinks.size() != 0)
				reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
                    private URI last;
					public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
                        URI uri = ref.getURI();
                        if (verbose && !uri.equals(last)) {
                            System.out.println("Check NetworkLink: " +
                                    (ref.isKmz() ? ref.getKmzRelPath() : uri.toString()));
                            System.out.println();
                            last = uri;
                        }
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

	private KmlWriter getWriter(File file, String name) {
		if (outPath != null) {
			if (!outPathCheck) {
				if (!outPath.exists() && !outPath.mkdirs()) {
					System.err.println("*** ERROR: Failed to create outputPath: " + outPath);
					outPath = null; // don't bother with the output again
					return null;
				}
				outPathCheck = true; // don't need to check again				
			}
			try {
				String lowerCaseName = name.toLowerCase();
				if (!lowerCaseName.endsWith(".kml") && !lowerCaseName.endsWith(".kmz"))
					name += ".kml";
                File out = new File(outPath, name);
                /*
                // check to not overwrite input file
                try {
                    if (file.getCanonicalFile().equals(out.getCanonicalFile())) {
                        System.err.println("*** ERROR: output cannot overwrite input");
                        return null;
                    }
                } catch(IOException e) {
                    if (file.getAbsoluteFile().equals(out.getAbsoluteFile())) {
                        System.err.println("*** ERROR: output cannot overwrite input");
                        return null;
                    }
                }
                */
                if (out.exists()) {
                    System.err.println("*** WARNING: target file " + out + " exists");
                    return null;
                }
                return new KmlWriter(out);
			} catch (IOException e) {
				System.err.println("*** ERROR: Failed to create output: " + name);
				e.printStackTrace();
			}
		}
		return null;
	}

	private void checkObject(IGISObject gisObj) {
        if (verbose) System.out.println(gisObj);
		features++;
		if (gisObj instanceof NetworkLink) {
			checkNetworkLink((NetworkLink)gisObj);
			addTag(NETWORK_LINK);
		} else if (gisObj instanceof Overlay) {
            Overlay ov = (Overlay)gisObj;
            addTag(ov.getClass());
			checkFeature(ov);
            if (ov.getIcon() == null)
                addTag(":Overlay missing icon");
		} else if (gisObj instanceof ContainerStart) {
            ContainerStart cs = (ContainerStart)gisObj;
            containerStartDate = cs.getStartTime();
            containerEndDate = cs.getEndTime();
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
			} else if (cl == ContainerEnd.class) {
                // Note: need to clear container time when matching ContainerEnd is found
                // if container with time has several child containers then we simply
                // clear inherited dates on first container end found but should match
                // one associated with ContainerStart having the time. Could use stack
                // to keep track or keep track of depth. For purpose of debugging
                // it doesn't matter.
                containerStartDate = null;
                containerEndDate = null;
            } else if (cl != DocumentStart.class && cl != Comment.class) {
                // ignore: DocumentStart + Comment objects
                addTag(cl); // e.g. Style, Schema, StyleMap, NetworkLinkControl
            }
		}
        lastObjClass = gisObj.getClass();
	}

	private void checkNetworkLink(NetworkLink networkLink) {
		TaggedMap link = networkLink.getLink();
		if (link != null) {
            String href = link.get(HREF);
			if (href == null)
				addTag(":NetworkLink missing or empty HREF");
			else {
                String url;
                try {
                    UrlRef urlRef = new UrlRef(new URI(href));
                    url = urlRef.isKmz() ? urlRef.getKmzRelPath() : urlRef.toString();
                } catch (MalformedURLException e) {
                    url = href;
                } catch (URISyntaxException e) {
                    url = href;
                }
                addTag(":url=" + url);
            }
		}
		else
			addTag(":NetworkLink missing Link");
		checkFeature(networkLink);
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
        else if (containerStartDate != null || containerEndDate != null) {
            addTag(":Container overrides feature time");
        }
        // otherwise feature doesn't have timeStamp or timeSpans

        if (f.getViewGroup() != null)
            addTag(LOOK_AT);

        //if (lastObj instanceof StyleSelector) {
        if (lastObjClass == Style.class || lastObjClass == StyleMap.class)
            addTag(":Feature uses inline " + getClassName(lastObjClass)); // Style or StyleMap
	}

	private void addTag(Class aClass) {
        String tag = getClassName(aClass);
        if (tag != null) addTag(tag);
	}

    private static String getClassName(Class aClass) {
        if (aClass != null) {
			String name = aClass.getName();
			int ind = name.lastIndexOf('.');
			if (ind > 0) {
				name = name.substring(ind + 1);
                return name;
			}
            return name;
		}
        return null;
    }

    private static void dumpException(IOException e) {
		String msg = e.getMessage();
		if (msg != null)
			System.out.println("\t*** " + e.getClass().getName() + ": " + msg);
		else {
			System.out.println("\t*** " + e.getClass().getName());
			e.printStackTrace(System.out);
		}
	}

	public static void usage() {
		System.out.println("Usage: java KmlMetaDump [options] <file, directory, or URL..>");
        System.out.println("\nIf a directory is choosen that all kml/kmz files in any subfolder will be examined");
		System.out.println("\nOptions:");
		System.out.println("\t-o<path-to-output-directory> Writes KML/KMZ to file in specified directory");
        System.out.println("\t\tusing same base file as original file.");
        System.out.println("\t\tFiles with same name in target location will be skipped as NOT to overwrite anything.");
		System.out.println("\t-f Follow networkLinks: recursively loads content from NetworkLinks");
		System.out.println("\t\tand adds features to resulting statistics");
        System.out.println("\t-v Set verbose which dumps out features");
		System.exit(1);
	}	

	public static void main (String args[]) {
		KmlMetaDump app = new KmlMetaDump();

		List<String> sources = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (arg.startsWith("-o") && arg.length() > 2)
					app.setOutPath(new File(arg.substring(2)));
				else if (arg.startsWith("-f"))
					app.setFollowLinks(true);
                else if (arg.startsWith("-v"))
					app.setVerbose(true);
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
					if (f.exists()) {
						try {
							f = f.getCanonicalFile();
						} catch (IOException e) {
							// ignore
						}
						app.checkSource(f);
					}
					else
						app.checkSource(new URL(arg));
				}
			} catch (MalformedURLException e) {
				System.out.println(arg);
				System.out.println("\t*** " + e.getMessage());
				System.out.println();
			} catch (IOException e) {
				dumpException(e);
				System.out.println();
			}
        }
    }

}

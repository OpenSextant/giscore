package org.mitre.giscore.utils;

import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.giscore.output.kml.KmlOutputStream;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple KML Debugging Tool to read KML/KMZ documents by File or URL and dump statistics
 * on number of feature elements (Placemarks, Points, Polygons, LineStrings, NetworkLinks, etc.)
 * and properties (ExtendedData, Schema, etc.) and optionally export the same KML to a
 * file (or stdout) to verify all content has been correctly interpreted. <p/>
 *
 * Notes following conditions if found:
 * <ul>
 *  <li> Feature inherits time from parent container
 *  <li> NetworkLink has missing or empty HREF
 *  <li> Overlay does not contain Icon element
 *  <li> Invalid TimeSpan if begin later than end value
 *  <li> End container with no matching start container
 *  <li> Starting container tag with no matching end container
 * </ul>
 * 
 * This tool helps to uncover issues in reading and writing target KML files.
 * Some KML files fail to parse and those cases are almost always those that don't
 * conform to the appropriate KML XML Schema or strictly follow the OGC KML standard
 * or the KML Reference Spec (see
 * http://code.google.com/apis/kml/documentation/kmlreference.html) such
 * as in coordinates element which states "Do not include spaces between the
 * three values that describe a coordinate", etc. Likewise, the OGC KML Best
 * Practices and KML Test Suite have additional restrictions. <p/>
 *
 * If logger is at debug level then all info, warnings and parsing messages will be logged. 
 * 
 * @author Jason Mathews, MITRE Corp.
 * Created: May 20, 2009 12:05:04 PM
 */
public class KmlMetaDump implements IKml {

	private boolean followLinks;
	private File outPath;
	private boolean outPathCheck;
	private int features;
	private boolean verbose;
	private Class lastObjClass;
	
	private boolean inheritsTime;
	private Date containerStartDate;
	private Date containerEndDate;
	private final Stack<ContainerStart> containers = new Stack<ContainerStart>();

	/**
	 * count of number of KML resources were processed and stats were tallied
	 * this means the number of times the tagSet keys were dumped into the totals set
	 * if dumpConut == 1 then the totals the same the single tagSet dumped.
 	 */
	private int dumpCount;

	private final Map<String,Integer> tagSet = new java.util.TreeMap<String,Integer>();
	private final Set<String> totals = new TreeSet<String>();
	private boolean useStdout;

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

	public void setUseStdout(boolean useStdout) {
		this.useStdout = useStdout;
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
		totals.addAll(tagSet.keySet()); // accumulate total tag set
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
			if (writer != null) {
				// if stdout then don't close System.out stream
				if (useStdout)
					writer.close(false);
				else
					writer.close();
			}
		}
		if (!containers.isEmpty())
			addTag(":Starting container tag with no matching end container");
		
		resetSourceState();

		if (followLinks) {
			List<URI> networkLinks = reader.getNetworkLinks();
			if (networkLinks.size() != 0) {
				reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
                    private URI last;
					public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
                        URI uri = ref.getURI();
                        if (verbose && !uri.equals(last)) {
							// first gisObj found from a new KML source 
                            System.out.println("Check NetworkLink: " +
                                    (ref.isKmz() ? ref.getKmzRelPath() : uri.toString()));
                            System.out.println();
                            last = uri;
							resetSourceState();
                        }
						checkObject(gisObj);
						return true;
					}
				});
			}
			resetSourceState();
		}

		dumpTags();
		System.out.println("\t# features=" + features);
		System.out.println();
		tagSet.clear();
		dumpCount++;
	}

	private void resetSourceState() {
		containers.clear();
		inheritsTime = false;
		containerStartDate = null;
		containerEndDate = null;
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

		if (useStdout) {
			try {
				KmlOutputStream kos = new KmlOutputStream(System.out);
				System.out.println();
				return new KmlWriter(kos);
			} catch (XMLStreamException e) {
				System.err.println("*** ERROR: Failed to create stdout outputStream");
				e.printStackTrace();
			}
		}

		return null;
	}

	private void checkObject(IGISObject gisObj) {
        if (verbose) System.out.println(gisObj);
        features++;
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
                    checkBag((GeometryBag) geom);
                } else addTag(geomClass);
            }
        } else if (cl == NetworkLink.class) {
            NetworkLink networkLink = (NetworkLink) gisObj;
            checkNetworkLink(networkLink);
            checkFeature(networkLink);
            addTag(NETWORK_LINK);
            // isn't NetworkLink like a Container where child features are affected by
            // properties of parent NetworkLink such as time, region, etc.
        } else if (cl == ContainerStart.class) {
            ContainerStart cs = (ContainerStart) gisObj;
            addTag(((ContainerStart) gisObj).getType()); // Documemnt | Folder
            containers.push(cs);
            Date startTime = cs.getStartTime();
            Date endTime = cs.getEndTime();
            if (startTime != null || endTime != null) {
                //
                // Features override TimePrimitives if defined in ancestor containers.
                // Features without time inherit time from their ancestors.
                //
                // "Feature elements shall be inherited by all Feature members of a hierarchy: atom:author, atom:link, Region,
                // and [TimePrimitive], unless overruled by the presence of such elements locally. Thus it is not necessary
                // for a child Feature to carry any of these elements where their local value is the same as that of its
                // parent Feature. Inheritance of these elements continues to any depth of nesting, but if overruled by
                // a local declaration, then the new value is inherited by all its children in turn.
                //
                // Source: OGC KML Best Practices document OGC 07-113r1
                //
                inheritsTime = true;
                if (verbose) System.out.println(cs.getType() + " container has time");
                if (startTime != null) {
                    if (endTime != null && startTime.compareTo(endTime) > 0) {
                        // assertion: the begin value is earlier than the end value.
                        // if fails then fails OGC KML test suite: ATC 4: TimeSpan [OGC-07-147r2: cl. 15.2.2]
                        addTag(":Invalid time range: start > end");
                        if (verbose) System.out.println(" Error: Invalid time range: start > end\n");
                    }
                    if (containerStartDate != null && verbose) System.out.println(" Overriding parent container start date");
                    // override any previous start date
                    containerStartDate = startTime;
                    // log.debug("use container start date");
                }
                if (endTime != null) {
                    if (containerEndDate != null && verbose) System.out.println(" Overriding parent container end date");
                    // override any previous end date
                    // log.debug("use container end date");
                    containerEndDate = endTime;
                }
            }
        } else if (cl == ContainerEnd.class) {
            //
            // when ContainerEnd is found then we pop the last containerStart found
            // and re-check if other containers still have inheritable time.
            //
            // Example:
            //
            // Folder1 [time]
            //     Folder2
            //         placemark1 *[inherits time from folder1]
            //     end folder2
            //     Folder3
            //         placemark2 *[inherits time from folder1]
            //     end folder3
            //  end folder1
            //
            if (!containers.empty()) {
                ContainerStart cs = containers.pop();
                if (verbose) System.out.println(containers.size() + "-end container " + cs.getType());
            } else {
                addTag(":end container with no matching start container");
            }

            if (inheritsTime) {
                // reset times with last start/end times in the hierachy if any is present
                inheritsTime = false;
                containerStartDate = null;
                containerEndDate = null;
                for (ContainerStart cs : containers) {
                    Date startDate = cs.getStartTime();
                    Date endDate = cs.getEndTime();
                    if (startDate != null || endDate != null) {
                        containerStartDate = startDate;
                        containerEndDate = endDate;
                    }
                } // for each container
                if (containerStartDate != null || containerEndDate != null) {
                    // log.info("Container has inheritable time");
                    inheritsTime = true;
                }
            }
        } else if (cl == Style.class) {
            addTag(cl);
            Style s = (Style) gisObj;
            if (s.hasBalloonStyle())
                addTag(IKml.BALLOON_STYLE);
            if (s.hasIconStyle())
                addTag(IKml.ICON_STYLE);
            if (s.hasLabelStyle())
                addTag(IKml.LABEL_STYLE);
            if (s.hasLineStyle())
                addTag(IKml.LINE_STYLE);
            if (s.hasPolyStyle())
                addTag(IKml.POLY_STYLE);
            // giscore does not support ListStyle
        } else if (gisObj instanceof Overlay) {
            Overlay ov = (Overlay) gisObj;
            addTag(ov.getClass());
            if (ov instanceof GroundOverlay) {
                GroundOverlay go = (GroundOverlay) ov;
                if (go.getNorth() != null || go.getSouth() != null
                        || go.getEast() != null || go.getWest() != null
                        || go.getRotation() != null)
                    addTag(IKml.LAT_LON_BOX);
            }
            checkFeature(ov);
            if (ov.getIcon() == null)
                addTag(":Overlay missing icon");
        } else if (cl != DocumentStart.class && cl != Comment.class) {
            // ignore: DocumentStart + Comment objects
            addTag(cl); // e.g. Schema, StyleMap, NetworkLinkControl
        }
        lastObjClass = gisObj.getClass();
	}

	private void checkBag(GeometryBag geometryBag) {
		for (Geometry g : geometryBag) {
			if (g != null) {
				Class gClass = g.getClass();
				if (gClass == GeometryBag.class)
					checkBag((GeometryBag)g);
				else
					addTag(gClass);
			}
		}
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
				if (startTime != null && endTime != null && startTime.compareTo(endTime) > 0) {
					// assertion: the begin value is earlier than the end value.
					// if fails then fails OGC KML test suite: ATC 4: TimeSpan [OGC-07-147r2: cl. 15.2.2]
					addTag(":Invalid time range: start > end");
					if (verbose) System.out.println(" Error: Invalid time range: start > end\n");
				}
			}
		} else if (containerStartDate != null || containerEndDate != null) {
            /*
                Features with no time properties inherit the time
                of its ancestors if they have time constraints.
            */
			addTag(":Feature inherits container time");
		}
        // otherwise feature doesn't have timeStamp or timeSpans

		if (f.hasExtendedData())
			addTag(EXTENDED_DATA);

		TaggedMap viewGroup = f.getViewGroup();
		if (viewGroup != null) {
            addTag(viewGroup.getTag()); // Camera or LookAt
		}

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
		System.out.println("\t-stdout Write KML output to STDOUT instead of writing files");
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
				else if (arg.startsWith("-stdout"))
					app.setUseStdout(true);
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
		
		if (app.dumpCount > 1 && !app.totals.isEmpty()) {
			System.out.println("Summary: count=" + app.dumpCount + "\n");
			for (String tag : app.totals) {
				// message/warnings start with : prefix, otherwise show key + count
				
				if (tag.startsWith(":")) tag = tag.substring(1);
				System.out.println("\t" + tag);
			}
		}
    }

}

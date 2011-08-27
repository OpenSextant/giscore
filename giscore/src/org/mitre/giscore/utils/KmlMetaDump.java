/****************************************************************************************
 *  KmlMetaDump.java
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.output.atom.IAtomConstants;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.mitre.itf.geodesy.Geodetic2DPoint;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Simple KML Debugging Tool to read KML/KMZ documents by File, URL or recursively
 * by directory and dump statistics on number of feature elements (Placemarks,
 * Points, Polygons, LineStrings, NetworkLinks, etc.) and properties (ExtendedData,
 * Schema, etc.) and optionally export the same KML to a file (or stdout) to
 * verify all content has been correctly interpreted.
 * <p/>
 * Parsing also includes support and validation for gx KML extensions (e.g. MultiTrack, Track, etc.)
 * <p/>
 *
 * Attempts to validate some of the OGC KML 2.2 specification rules and OGC KML Best Practices
 * (defined in OGC KML 2.2 Abstract Test Suite), which are not possible using standard
 * XML Schema validation practices using kml22.xsd and DOM validation.
 *
 * Lists following conditions if found:
 * <ul>
 *  <li> Camera altitudeMode cannot be clampToGround [ATC 54.2] (warning)
 *  <li> comma found instead of whitespace between tuples (error)
 *  <li> Container end date is later than that of its ancestors (info)
 *  <li> Container start date is earlier than that of its ancestors (info)
 *  <li> Document must explicitly reference a shared style (error)
 *  <li> End container with no matching start container (error)
 *  <li> Feature inherits time from parent container (info)
 *  <li> Feature uses inline [Style|StyleMap) (info)
 *  <li> Feature uses merged shared/inline Style (info)
 *  <li> Feature uses shared Style (info)
 *  <li> gx:SimpleArrayData has incorrect length (error)
 *  <li> gx:Track coord-when mismatch (error)
 *  <li> ignore invalid character in coordinate string (error)
 *  <li> ignore invalid string in coordinate (error)
 *  <li> Invalid LookAt values (error)
 *  <li> Invalid tilt value in LookAt [ATC 38.2] (error)
 *  <li> Invalid time range: start > end (error)
 *  <li> Invalid TimeSpan if begin later than end value (warning)
 *  <li> Invalid ViewGroup tag: XXX (warn)
 *  <li> LatLonAltBox appears to be very small area (warning)
 *  <li> LatLonAltBox fails to satisfy Altitude constraint (minAlt <= maxAlt) [ATC 8.3] (error)
 *  <li> LatLonAltBox fails to satisfy constraint (altMode != clampToGround) [ATC 8.4] (warning)
 *  <li> LatLonAltBox fails to satisfy constraints [ATC 8] (warning)
 *  <li> Max NetworkLink count exceeded (info)
 *  <li> minLodPixels must be less than maxLodPixels in Lod [ATC 39] (error)
 *  <li> Missing altitude in LookAt [ATC 38.3] (warning)
 *  <li> NetworkLink missing Link (info)
 *  <li> NetworkLink missing or empty HREF (info)
 *  <li> Out of order elements (error)
 *  <li> Overlay does not contain Icon element (info)
 *  <li> Region has invalid LatLonAltBox [ATC 8] (error)
 *  <li> Region has invalid LatLonAltBox: non-numeric value (error)
 *  <li> Region has invalid Lod: non-numeric value (error)
 *  <li> Shared styles in Folder not allowed [ATC 7] (warning)
 *  <li> Shared styles must have 'id' attribute [ATC 7] (warning)
 *  <li> Starting container tag with no matching end container (error)
 *  <li> StyleUrl has absolute URL (info)
 *  <li> StyleUrl has relative URL (info)
 *  <li> StyleUrl must contain '#' with identifier reference (error)
 *  <li> StyleMap Pair must contain StyleUrl or Style
 *  <li> StyleMap Pair with absolute StyleUrl (info)
 *  <li> StyleMap has inline Style (info)
 *  <li> Suspicious Pair id characters (warning)
 *  <li> Suspicious Schema id characters (warning)
 *  <li> Suspicious Schema name characters (warning)
 *  <li> Suspicious Style id characters (warning)
 *  <li> Suspicious StyleMap [normal|highlight] URL characters (warning)
 *  <li> Suspicious StyleMap id characters (warning)
 *  <li> Suspicious styleUrl characters (warning)
 *  <li> Unknown Track element: XXX (warning)
 * </ul>
 * Geometry checks: <br>
 * <ul>
 *  <li> Bad poly found, no outer ring (error)
 *  <li> Geometry spans -180/+180 longitude line (dateline wrap or antimeridian spanning problem) (warn)
 *  <li> GroundOverlay fails to satisfy east > west constraint [ATC 11] (warn)
 *  <li> GroundOverlay fails to satisfy north > south constraint [ATC 11] (warn)
 *  <li> GroundOverlay spans -180/+180 longitude line (info)
 *  <li> Inner ring clipped at DateLine (info)
 *  <li> Inner ring not contained within outer ring (warn)
 *  <li> Inner rings in Polygon must not overlap with each other (warn)
 *  <li> Line clipped at DateLine (info)
 *  <li> [Line|Inner/Outer Ring|LinearRing] has duplicate consecutive points (warn)
 *  <li> LinearRing cannot self-intersect (warn)
 *  <li> LinearRing must start and end with the same point (error)
 *  <li> Nested MultiGeometries (info)
 *  <li> Outer ring clipped at DateLine (info)
 * </ul>
 *
 * This tool helps to uncover issues in reading and writing target KML files.
 * Some KML files fail to parse and those cases are almost always those that don't
 * conform to the appropriate KML XML Schema or strictly follow the OGC KML standard
 * or the KML Reference Spec (see
 * http://code.google.com/apis/kml/documentation/kmlreference.html) such
 * as in coordinates element which states "Do not include spaces between the
 * three values that describe a coordinate", etc. Likewise, the OGC KML Best
 * Practices and KML Test Suite have additional restrictions some of which
 * are being checked. <p>
 *
 * If logger is at debug level then all info, warnings and parsing messages will be logged. <p>
 *
 * ATC x-x errors/warnings reference those defined in the OGC KML 2.2 Abstract Test Suite
 * Reference OGC 07-134r2 available at http://www.opengeospatial.org/standards/kml
 *
 * @author Jason Mathews, MITRE Corp.
 * Created: May 20, 2009 12:05:04 PM
 */
public class KmlMetaDump implements IKml {

	private boolean followLinks;
	private int maxLinkCount = 500;
	private File outPath;
	private boolean outPathCheck;
	private int features;
	private boolean verbose;

	private boolean inheritsTime;
    private boolean autoGenerateUniqueNames;
	private Date containerStartDate;
	private Date containerEndDate;
	private final Stack<ContainerStart> containers = new Stack<ContainerStart>();

	private Set<String> simpleFieldSet;

	/**
	 * count of number of KML resources were processed and stats were tallied
	 * this means the number of times the tagSet keys were dumped into the totals set
	 * if dumpCount == 1 then the totals the same the single tagSet dumped.
 	 */
	private int dumpCount;

	private final Map<String,Integer> tagSet = new java.util.TreeMap<String,Integer>();
	private final Set<String> totals = new TreeSet<String>();
	private boolean useStdout;

	private static final String CLAMP_TO_GROUND = "clampToGround";

	public KmlMetaDump() {
		try {
			//Get the root logger
			Logger root = Logger.getRootLogger();
			if (root != null) {
				MetaAppender appender = new MetaAppender();
				appender.setThreshold(Level.WARN);
				root.addAppender(appender);
			}
		} catch (Exception e) {
			//ignore
		}
	}

	public void checkSource(URL url) throws IOException {
		System.out.println(url);
		String name = url.getFile();
		// TODO: if url ends with '/' and servlet/etc. responding with KML then need appropriate name to save locally
		// name may be an empty string
		int ind = name.lastIndexOf('/');
		if (ind > 0) name = name.substring(ind + 1); // strip off path
		processKmlSource(new KmlReader(url), name);
	}

	public void checkSource(File file) {
		if (file.isDirectory()) {
			// System.out.println("XXX: check dir: " + file);//debug
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
			if (file.length() == 0) {
				System.out.println("\t*** Skip zero length file");
				return;
			}
			try {
				processKmlSource(new KmlReader(file), file.getName());
			} catch (IOException e) {
				dumpException(e);
				System.out.println();
			}
		}
	}

	public Set<String> getSimpleFieldSet() {
		return simpleFieldSet;
	}

	public void useSimpleFieldSet() {
		simpleFieldSet = new TreeSet<String>();
	}

    /**
     * Get tag set for last KML resource processed
     */
   public Map<String, Integer> getTagSet() {
       return tagSet;
   }

	public Set<String> getTotals() {
		return totals;
	}

	public void setFollowLinks(boolean followLinks) {
		this.followLinks = followLinks;
	}

	public void setMaxLinkCount(int maxLinkCount) {
		this.maxLinkCount = maxLinkCount <= 0 ? Integer.MAX_VALUE : maxLinkCount;
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

    public boolean isAutoGenerateUniqueNames() {
        return autoGenerateUniqueNames;
    }

    public void setAutoGenerateUniqueNames(boolean autoGenerateUniqueNames) {
        this.autoGenerateUniqueNames = autoGenerateUniqueNames;
    }

	private void addTag(String tag) {
		addTag(tag, false);
	}

	private void addTag(String tag, boolean verboseMode) {
		if (tag != null) {
			Integer val = tagSet.get(tag);
			if (val == null)
				val = Integer.valueOf(1);
			else
				val = Integer.valueOf(val.intValue() + 1);
			tagSet.put(tag, val);
            if ("Style".equals(tag))
                System.out.println("XXX: style XXX");
			if (verbose && verboseMode) {
				if (tag.startsWith(":")) tag = tag.substring(1);
				System.out.printf(" %s%n", tag);
			}
		}
	}

	private void dumpTags() {
		System.out.flush();
		System.out.println();
		Integer docCnt = tagSet.get(DOCUMENT); Integer fldCnt = tagSet.get(FOLDER);
		boolean metaProps = false;
		if ((docCnt == null || docCnt == 1) && (fldCnt == null || fldCnt == 1)) {
			// if have only one document and/or folder then omit these
			tagSet.remove(DOCUMENT); tagSet.remove(FOLDER);
		}
		for (Map.Entry<String,Integer> entry: tagSet.entrySet()) {
			String key = entry.getKey();
			// message/warnings start with : prefix, otherwise show key + count
			if (key.startsWith(":")) {
				System.out.println("\t" + key.substring(1));
				metaProps = true;
			} else {
				if (metaProps) {
					// if last property was a message/warnings then
					// print new line to separate the two groups of items
					System.out.println("\t--");
					metaProps = false;
				}
				System.out.format("\t%-20s %d%n", key, entry.getValue());
			}
		}
		totals.addAll(tagSet.keySet()); // accumulate total tag set
		System.out.flush();
	}

    /**
     * Process KML Source reading each feature and dump out stats when done
     * @param reader KmlReader
     * @param name Name part of KML file or URL
     */
	private void processKmlSource(KmlReader reader, String name) {
        tagSet.clear(); // clear tags
		reader.setMaxLinkCount(maxLinkCount);
        features = 0;
		KmlWriter writer = getWriter(reader, name);
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
			addTag(":Starting container tag with no matching end container", true);
		
		resetSourceState();

		if (followLinks) {
			List<URI> networkLinks = reader.getNetworkLinks();
			if (! networkLinks.isEmpty()) {
				reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
                    private URI last;
					public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
                        URI uri = ref.getURI();
						if (verbose && !uri.equals(last)) {
							// first gisObj found from a new KML source
							System.out.println("Check NetworkLink: " +
									(ref.isKmz() ? ref.getKmzRelPath() : uri.toString()));
							System.out.println();

							resetSourceState();
							last = uri;
						}
						checkObject(gisObj);
                        // Note imported network links are not dumped as KML
                        // if (writer != null && useStdout) writer.write(gisObj);
						return true;
					}
				});
				// following condition already intercepted via log4j handler
				//if (reader.isMaxLinkCountExceeded())
					//addTag(":Max NetworkLink count exceeded", true);
			}
			resetSourceState();
		}

		dumpTags();
        if (features != 0)
		    System.out.println("\t# features=" + features);
		System.out.println();		
		dumpCount++;
	}

	private void resetSourceState() {
		containers.clear();
		inheritsTime = false;
		containerStartDate = null;
		containerEndDate = null;
	}

	private KmlWriter getWriter(KmlReader reader, String name) {
		if (useStdout) {
			try {
				KmlOutputStream kos = new KmlOutputStream(System.out, reader.getEncoding());
				System.out.println();
				return new KmlWriter(kos);
			} catch (XMLStreamException e) {
				System.err.println("*** ERROR: Failed to create stdout outputStream");
                if (e.getCause() != null) e.getCause().printStackTrace();
				else e.printStackTrace();
				return null;
			}
		}

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
				if (StringUtils.isBlank(name)) {
					name = "out.kml";
				} else {
                    URL baseUrl = reader.getBaseUrl();
                    // check if name from URL with http parameters
                    if (baseUrl != null && !"file".equals(baseUrl.getProtocol())) {
                        int ind = name.indexOf('?');
                        if (ind > 0) name = name.substring(0, ind);
                    }
                    // replace non-valid and whitespace chars with _'s then collapse sequences of _'s into single _
                    // use inclusive vs exclusive regexp for valid file name.
                    name = name.replaceAll("[^A-Za-z0-9()._+\\-!]+", "_").replaceAll("_+", "_");
                    if (name.equals("_") || name.length() == 0) name = "out.kml"; // catch-all
                    String lowerCaseName = name.toLowerCase();
					if (!lowerCaseName.endsWith(".kml") && !lowerCaseName.endsWith(".kmz"))
						name += ".kml";
				}
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
                    if (autoGenerateUniqueNames) {
                        File origFile = out;
                        int ind = name.lastIndexOf('.');
                        String base, ext;
                        if (ind > 0) {
                            base = name.substring(0,ind);
                            ext = name.substring(ind);
                        } else {
                            base = name;
                            ext = ".kml";
                        }
                        // generate pseudo-random filename with unique prefix
                        for (int suffixMod = 10; ; suffixMod *= 2) {
                            out = new File(outPath, base + "-" + System.currentTimeMillis() % suffixMod + ext);
                            if (!out.exists()) break;
                        }
                        System.err.printf("*** INFO: target output file %s exists. Using %s%n", origFile, out.getName());
                    } else {
                        System.err.println("*** WARNING: target output file " + out + " exists");
                        return null;
                    }
                }
                return new KmlWriter(out, reader.getEncoding());
			} catch (IOException e) {
				System.err.println("*** ERROR: Failed to create output: " + name);
				if (e.getCause() != null) e.getCause().printStackTrace();
				else e.printStackTrace();
			}
		}

		return null;
	}

    private void checkObject(IGISObject gisObj) {
		final Class<? extends IGISObject> cl = gisObj.getClass();
        if (verbose) {
			// Style little too verbose
			if (cl == Style.class)
				System.out.println("Style id=" + ((Style)gisObj).getId());
			else
				System.out.println(gisObj);
		}
        if (cl == DocumentStart.class) return; // ignore DocumentStart root element.. contents dumped above

        if (gisObj instanceof Common) {
			 // Common -> Placemark + NetworkLink + Overlay {Screen/Ground/Photo}, Container {Folder/Document}
            checkCommon((Common)gisObj);
        }

        if (cl == Feature.class) {
            Feature f = (Feature) gisObj;
            Geometry geom = f.getGeometry();
            addTag(PLACEMARK);
            if (geom != null) {
                Class<? extends Geometry> geomClass = geom.getClass();
                if (geomClass == GeometryBag.class) {
                    addTag(MULTI_GEOMETRY);
                    checkBag((GeometryBag) geom); // handle MultiGeometry
                } else {
					addTag(geomClass);
					checkGeometry(geom); // Point, LineString, LinearRing, Polygon, Model
				}
            } else {
                checkElements(f); // check gx:Track, gx:MultiTrack geometries
            }
			/*
			final StyleSelector style = f.getStyle();
			if (style != null) {
				// Style defined within a Feature is called an "inline style"
                // and applies only to the Feature that contains it.
				addTag(":Placemark uses inline " + getClassName(style.getClass()), true); // Style or StyleMap
			}
			*/
        } else if (cl == NetworkLink.class) {
            NetworkLink networkLink = (NetworkLink) gisObj;
            checkNetworkLink(networkLink);
            addTag(NETWORK_LINK);
            // isn't NetworkLink like a Container where child features are affected by
            // properties of parent NetworkLink such as time, region, etc.
			/*
			final StyleSelector style = networkLink.getStyle();
			if (style != null) {
				// NetworkLinks with inline style: assumed allowed
				addTag(":NetworkLink uses inline " + getClassName(style.getClass()), true); // Style or StyleMap
			}
			*/
        } else if (cl == ContainerStart.class) {
            ContainerStart cs = (ContainerStart) gisObj;
            addTag(((ContainerStart) gisObj).getType()); // Document | Folder
            containers.push(cs);
			for (StyleSelector s : cs.getStyles()) {
				checkStyle(s, true);
			}
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
						if (verbose) System.out.println(" Error: Invalid time range: start > end");
					}
					if (containerStartDate != null) {
						if (verbose) System.out.println(" Overriding parent container start date");
						if (startTime.compareTo(containerStartDate) < 0)
							addTag(":Container start date is earlier than that of its ancestors", true);
					}
					// log.debug("use container start date");
				}
				// override any previous start date
				containerStartDate = startTime;
				if (endTime != null && containerEndDate != null) {
					if (verbose) System.out.println(" Overriding parent container end date");
					if (endTime.compareTo(containerEndDate) > 0)
						addTag(":Container end date is later than that of its ancestors", true);
				}
				// override any previous end date
				containerEndDate = endTime;
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
                addTag(":End container with no matching start container", true);
            }

            if (inheritsTime) {
                inheritsTime = false;
                containerStartDate = null;
                containerEndDate = null;
                // start at outer-most container and check if any container still defines time
                for (ContainerStart cs : containers) {
                    Date startDate = cs.getStartTime();
                    Date endDate = cs.getEndTime();
                    if (startDate != null || endDate != null) {
                        containerStartDate = startDate;
                        containerEndDate = endDate;
                        inheritsTime = true;
                    }
                } // for each container
		/*
                if (containerStartDate != null || containerEndDate != null) {
                    // log.info("Container has inheritable time");
                    inheritsTime = true;
                }
                */
            }
        } else if (gisObj instanceof StyleSelector) {
			// these are out of order styles
			// all in-sequence styles + styles should be part of the container they're contained in
			checkStyle((StyleSelector)gisObj, true);
        } else if (gisObj instanceof Overlay) {
            checkOverlay((Overlay) gisObj);
        } else if (cl == Element.class) {
            Element e = (Element)gisObj;
            String prefix = e.getPrefix();
            String name = e.getName();
            if (StringUtils.isEmpty(prefix)) prefix = "other"; 
			name = prefix + ":" + name;
            addTag(name);
        } else if (cl == Schema.class) {
            addTag(cl);
            Schema schema = (Schema)gisObj;
            String uri = schema.getId().toString();  // getId never null value
            if (!UrlRef.isIdentifier(uri)) {
                addTag(":Suspicious Schema id characters");
                if (verbose) System.out.println(" Warning: Schema id appears to contain invalid characters: " + uri);
            }
            String name = schema.getName(); // name never null or blank value
            if (!UrlRef.isIdentifier(name)) {
                addTag(":Suspicious Schema name characters");
                if (verbose) System.out.println(" Warning: Schema name may contain invalid characters: " + name);
            }
        } else if (cl != Comment.class) {
            // ignore: Comment objects but capture others
            addTag(cl); // e.g. NetworkLinkControl
        }
	}

    private void checkOverlay(Overlay ov) {
        addTag(ov.getClass());

        if (ov instanceof GroundOverlay) {
            GroundOverlay go = (GroundOverlay) ov;
            if (go.getNorth() != null || go.getSouth() != null
                    || go.getEast() != null || go.getWest() != null
                    || go.getRotation() != null) {
                addTag(IKml.LAT_LON_BOX);
                if (go.getEast() != null && go.getWest() != null) {
                    if (go.crossDateLine()) {
                        addTag(":GroundOverlay spans -180/+180 longitude line", true);
                        // e.g. west >= 0 && east < 0
                        // note associated bug:
                        // http://code.google.com/p/earth-issues/issues/detail?id=1145
                    }
                    // verify constraint: kml:east > kml:west
                    // Reference: OGC-07-147r2: cl. 11.3.2 ATC 11: LatLonBox
                    else if (go.getEast() <= go.getWest())
                        addTag(":GroundOverlay fails to satisfy east > west constraint [ATC 11]", true);
                }
                if (go.getNorth() != null && go.getSouth() != null && go.getNorth() <= go.getSouth())
                    addTag(":GroundOverlay fails to satisfy North > South constraint [ATC 11]", true);
            }
            AltitudeModeEnumType altMode = go.getAltitudeMode();
            if (altMode == AltitudeModeEnumType.relativeToSeaFloor || altMode == AltitudeModeEnumType.clampToSeaFloor)
                addTag("gx:altitudeMode");
        }

        TaggedMap icon = ov.getIcon();
        if (icon == null) {
            addTag(":Overlay missing icon", true);
        }
        /*
        else {
            // check for networkLink-like overlay
            String href = icon.get(HREF);
            if (StringUtils.isEmpty(href)) return; // no URL
            if (icon.get(HTTP_QUERY) != null || icon.get(VIEW_FORMAT) != null) {
               System.out.println("XXX: networkLink Overlay");
            }
        }
        */
        /*
        final StyleSelector style = ov.getStyle();
        if (style != null) {
            addTag(":" + getClassName(cl) + " uses inline " + getClassName(style.getClass()), true); // Style or StyleMap
        }
        */
    }

    private void checkStyle(StyleSelector style, boolean checkSharedStyle) {
		final Class<? extends StyleSelector> aClass = style.getClass();
		addTag(aClass);

		if (checkSharedStyle) {
			if (!containers.isEmpty()
					&& IKml.FOLDER.equals(containers.peek().getType())) {
				/*
				ATC 7: Shared style definition
				'shared' style definition (any element that may substitute for kml:AbstractStyleSelectorGroup)
				 satisfies all of the following constraints:
					-its parent element is kml:Document;
					-it has an 'id' attribute value.

				Reference: OGC Constraint OGC-07-147r2: cl. 6.4
				Shared styles shall only be encoded within a Document -> Not allowed in Folders

				http://code.google.com/apis/kml/documentation/kmlreference.html#document
				Do not put shared styles within a Folder.
				*/
				addTag(":Shared styles in Folder not allowed [ATC 7]");
				if (verbose) System.out.println(" Warning: Shared styles in Folder not allowed [ATC 7]");

				// if only one style in Folder and style id = null then by the strict rules
				// its an inline style (e.g. ListStyle) not a "shared" style and may be allowed.
				// Best practice is to use shared style defined in Document + referenced via styleUrl
			} else if (style.getId() == null) {
				// Google KML Reference:
				//  A style defined as the child of a <Document> is called a "shared style."
				//  A shared style must have an id defined for it.
				// KML spec:
				//  For a kml:Style or kml:StyleMap that applies to a kml:Document, the kml:Document itself
				//  must explicitly reference a shared style.
				addTag(":Document must explicitly reference a shared style");
				if (verbose) System.out.println(" Error: Document must explicitly reference a shared style. Inline styles only allowed in Placemarks");
			}
		}

		if (aClass == Style.class) {
			checkStyle((Style) style);
		} else if (aClass == StyleMap.class) {
			checkStyleMap((StyleMap) style);
		}
	}

	private void checkStyle(Style s) {
		String id = s.getId();
		if (id != null && !UrlRef.isIdentifier(id)) {
			addTag(":Suspicious Style id characters");
			if (verbose) System.out.println(" Warning: Style id appears to contain invalid characters: " + id);
			// id="hi+icon=http://maps.google.com/mapfiles/kml/shapes/poi.png" :: invalid chars => +=:/
			// id="earth/environmental_sciencePlacemark" :: invalid char => '/'
		}
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
		if (s.hasListStyle())
			addTag(IKml.LIST_STYLE);
	}

	private void checkStyleMap(StyleMap sm) {
		String id = sm.getId();
		if (id != null && !UrlRef.isIdentifier(id)) {
			addTag(":Suspicious StyleMap id characters");
			if (verbose) System.out.println(" Warning: StyleMap id appears to contain invalid characters: " + id);
		}

		// enumerate all StyleMap Pairs
		for (Iterator<Pair> it = sm.getPairs(); it.hasNext(); ) {
			Pair pair = it.next();
			if (pair != null) {
				final String key = pair.getKey(); // never null
				id = pair.getId();
				if (id != null && !UrlRef.isIdentifier(id)) {
					addTag(":Suspicious Pair id characters");
					if (verbose) System.out.println(" Warning: Pair id appears to contain invalid characters: " + id);
				}
				String styleUrl = pair.getStyleUrl();
				StyleSelector pairStyle = pair.getStyleSelector();
				if (styleUrl == null && pairStyle == null) {
					addTag(":StyleMap Pair must contain StyleUrl or Style");
					if (verbose) System.out.printf(" Warning: StyleMap Pair %s must contain StyleUrl or Style%n", key);
				} else {
                    // check styleUrl
					if (styleUrl != null) {
                        int ind = styleUrl.indexOf('#');
                        if (ind == 0) {
                            // URL starts with '#' -> reference anchor (e.g. #blue-icon)
                            if (!UrlRef.isIdentifier(styleUrl.substring(1))) {
                                addTag(":Suspicious StyleMap " + key + " URL characters");
                                if (verbose) System.out.printf(" Warning: StyleMap %s URL appears to contain invalid characters: %s%n", key, styleUrl);
                            }
                        } else {
                            if (ind == -1) addTag(":StyleUrl must contain '#' with identifier reference", true);
                            try {
                                URI uri = new URI(styleUrl);
                                if (uri.isAbsolute()) addTag(":StyleMap Pair with absolute StyleUrl", true);
                                else addTag(":StyleMap Pair with relative StyleUrl", true);
                            } catch (URISyntaxException e) {
                                if (UrlRef.isAbsoluteUrl(styleUrl))
                                    addTag(":StyleMap Pair with absolute StyleUrl", true);
                                else if (ind > 0)
                                    addTag(":StyleMap Pair with relative StyleUrl", true);
                                addTag(":Suspicious StyleMap " + key + " URL characters");
                                if (verbose) System.out.printf(" Warning: StyleMap %s URL appears to contain invalid characters: %s%n", key, styleUrl);
                            }
                        }
					}
                    // check inline style
					if (pairStyle != null) {
						addTag(":StyleMap has inline Style");
						checkStyle(pairStyle, false);
					}
				}
			}
		}
	}

	private void checkElements(Feature f) {
        for (Element e : f.getElements()) {
            if (e.getNamespaceURI() == null ||
                    ! e.getNamespaceURI().startsWith(IKml.NS_GOOGLE_KML_EXT_PREFIX))
                continue;
            if (TRACK.equals(e.getName()))
                checkTrack(e);
            else if (MULTI_TRACK.equals(e.getName())) {
                // http://code.google.com/apis/kml/documentation/kmlreference.html#gxmultitrack
                for (Element child : e.getChildren()) {
                    if (TRACK.equals(child.getName()))
                        checkTrack(child);
                }
            }
        }
    }

    private void checkTrack(Element e) {
        // http://code.google.com/apis/kml/documentation/kmlreference.html#gxtrack
        int whenCount = 0, coordCount = 0;
        for (Element child : e.getChildren()) {
            if ("when".equals(child.getName())) {
                whenCount++;
            } else if ("coord".equals(child.getName())) {
                coordCount++;
            } else if (EXTENDED_DATA.equals(child.getName())) {
                child = child.getChild("SchemaData", child.getNamespace()); // kml:SchemaData
                if (child == null) continue;
                child = child.getChild("SimpleArrayData", e.getNamespace()); // gx:SimpleArrayData
                if (child == null) continue;
				addTag("gx:SimpleArrayData");
                // check parallel "arrays" of values for <when> and <gx:coord> where the number of time and position values must be equal.
                // <gx:SimpleArrayData> element containing <gx:value> elements that correspond to each time/position on the track.
                /*
                <ExtendedData>
                <SchemaData schemaUrl="#schema">
                  <gx:SimpleArrayData name="cadence">
                    <gx:value>86</gx:value>
                    <gx:value>103</gx:value>
                    <gx:value>108</gx:value>
                    ...
                  </gx:SimpleArrayData>
                  ...
                 */
                int values = 0;
                for (Element value : child.getChildren()) {
                    if ("value".equals(value.getName())) values++;
                }
                if (values != whenCount && values != coordCount) {
                    addTag(":gx:SimpleArrayData has incorrect length");
                    if (verbose) System.out.format(" Error: SimpleArrayData %s has incorrect length (%d) - expecting %d%n",
                            child.getAttributes().get("name"), values, Math.max(whenCount, coordCount));
                }            
            }
            /*else {
                // Model, altitudeMode, angles, etc.
                addTag(":Unknown Track element: " + child.getName());
            }
            */
        }
        if (coordCount != whenCount) {            
            addTag(":gx:Track coord-when mismatch");
            if (verbose)
                System.out.format(" Error: Number of time (%d) and position (%d) values must match%n", whenCount, coordCount);
        }
    }

    private void checkBag(GeometryBag geometryBag) {
		checkGeometry(geometryBag);
		for (Geometry g : geometryBag) {
			if (g != null) {
				Class<?extends Geometry> gClass = g.getClass();
				if (gClass == GeometryBag.class) {
                    addTag(":Nested MultiGeometries");
					checkBag((GeometryBag)g);
                } else {
					addTag(gClass);
					checkGeometry(g);
				}
			}
		}
	}

	/**
	 * Check geometry for various tests
	 */
	private void checkGeometry(Geometry geom) {
		if (geom instanceof GeometryBase) {
			// check for gx:relativeToSeaFloor and gx:clampToSeaFloor value which imply the gx:altitudeMode is used
			AltitudeModeEnumType altMode = ((GeometryBase)geom).getAltitudeMode();
			if (altMode == AltitudeModeEnumType.relativeToSeaFloor || altMode == AltitudeModeEnumType.clampToSeaFloor)
				addTag("gx:altitudeMode");
		}

		// geom must have at least 2 points (points cannot span the line)
		// Polygon/LineRing must have at least 4 points
		if (geom instanceof Point || geom instanceof Model) return; // no checks for Points or Models

		if (verbose && geom.getNumPoints() > 1) {
			Geodetic2DPoint c = geom.getCenter();
			if (c != null) {
				System.out.format("Center point: %f,%f%n", c.getLongitudeAsDegrees(), c.getLatitudeAsDegrees());
			}
		}
		if (geom instanceof Polygon) {
			Polygon poly = (Polygon) geom;
			LinearRing outerRing = poly.getOuterRing();
			validateLinearRing("Outer ring", outerRing);
			// Verify that all the inner rings are in counter-clockwise point order, are fully
        	// contained in the outer ring, and are non-intersecting with each other.
			List<LinearRing> rings = poly.getLinearRings();
			final int n = rings.size();
			byte flags = 0;
        	for (int i = 0; i < n; i++) {
				LinearRing inner = rings.get(i);
				validateLinearRing("Inner ring", inner);
				//if (inner.clockwise())
					//addTag(":All inner rings in Polygon must be " +
						//"in counter-clockwise point order");
				// Verify that inner rings are properly contained inside outer ring
				if ((flags & 1) == 0 && !outerRing.contains(inner)) {
					flags |= 1;
					addTag(":Inner ring not contained within outer ring");
				}
				// Verify that inner rings don't overlap with each other
				if ((flags & 2) == 0 && i < n -1)
					for (int j = i + 1; j < n; j++) {
						if (inner.overlaps(rings.get(j))) {
							addTag(":Inner rings in Polygon must not overlap with each other");
							flags |= 2;
							break;
						}
					}
				if ((byte)3 == flags) break; // both bits set. stop checking
        	}
		} else if (geom instanceof Line) {
            final Line line = (Line) geom;
            if (line.clippedAtDateLine())
				addTag(":Line clipped at DateLine", true);
            if (line.getNumPoints() > 1) {
                // check if point list has duplicate consecutive points
                List<Point> pts = line.getPoints();
                final int n = pts.size();
                int dups = 0;
                Point last = pts.get(0);
                for (int i=1; i < n; i++) {
                    Point pt = pts.get(i);
                    if (last.equals(pt)) {
						addTag(":Line has duplicate consecutive points");
                        if (verbose) {
                            System.out.println(" Duplicate point at index: " + i);
                            dups++;
                        } else break;
                    }
                    last = pt;
                }
                if (verbose && dups != 0)
                    System.out.printf("XXX: %d duplicate points out of %d%n", dups, n);
            }
		} else if (geom instanceof LinearRing) {
			LinearRing ring = (LinearRing)geom;
			validateLinearRing("LinearRing", ring);
		}
		// otherwise: GeometryBag
		// else System.out.println(" other geometry: " + getClassName(geom.getClass()));
		
		// see http://www.cadmaps.com/gisblog/?cat=10
		// Detect dateline wrap or antimeridian spanning when geometry spans the -180/+180 longitude line
		Geodetic2DBounds bbox = geom.getBoundingBox();
		if (bbox != null && bbox.getWestLon().inDegrees() > bbox.getEastLon().inDegrees()) {
			addTag(":Geometry spans -180/+180 longitude line", true);
			// such geometries must be sub-divided to render correctly
		}
	}

	private void validateLinearRing(String label, LinearRing ring) {
		if (verbose) {
			addTag(":" + label + " points in " +
					(ring.clockwise() ? "clockwise" : "counter-clockwise") + " order");
			/*
			List<Point> list = new ArrayList<Point>(ring.getPoints());
			Collections.reverse(list);
			// this test always appear true
			// appears if not clockwise then always appears to be in counter-clockwise order
			if (new LinearRing(list).clockwise())
				addTag(":" + label + " points in counter-clockwise order");
			*/
		}
		if (ring.clippedAtDateLine())
			addTag(":" + label + " clipped at DateLine", true);
		try {
			List<Point> pts = ring.getPoints();
			final int n = pts.size();
            
            // check if point list has duplicate consecutive points
            int dups = 0;
            Point last = pts.get(0);
            for (int i=1; i < n; i++) {
                Point pt = pts.get(i);
                if (last.equals(pt)) {
                    if (verbose) {
                        System.out.println("Duplicate point at index: " + i);
                        dups++;
                    } else {
                        addTag(":" + label + " has duplicate consecutive points");
                        break;
                    }
                }
                last = pt;
            }
            if (verbose && dups != 0)
                System.out.printf("%d duplicate points out of %d%n", dups, n);

            // first/last point not the same 
			if (n > 2 && !pts.get(0).equals(pts.get(n - 1))) {
				List<Point> newPts = new ArrayList<Point>(n + 1);
				newPts.addAll(pts);
				newPts.add(pts.get(0)); // add first point to end
				pts = newPts;
            	addTag(":" + label + " must start and end with the same point");
			}
            
			// validate linear ring topology for self-intersection
			new LinearRing(pts, true);
			// error -> LinearRing cannot self-intersect
		} catch(IllegalArgumentException e) {
			// LinearRing fails validation
			addTag(":" + e.getMessage(), true);
		}
	}

	private void checkNetworkLink(NetworkLink networkLink) {
		TaggedMap link = networkLink.getLink();
		if (link == null) {
			addTag(":NetworkLink missing Link", true);
		} else {
			String href = link.get(HREF);
			if (href == null)
				addTag(":NetworkLink missing or empty HREF", true);
			else {
                String url;
                try {
					final URI uri = new URI(href);
					if (uri.isAbsolute()) {
						UrlRef urlRef = new UrlRef(uri);
						url = urlRef.isKmz() ? urlRef.getKmzRelPath() : urlRef.toString();
					} else {
						// relative link
						url = href;
					}
                } catch (MalformedURLException e) {
                    url = href;
                } catch (URISyntaxException e) {
                    url = href;
                }
                addTag(":url=" + url);
            }
		}
	}

    /**
     * Check Common object including: Placemark, Overlay {Screen/Ground/Photo}, NetworkLink,
     * and Container {Folder/Document}.
     */
    private void checkCommon(Common f) {
        String styleUrl = f.getStyleUrl();
        if (styleUrl != null) {
            /*
             styleUrl = xsd:anyURI type: an anyURI value can be absolute or relative,
             and may have an optional fragment identifier (i.e., URI Reference).
             Note: Spaces are, in principle, allowed in the lexical space of anyURI,
             however, their use is highly discouraged (unless they are encoded by %20).

             If the style is in the same file, uses a # reference, but Google Earth does not enforce this.
             If the style is defined in an external file, uses a full URL along with # referencing.
             styleUrl must contain '#' for id reference as in following cases:
              <styleUrl>#myIconStyleID</styleUrl>
              <styleUrl>http://someserver.com/somestylefile.xml#restaurant</styleUrl>
              <styleUrl>eateries.kml#del my-lunch-spot</styleUrl>
              <styleUrl>root://styleMaps#default+nicon=0x307+hicon=0x317</styleUrl> KML 2.0 style
            */
            final boolean isAbsoluteUrl = UrlRef.isAbsoluteUrl(styleUrl);
            if (isAbsoluteUrl) {
                addTag(":StyleUrl has absolute URL", true);
            }
            int ind = styleUrl.indexOf('#');
			if (ind == -1) {
				// google earth allows this but this is an error wrt XML syntax
                addTag(":StyleUrl must contain '#' with identifier reference", true);
			} else {
                if (!isAbsoluteUrl && ind > 0) {
                    addTag(":StyleUrl has relative URL");
                    if (verbose) System.out.println(" StyleUrl has relative URL: " + styleUrl);
                }
                // otherwise if ind == 0 then have reference anchor which is the most common styleUrl form

                // next check local reference: id reference should match NCName production in [Namespaces in XML]
				// match xsd:ID type for "id" attribute. The base type of ID is NCName.
				// Google Earth allows invalid characters in the "id" attribute and associated references
				// ind = 0 relative identifier and ind > 0: styleUrl can be absolute URL to style identifier
				// URL = [scheme://domain:port/path?query_string]#fragment_id
                if (!UrlRef.isIdentifier(styleUrl.substring(ind+1))) {
                    addTag(":Suspicious styleUrl characters");
                    if (verbose) System.out.println(" Warning: styleUrl appears to contain invalid characters: " + styleUrl);
					// e.g. #earth/environmental_sciencePlacemark
                }
            }
        }
        Date startTime = f.getStartTime();
        Date endTime = f.getEndTime();
        if (startTime != null || endTime != null) {
            if (startTime != null && startTime.equals(endTime)) {
                // if start == stop then assume timestamp/when -- no way to determine if TimeSpan was used with start=end=timestamp
                addTag(TIME_STAMP);
            } else {
                // otherwise TimeSpan used with start and/or end dates
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
            addTag(":Feature inherits container time", true);
        }
        // otherwise feature doesn't have timeStamp or timeSpans

        if (f.hasExtendedData()) {
            addTag(EXTENDED_DATA);
            if (simpleFieldSet != null)
                for (SimpleField sf : f.getFields()) {
                    simpleFieldSet.add(sf.getName());
                }
        }
        
        TaggedMap viewGroup = f.getViewGroup();
        if (viewGroup != null) {
            String tag = viewGroup.getTag();
            if (IKml.LOOK_AT.equals(tag)) {
                addTag(tag); // LookAt
                /*
                    ATC 38: LookAt
                    (1) if it is not a descendant of kml:Update, it contains all of the following child elements:
                        kml:longitude, kml:latitude, and kml:range;
                    (2) 0 <= kml:tilt <= 90;
                    (3) if kml:altitudeMode does not have the value "clampToGround", then the kml:altitude element is present
                */
				try {
					double tilt = handleTaggedElement(IKml.TILT, viewGroup, 0, 180);
					if (tilt < 0 || tilt > 90) {
						// (2) 0 <= kml:tilt <= 90;
						addTag(":Invalid LookAt values");
						if (verbose) System.out.format(" Error: Invalid tilt value in LookAt: %f [ATC 38.2]%n", tilt);
					}
				} catch (NumberFormatException nfe) {
					addTag(":LookAt has invalid tilt: non-numeric value");
					if (verbose) System.out.println(" Error: " + nfe.getMessage());
				}
               if (!CLAMP_TO_GROUND.equals(viewGroup.get(IKml.ALTITUDE_MODE, CLAMP_TO_GROUND)) &&
                           viewGroup.get(IKml.ALTITUDE) == null) {
                    // (3) if kml:altitudeMode does not have the value "clampToGround", then the kml:altitude element is present
                    addTag(":Invalid LookAt values"); // error
                    if (verbose) System.out.println(" Error: Missing altitude in LookAt [ATC 38.3]");
               }
            } else if (IKml.CAMERA.equals(tag)) {
                addTag(tag); // Camera
                /*
                    ATC 54: Camera
                    (1) if it is not a descendant of kml:Update, then the following child elements are present:
                        kml:latitude, kml:longitude, and kml:altitude;
                    (2) the value of kml:altitudeMode is not "clampToGround".

                    Reference: OGC-07-147r2: cl. 14.2.2
                */
                if (CLAMP_TO_GROUND.equals(viewGroup.get(IKml.ALTITUDE_MODE, CLAMP_TO_GROUND))) {
                    // (2) the value of kml:altitudeMode is not "clampToGround".
                    addTag(":Camera altitudeMode cannot be " + CLAMP_TO_GROUND + " [ATC 54.2]", true); // warning
               }
            } else {
                addTag(":Invalid ViewGroup tag: " + tag, true);
            }

            // check view for gx:extensions
            for(String key : viewGroup.keySet()) {
                if (key.startsWith("gx:")) {
                    // see if multiple-level element were added (e.g. gx:TimeSpan/begin)
                    int ind = key.indexOf('/', 3);
                    if (ind != -1) key = key.substring(0, ind);
                    addTag(key);
                }
            }
		}

        checkRegion(f);

        for (Element e : f.getElements()) {
            String prefix = e.getPrefix();
            String name = e.getName();
            if (StringUtils.isNotEmpty(prefix)) {
                if (IAtomConstants.ATOM_URI_NS.equals(e.getNamespaceURI()))
                    prefix = "atom"; // use atom regardless of prefix
                name = prefix + ":" + name;
            }

            addTag(name);
        }

        if (f instanceof Feature) {
            features++; // count of Placemark + NetworkLink + {Ground|Photo|Screen} Overlays
			Feature feature = (Feature)f;
			final StyleSelector style = feature.getStyle();
			if (style != null) {
				checkStyle(style, false); // Style or StyleMap
			}
			if (StringUtils.isNotBlank(f.getStyleUrl())) {
				if (style == null)
					addTag(":Feature uses shared Style", true); // used styleUrl
				else
                    addTag(":Feature uses merged shared/inline Style", true);
			} else if (style != null) {
                String styleClass = getClassName(style.getClass());
				addTag(":Feature uses inline " + styleClass, true); // Style | StyleMap
			}
        }
    }

    /**
     * Test ATC 8: Region/LatLonAltBox constraints. <p>
     * Reference: OGC-07-147r2: cl. 9.15.2
     *  
     * Verify that content of a kml:LatLonAltBox element satisfies all of the following constraints:
     * (1) kml:north > kml:south;
     * (2) kml:east > kml:west;
     * (3) kml:minAltitude <= kml:maxAltitude;
     * (4) if kml:minAltitude and kml:maxAltitude are both present,
     *     then kml:altitudeMode does not have the value "clampToGround".
     *
     * @param f Feature
     */
	private void checkRegion(Common f) {
		TaggedMap region = f.getRegion();
		if (region == null) return;

		addTag(REGION);
		try {
			double north = handleTaggedElement(IKml.NORTH, region, 0, 90);
			double south = handleTaggedElement(IKml.SOUTH, region, 0, 90);
			double east = handleTaggedElement(IKml.EAST, region, 0, 180);
			double west = handleTaggedElement(IKml.WEST, region, 0, 180);
			if (Math.abs(north - south) < 1e-5 || Math.abs(east - west) < 1e-5) {
				// incomplete bounding box or too small so skip it
				// 0.0001 (1e-4) degree dif  =~ 10 meter
				// 0.00001 (1e-5) degree dif =~ 1 meter
				// if n/s/e/w values all 0's then ignore LatLonAltBox
				if (north != 0 || south != 0 || east != 0 || west != 0)
					addTag(":LatLonAltBox appears to be very small area", true);
			} else {
				// Test ATC 8: Region - LatLonAltBox
				// Check valid Region-LatLonAltBox values:
				// 1. kml:north > kml:south; lat range: +/- 90
				// 2. kml:east > kml:west;   lon range: +/- 180
				if (north < south || east < west) {
					addTag(":Region has invalid LatLonAltBox [ATC 8]");
					if (verbose) System.out.println(" Error: LatLonAltBox fails to satisfy constraints [ATC 8]"); 
				}
			}
			double minAlt = handleTaggedElement(IKml.MIN_ALTITUDE, region, 0);
			double maxAlt = handleTaggedElement(IKml.MAX_ALTITUDE, region, 0);
			// check constraint: (3) kml:minAltitude <= kml:maxAltitude;
			if (minAlt > maxAlt) {
				addTag(":Region has invalid LatLonAltBox [ATC 8]");
				if (verbose) System.out.println(" Error: LatLonAltBox fails to satisfy Altitude constraint (minAlt <= maxAlt) [ATC 8.3]");
			}
		} catch (NumberFormatException nfe) {
			addTag(":Region has invalid LatLonAltBox: non-numeric value");
			if (verbose) System.out.println(" Error: " + nfe.getMessage());
		}
		// check constraint: (4)
		//  if kml:minAltitude and kml:maxAltitude are both present,
		//  then kml:altitudeMode does not have the value "clampToGround".
		if (region.get(IKml.MIN_ALTITUDE) != null && region.get(IKml.MAX_ALTITUDE) != null
					&& CLAMP_TO_GROUND.equals(region.get(IKml.ALTITUDE_MODE, CLAMP_TO_GROUND))) {
			addTag(":Region has invalid LatLonAltBox [ATC 8]");
			if (verbose) System.out.println(" Warning: LatLonAltBox fails to satisfy constraint (altMode != " + CLAMP_TO_GROUND + ") [ATC 8.4]");
		}

		try {
			/*
			Test ATC 39: Lod constraint:
			kml:minLodPixels shall be less than kml:maxLodPixels (where a value of -1 = infinite).
			It is also advised that kml:minFadeExtent + kml:maxFadeExtent is less than or equal to
			kml:maxLodPixels - kml:minLodPixels.
			*/
			double minLodPixels = handleTaggedElement(IKml.MIN_LOD_PIXELS, region, 0);
			double maxLodPixels = handleTaggedElement(IKml.MAX_LOD_PIXELS, region, -1);
			if (maxLodPixels == -1) maxLodPixels = Integer.MAX_VALUE; // -1 = infinite
			if (minLodPixels >= maxLodPixels) {
				addTag(":minLodPixels must be less than maxLodPixels in Lod [ATC 39]", true);
			}
		} catch (NumberFormatException nfe) {
			addTag(":Region has invalid Lod: non-numeric value");
			if (verbose) System.out.println(" Error: " + nfe.getMessage());
		}
	}

    private static double handleTaggedElement(String tag, TaggedMap region, int defaultValue) throws NumberFormatException {
        return handleTaggedElement(tag, region, defaultValue, 0);
    }

	private static double handleTaggedElement(String tag, TaggedMap region, int defaultValue, int maxAbsValue) throws NumberFormatException {
		String val = region.get(tag);
		if (val != null && !val.isEmpty()) {
			double rv;
			try {
				rv = Double.parseDouble(val);
			} catch (NumberFormatException nfe) {
				throw new NumberFormatException(String.format("The value '%s' of element '%s' is not valid", val, tag));
			}
			if (maxAbsValue > 0 && Math.abs(rv) > maxAbsValue) {
				throw new NumberFormatException(String.format("Invalid value out of range: %s=%s", tag, val));
			}
			return rv;
		}
		return defaultValue;
	}

	private void dumpStats() {
		if (dumpCount > 1 && !totals.isEmpty()) {
			System.out.println("Summary: " + dumpCount + " KML resources\n");
			boolean metaProp = false;
			for (String tag : totals) {
				// message/warnings start with : prefix, otherwise show key + count
				if (tag.startsWith(":")) {
					tag = tag.substring(1);
					metaProp = true;
				} else {
					if (metaProp) {
						// if last property was a message/warnings then
						// print new line to separate the two groups of items
						System.out.println("\t--");
						metaProp = false;
					}
				}
				System.out.println("\t" + tag);
			}
		}
		if (simpleFieldSet != null && !simpleFieldSet.isEmpty()) {
				System.out.println("\nExtendedData:");
				for (String name : simpleFieldSet) {
					System.out.println("\t" + name);
				}
		}
	}

	private void addTag(Class<? extends IGISObject> aClass) {
        String tag = getClassName(aClass);
        if (tag != null) addTag(tag);
	}

    private static String getClassName(Class<? extends IGISObject> aGClass) {
        if (aGClass != null) {
			String name = aGClass.getName();
			int ind = name.lastIndexOf('.');
			if (ind > 0) {
				name = name.substring(ind + 1);
                return name;
			}
            return name;
		}
        return null;
    }

    private static void dumpException(Exception e) {
		String msg = e.getMessage();
		if (msg != null)
			System.out.println("\t*** " + e.getClass().getName() + ": " + msg);
		else {
			System.out.println("\t*** " + e.getClass().getName());
            if (e.getCause() != null)
                e.getCause().printStackTrace(System.out);
            else
			    e.printStackTrace(System.out);
		}
	}

	public static void usage() {
		System.out.println("Usage: java KmlMetaDump [options] <file, directory, or URL..>");
		System.out.println("\nIf a directory is chosen then all kml/kmz files in any subfolder will be examined");
		System.out.println("\nOptions:");
		System.out.println("  -o<path-to-output-directory>");
		System.out.println("     Writes KML/KMZ to file in specified directory using");
		System.out.println("     same base file as original file.  Files with same name");
		System.out.println("     in target location will be skipped as NOT to overwrite anything.");
        System.out.println("     Use -a option with -o to generate unique file names.");
        System.out.println("  -a Auto-generate unique names when writing KML output to directory");
		System.out.println("  -f Follow networkLinks: recursively loads content from NetworkLinks");
		System.out.println("     and adds features to resulting statistics");
		System.out.println("  -m<MaxNetworkLinks>");
		System.out.println("     Sets max number of NetworkLinks to parse when -f option");
		System.out.println("     is enabled. Set value=0 to disable. [Default=500]");
		System.out.println("  -stdout Write KML output to STDOUT instead of writing files");
 		System.out.println("  -v Set verbose which dumps out features");
		System.out.println("  -x Dump full set of extended data property names");
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
                else if (arg.startsWith("-a"))
                    app.setAutoGenerateUniqueNames(true);
				else if (arg.startsWith("-x"))
					app.useSimpleFieldSet();
				else if (arg.startsWith("-m") && arg.length() > 2)
					app.setMaxLinkCount(Integer.parseInt(arg.substring(2)));
				else if (arg.equals("-stdout"))
					app.setUseStdout(true);
				else usage();
			} else
				sources.add(arg);
		}

		if (sources.isEmpty()) usage();

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
							e.printStackTrace();
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

		app.dumpStats();
    }

    private class MetaAppender extends AppenderSkeleton {

		@Override
		protected void append(LoggingEvent event) {
			String msg = event.getRenderedMessage();
			if (StringUtils.isBlank(msg)) return;
			LocationInfo location = event.getLocationInformation();
			if (location == null) return;
			String className = location.getClassName();
			// log only KML classes (e.g. org.mitre.giscore.input.kml.KmlInputStream)
			if (className != null &&
					(className.startsWith("org.mitre.giscore.events.") ||
					className.startsWith("org.mitre.giscore.input.kml."))) {
				// TODO: log XmlInputStream errors/warnings if keeping counts
				// truncate long error message in KmlInputStream.handleGeometry()
				if (msg.startsWith("comma found instead of whitespace between tuples before"))
					msg = msg.substring(0,48);
				else if (msg.startsWith("ignore invalid string in coordinate: "))
					msg = msg.substring(0,35);
				else if (msg.startsWith("ignore invalid character in coordinate string: "))
					msg = msg.substring(0,45);
				else if (msg.startsWith("Invalid coordinate: ")) {
					String tMessage = getFormattedMsg(event);
					if (tMessage == null) {
						// for summary strip off the specific invalid coordinate value
						msg = msg.substring(0,18);
					} else {
						msg = tMessage;
					}
				// (LinearRing.java:201) - LinearRing self-intersects at i=21 j=22
				//} else if (msg.contains("LinearRing self-intersects")) {
					//msg = "XXX: " + msg;
				} else if (msg.startsWith("Failed geometry: ")) {
					ThrowableInformation ti = event.getThrowableInformation();
					// WARN [main] (KmlInputStream.java:1913) - Failed geometry: LinearRing
					//java.lang.IllegalArgumentException: LinearRing must contain at least 4 Points
					if (ti != null && ti.getThrowable() != null)
						msg = ti.getThrowable().getMessage();
					else
						msg = "Bad geometry";
				}
				addTag(":" + msg);
			}
			// event.getThrowableStrRep();
		}

		private String getFormattedMsg(LoggingEvent event) {
			// ERROR [main] (KmlInputStream.java:2321) - Invalid coordinate: -122.212
			// java.lang.IllegalArgumentException: Latitude value exceeds pole value
			ThrowableInformation ti = event.getThrowableInformation();
			if (ti == null) return null;
			Throwable t = ti.getThrowable();
			if (t instanceof IllegalArgumentException) {
				final String tMessage = t.getMessage();
				if (StringUtils.isNotBlank(tMessage)) {
					// java.lang.IllegalArgumentException: Angle 686162.5993066976 radians is too big
					if (tMessage.startsWith("Angle ") && tMessage.endsWith("radians is too big"))
						return "Angle radians is too big";
					// else if (tMessage.equals("Latitude value exceeds pole value"))
					// TODO: do we want to filter/reformat any other errors?
					return tMessage;
				}
			}
			return null;
		}

		public void close() {
			// nothing to do
		}

		public boolean requiresLayout() {
			return false;
		}
	}

}

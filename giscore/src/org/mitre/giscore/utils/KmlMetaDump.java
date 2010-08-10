package org.mitre.giscore.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.output.kml.KmlWriter;
import org.mitre.itf.geodesy.Geodetic2DBounds;

/**
 * Simple KML Debugging Tool to read KML/KMZ documents by File or URL and dump statistics
 * on number of feature elements (Placemarks, Points, Polygons, LineStrings, NetworkLinks, etc.)
 * and properties (ExtendedData, Schema, etc.) and optionally export the same KML to a
 * file (or stdout) to verify all content has been correctly interpreted.
 *
 * Parsing also includes support for most gx KML extensions (e.g. MultiTrack, Track, etc.) 
 * <p/>
 *
 * Notes following conditions if found:
 * <ul>

 *  <li> NetworkLink has missing or empty HREF (info)
 *  <li> Overlay does not contain Icon element (info)
 *  <li> End container with no matching start container (error)
 *  <li> Starting container tag with no matching end container (error)
 *  <li> Geometry spans -180/+180 longtiude line (dateline wrap or antimeridian spanning problem) (warning)
 *  <li> Region has invalid LatLonAltBox (error)
 *  <li> LatLonAltBox fails to satisfy constraints [ATC 8]
 *  <li> LatLonAltBox fails to satisfy Altitude constraint (minAlt <= maxAlt) [ATC 8.3] (error)
 *  <li> LatLonAltBox fails to satisfy constraint (altMode != clampToGround) [ATC 8.4] (warning)
 *  <li> LatLonAltBox appears to be very small area (warning)
 *  <li> minLodPixels must be less than maxLodPixels in Lod [ATC 39] (error)
 *  <li> Camera altitudeMode cannot be clampToGround [ATC 54.2] (warning)
 *  <li> Invalid LookAt values (error)
 *  <li> Invalid tilt value in LookAt [ATC 38.2] (error)
 *  <li> Missing altitude in LookAt [ATC 38.3] (warning)
 *  <li> Invalid TimeSpan if begin later than end value (warning)
 *  <li> Feature inherits time from parent container (info)
 *  <li> Container start date is earlier than that of its ancestors (info)
 *  <li> Container end date is later than that of its ancestors (info)
 *  <li> Out of order elements
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
 * ATC x-x errors/warnings reference those defined in the OGC KML 2.2 Abstract Test Suite
 * Reference OGC 07-134r2 available at http://www.opengeospatial.org/standards/kml
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
	private Class<? extends IGISObject> lastObjClass;
	
	private boolean inheritsTime;
	private Date containerStartDate;
	private Date containerEndDate;
	private final Stack<ContainerStart> containers = new Stack<ContainerStart>();

	private Set<String> maximalSet;		

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
        if (features != 0)
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

	private KmlWriter getWriter(String name) {
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
				if (e.getCause() != null) e.getCause().printStackTrace();
				else e.printStackTrace();
			}
		}

		if (useStdout) {
			try {
				KmlOutputStream kos = new KmlOutputStream(System.out);
				System.out.println();
				return new KmlWriter(kos);
			} catch (XMLStreamException e) {
				System.err.println("*** ERROR: Failed to create stdout outputStream");
                if (e.getCause() != null) e.getCause().printStackTrace();
				else e.printStackTrace();
			}
		}

		return null;
	}

    private void checkObject(IGISObject gisObj) {
        if (gisObj instanceof WrappedObject) {
            // unwrap wrapped gis objects
            gisObj = ((WrappedObject) gisObj).getObject();
            addTag(":Out of order elements");
        }
        if (verbose) System.out.println(gisObj);
        final Class<? extends IGISObject> cl = gisObj.getClass();
        if (cl == DocumentStart.class) return; // ignore always present DocumentStart root element

        if (gisObj instanceof Common) {
            if (gisObj instanceof Feature) features++; // PlaceMark + NetworkLink + Overlay
            checkCommon((Common)gisObj);
        }

        if (cl == Feature.class) {
            Feature f = (Feature) gisObj;
            Geometry geom = f.getGeometry();
            addTag(PLACEMARK);
            if (geom != null) {
                checkGeometry(geom);
                Class<? extends Geometry> geomClass = geom.getClass();
                if (geomClass == GeometryBag.class) {
                    addTag(MULTI_GEOMETRY);
                    checkBag((GeometryBag) geom);
                } else addTag(geomClass);
            }
        } else if (cl == NetworkLink.class) {
            NetworkLink networkLink = (NetworkLink) gisObj;
            checkNetworkLink(networkLink);
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
					if (containerStartDate != null) {
						if (verbose) System.out.println(" Overriding parent container start date");
						if (startTime.compareTo(containerStartDate) < 0)
							addTag(":Container start date is earlier than that of its ancestors");
					}
					// log.debug("use container start date");
				}
				// override any previous start date
				containerStartDate = startTime;
				if (endTime != null) {
					if (containerEndDate != null) {
						if (verbose) System.out.println(" Overriding parent container end date");
						if (endTime.compareTo(containerEndDate) > 0)
							addTag(":Container end date is later than that of its ancestors");
					}
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
                addTag(":end container with no matching start container");
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
            if (ov.getIcon() == null)
                addTag(":Overlay missing icon");
        } else if (cl == Element.class) {
            Element e = (Element)gisObj;
            String prefix = e.getPrefix();
            String name = e.getName();
            if (prefix != null) name = prefix + ":" + name;
            addTag(name);
        } else if ( /* cl != DocumentStart.class && */ cl != Comment.class) {
            // ignore: DocumentStart + Comment objects
            addTag(cl); // e.g. Schema, StyleMap, NetworkLinkControl
        }
        lastObjClass = gisObj.getClass();
	}

    private void checkBag(GeometryBag geometryBag) {
		for (Geometry g : geometryBag) {
			if (g != null) {
				Class<?extends Geometry> gClass = g.getClass();
				if (gClass == GeometryBag.class)
					checkBag((GeometryBag)g);
				else
					addTag(gClass);
			}
		}
	}

	/**
	 * Detect dateline wrap or antimeridian spanning when geometry spans the -180/+180 longtiude line
	 */
	private void checkGeometry(Geometry geom) {
		// geom must have at least 2 points (points cannot span the line)
		if (geom.getNumPoints() < 2) return;
		Geodetic2DBounds bbox = geom.getBoundingBox();
		//if (geom instanceof Line && (((Line)geom).clippedAtDateLine())) System.out.println(":clipped");
		//else if (geom instanceof LinearRing && (((LinearRing)geom).clippedAtDateLine())) System.out.println(":clipped");
		// see http://www.cadmaps.com/gisblog/?cat=10
		if (bbox.getWestLon().inDegrees() > bbox.getEastLon().inDegrees()) {
			//System.out.println(geom.getClass().getName());
			addTag(":Geometry spans -180/+180 longtiude line");
			// such geometries must be sub-divided to render correctly
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

    private void checkCommon(Common f) {
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

        if (f.hasExtendedData()) {
            addTag(EXTENDED_DATA);
            if (maximalSet != null)
                for (SimpleField sf : f.getFields()) {
                    maximalSet.add(sf.getName());
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
                double tilt = handleTaggedElement(IKml.TILT, viewGroup, 0, 180);
                if (tilt < 0 || tilt > 90) {
                    // (2) 0 <= kml:tilt <= 90;
                    addTag(":Invalid LookAt values");
                    if (verbose) System.out.format(" Error: Invalid tilt value in LookAt: %f [ATC 38.2]%n", tilt);
                }
               if (!"clampToGround".equals(viewGroup.get(IKml.ALTITUDE_MODE, "clampToGround")) &&
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
                if ("clampToGround".equals(viewGroup.get(IKml.ALTITUDE_MODE, "clampToGround"))) {
                    // (2) the value of kml:altitudeMode is not "clampToGround".
                    addTag(":Camera altitudeMode cannot be clampToGround [ATC 54.2]"); // warning
               }
            } else {
                addTag(":Invalid ViewGroup tag: " + tag);
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
            if (prefix != null) name = prefix + ":" + name;
            addTag(name);
        }

        //if (lastObj instanceof StyleSelector) {
        if (lastObjClass == Style.class || lastObjClass == StyleMap.class)
            addTag(":Feature uses inline " + getClassName(lastObjClass)); // Style or StyleMap
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
					addTag(":LatLonAltBox appears to be very small area");
			} else {
				// Test ATC 8: Region - LatLonAltBox
				// Check valid Region-LatLonAltBox values:
				// 1. kml:north > kml:south; lat range: +/- 90
				// 2. kml:east > kml:west;   lon range: +/- 180
				if (north < south || east < west) {
					addTag(":Region has invalid LatLonAltBox");
					if (verbose) System.out.println(" Error: LatLonAltBox fails to satisfy constraints [ATC 8]"); 
				}
			}
			double minAlt = handleTaggedElement(IKml.MIN_ALTITUDE, region, 0);
			double maxAlt = handleTaggedElement(IKml.MAX_ALTITUDE, region, 0);
			// check constraint: (3) kml:minAltitude <= kml:maxAltitude;
			if (minAlt > maxAlt) {
				addTag(":Region has invalid LatLonAltBox");
				if (verbose) System.out.println(" Error: LatLonAltBox fails to satisfy Altitude constraint (minAlt <= maxAlt) [ATC 8.3]");
			}
            // check constraint: (4)
            //  if kml:minAltitude and kml:maxAltitude are both present,
			//  then kml:altitudeMode does not have the value "clampToGround".
            if (region.get(IKml.MIN_ALTITUDE) != null && region.get(IKml.MAX_ALTITUDE) != null
                    && "clampToGround".equals(region.get(IKml.ALTITUDE_MODE, "clampToGround"))) {
                addTag(":Region has invalid LatLonAltBox");
				if (verbose) System.out.println(" Warn: LatLonAltBox fails to satisfy constraint (altMode != clampToGround) [ATC 8.4]");
            }
		} catch (NumberFormatException nfe) {
			addTag(":Region has invalid LatLonAltBox");
			if (verbose) System.out.println(" Error: " + nfe.getMessage());
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
				addTag(":minLodPixels must be less than maxLodPixels in Lod [ATC 39]");
			}
		} catch (NumberFormatException nfe) {
			addTag(":Region has invalid Lod");
			if (verbose) System.out.println(" Error: " + nfe.getMessage());
		}
	}

    private static double handleTaggedElement(String tag, TaggedMap region, int defaultValue) throws NumberFormatException {
        return handleTaggedElement(tag, region, defaultValue, 0);
    }

	private static double handleTaggedElement(String tag, TaggedMap region, int defaultValue, int maxAbsValue) throws NumberFormatException {
		String val = region.get(tag);
		if (val != null && val.length() != 0) {
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

    private static void dumpException(IOException e) {
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
		System.out.println("\nIf a directory is choosen that all kml/kmz files in any subfolder will be examined");
		System.out.println("\nOptions:");
		System.out.println("\t-o<path-to-output-directory> Writes KML/KMZ to file in specified directory");
		System.out.println("\t\tusing same base file as original file.");
		System.out.println("\t\tFiles with same name in target location will be skipped as NOT to overwrite anything.");
		System.out.println("\t-f Follow networkLinks: recursively loads content from NetworkLinks");
		System.out.println("\t\tand adds features to resulting statistics");
		System.out.println("\t-stdout Write KML output to STDOUT instead of writing files");
 		System.out.println("\t-v Set verbose which dumps out features");
		System.out.println("\t-x Dump full set of extended data property names");
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
				else if (arg.startsWith("-x"))
				 	app.maximalSet = new TreeSet<String>();
				else if (arg.startsWith("-stdout"))
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
		
		if (app.dumpCount > 1 && !app.totals.isEmpty()) {
			System.out.println("Summary: " + app.dumpCount + " KML resources\n");
			boolean metaProp = false;
			for (String tag : app.totals) {
				// message/warnings start with : prefix, otherwise show key + count
				if (tag.startsWith(":")) {
					tag = tag.substring(1);
					metaProp = true;
				} else {
					if (metaProp) {
						// if last property was a message/warnings then
						// print new line to separate the two groups of items
						System.out.println();
						metaProp = false;
					}
				}
				System.out.println("\t" + tag);
			}
		}
		if (app.maximalSet != null && !app.maximalSet.isEmpty()) {
				System.out.println("\nExtendedData:");
				for (String name : app.maximalSet) {
					System.out.println("\t" + name);
				}
		}
    }

}

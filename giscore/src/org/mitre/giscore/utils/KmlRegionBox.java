package org.mitre.giscore.utils;

import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.UrlRef;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.DocumentType;
import org.mitre.itf.geodesy.Geodetic2DBounds;
import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLStreamException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

/**
 * Create KML output with bounding box outlines from KML regions.
 * 
 * Parse KML sources and extract each unique bounding boxes from Regions.
 * Creates a KML output file 'bbox.kml' (or as specified) in current directory
 * with a Placemark with Line geometry for the bounding box of each Region
 * where a valid LatLonAltBox has a non-zero region.
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Nov 5, 2009 9:18:24 PM
 */
public class KmlRegionBox {

	private KmlOutputStream kos;
	private final List<Geodetic2DBounds> regions = new ArrayList<Geodetic2DBounds>();
	private String outFile;
	private boolean followLinks;

	public void checkSource(URL url) throws IOException, XMLStreamException {
		System.out.println(url);
		processKmlSource(new KmlReader(url), url.toString());
	}

	public void checkSource(File file) throws XMLStreamException, IOException {
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
			String name = file.getName();
			if (name.equals("doc.kml")) {
				File parent = file.getParentFile();
				if (parent != null)
					name = parent.getName() + "/" + name;
			}
			processKmlSource(new KmlReader(file), name);
		}
	}

	private void processKmlSource(KmlReader reader, String source) throws XMLStreamException, IOException {
		IGISObject o;
		while ((o = reader.read()) != null) {
			checkObject(o, source);
		}
		reader.close();

		if (followLinks) {
			List<URI> networkLinks = reader.getNetworkLinks();
			if (networkLinks.size() != 0) {
				reader.importFromNetworkLinks(new KmlReader.ImportEventHandler() {
                    private URI last;
					public boolean handleEvent(UrlRef ref, IGISObject gisObj) {
                        URI uri = ref.getURI();
                        if (!uri.equals(last)) {
							// first gisObj found from a new KML source
                            System.out.println("Check NetworkLink: " +
                                    (ref.isKmz() ? ref.getKmzRelPath() : uri.toString()));
                            System.out.println();
                            last = uri;
                        }
						try {
							checkObject(gisObj, ref.toString());
						} catch (Exception e) {
							System.out.println("\t*** " + e.getMessage());
							return false;
						}
						return true;
					}
				});
			}
		}
	}

	private void checkObject(IGISObject o, String source) throws FileNotFoundException, XMLStreamException {
		if (o instanceof Common) {
				Common f = (Common) o;
				TaggedMap region = f.getRegion();
				if (region != null) {
					double north = handleTaggedElement(IKml.NORTH, region);
					double south = handleTaggedElement(IKml.SOUTH, region);
					double east = handleTaggedElement(IKml.EAST, region);
					double west = handleTaggedElement(IKml.WEST, region);
					String name = f.getName();
					if (Math.abs(north - south) < 1e-4 || Math.abs(east -  west) < 1e-4) {
						// incomplete bounding box or too small so skip it
						if (north != 0 || south != 0 || east != 0 || west != 0)
							System.out.println("\tbbox appears to be very small area: " + name);
						return;
					}
					List<Point> pts = new ArrayList<Point>(5);
					pts.add(new Point(north, west));
					pts.add(new Point(north, east));
					pts.add(new Point(south, east));
					pts.add(new Point(south, west));
					pts.add(pts.get(0));
					Line line = new Line(pts);

					Geodetic2DBounds bounds = line.getBoundingBox();
					if (regions.contains(bounds)) {
						System.out.println("\tduplicate bbox: " + bounds);
						return;
					}
				  	regions.add(bounds);
					//regions.put(bounds, bbox);

					Feature bbox = new Feature();
					bbox.setDescription(source);
					line.setTessellate(true);
					bbox.setGeometry(line);
					if (StringUtils.isNotBlank(name))
						bbox.setName(name + " bbox");
					else
						bbox.setName("bbox");
					if (kos == null) {
						if (StringUtils.isBlank(outFile)) outFile = "bbox.kml";
						kos = new KmlOutputStream(new FileOutputStream(outFile));
						kos.write(new DocumentStart(DocumentType.KML));
						ContainerStart cs = new ContainerStart(IKml.FOLDER);
						cs.setName("Region boxes");
						kos.write(cs);
					}
					kos.write(bbox);
				}
			}
	}

	private double handleTaggedElement(String tag, TaggedMap region) {
		String val = region.get(tag);
		if (val != null && val.length() != 0)
			try {
				return Double.parseDouble(val);
			} catch (NumberFormatException nfe) {
				  System.out.printf("\tInvalid value: %s=%s%n", tag, val);
			}
		return 0;
	}

	public static void main(String args[]) {

		KmlRegionBox app = new KmlRegionBox();

		List<String> sources = new ArrayList<String>();
		for (String arg : args) {
			if (arg.equals("-f"))
				app.followLinks = true;
			else if (arg.startsWith("-o"))
				app.outFile = arg.substring(2);
			else if (!arg.startsWith("-"))
				sources.add(arg);
			//System.out.println("Invalid argument: " + arg);
		}

		if (sources.size() == 0)
			System.out.println("Must specify file and/or URL");
			//usage();

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
					} else
						app.checkSource(new URL(arg));
				}
			} catch (MalformedURLException e) {
				System.out.println(arg);
				System.out.println("\t*** " + e.getMessage());
				System.out.println();
			} catch (IOException e) {
				System.out.println(e);
			} catch (XMLStreamException e) {
				System.out.println(e);
			}
		}

		if (app.kos != null)
			try {
				app.kos.close();
			} catch (IOException e) {
				System.out.println("\t*** " + e.getMessage());
			}
	}

}

package org.mitre.giscore.input.kml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.TaggedMap;
import org.mitre.giscore.input.IGISInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper to KmlInputStream that handles various house cleaning of parsing KML sources:
 *  -read from KMZ/KML files transparently
 *  -re-writing of URLs inside KMZ files and resolving relative URLs
 *  -removes duplicate networkLink URLs
 *  -removes placemark/features that don't provide a geometry (Point, Line, etc)
 *  -resursively loading all features from networkLinks 
 * 
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 5, 2009 9:12:19 AM
 */
public class KmlReader implements IKml {

    private static final Logger log = LoggerFactory.getLogger(KmlReader.class);

    private final List<IGISObject> features = new ArrayList<IGISObject>();

    // private final List<NetworkLink> networkLinks = new ArrayList<NetworkLink>();

    /**
     * if true indicates that the stream is for a KMZ compressed file
     * and network links with relative URLs need to be handled special
     */
    private boolean compressed;

    /**
     * base URL of KML resource if passed in as a stream
     */
    private final URL baseUrl;

    /**
     * holder for names of supported httpQuery fields as of 2/19/09 in Google Earth 5.0.11337.1968 with KML 2.2
     */
    private static final Map<String,String> httpQueryLabels = new HashMap<String,String>();

    static {
        final String[] labels = {
                "clientVersion", "4.3.7284.3916",
                "kmlVersion",   "2.2",
                "clientName",   "Google+Earth",
                "language",     "en"};

        for (int i=0; i < labels.length; i+= 2)
            httpQueryLabels.put(labels[i], labels[i+1]);
    }

    /**
     * names of supported viewFormat fields as of 2/19/09 in Google Earth 5.0.11337.1968 with KML 2.2
     */
    private static final List viewFormatLabels = Arrays.asList(
            "bboxEast",
            "bboxNorth",
            "bboxSouth",
            "bboxWest",
            "horizFov",
            "horizPixels",
            "lookatHeading",
            "lookatLat",
            "lookatLon",
            "lookatRange",
            "lookatTerrainAlt",
            "lookatTerrainLat",
            "lookatTerrainLon",
            "lookatTilt",
            "terrainEnabled",
            "vertFov",
            "vertPixels");

    /**
     * Pattern to match absolute URLs (e.g. http://host/file, ftp://host/file, file:/path/file, etc
     */
    private static final Pattern absUrlPattern = Pattern.compile("^[a-zA-Z]+:");

    public KmlReader(URL url) throws IOException {
        InputStream iStream = UrlRef.getInputStream(url);
        if (iStream instanceof ZipInputStream) compressed = true;
        baseUrl = url;
        readFromStream(iStream, features, null);
    }

    public KmlReader(File file) throws IOException {
        InputStream iStream = null;
        ZipFile zf = null;
        try {
            if (file.getName().toLowerCase().endsWith(".kmz")) {
                zf = new ZipFile(file);
                Enumeration e = zf.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    // simply find first kml file in the archive
                    // see note on KMZ in UrlRef.getInputStream() method for more detail
                    if (entry.getName().toLowerCase().endsWith(".kml")) {
                        iStream = zf.getInputStream(entry);
                        // indicate that the stream is for a KMZ compressed file
                        compressed = true;
                        break;
                    }
                }
                if (iStream == null)
                    throw new FileNotFoundException("Failed to find KML content in file: " + file);
            } else {
                // treat as normal .kml text file
                iStream = new BufferedInputStream(new FileInputStream(file));
            }

            URL url;
            try {
                url = file.toURI().toURL();
            } catch (Exception e) {
                // this should not happen
                log.warn("Failed to convert file URI to URL: " + e);
                url = file.toURL();
            }

            baseUrl = url;
            readFromStream(iStream, features, null);
        } finally {
            IOUtils.closeQuietly(iStream);
            if (zf != null)
                try {
                    zf.close();
                } catch (Exception e) {
                    // ignore
                }
        }
    }

    private void readFromStream(InputStream inputStream, List<IGISObject> features,
                                List<URI> networkLinks) throws IOException {
        try {
            IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, inputStream);
            IGISObject gisObj;
            List<URI> gisNetworkLinks = new ArrayList<URI>();
            while ((gisObj = kis.read()) != null) {
                // System.out.println("> " + gisObj.getClass().getName());
                if (gisObj instanceof Feature) {
                    Class type = gisObj.getClass();
                    if (type == NetworkLink.class) {
                        NetworkLink link = (NetworkLink)gisObj;
                        // adjust URL with httpQuery and viewFormat parameters
                        // if parent is compressed and URL is relative then rewrite URL
                        URI uri = getLinkHref(link.getLink());
                        if (uri != null) {
                            if (!gisNetworkLinks.contains(uri)) {
                                gisNetworkLinks.add(uri);
                                if (networkLinks != null) {
                                    //StringBuilder buf = new StringBuilder("Placeholder for NetworkLink");
                                    //buf.append(" href=").append(uri);
                                    //features.add(new Comment(buf.toString()));
                                    networkLinks.add(uri);
                                }
                                features.add(link);
                            }
                            else System.out.println("\t*** skipping networklink with dup href");
                        } else
                            System.out.println("\t*** skipping missing or empty NetworkLink href");
                    }
                    else if (type == GroundOverlay.class) {
                        GroundOverlay o = (GroundOverlay)gisObj;
                        TaggedMap icon = o.getIcon();
                        String href = icon != null ? getTrimmedValue(icon, HREF) : null;
                        if (href != null) // && icon.get(HREF) != null)
                            // can we have a GroundOverlay WO LINK ??
                            features.add(gisObj);
                        else
                            System.out.println("\t*** skipping null GroundOverlay href");
                    } else {
                        // include only those features with geometries: ignore camera, model, etc.
                        // e.g. Placemark with Model geometry (not yet supported)
                        if (type == Feature.class && ((Feature)gisObj).getGeometry() == null)
                            System.out.println("\t*** skipping null geometry: " + type.getName());
                        else
                            features.add(gisObj);
                    }
                } else {
                    // non-Feature (e.g. DocumentStart, ContainerEnd, Style, StyleMap, Schema, etc.)
                    // System.out.println("other=" + gisObj.getClass().getName());
                    features.add(gisObj);
                }
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /***
     * Adjust Link href URL if httpQuery and/or viewFormat parameters are defined
     * @param links
     * @return adjusted href URL, null if href is missing or empty string
     */
    private URI getLinkHref(TaggedMap links) {
        String href = links != null ? getTrimmedValue(links, HREF) : null;
        if (href == null) return null;
        URI uri = getLink(href);
        if (uri == null) return null;

        String httpQuery = getTrimmedValue(links, HTTP_QUERY);
        String viewFormat = getTrimmedValue(links, VIEW_FORMAT);
        href = uri.toString();

        // if have NetworkLink href with no httpQuery/viewFormat then
        // return href as-is otherwise modify href accordingly
        // Likewise if URI is local file then httpQuery and viewFormat are ignored
        if (viewFormat == null && httpQuery == null || "file".equals(uri.getScheme())) {
            // if URL was relative then getLink() rewrites URL to be absolute wrt to the baseURL
            // store modified HREF back in map
            links.put(HREF, href);
            return uri;
        }

        StringBuilder buf = new StringBuilder(href);
        // check if '?' is part of base HREF
        // sometimes last character of URL is ? in which case don't need to add anything
        if (href.charAt(href.length() - 1) != '?') {
            buf.append(href.indexOf('?') == -1 ? '?' : '&');
        }

        /*
            Construct HttpQuery and viewFormat values
            http://code.google.com/apis/kml/documentation/kmlreference.html

            KML NetworkLink Example:

            <NetworkLink>
               <Link>
                <href>baseUrl</href>
                <viewFormat>BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth];\
                    CAMERA=[lookatLon],[lookatLat],[lookatRange],[lookatTilt],[lookatHeading];\
                    VIEW=[horizFov],[vertFov],[horizPixels],[vertPixels],[terrainEnabled];\
                    LOOKAT=[lookatTerrainLon],[lookatTerrainLat],[lookatTerrainAlt]
                </viewFormat>
                <httpQuery>client=[clientVersion],[kmlVersion],[clientName],[language]</httpQuery>
               </Link>
             </NetworkLink>

            Issues following URL fetch via HTTP GET
            baseUrl?client=5.0.11337.1968,2.2,Google+Earth,en&BBOX=0,0,0,0;CAMERA=0,0,0,0,0;VIEW=0,0,0,0,0;LOOKAT=0,0,0
            if '?' is in the href URL then '&' is appended before httpQuery and/or viewFormat arguments
            Any spaces in httpQuery or viewformat are encoded as %20. Other []'s are encoded as %5B%5D

            seamap.kml with LookAt

            <LookAt>
                <longitude>-1.8111</longitude>
                <latitude>54.3053</latitude>
                <altitude>0</altitude>
                <range>9500000</range>
                <tilt>0</tilt>
                <heading>-2.0</heading>
            </LookAt>

            baseUrl?mode=NetworkLink&taxa_column=all_taxa&BBOX=-180,-12.00837846543677,180,90

            <LookAt>
                <longitude>-95.2654831941224</longitude>
                <latitude>38.95938957105111</latitude>
                <altitude>0</altitude>
                <range>11001000</range>
                <tilt>0</tilt>
                <heading>2.942013080353753e-014</heading>
                <altitudeMode>relativeToGround</altitudeMode>
            </LookAt>

            GET /placemark.kml?client2=Google+Earth,5.0.11337.1968,2.2,Google+Earth,en,%5Bfoobar%5D&
            BBOX=-180,-56.92725201297682,180,90;
            CAMERA=-40.00123907841759,25.00029463919559,-21474836.48,0,0;
            VIEW=60,54.921,751,676,1;
            LOOKAT=-40.00123610631735,25.00029821129455,-4824.05

            */

        if (httpQuery != null) {
            /*
             * <httpQuery>
             *  [clientVersion]  5.0.11337.1968      4.3.7284.3916
             *  [kmlVersion]     2.2
             *  [clientName]     Google+Earth
             *  [language]       en
             */
            for (int i=0; i < httpQuery.length(); i++) {
                char ch = httpQuery.charAt(i);
                if (ch == '[') {
                    int ind = httpQuery.indexOf(']', i + 1);
                    String val = null;
                    if (ind != -1) {
                        String key = httpQuery.substring(i + 1, ind);
                        val = httpQueryLabels.get(key);
                    }
                    if (val != null) {
                        // insert replacement value for key (e.g. clientVersion, kmlVersion. etc.)
                        buf.append(val);
                        i = ind;
                    } else
                        buf.append("%5B");
                }
                else if (ch == ']')
                    buf.append("%5D");
                else if (ch == ' ')
                    buf.append("%20");
                else
                    buf.append(ch);
            }

            // client=Google+Earth,4.3.7284.3916,2.2,%20Google+Earth,en&BBOX=0,0,0,0;CAMERA=0,0,0,0,0;VIEW=0,0,0,0,0;lookAt=0,0,0

            // add httpQuery parameters to URL
            // unscape HTML encoding &amp; -> &
            //href +=  + httpQuery.replace("&amp;", "&");
        }

        /*
    <viewFormat>

        Specifies the format of the query string that is appended to the Link's <href> before the file is fetched.
        (If the <href> specifies a local file, this element is ignored.)
        If you specify a <viewRefreshMode> of onStop and do not include the <viewFormat> tag in the file,
        the following information is automatically appended to the query string:

        BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]

    This information matches the Web Map Service (WMS) bounding box specification.
    If you specify an empty <viewFormat> tag, no information is appended to the query string.
    You can also specify a custom set of viewing parameters to add to the query string. If you supply a format string,
    it is used instead of the BBOX information. If you also want the BBOX information, you need to add those parameters
    along with the custom parameters.

    You can use any of the following parameters in your format string (and Google Earth will substitute the appropriate
    current value at the time it creates the query string):

        * [lookatLon], [lookatLat] - longitude and latitude of the point that <LookAt> is viewing
        * [lookatRange], [lookatTilt], [lookatHeading] - values used by the <LookAt> element (see descriptions of <range>, <tilt>, and <heading> in <LookAt>)
        * [lookatTerrainLon], [lookatTerrainLat], [lookatTerrainAlt] - point on the terrain in degrees/meters that <LookAt> is viewing
        * [cameraLon], [cameraLat], [cameraAlt] - degrees/meters of the eyepoint for the camera
        * [horizFov], [vertFov] - horizontal, vertical field of view for the camera
        * [horizPixels], [vertPixels] - size in pixels of the 3D viewer
        * [terrainEnabled] - indicates whether the 3D viewer is showing terrain
        */

        if (viewFormat != null) {
            if (httpQuery != null)
                buf.append('&');

            for (int i=0; i < viewFormat.length(); i++) {
                char ch = viewFormat.charAt(i);
                if (ch == '[') {
                    int ind = viewFormat.indexOf(']', i + 1);
                    if (ind != -1) {
                        String key = viewFormat.substring(i + 1, ind);
                        if (viewFormatLabels.contains(key)) {
                            // insert "0" as replacement value for key (e.g. bboxWest, lookatLon. etc.)
                            buf.append('0');
                            i = ind;
                            continue;
                        }
                    }
                    buf.append("%5B"); // hex-encode '['
                }
                else if (ch == ']')
                    buf.append("%5D");
                else if (ch == ' ')
                    buf.append("%20");
                else
                    buf.append(ch);
            }
        }

        href = buf.toString();
        // store modified HREF back in map
        links.put(HREF, href);
        try {
            return new URI(href);
        } catch (URISyntaxException e) {
            log.error("Failed to create URI from URL=" + href, e);
            return null;
        }
    }

    public boolean isCompressed() {
        return compressed;
    }

    public List<NetworkLink> getNetworkLinks() {
        List<NetworkLink> networkLinks = new ArrayList<NetworkLink>();
        for (IGISObject obj : features) {
            if (obj instanceof NetworkLink)
                networkLinks.add((NetworkLink)obj);
        }
        return networkLinks;
    }

    public List<URI> getNetworkLinkList() {
        List<URI> networkLinks = new LinkedList<URI>();
        for (IGISObject obj : features) {
            if (obj instanceof NetworkLink) {
                URI uri = getLinkUri((NetworkLink)obj);
                if (uri != null) {
                    // change any relative URLs to absolute ones
                    // if base KML document is compressed (KMZ) and URL is relative
                    // then modify URL
                    networkLinks.add(uri);
                }
            }
        }
        return networkLinks;
    }

    public List<IGISObject> getFeatures() {
        return features;
    }

    private URI getLink(String href) {
        // assumes href is not null nor zero length
        URI uri = null;
        try {
            if (href.indexOf(' ') != -1) {
                href = href.replace(" ", "%20"); // escape all whitespace otherwise new URI() throws an exception
                //log.debug("Escape whitespace in URL: " + href);//gjm
            }
            // check if URL is absolute otherwise its relative to base URL if defined
            if (absUrlPattern.matcher(href).lookingAt()) {
                // absolute URL (e.g. http://host/path/x.kml)
                // uri = new URL(href).toURI(); //gjm
                uri = new URI(href);
                //href = url.toExternalForm(); //gjm
            } else if (baseUrl == null) {
                log.warn("no base URL to resolve relative URL: " + href);
            } else {
                // relative URL
                // if compressed amd relative link then need special encoded kmz URI
                if (compressed) {
                    // if relative link and parent is KMZ file (compressed=true)
                    // then need to keep track of parent URL in addition
                    // to the relative link to match against the KMZ file entries.
                    uri = new UrlRef(baseUrl, href).getURI();
                } else {
                    uri = new URL(baseUrl, href).toURI();
                }
            }
        } catch (URISyntaxException e) {
            log.warn("Invalid link: " + href, e);
        } catch (MalformedURLException e) {
            log.warn("Invalid link: " + href, e);
        }
        return uri;
    }

    /**
     * Import KML objects from a list of networkLinks presumably after
     * reading a base KML document in KmlReader.
     *
     * @param linkedFeatures
     * @return list of visited networkLink URIs
     */
    public List<URI> importFromNetworkLinks(List<IGISObject> linkedFeatures) {
        List<URI> networkLinks = getNetworkLinkList();
        if (linkedFeatures == null) {
            log.error("Invalid arguments: cannot import");
            return networkLinks;
        }
        // keep track of URLs visited to prevent revisits
        List<URI> visited = new ArrayList<URI>();
        while (networkLinks.size() != 0) {
            URI uri = networkLinks.remove(0);
            if (!visited.contains(uri)) {
                visited.add(uri);
                InputStream is = null;
                try {
                    UrlRef ref = new UrlRef(uri);
                    is = ref.getInputStream();
                    if (is == null) continue;
                    int oldSize = networkLinks.size();
                    int oldFeatSize = linkedFeatures.size();
                    // need to add new networkLinks back to list to recursively import
                    readFromStream(is, linkedFeatures, networkLinks);
                    if (oldFeatSize != linkedFeatures.size())
                        System.out.println("\t*** got features from network links ***");
                    if (oldSize != networkLinks.size())
                        System.out.println("\t*** got new URLs from network links ***");
                } catch (java.net.ConnectException e) {
                    log.error("Failed to import from network link: " + uri + "\n" + e);
                } catch (FileNotFoundException e) {
                    log.error("Failed to import from network link: " + uri + "\n" + e);
                } catch (Exception e) {
                    log.error("Failed to import from network link: " + uri, e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } // while

        // add list of visited networkLinks to now empty list
        // networkLinks.addAll(visited);
        return visited;
    }

    private static String getTrimmedValue(TaggedMap map, String name) {
        String val = map.get(name);
        if (val != null) {
            val = val.trim();
            if (val.length() == 0) return null;
        }
        return val;
    }

    public static URI getLinkUri(NetworkLink link) {
        TaggedMap links = link.getLink();
        if (links != null) {
            String href = getTrimmedValue(links, HREF);
            if (href != null)
                try {
                    return new URI(href);
                } catch (URISyntaxException e) {
                    log.warn("Invalid link URI: " + href, e);
                }
        }

        return null;
    }

}

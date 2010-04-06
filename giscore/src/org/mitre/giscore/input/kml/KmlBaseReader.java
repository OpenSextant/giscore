package org.mitre.giscore.input.kml;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.TaggedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL rewriting logic extracted from KmlReader handles low-level rewriting
 * URL href if relative link along with some other helper methods.
 *
 * Makes best effort to resolve relative URLs but has some limitations such as if
 * KML has nested chain of network links with mix of KML and KMZ resources.
 * KMZ files nested inside KMZ files are not supported.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 30, 2009 12:04:01 PM
 */
public abstract class KmlBaseReader implements IKml {

	private static final Logger log = LoggerFactory.getLogger(KmlBaseReader.class);

	/**
	 * if true indicates that the stream is for a KMZ compressed file
	 * and network links with relative URLs need to be handled special
	 */
	protected boolean compressed;

	/**
	 * base URL of KML resource if passed in as a stream
	 */
	protected URL baseUrl;

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

		for (int i = 0; i < labels.length; i += 2)
			httpQueryLabels.put(labels[i], labels[i+1]);
	}

	/**
	 * names of supported viewFormat fields as of 2/19/09 in Google Earth 5.0.11337.1968 with KML 2.2
	 * see http://code.google.com/apis/kml/documentation/kmlreference.html#link
	 */
	private static final List<String> viewFormatLabels = Arrays.asList(
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
	protected static final Pattern absUrlPattern = Pattern.compile("^[a-zA-Z]+:");

	public boolean isCompressed() {
		return compressed;
	}

	/***
	 * Adjust Link href URL if httpQuery and/or viewFormat parameters are defined.
	 * Rewrites URL href if needed and returns URL as URI. Stores back href value in links
	 * TaggedMap if modifications were made to href otherwise left unchanged. 
	 *
	 * @param parent
	 * @param links TaggedMap object containing href link  @return adjusted href URL as URI, null if href is missing or empty string
	 * @return
	 */
	protected URI getLinkHref(UrlRef parent, TaggedMap links) {
		String href = links != null ? getTrimmedValue(links, HREF) : null;
		if (href == null) return null;
		URI uri = getLink(parent, href);
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

	protected URI getLink(UrlRef parent, String href) {
        // assumes href is not null nor zero length
        URI uri = null;
        try {
            if (href.indexOf(' ') != -1) {
                href = href.replace(" ", "%20"); // escape all whitespace otherwise new URI() throws an exception
                //log.debug("Escape whitespace in URL: " + href);
            }
            // check if URL is absolute otherwise its relative to base URL if defined
            if (absUrlPattern.matcher(href).lookingAt()) {
                // absolute URL (e.g. http://host/path/x.kml)
                // uri = new URL(href).toURI();
                uri = new URI(href);
                //href = url.toExternalForm();
            } else if (baseUrl == null) {
                log.warn("no base URL to resolve relative URL: " + href);
            } else {
                // relative URL
                // if compressed amd relative link then need special encoded kmz URI
				// if parent other than baseUrl then use explicit parent
				URL baseUrl = parent == null ? this.baseUrl : parent.getURL();
                //if (parent != null) System.out.format("XXX: parent=%s uisKmz=%b%n", parent, parent.isKmz());//debug
                /*
                    make best effort to resolve relative URLs but note limitations:
                    if for example parent KML includes networkLink to KMZ
                    which in turn links a KML which in turn has relative link to image overlay
                    then parent of overlay URI will not be compressed/kmz
                    and will fail to get inputStream to the image within KMZ file...
                */
                if (compressed || (parent != null && parent.getURL().getFile().endsWith(".kmz"))) {
                    //System.out.println("XXX: compressed: base="+ baseUrl);//debug
                    // if relative link and parent is KMZ file (compressed=true)
                    // then need to keep track of parent URL in addition
                    // to the relative link to match against the KMZ file entries.
                    uri = new UrlRef(baseUrl, href).getURI();
                } else {
                    //System.out.println("XXX: uncompressed: base="+ baseUrl);//debug
                    // what if from networklink that was compressed??
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

	protected static String getTrimmedValue(TaggedMap map, String name) {
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

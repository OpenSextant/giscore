/****************************************************************************************
 *  UrlRef.java
 *
 *  Created: Dec 1, 2008
 *
 *  (C) Copyright MITRE Corporation 2006
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
package org.mitre.giscore.input.kml;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

/**
 * UrlRef manages the encoding/decoding of internally created
 * KML/KMZ URLs to preserve the association between the
 * parent KMZ file and its relative file reference. Handles getting an inputStream
 * to KML linked resources whether its a fully qualified URL or a entry in a KMZ file.
 *  <p/>
 * If <code>KmlReader</code> is used to read a KMZ resource then href values for
 * relative URLs will be rewritten such that the links can be fetched later using
 * UrlRef.  A UrlRef is created for each linked resource (e.g. NetworkLink, GroundOverlay,
 * ScreenOverlay, IconStyle, etc.) during reading and an internal URI is used to reference
 * the resource.  If the parent file/URL is a KMZ file and link is a relative URL
 * then the association is preserved otherwise the URL is treated normally.
 *  <p/>
 * For example:
 *  Given the URI: <code>kmzhttp://server/test.kmz?file=kml/include.kml</code>
 *  UrlRef strips the "kmz" prefix and the "?file=" suffix from the URL resolving
 *  the resource as having a parent URL as <code>http://server/test.kmz</code> and
 *  a relative link to the file as <code>kml/include.kml</code>.
 *  <p/> 
 * Use getURI() to get the internal URI and getUrl() to return
 * the original URL.
 *
 * @author Jason Mathews
 */
public class UrlRef {

    // private static final Logger log = LoggerFactory.getLogger(UrlRef.class);

    private final URI uri;
    private final URL url;
    private final String kmzRelPath;

    private static final String MIMETYPE_KMZ = "application/vnd.google-earth.kmz";
    private static final String MIMETYPE_KML = "application/vnd.google-earth.kml+xml";
    private static final String ACCEPT_STRING = MIMETYPE_KML + ", " + MIMETYPE_KMZ + ", image/*, */*";

    /**
     * Convert URL to "kmz" URI with URL of parent KMZ and the kmz file path
     * which is the relative path to target file inside the KMZ.
     *
     * @param url  URL for KML/KMZ resource
     * @param kmz_file_path relative path within the parent KMZ archive to where the KML, overlay image,
	 * 			model, etc. is located
     * @throws URISyntaxException if URL has a missing relative file path or fails to construct properly
     * @throws NullPointerException if URL is null
     */
    public UrlRef(URL url, String kmz_file_path) throws URISyntaxException {
        this.url = url;
        if (kmz_file_path == null) {
            this.uri = url.toURI();
            this.kmzRelPath = null;
            return;
        }
        String urlStr = url.toExternalForm();
        // cleanup bad paths if needed
        if (kmz_file_path.startsWith("/"))
            kmz_file_path = kmz_file_path.substring(1);
        while (kmz_file_path.startsWith("../"))
            kmz_file_path = kmz_file_path.substring(3);
        if (kmz_file_path.length() == 0)
            throw new URISyntaxException(urlStr, "Missing relative file path");
        StringBuilder buf = new StringBuilder();
        // append kmz to front of the URL to mark as a special URI
        buf.append("kmz").append(urlStr);
        //System.out.println("path="+url.getPath());
        //System.out.println("file="+url.getFile());
        //System.out.println("query="+url.getQuery());
        if (url.getQuery() == null)
            buf.append('?');
        else
            buf.append('&');
        // append target file to URI as query part
        buf.append("file=").append(kmz_file_path);
        this.uri = new URI(buf.toString());
        this.kmzRelPath = kmz_file_path;
    }

    /**
     * Wrap URI with URLRef and decode URI if its an
     * internal kmz reference denoted with a "kmz" prefix to the URI
	 * (e.g. kmzfile:/C:/projects/giscore/data/kml/kmz/dir/content.kmz?file=kml/hi.kml). 
	 * Non-internal kmz URIs will be treated as normal URLs.
     *
     * @param uri
     * @throws  MalformedURLException
     *          If a protocol handler for the URL could not be found,
     *          or if some other error occurred while constructing the URL
     */
    public UrlRef(URI uri) throws MalformedURLException {
        this.uri = uri;
        String urlStr = uri.toString();
        if (!urlStr.startsWith("kmz")) {
            url = uri.toURL();
            kmzRelPath = null;
            return;
        }

        // handle special KMZ-encoded URI
        StringBuilder buf = new StringBuilder();
        int ind = urlStr.lastIndexOf("file=");
        // if ind == -1 then not well-formed KMZ URI
        if (ind <= 0) throw new MalformedURLException("Invalid KMZ URI missing file parameter");
        buf.append(urlStr.substring(3, ind - 1));
        // System.out.println("\trestored kmz_rel_path=" + urlStr.substring(ind + 5));
        url = new URL(buf.toString());
        kmzRelPath = urlStr.substring(ind + 5);
    }

	/**
	 * Determines if a UrlRef references a linked reference (networked linked KML,
	 * image overlay, icon, model, etc.) in a KMZ file.
	 *
	 * @return true if UrlRef reprensents a linked reference in a KMZ file
	 */
	public boolean isKmz() {
        return kmzRelPath != null;
    }

	/**
	 * Opens a connection to this <code>UrlRef</code> and returns an
     * <code>InputStream</code> for reading from that connection.
	 * 
	 * @return     an input stream for reading from the resource represented by the <code>UrlRef</code>.
	 * @throws FileNotFoundException if referenced link was not found in the parent KMZ file
	 * @throws IOException if an I/O error occurs
	 */
	public InputStream getInputStream() throws IOException {
        // check if non-KMZ URI
        if (kmzRelPath == null)
            return getInputStream(url);

        String kmzPath = kmzRelPath;
        // if whitespace appears in networkLink URLs then it's escaped to %20
        // so need to convert back to spaces to match exactly how it is stored in KMZ file
        int ind = kmzPath.indexOf("%20");
        if (ind != -1) {
            kmzPath = kmzPath.replace("%20", " "); // unescape all escaped whitespace chars
        }
        ZipInputStream zis = new ZipInputStream(url.openStream());
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (ind != -1)
                name = name.replace("%20", " "); // unescape all escaped whitespace chars
            // find matching KML file in archive
            if (kmzPath.equals(name)) {
                return zis;
            }
        }
        throw new FileNotFoundException("Relative URL not found in KMZ: " + kmzPath);
    }

    /**
     * This method gets the correct input stream for a URL.  Attempts to
	 * determine if URL is a KMZ (compressed KML file) first by the returned
	 * content type from the <code>URLConnection</code> and it that fails then
	 * by checking if a .kmz extension appears at end of the file name.
	 * If stream is for a KMZ file then the stream is advanced until the first
	 * KML file is found in the stream.
     *
     * @param url The url to the KML or KMZ file
     * @return The InputStream used to read the KML source.
     * @throws java.io.IOException when an I/O error prevents a document
     *         from being fully parsed.
     */
    public static InputStream getInputStream(URL url) throws IOException {
        // Open the connection
        URLConnection conn = url.openConnection();

        // Set HTTP headers to simulate a typical Google Earth client
        //
        // Examples:
		//
		//  Accept: application/vnd.google-earth.kml+xml, application/vnd.google-earth.kmz, image/*, */*
        //  Cache-Control: no-cache
        //  User-Agent: GoogleEarth/5.0.11337.1968(Windows;Microsoft Windows XP (Service Pack 3);en-US;kml:2.2;client:Free;type:default)
        //
        //  Accept: application/vnd.google-earth.kml+xml, application/vnd.google-earth.kmz, image/*, */*
        //  Cache-Control: no-cache
        //  User-Agent: GoogleEarth/4.3.7284.3916(Windows;Microsoft Windows XP (Service Pack 3);en-US;kml:2.2;client:Free;type:default)
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection)conn;
            httpConn.setRequestProperty("Accept", ACCEPT_STRING);
            httpConn.setRequestProperty("User-Agent",
                        "GoogleEarth/4.3.7284.3916(Windows;Microsoft Windows XP;en-US;kml:2.2;client:Free;type:default)"); 
        }

        // Connect to get the response headers
        conn.connect();

        if (MIMETYPE_KMZ.equals(conn.getContentType()) || url.getFile().toLowerCase().endsWith(".kmz")) {
            // kmz file requires special handling
            boolean closeOnExit = true;
            InputStream is = null;
            ZipInputStream zis = null;
            try {
                is = conn.getInputStream();
                zis = new ZipInputStream(is);
                ZipEntry entry;
                //   Simply find first kml file in the archive.
                //
                //   Note that KML documentation loosely defines that it takes first root-level KML file
                //   in KMZ archive as the main KML document but Google Earth (version 4.3 as of Dec-2008)
                //   actually takes the first kml file regardless of name (e.g. doc.kml which is convention only)
                //   and whether its in the root folder or subfolder. Otherwise would need to keep track
                //   of the first KML found but continue if first KML file is not in the root level then
                //   backtrack in stream to first KML if no root-level KML is found.
                while ((entry = zis.getNextEntry()) != null) {
                    // find first KML file in archive
                    if (entry.getName().toLowerCase().endsWith(".kml")) {
                        closeOnExit = false;
                        return zis; // start reading from stream
                    }
                }
                throw new FileNotFoundException("Failed to find KML content in KMZ file: " + url);
            } finally {
                if (closeOnExit) {
                     IOUtils.closeQuietly(zis);
                     IOUtils.closeQuietly(is);
                }
            }
        }

        // Else read the raw bytes.
        return new BufferedInputStream(conn.getInputStream());
    }

	/**
	 * @return the internal URI of the UrlRef
	 */
	public URI getURI() {
        return uri;
    }

	/**
	 * Returns original external URL. If "normal" URL then
	 * URL will be returned same as the URI. If internal "kmz"
	 * URI (e.g. kmzhttp://server/test.kmz?file=kml/include.kml)
	 * then URL returned is <code>http://server/test.kmz</code>.  
	 * @return original external URL
	 */
	public URL getURL() {
        return url;
    }

	/**
	 * Gets the relative path to the KMZ resource if UrlRef represents
	 * a linked reference (networked linked KML, image overlay, icon, model,
	 * etc.) in a KMZ file.  For example this would be how the Link href was
	 * explicitly defined in a NetworkLink, IconStyle, or GroundOverlay.
	 *
	 * @return relative path to the KMZ resource otherwise null
	 */
	public String getKmzRelPath() {
        return kmzRelPath;
    }

    /**
     * Convert internal "URI" form to portable URL form. For example
     * <code>kmzhttp://server/test.kmz?file=kml/include.kml</code>
	 * is converted into <code>http://server/test.kmz/kml/include.kml</code>.
	 * 
     * @return portable human-readable URL as formated String
     */
    public String toString() {
        String s = uri.toString();        
        // skip over kmz prefix in URLs for human-readable output
        // kmz prefix is for internal use only
        if (s.startsWith("kmz")) {
            s = s.substring(3);
            int ind = s.indexOf("?file=");
            if (ind != -1) s = s.substring(0, ind) + "/" + s.substring(ind + 6);
        }
        return s;
    }

}

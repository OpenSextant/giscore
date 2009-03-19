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
 * parent KMZ file and its relative KML reference.
 *  <p/>
 * A UrlRef is created for each networkLink or groundOverlay URL
 * and if the parent is KMZ and the association is preserved
 * otherwise the URL is treated normally.
 * Use getURI() to get the internal URI and getUrl() to return
 * the original URL.
 *  <p/>
 * For example:
 *   Given URI = kmzhttp://server/test.kmz?file=kml/include.kml
 *   strips the "kmz" prefix and the "?file=" suffix from the URL.
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
     * @param url  URL for KMZ resource
     * @param kmz_file_path relative path within the KMZ archive to where the KML is located
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
     * internal kmz reference denoted with a "kmz" prefix to the URI.
     *
     * @param uri
     * @throws MalformedURLException
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

    public boolean isKmz() {
        return kmzRelPath != null;
    }

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
     * This method gets the correct input stream for a URL. If the URL is a
     * http/https connection, the Accept-Encoding: gzip, deflate is added. It
     * the paramter is added, the response is checked to see if the response is
     * encoded in gzip, deflate or plain bytes. The correct input stream wrapper
     * is then selected and returned.
     *
     * @param url The url to the KML file
     * @return The InputStream used to validate and parse the SLD xml.
     * @throws java.io.IOException when an I/O error prevents a document
     *         from being fully parsed.
     */
    public static InputStream getInputStream(URL url) throws IOException {
        // Open the connection
        URLConnection conn = url.openConnection();

        // Set other HTTP headers to simulate Google Earth client
        //
        // Examples:
        // Accept: application/vnd.google-earth.kml+xml, application/vnd.google-earth.kmz, image/*, */*
        // Cache-Control: no-cache
        // User-Agent: GoogleEarth/5.0.11337.1968(Windows;Microsoft Windows XP (Service Pack 3);en-US;kml:2.2;client:Free;type:default)
        //
        // Accept: application/vnd.google-earth.kml+xml, application/vnd.google-earth.kmz, image/*, */*
        // Cache-Control: no-cache
        // User-Agent: GoogleEarth/4.3.7284.3916(Windows;Microsoft Windows XP (Service Pack 3);en-US;kml:2.2;client:Free;type:default)
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
                //   Simply find first kml file in the archive
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

    public URI getURI() {
        return uri;
    }

    public URL getURL() {
        return url;
    }

    public String getKmzRelPath() {
        return kmzRelPath;
    }

    /**
     * Convert internal "URI" form to portable URL form
     * e.g. kmzhttp://server/test.kmz?file=kml/include.kml -> http://server/test.kmz/kml/include.kml
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

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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>UrlRef</code> manages the encoding/decoding of internally created
 * KML/KMZ URLs to preserve the association between the
 * parent KMZ file and its relative file reference. Handles getting an inputStream
 * to KML linked resources whether its a fully qualified URL or a entry in a KMZ file.
 * <p/>
 * If <code>KmlReader</code> is used to read a KMZ resource then href values for
 * relative URLs will be rewritten as special URIs such that the links can be fetched
 * later using <code>UrlRef</code> to wrap the URI. A UrlRef is created for each linked
 * resource (e.g. NetworkLink, GroundOverlay, ScreenOverlay, IconStyle, etc.) during
 * reading and an internal URI is used to reference the resource.  If the parent
 * file/URL is a KMZ file and link is a relative URL then the association is preserved
 * otherwise the URL is treated normally.
 * <p/>
 * For example suppose we have a KMZ resource at <code>http://server/test.kmz</code>
 * and its root KML document includes a supporting KML file through
 * a relative link <code>kml/include.kml</code>. The URI to this networkLink
 * is returned as <code>kmzhttp://server/test.kmz?file=kml/include.kml</code>.
 * UrlRef strips the "kmz" prefix and the "?file=" suffix from the URI resolving
 * the resource as having a parent URL as <code>http://server/test.kmz</code>
 * and a relative link to the file as <code>kml/include.kml</code>.
 * <p/> 
 * Use <code>getURI()</code> to get the internal URI and <code>getUrl()</code>
 * to return the original URL.
 *
 * @author Jason Mathews
 */
public class UrlRef {

    private static final Logger log = LoggerFactory.getLogger(UrlRef.class);

    // private static final Logger log = LoggerFactory.getLogger(UrlRef.class);

    private final URI uri;
    private final URL url;
    private final String kmzRelPath;

    public static final String MIME_TYPE_KMZ = "application/vnd.google-earth.kmz";
    public static final String MIME_TYPE_KML = "application/vnd.google-earth.kml+xml";
    
    private static final String ACCEPT_STRING = MIME_TYPE_KML + ", " + MIME_TYPE_KMZ + ", image/*, */*";

    // "GoogleEarth/5.2.1.1329(Windows;Microsoft Windows (5.1.2600.3);en-US;kml:2.2;client:Free;type:default)";
    public static final String USER_AGENT = "GoogleEarth/5.2.1.1547(Windows;Microsoft Windows (5.1.2600.3);en-US;kml:2.2;client:Free;type:default)";

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
	 * @throws FileNotFoundException if referenced link was not found in the parent KMZ resource
     *          nor outside the KMZ at the same base context.
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
        boolean closeOnExit = true;
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (ind != -1)
                    name = name.replace("%20", " "); // unescape all escaped whitespace chars
                // find matching KML file in archive
                if (kmzPath.equals(name)) {
                    closeOnExit = false;
                    return zis;
                }
            }
        } finally {
            // must close ZipInputStream if failed to find entry
            if (closeOnExit)
                IOUtils.closeQuietly(zis);
        }
        // If href does not exist in KMZ then try with respect to parent context.
        // check if target exists outside of KMZ file in same context (file system or URL root).
        // e.g. http://kml-samples.googlecode.com/svn/trunk/kml/kmz/networklink/hier.kmz
        try {
            return getInputStream(new URL(url, kmzRelPath));
        } catch (IOException ioe) {
            // attempt to find target at same context of parent failed
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

        // Set HTTP headers to emulate a typical Google Earth client
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
            httpConn.setRequestProperty("User-Agent", USER_AGENT);
        }

        // Connect to get the response headers
        conn.connect();

        // Note: just looking at file extension may not be enough to indicate its KMZ vs KML (misnamed, etc.)
        // proper way might be to use PushbackInputStream and check first characters of stream.
        // KMZ/ZIP header should be PK\003\004
        String contentType = conn.getContentType();
        // contentType could end with mime parameters (e.g. application/vnd.google-earth.kmz; encoding=...) 
        if (contentType != null && contentType.startsWith(MIME_TYPE_KMZ) || url.getFile().toLowerCase().endsWith(".kmz")) {
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
	 * @return the internal URI of the UrlRef, never {@code null}
	 */
    @NonNull
	public URI getURI() {
        return uri;
    }

	/**
	 * Returns original external URL. If "normal" URL then
	 * URL will be returned same as the URI. If internal "kmz"
	 * URI (e.g. kmzhttp://server/test.kmz?file=kml/include.kml)
	 * then URL returned is <code>http://server/test.kmz</code>.  
	 * @return original external URL, never {@code null}
	 */
    @NonNull
	public URL getURL() {
        return url;
    }

	/**
	 * Gets the relative path to the KMZ resource if UrlRef represents
	 * a linked reference (networked linked KML, image overlay, icon, model,
	 * etc.) in a KMZ file.  For example this would be how the Link href was
	 * explicitly defined in a NetworkLink, IconStyle, or GroundOverlay.
	 *
	 * @return relative path to the KMZ resource otherwise {@code null}
	 */
    @CheckForNull
	public String getKmzRelPath() {
        return kmzRelPath;
    }

    /**
     * Normalize and convert internal "URI" form to portable URL form.
	 * For example <code>kmzfile:/C:/giscore/data/kml/content.kmz?file=kml/hi.kml</code>
	 * is converted into <code>file:/C:/giscore/data/kml/content.kmz/kml/hi.kml</code>.
	 * Non-file URIs only strip the kmz prefix and keep file= parameter.
	 *
     * @return portable human-readable URL as formatted String
     */
    public String toString() {
        String s = uri.toString();        
        // skip over kmz prefix in URLs for human-readable output
        // kmz prefix is for internal use only
        if (s.startsWith("kmz")) {
            s = s.substring(3);
			// at end have either have ?file= or &file=
			// rewrite if file: protocol
			if ("kmzfile".equals(uri.getScheme())) {
				int ind = s.lastIndexOf("file=");
				if (ind > 0) {
					char ch = s.charAt(--ind);
					if (ch == '?' || ch == '&')
						s = s.substring(0, ind) + "/" + s.substring(ind + 6);
				}
			}
		}
        return s;
    }

    /**
     * Test for relative identifier. True if string matches the set
     * of strings for NCName production in [Namespaces in XML].
     * Useful to test if target is reference to identifier in KML document
     * (e.g. StyleMap referencing local identifier of a Style).
     * 
     * @param str  the String to check, may be null
     * @return true if string matches a identifier reference
     */
    public static boolean isIdentifier(String str) {
        /*
         * check if string matches NCName production
         *  NCName ::=  (Letter | '_') (NCNameChar)*  -- An XML Name, minus the ":"
         *    NCNameChar ::=  Letter | Digit | '.' | '-' | '_' | CombiningChar | Extender
         */
        if (str == null) {
            return false;
        }
        int sz = str.length();
        if (sz == 0) return false;
        char c = str.charAt(0);
        if (c != '_' && !Character.isLetter(c)) return false;
        for (int i = 1; i < sz; i++) {
            c = str.charAt(i);
            if (c != '.' && c != '-' && c != '_' && !Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Escape invalid characters in URI string.
     * Must escape [] and whitespace characters
     * (e.g. http://mw1.google.com/mw-earth-vectordb/kml-samples/gp/seattle/gigapxl/$[level]/r$[y]_c$[x].jpg)
     * which would throw an URISyntaxException if URI is created from this URI string.
     * @param  href   The string to be parsed into a URI
     * @return escaped URI string
     */
    public static String escapeUri(String href) {
        /*
        excluded characters from URI syntax:

        control     = <US-ASCII coded characters 00-1F and 7F hexadecimal>
        space       = <US-ASCII coded character 20 hexadecimal>
        delims      = "<" | ">" | "#" | "%" | <">

       Other characters are excluded because gateways and other transport
       agents are known to sometimes modify such characters, or they are
       used as delimiters.

       unwise      = "{" | "}" | "|" | "\" | "^" | "[" | "]" | "`"

       Data corresponding to excluded characters must be escaped in order to
       be properly represented within a URI.

       http://www.ietf.org/rfc/rfc2396.txt
         */
        StringBuilder buf = new StringBuilder(href.length());
        for (char c : href.toCharArray()) {
            switch (c) {
                case ' ': // %20
                case '"': // %22
                case '<':
                case '>':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '`':
                case '\\':
                case '|': // %7C
                    buf.append('%').append(String.format("%02X", (int)c));
                    // note '#" is allowed in URI construction
                    break;
                default:
                    buf.append(c);
            }
        }
        String newVal = buf.toString();
        if (log.isDebugEnabled() && newVal.length() != href.length())
            log.debug("Escaped illegal characters in URL: " + href);
        return newVal;
    }
}

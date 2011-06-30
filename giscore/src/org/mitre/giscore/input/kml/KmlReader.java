/*
 *  KmlReader.java
 *
 *  @author Jason Mathews
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
 */
package org.mitre.giscore.input.kml;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;

import java.net.Proxy;
import java.net.URL;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.*;

/**
 * Wrapper to {@link KmlInputStream} that handles various house cleaning of parsing
 * KML and KMZ sources.  Caller does not need to know if target is KML or KMZ resource.
 * <p/>
 * Handles the following tasks:
 * <ul>
 * <li>read from KMZ/KML files or URLs transparently
 * <li>re-writing of URLs inside KMZ files to resolve relative URLs
 * <li>rewrites relative URLs of NetworkLinks, inline or shared IconStyles, and
 * 	 Screen/GroundOverlays with respect to parent URL.
 *   Use {@link UrlRef} to get InputStream of links and resolve URI to original URL.
 * <li>recursively read all features from referenced NetworkLinks
 * </ul>
 *
 * @author Jason Mathews, MITRE Corp.
 * Created: Mar 5, 2009 9:12:19 AM
 */
public class KmlReader extends KmlBaseReader implements IGISInputStream {

	private static final Logger log = LoggerFactory.getLogger(KmlReader.class);

	private InputStream iStream;

	private final KmlInputStream kis;

	private final List<URI> gisNetworkLinks = new ArrayList<URI>();

	private Proxy proxy;

	/**
	 * Creates a <code>KmlStreamReader</code> and attempts to read
	 * all GISObjects from a stream created from the <code>URL</code>.
	 * @param url   the KML or KMZ URL to be opened for reading.
	 * @throws java.io.IOException if an I/O error occurs
	 */
	public KmlReader(URL url) throws IOException {
			this(url, null);
	}

    /**
	 * Creates a <code>KmlStreamReader</code> and attempts to read
	 * all GISObjects from a stream created from the <code>URL</code>.
     *
	 * @param url   the KML or KMZ URL to be opened for reading, never <tt>null</tt>.
     * @param proxy the Proxy through which this connection
     *             will be made. If direct connection is desired,
     *             <code>null</code> should be specified.
     *
	 * @throws java.io.IOException if an I/O error occurs
	 * @throws NullPointerException if url is <tt>null</tt>
	 */
	public KmlReader(URL url, Proxy proxy) throws IOException {
        this.proxy = proxy;
        iStream = UrlRef.getInputStream(url, proxy);
		try {
			kis = new KmlInputStream(iStream);
		} catch (IOException e) {
			IOUtils.closeQuietly(iStream);
			throw e;
		}
		if (iStream instanceof ZipInputStream) compressed = true;
		baseUrl = url;
	}

	/**
	 * Creates a <code>KmlReader</code> and attempts
	 * to read all GISObjects from the <code>File</code>.
	 *
	 * @param      file   the KML or KMZ file to be opened for reading, never <tt>null</tt>.
	 * @throws IOException if an I/O error occurs
	 * @throws NullPointerException if file is <tt>null</tt>
	 */
	@SuppressWarnings("unchecked")
	public KmlReader(File file) throws IOException {
		if (file.getName().toLowerCase().endsWith(".kmz")) {
			// Note: some "KMZ" files fail validation using ZipFile but work with ZipInputStream
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// simply find first kml file in the archive
				// see note on KMZ in UrlRef.getInputStream() method for more detail
				if (entry.getName().toLowerCase().endsWith(".kml")) {
					iStream = zis;
					// indicate that the stream is for a KMZ compressed file
					compressed = true;
					break;
				}
			}
			if (iStream == null) {
				try {
					zis.close();
				} catch (IOException ioe) {
					// ignore
				}
				throw new FileNotFoundException("Failed to find KML content in file: " + file);
			}
		} else {
			// treat as normal .kml text file
			iStream = new BufferedInputStream(new FileInputStream(file));
		}

		try {
			kis = new KmlInputStream(iStream);
		} catch (IOException e) {
			IOUtils.closeQuietly(iStream);
			throw e;
		}

		URL url;
		try {
			url = file.toURI().toURL();
		} catch (Exception e) {
			// this should not happen
			log.warn("Failed to convert file URI to URL: " + e);
			url = null;
		}
		baseUrl = url;
	}

	/**
	 * Create KmlReader using provided InputStream.
	 *
	 * @param is  input stream for the kml content, never <code>null</code>
	 * @param isCompressed  True if the input stream is a compressed stream (e.g. KMZ resource)
 	 *				in which case relative links are resolved with respect to the baseUrl
     *              as KMZ "ZIP" entries as opposed to using the baseUrl as the parent URL context only.
	 * @param baseUrl the base URL context from which relative links are resolved
	 * @param proxy the Proxy through which URL connections
     *             will be made. If direct connection is desired,
     *             <code>null</code> should be specified.
	 * @throws IOException if an I/O error occurs
	 */
	public KmlReader(InputStream is, boolean isCompressed, URL baseUrl, Proxy proxy) throws IOException {
		try {
			kis = new KmlInputStream(is);
		} catch (IOException e) {
			IOUtils.closeQuietly(is);
			throw e;
		}
		this.proxy = proxy;
		compressed = isCompressed || is instanceof ZipInputStream;
		iStream = is;
		this.baseUrl = baseUrl;
	}

    /**
     * Returns the encoding style of the XML data.
     * @return the character encoding, defaults to "UTF-8". Never null.
     */
    @NonNull
    public String getEncoding() {
        return kis.getEncoding();
    }

	/**
	 * Get list of NetworkLinks visited.  If <code>importFromNetworkLinks()</code> was
	 * called then this will be the complete list including all NetworkLinks
	 * that are reachable starting from the base KML document and recursing
	 * into all linked KML sources.
	 *
	 * @return list of NetworkLink URIs
	 */
    @NonNull
	public List<URI> getNetworkLinks() {
		return gisNetworkLinks;
	}

	/**
	 * Reads next gis object from the stream.
	 * @return the next gis object present in the source, or <code>null</code>
	 * if there are no more objects present.
	 * @throws IOException if an I/O error occurs
	 */
    @CheckForNull
	public IGISObject read() throws IOException {
		return read(kis, null, null);
	}

	private IGISObject read(IGISInputStream inputStream, UrlRef parent, List<URI> networkLinks) throws IOException {
		IGISObject gisObj = inputStream.read();
		if (gisObj == null) return null;

		final Class<? extends IGISObject> aClass = gisObj.getClass();
		if (aClass == Feature.class) {
			Feature f = (Feature)gisObj;
			StyleSelector style = f.getStyle();
			if (style != null) {
				// handle IconStyle href if defined
				checkStyleType(parent, style);
			}
		} else if (aClass == ContainerStart.class) {
			for (StyleSelector s : ((ContainerStart)gisObj).getStyles()) {
				checkStyleType(parent, s);
			}
		} else if (gisObj instanceof NetworkLink) {
			// handle NetworkLink href
			NetworkLink link = (NetworkLink) gisObj;
			// adjust URL with httpQuery and viewFormat parameters
			// if parent is compressed and URL is relative then rewrite URL
			//log.debug("link href=" + link.getLink());
			URI uri = getLinkHref(parent, link.getLink());
			if (uri != null) {
				//log.debug(">link href=" + link.getLink());
				if (!gisNetworkLinks.contains(uri)) {
					gisNetworkLinks.add(uri);
					if (networkLinks != null) networkLinks.add(uri);
				} else log.debug("duplicate NetworkLink href");
			} else
				log.debug("NetworkLink href is empty or missing");
			// Note: NetworkLinks can have inline Styles & StyleMaps
		} else if (gisObj instanceof Overlay) {
			// handle GroundOverlay, ScreenOverlay or PhotoOverlay href
			Overlay o = (Overlay) gisObj;
			TaggedMap icon = o.getIcon();
			String href = icon != null ? getTrimmedValue(icon, HREF) : null;
			if (href != null) {
				// note PhotoOverlays may have entity replacements in URL
				// see http://code.google.com/apis/kml/documentation/photos.html
				// e.g. http://mw1.google.com/mw-earth-vectordb/kml-samples/gp/seattle/gigapxl/$[level]/r$[y]_c$[x].jpg</href>
				// Given zoom level Google Earth client maps this URL to URLs such as this: level=1 => .../0/r0_c0.jpg and level=3 => 3/r3_c1.jpg
				URI uri = getLink(parent, href);
				if (uri != null) {
					href = uri.toString();
					// store rewritten overlay URL back to property store
					icon.put(HREF, href);
					// can we have a GroundOverlay W/O LINK ??
				}
			}
			// Note: Overlays can have inline Styles & StyleMaps but should not be relevant to icon style hrefs
		} else if (aClass == Style.class) {
			// handle IconStyle href if defined
			checkStyle(parent, (Style)gisObj);
		} else if (aClass == StyleMap.class) {
			// check StyleMaps with inline Styles...
			checkStyleMap(parent, (StyleMap)gisObj);
		}

		return gisObj;
	}

	private void checkStyleType(UrlRef parent, StyleSelector s) {
		if (s instanceof Style) {
			// normalize iconStyle hrefs
			checkStyle(parent, (Style)s);
		} else if (s instanceof StyleMap) {
			checkStyleMap(parent, (StyleMap)s);
		}
	}

	private void checkStyleMap(UrlRef parent, StyleMap sm) {
		for(Iterator<Pair> it = sm.getPairs(); it.hasNext(); ) {
			Pair pair = it.next();
			StyleSelector style = pair.getStyleSelector();
			if (style instanceof Style) {
				// normalize iconStyle hrefs
				checkStyle(parent, (Style)style);
			}
			// ignore nested StyleMaps
		}
	}

	private void checkStyle(UrlRef parent, Style style) {
		if (style.hasIconStyle()) {
			String href = style.getIconUrl();
			// rewrite relative URLs with UrlRef to include context with parent source
			// note: could also use URI.isAbsolute() to test rel vs abs URL
			if (StringUtils.isNotBlank(href) && !absUrlPattern.matcher(href).lookingAt()) {
				//System.out.println("XXX: Relative iconStyle href: " + href);
				URI uri = getLink(parent, href);
				if (uri != null) {
					href = uri.toString();
					// store rewritten overlay URL back to property store
					style.setIconUrl(href);
				}
			}
		}
	}

	/**
	 * Recursively imports KML objects from all visited NetworkLinks starting
     * from the base KML document.  This must be called after reader is closed
     * otherwise an IllegalArgumentException will be thrown.
	 *
	 * @return list of visited networkLink URIs, empty list if
	 * 			no reachable networkLinks are found, never null
	 * @throws IllegalArgumentException if reader is still open
	 */
	public List<IGISObject> importFromNetworkLinks() {
        return _importFromNetworkLinks(null);
    }

	/**
	 * Recursively imports KML objects from all visited NetworkLinks starting
	 * from the base KML document.  Callback is provided to process each feature
	 * as the networkLinks are parsed.  This must be called after reader is closed
	 * otherwise an IllegalArgumentException will be thrown.
	 *
	 * @param handler ImportEventHandler is called when each new GISObject is encountered
	 * 			during parsing. This cannot be null.
	 * @throws IllegalArgumentException if ImportEventHandler is null or
	 * 			reader is still open when invoked
	 */
	public void importFromNetworkLinks(ImportEventHandler handler) {
		if (handler == null) throw new IllegalArgumentException("handler cannot be null");
		_importFromNetworkLinks(handler);
	}

	/**
	 * Recursively imports KML objects from all visited NetworkLinks starting
	 * from the base KML document.  This must be called after reader is closed
	 * otherwise an IllegalArgumentException will be thrown.
	 *
	 * @param handler ImportEventHandler is called when a new GISObject is parsed
     * @return list of visited networkLink URIs if no callback handler is specified,
     *      empty list if no reachable networkLinks are found or non-null call handler is provided
	 * @throws IllegalArgumentException if reader is still opened
	 */
	private List<IGISObject> _importFromNetworkLinks(ImportEventHandler handler) {
		if (iStream != null) throw new IllegalArgumentException("reader must first be closed");
		List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
		if (gisNetworkLinks.isEmpty()) return linkedFeatures;

		// keep track of URLs visited to prevent revisits
		Set<URI> visited = new HashSet<URI>();
		LinkedList<URI> networkLinks = new LinkedList<URI>();
		networkLinks.addAll(gisNetworkLinks);
        while (!networkLinks.isEmpty()) {
            URI uri = networkLinks.removeFirst();
            if (visited.add(uri)) {
                InputStream is = null;
				try {
                    UrlRef ref = new UrlRef(uri);
                    is = ref.getInputStream(proxy);
                    if (is == null) continue;
                    int oldSize = networkLinks.size();
                    int oldFeatSize = linkedFeatures.size();
                    KmlInputStream kis = new KmlInputStream(is);
                    if (log.isDebugEnabled()) log.debug("Parse networkLink: " + ref);
                    try {
                        IGISObject gisObj;
                        while ((gisObj = read(kis, ref, networkLinks)) != null) {
                            if (handler != null) {
                                if (!handler.handleEvent(ref, gisObj)) {
                                    // clear out temp list of links to abort following networkLinks
                                    log.info("Abort following networkLinks");
                                    networkLinks.clear();
                                    break;
                                }
                            } else
                                linkedFeatures.add(gisObj);
                        }
                    } finally {
                        kis.close();
                    }
					if (log.isDebugEnabled()) {
                        if (oldFeatSize != linkedFeatures.size())
                            log.debug("*** got features from network link ***");
                        if (oldSize != networkLinks.size())
                            log.debug("*** got new URLs from network link ***");
                    }
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

		return linkedFeatures;
	}

	/**
	 * Short-cut help method to read all GISObjects closing the stream and returning
	 * the list of GIS objects.  This is useful for most KML documents that can fit into memory
	 * otherwise read() should be used directly to iterate over each object.
	 *
	 * @return list of objects
	 * @throws IOException if an I/O error occurs
	 */
    @NonNull
	public List<IGISObject> readAll() throws IOException {
		List<IGISObject> features = new ArrayList<IGISObject>();
        try {
			IGISObject gisObj;
			while ((gisObj = read(kis, null, null)) != null) {
				features.add(gisObj);
			}
		} finally {
			close();
		}
		return features;
	}

	/**
	 * Closes this input stream and releases any system resources
     * associated with the stream.
	 * Once the reader has been closed, further read() invocations may throw an IOException.
     * Closing a previously closed reader has no effect.
	 */
	public void close() {
		if (iStream != null) {
			kis.close();
			IOUtils.closeQuietly(iStream);
			iStream = null;
		}
	}

    /**
     * Set proxy through which URL connections will be made for network links.
     * If direct connection is desired,  <code>null</code> should be specified.
     * This proxy will be used if <code>importFromNetworkLinks()</code> is called.
     * @param proxy
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Get proxy through which URL connections will be made for network links.
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * ImportEventHandler interface used for callers to implement handling
     * of GISObjects encountered as NetworkLinks are parsed. If the callback
     * handleEvent() method returns false then recursion is aborted no more
     * NetworkLink features are processed.
     * <pre>
     * KmlReader reader = new KmlReader(new URL(
     *   "http://kml-samples.googlecode.com/svn/trunk/kml/NetworkLink/visibility.kml"))
     * ... // read all features from reader
     * reader.close();
     * // reader stream must be closed (all features processed) before trying
     * // to import features from NetworkLinks.
     * reader.importFromNetworkLinks(
     *    new KmlReader.ImportEventHandler() {
     *          public boolean handleEvent(UrlRef ref, IGISObject gisObj)
     *       {
     *            // do something with gisObj
     *            return true;
     *       }
     *    });</pre>
     *
     * @see KmlReader#importFromNetworkLinks(ImportEventHandler)
     */
    public static interface ImportEventHandler {
        /**
         * The KmlReader will invoke this method for each GISObject encountered during parsing.
         * All elements will be reported in document order. Return false to abort importing
		 * features from network links.
         *
         * @param ref UriRef for NetworkLink resource
         * @param gisObj new IGISObject object. This will never be null.
		 * @return Return true to continue parsing and recursively follow NetworkLinks,
         *         false stops following NetworkLinks.
         */
		boolean handleEvent(UrlRef ref, IGISObject gisObj);
    }

	@NonNull
	public Iterator<Schema> enumerateSchemata() throws IOException {
		throw new UnsupportedOperationException();
	}
}
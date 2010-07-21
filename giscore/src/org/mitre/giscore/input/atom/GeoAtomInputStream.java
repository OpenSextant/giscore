/****************************************************************************************
 *  GeoAtomInputStream.java
 *
 *  Created: Jul 20, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
package org.mitre.giscore.input.atom;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.output.atom.IAtomConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoAtomInputStream extends GISInputStreamBase {
	private static final Logger logger = LoggerFactory.getLogger(GeoAtomInputStream.class);
	private static final XMLInputFactory ms_fact;	
	private InputStream is;
    private XMLEventReader stream;
    private Map<String,String> namespaceMap = new HashMap<String, String>(); 
    private String defaultNamespace;
	
    static {
		ms_fact = XMLInputFactory.newInstance();
    }
	
	public GeoAtomInputStream(InputStream stream) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		is = stream;
		try {
			this.stream = ms_fact.createXMLEventReader(is);
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
		DocumentStart ds = new DocumentStart(DocumentType.GeoAtom); 
		addLast(ds);
		
		try {
			readRootElement();
			readHeader();
		} catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Read the root element and record any namespaces and prefixes used for 
	 * the document. If the first element found isn't an atom:feed element then
	 * there's an error.
	 * <p>
	 * A valid document will not cause a stream exception to be thrown.
	 * 
	 * @throws XMLStreamException 
	 */
	private void readRootElement() throws XMLStreamException {
		XMLEvent ev = stream.nextEvent();
		while(ev != null && ! ev.isStartElement()) {
			ev = stream.nextEvent();
		}
		if (ev != null) {
			StartElement el = ev.asStartElement();
			if (! IAtomConstants.ATOM_URI_NS.equals(el.getName().getNamespaceURI())) {
				
			}
			Iterator<Namespace> niter = el.getNamespaces();
			while(niter.hasNext()) {
				Namespace n = niter.next();
				if (StringUtils.isBlank(n.getPrefix()))
					defaultNamespace = el.getNamespaceURI(null);
				else
					namespaceMap.put(n.getPrefix(), el.getNamespaceURI(n.getPrefix()));
			}
		}
	}

	/**
	 * Read elements from the XML stream until we find the first entry. At that
	 * point we need to stop reading and queue the atom header.
	 */
	private void readHeader() {
		
		
	}

	@Override
	public IGISObject read() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}

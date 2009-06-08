/****************************************************************************************
 *  XmlOutputStreamBase.java
 *
 *  Created: Feb 6, 2009
 *
 *  (C) Copyright MITRE Corporation 2009
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
package org.mitre.giscore.output;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.Namespace;

/**
 * A base class for those gis output stream implementations that output to XML
 * files.
 * 
 * @author DRAND
 * 
 */
public class XmlOutputStreamBase extends StreamVisitorBase implements
		IGISOutputStream {

    protected OutputStream stream;
	protected XMLStreamWriter writer;
	protected XMLOutputFactory factory; 
	
	/**
	 * Ctor
	 * 
	 * @param stream the underlying input stream.
     * @throws javax.xml.stream.XMLStreamException
	 */
	public XmlOutputStreamBase(OutputStream stream) throws XMLStreamException {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		this.stream = stream;
		factory = createFactory();
        writer = factory.createXMLStreamWriter(stream);
	}

	/**
	 * @return
	 */
	protected XMLOutputFactory createFactory() {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		return factory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mitre.giscore.output.IGISOutputStream#close()
	 */
	public void close() throws IOException {
		try {
			writer.flush();
			writer.close();
		} catch (XMLStreamException e) {
			final IOException e2 = new IOException();
			e2.initCause(e);
			throw e2;
		} finally {
            IOUtils.closeQuietly(stream);
        }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events
	 * .IGISObject)
	 */
	public void write(IGISObject object) {
		object.accept(this);
	}

	/*
	 * Don't bother quoting for these characters
	 */
	private static final String ALLOWED_SPECIAL_CHARACTERS = "{}[]\"=.-,#_!@$*()[]/:";

	/**
	 * See if the characters include special characters, and if it does use a
	 * CDATA. Special characters here means anything that isn't alphanumeric or
	 * writespace - CDATA is always valid, so this keeps it simple.
	 * 
	 * @param outputString
     * @throws XMLStreamException if there is an error with the underlying XML
	 */
	protected void handleCharacters(String outputString)
			throws XMLStreamException {
		boolean foundSpecial = false;
		for (int i = 0; i < outputString.length(); i++) {
			char ch = outputString.charAt(i);
			if (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)
					&& ALLOWED_SPECIAL_CHARACTERS.indexOf(ch) == -1) {
				foundSpecial = true;
				break;
			}
		}
		if (foundSpecial)
			writer.writeCData(outputString);
		else
			writer.writeCharacters(outputString);
	}

    /**
	 * Handle a simple element with non-null text, a common case for KML
	 *
	 * @param tag local name of the tag, may not be null
	 * @param content Content to write as parsed character data, if null then an empty XML element is written
	 * @throws XMLStreamException if there is an error with the underlying XML
	 */
    protected void handleNonNullSimpleElement(String tag, Object content) throws XMLStreamException {
        if (content != null) handleSimpleElement(tag, content);
    }

	/**
	 * Handle a simple element with text, a common case for KML
	 * 
	 * @param tag local name of the tag, may not be null
	 * @param content Content to write as parsed character data, if null then an empty XML element is written
	 * @throws XMLStreamException if there is an error with the underlying XML
	 */
	protected void handleSimpleElement(String tag, Object content)
			throws XMLStreamException {
        if (content == null)
            writer.writeEmptyElement(tag);
        else {
            writer.writeStartElement(tag);
            handleCharacters(content.toString());
            writer.writeEndElement();
        }
    }

    protected void handleSimpleElement(Namespace ns, String name, String value) throws XMLStreamException {
        if (ns == null) {
            handleSimpleElement(name, value);
        } else {
            writer.writeStartElement(ns.getPrefix(), name, ns.getURI());
            handleCharacters(value);
            writer.writeEndElement();
        }
    }

    protected void writeNamespace(Namespace ns) throws XMLStreamException {
        if (ns != null) writer.writeNamespace(ns.getPrefix(), ns.getURI());
    }

    /**
     * Simple Double formatter strips ".0" suffix (e.g. 1.0 -> 1).
     * First performs Double.toString() then strips redundant ".0" suffix if value has no fractional part.
     * @param d double value
     * @return formatted decimal value
     */
    protected static String formatDouble(double d) {
        String dval = Double.toString(d);
        int len = dval.length();
        return len > 2 && dval.endsWith(".0") ? dval.substring(0,len-2) : dval;
    }

}

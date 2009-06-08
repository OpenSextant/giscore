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
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Comment;
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
     * Creates a new XML output stream to write data to the specified 
     * underlying output stream with specified encoding.
     * The encoding on <code>writeStartDocument()</code> call to the writer must
     * match encoding of the <code>XmlOutputStreamBase</code>.
	 * 
	 * @param stream the underlying input stream
     * @param encoding the encoding to use
     * @throws XMLStreamException if there is an error with the underlying XML
	 */
	public XmlOutputStreamBase(OutputStream stream, String encoding) throws XMLStreamException {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		this.stream = stream;
		factory = createFactory();
        writer = StringUtils.isBlank(encoding)
                ? factory.createXMLStreamWriter(stream) // use default encoding 'Cp1252'
                : factory.createXMLStreamWriter(stream, encoding);
	}

    /**
     * Creates a new XML output stream to write data to the specified
     * underlying output stream with default encoding 'Cp1252'.
	 *
	 * @param stream the underlying input stream.
     * @throws XMLStreamException if there is an error with the underlying XML
	 */
	public XmlOutputStreamBase(OutputStream stream) throws XMLStreamException {
        this(stream, null);
    }

	/**
	 * @return
	 */
	protected XMLOutputFactory createFactory() {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		return factory;
	}

    /**
     * Close this writer and free any resources associated with the
     * writer.  This also closes the underlying output stream.
     * 
     * @throws IOException if an error occurs
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
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events.IGISObject)
	 */
	public void write(IGISObject object) {
		object.accept(this);
	}

	/*
	 * Don't bother quoting for these characters
	 */
	private static final String ALLOWED_SPECIAL_CHARACTERS = "{}[]\"=.-,#_!@$*()[]/:";

    /**
     * Writes XML comment to the output stream if text comment value is not null or empty.
     * The comment can contain any unescaped character (e.g. "declarations for <head> & <body>")
     * and any occurences of "--" (double-hyphen) will be hex-escaped to &#x2D;&#x2D;
     *  
     * @param comment Comment, never <code>null</code>
     */
    @Override
    public void visit(Comment comment) {
        String text = comment.getText();
        // ignore empty or null comments
        if (StringUtils.isNotEmpty(text))
            try {
                // string "--" (double-hyphen) MUST NOT occur within comments. Any other character may appear inside
                // e.g. <!-- declarations for <head> & <body> -->
                text = text.replace("--", "&#x2D;&#x2D;");
                StringBuilder buf = new StringBuilder();
                // prepend comment with space if not already whitespace
                if (!Character.isWhitespace(text.charAt(0)))
                    buf.append(' ');
                buf.append(text);
                // append comment text with space if not already whitespace
                if (text.length() == 1 || !Character.isWhitespace(text.charAt(text.length() - 1)))
                    buf.append(' ');
                writer.writeComment(buf.toString());
                writer.writeCharacters("\n");
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
    }

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
        writer.writeCharacters("\n");
    }

    /**
     * Handles simple element with a namespace.
     * 
     * @param ns Namespace. If null then delagates to handleSimpleElement with name and value. 
     * @param tag local name of the element, may not be null
	 * @param content Content to write as parsed character data, if null then an empty XML element is written
     * @throws XMLStreamException if there is an error with the underlying XML
     */
    protected void handleSimpleElement(Namespace ns, String tag, String content) throws XMLStreamException {
        if (ns == null) {
            handleSimpleElement(tag, content);
        } else {
            writer.writeStartElement(ns.getPrefix(), tag, ns.getURI());
            handleCharacters(content);
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }

    /**
     * Writes a namespace to the output stream.
     * If the prefix argument to this method is the empty string,
     * "xmlns", or null this method will delegate to writeDefaultNamespace
     * @param ns Namespace
     * @throws XMLStreamException if there is an error with the underlying XML
     * @throws IllegalStateException if the current state does not allow Namespace writing
     */
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

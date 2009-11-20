/****************************************************************************************
 *  CsvOutputStream.java
 *
 *  Created: Apr 10, 2009
 *
 *  @author DRAND
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
package org.mitre.giscore.output.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.ObjectUtils;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.StreamVisitorBase;
import org.mitre.giscore.utils.SafeDateFormat;

/**
 * A tabular output stream that creates a csv file given a single schema and a
 * set of {@link Row} or row subclass objects. This is a lossy transition if
 * there is data beyond the fields, and of course if the data cannot be
 * represented in a lossless fashion.
 *
 * @author DRAND
 */
public class CsvOutputStream extends StreamVisitorBase implements
        IGISOutputStream {
    private static final String ISO_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private SafeDateFormat dateFormatter;

    private Character valueDelimiter = ',';
    private String lineDelimiter = "\n";
    private Character quote = '"';
    private Boolean skipHeader = false;
    
    /**
     * The writer used to output the data, never <code>null</code> after
     * construction
     */
    private Writer writer = null;

    /**
     * The schema, may or may not be included. If included it must be the
     * first thing seen by the writer before any Row or Row subclass is
     * encountered. If it is not then an exception will be thrown.
     */
    private Schema schema = null;

    /**
     * Set to <code>true</code> after a row has been written for the first time.
     */
    private boolean writtenRow = false;

    /**
     * Ctor
     *
     * @param outputStream the output stream
     * @param arguments    the optional arguments, if present the first argument in the
     *                     array contains the value delimiter as a Character, the second
     *                     argument contains the line delimiter as a Character and the
     *                     third argument contains the quote character as a Character
     * @throws UnsupportedEncodingException if encoding causes an error
     */
    public CsvOutputStream(OutputStream outputStream, Object[] arguments)
            throws UnsupportedEncodingException {
        if (outputStream == null) {
            throw new IllegalArgumentException(
                    "outputStream should never be null");
        }
        writer = new OutputStreamWriter(outputStream, "UTF8");
        if (arguments != null) {
            if (arguments.length > 0 && arguments[0] != null) {
                lineDelimiter = (String) arguments[0];
            }
            if (arguments.length > 1 && arguments[1] != null) {
                valueDelimiter = (Character) arguments[1];
            }
            if (arguments.length > 2 && arguments[2] != null) {
                quote = (Character) arguments[2];
            }
            if (arguments.length > 3 && arguments[3] != null) {
            	skipHeader = (Boolean) arguments[3];
            }
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
        if (object == null) {
            throw new IllegalArgumentException(
                    "object should never be null");
        }
        object.accept(this);
    }

    /*
      * (non-Javadoc)
      *
      * @see java.io.Closeable#close()
      */
    public void close() throws IOException {
        writer.close();
    }

    /* (non-Javadoc)
      * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.Feature)
      */
    @Override
    public void visit(Feature feature) {
        visit((Row) feature);
    }

    /* (non-Javadoc)
      * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.Row)
      */
    @Override
    public void visit(Row row) {
        if (row == null) {
            throw new IllegalArgumentException(
                    "row should never be null");
        }
        writtenRow = true;
        boolean first = true;

        if (schema != null && row.getSchema() != null) {
            URI schemauri = row.getSchema();
            if (!schemauri.equals(schema.getId())) {
                throw new RuntimeException("Row schema doesn't match schema given");
            }
            try {
                for (String fieldname : schema.getKeys()) {
                    SimpleField field = schema.get(fieldname);
                    handleRow(row, first, field);
                    first = false;
                }
                writer.write(lineDelimiter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                for (SimpleField field : row.getFields()) {
                    handleRow(row, first, field);
                    first = false;
                }
                writer.write(lineDelimiter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Write the row's data for a given field
     *
     * @param row Row containing field to be written
     * @param first boolean indicating if this is the first field in row
     * @param field SimpleField to be written
     * @return data object value for this field
     * @throws IOException if an IO error occurs
     */
    private Object handleRow(Row row, boolean first, SimpleField field)
            throws IOException {
        if (!first) writer.write(valueDelimiter);
        Object value = row.getData(field);
        String outputString = formatValue(field.getType(), value);
        writer.write(escape(outputString));
        return value;
    }

    /**
     * Escape the string if it contains a quote character, otherwise just pass
     * it back unchanged
     *
     * @param outputString String to be escaped if necessary
     * @return escaped outputString
     */
    private String escape(String outputString) {
        StringBuilder rval = new StringBuilder(outputString.length() + 10);
        rval.append(quote);
        int count = outputString.length();
        for (int i = 0; i < count; i++) {
            char ch = outputString.charAt(i);
            if (ch == quote.charValue()) {
                rval.append(quote);
            }
            rval.append(ch);
        }
        rval.append(quote);
        return rval.toString();
    }

    // Thread-safe date formatter helper method
    private SafeDateFormat getDateFormatter() {
        if (dateFormatter == null) {
            SafeDateFormat thisDateFormatter = new SafeDateFormat(ISO_DATE_FMT);
            thisDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            dateFormatter = thisDateFormatter;
        }
        return dateFormatter;
    }

    /**
     * Format a value according to the type, defaults to using toString.
     *
     * @param type the type, assumed not <code>null</code>
     * @param data the data, may be a number of types, but must be coercible to
     *             the given type
     * @return a formatted value
     * @throws IllegalArgumentException if values cannot be formatted
     *                                  using specified data type.
     */
    private String formatValue(Type type, Object data) {
        if (data == null) {
            return "";
        } else if (Type.DATE.equals(type)) {
            Object val = data;
            if (val instanceof String) {
                try {
                    // Try converting to ISO?
                    val = KmlInputStream.parseDate((String) data);
                } catch (Exception e) {
                    // Fall through
                }
            }
            if (val instanceof Date) {
                return getDateFormatter().format((Date) val);
            } else {
                return val.toString();
            }
        } else if (Type.DOUBLE.equals(type) || Type.FLOAT.equals(type)) {
            if (data instanceof String) {
                return (String) data;
            }

            if (data instanceof Number) {
                return String.valueOf(data);
            } else {
                throw new IllegalArgumentException("Data that cannot be coerced to float: " + data);
            }
        } else if (Type.INT.equals(type) || Type.SHORT.equals(type)
                || Type.UINT.equals(type) || Type.USHORT.equals(type)) {
            if (data instanceof String) {
                return (String) data;
            }

            if (data instanceof Number) {
                return String.valueOf(data);
            } else {
                throw new IllegalArgumentException("Data that cannot be coerced to int: " + data);
            }
        } else {
            return data.toString();
        }
    }

    /* (non-Javadoc)
      * @see org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events.Schema)
      */
    @Override
    public void visit(Schema s) {
        if (writtenRow) {
            throw new RuntimeException("Can't set the schema after a row has been written");
        }
        if (schema != null) {
            throw new RuntimeException("Can't set the schema after a schema has already been set");
        }
        schema = s;
        if (skipHeader) return;
        
        boolean first = true;
        try {
            for (String field : schema.getKeys()) {
                if (!first) writer.write(valueDelimiter);
                writer.write(quote);
                writer.write(field);
                writer.write(quote);
                first = false;
            }
			writer.write(lineDelimiter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

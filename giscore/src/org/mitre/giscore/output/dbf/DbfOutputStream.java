/****************************************************************************************
 *  DbfOutputStream.java
 *
 *  Created: Jun 24, 2009
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
package org.mitre.giscore.output.dbf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.SimpleField.Type;
import org.mitre.giscore.input.dbf.IDbfConstants;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.shapefile.BinaryOutputStream;
import org.mitre.giscore.utils.ObjectBuffer;
import org.mitre.giscore.utils.StringHelper;

/**
 * Output a DBF file using the gisoutputstream interface.
 * 
 * @author DRAND
 */
public class DbfOutputStream implements IGISOutputStream, IDbfConstants {
	private static final String US_ASCII = "US-ASCII";
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private DateFormat inputDateFormats[] = new DateFormat[] {
			new SimpleDateFormat(IKml.ISO_DATE_FMT),
			new SimpleDateFormat("MM/dd/yyyy hh:mm:ss"),
			new SimpleDateFormat("MM/dd/yyyy hh:mm:ss"),
			new SimpleDateFormat("MM/dd/yyyy hh:mm"),
			new SimpleDateFormat("MM/dd/yyyy"),
			new SimpleDateFormat("dd-MMM-yyyy"), dateFormat };
	private DecimalFormat decimalFormat = new DecimalFormat(
			"+###############0.################;-###############0.################");

	/**
	 * Output stream, set in ctor.
	 */
	private BinaryOutputStream stream;

	/**
	 * A data holder for the rows being written.
	 */
	private ObjectBuffer buffer = new ObjectBuffer(2000);

	/**
	 * The schema. The first object handled must be the schema. This value
	 * should never be <code>null</code> after that. If a second schema arrives
	 * an illegal state exception is thrown.
	 */
	private Schema schema = null;

	/**
	 * Track the number of records to save in the header
	 */
	private int numRecords = 0;

	/**
	 * Ctor
	 * 
	 * @param outputStream
	 *            the output stream
	 * @param arguments
	 *            the optional arguments, none are defined for this stream
	 * @throws IOException
	 */
	public DbfOutputStream(OutputStream outputStream, Object[] arguments)
			throws IOException {
		if (outputStream == null) {
			throw new IllegalArgumentException(
					"outputStream should never be null");
		}
		stream = new BinaryOutputStream(outputStream);

		// Write the xBaseFile signature (should be 0x03 for dBase III)
		stream.writeByte(SIGNATURE);
	}

	@Override
	public void write(IGISObject object) {
		if (object instanceof Schema) {
			if (schema == null) {
				schema = (Schema) object;
			} else {
				throw new IllegalStateException(
						"Dbf can only handle one set of column definitions");
			}
		} else if (object instanceof Row) {
			try {
				writeRow((Row) object);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void writeRow(Row object) throws IOException {
		numRecords++;
		buffer.write(object);
	}

	@Override
	public void close() throws IOException {
		if (stream != null) {
			if (buffer.count() > 65535) {
				throw new IllegalStateException(
						"Trying to persist too many elements to DBF file, only 2^16 - 1 are allowed");
			}

			// Write today's date as the date of last update (3 byte binary YY
			// MM DD
			// format)
			String today = dateFormat.format(new Date(System
					.currentTimeMillis()));
			// 2 digit year is written with Y2K +1900 assumption so add 100
			// since
			// we're past 2000
			stream.write(100 + Byte.parseByte(today.substring(2, 4)));
			for (int i = 4; i <= 6; i += 2)
				stream.write(Byte.parseByte(today.substring(i, i + 2)));

			// Write record count, header length (based on number of fields),
			// and
			// record length
			stream.writeInt(buffer.count(), ByteOrder.LITTLE_ENDIAN);
			stream.writeShort((short) ((schema.getKeys().size() * 32) + 33),
					ByteOrder.LITTLE_ENDIAN);
			stream.writeShort((short) getRecordLength(), ByteOrder.LITTLE_ENDIAN);

			// Fill in reserved and unused header fields we don't care about
			// with
			// zeros
			for (int k = 0; k < 20; k++)
				stream.writeByte(NUL);

			byte len[] = outputHeader();
			
			try {
				outputRows(len);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			stream.close();
			stream = null;
		}
	}

	private short getRecordLength() {
		short rval = 1; // Marker byte for deleted records
		for (String fieldname : schema.getKeys()) {
			SimpleField field = schema.get(fieldname);
			rval += getFieldLength(field);
		}
		return rval;
	}

	private int getFieldLength(SimpleField field) {
		Type ft = field.getType();
		int fieldlen = field.getLength();
		if (Type.STRING.equals(field.getType())) {
			if (fieldlen > MAX_CHARLEN)
				fieldlen = MAX_CHARLEN;
		} else if (Type.DOUBLE.equals(ft) || Type.FLOAT.equals(ft)) {
			fieldlen = 34;
		} else if (Type.INT.equals(ft) || Type.UINT.equals(ft)) {
			fieldlen = 10;
		} else if (Type.SHORT.equals(ft) || Type.USHORT.equals(ft)) {
			fieldlen = 6;
		} else if (Type.OID.equals(ft)) {
			fieldlen = 10;
		} else if (Type.DATE.equals(ft)) {
			fieldlen = 8;
		} else if (Type.BOOL.equals(ft)) {
			fieldlen = 1;
		} else {
			fieldlen = 32;
		}
		return fieldlen;
	}

	private void outputRows(byte len[]) throws ClassNotFoundException,
			IOException, InstantiationException, IllegalAccessException {
		Row row = (Row) buffer.read();

		while (row != null) {
			stream.writeByte(' ');
			int i = 0;
			for (String fieldname : schema.getKeys()) {
				SimpleField field = schema.get(fieldname);
				short length = (short) len[i++];
				if (length < 0) length += 256;
				Type ft = field.getType();
				if (Type.STRING.equals(field.getType())) {
					String data = getString(row.getData(field));
					writeField(stream, data, length);
				} else if (Type.DOUBLE.equals(ft) || Type.FLOAT.equals(ft)) {
					Number data = getNumber(row.getData(field));
					if (data == null)
						writeField(stream, "", length);
					else
						writeField(stream, decimalFormat.format(data
								.doubleValue()), 34);
				} else if (Type.INT.equals(ft) || Type.UINT.equals(ft)
						|| Type.OID.equals(ft)) {
					Number data = getNumber(row.getData(field));
					if (data != null) {
						int val = (int) data.longValue();
						writeField(stream, Integer.toString(val), 10);
					} else {
						writeField(stream, "", 10);
					}
				} else if (Type.SHORT.equals(ft) || Type.USHORT.equals(ft)) {
					Number data = getNumber(row.getData(field));
					if (data != null) {
						short val = (short) data.longValue();
						writeField(stream, Short.toString(val), 6);
					} else {
						writeField(stream, "", 6);
					}
				} else if (Type.DATE.equals(ft)) {
					Date data = getDate(row.getData(field));
					if (data != null) {
						writeField(stream, dateFormat.format(data), 8);
					} else {
						writeField(stream, "", 8);
					}
				} else if (Type.BOOL.equals(ft)) {
					Boolean bool = getBoolean(row.getData(field));
					if (bool == null)
						writeField(stream, "?", 1);
					else if (bool)
						writeField(stream, "t", 1);
					else
						writeField(stream, "f", 1);
				} else {
					String data = getString(row.getData(field));
					if (data == null)
						writeField(stream, "", 32);
					else
						writeField(stream, data, 32);
				}
			}
			row = (Row) buffer.read();
		}

	}

	/**
	 * Write the field data, truncating at the field length
	 * 
	 * @param stream
	 *            the stream
	 * @param data
	 *            the string to write, may be more or less than the field
	 *            length. This will be converted to ascii
	 * @param length
	 *            the field length
	 * @throws IOException
	 */
	private void writeField(BinaryOutputStream stream, String data, int length)
			throws IOException {
		byte str[] = data.getBytes(US_ASCII);
		for (int i = 0; i < length; i++) {
			if (i < str.length) {
				stream.writeByte(str[i]);
			} else {
				stream.writeByte(' ');
			}
		}
	}

	private Boolean getBoolean(Object data) {
		if (data instanceof Boolean) {
			return (Boolean) data;
		} else if (data instanceof String) {
			String val = (String) data;
			val = val.toLowerCase();
			if (val.equals("?"))
				return null;
			return val.startsWith("t") || val.startsWith("y")
					|| "1".equals(val);
		} else {
			return false;
		}
	}

	private String getString(Object data) {
		if (data == null) {
			return "";
		} else {
			return data.toString();
		}
	}

	private Date getDate(Object data) {
		if (data == null) {
			return null;
		} else if (data instanceof Date) {
			return (Date) data;
		} else {
			String dstr = data.toString();
			for (int i = 0; i < inputDateFormats.length; i++) {
				try {
					return inputDateFormats[i].parse(dstr);
				} catch (ParseException pe) {
					// Continue
				}
			}
			return null;
		}
	}

	private Number getNumber(Object data) {
		if (data == null) {
			return null;
		} else if (data instanceof Number) {
			return (Number) data;
		} else {
			String str = data.toString();
			if (str.contains(".")) {
				return new Double(str);
			} else {
				return new Long(str);
			}
		}
	}

	private byte[] outputHeader() throws IOException {
		if (schema == null) {
			throw new IllegalStateException(
					"May not write dbf without a schema");
		}
		byte len[] = new byte[schema.getKeys().size()];
		int i = 0;
		for (String fieldname : schema.getKeys()) {
			// Write the field name, padded with null bytes
			byte name[] = new byte[11];
			byte fn[] = StringHelper.esriFieldName(fieldname);
			for(int k = 0; k < 11; k++) {
				if (k < fn.length) {
					name[k] = fn[k];
				} else {
					name[k] = 0;
				}
			}
			stream.write(name); // 0 -> 10
			SimpleField field = schema.get(fieldname);
			byte type;
			int fieldlen = getFieldLength(field);
			int fielddec = 0;
			Type ft = field.getType();
			if (Type.STRING.equals(ft)) {
				type = 'C';
			} else if (Type.DOUBLE.equals(ft) || Type.FLOAT.equals(ft)) {
				type = 'F';
				fielddec = 16;
			} else if (Type.INT.equals(ft) || Type.UINT.equals(ft)) {
				type = 'F';
			} else if (Type.SHORT.equals(ft) || Type.USHORT.equals(ft)) {
				type = 'F';
			} else if (Type.OID.equals(ft)) {
				type = 'F';
			} else if (Type.DATE.equals(ft)) {
				type = 'D';
			} else if (Type.BOOL.equals(ft)) {
				type = 'L';
			} else {
				type = 'C';
			}
			len[i++] = (byte) fieldlen;
			stream.writeByte(type); // 11
			stream.writeByte(NUL); // 12
			stream.writeByte(NUL); // 13
			stream.writeByte(NUL); // 14
			stream.writeByte(NUL); // 15
			stream.writeByte(fieldlen); // 16 Field length, max 254
			stream.writeByte(fielddec); // 17 Decimal count
			stream.writeByte(NUL); // 18 Reserved
			stream.writeByte(NUL); // 19 Reserved
			stream.writeByte(1); // 20 Work area id, 01h for dbase III
			stream.writeByte(NUL); // 21 Reserved
			stream.writeByte(NUL); // 22 Reserved
			stream.writeByte(NUL); // 23 Flag for set fields
			stream.writeByte(NUL); // 24 Reserved
			stream.writeByte(NUL); // 25 Reserved
			stream.writeByte(NUL); // 26 Reserved
			stream.writeByte(NUL); // 27 Reserved
			stream.writeByte(NUL); // 28 Reserved
			stream.writeByte(NUL); // 29 Reserved
			stream.writeByte(NUL); // 30 Reserved
			stream.writeByte(Type.OID.equals(ft) ? 1 : 0); // 31 Index field
			// marker
		}

		// Write end-of-header (EOH) carriage-return character (hex 0x0d)
		stream.writeByte(EOH);
		return len;
	}
}

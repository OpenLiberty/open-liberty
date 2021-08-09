/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.ws.logging.hpel.DeserializerException;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;
import com.ibm.ws.logging.internal.WsLogRecord;

/**
 * Formatter using "BitTwiddling" format.
 * 
 * --- Header record ---
 * EYE_CATCHER     (4 bytes)  value "HPEL"
 * VERSION         (1 byte)   value: 1
 * TYPE            (1 byte)   value: '1'
 * ..begin properties..
 * NAME            (String)
 * VALUE           (String)
 * ..end properties..
 * NULL            (1 byte)
 * 
 * --- Log Record ---
 * EYE_CATCHER     (4 bytes)  value: "HPEL"
 * VERSION         (1 byte)   value: 1
 * TYPE            (1 byte)   value: '0'
 * timestamp       (long,  8 bytes)
 * COUNTER         (long,  8 bytes) // Internal counter for each Log Record.
 * LEVEL           (short, 2 bytes)
 * THREADID        (int,   4 bytes)
 * SEQUENCE        (long,  8 bytes)
 * ..begin predefined optional fields..
 * FIELD ID        (1 byte)         // index in allFields array
 * FIELD VALUE     (String)
 * ..end predefined optional fields..
 * END_OF_VALUES   (1 byte)         // -1
 * Number of param (int,   4 bytes) // -1 if getParameters() is 'null'
 * ..begin params array
 * PARAM TYPE      (1 byte)
 * PARAM VALUE     (variable)       // see writeParam() method
 * ..end params array
 * Items in map    (int,   4 bytes) // -1 if get getExtentions() is 'null'
 * ..begin extention array..
 * EXTENSION KEY   (String)
 * EXTENSION VALUE (String)
 * ..end extension array..
 * Raw data size   (int,   4 bytes) // -1 if getRawData() is 'null'
 * Raw data value  ('size' bytes)
 */
public class BinaryLogRecordSerializerImpl implements LogRecordSerializer {
	private final static byte[] EYE_CATCHER = new byte[]{0x48,0x50,0x45,0x4c}; // HPEL in ASCII
	private final static char RECORD_TYPE = '0';
	private final static char HEADER_TYPE = '1';
	private final static byte VERSION = 1;
	
	/*
	 * Class representing an optional record field.
	 */
	private static abstract class Field {
		abstract String get(RepositoryLogRecord record);
		abstract void set(RepositoryLogRecordImpl record, String value);
	};
	
	/*
	 * Array of the optional record fields. Don't change the order of these
	 * elements and don't remove existing ones since index in that array is
	 * the id of that field recorded in the file. If future enhancement would
	 * require more fields - put them in the end of the array so that they
	 * get new ids instead of reusing existing ones.
	 */
	private final static Field[] ALL_FIELDS = new Field[] {
			new Field() { // RAW MESSAGE
				String get(RepositoryLogRecord r) { return r.getRawMessage(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setMessage(v); }
			},
			//650188 - MESSAGE ID field removed.  MessageId field will be populated during serialization
			new Field() { // LOCALIZED MESSAGE
				String get(RepositoryLogRecord r) { return r.getLocalizedMessage(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setLocalizedMessage(v); }
			},	
			new Field() { // LOGGER_NAME
				String get(RepositoryLogRecord r) { return r.getLoggerName(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setLoggerName(v); }
			},
			new Field() { // RESOURCE_BUNDLE_NAME
				String get(RepositoryLogRecord r) { return r.getResourceBundleName(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setResourceBundleName(v); }
			},
			new Field() { // SOURCE_CLASS_NAME
				String get(RepositoryLogRecord r) { return r.getSourceClassName(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setSourceClassName(v); }
			},
			new Field() { // SOURCE_METHOD_NAME
				String get(RepositoryLogRecord r) { return r.getSourceMethodName(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setSourceMethodName(v); }
			},
			new Field() { // THROWN
				String get(RepositoryLogRecord r) { return r.getStackTrace(); }
				void set(RepositoryLogRecordImpl r, String v) { r.setStackTrace(v); }
			},
			new Field() { // CORRELATION
				String get(RepositoryLogRecord r) { return r.getExtension(RepositoryLogRecord.CORRELATIONID); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(RepositoryLogRecord.CORRELATIONID, v); }
			},
			new Field() { // LOCALIZABLE
				private String get(int v) {
					switch(v) {
					case WsLogRecord.REQUIRES_LOCALIZATION:
						return "Y";
					case WsLogRecord.REQUIRES_NO_LOCALIZATION:
						return "N";
					default:
						return null;
					}
				}
				String get(RepositoryLogRecord r) { return get(r.getLocalizable()); }
				void set(RepositoryLogRecordImpl r, String v) {
					if (v != null) {
						if (v.equals("Y")) {
							r.setLocalizable(WsLogRecord.REQUIRES_LOCALIZATION);
						} else if (v.equals("N")) {
							r.setLocalizable(WsLogRecord.REQUIRES_NO_LOCALIZATION);
						}
					}
				}
			},
			new Field() { // MESSAGE_LOCALE
				String get(RepositoryLogRecord r) { return realLocale(r.getMessageLocale()); }
				void set(RepositoryLogRecordImpl r, String v) { r.setMessageLocale(v); }
				private String realLocale(String locName) {
					return locName == null ? Locale.getDefault().toString() : locName;
				}
			},
			new Field() { // COMPONENT
				String get(RepositoryLogRecord r) { return r.getExtension(RepositoryLogRecord.COMPONENT); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(RepositoryLogRecord.COMPONENT, v); }
			},
			new Field() { // PRODUCT
				String get(RepositoryLogRecord r) { return r.getExtension(RepositoryLogRecord.PRODUCT); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(RepositoryLogRecord.PRODUCT, v); }
			},
			new Field() { // ORGANIZATION
				String get(RepositoryLogRecord r) { return r.getExtension(RepositoryLogRecord.ORGANIZATION); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(RepositoryLogRecord.ORGANIZATION, v); }
			},
			new Field() { // THREAD_NAME
				String get(RepositoryLogRecord r) { return r.getExtension(RepositoryLogRecord.PTHREADID); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(RepositoryLogRecord.PTHREADID, v); }
			}
	};

	// List of extensions stored in 'optional' field section and should not be records with other extensions.
	private final static String[] SPECIAL_EXTENSIONS = new String[] {
		RepositoryLogRecord.CORRELATIONID,
		RepositoryLogRecord.COMPONENT,
		RepositoryLogRecord.PRODUCT,
		RepositoryLogRecord.ORGANIZATION,
		RepositoryLogRecord.PTHREADID
	};
	
	private final static short END_OF_VALUES = -1;
	
	private void writeHeader(char type, DataOutput writer) throws IOException {
		// Write a header 
		serializeEyeCatcher(writer);
		writer.writeByte(VERSION);
		writer.writeByte(type);
	}
	
	private static long counter = 0L; // sequence number of message.
	
	private void writeFixedFields(long millis, Level level, int threadId, long seq, DataOutput writer)
		throws IOException {
		// Write time first for ease of time filtering.
		writer.writeLong(millis);
		
		// Write our own sequence number for merging purposes.
		long seqNumber;
		synchronized(BinaryLogRecordSerializerImpl.class) {
			seqNumber = counter++;
		}
		writer.writeLong(seqNumber);
		
		// Write data always present in a log record.
		writer.writeShort(level.intValue());
		writer.writeInt(threadId);
		writer.writeLong(seq);
	}
	
	private void writeParams(Object[] params, DataOutput writer)
			throws IOException {
		// Write message parameters
		if (params == null) {
			writer.writeInt(-1);
		} else {
			writer.writeInt(params.length);
			for (int i=0; i<params.length; i++) {
				writeParam(params[i], writer);
			}
		}
	}
	
	public void serialize(RepositoryLogRecord logRecord, DataOutput writer)
		throws IOException {
		writeHeader(RECORD_TYPE, writer);
		
		writeFixedFields(
				logRecord.getMillis(),
				logRecord.getLevel(),
				logRecord.getThreadID(),
				logRecord.getSequence(),
				writer);

		// Write optional fields of the WsLogRecord class
		for (byte id=0; id<ALL_FIELDS.length; id++) {
			serializeField(id, ALL_FIELDS[id].get(logRecord), writer);
		}
		
		writer.writeByte(END_OF_VALUES);
		
		writeParams(logRecord.getParameters(), writer);
		
        Map<String, String> extensionsMap=logRecord.getExtensions();
        if (extensionsMap == null) {
        	// Record it as an empty Map
        	writer.writeInt(0);
        } else {
        	// Check if need to take care of special extensions.
        	HashMap<String, String> extensionsCopy = null;
        	for (String key: SPECIAL_EXTENSIONS) {
        		if (extensionsMap.containsKey(key)) {
        			if (extensionsCopy == null) {
        				extensionsCopy = new HashMap<String, String>(extensionsMap);
        			}
        			extensionsCopy.remove(key);
        		}
        	}
        	// Copy was created since some keys were removed. Use it instead of the original.
        	if (extensionsCopy != null) {
        		extensionsMap = extensionsCopy;
        	}
        	writer.writeInt(extensionsMap.size());
        	for (Entry<String, String> entry: extensionsMap.entrySet()) {
        		writeString(entry.getKey(), writer);
        		writeString(entry.getValue(), writer);
        	}
        }
        
        byte[] rawData = logRecord.getRawData();
        if (rawData == null) {
        	writer.writeInt(-1);
        } else {
        	writer.writeInt(rawData.length);
        	writer.write(rawData);
        }
		
	}
	
	private static void serializeField(byte fldId, String fldValue, DataOutput writer) throws IOException {
		if (fldValue != null) {
			writer.writeByte(fldId);
			writeString(fldValue, writer);
		}
	}

	public void serializeFileHeader(Properties header, DataOutput writer)
			throws IOException {
		writeHeader(HEADER_TYPE, writer);
		
		for (Entry<?,?> entry: header.entrySet()) {
			if (entry.getKey() != null) {
				writeString((String)entry.getKey(), writer);
				writeString((String)entry.getValue(), writer);
			}
		}
		writeString(null, writer);
	}

	public RepositoryLogRecordImpl deserializeLogTime(DataInput reader) throws IOException {
		RepositoryLogRecordImpl logRecord = new RepositoryLogRecordImpl();
		logRecord.setMillis(reader.readLong());
		logRecord.setInternalSeqNumber(reader.readLong());
		return logRecord;
	}

	public void deserializeLogHead(RepositoryLogRecordImpl logRecord, DataInput reader) throws IOException {
		logRecord.setLevel(reader.readShort());
		logRecord.setThreadID(reader.readInt());
		logRecord.setSequence(reader.readLong());
	}

	private final static int BUFFER_SIZE = 1024;
	/*
	 * Class implementing data safe copy in chunks of BUFFER_SIZE
	 */
	private abstract static class Chunk {
		Chunk next = null;
		final Object buffer;
		final int len;
		Chunk(Object buffer, int len) {this.buffer = buffer; this.len = len;}
		void readAll(DataInput reader, int size) throws IOException {
			Chunk current = this;
			for(int remind=size-BUFFER_SIZE; remind>0; remind -= current.len) {
				current.next = readNext(reader, remind < BUFFER_SIZE ? remind : BUFFER_SIZE);
				current = current.next;
			}
		}
		void copyAll(Object dst) {
			Chunk current = this;
			for (int offset = 0; current != null; current = current.next) {
				System.arraycopy(current.buffer, 0, dst, offset, current.len);
				offset += current.len;
			}
		}
		abstract Chunk readNext(DataInput reader, int len) throws IOException;
	}
	
	/*
	 * Implementation of safe copy for array of parameters.
	 */
	private static class ParamChunk extends Chunk {
		ParamChunk(DataInput reader, int len) throws IOException {
			super(new Object[BUFFER_SIZE], len);
			Object[] buffer = (Object[])this.buffer;
			for (int i=0; i<len; i++) {
				buffer[i] = readParam(reader);
			}
		}
		Chunk readNext(DataInput reader, int len) throws IOException {
			return new ParamChunk(reader, len);
		}
	}
	
	/*
	 * Implementation of safe copy for rawData byte array.
	 */
	private static class RawDataChunk extends Chunk {
		RawDataChunk(DataInput reader, int len) throws IOException {
			super(new byte[BUFFER_SIZE], len);
			byte[] buffer = (byte[])this.buffer;
			reader.readFully(buffer, 0, len);
		}
		Chunk readNext(DataInput reader, int len) throws IOException {
			return new RawDataChunk(reader, len);
		}
	}
	
	public void deserializeLogRecord(RepositoryLogRecordImpl logRecord, DataInput reader) throws IOException {
		
		// Set default localizable value in case none was stored for the record.
		logRecord.setLocalizable(WsLogRecord.DEFAULT_LOCALIZATION);
		for (byte id = reader.readByte(); id != END_OF_VALUES; id = reader.readByte()) {
			if (id < 0) {
				throw new DeserializerException("Incorrect ID of an optional field.", "non negative", Integer.toString(id));
			}
			String value = readString(reader);
			// id's not in current allFields could mean a difference in versions. Just ignore.
			if (id < ALL_FIELDS.length) {
				ALL_FIELDS[id].set(logRecord, value);
			}			
		}
		

		//defect 650188 - the messageID value needs to be based on the ID prefix of the message, after localization
		//has occurred. Message Id is the first 10 characters of the message.
		String result = logRecord.getLocalizedMessage() != null ? logRecord.getLocalizedMessage(): logRecord.getRawMessage();
		logRecord.setMessageID(HpelMessageConverter.getMessageId(result));				
		
		int size = reader.readInt();
		Object[] params;
		if (size < 0) {
			if (size != -1) {
				throw new DeserializerException("Incorrect indecator of a 'null' value", "-1", Integer.toString(size));
			}
			params = null;
		} else {
			if (size <= BUFFER_SIZE) {
				params = new Object[size];
				for (int i=0; i<size; i++) {
					params[i] = readParam(reader);
				}
			} else {
				ParamChunk start = new ParamChunk(reader, BUFFER_SIZE);
				start.readAll(reader, size);
				// Successfully read it from the stream, create the rawData now.
				params = new Object[size];
				start.copyAll(params);
			}
		}
		logRecord.setParameters(params);
		
		size = reader.readInt();
		if (size >= 0) {
			for (int i=0; i<size; i++) {
				String key = readString(reader);
				String value = readString(reader);
				logRecord.setExtension(key, value);
			}
		} else if (size != -1) {
			throw new DeserializerException("Incorrect indecator of a 'null' value", "-1", Integer.toString(size));
		}
		
		size = reader.readInt();
		byte[] rawData;
		if (size < 0) {
			if (size != -1) {
				throw new DeserializerException("Incorrect indecator of a 'null' value", "-1", Integer.toString(size));
			}
			rawData = null;
		} else {
			if (size <= BUFFER_SIZE) {
				rawData = new byte[size];
				reader.readFully(rawData);
			} else {
				RawDataChunk start = new RawDataChunk(reader, BUFFER_SIZE);
				start.readAll(reader, size);
				// Successfully read it from the stream, create the rawData now.
				rawData = new byte[size];
				start.copyAll(rawData);
			}
		}
		logRecord.setRawData(rawData);
		
		// Ensure that no unused bytes left.
		try {
			byte value = reader.readByte();
			throw new DeserializerException("Unexpected bytes in the stream", "nothing", Byte.toString(value));
		} catch (EOFException ex) {
			// This is exactly what expected.
		}
	}

	public Properties deserializeFileHeader(DataInput reader)
			throws IOException {
		Properties header = new Properties();
		for(String key = readString(reader); key != null; key = readString(reader)) {
			header.put(key, readString(reader));
		}
		
		// Ensure that no unused bytes left.
		try {
			byte value = reader.readByte();
			throw new DeserializerException("Unexpected bytes in the stream", "nothing", Byte.toString(value));
		} catch (EOFException ex) {
			// This is exactly what expected.
		}
		return header;
	}
	
	private static void serializeEyeCatcher(DataOutput writer) throws IOException {
		writer.write(EYE_CATCHER);
	}
	
	public int getEyeCatcherSize() {
		return EYE_CATCHER.length;
	}

	public int getType(DataInput reader) throws IOException {
		
		deserializeEyeCatcher(reader);
		
		// Check format version
		byte version = reader.readByte();
		if (VERSION != version) {
			throw new DeserializerException("Wrong version of the log format", Byte.toString(VERSION), Byte.toString(version));
		}
		
		char c = (char)reader.readByte();
		switch (c) {
			case HEADER_TYPE: return HEADER;
			case RECORD_TYPE: return RECORD;
		}
		throw new DeserializerException("Unknown log record type.", "0x" + Integer.toHexString(HEADER_TYPE) + " or 0x" + Integer.toHexString(RECORD_TYPE), "0x" + Integer.toHexString(c));
	}

	private static void deserializeEyeCatcher(DataInput reader) throws IOException {
		byte[] catchedEye = new byte[EYE_CATCHER.length]; // buffer to read eyeCatcher in.
		reader.readFully(catchedEye);
		for (int i=0; i<EYE_CATCHER.length; i++) {
			if (EYE_CATCHER[i] != catchedEye[i]) {
				throw new DeserializerException("Failed to find an eyeCatcher in a log record file.",
						displayBytes(EYE_CATCHER), displayBytes(catchedEye));
			}
		}
	}
	
	private static String displayBytes(byte[] bytes) {
		if (bytes == null) {
			return "";
		}
		StringBuilder buffer = new StringBuilder("[");
		for (int i=0; i<bytes.length; i++) {
			if (i > 0) {
				buffer.append(",");
			}
			buffer.append("0x");
			buffer.append(Integer.toHexString(bytes[i]));
		}
		buffer.append("]");
		return buffer.toString();
	}
	
	// For both "find" methods use Boyer-Moore algorithm with the additional
	// assumption that the EYE_CATCHER does not have repeatable bytes.
	
	public int findFirstEyeCatcher(byte[] buffer, int off, int len) {
		if (off < 0 || off >= buffer.length) {
			throw new IllegalArgumentException("Offset should be in the buffer boundaries");
		}
		if (off + len > buffer.length) {
			len = buffer.length - off;
		}
		// i - pointer in eyeCatcher, p - in the buffer.
		int p = off + EYE_CATCHER.length - 1;
		for (int i=EYE_CATCHER.length-1; i>=0; i--, p--) {
			if (p >= off + len) {
				return -1;
			}
			byte b = buffer[p];
			if (b != EYE_CATCHER[i]) {
				if (i == EYE_CATCHER.length-1) {
					// Check if the byte is from the middle of the EYE_CATCHER
					for (i--; i>=0; i--) {
						if (b == EYE_CATCHER[i]) {
							break;
						}
					}
					// shift buffer to have the byte in i-th position
					p += EYE_CATCHER.length - i;
				} else {
					// the whole buffer should be shifted due to the assumption.
					p += EYE_CATCHER.length * 2 - i;
				}
				i = EYE_CATCHER.length;
			}
		}
		return p+1;
	}
	
	public int findLastEyeCatcher(byte[] buffer, int off, int len) {
		if (off < 0 || off >= buffer.length) {
			throw new IllegalArgumentException("Offset should be in the buffer boundaries");
		}
		if (off + len > buffer.length) {
			len = buffer.length - off;
		}
		// i - pointer in eyeCatcher, p - in the buffer.
		int p = off + len - EYE_CATCHER.length;
		for (int i=0; i<EYE_CATCHER.length; i++, p++) {
			if (p < off) {
				return -1;
			}
			byte b = buffer[p];
			if (b != EYE_CATCHER[i]) {
				if (i == 0) {
					// Check if the byte is from the middle of the EYE_CATCHER
					for (i++; i<EYE_CATCHER.length; i++) {
						if (b == EYE_CATCHER[i]) {
							break;
						}
					}
					// shift buffer to have the byte in i-th position
					p -= i + 1;
				} else {
					// the whole buffer should be shifted due to the assumption.
					p -= EYE_CATCHER.length + i + 1;
				}
				i = -1;
			}
		}
		return p-EYE_CATCHER.length;
	}
	
	/*
	 * Serialize string. Need to take care of <code>null</code> value since
	 * {@link DataOutput#writeUTF(String)} does not handle it.
	 */
	private static void writeString(String str, DataOutput writer) throws IOException {
		if (str == null) {
			writer.writeInt(0);
		} else {
			int blocks = str.length() >> 15;
			writer.writeInt(blocks+1);
			int end = 0;
			for (int i=0; i<blocks; i++) {
				int start = end;
				end = (i+1)<<15;
				writer.writeUTF(str.substring(start, end));
			}
			writer.writeUTF(str.substring(end));
		}
	}
	
	/*
	 * Deserialize string. Make sure <code>null</code> is handled properly.
	 */
	private static String readString(DataInput reader) throws IOException {
		int blocks = reader.readInt();
		String result;
		if (blocks == 0) {
			result = null;
		} else if (blocks < 0) {
			throw new DeserializerException("Wrong 'null' indicator for a string", "0", Integer.toString(blocks));
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<blocks; i++) {
				sb.append(reader.readUTF());
			}
			result = sb.toString();
		}
		return result;
	}
	
	private final static char BYTE_ID = 'B';
	private final static char SHORT_ID = 'S';
	private final static char INTEGER_ID = 'I';
	private final static char CHAR_ID = 'C';
	private final static char LONG_ID = 'L';
	private final static char FLOAT_ID = 'F';
	private final static char DOUBLE_ID = 'D';
	private final static char DATE_ID = 'T';
	private final static char STRING_ID = 'O';
	private final static char NULL_ID = 'N';

	/*
	 * Serialize parameter Object. Need to take special care of {@link Number} and
	 * {@link Date} instances since they should be of the correct type for late binding.
	 */
	private static void writeParam(Object v, DataOutput writer) throws IOException {
		if (v instanceof Byte) {
			writer.write(BYTE_ID);
			writer.writeByte(((Byte) v).byteValue());
		} else if (v instanceof Short) {
			writer.write(SHORT_ID);
			writer.writeShort(((Short) v).shortValue());
		} else if (v instanceof Integer) {
			writer.write(INTEGER_ID);
			writer.writeInt(((Integer) v).intValue());
		} else if (v instanceof Character) {
			writer.write(CHAR_ID);
			writer.writeChar(((Character) v).charValue());
		} else if (v instanceof Long) {
			writer.write(LONG_ID);
			writer.writeLong(((Long) v).longValue());
		} else if (v instanceof Float) {
			writer.write(FLOAT_ID);
			writer.writeFloat(((Float) v).floatValue());
		} else if (v instanceof Double) {
			writer.write(DOUBLE_ID);
			writer.writeDouble(((Double) v).doubleValue());
		} else if (v instanceof Date) {
			writer.write(DATE_ID);
			writer.writeLong(((Date) v).getTime());
		} else if (v != null) {
			writer.write(STRING_ID);
			writeString(v.toString(), writer);
		} else {
			writer.write(NULL_ID);
		}
	}

	/*
	 * Deserialize parameter Object. Restores {@link Number} and {@link Date} instances
	 * into their original type.
	 */
	private static Object readParam(DataInput reader) throws IOException {
		int type = reader.readUnsignedByte();
		switch (type) {
		case BYTE_ID:
			return Byte.valueOf(reader.readByte());
		case SHORT_ID:
			return Short.valueOf(reader.readShort());
		case INTEGER_ID:
			return Integer.valueOf(reader.readInt());
		case CHAR_ID:
			return Character.valueOf(reader.readChar());
		case LONG_ID:
			return Long.valueOf(reader.readLong());
		case FLOAT_ID:
			return Float.valueOf(reader.readFloat());
		case DOUBLE_ID:
			return Double.valueOf(reader.readDouble());
		case DATE_ID:
			return new Date(reader.readLong());
		case STRING_ID:
			return readString(reader);
		case NULL_ID:
			return null;
		}
		throw new DeserializerException("Wrong type of a parameter", "one of the B,S,I,C,L,F,D,T,O,N", Character.toString((char)type));
	}

}
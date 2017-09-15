/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.ibm.ejs.ras.RasHelper;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.logging.hpel.DeserializerException;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

/**
 * Formatter using "BitTwiddling" format.
 * 
 * --- Header record ---
 * EYE_CATCHER     (4 bytes)  value "HPEL"
 * VERSION         (1 byte)   value: 2
 * TYPE            (1 byte)   value: '1'
 * ..begin properties..
 * NAME            (String)
 * VALUE           (String)
 * ..end properties..
 * NULL            (1 byte)
 * 
 * --- Log Record ---
 * EYE_CATCHER     (4 bytes)  value: "HPEL"
 * VERSION         (1 byte)   value: 2
 * TYPE            (1 byte)   value: '0'
 * timestamp       (long,  8 bytes)
 * COUNTER         (long,  8 bytes) // Internal counter for each Log Record.
 * LEVEL           (short, 2 bytes)
 * THREADID        (int,   4 bytes)
 * SEQUENCE        (long,  8 bytes)
 * ..bitmap of present values.. from 1 to 4 4byte blocks
 * First bitmap byte: SSLLlPER (1 byte)
 *   bits 7 and 8 (SS) indicates the number of 4byte blocks (00-1, 01-2, 10-3, 11-4)
 *   bits 5 and 6 (LL) represent Localizable value
 *   bit 4 (l) indicates if Locale is a byte index in predefined LOCALES array.
 *   bit 3 (P) indicates if Parameters array is present
 *   bit 2 (E) indicates if Extensions are present
 *   bit 1 (R) indicates if rawData is present
 * the rest of 3 bytes in the first block and all bytes in other blocks
 * indicate if corresponding value from ALL_FIELDS is present. (3+SS*4 bytes)
 * ..end of bitmap
 * LOCALE          (1 byte if 'l' is 1, String if 'l' is 0)
 * .."parameter" block is present only if P is 1.
 * Number of param (int,   4 bytes)
 * ..begin params array
 * PARAM TYPE      (1 byte)
 * PARAM VALUE     (variable)       // see writeParam() method
 * ..end params array
 * ..end of "parameter" block
 * .."extension" block is present only if E is 1.
 * Items in map    (int,   4 bytes)
 * ..begin extention array..
 * EXTENSION KEY   (String)
 * EXTENSION VALUE (String)
 * ..end extension array..
 * ..end of "extension" block
 * .."rawData" block is present only if R is 1.
 * Raw data size   (int,   4 bytes) // -1 if getRawData() is 'null'
 * Raw data value  ('size' bytes)
 * ..end of "rawData" block.
 * ..start of special fields block. Values are present only
 * ..if bit in bitmap position corresponding to them is 1.
 * FIELD VALUE (String)
 * ..end of special fields block..
 */
public class BinaryLogRecordSerializerVersion2Impl implements LogRecordSerializer {
	private final static byte[] EYE_CATCHER = new byte[]{0x48,0x50,0x45,0x4c}; // HPEL in ASCII
	private final static char RECORD_TYPE = '0';
	private final static char HEADER_TYPE = '1';
	private final static byte VERSION = 2;
	
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
	 * get new ids instead of reusing existing ones. The size of this array
	 * should not exceed 120 elements.
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
			},
			new Field() { // XCT Request ID
				String get(RepositoryLogRecord r) { return r.getExtension(REQUESTID); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(REQUESTID, v); }
			},
			new Field() { // AppName
				String get(RepositoryLogRecord r) { return r.getExtension(APPNAME); }
				void set(RepositoryLogRecordImpl r, String v) { r.setExtension(APPNAME, v); }
			}

	};
	
	private final static String REQUESTID = "requestID";
	private final static String APPNAME = "appName";

	// List of extensions stored in 'optional' field section and should not be records with other extensions.
	private final static String[] SPECIAL_EXTENSIONS = new String[] {
		RepositoryLogRecord.CORRELATIONID,
		RepositoryLogRecord.COMPONENT,
		RepositoryLogRecord.PRODUCT,
		RepositoryLogRecord.ORGANIZATION,
		RepositoryLogRecord.PTHREADID,
		REQUESTID,
		APPNAME
	};
	
	private final static String[] locales = new String[] {
		"cs_CZ", "de_DE", "en_GB", "en_US", "es_ES", "fr_FR", "hu_HU", "it_IT",
		"ja_JP", "ko_KR", "pl_PL", "pt_BR", "ro_RO", "ru_RU", "zh_TW", "zh_CN"
	};
	
	private void writeHeader(char type, DataOutput writer) throws IOException {
		// Write a header 
		serializeEyeCatcher(writer);
		writer.writeByte(VERSION);
		writer.writeByte(type);
	}
	
	private static long counter = 0L; // sequence number of a message.
	
	private void writeFixedFields(long millis, Level level, int threadId, long seq, DataOutput writer)
		throws IOException {
		// Write time first for ease of time filtering.
		writer.writeLong(millis);
		
		// Write our own sequence number for merging purposes.
		long seqNumber;
		synchronized(BinaryLogRecordSerializerVersion2Impl.class) {
			seqNumber = counter++;
		}
		writer.writeLong(seqNumber);
		
		// Write data always present in a log record.
		writer.writeShort(level.intValue());
		writer.writeInt(threadId);
		writer.writeLong(seq);
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

		byte bitmapKey = (byte)((ALL_FIELDS.length+7)>>5); // How many 4 byte segments bitmap has, 0 - 1 segment, 3(max) - 4 segments.
		byte[] bitmap = new byte[(bitmapKey + 1)*4];
		Arrays.fill(bitmap, (byte)0);
		bitmap[0] |= bitmapKey << 6;
		
		// Set two localizable bits
		bitmap[0] |= logRecord.getLocalizable() << 4;
		
		// Figure out locale and set its bit
		String locale = logRecord.getMessageLocale();
		if (locale == null) {
			locale = Locale.getDefault().toString();
		}
		int localeId = -1;
		for (int i=0; i<locales.length; i++) {
			if (locales[i].equals(locale)) {
				localeId = i;
				bitmap[0] |= 8; // 1 << 3;
				break;
			}
		}
		
		// Set parameters bit
		Object[] params = logRecord.getParameters();
		if (params != null) {
			bitmap[0] |= 4; // 1 << 2;
		}
		
		// Calculate extensions set and then set its bit
        Map<String, String> extensionsMap=logRecord.getExtensions();
        if (extensionsMap != null) {
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
        	if (extensionsMap != null) {
        		bitmap[0] |= 2; // 1 << 1;
        	}
        }
		
		// Set rawData bit
        byte[] rawData = logRecord.getRawData();
		if (rawData != null) {
			bitmap[0] |= 1; // 1 << 0;
		}
		
		// Collect optional fields into String array and fill up the mask.
		String[] fields = new String[ALL_FIELDS.length];
		int count = 0;
		for (byte id=0; id<ALL_FIELDS.length; id++) {
			String value = ALL_FIELDS[id].get(logRecord);
			if (value != null) {
				bitmap[(id>>3) + 1] |= 1 << (id & 0x7);
				fields[count++] = value;
			}
		}
		
		// Write bitmap out
		writer.write(bitmap);
		
		// Write locale out
		if (localeId < 0) {
			writeString(locale, writer);
		} else {
			writer.writeByte(localeId);
		}
		
		// Write parameters out
		if (params != null) {
			writer.writeInt(params.length);
			for (int i=0; i<params.length; i++) {
				writeParam(params[i], writer);
			}
		}
		
		// Write extensions out
        if (extensionsMap != null) {
        	writer.writeInt(extensionsMap.size());
        	for (Entry<String, String> entry: extensionsMap.entrySet()) {
        		writeString(entry.getKey(), writer);
        		writeString(entry.getValue(), writer);
        	}
        }
        
        // Write rawData out
        if (rawData != null) {
        	writer.writeInt(rawData.length);
        	writer.write(rawData);
        }
		
		// Write collected fields out
		for (int i=0; i<count; i++) {
			writeString(fields[i], writer);
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
		int firstByte = reader.readUnsignedByte();
		byte bitmapKey = (byte)(firstByte >>> 6);
		byte[] bitmap = new byte[(bitmapKey + 1)*4];
		bitmap[0] = (byte)(0x3F & firstByte);
		reader.readFully(bitmap, 1, bitmap.length-1);
		
		// Set localizable value
		logRecord.setLocalizable(0x3 & (bitmap[0] >>> 4));
		
		if ((bitmap[0] & 8) != 0) {
			int localeId = reader.readUnsignedByte();
			if (localeId < 0 || localeId >= locales.length) {
				throw new DeserializerException("Incorrect locale indicator", "[0,"+(locales.length-1)+"]", Integer.toString(localeId));
			}
			logRecord.setMessageLocale(locales[localeId]);
		} else {
			logRecord.setMessageLocale(readString(reader));
		}
		
		// Read Parameters if present
		if ((bitmap[0] & 4)  != 0) {
			int size = reader.readInt();
			if (size < 0) {
				throw new DeserializerException("Incorrect number of parameters", "positive integer", Integer.toString(size));
			} else {
				Object[] params;
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
				logRecord.setParameters(params);
			}
		} else {
			logRecord.setParameters(null);
		}
	
		// Read extensions if present
		if ((bitmap[0] & 2) != 0) {
			int size = reader.readInt();
			if (size >= 0) {
				for (int i=0; i<size; i++) {
					String key = readString(reader);
					String value = readString(reader);
					logRecord.setExtension(key, value);
				}
			} else {
				throw new DeserializerException("Incorrect number of extensions", "positive integer", Integer.toString(size));
			}
		}
		
		// Read rawData if present
		if ((bitmap[0] & 1) != 0) {
			int size = reader.readInt();
			if (size < 0) {
				throw new DeserializerException("Incorrect size of rawData", "positive integer", Integer.toString(size));
			} else {
				byte[] rawData;
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
				logRecord.setRawData(rawData);
			}
		} else {
			logRecord.setRawData(null);
		}
		
		// There could be more fields than what we have in ALL_FIELDS since adding
		// items in that array does not require change to the version number. Just
		// ignore extensions we are not aware of.
		int bitsToCheck = Math.min((bitmap.length-1)<<3, ALL_FIELDS.length);
		for (byte id=0; id<bitsToCheck; id++) {
			if (((bitmap[(id>>3)+1]) & (1 << (id & 7))) != 0) {
				ALL_FIELDS[id].set(logRecord, readString(reader));
			}			
		}

		//defect 650188 - the messageID value needs to be based on the ID prefix of the message, after localization
		//has occurred. Message Id is the first 10 characters of the message.
		String result = logRecord.getLocalizedMessage() != null ? logRecord.getLocalizedMessage(): logRecord.getRawMessage();
		logRecord.setMessageID(HpelMessageConverter.getMessageId(result));				
		
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
			writer.writeByte(0);
		} else {
			int blocks = str.length() >> 15;
			// Use smallest number of bytes possible for number of blocks.
			if (blocks > 0x3FFFFF) {
				// Need 4 bytes
				writer.writeByte(0xc0 | (blocks >> 24));
				writer.writeByte(0xFF & (blocks >> 16));
				writer.writeByte(0xFF & (blocks >> 8));
			} else if (blocks > 0x3FFF) {
				// Need 3 bytes
				writer.writeByte(0x80 | (blocks >> 16));
				writer.writeByte(0xFF & (blocks >> 8));
			} else if (blocks > 0x3F) {
				// Need 2 bytes
				writer.writeByte(0x40 | (blocks >> 8));
			}
			// Need to add 1 to number of blocks to avoid confusion with 'null'
			writer.writeByte(0xFF & (blocks+1));
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
		int blocks = reader.readUnsignedByte();
		int bitMask = blocks & 0xC0;
		if (bitMask == 0x40) {
			// Blocks are in 2 bytes.
			blocks = ((0x3F & blocks) << 8) | reader.readUnsignedByte();
		} else if (bitMask == 0x80) {
			// Blocks are in 3 bytes.
			blocks = ((0x3F & blocks) << 16) | (reader.readUnsignedByte() << 8) | reader.readUnsignedByte();
		} else if (bitMask == 0xC0) {
			// Blocks are in 4 bytes.
			blocks = ((0x3F & blocks) << 24) | (reader.readUnsignedByte() << 16) | (reader.readUnsignedByte() << 8) | reader.readUnsignedByte();
		}
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
			if (v instanceof Throwable) {
				if (v instanceof TruncatableThrowable) {
					writeString(RasHelper.throwableToString(((TruncatableThrowable) v).getWrappedException()), writer);
				} else {
					writeString(RasHelper.throwableToString((Throwable) v), writer);
				}
			} else {
				writeString(v.toString(), writer);
			}
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
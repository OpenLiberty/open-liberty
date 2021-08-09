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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;
import com.ibm.ws.logging.hpel.DeserializerException;
import com.ibm.ws.logging.hpel.LogFileReader;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.impl.LogRecordBrowser.IInternalRecordFilter;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

/**
 * Implementation to list all records in a file.
 */
public abstract class OneLogFileRecordIterator implements Iterator<RepositoryLogRecord> {
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = OneLogFileRecordIterator.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
	
	final File file;          // file of these records.
	private final long max;
	/** File formatter for files in the underlying repository */
	private LogRecordSerializer formatter = null;
	private final IInternalRecordFilter recFilter;
	Properties header = null; // header of this file
	
	/** reading stream open to retrieve log recrods from the file */
	protected final LogFileReader reader;
	/** next record available in the file to return in next() method */
	private RepositoryLogRecordImpl nextRecord = null;
	
	private final static LogFileReader DUMMY_READER = new LogFileReader() {
		public void close() throws IOException {
		}
		public long getFilePointer() throws IOException {
			return 0;
		}
		public boolean isOpen() {
			return false;
		}
		public long length() throws IOException {
			return 0;
		}
		public void readFully(byte[] buffer, int off, int len) throws IOException {
			throw new EOFException();
		}
		public int readLength() throws IOException {
			return 0;
		}
		public void seek(long pos) throws IOException {
			throw new EOFException();
		}
	};
	
	/*
	 * The logic of figuring out the right formatter to use is as following:
	 * 1. Assume that header information is correct and try each known formatter
	 *    in turn. Use the formatter returning header information.
	 * 2. If none of the formatters was able to find find header from the beginning
	 *    of the file assume that some random bytes were inserted into the beginning
	 *    of the file and try each formatter in turn to find first record. Use the
	 *    formatter which indicates that the first record is a header record.
	 * 3. If none of the formatters were able to obtain header information from the
	 *    start of the file attempt to use the backup copy in the end of the file
	 *    which is located based on the last 4 bytes of the file.
	 * 4. If the last 4 bytes does not result in the correct header information from
	 *    any of the formatters assume that file has random bytes appended in the end.
	 *    Try every formatter in turn to locate the last record.
	 * 5. If none of the above attempts resulted in a header information assume that
	 *    both primary and backup copies of the header information are corrupted.
	 *    Try each known formatter in turn to locate a first record. Use the one which
	 *    indicates that it's a message record.
	 * 6. If case above failed as well - close reader and assume that this is not
	 *    a record file.
	 */
	
	OneLogFileRecordIterator(File file, long max, IInternalRecordFilter recFilter) {
		this.file = file;
		this.max = max;
		this.recFilter = recFilter;
		
		LogFileReader tmpReader = null;
		try {
			tmpReader = createNewReader(file);
		} catch (IOException ex) {
			// Fall through leaving tmpReader as null
		}
		if (tmpReader == null) {
			// Create a dummy reader.
			reader = DUMMY_READER;
			header = null;
			return;
		} 

	    reader = tmpReader;
		
		try {
			header = readHeaderRecord(true);
		} catch (IOException ex) {
			logger.logp(Level.SEVERE, className, "OneLogFileRecordIterator", "HPEL_NoHeaderRecordInFileHead",
					new String[] {file.getAbsolutePath(), ex.getMessage()});
			try {
				header = findAndReadHeaderRecord(true);
			} catch (IOException ex2) {
				try {
					header = readHeaderRecord(false);
				} catch (IOException ex3) {
					try {
						header = findAndReadHeaderRecord(false);
					} catch (IOException ex4) {
						// Failed to read backup copy as well.
					}
				}
				
				// After reading backup header record we need to rewind to the first message record.
				try {
					rewindToMessageRecord();
				} catch (IOException ex3) {
					logger.logp(Level.SEVERE, className, "OneLogFileRecordIterator", "HPEL_NoRecordsInFile", file.getAbsolutePath());
					close();
					return;
				}
			}
		}
		// Adjust first record according to the minimum condition.
		setPositionByMin();
	}
	
	/**
	 * Reads first or last (backup) header record in the file. It assumes
	 * uncorrupted first or last 4 bytes indicating the size of the header
	 * record. As a side effect it set <code>formatter</code> field to the
	 * deserializer which successfuly read the record.
	 * 
	 * @param forward value <code>true</code> means to read the first record
	 * 		of the file, last otherwise.
	 * @return header information
	 * @throws IOException if can't read record with any formatter.
	 */
	private Properties readHeaderRecord(boolean forward) throws IOException {
		long fileSize = reader.length();
		int headerSize;
		int tailSize;
		byte[] buffer;
		long position;
		if (forward) {
			position = 0L;
			headerSize = reader.readLength();
			if (headerSize < 0 || fileSize < headerSize + 8) {
				throw new IOException("Header size is incorrect");
			}
			buffer = new byte[headerSize];
			reader.readFully(buffer, 0, headerSize);
			tailSize = reader.readLength();
		} else {
			reader.seek(fileSize - 4);
			tailSize = reader.readLength();
			if (tailSize < 0 || fileSize < tailSize + 8) {
				throw new IOException("Header size is incorrect");
			}
			position = fileSize - tailSize - 8L;
			reader.seek(position);
			headerSize = reader.readLength();
			buffer = new byte[tailSize];
			reader.readFully(buffer, 0, tailSize);
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		for(LogRecordSerializer current: LogRepositoryBrowserImpl.KNOWN_FORMATTERS) {
			try {
				bais.reset();
				DataInputStream input = new DataInputStream(bais);
				if (LogRecordSerializer.HEADER == current.getType(input)) {
					Properties result = current.deserializeFileHeader(input);
					if (result != null) {
						formatter = current;
						if (headerSize != tailSize) {
							logger.logp(Level.WARNING, className, "readHeaderRecord", "HPEL_InconsistencyInHeaderRecordSize",
									new Object[]{Integer.toString(tailSize), Long.toString(position), Integer.toString(headerSize), file.getAbsolutePath()});
						}
						return result;
					}
				}
			} catch (IOException ex) {
				// Assume incorrect formatter.
			}
		}
		throw new IOException("No formatter can read header record.");
	}
	
	private Properties findAndReadHeaderRecord(boolean forward) throws IOException {
		long fileSize = reader.length();
		for(LogRecordSerializer current: LogRepositoryBrowserImpl.KNOWN_FORMATTERS) {
			try {
				long position;
				if (forward) {
					reader.seek(0L);
					position = seekToNextRecord(current);
				} else {
					reader.seek(fileSize);
					position = seekToPrevRecord(current);
				}
				int headerSize = reader.readLength();
				if (headerSize < 0 || fileSize < position + headerSize + 8) {
					throw new IOException("Header size is incorrect");
				}
				DataInputStream input = readRecord(headerSize);
				int tailSize = reader.readLength();
				if (LogRecordSerializer.HEADER == current.getType(input)) {
					Properties result = current.deserializeFileHeader(input);
					if (result != null) {
						formatter = current;
						if (headerSize != tailSize) {
							logger.logp(Level.WARNING, className, "findAndReadHeaderRecord", "HPEL_InconsistencyInHeaderRecordSize",
									new Object[]{Integer.toString(tailSize), Long.toString(position), Integer.toString(headerSize), file.getAbsolutePath()});
						}
						return result;
					}
				}
			} catch (IOException ex) {
				// Assume incorrect formatter.
			}
		}
		throw new IOException("No formatter can read header record.");
	}
	
	/*
	 * Rewinds to the first message record of the file. It is used when no header
	 * record was found in the beginning of the file.
	 * 
	 * This message searches for the eye catcher string and checks that it can
	 * read record in that position. It continues until there's no more eye
	 * catcher strings in the file.
	 */
	private void rewindToMessageRecord() throws IOException {
		for(LogRecordSerializer current: formatter==null ?
				LogRepositoryBrowserImpl.KNOWN_FORMATTERS :
					new LogRecordSerializer[]{formatter}) {
			long position = 0L;
			long fileSize = reader.length();
			while (position + 8 < fileSize) {
				try {
					// Need to seek on each iteration since reading corrupt record
					// may cause skip over good records.
					reader.seek(position);
					position = seekToNextRecord(current);
					int recSize = reader.readLength();
					// check that recSize is sensible.
					if (recSize > 0 && position + recSize + 8 < fileSize) {
						DataInputStream input = readRecord(recSize);
						int tailSize =  reader.readLength();
						if (LogRecordSerializer.RECORD == current.getType(input)) {
							formatter = current;
							reader.seek(position);
							if (recSize != tailSize) {
								logger.logp(Level.WARNING, className, "rewindToMessageRecord", "HPEL_InconsistencyInLogRecordSize",
										new Object[]{Integer.toString(tailSize), Long.toString(position), Integer.toString(recSize), file.getAbsolutePath()});
							}
							return;
						}
					}
				} catch (DeserializerException ex3) {
					// This means we are still in corrupted area.
				} catch (EOFException ex3) {
					// This means record is too small even for getType()
				} catch (IOException ex3) {
					// No records according to the current formatter.
					break;
				}
				// The shift of 5 bytes from the previously found record location consists
				// of 4 bytes of the record length plus one byte to avoid finding the same
				// eye catcher again.
				position += 5;
			}
		}
		throw new IOException("No formatter can find a message record.");
	}
	
	/**
	 * Verifies that <code>record</code> satisfies the minimum boundary.
	 * 
	 * @param record next record whose minimum property should be verified
	 * @return <code>true</code> if <code>record</code> should be accepted
	 * 		according to the minimum boundary.
	 */
	protected boolean verifyMin(RepositoryLogRecordImpl record) {
		 // accept them all by default.
		return true;
	}
	
	/**
	 * Positions file stream to the location of a previously read record.
	 * @param position position of a previously read log record.
	 * @return <code>true</code> if this action succeeded; <code>false</code> otherwise.
	 */
	public boolean setPosition(long position) {
		try {
			long fileSize = reader.length();
			if (fileSize > position) {
				reader.seek(position);
				return true;
			} 
			
			logger.logp(Level.SEVERE, className, "setPosition", "HPEL_OffsetBeyondFileSize", new Object[]{file, Long.valueOf(position), Long.valueOf(fileSize)});
			
		} catch (IOException ex) {
			logger.logp(Level.SEVERE, className, "setPosition", "HPEL_ErrorSettingFileOffset", new Object[]{file, Long.valueOf(position), ex.getMessage()});
			// Fall through to return false.
		}
		return false;
	}
	
	/**
	 * Retrieves position in the reader's input stream.
	 * @return position in the stream or -1L if reading has failed.
	 */
	public long getPosition() {
		try {
			return reader.getFilePointer();
		} catch (IOException ex) {
			logger.logp(Level.SEVERE, className, "getPosition", "HPEL_ErrorReadingFileOffset", new Object[]{file, ex.getMessage()});
		}
		return -1L;
	}
	
	/**
	 * Set position according to implementation of the verifyMin() method.
	 * Change position in current file as close as possible to a first record
	 * satisfying the verifyMin() condition.
	 * @return false if any problem was encountered during process, true otherwise.
	 */
	private boolean setPositionByMin() {
		// min always points to a start of a record
		long min = getPosition();
		if (min < 0) {
			return false;
		}
		try {
			long max = reader.length();
			int recSize = reader.readLength();
			if (recSize < 0 || min + recSize + 8 > max) {
				// Even first record is incomplete
				return false;
			}
			DataInputStream input = readRecord(recSize);
			if (LogRecordSerializer.RECORD != formatter.getType(input)) {
				// First record is not a record
				return false;
			}
			RepositoryLogRecordImpl record = formatter.deserializeLogTime(input);
			// First record already satisfies condition
			if (verifyMin(record)) {
				return true;
			}
			reader.seek(max);
			long nextPosition = seekToPrevRecord(formatter);
			max = nextPosition;
			
			do {
				recSize = reader.readLength();
				if (recSize < 0 || nextPosition + recSize + 8 > max) {
					// corrupt record, good records are closer to the start.
					max = nextPosition;
				} else {
					input = readRecord(recSize);
					if (LogRecordSerializer.RECORD != formatter.getType(input)) {
						// Got all the way to the header record.
						max = nextPosition;
					} else {
						record =  formatter.deserializeLogTime(input);
						if (verifyMin(record)) {
							max = nextPosition;
						} else {
							min = nextPosition + recSize + 8;
						}
					}
				}
				reader.seek((min + max)/2);
				nextPosition = seekToNextRecord(formatter);
				// loop until seek to next record returns us back to max.
			} while (nextPosition < max);
			return true;
		} catch (IOException ex) {
			logger.logp(Level.SEVERE, className, "setPositionBySeq", "Error to set position in {0} by min condition: ", new Object[]{file, ex.getMessage()});
			logger.logp(Level.SEVERE, className, "setPositionBySeq", ex.getLocalizedMessage(), ex);
			// Fall through to return false.
		} finally {
			try {
				reader.seek(min);
			} catch (IOException ex) {
				// too late to fix, allow reading logic to work around it.
				// don't use return here it would mask all uncaught exceptions
			}
		}
		
		return false;
	}
	
	public boolean hasNext() {
		if (nextRecord == null) {
			nextRecord = getNext(-1);
		}
		return nextRecord != null;
	}
	
	/**
	 * returns next record from the stream matching required condition.
	 * 
	 * @param refSequenceNumber reference internal sequence number after which we can stop searching for records and return null.
	 * 		it is ignored if value less than zero.
	 * @return repository record matching condition.
	 */
	public RepositoryLogRecord findNext(long refSequenceNumber) {
		if (nextRecord == null) {
			nextRecord = getNext(refSequenceNumber);
		}
		if (nextRecord == null || refSequenceNumber >= 0 && refSequenceNumber < nextRecord.getInternalSeqNumber()) {
			return null;
		} else {
			RepositoryLogRecord result = nextRecord;
			nextRecord = null;
			return result;
		}
	}
	
	private RepositoryLogRecordImpl getNext(long refSequenceNumber) {
		while(reader.isOpen()) {

			// The size of the next record
			int recSize = -1;
			// Initialize position to -1 to distinguish case when reading position
			// in the file failed.
			long position = -1;

			try {
				// Read the length of the file.
				long fileSize = reader.length();
				// Get position in the file
				position = reader.getFilePointer();

				DataInputStream input = null;
				
				// Indicator that there's another eyeCatcher after corrupted region.
				boolean hasEyeCatcher = false;
				
				RepositoryLogRecordImpl result = null;

				do {
					if (refSequenceNumber >= 0 && result != null && refSequenceNumber < result.getInternalSeqNumber()) {
						return null;
					}
					result = null;

					// Check if filter didn't accept previous instance
					if (recSize > 0) {
						position += recSize + 8;
					}

					// Check if run out of the file to read the next record size
					if (position + 8 >= fileSize) {
						break;
					}
					// Read the length of the next record.
					recSize = reader.readLength();

					int tailSize;
					// Check if file has enough bytes for the record.
					if (recSize < 0 || recSize > fileSize - position - 8) {
						// This can happen if either record was not completely written to the file yet or that
						// the size information is corrupted. To work around the later case look for the backup
						// copy of the record size relative to the next record in the file.
						
						long nextPosition;
						try {
							// Skip the record size info (an integer) and one byte of the eyeCatcher.
							reader.seek(position + 5);
							nextPosition = seekToNextRecord(formatter);
							hasEyeCatcher = true;
						} catch (IOException ex) {
							// No next record in the rest of the file. Try to use file size instead.
							nextPosition = reader.length();
						}
						reader.seek(nextPosition - 4);
						tailSize = reader.readLength();
						// Neither first nor last copy of the record size can be used.
						if (tailSize < 0 || tailSize > fileSize - position - 8) {
							// It's only an indicator of a problem when there's more records in the file.
							if (hasEyeCatcher) {
								logger.logp(Level.WARNING, className, "hasNext", "HPEL_ErrorReadingLogRecordDoSkip",
										new Object[]{Long.toString(position), Long.toString(nextPosition-position), file.getAbsolutePath()});
							}
							break;
						}
						reader.seek(position + 4);
						input = readRecord(tailSize);
					} else {
						input = readRecord(recSize);
					}
					tailSize = reader.readLength();

					if (recSize != tailSize) {
						logger.logp(Level.WARNING, className, "hasNext", "HPEL_InconsistencyInLogRecordSize",
								new Object[]{Integer.toString(tailSize), Long.toString(position), Integer.toString(recSize), file.getAbsolutePath()});
					}
					
					if (LogRecordSerializer.RECORD != formatter.getType(input)) {
						break;
					}
					result = formatter.deserializeLogTime(input);

					// Check if we run out of records due to the upper limit.
					if (max >= 0 && max < result.getMillis()) {
						result = null;
						break;
					}

					// initialize record's location
					result.setRepositoryPointer(getPointer(file, position));

				} while(!verifyMin(result) || !recFilter.filterAccepts(formatter, input, result));

				if (result != null) {
					return result;
				} else if (hasEyeCatcher) {
					continue;
				}
			} catch (IOException ex) {
				logger.logp(Level.SEVERE, className, "hasNext", "HPEL_ErrorReadingLogRecord",
						new String[] {file.getAbsolutePath(), ex instanceof EOFException ? "EOF" : ex.getMessage()});

				// Try to recover from the start of the failed record to consider the
				// case when it was not completely written due to disk running of the
				// space and then written again after cleanup of the repository.
				if (position > 0) {
					try {
						// Include skipping 1 byte off the eyeCatcher.
						reader.seek(position + 5);
						position = seekToNextRecord(formatter);
						continue;
					} catch (IOException ex2) {
						logger.logp(Level.SEVERE, className, "hasNext", "HPEL_ErrorSkippingFailedLogRecord",
								new String[] {file.getAbsolutePath(), ex2 instanceof EOFException ? "EOF" : ex2.getMessage()});
						// Failed to recover. Fall through to close the reader.
					}
				}
			}

			// We run out of records. Close the reader.
			close();			
		}
		
		return null;
	}

	public RepositoryLogRecord next() {
		if (!reader.isOpen() || ( nextRecord == null && !hasNext() )) {
			return null;
		}
		RepositoryLogRecord result = nextRecord;
		nextRecord = null;
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException("Method is not applicable to this class");
	}
	
	/**
	 * Close file reader used by this iterator.
	 */
	public void close() {
		try {
			reader.close();
		} catch (IOException ex) {
			// Ignore to make sure that reader is set to null
		}
	}
	
	/**
	 * @return <code>true</code> if this iterator does not have any records to return anymore
	 */
	public boolean isDone() {
		return !reader.isOpen();
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	private DataInputStream readRecord(int recSize) throws IOException {
		byte[] buffer = new byte[recSize];
		reader.readFully(buffer, 0, recSize);
		return new DataInputStream(new ByteArrayInputStream(buffer));
	}
	
	/**
	 * Repositions reader to the location of the next record.
	 * This is done by searching next eyeCatcher and then seek
	 * 4 bytes before its start.
	 * @param formatter 
	 * @return current position in the file.
	 * @throws IOException if no eyeCatcher is found in the rest of the file.
	 */
	public long seekToNextRecord(LogRecordSerializer formatter) throws IOException {
		long fileSize = reader.length();
		long position = reader.getFilePointer();
		int location;
		int len = 0;
		int offset = 0;
		byte[] buffer = new byte[2048];
		
		do {
			if (offset > 0) {
				position += len - offset;
				// keep the last eyeCatcherSize-1 bytes of the buffer.
				for (int i=0; i<offset; i++) {
					buffer[i] = buffer[buffer.length-offset+i];
				}
			}
			if (position + formatter.getEyeCatcherSize() > fileSize) {
				throw new IOException("No eyeCatcher found in the rest of the file.");
			}
			if (position + buffer.length <= fileSize) {
				len = buffer.length;
			} else {
				len = (int)(fileSize - position);
			}
			reader.readFully(buffer, offset, len-offset);
			if (offset == 0) {
				offset = formatter.getEyeCatcherSize()-1;
			}
		} while ((location = formatter.findFirstEyeCatcher(buffer, 0, len)) < 0);
		
		position += location - 4;
		reader.seek(position);
		return position;
	}
	
	/**
	 * Repositions reader to the location of the previous record.
	 * This is done by searching prev eyeCatcher and then seek
	 * 4 bytes before its start.
	 * @param formatter 
	 * @return current position in the file.
	 * @throws IOException if no eyeCatcher is found in region from start of the file to the current position.
	 */
	public long seekToPrevRecord(LogRecordSerializer formatter) throws IOException {
		long position = reader.getFilePointer();
		byte[] buffer = new byte[2048];
		int location;
		int offset = 0;
		int len = 0;
		do {
			if (position <= formatter.getEyeCatcherSize()+3) {
				throw new IOException("No eyeCatcher found in the rest of the file.");
			}
			if (position > buffer.length) {
				len = buffer.length;
			} else {
				len = (int)position;
			}
			position -= len;
			if (offset > 0) {
				// keep the first eyeCatcherSize-1 bytes of the buffer.
				for (int i=0; i<offset; i++) {
					buffer[len-offset+i] = buffer[i];
				}
			}
			reader.seek(position);
			reader.readFully(buffer, 0, len-offset);
			if (offset == 0) {
				offset = formatter.getEyeCatcherSize()-1;
			}
		} while ((location = formatter.findLastEyeCatcher(buffer, 0, len)) < 0);
		
		position += location - 4;
		reader.seek(position);
		return position;
	}
	
	/**
	 * Creates the new instance of a reader to read input data with.
	 * 
	 * @param file File instance the reader will read from.
	 * @return LogFileReader instance to use for reading.
	 * @throws IOException
	 */
	protected LogFileReader createNewReader(File file) throws IOException {
		return new LogFileReaderImpl(file);
	}
	
	/**
	 * Creates the new instance of a reader to read input data with based on an existing one.
	 * 
	 * @param other the LogFileReader instance to base this reader on.
	 * @return LogFileReader instance to use for reading.
	 * @throws IOException
	 */
	protected LogFileReader createNewReader(LogFileReader other) throws IOException {
		if (other instanceof LogFileReaderImpl) {
			return new LogFileReaderImpl((LogFileReaderImpl)other);
		}
		throw new IOException("Instance of the " + other.getClass().getName() + " is not clonable by " + OneLogFileRecordIterator.class.getName() + ".");
	}
	
	/**
	 * Creates the location instance for the record in a file.
	 * 
	 * @param file File instance of a repository file containing the record.
	 * @param position byte offset of the record in the file.
	 * @return RepositoryPointer instance uniquily identifying record in the repository.
	 */
	protected abstract RepositoryPointer getPointer(File file, long position);
}

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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.logging.hpel.reader.LogRecordFilter;
import com.ibm.websphere.logging.hpel.reader.LogRecordHeaderFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.object.hpel.RemoteRepositoryCache;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;
import com.ibm.ws.logging.object.hpel.RepositoryPointerImpl;

/**
 * Generator of log record list.
 * This class provides helper methods to browse through log records stored
 * in the repository without worrying about the split into files.
 */
public class LogRecordBrowser {
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = LogRecordBrowser.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
	
	/** File browser for the underlying repository */
	private final LogRepositoryBrowser fileBrowser;
	
	/**
	 * Creates the LogRecordBrowser instance using <code>fileBrowser</code> to find necessary files.
	 * 
	 * @param fileBrowser the ILogRepositoryBrowser implementaiton to use for file browsing.
	 * @see LogRepositoryBrowser
	 */
	public LogRecordBrowser(LogRepositoryBrowser fileBrowser) {
		//super(fileBrowser);
		this.fileBrowser = fileBrowser;
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param min the low time limit (the oldest log record).
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records.
	 */
	public OnePidRecordListImpl recordsInProcess(long min, long max, final LogRecordFilter filter) {
		return startRecordsInProcess(min, max, filter==null ? new AllAcceptVerifier() : new FullFilterVerifier(filter));
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param after the location of a record we need to restart iteration after.
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records. 
	 */
	public OnePidRecordListImpl recordsInProcess(RepositoryPointer after, long max, final LogRecordFilter filter) {
		return restartRecordsInProcess(after, max, filter==null ? new AllAcceptVerifier() : new FullFilterVerifier(filter));
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param after a record from another repository we need to restart iteration after.
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records.
	 */
	public OnePidRecordListImpl recordsInProcess(RepositoryLogRecord after, long max, final LogRecordFilter filter) {
		return restartRecordsInProcess(after, max, filter==null ? new AllAcceptVerifier() : new FullFilterVerifier(filter));
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param min the low time limit (the oldest log record).
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records. 
	 */
	public OnePidRecordListImpl recordsInProcess(long min, long max, final LogRecordHeaderFilter filter) {
		return startRecordsInProcess(min, max, filter==null ? new AllAcceptVerifier() : new HeadFilterVerifier(filter));
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param after the location of a record we need to restart iteration after.
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records. 
	 */
	public OnePidRecordListImpl recordsInProcess(RepositoryPointer after, long max, LogRecordHeaderFilter filter) {
		return restartRecordsInProcess(after, max, filter==null ? new AllAcceptVerifier() : new HeadFilterVerifier(filter));
	}
	
	/**
	 * Returns records belonging to a process and satisfying the filter.
	 * 
	 * @param after a record from another repository we need to restart iteration after.
	 * @param max the upper time limit (the youngest log record).
	 * @param filter criteria to filter logs records on.
	 * @return the iterable list of records.	 
	 */
	public OnePidRecordListImpl recordsInProcess(RepositoryLogRecord after, long max, LogRecordHeaderFilter filter) {
		return restartRecordsInProcess(after, max, filter==null ? new AllAcceptVerifier() : new HeadFilterVerifier(filter));
	}
	
	/**
	 * Returns repository record corresponding to the <code>location</code> pointer.
	 * 
	 * @param location the location of a record.
	 * @return log record instance if <code>location</code> points to a record in the repository;
	 * 		<code>null</code> otherwise.
	 */
	public RepositoryLogRecord getRecord(RepositoryPointer location) {
		if (!(location instanceof RepositoryPointerImpl)) {
			return null;
		}
		RepositoryPointerImpl real = (RepositoryPointerImpl)location;
		long pos = real.getRecordOffset();
		File file = fileBrowser.findFile(real);
		if (pos < 0 || file == null) {
			return null;
		}
		OneFileRecordIterator current = new OneFileRecordIterator(file, -1, new AllAcceptVerifier());
		RepositoryLogRecord rec = null;
		if (current.setPosition(pos)) {
			rec = current.next();
		}
		current.close();
		if (rec != null && real.equals(rec.getRepositoryPointer())) {
			return rec;
		} 		
		
		return null;
		
	}
	
	
	// list of the records in the process filtered with <code>recFilter</code>
	private OnePidRecordListImpl startRecordsInProcess(long min, long max, IInternalRecordFilter recFilter) {
		// Find first file of this process records
		File file = fileBrowser.findByMillis(min);
		if (file == null) {
			file = fileBrowser.findNext((File)null, max);
		}
		return new OnePidRecordListMintimeImpl(file, min, max, recFilter);
	}
	
	// continue the list of the records in the process filtered with <code>recFilter</code>
	private OnePidRecordListImpl restartRecordsInProcess(final RepositoryPointer after, long max, final IInternalRecordFilter recFilter) {
		if (!(after instanceof RepositoryPointerImpl)) {
			throw new IllegalArgumentException("Specified location does not belong to this repository.");
		}
		return new OnePidRecordListLocationImpl((RepositoryPointerImpl)after, max, recFilter);
	}
	
	// continue the list of the records after a record from another repository filtered with <code>recFilter</code>
	private OnePidRecordListImpl restartRecordsInProcess(final RepositoryLogRecord after, long max, final IInternalRecordFilter recFilter) {
		if (!(after instanceof RepositoryLogRecordImpl)) {
			throw new IllegalArgumentException("Specified location does not belong to this repository.");
		}
		RepositoryLogRecordImpl real = (RepositoryLogRecordImpl)after;
		File file = fileBrowser.findByMillis(real.getMillis());
		// If record's time is not in our repository, take the first file
		if (file == null) {
			file = fileBrowser.findNext((File)null, max);
		}
		return new OnePidRecordListRecordImpl(file, real, max, recFilter);
	}
	
	/*
	 * Filter which accept all records.
	 */
	private static class AllAcceptVerifier implements IInternalRecordFilter {
		public boolean filterAccepts(LogRecordSerializer formatter, DataInputStream reader, RepositoryLogRecordImpl nextRecord) throws IOException {
			formatter.deserializeLogHead(nextRecord, reader);
			formatter.deserializeLogRecord(nextRecord, reader);
			return true;
		}

	
	}
	
	/*
	 * Filter which accept records based on the head part of the log record
	 */
	private static class HeadFilterVerifier implements IInternalRecordFilter {
		private final LogRecordHeaderFilter filter;
		HeadFilterVerifier(LogRecordHeaderFilter filter) {
			this.filter = filter;
		}
		public boolean filterAccepts(LogRecordSerializer formatter, DataInputStream reader, RepositoryLogRecordImpl nextRecord) throws IOException {
			formatter.deserializeLogHead(nextRecord, reader);
			boolean result = filter.accept(nextRecord);
			if (result) {
				formatter.deserializeLogRecord(nextRecord, reader);
			}
			return result;
		}

	}
	
	/*
	 * Filter which accept records based on their full information.
	 */
	private static class FullFilterVerifier implements IInternalRecordFilter {
		private final LogRecordFilter filter;
		FullFilterVerifier(LogRecordFilter filter) {
			this.filter = filter;
		}
		public boolean filterAccepts(LogRecordSerializer formatter, DataInputStream reader, RepositoryLogRecordImpl nextRecord) throws IOException {
			formatter.deserializeLogHead(nextRecord, reader);
			formatter.deserializeLogRecord(nextRecord, reader);
			return filter.accept(nextRecord);
		}
	}
	
	/**
	 * Interface for internal filtering processing of log records
	 * @author belyi
	 */
	protected interface IInternalRecordFilter {
		/**
		 * @param formatter formatter to use for parse record data.
		 * @param reader reader to read the rest of the log record.
		 * @param nextRecord record to fill and verify.
		 * @return <code>true</code> if record passed the filter. The record should be fully
		 * 			read if <code>true</code> is return.
		 * @throws IOException on any error received during reading of the record.
		 */
		boolean filterAccepts(LogRecordSerializer formatter, DataInputStream reader, RepositoryLogRecordImpl nextRecord) throws IOException;		
		
	}
	
	/**
	 * Implementation to list all records in a file.
	 */
	private class OneFileRecordIterator extends OneLogFileRecordIterator {
		private OneFileRecordIterator(File file, long max, IInternalRecordFilter recFilter) {
			super(file, max, recFilter);
		}
		
		protected RepositoryPointer getPointer(File file, long position) {
			return new RepositoryPointerImpl(fileBrowser.getIds(), file.getName(), position);
		}
	}
	
	/**
	 * Implementation to list all records belonging to the same process
	 */
	public class OnePidRecordIterator implements Iterator<RepositoryLogRecord>, Comparable<OnePidRecordIterator> {
		private int listIndex = -1;
		private int countDown = -1;
		
		private OneFileRecordIterator current = null;
		private OneFileRecordStatistics stats = null;
		private RepositoryLogRecord nextRecord = null;
		private final OnePidRecordListImpl parent;
		
		OnePidRecordIterator(OnePidRecordListImpl parent) {
			this.parent = parent;
		}
		
		/**
		 * Returns number of records in the <code>index</code>'s file of the query
		 * result.
		 * Note, to avoid unnecessary 'if' conditions it assumes that 'index' is never
		 * out of 0 to value return by total() range. Failing this assumption call will
		 * result in NullPointerException.
		 * 
		 * @param index index of the file in the result.
		 * @return the number of records in that file.
		 */
		public int size(int index) {
			return parent.getStatistics(index, true).size;
		}
		
		/**
		 * Returns internal id of the first record in <code>index</code>'s file of the
		 * query result.
		 * Note, to avoid unnecessary 'if' conditions it assumes that 'index' is never
		 * out of 0 to value return by total() range. Failing this assumption call will
		 * result in NullPointerException.
		 * 
		 * @param index index of the file in the result.
		 * @return internal Id of the first record in that file.
		 */
		public long getFirstId(int index) {
			return parent.getStatistics(index, true).firstId;
		}
		
		/**
		 * Returns internal id of the last record in <code>index</code>'s file of the
		 * query result.
		 * Note, to avoid unnecessary 'if' conditions it assumes that 'index' is never
		 * out of 0 to value return by total() range. Failing this assumption call will
		 * result in NullPointerException.
		 * 
		 * @param index index of the file in the result.
		 * @return internal Id of the first record in that file.
		 */
		public long getLastId(int index) {
			return parent.getStatistics(index, true).lastId;
		}
		
		/**
		 * Returns the total number of files in the result.
		 * 
		 * @return the number of files in the query result.
		 */
		public int total() {
			return parent.total();
		}
		
		/**
		 * Releases resources hold by this iterator
		 */
		public void close() {
			if (current != null) {
				current.close();
				current = null;
			}
			listIndex = -1;
		}
		
		/**
		 * @return <code>true</code> if this iterator does not have any records to return anymore
		 */
		public boolean isDone() {
			return listIndex < 0;
		}
		
		/**
		 * Sets range of records for this iterator.
		 * 
		 * @param index index of the file to start from.
		 * @param offset the number of records to skip in the first file.
		 * @param length the total number of records to return.
		 */
		public void setRange(int index, int offset, int length) {
			if (current != null) {
				throw new RuntimeException("Incorrect invokation of the setRange() method. current != null.");
			}
			listIndex = index;
			if (index >= 0) {
				while (offset > 0 && next() != null) {
					offset--;
				}
			}
			countDown = length;
		}
		
		public boolean hasNext() {
			while (listIndex >= 0 && nextRecord == null) {
				nextRecord = getNext(-1L);
			}
			
			return nextRecord != null;
		}
		
		/**
		 * Find next record using reference sequence number.
		 * 
		 * @param refSequenceNumber reference sequence. if negative, stop search after searching one file, otherwise
		 * 		stop search if internal sequence of the next record would exceed the specified 
		 * @return next record in the current file satisfying the original condition.
		 */
		public RepositoryLogRecord findNext(long refSequenceNumber) {
			if (nextRecord == null) {
				nextRecord = getNext(refSequenceNumber);
			}
			
			RepositoryLogRecord result = nextRecord;
			nextRecord = null;
			return result;
		}
		
		private RepositoryLogRecord getNext(long refSequenceNumber) {
			if (current == null) {
				if (listIndex >= 0) {
					current = parent.getIterator(listIndex);
				}
				/*
				 * Ran out of files. It can happen here instead of the 'while' loop bellow if
				 * hasNext() is called after this iterator ran out of records or an iterator
				 * is for a repository with no files.
				 */
				if (current == null) {
					close();
					return null;
				}
				stats = new OneFileRecordStatistics(current.file);
			}
			
			if (countDown == 0) {
				close();
				return null;
			}

			RepositoryLogRecord result;
			
			while ((result = current.findNext(refSequenceNumber)) == null) {
				if (refSequenceNumber >= 0L && !current.isDone()) {
					// search was interrupted due to sequence number.
					return null;
				}
				current.close();
				// We got all measurments in the 'current'. Check if 'base' needs to be init as well.
				parent.getStatistics(listIndex, false).update(stats);
				// Check if there's more iterators in 'base'.
				if ((current = parent.getIterator(++listIndex)) == null) {
					close();
					return null;
				}
				stats = new OneFileRecordStatistics(current.file);
				if (refSequenceNumber < 0) {
					// search was requested for one file.
					return null;
				}
			}

			// Keep measurements on the current iterator
			stats.count(result);

			if (countDown > 0) {
				countDown--;
			}
			
			return result;
		}

		public RepositoryLogRecord next() {
			if (listIndex < 0 || (nextRecord == null && !hasNext())) {
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
		 * returns time stamp of the file method next() searches in.
		 * 
		 * @return -1 if iterator already ran out of files to search in,
		 * 			0 if iterator didn't read its first file yet, or
		 *         >0 as a real time stamp of the current file.
		 */
		private long getCurrentTimestamp() {
			if (current == null) {
				if (listIndex < 0) {
					return -1L; // No more timestamps
				} else {
					return 0L; // There's still some files with timestamps left
				}
			} else {
				return fileBrowser.getLogFileTimestamp(current.file);
			}
		}
		
		@Override
		public int compareTo(OnePidRecordIterator o) {
			long thisTimestamp = getCurrentTimestamp();
			long otherTimestamp = o.getCurrentTimestamp();
			if (thisTimestamp == otherTimestamp) {
				// Either both have run out of files (value -1) or
				// both haven't read first file yet (value 0) or
				// both have the same real time stamp
				return 0; // either o or this can have smaller time stamp
			} else if (thisTimestamp < 0) {
				// this iterator ran out of files already
				return 1;  // o is a candidate to have smaller time stamp
			} else if (otherTimestamp < 0) {
				// other iterator ran out of files already
				return -1; // this is a candidate to have smaller time stamp
			} else {
				// Either iterators compare real time stamps or one of them has
				// value 0 and is a primary candidate to have smaller time stamp
				return thisTimestamp < otherTimestamp ? -1 : 1;
			}
		}
		
	}
	
	private final class OneFileRecordStatistics {
		final File file;
		boolean allCounted = false;
		int size = 0;
		long firstId = -1;
		long lastId = -1;
		
		OneFileRecordStatistics(File file) {
			this.file = file;
		}
		
		OneFileRecordStatistics(byte[] value) throws IllegalArgumentException {
			RemoteRepositoryCache.RemoteOneFileCache result = new RemoteRepositoryCache.RemoteOneFileCache(value);
			long timestamp = result.getTimestamp();
			file = fileBrowser.findByMillis(timestamp);
			if (file == null) {
				throw new IllegalArgumentException("File with indicated timestamp is most probably purged already");
			}
			if (timestamp != fileBrowser.getLogFileTimestamp(file)) {
				throw new IllegalArgumentException("Search on timestamp result in different file than expected. Most probably bytes are for different repository");
			}
			size = result.getSize();
			firstId = result.getFirstId();
			lastId = result.getLastId();
			allCounted = true;
		}
		
		byte[] toBytes() throws IllegalStateException {
			if (!allCounted) {
				throw new IllegalStateException("This method can be called only when all data is collected for the file");
			}
			RemoteRepositoryCache.RemoteOneFileCache result = new RemoteRepositoryCache.RemoteOneFileCache(fileBrowser.getLogFileTimestamp(file), size, firstId, lastId);
			return result.toByteArray();
		}
		
		void update(OneFileRecordStatistics other) {
			if (!allCounted) {
				size = other.size;
				firstId = other.firstId;
				lastId = other.lastId;
				allCounted = true;
			}
		}
		
		void count(RepositoryLogRecord record) {
			size++;
			lastId = ((RepositoryLogRecordImpl)record).getInternalSeqNumber();
			if (firstId < 0) {
				firstId = lastId;
			}
		}
	}
	
	/**
	 * Base class for an internal implementation of pid's record list
	 */
	public abstract class OnePidRecordListImpl {
		/** maximum millis value for log records */
		protected final long max;
		/** filter to eliminate unwanted records */
		protected final IInternalRecordFilter recFilter;
		/** header for all results of this list */
		protected Properties header = null;
		
		private final ArrayList<OneFileRecordStatistics> startList = new ArrayList<OneFileRecordStatistics>();
		private final ArrayList<OneFileRecordStatistics> endList = new ArrayList<OneFileRecordStatistics>();
		private int adjustment = 0; // Index adjustment if cache was set externally using setCache() method.
		private int total = -1; // The total number of files in the query.
		
		OnePidRecordListImpl(long max, IInternalRecordFilter recFilter) {
			this.max = max;
			this.recFilter = recFilter;
		}
		
		/**
		 * returns this result cache usable for remote transport
		 * 
		 * @return statistics collected so far
		 */
		public RemoteRepositoryCache getCache() {
			// Initialize total value.
			total();
			
			// Ensure first and last files are counted.
			if (!startList.isEmpty()) {
				countNow(startList.get(0), adjustment==0);
			}
			if (!endList.isEmpty() && total > 1) {
				countNow(endList.get(0), false);
			}

			int count = 0;
			for (OneFileRecordStatistics stat: startList) {
				if (!stat.allCounted) {
					break;
				}
				count++;
			}
			byte[][] start = new byte[count][];
			int i=0;
			for (OneFileRecordStatistics stat: startList) {
				if (!stat.allCounted) {
					break;
				}
				start[i++] = stat.toBytes();
			}
			
			count = 0;
			for (OneFileRecordStatistics stat: endList) {
				if (!stat.allCounted) {
					break;
				}
				count++;
			}
			
			byte[][] end = new byte[count][];
			i=0;
			for (OneFileRecordStatistics stat: endList) {
				if (!stat.allCounted) {
					break;
				}
				end[i++] = stat.toBytes();
			}
			
			return new RemoteRepositoryCache(total, start, end);
		}
		
		/**
		 * sets cache for this result based on the provided one
		 * 
		 * @param cache statistics sent in a remote call
		 */
		public void setCache(RemoteRepositoryCache cache) {
			total = cache.getTotal();
			
			startList.clear();
			for(byte[] fileCache: cache.getStart()) {
				try {
					startList.add(new OneFileRecordStatistics(fileCache));
				} catch (IllegalArgumentException ex) {
					total--;
				}
			}
			
			endList.clear();
			for(byte[] fileCache: cache.getEnd()) {
				try {
					endList.add(new OneFileRecordStatistics(fileCache));
				} catch (IllegalArgumentException ex) {
					total--;
				}
			}
			
			if (startList.isEmpty() || endList.isEmpty()) {
				// reinitialize cache if required boundary cache values are missing.
				if (total > 1 || (total > 0 && startList.isEmpty() && endList.isEmpty())) {
					total = -1;
					total();
				}
			}
			
			// Recalculate cache adjustment.
			adjustment = -1;
			if (total > 0) {
				OneFileRecordIterator it = getFirstIterator();
				if (it != null) {
					int i=0;
					long timestamp = fileBrowser.getLogFileTimestamp(it.file);
					for (OneFileRecordStatistics stat: startList) {
						if (timestamp == fileBrowser.getLogFileTimestamp(stat.file)) {
							adjustment = i;
							break;
						}
						i++;
					}
					if (adjustment < 0) {
						i = total-1;
						for (OneFileRecordStatistics stat: endList) {
							if (timestamp == fileBrowser.getLogFileTimestamp(stat.file)) {
								adjustment = i;
								break;
							}
							i--;
						}
					}
					if (adjustment < 0) {
						// Start file is not in cache yet. Need to see where it fits.
						// The only case we can handle is when it should be next in startList.
						if (timestamp < fileBrowser.getLogFileTimestamp(endList.get(endList.size()-1).file) &&
								timestamp > fileBrowser.getLogFileTimestamp(startList.get(startList.size()-1).file) &&
								total > startList.size() + endList.size()) {
							// It should be the next file in startList.
							adjustment = startList.size();
						} else {
							// A new file either outside of our cache range or somewhere inside of the cache.
							// In both cases we can't use provided cache and need to initialize a new one.
							total = -1;
							total();
						}
						
					}
				}
			}
			if (adjustment < 0) {
				adjustment = 0;
			}
			
		}
		
		public Properties getHeader() {
			if (header == null) {
				IInternalRecordFilter filter = new AllAcceptVerifier();
				for(File backup = fileBrowser.findNext((File)null, -1);
					backup != null && header == null;
					backup = fileBrowser.findNext(backup, -1)) {
					OneFileRecordIterator it = new OneFileRecordIterator(backup, -1, filter);
					header = it.header;
					it.close();
				}
			}
			return header;
		}
		
		/**
		 * Gets header containing just the information based on the repository location.
		 * It should be used only when real header information is not available.
		 * 
		 * @return Properties containing the parsed information.
		 */
		public Properties getParsedHeader() {
				Properties result = new Properties();
				result.put(ServerInstanceLogRecordList.HEADER_PROCESSID, fileBrowser.getProcessId());
				String label = fileBrowser.getLabel();
				int index = label==null ? -1 : label.indexOf(LogRepositoryBaseImpl.TIMESEPARATOR);
				if (index > 0 && index < label.length()-1) {
					result.put(ServerInstanceLogRecordList.HEADER_ISZOS, "Y");
					result.put(ServerInstanceLogRecordList.HEADER_JOBNAME, label.substring(0, index));
					result.put(ServerInstanceLogRecordList.HEADER_JOBID, label.substring(index+1));
				}
				return result;
		}
		
		/**
		 * Creates new iterator to go over log records generated by a process
		 * 
		 * @param offset number of records to skip from the start of the result.
		 * @param length maximum number of records to attempt to return.
		 * @return Iterator over <code>RepositoryLogRecord</code> objects
		 */
		public Iterator<RepositoryLogRecord> getNewIterator(int offset, int length) {
			OnePidRecordIterator result = new OnePidRecordIterator(this);
			setRange(result, offset, length);
			return result;
		}
		
		/**
		 * Returns statistics instance for a file containing process's log records.
		 * 
		 * @param index position in the list of files generated by the process
		 * @param complete indicator that statistics should be completed for all records before
		 *        instance is return to the caller. It should be <code>true</code> to have correct value
		 *        for 'size', 'firstId', and 'lastId' but could be <code>false</code> for 'file'.
		 * @return statistics instance for the requested file
		 */
		OneFileRecordStatistics getStatistics(int index, boolean complete) {
			// Trigger cache initialization
			total();
			
			index += adjustment;
			
			if (index < 0 || (index >= total)) {
				return null;
			}

			// --- Small comment concerning missing file handled bellow. ---
			// It's OK to update total here since it's used only to know how many files we still don't know
			// about (they are in neither startList nor endList). We don't update startList or endList even
			// when files are missing since that will screw up indexing of records we already return in one
			// one of the iterators. In short, in missing file situation we consider that we have full
			// repository statistics now. Caller of this method should use statistics it has or assume that
			// there were never any records in the missing file.
			OneFileRecordStatistics result;
			// Decide if startList or endList will result in going through less files.
			if (index < (total + startList.size() - endList.size())/2) {
				// startList is closer to the requested index
				// use 'end' to make sure we don't run over into endList.
				OneFileRecordStatistics end = endList.isEmpty() ? null : endList.get(endList.size()-1);
				long endTimestamp = end==null ? -1 : fileBrowser.getLogFileTimestamp(end.file);
				while (index >= startList.size()) {
					File file = fileBrowser.findNext(startList.get(startList.size()-1).file, max);
					if (file == null || (endTimestamp > 0 && endTimestamp <= fileBrowser.getLogFileTimestamp(file))) {
						// File is missing update total and return result from the endList.
						total = startList.size() + endList.size();
						return end;
					} else {
						startList.add(new OneFileRecordStatistics(file));
					}
				}
				result = startList.get(index);
			} else {
				// endList is closer to the requested index
				// use 'start' to make sure we don't run over into startList.
				OneFileRecordStatistics start = startList.isEmpty() ? null : startList.get(startList.size()-1);
				long endTimestamp = start==null ? -1 : fileBrowser.getLogFileTimestamp(start.file);
				while (total - index - 1 >= endList.size()) {
					File file = fileBrowser.findPrev(endList.get(endList.size()-1).file, endTimestamp);
					if (file == null || endTimestamp == fileBrowser.getLogFileTimestamp(file)) {
						// File is missing, update total and fall through to possibly complete
						// the result added in this loop.
						total = startList.size() + endList.size();
						 // Adjust index to return the earliest entry in the endList
						index = startList.size();
					} else {
						endList.add(new OneFileRecordStatistics(file));
					}
				}
				result = endList.get(total - index - 1);
			}
			
			if (complete) {
				countNow(result, index==0);
			}
			return result;
		}
		
		/**
		 * Count whole statistics in result if it wasn't done so yet.
		 * @param result file statistics we need to complete
		 * @param isFirst true if the file is first in the query
		 */
		private void countNow(OneFileRecordStatistics result, boolean isFirst) {
			if (!result.allCounted) {
				OneFileRecordIterator it = isFirst ? getFirstIterator() : new OneFileRecordIterator(result.file, max, recFilter);
				RepositoryLogRecord record;
				while((record = it.next()) != null) {
					result.count(record);
				}
				it.close();
				result.allCounted = true;
			}
		}
		
		public OneFileRecordIterator getIterator(int index) {
			OneFileRecordStatistics stats = getStatistics(index, false);
			if (stats == null) {
				return null;
			}
			OneFileRecordIterator result = index==0 ? getFirstIterator() : new OneFileRecordIterator(stats.file, max, recFilter);
			if (result.header == null) {
				result.header = getHeader();
			}

			return result;
		}
		
		/**
		 * Returns first file iterator.
		 * 
		 * @return iterator over the records in the first file covered by this query.
		 */
		protected abstract OneFileRecordIterator getFirstIterator();
		
		private int total() {
			if (total < 0) {
				startList.clear();
				endList.clear();
				OneFileRecordIterator it = getFirstIterator();
				if (it == null) {
					total = 0;
				} else {
					startList.add(new OneFileRecordStatistics(it.file));
					File last = max<0 ? fileBrowser.findPrev(null, -1) : fileBrowser.findByMillis(max);
					if (fileBrowser.getLogFileTimestamp(last) == fileBrowser.getLogFileTimestamp(it.file)) {
						total = 1;
					} else {
						endList.add(new OneFileRecordStatistics(last));
						total = fileBrowser.count(it.file, last);
					}
					if (header == null) {
						header = it.header;
					}
					it.close();
				}
			}
			return total > 0 ? total - adjustment : total;
		}
		
		/**
		 * Set range on the iterator using statistics collected for each file.
		 * Range can be set only once and only before retrieving any record from the iterator.
		 * 
		 * @param it Iterator to set range on.
		 * @param offset number of records to skip. Negative value means offset will be taken from the end of the result. Otherwise, from the start of the result.
		 * @param length maximum number of records to return.
		 */
		void setRange(OnePidRecordIterator it, int offset, int length) {
			// If request came for all results we don't need to use statistics now.
			if (offset == 0 && length < 0) {
				it.setRange(0, 0, -1);
				return;
			}
			OneFileRecordStatistics result;
			int index = offset < 0 ? total()-1 : 0;
			while ((result = getStatistics(index, true)) != null) {
				if (offset < 0) {
					if (result.size >= -offset) {
						it.setRange(index, result.size+offset, length);
						return;
					}
					offset += result.size;
					index--;
				} else {
					if (result.size > offset) {
						it.setRange(index, offset, length);
						return;
					}
					offset -= result.size;
					index++;
				}
			}
			if (offset < 0) {
				length = length>-offset ? length+offset : 0;
				it.setRange(0, 0, length);
			} else {
				// Offset is too big return no records.
				it.setRange(-1, -1, -1);
			}
		}
		
		/**
		 * Returns the timestamp of the first record in this Server Instance
		 * @return timestamp of the first log record.
		 */
		public abstract long getTimestamp();
		
	}
	
	private class OnePidRecordListMintimeImpl extends OnePidRecordListImpl {
		private final File file;
		private final long min;
		OnePidRecordListMintimeImpl(File file, long min, long max, IInternalRecordFilter recFilter) {
			super(max, recFilter);
			this.file = file;
			this.min = min;
		}
		
		protected OneFileRecordIterator getFirstIterator() {
			if (file != null) {
				return new OneFileRecordIterator(file, max, recFilter) {
					protected boolean verifyMin(RepositoryLogRecordImpl nextRecord) {
						return min < 0 || min <= nextRecord.getMillis();
					}
				};
			}
				
			return null;
			
		}
		
		public long getTimestamp() {
			return fileBrowser.getLogFileTimestamp(file);
		}
				
	}
	
	private class OnePidRecordListLocationImpl extends OnePidRecordListImpl {
		private final RepositoryPointerImpl location;
		OnePidRecordListLocationImpl(RepositoryPointerImpl location, long max, IInternalRecordFilter recFilter) {
			super(max, recFilter);
			this.location = location;
		}
		
		protected OneFileRecordIterator getFirstIterator() {
			long pos = location.getRecordOffset();
			File file = fileBrowser.findFile(location);
			if (file == null) {
				file = fileBrowser.findNext(location, max);
				pos = -1L;
			}
			if (file != null) {
				OneFileRecordIterator current = new OneFileRecordIterator(file, max, recFilter);
				if (pos > 0) {
					RepositoryLogRecord rec = null;
					if (current.setPosition(pos)) {
						rec = current.next();
					}
					if (rec == null || !location.equals(rec.getRepositoryPointer())) {
						// Just warn but let it be j
						logger.logp(Level.WARNING, className, "getFirstIterator", "HPEL_NoRecordAtLocation");
						current.close();
						return null;
					}
				}
				return current;
			}
			return null;
		}
		
		public long getTimestamp() {
			return fileBrowser.getLogFileTimestamp(new File(location.getFileId()));
		}
	
	}
	
	private class OnePidRecordListRecordImpl extends OnePidRecordListImpl {
		private final File file;
		private final RepositoryLogRecordImpl record;
		OnePidRecordListRecordImpl(File file, RepositoryLogRecordImpl record, long max, IInternalRecordFilter recFilter) {
			super(max, recFilter);
			this.file = file;
			this.record = record;
		}
		
		protected OneFileRecordIterator getFirstIterator() {
			if (file != null) {
				return new OneFileRecordIterator(file, max, recFilter) {
					protected boolean verifyMin(RepositoryLogRecordImpl nextRecord) {
						return record.getInternalSeqNumber() < nextRecord.getInternalSeqNumber();
					}
				};
			}
			return null;
		}
		
		public long getTimestamp() {
			return fileBrowser.getLogFileTimestamp(file);
		}
	
	}
	
}

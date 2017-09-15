/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import com.ibm.websphere.logging.hpel.reader.AbstractRemoteRepositoryReader;
import com.ibm.websphere.logging.hpel.reader.LogRepositoryException;
import com.ibm.websphere.logging.hpel.reader.LogRepositoryRuntimeException;
import com.ibm.websphere.logging.hpel.reader.RemoteInstanceDetails;
import com.ibm.websphere.logging.hpel.reader.RemoteInstanceResult;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;

/**
 * Implementation of the iterator for remote set of log records.
 * 
 * This implementation pulls data as needed from the remote side using specified implementation
 * of the {@link AbstractRemoteRepositoryReader}.
 */
public class RemoteLogRecordIterator implements Iterator<RepositoryLogRecord> {
	private final AbstractRemoteRepositoryReader reader;
	private final RemoteInstanceDetails indicator;
	private final Locale locale;
	
	// List of already downloaded log records.
	private final ArrayList<RepositoryLogRecord>logRecordList = new ArrayList<RepositoryLogRecord>();
	// Index in the logRecordList of the record to return in 'next()' call.
	private int curPos = 0;
	// Number of records this iterator still need to return
	private int leftToRead;
	// Reference point for the next pull of log records
	private RepositoryPointer pointer;
	// Number of records from the reference point which needs to be skipped on the next pull.
	private int offset = 0;

	/**
	 * constructs and initializes remote iterator.
	 * 
	 * @param reader handler to pull more data when necessary
	 * @param indicator identifier of the server instance we need log records from
	 * @param pointer the minimum condition of the query
	 * @param offset the number of records to skip from the start of the query result
	 * @param length the number of records to return from this iterator
	 * @param locale Locale to which translate record messages before returning them
	 */
	public RemoteLogRecordIterator(AbstractRemoteRepositoryReader reader, RemoteInstanceDetails indicator, RepositoryPointer pointer, int offset, int length, Locale locale) {
		this.reader = reader;
		this.indicator = indicator;
		this.pointer = pointer;
		this.offset = offset;
		this.leftToRead = length;
		this.locale = locale;
	}
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		if (curPos >= logRecordList.size()) {
			loadNext();
		}
		return curPos < logRecordList.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	public RepositoryLogRecord next() {
		RepositoryLogRecord result = null;
		if (curPos >= logRecordList.size()) {
			loadNext();
		}
		if (curPos < logRecordList.size()) {
			result = logRecordList.get(curPos++) ;
		}
		pointer = result==null ? null : result.getRepositoryPointer();
		return result;
	}
	
	private void loadNext() {
		boolean hadRecords = !logRecordList.isEmpty();
		logRecordList.clear();
		curPos = 0;
		if (leftToRead != 0)	{	// If there are more to get
			RemoteInstanceResult remoteResult;
			try {
				boolean firstRead = true;
				// Read until we get at least one record
				while (true) {
					// Read only maxRecords entries if leftToRead is too big.
					int read = this.reader.getMaxRecords() > 0 && (leftToRead < 0 || this.reader.getMaxRecords() < leftToRead) ? this.reader.getMaxRecords() : leftToRead;
					remoteResult = this.reader.readLogRecords(indicator, pointer, offset, read, locale);

					// If we got at least one record or we attempted to read all requested ones we don't need another loop.
					if (remoteResult.getLogList().size() > 0 || leftToRead == read || this.reader.getMaxRecords() <= 0) {
						break;
					}

					if (offset < 0) {
						// If we would go beyond last record we don't need another loop
						if (offset + read >= 0) {
							break;
						}
					} else if (leftToRead < 0) {
						// If we had records before we really ran out of records.
						if (hadRecords) {
							break;
						}
						
						// If there's no records at all we don't need another loop but we need to do this check
						// for the last record on every loop in case instance is purged in between our calls.
						RemoteInstanceResult temp = this.reader.readLogRecords(indicator, null, -1, 1, null);
						if (temp.getLogList().size() == 0) {
							break;
						}
						
						// There's two cases left - records could be before or after the region we attempted to read.
						// We don't need another loop if they are before but we can safely keep looking for them if
						// they are after the region. We need to check for the former case only once since we shift
						// our region forward only.
						if (firstRead) {
							int tmpOffset;
							for (tmpOffset=offset; tmpOffset>0; tmpOffset -= this.reader.getMaxRecords()) {
								int tmpRead = tmpOffset < this.reader.getMaxRecords() ? tmpOffset : this.reader.getMaxRecords();
								temp = this.reader.readLogRecords(indicator, null, tmpOffset-tmpRead, tmpRead, null);
								if (temp.getLogList().size() > 0) {
									break;
								}
							}
							// We found a record before our region. We don't need another loop.
							if (tmpOffset <= 0) {
								break;
							}
						}
					}

					if (leftToRead >= read) {
						leftToRead -= read;
					} else if (leftToRead >= 0) {
						break;
					}
					// Adjust offset to avoid reading the same records on the next loop.
					offset += read;
					firstRead = false;
				}
				indicator.setCache(remoteResult.getCache());
			} catch (LogRepositoryException e) {
				throw new LogRepositoryRuntimeException(e);
			}
			ArrayList<RepositoryLogRecord> newList = remoteResult.getLogList();
			if (newList != null && newList.size() > 0) {
				leftToRead -= newList.size();
				logRecordList.addAll(newList);
			} else {
				leftToRead = 0; // No more records on server.
			}
			// Check if negative offset went beyond start of the repository
			if (offset < 0 && indicator.getCache().isComplete()) {
				offset += indicator.getCache().getSize();
				if (offset < 0) {
					leftToRead += offset;
					// Check if negative offset results in 0 records to read.
					if (leftToRead < 0) {
						leftToRead = 0;
					}
				}
			}
			// We can set offset to 0 now since we'll use pointer for the next refill
			offset = 0;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException("Method is not applicable to this class");
	}

}

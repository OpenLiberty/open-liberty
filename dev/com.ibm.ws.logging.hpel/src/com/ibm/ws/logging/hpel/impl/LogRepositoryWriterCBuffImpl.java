/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.util.LinkedList;

import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;

/**
 * 
 */
public class LogRepositoryWriterCBuffImpl implements LogRepositoryWriter {
	// The size values are always changed with the instance lock taken.
	private long maxSize = 0L; // Total size in bytes for the buffer.
	private long currentSize = 0L; // Current size of the buffer.
	
	// Content of the stack is always changed with the instance lock taken.
	private final LinkedList<CBuffRecord> buffer = new LinkedList<CBuffRecord>();
	
	// Header is set only once and with the instance lock taken.
	private byte[] headerBytes = null;
	
	private final LogRepositoryWriter dumpWriter;
	
	/**
	 * Creates new instance of the in-memory trace buffer
	 * 
	 * @param writer LogRepositoryWriter implementation to use when dumping buffer on disk.
	 */
	public LogRepositoryWriterCBuffImpl(LogRepositoryWriter writer) {
		this.dumpWriter = writer;
	}
	
	/**
	 * Retrieves writer configured to be used when dumping buffer on disk.
	 * 
	 * @return current dump writer.
	 */
	public LogRepositoryWriter getWriter() {
		return dumpWriter;
	}
	
	public LogRepositoryManager getLogRepositoryManager() {
		return dumpWriter.getLogRepositoryManager();
	}

	/**
	 * Retrieves current size of the in-memory buffer.
	 * 
	 * @return Total size of records in buffer in bytes.
	 */
	public synchronized long getCurrentSize() {
		return currentSize;
	}
	
	/**
	 * Retrieves current limit on the in-memory buffer size.
	 * 
	 * @return The top limit on the total size of records in buffer.
	 * @see #getCurrentSize()
	 */
	public synchronized long getMaxSize() {
		return maxSize;
	}
	
	/**
	 * Sets new limit on the in-memory buffer size.
	 * 
	 * @param maxSize the new value for the top limit on the total size of records in buffer.
	 * @see #getMaxSize()
	 */
	public synchronized void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
		currentSize = adjustItems(currentSize);
	}
	
	/*
	 * Removes old records to satisfy buffer size limit.
	 * Note: This method should be called with the instance lock taken.
	 * 
	 * @param size the current size of the buffer.
	 * @return the new size of the buffer after removal of records to satisfy the limit.
	 */
	private long adjustItems(long size) {
		while (size > maxSize && buffer.size() > 0) {
			CBuffRecord old = buffer.removeFirst();
			size -= old.bytes.length;
		}
		return size;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryWriter#logRecord(long, byte[])
	 */
	public synchronized void logRecord(long timestamp, byte[] bytes) {
		buffer.addLast(new CBuffRecord(timestamp, bytes));
		currentSize = adjustItems(currentSize + bytes.length);
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryWriter#setHeader(byte[])
	 */
	public synchronized void setHeader(byte[] headerBytes) {
		if (this.headerBytes == null) {
			this.headerBytes = headerBytes;
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.logging.hpel.LogRepositoryWriter#stop()
	 */
	public synchronized void stop() {
		dumpWriter.stop();
		buffer.clear();
		currentSize = 0L;
	}
	
	/**
	 * Dumps records stored in buffer to disk using configured LogRepositoryWriter.
	 */
	public void dumpItems() {
		LinkedList<CBuffRecord> copy = new LinkedList<CBuffRecord>();
		synchronized(this) {
			copy.addAll(buffer);
			buffer.clear();
			currentSize = 0L;
			if (headerBytes == null) {
				return;
			}
		}
		
		dumpWriter.setHeader(headerBytes);
		for(CBuffRecord record: copy) {
			dumpWriter.logRecord(record.timestamp, record.bytes);
		}
	}

	private static class CBuffRecord {
		final long timestamp;
		final byte[] bytes;
		CBuffRecord(long timestamp, byte[] bytes) {
			this.timestamp = timestamp;
			this.bytes = bytes;
		}
	}
}

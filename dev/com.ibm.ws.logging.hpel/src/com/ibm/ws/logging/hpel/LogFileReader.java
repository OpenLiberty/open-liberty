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
package com.ibm.ws.logging.hpel;

import java.io.IOException;

/**
 * Reader of the log data from the disk.
 */
public interface LogFileReader {
	/**
	 * Returns size of the next record. It needs to be called before and after
	 * reading log record with a deserializer.
	 * 
	 * @return size of the next record in bytes.
	 * @throws IOException 
	 */ 
	int readLength() throws IOException;
	
	// File position methods
	
	/**
	 * Returns current position in the file.
	 * 
	 * @return current position in the file as a byte offset from the beginning of the file.
	 * @throws IOException 
	 */
	long getFilePointer() throws IOException;
	
	/**
	 * Sets current position in the file.
	 * 
	 * @param pos new position in the file as a byte offset from the beginning of the file.
	 * @throws IOException
	 */
	void seek(long pos) throws IOException;
	
	/**
	 * Returns the size of the underlying file.
	 * 
	 * @return file size in bytes
	 * @throws IOException
	 */
	long length() throws IOException;

	/**
	 * Reads bytes from the underlying file.
	 * 
	 * @param buffer the buffer to read bytes into.
	 * @param off the offset in be buffer of the first copied byte.
	 * @param len the number of bytes to read.
	 * @throws IOException
	 */
	void readFully(byte[] buffer, int off, int len) throws IOException;
	
	/**
	 * Closes input stream open on the underlying file.
	 * 
	 * @throws IOException
	 */
	void close() throws IOException;
	
	/**
	 * Checks if input stream is open on the underlying file.
	 * 
	 * @return <code>true</code> if stream is open, <code>false</code> otherwise.
	 */
	boolean isOpen();
	
}

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

import java.io.File;
import java.io.IOException;

/**
 * Writer of the log data to the disk.
 */
public interface LogFileWriter {
	/**
	 * Writes log record from the <code>buffer</code> into the underlying output stream.
	 * The size of the log record in bytes will be written before the log record bytes.
	 * 
	 * @param buffer array of bytes to write to disk.
	 * @throws IOException
	 */
	void write(byte[] buffer) throws IOException;
	
	/**
	 * Returns total number of bytes this writer would have written if <code>buffer</code>
	 * and <code>tail</code> are the final bytes written with it.
	 * 
	 * @param buffer array of bytes waiting to be written with the {@link #write(byte[])}
	 * method.
	 * @param tail array of bytes waiting to be sent with the final {@link #close(byte[])} call.
	 * @return the number of bytes this writer would have written after writting this buffer.
	 */
	long checkTotal(byte[] buffer, byte[] tail);
	
	/**
	 * Returns the current file associated with this writer.
	 * 
	 * @return File open by the underlying output system for writing.
	 */
	File currentFile();
	
	/**
	 * Flushes underlying output stream. 
	 * 
	 * @throws IOException 
	 */
	void flush() throws IOException;
	
	/**
	 * Flushes data and close the output stream.
	 * 
	 * @param tail Any additional bytes the user of this writer want to append to
	 * 		the file. Implementation should record it using the
	 * 		{@link #write(byte[])} method.
	 * @throws IOException 
	 */
	void close(byte[] tail) throws IOException;
	
}

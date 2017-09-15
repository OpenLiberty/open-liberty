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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ibm.ws.logging.hpel.LogFileWriter;

/**
 * Implementation of the {@link LogFileWriter} interface writing data to disk using {@link FileOutputStream} class.<br>
 * <b>Note:</b> For performance reasons methods of this class are thread unsafe - it expect the caller to take care of
 * only one thread using its methods at a time.
 */
public class LogFileWriterImpl extends AbstractBufferedLogFileWriter {
	private long total = 0;

	/**
	 * Creates the LogFileWriter instance writing to the file.
	 * 
	 * @param file File instance of the file to write to.
	 * @param bufferingEnabled indicator if buffering should be enabled.
	 * @throws IOException
	 */
	public LogFileWriterImpl(File file, boolean bufferingEnabled) throws IOException {
		super(file, bufferingEnabled);
	}

	public void close(byte[] tail) throws IOException {
		if (tail != null) {
			write(tail);
		}
		super.close(tail);
	}

	public void write(byte[] b) throws IOException {
		byte[] buffer = new byte[b.length + 8];
		writeLength(b.length, buffer, 0);
		System.arraycopy(b, 0, buffer, 4, b.length);
		writeLength(b.length, buffer, b.length+4);
		synchronized(fileStream) {
			fileStream.write(buffer);
		}
		total += buffer.length;
	}

	public long checkTotal(byte[] buffer, byte[] tail) {
		return total + buffer.length + tail.length + 16; // 4 bytes for each size
	}

	private void writeLength(int value, byte[] buffer, int offset) throws IOException {
		buffer[offset+3] = (byte) (value >>> 0);
		buffer[offset+2] = (byte) (value >>> 8);
		buffer[offset+1] = (byte) (value >>> 16);
		buffer[offset] = (byte) (value >>> 24);
	}

}

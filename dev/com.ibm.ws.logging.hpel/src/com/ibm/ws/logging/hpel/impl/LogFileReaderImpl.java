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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.ibm.websphere.logging.hpel.reader.GenericFile;
import com.ibm.ws.logging.hpel.LogFileReader;

/**
 * Implementation of the {@link LogFileReader} interface reading data from disk
 * into internal buffer in its constructor and releasing file afterwards.
 */
public class LogFileReaderImpl implements LogFileReader {
	/** Maximum size of the file this reader can accept */
	public final static int MAXSIZE = 64 * 1024 * 1024; // 64MB
	
	private int pointer = 0;
	private byte[] buffer;
	
	/**
	 * Creates the LogFileReader instance reading from the file.
	 * 
	 * @param file File instance to read data from.
	 * @throws IOException
	 */
	public LogFileReaderImpl(File file) throws IOException {
		if (!AccessHelper.isFile(file)) {
			throw new IOException("File \"" + file.getAbsolutePath() + "\" is not an existing file.");
		}
		long length = AccessHelper.getFileLength(file);
		if (length > MAXSIZE) {
			throw new IOException("File \"" + file.getAbsolutePath() + "\" is " + length + " bytes long which is too big for a WBL file");
		}
		// Check if the size of the file is too small to contain even a record size.
		if (length < 4) {
			throw new IOException("File \"" + file.getAbsolutePath() + "\" is " + length + " bytes long which is too small for a WBL file");
		}
		buffer = new byte[(int)length];
		if (length > 0) {
			InputStream fis;
			if (file instanceof GenericFile) {
				fis = ((GenericFile)file).getInputStream();
			} else {
				fis = AccessHelper.createFileInputStream(file);
			}
			int offset=0;
			int read;
			while((read=fis.read(buffer, offset, buffer.length-offset)) > 0) {
				offset += read;
				if (offset >= buffer.length) {
					break;
				}
			}
			fis.close();
			if (offset < buffer.length) {
				throw new IllegalArgumentException("Failed to read all " + buffer.length + " bytes from file \"" + file.getAbsolutePath() + "\". Only " + buffer.length + "bytes were read.");
			}
		}
	}
	
	/**
	 * Creates the LogFileReader instance as a clone of <code>other</code>.
	 * 
	 * @param other Another instance of the LogFileReader to copy file, length, and position from.
	 * @throws IOException
	 */
	public LogFileReaderImpl(LogFileReaderImpl other) throws IOException {
		pointer = other.pointer;
		buffer = other.buffer==null ? null : Arrays.copyOf(other.buffer, other.buffer.length);
	}
	
	private final byte[] size = new byte[4];
	public int readLength() throws IOException {
		readFully(size, 0, 4);
		return (((0xFF & size[0]) << 24) |
				((0xFF & size[1]) << 16) |
				((0xFF & size[2]) << 8) |
				 (0xFF & size[3]));
	}

	public void close() throws IOException {
		buffer = null;
	}
	
	public boolean isOpen() {
		return buffer != null;
	}

	public long getFilePointer() throws IOException {
		return pointer;
	}

	public long length() throws IOException {
		if (buffer==null) {
			throw new EOFException();
		}
		return buffer.length;
	}

	public void seek(long pos) throws IOException {
		if (buffer==null || pos > buffer.length || pos < 0) {
			throw new EOFException();
		}
		pointer = (int)pos;
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		if (b==null) {
			throw new IllegalArgumentException("Argument 'b' can't be 'null");
		}
		if (off < 0 || len < 0) {
			throw new IllegalArgumentException("Neither 'off' ("+ off +") nor 'len' ("+ len +") can have negative value.");
		}
		if (off + len > b.length) {
			throw new IllegalArgumentException("Sum of 'off' ("+ off +") and 'len' ("+ len +") can't be bigger than 'b.length' ("+ b.length +").");
		}
		if (buffer == null || pointer + len > buffer.length) {
			throw new EOFException();
		}
		System.arraycopy(buffer, pointer, b, off, len);
		pointer += len;
	}
}

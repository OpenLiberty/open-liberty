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

import java.io.File;
import java.io.IOException;

/**
 * Implementation of the buffered log writer for writting in text format.
 */
public class LogFileWriterTextImpl extends AbstractBufferedLogFileWriter {
	private long total = 0;
	
	/**
	 * Creates the LogFileWriter instance writing to the text file.
	 * 
	 * @param file File instance of the file to write to.
	 * @param bufferingEnabled indicator if buffering should be enabled.
	 * @throws IOException
	 */
	public LogFileWriterTextImpl(File file, boolean bufferingEnabled) throws IOException {
		super(file, bufferingEnabled);
	}

	public long checkTotal(byte[] buffer, byte[] tail) {
		return total + buffer.length;
	}

	public void write(byte[] buffer) throws IOException {
		synchronized(fileStream) {
			fileStream.write(buffer);
		}
		total += buffer.length;
	}

}

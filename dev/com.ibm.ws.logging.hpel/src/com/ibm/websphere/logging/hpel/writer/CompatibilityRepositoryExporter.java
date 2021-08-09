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
package com.ibm.websphere.logging.hpel.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

import com.ibm.websphere.logging.hpel.reader.HpelFormatter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;

/**
 * Implementation of the {@link RepositoryExporter} interface exporting log records
 * into a text file in Basic or Advanced WebSphere format. The method <code>storeHeader</code> must be called before
 * any records can be stored.  Each record is stored with the <code>storeRecord</code> function.  Failure to
 * follow the order will result in runtime exceptions.
 * 
 * @ibm-api
 */
public class CompatibilityRepositoryExporter implements RepositoryExporter {
	private final PrintStream out;
	private boolean closeStream = false;
	private final HpelFormatter formatter;
	private boolean isClosed = false; // value "true" indicates that exporter was already closed
	private boolean isInitialized = false; // value "true" indicates that storeHeader was issued at least once
	
	/**
	 * Creates an instance for storing records in a file in a Basic or Advanced text format.
	 * 
	 * @param outputFile    output file
	 * @param formatter     formatter to use when converting LogRecords into text
	 * @throws IOException  if an I/O error has occurred
	 */
	public CompatibilityRepositoryExporter(File outputFile, HpelFormatter formatter) throws IOException {
		this(new BufferedOutputStream(new FileOutputStream(outputFile, false)), formatter);
		closeStream = true;
	}

	/**
	 * Creates an instance for writing records into a stream in a Basic or Advanced text format.
	 * 
	 * @param out          output stream.
	 * @param formatter    formatter to use when converting LogRecords into text
	 * @see HpelFormatter
	 */
	protected CompatibilityRepositoryExporter(OutputStream out, HpelFormatter formatter) {
		this(new PrintStream(out), formatter);
	}
	
	/**
	 * Creates an instance for writing records into a stream in a Basic or Advanced text format.
	 * 
	 * @param out          output stream.
	 * @param formatter    formatter to use when converting LogRecords into text
	 * @see HpelFormatter
	 */
	public CompatibilityRepositoryExporter(PrintStream out, HpelFormatter formatter) {
		this.out = out;
		this.formatter = formatter;
	}
	
	/**
	 * flushes and closes the output stream
	 */
	public void close() {
		out.flush();
		if (closeStream) {
			out.close();
		}
		isClosed = true;
	}

	/**
	 * Stores the header properties into the output file
	 * @param header  Properties (key/value) storing header information
	 */
	public void storeHeader(Properties header) {
		storeHeader(header, null);
	}
	
	public void storeHeader(Properties header, String subProcess) {		
		if (isClosed) {
			throw new IllegalStateException("This instance of the exporter is already closed");
		}
		if (subProcess != null) {
			out.print("----------  ");
			out.print(subProcess);
			out.print("  ----------");
			out.print(formatter.getLineSeparator());
		}
		formatter.setHeaderProps(header);
		for (String headerLine: formatter.getHeader()) {
			out.print(headerLine);
			out.print(formatter.getLineSeparator()) ;
		}
		isInitialized = true;
	}

	/**
	 * Stores a RepositoryLogRecord into the proper text format
	 * @param record  RepositoryLogRecord which formatter will convert to Basic or Advanced output format
	 */
	public void storeRecord(RepositoryLogRecord record) {
		if (isClosed) {
			throw new IllegalStateException("This instance of the exporter is already closed");
		}
		if (!isInitialized) {
			throw new IllegalStateException("This instance of the exporter does not have header information yet");
		}
		String formatRecord = formatter.formatRecord(record);
		out.print(formatRecord);
		out.print(formatter.getLineSeparator());
	}

}

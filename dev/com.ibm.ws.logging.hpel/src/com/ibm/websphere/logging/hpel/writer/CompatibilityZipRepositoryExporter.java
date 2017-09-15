/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.logging.hpel.reader.HpelFormatter;

/**
 * Implementation of the {@link RepositoryExporter} interface exporting log records
 * into a compressed text file.
 * 
 * @ibm-api
 */
public class CompatibilityZipRepositoryExporter extends CompatibilityRepositoryExporter {
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = CompatibilityZipRepositoryExporter.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
	
	private final ZipOutputStream out;

	/**
	 * Creates an instance for storing records in a zipped file in a compatibility text format.
	 * 
	 * @param archiveFile   output file
	 * @param formatter     formatter to use when converting record messages into text
	 * @throws IOException  if an I/O error has occurred
	 */
	public CompatibilityZipRepositoryExporter(File archiveFile, HpelFormatter formatter)
			throws IOException {
		this(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archiveFile, false))), archiveFile.getName(), formatter);
	}
	
	private CompatibilityZipRepositoryExporter(ZipOutputStream out, String name, HpelFormatter formatter) throws IOException {
		super(out, formatter);
		this.out = out;
		if (name.endsWith(".zip")) {
			name = name.substring(0, name.length()-4);
		}
		// Enforce TXT extension to help applications depending on file name extension
		this.out.putNextEntry(new ZipEntry(name + ".txt"));
	}

	@Override
	public void close() {
		super.close();
		try {
			out.closeEntry();
			out.close();
		} catch (IOException ex) {
			logger.logp(Level.WARNING, className, "finish", "HPEL_ErrorClosingZipStream", ex);
		}
	}

}

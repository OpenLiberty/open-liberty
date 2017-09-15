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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.logging.hpel.LogFileWriter;
import com.ibm.ws.logging.hpel.LogRepositoryManager;
import com.ibm.ws.logging.hpel.LogRepositoryWriter;
import com.ibm.ws.logging.hpel.impl.AbstractHPELRepositoryExporter;
import com.ibm.ws.logging.hpel.impl.LogRepositoryBaseImpl;
import com.ibm.ws.logging.hpel.impl.LogRepositoryWriterImpl;

/**
 * Implementation of the {@link RepositoryExporter} interface exporting log records in
 * a compressed file containing HPEL formatted files.
 * 
 * @ibm-api
 */
public class HPELZipRepositoryExporter extends AbstractHPELRepositoryExporter {
	private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
	private final static String className = HPELZipRepositoryExporter.class.getName();
	private final static Logger logger = Logger.getLogger(className, BUNDLE_NAME);
	
	private final ZipOutputStream outputStream;
	private final HashSet<String> dirs = new HashSet<String>();
	private LogRepositoryZipManager parentManager = null;

	/**
	 * Constructs exporter storing log records in HPEL format.
	 * 
	 * @param archiveFile  output zipped file where repository log files will be created.
	 * @throws IOException if an I/O error has occurred
	 */
	public HPELZipRepositoryExporter(File archiveFile) throws IOException {
		OutputStream output = new FileOutputStream(archiveFile, false);
		outputStream = new ZipOutputStream(new BufferedOutputStream(output));
	}

	@Override
	public void close() {
		super.close();
		try {
			outputStream.close();
		} catch (IOException ex) {
			logger.logp(Level.WARNING, className, "finish", "HPEL_ErrorClosingZipStream", ex);
		}
	}

	@Override
	protected LogRepositoryWriter createWriter(final String pid, final String label) {
		parentManager = new LogRepositoryZipManager() {
			@Override
			protected String constructSubDirectory(long timestamp) {
				return getLogDirectoryName(timestamp, pid, label);
			}
		};
		return new LogRepositoryZipWriter(parentManager);
	}

	@Override
	protected LogRepositoryWriter createSubWriter(final String pid, final String label,
			String superPid) {
		return new LogRepositoryZipWriter(new LogRepositoryZipManager() {
			@Override
			protected String constructSubDirectory(long timestamp) {
				if (parentManager.ivSubDirectory == null) {
					parentManager.ivSubDirectory = parentManager.constructSubDirectory(timestamp);
				}
				return parentManager.ivSubDirectory + "/" + getLogDirectoryName(-1, pid, label);
			}
		});
	};
	
	private class LogRepositoryZipWriter extends LogRepositoryWriterImpl {
		
		public LogRepositoryZipWriter(LogRepositoryZipManager zipManager) {
			super(zipManager);
			// Set action to 'stop logging' to avoid shutting down the whole JVM.
			setOutOfSpaceAction(2);
		}
		
		@Override
		protected LogFileWriter createNewWriter(File file) throws IOException {
			// Add all parent directories first.
			if (!dirs.contains(file.getParent())) {
				LinkedList<String> missingDirs = new LinkedList<String>();
				
				for (File dir = file.getParentFile(); dir != null && !dirs.contains(dir.getPath()); dir = dir.getParentFile()) {
					missingDirs.addFirst(dir.getPath());
				}
				for (String newDir: missingDirs) {
					// a directory zipentry needs to be created
					//The ZipEntry api only recognizes an entry as a directory if its name ends with "/"
					ZipEntry subDirEntry = new ZipEntry(newDir + "/");
					try {
						outputStream.putNextEntry(subDirEntry);
						dirs.add(newDir);
					} catch (IOException ex) {
						// Report as warning but continue.
						logger.logp(Level.WARNING, className, "createNewWriter", "Exception while adding ZipEntry for directory " + newDir + ": " + ex.getMessage(), ex);
					}
				}
			}
						
			return new LogFileZipWriter(file);
		}
		
	}
	
	// Simple manager which just returns names of the repository files.
	private abstract class LogRepositoryZipManager extends LogRepositoryBaseImpl implements LogRepositoryManager {
		private final static long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
		private String ivSubDirectory = null;

		private LogRepositoryZipManager() {
			super(new File(""));
		}
		
		public File checkForNewFile(long total, long timestamp) {
			// Check if file rotation is necessary.
			if (total <= MAX_LOG_FILE_SIZE) {
				return null;
			}
			
			return startNewFile(timestamp);
		}
		
		public boolean purgeOldFiles() {
			// No purging old files from a zip.
			return false;
		}

		public File startNewFile(long timestamp) {
			if (ivSubDirectory == null) {
				ivSubDirectory = constructSubDirectory(timestamp);
			}
			return getLogFile(new File(ivSubDirectory), timestamp);
		}

		public void stop() {
			// Nothing to do on stop.
		}
		
		protected abstract String constructSubDirectory(long timestamp);
		
	};
	
	private class LogFileZipWriter implements LogFileWriter {
		private final File file;
		private long total = 0;

		/**
		 * Creates the LogFileWriter instance writing to the file.
		 * 
		 * @param file
		 *            File instance of the file to write to.
		 * @throws IOException
		 */
		public LogFileZipWriter(File file) throws IOException {
			this.file = file;
			
			//now create the log/trace data zip entry that log records will be written to
			ZipEntry entry = new ZipEntry(file.getPath());
			outputStream.putNextEntry(entry);
		}

		public void close(byte[] tail) throws IOException {
			if (tail != null) {
				write(tail);
			}
			outputStream.closeEntry();
		}

		public void write(byte[] b) throws IOException {
			writeLength(b.length);
			outputStream.write(b);
			writeLength(b.length);
			total += b.length + 8;
		}

		public long checkTotal(byte[] buffer, byte[] tail) {
			return total + buffer.length + tail.length + 16; // 4 bytes for each size
		}

		// Auxilary buffer to use for writting data size.
		private final byte[] size = new byte[4];

		private void writeLength(int value) throws IOException {
			size[3] = (byte) (value >>> 0);
			size[2] = (byte) (value >>> 8);
			size[1] = (byte) (value >>> 16);
			size[0] = (byte) (value >>> 24);
			outputStream.write(size);
		}

		public File currentFile() {
			return file;
		}

		public void flush() throws IOException {
			// We use buffered output. Don not flush.
		}

	};
	
}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.logging.hpel.reader.GenericFile;

/**
 * Extension to {@link File} implementing {@link GenericFile} for reading ZIP file.
 */
public class ZipGenericFile extends File implements GenericFile {
	private static final long serialVersionUID = -8616693308908035499L;
	
	private final ZipFile zf;
	private final String name;
	
	/**
	 * Create instance of this class treating input file as a directory.
	 * 
	 * @param file ZIP file
	 * @throws IOException if problem happens reading <code>file</code> as a ZIP file.
	 */
	public ZipGenericFile(File file) throws IOException {
		super(file.getAbsolutePath());
		if (file instanceof ZipGenericFile) {
			ZipGenericFile zFile = (ZipGenericFile)file;
			zf = zFile.zf;
			name = zFile.name;
		} else {
			zf = new ZipFile(this);
			name = null;
		}
	}
	
	private ZipGenericFile(ZipGenericFile parent, String child) {
		super(parent, child);
		zf = parent.zf;
		String tmpName = child;
		if (parent.name != null) {
			tmpName = parent.name + tmpName;
		}
		// Add slash if child is a directory without one.
		if (!tmpName.endsWith("/") && zf != null) {
			if (zf.getEntry(tmpName + "/") != null) {
				tmpName += "/";
			}
		}
		name = tmpName;
	}
	
	@Override
	public File getChild(String name) {
		return new ZipGenericFile(this, name);
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		if (zf == null) {
			throw new IOException("File does not exists.");
		}
		if (name == null) {
			throw new IOException("Root entry is not a file.");
		}
		ZipEntry entry = zf.getEntry(name);
		if (entry.isDirectory()) {
			throw new IOException("This entry is a directory.");
		}
		return zf.getInputStream(entry);
	}

	@Override
	public boolean canExecute() {
		return false;
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean createNewFile() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteOnExit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ZipGenericFile && super.equals(obj);
	}

	@Override
	public boolean exists() {
		return zf != null && (name==null || zf.getEntry(name) != null);
	}

	@Override
	public File getAbsoluteFile() {
		return this;
	}

	@Override
	public File getCanonicalFile() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCanonicalPath() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getFreeSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getTotalSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getUsableSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		if (zf == null) {
			return false;
		}
		if (name == null) {
			return true;
		}
		ZipEntry ze = zf.getEntry(name);
		return ze != null && ze.isDirectory();
	}

	@Override
	public boolean isFile() {
		if (zf == null) {
			return false;
		}
		if (name == null) {
			return false;
		}
		ZipEntry ze = zf.getEntry(name);
		return ze != null && !ze.isDirectory();
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public long lastModified() {
		if (zf == null) {
			return 0L;
		}
		if (name == null) {
			return super.lastModified();
		}
		ZipEntry ze = zf.getEntry(name);
		return ze != null ? ze.getTime() : 0L;
	}

	@Override
	public long length() {
		if (zf == null) {
			return 0L;
		}
		if (name == null) {
			return super.length();
		}
		ZipEntry ze = zf.getEntry(name);
		return ze != null ? ze.getSize() : 0L;
	}

	@Override
	public String[] list() {
		return list(null);
	}

	@Override
	public String[] list(FilenameFilter filter) {
		if (!isDirectory()) {
			return null;
		}
		Enumeration<? extends ZipEntry> entries = zf.entries();
		HashSet<String> names = new HashSet<String>();
		while (entries.hasMoreElements()) {
			String entry = entries.nextElement().getName();
			if (name == null || entry.startsWith(name)) {
				if (name != null) {
					entry = entry.substring(name.length());
				}
				int index = entry.indexOf('/');
				if (index>=0) {
					entry = entry.substring(0, index+1);
				}
				if (!entry.isEmpty() && (filter == null || filter.accept(this, entry))) {
					names.add(entry);
				}
			}
		}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public File[] listFiles() {
		return listFiles((FilenameFilter)null);
	}

	@Override
	public File[] listFiles(FileFilter filter) {
		String[] names = list();
		if (names == null) {
			return null;
		}
		ArrayList<ZipGenericFile> result = new ArrayList<ZipGenericFile>();
		for (int i=0; i<names.length; i++) {
			ZipGenericFile file = new ZipGenericFile(this, names[i]);
			if (filter == null || filter.accept(file)) {
				result.add(file);
			}
		}
		return result.toArray(new ZipGenericFile[result.size()]);
	}

	@Override
	public File[] listFiles(FilenameFilter filter) {
		String[] names = list(filter);
		if (names == null) {
			return null;
		}
		ZipGenericFile[] result = new ZipGenericFile[names.length];
		for (int i=0; i<names.length; i++) {
			result[i] = new ZipGenericFile(this, names[i]);
		}
		return result;
	}

	@Override
	public boolean mkdir() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean mkdirs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean renameTo(File dest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setExecutable(boolean executable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setLastModified(long time) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadable(boolean readable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadOnly() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI toURI() {
		throw new UnsupportedOperationException();
	}

	@Override
	public URL toURL() throws MalformedURLException {
		throw new UnsupportedOperationException();
	}

}

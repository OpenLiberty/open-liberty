/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *******************************************************************************/

package org.eclipse.osgi.internal.cds;

import static org.eclipse.osgi.internal.cds.CDSHookConfigurator.print;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

import com.ibm.oti.shared.SharedClassURLHelper;

/**
 * Wraps an actual BundleFile object for purposes of loading classes from the
 * shared classes cache.
 */
public class CDSBundleFile extends BundleFileWrapper {
	private final static String classFileExt = ".class"; //$NON-NLS-1$
	private final URL url; // the URL to the content of the real bundle file
	private SharedClassURLHelper urlHelper; // the url helper set by the classloader
	private boolean primed = false;
	private final Debug debug;

	/**
	 * The constructor
	 * 
	 * @param wrapped the real bundle file
	 */
	public CDSBundleFile(BundleFile wrapped, Debug debug) {
		super(wrapped);
		this.debug = debug;
		// get the url to the content of the real bundle file
		URL content = null;
		try {
			content = new URL("file", "", wrapped.getBaseFile().getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
			// do nothing
		}
		this.url = content;
	}

	public CDSBundleFile(BundleFile bundleFile, Debug debug, SharedClassURLHelper urlHelper) {
		this(bundleFile, debug);
		this.urlHelper = urlHelper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.osgi.storage.bundlefile.BundleFile#getEntry(java.lang.String)
	 *
	 * If path is not for a class then just use the wrapped bundle file to answer
	 * the call. If the path is for a class, it returns a CDSBundleEntry object. If
	 * the path is for a class, it will look for the magic cookie in the shared
	 * classes cache. If found, the bytes representing the magic cookie are stored
	 * in CDSBundleEntry object.
	 */
	@Override
	public BundleEntry getEntry(String path) {
		if (!primed || !path.endsWith(classFileExt)) {
			return super.getEntry(path);
		}
		byte[] classbytes = getClassBytes(path.substring(0, path.length() - classFileExt.length()));
		if (classbytes == null) {
			BundleEntry fromSuper = super.getEntry(path);
			if (fromSuper != null) {
				print(debug, () -> "Defining class from original bytes: " + url + ' ' + fromSuper.getName()); //$NON-NLS-1$
			}
			return fromSuper;
		}

		BundleEntry be = new CDSBundleEntry(path, classbytes, this);
		return be;
	}

	BundleEntry getWrappedEntry(String path) {
		return super.getEntry(path);
	}

	/**
	 * Returns the file url to the content of the actual bundle file
	 * 
	 * @return the file url to the content of the actual bundle file
	 */
	URL getURL() {
		return url;
	}

	/**
	 * Returns the url helper for this bundle file. This is set by the class loading
	 * hook
	 * 
	 * @return the url helper for this bundle file
	 */
	SharedClassURLHelper getURLHelper() {
		return urlHelper;
	}

	/**
	 * Sets the url helper for this bundle file. This is called by the class loading
	 * hook.
	 * 
	 * @param urlHelper the url helper
	 */
	void setURLHelper(SharedClassURLHelper urlHelper) {
		this.urlHelper = urlHelper;
		this.primed = false; // always unprime when a new urlHelper is set
	}

	/**
	 * Sets the primed flag for the bundle file. This is called by the class loading
	 * hook after the first class has been loaded from disk for this bundle file.
	 * 
	 * @param primed the primed flag
	 */
	void setPrimed(boolean primed) {
		this.primed = primed;
	}

	/**
	 * Searches in the shared classes cache for the specified class name.
	 * 
	 * @param name the name of the class
	 * @return the magic cookie to the shared class or null if the class is not in
	 *         the cache.
	 */
	private byte[] getClassBytes(String name) {
		if (urlHelper == null || url == null)
			return null;
		byte[] results = urlHelper.findSharedClass(null, url, name);
		print(debug, () -> results != null ? "Found shared class bytes for: " + name + ' ' + url //$NON-NLS-1$
				: "No shared class bytes found for: " + name + ' ' + url); //$NON-NLS-1$
		return results;
	}

	/**
	 * Returns the primed flag for this bundle file.
	 * 
	 * @return the primed flag
	 */
	public boolean getPrimed() {
		return this.primed;
	}
}

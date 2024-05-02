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

import com.ibm.oti.shared.HelperAlreadyDefinedException;
import com.ibm.oti.shared.Shared;
import com.ibm.oti.shared.SharedClassHelperFactory;
import com.ibm.oti.shared.SharedClassURLHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.internal.loader.classpath.FragmentClasspath;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.ContentProvider.Type;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapperChain;

public class CDSHookImpls extends ClassLoaderHook implements BundleFileWrapperFactoryHook {
	private static SharedClassHelperFactory factory = Shared.getSharedClassHelperFactory();

	// With Equinox bug 226038 (v3.4), the framework will now pass an instance
	// of BundleFileWrapperChain rather than the wrapped BundleFile.  This is
	// so that multiple wrapping hooks can each wrap the BundleFile and all
	// wrappers are accessible.
	//
	// The Wrapper chain will look like below:
	// WrapperChain -> Wrapper<N> -> WrapperChain -> CDSBundleFile -> WrapperChain -> BundleFile
	//
	private static CDSBundleFile getCDSBundleFile(BundleFile bundleFile) {
		if (bundleFile instanceof BundleFileWrapperChain) {
			return ((BundleFileWrapperChain) bundleFile).getWrappedType(CDSBundleFile.class);
		}
		return null;
	}

	@Override
	public void recordClassDefine(String name, Class<?> clazz, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry, ClasspathManager manager) { // only attempt to record the class define if:
		// 1) the class was found (clazz != null)
		// 2) the class has the magic class number CAFEBABE indicating a real class
		// 3) the bundle file for the classpath entry is of type CDSBundleFile
		// 4) class bytes is same as passed to weaving hook i.e. weaving hook did not modify the class bytes
		if ((null == clazz) || (false == hasMagicClassNumber(classbytes)) || (null == getCDSBundleFile(classpathEntry.getBundleFile()))) {
			return;
		}
		try {
			// check if weaving hook modified the class bytes
			byte originalClassBytes[] = entry.getBytes();
			if (originalClassBytes != classbytes) {
				// weaving hook has potentially modified the class bytes
				boolean modified = false;
				if (originalClassBytes.length == classbytes.length) {
					// do a byte-by-byte comparison
					modified = !Arrays.equals(classbytes, originalClassBytes);
				} else {
					modified = true;
				}
				if (modified) {
					// Class bytes have been modified by weaving hooks.
					// Such classes need to be stored as Orphans, so skip the call to storeSharedClass()
					return;
				}
			}
		} catch (IOException e) {
			// this should never happen, but in case it does, its safe to return
			return;
		}

		CDSBundleFile cdsFile = getCDSBundleFile(classpathEntry.getBundleFile());

		if (null == cdsFile.getURL()) {
			// something went wrong trying to determine the url to the real bundle file
			return;
		}

		// look for the urlHelper; if it does not exist then we are not sharing for this class loader
		SharedClassURLHelper urlHelper = cdsFile.getURLHelper();
		if (urlHelper == null) {
			// this should never happen but just in case get the helper from the base host bundle file.
			CDSBundleFile hostBundleFile = getCDSBundleFile(manager.getGeneration().getBundleFile());
			if (null != hostBundleFile) {
				// try getting the helper from the host base cdsFile
				urlHelper = hostBundleFile.getURLHelper();
			}

			if (null != urlHelper) {
				cdsFile.setURLHelper(urlHelper);
			}
		}
		if (null != urlHelper) {
			// store the class in the cache
			urlHelper.storeSharedClass(null, cdsFile.getURL(), clazz);
			cdsFile.setPrimed(true);
		}
	}

	private boolean hasMagicClassNumber(byte[] classbytes) {
		if (classbytes == null || classbytes.length < 4)
			return false;
		// TODO maybe there is a better way to do this? I'm not sure why I had to AND each byte with the value I was checking ...
		return (classbytes[0] & 0xCA) == 0xCA && (classbytes[1] & 0xFE) == 0xFE && (classbytes[2] & 0xBA) == 0xBA && (classbytes[3] & 0xBE) == 0xBE;
	}

	@Override
	public void classLoaderCreated(ModuleClassLoader classLoader) {
		// try to get the url helper for this class loader
		if (factory == null) {
			return;
		}
		try {
			SharedClassURLHelper urlHelper = factory.getURLHelper(classLoader);
			boolean minimizeUpdateChecks = urlHelper.setMinimizeUpdateChecks();
			// set the url helper for the host base CDSBundleFile
			CDSBundleFile hostFile = getCDSBundleFile(classLoader.getClasspathManager().getGeneration().getBundleFile());
			if (hostFile != null) {
				hostFile.setURLHelper(urlHelper);
				if (minimizeUpdateChecks) {
					// no need to prime if we were able to setMinimizeUpdateChecks
					hostFile.setPrimed(true);
				}
			}
			// No need to prime if we were able to setMinimizeUpdateChecks.
			// Mark all the BundleFiles on the classpath as primed.
			ClasspathManager cpManager = classLoader.getClasspathManager();
			for (ClasspathEntry entry : cpManager.getHostClasspathEntries()) {
				CDSBundleFile cdsBundleFile = getCDSBundleFile(entry.getBundleFile());
				if (cdsBundleFile != null) {
					cdsBundleFile.setURLHelper(urlHelper);
					if (minimizeUpdateChecks) {
						cdsBundleFile.setPrimed(true);
					}
				}
			}
			for (FragmentClasspath fragCP : cpManager.getFragmentClasspaths()) {
				for (ClasspathEntry entry : fragCP.getEntries()) {
					CDSBundleFile cdsBundleFile = getCDSBundleFile(entry.getBundleFile());
					if (cdsBundleFile != null) {
						cdsBundleFile.setURLHelper(urlHelper);
						if (minimizeUpdateChecks) {
							cdsBundleFile.setPrimed(true);
						}
					}
				}
			}
		} catch (HelperAlreadyDefinedException e) {
			// We should never get here.
			// If we do, we simply won't share for this ClassLoader
		}
	}

	@Override
	public boolean addClassPathEntry(ArrayList<ClasspathEntry> cpEntries, String cp, ClasspathManager hostmanager, Generation sourceGeneration) {
		CDSBundleFile hostFile = getCDSBundleFile(hostmanager.getGeneration().getBundleFile());
		CDSBundleFile sourceFile = getCDSBundleFile(sourceGeneration.getBundleFile());
		if ((hostFile != sourceFile) && (null != hostFile) && (null != sourceFile)) {
			// Set the helper that got set on the host base bundle file in classLoaderCreated.
			// This is to handle the case where fragments are dynamically attached
			SharedClassURLHelper urlHelper = hostFile.getURLHelper();
			sourceFile.setURLHelper(urlHelper);
			sourceFile.setPrimed(hostFile.getPrimed());
		}

		return false;
	}

	//////////////// BundleFileWrapperFactoryHook //////////////
	@Override
	public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
		if (generation.getContentType() == Type.CONNECT) {
			return null;
		}
		// wrap the real bundle file for purposes of loading shared classes.
		CDSBundleFile newBundleFile;
		if (!base && generation.getBundleInfo().getBundleId() != 0) {
			// initialize the urlHelper from the base one.
			SharedClassURLHelper urlHelper = null;
			BundleFile baseFile = generation.getBundleFile();
			if ((baseFile = getCDSBundleFile(baseFile)) != null) {
				urlHelper = ((CDSBundleFile) baseFile).getURLHelper();
			}
			newBundleFile = new CDSBundleFile(bundleFile, urlHelper);
		} else {
			newBundleFile = new CDSBundleFile(bundleFile);
		}

		return newBundleFile;
	}

	void registerHooks(HookRegistry hookRegistry) {
		// only register if sharing is enabled
		if (!Shared.isSharingEnabled()) {
			return;
		}
		hookRegistry.addClassLoaderHook(this);
		hookRegistry.addBundleFileWrapperFactoryHook(this);
	}
}

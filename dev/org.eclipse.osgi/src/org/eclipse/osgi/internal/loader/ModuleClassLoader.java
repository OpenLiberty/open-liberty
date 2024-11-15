/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public abstract class ModuleClassLoader extends ClassLoader implements BundleReference {
	public static class GenerationProtectionDomain extends ProtectionDomain implements BundleReference {
		private final Generation generation;

		public GenerationProtectionDomain(CodeSource codesource, PermissionCollection permissions, Generation generation) {
			super(codesource, permissions);
			this.generation = generation;
		}

		@Override
		public Bundle getBundle() {
			return generation.getRevision().getBundle();
		}
	}

	/**
	 * A PermissionCollection for AllPermissions; shared across all ProtectionDomains when security is disabled
	 */
	protected static final PermissionCollection ALLPERMISSIONS;
	protected static final boolean REGISTERED_AS_PARALLEL;
	static {
		boolean registered;
		try {
			registered = ClassLoader.registerAsParallelCapable();
		} catch (Throwable t) {
			registered = false;
		}
		REGISTERED_AS_PARALLEL = registered;
	}

	static {
		AllPermission allPerm = new AllPermission();
		ALLPERMISSIONS = allPerm.newPermissionCollection();
		if (ALLPERMISSIONS != null)
			ALLPERMISSIONS.add(allPerm);
	}

	/**
	 * Holds the result of a defining a class.
	 *
	 */
	public static class DefineClassResult {
		/**
		 * The class object that either got defined or was found as previously loaded.
		 */
		public final Class<?> clazz;
		/**
		 * Set to true if the class object got defined; set to false if the class
		 * did not get defined correctly or the class was found as a previously loaded
		 * class.
		 */
		public final boolean defined;

		public DefineClassResult(Class<?> clazz, boolean defined) {
			this.clazz = clazz;
			this.defined = defined;
		}
	}

	private final Map<String, Thread> classNameLocks = new HashMap<>(5);
	private final Object pkgLock = new Object();

	/**
	 * Constructs a new ModuleClassLoader.
	 * @param parent the parent classloader
	 */
	public ModuleClassLoader(ClassLoader parent) {
		super(parent);
	}

	/**
	 * Returns the generation of the host revision associated with this class loader
	 * @return the generation for this class loader
	 */
	protected abstract Generation getGeneration();

	/**
	 * Returns the Debug object for the Framework instance
	 * @return the Debug object for the Framework instance
	 */
	protected abstract Debug getDebug();

	/**
	 * Returns the classpath manager for this class loader
	 * @return the classpath manager for this class loader
	 */
	public abstract ClasspathManager getClasspathManager();

	/**
	 * Returns the configuration for the Framework instance
	 * @return the configuration for the Framework instance
	 */
	protected abstract EquinoxConfiguration getConfiguration();

	/**
	 * Returns the bundle loader for this class loader
	 * @return the bundle loader for this class loader
	 */
	public abstract BundleLoader getBundleLoader();

	/**
	 * Returns true if this class loader implementation has been
	 * registered with the JVM as a parallel class loader.
	 * This requires Java 7 or later.
	 * @return true if this class loader implementation has been
	 * registered with the JVM as a parallel class loader; otherwise
	 * false is returned.
	 */
	public abstract boolean isRegisteredAsParallel();

	/**
	 * Loads a class for the bundle.  First delegate.findClass(name) is called.
	 * The delegate will query the system class loader, bundle imports, bundle
	 * local classes, bundle hosts and fragments.  The delegate will call
	 * BundleClassLoader.findLocalClass(name) to find a class local to this
	 * bundle.
	 * @param name the name of the class to load.
	 * @param resolve indicates whether to resolve the loaded class or not.
	 * @return The Class object.
	 * @throws ClassNotFoundException if the class is not found.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// Just ask the delegate. This could result in findLocalClass(name) being
		// called.
		Class<?> clazz = getBundleLoader().findClass(name);
		// resolve the class if asked to.
		if (resolve) {
			resolveClass(clazz);
		}
		return (clazz);
	}

	// preparing for Java 9
	protected Class<?> findClass(String moduleName, String name) {
		try {
			return findLocalClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return findLocalClass(name);
	}

	/**
	 * Gets a resource for the bundle.  First delegate.findResource(name) is
	 * called. The delegate will query the system class loader, bundle imports,
	 * bundle local resources, bundle hosts and fragments.  The delegate will
	 * call BundleClassLoader.findLocalResource(name) to find a resource local
	 * to this bundle.
	 * @param name The resource path to get.
	 * @return The URL of the resource or null if it does not exist.
	 */
	@Override
	public URL getResource(String name) {
		if (getDebug().DEBUG_LOADER) {
			Debug.println("ModuleClassLoader[" + getBundleLoader() + "].getResource(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		URL url = getBundleLoader().findResource(name);
		if (url != null)
			return (url);

		if (getDebug().DEBUG_LOADER) {
			Debug.println("ModuleClassLoader[" + getBundleLoader() + "].getResource(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return (null);
	}

	// preparing for Java 9
	protected URL findResource(String moduleName, String name) {
		return findLocalResource(name);
	}

	@Override
	protected URL findResource(String name) {
		return findLocalResource(name);
	}

	/**
	 * Gets resources for the bundle.  First delegate.findResources(name) is
	 * called. The delegate will query the system class loader, bundle imports,
	 * bundle local resources, bundle hosts and fragments.  The delegate will
	 * call BundleClassLoader.findLocalResources(name) to find a resource local
	 * to this bundle.
	 * @param name The resource path to get.
	 * @return The Enumeration of the resource URLs.
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if (getDebug().DEBUG_LOADER) {
			Debug.println("ModuleClassLoader[" + getBundleLoader() + "].getResources(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		Enumeration<URL> result = getBundleLoader().findResources(name);
		if (getDebug().DEBUG_LOADER) {
			if (result == null || !result.hasMoreElements()) {
				Debug.println("ModuleClassLoader[" + getBundleLoader() + "].getResources(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		return result;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return findLocalResources(name);
	}

	/**
	 * Finds a library for this bundle.  Simply calls
	 * manager.findLibrary(libname) to find the library.
	 * @param libname The library to find.
	 * @return The absolution path to the library or null if not found
	 */
	@Override
	protected String findLibrary(String libname) {
		// let the manager find the library for us
		return getClasspathManager().findLibrary(libname);
	}

	public ClasspathEntry createClassPathEntry(BundleFile bundlefile, Generation entryGeneration) {
		return new ClasspathEntry(bundlefile, createProtectionDomain(bundlefile, entryGeneration), entryGeneration);
	}

	public DefineClassResult defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry) {
		// Note that we must check findLoadedClass again here since no locks are held between
		// calling findLoadedClass the first time and defineClass.
		// This is to allow weavers to get called while holding no locks.
		// See ClasspathManager.findLocalClass(String)
		boolean defined = false;
		Class<?> result = null;
		if (isRegisteredAsParallel()) {
			// lock by class name in this case
			boolean initialLock = lockClassName(name);
			try {
				result = findLoadedClass(name);
				if (result == null) {
					result = defineClass(name, classbytes, 0, classbytes.length, classpathEntry.getDomain());
					defined = true;
				}
			} finally {
				if (initialLock) {
					unlockClassName(name);
				}
			}
		} else {
			// lock by class loader instance in this case
			synchronized (this) {
				result = findLoadedClass(name);
				if (result == null) {
					result = defineClass(name, classbytes, 0, classbytes.length, classpathEntry.getDomain());
					defined = true;
				}
			}
		}
		return new DefineClassResult(result, defined);
	}

	public Class<?> publicFindLoaded(String classname) {
		if (isRegisteredAsParallel()) {
			return findLoadedClass(classname);
		}
		synchronized (this) {
			return findLoadedClass(classname);
		}
	}

	public Package publicGetPackage(String pkgname) {
		synchronized (pkgLock) {
			return getPackage(pkgname);
		}
	}

	public Package publicDefinePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
		synchronized (pkgLock) {
			Package pkg = getPackage(name);
			return pkg != null ? pkg : definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
		}
	}

	public URL findLocalResource(String resource) {
		return getClasspathManager().findLocalResource(resource);
	}

	public Enumeration<URL> findLocalResources(String resource) {
		return getClasspathManager().findLocalResources(resource);
	}

	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
		return getClasspathManager().findLocalClass(classname);
	}

	/**
	 * Creates a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain
	 * @param bundlefile The source bundlefile the domain is for.
	 * @param domainGeneration the source generation for the domain
	 * @return a ProtectionDomain which uses specified BundleFile and the permissions of the baseDomain
	 */
	@SuppressWarnings("deprecation")
	protected ProtectionDomain createProtectionDomain(BundleFile bundlefile, Generation domainGeneration) {
		// create a protection domain which knows about the codesource for this classpath entry (bug 89904)
		ProtectionDomain baseDomain = domainGeneration.getDomain();
		try {
			// use the permissions supplied by the domain passed in from the framework
			PermissionCollection permissions;
			if (baseDomain != null) {
				permissions = baseDomain.getPermissions();
			} else {
				// no domain specified.  Better use a collection that has all permissions
				// this is done just incase someone sets the security manager later
				permissions = ALLPERMISSIONS;
			}
			Certificate[] certs = null;
			if (getConfiguration().CLASS_CERTIFICATE) {
				Bundle b = getBundle();
				SignedContent signedContent = b == null ? null : b.adapt(SignedContent.class);
				if (signedContent != null && signedContent.isSigned()) {
					SignerInfo[] signers = signedContent.getSignerInfos();
					if (signers.length > 0) {
						certs = signers[0].getCertificateChain();
					}
				}
			}
			File file = bundlefile.getBaseFile();
			// Bug 477787: file will be null when the osgi.framework configuration property contains an invalid value.
			return new GenerationProtectionDomain(file == null ? null : new CodeSource(file.toURL(), certs), permissions, getGeneration());
			//return new ProtectionDomain(new CodeSource(bundlefile.getBaseFile().toURL(), certs), permissions);
		} catch (MalformedURLException e) {
			// Failed to create our own domain; just return the baseDomain
			return baseDomain;
		}
	}

	@Override
	public Bundle getBundle() {
		return getGeneration().getRevision().getBundle();
	}

	public List<URL> findEntries(String path, String filePattern, int options) {
		return getClasspathManager().findEntries(path, filePattern, options);
	}

	public Collection<String> listResources(String path, String filePattern, int options) {
		return getBundleLoader().listResources(path, filePattern, options);
	}

	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		return getClasspathManager().listLocalResources(path, filePattern, options);
	}

	@Override
	public String toString() {
		Bundle b = getBundle();
		StringBuilder result = new StringBuilder(super.toString());
		if (b == null)
			return result.toString();
		return result.append('[').append(b.getSymbolicName()).append(':').append(b.getVersion()).append("(id=").append(b.getBundleId()).append(")]").toString(); //$NON-NLS-1$//$NON-NLS-2$
	}

	public void loadFragments(Collection<ModuleRevision> fragments) {
		getClasspathManager().loadFragments(fragments);
	}

	private boolean lockClassName(String classname) {
		synchronized (classNameLocks) {
			Object lockingThread = classNameLocks.get(classname);
			Thread current = Thread.currentThread();
			if (lockingThread == current)
				return false;
			boolean previousInterruption = Thread.interrupted();
			try {
				while (true) {
					if (lockingThread == null) {
						classNameLocks.put(classname, current);
						return true;
					}

					classNameLocks.wait();
					lockingThread = classNameLocks.get(classname);
				}
			} catch (InterruptedException e) {
				previousInterruption = true;
				// must not throw LinkageError or ClassNotFoundException here because that will cause all threads
				// to fail to load the class (see bug 490902)
				throw new Error("Interrupted while waiting for classname lock: " + classname, e); //$NON-NLS-1$
			} finally {
				if (previousInterruption) {
					current.interrupt();
				}
			}
		}
	}

	private void unlockClassName(String classname) {
		synchronized (classNameLocks) {
			classNameLocks.remove(classname);
			classNameLocks.notifyAll();
		}
	}

	public void close() {
		getClasspathManager().close();
	}
}

/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleRequirement;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.buddy.PolicyHandler;
import org.eclipse.osgi.internal.loader.sources.MultiSourcePackage;
import org.eclipse.osgi.internal.loader.sources.NullPackageSource;
import org.eclipse.osgi.internal.loader.sources.PackageSource;
import org.eclipse.osgi.internal.loader.sources.SingleSourcePackage;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This object is responsible for all classloader delegation for a bundle.
 * It represents the loaded state of the bundle.  BundleLoader objects
 * are created lazily; care should be taken not to force the creation
 * of a BundleLoader unless it is necessary.
 * @see org.eclipse.osgi.internal.loader.BundleLoaderSources
 */
public class BundleLoader extends ModuleLoader {
	public final static String DEFAULT_PACKAGE = "."; //$NON-NLS-1$
	public final static String JAVA_PACKAGE = "java."; //$NON-NLS-1$

	public final static ClassContext CLASS_CONTEXT = AccessController.doPrivileged(new PrivilegedAction<ClassContext>() {
		@Override
		public ClassContext run() {
			return new ClassContext();
		}
	});
	public final static ClassLoader FW_CLASSLOADER = getClassLoader(EquinoxContainer.class);

	private static final int PRE_CLASS = 1;
	private static final int POST_CLASS = 2;
	private static final int PRE_RESOURCE = 3;
	private static final int POST_RESOURCE = 4;
	private static final int PRE_RESOURCES = 5;
	private static final int POST_RESOURCES = 6;

	private static final Pattern PACKAGENAME_FILTER = Pattern.compile("\\(osgi.wiring.package\\s*=\\s*([^)]+)\\)"); //$NON-NLS-1$

	private final ModuleWiring wiring;
	private final EquinoxContainer container;
	private final Debug debug;
	private final PolicyHandler policy;

	/* List of package names that are exported by this BundleLoader */
	private final Collection<String> exportedPackages;
	private final BundleLoaderSources exportSources;

	/* cache of required package sources. Key is packagename, value is PackageSource */
	private final Map<String, PackageSource> requiredSources = new HashMap<>();
	/* cache of imported packages. Key is packagename, Value is PackageSource */
	private final Map<String, PackageSource> importedSources = new HashMap<>();
	private final List<ModuleWire> requiredBundleWires;

	/* @GuardedBy("importedSources") */
	private boolean importsInitialized = false;
	/* @GuardedBy("importedSources") */
	private boolean dynamicAllPackages;
	/* If not null, list of package stems to import dynamically. */
	/* @GuardedBy("importedSources") */
	private String[] dynamicImportPackageStems;
	/* @GuardedBy("importedSources") */
	/* If not null, list of package names to import dynamically. */
	private String[] dynamicImportPackages;

	private final Object classLoaderCreatedMonitor = new Object();
	/* @GuardedBy("classLoaderCreatedMonitor") */
	private ModuleClassLoader classLoaderCreated;
	private volatile ModuleClassLoader classloader;
	private final ClassLoader parent;
	private final AtomicBoolean triggerClassLoaded = new AtomicBoolean(false);
	private final AtomicBoolean firstUseOfInvalidLoader = new AtomicBoolean(false);

	/**
	 * Returns the package name from the specified class name.
	 * The returned package is dot seperated.
	 *
	 * @param name   Name of a class.
	 * @return Dot separated package name or null if the class
	 *         has no package name.
	 */
	public final static String getPackageName(String name) {
		if (name != null) {
			int index = name.lastIndexOf('.'); /* find last period in class name */
			if (index > 0)
				return name.substring(0, index);
		}
		return DEFAULT_PACKAGE;
	}

	/**
	 * Returns the package name from the specified resource name.
	 * The returned package is dot seperated.
	 *
	 * @param name   Name of a resource.
	 * @return Dot separated package name or null if the resource
	 *         has no package name.
	 */
	public final static String getResourcePackageName(String name) {
		if (name != null) {
			/* check for leading slash*/
			int begin = ((name.length() > 1) && (name.charAt(0) == '/')) ? 1 : 0;
			int end = name.lastIndexOf('/'); /* index of last slash */
			if (end > begin)
				return name.substring(begin, end).replace('/', '.');
		}
		return DEFAULT_PACKAGE;
	}

	public BundleLoader(ModuleWiring wiring, EquinoxContainer container, ClassLoader parent) {
		this.wiring = wiring;
		this.container = container;
		this.debug = container.getConfiguration().getDebug();
		this.parent = parent;

		// init the provided packages set
		exportSources = new BundleLoaderSources(this);
		List<ModuleCapability> exports = wiring.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		exports = exports == null ? Collections.emptyList() : exports;
		exportedPackages = Collections.synchronizedCollection(exports.size() > 10 ? new HashSet<String>(exports.size()) : new ArrayList<String>(exports.size()));
		initializeExports(exports, exportSources, exportedPackages);

		// init the dynamic imports tables
		addDynamicImportPackage(wiring.getModuleRequirements(PackageNamespace.PACKAGE_NAMESPACE));

		// initialize the required bundle wires
		List<ModuleWire> currentRequireBundleWires = wiring.getRequiredModuleWires(BundleNamespace.BUNDLE_NAMESPACE);
		requiredBundleWires = currentRequireBundleWires == null || currentRequireBundleWires.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(currentRequireBundleWires);

		//Initialize the policy handler
		List<ModuleCapability> moduleDatas = wiring.getRevision().getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		@SuppressWarnings("unchecked")
		List<String> buddyList = (List<String>) (moduleDatas.isEmpty() ? null : moduleDatas.get(0).getAttributes().get(EquinoxModuleDataNamespace.CAPABILITY_BUDDY_POLICY));
		policy = buddyList != null
				? new PolicyHandler(this, buddyList, container.getStorage().getModuleContainer().getFrameworkWiring(),
						container.getBootLoader())
				: null;
		if (policy != null) {
			Module systemModule = container.getStorage().getModuleContainer().getModule(0);
			Bundle systemBundle = systemModule.getBundle();
			policy.open(systemBundle.getBundleContext());
		}
	}

	public ModuleWiring getWiring() {
		return wiring;
	}

	public void addFragmentExports(List<ModuleCapability> exports) {
		initializeExports(exports, exportSources, exportedPackages);
	}

	private static void initializeExports(List<ModuleCapability> exports, BundleLoaderSources sources, Collection<String> exportNames) {
		if (exports != null) {
			for (ModuleCapability export : exports) {
				String name = (String) export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
				if (sources.forceSourceCreation(export)) {
					if (!exportNames.contains(name)) {
						// must force filtered and reexport sources to be created early
						// to prevent lazy normal package source creation.
						// We only do this for the first export of a package name.
						sources.createPackageSource(export, true);
					}
				}
				exportNames.add(name);
			}
		}
	}

	final PackageSource createExportPackageSource(ModuleWire importWire, Collection<BundleLoader> visited) {
		String name = (String) importWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
		BundleLoader providerLoader = getProviderLoader(importWire);
		if (providerLoader == null) {
			return createMultiSource(name, new PackageSource[0]);
		}
		PackageSource requiredSource = providerLoader.findRequiredSource(name, visited);
		PackageSource exportSource = providerLoader.exportSources.createPackageSource(importWire.getCapability(), false);
		if (requiredSource == null)
			return exportSource;
		return createMultiSource(name, new PackageSource[] {requiredSource, exportSource});
	}

	private static PackageSource createMultiSource(String packageName, PackageSource[] sources) {
		if (sources.length == 1)
			return sources[0];
		List<SingleSourcePackage> sourceList = new ArrayList<>(sources.length);
		for (PackageSource source : sources) {
			SingleSourcePackage[] innerSources = source.getSuppliers();
			for (SingleSourcePackage innerSource : innerSources) {
				if (!sourceList.contains(innerSource)) {
					sourceList.add(innerSource);
				}
			}
		}
		return new MultiSourcePackage(packageName, sourceList.toArray(new SingleSourcePackage[sourceList.size()]));
	}

	public ModuleClassLoader getModuleClassLoader() {
		ModuleClassLoader result = classloader;
		if (result != null) {
			return result;
		}
		// doing optimistic class loader creating here to avoid holding any locks,
		// this may result in multiple classloaders being constructed but only one will be used.
		final List<ClassLoaderHook> hooks = container.getConfiguration().getHookRegistry().getClassLoaderHooks();
		final Generation generation = (Generation) wiring.getRevision().getRevisionInfo();
		if (System.getSecurityManager() == null) {
			result = createClassLoaderPrivledged(parent, generation.getBundleInfo().getStorage().getConfiguration(), this, generation, hooks);
		} else {
			final ClassLoader cl = parent;
			result = AccessController.doPrivileged(new PrivilegedAction<ModuleClassLoader>() {
				@Override
				public ModuleClassLoader run() {
					return createClassLoaderPrivledged(cl, generation.getBundleInfo().getStorage().getConfiguration(), BundleLoader.this, generation, hooks);
				}
			});
		}

		// Synchronize on classLoaderCreatedMonitor in order to ensure hooks are called before returning.
		// Note that we do hold a lock here while calling hooks.
		// Not ideal, but hooks really should do little work from classLoaderCreated method.
		synchronized (classLoaderCreatedMonitor) {
			if (classLoaderCreated == null) {
				// must set createdClassloader before calling hooks; otherwise we could enter
				// and endless loop if the hook causes re-entry (that would be a bad hook impl)
				classLoaderCreated = result;
				// only send to hooks if this thread wins in creating the class loader.
				final ModuleClassLoader cl = result;
				// protect with doPriv to avoid bubbling up permission checks that hooks may require
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						for (ClassLoaderHook hook : hooks) {
							hook.classLoaderCreated(cl);
						}
						return null;
					}

				});
				// finally set the class loader for use after calling hooks
				classloader = classLoaderCreated;
			} else {
				// return the classLoaderCreated here; not the final classloader
				// this is necessary in case re-entry by a hook.classLoaderCreated method
				result = classLoaderCreated;
				if (debug.DEBUG_LOADER) {
					Debug.println("BundleLoader[" + this + "].getModuleClassLoader() - created duplicate classloader"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		return result;
	}

	@Override
	protected void loadFragments(Collection<ModuleRevision> fragments) {
		addFragmentExports(wiring.getModuleCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
		loadClassLoaderFragments(fragments);
		clearManifestLocalizationCache();
	}

	protected void clearManifestLocalizationCache() {
		Generation hostGen = (Generation) wiring.getRevision().getRevisionInfo();
		hostGen.clearManifestCache();
		List<ModuleWire> hostWires = wiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		// doing a null check because there is no lock on the module database here
		if (hostWires != null) {
			for (ModuleWire fragmentWire : hostWires) {
				Generation fragGen = (Generation) fragmentWire.getRequirer().getRevisionInfo();
				fragGen.clearManifestCache();
			}
		}
	}

	void loadClassLoaderFragments(Collection<ModuleRevision> fragments) {
		ModuleClassLoader current = classloader;
		if (current != null) {
			current.loadFragments(fragments);
		}
	}

	static ModuleClassLoader createClassLoaderPrivledged(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation, List<ClassLoaderHook> hooks) {
		// allow hooks to extend the ModuleClassLoader implementation
		for (ClassLoaderHook hook : hooks) {
			ModuleClassLoader hookClassLoader = hook.createClassLoader(parent, configuration, delegate, generation);
			if (hookClassLoader != null) {
				// first one to return non-null wins.
				return hookClassLoader;
			}
		}
		// just use the default one
		return new EquinoxClassLoader(parent, configuration, delegate, generation);
	}

	public void close() {
		if (policy != null) {
			Module systemModule = container.getStorage().getModuleContainer().getModule(0);
			BundleContext context = systemModule.getBundle().getBundleContext();
			if (context != null) {
				policy.close(context);
			}
		}
		ModuleClassLoader current = classloader;
		if (current != null) {
			current.close();
		}
	}

	@Override
	protected ClassLoader getClassLoader() {
		return getModuleClassLoader();
	}

	public ClassLoader getParentClassLoader() {
		return this.parent;
	}

	/**
	 * This method gets a resource from the bundle.  The resource is searched
	 * for in the same manner as it would if it was being loaded from a bundle
	 * (i.e. all hosts, fragments, import, required bundles and
	 * local resources are searched).
	 *
	 * @param name the name of the desired resource.
	 * @return the resulting resource URL or null if it does not exist.
	 */
	final URL getResource(String name) {
		return getModuleClassLoader().getResource(name);
	}

	/**
	 * Finds a class local to this bundle.  Only the classloader for this bundle is searched.
	 * @param name The name of the class to find.
	 * @return The loaded Class or null if the class is not found.
	 * @throws ClassNotFoundException
	 */
	public Class<?> findLocalClass(String name) throws ClassNotFoundException {
		long start = 0;
		if (debug.DEBUG_LOADER) {
			Debug.println("BundleLoader[" + this + "].findLocalClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			start = System.currentTimeMillis();
		}
		try {
			Class<?> clazz = getModuleClassLoader().findLocalClass(name);
			if (debug.DEBUG_LOADER && clazz != null) {
				long time = System.currentTimeMillis() - start;
				Debug.println("BundleLoader[" + this + "] found local class " + name + " " + time + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return clazz;
		} catch (ClassNotFoundException e) {
			if (e.getCause() instanceof BundleException) {
				// Here we assume this is because of a lazy activation error
				throw e;
			}
			return null;
		}
	}

	/**
	 * Finds the class for a bundle.  This method is used for delegation by the bundle's classloader.
	 */
	public Class<?> findClass(String name) throws ClassNotFoundException {
		return findClass0(name, true, true);
	}

	// useful outside of the framework to do no exception delegation
	// but have it search parent for bootdelegation
	public Class<?> findClassNoException(String name) {
		try {
			return findClass0(name, true, false);
		} catch (ClassNotFoundException e) {
			// should rarely happen
			// e.g. when a lazy activation fails to start a bundle
			return null;
		}
	}

	public Class<?> findClassNoParentNoException(String name) {
		try {
			return findClass0(name, false, false);
		} catch (ClassNotFoundException e) {
			// should rarely happen
			// e.g. when a lazy activation fails to start a bundle
			return null;
		}
	}

	private Class<?> findClass0(String name, boolean parentDelegation, boolean generateException)
			throws ClassNotFoundException {
		if (parentDelegation && parent != null && name.startsWith(JAVA_PACKAGE)) {
			// 1) if startsWith "java." delegate to parent and terminate search
			// we want to throw ClassNotFoundExceptions if a java.* class cannot be loaded from the parent.
			return parent.loadClass(name);
		}

		if (debug.DEBUG_LOADER)
			Debug.println("BundleLoader[" + this + "].findClass(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String pkgName = getPackageName(name);
		boolean bootDelegation = false;
		// follow the OSGi delegation model
		if (parentDelegation && parent != null && container.isBootDelegationPackage(pkgName)) {
			// 2) if part of the bootdelegation list then delegate to parent and continue of failure
			try {
				return parent.loadClass(name);
			} catch (ClassNotFoundException cnfe) {
				// we want to continue
				bootDelegation = true;
			}
		}
		Class<?> result = null;
		try {
			result = (Class<?>) searchHooks(name, PRE_CLASS);
		} catch (FileNotFoundException e) {
			// will not happen
		}
		if (result != null)
			return result;
		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from import package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 3) found import source terminate search at the source
			result = source.loadClass(name);
			if (result == null) {
				// last ditch find loaded check in case something is reflectively
				// calling defineClass on our loader.
				result = getModuleClassLoader().publicFindLoaded(name);
			}
			if (result != null)
				return result;
			return generateException(name, generateException);
		}
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from required bundle package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 4) attempt to load from source but continue on failure
			result = source.loadClass(name);
		}
		// 5) search the local bundle
		if (result == null)
			result = findLocalClass(name);
		if (result != null)
			return result;
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (source == null) {
			source = findDynamicSource(pkgName);
			if (source != null) {
				result = source.loadClass(name);
				if (result != null)
					return result;
				return generateException(name, generateException);
			}
		}

		if (result == null)
			try {
				result = (Class<?>) searchHooks(name, POST_CLASS);
			} catch (FileNotFoundException e) {
				// will not happen
			}
		// do buddy policy loading
		if (result == null && policy != null)
			result = policy.doBuddyClassLoading(name);
		if (result != null)
			return result;
		// hack to support backwards compatibility for bootdelegation
		// or last resort; do class context trick to work around VM bugs
		if (parentDelegation && parent != null && !bootDelegation
				&& ((container.getConfiguration().compatibilityBootDelegation) || isRequestFromVM())) {
			// we don't need to continue if a CNFE is thrown here.
			try {
				return parent.loadClass(name);
			} catch (ClassNotFoundException e) {
				// we want to generate our own exception below
			}
		}
		return generateException(name, generateException);
	}

	private Class<?> generateException(String name, boolean generate) throws ClassNotFoundException {
		if (generate) {
			ClassNotFoundException e = new ClassNotFoundException(name + " cannot be found by " + this); //$NON-NLS-1$
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "].loadClass(" + name + ") failed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Debug.printStackTrace(e);
			}
			throw e;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <E> E searchHooks(String name, int type) throws ClassNotFoundException, FileNotFoundException {

		List<ClassLoaderHook> loaderHooks = container.getConfiguration().getHookRegistry().getClassLoaderHooks();
		if (loaderHooks == null)
			return null;
		E result = null;
		for (ClassLoaderHook hook : loaderHooks) {
			switch (type) {
				case PRE_CLASS :
					result = (E) hook.preFindClass(name, getModuleClassLoader());
					break;
				case POST_CLASS :
					result = (E) hook.postFindClass(name, getModuleClassLoader());
					break;
				case PRE_RESOURCE :
					result = (E) hook.preFindResource(name, getModuleClassLoader());
					break;
				case POST_RESOURCE :
					result = (E) hook.postFindResource(name, getModuleClassLoader());
					break;
				case PRE_RESOURCES :
					result = (E) hook.preFindResources(name, getModuleClassLoader());
					break;
				case POST_RESOURCES :
					result = (E) hook.postFindResources(name, getModuleClassLoader());
					break;
			}
			if (result != null) {
				return result;
			}
		}
		return result;
	}

	private boolean isRequestFromVM() {
		if (!container.getConfiguration().contextBootDelegation)
			return false;
		// works around VM bugs that require all classloaders to have access to parent packages
		Class<?>[] context = CLASS_CONTEXT.getClassContext();
		if (context == null || context.length < 2)
			return false;
		// skip the first class; it is the ClassContext class
		for (int i = 1; i < context.length; i++) {
			Class<?> clazz = context[i];
			// Find the first class in the context which is not BundleLoader or the ModuleClassLoader;
			// We ignore ClassLoader because ModuleClassLoader extends it.
			// We ignore Class because of Class.forName (bug 471551)
			if (clazz != BundleLoader.class && !ModuleClassLoader.class.isAssignableFrom(clazz) && clazz != ClassLoader.class && clazz != Class.class && !clazz.getName().equals("java.lang.J9VMInternals")) { //$NON-NLS-1$
				if (Bundle.class.isAssignableFrom(clazz)) {
					// We ignore any requests from Bundle (e.g. Bundle.loadClass case)
					return false;
				}
				// only find in parent if the class is not loaded with a ModuleClassLoader
				ClassLoader cl = getClassLoader(clazz);
				// extra check incase an adaptor adds another class into the stack besides an instance of ClassLoader
				if (cl != FW_CLASSLOADER) {
					// if the class is loaded from a class loader implemented by a bundle then we do not boot delegate
					ClassLoader last = null;
					while (cl != null && cl != last) {
						last = cl;
						if (cl instanceof ModuleClassLoader) {
							return false;
						}
						cl = getClassLoader(cl.getClass());
					}

					// request is not from a bundle
					return true;
				}
			}
		}
		return false;
	}

	private static ClassLoader getClassLoader(final Class<?> clazz) {
		if (System.getSecurityManager() == null)
			return clazz.getClassLoader();
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			@Override
			public ClassLoader run() {
				return clazz.getClassLoader();
			}
		});
	}

	/**
	 * Finds the resource for a bundle.  This method is used for delegation by the bundle's classloader.
	 */
	public URL findResource(String name) {
		if (debug.DEBUG_LOADER)
			Debug.println("BundleLoader[" + this + "].findResource(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
			name = name.substring(1); /* remove leading slash before search */
		String pkgName = getResourcePackageName(name);
		boolean bootDelegation = false;
		// follow the OSGi delegation model
		// First check the parent classloader for system resources, if it is a java resource.
		if (parent != null) {
			if (pkgName.startsWith(JAVA_PACKAGE))
				// 1) if startsWith "java." delegate to parent and terminate search
				// we never delegate java resource requests past the parent
				return parent.getResource(name);
			else if (container.isBootDelegationPackage(pkgName)) {
				// 2) if part of the bootdelegation list then delegate to parent and continue of failure
				URL result = parent.getResource(name);
				if (result != null)
					return result;
				bootDelegation = true;
			}
		}

		URL result = null;
		try {
			result = (URL) searchHooks(name, PRE_RESOURCE);
		} catch (FileNotFoundException e) {
			return null;
		} catch (ClassNotFoundException e) {
			// will not happen
		}
		if (result != null)
			return result;
		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from import package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 3) found import source terminate search at the source
			return source.getResource(name);
		}
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from required bundle package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 4) attempt to load from source but continue on failure
			result = source.getResource(name);
		}
		// 5) search the local bundle
		if (result == null)
			result = findLocalResource(name);
		if (result != null)
			return result;
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (source == null) {
			source = findDynamicSource(pkgName);
			if (source != null)
				// must return the result of the dynamic import and do not continue
				return source.getResource(name);
		}

		if (result == null)
			try {
				result = (URL) searchHooks(name, POST_RESOURCE);
			} catch (FileNotFoundException e) {
				return null;
			} catch (ClassNotFoundException e) {
				// will not happen
			}
		// do buddy policy loading
		if (result == null && policy != null)
			result = policy.doBuddyResourceLoading(name);
		if (result != null)
			return result;
		// hack to support backwards compatibility for bootdelegation
		// or last resort; do class context trick to work around VM bugs
		if (parent != null && !bootDelegation && (container.getConfiguration().compatibilityBootDelegation || isRequestFromVM()))
			// we don't need to continue if the resource is not found here
			return parent.getResource(name);
		return result;
	}

	/**
	 * Finds the resources for a bundle.  This  method is used for delegation by the bundle's classloader.
	 */
	public Enumeration<URL> findResources(String name) throws IOException {
		if (debug.DEBUG_LOADER)
			Debug.println("BundleLoader[" + this + "].findResources(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// do not delegate to parent because ClassLoader#getResources already did and it is final!!
		if ((name.length() > 1) && (name.charAt(0) == '/')) /* if name has a leading slash */
			name = name.substring(1); /* remove leading slash before search */
		String pkgName = getResourcePackageName(name);
		Enumeration<URL> result = Collections.emptyEnumeration();
		boolean bootDelegation = false;
		// follow the OSGi delegation model
		// First check the parent classloader for system resources, if it is a java resource.
		if (parent != null) {
			if (pkgName.startsWith(JAVA_PACKAGE))
				// 1) if startsWith "java." delegate to parent and terminate search
				// we never delegate java resource requests past the parent
				return parent.getResources(name);
			else if (container.isBootDelegationPackage(pkgName)) {
				// 2) if part of the bootdelegation list then delegate to parent and continue
				result = compoundEnumerations(result, parent.getResources(name));
				bootDelegation = true;
			}
		}
		try {
			Enumeration<URL> hookResources = searchHooks(name, PRE_RESOURCES);
			if (hookResources != null) {
				return compoundEnumerations(result, hookResources);
			}
		} catch (ClassNotFoundException e) {
			// will not happen
		} catch (FileNotFoundException e) {
			return result;
		}

		// 3) search the imported packages
		PackageSource source = findImportedSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from import package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 3) found import source terminate search at the source
			return compoundEnumerations(result, source.getResources(name));
		}
		// 4) search the required bundles
		source = findRequiredSource(pkgName, null);
		if (source != null) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] loading from required bundle package: " + source); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 4) attempt to load from source but continue on failure
			result = compoundEnumerations(result, source.getResources(name));
		}
		// 5) search the local bundle
		// compound the required source results with the local ones
		Enumeration<URL> localResults = findLocalResources(name);
		result = compoundEnumerations(result, localResults);
		// 6) attempt to find a dynamic import source; only do this if a required source was not found
		if (source == null && !result.hasMoreElements()) {
			source = findDynamicSource(pkgName);
			if (source != null)
				return compoundEnumerations(result, source.getResources(name));
		}
		if (!result.hasMoreElements())
			try {
				Enumeration<URL> hookResources = searchHooks(name, POST_RESOURCES);
				result = compoundEnumerations(result, hookResources);
			} catch (ClassNotFoundException e) {
				// will not happen
			} catch (FileNotFoundException e) {
				return null;
			}
		if (policy != null) {
			Enumeration<URL> buddyResult = policy.doBuddyResourcesLoading(name);
			result = compoundEnumerations(result, buddyResult);
		}
		// hack to support backwards compatibility for bootdelegation
		// or last resort; do class context trick to work around VM bugs
		if (!result.hasMoreElements()) {
			if (parent != null && !bootDelegation && (container.getConfiguration().compatibilityBootDelegation || isRequestFromVM()))
				// we don't need to continue if the resource is not found here
				return parent.getResources(name);
		}
		return result;
	}

	private boolean isSubPackage(String parentPackage, String subPackage) {
		String prefix = (parentPackage.length() == 0 || parentPackage.equals(DEFAULT_PACKAGE)) ? "" : parentPackage + '.'; //$NON-NLS-1$
		return subPackage.startsWith(prefix);
	}

	@Override
	protected Collection<String> listResources(String path, String filePattern, int options) {
		String pkgName = getResourcePackageName(path.endsWith("/") ? path : path + '/'); //$NON-NLS-1$
		if ((path.length() > 1) && (path.charAt(0) == '/')) /* if name has a leading slash */
			path = path.substring(1); /* remove leading slash before search */
		boolean subPackages = (options & BundleWiring.LISTRESOURCES_RECURSE) != 0;
		List<String> packages = new ArrayList<>();
		// search imported package names
		Map<String, PackageSource> importSources = getImportedSources(null);
		Collection<PackageSource> imports;
		synchronized (importSources) {
			imports = new ArrayList<>(importSources.values());
		}
		for (PackageSource source : imports) {
			String id = source.getId();
			if (id.equals(pkgName) || (subPackages && isSubPackage(pkgName, id)))
				packages.add(id);
		}

		// now add package names from required bundles
		Collection<BundleLoader> visited = new ArrayList<>();
		visited.add(this); // always add ourselves so we do not recurse back to ourselves
		for (ModuleWire bundleWire : requiredBundleWires) {
			BundleLoader loader = getProviderLoader(bundleWire);
			if (loader != null) {
				loader.addProvidedPackageNames(pkgName, packages, subPackages, visited);
			}
		}

		boolean localSearch = (options & BundleWiring.LISTRESOURCES_LOCAL) != 0;
		// Use LinkedHashSet for optimized performance of contains() plus
		// ordering guarantees.
		LinkedHashSet<String> result = new LinkedHashSet<>();
		Set<String> importedPackages = new HashSet<>(0);
		for (String name : packages) {
			// look for import source
			PackageSource externalSource = findImportedSource(name, null);
			if (externalSource != null) {
				// record this package is imported
				importedPackages.add(name);
			} else {
				// look for require bundle source
				externalSource = findRequiredSource(name, null);
			}
			// only add the content of the external source if this is not a localSearch
			if (externalSource != null && !localSearch) {
				String packagePath = name.replace('.', '/');
				Collection<String> externalResources = externalSource.listResources(packagePath, filePattern);
				for (String resource : externalResources) {
					if (!result.contains(resource)) // prevent duplicates; could happen if the package is split or exporter has fragments/multiple jars
						result.add(resource);
				}
			}
		}

		// now search locally
		Collection<String> localResources = getModuleClassLoader().listLocalResources(path, filePattern, options);
		for (String resource : localResources) {
			String resourcePkg = getResourcePackageName(resource);
			if (!importedPackages.contains(resourcePkg) && !result.contains(resource))
				result.add(resource);
		}
		return result;
	}

	@Override
	protected List<URL> findEntries(String path, String filePattern, int options) {
		return getModuleClassLoader().findEntries(path, filePattern, options);
	}

	public static <E> Enumeration<E> compoundEnumerations(Enumeration<E> list1, Enumeration<E> list2) {
		if (list2 == null || !list2.hasMoreElements())
			return list1 == null ? Collections.emptyEnumeration() : list1;
		if (list1 == null || !list1.hasMoreElements())
			return list2 == null ? Collections.emptyEnumeration() : list2;
		List<E> compoundResults = Collections.list(list1);
		while (list2.hasMoreElements()) {
			E item = list2.nextElement();
			if (!compoundResults.contains(item)) //don't add duplicates
				compoundResults.add(item);
		}
		return Collections.enumeration(compoundResults);
	}

	/**
	 * Finds a resource local to this bundle.  Only the classloader for this bundle is searched.
	 * @param name The name of the resource to find.
	 * @return The URL to the resource or null if the resource is not found.
	 */
	public URL findLocalResource(final String name) {
		return getModuleClassLoader().findLocalResource(name);
	}

	/**
	 * Returns an Enumeration of URLs representing all the resources with
	 * the given name. Only the classloader for this bundle is searched.
	 *
	 * @param  name the resource name
	 * @return an Enumeration of URLs for the resources
	 */
	public Enumeration<URL> findLocalResources(String name) {
		return getModuleClassLoader().findLocalResources(name);
	}

	/**
	 * Return a string representation of this loader.
	 * @return String
	 */
	@Override
	public final String toString() {
		ModuleRevision revision = wiring.getRevision();
		String name = revision.getSymbolicName();
		if (name == null)
			name = "unknown"; //$NON-NLS-1$
		return name + '_' + revision.getVersion();
	}

	/**
	 * Return true if the target package name matches
	 * a name in the DynamicImport-Package manifest header.
	 *
	 * @param pkgname The name of the requested class' package.
	 * @return true if the package should be imported.
	 */
	private final boolean isDynamicallyImported(String pkgname) {
		if (this instanceof SystemBundleLoader)
			return false; // system bundle cannot dynamically import
		// must check for startsWith("java.") to satisfy R3 section 4.7.2
		if (pkgname.startsWith("java.")) //$NON-NLS-1$
			return true;

		synchronized (importedSources) {
			/* "*" shortcut */
			if (dynamicAllPackages)
				return true;

			/* match against specific names */
			if (dynamicImportPackages != null)
				for (String dynamicImportPackage : dynamicImportPackages) {
					if (pkgname.equals(dynamicImportPackage)) {
						return true;
					}
				}

			/* match against names with trailing wildcards */
			if (dynamicImportPackageStems != null)
				for (String dynamicImportPackageStem : dynamicImportPackageStems) {
					if (pkgname.startsWith(dynamicImportPackageStem)) {
						return true;
					}
				}
		}
		return false;
	}

	final void addExportedProvidersFor(String packageName, List<PackageSource> result, Collection<BundleLoader> visited) {
		if (visited.contains(this))
			return;
		visited.add(this);
		// See if we locally provide the package.
		PackageSource local = null;
		if (isExportedPackage(packageName))
			local = exportSources.getPackageSource(packageName);
		else if (isSubstitutedExport(packageName)) {
			result.add(findImportedSource(packageName, visited));
			return; // should not continue to required bundles in this case
		}
		// Must search required bundles that are exported first.
		for (ModuleWire bundleWire : requiredBundleWires) {
			if (local != null || BundleNamespace.VISIBILITY_REEXPORT.equals(bundleWire.getRequirement().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE))) {
				// always add required bundles first if we locally provide the package
				// This allows a bundle to provide a package from a required bundle without
				// re-exporting the whole required bundle.
				BundleLoader loader = getProviderLoader(bundleWire);
				if (loader != null) {
					loader.addExportedProvidersFor(packageName, result, visited);
				}
			}
		}
		// now add the locally provided package.
		if (local != null)
			result.add(local);
	}

	private BundleLoader getProviderLoader(ModuleWire wire) {
		ModuleWiring provider = wire.getProviderWiring();
		if (provider == null) {
			if (firstUseOfInvalidLoader.getAndSet(true)) {
				// publish a framework event once per loader, include an exception to show the stack
				String message = "Invalid class loader from a refreshed bundle is being used: " + toString(); //$NON-NLS-1$
				container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, wiring.getBundle(), new IllegalStateException(message));
			}
			return null;
		}
		return (BundleLoader) provider.getModuleLoader();
	}

	final void addProvidedPackageNames(String packageName, List<String> result, boolean subPackages, Collection<BundleLoader> visited) {
		if (visited.contains(this))
			return;
		visited.add(this);
		synchronized (exportedPackages) {
			for (String exported : exportedPackages) {
				if (exported.equals(packageName) || (subPackages && isSubPackage(packageName, exported))) {
					if (!result.contains(exported))
						result.add(exported);
				}
			}
		}
		for (String substituted : wiring.getSubstitutedNames()) {
			if (substituted.equals(packageName) || (subPackages && isSubPackage(packageName, substituted))) {
				if (!result.contains(substituted))
					result.add(substituted);
			}
		}
		for (ModuleWire bundleWire : requiredBundleWires) {
			if (BundleNamespace.VISIBILITY_REEXPORT.equals(bundleWire.getRequirement().getDirectives().get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE))) {
				BundleLoader loader = getProviderLoader(bundleWire);
				if (loader != null) {
					loader.addProvidedPackageNames(packageName, result, subPackages, visited);
				}
			}
		}
	}

	public final boolean isExportedPackage(String name) {
		return exportedPackages.contains(name);
	}

	final boolean isSubstitutedExport(String name) {
		return wiring.isSubstitutedPackage(name);
	}

	private void addDynamicImportPackage(List<ModuleRequirement> packageImports) {
		if (packageImports == null || packageImports.isEmpty()) {
			return;
		}
		List<String> dynamicImports = new ArrayList<>(packageImports.size());
		for (ModuleRequirement packageImport : packageImports) {
			if (PackageNamespace.RESOLUTION_DYNAMIC.equals(packageImport.getDirectives().get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
				Matcher matcher = PACKAGENAME_FILTER.matcher(packageImport.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
				if (matcher.find()) {
					String dynamicName = matcher.group(1);
					if (dynamicName != null) {
						dynamicImports.add(dynamicName);
					}
				}
			}
		}
		if (dynamicImports.size() > 0)
			addDynamicImportPackage(dynamicImports.toArray(new String[dynamicImports.size()]));
	}

	/**
	 * Adds a list of DynamicImport-Package manifest elements to the dynamic
	 * import tables of this BundleLoader.  Duplicate packages are checked and
	 * not added again.  This method is not thread safe.  Callers should ensure
	 * synchronization when calling this method.
	 * @param packages the DynamicImport-Package elements to add.
	 */
	private void addDynamicImportPackage(String[] packages) {
		if (packages == null)
			return;

		synchronized (importedSources) {
			int size = packages.length;
			List<String> stems;
			if (dynamicImportPackageStems == null) {
				stems = new ArrayList<>(size);
			} else {
				stems = new ArrayList<>(size + dynamicImportPackageStems.length);
				for (String dynamicImportPackageStem : dynamicImportPackageStems) {
					stems.add(dynamicImportPackageStem);
				}
			}

			List<String> names;
			if (dynamicImportPackages == null) {
				names = new ArrayList<>(size);
			} else {
				names = new ArrayList<>(size + dynamicImportPackages.length);
				for (String dynamicImportPackage : dynamicImportPackages) {
					names.add(dynamicImportPackage);
				}
			}

			for (int i = 0; i < size; i++) {
				String name = packages[i];
				if (isDynamicallyImported(name))
					continue;
				if (name.equals("*")) { //$NON-NLS-1$
					// shortcut
					dynamicAllPackages = true;
					return;
				}

				if (name.endsWith(".*")) //$NON-NLS-1$
					stems.add(name.substring(0, name.length() - 1));
				else
					names.add(name);
			}

			size = stems.size();
			if (size > 0)
				dynamicImportPackageStems = stems.toArray(new String[size]);

			size = names.size();
			if (size > 0)
				dynamicImportPackages = names.toArray(new String[size]);
		}
	}

	/**
	 * Adds a list of DynamicImport-Package manifest elements to the dynamic
	 * import tables of this BundleLoader.  Duplicate packages are checked and
	 * not added again.
	 * @param packages the DynamicImport-Package elements to add.
	 */
	public final void addDynamicImportPackage(ManifestElement[] packages) {
		if (packages == null)
			return;
		List<String> dynamicImports = new ArrayList<>(packages.length);
		StringBuilder importSpec = new StringBuilder();
		for (ManifestElement dynamicImportElement : packages) {
			String[] names = dynamicImportElement.getValueComponents();
			Collections.addAll(dynamicImports, names);
			if (importSpec.length() > 0) {
				importSpec.append(',');
			}
			importSpec.append(dynamicImportElement.toString());
		}

		if (dynamicImports.size() > 0) {
			Map<String, String> dynamicImportMap = new HashMap<>();
			dynamicImportMap.put(Constants.DYNAMICIMPORT_PACKAGE, importSpec.toString());

			try {
				ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(dynamicImportMap);
				wiring.addDynamicImports(builder);
			} catch (BundleException e) {
				throw new RuntimeException(e);
			}
			// Add the dynamic imports to the loader second to be sure the requirement
			// gets added to the wiring first. This avoids issues if another
			// thread tries to dynamic resolve before all is done here.
			addDynamicImportPackage(dynamicImports.toArray(new String[dynamicImports.size()]));
		}
	}

	/*
	 * Finds a packagesource that is either imported or required from another bundle.
	 * This will not include an local package source
	 */
	private PackageSource findSource(String pkgName) {
		if (pkgName == null)
			return null;
		PackageSource result = findImportedSource(pkgName, null);
		if (result != null)
			return result;
		// Note that dynamic imports are not checked to avoid aggressive wiring (bug 105779)
		return findRequiredSource(pkgName, null);
	}

	private PackageSource findImportedSource(String pkgName, Collection<BundleLoader> visited) {
		Map<String, PackageSource> imports = getImportedSources(visited);
		synchronized (imports) {
			return imports.get(pkgName);
		}
	}

	private Map<String, PackageSource> getImportedSources(Collection<BundleLoader> visited) {
		synchronized (importedSources) {
			if (importsInitialized) {
				return importedSources;
			}
			List<ModuleWire> importWires = wiring.getRequiredModuleWires(PackageNamespace.PACKAGE_NAMESPACE);
			if (importWires != null) {
				for (ModuleWire importWire : importWires) {
					PackageSource source = createExportPackageSource(importWire, visited);
					if (source != null) {
						importedSources.put(source.getId(), source);
					}
				}
			}
			importsInitialized = true;
			return importedSources;
		}
	}

	private PackageSource findDynamicSource(String pkgName) {
		if (!isExportedPackage(pkgName) && isDynamicallyImported(pkgName)) {
			if (debug.DEBUG_LOADER) {
				Debug.println("BundleLoader[" + this + "] attempting to resolve dynamic package: " + pkgName); //$NON-NLS-1$ //$NON-NLS-2$
			}
			ModuleRevision revision = wiring.getRevision();
			ModuleWire dynamicWire = revision.getRevisions().getModule().getContainer().resolveDynamic(pkgName, revision);
			if (dynamicWire != null) {
				PackageSource source = createExportPackageSource(dynamicWire, null);
				if (debug.DEBUG_LOADER) {
					Debug.println("BundleLoader[" + this + "] using dynamic import source: " + source); //$NON-NLS-1$ //$NON-NLS-2$
				}
				synchronized (importedSources) {
					importedSources.put(source.getId(), source);
				}
				return source;
			}
		}
		return null;
	}

	private PackageSource findRequiredSource(String pkgName, Collection<BundleLoader> visited) {
		if (requiredBundleWires.isEmpty()) {
			return null;
		}
		synchronized (requiredSources) {
			PackageSource result = requiredSources.get(pkgName);
			if (result != null)
				return result.isNullSource() ? null : result;
		}
		if (visited == null)
			visited = new ArrayList<>();
		if (!visited.contains(this))
			visited.add(this); // always add ourselves so we do not recurse back to ourselves
		List<PackageSource> result = new ArrayList<>(3);
		for (ModuleWire bundleWire : requiredBundleWires) {
			BundleLoader loader = getProviderLoader(bundleWire);
			if (loader != null) {
				loader.addExportedProvidersFor(pkgName, result, visited);
			}
		}
		// found some so cache the result for next time and return
		PackageSource source;
		if (result.size() == 0) {
			// did not find it in our required bundles lets record the failure
			// so we do not have to do the search again for this package.
			source = NullPackageSource.getNullPackageSource(pkgName);
		} else if (result.size() == 1) {
			// if there is just one source, remember just the single source
			source = result.get(0);
		} else {
			// if there was more than one source, build a multisource and cache that.
			PackageSource[] srcs = result.toArray(new PackageSource[result.size()]);
			source = createMultiSource(pkgName, srcs);
		}
		synchronized (requiredSources) {
			requiredSources.put(source.getId(), source);
		}
		return source.isNullSource() ? null : source;
	}

	/*
	 * Gets the package source for the pkgName.  This will include the local package source
	 * if the bundle exports the package.  This is used to compare the PackageSource of a
	 * package from two different bundles.
	 */
	public final PackageSource getPackageSource(String pkgName) {
		PackageSource result = findSource(pkgName);
		if (!isExportedPackage(pkgName))
			return result;
		// if the package is exported then we need to get the local source
		PackageSource localSource = exportSources.getPackageSource(pkgName);
		if (result == null)
			return localSource;
		if (localSource == null)
			return result;
		return createMultiSource(pkgName, new PackageSource[] {result, localSource});
	}

	static final class ClassContext extends SecurityManager {
		// need to make this method public
		@Override
		public Class<?>[] getClassContext() {
			return super.getClassContext();
		}
	}

	@Override
	protected boolean getAndSetTrigger() {
		return triggerClassLoaded.getAndSet(true);
	}

	@Override
	public boolean isTriggerSet() {
		return triggerClassLoaded.get();
	}
}

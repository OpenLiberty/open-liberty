/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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

package org.eclipse.osgi.internal.loader.classpath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainerAdaptor.ContainerEvent;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.namespaces.EquinoxModuleDataNamespace;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader.DefineClassResult;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.weaving.WeavingHookConfigurator;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.NativeCodeFinder;
import org.eclipse.osgi.storage.Storage;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.HostNamespace;

/**
 * A helper class for {@link ModuleClassLoader} implementations. This class will
 * keep track of {@link ClasspathEntry} objects for the host bundle and any
 * attached fragment bundles. This class takes care of searching the
 * {@link ClasspathEntry} objects for a module class loader implementation.
 * Additional behavior may be added to a classpath manager by configuring a
 * {@link ClassLoaderHook}.
 * 
 * @see ModuleClassLoader
 * @see ClassLoaderHook
 * @since 3.2
 */
public class ClasspathManager {
	private static final FragmentClasspath[] emptyFragments = new FragmentClasspath[0];
	private static final String[] DEFAULT_CLASSPATH = new String[] { "." }; //$NON-NLS-1$

	private final Generation generation;
	private final ModuleClassLoader classloader;
	private final HookRegistry hookRegistry;
	private final Debug debug;

	// TODO Note that PDE has internal dependency on this field type/name (bug
	// 267238)
	private final ClasspathEntry[] entries;
	// TODO Note that PDE has internal dependency on this field type/name (bug
	// 267238)
	private volatile FragmentClasspath[] fragments;
	// a Map<String,String> where "libname" is the key and libpath" is the value
	private ArrayMap<String, String> loadedLibraries = null;
	// used to detect recusive defineClass calls for the same class on the same
	// class loader (bug 345500)
	private ThreadLocal<DefineContext> currentDefineContext = new ThreadLocal<>();

	/**
	 * Constructs a classpath manager for the given generation and module class
	 * loader
	 * 
	 * @param generation  the host generation for this classpath manager
	 * @param classloader the ModuleClassLoader for this classpath manager
	 */
	public ClasspathManager(Generation generation, ModuleClassLoader classloader) {
		EquinoxConfiguration configuration = generation.getBundleInfo().getStorage().getConfiguration();
		this.debug = configuration.getDebug();
		this.hookRegistry = configuration.getHookRegistry();
		this.generation = generation;
		this.classloader = classloader;
		String[] cp = getClassPath(generation.getRevision());
		this.fragments = buildFragmentClasspaths(this.classloader, this);
		this.entries = buildClasspath(cp, this, this.generation);
	}

	private static String[] getClassPath(ModuleRevision revision) {
		List<ModuleCapability> moduleDatas = revision
				.getModuleCapabilities(EquinoxModuleDataNamespace.MODULE_DATA_NAMESPACE);
		@SuppressWarnings("unchecked")
		List<String> cp = moduleDatas.isEmpty() ? null
				: (List<String>) moduleDatas.get(0).getAttributes()
						.get(EquinoxModuleDataNamespace.CAPABILITY_CLASSPATH);
		return cp == null ? DEFAULT_CLASSPATH : cp.toArray(new String[cp.size()]);
	}

	private FragmentClasspath[] buildFragmentClasspaths(ModuleClassLoader hostloader, ClasspathManager manager) {
		if (hostloader == null) {
			return emptyFragments;
		}
		List<ModuleWire> fragmentWires = hostloader.getBundleLoader().getWiring()
				.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE);
		if (fragmentWires == null || fragmentWires.isEmpty()) {
			// we don't hold locks while checking the graph, just return if no longer valid
			return emptyFragments;
		}
		List<FragmentClasspath> result = new ArrayList<>(fragmentWires.size());
		for (ModuleWire fragmentWire : fragmentWires) {
			ModuleRevision revision = fragmentWire.getRequirer();
			Generation fragGeneration = (Generation) revision.getRevisionInfo();

			String[] cp = getClassPath(revision);
			ClasspathEntry[] fragEntries = buildClasspath(cp, manager, fragGeneration);
			FragmentClasspath fragClasspath = new FragmentClasspath(fragGeneration, fragEntries);
			insertFragment(fragClasspath, result);
		}

		return result.toArray(new FragmentClasspath[result.size()]);
	}

	private static void insertFragment(FragmentClasspath fragClasspath, List<FragmentClasspath> existing) {
		// Find a place in the fragment list to insert this fragment.
		long fragID = fragClasspath.getGeneration().getRevision().getRevisions().getModule().getId();

		for (ListIterator<FragmentClasspath> iExisting = existing.listIterator(); iExisting.hasNext();) {
			long otherID = iExisting.next().getGeneration().getRevision().getRevisions().getModule().getId();
			if (fragID < otherID) {
				iExisting.previous();
				iExisting.add(fragClasspath);
				return;
			}
		}
		existing.add(fragClasspath);
	}

	/**
	 * Closes all the classpath entry resources for this classpath manager.
	 */
	public void close() {
		for (ClasspathEntry entry : entries) {
			if (entry != null) {
				try {
					entry.close();
				} catch (IOException e) {
					generation.getBundleInfo().getStorage().getAdaptor().publishContainerEvent(ContainerEvent.ERROR,
							generation.getRevision().getRevisions().getModule(), e);
				}
			}
		}
		FragmentClasspath[] currentFragments = getFragmentClasspaths();
		for (FragmentClasspath currentFragment : currentFragments) {
			currentFragment.close();
		}
	}

	private ClasspathEntry[] buildClasspath(String[] cp, ClasspathManager hostloader, Generation source) {
		ArrayList<ClasspathEntry> result = new ArrayList<>(cp.length);
		// add the regular classpath entries.
		for (String cpEntry : cp) {
			findClassPathEntry(result, cpEntry, hostloader, source);
		}
		return result.toArray(new ClasspathEntry[result.size()]);
	}

	/**
	 * Finds all the ClasspathEntry objects for the requested classpath. This method
	 * will first call all the configured class loading hooks
	 * {@link ClassLoaderHook#addClassPathEntry(ArrayList, String, ClasspathManager, Generation)}
	 * methods. This allows class loading hooks to add additional ClasspathEntry
	 * objects to the result for the requested classpath. Then the local host
	 * classpath entries and attached fragment classpath entries are searched.
	 * 
	 * @param result           a list of ClasspathEntry objects. This list is used
	 *                         to add new ClasspathEntry objects to.
	 * @param cp               the requested classpath.
	 * @param hostloader       the host classpath manager for the classpath
	 * @param sourceGeneration the source generation to search for the classpath
	 */
	private void findClassPathEntry(ArrayList<ClasspathEntry> result, String cp, ClasspathManager hostloader,
			Generation sourceGeneration) {
		List<ClassLoaderHook> loaderHooks = hookRegistry.getClassLoaderHooks();
		boolean hookAdded = false;
		for (ClassLoaderHook hook : loaderHooks) {
			hookAdded |= hook.addClassPathEntry(result, cp, hostloader, sourceGeneration);
		}
		if (!addClassPathEntry(result, cp, hostloader, sourceGeneration) && !hookAdded) {
			BundleException be = new BundleException(NLS.bind(Msg.BUNDLE_CLASSPATH_ENTRY_NOT_FOUND_EXCEPTION, cp,
					sourceGeneration.getRevision().toString()), BundleException.MANIFEST_ERROR);
			sourceGeneration.getBundleInfo().getStorage().getAdaptor().publishContainerEvent(ContainerEvent.INFO,
					sourceGeneration.getRevision().getRevisions().getModule(), be);
		}
	}

	/**
	 * Adds a ClasspathEntry for the requested classpath to the result. The local
	 * host classpath entries are searched first and then attached fragments
	 * classpath entries are searched. The search stops once the first classpath
	 * entry is found.
	 * 
	 * @param result      a list of ClasspathEntry objects. This list is used to add
	 *                    new ClasspathEntry objects to.
	 * @param cp          the requested classpath.
	 * @param hostManager the host classpath manager for the classpath
	 * @param source      the source generation to search for the classpath
	 * @return true if a ClasspathEntry was added to the result
	 */
	public boolean addClassPathEntry(ArrayList<ClasspathEntry> result, String cp, ClasspathManager hostManager,
			Generation source) {
		return addStandardClassPathEntry(result, cp, hostManager, source)
				|| addEclipseClassPathEntry(result, cp, hostManager, source);
	}

	public static boolean addStandardClassPathEntry(ArrayList<ClasspathEntry> result, String cp,
			ClasspathManager hostManager, Generation generation) {
		if (cp.equals(".")) { //$NON-NLS-1$
			result.add(hostManager.createClassPathEntry(generation.getBundleFile(), generation));
			return true;
		}
		ClasspathEntry element = hostManager.getClasspath(cp, generation);
		if (element != null) {
			result.add(element);
			return true;
		}
		// need to check in fragments for the classpath entry.
		// only check for fragments if the generation is the host's generation.
		if (hostManager.generation == generation) {
			FragmentClasspath[] hostFrags = hostManager.getFragmentClasspaths();
			for (FragmentClasspath fragCP : hostFrags) {
				element = hostManager.getClasspath(cp, fragCP.getGeneration());
				if (element != null) {
					result.add(element);
					return true;
				}
			}
		}
		return false;
	}

	private boolean addEclipseClassPathEntry(ArrayList<ClasspathEntry> result, String cp, ClasspathManager hostManager,
			Generation source) {
		String var = hasPrefix(cp);
		if (var != null)
			// find internal library using eclipse predefined vars
			return addInternalClassPath(var, result, cp, hostManager, source);
		if (cp.startsWith(NativeCodeFinder.EXTERNAL_LIB_PREFIX)) {
			cp = cp.substring(NativeCodeFinder.EXTERNAL_LIB_PREFIX.length());
			// find external library using system property substitution
			ClasspathEntry cpEntry = hostManager.getExternalClassPath(
					source.getBundleInfo().getStorage().getConfiguration().substituteVars(cp), source);
			if (cpEntry != null) {
				result.add(cpEntry);
				return true;
			}
		}
		return false;
	}

	private boolean addInternalClassPath(String var, ArrayList<ClasspathEntry> cpEntries, String cp,
			ClasspathManager hostManager, Generation source) {
		EquinoxConfiguration configuration = source.getBundleInfo().getStorage().getConfiguration();
		if (var.equals("ws")) //$NON-NLS-1$
			return ClasspathManager.addStandardClassPathEntry(cpEntries,
					"ws/" + configuration.getWS() + cp.substring(4), hostManager, source); //$NON-NLS-1$
		if (var.equals("os")) //$NON-NLS-1$
			return ClasspathManager.addStandardClassPathEntry(cpEntries,
					"os/" + configuration.getOS() + cp.substring(4), hostManager, source); //$NON-NLS-1$
		if (var.equals("nl")) { //$NON-NLS-1$
			cp = cp.substring(4);
			List<String> NL_JAR_VARIANTS = source.getBundleInfo().getStorage()
					.getConfiguration().ECLIPSE_NL_JAR_VARIANTS;
			for (String nlVariant : NL_JAR_VARIANTS) {
				if (ClasspathManager.addStandardClassPathEntry(cpEntries, "nl/" + nlVariant + cp, hostManager, source)) //$NON-NLS-1$
					return true;
			}
		}
		return false;
	}

	// return a String representing the string found between the $s
	private static String hasPrefix(String libPath) {
		if (libPath.startsWith("$ws$")) //$NON-NLS-1$
			return "ws"; //$NON-NLS-1$
		if (libPath.startsWith("$os$")) //$NON-NLS-1$
			return "os"; //$NON-NLS-1$
		if (libPath.startsWith("$nl$")) //$NON-NLS-1$
			return "nl"; //$NON-NLS-1$
		return null;
	}

	/**
	 * Creates a new ClasspathEntry object for the requested classpath if the source
	 * exists.
	 * 
	 * @param cp           the requested classpath.
	 * @param cpGeneration the source generation to search for the classpath
	 * @return a new ClasspathEntry for the requested classpath or null if the
	 *         source does not exist.
	 */
	public ClasspathEntry getClasspath(String cp, Generation cpGeneration) {
		BundleFile bundlefile = null;
		File file;
		BundleEntry cpEntry = cpGeneration.getBundleFile().getEntry(cp);
		// check for internal library directories in a bundle jar file
		if (cpEntry != null && cpEntry.getName().endsWith("/")) //$NON-NLS-1$
			bundlefile = createBundleFile(cp, cpGeneration);
		// check for internal library jars
		else if ((file = cpGeneration.getBundleFile().getFile(cp, false)) != null)
			bundlefile = createBundleFile(file, cpGeneration);
		if (bundlefile != null)
			return createClassPathEntry(bundlefile, cpGeneration);
		return null;
	}

	/**
	 * Uses the requested classpath as an absolute path to locate a source for a new
	 * ClasspathEntry.
	 * 
	 * @param cp           the requested classpath
	 * @param cpGeneration the source generation to search for the classpath
	 * @return a classpath entry which uses an absolut path as a source
	 */
	public ClasspathEntry getExternalClassPath(String cp, Generation cpGeneration) {
		File file = new File(cp);
		if (!file.isAbsolute())
			return null;
		BundleFile bundlefile = createBundleFile(file, cpGeneration);
		if (bundlefile != null)
			return createClassPathEntry(bundlefile, cpGeneration);
		return null;
	}

	public synchronized void loadFragments(Collection<ModuleRevision> addedFragments) {
		List<FragmentClasspath> result = new ArrayList<>(Arrays.asList(fragments));

		for (ModuleRevision addedFragment : addedFragments) {
			Generation fragGeneration = (Generation) addedFragment.getRevisionInfo();
			String[] cp = getClassPath(addedFragment);
			ClasspathEntry[] fragEntries = buildClasspath(cp, this, fragGeneration);
			FragmentClasspath fragClasspath = new FragmentClasspath(fragGeneration, fragEntries);
			insertFragment(fragClasspath, result);
		}

		fragments = result.toArray(new FragmentClasspath[result.size()]);
	}

	private static BundleFile createBundleFile(File content, Generation generation) {
		if (!content.exists()) {
			return null;
		}
		return generation.getBundleInfo().getStorage().createBundleFile(content, generation, content.isDirectory(),
				false);
	}

	private static BundleFile createBundleFile(String nestedDir, Generation generation) {
		return generation.getBundleInfo().getStorage().createNestedBundleFile(nestedDir, generation.getBundleFile(),
				generation);
	}

	private ClasspathEntry createClassPathEntry(BundleFile bundlefile, Generation source) {
		ClasspathEntry entry;
		if (classloader != null)
			entry = classloader.createClassPathEntry(bundlefile, source);
		else
			entry = new ClasspathEntry(bundlefile, source.getDomain(), source);
		return entry;
	}

	/**
	 * Finds a local resource by searching the ClasspathEntry objects of the
	 * classpath manager. This method will first call all the configured class
	 * loading hooks
	 * {@link ClassLoaderHook#preFindLocalResource(String, ClasspathManager)}
	 * methods. Then it will search for the resource. Finally it will call all the
	 * configured class loading hooks
	 * {@link ClassLoaderHook#postFindLocalResource(String, URL, ClasspathManager)}
	 * methods.
	 * 
	 * @param resource the requested resource name.
	 * @return the requested resource URL or null if the resource does not exist
	 */
	public URL findLocalResource(String resource) {
		List<ClassLoaderHook> hooks = hookRegistry.getClassLoaderHooks();
		boolean hookFailed = false;
		for (ClassLoaderHook hook : hooks) {
			try {
				hook.preFindLocalResource(resource, this);
			} catch (NoSuchElementException e) {
				// mark the resource load as failed, but continue to all the hooks
				hookFailed = true;
			}
		}
		URL result = null;
		try {
			if (!hookFailed) {
				result = findLocalResourceImpl(resource, -1);
			}
		} finally {
			for (ClassLoaderHook hook : hooks) {
				try {
					hook.postFindLocalResource(resource, result, this);
				} catch (NoSuchElementException e) {
					result = null;
				}
			}
		}
		return result;
	}

	private URL findLocalResourceImpl(String resource, int classPathIndex) {
		Module m = generation.getRevision().getRevisions().getModule();
		URL result = null;
		int[] curIndex = { 0 };

		// look in hook specific entries if any
		for (ClassLoaderHook hook : hookRegistry.getClassLoaderHooks()) {
			ClasspathEntry[] hookEntries = hook.getClassPathEntries(resource, this);
			if (hookEntries != null) {
				return findLocalResourceImpl(resource, hookEntries, m, classPathIndex, curIndex);
			}
		}

		curIndex[0] = 0;
		// look in classpath entries
		result = findLocalResourceImpl(resource, entries, m, classPathIndex, curIndex);
		if (result != null) {
			return result;
		}

		// look in fragment entries
		for (FragmentClasspath fragCP : getFragmentClasspaths()) {
			result = findLocalResourceImpl(resource, fragCP.getEntries(), m, classPathIndex, curIndex);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	private URL findLocalResourceImpl(String resource, ClasspathEntry[] cpEntries, Module m, int classPathIndex,
			int[] curIndex) {
		URL result;
		for (ClasspathEntry cpEntry : cpEntries) {
			if (cpEntry != null) {
				result = cpEntry.findResource(resource, m, curIndex[0]);
				if (result != null && (classPathIndex == -1 || classPathIndex == curIndex[0])) {
					return result;
				}
			}
			curIndex[0]++;
		}
		return null;
	}

	/**
	 * Finds the local resources by searching the ClasspathEntry objects of the
	 * classpath manager.
	 * 
	 * @param resource the requested resource name.
	 * @return an enumeration of the the requested resources
	 */
	public Enumeration<URL> findLocalResources(String resource) {
		Module m = generation.getRevision().getRevisions().getModule();
		List<URL> resources = new ArrayList<>(6);
		int[] classPathIndex = { 0 };

		// look in hook specific entries if any
		for (ClassLoaderHook hook : hookRegistry.getClassLoaderHooks()) {
			ClasspathEntry[] hookEntries = hook.getClassPathEntries(resource, this);
			if (hookEntries != null) {
				findLocalResources(resource, hookEntries, m, classPathIndex, resources);
				return resources.size() > 0 ? Collections.enumeration(resources) : Collections.emptyEnumeration();
			}
		}

		classPathIndex[0] = 0;
		// look in host classpath entries
		findLocalResources(resource, entries, m, classPathIndex, resources);
		// look in fragment entries
		for (FragmentClasspath fragCP : getFragmentClasspaths()) {
			findLocalResources(resource, fragCP.getEntries(), m, classPathIndex, resources);
		}

		if (resources.size() > 0)
			return Collections.enumeration(resources);
		return Collections.emptyEnumeration();
	}

	private void findLocalResources(String resource, ClasspathEntry[] cpEntries, Module m, int[] classPathIndex,
			List<URL> resources) {
		for (ClasspathEntry cpEntry : cpEntries) {
			if (cpEntry != null) {
				URL url = cpEntry.findResource(resource, m, classPathIndex[0]);
				if (url != null) {
					resources.add(url);
				}
			}
			classPathIndex[0]++;
		}
	}

	/**
	 * Finds a local entry by searching the ClasspathEntry objects of the classpath
	 * manager.
	 * 
	 * @param path the requested entry path.
	 * @return the requested entry or null if the entry does not exist
	 */
	public BundleEntry findLocalEntry(String path) {
		return findLocalEntry(path, -1);
	}

	/**
	 * Finds a local entry by searching the ClasspathEntry with the specified class
	 * path index.
	 * 
	 * @param path           the requested entry path.
	 * @param classPathIndex the index of the ClasspathEntry to search
	 * @return the requested entry or null if the entry does not exist
	 */
	public BundleEntry findLocalEntry(String path, int classPathIndex) {
		BundleEntry result = null;
		int[] curIndex = { 0 };

		// look in hook specific entries if any
		for (ClassLoaderHook hook : hookRegistry.getClassLoaderHooks()) {
			ClasspathEntry[] hookEntries = hook.getClassPathEntries(path, this);
			if (hookEntries != null) {
				return findLocalEntry(path, hookEntries, classPathIndex, curIndex);
			}
		}

		curIndex[0] = 0;
		// look in classpath entries
		result = findLocalEntry(path, entries, classPathIndex, curIndex);
		if (result != null) {
			return result;
		}

		// look in fragment entries
		for (FragmentClasspath fragCP : getFragmentClasspaths()) {
			result = findLocalEntry(path, fragCP.getEntries(), classPathIndex, curIndex);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	private BundleEntry findLocalEntry(String path, ClasspathEntry[] cpEntries, int classPathIndex, int[] curIndex) {
		for (ClasspathEntry cpEntry : cpEntries) {
			if (cpEntry != null) {
				if (classPathIndex == -1 || classPathIndex == curIndex[0]) {
					BundleEntry result = cpEntry.findEntry(path);
					if (result != null) {
						return result;
					}
				}
			}
			curIndex[0]++;
		}
		return null;
	}

	/**
	 * Finds a local class by searching the ClasspathEntry objects of the classpath
	 * manager. This method will first call all the configured class loader hooks
	 * {@link ClassLoaderHook#preFindLocalClass(String, ClasspathManager)} methods.
	 * Then it will search for the class. If a class is found then
	 * <ol>
	 * <li>All configured class loader hooks
	 * {@link ClassLoaderHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 * methods will be called.</li>
	 * <li>The class is then defined.</li>
	 * <li>Finally, all configured class loading stats hooks
	 * {@link ClassLoaderHook#recordClassDefine(String, Class, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 * methods are called.</li>
	 * </ol>
	 * Finally all the configured class loading hooks
	 * {@link ClassLoaderHook#postFindLocalClass(String, Class, ClasspathManager)}
	 * methods are called.
	 * 
	 * @param classname the requested class name.
	 * @return the requested class or null if the class does not exist
	 * @throws ClassNotFoundException if a ClassLoaderHook prevents the requested
	 *                                class from loading
	 */
	public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
		Class<?> result = null;
		List<ClassLoaderHook> hooks = hookRegistry.getClassLoaderHooks();
		try {
			for (ClassLoaderHook hook : hooks) {
				hook.preFindLocalClass(classname, this);
			}
			result = classloader.publicFindLoaded(classname);
			if (result != null)
				return result;
			result = findLocalClassImpl(classname, hooks);
			return result;
		} finally {
			for (ClassLoaderHook hook : hooks) {
				hook.postFindLocalClass(classname, result, this);
			}
		}
	}

	private Class<?> findLocalClassImpl(String classname, List<ClassLoaderHook> hooks) {
		Class<?> result;

		// look in hook specific entries if any
		for (ClassLoaderHook hook : hookRegistry.getClassLoaderHooks()) {
			ClasspathEntry[] hookEntries = hook.getClassPathEntries(classname, this);
			if (hookEntries != null) {
				return findLocalClassImpl(classname, hookEntries, hooks);
			}
		}

		// look in classpath entries
		result = findLocalClassImpl(classname, entries, hooks);
		if (result != null) {
			return result;
		}

		// look in fragment entries
		for (FragmentClasspath fragCP : getFragmentClasspaths()) {
			result = findLocalClassImpl(classname, fragCP.getEntries(), hooks);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	private Class<?> findLocalClassImpl(String classname, ClasspathEntry[] cpEntries, List<ClassLoaderHook> hooks) {
		Class<?> result;
		for (ClasspathEntry cpEntry : cpEntries) {
			if (cpEntry != null) {
				result = findClassImpl(classname, cpEntry, hooks);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private Class<?> findClassImpl(String name, ClasspathEntry classpathEntry, List<ClassLoaderHook> hooks) {
		String loaderTrace = debug.loaderWithClass(name);
		if (loaderTrace != null) {
			debug.trace(loaderTrace,
					"ModuleClassLoader[" + classloader.getBundleLoader() + " - " + classpathEntry.getBundleFile() //$NON-NLS-1$ //$NON-NLS-2$
					+ "].findClassImpl(" + name + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String filename = name.replace('.', '/').concat(".class"); //$NON-NLS-1$

		BundleEntry entry = classpathEntry.findEntry(filename);
		if (entry == null)
			return null;

		byte[] classbytes;
		try {
			classbytes = entry.getBytes();
		} catch (IOException e) {
			if (loaderTrace != null)
				debug.trace(loaderTrace,
						"  IOException reading " + filename + " from " + classpathEntry.getBundleFile()); //$NON-NLS-1$ //$NON-NLS-2$
			throw (LinkageError) new LinkageError("Error reading class bytes: " + name).initCause(e); //$NON-NLS-1$
		}
		if (loaderTrace != null) {
			debug.trace(loaderTrace,
					"  read " + classbytes.length + " bytes from " + classpathEntry.getBundleFile() + "!/" + filename); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			debug.trace(loaderTrace, "  defining class " + name); //$NON-NLS-1$
		}

		try {
			return defineClass(name, classbytes, classpathEntry, entry, hooks);
		} catch (Error e) {
			if (loaderTrace != null) {
				debug.trace(loaderTrace, "  error defining class " + name); //$NON-NLS-1$
			}
			throw e;
		}
	}

	static class DefineContext {
		Collection<String> currentlyProcessing = new ArrayList<>(5);
		Collection<String> currentlyDefining = new ArrayList<>(5);
	}

	/**
	 * Defines the specified class. This method will first call all the configured
	 * class loader hooks
	 * {@link ClassLoadingHook#processClass(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 * methods. If any hook modifies the bytes the all configured hook
	 * {@link ClassLoaderHook#rejectTransformation(String, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}
	 * methods are called. Then it will call the
	 * {@link ModuleClassLoader#defineClass(String, byte[], ClasspathEntry, BundleEntry)}
	 * method to define the class. After that, the class loader hooks are called to
	 * announce the class definition by calling
	 * {@link ClassLoaderHook#recordClassDefine(String, Class, byte[], ClasspathEntry, BundleEntry, ClasspathManager)}.
	 * 
	 * @param name           the name of the class to define
	 * @param classbytes     the class bytes
	 * @param classpathEntry the classpath entry used to load the class bytes
	 * @param entry          the BundleEntry used to load the class bytes
	 * @param hooks          the class loader hooks
	 * @return the defined class
	 */
	private Class<?> defineClass(String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
			List<ClassLoaderHook> hooks) {
		DefineClassResult result = null;
		boolean recursionDetected = false;
		try {
			definePackage(name, classpathEntry);
			DefineContext context = currentDefineContext.get();
			if (context == null) {
				context = new DefineContext();
				currentDefineContext.set(context);
			}

			// First call the hooks that do not handle recursion themselves
			if (!hookRegistry.getContainer().isProcessClassRecursionSupportedByAll()) {
				// One or more hooks do not support recursive class processing.
				// We need to detect recursions for this set of hooks.
				if (context.currentlyProcessing.contains(name)) {
					// Avoid recursion for the same class name for these hooks
					recursionDetected = true;
					// TODO consider thrown a ClassCircularityError here
					return null;
				}
				context.currentlyProcessing.add(name);
				try {

					for (ClassLoaderHook hook : hooks) {
						if (!hook.isProcessClassRecursionSupported()) {
							classbytes = processClass(hook, name, classbytes, classpathEntry, entry, this, hooks);
						}
					}
				} finally {
					context.currentlyProcessing.remove(name);
				}
			}

			// Now call the hooks that do support recursion without the check.
			for (ClassLoaderHook hook : hooks) {
				if (hook.isProcessClassRecursionSupported()) {
					// Note if the hooks don't take protective measures for a recursive class load
					// here
					// it will result in a stack overflow.
					classbytes = processClass(hook, name, classbytes, classpathEntry, entry, this, hooks);
				}
			}

			if (context.currentlyDefining.contains(name)) {
				// TODO consider thrown a ClassCircularityError here
				return null; // avoid recursive defines (bug 345500)
			}
			context.currentlyDefining.add(name);
			try {
				result = classloader.defineClass(name, classbytes, classpathEntry);
			} finally {
				context.currentlyDefining.remove(name);
			}
		} finally {
			// only call hooks if we properly called processClass above
			if (!recursionDetected) {
				// only pass the newly defined class to the hook
				Class<?> defined = result != null && result.defined ? result.clazz : null;
				for (ClassLoaderHook hook : hooks) {
					hook.recordClassDefine(name, defined, classbytes, classpathEntry, entry, this);
				}
			}
		}
		// return either the pre-loaded class or the newly defined class
		return result == null ? null : result.clazz;
	}

	private byte[] processClass(ClassLoaderHook hook, String name, byte[] classbytes, ClasspathEntry classpathEntry,
			BundleEntry entry, ClasspathManager classpathManager, List<ClassLoaderHook> hooks) {
		byte[] modifiedBytes = hook.processClass(name, classbytes, classpathEntry, entry, this);
		if (modifiedBytes != null) {
			// the WeavingHookConfigurator already calls the rejectTransformation method;
			// avoid calling it again.
			if (!(hook instanceof WeavingHookConfigurator)) {
				for (ClassLoaderHook rejectHook : hooks) {
					if (rejectHook.rejectTransformation(name, modifiedBytes, classpathEntry, entry, this)) {
						modifiedBytes = null;
						break;
					}
				}
			}
			if (modifiedBytes != null) {
				classbytes = modifiedBytes;
			}
		}
		return classbytes;
	}

	private void definePackage(String name, ClasspathEntry classpathEntry) {
		// Define the package if it is not the default package.
		int lastIndex = name.lastIndexOf('.');
		if (lastIndex < 0) {
			return;
		}
		String packageName = name.substring(0, lastIndex);
		Object pkg = classloader.publicGetPackage(packageName);
		if (pkg != null) {
			return;
		}

		// get info about the package from the classpath entry's manifest.
		String specTitle = null, specVersion = null, specVendor = null, implTitle = null, implVersion = null,
				implVendor = null;

		if (generation.getBundleInfo().getStorage().getConfiguration().DEFINE_PACKAGE_ATTRIBUTES) {
			ManifestPackageAttributes manifestPackageAttributes = classpathEntry
					.manifestPackageAttributesFor(packageName);
			TitleVersionVendor specification = manifestPackageAttributes.getSpecification();
			TitleVersionVendor implementation = manifestPackageAttributes.getImplementation();
			specTitle = specification.getTitle();
			specVersion = specification.getVersion();
			specVendor = specification.getVendor();
			implTitle = implementation.getTitle();
			implVersion = implementation.getVersion();
			implVendor = implementation.getVendor();
		}

		// The package is not defined yet define it before we define the class.
		// TODO still need to seal packages.
		classloader.publicDefinePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion,
				implVendor, null);
	}

	/**
	 * Returns the fragment classpaths of this classpath manager
	 * 
	 * @return the fragment classpaths of this classpath manager
	 */
	public FragmentClasspath[] getFragmentClasspaths() {
		return fragments;
	}

	/**
	 * Returns the host classpath entries for this classpath manager
	 * 
	 * @return the host classpath entries for this classpath manager
	 */
	public ClasspathEntry[] getHostClasspathEntries() {
		return entries;
	}

	/**
	 * Finds a library for the bundle represented by this class path manager
	 * 
	 * @param libname the library name
	 * @return The absolution path to the library or null if not found
	 */
	public String findLibrary(String libname) {
		synchronized (this) {
			if (loadedLibraries == null)
				loadedLibraries = new ArrayMap<>(1);
		}
		synchronized (loadedLibraries) {
			// we assume that each classloader will load a small number of of libraries
			// instead of wasting space with a map we iterate over our collection of found
			// libraries
			// each element is a String[2], each array is {"libname", "libpath"}
			String libpath = loadedLibraries.get(libname);
			if (libpath != null)
				return libpath;

			libpath = findLibrary0(libname);
			if (libpath != null)
				loadedLibraries.put(libname, libpath);
			return libpath;
		}
	}

	private String findLibrary0(String libname) {
		List<ClassLoaderHook> hooks = hookRegistry.getClassLoaderHooks();
		String result = null;
		for (ClassLoaderHook hook : hooks) {
			try {
				result = hook.preFindLibrary(libname, classloader);
				if (result != null) {
					return result;
				}
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		result = generation.findLibrary(libname);
		if (result != null) {
			return result;
		}

		// look in fragment generations
		FragmentClasspath[] currentFragments = getFragmentClasspaths();
		for (FragmentClasspath fragment : currentFragments) {
			result = fragment.getGeneration().findLibrary(libname);
			if (result != null) {
				return result;
			}
		}

		for (ClassLoaderHook hook : hooks) {
			result = hook.postFindLibrary(libname, classloader);
			if (result != null) {
				return result;
			}
		}
		return result;
	}

	/**
	 * @see ModuleClassLoader#findEntries(String, String, int)
	 */
	public List<URL> findEntries(String path, String filePattern, int options) {
		List<Generation> generations = new ArrayList<>();
		// first get the host bundle file
		generations.add(generation);
		// next get the attached fragments bundle files
		FragmentClasspath[] currentFragments = getFragmentClasspaths();
		for (FragmentClasspath fragmentClasspath : currentFragments)
			generations.add(fragmentClasspath.getGeneration());

		// now search over all the bundle files
		Enumeration<URL> eURLs = Storage.findEntries(generations, path, filePattern, options);
		if (eURLs == null)
			return Collections.emptyList();
		return Collections.unmodifiableList(Collections.list(eURLs));
	}

	/**
	 * @see ModuleClassLoader#listLocalResources(String, String, int)
	 */
	public Collection<String> listLocalResources(String path, String filePattern, int options) {
		List<BundleFile> bundleFiles = new ArrayList<>();

		// look for path first in hook specific entries if any
		for (ClassLoaderHook hook : hookRegistry.getClassLoaderHooks()) {
			ClasspathEntry[] hookEntries = hook.getClassPathEntries(path, this);
			if (hookEntries != null) {
				for (ClasspathEntry cpEntry : hookEntries) {
					cpEntry.addBundleFiles(bundleFiles);
				}
				return Storage.listEntryPaths(bundleFiles, path, filePattern, options);
			}
		}

		ClasspathEntry[] cpEntries = getHostClasspathEntries();
		for (ClasspathEntry cpEntry : cpEntries) {
			cpEntry.addBundleFiles(bundleFiles);
		}

		for (FragmentClasspath fragmentClasspath : getFragmentClasspaths()) {
			for (ClasspathEntry cpEntry : fragmentClasspath.getEntries()) {
				cpEntry.addBundleFiles(bundleFiles);
			}
		}

		return Storage.listEntryPaths(bundleFiles, path, filePattern, options);
	}

	public Generation getGeneration() {
		return generation;
	}

	public ModuleClassLoader getClassLoader() {
		return classloader;
	}
}

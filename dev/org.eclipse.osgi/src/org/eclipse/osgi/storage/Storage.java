/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
 *     Christoph Laeubrich - Bug 527175 - Storage#getSystemContent() should first make the file absolute
 *     Hannes Wellmann - Bug 577432 - Speed up and improve file processing in Storage
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.framework.internal.reliablefile.ReliableFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.osgi.framework.util.ObjectPool;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.framework.EquinoxContainerAdaptor;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory;
import org.eclipse.osgi.internal.hookregistry.StorageHookFactory.StorageHook;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.internal.url.URLStreamHandlerFactoryImpl;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.ContentProvider.Type;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapperChain;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.storage.bundlefile.MRUBundleFileList;
import org.eclipse.osgi.storage.bundlefile.NestedDirBundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;
import org.eclipse.osgi.storage.url.reference.Handler;
import org.eclipse.osgi.storagemanager.ManagedOutputStream;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

public class Storage {
	public static class StorageException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public StorageException() {
			super();
		}

		public StorageException(String message, Throwable cause) {
			super(message, cause);
		}

		public StorageException(String message) {
			super(message);
		}

		public StorageException(Throwable cause) {
			super(cause);
		}

	}

	public static final int VERSION = 6;
	private static final int CONTENT_TYPE_VERSION = 6;
	private static final int CACHED_SYSTEM_CAPS_VERION = 5;
	private static final int MR_JAR_VERSION = 4;
	private static final int LOWEST_VERSION_SUPPORTED = 3;
	public static final String BUNDLE_DATA_DIR = "data"; //$NON-NLS-1$
	public static final String BUNDLE_FILE_NAME = "bundleFile"; //$NON-NLS-1$
	public static final String FRAMEWORK_INFO = "framework.info"; //$NON-NLS-1$
	public static final String ECLIPSE_SYSTEMBUNDLE = "Eclipse-SystemBundle"; //$NON-NLS-1$
	public static final String DELETE_FLAG = ".delete"; //$NON-NLS-1$
	public static final String LIB_TEMP = "libtemp"; //$NON-NLS-1$

	private static final String JAVASE = "JavaSE"; //$NON-NLS-1$
	private static final String PROFILE_EXT = ".profile"; //$NON-NLS-1$
	private static final String NUL = new String(new byte[] {0});
	private static final String INITIAL_LOCATION = "initial@"; //$NON-NLS-1$

	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	private final EquinoxContainer equinoxContainer;
	private final String installPath;
	private final Location osgiLocation;
	private final File childRoot;
	private final File parentRoot;
	private final PermissionData permissionData;
	private final SecurityAdmin securityAdmin;
	private final EquinoxContainerAdaptor adaptor;
	private final ModuleDatabase moduleDatabase;
	private final ModuleContainer moduleContainer;
	private final Object saveMonitor = new Object();
	private long lastSavedTimestamp = -1;
	private final MRUBundleFileList mruList;
	private final FrameworkExtensionInstaller extensionInstaller;
	private final List<String> cachedHeaderKeys = Arrays.asList(Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_ACTIVATIONPOLICY, "Service-Component"); //$NON-NLS-1$
	private final boolean allowRestrictedProvides;
	private final AtomicBoolean refreshMRBundles = new AtomicBoolean(false);
	private final Version runtimeVersion;
	private final String javaSpecVersion;

	public static Storage createStorage(EquinoxContainer container) throws IOException, BundleException {
		String[] cachedInfo = new String[3];
		Storage storage = new Storage(container, cachedInfo);
		// Do some operations that need to happen on the fully constructed Storage before returning it
		storage.checkSystemBundle(cachedInfo);
		storage.refreshStaleBundles();
		storage.installExtensions();
		// TODO hack to make sure all bundles are in UNINSTALLED state before system
		// bundle init is called
		storage.getModuleContainer().setInitialModuleStates();
		return storage;
	}

	private Storage(EquinoxContainer container, String[] cachedInfo) throws IOException {
		// default to Java 8 since that is our min
		Version defaultVersion = Version.valueOf("1.8"); //$NON-NLS-1$
		Version javaVersion = defaultVersion;
		// set the profile and EE based off of the java.specification.version
		String javaSpecVersionProp = System.getProperty(EquinoxConfiguration.PROP_JVM_SPEC_VERSION);
		StringTokenizer st = new StringTokenizer(javaSpecVersionProp, " _-"); //$NON-NLS-1$
		javaSpecVersionProp = st.nextToken();
		try {
			String[] vComps = javaSpecVersionProp.split("\\."); //$NON-NLS-1$
			// only pay attention to the first three components of the version
			int major = vComps.length > 0 ? Integer.parseInt(vComps[0]) : 0;
			int minor = vComps.length > 1 ? Integer.parseInt(vComps[1]) : 0;
			int micro = vComps.length > 2 ? Integer.parseInt(vComps[2]) : 0;
			javaVersion = new Version(major, minor, micro);
		} catch (IllegalArgumentException e) {
			// do nothing
		}
		if (javaVersion.compareTo(defaultVersion) < 0) {
			// the Java specification property is wrong, we are compiled to the
			// defaultVersion
			// just use it instead.
			javaVersion = defaultVersion;
		}
		runtimeVersion = javaVersion;
		javaSpecVersion = javaSpecVersionProp;
		mruList = new MRUBundleFileList(getBundleFileLimit(container.getConfiguration()), container.getConfiguration().getDebug());
		equinoxContainer = container;
		extensionInstaller = new FrameworkExtensionInstaller(container.getConfiguration());
		allowRestrictedProvides = Boolean.parseBoolean(container.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_ALLOW_RESTRICTED_PROVIDES));

		// we need to set the install path as soon as possible so we can determine
		// the absolute location of install relative URLs
		Location installLoc = container.getLocations().getInstallLocation();
		URL installURL = installLoc.getURL();
		// assume install URL is file: based
		installPath = installURL.getPath();

		Location configLocation = container.getLocations().getConfigurationLocation();
		Location parentConfigLocation = configLocation.getParentLocation();
		Location osgiParentLocation = null;
		if (parentConfigLocation != null) {
			osgiParentLocation = parentConfigLocation.createLocation(null, parentConfigLocation.getDataArea(EquinoxContainer.NAME), true);
		}
		this.osgiLocation = configLocation.createLocation(osgiParentLocation, configLocation.getDataArea(EquinoxContainer.NAME), configLocation.isReadOnly());
		this.childRoot = new File(osgiLocation.getURL().getPath());

		if (Boolean.valueOf(container.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_CLEAN)).booleanValue()) {
			cleanOSGiStorage(osgiLocation, childRoot);
		}
		if (!this.osgiLocation.isReadOnly()) {
			this.childRoot.mkdirs();
		}
		Location parent = this.osgiLocation.getParentLocation();
		parentRoot = parent == null ? null : new File(parent.getURL().getPath());

		if (container.getConfiguration().getConfiguration(Constants.FRAMEWORK_STORAGE) == null) {
			// Set the derived value if not already set as part of configuration.
			// Note this is the parent directory of where the framework stores data (org.eclipse.osgi/)
			container.getConfiguration().setConfiguration(Constants.FRAMEWORK_STORAGE, childRoot.getParentFile().getAbsolutePath());
		}

		InputStream info = getInfoInputStream();
		DataInputStream data = info == null ? null : new DataInputStream(new BufferedInputStream(info));
		try {
			Map<Long, Generation> generations;
			try {
				generations = loadGenerations(data, cachedInfo);
			} catch (IllegalArgumentException e) {
				equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, "The persistent format for the framework data has changed.  The framework will be reinitialized: " + e.getMessage(), null); //$NON-NLS-1$
				generations = new HashMap<>(0);
				data = null;
				cleanOSGiStorage(osgiLocation, childRoot);
			}
			this.permissionData = loadPermissionData(data);
			this.securityAdmin = new SecurityAdmin(null, this.permissionData);
			this.adaptor = new EquinoxContainerAdaptor(equinoxContainer, this, generations);
			this.moduleDatabase = new ModuleDatabase(this.adaptor);
			this.moduleContainer = new ModuleContainer(this.adaptor, this.moduleDatabase);
			if (data != null) {
				try {
					moduleDatabase.load(data);
					lastSavedTimestamp = moduleDatabase.getTimestamp();
				} catch (IllegalArgumentException e) {
					equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, "Incompatible version.  Starting with empty framework.", e); //$NON-NLS-1$
					// Clean up the cache.
					// No need to clean up the database. Nothing got loaded.
					cleanOSGiStorage(osgiLocation, childRoot);
					// should free up the generations map
					generations.clear();
				}
			}
		} finally {
			if (data != null) {
				try {
					data.close();
				} catch (IOException e) {
					// just move on
				}
			}
		}
	}

	public Version getRuntimeVersion() {
		return runtimeVersion;
	}

	public MRUBundleFileList getMRUBundleFileList() {
		return mruList;
	}

	private int getBundleFileLimit(EquinoxConfiguration configuration) {
		int propValue = 100; // enable to 100 open files by default
		try {
			String prop = configuration.getConfiguration(EquinoxConfiguration.PROP_FILE_LIMIT);
			if (prop != null)
				propValue = Integer.parseInt(prop);
		} catch (NumberFormatException e) {
			// use default of 100
		}
		return propValue;
	}

	private void installExtensions() {
		Module systemModule = moduleContainer.getModule(0);
		ModuleRevision systemRevision = systemModule == null ? null : systemModule.getCurrentRevision();
		ModuleWiring systemWiring = systemRevision == null ? null : systemRevision.getWiring();
		if (systemWiring == null) {
			return;
		}
		Collection<ModuleRevision> fragments = new ArrayList<>();
		for (ModuleWire hostWire : systemWiring.getProvidedModuleWires(HostNamespace.HOST_NAMESPACE)) {
			fragments.add(hostWire.getRequirer());
		}
		try {
			getExtensionInstaller().addExtensionContent(fragments, null);
		} catch (BundleException e) {
			getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, e.getMessage(), e);
		}
	}

	private static PermissionData loadPermissionData(DataInputStream in) throws IOException {
		PermissionData permData = new PermissionData();
		if (in != null) {
			permData.readPermissionData(in);
		}
		return permData;
	}

	private void refreshStaleBundles() throws BundleException {
		Collection<Module> needsRefresh = new ArrayList<>(0);

		// First uninstall any modules that had their content changed or deleted
		for (Module module : moduleContainer.getModules()) {
			if (module.getId() == Constants.SYSTEM_BUNDLE_ID)
				continue;
			ModuleRevision revision = module.getCurrentRevision();
			Generation generation = (Generation) revision.getRevisionInfo();
			if (needsDiscarding(generation)) {
				needsRefresh.add(module);
				moduleContainer.uninstall(module);
				generation.delete();
			}
		}
		// Next check if we need to refresh Multi-Release Jar bundles
		// because the runtime version changed.
		if (refreshMRBundles.get()) {
			needsRefresh.addAll(refreshMRJarBundles());
		}

		// refresh the modules that got deleted or are Multi-Release bundles
		if (!needsRefresh.isEmpty()) {
			moduleContainer.refresh(needsRefresh);
		}
	}

	private boolean needsDiscarding(Generation generation) {
		for (StorageHook<?, ?> hook : generation.getStorageHooks()) {
			try {
				hook.validate();
			} catch (IllegalStateException e) {
				equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.WARNING, "Error validating installed bundle.", e); //$NON-NLS-1$
				return true;
			}
		}
		File content = generation.getContent();
		if (content == null) {
			return false;
		}
		if (getConfiguration().inCheckConfigurationMode()) {
			if (generation.isDirectory()) {
				content = new File(content, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
			}
			return generation.getLastModified() != secureAction.lastModified(content);
		}
		if (!content.exists()) {
			// the content got deleted since last time!
			return true;
		}
		return false;
	}

	private void checkSystemBundle(String[] cachedInfo) {
		Module systemModule = moduleContainer.getModule(0);
		Generation newGeneration = null;
		try {
			if (systemModule == null) {
				BundleInfo info = new BundleInfo(this, 0, Constants.SYSTEM_BUNDLE_LOCATION, 0);
				newGeneration = info.createGeneration();

				File contentFile = getSystemContent();
				newGeneration.setContent(contentFile, Type.DEFAULT);

				// First we must make sure the VM profile has been loaded
				loadVMProfile(newGeneration);
				// dealing with system bundle find the extra capabilities and exports
				String extraCapabilities = getSystemExtraCapabilities();
				String extraExports = getSystemExtraPackages();

				ModuleRevisionBuilder builder = getBuilder(newGeneration, extraCapabilities, extraExports);
				systemModule = moduleContainer.install(null, Constants.SYSTEM_BUNDLE_LOCATION, builder, newGeneration);
				moduleContainer.resolve(Collections.singletonList(systemModule), false);
			} else {
				ModuleRevision currentRevision = systemModule.getCurrentRevision();
				Generation currentGeneration = currentRevision == null ? null : (Generation) currentRevision.getRevisionInfo();
				if (currentGeneration == null) {
					throw new IllegalStateException("No current revision for system bundle."); //$NON-NLS-1$
				}
				try {
					// First we must make sure the VM profile has been loaded
					loadVMProfile(currentGeneration);
					// dealing with system bundle find the extra capabilities and exports
					String extraCapabilities = getSystemExtraCapabilities();
					String extraExports = getSystemExtraPackages();
					File contentFile = currentGeneration.getContent();
					if (systemNeedsUpdate(contentFile, currentRevision, currentGeneration, extraCapabilities, extraExports, cachedInfo)) {
						newGeneration = currentGeneration.getBundleInfo().createGeneration();
						newGeneration.setContent(contentFile, Type.DEFAULT);
						ModuleRevisionBuilder newBuilder = getBuilder(newGeneration, extraCapabilities, extraExports);
						moduleContainer.update(systemModule, newBuilder, newGeneration);
						moduleContainer.refresh(Collections.singleton(systemModule));
					} else {
						if (currentRevision.getWiring() == null) {
							// must resolve before continuing to ensure extensions get attached
							moduleContainer.resolve(Collections.singleton(systemModule), true);
						}
					}
				} catch (BundleException e) {
					throw new IllegalStateException("Could not create a builder for the system bundle.", e); //$NON-NLS-1$
				}
			}
			ModuleRevision currentRevision = systemModule.getCurrentRevision();
			List<ModuleCapability> nativeEnvironments = currentRevision.getModuleCapabilities(NativeNamespace.NATIVE_NAMESPACE);
			Map<String, Object> configMap = equinoxContainer.getConfiguration().getInitialConfig();
			for (ModuleCapability nativeEnvironment : nativeEnvironments) {
				nativeEnvironment.setTransientAttrs(configMap);
			}
			Version frameworkVersion = null;
			if (newGeneration != null) {
				frameworkVersion = findFrameworkVersion();
			} else {
				String sVersion = cachedInfo[0];
				frameworkVersion = sVersion == null ? findFrameworkVersion() : Version.parseVersion(sVersion);
			}
			if (frameworkVersion != null) {
				this.equinoxContainer.getConfiguration().setConfiguration(Constants.FRAMEWORK_VERSION, frameworkVersion.toString());
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException("Error occurred while checking the system module.", e); //$NON-NLS-1$
		} finally {
			if (newGeneration != null) {
				newGeneration.getBundleInfo().unlockGeneration(newGeneration);
			}
		}
	}

	private Version findFrameworkVersion() {
		Requirement osgiPackageReq = ModuleContainer.createRequirement(PackageNamespace.PACKAGE_NAMESPACE, Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework)"), Collections.emptyMap()); //$NON-NLS-1$ //$NON-NLS-2$
		Collection<BundleCapability> osgiPackages = moduleContainer.getFrameworkWiring().findProviders(osgiPackageReq);
		for (BundleCapability packageCapability : osgiPackages) {
			if (packageCapability.getRevision().getBundle().getBundleId() == 0) {
				Version v = (Version) packageCapability.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				if (v != null) {
					return v;
				}
			}
		}
		return null;
	}

	private Collection<Module> refreshMRJarBundles() throws BundleException {
		Collection<Module> mrJarBundles = new ArrayList<>();
		for (Module m : moduleContainer.getModules()) {
			Generation generation = (Generation) m.getCurrentRevision().getRevisionInfo();
			// Note that we check the raw headers here incase we are working off an old version of the persistent storage
			if (Boolean.parseBoolean(generation.getRawHeaders().get(BundleInfo.MULTI_RELEASE_HEADER))) {
				refresh(m);
				mrJarBundles.add(m);
			}
		}
		return mrJarBundles;
	}

	public void close() {
		try {
			save();
		} catch (IOException e) {
			getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Error saving on shutdown", e); //$NON-NLS-1$
		}

		// close all the generations
		List<Module> modules = moduleContainer.getModules();
		for (Module module : modules) {
			for (ModuleRevision revision : module.getRevisions().getModuleRevisions()) {
				Generation generation = (Generation) revision.getRevisionInfo();
				if (generation != null) {
					generation.close();
				}
			}
		}
		for (ModuleRevision removalPending : moduleContainer.getRemovalPending()) {
			Generation generation = (Generation) removalPending.getRevisionInfo();
			if (generation != null) {
				generation.close();
			}
		}
		mruList.shutdown();
		adaptor.shutdownExecutors();
	}

	private boolean systemNeedsUpdate(File systemContent, ModuleRevision currentRevision, Generation existing, String extraCapabilities, String extraExports, String[] cachedInfo) throws BundleException {
		if (!extraCapabilities.equals(cachedInfo[1])) {
			return true;
		}
		if (!extraExports.equals(cachedInfo[2])) {
			return true;
		}
		if (systemContent == null) {
			// only do a version check in this case
			ModuleRevisionBuilder newBuilder = getBuilder(existing, extraCapabilities, extraExports);
			return !currentRevision.getVersion().equals(newBuilder.getVersion());
		}
		if (existing.isDirectory()) {
			systemContent = new File(systemContent, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		}
		return existing.getLastModified() != secureAction.lastModified(systemContent);

	}

	private void cleanOSGiStorage(Location location, File root) {
		if (location.isReadOnly() || !StorageUtil.rm(root, getConfiguration().getDebug().DEBUG_STORAGE)) {
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "The -clean (osgi.clean) option was not successful. Unable to clean the storage area: " + root.getAbsolutePath(), null); //$NON-NLS-1$
		}
		if (!location.isReadOnly()) {
			// make sure to recreate to root folder
			root.mkdirs();
		}
	}

	public ModuleDatabase getModuleDatabase() {
		return moduleDatabase;
	}

	public ModuleContainerAdaptor getAdaptor() {
		return adaptor;
	}

	public ModuleContainer getModuleContainer() {
		return moduleContainer;
	}

	public EquinoxConfiguration getConfiguration() {
		return equinoxContainer.getConfiguration();
	}

	public EquinoxLogServices getLogServices() {
		return equinoxContainer.getLogServices();
	}

	public FrameworkExtensionInstaller getExtensionInstaller() {
		return extensionInstaller;
	}

	public boolean isReadOnly() {
		return osgiLocation.isReadOnly();
	}

	public URLConnection getContentConnection(Module module, String bundleLocation, final InputStream in)
			throws BundleException {
		try {
			List<StorageHookFactory<?, ?, ?>> storageHooks = getConfiguration().getHookRegistry()
					.getStorageHookFactories();
			for (StorageHookFactory<?, ?, ?> storageHook : storageHooks) {
				URLConnection hookContent = storageHook.handleContentConnection(module, bundleLocation, in);
				if (hookContent != null) {
					return hookContent;
				}
			}

			if (in != null) {
				return new URLConnection(null) {
					/**
					 * @throws IOException
					 */
					@Override
					public void connect() throws IOException {
						connected = true;
					}

					/**
					 * @throws IOException
					 */
					@Override
					public InputStream getInputStream() throws IOException {
						return (in);
					}
				};
			}
			if (module == null) {
				if (bundleLocation == null) {
					throw new IllegalArgumentException("Module and location cannot be null"); //$NON-NLS-1$
				}
				return getContentConnection(bundleLocation);
			}
			return getContentConnection(getUpdateLocation(module));
		} catch (IOException e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}
	}

	private String getUpdateLocation(final Module module) {
		if (System.getSecurityManager() == null)
			return getUpdateLocation0(module);
		return AccessController.doPrivileged((PrivilegedAction<String>) () -> getUpdateLocation0(module));
	}

	String getUpdateLocation0(Module module) {
		ModuleRevision current = module.getCurrentRevision();
		Generation generation = (Generation) current.getRevisionInfo();
		String updateLocation = generation.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
		if (updateLocation == null) {
			updateLocation = module.getLocation();
		}
		if (updateLocation.startsWith(INITIAL_LOCATION)) {
			updateLocation = updateLocation.substring(INITIAL_LOCATION.length());
		}
		return updateLocation;
	}

	private URLConnection getContentConnection(final String spec) throws IOException {
		if (System.getSecurityManager() == null) {
			return LocationHelper.getConnection(createURL(spec));
		}
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<URLConnection>) () -> LocationHelper.getConnection(createURL(spec)));
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof IOException)
				throw (IOException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	URL createURL(String spec) throws MalformedURLException {
		if (spec.startsWith(URLStreamHandlerFactoryImpl.PROTOCOL_REFERENCE)) {
			return new URL(null, spec, new Handler(equinoxContainer.getConfiguration().getConfiguration(EquinoxLocations.PROP_INSTALL_AREA)));
		}
		return new URL(spec);
	}

	public Generation install(Module origin, String bundleLocation, InputStream toInstall) throws BundleException {
		URLConnection content = getContentConnection(null, bundleLocation, toInstall);
		if (osgiLocation.isReadOnly()) {
			throw new BundleException("The framework storage area is read only.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		}
		URL sourceURL = content.getURL();
		InputStream in;
		try {
			in = content.getInputStream();
		} catch (Throwable e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}

		// Check if the bundle already exists at this location
		// before doing the staging and generation creation.
		// This is important since some installers seem to continually
		// re-install bundles using the same location each startup
		Module existingLocation = moduleContainer.getModule(bundleLocation);
		if (existingLocation != null) {
			// NOTE this same logic is also in the ModuleContainer
			// This is necessary because the container does the location locking.
			// Another thread could win the location lock and install before this thread does.
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
			if (origin != null) {
				// Check that the existing location is visible from the origin module
				Bundle bundle = origin.getBundle();
				BundleContext context = bundle == null ? null : bundle.getBundleContext();
				if (context != null && context.getBundle(existingLocation.getId()) == null) {
					Bundle b = existingLocation.getBundle();
					throw new BundleException(NLS.bind(Msg.ModuleContainer_NameCollisionWithLocation, new Object[] {b.getSymbolicName(), b.getVersion(), bundleLocation}), BundleException.REJECTED_BY_HOOK);
				}
			}
			return (Generation) existingLocation.getCurrentRevision().getRevisionInfo();
		}

		ContentProvider contentProvider = getContentProvider(in, sourceURL);
		Type contentType = contentProvider.getType();
		File staged = contentProvider.getContent();

		Generation generation = null;
		try {
			Long nextID = moduleDatabase.getAndIncrementNextId();
			BundleInfo info = new BundleInfo(this, nextID, bundleLocation, 0);
			generation = info.createGeneration();

			File contentFile = getContentFile(staged, contentType, nextID, generation.getGenerationId());
			generation.setContent(contentFile, contentType);
			// Check that we can open the bundle file
			generation.getBundleFile().open();
			setStorageHooks(generation);

			ModuleRevisionBuilder builder = getBuilder(generation);
			builder.setId(nextID);

			Module m = moduleContainer.install(origin, bundleLocation, builder, generation);
			if (!nextID.equals(m.getId())) {
				// this revision is already installed. delete the generation
				generation.delete();
				return (Generation) m.getCurrentRevision().getRevisionInfo();
			}
			return generation;
		} catch (Throwable t) {
			if (contentType == Type.DEFAULT) {
				try {
					delete(staged);
				} catch (IOException e) {
					// tried our best
				}
			}
			if (generation != null) {
				generation.delete();
				generation.getBundleInfo().delete();
			}
			if (t instanceof SecurityException) {
				// TODO hack from ModuleContainer
				// if the cause is a bundle exception then throw that
				if (t.getCause() instanceof BundleException) {
					throw (BundleException) t.getCause();
				}
				throw (SecurityException) t;
			}
			if (t instanceof BundleException) {
				throw (BundleException) t;
			}
			throw new BundleException("Error occurred installing a bundle.", t); //$NON-NLS-1$
		} finally {
			if (generation != null) {
				generation.getBundleInfo().unlockGeneration(generation);
			}
		}
	}

	ContentProvider getContentProvider(final InputStream in, final URL sourceURL) {
		if (in instanceof ContentProvider) {
			return (ContentProvider) in;
		}
		return new ContentProvider() {

			@Override
			public Type getType() {
				return Type.DEFAULT;
			}

			@Override
			public File getContent() throws BundleException {
				return stageContent(in, sourceURL);
			}
		};
	}

	private void setStorageHooks(Generation generation) throws BundleException {
		if (generation.getBundleInfo().getBundleId() == 0) {
			return; // ignore system bundle
		}
		List<StorageHookFactory<?, ?, ?>> factories = new ArrayList<>(getConfiguration().getHookRegistry().getStorageHookFactories());
		List<StorageHook<?, ?>> hooks = new ArrayList<>(factories.size());
		for (StorageHookFactory<?, ?, ?> storageHookFactory : factories) {
			@SuppressWarnings("unchecked")
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) storageHookFactory;
			StorageHook<Object, Object> hook = next.createStorageHookAndValidateFactoryClass(generation);
			if (hook != null) {
				hooks.add(hook);
			}
		}
		generation.setStorageHooks(Collections.unmodifiableList(hooks), true);
		for (StorageHook<?, ?> hook : hooks) {
			hook.initialize(generation.getHeaders());
		}
	}

	public ModuleRevisionBuilder getBuilder(Generation generation) throws BundleException {
		return getBuilder(generation, null, null);
	}

	public ModuleRevisionBuilder getBuilder(Generation generation, String extraCapabilities, String extraExports) throws BundleException {
		Dictionary<String, String> headers = generation.getHeaders();
		Map<String, String> mapHeaders;
		if (headers instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, String> unchecked = (Map<String, String>) headers;
			mapHeaders = unchecked;
		} else {
			mapHeaders = new HashMap<>();
			for (Enumeration<String> eKeys = headers.keys(); eKeys.hasMoreElements();) {
				String key = eKeys.nextElement();
				mapHeaders.put(key, headers.get(key));
			}
		}
		if (generation.getBundleInfo().getBundleId() != 0) {
			ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(mapHeaders, null, //
					(generation.getContentType() == Type.CONNECT ? "" : null), //$NON-NLS-1$
					(allowRestrictedProvides ? "" : null)); //$NON-NLS-1$
			if ((builder.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				for (ModuleRevisionBuilder.GenericInfo reqInfo : builder.getRequirements(HostNamespace.HOST_NAMESPACE)) {
					if (HostNamespace.EXTENSION_BOOTCLASSPATH.equals(reqInfo.getDirectives().get(HostNamespace.REQUIREMENT_EXTENSION_DIRECTIVE))) {
						throw new BundleException("Boot classpath extensions are not supported.", //$NON-NLS-1$
								BundleException.UNSUPPORTED_OPERATION, new UnsupportedOperationException());
					}
				}
			}
			return builder;
		}

		return OSGiManifestBuilderFactory.createBuilder(mapHeaders, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, extraExports, extraCapabilities);
	}

	private String getSystemExtraCapabilities() {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		StringBuilder result = new StringBuilder();

		String systemCapabilities = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
		if (systemCapabilities != null && systemCapabilities.trim().length() > 0) {
			result.append(systemCapabilities).append(", "); //$NON-NLS-1$
		}

		String extraSystemCapabilities = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA);
		if (extraSystemCapabilities != null && extraSystemCapabilities.trim().length() > 0) {
			result.append(extraSystemCapabilities).append(", "); //$NON-NLS-1$
		}

		result.append(EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE).append("; "); //$NON-NLS-1$
		result.append(EquinoxConfiguration.PROP_OSGI_OS).append("=").append(equinoxConfig.getOS()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_WS).append("=").append(equinoxConfig.getWS()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_ARCH).append("=").append(equinoxConfig.getOSArch()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		result.append(EquinoxConfiguration.PROP_OSGI_NL).append("=").append(equinoxConfig.getNL()); //$NON-NLS-1$

		String osName = equinoxConfig.getConfiguration(Constants.FRAMEWORK_OS_NAME);
		osName = osName == null ? null : osName.toLowerCase();
		String processor = equinoxConfig.getConfiguration(Constants.FRAMEWORK_PROCESSOR);
		processor = processor == null ? null : processor.toLowerCase();
		String osVersion = equinoxConfig.getConfiguration(Constants.FRAMEWORK_OS_VERSION);
		osVersion = osVersion == null ? null : osVersion.toLowerCase();
		String language = equinoxConfig.getConfiguration(Constants.FRAMEWORK_LANGUAGE);
		language = language == null ? null : language.toLowerCase();

		result.append(", "); //$NON-NLS-1$
		result.append(NativeNamespace.NATIVE_NAMESPACE).append("; "); //$NON-NLS-1$
		if (osName != null) {
			osName = getAliasList(equinoxConfig.getAliasMapper().getOSNameAliases(osName));
			result.append(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE).append(":List<String>=").append(osName).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (processor != null) {
			processor = getAliasList(equinoxConfig.getAliasMapper().getProcessorAliases(processor));
			result.append(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE).append(":List<String>=").append(processor).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		result.append(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE).append(":Version").append("=\"").append(osVersion).append("\"; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.append(NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE).append("=\"").append(language).append('\"'); //$NON-NLS-1$
		return result.toString();
	}

	String getAliasList(Collection<String> aliases) {
		if (aliases.isEmpty()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		for (String alias : aliases) {
			builder.append(alias).append(',');
		}
		builder.setLength(builder.length() - 1);
		builder.append('"');
		return builder.toString();
	}

	private String getSystemExtraPackages() {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		StringBuilder result = new StringBuilder();

		String systemPackages = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES);
		if (systemPackages != null) {
			result.append(systemPackages);
		}

		String extraSystemPackages = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		if (extraSystemPackages != null && extraSystemPackages.trim().length() > 0) {
			if (result.length() > 0) {
				result.append(", "); //$NON-NLS-1$
			}
			result.append(extraSystemPackages);
		}

		return result.toString();
	}

	private void refresh(Module module) throws BundleException {
		ModuleRevision current = module.getCurrentRevision();
		Generation currentGen = (Generation) current.getRevisionInfo();
		File content = currentGen.getContent();
		if (content == null) {
			// TODO Handle connect bundle
			return;
		}
		String spec = (currentGen.getContentType() == Type.REFERENCE ? "reference:" : "") + content.toURI().toString(); //$NON-NLS-1$ //$NON-NLS-2$
		URLConnection contentConn;
		try {
			contentConn = getContentConnection(spec);
		} catch (IOException e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}
		update(module, contentConn);
	}

	public Generation update(Module module, InputStream updateIn) throws BundleException {
		return update(module, getContentConnection(module, null, updateIn));
	}

	private Generation update(Module module, URLConnection content) throws BundleException {

		if (osgiLocation.isReadOnly()) {
			throw new BundleException("The framework storage area is read only.", BundleException.INVALID_OPERATION); //$NON-NLS-1$
		}
		URL sourceURL = content.getURL();
		InputStream in;
		try {
			in = content.getInputStream();
		} catch (Throwable e) {
			throw new BundleException("Error reading bundle content.", e); //$NON-NLS-1$
		}

		ContentProvider contentProvider = getContentProvider(in, sourceURL);
		Type contentType = contentProvider.getType();
		File staged = contentProvider.getContent();

		ModuleRevision current = module.getCurrentRevision();
		Generation currentGen = (Generation) current.getRevisionInfo();

		BundleInfo bundleInfo = currentGen.getBundleInfo();
		Generation newGen = bundleInfo.createGeneration();

		try {
			File contentFile = getContentFile(staged, contentType, bundleInfo.getBundleId(), newGen.getGenerationId());
			newGen.setContent(contentFile, contentType);
			// Check that we can open the bundle file
			newGen.getBundleFile().open();
			setStorageHooks(newGen);

			ModuleRevisionBuilder builder = getBuilder(newGen);
			moduleContainer.update(module, builder, newGen);
		} catch (Throwable t) {
			if (contentType == Type.DEFAULT) {
				try {
					delete(staged);
				} catch (IOException e) {
					// tried our best
				}
			}
			newGen.delete();
			if (t instanceof SecurityException) {
				// TODO hack from ModuleContainer
				// if the cause is a bundle exception then throw that
				if (t.getCause() instanceof BundleException) {
					throw (BundleException) t.getCause();
				}
				throw (SecurityException) t;
			}
			if (t instanceof BundleException) {
				throw (BundleException) t;
			}
			throw new BundleException("Error occurred updating a bundle.", t); //$NON-NLS-1$
		} finally {
			bundleInfo.unlockGeneration(newGen);
		}
		return newGen;
	}

	private File getContentFile(final File staged, Type contentType, final long bundleID, final long generationID) throws BundleException {
		if (System.getSecurityManager() == null)
			return getContentFile0(staged, contentType, bundleID, generationID);
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<File>) () -> getContentFile0(staged, contentType, bundleID, generationID));
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof BundleException)
				throw (BundleException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	File getContentFile0(File staged, Type contentType, long bundleID, long generationID) throws BundleException {
		File contentFile = staged;

		if (contentType == Type.DEFAULT) {
			File generationRoot = new File(childRoot, bundleID + "/" + generationID); //$NON-NLS-1$
			generationRoot.mkdirs();
			if (!generationRoot.isDirectory()) {
				throw new BundleException("Could not create generation directory: " + generationRoot.getAbsolutePath()); //$NON-NLS-1$
			}
			contentFile = new File(generationRoot, BUNDLE_FILE_NAME);
			try {
				StorageUtil.move(staged, contentFile, getConfiguration().getDebug().DEBUG_STORAGE);
			} catch (IOException e) {
				throw new BundleException("Error while renaming bundle file to final location: " + contentFile, //$NON-NLS-1$
						BundleException.READ_ERROR, e);
			}
		}
		return contentFile;
	}

	private static String getBundleFilePath(long bundleID, long generationID) {
		return bundleID + "/" + generationID + "/" + BUNDLE_FILE_NAME; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Gets a file from storage and conditionally checks the parent storage area
	 * if the file does not exist in the child configuration.
	 * Note, this method does not check for escaping of paths from the root storage area.
	 * @param path the path relative to the root of the storage area
	 * @param checkParent if true then check the parent storage (if any) when the file
	 * does not exist in the child storage area
	 * @return the file being requested. A {@code null} value is never returned.  The file
	 * returned may not exist.
	 * @throws StorageException if there was an issue getting the file
	 */
	public File getFile(String path, boolean checkParent) throws StorageException {
		return getFile(null, path, checkParent);
	}

	/**
	 * Same as {@link #getFile(String, boolean)} except takes a base parameter which is
	 * appended to the root storage area before looking for the path.  If base is not
	 * null then additional checks are done to make sure the path does not escape out
	 * of the base path.
	 * @param base the additional base path to append to the root storage area.  May be
	 * {@code null}, in which case no check is done for escaping out of the base path.
	 * @param path the path relative to the root + base storage area.
	 * @param checkParent if true then check the parent storage (if any) when the file
	 * does not exist in the child storage area
	 * @return the file being requested. A {@code null} value is never returned.  The file
	 * returned may not exist.
	 * @throws StorageException if there was an issue getting the file
	 */
	public File getFile(String base, String path, boolean checkParent) throws StorageException {
		// first check the child location
		File childPath = getFile(childRoot, base, path);
		// now check the parent
		if (checkParent && parentRoot != null) {
			if (childPath.exists()) {
				return childPath;
			}
			File parentPath = getFile(parentRoot, base, path);
			if (parentPath.exists()) {
				// only use the parent file only if it exists;
				return parentPath;
			}
		}
		// did not exist in both locations; use the child path
		return childPath;
	}

	private static File getFile(File root, String base, String path) {
		if (base == null) {
			// return quick; no need to check for path traversal
			return new File(root, path);
		}

		// if base is not null then move root to include the base
		File rootBase = new File(root, base);
		File result = new File(rootBase, path);
		if (path.contains("..")) { //$NON-NLS-1$
			// do the extra check to make sure the path did not escape the root path
			Path resultNormalized = result.toPath().normalize();
			Path rootBaseNormalized = rootBase.toPath().normalize();
			if (!resultNormalized.startsWith(rootBaseNormalized)) {
				throw new StorageException("Invalid path: " + path); //$NON-NLS-1$
			}
		}
		// Additional check if it is a special device instead of a regular file.
		if (StorageUtil.isReservedFileName(result)) {
			throw new StorageException("Invalid filename: " + path); //$NON-NLS-1$
		}
		return result;
	}

	File stageContent(final InputStream in, final URL sourceURL) throws BundleException {
		if (System.getSecurityManager() == null)
			return stageContent0(in, sourceURL);
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<File>) () -> stageContent0(in, sourceURL));
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof BundleException)
				throw (BundleException) e.getException();
			throw (RuntimeException) e.getException();
		}
	}

	File stageContent0(InputStream in, URL sourceURL) throws BundleException {
		File outFile = null;
		try (InputStream stream = in) {
			outFile = ReliableFile.createTempFile(BUNDLE_FILE_NAME, ".tmp", childRoot); //$NON-NLS-1$
			String protocol = sourceURL == null ? null : sourceURL.getProtocol();

			if ("file".equals(protocol)) { //$NON-NLS-1$
				File inFile = new File(sourceURL.getPath());
				inFile = LocationHelper.decodePath(inFile);
				if (inFile.isDirectory()) {
					// need to delete the outFile because it is not a directory
					outFile.delete();
				}
				StorageUtil.copy(inFile, outFile);
			} else {
				StorageUtil.readFile(in, outFile);
			}
			return outFile;
		} catch (IOException e) {
			if (outFile != null) {
				outFile.delete();
			}
			throw new BundleException(Msg.BUNDLE_READ_EXCEPTION, BundleException.READ_ERROR, e);
		}
	}

	/**
	 * Attempts to set the permissions of the file in a system dependent way.
	 * @param file the file to set the permissions on
	 */
	public void setPermissions(File file) {
		String commandProp = getConfiguration().getConfiguration(EquinoxConfiguration.PROP_SETPERMS_CMD);
		if (commandProp == null)
			commandProp = getConfiguration().getConfiguration(Constants.FRAMEWORK_EXECPERMISSION);
		if (commandProp == null)
			return;
		String[] commandComponents = ManifestElement.getArrayFromList(commandProp, " "); //$NON-NLS-1$
		List<String> command = new ArrayList<>(commandComponents.length + 1);
		boolean foundFullPath = false;
		for (String commandComponent : commandComponents) {
			if ("[fullpath]".equals(commandComponent) || "${abspath}".equals(commandComponent)) { //$NON-NLS-1$ //$NON-NLS-2$
				command.add(file.getAbsolutePath());
				foundFullPath = true;
			} else {
				command.add(commandComponent);
			}
		}
		if (!foundFullPath)
			command.add(file.getAbsolutePath());
		try {
			Runtime.getRuntime().exec(command.toArray(new String[command.size()])).waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BundleFile createBundleFile(File content, Generation generation, boolean isDirectory, boolean isBase) {
		BundleFile result = null;
		ConnectModule connectModule = null;
		if (generation.getContentType() == Type.CONNECT) {
			connectModule = equinoxContainer.getConnectModules().getConnectModule(generation.getBundleInfo().getLocation());
		}
		try {
			if (connectModule != null && isBase) {
				result = equinoxContainer.getConnectModules().getConnectBundleFile(connectModule, content, generation,
						mruList, getConfiguration().getDebug());
			} else if (isDirectory) {
				boolean strictPath = Boolean.parseBoolean(getConfiguration().getConfiguration(
						EquinoxConfiguration.PROPERTY_STRICT_BUNDLE_ENTRY_PATH, Boolean.FALSE.toString()));
				result = new DirBundleFile(content, strictPath);
			} else {
				result = new ZipBundleFile(content, generation, mruList, getConfiguration().getDebug(),
						getConfiguration().runtimeVerifySignedBundles);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not create bundle file.", e); //$NON-NLS-1$
		}
		return wrapBundleFile(result, generation, isBase);
	}

	public BundleFile createNestedBundleFile(String nestedDir, BundleFile bundleFile, Generation generation) {
		return createNestedBundleFile(nestedDir, bundleFile, generation, Collections.emptyList());
	}

	public BundleFile createNestedBundleFile(String nestedDir, BundleFile bundleFile, Generation generation, Collection<String> filterPrefixes) {
		// here we assume the content is a path offset into the base bundle file;  create a NestedDirBundleFile
		return wrapBundleFile(new NestedDirBundleFile(bundleFile, nestedDir, filterPrefixes), generation, false);
	}

	public BundleFile wrapBundleFile(BundleFile bundleFile, Generation generation, boolean isBase) {
		// try creating a wrapper bundlefile out of it.
		List<BundleFileWrapperFactoryHook> wrapperFactories = getConfiguration().getHookRegistry().getBundleFileWrapperFactoryHooks();
		BundleFileWrapperChain wrapped = wrapperFactories.isEmpty() ? null : new BundleFileWrapperChain(bundleFile, null);
		for (BundleFileWrapperFactoryHook wrapperFactory : wrapperFactories) {
			BundleFileWrapper wrapperBundle = wrapperFactory.wrapBundleFile(bundleFile, generation, isBase);
			if (wrapperBundle != null && wrapperBundle != bundleFile)
				bundleFile = wrapped = new BundleFileWrapperChain(wrapperBundle, wrapped);
		}

		return bundleFile;
	}

	public void compact() {
		if (!osgiLocation.isReadOnly()) {
			compact(childRoot);
		}
	}

	private void compact(File directory) {
		if (getConfiguration().getDebug().DEBUG_STORAGE)
			Debug.println("compact(" + directory.getPath() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		String list[] = directory.list();
		if (list == null)
			return;

		int len = list.length;
		for (int i = 0; i < len; i++) {
			if (BUNDLE_DATA_DIR.equals(list[i]))
				continue; /* do not examine the bundles data dir. */
			File target = new File(directory, list[i]);
			// if the file is a directory
			if (!target.isDirectory())
				continue;
			File delete = new File(target, DELETE_FLAG);
			// and the directory is marked for delete
			if (delete.exists()) {
				try {
					deleteFlaggedDirectory(target);
				} catch (IOException e) {
					if (getConfiguration().getDebug().DEBUG_STORAGE) {
						Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} else {
				compact(target); /* descend into directory */
			}
		}
	}

	void delete(final File delete) throws IOException {
		if (System.getSecurityManager() == null) {
			deleteFlaggedDirectory(delete);
		} else {
			try {
				AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
					deleteFlaggedDirectory(delete);
					return null;
				});
			} catch (PrivilegedActionException e) {
				if (e.getException() instanceof IOException)
					throw (IOException) e.getException();
				throw (RuntimeException) e.getException();
			}
		}
	}

	private void deleteFlaggedDirectory(File delete) throws IOException {
		if (!StorageUtil.rm(delete, getConfiguration().getDebug().DEBUG_STORAGE)) {
			ensureDeleteFlagFileExists(delete.toPath());
		}
	}

	public void save() throws IOException {
		if (isReadOnly()) {
			return;
		}
		if (System.getSecurityManager() == null) {
			save0();
		} else {
			try {
				AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
					save0();
					return null;
				});
			} catch (PrivilegedActionException e) {
				if (e.getException() instanceof IOException)
					throw (IOException) e.getException();
				throw (RuntimeException) e.getException();
			}
		}
	}

	void save0() throws IOException {
		StorageManager childStorageManager = null;
		ManagedOutputStream mos = null;
		DataOutputStream out = null;
		boolean success = false;
		moduleDatabase.readLock();
		try {
			synchronized (this.saveMonitor) {
				if (lastSavedTimestamp == moduleDatabase.getTimestamp())
					return;
				childStorageManager = getChildStorageManager();
				mos = childStorageManager.getOutputStream(FRAMEWORK_INFO);
				out = new DataOutputStream(new BufferedOutputStream(mos));
				saveGenerations(out);
				savePermissionData(out);
				moduleDatabase.store(out, true);
				lastSavedTimestamp = moduleDatabase.getTimestamp();
				success = true;
			}
		} finally {
			if (!success) {
				if (mos != null) {
					mos.abort();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// tried our best
				}
			}
			if (childStorageManager != null) {
				childStorageManager.close();
			}
			moduleDatabase.readUnlock();
		}
	}

	private void savePermissionData(DataOutputStream out) throws IOException {
		permissionData.savePermissionData(out);
	}

	private void saveGenerations(DataOutputStream out) throws IOException {
		List<Module> modules = moduleContainer.getModules();
		List<Generation> generations = new ArrayList<>();
		for (Module module : modules) {
			ModuleRevision revision = module.getCurrentRevision();
			if (revision != null) {
				Generation generation = (Generation) revision.getRevisionInfo();
				if (generation != null) {
					generations.add(generation);
				}
			}
		}
		out.writeInt(VERSION);

		out.writeUTF(runtimeVersion.toString());

		Version curFrameworkVersion = findFrameworkVersion();
		out.writeUTF(curFrameworkVersion == null ? Version.emptyVersion.toString() : curFrameworkVersion.toString());

		saveLongString(out, getSystemExtraCapabilities());
		saveLongString(out, getSystemExtraPackages());

		out.writeInt(cachedHeaderKeys.size());
		for (String headerKey : cachedHeaderKeys) {
			out.writeUTF(headerKey);
		}

		out.writeInt(generations.size());
		for (Generation generation : generations) {
			BundleInfo bundleInfo = generation.getBundleInfo();
			out.writeLong(bundleInfo.getBundleId());
			out.writeUTF(bundleInfo.getLocation());
			out.writeLong(bundleInfo.getNextGenerationId());
			out.writeLong(generation.getGenerationId());
			out.writeBoolean(generation.isDirectory());
			Type contentType = generation.getContentType();
			out.writeInt(contentType.ordinal());
			out.writeBoolean(generation.hasPackageInfo());
			if (bundleInfo.getBundleId() == 0 || contentType == Type.CONNECT) {
				// just write empty string for system bundle content and connect content in this case
				out.writeUTF(""); //$NON-NLS-1$
			} else {
				if (contentType == Type.REFERENCE) {
					// make reference installs relative to the install path
					out.writeUTF(new FilePath(installPath).makeRelative(new FilePath(generation.getContent().getAbsolutePath())));
				} else {
					// make normal installs relative to the storage area
					out.writeUTF(Storage.getBundleFilePath(bundleInfo.getBundleId(), generation.getGenerationId()));
				}
			}
			out.writeLong(generation.getLastModified());

			Dictionary<String, String> headers = generation.getHeaders();
			for (String headerKey : cachedHeaderKeys) {
				String value = headers.get(headerKey);
				if (value != null) {
					out.writeUTF(value);
				} else {
					out.writeUTF(NUL);
				}
			}

			out.writeBoolean(generation.isMRJar());
		}

		saveStorageHookData(out, generations);
	}

	private void saveLongString(DataOutputStream out, String value) throws IOException {
		if (value == null) {
			out.writeInt(0);
		} else {
			// don't use out.writeUTF because it has a hard string limit
			byte[] data = value.getBytes(StandardCharsets.UTF_8);
			out.writeInt(data.length);
			out.write(data);
		}
	}

	private String readLongString(DataInputStream in) throws IOException {
		int length = in.readInt();
		byte[] data = new byte[length];
		in.readFully(data);
		return new String(data, StandardCharsets.UTF_8);
	}

	private void saveStorageHookData(DataOutputStream out, List<Generation> generations) throws IOException {
		List<StorageHookFactory<?, ?, ?>> factories = getConfiguration().getHookRegistry().getStorageHookFactories();
		out.writeInt(factories.size());
		for (StorageHookFactory<?, ?, ?> factory : factories) {
			out.writeUTF(factory.getKey());
			out.writeInt(factory.getStorageVersion());

			// create a temporary in memory stream so we can figure out the length
			ByteArrayOutputStream tempBytes = new ByteArrayOutputStream();
			try (DataOutputStream temp = new DataOutputStream(tempBytes)) {
				Object saveContext = factory.createSaveContext();
				for (Generation generation : generations) {
					if (generation.getBundleInfo().getBundleId() == 0) {
						continue; // ignore system bundle
					}
					@SuppressWarnings({"rawtypes", "unchecked"})
					StorageHook<Object, Object> hook = generation.getStorageHook((Class) factory.getClass());
					if (hook != null) {
						hook.save(saveContext, temp);
					}
				}
			}
			out.writeInt(tempBytes.size());
			out.write(tempBytes.toByteArray());
		}
	}

	private Map<Long, Generation> loadGenerations(DataInputStream in, String[] cachedInfo) throws IOException {
		if (in == null) {
			return new HashMap<>(0);
		}
		int version = in.readInt();
		if (version > VERSION || version < LOWEST_VERSION_SUPPORTED) {
			throw new IllegalArgumentException("Found persistent version \"" + version + "\" expecting \"" + VERSION + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		Version savedRuntimeVersion = (version >= MR_JAR_VERSION) ? Version.parseVersion(in.readUTF()) : null;
		if (savedRuntimeVersion == null || !savedRuntimeVersion.equals(runtimeVersion)) {
			refreshMRBundles.set(true);
		}

		cachedInfo[0] = (version >= CACHED_SYSTEM_CAPS_VERION) ? in.readUTF() : null;
		cachedInfo[1] = (version >= CACHED_SYSTEM_CAPS_VERION) ? readLongString(in) : null;
		cachedInfo[2] = (version >= CACHED_SYSTEM_CAPS_VERION) ? readLongString(in) : null;

		int numCachedHeaders = in.readInt();
		List<String> storedCachedHeaderKeys = new ArrayList<>(numCachedHeaders);
		for (int i = 0; i < numCachedHeaders; i++) {
			storedCachedHeaderKeys.add(ObjectPool.intern(in.readUTF()));
		}

		int numInfos = in.readInt();
		Map<Long, Generation> result = new HashMap<>(numInfos);
		List<Generation> generations = new ArrayList<>(numInfos);
		Type[] contentTypes = Type.values();
		for (int i = 0; i < numInfos; i++) {
			long infoId = in.readLong();
			String infoLocation = ObjectPool.intern(in.readUTF());
			long nextGenId = in.readLong();
			long generationId = in.readLong();
			boolean isDirectory = in.readBoolean();

			Type contentType = Type.DEFAULT;
			if (version >= CONTENT_TYPE_VERSION) {
				contentType = contentTypes[in.readInt()];
			} else {
				if (in.readBoolean()) {
					contentType = Type.REFERENCE;
				}
			}

			boolean hasPackageInfo = in.readBoolean();
			String contentPath = in.readUTF();
			long lastModified = in.readLong();

			Map<String, String> cachedHeaders = new HashMap<>(storedCachedHeaderKeys.size());
			for (String headerKey : storedCachedHeaderKeys) {
				String value = in.readUTF();
				if (NUL.equals(value)) {
					value = null;
				} else {
					value = ObjectPool.intern(value);
				}
				cachedHeaders.put(headerKey, value);
			}
			boolean isMRJar = (version >= MR_JAR_VERSION) ? in.readBoolean() : false;

			File content = null;
			if (contentType != Type.CONNECT) {
				if (infoId == 0) {
					content = getSystemContent();
					isDirectory = content != null ? content.isDirectory() : false;
					// Note that we do not do any checking for absolute paths with
					// the system bundle. We always take the content as discovered
					// by getSystemContent()
				} else {
					content = new File(contentPath);
					if (!content.isAbsolute()) {
						// make sure it has the absolute location instead
						switch (contentType) {
						case REFERENCE:
							// reference installs are relative to the installPath
							content = new File(installPath, contentPath);
							break;
						case DEFAULT:
							// normal installs are relative to the storage area
							content = getFile(contentPath, true);
							break;
						default:
							throw new IllegalArgumentException("Unknown type: " + contentType); //$NON-NLS-1$
						}
					}
				}
			}
			BundleInfo info = new BundleInfo(this, infoId, infoLocation, nextGenId);
			Generation generation = info.restoreGeneration(generationId, content, isDirectory, contentType, hasPackageInfo, cachedHeaders, lastModified, isMRJar);
			result.put(infoId, generation);
			generations.add(generation);
		}

		connectPersistentBundles(generations);
		loadStorageHookData(generations, in);
		return result;
	}

	private void connectPersistentBundles(List<Generation> generations) {
		generations.forEach(g -> {
			try {
				if (g.getContentType() == Type.CONNECT) {
					equinoxContainer.getConnectModules().connect(g.getBundleInfo().getLocation());
				}
			} catch (IllegalStateException e) {
				if (!(e.getCause() instanceof BundleException)) {
					throw e;
				}
			}
		});
	}

	private void loadStorageHookData(List<Generation> generations, DataInputStream in) throws IOException {
		List<StorageHookFactory<?, ?, ?>> factories = new ArrayList<>(getConfiguration().getHookRegistry().getStorageHookFactories());
		Map<Generation, List<StorageHook<?, ?>>> hookMap = new HashMap<>();
		int numFactories = in.readInt();
		for (int i = 0; i < numFactories; i++) {
			String factoryName = in.readUTF();
			int version = in.readInt();
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> factory = null;
			for (Iterator<StorageHookFactory<?, ?, ?>> iFactories = factories.iterator(); iFactories.hasNext();) {
				@SuppressWarnings("unchecked")
				StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) iFactories.next();
				if (next.getKey().equals(factoryName)) {
					factory = next;
					iFactories.remove();
					break;
				}
			}
			int dataSize = in.readInt();
			byte[] bytes = new byte[dataSize];
			in.readFully(bytes);
			if (factory != null) {
				try (DataInputStream temp = new DataInputStream(new ByteArrayInputStream(bytes))) {
					if (factory.isCompatibleWith(version)) {
						Object loadContext = factory.createLoadContext(version);
						for (Generation generation : generations) {
							if (generation.getBundleInfo().getBundleId() == 0) {
								continue; // ignore system bundle
							}
							StorageHook<Object, Object> hook = factory.createStorageHookAndValidateFactoryClass(generation);
							if (hook != null) {
								hook.load(loadContext, temp);
								getHooks(hookMap, generation).add(hook);
							}
						}
					} else {
						// recover by reinitializing the hook
						for (Generation generation : generations) {
							if (generation.getBundleInfo().getBundleId() == 0) {
								continue; // ignore system bundle
							}
							StorageHook<Object, Object> hook = factory.createStorageHookAndValidateFactoryClass(generation);
							if (hook != null) {
								hook.initialize(generation.getHeaders());
								getHooks(hookMap, generation).add(hook);
							}
						}
					}
				} catch (BundleException e) {
					throw new IOException(e);
				}
			}
		}
		// now we need to recover for any hooks that are left
		for (StorageHookFactory<?, ?, ?> storageHookFactory : factories) {
			@SuppressWarnings("unchecked")
			StorageHookFactory<Object, Object, StorageHook<Object, Object>> next = (StorageHookFactory<Object, Object, StorageHook<Object, Object>>) storageHookFactory;
			// recover by reinitializing the hook
			for (Generation generation : generations) {
				if (generation.getBundleInfo().getBundleId() == 0) {
					continue; // ignore system bundle
				}
				StorageHook<Object, Object> hook = next.createStorageHookAndValidateFactoryClass(generation);
				if (hook != null) {
					try {
						hook.initialize(generation.getHeaders());
						getHooks(hookMap, generation).add(hook);
					} catch (BundleException e) {
						throw new IOException(e);
					}
				}
			}
		}
		// now set the hooks to the generations
		for (Generation generation : generations) {
			generation.setStorageHooks(Collections.unmodifiableList(getHooks(hookMap, generation)), false);
		}
	}

	private static List<StorageHook<?, ?>> getHooks(Map<Generation, List<StorageHook<?, ?>>> hookMap, Generation generation) {
		List<StorageHook<?, ?>> result = hookMap.get(generation);
		if (result == null) {
			result = new ArrayList<>();
			hookMap.put(generation, result);
		}
		return result;
	}

	private File getSystemContent() {
		String frameworkValue = equinoxContainer.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_FRAMEWORK);
		if (frameworkValue == null || !frameworkValue.startsWith("file:")) { //$NON-NLS-1$
			return null;
		}
		// TODO assumes the location is a file URL
		File result = new File(frameworkValue.substring(5)).getAbsoluteFile();
		if (!result.exists()) {
			throw new IllegalStateException("Configured framework location does not exist: " + result.getAbsolutePath()); //$NON-NLS-1$
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	private void loadVMProfile(Generation systemGeneration) {
		EquinoxConfiguration equinoxConfig = equinoxContainer.getConfiguration();
		Properties profileProps = findVMProfile(systemGeneration);
		String systemExports = equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES);
		// set the system exports property using the vm profile; only if the property is not already set
		if (systemExports == null) {
			systemExports = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
			if (systemExports != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_SYSTEMPACKAGES, systemExports);
		}

		// set the org.osgi.framework.bootdelegation property according to the java profile
		String type = equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_BOOTDELEGATION); // a null value means ignore
		String profileBootDelegation = profileProps.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
		if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_OVERRIDE.equals(type)) {
			if (profileBootDelegation == null)
				equinoxConfig.clearConfiguration(Constants.FRAMEWORK_BOOTDELEGATION); // override with a null value
			else
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_BOOTDELEGATION, profileBootDelegation); // override with the profile value
		} else if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_NONE.equals(type))
			equinoxConfig.clearConfiguration(Constants.FRAMEWORK_BOOTDELEGATION); // remove the bootdelegation property in case it was set
		// set the org.osgi.framework.executionenvironment property according to the java profile
		if (equinoxConfig.getConfiguration(Constants.FRAMEWORK_EXECUTIONENVIRONMENT) == null) {
			// get the ee from the java profile; if no ee is defined then try the java profile name
			String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, profileProps.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME));
			if (ee != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
		}
		// set the org.osgi.framework.system.capabilities property according to the java profile
		if (equinoxConfig.getConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES) == null) {
			String systemCapabilities = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
			if (systemCapabilities != null)
				equinoxConfig.setConfiguration(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities);
		}
	}

	private Properties findVMProfile(Generation systemGeneration) {
		Properties result = readConfiguredJavaProfile(systemGeneration);
		String vmProfile = null;
		try {
			if (result != null) {
				return result;
			}

			if (Version.valueOf("9").compareTo(runtimeVersion) <= 0) { //$NON-NLS-1$
				result = calculateVMProfile(runtimeVersion);
				if (result != null) {
					return result;
				}
				// could not calculate; fall back to reading profile files
			}

			String embeddedProfileName = "-"; //$NON-NLS-1$
			// If javaSE 1.8 then check for release file for profile name.
			if (runtimeVersion != null && Version.valueOf("1.8").compareTo(runtimeVersion) <= 0) { //$NON-NLS-1$
				String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
				if (javaHome != null) {
					File release = new File(javaHome, "release"); //$NON-NLS-1$
					if (release.exists()) {
						Properties releaseProps = new Properties();
						try (InputStream releaseStream = new FileInputStream(release)) {
							releaseProps.load(releaseStream);
							String releaseName = releaseProps.getProperty("JAVA_PROFILE"); //$NON-NLS-1$
							if (releaseName != null) {
								// make sure to remove extra quotes
								releaseName = releaseName.replaceAll("^\\s*\"?|\"?\\s*$", ""); //$NON-NLS-1$ //$NON-NLS-2$
								embeddedProfileName = "_" + releaseName + "-"; //$NON-NLS-1$ //$NON-NLS-2$
							}
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}

			result = new Properties();
			vmProfile = JAVASE + embeddedProfileName + javaSpecVersion;
			try (InputStream profileIn = createProfileStream(systemGeneration, vmProfile, embeddedProfileName)) {
				if (profileIn != null) {
					result.load(profileIn);
				}
			} catch (IOException e) {
				// ignore
				// TODO consider logging ...
			}
		} finally {
			// set the profile name if it does not provide one
			if (result != null && result.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME) == null) {
				if (vmProfile != null) {
					result.put(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME, vmProfile.replace('_', '/'));
				} else {
					// last resort; default to the absolute minimum profile name for the framework
					result.put(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME, "JavaSE-1.7"); //$NON-NLS-1$
				}
			}
		}
		return result;
	}

	private InputStream createProfileStream(Generation systemGeneration, String vmProfile,
			String embeddedProfileName) {
		InputStream profileIn = null;
		if (vmProfile != null) {
			// look for a profile in the system bundle based on the vm profile
			String javaProfile = vmProfile + PROFILE_EXT;
			profileIn = findInSystemBundle(systemGeneration, javaProfile);
			if (profileIn == null) {
				profileIn = getNextBestProfile(systemGeneration, JAVASE, runtimeVersion, embeddedProfileName);
			}
		}
		if (profileIn == null) {
			// the profile url is still null then use the min profile the framework can use
			profileIn = findInSystemBundle(systemGeneration, "JavaSE-1.8.profile"); //$NON-NLS-1$
		}
		return profileIn;
	}

	private Properties readConfiguredJavaProfile(Generation systemGeneration) {
		// check for the java profile property for a url
		String propJavaProfile = equinoxContainer.getConfiguration().getConfiguration(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE);
		if (propJavaProfile != null) {
			try (InputStream profileIn = createPropStream(systemGeneration, propJavaProfile)){
				if (profileIn != null) {
					Properties result = new Properties();
					result.load(profileIn);
					return result;
				}
			} catch (IOException e) {
				// consider logging
			}
		}
		return null;
	}

	private InputStream createPropStream(Generation systemGeneration, String propJavaProfile) {
		try {
			// we assume a URL
			return new URL(propJavaProfile).openStream();
		} catch (IOException e) {
			// try using a relative path in the system bundle
			return findInSystemBundle(systemGeneration, propJavaProfile);
		}
	}

	@SuppressWarnings("deprecation")
	private Properties calculateVMProfile(Version javaVersion) {
		String systemPackages = calculateVMPackages();
		if (systemPackages == null) {
			return null;
		}
		String executionEnvs = calculateVMExecutionEnvs(javaVersion);
		String eeCapabilities = calculateEECapabilities(javaVersion);

		Properties result = new Properties();
		result.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages);
		result.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, executionEnvs);
		result.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, eeCapabilities);
		return result;
	}

	private String calculateVMExecutionEnvs(Version javaVersion) {
		StringBuilder result = new StringBuilder("OSGi/Minimum-1.0, OSGi/Minimum-1.1, OSGi/Minimum-1.2, JavaSE/compact1-1.8, JavaSE/compact2-1.8, JavaSE/compact3-1.8, JRE-1.1, J2SE-1.2, J2SE-1.3, J2SE-1.4, J2SE-1.5, JavaSE-1.6, JavaSE-1.7, JavaSE-1.8"); //$NON-NLS-1$
		Version v = new Version(9, 0, 0);
		while (v.compareTo(javaVersion) <= 0) {
			result.append(',').append(' ').append(JAVASE).append('-').append(v.getMajor());
			if (v.getMinor() > 0) {
				result.append('.').append(v.getMinor());
			}
			if (v.getMajor() == javaVersion.getMajor()) {
				v = new Version(v.getMajor(), v.getMinor() + 1, 0);
			} else {
				v = new Version(v.getMajor() + 1, 0, 0);
			}
		}
		return result.toString();
	}

	private String calculateEECapabilities(Version javaVersion) {
		Version v = new Version(9, 0, 0);
		StringBuilder versionsBulder = new StringBuilder();
		while (v.compareTo(javaVersion) <= 0) {
			versionsBulder.append(',').append(' ').append(v.getMajor()).append('.').append(v.getMinor());
			if (v.getMajor() == javaVersion.getMajor()) {
				v = new Version(v.getMajor(), v.getMinor() + 1, 0);
			} else {
				v = new Version(v.getMajor() + 1, 0, 0);
			}
		}
		String versionsList = versionsBulder.toString();

		StringBuilder result = new StringBuilder("osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\", osgi.ee; osgi.ee=\"JRE\"; version:List<Version>=\"1.0, 1.1\", osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8"); //$NON-NLS-1$
		result.append(versionsList).append("\""); //$NON-NLS-1$
		result.append(",osgi.ee; osgi.ee=\"JavaSE/compact1\"; version:List<Version>=\"1.8"); //$NON-NLS-1$
		result.append(versionsList).append("\""); //$NON-NLS-1$
		result.append(",osgi.ee; osgi.ee=\"JavaSE/compact2\"; version:List<Version>=\"1.8"); //$NON-NLS-1$
		result.append(versionsList).append("\""); //$NON-NLS-1$
		result.append(",osgi.ee; osgi.ee=\"JavaSE/compact3\"; version:List<Version>=\"1.8"); //$NON-NLS-1$
		result.append(versionsList).append("\""); //$NON-NLS-1$

		return result.toString();
	}

	@SuppressWarnings("unchecked")
	private String calculateVMPackages() {
		try {
			List<String> packages = new ArrayList<>();
			Method classGetModule = Class.class.getMethod("getModule"); //$NON-NLS-1$
			Object thisModule = classGetModule.invoke(getClass());
			Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer"); //$NON-NLS-1$
			Method boot = moduleLayerClass.getMethod("boot"); //$NON-NLS-1$
			Method modules = moduleLayerClass.getMethod("modules"); //$NON-NLS-1$
			Class<?> moduleClass = Class.forName("java.lang.Module"); //$NON-NLS-1$
			Method getDescriptor = moduleClass.getMethod("getDescriptor"); //$NON-NLS-1$
			Class<?> moduleDescriptorClass = Class.forName("java.lang.module.ModuleDescriptor"); //$NON-NLS-1$
			Method exports = moduleDescriptorClass.getMethod("exports"); //$NON-NLS-1$
			Method isAutomatic = moduleDescriptorClass.getMethod("isAutomatic"); //$NON-NLS-1$
			Method packagesMethod = moduleDescriptorClass.getMethod("packages"); //$NON-NLS-1$
			Class<?> exportsClass = Class.forName("java.lang.module.ModuleDescriptor$Exports"); //$NON-NLS-1$
			Method isQualified = exportsClass.getMethod("isQualified"); //$NON-NLS-1$
			Method source = exportsClass.getMethod("source"); //$NON-NLS-1$

			Object bootLayer = boot.invoke(null);
			Set<?> bootModules = (Set<?>) modules.invoke(bootLayer);
			for (Object m : bootModules) {
				if (m.equals(thisModule)) {
					// Do not calculate the exports from the framework module.
					// This is to handle the case where the framework is on the module path
					// to avoid double exports from the system.bundles
					continue;
				}
				Object descriptor = getDescriptor.invoke(m);
				if ((Boolean) isAutomatic.invoke(descriptor)) {
					/*
					 * Automatic modules are supposed to export all their packages.
					 * However, java.lang.module.ModuleDescriptor::exports returns an empty set for them.
					 * Add all their packages (as returned by java.lang.module.ModuleDescriptor::packages)
					 * to the list of VM supplied packages.
					 */
					packages.addAll((Set<String>) packagesMethod.invoke(descriptor));
				} else {
					for (Object export : (Set<?>) exports.invoke(descriptor)) {
						String pkg = (String) source.invoke(export);
						if (!((Boolean) isQualified.invoke(export))) {
							packages.add(pkg);
						}
					}
				}
			}
			Collections.sort(packages);
			StringBuilder result = new StringBuilder();
			for (String pkg : packages) {
				if (result.length() != 0) {
					result.append(',').append(' ');
				}
				result.append(pkg);
			}
			return result.toString();
		} catch (Exception e) {
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, "Error determining system packages.", e); //$NON-NLS-1$
			return null;
		}
	}

	private InputStream getNextBestProfile(Generation systemGeneration, String javaEdition, Version javaVersion, String embeddedProfileName) {
		if (javaVersion == null || javaEdition != JAVASE)
			return null; // we cannot automatically choose the next best profile unless this is a JavaSE vm
		InputStream bestProfile = findNextBestProfile(systemGeneration, javaEdition, javaVersion, embeddedProfileName);
		if (bestProfile == null && !"-".equals(embeddedProfileName)) { //$NON-NLS-1$
			// Just use the base javaEdition name without the profile name as backup
			return getNextBestProfile(systemGeneration, javaEdition, javaVersion, "-"); //$NON-NLS-1$
		}
		return bestProfile;
	}

	private InputStream findNextBestProfile(Generation systemGeneration, String javaEdition, Version javaVersion, String embeddedProfileName) {
		InputStream result = null;
		int major = javaVersion.getMajor();
		int minor = javaVersion.getMinor();
		do {
			// If minor is zero then it is not included in the name
			String profileResourceName = javaEdition + embeddedProfileName + major + ((minor > 0) ? "." + minor : "") + PROFILE_EXT; //$NON-NLS-1$ //$NON-NLS-2$
			result = findInSystemBundle(systemGeneration, profileResourceName);
			if (minor > 0) {
				minor -= 1;
			} else if (major > 9) {
				major -= 1;
			} else if (major <= 9 && major > 1) {
				minor = 8;
				major = 1;
			} else {
				// we have reached the end of our search; return the existing result;
				return result;
			}
		} while (result == null && minor >= 0);
		return result;
	}

	private InputStream findInSystemBundle(Generation systemGeneration, String entry) {
		BundleFile systemContent = systemGeneration.getBundleFile();
		BundleEntry systemEntry = systemContent != null ? systemContent.getEntry(entry) : null;
		InputStream result = null;
		if (systemEntry != null) {
			try {
				result = systemEntry.getInputStream();
			} catch (IOException e) {
				// Do nothing
			}
		}
		if (result == null) {
			// Check the ClassLoader in case we're launched off the Java boot classpath
			ClassLoader loader = getClass().getClassLoader();
			result = loader == null ? ClassLoader.getSystemResourceAsStream(entry) : loader.getResourceAsStream(entry);
		}
		return result;
	}

	public static Enumeration<URL> findEntries(List<Generation> generations, String path, String filePattern, int options) {
		List<BundleFile> bundleFiles = new ArrayList<>(generations.size());
		for (Generation generation : generations)
			bundleFiles.add(generation.getBundleFile());
		// search all the bundle files
		List<String> pathList = listEntryPaths(bundleFiles, path, filePattern, options);
		// return null if no entries found
		if (pathList.size() == 0)
			return null;
		// create an enumeration to enumerate the pathList (generations must not change)
		Stream<URL> entries = pathList.stream().flatMap(p -> generations.stream().map(g -> g.getEntry(p)))
				.filter(Objects::nonNull);
		return InternalUtils.asEnumeration(entries.iterator());
	}

	/**
	 * Returns the names of resources available from a list of bundle files.
	 * No duplicate resource names are returned, each name is unique.
	 * @param bundleFiles the list of bundle files to search in
	 * @param path The path name in which to look.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path.
	 * @param options The options for listing resource names.
	 * @return a list of resource names.  If no resources are found then
	 * the empty list is returned.
	 * @see BundleWiring#listResources(String, String, int)
	 */
	public static List<String> listEntryPaths(List<BundleFile> bundleFiles, String path, String filePattern, int options) {
		// Use LinkedHashSet for optimized performance of contains() plus
		// ordering guarantees.
		LinkedHashSet<String> pathList = new LinkedHashSet<>();
		Filter patternFilter = null;
		Hashtable<String, String> patternProps = null;
		if (filePattern != null) {
			// Optimization: If the file pattern does not include a wildcard  or escape char then it must represent a single file.
			// Avoid pattern matching and use BundleFile.getEntry() if recursion was not requested.
			if ((options & BundleWiring.FINDENTRIES_RECURSE) == 0 && filePattern.indexOf('*') == -1 && filePattern.indexOf('\\') == -1) {
				if (path.length() == 0)
					path = filePattern;
				else
					path += path.charAt(path.length() - 1) == '/' ? filePattern : '/' + filePattern;
				for (BundleFile bundleFile : bundleFiles) {
					if (bundleFile.getEntry(path) != null && !pathList.contains(path))
						pathList.add(path);
				}
				return new ArrayList<>(pathList);
			}
			// For when the file pattern includes a wildcard.
			try {
				// create a file pattern filter with 'filename' as the key
				patternFilter = FilterImpl.newInstance("(filename=" + sanitizeFilterInput(filePattern) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				// create a single hashtable to be shared during the recursive search
				patternProps = new Hashtable<>(2);
			} catch (InvalidSyntaxException e) {
				// TODO something unexpected happened; log error and return nothing
				//				Bundle b = context == null ? null : context.getBundle();
				//				eventPublisher.publishFrameworkEvent(FrameworkEvent.ERROR, b, e);
				return new ArrayList<>(pathList);
			}
		}
		// find the entry paths for the datas
		for (BundleFile bundleFile : bundleFiles) {
			listEntryPaths(bundleFile, path, patternFilter, patternProps, options, pathList);
		}
		return new ArrayList<>(pathList);
	}

	public static String sanitizeFilterInput(String filePattern) throws InvalidSyntaxException {
		StringBuilder buffer = null;
		boolean foundEscape = false;
		for (int i = 0; i < filePattern.length(); i++) {
			char c = filePattern.charAt(i);
			switch (c) {
				case '\\' :
					// we either used the escape found or found a new escape.
					foundEscape = foundEscape ? false : true;
					if (buffer != null)
						buffer.append(c);
					break;
				case '(' :
				case ')' :
					if (!foundEscape) {
						if (buffer == null) {
							buffer = new StringBuilder(filePattern.length() + 16);
							buffer.append(filePattern.substring(0, i));
						}
						// must escape with '\'
						buffer.append('\\');
					} else {
						foundEscape = false; // used the escape found
					}
					if (buffer != null)
						buffer.append(c);
					break;
				default :
					// if we found an escape it has been used
					foundEscape = false;
					if (buffer != null)
						buffer.append(c);
					break;
			}
		}
		if (foundEscape)
			throw new InvalidSyntaxException("Trailing escape characters must be escaped.", filePattern); //$NON-NLS-1$
		return buffer == null ? filePattern : buffer.toString();
	}

	// Use LinkedHashSet for optimized performance of contains() plus ordering
	// guarantees.
	private static LinkedHashSet<String> listEntryPaths(BundleFile bundleFile, String path, Filter patternFilter, Hashtable<String, String> patternProps, int options, LinkedHashSet<String> pathList) {
		if (pathList == null)
			pathList = new LinkedHashSet<>();
		boolean recurse = (options & BundleWiring.FINDENTRIES_RECURSE) != 0;
		Enumeration<String> entryPaths = bundleFile.getEntryPaths(path, recurse);
		if (entryPaths == null)
			return pathList;
		while (entryPaths.hasMoreElements()) {
			String entry = entryPaths.nextElement();
			int lastSlash = entry.lastIndexOf('/');
			if (patternProps != null) {
				int secondToLastSlash = entry.lastIndexOf('/', lastSlash - 1);
				int fileStart;
				int fileEnd = entry.length();
				if (lastSlash < 0)
					fileStart = 0;
				else if (lastSlash != entry.length() - 1)
					fileStart = lastSlash + 1;
				else {
					fileEnd = lastSlash; // leave the lastSlash out
					if (secondToLastSlash < 0)
						fileStart = 0;
					else
						fileStart = secondToLastSlash + 1;
				}
				String fileName = entry.substring(fileStart, fileEnd);
				// set the filename to the current entry
				patternProps.put("filename", fileName); //$NON-NLS-1$
			}
			// prevent duplicates and match on the patternFilter
			if (!pathList.contains(entry) && (patternFilter == null || patternFilter.matchCase(patternProps)))
				pathList.add(entry);
		}
		return pathList;
	}

	public String copyToTempLibrary(Generation generation, String absolutePath) {
		File libTempDir = new File(childRoot, LIB_TEMP);
		// we assume the absolutePath is a File path
		File realLib = new File(absolutePath);
		String libName = realLib.getName();
		// find a temp dir for the bundle data and the library;
		File bundleTempDir = null;
		File libTempFile = null;
		// We need a somewhat predictable temp dir for the libraries of a given bundle;
		// This is not strictly necessary but it does help scenarios where one native library loads another native library without using java.
		// On some OSes this causes issues because the second library is cannot be found.
		// This has been worked around by the bundles loading the libraries in a particular order (and setting some LIB_PATH env).
		// The one catch is that the libraries need to be in the same directory and they must use their original lib names.
		//
		// This bit of code attempts to do that by using the bundle ID as an ID for the temp dir along with an incrementing ID
		// in cases where the temp dir may already exist.
		long bundleID = generation.getBundleInfo().getBundleId();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			bundleTempDir = new File(libTempDir, bundleID + "_" + i); //$NON-NLS-1$
			libTempFile = new File(bundleTempDir, libName);
			if (bundleTempDir.exists()) {
				if (libTempFile.exists())
					continue; // to to next temp file
				break;
			}
			break;
		}
		if (!bundleTempDir.isDirectory()) {
			bundleTempDir.mkdirs();
			bundleTempDir.deleteOnExit();
			// This is just a safeguard incase the VM is terminated unexpectantly, it also looks like deleteOnExit cannot really work because
			// the VM likely will still have a lock on the lib file at the time of VM exit.
			try { // need to create a delete flag to force removal the temp libraries
				ensureDeleteFlagFileExists(libTempDir.toPath());
			} catch (IOException e) {
				// do nothing; that would mean we did not make the temp dir successfully
			}
		}
		// copy the library file
		try {
			StorageUtil.copy(realLib, libTempFile);
			// set permissions if needed
			setPermissions(libTempFile);
			libTempFile.deleteOnExit(); // this probably will not work because the VM will probably have the lib locked at exit
			// return the temporary path
			return libTempFile.getAbsolutePath();
		} catch (IOException e) {
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, e.getMessage(), e);
			return null;
		}
	}

	private static void ensureDeleteFlagFileExists(Path directory) throws IOException {
		Path deleteFlag = directory.resolve(DELETE_FLAG);
		if (!Files.exists(deleteFlag) ) {
			Files.createFile(deleteFlag);
		}
	}

	public SecurityAdmin getSecurityAdmin() {
		return securityAdmin;
	}

	protected StorageManager getChildStorageManager() throws IOException {
		String locking = getConfiguration().getConfiguration(LocationHelper.PROP_OSGI_LOCKING, LocationHelper.LOCKING_NIO);
		StorageManager sManager = new StorageManager(childRoot, isReadOnly() ? LocationHelper.LOCKING_NONE : locking, isReadOnly());
		try {
			sManager.open(!isReadOnly());
		} catch (IOException ex) {
			if (getConfiguration().getDebug().DEBUG_STORAGE) {
				Debug.println("Error reading framework.info: " + ex.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(ex);
			}
			String message = NLS.bind(Msg.ECLIPSE_STARTUP_FILEMANAGER_OPEN_ERROR, ex.getMessage());
			equinoxContainer.getLogServices().log(EquinoxContainer.NAME, FrameworkLogEntry.ERROR, message, ex);
			getConfiguration().setProperty(EclipseStarter.PROP_EXITCODE, "15"); //$NON-NLS-1$
			String errorDialog = "<title>" + Msg.ADAPTOR_STORAGE_INIT_FAILED_TITLE + "</title>" + NLS.bind(Msg.ADAPTOR_STORAGE_INIT_FAILED_MSG, childRoot) + "\n" + ex.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			getConfiguration().setProperty(EclipseStarter.PROP_EXITDATA, errorDialog);
			throw ex;
		}
		return sManager;
	}

	private InputStream getInfoInputStream() throws IOException {
		StorageManager storageManager = getChildStorageManager();
		InputStream storageStream = null;
		try {
			storageStream = storageManager.getInputStream(FRAMEWORK_INFO);
		} catch (IOException ex) {
			if (getConfiguration().getDebug().DEBUG_STORAGE) {
				Debug.println("Error reading framework.info: " + ex.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(ex);
			}
		} finally {
			storageManager.close();
		}
		if (storageStream == null && parentRoot != null) {
			StorageManager parentStorageManager = null;
			try {
				parentStorageManager = new StorageManager(parentRoot, LocationHelper.LOCKING_NONE, true);
				parentStorageManager.open(false);
				storageStream = parentStorageManager.getInputStream(FRAMEWORK_INFO);
			} catch (IOException e1) {
				// That's ok we will regenerate the framework.info
			} finally {
				if (parentStorageManager != null) {
					parentStorageManager.close();
				}
			}
		}
		return storageStream;
	}

	EquinoxContainer getEquinoxContainer() {
		return equinoxContainer;
	}
}

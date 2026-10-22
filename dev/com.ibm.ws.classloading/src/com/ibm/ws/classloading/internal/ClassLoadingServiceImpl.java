/*******************************************************************************
 * Copyright (c) 2010, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.LibraryPrecedence.beforeApp;
import static com.ibm.ws.classloading.internal.ClassLoadingConstants.SHARED_LIBRARY_DOMAIN;
import static com.ibm.ws.classloading.internal.ClassLoadingConstants.SPI_SHARED_LIBRARY_DOMAIN;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.File;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.classloading.LibertyClassLoadingService;
import com.ibm.ws.classloading.MetaInfServicesProvider;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.ClassLoaderFactory.PostCreateAction;
import com.ibm.ws.classloading.internal.providers.WeakLibraryListener;
import com.ibm.ws.classloading.internal.util.CanonicalStore;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.classloading.internal.util.Factory;
import com.ibm.ws.classloading.internal.util.MultiMap;
import com.ibm.ws.classloading.serializable.ClassLoaderIdentityImpl;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.utils.KeyBasedLockStore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.ClassTransformer;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.classloading.ResourceProvider;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.logging.Introspector;

@Component(service = { ClassLoadingService.class, LibertyClassLoadingService.class, ClassLoaderIdentifierService.class, Introspector.class, RuntimeUpdateListener.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class ClassLoadingServiceImpl implements LibertyClassLoadingService<LibertyLoader>, ClassLoaderIdentifierService, Introspector, RuntimeUpdateListener {
    static final TraceComponent tc = Tr.register(ClassLoadingServiceImpl.class);

    private final Map<ClassLoader, StackTraceElement[]> leakDetectionMap = new ConcurrentHashMap<ClassLoader, StackTraceElement[]>();
    private final Set<AppClassLoader> appClassLoaders = Collections.newSetFromMap(new WeakHashMap<AppClassLoader, Boolean>());

    private static final int TCCL_LOCK_WAIT = Integer.getInteger("com.ibm.ws.classloading.tcclLockWaitTimeMillis", 15000);
    static final String REFERENCE_GENERATORS = "generators";

    private static final KeyBasedLockStore<String, ReentrantLock> tcclLockStore = new KeyBasedLockStore<>(new Function<String, ReentrantLock>() {
        @Override
        public ReentrantLock apply(String key) {
            return new ReentrantLock();
        }
    });
    
    /**
     * Capture window: the FEATURE_BUNDLES_RESOLVED notification for the current UPDATE, or null.
     *
     * <p>Only one window is tracked. FeatureManager applies one feature change at a time, so
     * windows are not expected to overlap. If a window is never closed (e.g. the update fails
     * before FEATURE_BUNDLES_RESOLVED completes), the next UPDATE overwrites it and the
     * abandoned data is dropped without a recycle. This is an accepted limitation.</p>
     */
    private volatile RuntimeUpdateNotification captureNotification = null;

    /**
     * ACLs whose gateway went UNRESOLVED during the current capture window.
     * Used only to drive aclStore.remove(); app names come from capturedAppNames.
     * Concurrent because the listener may add while the window is being closed.
     */
    private volatile Set<AppClassLoader> capturedAcls = null;

    /**
     * App names to recycle, collected directly from GCL.getOwnerAppNames() during
     * the capture window. Replaces the delegate-expansion scan in runRecycleListener.
     * Concurrent because the listener may add while the window is being closed.
     */
    private volatile Set<String> capturedAppNames = null;

    private final BundleContext bundleContext;
    private final CanonicalStore<ClassLoaderIdentity, AppClassLoader> aclStore = new CanonicalStore<ClassLoaderIdentity, AppClassLoader>();
    private final CanonicalStore<String, ThreadContextClassLoader> tcclStore = new CanonicalStore<String, ThreadContextClassLoader>();
    private final RegionDigraph digraph;
    private final ClassRedefiner redefiner;
    /**
     * Optional reference to the application recycle coordinator; used to recycle applications
     * when a feature bundle is uninstalled. OPTIONAL/DYNAMIC to avoid an OSGi dependency cycle
     * (com.ibm.ws.app.manager.lifecycle must not depend on com.ibm.ws.classloading).
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected volatile ApplicationRecycleCoordinator recycleCoordinator;

    /**
     * Captures the AppClassLoader whose gateway bundle went UNRESOLVED during a
     * ProvisioningMode.UPDATE operation. Fires only while a capture window is open.
     *
     * <p>Normally there is one live AppClassLoader per gateway bundle: the bundle location
     * is keyed on the ClassLoaderIdentity string. However, a recycled loader can linger in
     * {@code appClassLoaders} until garbage collected (the bundle is reused), and child
     * loaders (e.g. EAR modules) share their parent's gateway bundle. We therefore capture
     * every match rather than stopping at the first.</p>
     */
    private final SynchronousBundleListener gatewayUnresolvedListener = new SynchronousBundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            if (event.getType() != BundleEvent.UNRESOLVED) {
                return;
            }
            if (captureNotification == null) {
                return; // window not open
            }
            Bundle bundle = event.getBundle();
            // classloaders also holds external bundles used by bundle add-on loaders;
            // only the generated gateway bundles carry the marker header.
            if (!classloaders.containsKey(bundle)
                || !"true".equalsIgnoreCase(bundle.getHeaders("").get(GatewayBundleFactory.GATEWAY_BUNDLE_MARKER))) {
                return; // not a gateway bundle
            }
            // Local copies: the window may be closed (fields nulled) concurrently.
            Set<AppClassLoader> aclsToEvict = capturedAcls;
            Set<String> appNames = capturedAppNames;
            if (aclsToEvict == null || appNames == null) {
                return; // window closed
            }
            synchronized (classloaders) {
                // Collect app names from the reverse link on each GCL.
                Set<GatewayClassLoader> gcls = classloaders.get(bundle);
                if (gcls != null) {
                    for (GatewayClassLoader gcl : gcls) {
                        appNames.addAll(gcl.getOwnerAppNames());
                    }
                }
                // Capture ACL instances (needed for aclStore.remove in runRecycleListener).
                synchronized (appClassLoaders) {
                    for (AppClassLoader acl : appClassLoaders) {
                        if (bundle.equals(acl.getBundle())) {
                            aclsToEvict.add(acl);
                        }
                    }
                }
            }
        }
    };

    private final BundleListener listener = new BundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            if (event.getType() == BundleEvent.UNINSTALLED) {
                classloaders.remove(event.getBundle());
            } else if (event.getType() == BundleEvent.RESOLVED) {
                synchronized (classloaders) {
                    Set<GatewayClassLoader> gwCLs = classloaders.get(event.getBundle());
                    if (gwCLs != null) {
                        for (GatewayClassLoader gwCL : gwCLs) {
                            gwCL.populateNewLoader();
                        }
                    }
                }
            }
        }
    };;
    final Map<Bundle, Set<GatewayClassLoader>> classloaders = Collections.synchronizedMap(new HashMap<Bundle, Set<GatewayClassLoader>>());
    private final CompositeResourceProvider resourceProviders = new CompositeResourceProvider();
    private final Map<String, WeakReference<Bundle>> rememberedBundles = new ConcurrentHashMap<String, WeakReference<Bundle>>();
    private final ReferenceQueue<Bundle> collectedBundles = new ReferenceQueue<Bundle>();
    private final ConcurrentServiceReferenceSet<ClassGenerator> generatorRefs = new ConcurrentServiceReferenceSet<ClassGenerator>(REFERENCE_GENERATORS);
    private final ClassGeneratorManager generatorManager = new ClassGeneratorManager(generatorRefs);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile List<ApplicationExtensionLibrary> appExtLibs;

    private final GlobalClassloadingConfiguration globalConfig;

    // These services are not expected to come and go often,
    // but we want quick iteration so using copy on write approach
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY,
                    fieldOption = FieldOption.UPDATE,
                    target = "(io.openliberty.classloading.system.transformer=true)")
    private final List<ClassFileTransformer> systemTransformers = new CopyOnWriteArrayList<>();
    List<ClassFileTransformer> unitTestOnlyGetSystemTransformers() {
        return systemTransformers;
    }

    /**
     * Mapping from META-INF services file names to the corresponding service provider implementation class name.
     */
    final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> metaInfServicesProviders = new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

    /**
     * Mapping from META-INF services provider implementation names to the corresponding registered service.
     */
    final ConcurrentServiceReferenceMap<String, MetaInfServicesProvider> metaInfServicesRefs = new ConcurrentServiceReferenceMap<String, MetaInfServicesProvider>("MetaInfServicesProvider");

    private Map<String, ProtectionDomain> protectionDomainMap = null;

    /**
     * For converting type/app/module/comp to a metadata ID under getClassLoaderIdentifier.
     */
    protected final MetaDataIdentifierService metadataIdentifierService;

    /**
     * reference to the global library - primarily used for dump introspector output
     */
    private final AtomicReference<Library> globalSharedLibrary = new AtomicReference<>();

    List<ClassFileTransformer> getSystemTransformers() {
        return systemTransformers;
    }

    @Activate
    public ClassLoadingServiceImpl(ComponentContext cCtx,
                            @Reference GlobalClassloadingConfiguration globalConfig,
                            @Reference RegionDigraph digraph,
                            @Reference MetaDataIdentifierService mdiService,
                            @Reference(service = URLStreamHandlerService.class, target = "(url.handler.protocol=wsjar)") URLStreamHandlerService wsjar,
                            @Reference(cardinality = ReferenceCardinality.OPTIONAL) Instrumentation instr) {
        // Declared a dependency on the URLStreamHandlerService so the wsjar protocol
        // doesn't go away while we still may still need it; no need to store the instance of wsjar here.
        this.bundleContext = cCtx.getBundleContext();
        this.globalConfig = globalConfig;
        this.digraph = digraph;
        this.metadataIdentifierService = mdiService;
        this.redefiner = new ClassRedefiner(instr);

        generatorRefs.activate(cCtx);
        metaInfServicesRefs.activate(cCtx);
    }

    // NOTE: it is valid to have both a constructor activate and a method activate.
    // Here we separate out registering the listener from the constructor because
    // it is best to not escape this as a listener before the constructor is complete;
    // otherwise we risk the listener being called before the JVM has finalized the construction
    // of the object.
    @Activate
    protected void activate() {
        // use the system bundle so that it is ensured to see all bundle events
        Bundle systemBundle = this.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        BundleContext systemContext = systemBundle.getBundleContext();
        systemContext.addBundleListener(listener);
        systemContext.addBundleListener(gatewayUnresolvedListener);
    }

    @Deactivate
    protected void deactivate(ComponentContext cCtx) {
        generatorRefs.deactivate(cCtx);
        metaInfServicesRefs.deactivate(cCtx);
        Bundle systemBundle = this.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        BundleContext systemContext = systemBundle.getBundleContext();
        systemContext.removeBundleListener(listener);
        systemContext.removeBundleListener(gatewayUnresolvedListener);
        // Clear any open capture window so it does not carry into the next operation
        captureNotification = null;
        capturedAcls = null;
        capturedAppNames = null;
        this.cleanupRememberedBundles();
        this.resourceProviders.clear();
    }

    @Reference(name = REFERENCE_GENERATORS, service = ClassGenerator.class, cardinality = MULTIPLE, policy = DYNAMIC)
    protected void addGenerator(ServiceReference<ClassGenerator> ref) {
        generatorRefs.addReference(ref);
    }

    protected void removeGenerator(ServiceReference<ClassGenerator> ref) {
        generatorRefs.removeReference(ref);
    }

    @Reference(service = MetaInfServicesProvider.class, cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    protected void addMetaInfServicesProvider(ServiceReference<MetaInfServicesProvider> ref) {
        String path = (String) ref.getProperty("file.path");
        String implClassName = (String) ref.getProperty("implementation.class");

        metaInfServicesRefs.putReference(implClassName, ref);

        ConcurrentLinkedQueue<String> newList = new ConcurrentLinkedQueue<String>();
        ConcurrentLinkedQueue<String> oldList = metaInfServicesProviders.putIfAbsent(path, newList);
        (oldList == null ? newList : oldList).add(implClassName);
    }

    protected void removeMetaInfServicesProvider(ServiceReference<MetaInfServicesProvider> ref) {
        String path = (String) ref.getProperty("file.path");
        String implClassName = (String) ref.getProperty("implementation.class");

        ConcurrentLinkedQueue<String> list = metaInfServicesProviders.get(path);
        if (list != null)
            list.remove(implClassName);

        metaInfServicesRefs.removeReference(implClassName, ref);
    }

    @Reference(cardinality = MULTIPLE,
               policy = DYNAMIC,
               policyOption = GREEDY)
    protected void addResourceProvider(ResourceProvider rp) {
        resourceProviders.add(rp);
    }

    protected void removeResourceProvider(ResourceProvider rp) {
        resourceProviders.remove(rp);
    }

    @Override
    public AppClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration clConfig) {
        if (clConfig.getIncludeAppExtensions())
            addAppExtensionLibs(clConfig);
        AppClassLoader result = createAppClassLoader(new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig, getSystemTransformers())
            .setClassPath(classPath)
            .configure(gwConfig)
            .configure(clConfig));

        this.rememberBundle(result.getBundle());
        return result;
    }

    @Override
    public AppClassLoader createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration clConfig) {
        return createAppClassLoader(new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig, getSystemTransformers())
            .setSharedLibPath(classPath)
            .configure(createGatewayConfiguration())
            .useBundleAddOnLoader(gwClassLoader)
            .configure(clConfig));
    }

    @Override
    public AppClassLoader createChildClassLoader(List<Container> classPath, ClassLoaderConfiguration config) {
        if (config.getIncludeAppExtensions())
            addAppExtensionLibs(config);
        return createAppClassLoader(new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig, getSystemTransformers())
            .setClassPath(classPath)
            .configure(config));
    }

    private AppClassLoader createAppClassLoader(ClassLoaderFactory factory) {
        AppClassLoader loader = factory.create();
        synchronized(appClassLoaders) {
            appClassLoaders.add(loader);
        }
        return loader;
    }

    @Override
    public GatewayConfigurationImpl createGatewayConfiguration() {
        return new GatewayConfigurationImpl();
    }

    @Override
    public ClassLoaderConfigurationImpl createClassLoaderConfiguration() {
        return new ClassLoaderConfigurationImpl();
    }

    @Override
    public ClassLoaderIdentityImpl createIdentity(final String domain, final String id) {
        return new ClassLoaderIdentityImpl(domain, id);
    }

    @Override
    @FFDCIgnore(ClassCastException.class)
    public ShadowClassLoader getShadowClassLoader(ClassLoader loader) {
        try {
            return new ShadowClassLoader((AppClassLoader) loader);
        } catch (ClassCastException e) {
            return null;
        }
    }

    static class ClassFileTransformerAdapter implements ClassFileTransformer {
        private final ClassTransformer transformer;

        ClassFileTransformerAdapter(ClassTransformer transformer) {
            this.transformer = transformer;
        }

        @Override
        public byte[] transform(ClassLoader loader, String name, Class<?> classBeingRedefined, ProtectionDomain pd, byte[] bytes) throws IllegalClassFormatException {
            return transformer.transformClass(name, bytes, pd == null ? null : pd.getCodeSource(), loader);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ClassFileTransformerAdapter && ((ClassFileTransformerAdapter) o).transformer.equals(this.transformer);
        }

        @Override
        public int hashCode() {
            return this.transformer.hashCode();
        }
    }

    @Override
    public boolean registerTransformer(final ClassTransformer transformer, ClassLoader loader) {
        try {
            return ((AppClassLoader) loader).addTransformer(new ClassFileTransformerAdapter(transformer));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public boolean unregisterTransformer(ClassTransformer transformer, ClassLoader loader) {
        try {
            return ((AppClassLoader) loader).removeTransformer(new ClassFileTransformerAdapter(transformer));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public UnifiedClassLoader unify(ClassLoader parent, ClassLoader... followOns) {
        return new UnifiedClassLoader(parent, followOns);
    }

    @Override
    /**
     * Create or retrieve the shared class loader for a shared library that requires SPI visibility.
     * The resulting library SPI classloader instance is unique from that obtained using
     * {@link getSharedLibraryClassloader()}.  Both instances see the same binaries and API, and
     * both are managed similarly regarding library updates; only the SPI loader can see SPI packages.
     * The library SPI loader must not be a delegate of an application classloader.
     *
     * @param lib the shared library to create an SPI class loader for
     * @param ownerId a non-empty string used to create a purposeful class loader identity.
     *  e.g. "BELL"
     * @return the unique class loader for the provided library that requires SPI visibility.
     */
    public AppClassLoader getSharedLibrarySpiClassLoader(Library lib, final String ownerId) {

        if (ownerId == null || "".equals(ownerId)) {
            throw new IllegalArgumentException("Argument ownerId cannot be null or empty");
        }
        return getSharedLibraryClassLoader(lib, true, ownerId);
    }

    @Override
    public AppClassLoader getSharedLibraryClassLoader(Library lib) {

        return getSharedLibraryClassLoader(lib, false, null);
    }

    /**
     * Helper method to create or retrieve the class loader for a shared library.
     * 
     * @see #getSharedLibraryClassLoader(Library)
     * @see #getSharedLibrarySpiClassLoader(Library,boolean,String)
     */
    private AppClassLoader getSharedLibraryClassLoader(Library lib, boolean spiVisibility, String ownerId) {

        final String libId;
        final String libDomain;
        if (spiVisibility) {
            libId = lib.id() + ":" + ownerId;
            libDomain = SPI_SHARED_LIBRARY_DOMAIN;
        } else {
            libId = lib.id();
            libDomain = SHARED_LIBRARY_DOMAIN;
        }
        ClassLoaderIdentity clId = createIdentity(libDomain, libId);

        AppClassLoader loader = aclStore.retrieve(clId);
        if (loader != null)
            return loader;
        EnumSet<ApiType> apiTypeVisibility = lib.getApiTypeVisibility();

        ClassLoaderConfiguration clsCfg = createClassLoaderConfiguration()
                        // if the library is searched before app then we must use parentLast for the shared library
                        .setDelegateToParentAfterCheckingLocalClasspath(globalConfig.libraryPrecedence() == beforeApp)
                        .setId(clId)
                        .setSharedLibraries(lib.id()); // Configure lib binaries

        Collection<Fileset> filesets = lib.getFilesets();
        if (filesets != null && filesets.isEmpty() == false) {
            for (Fileset fileset : filesets) {
                setProtectionDomain(fileset.getFileset(), clsCfg);
            }
        } else {
            Collection<File> files = lib.getFiles();
            if (files != null && files.isEmpty() == false) {
                setProtectionDomain(files, clsCfg);
            }
        }

        GatewayConfiguration gwConfig = createGatewayConfiguration().setApplicationName(libDomain + ": " + libId)
                        .setDynamicImportPackage("*")
                        .setApiTypeVisibility(apiTypeVisibility);
        ((GatewayConfigurationImpl) gwConfig).setSpiVisibility(spiVisibility);

        AppClassLoader result = new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig, getSystemTransformers())
                        .configure(gwConfig)
                        .configure(clsCfg)
                        .onCreate(listenForLibraryChanges(lib.id())) // Listen for updates to lib
                        .getCanonical();

        this.rememberBundle(result.getBundle());
        return result;
    }

    private void setProtectionDomain(Collection<File> files, ClassLoaderConfiguration clsCfg) {
        for (File file : files) {
            String path = file.getPath();
            path = path.replace("\\", "/");

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "path: " + path);
            }
            String matchingDomainKey = getProtectionDomainMapKey(path);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "matchingDomainKey: " + matchingDomainKey);
            }
            if (matchingDomainKey != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Setting the protection domain");
                }

                clsCfg.setProtectionDomain(protectionDomainMap.get(matchingDomainKey));
                break;
            }
        }
    }

    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    public Map<String, ProtectionDomain> getProtectionDomainMap() {
        return protectionDomainMap;
    }

    /**
     * @param path
     * @return
     */
    public String getProtectionDomainMapKey(String path) {
        if (protectionDomainMap == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "protectionDomainMap is null");
            }
            return null;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "path: " + path);
            Tr.debug(tc, "protectionDomainMap.size = " + protectionDomainMap.size());
            for (Map.Entry<String, ProtectionDomain> entry : protectionDomainMap.entrySet()) {
                
                Tr.debug(tc, "Key (codeBase)= " + entry.getKey() + ", Value (protectionDomain) = " + entry.getValue());
            }
        }
        
        if (protectionDomainMap.containsKey(path)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "protectionDomainMap is not null, returning path: " + path);
            }
            return path;
        }

        for (String codebase : protectionDomainMap.keySet()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "codebase = " + codebase);
                Tr.debug(tc, "path = " + path);
            }
            if (codebase.endsWith("-")) {
                if (path.startsWith(codebase.substring(0, codebase.indexOf('-'))))
                    return codebase;
            } else if (codebase.endsWith("*")) {
                String temp = codebase.substring(0, codebase.indexOf('*') - 1);
                File jarFile = new File(path);
                String pathParent = jarFile.getParent().replace("\\", "/");;
                if (pathParent != null && pathParent.equalsIgnoreCase(temp))
                    return codebase;
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "nothing matched in the protectionDomainMap, returning null");
        }
        
        return null;
    }

    /** create an action that will create a listener when invoked */
    private PostCreateAction listenForLibraryChanges(final String libid) {
        return new PostCreateAction() {
            @Override
            public void invoke(AppClassLoader acl) {
                listenForLibraryChanges(libid, acl);
            }
        };
    }

    /**
     * Create a listener to remove a loader from the canonical store on library update.
     * Also registers the shared-library loader in {@code appClassLoaders} so the
     * UNRESOLVED capture callback can find it.
     */
    private void listenForLibraryChanges(String libid, AppClassLoader acl) {
        // Register in appClassLoaders so the introspector can list it and so the
        // aclStore eviction in gatewayUnresolvedListener can capture the ACL instance.
        synchronized (appClassLoaders) {
            appClassLoaders.add(acl);
        }
        // Capture the identity at listener-creation time. The ACL may be GC'd by the time
        // update() fires, but the identity is needed to determine whether to recycle an app.
        final ClassLoaderIdentity identity = acl.getKey();
        // ensure this loader is removed from the canonical store when the library is updated
        new WeakLibraryListener(libid, identity.getId(), acl, bundleContext) {
            @Override
            protected void update() {
                AppClassLoader cl = get();
                if (cl != null) {
                    aclStore.remove(cl);
                }
                deregister();

                // E3b: a fileset on-disk change fires notifyListeners(false) via FilesetListener
                // and reaches this update() callback. The gateway-UNRESOLVED capture
                // (gatewayUnresolvedListener) is not involved, so we must push the recycle here.
                // Only top-level module loaders represent a running application to recycle;
                // shared-library loaders (domain "Shared Library" / SPI) are eviction-only.
                if (isTopLevelAppDomain(identity.getDomain())) {
                    String appName = extractAppName(identity.getId());
                    ApplicationRecycleCoordinator coord = recycleCoordinator;
                    if (coord != null) {
                        coord.recycleApplications(Collections.singleton(appName));
                    }
                }
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(InterruptedException.class)
    /* re-thrown as an IllegalStateException and FFDC'd later */
    public ThreadContextClassLoader createThreadContextClassLoader(final ClassLoader applicationClassLoader) {
        final String methodName = "createThreadContextClassLoader(): ";
        if (applicationClassLoader == null) {
            throw new IllegalArgumentException("ClassLoader argument is null");
        }

        final String key;
        if (applicationClassLoader instanceof AppClassLoader) {
            key = ((AppClassLoader) applicationClassLoader).getKey().toString();
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Instance of LibertyClassLoader, key = " + key);
        } else if (applicationClassLoader instanceof BundleReference) {
            key = Long.toString(((BundleReference) applicationClassLoader).getBundle().getBundleId());
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Instance of BundleReference, key = " + key);
        } else {
            throw new IllegalArgumentException(applicationClassLoader.toString() + " + is an unexpected ClassLoader type");
        }

        ThreadContextClassLoader result;
        final ReentrantLock tcclStoreLock = tcclLockStore.getLock(key);
        try {
            if (tcclStoreLock.tryLock(TCCL_LOCK_WAIT, TimeUnit.MILLISECONDS)) {
                // using an anonymous inner class here for clarity - the object should be GCable as soon as the method call returns
                Factory<ThreadContextClassLoader> factory = new Factory<ThreadContextClassLoader>() {
                    @Override
                    public ThreadContextClassLoader createInstance() {
                        return ClassLoadingServiceImpl.this.createTCCL(applicationClassLoader, key);
                    }
                };

                do {
                    result = this.tcclStore.retrieveOrCreate(key, factory); 
                    if (!!!result.isFor(applicationClassLoader)) {
                        // this is a stale entry for a previous ClassLoader that had the same key
                        this.tcclStore.remove(result);
                        result = null;
                    }
                } while (result == null);

                result.incrementRefCount();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    leakDetectionMap.put(result, Thread.currentThread().getStackTrace());
                }
            } else {
                // could not acquire the tcclStoreLock
                throw new IllegalStateException("Unable to acquire TCCL store lock");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread interrupted while acquiring TCCL store lock");
        } finally {
            if (tcclStoreLock.isHeldByCurrentThread()) {
                tcclStoreLock.unlock();
            }
        }
        return result;
    }

    private ThreadContextClassLoader createTCCL(ClassLoader cl, String key) {

        /*
         * Always create a new TCCL to handle features being added/removed
         */

        GatewayConfiguration gwConfig = this.createGatewayConfiguration()
                        .setApplicationName("ThreadContextClassLoader")
                        .setDynamicImportPackage("*;thread-context=\"true\"")
                        .setDelegateToSystem(false);
        ClassLoaderConfiguration clConfig = this.createClassLoaderConfiguration().setId(createIdentity("Thread Context", key));
        GatewayBundleFactory gatewayBundleFactory = new GatewayBundleFactory(bundleContext, digraph, classloaders, globalConfig.jvmPackages());
        GatewayClassLoader aug = gatewayBundleFactory.createGatewayBundleClassLoader(gwConfig, clConfig, resourceProviders);

        ThreadContextClassLoader tccl;
        if (cl instanceof BundleReference) {
            tccl = new ThreadContextClassLoaderForBundles(aug, cl, key, this);
        } else {
            tccl = new ThreadContextClassLoader(aug, cl, key, this);
        }

        return tccl;
    }

    private void cleanupRememberedBundles() {
        final String methodName = "cleanupRememberedBundles(): ";
        for (WeakReference<Bundle> r : this.rememberedBundles.values()) {
            Bundle b = r.get();
            if (b != null) {
                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "Uninstalling bundle location: " + b.getLocation() + ", bundle id: " + b.getBundleId());
                    b.uninstall();
                } catch (BundleException ignored) {
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    private void forgetStaleBundles() {
        for (Object o = this.collectedBundles.poll(); o != null; o = this.collectedBundles.poll()) {
            this.rememberedBundles.values().remove(o);
        }
    }

    private void rememberBundle(Bundle bundle) {
        if (bundle == null)
            return;
        this.forgetStaleBundles();
        WeakReference<Bundle> oldRef = this.rememberedBundles.get(bundle.getLocation());

        // If the existing and new bundles are the same just return
        if ((oldRef != null) && (oldRef.get() == bundle)) {
            return;
        }
        WeakReference<Bundle> ref = new WeakReference<Bundle>(bundle, this.collectedBundles);
        oldRef = this.rememberedBundles.put(bundle.getLocation(), ref);
        // Explicitly forget the old bundle at this location
        if (oldRef != null) {
            oldRef.clear();
        }
    }

    @Override
    public void destroyThreadContextClassLoader(ClassLoader loader) {
        if (loader instanceof ThreadContextClassLoader) {
            ThreadContextClassLoader tccl = (ThreadContextClassLoader) loader;
            ReentrantLock tcclStoreLock = tcclLockStore.getLock(tccl.getKey());
            tcclStoreLock.lock();
            try {
                if (tccl.decrementRefCount() <= 0) {
                    this.tcclStore.remove(tccl);
                    leakDetectionMap.remove(tccl);
                }
            } finally {
                tcclStoreLock.unlock();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.classloading.ClassLoaderIdentityService#getClassLoaderIdentity(java.lang.ClassLoader)
     */
    @Override
    public String getClassLoaderIdentifier(ClassLoader classloader) throws IllegalArgumentException {
        // Disable for bundles, because the bundle id can change across instances of the server,
        // meaning the identifier would not be reliable
        if (classloader instanceof ThreadContextClassLoader && !(classloader instanceof ThreadContextClassLoaderForBundles)) {
            return ((ThreadContextClassLoader) classloader).getKey();
        } else if (classloader instanceof AppClassLoader) {
            return ((AppClassLoader) classloader).getKey().toString();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.classloading.ClassLoaderIdentityService#getClassLoader(com.ibm.wsspi.classloading.ClassLoaderIdentity)
     */
    @Override
    public ClassLoader getClassLoader(String identifier) throws IllegalArgumentException {
        if (identifier == null)
            return null;
        ThreadContextClassLoader classloader = tcclStore.retrieve(identifier);
        if (classloader != null) {
            return classloader;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassLoaderIdentifier(String type, String appName, String moduleName, String componentName) {

        String metadataId = metadataIdentifierService.getMetaDataIdentifier(type, appName, moduleName, componentName);
        MetaData metadata = metadataIdentifierService.getMetaData(metadataId);

        ClassLoader classLoader = metadataIdentifierService.getClassLoader(type, (ComponentMetaData) metadata);
        return getClassLoaderIdentifier(classLoader);
    }

    @Override
    public boolean isAppClassLoader(ClassLoader cl) {
        return cl instanceof AppClassLoader;
    }

    @Override
    public boolean isThreadContextClassLoader(ClassLoader cl) {
        return cl instanceof ThreadContextClassLoader;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.classloading.ClassLoadingService#setSharedLibraryProtectionDomains(java.util.Map)
     */
    @Override
    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    public void setSharedLibraryProtectionDomains(Map<String, ProtectionDomain> protectionDomainMap) {
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "protectionDomainMap.size = " + protectionDomainMap.size());
            for (Map.Entry<String, ProtectionDomain> entry : protectionDomainMap.entrySet()) {
                Tr.debug(tc, "Key (codeBase)= " + entry.getKey() + ", Value (protectionDomain) = " + entry.getValue());
            }

        }
        
        this.protectionDomainMap = protectionDomainMap;
    }

    public void setGlobalSharedLibrary(Library gsl) {
        this.globalSharedLibrary.set(gsl);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
    @Override
    public String getIntrospectorName() {
        return "ClassLoadingServiceIntrospector";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
    @Override
    public String getIntrospectorDescription() {
        return "ClassLoadingService diagnostics - leaked/active TCCLs, active resource providers, etc.";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#introspect(java.io.PrintWriter)
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {

        out.println("Resource Providers:");
        MultiMap<String, ResourceProvider> resourceProviderMap = resourceProviders.getProviderMap();
        for (String resName : resourceProviderMap.keys()) {
            out.println("  " + resName + " provided by:");
            for (ResourceProvider rp : resourceProviderMap.get(resName)) {
                out.println("    " + rp); //TODO: print bundle info from rp reflectively
            }
        }

        out.println();
        out.println();

        out.println("Gateway Loaders:");
        for (Entry<Bundle, Set<GatewayClassLoader>> entry : classloaders.entrySet()) {
            Bundle b = entry.getKey();
            out.println("  " + b.getSymbolicName());
            for (GatewayClassLoader gcl : entry.getValue()) {
                out.println("    " + gcl.getBundle().getSymbolicName() + " " + gcl.getApiTypeVisibility());
            }
        }

        out.println();
        out.println();

        Library gsl = globalSharedLibrary.get();
        if (gsl == null) {
            out.println("Global Shared Library not configured");
        } else {
            out.println("Global Shared Library contents:");
            out.println("  filesets:");
            for (Fileset fileset : gsl.getFilesets()) {
                String dir = fileset.getDir();
                for (File file : fileset.getFileset()) {
                    out.println("    " + dir + "  " + file.getPath());
                }
            }
            out.println("  folders:");
            for (File folder : gsl.getFolders()) {
                out.println("    " + folder.getAbsolutePath());
            }
            out.println("  files:");
            for (File file : gsl.getFiles()) {
                out.println("    " + file.getAbsolutePath());
            }
        }

        out.println();
        out.println();
        out.println("Existing appClassLoaders:");
        synchronized (appClassLoaders) {
            for (AppClassLoader loader : appClassLoaders) {
                out.println("  " + loader.toDiagString()); 
            }
        }

        out.println();
        out.println();
        out.println("Leaked (or active) TCCLs - note that tracing must be enabled to see these stacks:");
        for (Map.Entry<ClassLoader, StackTraceElement[]> entry : leakDetectionMap.entrySet()) {
            ClassLoader cl = entry.getKey();
            StackTraceElement[] stes = entry.getValue();
            out.println("  " + cl + " created via");
            for (StackTraceElement ste : stes) {
                out.println("    " + ste.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Called by {@link RuntimeUpdateManager} as each notification is created for an update.
     * When a {@code FEATURE_BUNDLES_RESOLVED} notification arrives and the current operation is a
     * ProvisioningMode.UPDATE (confirmed by the presence of an incomplete
     * {@code FEATURE_UPDATES_COMPLETED} notification), this method opens a capture window and
     * registers a completion listener that will run the transitive graph eviction and recycle
     * call once FeatureManager has finished provisioning.</p>
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (!RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED.equals(notification.getName())) {
            return; // only interested in FEATURE_BUNDLES_RESOLVED
        }

        // Confirm this is a ProvisioningMode.UPDATE, not a REFRESH.
        // FEATURE_UPDATES_COMPLETED is only created for UPDATE operations.
        RuntimeUpdateNotification fuc = updateManager.getNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
        if (fuc == null || fuc.isDone()) {
            return; // not an UPDATE operation, or already completed — nothing to do
        }

        // Open the capture window bound to this notification object.
        // Publish both sets before opening the window.
        capturedAcls = ConcurrentHashMap.<AppClassLoader> newKeySet();
        capturedAppNames = ConcurrentHashMap.<String> newKeySet();
        captureNotification = notification;

        // Register the recycle listener; it fires synchronously on the FeatureManager thread
        // when featureBundlesResolved.setResult(true) is called at the end of provisioning.
        notification.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                runRecycleListener();
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                // Close the window without recycling on failure path
                captureNotification = null;
                capturedAcls = null;
                capturedAppNames = null;
            }
        });
    }

    /**
     * Fires at {@code FEATURE_BUNDLES_RESOLVED} completion. Closes the capture window, computes
     * the transitive set of affected {@link AppClassLoader}s, evicts them from {@code aclStore},
     * and submits a single {@code recycleApplications} call for the affected application names.
     *
     * <p>Must not propagate any exception — a thrown exception would abort
     * {@code FeatureManager.update()}.</p>
     */
    private void runRecycleListener() {
        try {
            // Step 1: close window.
            captureNotification = null;

            // Step 2: take the captured ACLs and app names.
            Set<AppClassLoader> aclsToEvict = capturedAcls;
            capturedAcls = null;
            Set<String> appsToRecycle = capturedAppNames;
            capturedAppNames = null;

            if ((aclsToEvict == null || aclsToEvict.isEmpty()) && (appsToRecycle == null || appsToRecycle.isEmpty())) {
                return; // nothing to do
            }

            if (FrameworkState.isStopping()) {
                return; // server is going down; skip recycling
            }

            // Step 3: evict captured ACLs from aclStore.
            // App names are already in appsToRecycle (populated by the listener).
            // No delegate-expansion scan needed.
            if (aclsToEvict != null) {
                for (AppClassLoader acl : aclsToEvict) {
                    if (!aclStore.remove(acl) && tc.isDebugEnabled()) {
                        // Expected for a lingering loader that was already replaced in aclStore.
                        Tr.debug(tc, "Loader not removed from aclStore (already evicted or replaced): " + acl.getKey());
                    }
                }
            }

            // Step 4: recycle applications.
            if (appsToRecycle != null && !appsToRecycle.isEmpty()) {
                ApplicationRecycleCoordinator coord = recycleCoordinator;
                if (coord != null) {
                    coord.recycleApplications(appsToRecycle);
                } else if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "recycleCoordinator is absent; cannot recycle apps after feature update: " + appsToRecycle);
                }
            }
        } catch (Throwable t) {
            // Must not propagate — this runs on the FeatureManager thread inside setResult().
            // The enclosing catch is FFDC-instrumented, so the failure is recorded in the FFDC logs.
            Tr.event(tc, "Unexpected error in feature-update recycle listener", t);
        }
    }

    /**
     * Returns {@code true} for {@link ClassLoaderIdentity} domains that represent a running
     * application module whose app name can be recycled: {@code "WebModule"}, {@code "EJBModule"},
     * {@code "ClientModule"}, {@code "ConnectorModule"}, {@code "SpringModule"}.
     *
     * <p>Returns {@code false} for {@code "EARApplication"} (handled separately by id),
     * {@code "Shared Library"}, {@code "Shared Library SPI"}, and {@code "Thread Context"}.</p>
     */
    static boolean isTopLevelAppDomain(String domain) {
        return "WebModule".equals(domain)
            || "EJBModule".equals(domain)
            || "ClientModule".equals(domain)
            || "ConnectorModule".equals(domain)
            || "SpringModule".equals(domain);
    }

    /**
     * Extracts the application name from a {@link ClassLoaderIdentity#getId()} string.
     *
     * <p>For WAR, EJB, RAR, EAR sub-modules, and Spring applications the id is
     * {@code "appName#moduleName"}. Split on {@code #} and take the first segment.</p>
     */
    static String extractAppName(String id) {
        int hash = id.indexOf('#');
        return hash >= 0 ? id.substring(0, hash) : id;
    }

    private void addAppExtensionLibs(ClassLoaderConfiguration config) {
        for (ApplicationExtensionLibrary appExt : appExtLibs)
            config.addSharedLibraries(appExt.getReference().id());
    }

    @Override
    public boolean isThreadContextClassLoaderForAppClassLoader(ClassLoader tccl, ClassLoader appClassLoader) {
       if (! isThreadContextClassLoader(tccl)) {
           return false;
       }
       
       if (! isAppClassLoader(appClassLoader)) {
           return false;
       }
       
       ThreadContextClassLoader castedTCCL = (ThreadContextClassLoader) tccl;
       
       return castedTCCL.isFor(appClassLoader);
    }
}

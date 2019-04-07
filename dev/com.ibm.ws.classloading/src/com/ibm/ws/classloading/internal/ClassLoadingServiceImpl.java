/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.ClassLoadingConstants.SHARED_LIBRARY_DOMAIN;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.ClassTransformer;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.classloading.ResourceProvider;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.logging.Introspector;

@Component(service = { ClassLoadingService.class, LibertyClassLoadingService.class, ClassLoaderIdentifierService.class, Introspector.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class ClassLoadingServiceImpl implements LibertyClassLoadingService, ClassLoaderIdentifierService, Introspector {
    static final TraceComponent tc = Tr.register(ClassLoadingServiceImpl.class);
    private final Map<ClassLoader, StackTraceElement[]> leakDetectionMap = new HashMap<ClassLoader, StackTraceElement[]>();

    private static final int TCCL_LOCK_WAIT = Integer.getInteger("com.ibm.ws.classloading.tcclLockWaitTimeMillis", 15000);
    static final String REFERENCE_GENERATORS = "generators";

    private BundleContext bundleContext;
    private CanonicalStore<ClassLoaderIdentity, AppClassLoader> aclStore;
    private CanonicalStore<String, ThreadContextClassLoader> tcclStore;
    private final ReentrantLock tcclStoreLock = new ReentrantLock();
    private RegionDigraph digraph;
    private ClassRedefiner redefiner = new ClassRedefiner(null);
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

    private GlobalClassloadingConfiguration globalConfig;

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
    protected MetaDataIdentifierService metadataIdentifierService;

    /**
     * reference to the global library - primarily used for dump introspector output
     */
    private final AtomicReference<Library> globalSharedLibrary = new AtomicReference<>();
    
    @Reference
    protected void setGlobalClassloadingConfiguration(GlobalClassloadingConfiguration globalConfig) {
        this.globalConfig = globalConfig;
    }
    
    protected void unsetGlobalClassloadingConfiguration(GlobalClassloadingConfiguration globalConfig) {
        if(this.globalConfig == globalConfig) {
            this.globalConfig = null;
        }      
    }

    @Activate
    protected void activate(ComponentContext cCtx, Map<String, Object> properties) {
        generatorRefs.activate(cCtx);
        metaInfServicesRefs.activate(cCtx);
        this.bundleContext = cCtx.getBundleContext();
        this.aclStore = new CanonicalStore<ClassLoaderIdentity, AppClassLoader>();
        this.tcclStore = new CanonicalStore<String, ThreadContextClassLoader>();
        // use the system bundle so that it is ensured to see all bundle events
        Bundle systemBundle = this.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        BundleContext systemContext = systemBundle.getBundleContext();
        systemContext.addBundleListener(listener);
    }

    @Deactivate
    protected void deactivate(ComponentContext cCtx) {
        generatorRefs.deactivate(cCtx);
        metaInfServicesRefs.deactivate(cCtx);
        Bundle systemBundle = this.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        BundleContext systemContext = systemBundle.getBundleContext();
        systemContext.removeBundleListener(listener);
        this.bundleContext = null;
        this.cleanupRememberedBundles();
        this.aclStore = null;
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

    @Reference
    protected void setRegionDigraph(RegionDigraph digraph) {
        this.digraph = digraph;
    }

    protected void unsetRegionDigraph(RegionDigraph digraph) {}

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setInstrumentation(Instrumentation inst) {
        redefiner = new ClassRedefiner(inst);
    }

    protected void unsetInstrumentation(Instrumentation inst) {
        redefiner = null;
    }

    /**
     * Declarative Services method for setting the metadata identifier service.
     *
     * @param svc the service
     */
    @Reference(service = MetaDataIdentifierService.class, name = "metadataIdentifierService")
    protected void setMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = svc;
    }

    /**
     * Declarative Services method for unsetting the metadata identifier service.
     *
     * @param svc the service
     */
    protected void unsetMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = null;
    }

    @Reference(service = URLStreamHandlerService.class, target = "(url.handler.protocol=wsjar)")
    protected void setURLStreamHandlerService(URLStreamHandlerService svc) {
        // Declare a dependency on the URLStreamHandlerService so the wsjar protocol
        // doesn't go away while we still may still need it
    }

    protected void unsetURLStreamHandlerService(URLStreamHandlerService svc) {}

    @Override
    public AppClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration clConfig) {
        if (clConfig.getIncludeAppExtensions())
            addAppExtensionLibs(clConfig);
        AppClassLoader result = new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig)
                        .setClassPath(classPath)
                        .configure(gwConfig)
                        .configure(clConfig)
                        .create();

        this.rememberBundle(result.getBundle());
        return result;
    }

    @Override
    public AppClassLoader createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration clConfig) {
        return new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig)
                        .setSharedLibPath(classPath)
                        .configure(createGatewayConfiguration())
                        .useBundleAddOnLoader(gwClassLoader)
                        .configure(clConfig)
                        .create();
    }

    @Override
    public AppClassLoader createChildClassLoader(List<Container> classPath, ClassLoaderConfiguration config) {
        if (config.getIncludeAppExtensions())
            addAppExtensionLibs(config);
        return new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig)
                        .setClassPath(classPath)
                        .configure(config)
                        .create();
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
    public AppClassLoader getSharedLibraryClassLoader(Library lib) {

        ClassLoaderIdentity clId = createIdentity(SHARED_LIBRARY_DOMAIN, lib.id());

        AppClassLoader loader = aclStore.retrieve(clId);
        if (loader != null)
            return loader;
        EnumSet<ApiType> apiTypeVisibility = lib.getApiTypeVisibility();

        ClassLoaderConfiguration clsCfg = createClassLoaderConfiguration().setId(clId).setSharedLibraries(lib.id());

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

        AppClassLoader result = new ClassLoaderFactory(bundleContext, digraph, classloaders, aclStore, resourceProviders, redefiner, generatorManager, globalConfig)
                        .configure(createGatewayConfiguration().setApplicationName(SHARED_LIBRARY_DOMAIN + ": " + lib.id())
                                        .setDynamicImportPackage("*")
                                        .setApiTypeVisibility(apiTypeVisibility))
                        .configure(clsCfg)
                        .onCreate(listenForLibraryChanges(lib.id()))
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

        if (protectionDomainMap.containsKey(path)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "protectionDomainMap is not null, returning path: " + path);
            }
            return path;
        }

        for (String codebase : protectionDomainMap.keySet()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "codebase = " + codebase);
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

    /** create a listener to remove a loader from the canonical store on library update */
    private void listenForLibraryChanges(String libid, AppClassLoader acl) {
        // ensure this loader is removed from the canonical store when the library is updated
        new WeakLibraryListener(libid, acl.getKey().getId(), acl, bundleContext) {
            @Override
            protected void update() {
                Object cl = get();
                if (cl instanceof AppClassLoader && aclStore != null)
                    aclStore.remove((AppClassLoader) cl);
                deregister();
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
        try {
            if (tcclStoreLock.tryLock(TCCL_LOCK_WAIT, TimeUnit.MILLISECONDS)) {
                do {
                    result = this.tcclStore.retrieveOrCreate(key, new Factory<ThreadContextClassLoader>() {
                        @Override
                        public ThreadContextClassLoader createInstance() {
                            return ClassLoadingServiceImpl.this.createTCCL(applicationClassLoader, key);
                        }
                    }); // using an anonymous inner class here for clarity - the object should be GCable as soon as the method call returns
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
        GatewayBundleFactory gatewayBundleFactory = new GatewayBundleFactory(bundleContext, digraph, classloaders);
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
            tcclStoreLock.lock();
            try {
                ThreadContextClassLoader tccl = (ThreadContextClassLoader) loader;
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

    private void addAppExtensionLibs(ClassLoaderConfiguration config) {
        for (ApplicationExtensionLibrary appExt : appExtLibs)
            config.addSharedLibraries(appExt.getReference().id());
    }
}

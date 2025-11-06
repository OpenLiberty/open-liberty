/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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

import static com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.LibraryPrecedence.afterApp;
import static com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.LibraryPrecedence.beforeApp;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.AFTER_DELEGATES;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.BEFORE_DELEGATES;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.PARENT;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.SELF;
import static com.ibm.ws.classloading.internal.ClassLoadingConstants.LS;
import static com.ibm.ws.classloading.internal.LibertyLoader.DelegatePolicy.excludeParent;
import static com.ibm.ws.classloading.internal.LibertyLoader.DelegatePolicy.includeParent;
import static com.ibm.ws.classloading.internal.LibertyLoader.DelegatePolicy.searchedParent;
import static com.ibm.ws.classloading.internal.Util.freeze;
import static com.ibm.ws.classloading.internal.Util.list;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.LibraryPrecedence;
import com.ibm.ws.classloading.internal.providers.Providers;
import com.ibm.ws.classloading.internal.providers.Providers.LoaderInfo;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.classloading.internal.util.FeatureSuggestion;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;
import com.ibm.wsspi.library.Library;

/**
 * This class loader needs to be public in order for Spring's ReflectiveLoadTimeWeaver
 * to discover the special methods:
 */
public class AppClassLoader extends ContainerClassLoader implements SpringLoader {
    static final TraceComponent tc = Tr.register(AppClassLoader.class);

    private static final Set<String> forbiddenClassNames = Collections.unmodifiableSet( loadForbidden() );

    private static final String FORBIDDEN_PROPERTIES = "forbidden.properties";

    public static final String NOTHING_FORBIDDEN_PROPERTY = "io.openliberty.classloading.nothing.forbidden";

    /**
     * Load all available {@link #FORBIDDEN_PROPERTIES} resources, answering
     * property keys as forbidden class names.  Forbidden class names must be
     * specified as fully qualified class names.
     *
     * Load failures will result in either an empty collection of forbidden
     * class names, or partially loaded forbidden class names.  A null collection
     * will never be returned.  A load failure will not throw a non-runtime
     * exception.
     *
     * @return The keys of all available forbidden properties resources.
     */
    private static Set<String> loadForbidden() {
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Loading forbidden.properties");
        }

        if (Boolean.getBoolean(NOTHING_FORBIDDEN_PROPERTY)) {
            return Collections.emptySet();
        }

        Set<String> forbidden = new HashSet<>();
        try (InputStream inputStream = AppClassLoader.class.getResourceAsStream(FORBIDDEN_PROPERTIES)) {
            Properties props = new Properties();
            props.load(inputStream);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String, String> castProps = (Map) props;
            forbidden.addAll(castProps.keySet());
        } catch (IOException e) {
            // AutoFFDC
        }
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Loaded Forbidden Set" + forbidden);
        }
        return forbidden;
    }

    private static final PermissionCollection ALLPERMISSIONS;

    static {
        ClassLoader.registerAsParallelCapable();
        AllPermission allPerm = new AllPermission();
        ALLPERMISSIONS = allPerm.newPermissionCollection();
        if (ALLPERMISSIONS != null) {
            ALLPERMISSIONS.add(allPerm);
        }
    }

    enum SearchLocation {
        BEFORE_DELEGATES, PARENT, SELF, AFTER_DELEGATES
    };

    static final List<SearchLocation> PARENT_FIRST_SEARCH_ORDER = freeze(list(BEFORE_DELEGATES, PARENT, SELF, AFTER_DELEGATES));

    private final Set<String> packagesDefined = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()); 

    static final String CLASS_LOADING_TRACE_PREFIX = "com.ibm.ws.class.load.";
    static final String DEFAULT_PACKAGE = "default.package";
    /** per class loader collection of per-package trace components */
    final ConcurrentMap<String, TraceComponent> perPackageClassLoadingTraceComponents = new ConcurrentHashMap<String, TraceComponent>();

    private TraceComponent getClassLoadingTraceComponent(String pkg) {
        // tc will be null if this is the first time we used the default package or a package defined by another CL
        TraceComponent tc = perPackageClassLoadingTraceComponents.get(pkg);
        if (tc == null) {
            tc = Tr.register(CLASS_LOADING_TRACE_PREFIX + pkg, AppClassLoader.class, (String) null);
            perPackageClassLoadingTraceComponents.put(pkg, tc);
        }
        return tc;
    }

    protected final ClassLoaderConfiguration config;
    private final AtomicReference<List<Library>> overrideLibraries;
    private final AtomicReference<List<Library>> privateLibraries;
    private final Iterable<LibertyLoader> beforeAppDelegateLoaders;
    private final Iterable<LibertyLoader> afterAppDelegateLoaders;
    private final List<File> nativeLibraryFiles = new ArrayList<File>();
    private final List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();
    private final List<ClassFileTransformer> systemTransformers;
    private final DeclaredApiAccess apiAccess;
    private final ClassGenerator generator;
    private final ConcurrentHashMap<String, ProtectionDomain> protectionDomains = new ConcurrentHashMap<String, ProtectionDomain>();
    private final LibraryPrecedence libraryPrecedence;

    AppClassLoader(ClassLoader parent, ClassLoaderConfiguration config, List<Container> containers, DeclaredApiAccess access, ClassRedefiner redefiner, ClassGenerator generator, GlobalClassloadingConfiguration globalConfig, List<ClassFileTransformer> systemTransformers) {
        super(containers, parent, redefiner, globalConfig);
        this.libraryPrecedence = globalConfig.libraryPrecedence();
        this.systemTransformers = systemTransformers;
        this.config = config;
        this.apiAccess = access;
        for (Container container : config.getNativeLibraryContainers())
            addNativeLibraryContainer(container);

        this.overrideLibraries = new AtomicReference<>(Providers.getOverrideLibraries(config));
        this.privateLibraries = new AtomicReference<>(Providers.getPrivateLibraries(config));

        List<LibertyLoader> tmpBeforeApp = new ArrayList<>();
        List<LibertyLoader> tmpAfterApp = new ArrayList<>();
        for (LoaderInfo loaderInfo : Providers.getDelegateLoaders(config, apiAccess, libraryPrecedence)) {
            switch (loaderInfo.precedence) {
                case afterApp:
                    tmpAfterApp.add(loaderInfo.loader);
                    break;
                case beforeApp:
                    tmpBeforeApp.add(loaderInfo.loader);
                    break;
                default:
                    break;
            }
        }
        this.beforeAppDelegateLoaders = tmpBeforeApp.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(tmpBeforeApp);
        this.afterAppDelegateLoaders = tmpAfterApp.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(tmpAfterApp);
        this.generator = generator;
    }

    /** Provides the before delegate loaders so the {@link ShadowClassLoader} can mimic the structure. */
    Iterable<LibertyLoader> getBeforeAppDelegateLoaders() {
        return beforeAppDelegateLoaders;
    }

    /** Provides the before delegate loaders so the {@link ShadowClassLoader} can mimic the structure. */
    Iterable<LibertyLoader> getAfterAppDelegateLoaders() {
        return afterAppDelegateLoaders;
    }

    /** Provides the search order so the {@link ShadowClassLoader} can use it. */
    Iterable<SearchLocation> getSearchOrder() {
        return PARENT_FIRST_SEARCH_ORDER;
    }

    /**
     * Spring method to register the given ClassFileTransformer on this ClassLoader
     */
    @Override
    public boolean addTransformer(final ClassFileTransformer cft) {
        transformers.add(cft);

        // Also recursively register with parent(s), until a non-AppClassLoader or GlobalSharedLibrary loader is encountered.
        if (parent instanceof AppClassLoader) {
            if (Util.isGlobalSharedLibraryLoader(((AppClassLoader) parent))) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "addTransformer - skipping parent loader because it is a GlobalSharedLibrary");
                }
            } else {
                return ((AppClassLoader) parent).addTransformer(cft);
            }
        }
        return true;
    }

    /**
     * Spring method to obtain a throwaway class loader for this ClassLoader
     */
    @Override
    public ClassLoader getThrowawayClassLoader() {
        return new ShadowClassLoader(this);
    }

    @Override
    public EnumSet<ApiType> getApiTypeVisibility() {
        return apiAccess.getApiTypeVisibility();
    }

    @Override
    public URL getResource(String name) {
        URL result = findResourceCommonLibraryClassLoaders(name, beforeApp);
        if (result == null) {
            result = parent.getResource(name);
        }
        if (result == null) {
            result = findResource(name);
        }
        return result;
    }

    /**
     * Search order:
     * 1. This classloader.
     * 2. The common library classloaders.
     *
     * Note: the method is marked 'final' so that derived classes (such as ParentLastClassLoader)
     * don't override this method and lose the common library classloader support.
     * For finding directories in jar: scheme URLs we need to add a / to the end of the name passed
     * and strip the / from the resulting URL.
     */
    @Override
    @Trivial
    public final URL findResource(String name) {
        return findResourceInternal(name, false);
    }

    @Override
    @Trivial
    protected URL delegateFindResource(String name) {
        return findResourceInternal(name, true);
    }

    private URL findResourceInternal(String name, boolean delegate) {
        URL result = null;
        Object token = ThreadIdentityManager.runAsServer();
        try {
            if (delegate) {
                result = findResourceCommonLibraryClassLoaders(name, beforeApp);
            }
            if (result == null) {
                result = super.findResource(name);
            }
            if (result == null) {
                result = findResourceCommonLibraryClassLoaders(name, afterApp);
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }
        return result;
    }

    /**
     * Search order:
     * 1. This classloader.
     * 2. The common library classloaders.
     *
     * Note: For finding directories in jar: scheme URLs we need to add a / to the end of the name passed
     * and strip the / from the resulting URL. We also need to handle duplicates.
     * We need to use a resourceMap with string keys as hashcode and equals of URL are expensive.
     */
    @Override
    @Trivial
    public CompositeEnumeration<URL> findResources(String name) throws IOException {
        return findResourcesInternal(name, false);
    }

    @Override
    protected Enumeration<URL> delegateFindResources(String name) throws IOException {
        return findResourcesInternal(name, true);
    }
    @Trivial
    private CompositeEnumeration<URL> findResourcesInternal(String name, boolean delegate) throws IOException {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            CompositeEnumeration<URL> enumerations = new CompositeEnumeration<URL>();
            if (delegate) {
                findResourcesCommonLibraryClassLoaders(name, enumerations, beforeApp);
            }
            enumerations.add(super.findResources(name));
            return findResourcesCommonLibraryClassLoaders(name, enumerations, afterApp);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResourcesCommonLibraryClassLoaders(name, new CompositeEnumeration<>(), beforeApp) //
                        .add(this.parent.getResources(name)) //
                        .add(this.findResources(name));
    }

    /** Returns the Bundle of the Top Level class loader */
    @Override
    public Bundle getBundle() {
        return parent instanceof GatewayClassLoader ? ((GatewayClassLoader) parent).getBundle() : parent instanceof LibertyLoader ? ((LibertyLoader) parent).getBundle() : null;
    }

    boolean removeTransformer(ClassFileTransformer transformer) {
        // Also recursively remove from parent(s), until a non-AppClassLoader is encountered.
        if (parent instanceof AppClassLoader) {
            ((AppClassLoader) parent).removeTransformer(transformer);
        }
        return this.transformers.remove(transformer);
    }

    /**
     * @{inheritDoc
     *
     *              Search order:
     *              1. This classloader.
     *              2. The common library classloaders.
     *
     *              Note: the method is marked 'final' so that derived classes (such as ParentLastClassLoader)
     *              don't override this method and lose the common library classloader support.
     */
    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    protected final Class<?> findClass(String name, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        String resourceName = Util.convertClassNameToResourceName(name);
        ByteResourceInformation byteResInfo = findClassBytes(name, resourceName);
        if (byteResInfo == null) {
            // Check the common libraries.
            return findClassCommonLibraryClassLoaders(name, returnNull, afterApp, delegatePolicy);
        }

        if (isParentFirst() && delegatePolicy != searchedParent && parent != null) {
            // This loader is parent first but was delegated to without first checking the parent;
            // Check now before allowing the class to be defined in this loader's class space.
            Class<?> checkParentResult = null;
            if (parent instanceof NoClassNotFoundLoader) {
                checkParentResult = ((NoClassNotFoundLoader) parent).loadClassNoException(name);
            } else {
                try {
                    checkParentResult = parent.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // move on to defining the local class for this loader
                }
            }
            if (checkParentResult != null) {
                return checkParentResult;
            }
        }
        byte[] bytes = transformers.isEmpty() && systemTransformers.isEmpty() ?
                        byteResInfo.getBytes() : transformClassBytes(name, byteResInfo);

        return definePackageAndClass(name, resourceName, byteResInfo, bytes);
    }

    byte[] transformClassBytes(String name, ByteResourceInformation toTransform) throws ClassNotFoundException {
        byte[] originalBytes;
        boolean fromSCC = toTransform.foundInClassCache();
        if (!fromSCC) {
            originalBytes = toTransform.getBytes();
        } else {
            // Since the class was stored to the shared class cache (SCC), it was not transformed previously.
            // Transforming the actual class bytes ensures the expected bytes and alleviates the need for the
            // user to delete the SCC when there is a change requiring the class be transformed.
            // By not transforming bytes loaded from the SCC we avoid numerous types of exceptions or quitting
            // out of the transformation code, which will result in the class not being transformed anyway.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to transform \" + name + \" found in shared class cache because we have "
                         + (!transformers.isEmpty() ? "[transformers]" : "")
                         + (!systemTransformers.isEmpty() ? "[system transformers]" : ""));
            }
            try {
                originalBytes = toTransform.getActualBytes();
            } catch (IOException e) {
                // auto FFDC
                // fallback to the cached class
                return toTransform.getBytes();
            }
        }
        byte[] bytes = transformClassBytes(originalBytes, name, transformers);
        bytes = transformClassBytes(bytes, name, systemTransformers);
        if (fromSCC) {
            // If the transform didn't change the bytes, then return the original bytes
            // returned from the shared classes cache.
            if (bytes == originalBytes || Arrays.equals(bytes, originalBytes)) {
                bytes = toTransform.getBytes();
            }
        }
        return bytes;
    }

    private byte[] transformClassBytes(final byte[] originalBytes, String name, List<ClassFileTransformer> cfts) throws ClassNotFoundException {
        byte[] bytes = originalBytes;
        for (ClassFileTransformer transformer : cfts) {
            bytes = doTransformation(name, bytes, transformer);
        }
        return bytes;
    }

    private byte[] doTransformation(String name, byte[] bytes, ClassFileTransformer transformer) throws ClassNotFoundException {
        try {
            byte[] newBytes = transformer.transform(this, name, null, config.getProtectionDomain(), bytes);
            if (newBytes != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (bytes == newBytes)
                        Tr.debug(tc, "transformer " + transformer + " was invoked but returned an unaltered byte array");
                    else
                        Tr.debug(tc, "transformer " + transformer + " successfully transformed the class bytes");
                }
                bytes = newBytes;
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "transformer " + transformer + " was invoked but did not alter the loaded bytes");
            }
        } catch (IllegalClassFormatException ex) {
            // FFDC
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "bad transform - transformer: " + transformer + " attempting to transform class: " + name, ex);
            }
            throw new ClassNotFoundException(name, ex);
        }
        return bytes;
    }

    /*
     * Update the PermissionCollection for the Protection domain if it isn't set.
     */
    private ProtectionDomain setPermissionCollectionIfNeeded(ProtectionDomain pd) {
        java.security.PermissionCollection pc = null;
        if (pd.getPermissions() == null) {
            if (System.getSecurityManager() == null) {
                // No need to do anything else when there is no security manager.
                // This handles cases where the security manager isn't supported (e.g. Java 24).
                pc = ALLPERMISSIONS;
            } else {
                try {
                    pc = AccessController.doPrivileged(new PrivilegedExceptionAction<java.security.PermissionCollection>() {
                        @Override
                        public java.security.PermissionCollection run() {
                            java.security.Policy p = java.security.Policy.getPolicy();
                            java.security.PermissionCollection fpc = p.getPermissions(pd.getCodeSource());
                            return fpc;
                        }
                    });
                } catch (PrivilegedActionException paex) {
                } 
            }
        }
        return pc == null ? pd : new ProtectionDomain(pd.getCodeSource(), pc);
    }
    
    private Class<?> definePackageAndClass(final String name, String resourceName, final ByteResourceInformation byteResourceInformation, byte[] bytes) throws ClassFormatError {
        // Now define a package for this class if it has one
        int lastDotIndex = name.lastIndexOf('.');
        String packageName = DEFAULT_PACKAGE;
        if (lastDotIndex != -1) {
            packageName = name.substring(0, lastDotIndex);
            definePackage(byteResourceInformation, packageName);
        }

        ProtectionDomain pd = getClassSpecificProtectionDomain(byteResourceInformation.getContainerURL());
        pd = setPermissionCollectionIfNeeded(pd);

        Class<?> clazz = null;
        try {
            clazz = defineClass(name, bytes, 0, bytes.length, pd);
        } finally {
            final TraceComponent cltc;
            if (TraceComponent.isAnyTracingEnabled() && (cltc = getClassLoadingTraceComponent(packageName)).isDebugEnabled()) {
                String loc = byteResourceInformation.getContainerURL().toString();
                String message = clazz == null ? "CLASS FAIL" : "CLASS LOAD";
                Tr.debug(cltc, String.format("%s: [%s] [%s] [%s]", message, getKey(), loc, name));
            }
        }
        byteResourceInformation.storeInClassCache(clazz, bytes);
        return clazz;
    }

    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    private ProtectionDomain getClassSpecificProtectionDomain(final ContainerURL containerUrl) {
        if (containerUrl == null) {
            // not expected; there will have been some FFDCs if this is null
            return config.getProtectionDomain();
        }
        ProtectionDomain pd = null;
        try {
            pd = AccessController.doPrivileged(new PrivilegedExceptionAction<ProtectionDomain>() {
                @Override
                public ProtectionDomain run() {
                    return getClassSpecificProtectionDomainPrivileged(containerUrl);
                }
            });
        } catch (PrivilegedActionException paex) {
            //auto FFDC
            pd = config.getProtectionDomain();
        }
        return pd;

    }

    ProtectionDomain getClassSpecificProtectionDomainPrivileged(ContainerURL containerUrl) {
        return protectionDomains.computeIfAbsent(containerUrl.urlString, (c) -> {
            ProtectionDomain pdFromConfig = config.getProtectionDomain();
            CodeSource cs = new CodeSource(containerUrl.url, pdFromConfig.getCodeSource().getCertificates());
            return new ProtectionDomain(cs, pdFromConfig.getPermissions());
        });
    }

    /**
     * This method will define the package using the byteResourceInformation for a class to get the URL for this package to try to load a manifest. If a manifest can't be loaded
     * from the URL it will create the package with no package versioning or sealing information.
     *
     * @param byteResourceInformation The information about the class file
     * @param packageName The name of the package to create
     */
    private void definePackage(ByteResourceInformation byteResourceInformation, String packageName) {

        // Using packagesDefined instead of getPackage since getPackage has a lot of path length
        // and in the race condition we avoid all the Manifest length.
        if (!packagesDefined.contains(packageName)) {
            synchronized (getClassLoadingLock(packageName)) {
                // have to check again
                if (!packagesDefined.contains(packageName)) {
                    byteResourceInformation.definePackage(packageName, this);
                    packagesDefined.add(packageName);
                }
            }
        }
    }

    @Override
    @Trivial
    protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, includeParent, false);
    }

    @Override
    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    protected final Class<?> loadClass(String name, boolean resolve, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        // Fail classes which are forbidden.  For example, by a CVE.
        if ( forbiddenClassNames.contains(name) ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "loadClass " + name + " forbidden");
            }
            // Since this is not a usual class-not-found case,
            // directly throw a simple CNFE.
            //
            // A more detailed message might be displayed, but,
            // in most cases the JVM is going to catch the CNFE,
            // forget the message and then throw a NoClassDefFoundError
            // with only the class name.  Having a more detailed
            // message adds little value outside of direct calls
            // to 'Class.forName' and 'ClassLoader.loadClass'.

            if (returnNull) {
                return null;
            }
            throw new ClassNotFoundException(name);
        }

        ClassNotFoundException cnfe = null;
        Object token = ThreadIdentityManager.runAsServer();
        try {
            Class<?> result = findOrDelegateLoadClass(name, delegatePolicy, returnNull);
            if (result != null) {
                return result;
            }
        } catch (ClassNotFoundException e) {
            cnfe = e;
        } finally {
            ThreadIdentityManager.reset(token);
        }

        // Could not generate class - throw CNFE
        // Even if going to return null, still call getExceptionWithSuggestion so that
        // the appropriate info message is output to the message.log.
        // If onlySeardchSelf this is a delegation in which case we do NOT want to log a feature suggestion.
        // Doing so will cause the message to get logged before parent/gateway delegation when using parentLast delegation
        ClassNotFoundException toThrow = delegatePolicy == includeParent ? FeatureSuggestion.getExceptionWithSuggestion(cnfe, name, returnNull) : cnfe;

        if (returnNull) {
            return null;
        }

        throw toThrow;
    }

    @Trivial
    Class<?> generateClass(String name) throws ClassNotFoundException {
        Class<?> generatedClass = null;
        if (generator != null) {
            byte[] bytes = generator.generateClass(name, this);
            if (bytes != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "defining generated class " + name);


                String remoteInterfaceClassName = null;
                ProtectionDomain pd = null;

                if (name != null && name.endsWith("_Stub") && !name.endsWith("._Stub"))
                {

                    StringBuilder nameBuilder = new StringBuilder(name);
                    nameBuilder.setLength(nameBuilder.length() - 5);

                    int packageOffset = nameBuilder.lastIndexOf(".") + 1;
                    if (nameBuilder.charAt(packageOffset) == '_')
                        nameBuilder.deleteCharAt(packageOffset);


                    remoteInterfaceClassName = nameBuilder.toString();

                    try {
                        Class<?> remoteClass = super.loadClass(remoteInterfaceClassName, false);

                        pd = AccessController.doPrivileged(new PrivilegedExceptionAction<java.security.ProtectionDomain>() {
                            @Override
                            public java.security.ProtectionDomain run() {
                                return remoteClass.getProtectionDomain();
                            }
                        });
                    } catch (PrivilegedActionException paex) {                 
                    }

                    pd = setPermissionCollectionIfNeeded(pd);
                    generatedClass = defineClass(name, bytes, 0, bytes.length, pd);

                } else {
                    generatedClass = defineClass(name, bytes, 0, bytes.length, config.getProtectionDomain());
                }
          }
        }
        return generatedClass;
    }

    /**
     * Find a class on this loader's class path or delegate to the parent class
     * loader.
     */
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> findOrDelegateLoadClass(String name, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        final boolean RETURN_NULL_FOR_NO_CLASS = true;
        Class<?> beforeAppLoad = findClassCommonLibraryClassLoaders(name, RETURN_NULL_FOR_NO_CLASS, beforeApp, delegatePolicy);
        if (beforeAppLoad != null) {
            return beforeAppLoad;
        }

        // parent is really only null for unit tests
        if (parent == null) {
            return super.loadClass(name, false);
        }
        Class<?> result = null;
        ClassNotFoundException findException = null;
        synchronized (getClassLoadingLock(name)) {
            result = findLoadedClass(name);
            if (result == null) {
                if (delegatePolicy == includeParent) {
                    if (parent instanceof NoClassNotFoundLoader) {
                        result = ((NoClassNotFoundLoader) parent).loadClassNoException(name);
                    } else {
                        try {
                            result = parent.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            // move on to local findClass
                        }
                    }
                    delegatePolicy = searchedParent;
                }
                if (result == null) {
                    try {
                        result = findClass(name, delegatePolicy, returnNull);
                    } catch (ClassNotFoundException cnfe) {
                        findException = cnfe;
                    }

                    if (result == null) {
                        // The class could not be found on the local class path or by
                        // delegating to parent/library class loaders.  Try to generate it.
                        result = generateClass(name);
                    }
                }
            }
        }

        if (result != null || returnNull) {
            return result;
        }

        throw findException;
    }

    @Trivial
    private Iterable<LibertyLoader> getDelegates(LibraryPrecedence precedence) {
        if (precedence == LibraryPrecedence.beforeApp) {
            return beforeAppDelegateLoaders;
        }
        return afterAppDelegateLoaders;
    }
    /**
     * Search for the class using the common library classloaders.
     *
     * @param name The class name.
     *
     * @return The class, if found.
     *
     * @throws ClassNotFoundException if the class isn't found.
     */
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> findClassCommonLibraryClassLoaders(String name, boolean returnNull, LibraryPrecedence precedence, DelegatePolicy fromDelegation) throws ClassNotFoundException {
        DelegatePolicy delegatePolicy;
        if (fromDelegation == searchedParent) {
            // parent already searched 
            delegatePolicy = searchedParent;
        } else {
            delegatePolicy = excludeParent;
        }
        for (LibertyLoader cl : getDelegates(precedence)) {
            try {
                Class<?> rc = cl.loadClass(name, false, delegatePolicy, true);
                if (rc != null) {
                    return rc;
                }
            } catch (ClassNotFoundException e) {
                // Ignore. Will throw at the end if class is not found.
            }
        }
        // If we reached here, then the class was not loaded.
        if (returnNull) {
            return null;
        }
        throw new ClassNotFoundException(name);
    }

    @Trivial
    protected boolean isParentFirst() {
        return true;
    }

    /**
     * Search for the resource using the common library classloaders.
     *
     * @param name The resource name.
     *
     * @return The resource, if found. Otherwise null.
     */
    protected URL findResourceCommonLibraryClassLoaders(String name, LibraryPrecedence precedence) {
        for (LibertyLoader cl : getDelegates(precedence)) {
            URL url = cl.delegateFindResource(name);
            if (url != null) {
                return url;
            }
        }
        // If we reached here, then the resource was not found.
        return null;
    }

    /**
     * Search for the resources using the common library classloaders.
     *
     * @param name The resource name.
     * @param enumerations A CompositeEnumeration<URL>, which is populated by this method.
     *
     * @return The enumerations parameter is populated by this method and returned. It contains
     *         all the resources found under all the common library classloaders.
     */
    protected CompositeEnumeration<URL> findResourcesCommonLibraryClassLoaders(String name, CompositeEnumeration<URL> enumerations, LibraryPrecedence precedence) throws IOException {
        for (LibertyLoader cl : getDelegates(precedence)) {
            enumerations.add(cl.delegateFindResources(name));
        }
        return enumerations;
    }

    @Override
    protected void lazyInit() {
        // process all the override and private libraries

        List<Library> curOverrideLibraries = overrideLibraries.getAndSet(null);
        if (curOverrideLibraries != null) {
            for (Library lib : curOverrideLibraries) {
                copyLibraryElementsToClasspath(lib, true);
            }
        }

        List<Library> curPrivateLibraries = privateLibraries.getAndSet(null);
        if (curPrivateLibraries != null) {
            for (Library lib : curPrivateLibraries) {
                copyLibraryElementsToClasspath(lib, false);
            }
        }
    }

    /**
     * Takes the Files and Folders from the Library
     * and adds them to the various classloader classpaths
     *
     * @param library
     */
    private void copyLibraryElementsToClasspath(Library library, boolean prepend) {
        Collection<File> files = library.getFiles();
        addToClassPath(library.getContainers(), prepend);
        if (files != null && !!!files.isEmpty()) {
            for (File file : files) {

                nativeLibraryFiles.add(file);
            }
        }

        for (Fileset fileset : library.getFilesets()) {
            for (File file : fileset.getFileset()) {

                nativeLibraryFiles.add(file);
            }
        }
    }

    /**
     * Determine if it's a windows library file name (ends with ".dll").
     *
     * @param basename The file name.
     *
     * @return true if it's a windows library name (ends with ".dll").
     */
    private static boolean isWindows(String basename) {
        return (basename.endsWith(".dll") || basename.endsWith(".DLL"));
    }

    /**
     * Check if the given file's name matches the given library basename.
     *
     * @param f The file to check.
     * @param basename The basename to compare the file against.
     *
     * @return true if the file exists and its name matches the given basename.
     *         false otherwise.
     */
    private static boolean checkLib(final File f, String basename) {
        boolean fExists = System.getSecurityManager() == null ? f.exists() : AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return f.exists();
            }
        });
        return fExists &&
                        (f.getName().equals(basename) || (isWindows(basename) && f.getName().equalsIgnoreCase(basename)));
    }

    @Override
    protected String findLibrary(String libname) {
        if (libname == null || libname.length() == 0) {
            return null;
        }

        String path = super.findLibrary(libname);
        if (path != null) {
            return path;
        }

        Object token = ThreadIdentityManager.runAsServer();
        try {
            String psLibname = System.mapLibraryName(libname); // platform specific.
            for (File f : nativeLibraryFiles) {
                if (checkLib(f, psLibname)) {
                    return f.getAbsolutePath();
                }
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }

        return null; // not found.
    }

    @Override
    public ClassLoaderIdentity getKey() {
        return config.getId();
    }

    public String toDiagString() {
        StringBuilder sb = new StringBuilder();
        sb.append(config).append(LS);

        sb.append("    API Visibility: ");
        for (ApiType type : apiAccess.getApiTypeVisibility()) {
            sb.append(type).append(" ");
        }
        sb.append(LS);

        sb.append("    ClassPath: ").append(LS);
        for (Collection<URL> containerURLs : getClassPath()) {
            sb.append("      * ");
            for (URL url : containerURLs) {
                sb.append(url.toString()).append(" | ");
            }
            sb.append(LS);
        }
        sb.append(LS);

        sb.append("    CodeSources: ");
        for (Map.Entry<String, ProtectionDomain> entry : protectionDomains.entrySet()) {
            sb.append(LS).append("      ").append(entry.getKey()).append(" = ")
            .append(entry.getValue().getCodeSource().getLocation());
        }
        sb.append(LS);

        return sb.toString();
    }

    @Override
    public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
        return defineClass(name, b, 0, b.length, protectionDomain);
    }
}

/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.DELEGATES;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.PARENT;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.SELF;
import static com.ibm.ws.classloading.internal.Util.freeze;
import static com.ibm.ws.classloading.internal.Util.list;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.url.WSJarURLConnection;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.providers.Providers;
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
    static {
        ClassLoader.registerAsParallelCapable();
    }
    static final TraceComponent tc = Tr.register(AppClassLoader.class);

    enum SearchLocation {
        PARENT, SELF, DELEGATES
    };

    static final List<SearchLocation> PARENT_FIRST_SEARCH_ORDER = freeze(list(PARENT, SELF, DELEGATES));

    static final String CLASS_LOADING_TRACE_PREFIX = "com.ibm.ws.class.load.";
    static final String DEFAULT_PACKAGE = "default.package";
    /** per class loader collection of per-package trace components */
    final ConcurrentMap<String, TraceComponent> perPackageClassLoadingTraceComponents = new ConcurrentHashMap<String, TraceComponent>();

    private TraceComponent registerClassLoadingTraceComponent(String pkg) {
        TraceComponent tc = Tr.register(CLASS_LOADING_TRACE_PREFIX + pkg, AppClassLoader.class, (String) null);
        perPackageClassLoadingTraceComponents.put(pkg, tc);
        return tc;
    }

    private TraceComponent getClassLoadingTraceComponent(String pkg) {
        TraceComponent tc = perPackageClassLoadingTraceComponents.get(pkg);
        // tc will be null if this is the first time we used the default package or a package defined by another CL
        return tc == null ? registerClassLoadingTraceComponent(pkg) : tc;
    }

    protected final ClassLoaderConfiguration config;
    private volatile List<Library> privateLibraries;
    private final Iterable<LibertyLoader> delegateLoaders;
    private final List<File> nativeLibraryFiles = new ArrayList<File>();
    private final List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();
    private final DeclaredApiAccess apiAccess;
    private final ClassGenerator generator;
    private final ConcurrentHashMap<String, ProtectionDomain> protectionDomains = new ConcurrentHashMap<String, ProtectionDomain>();
    protected final ClassLoader parent;

    AppClassLoader(ClassLoader parent, ClassLoaderConfiguration config, List<Container> containers, DeclaredApiAccess access, ClassRedefiner redefiner, ClassGenerator generator, GlobalClassloadingConfiguration globalConfig) {
        super(containers, parent, redefiner, globalConfig);
        this.parent = parent;
        this.config = config;
        this.apiAccess = access;
        for (Container container : config.getNativeLibraryContainers())
            addNativeLibraryContainer(container);
        this.privateLibraries = Providers.getPrivateLibraries(config);
        this.delegateLoaders = Providers.getDelegateLoaders(config, apiAccess);
        this.generator = generator;
    }

    /** Provides the delegate loaders so the {@link ShadowClassLoader} can mimic the structure. */
    Iterable<LibertyLoader> getDelegateLoaders() {
        return delegateLoaders;
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
    public final URL findResource(String name) {
        URL result = null;
        Object token = ThreadIdentityManager.runAsServer();
        try {
            result = super.findResource(name);
            if (result == null) {
                result = findResourceCommonLibraryClassLoaders(name);
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
        Object token = ThreadIdentityManager.runAsServer();
        try {
            CompositeEnumeration<URL> enumerations = new CompositeEnumeration<URL>(super.findResources(name));
            return findResourcesCommonLibraryClassLoaders(name, enumerations);
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
        /*
         * The default implementation of getResources never calls getResources on its parent, instead it just calls findResources on all of the loaders parents. We know that our
         * parent will be a gateway class loader that changes the order that resources are loaded but it does this in getResources (as that is where the order *should* be changed
         * according to the JavaDoc). Therefore call getResources on our parent and then findResources on ourself.
         */
        // Note we don't need to worry about getSystemResources as our parent will do that for us
        try {
            final String f_name = name;
            final ClassLoader f_parent = parent;

            Enumeration<URL> eURL = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Enumeration<URL>>() {
                @Override
                public Enumeration<URL> run() throws Exception {
                    return f_parent.getResources(f_name);
                }
            });

            return new CompositeEnumeration<URL>(eURL).add(this.findResources(name));

        } catch (PrivilegedActionException pae) {
            return null;
        }
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
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        if (transformers.isEmpty()) {
            Class<?> clazz = null;
            Object token = ThreadIdentityManager.runAsServer();
            try {
                synchronized (getClassLoadingLock(name)) {
                    // This method may be invoked directly instead of via loadClass
                    // (e.g. when doing a "shallow" scan of the common library classloaders).
                    // So we first must check whether we've already defined/loaded the class.
                    // Otherwise we'll get a LinkageError on the second findClass call because
                    // it will attempt to define the class a 2nd time.
                    clazz = findLoadedClass(name);
                    if (clazz == null) {
                        ByteResourceInformation byteResInfo = this.findClassBytes(name);
                        clazz = definePackageAndClass(name, byteResInfo, byteResInfo.getBytes());
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                // Check the common libraries.
                clazz = findClassCommonLibraryClassLoaders(name);
            } finally {
                ThreadIdentityManager.reset(token);
            }
            return clazz;
        }

        ByteResourceInformation byteResourceInformation;
        try {
            byteResourceInformation = findClassBytes(name);
        } catch (ClassNotFoundException cnfe) {
            // Check the common libraries.
            return findClassCommonLibraryClassLoaders(name);
        }

        byte[] bytes = transformClassBytes(byteResourceInformation.getBytes(), name);
        

        return definePackageAndClass(name, byteResourceInformation, bytes);
    }

    byte[] transformClassBytes(final byte[] originalBytes, String name) throws ClassNotFoundException {
        byte[] bytes = originalBytes;
        for (ClassFileTransformer transformer : transformers) {
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
        }
        return bytes;
    }

    private Class<?> definePackageAndClass(final String name, final ByteResourceInformation byteResourceInformation, byte[] bytes) throws ClassFormatError {
        final TraceComponent cltc;
        // Now define a package for this class if it has one
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex != -1) {
            String packageName = name.substring(0, lastDotIndex);

            // See if this package is already defined, we will handle multi-threaded code with a try catch later
            if (this.getPackage(packageName) == null) {
                definePackage(byteResourceInformation, packageName);
                cltc = registerClassLoadingTraceComponent(packageName);
            } else {
                cltc = getClassLoadingTraceComponent(packageName);
            }
        } else {
            cltc = getClassLoadingTraceComponent(DEFAULT_PACKAGE);
        }

        URL resourceURL = byteResourceInformation.getResourceUrl();
        ProtectionDomain pd = getClassSpecificProtectionDomain(name, resourceURL);

        Class<?> clazz = null;
        try {
            clazz = defineClass(name, bytes, 0, bytes.length, pd);
        } finally {
            if (cltc.isDebugEnabled()) {
                String loc = "" + byteResourceInformation.getResourceUrl();
                String path = byteResourceInformation.getResourcePath();
                if (loc.endsWith(path))
                    loc = loc.substring(0, loc.length() - path.length());
                if (loc.endsWith("!/"))
                    loc = loc.substring(0, loc.length() - 2);
                String message = clazz == null ? "CLASS FAIL" : "CLASS LOAD";
                Tr.debug(cltc, String.format("%s: [%s] [%s] [%s]", message, getKey(), loc, name));
            }
        }
        if (!byteResourceInformation.foundInClassCache() && hook != null) {
            URL sharedClassCacheURL = getSharedClassCacheURL(resourceURL, byteResourceInformation.getResourcePath());
            if (sharedClassCacheURL != null && Arrays.equals(bytes, byteResourceInformation.getBytes())) {
                hook.storeClass(sharedClassCacheURL, clazz);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Called shared class cache to store class", new Object[] {clazz.getName(), sharedClassCacheURL});
                }
            }
        }
        return clazz;
    }

    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    private ProtectionDomain getClassSpecificProtectionDomain(final String name, final URL resourceUrl) {
        ProtectionDomain pd = config.getProtectionDomain();
        try {
            pd = AccessController.doPrivileged(new PrivilegedExceptionAction<ProtectionDomain>() {
                @Override
                public ProtectionDomain run() {
                    return getClassSpecificProtectionDomainPrivileged(name, resourceUrl);
                }
            });
        } catch (PrivilegedActionException paex) {
            //auto FFDC
            return config.getProtectionDomain();
        }
        return pd;

    }

    private ProtectionDomain getClassSpecificProtectionDomainPrivileged(String className, URL resourceUrl) {
        ProtectionDomain pdFromConfig = config.getProtectionDomain();
        ProtectionDomain pd;

        try {
            URLConnection conn = resourceUrl.openConnection();
            URL containerUrl;
            if (conn instanceof JarURLConnection) {
                containerUrl = ((JarURLConnection) conn).getJarFileURL();
            } else if (conn instanceof WSJarURLConnection) {
                containerUrl = ((WSJarURLConnection) conn).getFile().toURI().toURL();
            } else {
                // this is most likely a file URL - i.e. the contents of the classes are expanded on the disk.
                // so a path like:  .../myServer/dropins/myWar.war/WEB-INF/classes/com/myPkg/MyClass.class
                // should convert to: .../myServer/dropins/myWar.war/WEB-INF/classes/
                containerUrl = new URL(resourceUrl.toString().replace(Util.convertClassNameToResourceName(className), ""));
            }
            String containerUrlString = containerUrl.toString();
            pd = protectionDomains.get(containerUrlString);
            if (pd == null) {
                CodeSource cs = new CodeSource(containerUrl, pdFromConfig.getCodeSource().getCertificates());
                pd = new ProtectionDomain(cs, pdFromConfig.getPermissions());
                protectionDomains.putIfAbsent(containerUrlString, pd);
                pd = protectionDomains.get(containerUrlString);
            }
        } catch (IOException ex) {
            // Auto-FFDC - and then use the protection domain from the classloader configuration
            pd = pdFromConfig;
        }
        return pd;
    }

    /**
     * This method will define the package using the byteResourceInformation for a class to get the URL for this package to try to load a manifest. If a manifest can't be loaded
     * from the URL it will create the package with no package versioning or sealing information.
     *
     * @param byteResourceInformation The information about the class file
     * @param packageName The name of the package to create
     */
    @FFDCIgnore(value = { IllegalArgumentException.class })
    private void definePackage(ByteResourceInformation byteResourceInformation, String packageName) {
        // If the package is in a JAR then we can load the JAR manifest to see what package definitions it's got
        Manifest manifest = byteResourceInformation.getManifest();

        try {
            // The URLClassLoader.definePackage() will NPE with a null manifest so use the other definePackage if we don't have a manifest
            if (manifest == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            } else {
                definePackage(packageName, manifest, byteResourceInformation.getResourceUrl());
            }
        } catch (IllegalArgumentException e) {
            // Ignore, this happens if the package is already defined but it is hard to guard against this in a thread safe way. See:
            // http://bugs.sun.com/view_bug.do?bug_id=4841786
        }
    }

    final ByteResourceInformation findClassBytes(String className) throws ClassNotFoundException {
        String resourceName = Util.convertClassNameToResourceName(className);
        try {
            ByteResourceInformation result = findClassBytes(className, resourceName);
            if (result == null) {
                String message = String.format("Could not find class '%s' as resource '%s'", className, resourceName);
                throw new ClassNotFoundException(message);
            }
            return result;
        } catch (IOException e) {
            Tr.error(tc, "cls.class.file.not.readable", className, resourceName);
            String message = String.format("Could not read class '%s' as resource '%s'", className, resourceName);
            ClassFormatError error = new ClassFormatError(message);
            error.initCause(e);
            throw error;
        }
    }

    @Override
    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Object token = ThreadIdentityManager.runAsServer();
        synchronized (getClassLoadingLock(name)) {
            try {
                return findOrDelegateLoadClass(name);
            } catch (ClassNotFoundException e) {
                // The class could not be found on the local class path or by
                // delegating to parent/library class loaders.  Try to generate it.
                Class<?> generatedClass = generateClass(name);
                if (generatedClass != null)
                    return generatedClass;

                // could not generate class - throw CNFE
                throw FeatureSuggestion.getExceptionWithSuggestion(e);
            } finally {
                ThreadIdentityManager.reset(token);
            }
        }
    }

    @Trivial
    Class<?> generateClass(String name) throws ClassNotFoundException {
        Class<?> generatedClass = null;
        if (generator != null) {
            byte[] bytes = generator.generateClass(name, this);
            if (bytes != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "defining generated class " + name);
                generatedClass = defineClass(name, bytes, 0, bytes.length, config.getProtectionDomain());
            }
        }
        return generatedClass;
    }

    /**
     * Find a class on this loader's class path or delegate to the parent class
     * loader.
     */
    protected Class<?> findOrDelegateLoadClass(String name) throws ClassNotFoundException {
        // The resolve parameter is a legacy parameter that is effectively
        // never used as of JDK 1.1 (see footnote 1 of section 5.3.2 of the 2nd
        // edition of the JVM specification).  The only caller of
        // loadClass(String, boolean) is java.lang.ClassLoader.loadClass(String),
        // and that method always passes false, so we ignore the parameter.
        return super.loadClass(name, false);
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
    private Class<?> findClassCommonLibraryClassLoaders(String name) throws ClassNotFoundException {
        for (LibertyLoader cl : delegateLoaders) {
            try {
                return cl.findClass(name);
            } catch (ClassNotFoundException e) {
                // Ignore. Will throw at the end if class is not found.
            }
        }
        // If we reached here, then the class was not loaded.
        throw new ClassNotFoundException(name);
    }

    /**
     * Search for the resource using the common library classloaders.
     *
     * @param name The resource name.
     *
     * @return The resource, if found. Otherwise null.
     */
    private URL findResourceCommonLibraryClassLoaders(String name) {
        for (LibertyLoader cl : delegateLoaders) {
            URL url = cl.findResource(name);
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
    private CompositeEnumeration<URL> findResourcesCommonLibraryClassLoaders(String name, CompositeEnumeration<URL> enumerations) throws IOException {
        for (LibertyLoader cl : delegateLoaders) {
            enumerations.add(cl.findResources(name));
        }
        return enumerations;
    }

    @Override
    protected void lazyInit() {
        // process all the libraries
        if (privateLibraries != null)
            for (Library lib : privateLibraries)
                copyLibraryElementsToClasspath(lib);
        // nullify the field - it's not needed any more
        privateLibraries = null;
    }

    /**
     * Takes the Files and Folders from the Library
     * and adds them to the various classloader classpaths
     *
     * @param library
     */
    private void copyLibraryElementsToClasspath(Library library) {
        Collection<File> files = library.getFiles();
        addToClassPath(library.getContainers());
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
}
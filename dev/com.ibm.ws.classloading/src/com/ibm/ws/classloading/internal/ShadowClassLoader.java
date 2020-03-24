/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation;
import com.ibm.ws.classloading.internal.ContainerClassLoader.ByteResourceInformation;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;

/**
 * This {@link ClassLoader} loads any classes requested of it that can be
 * loaded as resources from its parent.
 * It does not cause the parent {@link ClassLoader} to load any classes.
 * It does not support the loading of resources.
 * <p>
 * This {@link ClassLoader} should only be used for introspection.
 * The loaded classes should not be instantiated.
 * <p>
 * The {@link ClassLoader} should be discarded as early as possible to allow
 * any associated resources to be cleared up.
 */
class ShadowClassLoader extends IdentifiedLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    static final TraceComponent tc = Tr.register(ShadowClassLoader.class);

    private static final Enumeration<URL> EMPTY_ENUMERATION = new Enumeration<URL>() {
        @Override
        public URL nextElement() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasMoreElements() {
            return false;
        }
    };

    private final AppClassLoader shadowedLoader;
    private final Iterable<ClassLoader> delegateLoaders;

    ShadowClassLoader(AppClassLoader shadowed) {
        super(getShadow(shadowed.getParent()));
        this.shadowedLoader = shadowed;
        this.delegateLoaders = getShadows(shadowed.getDelegateLoaders());
    }

    /** create a {@link ShadowClassLoader} for the specified loader if it is an {@link AppClassLoader}. */
    private static ClassLoader getShadow(ClassLoader loader) {
        return loader instanceof AppClassLoader ? new ShadowClassLoader((AppClassLoader) loader) : loader;
    }

    private static List<ClassLoader> getShadows(Iterable<? extends ClassLoader> loaders) {
        ArrayList<ClassLoader> result = new ArrayList<ClassLoader>();
        for (ClassLoader delegate : loaders)
            result.add(getShadow(delegate));
        return result;
    }

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
        // The resolve parameter is a legacy parameter that is effectively
        // never used as of JDK 1.1 (see footnote 1 of section 5.3.2 of the 2nd
        // edition of the JVM specification).  The only caller of this method is
        // is java.lang.ClassLoader.loadClass(String), and that method always
        // passes false, so we ignore the parameter.

        {
            Class<?> result = findLoadedClass(className);
            if (result != null)
                return result;
        }

        ClassNotFoundException lastException = null;
        // use the shadowed loader's search order when searching for a class
        for (SearchLocation what : shadowedLoader.getSearchOrder()) {
            try {
                switch (what) {
                    case PARENT:
                        return getParent().loadClass(className);
                    case SELF:
                        return findClass(className);
                    case DELEGATES:
                        for (ClassLoader delegate : delegateLoaders) {
                            try {
                                return delegate.loadClass(className);
                            } catch (ClassNotFoundException e) {
                                lastException = e;
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown class loader search ordering element: " + what);
                }
            } catch (ClassNotFoundException e) {
                lastException = e;
            }
        }
        throw lastException;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        String resourceName = Util.convertClassNameToResourceName(name);
        final ByteResourceInformation classBytesResourceInformation = shadowedLoader.findClassBytes(name, resourceName);

        if (classBytesResourceInformation == null) {
            throw new ClassNotFoundException(name);
        }

        // Now define a package for this class if it has one
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex != -1) {
            String packageName = name.substring(0, lastDotIndex);

            // Try to avoid defining a package twice
            if (this.getPackage(packageName) == null) {
                definePackage(classBytesResourceInformation, packageName);
            }
        }

        byte[] bytes = classBytesResourceInformation.getBytes();
        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * Defines the package for this class by trying to load the manifest for the package and if that fails just sets all of the package properties to <code>null</code>.
     * 
     * @param classBytesResourceInformation
     * @param packageName
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private void definePackage(final ByteResourceInformation classBytesResourceInformation, String packageName) {
        /*
         * We don't extend URL classloader so we automatically pass the manifest from the JAR to our supertype, still try to get hold of it though so that we can see if
         * we can load the information ourselves from it
         */
        Manifest manifest = classBytesResourceInformation.getManifest();
        try {
            if (manifest == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            } else {
                String classResourceName = classBytesResourceInformation.getResourcePath();

                /*
                 * Strip the class name off the end of the package, should always have at least one '/' as we have at least one . in the name, also end with a trailing
                 * '/' to match the name style for package definitions in manifest files
                 */
                String packageResourceName = classResourceName.substring(0, classResourceName.lastIndexOf('/') + 1);

                // Default all of the package info to null
                String specTitle = null;
                String specVersion = null;
                String specVendor = null;
                String implTitle = null;
                String implVersion = null;
                String implVendor = null;
                URL sealBaseUrl = null;

                // See if there is a package defined in the manifest 
                Attributes packageAttributes = manifest.getAttributes(packageResourceName);
                if (packageAttributes != null && !packageAttributes.isEmpty()) {
                    specTitle = packageAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
                    specVersion = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
                    specVendor = packageAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);

                    implTitle = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                    implVersion = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    implVendor = packageAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

                    String sealedValue = packageAttributes.getValue(Attributes.Name.SEALED);
                    if (sealedValue != null && Boolean.parseBoolean(sealedValue)) {
                        sealBaseUrl = classBytesResourceInformation.getResourceUrl();
                    }
                }

                definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBaseUrl);
            }
        } catch (IllegalArgumentException ignored) {
            // This happens if the package is already defined but it is hard to guard against this in a thread safe way. See:
            // http://bugs.sun.com/view_bug.do?bug_id=4841786
        }
    }

    /////////////////////////////////////////////////
    // DELEGATE EVERYTHING ELSE TO SHADOWED LOADER //
    /////////////////////////////////////////////////

    @Override
    public URL getResource(String name) {
        return name == null ? null : shadowedLoader.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return name == null ? null : shadowedLoader.getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return name == null ? EMPTY_ENUMERATION : shadowedLoader.getResources(name);
    }

    @Override
    public EnumSet<ApiType> getApiTypeVisibility() {
        return shadowedLoader.getApiTypeVisibility();
    }

    @Override
    public ClassLoaderIdentity getKey() {
        return shadowedLoader.getKey();
    }

    @Override
    public Bundle getBundle() {
        return shadowedLoader.getBundle();
    }
}

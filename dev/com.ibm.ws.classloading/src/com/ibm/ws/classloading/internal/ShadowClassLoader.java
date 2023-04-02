/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
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
import com.ibm.ws.classloading.internal.util.Keyed;
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
class ShadowClassLoader extends LibertyLoader implements Keyed<ClassLoaderIdentity> {
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
    private final Iterable<LibertyLoader> delegateLoaders;

    ShadowClassLoader(AppClassLoader shadowed) {
        super(getShadow(shadowed.parent));
        this.shadowedLoader = shadowed;
        this.delegateLoaders = getShadows(shadowed.getDelegateLoaders());
    }

    /** create a {@link ShadowClassLoader} for the specified loader if it is an {@link AppClassLoader}. */
    private static ClassLoader getShadow(ClassLoader loader) {
        return loader instanceof AppClassLoader ? new ShadowClassLoader((AppClassLoader) loader) : loader;
    }

    /** create a {@link ShadowClassLoader} for the specified loader if it is an {@link AppClassLoader}. */
    private static LibertyLoader getShadow(LibertyLoader loader) {
        return loader instanceof AppClassLoader ? new ShadowClassLoader((AppClassLoader) loader) : loader;
    }

    private static List<LibertyLoader> getShadows(Iterable<? extends LibertyLoader> loaders) {
        ArrayList<LibertyLoader> result = new ArrayList<>();
        for (LibertyLoader delegate : loaders)
            result.add(getShadow(delegate));
        return result;
    }

    @Override
    protected final Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
        return loadClass(className, resolveClass, false, false);
    }

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> loadClass(String className, boolean resolveClass, boolean onlySearchSelf, boolean returnNull) throws ClassNotFoundException {
        // The resolve parameter is a legacy parameter that is effectively
        // never used as of JDK 1.1 (see footnote 1 of section 5.3.2 of the 2nd
        // edition of the JVM specification).  The only caller of this method is
        // is java.lang.ClassLoader.loadClass(String), and that method always
        // passes false, so we ignore the parameter.

        ClassNotFoundException lastException = null;
        synchronized (getClassLoadingLock(className)) {
            Class<?> result = findLoadedClass(className);
            if (result != null)
                return result;

            if (onlySearchSelf) {
                return findClass(className, returnNull);
            }

            // use the shadowed loader's search order when searching for a class
            for (SearchLocation what : shadowedLoader.getSearchOrder()) {
                try {
                    switch (what) {
                        case PARENT:
                            if (parent instanceof LibertyLoader) {
                                result = ((LibertyLoader) parent).loadClass(className, false, false, returnNull);
                                if (result != null) {
                                    return result;
                                }
                            } else {
                                return parent.loadClass(className);
                            }
                        case SELF:
                            result = findClass(className, returnNull);
                            if (result != null) {
                                return result;
                            }
                        case DELEGATES:
                            for (LibertyLoader delegate : delegateLoaders) {
                                try {
                                    result = delegate.loadClass(className, false, false, returnNull);
                                    if (result != null) {
                                        return result;
                                    }
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
        }
        if (returnNull) {
            return null;
        }

        if (lastException == null) {
            lastException = new ClassNotFoundException(className);
        }
        throw lastException;
    }

    @Override
    protected Class<?> findClass(String name, boolean returnNull) throws ClassNotFoundException {
        String resourceName = Util.convertClassNameToResourceName(name);
        final ByteResourceInformation classBytesResourceInformation = shadowedLoader.findClassBytes(name, resourceName);

        if (classBytesResourceInformation == null) {
            if (returnNull) {
                return null;
            }
            throw new ClassNotFoundException(name);
        }

        // Now define a package for this class if it has one
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex != -1) {
            String packageName = name.substring(0, lastDotIndex);

            // Try to avoid defining a package twice
            if (this.getPackage(packageName) == null) {
                classBytesResourceInformation.definePackage(packageName, this);
            }
        }

        byte[] bytes = classBytesResourceInformation.getBytes();
        return defineClass(name, bytes, 0, bytes.length);
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

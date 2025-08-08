/*******************************************************************************
 * Copyright (c) 2010, 2025 IBM Corporation and others.
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
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.AFTER_DELEGATES;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.BEFORE_DELEGATES;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.PARENT;
import static com.ibm.ws.classloading.internal.AppClassLoader.SearchLocation.SELF;
import static com.ibm.ws.classloading.internal.Util.freeze;
import static com.ibm.ws.classloading.internal.Util.list;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;

/**
 * A version of the standard URLClassLoader that checks the child level first
 * and the parent classloader second.
 */
class ParentLastClassLoader extends AppClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    ParentLastClassLoader(ClassLoader parent, ClassLoaderConfiguration config, List<Container> urls, DeclaredApiAccess access, ClassRedefiner redefiner, ClassGenerator generator, GlobalClassloadingConfiguration globalConfig, List<ClassFileTransformer> systemTransformers) {
        super(parent, config, urls, access, redefiner, generator, globalConfig, systemTransformers);
    }

    static final List<SearchLocation> PARENT_LAST_SEARCH_ORDER = freeze(list(BEFORE_DELEGATES, SELF, AFTER_DELEGATES, PARENT));

    /** Provides the search order so the {@link ShadowClassLoader} can use it. */
    @Override
    Iterable<SearchLocation> getSearchOrder() {
        return PARENT_LAST_SEARCH_ORDER;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    @Trivial
    public URL getResource(String name) {
        URL result = findResourceCommonLibraryClassLoaders(name, beforeApp);
        if (result == null) {
            result = findResource(name);
        }
        if (result == null) {
            result = parent.getResource(name);
        }
        return result;
    }

    @Override
    @Trivial
    public Enumeration<URL> getResources(String resName) throws IOException {
        // search order: 1) beforeApp common libraries 2)  my class path and afterApp common libraries 3) parent loader
        return findResourcesCommonLibraryClassLoaders(resName, new CompositeEnumeration<>(), beforeApp) //
                        .add(this.findResources(resName)) //
                        .add(this.parent.getResources(resName));
    }

    @FFDCIgnore(ClassNotFoundException.class)
    @Override
    @Trivial
    protected Class<?> findOrDelegateLoadClass(String className, boolean onlySearchSelf, boolean returnNull) throws ClassNotFoundException {
        final boolean RETURN_NULL_FOR_NO_CLASS = true;
        Class<?> beforeAppLoad = findClassCommonLibraryClassLoaders(className, RETURN_NULL_FOR_NO_CLASS, beforeApp);
        if (beforeAppLoad != null) {
            return beforeAppLoad;
        }
        ClassNotFoundException findClassException = null;
        // search order: 1) my class path 2) parent loader
        Class<?> rc = null;

        synchronized (getClassLoadingLock(className)) {
            // first check whether we already loaded this class
            rc = findLoadedClass(className);

            if (rc == null) {
                // first check our classpath
                try {
                    rc = findClass(className, returnNull);
                } catch (ClassNotFoundException cnfe) {
                    findClassException = cnfe;
                }
                if (rc == null) {
                    // See if we can generate the class here before
                    // checking the parent:
                    rc = generateClass(className);
                }
            }
        }

        if (rc != null) {
            return rc;
        }

        // no luck? try the parent next unless we are only checking ourself
        if (onlySearchSelf) {
            if (returnNull) {
                return null;
            }
            throw findClassException;
        }

        if (this.parent instanceof NoClassNotFoundLoader) {
            rc = ((NoClassNotFoundLoader) this.parent).loadClassNoException(className);
            if (rc != null || returnNull) {
                return rc;
            }
            throw findClassException;
        }

        if (returnNull) {
            try {
                return this.parent.loadClass(className);
            } catch (ClassNotFoundException cnfe) {
                return null;
            }
        }
        
        return this.parent.loadClass(className);
    }
}

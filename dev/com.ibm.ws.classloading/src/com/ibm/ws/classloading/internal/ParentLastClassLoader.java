/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
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

import java.io.IOException;
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

/**
 * A version of the standard URLClassLoader that checks the child level first
 * and the parent classloader second.
 */
class ParentLastClassLoader extends AppClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    ParentLastClassLoader(ClassLoader parent, ClassLoaderConfiguration config, List<Container> urls, DeclaredApiAccess access, ClassRedefiner redefiner, ClassGenerator generator, GlobalClassloadingConfiguration globalConfig) {
        super(parent, config, urls, access, redefiner, generator, globalConfig);
    }

    static final List<SearchLocation> PARENT_LAST_SEARCH_ORDER = freeze(list(SELF, DELEGATES, PARENT));

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
    public URL getResource(String resName) {
        // search order: 1) my class path 2) parent loader
        URL result = findResource(resName);
        return result == null ? this.parent.getResource(resName) : result;
    }

    @Override
    @Trivial
    public Enumeration<URL> getResources(String resName) throws IOException {
        // search order: 1) my class path 2) parent loader
        return super.findResources(resName).add(this.parent.getResources(resName));
    }

    @FFDCIgnore(ClassNotFoundException.class)
    @Override
    @Trivial
    protected Class<?> findOrDelegateLoadClass(String className) throws ClassNotFoundException {
        // search order: 1) my class path 2) parent loader
        Class<?> rc;
        synchronized (getClassLoadingLock(className)) {
            // first check whether we already loaded this class
            rc = findLoadedClass(className);
            if (rc == null) {
                try {
                    // first check our classpath
                    rc = findClass(className);
                } catch (ClassNotFoundException cnfe) {
                    // See if we can generate the class here before
                    // checking the parent:
                    Class<?> generatedClass = generateClass(className);
                    if (generatedClass != null)
                        return generatedClass;

                    // no luck? try the parent next
                }
            }
        }
        return rc == null ? this.parent.loadClass(className) : rc;
    }

}

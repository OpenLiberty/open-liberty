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
import static com.ibm.ws.classloading.internal.LibertyLoader.DelegatePolicy.includeParent;
import static com.ibm.ws.classloading.internal.Util.freeze;
import static com.ibm.ws.classloading.internal.Util.list;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
        boolean traceEnabled = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        // path is null when trace is off — avoids string allocation on the hot path.
        return getResourceInternal(name, traceEnabled ? this.toString() : null);
    }

    /**
     * Overrides parent-first search order to implement parent-last, emitting trace
     * at the point of discovery.
     *
     * Search order (parent-last):
     * 1. beforeApp library delegates
     * 2. local classpath + afterApp library delegates
     * 3. parent classloader
     *
     * @param name The resource name.
     * @param path The delegation path so far, or null if trace is disabled.
     * @return The URL of the resource, or null if not found.
     */
    @Override
    @Trivial
    URL getResourceInternal(String name, String path) {
        // 1. beforeApp library delegates
        URL url = findResourceCommonLibraryClassLoaders(name, beforeApp, path);
        if (url != null) {
            return url;
        }

        // 2. local classpath + afterApp library delegates
        url = findResourceInternal(name, false, path);
        if (url != null) {
            return url;
        }

        // 3. parent classloader (searched last — parent-last order)
        String parentPath = path != null ? path + " -> " + parent : null;
        if (parent instanceof AppClassLoader) {
            // Thread path into the parent so the full chain is visible in its trace.
            url = ((AppClassLoader) parent).getResourceInternal(name, parentPath);
        } else {
            url = parent.getResource(name);
            if (url != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, String.format("Resource=[%s] found at location=[%s] by parent classloader=[%s]; delegation path=[%s]",
                        name, url, parent, parentPath));
            }
        }
        if (url != null) {
            return url;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format("Resource=[%s] not found; classloader=[%s]", name, this));
        }
        return null;
    }

    @Override
    @Trivial
    public Enumeration<URL> getResources(String resName) throws IOException {
        boolean traceEnabled = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        // path is null when trace is off — avoids string allocation on the hot path.
        return getResourcesInternal(resName, traceEnabled ? this.toString() : null);
    }

    /**
     * Overrides parent-first search order to implement parent-last.
     *
     * Search order (parent-last):
     * 1. beforeApp library delegates
     * 2. local classpath + afterApp library delegates
     * 3. parent classloader
     *
     * @param name The resource name.
     * @param path The delegation path so far, or null if trace is disabled.
     * @return An enumeration of all matching URLs.
     */
    @Override
    @Trivial
    Enumeration<URL> getResourcesInternal(String resName, String path) throws IOException {
        // 1. beforeApp library delegates
        CompositeEnumeration<URL> results = findResourcesCommonLibraryClassLoaders(resName, new CompositeEnumeration<>(), beforeApp, path);

        // 2. local classpath + afterApp library delegates
        results.add(findResourcesInternal(resName, false, path));

        // 3. parent classloader (searched last — parent-last order)
        String parentPath = path != null ? path + " -> " + parent : null;
        Enumeration<URL> parentResults;
        if (parent instanceof AppClassLoader) {
            // Thread path into the parent so the full chain is visible in its trace.
            parentResults = ((AppClassLoader) parent).getResourcesInternal(resName, parentPath);
        } else {
            parentResults = this.parent.getResources(resName);
            if (path != null) {
                List<URL> urls = Collections.list(parentResults);
                if (!urls.isEmpty()) {
                    Tr.debug(tc, String.format("Resources=[%s] found at locations=%s from parent classloader=[%s]; delegation path=[%s]",
                            resName, urls, parent, parentPath));
                }
                parentResults = Collections.enumeration(urls);
            }
        }
        results.add(parentResults);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            List<URL> all = Collections.list(results);
            if (all.isEmpty()) {
                Tr.debug(tc, String.format("Resources=[%s] not found; classloader=[%s]", resName, this));
            }
            return Collections.enumeration(all);
        }
        return results;
    }
    
    @Override
    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> findOrDelegateLoadClass(String className, DelegatePolicy delegatePolicy, boolean returnNull, String path) throws ClassNotFoundException {
        final boolean RETURN_NULL_FOR_NO_CLASS = true;
        Class<?> beforeAppLoad = findClassCommonLibraryClassLoaders(className, RETURN_NULL_FOR_NO_CLASS, beforeApp, delegatePolicy, path);
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
                    rc = findClassInternal(className, delegatePolicy, returnNull, path);
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
        if (delegatePolicy != includeParent) {
            if (returnNull) {
                return null;
            }
            throw findClassException;
        }

        // Extend the delegation path to the parent before delegating (searched last — parent-last order).
        String parentPath = path != null ? path + " -> " + parent : null;
        if (this.parent instanceof AppClassLoader) {
            // Thread path into the parent so the full chain is visible in its trace.
            rc = ((AppClassLoader) this.parent).loadClassInternal(className, false, includeParent, returnNull, parentPath);
            return rc;
        }

        if (this.parent instanceof NoClassNotFoundLoader) {
            rc = ((NoClassNotFoundLoader) this.parent).loadClassNoException(className);
            if (rc != null) {
                TraceComponent t = activeTraceComponentIfEnabled(className);
                if (t != null) {
                    Tr.debug(t, String.format("Class=[%s] loaded by parent classloader=[%s]; delegation path=[%s]",
                            className, parent, parentPath));
                }
            }
            if (rc != null || returnNull) {
                return rc;
            }
            throw findClassException;
        }

        if (returnNull) {
            try {
                rc = this.parent.loadClass(className);
                if (rc != null) {
                    TraceComponent t = activeTraceComponentIfEnabled(className);
                    if (t != null) {
                        Tr.debug(t, String.format("Class=[%s] loaded by parent classloader=[%s]; delegation path=[%s]",
                                className, parent, parentPath));
                    }
                }
                return rc;
            } catch (ClassNotFoundException cnfe) {
                return null;
            }
        }

        rc = this.parent.loadClass(className);
        if (rc != null) {
            TraceComponent t = activeTraceComponentIfEnabled(className);
            if (t != null) {
                Tr.debug(t, String.format("Class=[%s] loaded by parent classloader=[%s]; delegation path=[%s]",
                        className, parent, parentPath));
            }
        }
        return rc;
    }

    @Override
    @Trivial
    protected boolean isParentFirst() {
        return false;
    }
}

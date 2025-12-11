/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.MetaInfServicesProvider;
import com.ibm.ws.classloading.internal.util.Keyed;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A specific type of UnifiedClassLoader: ThreadContextClassLoader
 */
public class ThreadContextClassLoader extends UnifiedClassLoader implements Keyed<String> {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    static final TraceComponent tc = Tr.register(ThreadContextClassLoader.class);
    private final AtomicReference<Bundle> bundle = new AtomicReference<Bundle>();
    protected final String key;
    private final AtomicInteger refCount = new AtomicInteger(0);

    /**
     * This variable MUST be named appLoader because the WebsphereLibertyClassLoaderHandler class in the open source classgraph
     * library is doing reflection in order to call the getClassPath() method on the AppClassLoader's smartClassPath field.
     * See the findClassOrder method in classgraph:
     * https://github.com/classgraph/classgraph/blob/latest/src/main/java/nonapi/io/github/classgraph/classloaderhandler/WebsphereLibertyClassLoaderHandler.java
     * classgraph is used by Eclipse JNoSQL and without this logic tests in io.openliberty.data.internal_fat_nosql fat bucket will fail.
     */
    private final ClassLoader appLoader;
    private final ClassLoadingServiceImpl clSvc;

    public ThreadContextClassLoader(GatewayClassLoader augLoader, ClassLoader appLoader, String key, ClassLoadingServiceImpl clSvc) {
        this(augLoader, appLoader, key, clSvc, appLoader instanceof AppClassLoader ? new ClassLoaderRef((AppClassLoader) appLoader) : appLoader);
    }

    private ThreadContextClassLoader(GatewayClassLoader augLoader, ClassLoader appLoader, String key, ClassLoadingServiceImpl clSvc, ClassLoader appLoaderRef) {
        super(appLoader instanceof ParentLastClassLoader ? appLoaderRef : augLoader, appLoader instanceof ParentLastClassLoader ? augLoader : appLoaderRef);
        bundle.set(augLoader.getBundle());
        this.key = key;
        this.appLoader = appLoaderRef;
        this.clSvc = clSvc;
    }

    /**
     * Cleans up the TCCL instance. Once called, this TCCL is effectively disabled.
     * It's associated gateway bundle will have been removed.
     */
    private void cleanup() {
        final String methodName = "cleanup(): ";
        try {
            final Bundle b = bundle.getAndSet(null);
            if (b != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + "Uninstalling bundle location: " + b.getLocation() + ", bundle id: " + b.getBundleId());
                }
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            try {
                                b.uninstall();
                                return null;
                            } catch (BundleException ignored) {
                                return null;
                            }
                        }
                    });
                } else {
                    b.uninstall();
                }
            }
        } catch (BundleException ignored) {
        } catch (IllegalStateException ignored) {

        }
    }

    /**
     * The ClassLoadingService implementation should call this method when it's
     * destroyThreadContextClassLoader method is called. Each call to destroyTCCL
     * should decrement this ref counter. When there are no more references to this
     * TCCL, it will be cleaned up, which effectively invalidates it.
     *
     * Users of the ClassLoadingService should understand that:<ol>
     * <li>They are responsible for destroying every instance of a TCCL that they
     * create via the CLS.createTCCL method, and,
     * <li>If they still have references to the TCCL instance after destroying all
     * instances they created, they will be holding on to a stale (leaked) classloader,
     * which can result in OOM situations.</li>
     * </ol>
     *
     * @return the new current count of references to this TCCL instance
     */
    int decrementRefCount() {
        final String methodName = "decrementRefCount(): ";
        final int count = refCount.decrementAndGet();
        if (count < 0) {
            // more destroys than creates
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " refCount < 0 - too many calls to destroy/cleaup", new Throwable("stack trace"));
            }
        }
        if (count == 0) {
            cleanup();
        }
        return count;
    }

    /**
     * The ClassLoadingService implementation should call this method when its
     * createThreadContextClassLoader method is called, both for new and pre-existing
     * instances. Each call to createTCCL should increment this ref counter - likewise,
     * each call to destroy should call decrementRefCount();
     *
     * @return the new current count of references to this TCCL instance
     */
    int incrementRefCount() {
        return refCount.incrementAndGet();
    }

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    @Trivial
    protected Class<?> findClass(String className, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;
        Class<?> c = null;
        try {
            c = super.findClass(className, delegatePolicy, returnNull);
        } catch (ClassNotFoundException x) {
            cnfe = x;
        }

        // Special case to find and load META-INF/services provider classes
        if (c == null) {
            MetaInfServicesProvider metaInfServices = clSvc.metaInfServicesRefs.getService(className);
            c = metaInfServices == null ? null : metaInfServices.getProviderImplClass();
        }
        if (c != null || returnNull) {
            return c;
        }
        throw cnfe;
    }

    /*********************************************************************************/
    /** Override classloading related methods so this class shows up in stacktraces **/
    /*********************************************************************************/
    @Override
    protected Class<?> loadClass(String name, boolean resolve, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        return super.loadClass(name, resolve, delegatePolicy, returnNull);
    }

    @Override
    protected URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null) {
            ConcurrentLinkedQueue<String> providerNames = clSvc.metaInfServicesProviders.get(name);
            if (providerNames != null)
                for (String providerImplClassName : providerNames) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, providerImplClassName);
                    ServiceReference<MetaInfServicesProvider> ref = clSvc.metaInfServicesRefs.getReference(providerImplClassName);
                    if (ref != null) {
                        url = (URL) ref.getProperty("file.url");
                        break;
                    }
                }
        }
        return url;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> urlEnum = super.findResources(name);
        ConcurrentLinkedQueue<String> providerNames = clSvc.metaInfServicesProviders.get(name);
        if (providerNames != null && !providerNames.isEmpty()) {
            Set<URL> urls = new LinkedHashSet<URL>();
            while (urlEnum.hasMoreElements())
                urls.add(urlEnum.nextElement());
            for (String providerImplClassName : providerNames) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, providerImplClassName);
                ServiceReference<MetaInfServicesProvider> ref = clSvc.metaInfServicesRefs.getReference(providerImplClassName);
                if (ref != null) {
                    urls.add((URL) ref.getProperty("file.url"));
                }
            }
            urlEnum = Collections.enumeration(urls);
        }
        return urlEnum;
    }

    @Override
    public String getKey() {
        return key;
    }

    final boolean isFor(ClassLoader classLoader) {
        ClassLoader classLoaderToCompare = appLoader;
        if (appLoader instanceof ClassLoaderRef) {
            classLoaderToCompare = ((ClassLoaderRef) appLoader).getReferent();
        }
        return classLoader == classLoaderToCompare;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(": Application ClassLoader ");
        sb.append(appLoader);
        return sb.toString();
    }

    // This is reflectively called by Spring for GLIBC proxy classes.
    // To work properly with package private application class beans these proxies
    // must be defined with the app loader so they have proper visibility to their
    // super classes.
    @Override
    @Trivial
    public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
        if (appLoader instanceof SpringLoader) {
            return ((SpringLoader) appLoader).publicDefineClass(name, b, protectionDomain);
        }
        return defineClass(name, b, 0, b.length, protectionDomain);
    }
}

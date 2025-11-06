/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.LibertyLoader.DelegatePolicy.includeParent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.LibertyClassLoader;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;

/**
 * This class is used to create weak references to AppClassLoaders
 */
@Trivial
public class ClassLoaderRef extends LibertyLoader implements SpringLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private static final TraceComponent tc = Tr.register(ClassLoaderRef.class);

    private final AtomicBoolean warningEmitted = new AtomicBoolean(false);

    private final String key;

    private final WeakReference<AppClassLoader> classLoaderWeakRef;

    /**
     * To mimic AppClassLoader, this class has a smartClassPath field for calling the getClassPath method.
     * This variable MUST be named smartClassPath because the WebsphereLibertyClassLoaderHandler class in the open source classgraph
     * library is doing reflection in order to call the getClassPath() method on the AppClassLoader, smartClassPath field.
     * To make it simple, this field just references <code>this</code> instance and there is a getClassPath() method on this class.
     * See the findClassOrder method in classgraph:
     * https://github.com/classgraph/classgraph/blob/latest/src/main/java/nonapi/io/github/classgraph/classloaderhandler/WebsphereLibertyClassLoaderHandler.java
     * classgraph is used by Eclipse JNoSQL and without this logic tests in io.openliberty.data.internal_fat_nosql fat bucket will fail.
     */
    public final ClassLoaderRef smartClassPath = this;

    ClassLoaderRef(AppClassLoader appClassLoader) {
        super(appClassLoader.getParent());
        ClassLoaderIdentity identity = appClassLoader.getKey();
        key = identity == null ? null : identity.toString();
        classLoaderWeakRef = new WeakReference<>(appClassLoader);
    }

    @Override
    protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, includeParent, false);
    }

    /**
     * Returns the application class loader if it is still referenced
     * and puts out a warning one time if it is not referenced any longer
     *
     * @return the AppClassLoader or null
     */
    private AppClassLoader getAppLoader() {
        AppClassLoader appCL = classLoaderWeakRef.get();
        if (appCL == null) {
            if (key != null && warningEmitted.compareAndSet(false, true)) {
                Tr.warning(tc, "app.classloader.removed", Thread.currentThread().getName(), key);
            }
        }
        return appCL;
    }

    AppClassLoader getReferent() {
        return classLoaderWeakRef.get();
    }

    @Override
    public URL getResource(String resName) {
        AppClassLoader appCL = getAppLoader();
        return appCL != null ? appCL.getResource(resName) : parent.getResource(resName);
    }

    @Override
    public InputStream getResourceAsStream(String resName) {
        AppClassLoader appCL = getAppLoader();
        return appCL != null ? appCL.getResourceAsStream(resName) : parent.getResourceAsStream(resName);
    }

    @Override
    public Enumeration<URL> getResources(String resName) throws IOException {
        AppClassLoader appCL = getAppLoader();
        return appCL != null ? appCL.getResources(resName) : parent.getResources(resName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ClassLoaderRef) {
            return Objects.equals(classLoaderWeakRef.get(), ((ClassLoaderRef) o).classLoaderWeakRef.get());
        }
        return false;
    }

    @Override
    public boolean addTransformer(ClassFileTransformer cft) {
        AppClassLoader appCL = getAppLoader();
        if (appCL != null) {
            return appCL.addTransformer(cft);
        }
        return false;
    }

    @Override
    public ClassLoader getThrowawayClassLoader() {
        AppClassLoader appCL = getAppLoader();
        return appCL != null ? appCL.getThrowawayClassLoader() : this;
    }

    @Override
    public Class<?> publicDefineClass(String name, byte[] b, ProtectionDomain protectionDomain) {
        AppClassLoader appCL = getAppLoader();
        return appCL != null ? appCL.publicDefineClass(name, b, protectionDomain) : defineClass(name, b, 0, b.length, protectionDomain);
    }

    @Override
    public EnumSet<ApiType> getApiTypeVisibility() {
        AppClassLoader appCL = classLoaderWeakRef.get();
        if (appCL != null) {
            return appCL.getApiTypeVisibility();
        }
        if (parent instanceof DeclaredApiAccess) {
            return ((DeclaredApiAccess) parent).getApiTypeVisibility();
        }
        if (parent instanceof LibertyClassLoader) {
            return ((LibertyClassLoader) parent).getApiTypeVisibility();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        // classLoaderWeakRef can be null if toString() is called by trace during 
        // super constructor
        if (classLoaderWeakRef != null) {
            sb.append(": Application ClassLoader ");
            sb.append(classLoaderWeakRef.get());
        }
        return sb.toString();
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        AppClassLoader appCL = getAppLoader();
        if (appCL == null) {
            if (delegatePolicy != DelegatePolicy.includeParent) {
                return findClass(className, delegatePolicy, returnNull);
            }

            if (parent instanceof NoClassNotFoundLoader) {
                Class<?> result = ((NoClassNotFoundLoader) parent).loadClassNoException(className);
                if (result == null) {
                    if (returnNull) {
                        return null;
                    }
                    throw new ClassNotFoundException(className);
                }

                return result;
            } else {
                try {
                    return parent.loadClass(className);
                } catch (ClassNotFoundException e) {
                    if (returnNull) {
                        return null;
                    }
                    throw e;
                }
            }
        }

        return appCL.loadClass(className, resolve, delegatePolicy, returnNull);
    }

    @Override
    protected Class<?> findClass(String className, DelegatePolicy delegatePolicy, boolean returnNull) throws ClassNotFoundException {
        if (returnNull) {
            return null;
        }
        throw new ClassNotFoundException(className);
    }

    @Override
    public Bundle getBundle() {
        return parent instanceof GatewayClassLoader ? ((GatewayClassLoader) parent).getBundle() : parent instanceof LibertyLoader ? ((LibertyLoader) parent).getBundle() : null;
    }

    /**
     * This method is called by the classgraph open source library doing reflection on
     * AppClassLoader. Since ClassLoaderRef now is used instead of AppClassLoader in
     * ThreadContextClassLoader, we need to be able to still allow classgraph to work.
     * See more details in the smartClassPath javadoc above.
     *
     * @return AppClassLoader.getClassPath() result
     */
    public Collection<Collection<URL>> getClassPath() {
        AppClassLoader appCL = classLoaderWeakRef.get();
        if (appCL != null) {
            return appCL.getClassPath();
        }
        return null;
    }
}

/*******************************************************************************
 * Copyright (c) 2010, 2026 IBM Corporation and others.
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration.JVMPackages;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoadingConfigurationException;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;

/*
 * This class needs to implement BundleReference.
 * This is particularly necessary for the OSGi JNDI implementation which walks
 *  the classloader hierarchy looking for the Bundle classloader.
 */
class GatewayClassLoader extends ClassLoader implements DeclaredApiAccess, BundleReference, NoClassNotFoundLoader {

    private static class Delegation {
        // This is only used to place a non-class loader class on the call stack which is loaded from a bundle.
        // This is needed as a workaround for defect 89337.
        @Trivial
        static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
            return loader == null ? null : loader.loadClass(className);
        }
        @Trivial
        static Class<?> loadClass(String className, BundleLoader loader) {
            return loader == null ? null : loader.findClassNoException(className);
        }
    }

    private final GatewayConfiguration config;
    private final JVMPackages jvmPackages;
    private final Object wiringMonitor = new Object() {};
    private final Bundle bundle;
    private BundleWiring wiring = null;
    private final ClassLoader cl;
    private volatile BundleLoader bLoader;
    private final CompositeResourceProvider resourceProviders;

    static GatewayClassLoader createGatewayClassLoader(Map<Bundle, Set<GatewayClassLoader>> classloaders,
                                                       GatewayConfiguration config,
                                                       ClassLoader bundleLoader,
                                                       CompositeResourceProvider resourceProviders,
                                                       JVMPackages jvmPackages) {
        GatewayClassLoader result = new GatewayClassLoader(config, bundleLoader, resourceProviders, jvmPackages);
        if (classloaders != null) {
            Bundle b = result.getBundle();
            if (b != null) {
                synchronized (classloaders) {
                    Set<GatewayClassLoader> loaders = classloaders.get(b);
                    if (loaders == null) {
                        loaders = Collections.newSetFromMap(new WeakHashMap<GatewayClassLoader, Boolean>());
                        classloaders.put(b, loaders);
                    }
                    loaders.add(result);
                }
            }
        }
        return result;
    }

    private GatewayClassLoader(GatewayConfiguration config, ClassLoader bundleLoader, CompositeResourceProvider resourceProviders, JVMPackages jvmPackages) {
        super(jvmPackages.delegate());
        this.config = config;
        this.jvmPackages = jvmPackages;
        // stash the bundle revision to delegate to its class loader
        if (bundleLoader instanceof BundleReference) {
            this.cl = null;
            this.bundle = ((BundleReference) bundleLoader).getBundle();
            this.wiring = bundle.adapt(BundleWiring.class);
            if (this.wiring == null) {
                throw new IllegalStateException("Gateway bundle is not resolved.");
            }
            // Just getting the loader again to make sure it is the latest.
            // This is Equinox specific stuff to avoid CNFE if possible
            ModuleClassLoader moduleLoader = (ModuleClassLoader) wiring.getClassLoader();
            if (moduleLoader == null) {
                throw new IllegalStateException("Gateway bundle does not have a class loader.");
            }
            this.bLoader = moduleLoader.getBundleLoader();
        } else {
            // not really a bundle class loader!!
            this.bundle = null;
            this.cl = bundleLoader;
        }
        this.resourceProviders = resourceProviders;
    }

    @Override
    @Trivial
    public EnumSet<ApiType> getApiTypeVisibility() {
        return config.getApiTypeVisibility();
    }

    /**
     * {@inheritDoc}
     * 
     * Search order:
     * 1. Searches the bundles
     * 2. Searches the system resources
     */
    @Override
    @Trivial
    public URL getResource(String resName) {
        // Do bundle first resource loading
        URL result = this.findResource(resName);
        // second check the system loader
        return result == null ? jvmPackages.getResource(resName) : result;
    }

    /**
     * {@inheritDoc}
     * 
     * Search order:
     * 1. Searches the bundle loader
     * 2. Searches the resource provider
     */
    @Override
    protected URL findResource(String name) {
        URL result = null;
        // Only check the parent bundle loader if the request is outside of "" or "/"
        if (!!!"".equals(name) && !!!"/".equals(name)) {
            // First try the bundleLoader
            if (cl != null) {
                result = cl.getResource(name);
            } else {
                BundleLoader current = bLoader;
                result = current == null ? null : current.findResource(name);
            }
        }
        // This doesn't have access to ALL split packages (it just gets one) so it's augmented with a resource provider  
        return result == null ? resourceProviders.findResource(name) : result;
    }

    @Override
    @Trivial
    public Enumeration<URL> getResources(String resName) throws IOException {
        // First check for the bundles' resources then check the system loader
        return findResources(resName).add(jvmPackages.getResources(resName));
    }

    @Override
    protected CompositeEnumeration<URL> findResources(String name) throws IOException {
        CompositeEnumeration<URL> result = new CompositeEnumeration<URL>();

        // Only check the parent bundle loader if the request is outside of "" or "/"
        if (!!!"".equals(name) && !!!"/".equals(name)) {
            // First try the bundleLoader
            Enumeration<URL> urls;
            if (cl != null) {
                urls = cl.getResources(name);
            } else {
                BundleLoader current = bLoader;
                urls = current == null ? Collections.emptyEnumeration() : current.findResources(name);
            }
            result.add(urls);
        }

        resourceProviders.findResources(name, result);
        return result;
    }

    @Override
    @Trivial
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        return loadClassImpl(className, true);
    }

    @FFDCIgnore(ClassNotFoundException.class)
    @Trivial
    private Class<?> loadClassImpl(String className, boolean throwException) throws ClassNotFoundException {
        // The resolve parameter is a legacy parameter that is effectively
        // never used as of JDK 1.1 (see footnote 1 of section 5.3.2 of the 2nd
        // edition of the JVM specification).  The only caller of this method
        // is java.lang.ClassLoader.loadClass(String), and that method always
        // passes false, so we ignore the parameter.

        if (cl != null) {
            if (config.getDelegateToSystem()) {
                try {
                    // first check the bundle loader
                    return Delegation.loadClass(className, cl);
                } catch (ClassNotFoundException perfectlyNormal) {
                    // second check the system classloader
                    return jvmPackages.loadClass(className, throwException);
                }
            } else {
                return Delegation.loadClass(className, cl);
            }
        }
        Class<?> result = Delegation.loadClass(className, bLoader);
        if (result != null) {
            return result;
        }
        if (config.getDelegateToSystem()) {
            return jvmPackages.loadClass(className, throwException);
        }
        if (throwException) {
            throw new ClassNotFoundException(className);
        }
        return null;
    }


    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public Class<?> loadClassNoException(String name) {
        try {
            return loadClassImpl(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    void populateNewLoader() throws ClassLoadingConfigurationException {
        if (bundle != null) {
            synchronized (wiringMonitor) {
                if (wiring == null || !wiring.isCurrent()) {
                    wiring = bundle.adapt(BundleWiring.class);
                    if (wiring != null) {
                        ModuleClassLoader newLoader = (ModuleClassLoader) wiring.getClassLoader();
                        if (newLoader == null) {
                            throw new ClassLoadingConfigurationException("No class loader available for the gateway bundle.");
                        }
                        // This is Equinox specific stuff to avoid CNFE if possible
                        this.bLoader = newLoader.getBundleLoader();
                    }
                }
            }
        }
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

}

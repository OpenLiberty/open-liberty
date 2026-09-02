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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
    private static final TraceComponent tc = Tr.register(GatewayClassLoader.class);

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
    private final String toStringCache;

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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created GatewayClassLoader: " + result.toDiagString());
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
        this.toStringCache = toShortString();
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
        if (result != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, String.format("Resource=[%s] found at location=[%s] from liberty API packages; classloader=[%s]",
                        resName, result, this));
            }
            return result;
        }
        
        // second check the system loader
        result = jvmPackages.getResource(resName);
        if (result != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, String.format("Resource=[%s] found at location=[%s] from JVM packages; classloader=[%s]",
                        resName, result, this));
            }
            return result;
        } 
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format("Resource=[%s] not found; classloader=[%s]",
                                       resName, this));
        }      
        return null;
        
    }

    /**
     * {@inheritDoc}
     * 
     * Search order:
     * 1. Searches the bundle loader
     * 2. Searches the resource provider
     */
    @Override
    @Trivial
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
        CompositeEnumeration<URL> bundleResources = findResources(resName);
        Enumeration<URL> systemResources = jvmPackages.getResources(resName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            List<URL> bundleUrls = Collections.list(bundleResources);
            List<URL> jvmUrls = Collections.list(systemResources);
            if (!bundleUrls.isEmpty()) {
                Tr.debug(tc, String.format("Resources=[%s] found at locations=%s from liberty API packages; classloader=[%s]",
                        resName, bundleUrls, this));
            }
            if (!jvmUrls.isEmpty()) {
                Tr.debug(tc, String.format("Resources=[%s] found at locations=%s from JVM packages; classloader=[%s]",
                        resName, jvmUrls, this));
            }
            if (bundleUrls.isEmpty() && jvmUrls.isEmpty()) {
                Tr.debug(tc, String.format("Resources=[%s] not found; classloader=[%s]",
                        resName, this));
            }
            bundleUrls.addAll(jvmUrls);
            return Collections.enumeration(bundleUrls);
        }
        return bundleResources.add(systemResources);
    }

    @Override
    @Trivial
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

    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    private Class<?> loadClassImpl(String className, boolean throwException) throws ClassNotFoundException {
        // The resolve parameter is a legacy parameter that is effectively
        // never used as of JDK 1.1 (see footnote 1 of section 5.3.2 of the 2nd
        // edition of the JVM specification).  The only caller of this method
        // is java.lang.ClassLoader.loadClass(String), and that method always
        // passes false, so we ignore the parameter.

        Class<?> result = null;
        boolean fromJvmPackages = false;
        if (cl != null) {
            if (config.getDelegateToSystem()) {
                try {
                    // first check the bundle loader (liberty API packages)
                    result = Delegation.loadClass(className, cl);
                } catch (ClassNotFoundException perfectlyNormal) {
                    // second check the system classloader (JVM packages)
                    result = jvmPackages.loadClass(className, throwException);
                    fromJvmPackages = true;
                }
            } else {
                result = Delegation.loadClass(className, cl);
            }
        } else {
            // liberty API packages via OSGi bundle loader
            result = Delegation.loadClass(className, bLoader);
            if (result == null) {
                if (config.getDelegateToSystem()) {
                    result = jvmPackages.loadClass(className, throwException);
                    fromJvmPackages = true;
                } else if (throwException) {
                    throw new ClassNotFoundException(className);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (result != null) {
                final Class<?> result0 = result;
                ClassLoader definingLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) result0::getClassLoader);
                ProtectionDomain pd = AccessController.doPrivileged((PrivilegedAction<ProtectionDomain>) result0::getProtectionDomain);
                String location = pd.getCodeSource() != null ? String.valueOf(pd.getCodeSource().getLocation()) : "unknown";
                String message = "";
                if (definingLoader != null) {
                    message = fromJvmPackages ? "loaded from JVM packages" : "loaded from liberty API packages";
                } else {
                    message = "loaded by bootstrap loader";
                }
                
                Tr.debug(tc, String.format("Class=[%s] %s; classloader=[%s]; location=[%s]",
                                           className, message,
                                           definingLoader != null ? definingLoader.toString() : "bootstrap",
                                           location));
            } else {
                Tr.debug(tc, String.format("Class=[%s] failed to load; classloader=[%s]", className, this));
            }
        }
        return result;
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
    @Trivial
    public Bundle getBundle() {
        return bundle;
    }

    @Trivial
    private String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GatewayClassLoader@");
        sb.append(Integer.toHexString(this.hashCode()));
        if (bundle != null) {
            sb.append(":bundle=[").append(bundle.getSymbolicName());
            sb.append(":").append(bundle.getVersion()).append("]");
        }
        
        return sb.toString();
    }
    
    @Trivial
    public String toDiagString() {
        StringBuilder sb = new StringBuilder(toShortString());
        
        if (config.getApiTypeVisibility() != null) {
            sb.append(":apis=").append(config.getApiTypeVisibility());
        }
        if (config.getDelegateToSystem()) {
            sb.append(":delegateToSystem=true");
        }
        
        return sb.toString();
    }

    @Override
    @Trivial
    public String toString() {
        return toStringCache;
    }

}

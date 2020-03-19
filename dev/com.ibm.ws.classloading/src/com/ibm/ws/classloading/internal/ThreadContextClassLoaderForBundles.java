/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * Created for defect 83996. A wrapped UnifiedClassLoader for
 * OSGi application classloaders that implement BundleReference
 */
public class ThreadContextClassLoaderForBundles extends ThreadContextClassLoader implements BundleReference
{
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public ThreadContextClassLoaderForBundles(GatewayClassLoader augLoader, ClassLoader appLoader, String key, ClassLoadingServiceImpl clSvc) {
        super(augLoader, appLoader, key, clSvc);
        _bundleClassLoader = (BundleReference) appLoader;
    }

    private final BundleReference _bundleClassLoader;

    @Override
    public Bundle getBundle() {
        return _bundleClassLoader.getBundle();
    }

    /*********************************************************************************/
    /** Override classloading related methods so this class shows up in stacktraces **/
    /*********************************************************************************/
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    @Override
    protected URL findResource(String arg0) {
        return super.findResource(arg0);
    }

    @Override
    protected Enumeration<URL> findResources(String arg0) throws IOException {
        return super.findResources(arg0);
    }
}

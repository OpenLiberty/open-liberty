/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.equinox.module.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.EquinoxClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;

import com.ibm.wsspi.kernel.equinox.module.ModuleDelegateClassLoaderFactory;

/**
 * A bundle class loader that delegates to a class loader returned by {@link ModuleDelegateClassLoaderFactory#getDelegateClassLoader(org.osgi.framework.Bundle)}.
 * The purpose of this class loader is to simply delegate local class/resource lookups to the
 * delegate class loader.
 */
public class ModuleDelegateClassLoader extends EquinoxClassLoader {
    private final ClassLoader delegateClassLoader;

    /**
     * @param parent the parent class loader; this is handed to us from the framework, needed to create an EquinoxClassLoader
     * @param configuration the configuration handed to us from the framework, needed to create an EquinoxClassLoader
     * @param delegate the BundleLoader handed to us from the framework, needed to create an EquinoxClassLoader
     * @param generation the generation handed to us from the framework, needed to create an EquinoxClassLoader
     * @param delegateClassLoader the delegate class loader gotten from {@link ModuleDelegateClassLoaderFactory#getDelegateClassLoader(org.osgi.framework.Bundle)}.
     */
    public ModuleDelegateClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation, ClassLoader delegateClassLoader) {
        super(parent, configuration, delegate, generation);
        this.delegateClassLoader = delegateClassLoader;
    }

    @Override
    public Class<?> findLocalClass(String classname) throws ClassNotFoundException {
        return delegateClassLoader.loadClass(classname);
    }

    @Override
    public URL findLocalResource(String resource) {
        return delegateClassLoader.getResource(resource);
    }

    @Override
    public Enumeration<URL> findLocalResources(String resource) {
        try {
            return delegateClassLoader.getResources(resource);
        } catch (IOException e) {
            // auto-FFDC; this is unexpected and we don't want to propagate up
            return Collections.enumeration(Collections.<URL> emptyList());
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.internal.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Child-first delegating classloader.
 * Delegates bootstrap packages to this (the application classloader), to
 * allow com.ibm.ws.kernel.launch classes to access already loaded
 * instances.
 */
public final class BootstrapChildFirstURLClassloader extends URLClassLoader {
    /**
     * Delegates to constructor of superclass (URLClassLoader)
     * 
     * @param urls
     *            the URLs from which to load classes and resources
     * @param parent
     *            the parent class loader for delegation
     * 
     * @throws java.lang.SecurityException
     *             if a security manager exists and its
     *             checkCreateClassLoader method doesn't allow creation of a
     *             class loader.
     */
    public BootstrapChildFirstURLClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    // NOTE that the rest of the methods in this class are duplicated in 
    // com.ibm.ws.kernel.internal.classloader.BootstrapChildFirstJarClassloader
    // Any changes must be made to both sources
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = null;

        if (name == null || name.length() == 0)
            return null;

        result = findLoadedClass(name);
        if (result == null) {
            if (name.startsWith(BootstrapChildFirstJarClassloader.KERNEL_BOOT_CLASS_PREFIX))
                result = super.loadClass(name, resolve);
            else {
                try {
                    // Try to load the class from this classpath
                    result = findClass(name);
                } catch (ClassNotFoundException cnfe) {
                    result = super.loadClass(name, resolve);
                }
            }
        }

        return result;
    }

    @Override
    public URL getResource(String name) {
        if (name == null || name.length() == 0)
            return null;

        URL result = null;
        if (name.startsWith(BootstrapChildFirstJarClassloader.KERNEL_BOOT_RESOURCE_PREFIX)) {
            result = super.getResource(name);
        } else {
            // Try to get the resource from this classpath
            result = super.findResource(name);
            if (result == null) {
                result = super.getResource(name);
            }
        }
        return result;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name == null || name.length() == 0)
            return Collections.<URL> enumeration(Collections.<URL> emptyList());
        Enumeration<URL> parentResources = super.getResources(name);
        Enumeration<URL> localResources = super.findResources(name);
        if (name.startsWith(BootstrapChildFirstJarClassloader.KERNEL_BOOT_RESOURCE_PREFIX)) {
            return BootstrapChildFirstJarClassloader.compoundEnumerations(parentResources, localResources);
        } else {
            return BootstrapChildFirstJarClassloader.compoundEnumerations(localResources, parentResources);
        }
    }

    @Override
    public Enumeration<URL> findResources(String resourceName) throws IOException {
        return Collections.<URL> enumeration(Collections.<URL> emptyList());
    }
}

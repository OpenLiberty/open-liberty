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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Child-first delegating classloader.
 * Delegates bootstrap packages to this (the application classloader), to
 * allow com.ibm.ws.kernel.launch classes to access already loaded
 * instances.
 */
public final class BootstrapChildFirstJarClassloader extends JarFileClassLoader {
    static final String KERNEL_BOOT_CLASS_PREFIX = "com.ibm.ws.kernel.boot.";
    static final String KERNEL_BOOT_RESOURCE_PREFIX = "com/ibm/ws/kernel/boot/";

    static <E> Enumeration<E> compoundEnumerations(Enumeration<E> e1, Enumeration<E> e2) {
        if (e2 == null || !e2.hasMoreElements())
            return e1;
        if (e1 == null || !e1.hasMoreElements())
            return e2;
        List<E> compoundResults = new ArrayList<E>();
        while (e1.hasMoreElements())
            compoundResults.add(e1.nextElement());
        while (e2.hasMoreElements()) {
            E item = e2.nextElement();
            if (!compoundResults.contains(item)) //don't add duplicates
                compoundResults.add(item);
        }
        return Collections.enumeration(compoundResults);
    }

    /**
     * Delegates to constructor of superclass (JarFileClassLoader)
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
    public BootstrapChildFirstJarClassloader(URL[] urls, ClassLoader parent) {
        super(urls, false, parent);
    }

    // NOTE that the rest of the methods in this class are duplicated in 
    // com.ibm.ws.kernel.internal.classloader.BootstrapChildFirstURLClassloader
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

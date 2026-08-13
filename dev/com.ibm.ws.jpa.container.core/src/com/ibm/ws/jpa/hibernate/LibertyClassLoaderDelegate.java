/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jpa.hibernate;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Liberty implementation of Hibernate's ClassLoaderService interface.
 * This class provides classloading services to Hibernate using Liberty's
 * classloading infrastructure, ensuring proper class visibility in Liberty's
 * modular OSGi environment.
 * 
 * This implementation delegates to the thread context classloader, which in
 * Liberty's JPA container is set to the application's classloader that has
 * visibility to both application classes and JPA provider classes.
 */
@Trivial
public class LibertyClassLoaderDelegate {
    private static final TraceComponent tc = Tr.register(LibertyClassLoaderDelegate.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    /**
     * Load a class by name using Liberty's classloading infrastructure.
     * 
     * @param className the fully qualified class name
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class<?> classForName(String className) throws ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "classForName: " + className);
        
        ClassLoader cl = getClassLoader();
        return Class.forName(className, true, cl);
    }

    /**
     * Get the appropriate classloader for Hibernate to use.
     * In Liberty's JPA container, the thread context classloader is set to
     * the application's classloader which has visibility to both application
     * classes and JPA provider classes.
     * 
     * @return the ClassLoader to use for loading classes
     */
    public static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = LibertyClassLoaderDelegate.class.getClassLoader();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getClassLoader: " + cl);
        return cl;
    }

    /**
     * Locate a resource by name.
     * 
     * @param resourceName the resource name
     * @return URL to the resource, or null if not found
     */
    public static URL locateResource(String resourceName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "locateResource: " + resourceName);
        
        ClassLoader cl = getClassLoader();
        return cl.getResource(resourceName);
    }

    /**
     * Locate a resource as a stream.
     * 
     * @param resourceName the resource name
     * @return InputStream to the resource, or null if not found
     */
    public static InputStream locateResourceStream(String resourceName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "locateResourceStream: " + resourceName);
        
        ClassLoader cl = getClassLoader();
        return cl.getResourceAsStream(resourceName);
    }

    /**
     * Locate all resources with the given name.
     * 
     * @param resourceName the resource name
     * @return Collection of URLs to matching resources
     */
    public static Collection<URL> locateResources(String resourceName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "locateResources: " + resourceName);
        
        try {
            ClassLoader cl = getClassLoader();
            Enumeration<URL> urls = cl.getResources(resourceName);
            return Collections.list(urls);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Error locating resources: " + resourceName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Load a class by name, returning null if not found instead of throwing exception.
     * 
     * @param className the fully qualified class name
     * @return the loaded Class object, or null if not found
     */
    public static Class<?> classForNameOrNull(String className) {
        try {
            return classForName(className);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Class not found: " + className);
            return null;
        }
    }

    /**
     * Generate a new aggregate classloader capable of loading from multiple classloaders.
     * In Liberty's environment, we return the thread context classloader which already
     * has the necessary visibility.
     * 
     * @param classLoaders the classloaders to aggregate
     * @return an aggregate ClassLoader
     */
    public static ClassLoader createAggregateClassLoader(ClassLoader... classLoaders) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "createAggregateClassLoader called, returning thread context classloader");
        
        // In Liberty's JPA container, the thread context classloader already has
        // visibility to all necessary classes, so we don't need to create a new
        // aggregate classloader
        return getClassLoader();
    }
}


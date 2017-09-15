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
package com.ibm.ws.classloading.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.ws.classloading.internal.util.MultiMap;
import com.ibm.wsspi.classloading.ResourceProvider;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;

/**
 * A collection of resource providers that can searched for a named resource.
 */
// The search is made efficient by pre-calculating the list of providers for each named resource.
class CompositeResourceProvider {

    private final ReadWriteLock lockFactory = new ReentrantReadWriteLock();
    private final MultiMap<String, ResourceProvider> providerMap = new MultiMap<String, ResourceProvider>();

    void add(ResourceProvider provider) {
        Lock lock = this.lockFactory.writeLock();
        lock.lock();
        try {
            for (String res : provider.getResourceNames())
                providerMap.add(Util.normalizeResourceName(res), provider);
        } finally {
            lock.unlock();
        }
    }

    void remove(ResourceProvider provider) {
        Lock lock = this.lockFactory.writeLock();
        lock.lock();
        try {
            for (String res : provider.getResourceNames())
                providerMap.remove(Util.normalizeResourceName(res), provider);
        } finally {
            lock.unlock();
        }
    }

    private Set<ResourceProvider> getProviders(String resourceName) {
        Lock lock = this.lockFactory.readLock();
        lock.lock();
        try {
            return providerMap.get(resourceName);
        } finally {
            lock.unlock();
        }
    }

    URL findResource(String resourceName) throws SecurityException {
        resourceName = Util.normalizeResourceName(resourceName);
        for (ResourceProvider p : getProviders(resourceName)) {
            URL u = p.findResource(resourceName);
            if (u != null)
                return u;
        }
        return null;
    }

    CompositeEnumeration<URL> findResources(String resourceName, CompositeEnumeration<URL> enumerations) throws SecurityException, IOException {
        resourceName = Util.normalizeResourceName(resourceName);
        for (ResourceProvider p : getProviders(resourceName))
            enumerations.add(p.findResources(resourceName));
        return enumerations;
    }

    /**
     *
     * @see com.ibm.ws.classloading.internal.util.MultiMap#clear()
     */
    void clear() {
        providerMap.clear();
    }

    /**
     * Used for diagnostics in the CLSIntrospector
     */
    MultiMap<String, ResourceProvider> getProviderMap() {
        return providerMap;
    }

}

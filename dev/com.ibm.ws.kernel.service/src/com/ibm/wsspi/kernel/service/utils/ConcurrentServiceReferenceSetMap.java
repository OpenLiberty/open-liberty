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
package com.ibm.wsspi.kernel.service.utils;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * This provides a map implementation for lazy-resolution of multiple services.
 * Use this class when you have frequent iteration with infrequent add/removal.
 * Entries are not stored in any particular order, but services with the same
 * key are stored in reverse order of {@link ServiceReference#compareTo}: highest
 * service.ranking then lowest (first) service.id.
 * <p>
 * Usage (following OSGi DS naming conventions/patterns):
 * 
 * <code><pre>
 * private final ConcurrentServiceReferenceSetMap&ltK,V&gt serviceSet = new ConcurrentServiceReferenceSet&ltT&gt("referenceName");
 * 
 * protected void activate(ComponentContext ctx) {
 * &nbsp;serviceSet.activate(ctx);
 * }
 * 
 * protected void deactivate(ComponentContext ctx) {
 * &nbsp;serviceSet.deactivate(ctx);
 * }
 * 
 * protected void setReferenceName(ServiceReference&ltV&gt ref) {
 * &nbsp;K key;
 * &nbsp;serviceMap.putReference(key, ref);
 * }
 * 
 * protected void unsetReferenceName(ServiceReference&ltV&gt ref) {
 * &nbsp;K key;
 * &nbsp;serviceMap.removeReference(key, ref);
 * }
 * 
 * public Iterator&ltT&gt getServices() {
 * &nbsp;return serviceSet.getServices();
 * }
 * </pre></code>
 * 
 * 
 */
public class ConcurrentServiceReferenceSetMap<K, V> {

    private final String referenceName;

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<ComponentContext>();

    // map of sets of servicereferences, sorted by the rank order of the service reference
    private final ConcurrentMap<K, ConcurrentServiceReferenceSet<V>> elementMap =
                    new ConcurrentHashMap<K, ConcurrentServiceReferenceSet<V>>();

    /**
     * Create a new ConcurrentServiceReferenceMap for the named service.
     * e.g. from bnd.bnd: referenceName=.... or from component.xml: <reference name="referenceName".... >
     * 
     * @param name Name of DS reference
     */
    public ConcurrentServiceReferenceSetMap(String name) {
        referenceName = name;
    }

    public void activate(ComponentContext context) {
        if (contextRef.get() != null) {
            // Components are never activated twice.  This is either DS or
            // programmer error.
            throw new IllegalStateException("already activated");
        }

        this.contextRef.set(context);
    }

    /**
     * Deactivates the map. Will trigger a release of all held services.
     */
    public void deactivate(ComponentContext context) {
        if (contextRef.get() == null) {
            // Components are only deactivated if they activate successfully,
            // and component instances are discarded after being deactivated.
            // This is either DS or programmer error.
            throw new IllegalStateException("not activated");
        }

        this.contextRef.set(null);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[refName=" + referenceName
               + ", isEmpty=" + elementMap.isEmpty()
               + ", isActive=" + (contextRef.get() != null)
               + "]";
    }

    /**
     * Associates the reference with the key.
     * 
     * @param key Key associated with this reference
     * @param reference ServiceReference for the target service
     * @return true if this is replacing a previous (non-null) service reference
     */
    public boolean putReference(K key, ServiceReference<V> reference) {
        if (key == null || reference == null)
            return false;

        ConcurrentServiceReferenceSet<V> csrs = elementMap.get(key);
        if (csrs == null) {
            // No set for this key: allocate a new set that shares a context reference.
            ConcurrentServiceReferenceSet<V> newSet = new ConcurrentServiceReferenceSet<V>(referenceName, contextRef);

            // Try to add the set to the map: the value returned will be non-null if something else beat us there.
            ConcurrentServiceReferenceSet<V> currentSet = elementMap.putIfAbsent(key, newSet);
            csrs = (currentSet == null) ? newSet : currentSet;
        }

        return csrs.addReference(reference);
    }

    /**
     * Removes the reference associated with the key.
     * 
     * @param key Key associated with this reference
     * @param reference ServiceReference for the target service
     * @return true if reference was unset (not previously replaced)
     */
    public boolean removeReference(K key, ServiceReference<V> reference) {
        if (key == null)
            return false;

        ConcurrentServiceReferenceSet<V> csrs = elementMap.get(key);
        if (csrs == null) {
            return false;
        }
        return csrs.removeReference(reference);
    }

    /**
     * Check if there are any registered/added service references: this will return
     * true if all the known sets for all known keys report as empty.
     * 
     * @return true if the known sets of registered service references are empty.
     * 
     */
    public boolean isEmpty() {
        boolean empty = true;
        boolean done = false;
        while (empty && !done) {
            for (ConcurrentServiceReferenceSet<V> s : elementMap.values()) {
                empty &= s.isEmpty();
            }
            done = true;
        }
        return empty;
    }

    /**
     * Retrieve an iterator for the services associated with the given key.<br>
     * Services are returned in service rank order.
     * 
     * @param key The key associated with the requested service
     * @return Iterator for services if any available, null otherwise.
     */
    public Iterator<V> getServices(K key) {
        ComponentContext ctx = contextRef.get();

        if (ctx == null || key == null)
            return null;

        ConcurrentServiceReferenceSet<V> e = elementMap.get(key);
        if (e == null || e.isEmpty()) {
            return null;
        } else {
            return e.getServices();
        }
    }

    /**
     * Retrieve an iterator for service & service reference pairs for a given key<br>
     * Services are returned in service rank order.<br>
     * Service References are available to query properties etc.<br>
     * 
     * @param key The key associated with the requested service
     * @return Iterator supplying pairs of service & service reference if any services are available, null otherwise.
     */
    public Iterator<ServiceAndServiceReferencePair<V>> getServicesWithReferences(K key) {
        ComponentContext ctx = contextRef.get();

        if (ctx == null || key == null)
            return null;

        ConcurrentServiceReferenceSet<V> e = elementMap.get(key);
        if (e == null || e.isEmpty()) {
            return null;
        } else {
            return e.getServicesWithReferences();
        }
    }

}

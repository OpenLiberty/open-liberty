/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * This provides a simple map implementation for lazy-resolution of services.
 * Use this class when you have frequent retrieval with infrequent add/removal.
 * Entries are not stored in any particular order.
 * <p>
 * Usage (following OSGi DS naming conventions/patterns):
 * <code><pre>
 * private final ConcurrentServiceReferenceMap&ltK,V&gt serviceMap = new ConcurrentServiceReferenceMap&ltK,V&gt("referenceName");
 * 
 * protected void activate(ComponentContext ctx) {
 * &nbsp;serviceMap.activate(ctx);
 * }
 * 
 * protected void deactivate(ComponentContext ctx) {
 * &nbsp;serviceMap.deactivate(ctx);
 * }
 * 
 * protected void setReferenceName(ServiceReference&ltV&gt ref) {
 * &nbsp;K key;
 * &nbsp;serviceMap.addReference(key, ref);
 * }
 * 
 * protected void unsetReferenceName(ServiceReference&ltV&gt ref) {
 * &nbsp;K key;
 * &nbsp;serviceMap.removeReference(key, ref);
 * }
 * 
 * public ServiceReference&ltV&gt getReferenceName(K key) {
 * &nbsp;return serviceMap.getServices(key);
 * }
 * </pre></code>
 * 
 */
public class ConcurrentServiceReferenceMap<K, V> {

    private final String referenceName;

    private volatile ComponentContext context;

    // Sort elements by the rank order of the service reference
    private final ConcurrentMap<K, ConcurrentServiceReferenceElement<V>> elementMap =
                    new ConcurrentHashMap<K, ConcurrentServiceReferenceElement<V>>();

    /**
     * Create a new ConcurrentServiceReferenceMap for the named service.
     * e.g. from bnd.bnd: referenceName=.... or from component.xml: <reference name="referenceName".... >
     * 
     * @param name Name of DS reference
     */
    public ConcurrentServiceReferenceMap(String name) {
        referenceName = name;
    }

    public void activate(ComponentContext context) {
        if (this.context != null) {
            // Components are never activated twice.  This is either DS or
            // programmer error.
            throw new IllegalStateException("already activated");
        }

        // Set the current/active component context
        this.context = context;
    }

    /**
     * Deactivates the map.
     */
    public synchronized void deactivate(ComponentContext context) {
        if (this.context == null) {
            // Components are only deactivated if they activate successfully,
            // and component instances are discarded after being deactivated.
            // This is either DS or programmer error.
            throw new IllegalStateException("not activated");
        }

        this.context = null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[refName=" + referenceName
               + ", isEmpty=" + elementMap.isEmpty()
               + ", isActive=" + (context != null)
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

        ConcurrentServiceReferenceElement<V> element = new ConcurrentServiceReferenceElement<V>(referenceName, reference);
        return (elementMap.put(key, element) != null);
    }

    /**
     * Associates the reference with the key but only if there is not an
     * existing reference associated with that key. It will only attempt to add
     * the reference to the map if <code>key</code> is not <code>null</code>.
     * 
     * @param key Key associated with this reference
     * @param reference ServiceReference for the target service
     * @return The service reference that was previously associated with the key or <code>null</code> otherwise
     * @see ConcurrentMap#putIfAbsent(Object, Object)
     */
    public ServiceReference<V> putReferenceIfAbsent(K key, ServiceReference<V> reference) {
        // If the key is null we can't do anything
        if (key == null)
            return null;
        if (reference == null) {
            // If the reference is null we can't add it to the map but we could still return an existing on if there was one so check this
            return getReference(key);
        }

        ConcurrentServiceReferenceElement<V> element = new ConcurrentServiceReferenceElement<V>(referenceName, reference);
        ConcurrentServiceReferenceElement<V> existingEntry = elementMap.putIfAbsent(key, element);
        if (existingEntry == null) {
            return null;
        } else {
            return existingEntry.getReference();
        }
    }

    /**
     * Removes the reference associated with the key.
     * 
     * @param key Key associated with this reference
     * @param reference ServiceReference associated with service to be unset.
     * @return true if reference was unset (not previously replaced)
     */
    public boolean removeReference(K key, ServiceReference<V> reference) {
        if (key == null || reference == null)
            return false;
        ConcurrentServiceReferenceElement<V> element = new ConcurrentServiceReferenceElement<V>(referenceName, reference);
        return elementMap.remove(key, element);
    }

    /**
     * Check if there are any registered/added service references: this will return
     * true if the set is empty (none available). If the set is not
     * empty, the services will only be resolvable if there is a viable
     * component context.
     * 
     * @return true if the list of registered service references is empty.
     * 
     */
    public boolean isEmpty() {
        return elementMap.isEmpty();
    }

    /**
     * Answers the number of elements in this Map.
     * 
     * @return the number of elements in this Map
     */
    public int size() {
        return elementMap.size();
    }

    /**
     * Answers a Set of the keys contained in this Map in no specific order.
     * The set is backed by this Map so changes to one are reflected by the
     * other. The set does not support adding.
     * 
     * @return a Set of the keys
     */
    public Set<K> keySet() {
        return elementMap.keySet();
    }

    public Iterable<ServiceReference<V>> references() {
        return new Iterable<ServiceReference<V>>() {

            @Override
            public Iterator<ServiceReference<V>> iterator() {
                return new Iterator<ServiceReference<V>>() {
                    Iterator<ConcurrentServiceReferenceElement<V>> it = elementMap.values().iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public ServiceReference<V> next() {
                        ConcurrentServiceReferenceElement<V> e = it.next();
                        return e.getReference();
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    /**
     * Retrieve the service associated with key.
     * 
     * @param key The key associated with the requested service
     * @return The service if available, null otherwise.
     */
    public V getService(K key) {
        ComponentContext ctx = context;

        if (ctx == null || key == null)
            return null;

        ConcurrentServiceReferenceElement<V> e = elementMap.get(key);
        if (e == null) {
            return null;
        } else {
            return e.getService(ctx);
        }
    }

    /**
     * @return T
     * @throws IllegalStateException if the internal state is such that
     *             locating the service is not possible or if the service
     *             is not retrievable
     */
    public V getServiceWithException(K key) {
        ComponentContext ctx = context;

        if (ctx == null)
            throw new IllegalStateException("context is null");
        if (key == null)
            throw new IllegalStateException("key is null");

        ConcurrentServiceReferenceElement<V> e = elementMap.get(key);
        if (e == null)
            throw new IllegalStateException("No such element for " + key);

        V service = e.getService(ctx);
        if (service == null)
            throw new IllegalStateException("Located service is null: " + e.getReference());

        return service;
    }

    /**
     * Returns the ServiceReference associated with key
     * 
     * @param key The key associated with the service
     * @return ServiceRerefence associated with key, or null
     */
    public ServiceReference<V> getReference(K key) {
        if (key == null)
            return null;

        ConcurrentServiceReferenceElement<V> e = elementMap.get(key);
        if (e == null) {
            return null;
        } else {
            return e.getReference();
        }
    }

    /**
     * Iterate over all services in the map in no specific order. The iterator
     * will return the service associated with each ServiceReference as it progresses.
     * Creation of the iterator does not eagerly resolve services: resolution
     * is done only once per service reference, and only when "next" would
     * retrieve that service.
     */
    public Iterator<V> getServices() {
        final List<V> empty = Collections.emptyList();

        if (context == null) {
            return empty.iterator();
        }

        return new ValueIterator(elementMap.values().iterator());
    }

    private final class ValueIterator implements Iterator<V> {
        final Iterator<ConcurrentServiceReferenceElement<V>> c_refs;
        V next;

        ValueIterator(Iterator<ConcurrentServiceReferenceElement<V>> iterator) {
            c_refs = iterator;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            // If the context is null because the component was deactivated, we have no next.
            ComponentContext ctx = context;
            if (ctx == null) {
                return false;
            }

            while (next == null && c_refs.hasNext()) {
                next = c_refs.next().getService(ctx);
            }

            return next != null;
        }

        /** {@inheritDoc} */
        @Override
        public V next() {
            // We assume the user has called hasNext.
            V service = next;
            if (service == null) {
                throw new NoSuchElementException();
            }

            next = null;
            return service;
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName()
                   + "[next=" + next
                   + "]";
        }
    }
}

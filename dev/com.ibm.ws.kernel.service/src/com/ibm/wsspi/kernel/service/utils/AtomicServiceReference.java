/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.service.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Small class performing atomic operations to find/retrieve/cache
 * the instance of a DeclarativeServices component reference from
 * service registry via the service reference.
 * <p>
 * Use this to maintain a lazy-resolved reference of cardinality one
 * (single service reference associated with a single, lazily-resolved
 * service instance).
 * <p>
 * Usage (following OSGi DS naming conventions/patterns):
 * <code>
 *
 * <pre>
 * private final AtomicServiceReference&ltT&gt serviceRef = new AtomicServiceReference&ltT&gt("referenceName");
 *
 * protected void activate(ComponentContext ctx) {
 * &nbsp;serviceRef.activate(ctx);
 * }
 *
 * protected void deactivate(ComponentContext ctx) {
 * &nbsp;serviceRef.deactivate(ctx);
 * }
 *
 * protected void setReferenceName(ServiceReference&ltT&gt ref) {
 * &nbsp;serviceRef.setReference(ref);
 * }
 *
 * protected void unsetReferenceName(ServiceReference&ltT&gt ref) {
 * &nbsp;serviceRef.unsetReference(ref);
 * }
 *
 * private T getReferenceName() {
 * &nbsp;return serviceRef.getService();
 * }
 * </pre>
 *
 * </code>
 */
public class AtomicServiceReference<T> {
    private final String referenceName;

    private final AtomicReference<ComponentContext> contextRef = new AtomicReference<>();

    static class ReferenceTuple<T> implements Comparable<ReferenceTuple<T>> {
        final ServiceReference<T> serviceRef;
        final AtomicReference<T> locatedService = new AtomicReference<>();

        ReferenceTuple(ServiceReference<T> ref) {
            serviceRef = ref;
        }

        @Override
        public String toString() {
            return "ref=" + serviceRef + ",svc=" + locatedService;
        }

        @Override
        public int compareTo(ReferenceTuple<T> o) {
            // reverse sort because priority queue peeks the least
            return -(this.serviceRef.compareTo(o.serviceRef));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object o) {
            if (o instanceof ReferenceTuple) {
                return this.serviceRef.equals(((ReferenceTuple) o).serviceRef);
            }
            return false;
        }
    }

    // size of 2; it will be very uncommon to this to be used with more than 2 at a time
    private final PriorityQueue<ReferenceTuple<T>> references = new PriorityQueue<>(2);
    private final ReadWriteLock referencesLock = new ReentrantReadWriteLock();

    /**
     * Create a new AtomicServiceReference for the named service.
     * e.g. from bnd.bnd: referenceName=.... or from component.xml: <reference name="referenceName".... >
     *
     * @param name Name of DS reference
     */
    public AtomicServiceReference(String name) {
        referenceName = name;
    }

    public void activate(ComponentContext context) {
        contextRef.set(context);
    }

    public void deactivate(ComponentContext context) {
        contextRef.set(null);
        referencesLock.writeLock().lock();
        try {
            // clear services located from the context that is deactivating
            references.forEach((r) -> r.locatedService.set(null));
        } finally {
            referencesLock.writeLock().unlock();
        }
    }

    /**
     * Update service reference associated with this service.
     *
     * @param referenceName ServiceReference for the target service. Service references are
     *                          equal if they point to the same service registration, and are ordered by
     *                          increasing service.ranking and decreasing service.id. ServiceReferences hold
     *                          no service properties: requests/queries for properties are forwarded onto
     *                          the backing service registration.
     * @return true if this is replacing a previous (non-null) service reference
     */
    public boolean setReference(ServiceReference<T> reference) {
        referencesLock.writeLock().lock();
        try {
            // TODO not sure null should be handled
            if (reference == null) {
                boolean isEmpty = references.isEmpty();
                // if null just remove all references
                references.clear();
                return !isEmpty;
            }
            Iterator<ReferenceTuple<T>> iRefs = references.iterator();
            ReferenceTuple<T> highest = null;
            ReferenceTuple<T> existing = null;
            while (iRefs.hasNext()) {
                ReferenceTuple<T> next = iRefs.next();
                if (highest == null) {
                    highest = next;
                }
                if (reference.equals(next.serviceRef)) {
                    iRefs.remove();
                    existing = next;
                    break;
                }
            }
            ReferenceTuple<T> newTuple = existing != null ? existing : new ReferenceTuple<>(reference);
            references.add(newTuple);
            return highest != null && highest != references.peek();
        } finally {
            referencesLock.writeLock().unlock();
        }
    }

    /**
     * Clear the service reference associated with this service. This first
     * checks to see whether or not the reference being unset matches the
     * current reference. For Declarative Services dynamic components: if a replacement
     * is available for a dynamic reference, DS will call set with the new
     * reference before calling unset to clear the old one.
     *
     * @param reference ServiceReference associated with service to be unset.
     * @return true if a non-null value was replaced
     */
    public boolean unsetReference(ServiceReference<T> reference) {
        referencesLock.writeLock().lock();
        try {
            // TODO not sure null should be handled
            if (reference == null) {
                // nothing really to do
                return false;
            }
            Iterator<ReferenceTuple<T>> iRefs = references.iterator();
            ReferenceTuple<T> highest = null;
            while (iRefs.hasNext()) {
                ReferenceTuple<T> next = iRefs.next();
                if (highest == null) {
                    highest = next;
                }
                if (reference.equals(next.serviceRef)) {
                    iRefs.remove();
                    break;
                }
            }
            return highest != null && highest.serviceRef.equals(reference);
        } finally {
            referencesLock.writeLock().unlock();
        }
    }

    /**
     * @return ServiceReference for the target service. Service references are
     *         equal if they point to the same service registration, and are ordered by
     *         increasing service.ranking and decreasing service.id. ServiceReferences hold
     *         no service properties: requests/queries for properties are forwarded onto
     *         the backing service registration.
     */
    public ServiceReference<T> getReference() {
        referencesLock.readLock().lock();
        try {
            ReferenceTuple<T> highest = references.peek();
            return highest == null ? null : highest.serviceRef;
        } finally {
            referencesLock.readLock().unlock();
        }
    }

    /**
     * @return T or null if unavailable
     */
    public T getService() {
        return getService(false);
    }

    /**
     * @return T
     * @throws IllegalStateException if the internal state is such that
     *                                   locating the service is not possible or if the service
     *                                   is not retrievable
     */
    public T getServiceWithException() {
        return getService(true);
    }

    /**
     * Try to locate the service
     *
     * @param throwException if true, throw exception when required services are
     *                           missing
     * @return T or null if unavailable
     */
    @SuppressWarnings("unchecked")
    private T getService(boolean throwException) {

        final ComponentContext currentContext = contextRef.get();
        if (currentContext == null) {
            if (throwException) {
                throw new IllegalStateException("Not activated yet: " + toString());
            }
            return null;
        }

        final ReferenceTuple<T> highest;
        referencesLock.readLock().lock();
        try {
            highest = references.peek();
        } finally {
            referencesLock.readLock().unlock();
        }
        if (highest == null) {
            if (throwException) {
                throw new IllegalStateException("No service reference available: " + toString());
            }
            return null;
        }

        T svc = highest.locatedService.get();
        if (svc != null) {
            return svc;
        }
        // We have to locate / resolve the service from the reference
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            svc = AccessController.doPrivileged(new PrivilegedAction<T>() {

                @Override
                public T run() {
                    return currentContext.locateService(referenceName, highest.serviceRef);
                }
            });
        } else {
            svc = currentContext.locateService(referenceName, highest.serviceRef);
        }

        // if we're asked to throw, throw if we couldn't find the service
        if (svc == null) {
            if (throwException) {
                throw new IllegalStateException("Located service is null," + toString());
            }
        }
        highest.locatedService.set(svc);
        return svc;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[name=" + referenceName
               + "," + references
               + "]";
    }
}

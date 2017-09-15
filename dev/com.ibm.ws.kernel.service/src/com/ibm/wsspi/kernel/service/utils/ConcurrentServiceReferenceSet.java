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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * This provides a simple set implementation for lazy-resolution of services.
 * Use this class when you have frequent iteration with infrequent add/removal.
 * Services are stored in reverse order of {@link ServiceReference#compareTo}: highest
 * service.ranking then lowest (first) service.id.
 * <p>
 * Usage (following OSGi DS naming conventions/patterns):
 * <code><pre>
 * private final ConcurrentServiceReferenceSet&ltT&gt serviceSet = new ConcurrentServiceReferenceSet&ltT&gt("referenceName");
 * 
 * protected void activate(ComponentContext ctx) {
 * &nbsp;serviceSet.activate(ctx);
 * }
 * 
 * protected void deactivate(ComponentContext ctx) {
 * &nbsp;serviceSet.deactivate(ctx);
 * }
 * 
 * protected void setReferenceName(ServiceReference&ltT&gt ref) {
 * &nbsp;serviceSet.addReference(ref);
 * }
 * 
 * protected void unsetReferenceName(ServiceReference&ltT&gt ref) {
 * &nbsp;serviceSet.removeReference(ref);
 * }
 * 
 * public Iterator&ltT&gt getReferenceName() {
 * &nbsp;return serviceSet.getServices();
 * }
 * </pre></code>
 * 
 */
public class ConcurrentServiceReferenceSet<T> {

    private final String referenceName;

    private final AtomicReference<ComponentContext> contextRef;

    /**
     * Map of service reference to element. Modifications should only be made
     * while holding a lock on this field.
     */
    private final Map<ServiceReference<T>, ConcurrentServiceReferenceElement<T>> elementMap =
                    new LinkedHashMap<ServiceReference<T>, ConcurrentServiceReferenceElement<T>>();

    /**
     * Set of services in {@link #elementMap}, sorted by descending service
     * ranking and ascending service id. We use a concurrent data structure to
     * allow iteration while updating, but changes should only be made while
     * holding a lock on {@link #elementMap} to ensure consistency between.
     */
    private ConcurrentSkipListSet<ConcurrentServiceReferenceElement<T>> elementSet =
                    new ConcurrentSkipListSet<ConcurrentServiceReferenceElement<T>>();

    /**
     * True if {@link #elementSet} needs to be refreshed from {@link #elementMap} because the service ranking of an element changed.
     * This field should only be modified while holding a lock on {@link #elementMap}. Resorting requires recreating the set, which is
     * relatively expensive, so we do it lazily.
     */
    private boolean elementSetUnsorted;

    /**
     * Create a new ConcurrentServiceReferenceSet for the named service.
     * e.g. from bnd.bnd: referenceName=.... or from component.xml: <reference name="referenceName".... >
     * 
     * @param name Name of DS reference
     */
    public ConcurrentServiceReferenceSet(String name) {
        this(name, new AtomicReference<ComponentContext>());
    }

    ConcurrentServiceReferenceSet(String name, AtomicReference<ComponentContext> contextRef) {
        referenceName = name;
        this.contextRef = contextRef;
    }

    public void activate(ComponentContext context) {
        if (contextRef.get() != null) {
            // Components are never activated twice.  This is either DS or
            // programmer error.
            throw new IllegalStateException("already activated");
        }

        contextRef.set(context);
    }

    public void deactivate(ComponentContext context) {
        if (contextRef.get() == null) {
            // Components are only deactivated if they activate successfully,
            // and component instances are discarded after being deactivated.
            // This is either DS or programmer error.
            throw new IllegalStateException("not activated");
        }

        contextRef.set(null);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[refName=" + referenceName
               + ", isEmpty=" + isEmpty()
               + ", isActive=" + isActive()
               + "]";
    }

    /**
     * Adds the service reference to the set, or notifies the set that the
     * service ranking for the reference might have been updated.
     * 
     * @param reference ServiceReference for the target service
     * @return true if this set already contained the service reference
     */
    public boolean addReference(ServiceReference<T> reference) {
        if (reference == null)
            return false;

        ConcurrentServiceReferenceElement<T> element = new ConcurrentServiceReferenceElement<T>(referenceName, reference);

        synchronized (elementMap) {
            ConcurrentServiceReferenceElement<T> oldElement = elementMap.put(reference, element);
            if (oldElement != null) {
                if (!element.getRanking().equals(oldElement.getRanking())) {
                    elementSetUnsorted = true;
                }
                return true;
            }

            elementSet.add(element);
        }
        return false;
    }

    /**
     * Removes the service reference from the set
     * 
     * @param reference ServiceReference associated with service to be unset
     * @return true if this set contained the service reference
     */
    public boolean removeReference(ServiceReference<T> reference) {
        synchronized (elementMap) {
            ConcurrentServiceReferenceElement<T> element = elementMap.remove(reference);
            if (element == null) {
                return false;
            }

            elementSet.remove(element);
            return true;
        }
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
        return elementSet.isEmpty();
    }

    public boolean isActive() {
        return contextRef.get() != null;
    }

    /**
     * Find the provided reference in the set, and return the corresponding service.
     * Subject to the same restrictions/behavior as getServices.
     * 
     * @param serviceReference Service reference to find in the set
     * @return service associated with service reference, or null if the service could not be located.
     */
    public T getService(ServiceReference<T> serviceReference) {

        if (serviceReference != null) {
            ComponentContext ctx = contextRef.get();
            if (ctx != null) {
                ConcurrentServiceReferenceElement<T> element;
                synchronized (elementMap) {
                    element = elementMap.get(serviceReference);
                }
                if (element != null) {
                    return element.getService(ctx);
                }
            }
        }
        return null;
    }

    /**
     * The ConcurrentReferenceSet is ordered by the usual service ranking rules:
     * highest service.ranking then lowest (first) service.id.
     * 
     * @return The "first" service according to the ranking
     */
    public T getHighestRankedService() {
        Iterator<T> iterator = getServices();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * The ConcurrentReferenceSet is ordered by the usual service ranking rules:
     * highest service.ranking then lowest (first) service.id.
     * 
     * @return The "first" service reference according to the ranking
     */
    public ServiceReference<T> getHighestRankedReference() {
        Iterator<ConcurrentServiceReferenceElement<T>> iterator = elementSet.iterator();
        return iterator.hasNext() ? iterator.next().getReference() : null;
    }

    /**
     * Return an iterator for the elements in service ranking order.
     */
    private Iterator<ConcurrentServiceReferenceElement<T>> elements() {
        Collection<ConcurrentServiceReferenceElement<T>> set;
        synchronized (elementMap) {
            if (elementSetUnsorted) {
                elementSet = new ConcurrentSkipListSet<ConcurrentServiceReferenceElement<T>>(elementMap.values());
                elementSetUnsorted = false;
            }
            set = elementSet;
        }

        return set.iterator();
    }

    /**
     * Allocate and return an iterator: The iterator will
     * return the service associated with each ServiceReference as it progresses.
     * Creation of the iterator does not eagerly resolve services: resolution
     * is done only once per service reference, and only when "next" would
     * retrieve that service.
     * 
     * @return
     */
    public Iterator<T> getServices() {
        final List<T> empty = Collections.emptyList();

        if (contextRef.get() == null) {
            return empty.iterator();
        }

        return new ServiceIterator(elements());
    }

    private final class ServiceIterator implements Iterator<T> {
        final Iterator<ConcurrentServiceReferenceElement<T>> c_refs;
        T next;

        ServiceIterator(Iterator<ConcurrentServiceReferenceElement<T>> iterator) {
            c_refs = iterator;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            // If the context is null because the component was deactivated, we have no next.
            ComponentContext ctx = contextRef.get();
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
        public T next() {
            // We assume the user has called hasNext.
            T service = next;
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

    /**
     * Allocate and return an iterator: The iterator will
     * return the service associated with each ServiceReference as it progresses.
     * Creation of the iterator does not eagerly resolve services: resolution
     * is done only once per service reference, and only when "next" would
     * retrieve that service.
     * 
     * @return
     */
    public Iterator<ServiceAndServiceReferencePair<T>> getServicesWithReferences() {
        final List<ServiceAndServiceReferencePair<T>> empty = Collections.emptyList();
        if (contextRef.get() == null) {
            return empty.iterator();
        }
        return new ServiceAndReferenceIterator(elements());
    }

    private final class ServiceAndReferenceIterator implements Iterator<ServiceAndServiceReferencePair<T>> {
        final Iterator<ConcurrentServiceReferenceElement<T>> c_refs;
        ServiceAndServiceReferencePair<T> next;

        ServiceAndReferenceIterator(Iterator<ConcurrentServiceReferenceElement<T>> iterator) {
            c_refs = iterator;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            // If the context is null because the component was deactivated, we have no next.
            ComponentContext ctx = contextRef.get();
            if (ctx == null) {
                return false;
            }

            while (next == null && c_refs.hasNext()) {
                final ConcurrentServiceReferenceElement<T> element = c_refs.next();
                final T service = element.getService(ctx);
                if (service != null) {
                    next = new ServiceAndServiceReferencePair<T>() {
                        /** {@inheritDoc} */
                        @Override
                        public T getService() {
                            return service;
                        }

                        /** {@inheritDoc} */
                        @Override
                        public ServiceReference<T> getServiceReference() {
                            return element.getReference();
                        }
                    };
                }
            }

            return next != null;
        }

        /** {@inheritDoc} */
        @Override
        public ServiceAndServiceReferencePair<T> next() {
            // We assume the user has called hasNext.
            ServiceAndServiceReferencePair<T> pair = next;
            if (pair == null) {
                throw new NoSuchElementException();
            }

            next = null;
            return pair;
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

    public Iterable<T> services() {
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return getServices();
            }
        };
    }

    public Iterable<ServiceReference<T>> references() {
        return new Iterable<ServiceReference<T>>() {

            @Override
            public Iterator<ServiceReference<T>> iterator() {
                return new Iterator<ServiceReference<T>>() {
                    Iterator<ConcurrentServiceReferenceElement<T>> it = elements();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public ServiceReference<T> next() {
                        ConcurrentServiceReferenceElement<T> e = it.next();
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
}

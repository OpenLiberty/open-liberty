/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;

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
 * <code><pre>
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
 * </pre></code>
 */
public class AtomicServiceReference<T> {
    private final String referenceName;

    static class ReferenceTuple<T> {
        final ComponentContext context;
        final ServiceReference<T> serviceRef;
        final T locatedService;

        ReferenceTuple(ComponentContext ctx, ServiceReference<T> ref, T svc) {
            context = ctx;
            serviceRef = ref;
            locatedService = svc;
        }

        @Override
        public String toString() {
            return "ctx=" + context + ",ref=" + serviceRef + ",svc=" + locatedService;
        }
    }

    private final AtomicReference<ReferenceTuple<T>> tuple = new AtomicReference<ReferenceTuple<T>>(new ReferenceTuple<T>(null, null, null));

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
        ReferenceTuple<T> previous = null;
        ReferenceTuple<T> newTuple = null;

        // Try this until we either manage to make our update or we know we don't have to
        do {
            // Get the current tuple
            previous = tuple.get();

            // If we don't have a previous value
            newTuple = new ReferenceTuple<T>(context, previous.serviceRef, null);

            // Try to set our new value 
        } while (!tuple.compareAndSet(previous, newTuple));
    }

    public void deactivate(ComponentContext context) {
        ReferenceTuple<T> previous = null;
        ReferenceTuple<T> newTuple = null;

        // Try this until we manage to set/replace the value
        do {
            // Get the current tuple
            previous = tuple.get();

            // Create a new tuple that clears out the context (preserve the reference to the service,
            // as that can have attributes that should be accessible by a user between deactivate
            // and unset
            newTuple = new ReferenceTuple<T>(null, previous.serviceRef, null);

            // Try to save the new tuple: retry if someone changed the value meanwhile
        } while (!tuple.compareAndSet(previous, newTuple));
    }

    /**
     * Update service reference associated with this service.
     * 
     * @param referenceName ServiceReference for the target service. Service references are
     *            equal if they point to the same service registration, and are ordered by
     *            increasing service.ranking and decreasing service.id. ServiceReferences hold
     *            no service properties: requests/queries for properties are forwarded onto
     *            the backing service registration.
     * @return true if this is replacing a previous (non-null) service reference
     */
    public boolean setReference(ServiceReference<T> reference) {
        ReferenceTuple<T> previous = null;
        ReferenceTuple<T> newTuple = null;

        // Try this until we either manage to make our update or we know we don't have to
        do {
            // Get the current tuple
            previous = tuple.get();

            // If we have something to do.. 
            if (reference != previous.serviceRef) {
                newTuple = new ReferenceTuple<T>(previous.context, reference, null);
            } else {
                break; // break out. Nothing to see here.
            }

            // Try to save the new tuple: retry if someone changed the value meanwhile
        } while (!tuple.compareAndSet(previous, newTuple));

        return previous.serviceRef != null;
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
        ReferenceTuple<T> previous = null;
        ReferenceTuple<T> newTuple = null;

        // Try this until we either manage to make our update or we know we don't have to
        do {
            // Get the current tuple
            previous = tuple.get();

            // If we have something to do.. 
            if (reference == previous.serviceRef) {
                newTuple = new ReferenceTuple<T>(previous.context, null, null);
            } else {
                break; // break out. Nothing to see here.
            }

            // Try to save the new tuple: retry if someone changed the value meanwhile
        } while (!tuple.compareAndSet(previous, newTuple));

        return newTuple != null;
    }

    /**
     * @return ServiceReference for the target service. Service references are
     *         equal if they point to the same service registration, and are ordered by
     *         increasing service.ranking and decreasing service.id. ServiceReferences hold
     *         no service properties: requests/queries for properties are forwarded onto
     *         the backing service registration.
     */
    public ServiceReference<T> getReference() {
        ReferenceTuple<T> current = tuple.get();
        return current != null ? current.serviceRef : null;
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
     *             locating the service is not possible or if the service
     *             is not retrievable
     */
    public T getServiceWithException() {
        return getService(true);
    }

    /**
     * Try to locate the service
     * 
     * @param throwException if true, throw exception when required services are
     *            missing
     * @return T or null if unavailable
     */
    @SuppressWarnings("unchecked")
    private T getService(boolean throwException) {
        T svc = null;
        ReferenceTuple<T> current = null;
        ReferenceTuple<T> newTuple = null;

        do {
            // Get the current tuple
            current = tuple.get();

            // We have both a context and a service reference.. 
            svc = current.locatedService;
            if (svc != null) {
                break; // break out. We know the answer, yes
            }

            // If we're missing the required bits, bail... 
            if (current.context == null || current.serviceRef == null) {
                if (throwException)
                    throw new IllegalStateException("Required attribute is null," + toString());
                break; // break out. Nothing more to do here
            }

            // We have to locate / resolve the service from the reference 
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final ReferenceTuple<T> finalCurrent = current;
                svc = AccessController.doPrivileged(new PrivilegedAction<T>() {

                    @Override
                    public T run() {
                        return finalCurrent.context.locateService(referenceName, finalCurrent.serviceRef);
                    }
                });
            } else {
                svc = current.context.locateService(referenceName, current.serviceRef);
            }

            // if we're asked to throw, throw if we couldn't find the service
            if (svc == null) {
                if (throwException)
                    throw new IllegalStateException("Located service is null," + toString());

                break; // break out. Nothing more to do here
            }

            // Create a new tuple: keep the context and reference, set the cached service 
            newTuple = new ReferenceTuple<T>(current.context, current.serviceRef, svc);

            // Try to save the new tuple: retry if someone changed the value meanwhile
        } while (!tuple.compareAndSet(current, newTuple));

        return svc;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[name=" + referenceName
               + "," + tuple.get()
               + "]";
    }
}

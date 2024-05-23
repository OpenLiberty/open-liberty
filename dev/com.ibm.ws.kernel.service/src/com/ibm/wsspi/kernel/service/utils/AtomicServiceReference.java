/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

import java.util.Objects;

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

    private final ConcurrentServiceReferenceSet<T> referenceSet;

    /**
     * Create a new AtomicServiceReference for the named service.
     * e.g. from bnd.bnd: referenceName=.... or from component.xml: <reference name="referenceName".... >
     *
     * @param name Name of DS reference
     */
    public AtomicServiceReference(String name) {
        referenceSet = new ConcurrentServiceReferenceSet<>(name);
    }

    public void activate(ComponentContext context) {
        if (referenceSet.isActive()) {
            return;
        }
        referenceSet.activate(context);
    }

    public void deactivate(ComponentContext context) {
        if (referenceSet.isActive()) {
            referenceSet.deactivate(context);
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
        ServiceReference<T> currentHighest = referenceSet.getHighestRankedReference();
        return !referenceSet.addReference(reference) && currentHighest != null && Objects.equals(reference, referenceSet.getHighestRankedReference());
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
        ServiceReference<T> currentHighest = referenceSet.getHighestRankedReference();
        return referenceSet.removeReference(reference) && Objects.equals(reference, currentHighest);
    }

    /**
     * @return ServiceReference for the target service. Service references are
     *         equal if they point to the same service registration, and are ordered by
     *         increasing service.ranking and decreasing service.id. ServiceReferences hold
     *         no service properties: requests/queries for properties are forwarded onto
     *         the backing service registration.
     */
    public ServiceReference<T> getReference() {
        return referenceSet.getHighestRankedReference();
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
    private T getService(boolean throwException) {
        T service = referenceSet.getHighestRankedService();
        if (service == null && throwException) {
            throw new IllegalStateException("Located service is null: " + referenceSet.toString());
        }
        return service;
    }

    @Override
    public String toString() {
        return referenceSet.toString();
    }
}

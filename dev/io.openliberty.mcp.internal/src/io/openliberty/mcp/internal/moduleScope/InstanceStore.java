/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.moduleScope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

/**
 * A thread-safe store of bean instances, intended for use by a context
 */
// Unchecked casts are safe because we ensure that the keys and values in instances map match
@SuppressWarnings("unchecked")
class InstanceStore {

    private static final TraceComponent tc = Tr.register(InstanceStore.class);

    /**
     * An entry in the instance store
     *
     * @param <T> the instance type
     * @param instance the instance
     * @param creationalContext the creational context used to create the instance
     */
    @Trivial
    record InstanceEntry<T>(T instance, CreationalContext<T> creationalContext) {}

    /**
     * The map of instances. Every key of type {@code Contextual<T>} must have a value of type {@code InstanceEntry<T>}.
     */
    private final ConcurrentMap<Contextual<?>, InstanceEntry<?>> instances = new ConcurrentHashMap<>();

    /**
     * Gets or creates an instance for a contextual. If a new instance is created, it is added to the store.
     *
     * @param <T> the contextual type
     * @param contextual the contextual
     * @param creationalContext the creational context to use if an instance needs to be created
     * @return the instance, either retrieved from the store or newly created.
     */
    @Trivial
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (creationalContext == null) {
            var entry = instances.get(contextual);
            return entry == null ? null : (T) entry.instance();
        } else {
            var entry = instances.computeIfAbsent(contextual, (c) -> new InstanceEntry<>(createInstance(contextual, creationalContext), creationalContext));
            return (T) entry.instance;
        }
    }

    @Trivial
    private <T> T createInstance(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Creating new instance of " + contextual);
        }
        return contextual.create(creationalContext);
    }

    /**
     * Remove and destroy instance from the store.
     *
     * @param <T> the contextual type
     * @param contextual the contextual
     */
    @Trivial
    public <T> void destroy(Contextual<T> contextual) {
        var entry = (InstanceEntry<T>) instances.remove(contextual);
        if (entry != null) {
            contextual.destroy(entry.instance(), entry.creationalContext());
        }
    }

    /**
     * Destroy all instances in the store
     */
    public void destroyAll() {
        for (var entry : instances.entrySet()) {
            destroy(entry.getKey(), entry.getValue());
        }
    }

    private <T> void destroy(Contextual<?> contextual, InstanceEntry<?> entry) {
        Contextual<T> contextualT = (Contextual<T>) contextual;
        InstanceEntry<T> entryT = (InstanceEntry<T>) entry;
        contextualT.destroy(entryT.instance(), entryT.creationalContext());
    }
}

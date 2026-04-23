/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Maintains separate instances per module (WAR/EJB-JAR) within an application.
 * <p>
 * Subclasses must implement {@link #createInstance(J2EEName)} to define how instances
 * are created for each module.
 * </p>
 *
 * @param <T> the type of instance managed per module
 */
public abstract class AbstractModuleScopedStore<T> {

    protected final ConcurrentMap<J2EEName, T> instances = new ConcurrentHashMap<>();

    /**
     * Retrieves the instance for the current thread's module context.
     *
     * @return the instance for the current module
     */
    public T getCurrent() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return getForModule(component.getModuleMetaData().getJ2EEName());
    }

    /**
     * Retrieves the instance for a specific module by name.
     *
     * @param moduleName the J2EE name of the module
     * @return the instance for the specified module
     */
    public T getForModule(J2EEName moduleName) {
        return instances.computeIfAbsent(moduleName, this::createInstance);
    }

    /**
     * Returns all instances across all modules.
     *
     * @return collection of all instances
     */
    public Collection<T> getAll() {
        return instances.values();
    }

    /**
     * Returns an unmodifiable view of the internal mappings.
     *
     * @return unmodifiable map of module names to instances
     */
    public Map<J2EEName, T> getAllMappings() {
        return Collections.unmodifiableMap(instances);
    }

    /**
     * Creates a new instance for the specified module.
     * <p>
     * This method is called once per module when an instance is first requested.
     * Implementations should create and configure the instance appropriately.
     * </p>
     *
     * @param moduleName the J2EE name of the module
     * @return a new instance for the module
     */
    protected abstract T createInstance(J2EEName moduleName);
}

// Made with Bob

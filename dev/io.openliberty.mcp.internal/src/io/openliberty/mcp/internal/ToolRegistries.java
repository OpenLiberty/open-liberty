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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import jakarta.json.bind.Jsonb;

/**
 * Manages tool registries, maintaining one registry per module.
 */
public class ToolRegistries {

    private ConcurrentMap<J2EEName, ToolRegistry> registries = new ConcurrentHashMap<>();
    private final SchemaRegistry schemaRegistry;
    private final Jsonb jsonb;

    public ToolRegistries(SchemaRegistry schemaRegistry, Jsonb jsonb) {
        this.schemaRegistry = schemaRegistry;
        this.jsonb = jsonb;
    }

    /**
     * Retrieves the tool registry for the current thread's module context.
     *
     * @return the tool registry for the current module
     */
    public ToolRegistry getCurrent() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return getForModule(component.getModuleMetaData().getJ2EEName());
    }

    /**
     * Retrieves the tool registry for a specific module by name.
     *
     * @param moduleName the J2EE name of the module
     * @return the tool registry for the specified module
     */
    public ToolRegistry getForModule(J2EEName moduleName) {
        return registries.computeIfAbsent(moduleName, m -> new ToolRegistry(schemaRegistry, jsonb));
    }

    /**
     * Returns all tool registries across all modules.
     *
     * @return collection of all tool registries
     */
    public Collection<ToolRegistry> getAll() {
        return registries.values();
    }
}

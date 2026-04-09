/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.mcp.internal.config.McpConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages request trackers, maintaining one tracker per module.
 */
@ApplicationScoped
public class McpRequestTrackers {

    @Inject
    McpConfig mcpConfig;

    private ConcurrentHashMap<J2EEName, McpRequestTracker> requestTrackers = new ConcurrentHashMap<>();

    /**
     * Retrieves the request tracker for the current thread's module context.
     * Use this in request-handling code where module context is available.
     *
     * @return the request tracker for the current module
     */
    public McpRequestTracker getCurrent() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return getForModule(component.getModuleMetaData().getJ2EEName());
    }

    /**
     * Retrieves the request tracker for a specific module by name.
     * Use this when you need to access a specific module's tracker from outside its context.
     *
     * @param moduleName the J2EE name of the module
     * @return the request tracker for the specified module
     */
    public McpRequestTracker getForModule(J2EEName moduleName) {
        return requestTrackers.computeIfAbsent(moduleName, m -> new McpRequestTracker(mcpConfig));
    }

    /**
     * Returns all request trackers across all modules.
     * Use for administrative operations or cleanup.
     *
     * @return collection of all request trackers
     */
    public Collection<McpRequestTracker> getAll() {
        return requestTrackers.values();
    }
}

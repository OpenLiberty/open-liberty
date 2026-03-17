/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.sessions;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.McpRequestTrackers;
import io.openliberty.mcp.internal.config.McpConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages multiple {@link McpSessionStore} instances, one per application module.
 * <p>
 * In multi-module EAR deployments, each WAR or EJB module maintains its own isolated
 * session store to prevent cross-module session access and ensure proper scoping of
 * MCP tools and resources.
 * </p>
 *
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li><b>{@link #getCurrent()}</b> - Retrieves the session store for the current
 * thread's module context. Use this in request-handling code where module
 * context is available.</li>
 * <li><b>{@link #getForModule(J2EEName)}</b> - Retrieves the session store for a
 * specific module by name. Use this when you need to access a specific module's
 * sessions from outside its context.</li>
 * <li><b>{@link #getAll()}</b> - Returns all active session stores across all modules.
 * Use for administrative operations or cleanup.</li>
 * </ul>
 *
 * @see McpSessionStore
 * @since 1.0
 */

@ApplicationScoped
public class McpSessionStores {

    @Inject
    McpRequestTrackers requestTrackers;

    @Inject
    McpConfig mcpConfig;

    private ConcurrentHashMap<J2EEName, McpSessionStore> stores = new ConcurrentHashMap<>();

    public McpSessionStore getCurrent() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return getForModule(component.getModuleMetaData().getJ2EEName());
    }

    public McpSessionStore getForModule(J2EEName moduleName) {
        McpRequestTracker requestTracker = requestTrackers.getForModule(moduleName);
        return stores.computeIfAbsent(moduleName, m -> new McpSessionStore(requestTracker, mcpConfig));
    }

    public Collection<McpSessionStore> getAll() {
        return stores.values();
    }
}

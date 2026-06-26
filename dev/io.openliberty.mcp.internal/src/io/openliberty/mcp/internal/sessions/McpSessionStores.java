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

import com.ibm.websphere.csi.J2EEName;

import io.openliberty.mcp.internal.AbstractModuleScopedStore;
import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.McpRequestTrackers;
import io.openliberty.mcp.internal.config.McpConfig;
import jakarta.annotation.PreDestroy;
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
 * @see McpSessionStore
 */
@ApplicationScoped
public class McpSessionStores extends AbstractModuleScopedStore<McpSessionStore> {

    @Inject
    McpRequestTrackers requestTrackers;

    @Inject
    McpConfig mcpConfig;

    @Override
    protected McpSessionStore createInstance(J2EEName moduleName) {
        McpRequestTracker requestTracker = requestTrackers.getForModule(moduleName);
        return new McpSessionStore(requestTracker, mcpConfig);
    }

    /**
     * Called when the application is stopping. Ends all active sessions across all modules
     * and records their metrics. 
     */
    @PreDestroy
    protected void shutdown() {
        for (McpSessionStore store : getAll()) {
            store.endAllSessions();
        }
    }
}

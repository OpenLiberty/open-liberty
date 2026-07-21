/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.sessions;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

import org.mcpjava.server.ImplementationInfo;

import io.openliberty.mcp.internal.metrics.McpSessionMetrics;

/**
 *
 */

public class McpSession {

    private final McpSessionId sessionId;
    private final Principal userId;
    private final Instant created;
    private Instant lastAccessed;
    private McpSessionMetrics metrics;
    private final ImplementationInfo clientInfo;
    private final Map<String, Object> clientCapabilities;

    public McpSession(McpSessionId sessionId, Principal userId, McpSessionMetrics metrics, ImplementationInfo clientInfo, Map<String, Object> clientCapabilities) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.created = Instant.now();
        this.lastAccessed = this.created;
        this.metrics = metrics;
        this.clientInfo = clientInfo;
        this.clientCapabilities = clientCapabilities;
    }

    /**
     * This method should be called whenever the session is accessed,
     * in order to track session activity and support idle timeout logic.
     */
    public void touch() {
        this.lastAccessed = Instant.now();
    }

    public McpSessionId getSessionId() {
        return sessionId;
    }

    public Principal getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return created;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public McpSessionMetrics getMetrics() {
        return metrics;
    }

    public ImplementationInfo getClientInfo() {
        return clientInfo;
    }

    public Map<String, Object> getClientCapabilities() {
        return clientCapabilities;
    }

}

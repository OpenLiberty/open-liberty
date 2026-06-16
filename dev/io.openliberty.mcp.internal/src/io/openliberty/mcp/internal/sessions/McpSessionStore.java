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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;

/**
 * Manages active MCP sessions for the server.
 * <p>
 * Each session is uniquely identified by a UUID and has an associated {@link McpSession}
 */
public class McpSessionStore {

    private McpRequestTracker requestTracker;

    private McpConfig mcpConfig;

    private final ConcurrentMap<String, McpSession> sessions = new ConcurrentHashMap<>();

    public McpSessionStore(McpRequestTracker requestTracker, McpConfig mcpConfig) {
        this.requestTracker = requestTracker;
        this.mcpConfig = mcpConfig;
    }

    public boolean isStateless() {
        return mcpConfig.stateless();
    }

    /**
     * Creates a new MCP session with a unique session ID and stores it mapping to a userId which can also be null if not authentication was used to create the session.
     *
     * @return the newly generated session ID
     */
    public String createSession(Principal userId, McpSessionMetrics metrics) {

        if (isStateless()) {
            return null;
        }

        String sessionId = UUID.randomUUID().toString();
        McpSession mcpSession = new McpSession(sessionId, userId, metrics);
        sessions.put(sessionId, mcpSession);
        metrics.setMcpSession(mcpSession);
        return sessionId;
    }

    /**
     * Retrieves the SessionInfo associated with the given session ID.
     * If a session is found, its last accessed time is updated to the current instant.
     * This is useful for implementing idle session timeout mechanisms.
     *
     * @param sessionId the ID of the session
     * @return the corresponding {@link McpSession}, or {@code null} if not found
     */
    public McpSession getSession(String sessionId) {
        if (isStateless()) {
            return null;
        }
        McpSession session = sessions.get(sessionId);
        if (session != null) {
            session.touch();
            return session;
        }
        return null;
    }

    /**
     * Checks if the session ID is valid and not expired.
     * Also removes any expired sessions as a side effect.
     */
    public boolean isValid(String sessionId) {
        cleanupOldSessions();
        return sessionId != null && sessions.containsKey(sessionId);
    }

    /**
     * Deletes the session associated with the given session ID.
     */
    public void deleteSession(String sessionId) {
        McpSession session = sessions.remove(sessionId);

        if (session != null) {
            requestTracker.cancelSessionRequests(session.getSessionId());

            // Record session end metrics
            McpSessionMetrics metrics = session.getMetrics();
            if (metrics != null) {
                McpSessionMetrics.sessionEnded(metrics);
            }
        }
    }

    /**
     * Removes any sessions that have expired based on the session timeout duration.
     */
    public void cleanupOldSessions() {
        Duration sessionTimeout = mcpConfig.sessionTimeout();
        Instant now = Instant.now();
        
        // Collect expired session IDs
        List<String> expiredSessionIds = new ArrayList<>();
        for (var entry : sessions.entrySet()) {
            boolean expired = Duration.between(entry.getValue().getLastAccessed(), now)
                                      .compareTo(sessionTimeout) > 0;
            if (expired) {
                expiredSessionIds.add(entry.getKey());
            }
        }
        
        // Remove expired sessions and record metrics
        for (String sessionId : expiredSessionIds) {
            McpSession session = sessions.remove(sessionId);
            if (session != null) {
                McpSessionMetrics metrics = session.getMetrics();
                if (metrics != null) {
                    metrics.setErrorType("timeout");
                    McpSessionMetrics.sessionEnded(metrics);
                }
            }
        }
    }

    /**
     * Ends all active sessions and records their metrics.
     * Called during application shutdown.
     */
    public void endAllSessions() {
        for (McpSession session : sessions.values()) {
            McpSessionMetrics metrics = session.getMetrics();
            if (metrics != null) {
                McpSessionMetrics.sessionEnded(metrics);
            }
        }
        sessions.clear();
    }
}

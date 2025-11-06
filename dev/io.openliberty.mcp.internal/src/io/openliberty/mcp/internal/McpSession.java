/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.openliberty.mcp.internal.requests.ExecutionRequestId;

/**
 *
 */

public class McpSession {
    private final String sessionId;
    private final Instant created;
    private Instant lastAccessed;

    private final Set<ExecutionRequestId> activeRequests = ConcurrentHashMap.newKeySet();

    public McpSession(String sessionId) {
        this.sessionId = sessionId;
        this.created = Instant.now();
        this.lastAccessed = this.created;
    }

    public void addRequest(ExecutionRequestId requestId) {
        activeRequests.add(requestId);
    }

    public boolean isRequestActive(ExecutionRequestId requestId) {
        return activeRequests.contains(requestId);
    }

    public void removeRequest(ExecutionRequestId requestId) {
        activeRequests.remove(requestId);
    }

    public boolean hasNoActiveRequests() {
        return activeRequests.isEmpty();
    }

    public Instant getCreatedAt() {
        return created;
    }

    public void clearAllRequests() {
        activeRequests.clear();
    }

    /**
     * This method should be called whenever the session is accessed,
     * in order to track session activity and support idle timeout logic.
     */
    public void touch() {
        this.lastAccessed = Instant.now();
    }

    /**
     * Returns a snapshot of all currently active requests for this session.
     *
     * @return a Set of active {@link ExecutionRequestId}s
     */
    public Set<ExecutionRequestId> getActiveRequests() {
        return new HashSet<>(activeRequests);
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

}

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

/**
 *
 */

public class McpSession {

    private final McpSessionId sessionId;
    private final Principal userId;
    private final Instant created;
    private Instant lastAccessed;

    public McpSession(String sessionId, Principal userId) {
        this.sessionId = new McpSessionId(sessionId);
        this.userId = userId;
        this.created = Instant.now();
        this.lastAccessed = this.created;
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

}

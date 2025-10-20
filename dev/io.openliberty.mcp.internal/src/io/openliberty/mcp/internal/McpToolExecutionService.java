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

import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.messaging.Cancellation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service layer that coordinates request registration and cleanup
 * between McpSession and McpRequestTracker
 *
 * Singel point of truth for tool lifecycle management
 */

@ApplicationScoped
public class McpToolExecutionService {
    @Inject
    McpRequestCoordinator requestCoordinator;

    @Inject
    McpSessionStore sessionStore;

    public void startTool(McpSession session, ExecutionRequestId id, Cancellation cancel) {
        requestCoordinator.registerRequest(session, id, cancel);
    }

    public void completeTool(McpSession session, ExecutionRequestId id) {
        requestCoordinator.deregisterRequest(session, id, null);
    }
}

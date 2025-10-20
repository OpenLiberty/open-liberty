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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.messaging.Cancellation;

/**
 * Insures consistent registration and deregistration of requests
 * between McpSession and McpRequestTracker
 */
public class McpRequestCoordinator {

    private static final TraceComponent tc = Tr.register(McpRequestCoordinator.class);

    private final McpRequestTracker tracker;

    public McpRequestCoordinator(McpRequestTracker tracker) {
        this.tracker = tracker;
    }

    /*
     * Register a new request across tracker and session
     */

    public void registerRequest(McpSession session, ExecutionRequestId id, Cancellation cancellation) {
        try {
            tracker.registerOngoingRequest(id, cancellation);
            session.addRequest(id);
        } catch (RuntimeException e) {
            tracker.deregisterOngoingRequest(id);
            throw e;
        }
    }

    /**
     * Deregister a request across tracker and session
     */
    public void deregisterRequest(McpSession session, ExecutionRequestId id, Cancellation cancellation) {
        tracker.deregisterOngoingRequest(id);
        session.removeRequest(id);
    }

    /**
     * Cleaning uprequests when session is deleted
     */
    public void cleanUpSession(McpSession session) {
        tracker.cancelSessionRequests(session);
        session.clearAllRequests();
    }
}

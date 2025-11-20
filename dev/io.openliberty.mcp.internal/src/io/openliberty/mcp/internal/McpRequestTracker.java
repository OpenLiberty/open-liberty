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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.internal.config.McpConfiguration;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.CancellationImpl;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.messaging.Cancellation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This is a connection tracker bean. It keeps track of ongoing tool call requests
 */

@ApplicationScoped
public class McpRequestTracker {

    private static final TraceComponent tc = Tr.register(McpRequestTracker.class);

    private ConcurrentMap<ExecutionRequestId, CancellationImpl> ongoingRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<ExecutionRequestId>> sessionToRequestIds = new ConcurrentHashMap<>();

    private static final ServiceCaller<McpConfiguration> mcpConfigService = new ServiceCaller<>(McpRequestTracker.class, McpConfiguration.class);

    public McpRequestTracker() {
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    public void deregisterOngoingRequest(ExecutionRequestId id) {
        ongoingRequests.remove(id);
    }

    public void registerOngoingRequest(ExecutionRequestId requestId, CancellationImpl cancellation) {
        CancellationImpl previous = ongoingRequests.putIfAbsent(requestId, cancellation);
        if (previous != null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS,
                                       Tr.formatMessage(tc, "CWMCM0008E.invalid.request.params", requestId.id()));
        }
    }

    public boolean isOngoingRequest(ExecutionRequestId id) {
        return ongoingRequests.containsKey(id);
    }

    public Cancellation getOngoingRequestCancellation(ExecutionRequestId id) {
        return ongoingRequests.get(id);
    }

    /**
     * Cancels all ongoing requests associated with the given session.
     * <p>
     * Will skip cancellation if the server is in stateless mode.
     * request is cancelled with a fixed reason: {@code "Session cancelled"}
     */
    public void cancelSessionRequests(String sessionId) {
        Boolean stateless = mcpConfigService.run(McpConfiguration::isStateless).orElse(false);
        if (Boolean.TRUE.equals(stateless)) {
            return;
        }

        Set<ExecutionRequestId> requests = sessionToRequestIds.remove(sessionId);
        if (requests == null) {
            return;
        }

        for (ExecutionRequestId id : requests) {
            Cancellation cancellation = ongoingRequests.remove(id);
            if (cancellation instanceof CancellationImpl impl) {
                impl.cancel(Optional.of("Session cancelled"));
            }
        }
    }
}

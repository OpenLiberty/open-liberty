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

import static io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode.INVALID_PARAMS;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.internal.config.McpConfiguration;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.CancellationImpl;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.messaging.Cancellation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This is a connection tracker bean. It keeps track of ongoing tool call requests
 */

@ApplicationScoped
public class McpConnectionTracker {

    private static final TraceComponent tc = Tr.register(McpConnectionTracker.class);
    private final ConcurrentMap<String, Cancellation> ongoingRequests;
    private static final ServiceCaller<McpConfiguration> mcpConfigService = new ServiceCaller<>(McpConnectionTracker.class, McpConfiguration.class);

    public McpConnectionTracker() {
        this.ongoingRequests = new ConcurrentHashMap<>();
    }

    public void deregisterOngoingRequest(ExecutionRequestId id) {
        ongoingRequests.remove(id.toString());
    }

    public void registerOngoingRequest(ExecutionRequestId id, Cancellation cancellation) {
        Cancellation previous = ongoingRequests.putIfAbsent(id.toString(), cancellation);
        if (previous != null) {
            throw new JSONRPCException(INVALID_PARAMS, Tr.formatMessage(tc, "CWMCM0008E.invalid.request.params", id.id()));
        }
    }

    public boolean isOngoingRequest(ExecutionRequestId id) {
        return ongoingRequests.containsKey(id.toString());
    }

    public Cancellation getOngoingRequestCancellation(ExecutionRequestId id) {
        return ongoingRequests.get(id.toString());
    }

    /**
     * Cancels all ongoing requests associated with the given session.
     * <p>
     * Will skip cancellation if the server is in stateless mode.
     * request is cancelled with a fixed reason: {@code "Session cancelled"}
     */
    public void cancelSessionRequests(McpSession session) {

        Boolean stateless = mcpConfigService.run(config -> config.isStateless()).orElse(false);
        if (Boolean.TRUE.equals(stateless)) {
            return;
        }

        for (ExecutionRequestId id : session.getActiveRequests()) {
            Cancellation cancellation = ongoingRequests.remove(id.toString());
            if (cancellation instanceof CancellationImpl) {
                ((CancellationImpl) cancellation).cancel(Optional.of("Session cancelled"));
            }
        }
    }

}

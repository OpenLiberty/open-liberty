/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.mcpjava.server.ImplementationInfo;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.sessions.McpSession;
import jakarta.json.JsonObject;

/**
 * A request to run an MCP Operation,
 *
 * @param method the method being called, or the notification being delivered
 * @param protocolVersion the protocol version used for the request, may be {@code null} for a {@link RequestMethod#isPreInit() pre-init} request
 * @param id the request id, {@code null} for a notification
 * @param session the session, {@code null} if there's no associated session
 * @param args the unparsed method arguments object. May be empty if no args provided, but not {@code null}
 * @param clientInfo the client info for the request, which comes from the session or from the request {@code _meta} depending on the protocol version.
 *            {@link ImplementationInfoImpl#UNKNOWN} if it is not available.
 * @param clientCapabilities the client capabilities for the request, which come from the session or from the request {@code _meta} depending on the protocol version.
 *            Empty if they are not available.
 */
@Trivial
public record McpOperationRequest(RequestMethod method,
                                  McpProtocolVersion protocolVersion,
                                  RequestId id,
                                  McpSession session,
                                  JsonObject args,
                                  ImplementationInfo clientInfo,
                                  Map<String, Object> clientCapabilities) {

    public McpOperationRequest {
        requireNonNull(method, "method is required");
        if (!method.isPreInit()) {
            requireNonNull(protocolVersion, "protocolVersion is required");
        }
        requireNonNull(args, "args is required");
        requireNonNull(clientInfo, "clientInfo is required");
        requireNonNull(clientCapabilities, "clientCapabilities is required");
    }
}

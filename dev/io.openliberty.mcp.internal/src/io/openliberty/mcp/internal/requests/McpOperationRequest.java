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

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.sessions.McpSession;
import jakarta.json.JsonObject;

/**
 * A request to run an MCP Operation,
 *
 * @param method the method being called, or the notification being delivered
 * @param protocolVersion the protocol version used for the request
 * @param id the request id, {@code null} for a notification
 * @param session the session, {@code null} if there's no associated session
 * @param args the unparsed method arguments object. May be empty if no args provided, but not {@code null}
 */
@Trivial
public record McpOperationRequest(RequestMethod method,
                                  McpProtocolVersion protocolVersion,
                                  RequestId id,
                                  McpSession session,
                                  JsonObject args) {

    public McpOperationRequest {
        requireNonNull(method, "method is required");
        requireNonNull(protocolVersion, "protocolVersion is required");
        requireNonNull(args, "args is required");
    }
}

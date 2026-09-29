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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.mcpjava.server.ImplementationInfo;
import org.mcpjava.server.McpRequest;

import com.ibm.websphere.ras.annotation.Sensitive;

import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.sessions.McpSession;

/**
 * Gives users access to additional information about the request.
 * <p>
 * Implements the {@link McpRequest} special argument type.
 */
public class McpRequestImpl implements McpRequest {

    private io.openliberty.mcp.internal.requests.McpRequest requestMessage;
    private McpTransport transport;
    private McpSession session;
    private Map<String, Object> meta;

    public McpRequestImpl(io.openliberty.mcp.internal.requests.McpRequest requestMessage,
                          McpTransport transport,
                          McpSession session,
                          Map<String, Object> meta) {
        this.requestMessage = requestMessage;
        this.transport = transport;
        this.session = session;
        this.meta = meta;
    }

    @Override
    public Map<String, Object> metadata() {
        return meta;
    }

    @Override
    public ImplementationInfo clientInfo() {
        if (session == null) {
            return ImplementationInfoImpl.UNKNOWN;
        }
        return session.getClientInfo();
    }

    @Override
    public Object id() {
        return requestMessage.id().value();
    }

    @Override
    public String protocolVersion() {
        return transport.getProtocolVersion().getVersion();
    }

    @Override
    public Map<String, Object> rawClientCapabilities() {
        if (session == null) {
            return Collections.emptyMap();
        }
        return session.getClientCapabilities();
    }

    @Override
    @Sensitive
    public Optional<String> sessionId() {
        if (session == null) {
            return Optional.empty();
        } else {
            return Optional.of(session.getSessionId().value());
        }
    }

}

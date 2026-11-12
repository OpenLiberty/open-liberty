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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.sessions.McpSession;
import jakarta.json.bind.Jsonb;

/**
 * Gives users access to additional information about the request.
 * <p>
 * Implements the {@link McpRequest} special argument type.
 */
public class McpRequestImpl implements McpRequest {

    private static final TraceComponent tc = Tr.register(McpRequestImpl.class);

    // Only the reserved, prefixed keys are read. Unprefixed keys are application-defined metadata
    private static final String META_CLIENT_INFO = "io.modelcontextprotocol/clientInfo";
    private static final String META_CLIENT_CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";

    private final io.openliberty.mcp.internal.requests.McpRequest requestMessage;
    private final McpTransport transport;
    private final McpSession session;
    private final Map<String, Object> meta;
    private final Jsonb jsonb;

    public McpRequestImpl(io.openliberty.mcp.internal.requests.McpRequest requestMessage,
                          McpTransport transport,
                          McpSession session,
                          Map<String, Object> meta,
                          Jsonb jsonb) {
        this.requestMessage = requestMessage;
        this.transport = transport;
        this.session = session;
        this.meta = meta;
        this.jsonb = jsonb;
    }

    @Override
    public Map<String, Object> metadata() {
        return meta;
    }

    @Override
    public ImplementationInfo clientInfo() {
        if (transport.getProtocolVersion().supportsRequestLevelClientInfo()) {
            return extractClientInfoFromMeta();
        }
        if (session == null) {
            return ImplementationInfoImpl.UNKNOWN;
        }
        ImplementationInfo info = session.getClientInfo();
        return info != null ? info : ImplementationInfoImpl.UNKNOWN;
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
        if (transport.getProtocolVersion().supportsRequestLevelClientInfo()) {
            return extractCapabilitiesFromMeta();
        }
        if (session == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> caps = session.getClientCapabilities();
        return caps != null ? caps : Collections.emptyMap();
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

    @FFDCIgnore(Exception.class)
    private ImplementationInfo extractClientInfoFromMeta() {
        // clientInfo is optional, so a missing value is not an error
        Object clientInfoObj = meta == null ? null : meta.get(META_CLIENT_INFO);
        if (clientInfoObj == null) {
            return ImplementationInfoImpl.UNKNOWN;
        }
        if (clientInfoObj instanceof ImplementationInfo info) {
            return info;
        }
        try {
            String json = jsonb.toJson(clientInfoObj);
            ImplementationInfo info = jsonb.fromJson(json, ImplementationInfoImpl.class);
            return info != null ? info : ImplementationInfoImpl.UNKNOWN;
        } catch (Exception e) {
            // clientInfo is self-reported and unverified, so an unusable value is treated as unknown
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to read client info from request _meta, using unknown client info: " + e);
            }
            return ImplementationInfoImpl.UNKNOWN;
        }
    }

    private Map<String, Object> extractCapabilitiesFromMeta() {
        Object capsObj = meta == null ? null : meta.get(META_CLIENT_CAPABILITIES);
        if (capsObj instanceof Map<?, ?> capsMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) capsMap;
            return Collections.unmodifiableMap(result);
        }
        return Collections.emptyMap();
    }

}

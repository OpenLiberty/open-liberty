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
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Gives users access to additional information about the request.
 * <p>
 * Implements the {@link McpRequest} special argument type.
 */
public class McpRequestImpl implements McpRequest {

    private static final String META_CLIENT_INFO_QUALIFIED = "io.modelcontextprotocol/clientInfo";
    private static final String META_CLIENT_INFO_SHORT = "clientInfo";
    private static final String META_CLIENT_CAPABILITIES_QUALIFIED = "io.modelcontextprotocol/clientCapabilities";
    private static final String META_CLIENT_CAPABILITIES_SHORT = "clientCapabilities";
    private static final String META_CAPABILITIES_SHORT = "capabilities";

    private final io.openliberty.mcp.internal.requests.McpRequest requestMessage;
    private final McpTransport transport;
    private final McpSession session;
    private final Map<String, Object> meta;
    private final Jsonb jsonb;

    public McpRequestImpl(io.openliberty.mcp.internal.requests.McpRequest requestMessage,
                          McpTransport transport,
                          McpSession session,
                          Map<String, Object> meta) {
        this(requestMessage, transport, session, meta, null);
    }

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
        if (transport != null && transport.getProtocolVersion() != null && transport.getProtocolVersion().supportsRequestLevelClientInfo()) {
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
        return requestMessage != null && requestMessage.id() != null ? requestMessage.id().value() : null;
    }

    @Override
    public String protocolVersion() {
        return transport != null && transport.getProtocolVersion() != null ? transport.getProtocolVersion().getVersion() : null;
    }

    @Override
    public Map<String, Object> rawClientCapabilities() {
        if (transport != null && transport.getProtocolVersion() != null && transport.getProtocolVersion().supportsRequestLevelClientInfo()) {
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

    private ImplementationInfo extractClientInfoFromMeta() {
        if (meta == null) {
            return ImplementationInfoImpl.UNKNOWN;
        }
        Object clientInfoObj = meta.get(META_CLIENT_INFO_QUALIFIED);
        if (clientInfoObj == null) {
            clientInfoObj = meta.get(META_CLIENT_INFO_SHORT);
        }
        if (clientInfoObj == null) {
            return ImplementationInfoImpl.UNKNOWN;
        }
        if (clientInfoObj instanceof ImplementationInfo info) {
            return info;
        }
        try {
            Jsonb serializer = getJsonb();
            String json = serializer.toJson(clientInfoObj);
            ImplementationInfo info = serializer.fromJson(json, ImplementationInfoImpl.class);
            return info != null ? info : ImplementationInfoImpl.UNKNOWN;
        } catch (Exception e) {
            return ImplementationInfoImpl.UNKNOWN;
        }
    }

    private Map<String, Object> extractCapabilitiesFromMeta() {
        if (meta == null) {
            return Collections.emptyMap();
        }
        Object capsObj = meta.get(META_CLIENT_CAPABILITIES_QUALIFIED);
        if (capsObj == null) {
            capsObj = meta.get(META_CLIENT_CAPABILITIES_SHORT);
        }
        if (capsObj == null) {
            capsObj = meta.get(META_CAPABILITIES_SHORT);
        }
        if (capsObj instanceof Map<?, ?> capsMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) capsMap;
            return result;
        }
        return Collections.emptyMap();
    }

    private Jsonb getJsonb() {
        if (jsonb != null) {
            return jsonb;
        }
        return DefaultJsonbHolder.INSTANCE;
    }

    private static class DefaultJsonbHolder {
        static final Jsonb INSTANCE = JsonbBuilder.create(new JsonbConfig().withAdapters(new ImplementationInfoImpl.Adapter(),
                                                                                          new IconImpl.Adapter()));
    }

}

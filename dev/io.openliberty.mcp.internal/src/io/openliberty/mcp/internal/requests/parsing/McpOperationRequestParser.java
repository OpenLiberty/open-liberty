/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.HeaderValidation;
import io.openliberty.mcp.internal.McpInternal;
import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.MCPRequestValidationException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.McpResponseException;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ApplicationScoped
public class McpOperationRequestParser {

    /**  */
    private static final String ACCEPT_HEADER = "Accept";

    private static final TraceComponent tc = Tr.register(McpOperationRequestParser.class);

    private static final List<String> REQUIRED_MCP_MIME_TYPES = List.of("text/event-stream", "application/json");
    public static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    private McpSessionStore sessionStore;
    private Jsonb jsonb;
    private McpConfig config;

    // No-args constructor for CDI
    McpOperationRequestParser() {}

    @Inject
    public McpOperationRequestParser(McpSessionStore sessionStore, McpConfig config, @McpInternal Jsonb jsonb) {
        this.sessionStore = sessionStore;
        this.config = config;
        this.jsonb = jsonb;
    }

    /**
     * Extracts the information needed for error responses
     *
     * @param req the servlet request
     * @return
     * @throws IOException
     */
    @FFDCIgnore({ NoSuchElementException.class, McpResponseException.class })
    public PartialMcpOperationRequest preParse(HttpServletRequest req) throws IOException {
        McpProtocolVersion protocolVersion;
        try {
            protocolVersion = readProtocolVersionHeader(req);
        } catch (NoSuchElementException e) {
            protocolVersion = null;
        }

        String acceptHeader = req.getHeader(ACCEPT_HEADER);
        String sessionIdHeader = req.getHeader(MCP_SESSION_ID_HEADER);

        JsonRpcRequestTO jsonRpcRequest;
        RequestId requestId;
        RequestMethod method;
        McpResponseException parseException = null;
        try {
            jsonRpcRequest = readJsonRpcRequest(req);
            validateJsonRpcRequest(jsonRpcRequest);
            requestId = parseAndValidateId(jsonRpcRequest.id(), new ArrayList<>());
            method = readRequestMethod(jsonRpcRequest);
        } catch (McpResponseException e) {
            // If the request isn't valid JSON-RPC 2.0, we don't read it at all
            parseException = e;
            jsonRpcRequest = null;
            requestId = RequestId.UNPARSABLE;
            method = null;
        }

        return new PartialMcpOperationRequest(acceptHeader, protocolVersion, sessionIdHeader, jsonRpcRequest, parseException, requestId, method);
    }

    /**
     * Reads and parses the protocol version header. A missing header is interpreted as 2025-03-26
     *
     * @return the protocol version interpreted from the header
     * @throws NoSuchElementException if the protocol is not one we support
     */
    private McpProtocolVersion readProtocolVersionHeader(HttpServletRequest req) {
        String protocolVersion = req.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        if (protocolVersion == null) {
            return McpProtocolVersion.V_2025_03_26;
        } else {
            return McpProtocolVersion.parse(protocolVersion);
        }
    }

    @FFDCIgnore({ JsonbException.class, Throwable.class })
    private JsonRpcRequestTO readJsonRpcRequest(HttpServletRequest req) throws IOException {
        try (BufferedReader re = req.getReader()) {
            JsonRpcRequestTO requestTO = jsonb.fromJson(re, JsonRpcRequestTO.class);
            if (requestTO == null) {
                // The body was the JSON literal null
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_REQUEST, List.of("Request body must be a JSON object"));
            }
            return requestTO;
        } catch (JsonbException e) {
            // Usually here it's a JSON parsing exception, and in that case we want the message from the underlying JsonException
            // since JSON-B reports "Internal error"
            JsonException jsonEx = unwrapToJsonException(e);
            String message = jsonEx != null ? jsonEx.getMessage() : e.getMessage();
            throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR, List.of(message));
        }
    }

    private JsonException unwrapToJsonException(JsonbException e) {
        Throwable c = e.getCause();
        while (true) {
            if (c == null) {
                return null;
            }
            if (c instanceof JsonException jsonEx) {
                return jsonEx;
            }
            c = c.getCause();
        }
    }

    /**
     * Checks that the received message is a valid JSON-RPC request
     */
    private void validateJsonRpcRequest(JsonRpcRequestTO to) {
        List<String> errors = new ArrayList<>();

        validateJsonRpcVersion(to.jsonrpc(), errors);
        validateMethodPresent(to.method(), errors);
        parseAndValidateId(to.id(), errors);

        if (!errors.isEmpty()) {
            throw new MCPRequestValidationException(errors);
        }
    }

    private static void validateJsonRpcVersion(String jsonRpc, List<String> errors) {
        if (!"2.0".equals(jsonRpc)) {
            errors.add(Tr.formatMessage(tc, "jsonrpc.exception.validation.invalid.version", jsonRpc));
        }
    }

    private static void validateMethodPresent(String method, List<String> errors) {
        if (method == null || method.isBlank()) {
            errors.add(Tr.formatMessage(tc, "jsonrpc.validation.empty.method"));
        }
    }

    private static RequestId parseAndValidateId(JsonValue id, List<String> errors) {
        if (id == null) {
            return null;
        }
        return switch (id.getValueType()) {
            case NUMBER -> new RequestId(((JsonNumber) id).bigDecimalValue());
            case STRING -> {
                String idString = ((JsonString) id).getString();
                if (idString.isBlank()) {
                    errors.add(Tr.formatMessage(tc, "jsonrpc.exception.validation.empty.string.id", idString));
                    yield RequestId.UNPARSABLE;
                }
                yield new RequestId(idString);
            }
            default -> {
                errors.add(Tr.formatMessage(tc, "jsonrpc.exception.validation.invalid.id.type"));
                yield RequestId.UNPARSABLE;
            }
        };
    }

    @FFDCIgnore(NoSuchElementException.class)
    private RequestMethod readRequestMethod(JsonRpcRequestTO requestTO) {
        try {
            return RequestMethod.getForMethodName(requestTO.method());
        } catch (NoSuchElementException e) {
            return null;
        }

    }

    /**
     * Parse and validate the incoming request
     */
    public McpOperationRequest parse(PartialMcpOperationRequest partialReq) throws IOException {
        if (!isValidAcceptHeader(partialReq.acceptHeader())) {
            throw new HttpResponseException(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }

        validateProtocolVersion(partialReq);

        if (partialReq.parseException() != null) {
            throw partialReq.parseException();
        }

        JsonRpcRequestTO requestTO = partialReq.requestTo();

        McpSession session = parseSession(partialReq);

        RequestMethod method = parseRequestMethod(requestTO);

        validateSessionPresentIfRequired(session, method);

        JsonObject args = requestTO.params() == null ? JsonObject.EMPTY_JSON_OBJECT : requestTO.params();

        return new McpOperationRequest(method, partialReq.protocolVersion(), partialReq.id(), session, args);
    }

    /**
     * Calculates whether the client accepts MCP's required content types.
     */
    private boolean isValidAcceptHeader(String acceptHeader) throws IOException {
        if (acceptHeader == null)
            return false;
        for (String mcpMime : REQUIRED_MCP_MIME_TYPES) {
            if (!HeaderValidation.acceptContains(acceptHeader, mcpMime))
                return false;
        }

        return true;
    }

    /**
     * @param partialReq
     */
    private void validateProtocolVersion(PartialMcpOperationRequest partialReq) {
        if (partialReq.protocolVersion() == null) {
            String supportedValues = Arrays.stream(McpProtocolVersion.availableValues())
                                           .map(McpProtocolVersion::getVersion)
                                           .collect(Collectors.joining(", "));
            String excpetionMessage = Tr.formatMessage(tc, "unsupported.mcp.http.version", supportedValues);
            throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, excpetionMessage);
        }
    }

    /**
     * Find the session from a partial request
     *
     * @param partialReq
     * @return the session, or {@code null} if the session header is not present
     * @throws McpResponseException if the session header is present but does not match a valid session
     */
    private McpSession parseSession(PartialMcpOperationRequest partialReq) {
        McpSession session;

        final String sessionIdHeader = partialReq.sessionIdHeader();
        if (sessionIdHeader != null) {
            session = sessionStore.getSession(new McpSessionId(sessionIdHeader));
            if (session == null) {
                throw new HttpResponseException(HttpServletResponse.SC_NOT_FOUND, "Invalid or Expired Session Id");
            }
        } else {
            session = null;
        }
        return session;
    }

    @FFDCIgnore(NoSuchElementException.class)
    private RequestMethod parseRequestMethod(JsonRpcRequestTO requestTO) {
        RequestMethod method;
        try {
            method = RequestMethod.getForMethodName(requestTO.method());
        } catch (NoSuchElementException e) {
            throw new JSONRPCException(JSONRPCErrorCode.METHOD_NOT_FOUND, List.of(requestTO.method() + " not found"));
        }
        return method;
    }

    private void validateSessionPresentIfRequired(McpSession session, RequestMethod method) {
        if (!config.stateless() && method != RequestMethod.INITIALIZE && method != RequestMethod.PING) {
            if (session == null) {
                throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST,
                                                "Missing Mcp-Session-Id header");
            }
        }
    }

}

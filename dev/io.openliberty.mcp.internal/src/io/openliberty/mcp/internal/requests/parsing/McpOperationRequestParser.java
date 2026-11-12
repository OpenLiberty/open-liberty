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
import io.openliberty.mcp.internal.requests.McpOperationPartialRequest;
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

    private static final TraceComponent tc = Tr.register(McpOperationRequestParser.class);

    public static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
    private static final List<String> REQUIRED_MCP_MIME_TYPES = List.of("text/event-stream", "application/json");
    private static final String ACCEPT_HEADER = "Accept";

    private final McpSessionStore sessionStore;
    private final Jsonb jsonb;
    private final McpConfig config;

    // No-args constructor for CDI
    McpOperationRequestParser() {
        sessionStore = null;
        jsonb = null;
        config = null;
    }

    @Inject
    public McpOperationRequestParser(McpSessionStore sessionStore, McpConfig config, @McpInternal Jsonb jsonb) {
        this.sessionStore = sessionStore;
        this.config = config;
        this.jsonb = jsonb;
    }

    /**
     * Extracts the information needed for error responses.
     * <p>
     * Other than an {@code IOException} if the request stream cannot be read, this method will not throw exceptions.
     *
     * @param req the servlet request
     * @return the partially parsed request
     * @throws IOException if there is an error reading from the request stream
     */
    @FFDCIgnore({ NoSuchElementException.class, RuntimeException.class })
    public McpOperationPartialRequest preParse(HttpServletRequest req) throws IOException {
        String protocolVersionHeader = req.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        McpProtocolVersion protocolVersion;
        try {
            protocolVersion = readProtocolVersionHeader(protocolVersionHeader);
        } catch (NoSuchElementException e) {
            protocolVersion = null;
        }

        String acceptHeader = req.getHeader(ACCEPT_HEADER);
        String sessionIdHeader = req.getHeader(MCP_SESSION_ID_HEADER);
        McpSessionId sessionId = sessionIdHeader == null ? null : new McpSessionId(sessionIdHeader);

        JsonRpcRequestTO jsonRpcRequest;
        RequestId requestId;
        RequestMethod method;
        RuntimeException parseException = null;
        try {
            jsonRpcRequest = readJsonRpcRequest(req);
            validateJsonRpcRequest(jsonRpcRequest);
            requestId = parseAndValidateId(jsonRpcRequest.id(), new ArrayList<>());
            method = readRequestMethod(jsonRpcRequest);
        } catch (RuntimeException e) {
            // Any exception we catch here will be thrown as-is in parse.
            // We don't need to log or FFDC here, that'll happen if necessary when it gets handled later after being thrown.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Exception caught during preParse. Throwing will be deferred until parse is called.", e);
            }
            // If the request isn't valid JSON-RPC 2.0, we don't read it at all
            parseException = e;
            jsonRpcRequest = null;
            requestId = RequestId.UNPARSABLE;
            method = null;
        }

        return new McpOperationPartialRequest(acceptHeader, protocolVersion, protocolVersionHeader != null, sessionId, jsonRpcRequest, parseException, requestId, method);
    }

    /**
     * Reads and parses the protocol version header. A missing header is interpreted as 2025-03-26
     *
     * @return the protocol version interpreted from the header
     * @throws NoSuchElementException if the protocol is not one we support
     */
    private McpProtocolVersion readProtocolVersionHeader(String protocolVersion) {
        McpProtocolVersion result;
        if (protocolVersion == null) {
            result = McpProtocolVersion.V_2025_03_26;
        } else {
            result = McpProtocolVersion.parse(protocolVersion);
        }

        if (config.isAccepted(result)) {
            return result;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Reads the message from the stream into the transfer object
     *
     * @param req the servlet request
     * @return the transfer object
     * @throws IOException if the stream cannot be opened
     * @throws JSONRPCException if the message is not well formed enough to create the transfer object
     */
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
            JsonException jsonEx = tryUnwrapToJsonException(e);
            String message = jsonEx != null ? jsonEx.getMessage() : e.getMessage();
            throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR, List.of(message));
        }
    }

    private JsonException tryUnwrapToJsonException(JsonbException e) {
        Throwable c = e.getCause();
        // The limit here is just a safeguard. The loop should always terminate early.
        for (int i = 0; i < 20; i++) {
            if (c == null) {
                return null;
            }
            if (c instanceof JsonException jsonEx) {
                return jsonEx;
            }
            c = c.getCause();
        }
        return null;
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

    /**
     * Gets the requested method, or {@code null} if no method exists for the requested name.
     */
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
    public McpOperationRequest parse(McpOperationPartialRequest partialReq) throws IOException {
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
    private void validateProtocolVersion(McpOperationPartialRequest partialReq) {
        if (!partialReq.protocolVersionHeaderPresent()
            && partialReq.method() != null
            && partialReq.method().isPreInit()) {
            // Pre-init method, protocol version header not required
            return;
        }
        if (partialReq.protocolVersion() == null) {
            throw unsupportedProtocolVersion();
        }
    }

    private HttpResponseException unsupportedProtocolVersion() {
        String supportedValues = config.acceptedProtocolVersions().stream()
                                       .map(McpProtocolVersion::getVersion)
                                       .collect(Collectors.joining(", "));
        String exceptionMessage = Tr.formatMessage(tc, "unsupported.mcp.http.version", supportedValues);
        return new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, exceptionMessage);
    }

    /**
     * Find the session from a partial request
     *
     * @param partialReq
     * @return the session, or {@code null} if the session header is not present
     * @throws McpResponseException if the session header is present but does not match a valid session
     */
    private McpSession parseSession(McpOperationPartialRequest partialReq) {
        McpSession session;

        final McpSessionId sessionId = partialReq.sessionId();
        if (sessionId != null) {
            session = sessionStore.getSession(sessionId);
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
        if (!config.stateless() && !method.isPreInit()) {
            if (session == null) {
                throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST,
                                                "Missing Mcp-Session-Id header");
            }
        }
    }

    /**
     * Checks the protocol version header of a request which is not processed by {@link #init}, such as a session DELETE.
     * A missing header is allowed. A header which is unknown, or outside the configured range, is rejected.
     *
     * @param req the request
     * @throws HttpResponseException with status 400 if the header names a version which is not accepted
     */
    @FFDCIgnore(NoSuchElementException.class)
    public void validateProtocolVersionHeaderForDelete(HttpServletRequest req) {
        String header = req.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        if (header == null) {
            return;
        }

        try {
            if (!config.isAccepted(McpProtocolVersion.parse(header))) {
                throw unsupportedProtocolVersion();
            }
        } catch (NoSuchElementException e) {
            throw unsupportedProtocolVersion();
        }
    }

}

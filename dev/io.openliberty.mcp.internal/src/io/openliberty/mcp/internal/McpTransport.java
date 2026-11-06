/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.mcpjava.server.tools.ToolResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.responses.McpErrorResponse;
import io.openliberty.mcp.internal.responses.McpResponse;
import io.openliberty.mcp.internal.responses.McpResultResponse;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Represents the current request and allows the response to be sent
 */
public class McpTransport {
    public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
    private static final TraceComponent tc = Tr.register(McpTransport.class);
    private HttpServletRequest req;
    private HttpServletResponse res;
    private Jsonb jsonb;
    private McpOperationRequest mcpRequest;
    private Writer writer;
    private final long asyncTimeoutMs;
    private String appName;
    private RequestId requestId;
    private McpProtocolVersion protocolVersion;

    public McpTransport(HttpServletRequest req, HttpServletResponse res, Jsonb jsonb, long asyncTimeoutMs) throws IOException {
        this.req = req;
        this.res = res;
        this.jsonb = jsonb;
        this.asyncTimeoutMs = asyncTimeoutMs;
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        writer = res.getWriter(); // Writer must be acquired after setting response character encoding
        appName = extractAppName();
    }

    public McpOperationRequest getMcpOperationRequest() {
        return this.mcpRequest;
    }

    public void setMcpOperationRequest(McpOperationRequest request) {
        this.mcpRequest = request;
    }

    /*
     * Sets an HTTP header in the response.
     * <p>
     * Intended only for sending the Session ID header with initialize response
     */
    void setResponseHeader(String name, @Sensitive String value) {
        res.setHeader(name, value);
    }

    /**
     * Returns the current session associated with this transport, or null if no valid session was provided.
     *
     * @return the {@link McpSession} for this transport, or null if not available
     */
    public McpSession getSession() {
        return mcpRequest.session();
    }

    /*
     * Returns whether the current request has been successfully authenticated.
     */
    public boolean isAuthenticated() {
        return req.getUserPrincipal() != null;
    }

    /**
     * Returns the session ID associated with this transport.
     * This is the raw ID value provided by the client in the {@code Mcp-Session-Id} header,
     * or assigned by the server during initialization.
     *
     * @return the session ID string, or {@code null} if none is available
     */
    @Trivial
    public McpSessionId getSessionId() {
        if (mcpRequest != null && mcpRequest.session() != null) {
            return mcpRequest.session().getSessionId();
        } else {
            return null;
        }
    }

    /**
     * Sends the MCP Server's response with the results for the client's request
     *
     * @param result the result field to include in the MCP Server response
     * @throws IOException
     */
    public void sendResponse(Object result) {
        McpResponse mcpResponse = new McpResultResponse(mcpRequest.id(), result);
        res.setContentType("application/json");
        jsonb.toJson(mcpResponse, writer);
    }

    /**
     * Sends a 202 response for requests that don't expect anything to be sent back
     *
     * E.g. when a MCP client sends a notification, it'a not expecting anything back in the response
     */
    public void sendEmptyResponse() {
        res.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

    /**
     * sends custom error message response back to client depending on the error with 500 HTTPS error code
     *
     * @param e the exception that was caught
     * @throws IOException
     */
    public void sendError(Throwable e) throws IOException {
        String exceptionMessage = Tr.formatMessage(tc, "unexpected.server.error", new Object[] { req.getMethod(), req.getRequestURI(), req.getQueryString() });
        Tr.error(tc, "CWMCM0015E.unexpected.server.error.exception", req.getMethod(), req.getRequestURI(), req.getQueryString(), e.getMessage());
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exceptionMessage);
    }

    /**
     * sends custom error message response back to client depending on the error with 403 HTTPS error code
     *
     * @param e the exception that was caught
     * @throws IOException
     */
    public void sendAuthError(Throwable e) throws IOException {
        Tr.audit(tc, "CWMCM0033A.forbidden.error", req.getMethod(), req.getRequestURI(), req.getQueryString(), e.getMessage());
        res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
    }

    /**
     * This method is responsible for sending a JSON-RPC error response.
     * It takes a JSONRPCException as an argument and constructs an McpErrorResponse object.
     * This error response is then serialised to JSON and written to the provided writer.
     * <p>
     * Per the JSON-RPC 2.0 specification, notifications (requests with no id) must not be
     * responded to, including with error responses. If the current request is a notification,
     * this method returns without sending anything.
     *
     * @param e The JSONRPCException to be included in the error response.
     */
    public void sendJsonRpcException(JSONRPCException e) {
        // JSON-RPC notifications must not receive a response.
        if (requestId == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Ignoring JSONRPCException for notification (no id): " + e.getMessage(), e);
            }
            sendEmptyResponse();
            return;
        }
        McpResponse mcpResponse = new McpErrorResponse(requestId, e);

        res.setContentType("application/json");
        jsonb.toJson(mcpResponse, writer);
    }

    /**
     * Sends a response back to client when an HTTP exception occurs
     *
     * @param e The HttpResponseException to be included in the error response.
     * @throws IOException
     */
    public void sendHttpException(HttpResponseException e) throws IOException {
        res.setStatus(e.getStatusCode());
        if (e.getHeaders() != null)
            e.getHeaders().forEach((key, val) -> res.setHeader(key, val));
        if (e.getMessage() != null) {
            res.setContentType("text/plain");
            writer.write(e.getMessage());
        }
    }

    public String getRequestIpAddress() {
        String ipAddress = req.getRemoteAddr();
        String proxyAddress = req.getHeader("X-Forwarded-For");
        if (proxyAddress != null && !proxyAddress.equals(ipAddress)) {
            Tr.error(tc, "CWMCM0021E.unknown.proxy.address");
            throw new HttpResponseException(HttpServletResponse.SC_FORBIDDEN, "");
        }
        return ipAddress;
    }

    public McpProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(McpProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setRequestId(RequestId requestId) {
        this.requestId = requestId;
    }

    public <T> CompletionStage<T> sendResultAsync(CompletionStage<T> stage) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(asyncTimeoutMs);
        return stage.handle((result, throwable) -> {
            try {
                if (throwable == null) {
                    sendResponse(result);

                } else {
                    Throwable cause = throwable instanceof java.util.concurrent.CompletionException ? throwable.getCause() : throwable;
                    if (cause instanceof JSONRPCException jsonRpcEx) {
                        sendJsonRpcException(jsonRpcEx);
                    } else if (cause instanceof HttpResponseException httpEx) {
                        sendHttpException(httpEx);
                    } else {
                        sendError(throwable);
                    }
                }
            } catch (Exception e) {
                Tr.error(tc, "CWMCM0016E.error.sending.response.exception", e);
                sendResponse(ToolResponse.ofError(Tr.formatMessage(tc, "internal.server.error")));
            } finally {
                asyncContext.complete();
            }
            return result;
        });
    }

    /**
     * Check if user is in role
     */
    public boolean isUserInRole(String role) {
        return req.isUserInRole(role);
    }

    /**
     * Check if user has any of the roles in the List
     */
    public boolean isUserInRole(List<String> roleList) {
        for (String role : roleList) {
            if (isUserInRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the application name from the component metadata.
     * This is used to track which application's MBeans should be cleaned up on unload.
     *
     * @return the application name, or "unknown-app" if it cannot be determined
     */
    private String extractAppName() {
        ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (componentMetaData != null) {
            String appName = componentMetaData.getJ2EEName().getApplication();
            if (appName != null && !appName.isEmpty()) {
                return appName;
            }
        }
        return "unknown-app";
    }

    /**
     * Returns the application name associated with this transport.
     *
     * @return the application name
     */
    public String getAppName() {
        return appName;
    }

    /**
     *
     * @return the user principle injected after authorisation (can be null if authorisation failed)
     */
    public Principal getUser() {
        return req.getUserPrincipal();
    }

    /**
     * @return the req
     */
    public HttpServletRequest getReq() {
        return req;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "McpTransport@" + Integer.toHexString(System.identityHashCode(this))
               + "[requestId=" + requestId
               + ", sessionId=" + getSessionId()
               + ", appName=" + appName + "]";
    }

}

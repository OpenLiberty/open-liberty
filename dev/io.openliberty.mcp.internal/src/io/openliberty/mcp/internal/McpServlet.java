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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.config.McpConfiguration;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.meta.MetaImpl;
import io.openliberty.mcp.internal.requests.CancellationImpl;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpNotificationParams;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
import io.openliberty.mcp.internal.security.Authorizer;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.tools.ToolManager.ToolArguments;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.meta.Meta;
import io.openliberty.mcp.request.RequestId;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class McpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(McpServlet.class);
    private static final ServiceCaller<McpConfiguration> mcpConfigService = new ServiceCaller<>(McpServlet.class, McpConfiguration.class);

    @Inject
    BeanManager bm;

    @Inject
    McpSessionStore sessionStore;

    @Inject
    McpRequestTracker requestTracker;

    @Inject
    McpCdiExtension cdiExtension;

    private Jsonb jsonb;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = cdiExtension.getJsonb();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        McpTransport transport = new McpTransport(req, resp, jsonb);
        String excpetionMessage = Tr.formatMessage(tc, "CWMCM0009I.get.disallowed");
        HttpResponseException e = new HttpResponseException(
                                                            HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                                            excpetionMessage).withHeader("Allow", "POST");
        transport.sendHttpException(e);

    }

    @Override
    @FFDCIgnore({ JSONRPCException.class, HttpResponseException.class })
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, JSONRPCException {
        McpTransport transport = new McpTransport(req, resp, jsonb);
        try {
            Boolean stateless = mcpConfigService.run(config -> {
                boolean s = config.isStateless();
                return s;
            }).orElse(false);

            transport.init(sessionStore);

            RequestMethod method = transport.getMcpRequest().getRequestMethod();

            if (!stateless && method != RequestMethod.INITIALIZE && method != RequestMethod.PING) {
                McpSession session = transport.getSession();
                if (session == null) {
                    throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST,
                                                    "Missing Mcp-Session-Id header");
                }
            }
            callRequest(transport);
        } catch (JSONRPCException e) {
            transport.sendJsonRpcException(e);
        } catch (HttpResponseException e) {
            transport.sendHttpException(e);
        } catch (Exception e) {
            transport.sendError(e);
        }
    }

    protected void callRequest(McpTransport transport)
                    throws JSONRPCException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
        RequestMethod method = transport.getMcpRequest().getRequestMethod();
        switch (method) {
            case TOOLS_CALL -> callTool(transport);
            case TOOLS_LIST -> listTools(transport);
            case INITIALIZE -> initialize(transport);
            case INITIALIZED -> initialized(transport);
            case PING -> ping(transport);
            case CANCELLED -> cancelRequest(transport);
            default -> throw new JSONRPCException(JSONRPCErrorCode.METHOD_NOT_FOUND, List.of(String.valueOf(method + " not found")));
        }

    }

    /*
     * Delete method for deleting sessionId
     */

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean stateless = Boolean.TRUE.equals(
                                                mcpConfigService.run(McpConfiguration::isStateless).orElse(false));

        if (stateless) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Session not found");
            return;
        }

        final String sessionId = req.getHeader(McpTransport.MCP_SESSION_ID_HEADER);

        if (sessionId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id");
            return;
        }

        if (sessionStore.isValid(sessionId)) {
            sessionStore.deleteSession(sessionId);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Session not found");
        }
    }

    @FFDCIgnore({ IllegalAccessException.class, IllegalArgumentException.class })
    private void callTool(McpTransport transport) {

        ExecutionRequestId requestId = createOngoingRequestId(transport);
        McpToolCallParams params = transport.getParams(McpToolCallParams.class);

        if (requestId != null && requestTracker.isOngoingRequest(requestId)) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS,
                                       Tr.formatMessage(tc, "CWMCM0008E.invalid.request.params", requestId.id()));
        }

        try {
            if (params.getMetadata() == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Attempt to call non-existant tool: " + params.getName());
                }
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Method " + params.getName() + " not found"));
            }

            Authorizer.requireAuthorized(transport, params.getMetadata());

            if (params.getMetadata().returnsCompletionStage()) {
                callToolMethodAndSendResponseAsync(transport, requestId, params);
            } else {
                callToolSynchronously(transport, requestId, params);
            }
        } catch (IllegalAccessException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + params.getName()));
        } catch (IllegalArgumentException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
        }
    }

    private void callToolSynchronously(McpTransport transport,
                                       ExecutionRequestId requestId,
                                       McpToolCallParams params)
                    throws IllegalAccessException, IllegalArgumentException {

        ToolArguments toolArgs = createToolArguments(params);
        if (requestId != null) {
            requestTracker.registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        try {
            var handler = params.getMetadata().handler();

            ToolResponse response = handler.apply(toolArgs);
            transport.sendResponse(response);
        } finally {
            cleanup(requestId);
        }
    }

    private void callToolMethodAndSendResponseAsync(McpTransport transport,
                                                    ExecutionRequestId requestId,
                                                    McpToolCallParams params)
                    throws IllegalAccessException, IllegalArgumentException {
        ToolArguments toolArgs = createToolArguments(params);

        if (requestId != null) {
            requestTracker.registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        var handler = params.getMetadata().asyncHandler();

        CompletionStage<ToolResponse> response = handler.apply(toolArgs);
        transport.sendResultAsync(response)
                 .whenComplete((result, throwable) -> cleanup(requestId));
    }

    /**
     * @return
     */
    private ToolArguments createToolArguments(McpToolCallParams params) {
        Map<String, Object> args = params.getArguments(jsonb);
        Meta meta = new MetaImpl(params.getMeta(), jsonb);
        return new ToolArgumentsImpl(args, new CancellationImpl(), meta);
    }

    record ToolArgumentsImpl(Map<String, Object> args, Cancellation cancellation, Meta meta) implements ToolArguments {}

    private void cleanup(ExecutionRequestId requestId) {
        if (requestId != null && requestTracker.isOngoingRequest(requestId)) {
            requestTracker.deregisterOngoingRequest(requestId);
        }
    }

    /**
     * @param request
     * @return
     * @throws IOException
     */
    private void listTools(McpTransport transport) throws IOException {
        ToolRegistry toolRegistry = ToolRegistry.get();

        List<ToolDescription> response = new LinkedList<>();

        if (toolRegistry.hasTools()) {
            for (ToolMetadata tmd : toolRegistry.getAllTools()) {
                if (Authorizer.isAuthorized(transport, tmd)) {
                    response.add(new ToolDescription(tmd));
                }
            }
            ToolResult toolResult = new ToolResult(response);
            transport.sendResponse(toolResult);
        }
    }

    /**
     * @param request
     * @param writer
     * @return
     * @throws IOException
     */
    @FFDCIgnore(NoSuchElementException.class)
    private void initialize(McpTransport transport) throws IOException {
        McpInitializeParams params = transport.getParams(McpInitializeParams.class);

        McpProtocolVersion version;
        try {
            version = McpProtocolVersion.parse(params.getProtocolVersion());
        } catch (NoSuchElementException e) {
            // Client requested version not supported
            // Respond with our preferred version
            version = McpProtocolVersion.V_2025_06_18;
        }
        // TODO store client capabilities
        // TODO store client info

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Client initializing: " + params.getClientInfo(), params.getCapabilities());
        }

        String sessionId = sessionStore.createSession();

        ServerCapabilities caps = ServerCapabilities.of(new Capabilities.Tools(false));

        // TODO: provide a way for the user to set server info
        ServerInfo info = new ServerInfo("test-server", "Test Server", "0.1");
        McpInitializeResult result = new McpInitializeResult(version, caps, info, null);

        transport.setResponseHeader(McpTransport.MCP_SESSION_ID_HEADER, sessionId);
        transport.sendResponse(result);
    }

    private void initialized(McpTransport transport) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Client initialized");
        }
        transport.sendEmptyResponse();
    }

    private void ping(McpTransport transport) {
        transport.sendResponse(new Object());
    }

    private void cancelRequest(McpTransport transport) {
        McpNotificationParams notificationParams = transport.getMcpRequest().getParams(McpNotificationParams.class, jsonb);
        RequestId mcpReqId = notificationParams.getRequestId();
        McpSessionId sessionId = transport.getSessionId();
        if (sessionId == null) {
            transport.sendEmptyResponse();
            return;
        }

        ExecutionRequestId requestId = new ExecutionRequestId(mcpReqId, sessionId);
        Optional<String> reason = Optional.ofNullable(notificationParams.getReason());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Cancellation requested for " + requestId);
        }

        Cancellation cancellation = requestTracker.getOngoingRequestCancellation(requestId);
        if (cancellation != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Cancelling task");
            }
            ((CancellationImpl) cancellation).cancel(reason);
        }
        transport.sendEmptyResponse();
    }

    private ExecutionRequestId createOngoingRequestId(McpTransport transport) {
        McpSessionId sessionId = transport.getSessionId();
        if (sessionId != null) {
            return new ExecutionRequestId(
                                          transport.getMcpRequest().id(),
                                          sessionId);
        } else {
            return null;
        }
    }
}

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

import static io.openliberty.mcp.internal.McpServletInitializer.STATELESS_INIT_PARAM;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.security.sasl.AuthenticationException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.McpResponseException;
import io.openliberty.mcp.internal.meta.MetaImpl;
import io.openliberty.mcp.internal.requests.CancellationImpl;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpNotificationParams;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.requests.McpToolListParams;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
import io.openliberty.mcp.internal.security.Authorizer;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.tools.ToolResponses;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.meta.Meta;
import io.openliberty.mcp.request.RequestId;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolManager.ToolArguments;
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
    private static final int PAGE_SIZE = 20;

    @Inject
    BeanManager bm;

    @Inject
    McpSessionStore sessionStore;

    @Inject
    McpRequestTracker requestTracker;

    @Inject
    McpCdiExtension cdiExtension;

    @Inject
    EncoderRegistry encoderRegistry;

    private Jsonb jsonb;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = cdiExtension.getJsonb();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        McpTransport transport = new McpTransport(req, resp, jsonb);
        String excpetionMessage = Tr.formatMessage(tc, "get.disallowed");
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
            transport.init(sessionStore);

            RequestMethod method = transport.getMcpRequest().getRequestMethod();

            if (!isServerStateless() && method != RequestMethod.INITIALIZE && method != RequestMethod.PING) {
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
        if (isServerStateless()) {
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

    @FFDCIgnore(ToolCallException.class)
    private void callTool(McpTransport transport) {
        ExecutionRequestId requestId = createOngoingRequestId(transport);
        McpToolCallParams params = transport.getParams(McpToolCallParams.class);
        McpRequest request = transport.getMcpRequest();

        if (requestId != null && requestTracker.isOngoingRequest(requestId)) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS,
                                       Tr.formatMessage(tc, "invalid.request.params", requestId.id()));
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
                callToolAndSendResponseAsync(transport, requestId, request, params);
            } else {
                callToolAndSendResponseSync(transport, requestId, request, params);
            }
        } catch (ToolCallException e) {
            // Catch validation errors that occur before calling the tool and should result in a tool call error response
            ToolResponse response = ToolResponses.createBusinessErrorResponse(e);
            transport.sendResponse(response);
            return;
        }
    }

    @FFDCIgnore({ McpResponseException.class, ToolCallException.class, Exception.class })
    private void callToolAndSendResponseSync(McpTransport transport,
                                             ExecutionRequestId requestId,
                                             McpRequest mcpRequest,
                                             McpToolCallParams params) {

        ToolArguments toolArgs = createToolArguments(mcpRequest, params);
        if (requestId != null) {
            requestTracker.registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        ToolResponse response;
        try {
            var handler = params.getMetadata().handler();
            response = handler.apply(toolArgs);
        } catch (McpResponseException e) {
            // These exceptions indicate a specific response should be used
            throw e;
        } catch (ToolCallException e) {
            // ToolCallException is the only business exception type that can be thrown by a handler
            response = ToolResponses.createBusinessErrorResponse(e);
        } catch (Exception e) {
            // Any other exception should be turned into an error tool response
            response = ToolResponses.createNonBusinessErrorResponse(e, params.getName());
        } finally {
            cleanup(requestId);
        }
        response = removeStructuredContentIfNotSupported(response, transport);
        transport.sendResponse(response);
    }

    private void callToolAndSendResponseAsync(McpTransport transport,
                                              ExecutionRequestId requestId,
                                              McpRequest mcpRequest,
                                              McpToolCallParams params) {
        ToolArguments toolArgs = createToolArguments(mcpRequest, params);

        if (requestId != null) {
            requestTracker.registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        var handler = params.getMetadata().asyncHandler();

        CompletionStage<ToolResponse> response = callHandlerAndCatchException(handler, toolArgs);
        response = response.thenApply(r -> removeStructuredContentIfNotSupported(r, transport))
                           .exceptionally(throwable -> {
                               if (throwable instanceof CompletionException) {
                                   throwable = throwable.getCause();
                               }
                               if (throwable instanceof McpResponseException responseEx) {
                                   throw responseEx;
                               } else if (throwable instanceof ToolCallException toolEx) {
                                   return ToolResponses.createBusinessErrorResponse(toolEx);
                               } else {
                                   return ToolResponses.createNonBusinessErrorResponse(throwable,
                                                                                       params.getName());
                               }
                           });

        transport.sendResultAsync(response)
                 .whenComplete((result, throwable) -> cleanup(requestId));
    }

    @FFDCIgnore(Exception.class)
    private static <T, R> CompletionStage<R> callHandlerAndCatchException(Function<T, CompletionStage<R>> handler, T arg) {
        try {
            return handler.apply(arg);
        } catch (Exception e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private ToolResponse removeStructuredContentIfNotSupported(ToolResponse response, McpTransport transport) {
        if (transport.getProtocolVersion().supportsStructuredContent()) {
            return response;
        }

        if (response.structuredContent() == null) {
            return response;
        }

        List<? extends Content> responseContent = response.content() != null ? response.content() : List.of(new TextContent(jsonb.toJson(response.structuredContent())));

        return new ToolResponse(response.isError(), responseContent, null, response._meta());
    }

    /**
     * @return
     */
    private ToolArguments createToolArguments(McpRequest request, McpToolCallParams params) {
        Map<String, Object> args = params.getArguments(jsonb);
        Meta meta = new MetaImpl(params.getMeta(), jsonb);
        RequestId requestId = request.id();

        return new ToolArgumentsImpl(args, new CancellationImpl(), meta, encoderRegistry, requestId);
    }

    public record ToolArgumentsImpl(Map<String, Object> args,
                                    Cancellation cancellation,
                                    Meta meta,
                                    EncoderRegistry encoderRegistry,
                                    RequestId requestId) implements ToolArguments {}

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

        if (!toolRegistry.hasTools()) {
            transport.sendResponse(new ToolResult(List.of()));
            return;
        }

        boolean supportsStructuredContent = transport.getProtocolVersion().supportsStructuredContent();
        McpToolListParams params = transport.getParams(McpToolListParams.class);
        String cursor = params != null ? params.getCursor() : null;

        List<ToolMetadata> allTools = toolRegistry.getAllTools();

        int startIndex = findStartIndex(allTools, cursor);

        //get PAGE_SIZE + 1 tools to see if there's more authorised tools after PAGE_SIZE
        List<ToolMetadata> authorisedTools = allTools.stream()
                                                     .skip(startIndex)
                                                     .filter(tmd -> Authorizer.isAuthorized(transport, tmd))
                                                     .limit(PAGE_SIZE + 1)
                                                     .toList();

        boolean theresMore = authorisedTools.size() > PAGE_SIZE;

        List<ToolDescription> response = authorisedTools.stream()
                                                        .limit(PAGE_SIZE)
                                                        .map(toolMetadata -> {
                                                            return new ToolDescription(toolMetadata, supportsStructuredContent);
                                                        })
                                                        .toList();

        String nextCursor = theresMore ? authorisedTools.get(PAGE_SIZE - 1).name() : null;

        ToolResult toolResult = new ToolResult(response, nextCursor);
        transport.sendResponse(toolResult);
    }

    private int findStartIndex(List<ToolMetadata> allTools, String cursor) {

        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < allTools.size(); i++) {
            if (allTools.get(i).name().equals(cursor)) {
                return i + 1;
            }
        }
        throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS,
                                   Tr.formatMessage(tc, "CWMCM0022E.invalid.cursor.value", cursor));
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
            version = McpProtocolVersion.V_2025_11_25;
        }
        // TODO store client capabilities
        // TODO store client info

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Client initializing: " + params.getClientInfo(), params.getCapabilities());
        }
        Principal userId = transport.getUser();

        String sessionId = sessionStore.createSession(userId);

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

    private void cancelRequest(McpTransport transport) throws IOException {
        McpNotificationParams notificationParams = transport.getMcpRequest().getParams(McpNotificationParams.class, jsonb);
        RequestId mcpReqId = notificationParams.getRequestId();
        McpSessionId sessionId = transport.getSessionId();
        Principal userId = transport.getUser();

        if (sessionId == null) {
            transport.sendEmptyResponse();
            return;
        } else {
            var session = sessionStore.getSession(sessionId.value());
            if (session == null || !Objects.equals(session.getUserId(), userId)) {
                transport.sendAuthError(new AuthenticationException(Tr.formatMessage(tc, "unauthorized.cancellation")));
                return;
            }
        }

        ExecutionRequestId requestId = new ExecutionRequestId(mcpReqId, sessionId, userId);
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
        Principal userId = transport.getUser();
        if (sessionId != null) {
            return new ExecutionRequestId(
                                          transport.getMcpRequest().id(),
                                          sessionId, userId);
        } else {
            return null;
        }
    }

    /**
     * Gets the stateless configuration for the MCP server. The configuration is stored in the
     * servlet config as an initParam by McpServletInitializer
     *
     * @return true if stateless mode is enabled, and false otherwise
     */
    private boolean isServerStateless() {
        return Boolean.parseBoolean(getServletConfig().getInitParameter(STATELESS_INIT_PARAM));
    }

}

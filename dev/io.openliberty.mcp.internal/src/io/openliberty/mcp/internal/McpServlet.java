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
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.encoders.EncoderRegistries;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.McpResponseException;
import io.openliberty.mcp.internal.meta.MetaImpl;
import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
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
import io.openliberty.mcp.internal.sessions.McpSessionStores;
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
    EncoderRegistries encoderRegistries;

    @Inject
    McpSessionStores sessionStores;

    @Inject
    McpRequestTrackers requestTrackers;

    @Inject
    McpCdiExtension cdiExtension;

    @Inject
    ConverterRegistries converterRegistries;

    @Inject
    McpConfig mcpConfig;

    private Jsonb jsonb;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = cdiExtension.getJsonb();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        McpTransport transport = new McpTransport(req, resp, jsonb, mcpConfig.asyncTimeout());
        String excpetionMessage = Tr.formatMessage(tc, "get.disallowed");
        HttpResponseException e = new HttpResponseException(
                                                            HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                                            excpetionMessage).withHeader("Allow", "POST");
        transport.sendHttpException(e);

    }

    @Override
    @FFDCIgnore({ JSONRPCException.class, HttpResponseException.class })
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, JSONRPCException {
        McpTransport transport = new McpTransport(req, resp, jsonb, mcpConfig.asyncTimeout());
        McpOperationMetrics metrics = new McpOperationMetrics();

        try {
            transport.init(sessionStores.getCurrent());

            RequestMethod method = transport.getMcpRequest().getRequestMethod();

            if (!isServerStateless() && method != RequestMethod.INITIALIZE && method != RequestMethod.PING) {
                McpSession session = transport.getSession();
                if (session == null) {
                    throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST,
                                                    "Missing Mcp-Session-Id header");
                }
            }

            metrics.setTransport(transport);

            callRequest(transport, metrics);
        } catch (JSONRPCException e) {
            String jsonRpcErrorMsg = "JSONRPCException ";
            if (e.getErrorCode() != null) {
                jsonRpcErrorMsg += "{code=" + e.getErrorCode().getCode()
                                   + ", message='" + e.getErrorCode().getMessage()
                                   + "', data=" + String.valueOf(e.getData()) + "}";
            } else {
                jsonRpcErrorMsg += "{data=" + String.valueOf(e.getData()) + "}";
            }

            traceEvent("The following error was returned to the user: '" + jsonRpcErrorMsg + "'");

            metrics.setOutcome("error", e.getErrorCode().name());
            McpOperationMetrics.operationEnded(metrics);

            transport.sendJsonRpcException(e);
        } catch (HttpResponseException e) {
            String errorMsg = "HTTP " + e.getStatusCode();
            if (e.getMessage() != null) {
                errorMsg += " - " + e.getMessage();
            }
            traceEvent("The following error was returned to the user: '" + errorMsg + "'");

            metrics.setOutcome("error", "http_error");
            McpOperationMetrics.operationEnded(metrics);

            transport.sendHttpException(e);
        } catch (Exception e) {
            String errorMsg = e.getClass().getSimpleName();
            if (e.getMessage() != null) {
                errorMsg += ": " + e.getMessage();
            }
            traceEvent("The following error was returned to the user: '" + errorMsg + "'");

            metrics.setOutcome("error", "internal_error");
            McpOperationMetrics.operationEnded(metrics);

            transport.sendError(e);
        }
    }

    protected void callRequest(McpTransport transport, McpOperationMetrics metrics)
                    throws JSONRPCException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
        RequestMethod method = transport.getMcpRequest().getRequestMethod();

        metrics.setMethodName(method.getMethodName());

        switch (method) {
            case TOOLS_CALL -> {
                callTool(transport, metrics);
            }
            case TOOLS_LIST -> {
                listTools(transport, metrics);
            }
            case INITIALIZE -> {
                initialize(transport, metrics);
            }
            case INITIALIZED -> {
                initialized(transport, metrics);
            }
            case PING -> {
                ping(transport, metrics);
            }
            case CANCELLED -> {
                cancelRequest(transport, metrics);
            }
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

        String sessionIdStr = req.getHeader(McpTransport.MCP_SESSION_ID_HEADER);

        if (sessionIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id");
            return;
        }

        McpSessionId sessionId = new McpSessionId(sessionIdStr);

        if (sessionStores.getCurrent().isValid(sessionId)) {
            sessionStores.getCurrent().deleteSession(sessionId);
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Session not found");
        }
    }

    @FFDCIgnore(ToolCallException.class)
    private void callTool(McpTransport transport, McpOperationMetrics metrics) {
        traceEvent("A tool call request has arrived");

        ExecutionRequestId requestId = createOngoingRequestId(transport);

        McpToolCallParams params = transport.getParams(McpToolCallParams.class);
        if (params != null && params.getName() != null) {
            metrics.setToolName(params.getName());
        }
        McpRequest request = transport.getMcpRequest();

        try {
            if (requestId != null && requestTrackers.getCurrent().isOngoingRequest(requestId)) {
                throw new JSONRPCException(
                                           JSONRPCErrorCode.INVALID_PARAMS,
                                           Tr.formatMessage(tc, "invalid.request.params", requestId.id()));
            }

            if (params.getMetadata() == null) {
                traceEvent("Attempt to call non-existant tool: " + params.getName());
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Method " + params.getName() + " not found"));
            }

            Authorizer.requireAuthorized(transport, params.getMetadata());

            if (params.getMetadata().returnsCompletionStage()) {
                ToolArguments toolArgs = createToolArguments(request, params);
                callToolAndSendResponseAsync(transport, requestId, params, toolArgs, metrics);
            } else {
                callToolAndSendResponseSync(transport, requestId, request, params, metrics);
            }
        } catch (ToolCallException e) {
            // Catch validation errors that occur before calling the tool and should result in a tool call error response
            ToolResponse response = ToolResponses.createBusinessErrorResponse(e);
            if (response.isError()) {
                traceEvent("The tool method '" + params.getName() + "' returned the following error to the user: '" + extractToolResponseValue(response) + "'");
            }
            sendToolResponseAndEndMetrics(transport, response, metrics);
        }
    }

    @FFDCIgnore({ McpResponseException.class, ToolCallException.class, Exception.class })
    private void callToolAndSendResponseSync(McpTransport transport,
                                             ExecutionRequestId requestId,
                                             McpRequest mcpRequest,
                                             McpToolCallParams params,
                                             McpOperationMetrics metrics) {

        ToolArguments toolArgs = createToolArguments(mcpRequest, params);
        if (requestId != null) {
            requestTrackers.getCurrent().registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        ToolResponse response;
        try {
            var handler = params.getMetadata().handler();
            traceEvent("The tool method '" + params.getName() + "' is about to be called");
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
        if (response.isError()) {
            traceEvent("The tool method '" + params.getName() + "' returned the following error to the user: '" + extractToolResponseValue(response) + "'");
        } else {
            traceEvent("The tool method '" + params.getName() + "' returned: '" + extractToolResponseValue(response) + "'");
        }
        sendToolResponseAndEndMetrics(transport, response, metrics);
    }

    private void callToolAndSendResponseAsync(McpTransport transport,
                                              ExecutionRequestId requestId,
                                              McpToolCallParams params,
                                              ToolArguments toolArgs,
                                              McpOperationMetrics metrics) {

        if (requestId != null) {
            requestTrackers.getCurrent().registerOngoingRequest(requestId, (CancellationImpl) toolArgs.cancellation());
        }

        var handler = params.getMetadata().asyncHandler();

        traceEvent("The tool method '" + params.getName() + "' is about to be called");
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
                                   return ToolResponses.createNonBusinessErrorResponse(throwable, params.getName());
                               }
                           });

        transport.sendResultAsync(response)
                 .whenComplete((result, throwable) -> completeAsyncMetrics(result, throwable, params.getName(), metrics))
                 .whenComplete((result, throwable) -> traceAsyncResult(result, params.getName()))
                 .whenComplete((result, throwable) -> cleanup(requestId));
    }

    private void completeAsyncMetrics(ToolResponse result, Throwable throwable, String toolName, McpOperationMetrics metrics) {
        String status = determineAsyncStatus(result, throwable);
        String errorType = determineAsyncErrorType(result, throwable);

        metrics.setOutcome(status, errorType);
        McpOperationMetrics.operationEnded(metrics);
    }

    private String determineAsyncStatus(ToolResponse result, Throwable throwable) {
        if (throwable != null) {
            return "error";
        }
        if (result != null && result.isError()) {
            return "error";
        }
        return "ok";
    }

    private String determineAsyncErrorType(ToolResponse result, Throwable throwable) {
        if (throwable != null) {
            Throwable actual = throwable instanceof CompletionException ? throwable.getCause() : throwable;

            if (actual instanceof JSONRPCException jsonRpcEx) {
                return jsonRpcEx.getErrorCode().name();
            }
            return "internal_error";
        }

        if (result != null && result.isError()) {
            return "tool_error";
        }

        return null;
    }

    /**
     * Traces the result of an async tool call.
     * This only traces successful completions (when result is not null).
     * Exceptions are logged separately in McpTransport.sendResultAsync().
     */
    private void traceAsyncResult(ToolResponse result, String toolName) {
        if (result != null) {
            if (result.isError()) {
                traceEvent("The tool method '" + toolName + "' returned the following error to the user: '" + extractToolResponseValue(result) + "'");
            } else {
                traceEvent("The tool method '" + toolName + "' returned: '" + extractToolResponseValue(result) + "'");
            }
        }
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
        Map<String, Object> args = params.getArguments(jsonb, converterRegistries.getCurrent());
        Meta meta = new MetaImpl(params.getMeta(), jsonb);
        RequestId requestId = request.id();

        return new ToolArgumentsImpl(args, new CancellationImpl(), meta, encoderRegistries.getCurrent(), requestId);
    }

    public record ToolArgumentsImpl(Map<String, Object> args,
                                    Cancellation cancellation,
                                    Meta meta,
                                    EncoderRegistry encoderRegistry,
                                    RequestId requestId) implements ToolArguments {}

    /**
     * Sends a tool response and ends metrics recording based on the response's error status.
     * This method is specifically for tool call operations that return ToolResponse objects.
     *
     * @param transport the transport to send the response through
     * @param response the tool response to send
     * @param metrics the metrics object to update and end
     */
    private void sendToolResponseAndEndMetrics(McpTransport transport, ToolResponse response, McpOperationMetrics metrics) {
        String status = response.isError() ? "error" : "ok";
        String errorType = response.isError() ? "tool_error" : null;

        metrics.setOutcome(status, errorType);
        McpOperationMetrics.operationEnded(metrics);

        transport.sendResponse(response);
    }

    /**
     * Send a successful response and complete metrics.
     * Use this for non-tool operations that return generic objects.
     */
    private void sendSuccessResponseAndEndMetrics(McpTransport transport, Object response, McpOperationMetrics metrics) {
        metrics.setOutcome("ok", null);
        McpOperationMetrics.operationEnded(metrics);
        transport.sendResponse(response);
    }

    /**
     * Send an empty response and complete metrics
     */
    private void sendEmptyResponseAndEndMetrics(McpTransport transport, McpOperationMetrics metrics) {
        metrics.setOutcome("ok", null);
        McpOperationMetrics.operationEnded(metrics);
        transport.sendEmptyResponse();
    }

    /**
     * Send an auth error and complete metrics
     */
    private void sendAuthErrorAndEndMetrics(McpTransport transport, AuthenticationException e, String errorType, McpOperationMetrics metrics) throws IOException {
        metrics.setOutcome("error", errorType);
        McpOperationMetrics.operationEnded(metrics);
        transport.sendAuthError(e);
    }

    private void cleanup(ExecutionRequestId requestId) {
        if (requestId != null && requestTrackers.getCurrent().isOngoingRequest(requestId)) {
            requestTrackers.getCurrent().deregisterOngoingRequest(requestId);
        }
    }

    /**
     * @param request
     * @return
     * @throws IOException
     */
    private void listTools(McpTransport transport, McpOperationMetrics metrics) throws IOException {
        ToolRegistry toolRegistry = ToolRegistry.get();

        if (!toolRegistry.hasTools()) {
            sendSuccessResponseAndEndMetrics(transport, new ToolResult(List.of()), metrics);
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

        sendSuccessResponseAndEndMetrics(transport, toolResult, metrics);
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
    private void initialize(McpTransport transport, McpOperationMetrics operationMetrics) throws IOException {
        McpSessionMetrics sessionMetrics = new McpSessionMetrics();

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

        traceEvent("Client initializing: " + params.getClientInfo(), params.getCapabilities());
        Principal userId = transport.getUser();

        McpSessionId sessionId = sessionStores.getCurrent().createSession(userId, sessionMetrics);
        sessionMetrics.setTransport(transport);

        ServerCapabilities caps = ServerCapabilities.of(new Capabilities.Tools(false));
        ServerInfo info = mcpConfig.serverInfo();
        McpInitializeResult result = new McpInitializeResult(version, caps, info, null);

        if (sessionId != null) {
            transport.setResponseHeader(McpTransport.MCP_SESSION_ID_HEADER, sessionId.value());
        }
        sendSuccessResponseAndEndMetrics(transport, result, operationMetrics);
    }

    private void initialized(McpTransport transport, McpOperationMetrics metrics) {
        traceEvent("Client initialized");
        sendEmptyResponseAndEndMetrics(transport, metrics);
    }

    private void ping(McpTransport transport, McpOperationMetrics metrics) {
        sendSuccessResponseAndEndMetrics(transport, new Object(), metrics);
    }

    private void cancelRequest(McpTransport transport, McpOperationMetrics metrics) throws IOException {
        McpNotificationParams notificationParams = transport.getMcpRequest().getParams(McpNotificationParams.class, jsonb);
        RequestId mcpReqId = notificationParams.getRequestId();
        McpSessionId sessionId = transport.getSessionId();
        Principal userId = transport.getUser();

        if (sessionId == null) {
            sendEmptyResponseAndEndMetrics(transport, metrics);
            return;
        }

        var session = sessionStores.getCurrent().getSession(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), userId)) {
            sendAuthErrorAndEndMetrics(transport,
                                       new AuthenticationException(Tr.formatMessage(tc, "unauthorized.cancellation")),
                                       "AuthenticationException",
                                       metrics);
            return;
        }

        ExecutionRequestId requestId = new ExecutionRequestId(mcpReqId, sessionId, userId);
        Optional<String> reason = Optional.ofNullable(notificationParams.getReason());

        traceEvent("Cancellation requested for " + requestId);

        Cancellation cancellation = requestTrackers.getCurrent().getOngoingRequestCancellation(requestId);
        if (cancellation != null) {
            traceEvent("Cancelling task");
            ((CancellationImpl) cancellation).cancel(reason);
        }

        sendEmptyResponseAndEndMetrics(transport, metrics);
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

    /**
     * Logs an event trace message if event tracing is enabled.
     *
     * @param message the message to log
     * @param inserts optional additional objects to include in the trace
     */
    private void traceEvent(String message, Object... inserts) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, message, inserts);
        }
    }

    /**
     * Extracts the most relevant value from a ToolResponse for logging purposes.
     * <p>
     * This method prioritises structured content over text content, and extracts
     * plain text from single TextContent objects for cleaner log output.
     *
     * @param response the ToolResponse to extract a value from
     * @return the structured content if present, the text from a single TextContent,
     * the full content list if multiple items exist, or the response itself
     * if no content is available
     */
    private Object extractToolResponseValue(ToolResponse response) {
        if (response.structuredContent() != null) {
            return response.structuredContent();
        }
        if (response.content() != null && !response.content().isEmpty()) {
            // Extract text from TextContent objects
            List<? extends Content> contentList = response.content();
            if (contentList.size() == 1 && contentList.get(0) instanceof TextContent textContent) {
                return textContent.text();
            }
            return response.content();
        }
        return response;
    }

}

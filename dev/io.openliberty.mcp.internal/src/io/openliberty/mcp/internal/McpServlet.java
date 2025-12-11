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
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.config.McpConfiguration;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.CancellationImpl;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpNotificationParams;
import io.openliberty.mcp.internal.requests.McpRequestId;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
import io.openliberty.mcp.messaging.Cancellation;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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

    private Jsonb jsonb;

    @Inject
    BeanManager bm;

    @Inject
    McpSessionStore sessionStore;

    @Inject
    McpRequestTracker requestTracker;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = JsonbBuilder.create();
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
            McpSession session = sessionStore.getSession(sessionId);

            if (session != null) {
                requestTracker.cancelSessionRequests(sessionId);
            }
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

            if (params.getMetadata().returnsCompletionStage()) {
                callToolMethodAndSendResponseAsync(transport, requestId, params, params.getMethod());
            } else {
                callToolSynchronously(transport, requestId, params, params.getMethod());
            }
        } catch (IllegalAccessException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + params.getName()));
        } catch (IllegalArgumentException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
        }
    }

    @FFDCIgnore({ JSONRPCException.class, InvocationTargetException.class })
    private void callToolSynchronously(McpTransport transport,
                                       ExecutionRequestId requestId,
                                       McpToolCallParams params,
                                       Method method)
                    throws IllegalAccessException, IllegalArgumentException {
        CreationalContext<Object> cc = bm.createCreationalContext(null);

        if (requestId != null) {
            CancellationImpl cancellation = new CancellationImpl();
            cancellation.setRequestId(requestId);
            requestTracker.registerOngoingRequest(requestId, cancellation);
        }

        try {
            Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);

            Object[] arguments = params.getArguments(jsonb);
            addSpecialArguments(arguments, requestId, params.getMetadata());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Calling tool " + params.getMetadata().name(), arguments);
            }

            // Call the tool method synchronously
            Object result = method.invoke(bean, arguments);

            transport.sendResponse(evaluateToolResponse(result, params));

        } catch (JSONRPCException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (isBusinessException(t, params)) {
                transport.sendResponse(toErrorResponse(e.getCause()));
            } else {
                Tr.error(tc, "CWMCM0010E.internal.server.error.detailed",
                         params.getMetadata().name(),
                         e.getCause());
                transport.sendResponse(ToolResponse.error(Tr.formatMessage(tc, "CWMCM0011E.internal.server.error")));
            }
        } finally {
            cleanup(requestId, cc, params);
        }
    }

    @FFDCIgnore({ InvocationTargetException.class })
    private void callToolMethodAndSendResponseAsync(McpTransport transport,
                                                    ExecutionRequestId requestId,
                                                    McpToolCallParams params,
                                                    Method method)
                    throws IllegalAccessException, IllegalArgumentException {
        CreationalContext<Object> cc = bm.createCreationalContext(null);

        if (requestId != null) {
            CancellationImpl cancellation = new CancellationImpl();
            cancellation.setRequestId(requestId);
            requestTracker.registerOngoingRequest(requestId, cancellation);
        }

        try {
            Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);

            Object[] arguments = params.getArguments(jsonb);
            addSpecialArguments(arguments, requestId, params.getMetadata());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Calling tool " + params.getMetadata().name(), arguments);
            }
            CompletionStage<?> stage = ((CompletionStage<?>) method.invoke(bean, arguments));
            stage = stage.thenApply(result -> evaluateToolResponse(result, params))
                         .exceptionally(throwable -> {
                             Tr.error(tc,
                                      "CWMCM0010E.internal.server.error.detailed",
                                      params.getMetadata().name(),
                                      throwable.getCause());
                             return ToolResponse.error(Tr.formatMessage(tc, "CWMCM0011E.internal.server.error"));
                         });
            transport.sendResultAsync(stage)
                     .whenComplete((result, throwable) -> cleanup(requestId, cc, params));
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (isBusinessException(t, params)) {
                transport.sendResponse(toErrorResponse(e.getCause()));
            } else {
                Tr.error(tc, "CWMCM0010E.internal.server.error.detailed",
                         params.getMetadata().name(),
                         e.getCause());
                transport.sendResponse(ToolResponse.error(Tr.formatMessage(tc, "CWMCM0011E.internal.server.error")));
            }
            cleanup(requestId, cc, params);
        }
    }

    private void cleanup(ExecutionRequestId requestId, CreationalContext<Object> cc, McpToolCallParams params) {
        if (requestId != null && requestTracker.isOngoingRequest(requestId)) {
            requestTracker.deregisterOngoingRequest(requestId);
        }
        try {
            cc.release();
        } catch (Exception ex) {
            Tr.warning(tc, "CWMCM0012E.bean.release.fail", ex, params.getName());
        }
    }

    private Object evaluateToolResponse(Object result, McpToolCallParams params) {
        boolean includeStructuredContent = params.getMetadata().annotation().structuredContent();

        // Map method response to a ToolResponse
        if (result instanceof ToolResponse response) {
            return response;
        } else if (result instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(item -> item instanceof Content)) {
            @SuppressWarnings("unchecked")
            List<Content> contents = (List<Content>) list;
            return ToolResponse.success(contents);
        } else if (result instanceof Content content) {
            return ToolResponse.success(content);
        } else if (result instanceof String s) {
            return ToolResponse.success(s);
        } else if (includeStructuredContent) {
            return ToolResponse.structuredSuccess(jsonb.toJson(result), result);
        } else {
            return ToolResponse.success(Objects.toString(result));
        }
    }

    /**
     * @param t
     * @return
     */
    private boolean isBusinessException(Throwable t, McpToolCallParams params) {
        if (t instanceof ToolCallException) {
            return true;
        }

        if (params != null && params.getMetadata() != null) {
            for (Class<? extends Throwable> clazz : params.getMetadata().businessExceptions()) {
                if (clazz.isAssignableFrom(t.getClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Helper method for ToolResponse Error
    private ToolResponse toErrorResponse(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return ToolResponse.error(msg);
    }

    /**
     * Adds the values for any special arguments to {@code argumentsArray}
     *
     * @param argumentsArray the array of arguments for the tool method
     * @param requestId the ongoing request Id
     * @param toolMetadata the tool metadata
     */
    private void addSpecialArguments(Object[] argumentsArray,
                                     ExecutionRequestId requestId,
                                     ToolMetadata toolMetadata) {
        for (SpecialArgumentMetadata argMetadata : toolMetadata.specialArguments()) {
            switch (argMetadata.typeResolution().specialArgsType()) {
                case CANCELLATION -> {
                    CancellationImpl cancellation;
                    if (requestId != null) {
                        cancellation = (CancellationImpl) requestTracker.getOngoingRequestCancellation(requestId);
                    } else {
                        cancellation = new CancellationImpl();
                    }

                    // Always inject it into args
                    argumentsArray[argMetadata.index()] = cancellation;
                }
                default -> {

                }
            }
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
                response.add(new ToolDescription(tmd));
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
        McpRequestId mcpReqId = notificationParams.getRequestId();
        String sessionId = transport.getSessionId();
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
        String sessionId = transport.getSessionId();
        if (sessionId != null) {
            return new ExecutionRequestId(transport.getMcpRequest().id(),
                                          sessionId);
        } else {
            return null;
        }
    }
}

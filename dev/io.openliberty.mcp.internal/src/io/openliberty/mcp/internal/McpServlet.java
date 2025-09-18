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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
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
    private Jsonb jsonb;

    @Inject
    BeanManager bm;

    @Inject
    McpConnectionTracker connection;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jsonb = JsonbBuilder.create();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        McpTransport transport = new McpTransport(req, resp, jsonb);
        String accept = req.getHeader("Accept");
        // Return 405, with SSE-specific message if "text/event-stream" is requested.
        if (accept != null && HeaderValidation.acceptContains(accept, "text/event-stream")) {
            HttpResponseException e = new HttpResponseException(
                                                                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                                                "GET not supported yet. SSE not implemented.")
                                                                                                              .withHeader("Allow", "POST");
            transport.sendHttpException(e);
        } else {
            HttpResponseException e = new HttpResponseException(
                                                                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                                                "GET method not allowed.")
                                                                                          .withHeader("Allow", "POST");
            transport.sendHttpException(e);
        }
    }

    @Override
    @FFDCIgnore({ JSONRPCException.class, HttpResponseException.class })
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, JSONRPCException {
        McpTransport transport = new McpTransport(req, resp, jsonb);
        try {
            transport.init();
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

    @FFDCIgnore({ JSONRPCException.class, InvocationTargetException.class, IllegalAccessException.class, IllegalArgumentException.class })
    private void callTool(McpTransport transport) {
        ExecutionRequestId requestId = createOngoingRequestId(transport);
        McpToolCallParams params = transport.getParams(McpToolCallParams.class);

        if (params.getMetadata() == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Attempt to call non-existant tool: " + params.getName());
            }
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Method " + params.getName() + " not found"));
        }

        CreationalContext<Object> cc = bm.createCreationalContext(null);
        Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);
        try {
            Object[] arguments = params.getArguments(jsonb);
            addSpecialArguments(arguments, requestId, params.getMetadata());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Calling tool " + params.getMetadata().name(), arguments);
            }

            // Call the tool method
            Object result = params.getMethod().invoke(bean, arguments);

            boolean includeStructuredContent = params.getMetadata().annotation().structuredContent();

            // Map method response to a ToolResponse
            if (result instanceof ToolResponse response) {
                transport.sendResponse(response);
            } else if (result instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(item -> item instanceof Content)) {
                @SuppressWarnings("unchecked")
                List<Content> contents = (List<Content>) list;
                transport.sendResponse(ToolResponse.success(contents));
            } else if (result instanceof Content content) {
                transport.sendResponse(ToolResponse.success(content));
            } else if (result instanceof String s) {
                transport.sendResponse(ToolResponse.success(s));
            } else if (includeStructuredContent) {
                transport.sendResponse(ToolResponse.structuredSuccess(jsonb.toJson(result), result));
            } else {
                transport.sendResponse(ToolResponse.success(Objects.toString(result)));
            }
        } catch (JSONRPCException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (isBusinessException(t, params)) {
                transport.sendResponse(toErrorResponse(e.getCause()));
            } else {
                Tr.error(tc, "The {0} tool method threw an unexpected exception. The exception was {1}",
                         params.getMetadata().name(),
                         e.getCause());
                transport.sendResponse(ToolResponse.error("Internal server error"));
            }
        } catch (IllegalAccessException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + params.getName()));
        } catch (IllegalArgumentException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
        } finally {
            if (connection.isOngoingRequest(requestId)) {
                connection.deregisterOngoingRequest(requestId);
            }
            try {
                cc.release();
            } catch (Exception ex) {
                Tr.warning(tc, "Failed to release bean: " + ex);
            }
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
    private void addSpecialArguments(Object[] argumentsArray, ExecutionRequestId requestId, ToolMetadata toolMetadata) {
        for (SpecialArgumentMetadata argMetadata : toolMetadata.specialArguments()) {
            switch (argMetadata.typeResolution().specialArgsType()) {
                case CANCELLATION -> {
                    CancellationImpl cancellation = new CancellationImpl();
                    cancellation.setRequestId(requestId);
                    connection.registerOngoingRequest(requestId, cancellation);
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

        ServerCapabilities caps = ServerCapabilities.of(new Capabilities.Tools(false));

        // TODO: provide a way for the user to set server info
        ServerInfo info = new ServerInfo("test-server", "Test Server", "0.1");
        McpInitializeResult result = new McpInitializeResult(version, caps, info, null);
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
        McpRequestId mcpRedId = notificationParams.getRequestId();
        ExecutionRequestId requestId = new ExecutionRequestId(mcpRedId, transport.getRequestIpAddress());
        Optional<String> reason = Optional.ofNullable(notificationParams.getReason());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Cancellation requested for " + requestId);
        }

        Cancellation cancellation = connection.getOngoingRequestCancellation(requestId);
        if (cancellation != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Cancelling task");
            }
            ((CancellationImpl) cancellation).cancel(reason);
        }
        transport.sendEmptyResponse();
    }

    private ExecutionRequestId createOngoingRequestId(McpTransport transport) {
        return new ExecutionRequestId(transport.getMcpRequest().id(),
                                      transport.getRequestIpAddress());
    }
}

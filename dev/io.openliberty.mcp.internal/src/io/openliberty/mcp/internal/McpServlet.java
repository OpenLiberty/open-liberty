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
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.Capabilities.ServerCapabilities;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.internal.responses.McpInitializeResult;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;
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
            default -> throw new JSONRPCException(JSONRPCErrorCode.METHOD_NOT_FOUND, List.of(String.valueOf(method + " not found")));
        }

    }

    @FFDCIgnore({ JSONRPCException.class, InvocationTargetException.class, IllegalAccessException.class, IllegalArgumentException.class })
    private void callTool(McpTransport transport) {
        McpToolCallParams params = transport.getParams(McpToolCallParams.class);
        if (params.getMetadata() == null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Method " + params.getName() + " not found"));
        }
        CreationalContext<Object> cc = bm.createCreationalContext(null);
        Object bean = bm.getReference(params.getBean(), params.getBean().getBeanClass(), cc);
        try {
            Object result = params.getMethod().invoke(bean, params.getArguments(jsonb));
            boolean includeStructuredContent = params.getMetadata().annotation().structuredContent();
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
            transport.sendResponse(toErrorResponse(e.getCause()));
        } catch (IllegalAccessException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, List.of("Could not call " + params.getName()));
        } catch (IllegalArgumentException e) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Incorrect arguments in params"));
        } finally {
            try {
                cc.release();
            } catch (Exception ex) {
                Tr.warning(tc, "Failed to release bean: " + ex);
            }
        }
    }

    // Helper method for ToolResponse Error
    private ToolResponse toErrorResponse(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return ToolResponse.error(msg);
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
    private void initialize(McpTransport transport) throws IOException {
        McpInitializeParams params = transport.getParams(McpInitializeParams.class);
        // TODO validate protocol
        // TODO store client capabilities
        // TODO store client info

        ServerCapabilities caps = ServerCapabilities.of(new Capabilities.Tools(false));

        // TODO: provide a way for the user to set server info
        ServerInfo info = new ServerInfo("test-server", "Test Server", "0.1");
        McpInitializeResult result = new McpInitializeResult("2025-06-18", caps, info, null);
        transport.sendResponse(result);
    }

    private void initialized(McpTransport transport) {
        transport.sendEmptyResponse();
    }

    private void ping(McpTransport transport) {
        transport.sendResponse(new Object());
    }

}

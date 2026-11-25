/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import static io.openliberty.mcp.internal.fat.utils.JsonUtil.get;

import org.json.JSONObject;

/**
 * A JSON-RPC response to any MCP request, as returned by {@link McpClient#call(McpRequest)} and
 * {@link McpClient#call(String)}.
 *
 * <p>Wraps the raw JSON-RPC response together with the {@code id} and the protocol version of
 * the request it answers, so that {@link JsonRpcResponseMatcher} can validate the response
 * against its request. Use the matchers to assert on a response.
 *
 * <p>Subclasses, such as {@link ToolResponse}, add the getters for the parts of the result of one
 * kind of request.
 */
public class JsonRpcResponse {

    private final JSONObject json;
    private final Object requestId;
    private final String protocolVersion;

    JsonRpcResponse(JSONObject json, Object requestId, String protocolVersion) {
        this.json = json;
        this.requestId = requestId;
        this.protocolVersion = protocolVersion;
    }

    /** Returns the full JSON-RPC response. */
    public JSONObject getJson() {
        return json;
    }

    /** Returns the {@code id} of the request this is a response to, or {@link JSONObject#NULL} if the request had no usable id. */
    public Object getRequestId() {
        return requestId;
    }

    /** Returns the MCP protocol version used for the request this is a response to. */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /** Returns the {@code id} of the response. */
    public Object getId() {
        return json.opt("id");
    }

    /** Returns the JSON-RPC {@code result} object, or {@code null} if the response has none (e.g. a JSON-RPC error). */
    public JSONObject getResult() {
        return get(json, JSONObject.class, "result");
    }

    /** Returns the JSON-RPC {@code error} object, or {@code null} if the response is not a JSON-RPC error. */
    public JSONObject getError() {
        return get(json, JSONObject.class, "error");
    }

    /** Returns the JSON-RPC error {@code code}, or {@code null} if the response is not a JSON-RPC error or has no integer code. */
    public Integer getErrorCode() {
        return get(json, Integer.class, "error", "code");
    }

    /** Returns the JSON-RPC error {@code message}, or {@code null} if there is none. */
    public String getErrorMessage() {
        return get(json, String.class, "error", "message");
    }

    /** Returns the JSON-RPC error {@code data} (a {@code JSONArray}, a string, ...), or {@code null} if there is none. */
    public Object getErrorData() {
        return get(json, Object.class, "error", "data");
    }

    @Override
    public String toString() {
        return json.toString();
    }
}

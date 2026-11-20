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
import static io.openliberty.mcp.internal.fat.utils.JsonUtil.toListOf;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The response to a {@code tools/call} request, as returned by {@link McpClient#callTool(String)}.
 *
 * <p>Adds the parts of a tool result that tests commonly assert on to the generic
 * {@link JsonRpcResponse}. Use {@link ToolResponseMatcher} to assert on a tool result and
 * {@link JsonRpcResponseMatcher} to assert on a JSON-RPC error.
 */
public class ToolResponse extends JsonRpcResponse {

    ToolResponse(JSONObject json, Object requestId, String protocolVersion) {
        super(json, requestId, protocolVersion);
    }

    /** Returns the {@code isError} flag of the tool result, or {@code false} if there is no result. */
    public boolean isError() {
        Boolean isError = get(getJson(), Boolean.class, "result", "isError");
        return isError != null && isError;
    }

    /** Returns the tool result's {@code structuredContent} object, or {@code null} if there is none. */
    public JSONObject getStructuredContent() {
        return get(getJson(), JSONObject.class, "result", "structuredContent");
    }

    /** Returns the tool result's {@code _meta} object, or {@code null} if there is none. */
    public JSONObject getMeta() {
        return get(getJson(), JSONObject.class, "result", "_meta");
    }

    /** Returns the items of the tool result's {@code content} array, or an empty list if there are none. */
    public List<JSONObject> getContent() {
        JSONArray content = get(getJson(), JSONArray.class, "result", "content");
        return toListOf(content, JSONObject.class);
    }
}

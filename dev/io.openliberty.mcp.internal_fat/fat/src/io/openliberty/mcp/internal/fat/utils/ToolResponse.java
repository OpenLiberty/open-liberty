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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The response to a {@code tools/call} request, as returned by {@link McpClient#callTool(String)}.
 *
 * <p>Wraps the raw JSON-RPC response and exposes the parts of the tool result that tests
 * commonly assert on. Use {@link ToolResponseMatcher} to assert on a response.
 */
public class ToolResponse {

    private final JSONObject json;

    ToolResponse(JSONObject json) {
        this.json = json;
    }

    /** Returns the full JSON-RPC response. */
    public JSONObject getJson() {
        return json;
    }

    /** Returns the JSON-RPC {@code result} object, or {@code null} if the response has none (e.g. a JSON-RPC error). */
    public JSONObject getResult() {
        return json.optJSONObject("result");
    }

    /** Returns the JSON-RPC {@code error} object, or {@code null} if the response is not a JSON-RPC error. */
    public JSONObject getError() {
        return json.optJSONObject("error");
    }

    /** Returns the {@code isError} flag of the tool result, or {@code false} if there is no result. */
    public boolean isError() {
        JSONObject result = getResult();
        return result != null && result.optBoolean("isError", false);
    }

    /** Returns the items of the tool result's {@code content} array, or an empty list if there are none. */
    public List<JSONObject> getContent() {
        List<JSONObject> items = new ArrayList<>();
        JSONObject result = getResult();
        JSONArray content = result == null ? null : result.optJSONArray("content");
        if (content != null) {
            for (int i = 0; i < content.length(); i++) {
                items.add(content.optJSONObject(i));
            }
        }
        return items;
    }

    @Override
    public String toString() {
        return json.toString();
    }
}

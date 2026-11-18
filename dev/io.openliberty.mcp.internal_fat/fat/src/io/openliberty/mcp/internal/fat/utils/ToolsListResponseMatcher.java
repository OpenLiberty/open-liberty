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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Hamcrest matcher for asserting on the result of a {@code tools/list} request without spelling
 * out the whole JSON-RPC response. It also validates that the JSON-RPC envelope of the response
 * is valid, including that its {@code id} is the id of the request.
 *
 * <pre>{@code
 * McpResponse response = client.send(client.request("tools/list", null));
 * assertThat(response, toolsList().withTools(new JSONArray("""
 *                 [
 *                   {"name": "echo", "inputSchema": {"type": "object"}}
 *                 ]
 *                 """)));
 * }</pre>
 *
 * <p>To assert on a JSON-RPC error use {@link JsonRpcResponseMatcher#jsonRpcResponse()}.
 */
public class ToolsListResponseMatcher extends TypeSafeDiagnosingMatcher<McpResponse> {

    private JSONArray expectedTools;

    private ToolsListResponseMatcher() {}

    /** Returns a matcher requiring a {@code tools/list} result, with no further conditions. */
    public static ToolsListResponseMatcher toolsList() {
        return new ToolsListResponseMatcher();
    }

    /**
     * Requires the {@code result} of the response to hold exactly the given tools, and nothing
     * else: in particular there must be no {@code nextCursor}, so all the tools are on one page.
     *
     * <p>The tools, and the items of any array inside a tool, may be in any order. Each tool is
     * compared as a whole JSON object and is strict about extra fields: a tool of the response may
     * not have a field that the expected tool does not have.
     */
    public ToolsListResponseMatcher withTools(JSONArray expectedTools) {
        this.expectedTools = expectedTools;
        return this;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid tools/list response");
        if (expectedTools != null) {
            description.appendText(" with exactly the tools ").appendText(expectedTools.toString());
        }
    }

    @Override
    protected boolean matchesSafely(McpResponse response, Description mismatch) {
        String envelopeProblem = JsonRpcEnvelope.validate(response);
        if (envelopeProblem != null) {
            mismatch.appendText("had an invalid JSON-RPC envelope, it ").appendText(envelopeProblem)
                    .appendText(": ").appendText(response.toString());
            return false;
        }
        if (response.getResult() == null) {
            mismatch.appendText("was a JSON-RPC error rather than a tools/list result: ").appendText(response.toString());
            return false;
        }
        if (expectedTools != null && !matchesTools(response, mismatch)) {
            return false;
        }
        return true;
    }

    private boolean matchesTools(McpResponse response, Description mismatch) {
        try {
            JSONObject expectedResult = new JSONObject().put("tools", expectedTools);
            JSONCompareResult comparison = JSONCompare.compareJSON(expectedResult.toString(), response.getResult().toString(), JSONCompareMode.NON_EXTENSIBLE);
            if (comparison.failed()) {
                mismatch.appendText("result differed: ").appendText(comparison.getMessage());
                return false;
            }
        } catch (Exception e) {
            mismatch.appendText("result could not be compared: ").appendText(e.toString());
            return false;
        }
        return true;
    }
}

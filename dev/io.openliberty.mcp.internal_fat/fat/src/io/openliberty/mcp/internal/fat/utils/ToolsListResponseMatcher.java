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

import java.util.Iterator;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Hamcrest matcher for asserting on the result of a {@code tools/list} request without spelling
 * out the whole JSON-RPC response. It also validates that the JSON-RPC envelope of the response
 * is valid, including that its {@code id} is the id of the request.
 *
 * <p>Start with {@link #toolsList()} and chain the conditions the response must meet:
 *
 * <pre>{@code
 * JsonRpcResponse response = client.call(client.request("tools/list", null));
 * assertThat(response, toolsList().withTools(new JSONArray("""
 *                 [
 *                   {"name": "echo", "inputSchema": {"type": "object"}}
 *                 ]
 *                 """)));
 *
 * // Only check the fields that the test mentions, and the order of the tools
 * assertThat(response, toolsList().withTools(new JSONArray("""
 *                 [
 *                   {"name": "echo"}
 *                 ]
 *                 """), JSONCompareMode.STRICT_ORDER));
 * }</pre>
 *
 * <p>To assert on a JSON-RPC error use {@link JsonRpcResponseMatcher#jsonRpcResponse()}. To assert
 * on the result of a tool call use {@link ToolResponseMatcher#toolResponse()}.
 */
public class ToolsListResponseMatcher extends TypeSafeDiagnosingMatcher<JsonRpcResponse> {

    private JsonExpectation expectedTools;

    private ToolsListResponseMatcher() {}

    /** Returns a matcher requiring a {@code tools/list} result, with no further conditions. */
    public static ToolsListResponseMatcher toolsList() {
        return new ToolsListResponseMatcher();
    }

    /**
     * Requires the {@code result} of the response to hold exactly the given tools, and nothing
     * else: in particular there must be no {@code nextCursor}, so all the tools are on one page.
     *
     * <p>The tools are compared with {@link JSONCompareMode#NON_EXTENSIBLE}: the tools, and the
     * items of any array inside a tool, may be in any order, and each tool is compared as a whole
     * JSON object that may not have a field that the expected tool does not have. Use
     * {@link #withTools(JSONArray, JSONCompareMode)} to compare differently.
     */
    public ToolsListResponseMatcher withTools(JSONArray expectedTools) {
        return withTools(expectedTools, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Like {@link #withTools(JSONArray)}, but compares the {@code tools} array with the given
     * mode. The mode controls two things: whether the tools must be in the order given, and whether
     * a tool of the response may have fields that the expected tool does not mention.
     * <ul>
     * <li>{@link JSONCompareMode#STRICT}: in order, no extra fields</li>
     * <li>{@link JSONCompareMode#NON_EXTENSIBLE}: any order, no extra fields</li>
     * <li>{@link JSONCompareMode#STRICT_ORDER}: in order, extra fields allowed</li>
     * <li>{@link JSONCompareMode#LENIENT}: any order, extra fields allowed</li>
     * </ul>
     *
     * <p>Whatever the mode, the response must have exactly as many tools as expected, and the
     * {@code result} may have no field other than {@code tools}.
     */
    public ToolsListResponseMatcher withTools(JSONArray expectedTools, JSONCompareMode compareMode) {
        this.expectedTools = new JsonExpectation(expectedTools, compareMode);
        return this;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid tools/list response");
        if (expectedTools != null) {
            expectedTools.describeTo(description.appendText(" with "), "tools");
        }
    }

    @Override
    protected boolean matchesSafely(JsonRpcResponse response, Description mismatch) {
        String envelopeProblem = JsonRpcEnvelopeValidation.validate(response);
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

    /** Checks the {@code tools} array against the expectation, and that the result has nothing but the tools. */
    private boolean matchesTools(JsonRpcResponse response, Description mismatch) {
        JSONArray actual = JsonUtil.get(response.getJson(), JSONArray.class, "result", "tools");
        if (actual == null) {
            mismatch.appendText("had no tools: ").appendText(response.toString());
            return false;
        }
        String difference = expectedTools.difference(actual);
        if (difference != null) {
            mismatch.appendText("tools ").appendText(difference);
            return false;
        }
        Iterator<?> keys = response.getResult().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (!"tools".equals(key)) {
                mismatch.appendText("result had the unexpected field ").appendValue(key).appendText(": ").appendText(response.toString());
                return false;
            }
        }
        return true;
    }
}
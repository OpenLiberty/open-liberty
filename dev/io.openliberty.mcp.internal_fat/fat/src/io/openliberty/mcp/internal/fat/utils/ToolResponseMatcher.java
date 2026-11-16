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
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Hamcrest matcher for asserting on a {@link ToolResponse} without spelling out the whole
 * JSON-RPC response.
 *
 * <p>Start with {@link #toolResponse()} and chain the conditions the response must meet:
 *
 * <pre>{@code
 * ToolResponse response = client.callTool("myTool");
 * assertThat(response, toolResponse().withTextContent("expected text"));
 * assertThat(response, toolResponse().withErrorText("expected error text"));
 * assertThat(response, toolResponse().withTextContent(json.toString()).withStructureContent(json));
 * assertThat(response, toolResponse().withJsonRpcError(-32602, "Invalid params", "Method foo not found"));
 * }</pre>
 */
public class ToolResponseMatcher extends TypeSafeDiagnosingMatcher<ToolResponse> {

    private Boolean expectedIsError;
    private List<String> expectedTexts;
    private JSONObject expectedStructureContent;
    private Integer expectedRpcErrorCode;
    private String expectedRpcErrorMessage;
    private List<String> expectedRpcErrorData;

    private ToolResponseMatcher() {}

    /** Returns a matcher with no conditions other than that the response has a tool result. */
    public static ToolResponseMatcher toolResponse() {
        return new ToolResponseMatcher();
    }

    /**
     * Requires a successful tool response ({@code isError} is not {@code true}) whose content is
     * exactly one {@code text} item with exactly the given text.
     */
    public ToolResponseMatcher withTextContent(String expectedText) {
        return withTextContents(expectedText);
    }

    /**
     * Requires a successful tool response ({@code isError} is not {@code true}) whose content is
     * exactly the given {@code text} items, in order.
     */
    public ToolResponseMatcher withTextContents(String... expectedTexts) {
        this.expectedIsError = false;
        this.expectedTexts = Arrays.asList(expectedTexts);
        return this;
    }

    /**
     * Requires a tool error response ({@code isError} is {@code true}) whose content is exactly
     * one {@code text} item with exactly the given text.
     */
    public ToolResponseMatcher withErrorText(String expectedText) {
        this.expectedIsError = true;
        this.expectedTexts = List.of(expectedText);
        return this;
    }

    /** Requires the tool result's {@code structuredContent} to equal the given object (strict comparison). */
    public ToolResponseMatcher withStructureContent(JSONObject expectedStructureContent) {
        this.expectedStructureContent = expectedStructureContent;
        return this;
    }

    /**
     * Requires the response to be a JSON-RPC error (rather than a tool result) with the given
     * {@code code}, {@code message} and {@code data} items.
     */
    public ToolResponseMatcher withJsonRpcError(int code, String message, String... data) {
        this.expectedRpcErrorCode = code;
        this.expectedRpcErrorMessage = message;
        this.expectedRpcErrorData = Arrays.asList(data);
        return this;
    }

    @Override
    public void describeTo(Description description) {
        if (expectedRpcErrorCode != null) {
            description.appendText("a JSON-RPC error with code ").appendValue(expectedRpcErrorCode)
                       .appendText(", message ").appendValue(expectedRpcErrorMessage)
                       .appendText(" and data ").appendValue(expectedRpcErrorData);
            return;
        }
        description.appendText("a tool response");
        if (expectedIsError != null) {
            description.appendText(expectedIsError ? " with isError true" : " with isError not true");
        }
        if (expectedTexts != null) {
            description.appendText(" and text content of ").appendValue(expectedTexts);
        }
        if (expectedStructureContent != null) {
            description.appendText(" and structured content of ").appendText(expectedStructureContent.toString());
        }
    }

    @Override
    protected boolean matchesSafely(ToolResponse response, Description mismatch) {
        if (expectedRpcErrorCode != null) {
            return matchesJsonRpcError(response, mismatch);
        }
        if (response.getResult() == null) {
            mismatch.appendText("had no result: ").appendText(response.toString());
            return false;
        }
        if (expectedIsError != null && response.isError() != expectedIsError) {
            mismatch.appendText("isError was ").appendValue(response.isError()).appendText(": ").appendText(response.toString());
            return false;
        }
        if (expectedTexts != null && !matchesTexts(response, mismatch)) {
            return false;
        }
        if (expectedStructureContent != null && !matchesStructureContent(response, mismatch)) {
            return false;
        }
        return true;
    }

    private boolean matchesTexts(ToolResponse response, Description mismatch) {
        List<JSONObject> content = response.getContent();
        if (content.size() != expectedTexts.size()) {
            mismatch.appendText("had ").appendValue(content.size()).appendText(" content items: ").appendText(response.toString());
            return false;
        }
        for (int i = 0; i < content.size(); i++) {
            JSONObject item = content.get(i);
            if (item == null || !"text".equals(item.optString("type"))) {
                mismatch.appendText("content item ").appendValue(i).appendText(" was not of type text: ").appendText(response.toString());
                return false;
            }
            String actualText = item.optString("text", null);
            if (!expectedTexts.get(i).equals(actualText)) {
                mismatch.appendText("text content item ").appendValue(i).appendText(" was ").appendValue(actualText);
                return false;
            }
        }
        return true;
    }

    private boolean matchesStructureContent(ToolResponse response, Description mismatch) {
        JSONObject actual = response.getStructuredContent();
        if (actual == null) {
            mismatch.appendText("had no structuredContent: ").appendText(response.toString());
            return false;
        }
        try {
            JSONCompareResult comparison = JSONCompare.compareJSON(expectedStructureContent.toString(), actual.toString(), JSONCompareMode.STRICT);
            if (comparison.failed()) {
                mismatch.appendText("structuredContent differed: ").appendText(comparison.getMessage());
                return false;
            }
        } catch (Exception e) {
            mismatch.appendText("structuredContent could not be compared: ").appendText(e.toString());
            return false;
        }
        return true;
    }

    private boolean matchesJsonRpcError(ToolResponse response, Description mismatch) {
        JSONObject error = response.getError();
        if (error == null) {
            mismatch.appendText("was not a JSON-RPC error: ").appendText(response.toString());
            return false;
        }
        if (error.optInt("code") != expectedRpcErrorCode || !expectedRpcErrorMessage.equals(error.optString("message", null))) {
            mismatch.appendText("error had code ").appendValue(error.opt("code")).appendText(" and message ").appendValue(error.opt("message"));
            return false;
        }
        JSONArray data = error.optJSONArray("data");
        List<Object> actualData = new ArrayList<>();
        for (int i = 0; data != null && i < data.length(); i++) {
            actualData.add(data.opt(i));
        }
        if (data == null || !expectedRpcErrorData.equals(actualData)) {
            mismatch.appendText("error data was ").appendValue(error.opt("data"));
            return false;
        }
        return true;
    }
}

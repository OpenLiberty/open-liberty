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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Hamcrest matcher for asserting on the tool result of a {@link ToolResponse} without spelling
 * out the whole JSON-RPC response. It also validates that the JSON-RPC envelope of the response
 * is valid.
 *
 * <p>Start with {@link #toolResponse()} and chain the conditions the response must meet:
 *
 * <pre>{@code
 * ToolResponse response = client.callTool("myTool");
 * assertThat(response, toolResponse().withTextContent("expected text"));
 * assertThat(response, toolResponse().withError("expected error text"));
 * assertThat(response, toolResponse().withTextContent(json.toString()).withStructureContent(json));
 * }</pre>
 *
 * <p>To assert on a JSON-RPC error use {@link JsonRpcResponseMatcher#jsonRpcResponse()}.
 */
public class ToolResponseMatcher extends TypeSafeDiagnosingMatcher<ToolResponse> {

    private Boolean expectedIsError;
    private List<String> expectedTexts;
    private JSONObject expectedStructureContent;
    private boolean expectNoStructuredContent;

    private ToolResponseMatcher() {}

    /** Returns a matcher with no conditions other than that the response has a tool result. */
    public static ToolResponseMatcher toolResponse() {
        return new ToolResponseMatcher();
    }

    /**
     * Requires a successful tool response ({@code isError} is {@code false}) whose content is
     * exactly one {@code text} item with exactly the given text.
     */
    public ToolResponseMatcher withTextContent(String expectedText) {
        return withTextContents(expectedText);
    }

    /**
     * Requires a successful tool response ({@code isError} is {@code false}) whose content is
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
     *
     * <p>Like {@link #withTextContent(String)}, this requires the {@code isError} field to be
     * present in the result.
     */
    public ToolResponseMatcher withError(String expectedText) {
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
     * Requires the tool result to have no {@code structuredContent} field. Use this for tools that
     * return plain text, so that the test fails if the server starts adding structured content.
     */
    public ToolResponseMatcher withNoStructuredContent() {
        this.expectNoStructuredContent = true;
        return this;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid tool response");
        String joiner = " with ";
        if (expectedIsError != null) {
            description.appendText(joiner).appendText(expectedIsError ? "isError true" : "isError false");
            joiner = " and ";
        }
        if (expectedTexts != null) {
            description.appendText(joiner).appendText("text content of ").appendValue(expectedTexts);
            joiner = " and ";
        }
        if (expectedStructureContent != null) {
            description.appendText(joiner).appendText("structured content of ").appendText(expectedStructureContent.toString());
            joiner = " and ";
        }
        if (expectNoStructuredContent) {
            description.appendText(joiner).appendText("no structured content");
        }
    }

    @Override
    protected boolean matchesSafely(ToolResponse response, Description mismatch) {
        String envelopeProblem = JsonRpcEnvelope.validate(response);
        if (envelopeProblem != null) {
            mismatch.appendText("had an invalid JSON-RPC envelope, it ").appendText(envelopeProblem)
                    .appendText(": ").appendText(response.toString());
            return false;
        }
        if (response.getResult() == null) {
            mismatch.appendText("was a JSON-RPC error rather than a tool result: ").appendText(response.toString());
            return false;
        }
        if (expectedIsError != null && !response.getResult().has("isError")) {
            mismatch.appendText("had no isError field: ").appendText(response.toString());
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
        if (expectNoStructuredContent && response.getResult().has("structuredContent")) {
            mismatch.appendText("had structuredContent: ").appendText(response.toString());
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
}
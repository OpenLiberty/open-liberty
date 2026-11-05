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
import java.util.Iterator;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
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
 * assertThat(response, toolResponse().withTextContent(json.toString()).withStructuredContent(json));
 *
 * // Content that is not plain text (image, audio, annotations, resource links, ...)
 * assertThat(response, toolResponse().withContent(new JSONObject("""
 *                 {"type": "image", "data": "aGkJ", "mimeType": "image/png"}
 *                 """)));
 *
 * // The custom entries of the _meta object of the tool result
 * assertThat(response, toolResponse().withTextContent("done").withCustomMeta(new JSONObject("{\"key\": \"value\"}")));
 *
 * // Only check the fields that the test mentions
 * assertThat(response, toolResponse().withContent(JSONCompareMode.STRICT_ORDER, new JSONObject("""
 *                 {"type": "text", "text": "Echo: hello"}
 *                 """)));
 * }</pre>
 *
 * <p>To assert on a JSON-RPC error use {@link JsonRpcResponseMatcher#jsonRpcResponse()}.
 */
public class ToolResponseMatcher extends TypeSafeDiagnosingMatcher<ToolResponse> {

    /**
     * The key prefix reserved by the MCP specification for protocol defined {@code _meta} entries.
     * Which of these the server includes depends on the protocol version, so they are ignored when
     * matching {@code _meta}.
     */
    private static final String RESERVED_META_PREFIX = "io.modelcontextprotocol";

    /** Expected JSON (a {@link JSONObject} or a {@link JSONArray}) together with how strictly to compare it. */
    private record JsonExpectation(Object json, JSONCompareMode compareMode) {}

    private Boolean expectedIsError;
    private List<String> expectedTexts;
    private JsonExpectation expectedContent;
    private JsonExpectation expectedStructuredContent;
    private boolean expectNoStructuredContent;
    private JsonExpectation expectedCustomMeta;

    private ToolResponseMatcher() {}

    /** Returns a matcher with no conditions other than that the response has a tool result. */
    public static ToolResponseMatcher toolResponse() {
        return new ToolResponseMatcher();
    }

    /**
     * Requires a successful tool response ({@code isError} is {@code false}) whose content is
     * exactly one {@code text} item with exactly the given text.
     *
     * <p>Only the {@code type} and {@code text} of the item are checked, so any other field of
     * the item, such as {@code annotations}, is ignored. Use {@link #withContent(JSONObject...)}
     * to check the whole item.
     */
    public ToolResponseMatcher withTextContent(String expectedText) {
        return withTextContents(expectedText);
    }

    /**
     * Requires a successful tool response ({@code isError} is {@code false}) whose content is
     * exactly the given {@code text} items, in order.
     *
     * <p>Only the {@code type} and {@code text} of each item are checked, so any other field of
     * an item, such as {@code annotations}, is ignored. Use {@link #withContent(JSONObject...)}
     * to check whole items.
     */
    public ToolResponseMatcher withTextContents(String... expectedTexts) {
        this.expectedIsError = false;
        this.expectedTexts = Arrays.asList(expectedTexts);
        return this;
    }

    /**
     * Requires a successful tool response ({@code isError} is {@code false}) whose {@code content}
     * array is exactly the given items, in order. Unlike {@link #withTextContents(String...)}, each
     * item is compared as a whole JSON object, so this can assert on content of any type
     * ({@code text}, {@code image}, {@code audio}, {@code resource_link}, {@code resource}) and on
     * fields such as {@code annotations} and {@code _meta}.
     *
     * <p>The comparison is {@link JSONCompareMode#STRICT}: an item of the response may not have a
     * field that the expected item does not have, and the number and the order of the items must
     * match. Use {@link #withContent(JSONCompareMode, JSONObject...)} to compare differently.
     *
     * <p>To assert on a tool error use {@link #withError(String)}.
     */
    public ToolResponseMatcher withContent(JSONObject... expectedItems) {
        return withContent(JSONCompareMode.STRICT, expectedItems);
    }

    /**
     * Like {@link #withContent(JSONObject...)}, but compares the {@code content} array with the
     * given mode. For example {@link JSONCompareMode#STRICT_ORDER} allows the response items to
     * have fields that the expected items do not mention, at any depth, but still requires the
     * items to match one to one and in order.
     */
    public ToolResponseMatcher withContent(JSONCompareMode compareMode, JSONObject... expectedItems) {
        JSONArray expected = new JSONArray();
        for (JSONObject item : expectedItems) {
            expected.put(item);
        }
        this.expectedIsError = false;
        this.expectedContent = new JsonExpectation(expected, compareMode);
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

    /**
     * Requires the tool result's {@code structuredContent} to equal the given object, comparing
     * with {@link JSONCompareMode#STRICT}. Use {@link #withStructuredContent(JSONObject, JSONCompareMode)}
     * to compare differently.
     */
    public ToolResponseMatcher withStructuredContent(JSONObject expectedStructuredContent) {
        return withStructuredContent(expectedStructuredContent, JSONCompareMode.STRICT);
    }

    /**
     * Requires the tool result's {@code structuredContent} to match the given object, comparing
     * with the given mode.
     */
    public ToolResponseMatcher withStructuredContent(JSONObject expectedStructuredContent, JSONCompareMode compareMode) {
        this.expectedStructuredContent = new JsonExpectation(expectedStructuredContent, compareMode);
        return this;
    }

    /**
     * Requires the custom entries of the {@code _meta} object of the tool result (not of a content
     * item) to equal the given object, comparing with {@link JSONCompareMode#STRICT}. Use
     * {@link #withCustomMeta(JSONObject, JSONCompareMode)} to compare differently.
     *
     * <p>Entries whose key starts with {@code io.modelcontextprotocol} are reserved by the MCP
     * specification and depend on the protocol version, so they are ignored: the expected object
     * should only contain the custom entries the tool is expected to return. The response must
     * have a {@code _meta} object, even if the expected object is empty.
     */
    public ToolResponseMatcher withCustomMeta(JSONObject expectedCustomMeta) {
        return withCustomMeta(expectedCustomMeta, JSONCompareMode.STRICT);
    }

    /**
     * Like {@link #withCustomMeta(JSONObject)}, but compares the custom entries with the given mode.
     */
    public ToolResponseMatcher withCustomMeta(JSONObject expectedCustomMeta, JSONCompareMode compareMode) {
        this.expectedCustomMeta = new JsonExpectation(expectedCustomMeta, compareMode);
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
        if (expectedContent != null) {
            describeExpectation(description.appendText(joiner), "content", expectedContent);
            joiner = " and ";
        }
        if (expectedStructuredContent != null) {
            describeExpectation(description.appendText(joiner), "structured content", expectedStructuredContent);
            joiner = " and ";
        }
        if (expectedCustomMeta != null) {
            describeExpectation(description.appendText(joiner), "custom _meta", expectedCustomMeta);
            joiner = " and ";
        }
        if (expectNoStructuredContent) {
            description.appendText(joiner).appendText("no structured content");
        }
    }

    private static void describeExpectation(Description description, String name, JsonExpectation expectation) {
        description.appendText(name).appendText(" of ").appendText(expectation.json().toString())
                   .appendText(" (").appendText(expectation.compareMode().name()).appendText(")");
    }

    @Override
    protected boolean matchesSafely(ToolResponse response, Description mismatch) {
        String envelopeProblem = JsonRpcEnvelopeValidation.validate(response);
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
        if (expectedContent != null && !matchesContent(response, mismatch)) {
            return false;
        }
        if (expectedStructuredContent != null && !matchesStructuredContent(response, mismatch)) {
            return false;
        }
        if (expectedCustomMeta != null && !matchesCustomMeta(response, mismatch)) {
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

    private boolean matchesContent(ToolResponse response, Description mismatch) {
        JSONArray actual = JsonUtil.get(response.getJson(), JSONArray.class, "result", "content");
        return matchesJson("content", expectedContent, actual, response, mismatch);
    }

    private boolean matchesStructuredContent(ToolResponse response, Description mismatch) {
        return matchesJson("structuredContent", expectedStructuredContent, response.getStructuredContent(), response, mismatch);
    }

    private boolean matchesCustomMeta(ToolResponse response, Description mismatch) {
        JSONObject meta = response.getMeta();
        JSONObject actual = null;
        if (meta != null) {
            try {
                actual = new JSONObject();
                Iterator<?> keys = meta.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (!key.startsWith(RESERVED_META_PREFIX)) {
                        actual.put(key, meta.opt(key));
                    }
                }
            } catch (Exception e) {
                mismatch.appendText("_meta could not be read: ").appendText(e.toString());
                return false;
            }
        }
        return matchesJson("_meta", expectedCustomMeta, actual, response, mismatch);
    }

    private static boolean matchesJson(String name, JsonExpectation expected, Object actual, ToolResponse response, Description mismatch) {
        if (actual == null) {
            mismatch.appendText("had no ").appendText(name).appendText(": ").appendText(response.toString());
            return false;
        }
        try {
            JSONCompareResult comparison = JSONCompare.compareJSON(expected.json().toString(), actual.toString(), expected.compareMode());
            if (comparison.failed()) {
                mismatch.appendText(name).appendText(" differed: ").appendText(comparison.getMessage());
                return false;
            }
        } catch (Exception e) {
            mismatch.appendText(name).appendText(" could not be compared: ").appendText(e.toString());
            return false;
        }
        return true;
    }
}
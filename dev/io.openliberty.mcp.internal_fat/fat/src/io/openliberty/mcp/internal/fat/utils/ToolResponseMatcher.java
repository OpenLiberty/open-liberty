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
 * assertThat(response, toolResponse().withTextContent(json.toString()).withStructureContent(json));
 *
 * // Content that is not plain text (i.e. image, audio, annotations, resource links)
 * assertThat(response, toolResponse().withContent(new JSONObject("""
 *                 {"type": "image", "data": "aGkJ", "mimeType": "image/png"}
 *                 """)));
 *
 * // The _meta object of the tool result
 * assertThat(response, toolResponse().withTextContent("done").withMeta(new JSONObject("{\"key\": \"value\"}")));
 *
 * // Only check the fields that the test mentions
 * assertThat(response, toolResponse().withContent(new JSONObject("""
 *                 {"type": "text", "text": "Echo: hello"}
 *                 """)).lenient());
 * }</pre>
 *
 * <p>To assert on a JSON-RPC error use {@link JsonRpcResponseMatcher#jsonRpcResponse()}.
 */
public class ToolResponseMatcher extends TypeSafeDiagnosingMatcher<ToolResponse> {

    private Boolean expectedIsError;
    private List<String> expectedTexts;
    private List<JSONObject> expectedContent;
    private JSONObject expectedStructureContent;
    private boolean expectNoStructuredContent;
    private JSONObject expectedMeta;
    private boolean lenient;

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
     * <p>The comparison is strict: an item of the response may not have a field that the expected
     * item does not have. Call {@link #lenient()} to allow that. The number and the order of the
     * items must always match.
     *
     * <p>To assert on a tool error use {@link #withError(String)}.
     */
    public ToolResponseMatcher withContent(JSONObject... expectedItems) {
        this.expectedIsError = false;
        this.expectedContent = Arrays.asList(expectedItems);
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
     * Requires the tool result's {@code structuredContent} to equal the given object. The
     * comparison is strict unless {@link #lenient()} is called.
     */
    public ToolResponseMatcher withStructureContent(JSONObject expectedStructureContent) {
        this.expectedStructureContent = expectedStructureContent;
        return this;
    }

    /**
     * Requires the {@code _meta} object of the tool result (not of a content item) to equal the
     * given object. The comparison is strict unless {@link #lenient()} is called.
     */
    public ToolResponseMatcher withMeta(JSONObject expectedMeta) {
        this.expectedMeta = expectedMeta;
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

    /**
     * Makes the comparison of {@link #withContent(JSONObject...)},
     * {@link #withStructureContent(JSONObject)} and {@link #withMeta(JSONObject)} lenient, so that a test only has to spell out the
     * fields it is interested in: the response may contain fields that the expected JSON does not
     * mention, at any depth.
     *
     * <p>Arrays are still compared in order and must have the same number of items, so the
     * content items must still match one to one. The checks made by
     * {@link #withTextContent(String)}, {@link #withTextContents(String...)} and
     * {@link #withError(String)} are not affected, as they only look at the type and text of the
     * items anyway.
     */
    public ToolResponseMatcher lenient() {
        this.lenient = true;
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
            description.appendText(joiner).appendText(lenient ? "content containing " : "content of ").appendText(expectedContent.toString());
            joiner = " and ";
        }
        if (expectedStructureContent != null) {
            description.appendText(joiner).appendText(lenient ? "structured content containing " : "structured content of ")
                       .appendText(expectedStructureContent.toString());
            joiner = " and ";
        }
        if (expectedMeta != null) {
            description.appendText(joiner).appendText(lenient ? "_meta containing " : "_meta of ").appendText(expectedMeta.toString());
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
        if (expectedContent != null && !matchesContent(response, mismatch)) {
            return false;
        }
        if (expectedStructureContent != null && !matchesStructureContent(response, mismatch)) {
            return false;
        }
        if (expectedMeta != null && !matchesMeta(response, mismatch)) {
            return false;
        }
        if (expectNoStructuredContent && response.getResult().has("structuredContent")) {
            mismatch.appendText("had structuredContent: ").appendText(response.toString());
            return false;
        }
        return true;
    }

    /** The mode used to compare JSON: strict, or lenient about extra fields but not about array order. */
    private JSONCompareMode compareMode() {
        return lenient ? JSONCompareMode.STRICT_ORDER : JSONCompareMode.STRICT;
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
        JSONArray actual = response.getResult().optJSONArray("content");
        if (actual == null) {
            mismatch.appendText("had no content array: ").appendText(response.toString());
            return false;
        }
        JSONArray expected = new JSONArray();
        for (JSONObject item : expectedContent) {
            expected.put(item);
        }
        try {
            JSONCompareResult comparison = JSONCompare.compareJSON(expected.toString(), actual.toString(), compareMode());
            if (comparison.failed()) {
                mismatch.appendText("content differed: ").appendText(comparison.getMessage());
                return false;
            }
        } catch (Exception e) {
            mismatch.appendText("content could not be compared: ").appendText(e.toString());
            return false;
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
            JSONCompareResult comparison = JSONCompare.compareJSON(expectedStructureContent.toString(), actual.toString(), compareMode());
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

    private boolean matchesMeta(ToolResponse response, Description mismatch) {
        JSONObject actual = response.getResult().optJSONObject("_meta");
        if (actual == null) {
            mismatch.appendText("had no _meta: ").appendText(response.toString());
            return false;
        }
        try {
            JSONCompareResult comparison = JSONCompare.compareJSON(expectedMeta.toString(), actual.toString(), compareMode());
            if (comparison.failed()) {
                mismatch.appendText("_meta differed: ").appendText(comparison.getMessage());
                return false;
            }
        } catch (Exception e) {
            mismatch.appendText("_meta could not be compared: ").appendText(e.toString());
            return false;
        }
        return true;
    }
}
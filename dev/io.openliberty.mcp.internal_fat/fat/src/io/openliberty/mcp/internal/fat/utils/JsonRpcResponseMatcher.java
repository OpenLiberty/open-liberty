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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;

/**
 * Hamcrest matcher for asserting that a {@link JsonRpcResponse} to any request is a JSON-RPC error.
 * It also validates that the JSON-RPC envelope of the response is valid.
 *
 * <p>Start with {@link #jsonRpcResponse()} and chain the conditions the error must meet:
 *
 * <pre>{@code
 * assertThat(response, jsonRpcResponse().withErrorCode(PARSE_ERROR));
 * assertThat(response, jsonRpcResponse().withErrorCode(INVALID_PARAMS)
 *                                       .withErrorDataItems("Method foo not found"));
 * assertThat(response, jsonRpcResponse().withErrorCode(INVALID_PARAMS)
 *                                       .withErrorData("CWMCM0022E: The cursor value is invalid."));
 * assertThat(response, jsonRpcResponse().withErrorCode(PARSE_ERROR)
 *                                       .withErrorData(hasItem(containsString("Invalid token"))));
 * }</pre>
 *
 * <p>To assert on the result of a tool call use {@link ToolResponseMatcher#toolResponse()}.
 */
public class JsonRpcResponseMatcher extends TypeSafeDiagnosingMatcher<JsonRpcResponse> {

    private Integer errorCode;
    private String errorMessage;
    private Matcher<?> errorDataMatcher;

    private JsonRpcResponseMatcher() {}

    /** Returns a matcher requiring a JSON-RPC error with no further conditions. */
    public static JsonRpcResponseMatcher jsonRpcResponse() {
        return new JsonRpcResponseMatcher();
    }

    /** Requires the error's {@code code} to equal the given code. See {@link JsonRpcErrorCode}. */
    public JsonRpcResponseMatcher withErrorCode(int code) {
        this.errorCode = code;
        return this;
    }

    /** Requires the error's {@code message} to equal the given message. */
    public JsonRpcResponseMatcher withErrorMessage(String message) {
        this.errorMessage = message;
        return this;
    }

    /** Requires the error's {@code data} to be a string equal to the given text. */
    public JsonRpcResponseMatcher withErrorData(String data) {
        return withErrorData(equalTo(data));
    }

    /**
     * Requires the error's {@code data} to be an array of exactly the given strings, in order.
     *
     * <p>The array items are compared as strings; use {@link #withErrorData(Matcher)} for
     * anything else.
     */
    public JsonRpcResponseMatcher withErrorDataItems(String... items) {
        return withErrorData(contains(items));
    }

    /**
     * Requires the error's {@code data} to satisfy the given matcher. A JSON array is presented
     * to the matcher as a {@code List}, any other value as it is parsed (a {@code String}, ...).
     */
    public JsonRpcResponseMatcher withErrorData(Matcher<?> dataMatcher) {
        this.errorDataMatcher = dataMatcher;
        return this;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid JSON-RPC error response");
        String joiner = " with ";
        if (errorCode != null) {
            description.appendText(joiner).appendText("code ").appendValue(errorCode);
            joiner = " and ";
        }
        if (errorMessage != null) {
            description.appendText(joiner).appendText("message ").appendValue(errorMessage);
            joiner = " and ";
        }
        if (errorDataMatcher != null) {
            description.appendText(joiner).appendText("data ").appendDescriptionOf(errorDataMatcher);
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
        if (response.getError() == null) {
            mismatch.appendText("was not a JSON-RPC error: ").appendText(response.toString());
            return false;
        }
        if (errorCode != null && !errorCode.equals(response.getErrorCode())) {
            mismatch.appendText("error code was ").appendValue(response.getErrorCode()).appendText(": ").appendText(response.toString());
            return false;
        }
        if (errorMessage != null && !errorMessage.equals(response.getErrorMessage())) {
            mismatch.appendText("error message was ").appendValue(response.getErrorMessage()).appendText(": ").appendText(response.toString());
            return false;
        }
        if (errorDataMatcher != null) {
            Object actualData = toMatchable(response.getErrorData());
            if (!errorDataMatcher.matches(actualData)) {
                mismatch.appendText("error data ");
                errorDataMatcher.describeMismatch(actualData, mismatch);
                return false;
            }
        }
        return true;
    }

    /** Converts a JSON array into a {@code List} so that it can be matched with Hamcrest collection matchers. */
    private static Object toMatchable(Object data) {
        if (data instanceof JSONArray array) {
            List<Object> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                items.add(array.opt(i));
            }
            return items;
        }
        return data;
    }
}
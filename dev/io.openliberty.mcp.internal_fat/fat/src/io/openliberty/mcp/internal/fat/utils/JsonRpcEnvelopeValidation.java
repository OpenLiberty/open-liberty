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

import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.INVALID_REQUEST;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.PARSE_ERROR;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * The validity checks on the JSON-RPC envelope that are common to every response, shared by
 * {@link JsonRpcResponseMatcher}, {@link ToolResponseMatcher} and {@link ToolsListResponseMatcher}
 * so that every assertion on a response also checks that the response is a well-formed
 * JSON-RPC 2.0 response to the request.
 *
 * <p>The type and value of each envelope field is checked first. Then, like the other matchers,
 * the envelope is compared with JSONAssert in {@link JSONCompareMode#STRICT} mode against the
 * envelope those checks describe, so a response with a field that JSON-RPC does not allow, at the
 * top level or in {@code error}, is rejected. The content of {@code result} and of
 * {@code error.data} is not checked here: that is up to the matchers.
 *
 * <p>Checks that depend on the protocol version of the request ({@link JsonRpcResponse#getProtocolVersion()})
 * belong here.
 */
final class JsonRpcEnvelopeValidation {

    private JsonRpcEnvelopeValidation() {}

    /**
     * Checks the JSON-RPC envelope of the response.
     *
     * <p>The response {@code id} must equal the id of the request. The exception is a parse error
     * or an invalid request error, where the server may not have been able to read the id of the
     * request, so a {@code null} id is also accepted.
     *
     * <p>The envelope may have no fields other than {@code jsonrpc}, {@code id} and either
     * {@code result} or {@code error}, and {@code error} may have no fields other than
     * {@code code}, {@code message} and {@code data}.
     *
     * @return a description of the first problem found, or {@code null} if the envelope is valid
     */
    static String validate(JsonRpcResponse response) {
        JSONObject json = response.getJson();

        if (!"2.0".equals(json.opt("jsonrpc"))) {
            return "jsonrpc was " + json.opt("jsonrpc") + " rather than \"2.0\"";
        }

        boolean hasResult = json.has("result");
        boolean hasError = json.has("error");
        if (hasResult == hasError) {
            return hasResult ? "had both a result and an error" : "had neither a result nor an error";
        }
        if (hasResult && response.getResult() == null) {
            return "result was " + json.opt("result") + " rather than an object";
        }

        boolean idMayBeNull = false;
        if (hasError) {
            JSONObject error = response.getError();
            if (error == null) {
                return "error was not an object";
            }
            Integer code = response.getErrorCode();
            if (code == null) {
                return "error code was " + error.opt("code") + " rather than an integer";
            }
            if (!(error.opt("message") instanceof String)) {
                return "error message was " + error.opt("message") + " rather than a string";
            }
            idMayBeNull = code == PARSE_ERROR || code == INVALID_REQUEST;
        }

        if (!json.has("id")) {
            return "had no id";
        }
        Object id = json.opt("id");
        boolean idIsNull = JSONObject.NULL.equals(id);
        boolean idIsRequestId = response.getRequestId().equals(id);
        if (!idIsRequestId && !(idIsNull && idMayBeNull)) {
            return "id was " + id + " rather than " + response.getRequestId() + ", the id of the request";
        }
        if (idIsNull && !idMayBeNull) {
            return "id was null, which is only valid for a parse error or an invalid request";
        }

        return compareWithExpectedEnvelope(response, id);
    }

    /** Rejects any field that the checks above do not allow, by comparing strictly with the envelope they describe. */
    private static String compareWithExpectedEnvelope(JsonRpcResponse response, Object id) {
        JSONObject json = response.getJson();
        try {
            JSONObject expected = new JSONObject().put("jsonrpc", "2.0").put("id", id);
            if (json.has("result")) {
                expected.put("result", response.getResult());
            } else {
                JSONObject error = response.getError();
                JSONObject expectedError = new JSONObject().put("code", response.getErrorCode()).put("message", response.getErrorMessage());
                if (error.has("data")) {
                    expectedError.put("data", error.get("data"));
                }
                expected.put("error", expectedError);
            }
            JSONCompareResult comparison = JSONCompare.compareJSON(expected.toString(), json.toString(), JSONCompareMode.STRICT);
            if (comparison.failed()) {
                return "was not a JSON-RPC 2.0 response: " + comparison.getMessage();
            }
        } catch (Exception e) {
            return "could not be compared with a JSON-RPC 2.0 response: " + e;
        }
        return null;
    }
}
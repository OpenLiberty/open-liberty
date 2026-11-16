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

/**
 * The validity checks on the JSON-RPC envelope that are common to every response, shared by
 * {@link JsonRpcResponseMatcher} and {@link ToolResponseMatcher} so that every assertion on a
 * response also checks that the response is a well-formed JSON-RPC 2.0 response to the request.
 *
 * <p>Checks that depend on the protocol version of the request ({@link McpResponse#getProtocolVersion()})
 * belong here.
 */
final class JsonRpcEnvelope {

    private JsonRpcEnvelope() {}

    /**
     * Checks the JSON-RPC envelope of the response.
     *
     * <p>The response {@code id} must equal the id of the request. The exception is a parse error
     * or an invalid request error, where the server may not have been able to read the id of the
     * request, so a {@code null} id is also accepted.
     *
     * @return a description of the first problem found, or {@code null} if the envelope is valid
     */
    static String validate(McpResponse response) {
        JSONObject json = response.getJson();

        if (!"2.0".equals(json.opt("jsonrpc"))) {
            return "jsonrpc was " + json.opt("jsonrpc") + " rather than \"2.0\"";
        }

        boolean hasResult = json.has("result");
        boolean hasError = json.has("error");
        if (hasResult == hasError) {
            return hasResult ? "had both a result and an error" : "had neither a result nor an error";
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

        return null;
    }
}
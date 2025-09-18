/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.exceptions.jsonrpc.MCPRequestValidationException;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

public record McpRequest(String jsonrpc,
                         McpRequestId id,
                         String method,
                         JsonObject params) {

//    Returns the enum value of the supported tool methods
    /**
     * Converts the string value of the method from an MCP request into a matching enum value
     *
     * @return the matching {@link RequestMethod} enum value
     */
    public RequestMethod getRequestMethod() {
        return RequestMethod.getForMethodName(this.method);
    }

    /**
     * Deserialises the MCP request params value from JSON into an object of the specified type
     *
     * @param <T> the target type to map the JSON into
     * @param type the class we want to deserialise the JSON into
     * @param jsonb the jsonb deserialiser to convert the JSON string into an object
     * @return
     */
    public <T> T getParams(Class<T> type, Jsonb jsonb) {
        String json = jsonb.toJson(this.params);
        return jsonb.fromJson(json, type);
    }

    public static McpRequest createValidMCPRequest(Reader reader) throws JsonException, MCPRequestValidationException {

        JsonObject requestJson = Json.createReader(reader).readObject();

        List<String> errors = new ArrayList<>();

        String jsonRpc = requestJson.getString("jsonrpc", null);
        JsonValue id = requestJson.getOrDefault("id", null);
        String method = requestJson.getString("method", null);
        JsonObject params = requestJson.getJsonObject("params");

        validateJsonRpc(jsonRpc, errors);
        validateMethod(method, errors);

        if (id == null) {
            if (!errors.isEmpty()) {
                throw new MCPRequestValidationException(errors);
            }
            return createMCPNotificationRequest(jsonRpc, method, params);
        }

        McpRequestId idObj = parseAndValidateId(id, errors);

        if (!errors.isEmpty()) {
            throw new MCPRequestValidationException(errors);
        }
        return new McpRequest(jsonRpc, idObj, method, params);
    }

    private static McpRequest createMCPNotificationRequest(String jsonRpc,
                                                           String method,
                                                           JsonObject params) {
        return new McpRequest(jsonRpc, null, method, params);
    }

    private static void validateJsonRpc(String jsonRpc, List<String> errors) {
        if (!"2.0".equals(jsonRpc)) {
            errors.add("jsonrpc field must be present. Only JSONRPC 2.0 is currently supported");
        }
    }

    private static void validateMethod(String method, List<String> errors) {
        if (method == null || method.isBlank()) {
            errors.add("method must be present and not empty");
        }
    }

    private static McpRequestId parseAndValidateId(JsonValue id, List<String> errors) {
        return switch (id.getValueType()) {
            case NUMBER -> new McpRequestId(((JsonNumber) id).bigDecimalValue());
            case STRING -> {
                String idString = ((JsonString) id).getString();
                if (idString.isBlank()) {
                    errors.add("id must not be empty");
                    yield null;
                }
                yield new McpRequestId(idString);
            }
            default -> {
                errors.add("id must be a string or number");
                yield null;
            }
        };
    }
}

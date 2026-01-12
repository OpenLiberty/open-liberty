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

import java.lang.reflect.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.request.RequestId;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

/**
 * Instructions for how Jsonb should deserialize JSON values into a {@link RequestId} type
 */
public class McpRequestIdDeserializer implements JsonbDeserializer<RequestId> {
    private static final TraceComponent tc = Tr.register(McpRequestIdDeserializer.class);

    @Override
    public RequestId deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {

        JsonValue jsonVal = parser.getValue();
        switch (jsonVal.getValueType()) {
            case STRING:
                String strVal = ((JsonString) jsonVal).getString();
                if (strVal.isBlank())
                    throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR,
                                               Tr.formatMessage(tc, "CWMCM0019E.jsonrpc.validation.empty.string.id", strVal));
                return new RequestId(strVal);
            case NUMBER:
                return new RequestId(((JsonNumber) jsonVal).bigDecimalValue());
            default:
                throw new JSONRPCException(JSONRPCErrorCode.PARSE_ERROR, Tr.formatMessage(tc, "CWMCM0021E.jsonrpc.validation.invalid.id.type"));

        }
    }
}

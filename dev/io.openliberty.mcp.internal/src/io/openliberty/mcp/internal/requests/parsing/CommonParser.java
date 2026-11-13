/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsing;

import java.util.List;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;

/**
 * Common methods shared between parsers
 */
public class CommonParser {

    /**
     * Parse a transfer object (TO) from a sub-part of a request
     *
     * @param <T> the TO type
     * @param jsonb the jsonb instance
     * @param inputJson the unparsed JSON object
     * @param transferObjectClass the TO type
     * @return the parsed TO
     * @throws JSONRPCException if there is a parsing error. Status code is {@link JSONRPCErrorCode#INVALID_PARAMS}.
     */
    @FFDCIgnore(JsonbException.class)
    public static <T> T parseParamsTransferObject(Jsonb jsonb, JsonValue inputJson, Class<T> transferObjectClass) {
        try {
            T requestTO = jsonb.fromJson(inputJson.toString(), transferObjectClass);
            if (requestTO == null) {
                // The body was a JSON literal null
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, null);
            }
            return requestTO;
        } catch (JsonbException ex) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of(ex.getMessage()));
        }
    }

}

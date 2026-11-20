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

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Transfer object for a JSON-RPC request/notification message.
 *
 * @see https://www.jsonrpc.org/specification#request_object
 */
@Trivial
public record JsonRpcRequestTO(String jsonrpc, String method, JsonValue id, JsonObject params) {

    @JsonbCreator
    public static JsonRpcRequestTO create(@JsonbProperty("jsonrpc") String jsonrpc,
                                          @JsonbProperty("method") String method,
                                          @JsonbProperty("id") JsonValue id,
                                          @JsonbProperty("params") JsonObject params) {

        return new JsonRpcRequestTO(jsonrpc, method, id, params);
    }

}

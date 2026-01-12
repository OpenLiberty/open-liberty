/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.request;

/**
 * Represents the ID of a JSON-RPC request.
 * Must be a non-null String or Number.
 *
 * @param value the value of the request ID, which must be a valid JSON-RPC ID
 *     (string or number). Used to uniquely identify each request.
 */
public record RequestId(Object value) {
    public RequestId {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new IllegalArgumentException("value must be string or number");
        }
    }

    public String asString() {
        return value.toString();
    }

    public Integer asInteger() {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("RequestId is not a number");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
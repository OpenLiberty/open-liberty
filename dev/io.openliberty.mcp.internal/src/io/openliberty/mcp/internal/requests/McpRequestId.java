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

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;

/**
 * Stores the id of an MCP Request, which can be represented as a String or Number.
 */
@JsonbTypeSerializer(McpRequestIdSerializer.class)
@JsonbTypeDeserializer(McpRequestIdDeserializer.class)
public final class McpRequestId {

    private final String strVal;
    private final BigDecimal numVal;

    public McpRequestId(String value) {
        this.strVal = value;
        this.numVal = null;
    }

    public McpRequestId(BigDecimal value) {
        this.numVal = value;
        this.strVal = null;
    }

    /**
     * Retrieves the stored MCP Request ID value.
     *
     * @return the MCP Request ID value as an Object.
     * It will be either a String or BigDecimal depending on the type of the ID.
     */
    public Object getValue() {
        if (strVal != null)
            return strVal;
        return numVal;
    }

    /**
     * Compares two id values for equality, considering only String and BigDecimal types.
     *
     * @param val1 The first object to compare.
     * @param val2 The second object to compare.
     * @return True if the objects are equal, false otherwise.
     */
    public static boolean mcpIdsAreEqual(McpRequestId obj1, McpRequestId obj2) {
        Object val1 = obj1.getValue();
        Object val2 = obj2.getValue();
        if (val1 instanceof String && val2 instanceof String) {
            return val1.equals(val2);
        }
        if (val1 instanceof BigDecimal && val2 instanceof BigDecimal) {
            // checks both Big Decimals have the same value
            return ((BigDecimal) val1).compareTo((BigDecimal) val2) == 0;
        }
        return false;
    }

    /**
     * Overrides the equals method to compare if two MCP Request IDs are equal
     *
     * @param obj The McpRequestId object to compare.
     * @return True if the MCP Request IDs are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof McpRequestId) {
            McpRequestId objToCompare = (McpRequestId) obj;
            return mcpIdsAreEqual(this, objToCompare);
        }
        return false;
    }

    /**
     * Overrides the hashCode method to generate a hash code based on the stored id value.
     *
     * @return The hash code for this McpRequestId object based on the stored id value.
     */
    @Override
    public int hashCode() {
        if (strVal != null)
            return Objects.hash(strVal);
        return Objects.hash(numVal);
    }

}

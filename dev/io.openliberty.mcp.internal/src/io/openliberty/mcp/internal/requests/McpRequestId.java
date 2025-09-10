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

import java.util.Objects;

/**
 * Used to store the id of an McpRequest,
 * which can either be represented as a String or Integer.
 */
public final class McpRequestId {

    private final Object value;

    public McpRequestId(Object value) {
        if (!isString(value) && !isInteger(value))
            throw new IllegalArgumentException("Request Id must be a String or Number");
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public static boolean isString(Object val) {
        return val instanceof String;
    }

    public static boolean isInteger(Object val) {
        return val instanceof Integer || val instanceof Short || val instanceof Long || val instanceof Byte;
    }

    /**
     * Compares two id values for equality, considering only String and Number types.
     *
     * @param val1 The first object to compare.
     * @param val2 The second object to compare.
     * @return True if the objects are equal, false otherwise.
     */
    public static boolean mcpIdsAreEqual(Object val1, Object val2) {
        if (isString(val1) && isString(val2))
            return val1.equals(val2);
        if (isInteger(val1) && isInteger(val2)) {
            Number num1 = (Number) val1;
            Number num2 = (Number) val2;
            return num1.longValue() == num2.longValue();
        }
        return false;
    }

    /**
     * Overrides the equals method to compare if the inputted object
     * has the same id value as this object
     *
     * @param obj The object to compare.
     * @return True if the inputted object is either a McpRequestId with the same id value
     * or the inputted object is an id with the same value as this object, false if otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof McpRequestId) {
            McpRequestId objToCompare = (McpRequestId) obj;
            return mcpIdsAreEqual(this.value, objToCompare.getValue());
        }
        return mcpIdsAreEqual(this.value, obj);
    }

    /**
     * Overrides the hashCode method to generate a hash code based on the stored id value.
     *
     * @return The hash code for this McpRequestId object based on the stored id value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

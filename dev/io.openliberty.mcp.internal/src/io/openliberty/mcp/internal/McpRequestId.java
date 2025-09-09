/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Objects;

/**
 *
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

    private boolean isEqual(Object val1, Object val2) {
        if (isString(val1) && isString(val2))
            return val1.equals(val2);
        if (isInteger(val1) && isInteger(val2)) {
            Number num1 = (Number) val1;
            Number num2 = (Number) val2;
            return num1.longValue() == num2.longValue();
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof McpRequestId) {
            McpRequestId objToCompare = (McpRequestId) obj;
            return isEqual(this.value, objToCompare.getValue());
        }
        return isEqual(this.value, obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

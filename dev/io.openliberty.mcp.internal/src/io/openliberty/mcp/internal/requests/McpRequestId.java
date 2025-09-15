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
     * @return the strVal
     */
    public String getStrVal() {
        return strVal;
    }

    /**
     * @return the numVal
     */
    public BigDecimal getNumVal() {
        return numVal;
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
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        McpRequestId other = (McpRequestId) obj;
        return Objects.equals(numVal, other.numVal) && Objects.equals(strVal, other.strVal);
    }

    /**
     * Overrides the hashCode method to generate a hash code based on the stored id value.
     *
     * @return The hash code for this McpRequestId object based on the stored id value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(numVal, strVal);
    }

    @Override
    public String toString() {
        if (getStrVal() != null)
            return getStrVal();
        return getNumVal().toString();
    }

}

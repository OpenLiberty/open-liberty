/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.metrics;

import static io.openliberty.mcp.internal.monitor.metrics.JmxHelper.escapeJmxValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Represents the attributes of an MCP (Model Context Protocol) session for monitoring and metrics collection.
 *
 * <p>This record captures various attributes of MCP sessions including:
 * <ul>
 * <li>Error type (optional)</li>
 * <li>Protocol versions (optional)</li>
 * <li>Network protocol details (optional)</li>
 * </ul>
 *
 * <p>Instances should be created using the {@link Builder} pattern via {@link #builder()}.
 *
 * @see Builder
 */
public record McpSessionStatAttributes(
                                       String errorType,
                                       String jsonrpcProtocolVersion,
                                       String mcpProtocolVersion,
                                       String networkProtocolName,
                                       String networkProtocolVersion,
                                       String networkTransport,
                                       String mcpSessionStat_ID) {

    private static final TraceComponent tc = Tr.register(McpSessionStatAttributes.class);

    /**
     * Computes the JMX-safe identifier.
     */
    public McpSessionStatAttributes {
        // Compute the JMX-safe identifier if not provided
        if (mcpSessionStat_ID == null) {
            mcpSessionStat_ID = resolveKeyID(errorType, jsonrpcProtocolVersion, mcpProtocolVersion,
                                             networkProtocolName, networkProtocolVersion, networkTransport);
        }
    }

    /**
     * Resolve the object name (specifically the name property)
     * <code> domain:type=type,name="this"</code>
     *
     * This is also used when registering MBean into the MeterCollection.
     *
     * This method creates a JMX-safe identifier by using a structured format with key-value pairs
     * separated by semicolons, wrapping the entire ID in quotes, and escaping special characters
     * to prevent collisions and handle user-entered data safely.
     *
     * @return a JMX-safe identifier string
     */
    private static String resolveKeyID(String errorType, String jsonrpcProtocolVersion, String mcpProtocolVersion,
                                       String networkProtocolName, String networkProtocolVersion, String networkTransport) {
        StringBuilder sb = new StringBuilder();
        sb.append("\""); // starting quote

        // Start with a base identifier to ensure we always have something
        sb.append("session");

        /*
         * Optional fields - only append if not null
         */
        if (errorType != null) {
            sb.append(";errorType:").append(escapeJmxValue(errorType));
        }

        if (jsonrpcProtocolVersion != null) {
            sb.append(";jsonrpcVer:").append(escapeJmxValue(jsonrpcProtocolVersion));
        }

        if (mcpProtocolVersion != null) {
            sb.append(";mcpVer:").append(escapeJmxValue(mcpProtocolVersion));
        }

        if (networkProtocolName != null) {
            sb.append(";netProto:").append(escapeJmxValue(networkProtocolName));
        }

        if (networkProtocolVersion != null) {
            sb.append(";netProtoVer:").append(escapeJmxValue(networkProtocolVersion));
        }

        if (networkTransport != null) {
            sb.append(";netTransport:").append(escapeJmxValue(networkTransport));
        }

        sb.append("\""); // ending quote
        return sb.toString();
    }

    /**
     * Returns the JMX-safe identifier for this MCP session.
     * This is an alias for the record accessor method for backward compatibility.
     *
     * @return the MCP session stat ID
     */
    public String getMcpSessionStatID() {
        return mcpSessionStat_ID;
    }

    /**
     * Converts the attributes to a Map suitable for creating JMX ObjectName properties.
     * Only non-null attributes are included in the map.
     *
     * @return a LinkedHashMap of attribute key-value pairs
     */
    public Map<String, String> toAttributeMap() {
        Map<String, String> attributes = new LinkedHashMap<>();

        // Add a base identifier for sessions
        attributes.put("session", "true");

        // Add optional attributes only if they are not null
        if (errorType != null) {
            attributes.put("errorType", errorType);
        }

        if (jsonrpcProtocolVersion != null) {
            attributes.put("jsonrpcVer", jsonrpcProtocolVersion);
        }

        if (mcpProtocolVersion != null) {
            attributes.put("mcpVer", mcpProtocolVersion);
        }

        if (networkProtocolName != null) {
            attributes.put("netProto", networkProtocolName);
        }

        if (networkProtocolVersion != null) {
            attributes.put("netProtoVer", networkProtocolVersion);
        }

        if (networkTransport != null) {
            attributes.put("netTransport", networkTransport);
        }

        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /*
         * Conditionally required fields for MCP sessions
         */
        private String errorType;

        /*
         * Exception related fields are optional. We are unable to facilitate capturing
         * Exceptions But we will leave it here. Additional Context : We can capture
         * exceptions thrown by servlets by surrounding the the chainFilter with try
         * catch. But we have no way of capturing application exception of
         * Jaxrs/restfulws exceptions
         */

        private String jsonrpcProtocolVersion;
        private String mcpProtocolVersion;
        private String networkProtocolName;
        private String networkProtocolVersion;
        private String networkTransport;

        /*
         * Define a constructor with default protection so others do not call it directly and instead
         * call the builder() method above.
         */
        Builder() {}

        /**
         * Builds an instance of {@link McpSessionStatAttributes} with values from this
         * builder. Returns an empty Optional if the required fields are not filled.
         *
         * @return Optional containing the {@link McpSessionStatAttributes} instance if valid,
         * or empty Optional if validation fails
         */
        @FFDCIgnore(value = { IllegalStateException.class })
        public Optional<McpSessionStatAttributes> build() {
            try {
                return Optional.of(new McpSessionStatAttributes(
                                                                errorType,
                                                                jsonrpcProtocolVersion,
                                                                mcpProtocolVersion,
                                                                networkProtocolName,
                                                                networkProtocolVersion,
                                                                networkTransport,
                                                                null // Let the compact constructor compute the ID
                ));
            } catch (IllegalStateException ise) {
                //do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, String.format("Invalid MCP Stats attributes : \n %s", toString()));
                }
            }
            return Optional.empty();
        }

        public Builder withErrorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder withJsonrpcProtocolVersion(String jsonrpcProtocolVersion) {
            this.jsonrpcProtocolVersion = jsonrpcProtocolVersion;
            return this;
        }

        public Builder withMcpProtocolVersion(String mcpProtocolVersion) {
            this.mcpProtocolVersion = mcpProtocolVersion;
            return this;
        }

        public Builder withNetworkProtocolName(String networkProtocolName) {
            this.networkProtocolName = networkProtocolName;
            return this;
        }

        public Builder withNetworkProtocolVersion(String networkProtocolVersion) {
            this.networkProtocolVersion = networkProtocolVersion;
            return this;
        }

        public Builder withNetworkTransport(String networkTransport) {
            this.networkTransport = networkTransport;
            return this;
        }

        public boolean validate() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Builder [errorType=" + errorType + ", jsonrpcProtocolVersion=" + jsonrpcProtocolVersion + ", mcpProtocolVersion="
                   + mcpProtocolVersion + ", networkProtocolName=" + networkProtocolName + ", networkProtocolVersion=" + networkProtocolVersion + ", networkTransport="
                   + networkTransport + "]";
        }

    }
}
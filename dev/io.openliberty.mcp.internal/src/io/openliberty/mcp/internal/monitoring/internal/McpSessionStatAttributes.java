/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitoring.internal;

import java.util.Objects;
import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class McpSessionStatAttributes {

    private static final TraceComponent tc = Tr.register(McpSessionStatAttributes.class);

    /*
     * Conditionally required as per MCP Semantics Convention
     */
    private final String errorType;

    /*
     * Recommended fields.
     */
    private final String jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport;

    /**
     * Constructor for McpSessionStatAttributes. This should not be called directly, but
     * should be instantiated through {@link Builder#build()}
     *
     * @param builder see {@link Builder}
     * @throws IllegalStateException if the builder's validation fails
     */
    /**
     * @param errorType
     * @param jsonrpcProtocolVersion
     * @param mcpProtocolVersion
     * @param networkProtocolName
     * @param networkProtocolVersion
     * @param networkTransport
     */
    public McpSessionStatAttributes(Builder builder) throws IllegalStateException {
        if (!builder.validate()) {
            throw new IllegalStateException("Invalid MCP Stats attributes");
        } ;
        this.errorType = (builder.errorType.isPresent() ? builder.errorType.get() : null);
        this.jsonrpcProtocolVersion = (builder.jsonrpcProtocolVersion.isPresent() ? builder.jsonrpcProtocolVersion.get() : null);
        this.mcpProtocolVersion = (builder.mcpProtocolVersion.isPresent() ? builder.mcpProtocolVersion.get() : null);
        this.networkProtocolName = (builder.networkProtocolName.isPresent() ? builder.networkProtocolName.get() : null);
        this.networkProtocolVersion = (builder.networkProtocolVersion.isPresent() ? builder.networkProtocolVersion.get() : null);
        this.networkTransport = (builder.networkTransport.isPresent() ? builder.networkTransport.get() : null);
    }

    /**
     * @return the tc
     */
    public static TraceComponent getTc() {
        return tc;
    }

    /**
     * @return the errorType
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @return the jsonrpcProtocolVersion
     */
    public String getJsonrpcProtocolVersion() {
        return jsonrpcProtocolVersion;
    }

    /**
     * @return the mcpProtocolVersion
     */
    public String getMcpProtocolVersion() {
        return mcpProtocolVersion;
    }

    /**
     * @return the networkProtocolName
     */
    public String getNetworkProtocolName() {
        return networkProtocolName;
    }

    /**
     * @return the networkProtocolVersion
     */
    public String getNetworkProtocolVersion() {
        return networkProtocolVersion;
    }

    /**
     * @return the networkTransport
     */
    public String getNetworkTransport() {
        return networkTransport;
    }

    /**
     * Generates a JMX-safe identifier string by concatenating all non-null attribute values with underscores.
     * This string is used as the key for MBean registration in the Liberty monitoring framework.
     * The underscore separates different attributes, but attribute values are preserved as-is.
     *
     * @return a string representation suitable for use as a JMX ObjectName property value
     */
    @Override
    public String toString() {
        return String.join("_",
                           java.util.stream.Stream.of(
                                                      errorType, jsonrpcProtocolVersion, mcpProtocolVersion,
                                                      networkProtocolName, networkProtocolVersion, networkTransport)
                                                  .filter(s -> s != null)
                                                  .toArray(String[]::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorType, jsonrpcProtocolVersion, mcpProtocolVersion,
                            networkProtocolName, networkProtocolVersion, networkTransport);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        McpSessionStatAttributes other = (McpSessionStatAttributes) obj;
        return Objects.equals(errorType, other.errorType)
               && Objects.equals(jsonrpcProtocolVersion, other.jsonrpcProtocolVersion)
               && Objects.equals(mcpProtocolVersion, other.mcpProtocolVersion)
               && Objects.equals(networkProtocolName, other.networkProtocolName)
               && Objects.equals(networkProtocolVersion, other.networkProtocolVersion)
               && Objects.equals(networkTransport, other.networkTransport);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String mcpMethodName;

        /*
         * Conditionally required fields for MCP sessions
         */
        private Optional<String> errorType = Optional.empty();

        /*
         * Exception related fields are optional. We are unable to facilitate capturing
         * Exceptions But we will leave it here. Additional Context : We can capture
         * exceptions thrown by servlets by surrounding the the chainFilter with try
         * catch. But we have no way of capturing application exception of
         * Jaxrs/restfulws exceptions
         */

        private Optional<String> jsonrpcProtocolVersion = Optional.empty();
        private Optional<String> mcpProtocolVersion = Optional.empty();
        private Optional<String> networkProtocolName = Optional.empty();
        private Optional<String> networkProtocolVersion = Optional.empty();
        private Optional<String> networkTransport = Optional.empty();

        /*
         * Define a constructor with default protection so others do not call it directly and instead
         * call the builder() method above.
         */
        Builder() {}

        /**
         * Builds an instance of {@link McpSessionStatAttributes} with values from this
         * builder. Will validate and throw an {@link IllegalStateException} if the
         * required fields are not filled.
         *
         * @return Instance of {@link McpSessionStatAttributes}
         * @throws IllegalStateException
         */
        @FFDCIgnore(value = { IllegalStateException.class })
        public McpSessionStatAttributes build() {
            try {
                return new McpSessionStatAttributes(this);
            } catch (IllegalStateException ise) {
                //do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, String.format("Invalid MCP Stats attributes : \n %s", toString()));
                }
            }
            return null;
        }

        public Builder withErrorType(Optional<String> errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder withJsonrpcProtocolVersion(Optional<String> jsonrpcProtocolVersion) {
            this.jsonrpcProtocolVersion = jsonrpcProtocolVersion;
            return this;
        }

        public Builder withMcpProtocolVersion(Optional<String> mcpProtocolVersion) {
            this.mcpProtocolVersion = mcpProtocolVersion;
            return this;
        }

        public Builder withNetworkProtocolName(Optional<String> networkProtocolName) {
            this.networkProtocolName = networkProtocolName;
            return this;
        }

        public Builder withNetworkProtocolVersion(Optional<String> networkProtocolVersion) {
            this.networkProtocolVersion = networkProtocolVersion;
            return this;
        }

        public Builder withNetworkTransport(Optional<String> networkTransport) {
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

// Made with Bob

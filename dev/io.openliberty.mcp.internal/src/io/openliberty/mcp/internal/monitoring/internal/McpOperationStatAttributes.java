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

/**
 * Represents the attributes of an MCP (Model Context Protocol) operation for monitoring and metrics collection.
 *
 * <p>This class captures various attributes of MCP operations including:
 * <ul>
 * <li>MCP method name (mandatory)</li>
 * <li>Tool and prompt information (optional)</li>
 * <li>Network protocol details (optional)</li>
 * <li>Response status and error information (optional)</li>
 * </ul>
 *
 * <p>Instances should be created using the {@link Builder} pattern via {@link #builder()}.
 * The builder validates that all mandatory fields are provided before constructing an instance.
 *
 * @see Builder
 */
public class McpOperationStatAttributes {

    private static final TraceComponent tc = Tr.register(McpOperationStatAttributes.class);

    /*
     * Mandatory fields - The MCP method name is required to identify the operation type.
     */
    private final String mcpMethodName;

    /*
     * Conditionally required fields for MCP operations
     */
    private final String errorType, genAiPromptName, genAiToolName, rpcResponseStatusCode;

    /*
     * Optional fields.
     * We are unable to facilitate capturing Exceptions
     * But we will leave it here.
     * Additional Context : We can capture exceptions thrown by servlets
     * by surrounding the the chainFilter with try catch. But we have no way
     * of capturing application exception of Jaxrs/restfulws exceptions
     */
    private final String genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri;

    /**
     * Private constructor for McpOperationStatAttributes. This should not be called directly, but
     * should be instantiated through {@link Builder#build()}.
     *
     * @param builder the builder containing the attribute values
     * @throws IllegalStateException if the builder's validation fails
     */
    private McpOperationStatAttributes(Builder builder) throws IllegalStateException {
        if (!builder.validate()) {
            throw new IllegalStateException("Invalid MCP Stats attributes");
        } ;
        this.mcpMethodName = builder.mcpMethodName;

        this.errorType = builder.errorType.orElse(null);
        this.genAiPromptName = builder.genAiPromptName.orElse(null);
        this.genAiToolName = builder.genAiToolName.orElse(null);
        this.rpcResponseStatusCode = builder.rpcResponseStatusCode.orElse(null);
        this.genAiOperationName = builder.genAiOperationName.orElse(null);
        this.jsonrpcProtocolVersion = builder.jsonrpcProtocolVersion.orElse(null);
        this.mcpProtocolVersion = builder.mcpProtocolVersion.orElse(null);
        this.networkProtocolName = builder.networkProtocolName.orElse(null);
        this.networkProtocolVersion = builder.networkProtocolVersion.orElse(null);
        this.networkTransport = builder.networkTransport.orElse(null);
        this.mcpResourceUri = builder.mcpResourceUri.orElse(null);
    }

    /**
     * @return the tc
     */
    public static TraceComponent getTc() {
        return tc;
    }

    /**
     * @return the mcpMethodName
     */
    public String getMcpMethodName() {
        return mcpMethodName;
    }

    /**
     * @return the errorType
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @return the genAiPromptName
     */
    public String getGenAiPromptName() {
        return genAiPromptName;
    }

    /**
     * @return the genAiToolName
     */
    public String getGenAiToolName() {
        return genAiToolName;
    }

    /**
     * @return the rpcResponseStatusCode
     */
    public String getRpcResponseStatusCode() {
        return rpcResponseStatusCode;
    }

    /**
     * @return the genAiOperationName
     */
    public String getGenAiOperationName() {
        return genAiOperationName;
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
     * @return the mcpResourceUri
     */
    public String getMcpResourceUri() {
        return mcpResourceUri;
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
                                                      mcpMethodName, genAiToolName, errorType, genAiPromptName,
                                                      rpcResponseStatusCode, genAiOperationName, jsonrpcProtocolVersion,
                                                      mcpProtocolVersion, networkProtocolName, networkProtocolVersion,
                                                      networkTransport, mcpResourceUri)
                                                  .filter(s -> s != null)
                                                  .toArray(String[]::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash(mcpMethodName, errorType, genAiPromptName, genAiToolName,
                            rpcResponseStatusCode, genAiOperationName, jsonrpcProtocolVersion,
                            mcpProtocolVersion, networkProtocolName, networkProtocolVersion,
                            networkTransport, mcpResourceUri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        McpOperationStatAttributes other = (McpOperationStatAttributes) obj;
        return Objects.equals(mcpMethodName, other.mcpMethodName)
               && Objects.equals(errorType, other.errorType)
               && Objects.equals(genAiPromptName, other.genAiPromptName)
               && Objects.equals(genAiToolName, other.genAiToolName)
               && Objects.equals(rpcResponseStatusCode, other.rpcResponseStatusCode)
               && Objects.equals(genAiOperationName, other.genAiOperationName)
               && Objects.equals(jsonrpcProtocolVersion, other.jsonrpcProtocolVersion)
               && Objects.equals(mcpProtocolVersion, other.mcpProtocolVersion)
               && Objects.equals(networkProtocolName, other.networkProtocolName)
               && Objects.equals(networkProtocolVersion, other.networkProtocolVersion)
               && Objects.equals(networkTransport, other.networkTransport)
               && Objects.equals(mcpResourceUri, other.mcpResourceUri);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String mcpMethodName;

        /*
         * Conditionally required as per HTTP Semantics Convention
         */
        private Optional<String> errorType = Optional.empty();
        private Optional<String> genAiPromptName = Optional.empty();
        private Optional<String> genAiToolName = Optional.empty();
        private Optional<String> rpcResponseStatusCode = Optional.empty();

        /*
         * Exception related fields are optional. We are unable to facilitate capturing
         * Exceptions But we will leave it here. Additional Context : We can capture
         * exceptions thrown by servlets by surrounding the the chainFilter with try
         * catch. But we have no way of capturing application exception of
         * Jaxrs/restfulws exceptions
         */

        private Optional<String> genAiOperationName = Optional.empty();
        private Optional<String> jsonrpcProtocolVersion = Optional.empty();
        private Optional<String> mcpProtocolVersion = Optional.empty();
        private Optional<String> networkProtocolName = Optional.empty();
        private Optional<String> networkProtocolVersion = Optional.empty();
        private Optional<String> mcpResourceUri = Optional.empty();
        private Optional<String> networkTransport = Optional.empty();

        /*
         * Define a constructor with default protection so others do not call it directly and instead
         * call the builder() method above.
         */
        Builder() {}

        /**
         * Builds an instance of {@link McpOperationStatAttributes} with values from this
         * builder. Will validate and throw an {@link IllegalStateException} if the
         * required fields are not filled.
         *
         * @return Instance of {@link McpOperationStatAttributes}
         * @throws IllegalStateException
         */
        @FFDCIgnore(value = { IllegalStateException.class })
        public McpOperationStatAttributes build() {
            try {
                return new McpOperationStatAttributes(this);
            } catch (IllegalStateException ise) {
                //do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, String.format("Invalid MCP Stats attributes : \n %s", toString()));
                }
            }
            return null;
        }

        public Builder withMcpMethodName(String mcpMethodName) {
            this.mcpMethodName = mcpMethodName;
            return this;
        }

        public Builder withErrorType(Optional<String> errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder withGenAiPromptName(Optional<String> genAiPromptName) {
            this.genAiPromptName = genAiPromptName;
            return this;
        }

        public Builder withGenAiToolName(Optional<String> genAiToolName) {
            this.genAiToolName = genAiToolName;
            return this;
        }

        public Builder withRpcResponseStatusCode(Optional<String> rpcResponseStatusCode) {
            this.rpcResponseStatusCode = rpcResponseStatusCode;
            return this;
        }

        public Builder withGenAiOperationName(Optional<String> genAiOperationName) {
            this.genAiOperationName = genAiOperationName;
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

        public Builder withMcpResourceUri(Optional<String> mcpResourceUri) {
            this.mcpResourceUri = mcpResourceUri;
            return this;
        }

        public boolean validate() {
            return (mcpMethodName != null);
        }

//                @Override
//                public String toString() {
//                        return String.format(
//                                        " ------- \n"
//                        + "Request Method (mandatory): [%s] \n"
//                        + "Scheme (mandatory): [%s] \n"
//                        + "Network Protocol Name (optional): [%s] \n"
//                        + "Network Protocol Version (mandatory): [%s] \n"
//                        + "Server Name (mandatory): [%s] \n"
//                        + "Server Port (mandatory): [%d] \n"
//                        + "HTTP Route (Optional: can be empty): [%s] \n"
//                        + "Response Status (Optional: can be -1): [%d] \n"
//                        + "Error Type(Optional - can be empty): [%s]",
//                                        requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName, serverPort,
//                                        httpRoute.orElse(null), responseStatus.orElse(null), errorType.orElse(null));
//                }

        @Override
        public String toString() {
            return "Builder [mcpMethodName=" + mcpMethodName + ", errorType=" + errorType + ", genAiPromptName="
                   + genAiPromptName + ", genAiToolName=" + genAiToolName + ", rpcResponseStatusCode="
                   + rpcResponseStatusCode + ", genAiOperationName=" + genAiOperationName
                   + ", jsonrpcProtocolVersion=" + jsonrpcProtocolVersion + ", mcpProtocolVersion="
                   + mcpProtocolVersion + ", networkProtocolName=" + networkProtocolName
                   + ", networkProtocolVersion=" + networkProtocolVersion + ", networkTransport="
                   + networkTransport + ", mcpResourceUri=" + mcpResourceUri + "]";
        }

    }
}

// Made with Bob

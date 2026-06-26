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

import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Represents the attributes of an MCP (Model Context Protocol) operation for monitoring and metrics collection.
 *
 * <p>This record captures various attributes of MCP operations including:
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
public record McpOperationStatAttributes(
    String mcpMethodName,
    String errorType,
    String genAiPromptName,
    String genAiToolName,
    String rpcResponseStatusCode,
    String genAiOperationName,
    String jsonrpcProtocolVersion,
    String mcpProtocolVersion,
    String networkProtocolName,
    String networkProtocolVersion,
    String networkTransport,
    String mcpResourceUri,
    String mcpOperationStat_ID
) {

    private static final TraceComponent tc = Tr.register(McpOperationStatAttributes.class);

    /**
     * Computes the JMX-safe identifier.
     */
    public McpOperationStatAttributes {
        if (mcpMethodName == null) {
            throw new IllegalStateException("Invalid MCP Stats attributes: mcpMethodName is required");
        }
        
        // Compute the JMX-safe identifier if not provided
        if (mcpOperationStat_ID == null) {
            mcpOperationStat_ID = resolveKeyID(mcpMethodName, genAiToolName, errorType, genAiPromptName,
                                               rpcResponseStatusCode, genAiOperationName, jsonrpcProtocolVersion,
                                               mcpProtocolVersion, networkProtocolName, networkProtocolVersion,
                                               networkTransport, mcpResourceUri);
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
    private static String resolveKeyID(String mcpMethodName, String genAiToolName, String errorType,
                                       String genAiPromptName, String rpcResponseStatusCode, String genAiOperationName,
                                       String jsonrpcProtocolVersion, String mcpProtocolVersion, String networkProtocolName,
                                       String networkProtocolVersion, String networkTransport, String mcpResourceUri) {
        StringBuilder sb = new StringBuilder();
        sb.append("\""); // starting quote
        sb.append("mcpMethod:").append(escapeJmxValue(mcpMethodName));
        
        /*
         * Optional fields - only append if not null
         */
        if (genAiToolName != null) {
            sb.append(";genAiTool:").append(escapeJmxValue(genAiToolName));
        }
        
        if (errorType != null) {
            sb.append(";errorType:").append(escapeJmxValue(errorType));
        }
        
        if (genAiPromptName != null) {
            sb.append(";genAiPrompt:").append(escapeJmxValue(genAiPromptName));
        }
        
        if (rpcResponseStatusCode != null) {
            sb.append(";rpcStatus:").append(escapeJmxValue(rpcResponseStatusCode));
        }
        
        if (genAiOperationName != null) {
            sb.append(";genAiOp:").append(escapeJmxValue(genAiOperationName));
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
        
        if (mcpResourceUri != null) {
            sb.append(";resourceUri:").append(escapeJmxValue(mcpResourceUri));
        }
        
        sb.append("\""); // ending quote
        return sb.toString();
    }
    
    /**
     * Returns the JMX-safe identifier for this MCP operation.
     * This is an alias for the record accessor method for backward compatibility.
     *
     * @return the MCP operation stat ID
     */
    public String getMcpOperationStatID() {
        return mcpOperationStat_ID;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String mcpMethodName;

        /*
         * Conditionally required as per HTTP Semantics Convention
         */
        private String errorType;
        private String genAiPromptName;
        private String genAiToolName;
        private String rpcResponseStatusCode;

        /*
         * Exception related fields are optional. We are unable to facilitate capturing
         * Exceptions But we will leave it here. Additional Context : We can capture
         * exceptions thrown by servlets by surrounding the the chainFilter with try
         * catch. But we have no way of capturing application exception of
         * Jaxrs/restfulws exceptions
         */

        private String genAiOperationName;
        private String jsonrpcProtocolVersion;
        private String mcpProtocolVersion;
        private String networkProtocolName;
        private String networkProtocolVersion;
        private String mcpResourceUri;
        private String networkTransport;

        /*
         * Define a constructor with default protection so others do not call it directly and instead
         * call the builder() method above.
         */
        Builder() {}

        /**
         * Builds an instance of {@link McpOperationStatAttributes} with values from this
         * builder. Returns an empty Optional if the required fields are not filled.
         *
         * @return Optional containing the {@link McpOperationStatAttributes} instance if valid,
         *         or empty Optional if validation fails
         */
        @FFDCIgnore(value = { IllegalStateException.class })
        public Optional<McpOperationStatAttributes> build() {
            try {
                return Optional.of(new McpOperationStatAttributes(
                    mcpMethodName,
                    errorType,
                    genAiPromptName,
                    genAiToolName,
                    rpcResponseStatusCode,
                    genAiOperationName,
                    jsonrpcProtocolVersion,
                    mcpProtocolVersion,
                    networkProtocolName,
                    networkProtocolVersion,
                    networkTransport,
                    mcpResourceUri,
                    null //  constructor computes the ID
                ));
            } catch (IllegalStateException ise) {
                //do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, String.format("Invalid MCP Stats attributes : \n %s", toString()));
                }
            }
            return Optional.empty();
        }

        public Builder withMcpMethodName(String mcpMethodName) {
            this.mcpMethodName = mcpMethodName;
            return this;
        }

        public Builder withErrorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder withGenAiPromptName(String genAiPromptName) {
            this.genAiPromptName = genAiPromptName;
            return this;
        }

        public Builder withGenAiToolName(String genAiToolName) {
            this.genAiToolName = genAiToolName;
            return this;
        }

        public Builder withRpcResponseStatusCode(String rpcResponseStatusCode) {
            this.rpcResponseStatusCode = rpcResponseStatusCode;
            return this;
        }

        public Builder withGenAiOperationName(String genAiOperationName) {
            this.genAiOperationName = genAiOperationName;
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

        public Builder withMcpResourceUri(String mcpResourceUri) {
            this.mcpResourceUri = mcpResourceUri;
            return this;
        }

        public boolean validate() {
            return (mcpMethodName != null);
        }

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

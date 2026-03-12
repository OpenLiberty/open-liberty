/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor;

import java.util.Optional;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class McpStatAttributes {
        
        private final String mcpStat_ID;
        
        private static final TraceComponent tc = Tr.register(McpStatAttributes.class);
        
        /*
         * Mandatory fields - Technically networkProtocolName is optional as per http semantics
         * It is conditionally required if scheme is NOT http and protocol version is set.
         * But we'll make it mandatory anyways
         */
        private final String mcpMethodName;
        
        /*
         * Conditionally required as per HTTP Semantics Convention
         */ 
        private final String errorType, genAiPromptName, genAiToolName, rpcResponseStatusCode;
        
        /*
         * Optional fields.
         * We are unable to facilitate capturing Exceptions
         * But we will leave it here.
         * Additional Context : We can capture  exceptions thrown by servlets
         * by surrounding the the chainFilter with try catch. But we have no way
         * of capturing application exception of Jaxrs/restfulws exceptions
         */
        private final String genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri;
        
        
        
        /**
         * Constructor for HttpStatsAttributes. This should not be called directly, but
         * should be instantiated through {@link Builder#build()}
         * 
         * @param builder see {@link Builder}
         * @throws IllegalStateException if the builder's validation fails
         */
        /**
		 * @param httpStat_ID
		 * @param mcpMethodName
		 * @param errorType
		 * @param genAiPromptName
		 * @param genAiToolName
		 * @param rpcResponseStatusCode
		 * @param genAiOperationName
		 * @param jsonrpcProtocolVersion
		 * @param mcpProtocolVersion
		 * @param networkProtocolName
		 * @param networkProtocolVersion
		 * @param networkTransport
		 * @param mcpResourceUri
		 */
		public McpStatAttributes(Builder builder) throws IllegalStateException {
			if (!builder.validate()) {
              throw new IllegalStateException("Invalid MCP Stats attributes");
			};
			this.mcpMethodName = builder.mcpMethodName;
			
			this.errorType = (builder.errorType.isPresent() ? builder.errorType.get() : null);
			this.genAiPromptName = (builder.genAiPromptName.isPresent() ? builder.genAiPromptName.get() : null);
			this.genAiToolName = (builder.genAiToolName.isPresent() ? builder.genAiToolName.get() : null);
			this.rpcResponseStatusCode = (builder.rpcResponseStatusCode.isPresent() ? builder.rpcResponseStatusCode.get() : null);
			this.genAiOperationName = (builder.genAiOperationName.isPresent() ? builder.genAiOperationName.get() : null);
			this.jsonrpcProtocolVersion = (builder.jsonrpcProtocolVersion.isPresent() ? builder.jsonrpcProtocolVersion.get() : null);
			this.mcpProtocolVersion = (builder.mcpProtocolVersion.isPresent() ? builder.mcpProtocolVersion.get() : null);
			this.networkProtocolName = (builder.networkProtocolName.isPresent() ? builder.networkProtocolName.get() : null);
			this.networkProtocolVersion = (builder.networkProtocolVersion.isPresent() ? builder.networkProtocolVersion.get() : null);
			this.networkTransport = (builder.networkTransport.isPresent() ? builder.networkTransport.get() : null);
			this.mcpResourceUri = (builder.mcpResourceUri.isPresent() ? builder.mcpResourceUri.get() : null);
			
			this.mcpStat_ID = this.mcpMethodName;
		}
		

//        @Override
//        public String toString() {
//                return String.format(
//                                " ------- \n" 
//                + "Request Method (mandatory): [%s] \n" 
//                + "Scheme (mandatory): [%s] \n"
//                + "Network Protocol Name (optional): [%s] \n" 
//                + "Network Protocol Version (mandatory): [%s] \n" 
//                + "Server Name (mandatory): [%s] \n" 
//                + "Server Port (mandatory): [%d] \n"
//                + "HTTP Route (Optional: can be empty): [%s] \n" 
//                + "Response Status (Optional: can be -1): [%d] \n" 
//                + "Error Type(Optional - can be empty): [%s]",
//                                requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName, serverPort,
//                                httpRoute, responseStatus, errorType);
//        }
        
        /**
		 * @return the httpStat_ID
		 */
		public String getMcpStat_ID() {
			return mcpStat_ID;
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

		@Override
		public String toString() {
			return "McpStatAttributes [mcpStat_ID=" + mcpStat_ID + ", mcpMethodName=" + mcpMethodName + ", errorType="
					+ errorType + ", genAiPromptName=" + genAiPromptName + ", genAiToolName=" + genAiToolName
					+ ", rpcResponseStatusCode=" + rpcResponseStatusCode + ", genAiOperationName=" + genAiOperationName
					+ ", jsonrpcProtocolVersion=" + jsonrpcProtocolVersion + ", mcpProtocolVersion="
					+ mcpProtocolVersion + ", networkProtocolName=" + networkProtocolName + ", networkProtocolVersion="
					+ networkProtocolVersion + ", networkTransport=" + networkTransport + ", mcpResourceUri="
					+ mcpResourceUri + "]";
		}
        
        
        public static Builder builder() {
                return new Builder();
        }
        
        
        


		public static class Builder {

                private String mcpMethodName;

                
                /*
                 * Conditionally required as per HTTP Semantics Convention
                 */
                private Optional<String> errorType = Optional.ofNullable(null);
                private Optional<String> genAiPromptName = Optional.ofNullable(null);
                private Optional<String> genAiToolName = Optional.ofNullable(null);
                private Optional<String> rpcResponseStatusCode = Optional.ofNullable(null);

                /*
                 * Exception related fields are optional. We are unable to facilitate capturing
                 * Exceptions But we will leave it here. Additional Context : We can capture
                 * exceptions thrown by servlets by surrounding the the chainFilter with try
                 * catch. But we have no way of capturing application exception of
                 * Jaxrs/restfulws exceptions
                 */
                private Optional<String> genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri = Optional.ofNullable(null);

                /*
                 * Define a constructor with default protection so others do not call it directly and instead
                 * call the builder() method above.
                 */
                Builder() {
                }

                /**
                 * Builds an instance of {@link HttpStatAttributes} with values from this
                 * builder. Will validate and throw an {@link IllegalStateException} if the
                 * required fields are not filled.
                 * 
                 * @return Instance of {@link HttpStatAttributes}
                 * @throws IllegalStateException
                 */
                @FFDCIgnore(value = { IllegalStateException.class })
                public McpStatAttributes build() {
                        try {
                                return new McpStatAttributes(this);
                        } catch (IllegalStateException ise) {
                                //do nothing
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, String.format("Invalid HTTP Stats attributes : \n %s", toString()));
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

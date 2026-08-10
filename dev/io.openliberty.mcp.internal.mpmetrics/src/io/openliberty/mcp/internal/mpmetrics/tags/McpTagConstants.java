/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.mpmetrics.tags;

/**
 *
 */

public final class McpTagConstants {
        public static final String ERROR_TYPE = "error_type";
        public static final String GEN_AI_OPERATION_NAME = "gen_ai_operation_name";
        public static final String GEN_AI_PROMPT_NAME = "gen_ai_prompt_name";
        public static final String GEN_AI_TOOL_NAME = "gen_ai_tool_name";
        public static final String JSONRPC_PROTOCOL_VERSION="jsonrpc_protocol_version";
        public static final String MCP_METHOD_NAME = "mcp_method_name";
        public static final String MCP_PROTOCOL_VERSION = "mcp_protocol_version";
        public static final String MCP_RESOURCE_URI = "mcp_resource_uri";
        public static final String NETWORK_PROTOCOL_NAME = "network_protocol_name";
        public static final String NETWORK_PROTOCOL_VERSION = "network_protocol_version";
        public static final String NETWORK_TRANSPORT = "network_transport";
        public static final String RPC_RESPONSE_STATUS_CODE = "rpc_response_status_code";
        
        // MCP Metrics
        public static final String MCP_SERVER_OPERATION_DURATION_NAME = "mcp.server.operation.duration";
        public static final String MCP_SERVER_OPERATION_DURATION_DESC = "Duration of MCP operation calls.";

}

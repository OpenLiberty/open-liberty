/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests.parsing;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.exceptions.jsonrpc.McpResponseException;
import io.openliberty.mcp.internal.requests.RequestId;

/**
 * A partially parsed and unvalidated MCP request
 * <p>
 * All fields may be {@code null}
 *
 * @param acceptHeader the value of the Accept header
 * @param protocolVersion the protocol version from the MCP-Protocol-Version header, or {@code null} if it is not supported
 * @param sessionIdHeader the value of the Mcp-Session-Id header
 * @param requestTo the parsed JSON-RPC request, or {@code null} if the body could not be parsed
 * @param parseException the exception to throw when the request is fully parsed if the body could not be parsed, otherwise {@code null}
 * @param id the request id, or {@code null} if it is missing or invalid
 * @param method the request method, or {@code null} if it could not be parsed from the request
 */
@Trivial
public record PartialMcpOperationRequest(String acceptHeader,
                                         McpProtocolVersion protocolVersion,
                                         String sessionIdHeader,
                                         JsonRpcRequestTO requestTo,
                                         McpResponseException parseException,
                                         RequestId id,
                                         RequestMethod method) {

}

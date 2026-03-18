/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor;

public interface McpStatsMXBean {

	/*
	 * MCP Attributes
	 */
	public String getMcpMethodName();

	public String getErrorType();

	public String getGenAiPromptName();

	public String getGenAiToolName();

	public String getRpcResponseStatusCode() ;

	public String getGenAiOperationName() ;

	public String getJsonrpcProtocolVersion();

	public String getMcpProtocolVersion() ;

	public String getNetworkProtocolName() ;

	public String getNetworkProtocolVersion() ;

	public String getNetworkTransport() ;

	public String getMcpResourceUri() ;
	
	/*
	 * Metric values
	 */
	public long getCount();

	public double getDuration();
}

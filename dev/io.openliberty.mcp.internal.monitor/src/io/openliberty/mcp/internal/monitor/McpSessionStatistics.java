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

import com.ibm.websphere.monitor.meters.StatisticsMeter;
import com.ibm.websphere.monitor.meters.StatisticsReading;

import io.openliberty.mcp.internal.monitoring.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitoring.McpSessionStatAttributes;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;


public class McpSessionStatistics extends Meter implements McpSessionStatisticsMXBean {
    
    /*
     * Conditionally required as per HTTP Semantics Convention
     */ 
    private final String errorType;
    
    /*
     * Optional fields.
     * We are unable to facilitate capturing Exceptions
     * But we will leave it here.
     * Additional Context : We can capture  exceptions thrown by servlets
     * by surrounding the the chainFilter with try catch. But we have no way
     * of capturing application exception of Jaxrs/restfulws exceptions
     */
    private final String jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport;
	
	
	private Counter sessionCount;
	private StatisticsMeter sessionDuration;
	
	public McpSessionStatistics(McpSessionStatAttributes mcpStatAttributes) {
		this.errorType = mcpStatAttributes.getErrorType();
		this.jsonrpcProtocolVersion = mcpStatAttributes.getJsonrpcProtocolVersion();
		this.mcpProtocolVersion = mcpStatAttributes.getMcpProtocolVersion();
		this.networkProtocolName = mcpStatAttributes.getNetworkProtocolName();
		this.networkProtocolVersion = mcpStatAttributes.getNetworkProtocolVersion();
		this.networkTransport = mcpStatAttributes.getNetworkTransport();
		
		sessionCount = new Counter();
		sessionCount.setDescription("MCP sessions");
		
		sessionDuration = new StatisticsMeter();
		sessionDuration.setDescription("Duration of session");
		sessionDuration.setUnit("seconds");

	}

	public void incrementToolCallCountBy(int i) {
		sessionCount.incrementBy(i);
	}
	
	public void addToolTimeStat(long time) {
		sessionDuration.addDataPoint(time);
	}


	@Override
	public String getErrorType() {
		return errorType;
	}

	@Override
	public String getJsonrpcProtocolVersion() {
		return jsonrpcProtocolVersion;
	}

	@Override
	public String getMcpProtocolVersion() {
		return mcpProtocolVersion;
	}

	@Override
	public String getNetworkProtocolName() {
		return networkProtocolName;
	}

	@Override
	public String getNetworkProtocolVersion() {
		return networkProtocolVersion;
	}

	@Override
	public String getNetworkTransport() {
		return networkTransport;
	}


	@Override
	public long getCount() {
		return sessionCount.getCurrentValue();
	}

	@Override
	public double getDuration() {
		return sessionDuration.getTotal();
	}

}

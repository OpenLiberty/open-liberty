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
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import io.openliberty.mcp.monitor.McpOperationStatsMXBean;
import io.openliberty.mcp.internal.monitoring.internal.McpOperationStatAttributes;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;


/**
 * Records statistics for MCP (Model Context Protocol) operations.
 * <p>
 * This class tracks metrics for individual MCP operations, including call counts and durations.
 * A new instance is created for each unique combination of operation attributes (method name,
 * tool name, error type, etc.). All instances are managed by {@link McpStatsMonitorImpl}.
 * <p>
 * The statistics are exposed via JMX through the {@link McpOperationStatsMXBean} interface
 * and can be consumed by monitoring systems like MicroProfile Metrics and MicroProfile Telemetry.
 */
public class McpOperationStats extends Meter implements McpOperationStatsMXBean {
    private static final TraceComponent tc = Tr.register(McpOperationStats.class);

	private final String mcpMethodName;
    
    /*
     * Conditionally required fields for MCP operations
     */
    private final String errorType, genAiPromptName, genAiToolName, rpcResponseStatusCode;
    
    /*
     * Optional protocol and network attributes for the MCP operation
     */
    private final String genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri;
	
	
	private Counter toolCallCount;
	private StatisticsMeter toolCallRunDuration;
	
	public McpOperationStats(McpOperationStatAttributes mcpStatAttributes) {
		this.mcpMethodName = mcpStatAttributes.getMcpMethodName();
		this.errorType = mcpStatAttributes.getErrorType();
		this.genAiPromptName = mcpStatAttributes.getGenAiPromptName();
		this.genAiToolName = mcpStatAttributes.getGenAiToolName();
		this.rpcResponseStatusCode = mcpStatAttributes.getRpcResponseStatusCode();
		this.genAiOperationName = mcpStatAttributes.getGenAiOperationName();
		this.jsonrpcProtocolVersion = mcpStatAttributes.getJsonrpcProtocolVersion();
		this.mcpProtocolVersion = mcpStatAttributes.getMcpProtocolVersion();
		this.networkProtocolName = mcpStatAttributes.getNetworkProtocolName();
		this.networkProtocolVersion = mcpStatAttributes.getNetworkProtocolVersion();
		this.networkTransport = mcpStatAttributes.getNetworkTransport();
		this.mcpResourceUri = mcpStatAttributes.getMcpResourceUri();
		
		toolCallCount = new Counter();
		toolCallCount.setDescription(Tr.formatMessage(tc, "mcp.operation.count.description"));
		
		toolCallRunDuration = new StatisticsMeter();
		toolCallRunDuration.setDescription(Tr.formatMessage(tc, "mcp.operation.duration.description"));
		toolCallRunDuration.setUnit(Tr.formatMessage(tc, "mcp.metric.unit.nanoseconds"));

	}

	public void incrementToolCallCountBy(int i) {
		toolCallCount.incrementBy(i);
	}
	
	public void addToolTimeStat(long time) {
		toolCallRunDuration.addDataPoint(time);
	}

	@Override
	public String getMcpMethodName() {
		return mcpMethodName;
	}

	@Override
	public String getErrorType() {
		return errorType;
	}

	@Override
	public String getGenAiPromptName() {
		return genAiPromptName;
	}

	@Override
	public String getGenAiToolName() {
		return genAiToolName;
	}

	@Override
	public String getRpcResponseStatusCode() {
		return rpcResponseStatusCode;
	}

	@Override
	public String getGenAiOperationName() {
		return genAiOperationName;
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
	public String getMcpResourceUri() {
		return mcpResourceUri;
	}

	@Override
	public long getCount() {
		return toolCallCount.getCurrentValue();
	}

	@Override
	public double getDuration() {
		return toolCallRunDuration.getTotal();
	}

}

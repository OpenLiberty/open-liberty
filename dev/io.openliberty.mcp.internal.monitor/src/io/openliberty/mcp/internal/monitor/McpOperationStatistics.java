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

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.monitor.metrics.McpOperationStatAttributes;
import io.openliberty.mcp.monitor.McpOperationStatsMXBean;

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
public class McpOperationStatistics extends Meter implements McpOperationStatsMXBean {
    private static final TraceComponent tc = Tr.register(McpOperationStatistics.class);

    private final String mcpMethodName;

    /*
     * Conditionally required fields for MCP operations
     */
    private final String errorType, genAiPromptName, genAiToolName, rpcResponseStatusCode;

    /*
     * Optional protocol and network attributes for the MCP operation
     */
    private final String genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri;

    private Counter operationCount;
    private StatisticsMeter operationDuration;

    public McpOperationStatistics(McpOperationStatAttributes mcpStatAttributes) {
        this.mcpMethodName = mcpStatAttributes.mcpMethodName();
        this.errorType = mcpStatAttributes.errorType();
        this.genAiPromptName = mcpStatAttributes.genAiPromptName();
        this.genAiToolName = mcpStatAttributes.genAiToolName();
        this.rpcResponseStatusCode = mcpStatAttributes.rpcResponseStatusCode();
        this.genAiOperationName = mcpStatAttributes.genAiOperationName();
        this.jsonrpcProtocolVersion = mcpStatAttributes.jsonrpcProtocolVersion();
        this.mcpProtocolVersion = mcpStatAttributes.mcpProtocolVersion();
        this.networkProtocolName = mcpStatAttributes.networkProtocolName();
        this.networkProtocolVersion = mcpStatAttributes.networkProtocolVersion();
        this.networkTransport = mcpStatAttributes.networkTransport();
        this.mcpResourceUri = mcpStatAttributes.mcpResourceUri();

        operationCount = new Counter();
        operationCount.setDescription(Tr.formatMessage(tc, "mcp.operation.count.description"));

        operationDuration = new StatisticsMeter();
        operationDuration.setDescription(Tr.formatMessage(tc, "mcp.operation.duration.description"));
        operationDuration.setUnit("nanoseconds");

    }

    public void incrementOperationCountBy(int i) {
        operationCount.incrementBy(i);
    }

    public void addOperationTimeStat(long time) {
        operationDuration.addDataPoint(time);
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
        long count = operationCount.getCurrentValue();
        return count;
    }

    @Override
    public Counter getCountDetails() {
        return operationCount;
    }

    @Override
    public double getDuration() {
        return operationDuration.getTotal();
    }

    @Override
    public StatisticsMeter getDurationDetails() {
        return operationDuration;
    }

}

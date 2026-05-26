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

import io.openliberty.mcp.internal.monitoring.internal.McpSessionStatAttributes;
import io.openliberty.mcp.monitor.McpSessionStatsMXBean;

public class McpSessionStatistics extends Meter implements McpSessionStatsMXBean {

    private static final TraceComponent tc = Tr.register(McpSessionStatistics.class);

    /*
     * Conditionally required fields for MCP sessions
     */
    private final String errorType;

    /*
     * Optional protocol and network attributes for the MCP session
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

        // Total number of MCP sessions that have been created
        sessionCount = new Counter();
        sessionCount.setDescription(Tr.formatMessage(tc, "mcp.session.count.description"));

        sessionDuration = new StatisticsMeter();
        sessionDuration.setDescription(Tr.formatMessage(tc, "mcp.session.duration.description"));
        sessionDuration.setUnit("nanoseconds");

    }

    public void incrementSessionCountBy(int i) {
        sessionCount.incrementBy(i);
    }

    public void addSessionDurationStat(long time) {
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

// Made with Bob

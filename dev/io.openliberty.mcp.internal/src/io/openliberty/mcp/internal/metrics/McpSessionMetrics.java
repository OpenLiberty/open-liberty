/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.metrics;

import java.time.Instant;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.monitoring.McpSessionStatAttributes;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;
import io.openliberty.mcp.internal.sessions.McpSession;

/**
 *
 */
public final class McpSessionMetrics {
    private McpSession mcpSession;
    private McpTransport transport;

    private long startTimeNanos;
    private Instant startTIme;

    private String errorType;

    private static final TraceComponent tc = Tr.register(McpSessionMetrics.class);

    private McpSessionStatAttributes.Builder attributesBuilder;

    public McpSessionMetrics() {
        this.startTimeNanos = System.nanoTime();
        this.startTIme = Instant.now();
    }

    public McpSessionStatAttributes.Builder getAttributesBuilder() {
        return attributesBuilder;
    }

    public void setAttributesBuilder(McpSessionStatAttributes.Builder builder) {
        this.attributesBuilder = builder;
    }

    /**
     * @return the startTimeNanos
     */
    public long getDurationNanos() {
        return System.nanoTime() - startTimeNanos;
    }

    /**
     * @return the errorType
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * @param errorType the errorType to set
     */
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    /**
     * @return the transport
     */
    public McpTransport getTransport() {
        return transport;
    }

    public void setTransport(McpTransport transport) {
        this.transport = transport;
    }

    /**
     * @return the startTIme
     */
    public Instant getStartTIme() {
        return startTIme;
    }

    public static void sessionStarted(McpSessionMetrics metrics) {
        McpStatsMonitor monitor = McpStatsMonitorHolder.get();
        if (monitor == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Monitor is null in sessionStarted");
            }
            return;
        }

        monitor.recordSessionStart(metrics);
    }

    public static void sessionEnded(McpSessionMetrics metrics) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && metrics.getMcpSession() != null) {
            Tr.debug(tc, "sessionEnded hook called for session :" + metrics.getMcpSession().getSessionId());
        }

        McpStatsMonitor monitor = McpStatsMonitorHolder.get();
        if (monitor == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Monitor is null in sessionEnded");
            }
            return;
        }

        monitor.recordSessionEnd(metrics);
    }

    /**
     * @return the mcpSession
     */
    public McpSession getMcpSession() {
        return mcpSession;
    }

    /**
     * @param mcpSession the mcpSession to set
     */
    public void setMcpSession(McpSession mcpSession) {
        this.mcpSession = mcpSession;
    }
}
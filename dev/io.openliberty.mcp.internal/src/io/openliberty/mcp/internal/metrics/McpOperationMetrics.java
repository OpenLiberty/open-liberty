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

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;

/**
 * Captures metrics data for individual MCP (Model Context Protocol) operations.
 *
 * <p>This class tracks timing and metadata for discrete MCP operations such as tool calls,
 * prompts, resource requests, and protocol messages. Each operation represents a single
 * request-response cycle within an MCP session.
 *
 * <p>Key responsibilities:
 * <ul>
 * <li>Recording operation start time for duration calculation</li>
 * <li>Tracking operation-specific metadata (method name, tool name, status, errors)</li>
 * <li>Forwarding metrics to the monitoring system via {@link McpStatsMonitor}</li>
 * <li>Supporting both successful and failed operation outcomes</li>
 * </ul>
 *
 * <p>Usage pattern:
 *
 * <pre>
 * McpOperationMetrics metrics = new McpOperationMetrics();
 * metrics.setMethodName("tools/call");
 * metrics.setToolName("calculator");
 * metrics.setTransport(transport);
 * McpOperationMetrics.operationStarted(metrics);
 * // ... perform operation ...
 * metrics.setOutcome("ok", null); // or setOutcome("error", "timeout")
 * McpOperationMetrics.operationEnded(metrics);
 * </pre>
 *
 * @see McpStatsMonitor
 * @see McpOperationStatAttributes
 */
public final class McpOperationMetrics {
    private ExecutionRequestId executionRequestId;
    private McpTransport transport;

    private long startTimeNanos;

    private String methodName;
    private String toolName;
    private String status;
    private String errorType;
    private String appName;

    private final AtomicBoolean ended = new AtomicBoolean(false);

    private static final TraceComponent tc = Tr.register(McpOperationMetrics.class);

    public McpOperationMetrics() {
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * @return the start time in nanoseconds
     */
    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName the methodName to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return the toolName
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * @param toolName the toolName to set
     */
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
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
        if (transport != null) {
            this.appName = transport.getAppName();
        }
    }

    /**
     * @return the appName
     */
    public String getAppName() {
        return appName;
    }

    public void setOutcome(String status, String errorType) {
        this.status = status;
        this.errorType = errorType;
    }

    @Deprecated
    public static void operationStarted(McpOperationMetrics metrics) {}

    public static void operationEnded(McpOperationMetrics metrics) {
        // Validate that we have sufficient information to record metrics
        if (metrics.methodName == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "operationEnded called but methodName is null - skipping metrics recording");
            }
            return;
        }

        // Prevent recording metrics twice for the same operation
        if (!metrics.ended.compareAndSet(false, true)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "operationEnded already called for method: " + metrics.getMethodName() + " - skipping duplicate recording");
            }
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "operationEnded hook called for method: " + metrics.getMethodName());
        }

        McpStatsMonitor monitor = McpStatsMonitorHolder.get();

        if (monitor != null) {
            monitor.recordOperationEnd(metrics);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Monitor is null in operationEnded");
            }
        }
    }
}
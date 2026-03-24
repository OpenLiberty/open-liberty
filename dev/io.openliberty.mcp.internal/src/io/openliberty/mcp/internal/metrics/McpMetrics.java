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
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;
import io.openliberty.mcp.internal.requests.ExecutionRequestId;

/**
 *
 */
public final class McpMetrics {
    private ExecutionRequestId executionRequestId;
    private McpTransport transport;

    private long startTimeNanos;
    private Instant startTIme;

    private String methodName;
    private String toolName;
    private String status;
    private String errorType;

    private static final TraceComponent tc = Tr.register(McpMetrics.class);

    public McpMetrics() {
        this.startTimeNanos = System.nanoTime();
        this.startTIme = Instant.now();
    }

    /**
     * @return the startTimeNanos
     */
    public long getDurationNanos() {
        return System.nanoTime() - startTimeNanos;
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
     * @return the executionRequestId
     */
    public ExecutionRequestId getExecutionRequestId() {
        return executionRequestId;
    }

    public void setExecutionRequestId(ExecutionRequestId executionRequestId) {
        this.executionRequestId = executionRequestId;
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

    public void setOutcome(String status, String errorType) {
        this.status = status;
        this.errorType = errorType;
    }

    public static void operationStarted(McpMetrics metrics) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "operationStarted hook called for method: " + metrics.getMethodName());
        }

        McpStatsMonitor monitor = McpStatsMonitorHolder.get();
        if (monitor == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Monitor is null in operationStarted");
            }
            return;
        }

        monitor.recordOperationStart(metrics);
    }

    public static void operationEnded(McpMetrics metrics) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "operationEnded hook called for method: " + metrics.getMethodName());
        }

        McpStatsMonitor monitor = McpStatsMonitorHolder.get();
        if (monitor == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Monitor is null in operationEnded");
            }
            return;
        }

        monitor.recordOperationEnd(metrics);
    }
}
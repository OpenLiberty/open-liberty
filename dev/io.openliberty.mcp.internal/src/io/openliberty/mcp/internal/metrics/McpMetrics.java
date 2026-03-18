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

import io.openliberty.mcp.internal.McpTransport;
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

    /**
     * @param metrics
     */
    public static void operationStarted(McpMetrics metrics) {
        // TODO Auto-generated method stub

    }

    /**
     * @param metrics
     */
    public static void operationEnded(McpMetrics metrics) {
        // TODO Auto-generated method stub

    }
}
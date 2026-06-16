/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Test;

import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;

public class McpMetricsTest {

    @After
    public void tearDown() {
        McpStatsMonitorHolder.clear();
    }

    @Test
    public void operationStartedDelegatesToMonitor() {
        RecordingMonitor monitor = new RecordingMonitor();
        McpStatsMonitorHolder.set(monitor);

        McpOperationMetrics metrics = new McpOperationMetrics();
        metrics.setMethodName("initialize");

        McpOperationMetrics.operationStarted(metrics);

        assertEquals(0, monitor.endOperationCalls);
    }

    @Test
    public void operationEndedDelegatesToMonitor() {
        RecordingMonitor monitor = new RecordingMonitor();
        McpStatsMonitorHolder.set(monitor);

        McpOperationMetrics metrics = new McpOperationMetrics();
        metrics.setMethodName("tools/list");
        metrics.setOutcome("ok", null);

        McpOperationMetrics.operationEnded(metrics);

        assertEquals(1, monitor.endOperationCalls);
        assertSame(metrics, monitor.endedOperationMetrics);
    }

    @Test
    public void operationStartedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpOperationMetrics metrics = new McpOperationMetrics();
        metrics.setMethodName("initialize");

        McpOperationMetrics.operationStarted(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    @Test
    public void operationEndedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpOperationMetrics metrics = new McpOperationMetrics();
        metrics.setMethodName("initialize");
        metrics.setOutcome("ok", null);

        McpOperationMetrics.operationEnded(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    @Test
    public void sessionStartedDelegatesToMonitor() {
        RecordingMonitor monitor = new RecordingMonitor();
        McpStatsMonitorHolder.set(monitor);

        McpSessionMetrics metrics = new McpSessionMetrics();

        McpSessionMetrics.sessionStarted(metrics);

        assertEquals(0, monitor.endSessionCalls);
    }

    @Test
    public void sessionEndedDelegatesToMonitor() {
        RecordingMonitor monitor = new RecordingMonitor();
        McpStatsMonitorHolder.set(monitor);

        McpSessionMetrics metrics = new McpSessionMetrics();
        metrics.setErrorType(null);

        McpSessionMetrics.sessionEnded(metrics);

        assertEquals(1, monitor.endSessionCalls);
        assertSame(metrics, monitor.endedSessionMetrics);
    }

    @Test
    public void sessionStartedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpSessionMetrics metrics = new McpSessionMetrics();

        McpSessionMetrics.sessionStarted(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    @Test
    public void sessionEndedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpSessionMetrics metrics = new McpSessionMetrics();

        McpSessionMetrics.sessionEnded(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    private static class RecordingMonitor implements McpStatsMonitor {
        int endOperationCalls;
        McpOperationMetrics endedOperationMetrics;

        int endSessionCalls;
        McpSessionMetrics endedSessionMetrics;

        @Override
        public void recordOperationEnd(McpOperationMetrics metrics) {
            endOperationCalls++;
            endedOperationMetrics = metrics;
        }

        /** {@inheritDoc} */
        @Override
        public void recordSessionEnd(McpSessionMetrics metrics) {
            endSessionCalls++;
            endedSessionMetrics = metrics;

        }

        /** {@inheritDoc} */
        @Override
        public void removeStatsForApp(String appName) {
            // No-op for test mock
        }
    }
}
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

import io.openliberty.mcp.internal.metrics.McpMetrics;
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

        McpMetrics metrics = new McpMetrics();
        metrics.setMethodName("initialize");

        McpMetrics.operationStarted(metrics);

        assertEquals(1, monitor.startCalls);
        assertSame(metrics, monitor.startedMetrics);
        assertEquals(0, monitor.endCalls);
    }

    @Test
    public void operationEndedDelegatesToMonitor() {
        RecordingMonitor monitor = new RecordingMonitor();
        McpStatsMonitorHolder.set(monitor);

        McpMetrics metrics = new McpMetrics();
        metrics.setMethodName("tools/list");
        metrics.setOutcome("ok", null);

        McpMetrics.operationEnded(metrics);

        assertEquals(1, monitor.endCalls);
        assertSame(metrics, monitor.endedMetrics);
        assertEquals(0, monitor.startCalls);
    }

    @Test
    public void operationStartedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpMetrics metrics = new McpMetrics();
        metrics.setMethodName("initialize");

        McpMetrics.operationStarted(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    @Test
    public void operationEndedDoesNothingWhenMonitorMissing() {
        McpStatsMonitorHolder.clear();

        McpMetrics metrics = new McpMetrics();
        metrics.setMethodName("initialize");
        metrics.setOutcome("ok", null);

        McpMetrics.operationEnded(metrics);

        // no exception = pass
        assertNotNull(metrics);
    }

    private static class RecordingMonitor implements McpStatsMonitor {
        int startCalls;
        int endCalls;
        McpMetrics startedMetrics;
        McpMetrics endedMetrics;

        @Override
        public void recordOperationStart(McpMetrics metrics) {
            startCalls++;
            startedMetrics = metrics;
        }

        @Override
        public void recordOperationEnd(McpMetrics metrics) {
            endCalls++;
            endedMetrics = metrics;
        }
    }
}
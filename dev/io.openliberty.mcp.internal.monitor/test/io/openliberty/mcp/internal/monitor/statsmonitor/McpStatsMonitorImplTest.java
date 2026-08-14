/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.statsmonitor;


import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Test;

import io.openliberty.mcp.internal.monitor.McpStatsMonitorImpl;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;

public class McpStatsMonitorImplTest {

    @After
    public void tearDown() {
        McpStatsMonitorHolder.clear();
    }

    @Test
    public void constructorRegistersHolder() {
        McpStatsMonitorImpl impl = new McpStatsMonitorImpl();

        // Constructor should automatically register in holder
        assertSame(impl, McpStatsMonitorHolder.get());
    }

    @Test
    public void holderCanBeCleared() {
        McpStatsMonitorImpl impl = new McpStatsMonitorImpl();

        // Verify it's registered
        assertSame(impl, McpStatsMonitorHolder.get());
        
        // Clear the holder
        McpStatsMonitorHolder.clear();

        assertNull(McpStatsMonitorHolder.get());
    }
}
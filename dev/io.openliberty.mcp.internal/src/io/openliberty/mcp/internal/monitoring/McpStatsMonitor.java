/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitoring;

import java.time.Duration;

/**
 *
 */

public interface McpStatsMonitor {

    /**
     *
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
    void updateMcpStatDuration(McpStatAttributes.Builder builder, Duration duration, String appName);

    /**
     * @return the tl_mcpStatsBuilder
     */
    ThreadLocal<McpStatAttributes.Builder> getTl_mcpStatsBuilder();

    /**
     * @return the tl_startNanos
     */
    ThreadLocal<Long> getTl_startNanos();
}
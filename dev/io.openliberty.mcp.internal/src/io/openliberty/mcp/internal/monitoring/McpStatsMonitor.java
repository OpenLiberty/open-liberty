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

import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;

/**
 *
 */

public interface McpStatsMonitor {

    void recordOperationEnd(McpOperationMetrics metrics);

    void recordSessionEnd(McpSessionMetrics metrics);

    /**
     * Removes all statistics (MBeans, metrics, and internal data structures) for the specified application.
     * This method is called when an application is unloaded to prevent memory leaks.
     *
     * @param appName the name of the application whose statistics should be removed
     */
    void removeStatsForApp(String appName);
}
/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.metrics;

import java.time.Duration;



/**
 * Adapter interface for forwarding MCP (Model Context Protocol) metrics to different metrics runtime implementations.
 *
 * <p>This interface is intended to be implemented as an OSGi service component by metrics runtime bundles
 * (e.g., MicroProfile Metrics, MicroProfile Telemetry). The {@link MetricsManager} discovers and invokes
 * all registered implementations to forward MCP operation and session metrics.
 *
 * <p>Implementations should be registered as OSGi services with appropriate service ranking if multiple
 * metrics runtimes are active simultaneously.
 *
 * @see MetricsManager
 */
public interface McpMetricAdapter {
	
	/**
	 * Updates MCP operation metrics in the implementing metrics runtime.
	 *
	 * <p>This method is called when an MCP operation (e.g., tool call, prompt execution) completes,
	 * providing the operation attributes and duration for metrics recording.
	 *
	 * @param mcpStatAttributes the attributes of the completed MCP operation, including method name,
	 *                          tool name, response status, and protocol information
	 * @param duration the duration of the operation
	 */
	public void updateMcpOperationMetrics(McpOperationStatAttributes mcpStatAttributes, Duration duration);
	
	/**
	 * Updates MCP session metrics in the implementing metrics runtime.
	 *
	 * <p>This method is called when an MCP session completes, providing the session attributes
	 * and duration for metrics recording.
	 *
	 * @param mcpStatAttributes the attributes of the completed MCP session, including session ID
	 *                          and related metadata
	 * @param duration the duration of the session
	 */
	public void updateMcpSessionMetrics(McpSessionStatAttributes mcpStatAttributes, Duration duration);
	
	/**
	 * Removes all metrics associated with the specified application.
	 *
	 * <p>This method is called when an application is unloaded to clean up metrics and prevent memory leaks.
	 * Implementations should remove all operation and session metrics that were created for the specified application.
	 *
	 * @param appName the name of the application being unloaded
	 */
	public void removeMetricsForApp(String appName);

}
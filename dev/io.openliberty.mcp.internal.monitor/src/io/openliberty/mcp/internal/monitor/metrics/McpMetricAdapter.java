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

import io.openliberty.mcp.internal.monitoring.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitoring.McpSessionStatAttributes;



/**
 * Intended to be a service-component.
 * Implemented by subsequent Metric run-times in their respective bundles.
 */
public interface McpMetricAdapter {
	
	/**
	 * Given the HttpStatAttributes, update the mcp metric of the respective Metrics runtime
	 * 
	 * @param httpStatAttributes. Class = McpStatAttributes
	 * @param duration
	 */
	public void updateMcpOperationMetrics(McpOperationStatAttributes mcpStatAttributes, Duration duration);
	
	/**
	 * Given the HttpStatAttributes, update the mcp metric of the respective Metrics runtime
	 * 
	 * @param httpStatAttributes. Class = McpStatAttributes
	 * @param duration
	 */
	public void updateMcpSessionMetrics(McpSessionStatAttributes mcpStatAttributes, Duration duration);

}
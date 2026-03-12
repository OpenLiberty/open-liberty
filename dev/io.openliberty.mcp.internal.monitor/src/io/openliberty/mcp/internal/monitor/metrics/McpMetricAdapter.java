package io.openliberty.mcp.internal.monitor.metrics;

import java.time.Duration;

import io.openliberty.mcp.internal.monitor.McpStatAttributes;



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
	public void updateMcpMetrics(McpStatAttributes mcpStatAttributes, Duration duration);

}
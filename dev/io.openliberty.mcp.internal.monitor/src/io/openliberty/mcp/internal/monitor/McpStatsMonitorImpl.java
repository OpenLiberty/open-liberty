/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.mcp.internal.metrics.McpMetrics;
import io.openliberty.mcp.internal.monitor.metrics.MetricsManager;
import io.openliberty.mcp.internal.monitoring.McpStatAttributes;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Activate;

/**
 *
 */
@Monitor(group = "MCP")
@Component(service = McpStatsMonitor.class, immediate = true)
public class McpStatsMonitorImpl extends StatisticActions implements McpStatsMonitor  {

	private static final TraceComponent tc = Tr.register(McpStatsMonitorImpl.class);

	private static final ThreadLocal<McpStatAttributes.Builder> tl_mcpStatsBuilder = new ThreadLocal<McpStatAttributes.Builder>();
	private static final  ThreadLocal<Long> tl_startNanos = new ThreadLocal<Long>();
	
	private static final ConcurrentHashMap<String,Set<String>> appNameToStat = new ConcurrentHashMap<String,Set<String>>();
	
	private static McpStatsMonitorImpl instance;

	/*
	 * Instance block to create singleton.
	 * The "Liberty-Monitoring-Components" in the bnd.bnd
	 * specifies the monitor runtime to create an instance
	 * of this class. We'll leverage that to
	 * create the singleton.
	 * 
	 * Unconventional as we well set "this" particular instance.
	 */
	{
		if (instance == null) {
			Tr.debug(tc, "McpStatsMonitor Sucessfully linked");
			instance = this;
		} else {
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("Multiple attempts to create %s. We already have an instance", McpStatsMonitorImpl.class.getName()));
			}
		}

	}

	public static McpStatsMonitorImpl getInstance() {
		if (instance != null) {
			return instance;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("No instance of %s found", McpStatsMonitorImpl.class.getName()));
			}

		}
		return null;
	}

	@PublishedMetric
	public MeterCollection<McpStats> McpStatsCollection = new MeterCollection<McpStats>("McpMetrics", this);


    /**
     * 
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateMcpStatDuration(McpStatAttributes.Builder builder, Duration duration, String appName) {

		McpStatAttributes mcpStatsAttributes;
		
		mcpStatsAttributes = builder.build();
		if (mcpStatsAttributes == null) return;
		
		/*
		 * Create and/or update MBean
		 */
		String keyID = mcpStatsAttributes.getMcpStat_ID();
				

		McpStats mcpStats = McpStatsCollection.get(keyID);
		if (mcpStats == null) {
			mcpStats = initializeMcpStat(keyID, mcpStatsAttributes, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (mcpStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		mcpStats.addToolTimeStat(duration.toNanos());
		
		
		if (MetricsManager.getInstance() != null ) {
			MetricsManager.getInstance(). updateMcpToolDurationMetrics(mcpStatsAttributes, duration);
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No Available Metric runtimes to forward Mcp stats to.");
			}
		}
	}

	private synchronized McpStats initializeMcpStat(String key, McpStatAttributes statAttri, String appName) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (McpStatsCollection.get(key) != null) {
			return McpStatsCollection.get(key);
		}

		McpStats mcpMetricStats = new McpStats(statAttri);
		McpStatsCollection.put(key, mcpMetricStats);
		
		//Shut down by monitor-1.0 filter attribute
		if (McpStatsCollection.get(key) == null) {
			return null;
		}
		
		/*
		 * null means from server.
		 * Specifically splash page.
		 * 
		 * Add to appName -> stat cache
		 */
		if (appName != null) {
			appNameToStat.compute(appName, (appNameKey, currValSet) -> {
				if (currValSet == null) {
					HashSet<String> hs = new HashSet<String>();
					hs.add(key);
					return hs;
				} else {
					currValSet.add(key);
					return currValSet;
				}
			});
		}
		
		return mcpMetricStats;
	}
	
	
	public void removeStat(String appName) {
		Set<String> retSet = appNameToStat.get(appName);
		if (retSet != null) {
			for (String statName : retSet) {
				McpStatsCollection.remove(statName);
			}
		}
	}

	@Activate
	public void activate() {
	    McpStatsMonitorHolder.set(this);
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "McpStatsMonitorImpl registered with McpStatsMonitorHolder");
	    }
	}

	@Deactivate
	public void deactivate() {
	    McpStatsMonitorHolder.clear();
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "McpStatsMonitorImpl cleared from McpStatsMonitorHolder");
	    }
	}
	
	@Override
	public void recordOperationStart(McpMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Recording operation start for method: " + metrics.getMethodName());
	    }

	    tl_mcpStatsBuilder.remove();
	    tl_startNanos.remove();

	    tl_startNanos.set(System.nanoTime());

	    McpStatAttributes.Builder builder = McpStatAttributes.builder();
	    builder.withMcpMethodName(metrics.getMethodName());

	    if (metrics.getToolName() != null) {
	        builder.withGenAiToolName(Optional.of(metrics.getToolName()));
	    }

	    if (metrics.getTransport() != null) {
	        try {
	            if (metrics.getTransport().getMcpRequest() != null) {
	                String jsonrpcVersion = metrics.getTransport().getMcpRequest().jsonrpc();
	                if (jsonrpcVersion != null) {
	                    builder.withJsonrpcProtocolVersion(Optional.of(jsonrpcVersion));
	                }
	            }

	            if (metrics.getTransport().getProtocolVersion() != null) {
	                builder.withMcpProtocolVersion(Optional.of(metrics.getTransport().getProtocolVersion().toString()));
	            }

	            if (metrics.getTransport().getReq() != null) {
	                String protocol = metrics.getTransport().getReq().getProtocol();
	                if (protocol != null) {
	                    String[] protocolParts = protocol.split("/");
	                    if (protocolParts.length >= 2) {
	                        builder.withNetworkProtocolName(Optional.of(protocolParts[0]));
	                        builder.withNetworkProtocolVersion(Optional.of(protocolParts[1]));
	                    }
	                }
	            }

	            builder.withNetworkTransport(Optional.of("tcp"));
	        } catch (Exception e) {
	            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	                Tr.debug(tc, "Error extracting transport information: " + e.getMessage());
	            }
	        }
	    }

	    tl_mcpStatsBuilder.set(builder);
	}

	@Override
	public void recordOperationEnd(McpMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Recording operation end for method: " + metrics.getMethodName());
	        Tr.debug(tc, "MCP status=" + metrics.getStatus() + ", errorType=" + metrics.getErrorType());

	    }

	    try {
	        McpStatAttributes.Builder builder = tl_mcpStatsBuilder.get();
	        Long startNanos = tl_startNanos.get();

	        if (builder == null || startNanos == null) {
	            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	                Tr.debug(tc, "Missing MCP monitor state. Operation start may not have been recorded.");
	            }
	            return;
	        }

	        long elapsedNanos = System.nanoTime() - startNanos;
	        Duration duration = Duration.ofNanos(elapsedNanos);

	        if (metrics.getStatus() != null) {
	            builder.withRpcResponseStatusCode(Optional.of(metrics.getStatus()));
	        }

	        if (metrics.getErrorType() != null) {
	            builder.withErrorType(Optional.of(metrics.getErrorType()));
	        }

	        updateMcpStatDuration(builder, duration, null);
	    } finally {
	        tl_mcpStatsBuilder.remove();
	        tl_startNanos.remove();
	    }
	}
	
	

}
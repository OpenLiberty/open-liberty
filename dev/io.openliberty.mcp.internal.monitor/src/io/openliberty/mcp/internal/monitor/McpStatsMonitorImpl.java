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
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
import io.openliberty.mcp.internal.monitor.metrics.MetricsManager;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;
import io.openliberty.mcp.internal.monitoring.internal.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitoring.internal.McpSessionStatAttributes;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Statistics Monitor implementation that tracks operation and session metrics.
 * 
 * This class uses the {@code @Monitor} annotation which causes the Liberty monitoring framework
 * to instantiate this class and register it as an OSGi service (via Liberty-Monitoring-Components
 * in bnd.bnd). The {@code @PublishedMetric} annotations on the MeterCollection fields cause the
 * monitoring framework to automatically register MBeans for statistics tracking.
 * 
 */
@Monitor(group = "MCP")
public class McpStatsMonitorImpl extends StatisticActions implements McpStatsMonitor  {
	
	private static final ServiceCaller<MetricsManager> metricsManagerService = new ServiceCaller<>(McpStatsMonitorImpl.class, MetricsManager.class);
	
	private static McpStatsMonitorImpl instance;

	private static final TraceComponent tc = Tr.register(McpStatsMonitorImpl.class);

	private static final ConcurrentHashMap<String, Set<McpOperationStatAttributes>> appNameToOperationStats = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Set<McpSessionStatAttributes>> appNameToSessionStats = new ConcurrentHashMap<>();

	@PublishedMetric
	public MeterCollection<McpOperationStatistics> mcpOperationStatsCollection = new MeterCollection<McpOperationStatistics>("McpOperation", this);

	@PublishedMetric
	public MeterCollection<McpSessionStatistics> mcpSessionStatsCollection = new MeterCollection<McpSessionStatistics>("McpSession", this);
	
	/**
	 * Generates a unique String ID for MeterCollection from operation stat attributes.
	 * MeterCollection requires String keys for JMX ObjectName registration. The toString() method
	 * creates a JMX-safe identifier by joining all non-null attribute values with underscores.
	 */
	private static String generateOperationStatId(McpOperationStatAttributes attrs) {
		return attrs.toString();
	}
	
	/**
	 * Generates a unique String ID for MeterCollection from session stat attributes.
	 * MeterCollection requires String keys for JMX ObjectName registration. The toString() method
	 * creates a JMX-safe identifier by joining all non-null attribute values with underscores.
	 */
	private static String generateSessionStatId(McpSessionStatAttributes attrs) {
		return attrs.toString();
	}


    /**
     * 
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateMcpOperationStatDuration(McpOperationStatAttributes.Builder builder, Duration duration, String appName) {
		McpOperationStatAttributes mcpStatsAttributes;
		
		mcpStatsAttributes = builder.build();
		if (mcpStatsAttributes == null) {
			return;
		}
		
		/*
		 * Create and/or update MBean
		 */
		String statId = generateOperationStatId(mcpStatsAttributes);
		McpOperationStatistics mcpStats = mcpOperationStatsCollection.get(statId);
		if (mcpStats == null) {
			mcpStats = initializeMcpOperationStat(mcpStatsAttributes, statId, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (mcpStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		mcpStats.addToolTimeStat(duration.toNanos());
		
		
		metricsManagerService.run(metricsManager -> {
			metricsManager.updateMcpOperationDurationMetrics(mcpStatsAttributes, duration);
			return null;
		});
	}
	
	/**
     * 
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateMcpSessionStatDuration(McpSessionStatAttributes.Builder builder, Duration duration, String appName) {
		McpSessionStatAttributes mcpStatsAttributes;
		
		mcpStatsAttributes = builder.build();
		if (mcpStatsAttributes == null) {
			return;
		}
		
		/*
		 * Create and/or update MBean
		 */
		String statId = generateSessionStatId(mcpStatsAttributes);
		McpSessionStatistics mcpStats = mcpSessionStatsCollection.get(statId);
		if (mcpStats == null) {
			mcpStats = initializeMcpSessionStat(mcpStatsAttributes, statId, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (mcpStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		mcpStats.addSessionDurationStat(duration.toNanos());
		
		
		metricsManagerService.run(metricsManager -> {
			metricsManager.updateMcpSessionDurationMetrics(mcpStatsAttributes, duration);
			return null;
		});
	}

	private McpOperationStatistics initializeMcpOperationStat(McpOperationStatAttributes statAttri, String statId, String appName) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "initializeMcpOperationStat", statAttri);
		}
		
		synchronized(this) {
			/*
			 * Check again it was added, thread that was blocking may have been adding it
			 */
			if (mcpOperationStatsCollection.get(statId) != null) {
				return mcpOperationStatsCollection.get(statId);
			}

			McpOperationStatistics mcpMetricStats = new McpOperationStatistics(statAttri);
			mcpOperationStatsCollection.put(statId, mcpMetricStats);

			//Shut down by monitor-1.0 filter attribute
			if (mcpOperationStatsCollection.get(statId) == null) {
				return null;
			}
			
			/*
			 * null means from server.
			 * Specifically splash page.
			 *
			 * Add to appName -> stat cache
			 */
			if (appName != null) {
				appNameToOperationStats.computeIfAbsent(appName, k -> new HashSet<>()).add(statAttri);
			}
			
			return mcpMetricStats;
		}
	}
	
	private McpSessionStatistics initializeMcpSessionStat(McpSessionStatAttributes statAttri, String statId, String appName) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "initializeMcpSessionStat", statAttri);
		}
		
		synchronized(this) {
			/*
			 * Check again it was added, thread that was blocking may have been adding it
			 */
			if (mcpSessionStatsCollection.get(statId) != null) {
				return mcpSessionStatsCollection.get(statId);
			}

			McpSessionStatistics mcpMetricStats = new McpSessionStatistics(statAttri);
			mcpSessionStatsCollection.put(statId, mcpMetricStats);

			//Shut down by monitor-1.0 filter attribute
			if (mcpSessionStatsCollection.get(statId) == null) {
				return null;
			}
			
			/*
			 * null means from server.
			 * Specifically splash page.
			 *
			 * Add to appName -> stat cache
			 */
			if (appName != null) {
				appNameToSessionStats.computeIfAbsent(appName, k -> new HashSet<>()).add(statAttri);
			}
			
			return mcpMetricStats;
		}
	}
	
	
	/**
	 * Removes all statistics (both operation and session) associated with the specified application.
	 * This method is called when an application is unloaded to clean up MBeans and prevent memory leaks.
	 *
	 * @param appName The name of the application being unloaded
	 */
	@Override
	public void removeStatsForApp(String appName) {
		Set<McpOperationStatAttributes> operationStats = appNameToOperationStats.remove(appName);
		Set<McpSessionStatAttributes> sessionStats = appNameToSessionStats.remove(appName);
		
		int removedCount = 0;
		
		if (operationStats != null) {
			for (McpOperationStatAttributes statAttri : operationStats) {
				String statId = generateOperationStatId(statAttri);
				mcpOperationStatsCollection.remove(statId);
				removedCount++;
			}
		}
		
		if (sessionStats != null) {
			for (McpSessionStatAttributes statAttri : sessionStats) {
				String statId = generateSessionStatId(statAttri);
				mcpSessionStatsCollection.remove(statId);
				removedCount++;
			}
		}
		
		if (removedCount > 0) {
			// Notify metrics adapters to clean up their metrics
			metricsManagerService.run(metricsManager -> {
				metricsManager.removeMetricsForApp(appName);
				return null;
			});
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "Removed " + removedCount + " statistics for application: " + appName);
			}
		}
	}
	
	/**
	 * Removes all MBeans when MCP monitoring is disabled, but keeps the tracking data
	 * so MBeans can be re-registered if monitoring is re-enabled.
	 * This is different from removeStatsForApp which is called when an app is unloaded.
	 */
	public void removeAllMBeansForMonitoringDisabled() {
		int removedCount = 0;
		
		// Remove all operation MBeans but keep tracking data
		for (Set<McpOperationStatAttributes> operationStats : appNameToOperationStats.values()) {
			if (operationStats != null) {
				for (McpOperationStatAttributes statAttri : operationStats) {
					String statId = generateOperationStatId(statAttri);
					mcpOperationStatsCollection.remove(statId);
					removedCount++;
				}
			}
		}
		
		// Remove all session MBeans but keep tracking data
		for (Set<McpSessionStatAttributes> sessionStats : appNameToSessionStats.values()) {
			if (sessionStats != null) {
				for (McpSessionStatAttributes statAttri : sessionStats) {
					String statId = generateSessionStatId(statAttri);
					mcpSessionStatsCollection.remove(statId);
					removedCount++;
				}
			}
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "Removed " + removedCount + " MBeans when MCP monitoring was disabled (tracking data preserved for re-registration)");
		}
		
		// Notify metrics adapters to clean up their metrics
		metricsManagerService.run(metricsManager -> {
			for (String appName : appNameToOperationStats.keySet()) {
				metricsManager.removeMetricsForApp(appName);
			}
			return null;
		});
	}
	
	/**
	 * Re-registers all MBeans for all tracked applications.
	 * This is called when MCP monitoring is re-enabled after being disabled.
	 */
	public void reregisterAllMBeans() {
		int registeredCount = 0;
		
		// Re-register operation statistics MBeans for all apps
		for (Map.Entry<String, Set<McpOperationStatAttributes>> entry : appNameToOperationStats.entrySet()) {
			String appName = entry.getKey();
			Set<McpOperationStatAttributes> operationStats = entry.getValue();
			
			if (operationStats != null) {
				for (McpOperationStatAttributes statAttri : operationStats) {
					String statId = generateOperationStatId(statAttri);
					// Always re-register since MBeans were removed when monitoring was disabled
					McpOperationStatistics stat = initializeMcpOperationStat(statAttri, statId, appName);
					if (stat != null) {
						registeredCount++;
					}
				}
			}
		}
		
		// Re-register session statistics MBeans for all apps
		for (Map.Entry<String, Set<McpSessionStatAttributes>> entry : appNameToSessionStats.entrySet()) {
			String appName = entry.getKey();
			Set<McpSessionStatAttributes> sessionStats = entry.getValue();
			
			if (sessionStats != null) {
				for (McpSessionStatAttributes statAttri : sessionStats) {
					String statId = generateSessionStatId(statAttri);
					// Always re-register since MBeans were removed when monitoring was disabled
					McpSessionStatistics stat = initializeMcpSessionStat(statAttri, statId, appName);
					if (stat != null) {
						registeredCount++;
					}
				}
			}
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "Re-registered " + registeredCount + " MBeans after MCP monitoring was re-enabled");
		}
	}

	/**
		* Constructor called by the monitoring framework when this class is instantiated.
		* Sets this instance in the holder to allow the io.openliberty.mcp.internal bundle
		* to access it without creating a cyclic dependency.
		*/
	/**
	 * Instance block to create singleton.
	 * The "Liberty-Monitoring-Components" in the bnd.bnd
	 * specifies the monitor runtime to create an instance
	 * of this class. We'll leverage that to create the singleton.
	 */
	{
		if (instance == null) {
			instance = this;
			// Register with holder for cross-bundle access
			McpStatsMonitorHolder.set(this);
			Tr.info(tc, "CWMCP0001I: McpStatsMonitorImpl instantiated by monitoring framework");
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Multiple attempts to create McpStatsMonitorImpl. We already have an instance");
			}
		}
	}

	public McpStatsMonitorImpl() {
		// Instance block above handles singleton registration
	}
	
	public static McpStatsMonitorImpl getInstance() {
		if (instance != null) {
			return instance;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No instance of McpStatsMonitorImpl found");
			}
		}
		return null;
	}
	
	/**
	 * Returns a set of all application names currently being tracked for statistics.
	 * This combines both operation and session statistics tracking.
	 *
	 * @return Set of application names
	 */
	public static Set<String> getTrackedAppNames() {
		Set<String> allAppNames = new HashSet<>();
		allAppNames.addAll(appNameToOperationStats.keySet());
		allAppNames.addAll(appNameToSessionStats.keySet());
		return allAppNames;
	}
	
	/**
	 * Clear the singleton instance. This is primarily for testing purposes.
	 */
	static void clearInstance() {
		instance = null;
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "McpStatsMonitorImpl singleton instance cleared");
		}
	}
	
	@Override
	public void recordOperationStart(McpOperationMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(this, tc, "Recording operation start for method: " + metrics.getMethodName());
	    }

	    McpOperationStatAttributes.Builder builder = McpOperationStatAttributes.builder();
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
	                builder.withMcpProtocolVersion(Optional.of(metrics.getTransport().getProtocolVersion().getVersion()));
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
	                Tr.debug(this, tc, "Error extracting transport information: " + e.getMessage());
	            }
	        }
	    }

	    // Store the builder in the metrics object so it's available when recordOperationEnd is called,
	    // even if that happens on a different thread (e.g., async operations)
	    metrics.setAttributesBuilder(builder);
	}

	@Override
	public void recordOperationEnd(McpOperationMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(this, tc, "Recording operation end for method: " + metrics.getMethodName());
	        Tr.debug(this, tc, "MCP status=" + metrics.getStatus() + ", errorType=" + metrics.getErrorType());
	    }

	    McpOperationStatAttributes.Builder builder = metrics.getAttributesBuilder();
	    
	    if (builder == null) {
	        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(this, tc, "Missing MCP monitor state. Operation start may not have been recorded.");
	        }
	        return;
	    }

	    long elapsedNanos = System.nanoTime() - metrics.getStartTimeNanos();
	    Duration duration = Duration.ofNanos(elapsedNanos);

	    if (metrics.getStatus() != null) {
	        builder.withRpcResponseStatusCode(Optional.of(metrics.getStatus()));
	    }

	    if (metrics.getErrorType() != null) {
	        builder.withErrorType(Optional.of(metrics.getErrorType()));
	    }

	    updateMcpOperationStatDuration(builder, duration, metrics.getAppName());
	}

	@Override
	public void recordSessionStart(McpSessionMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && metrics.getMcpSession() != null) {
	        Tr.debug(this, tc, "Recording session start for session: " + metrics.getMcpSession().getSessionId());
	    }

	    McpSessionStatAttributes.Builder builder = McpSessionStatAttributes.builder();

	    if (metrics.getTransport() != null) {
	        try {
	            if (metrics.getTransport().getMcpRequest() != null) {
	                String jsonrpcVersion = metrics.getTransport().getMcpRequest().jsonrpc();
	                if (jsonrpcVersion != null) {
	                    builder.withJsonrpcProtocolVersion(Optional.of(jsonrpcVersion));
	                }
	            }

	            if (metrics.getTransport().getProtocolVersion() != null) {
	                builder.withMcpProtocolVersion(Optional.of(metrics.getTransport().getProtocolVersion().getVersion()));
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
	                Tr.debug(this, tc, "Error extracting transport information: " + e.getMessage());
	            }
	        }
	    }
	    
	    metrics.setAttributesBuilder(builder);
	}

	@Override
	public void recordSessionEnd(McpSessionMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(this, tc, "MCP errorType=" + metrics.getErrorType());

	    }

	       McpSessionStatAttributes.Builder builder = metrics.getAttributesBuilder();

	       if (builder == null || metrics.getStartTIme() == null) {
	           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	               Tr.debug(this, tc, "Missing MCP monitor state. Operation start may not have been recorded.");
	           }
	           return;
	       }

	       long elapsedNanos = metrics.getDurationNanos();
	       Duration duration = Duration.ofNanos(elapsedNanos);

	       if (metrics.getErrorType() != null) {
	           builder.withErrorType(Optional.of(metrics.getErrorType()));
	       }

	       updateMcpSessionStatDuration(builder, duration, metrics.getAppName());
	}
	
	

}
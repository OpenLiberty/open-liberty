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

import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
import io.openliberty.mcp.internal.monitor.metrics.MetricsManager;
import io.openliberty.mcp.internal.monitoring.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitoring.McpSessionStatAttributes;
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

	private static final ThreadLocal<McpOperationStatAttributes.Builder> tl_mcpOperationStatsBuilder = new ThreadLocal<McpOperationStatAttributes.Builder>();
	private static final  ThreadLocal<Long> tl_oerationStartNanos = new ThreadLocal<Long>();

	
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
	public MeterCollection<McpOperationStats> McpOperationStatsCollection = new MeterCollection<McpOperationStats>("McpOperationMetrics", this);
	
	@PublishedMetric
	public MeterCollection<McpSessionStatistics> McpSessionStatsCollection = new MeterCollection<McpSessionStatistics>("McpSession", this);


    /**
     * 
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateMcpOperationStatDuration(McpOperationStatAttributes.Builder builder, Duration duration, String appName) {

		McpOperationStatAttributes mcpStatsAttributes;
		
		mcpStatsAttributes = builder.build();
		if (mcpStatsAttributes == null) return;
		
		/*
		 * Create and/or update MBean
		 */
		String keyID = mcpStatsAttributes.getMcpStat_ID();
				

		McpOperationStats mcpStats = McpOperationStatsCollection.get(keyID);
		if (mcpStats == null) {
			mcpStats = initializeMcpOperationStat(keyID, mcpStatsAttributes, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (mcpStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		mcpStats.addToolTimeStat(duration.toNanos());
		
		
		if (MetricsManager.getInstance() != null ) {
			MetricsManager.getInstance(). updateMcpOperationDurationMetrics(mcpStatsAttributes, duration);
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No Available Metric runtimes to forward Mcp stats to.");
			}
		}
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
		if (mcpStatsAttributes == null) return;
		
		/*
		 * Create and/or update MBean
		 */
		String keyID = mcpStatsAttributes.getMcpStat_ID();
				

		McpSessionStatistics mcpStats = McpSessionStatsCollection.get(keyID);
		if (mcpStats == null) {
			mcpStats = initializeMcpSessionStat(keyID, mcpStatsAttributes, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (mcpStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		mcpStats.addToolTimeStat(duration.toNanos());
		
		
		if (MetricsManager.getInstance() != null ) {
			MetricsManager.getInstance(). updateMcpSessionDurationMetrics(mcpStatsAttributes, duration);
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No Available Metric runtimes to forward Mcp stats to.");
			}
		}
	}

	private synchronized McpOperationStats initializeMcpOperationStat(String key, McpOperationStatAttributes statAttri, String appName) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (McpOperationStatsCollection.get(key) != null) {
			return McpOperationStatsCollection.get(key);
		}

		McpOperationStats mcpMetricStats = new McpOperationStats(statAttri);
		McpOperationStatsCollection.put(key, mcpMetricStats);
		
		//Shut down by monitor-1.0 filter attribute
		if (McpOperationStatsCollection.get(key) == null) {
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
	
	private synchronized McpSessionStatistics initializeMcpSessionStat(String key, McpSessionStatAttributes statAttri, String appName) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (McpSessionStatsCollection.get(key) != null) {
			return McpSessionStatsCollection.get(key);
		}

		McpSessionStatistics mcpMetricStats = new McpSessionStatistics(statAttri);
		McpSessionStatsCollection.put(key, mcpMetricStats);
		
		//Shut down by monitor-1.0 filter attribute
		if (McpSessionStatsCollection.get(key) == null) {
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
				McpOperationStatsCollection.remove(statName);
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
	public void recordOperationStart(McpOperationMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Recording operation start for method: " + metrics.getMethodName());
	    }

	    tl_mcpOperationStatsBuilder.remove();
	    tl_oerationStartNanos.remove();

	    tl_oerationStartNanos.set(System.nanoTime());

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

	    tl_mcpOperationStatsBuilder.set(builder);
	}

	@Override
	public void recordOperationEnd(McpOperationMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Recording operation end for method: " + metrics.getMethodName());
	        Tr.debug(tc, "MCP status=" + metrics.getStatus() + ", errorType=" + metrics.getErrorType());

	    }

	    try {
	        McpOperationStatAttributes.Builder builder = tl_mcpOperationStatsBuilder.get();
	        Long startNanos = tl_oerationStartNanos.get();

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

	        updateMcpOperationStatDuration(builder, duration, null);
	    } finally {
	        tl_mcpOperationStatsBuilder.remove();
	        tl_oerationStartNanos.remove();
	    }
	}

	@Override
	public void recordSessionStart(McpSessionMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && metrics.getMcpSession() != null) {
	        Tr.debug(tc, "Recording session start for session: " + metrics.getMcpSession().getSessionId());
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
	    
	    metrics.setAttributesBuilder(builder);
	}

	@Override
	public void recordSessionEnd(McpSessionMetrics metrics) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "MCP errorType=" + metrics.getErrorType());

	    }

        McpSessionStatAttributes.Builder builder = metrics.getAttributesBuilder();

        if (builder == null || metrics.getStartTIme() == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Missing MCP monitor state. Operation start may not have been recorded.");
            }
            return;
        }

        long elapsedNanos = metrics.getDurationNanos();
        Duration duration = Duration.ofNanos(elapsedNanos);

        if (metrics.getErrorType() != null) {
            builder.withErrorType(Optional.of(metrics.getErrorType()));
        }

        updateMcpSessionStatDuration(builder, duration, null);
	}
	
	

}
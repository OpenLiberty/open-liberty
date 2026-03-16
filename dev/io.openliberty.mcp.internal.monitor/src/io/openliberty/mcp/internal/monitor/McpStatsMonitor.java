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

import com.ibm.websphere.monitor.annotation.Args;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeBeforeCall;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.mcp.internal.monitor.metrics.MetricsManager;
import io.openliberty.mcp.internal.McpServlet;
import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.metrics.McpMetrics;
import io.openliberty.mcp.internal.requests.McpToolCallParams;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 */
@Monitor(group = "MCP")
public class McpStatsMonitor extends StatisticActions {

	private static final TraceComponent tc = Tr.register(McpStatsMonitor.class);

	private static final ThreadLocal<McpStatAttributes.Builder> tl_mcpStatsBuilder = new ThreadLocal<McpStatAttributes.Builder>();
	private static final ThreadLocal<Long> tl_startNanos = new ThreadLocal<Long>();
	
	private static final ConcurrentHashMap<String,Set<String>> appNameToStat = new ConcurrentHashMap<String,Set<String>>();
	
	private static McpStatsMonitor instance;

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
			instance = this;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("Multiple attempts to create %s. We already have an instance", McpStatsMonitor.class.getName()));
			}
		}

	}

	public static McpStatsMonitor getInstance() {
		if (instance != null) {
			return instance;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("No instance of %s found", McpStatsMonitor.class.getName()));
			}

		}
		return null;
	}

	@PublishedMetric
	public MeterCollection<McpStats> McpStatsCollection = new MeterCollection<McpStats>("McpMetrics", this);
	
	@SuppressWarnings("restriction")
	@ProbeAtEntry
    @ProbeSite(
        clazz = "io.openliberty.mcp.internal.metrics.McpMetrics",
        method = "operationStarted",
        args = "io.openliberty.mcp.internal.metrics.McpMetrics"
    )
    public void atOperationStarted(@Args Object[] myargs) {
        if (myargs.length > 0 && myargs[0] instanceof McpMetrics metrics) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "MCP operation started: method=" + metrics.getMethodName()
                        + ", tool=" + metrics.getToolName());
            }
        }        
    }

	@SuppressWarnings("restriction")
	@ProbeAtEntry
	@ProbeSite(
			clazz = "io.openliberty.mcp.internal.metrics.McpMetrics",
			method = "operationEnded",
			args = "io.openliberty.mcp.internal.metrics.McpMetrics"
			)
	public void atOperationEnded(@Args Object[] myargs) {
		if(myargs.length == 0 || !(myargs[0] instanceof McpMetrics metrics)) {
			return;
		}
		McpStatAttributes.Builder builder = McpStatAttributes.builder();
		builder.withMcpMethodName(metrics.getMethodName());

	    if (metrics.getToolName() != null) {
	        builder.withGenAiToolName(Optional.of(metrics.getToolName()));
	    }
	    
	    if(metrics.getErrorType() != null) {
	    	builder.withGenAiToolName(Optional.of(metrics.getErrorType()));
	    }
	    
	    if (metrics.getTransport() != null) {
            if (metrics.getTransport().getMcpRequest() != null) {
                builder.withJsonrpcProtocolVersion(
                        Optional.ofNullable(metrics.getTransport().getMcpRequest().jsonrpc()));
            }

            if (metrics.getTransport().getProtocolVersion() != null) {
                builder.withMcpProtocolVersion(
                        Optional.of(metrics.getTransport().getProtocolVersion().toString()));
            }

            if (metrics.getTransport().getReq() != null && metrics.getTransport().getReq().getProtocol() != null) {
                String[] fullProtocol = metrics.getTransport().getReq().getProtocol().split("/");
                if (fullProtocol.length == 2) {
                    builder.withNetworkProtocolName(Optional.of(fullProtocol[0]));
                    builder.withNetworkProtocolVersion(Optional.of(fullProtocol[1]));
                }
                builder.withNetworkTransport(Optional.of("tcp"));
            }
	    }
	    
	    updateMcpStatDuration(builder, Duration.ofNanos(metrics.getDurationNanos()), null);
	}
		
	
	
	@SuppressWarnings("restriction")
	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpServlet", method = "callRequest", args = "io.openliberty.mcp.internal.McpTransport")
	public void atSendResponse(@This McpServlet mcpServelet, @Args Object[] myargs) {

		tl_mcpStatsBuilder.set(null);; //reset just in case
		
		tl_startNanos.set(System.nanoTime());
		McpStatAttributes.Builder builder = McpStatAttributes.builder();
		
		if (myargs.length > 0) {
			if (myargs[0] != null && myargs[0] instanceof McpTransport) {
				McpTransport transport = (McpTransport) myargs[0];
				String method = transport.getMcpRequest().method();
				builder.withMcpMethodName(method);
				if (method.equals("tool\\call")) {
					McpToolCallParams params = transport.getParams(McpToolCallParams.class);
					builder.withGenAiToolName(Optional.of(params.getName()));
				}
				builder.withJsonrpcProtocolVersion(Optional.of(transport.getMcpRequest().jsonrpc()));
				builder.withMcpProtocolVersion(Optional.of(transport.getProtocolVersion().toString()));
				
				String[] fullProtocal = transport.getReq().getProtocol().split("/");
				builder.withNetworkProtocolName(Optional.of(fullProtocal[0]));
				builder.withNetworkProtocolVersion(Optional.of(fullProtocal[1]));
				builder.withNetworkTransport(Optional.of("tcp"));
				
				tl_mcpStatsBuilder.set(builder);	
				
			}
		}
	}
	
	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpServlet", method = "callRequest", args = "io.openliberty.mcp.internal.McpTransport")
	public void atSendResponseReturn(@SuppressWarnings("restriction") @This McpServlet mcpServelet, @Args Object[] myargs) {

		long elapsedNanos = System.nanoTime() - tl_startNanos.get();
		McpStatAttributes.Builder retrievedMcpStatAttributesBuilder = tl_mcpStatsBuilder.get();

		if (retrievedMcpStatAttributesBuilder == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Unable to retrieve HttpStatAttributes. Unable to record time.");
			}
			return;
		}
		updateMcpStatDuration(retrievedMcpStatAttributesBuilder, Duration.ofNanos(elapsedNanos), null);

	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpTransport", method = "sendError", args = "java.lang.Throwable")
	public void atSendErrorEntry(@SuppressWarnings("restriction") @This McpTransport transport, @Args Object[] myargs) {
		processErrorTypeAttributes(transport, myargs);	
	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpTransport", method = "sendHttpException", args = "java.lang.Throwable")
	public void atSendHttpExceptionEntry(@SuppressWarnings("restriction") @This McpTransport transport, @Args Object[] myargs) {
		processErrorTypeAttributes(transport, myargs);
	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpTransport", method = "sendAuthError", args = "java.lang.Throwable")
	public void atSendAuthHttpExceptionEntry(@SuppressWarnings("restriction") @This McpTransport transport, @Args Object[] myargs) {
		processErrorTypeAttributes(transport, myargs);
	}
	@ProbeAtEntry
	@ProbeSite(clazz = "io.openliberty.mcp.internal.McpTransport", method = "sendJsonRpcException", args = "io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException")
	public void atSendJsonRpcExceptionEntry(@SuppressWarnings("restriction") @This McpTransport transport, @Args Object[] myargs) {
		processErrorTypeAttributes(transport, myargs);
	}
	
	@SuppressWarnings("restriction")
	public void processErrorTypeAttributes(McpTransport transport, Object[] myargs) {
		if (myargs.length > 0) {
			if (myargs[0] != null && myargs[0] instanceof Throwable) {
				Throwable t = (Throwable) myargs[0];
				McpStatAttributes.Builder builder = tl_mcpStatsBuilder.get();
				if( builder != null) {
					builder.withErrorType(Optional.of(t.getCause().getMessage()));
				}
			}else if (myargs[0] != null && myargs[0] instanceof JSONRPCException) {
				JSONRPCException t = (JSONRPCException) myargs[0];
				McpStatAttributes.Builder builder = tl_mcpStatsBuilder.get();
				if( builder != null) {
					builder.withErrorType(Optional.of(t.getCause().getMessage()));
					builder.withRpcResponseStatusCode(Optional.of(String.valueOf(t.getErrorCode().getCode())));
				}
			}
		}	
	}


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
				Tr.debug(tc, "No Available Metric runtimes to forward HTTP stats to.");
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
	
//    @ProbeAtEntry
//    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "destroy")
//    public void atServletDestroy(@This GenericServlet s) {
//    	
//        String appName = (String) s.getServletContext().getAttribute("com.ibm.websphere.servlet.enterprise.application.name");
//        removeStat(appName);
//    	
//    }

}

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

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.mcp.internal.metrics.McpOperationMetrics;
import io.openliberty.mcp.internal.metrics.McpSessionMetrics;
import io.openliberty.mcp.internal.monitor.metrics.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitor.metrics.McpSessionStatAttributes;
import io.openliberty.mcp.internal.monitor.metrics.MetricsManager;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;

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
public class McpStatsMonitorImpl extends StatisticActions implements McpStatsMonitor {

    private static final ServiceCaller<MetricsManager> metricsManagerService = new ServiceCaller<>(McpStatsMonitorImpl.class, MetricsManager.class);

    private static McpStatsMonitorImpl instance;

    private static final TraceComponent tc = Tr.register(McpStatsMonitorImpl.class);

    private static final ConcurrentHashMap<String, Set<McpOperationStatAttributes>> appNameToOperationStats = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<McpSessionStatAttributes>> appNameToSessionStats = new ConcurrentHashMap<>();

    @PublishedMetric
    public MeterCollection<McpOperationStatistics> mcpOperationStatsCollection = new MeterCollection<>("McpOperation", this);

    @PublishedMetric
    public MeterCollection<McpSessionStatistics> mcpSessionStatsCollection = new MeterCollection<>("McpSession", this);

    /**
     * Generates a unique String ID for MeterCollection from operation stat attributes.
     * MeterCollection requires String keys for JMX ObjectName registration. The mcpOperationStat_ID
     * field contains a pre-computed JMX-safe identifier.
     */
    private static String generateOperationStatId(McpOperationStatAttributes attrs) {
        return attrs.mcpOperationStat_ID();
    }

    /**
     * Generates a unique String ID for MeterCollection from session stat attributes.
     * MeterCollection requires String keys for JMX ObjectName registration. The mcpSessionStat_ID
     * field contains a pre-computed JMX-safe identifier.
     */
    private static String generateSessionStatId(McpSessionStatAttributes attrs) {
        return attrs.mcpSessionStat_ID();
    }

    /**
     *
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
    public void updateMcpOperationStatDuration(McpOperationStatAttributes.Builder builder, Duration duration, String appName) {
        if (!McpMonitorAppStateListener.isMcpEnabled()) {
            return;
        }

        Optional<McpOperationStatAttributes> mcpStatsAttributesOpt = builder.build();
        if (mcpStatsAttributesOpt.isEmpty()) {
            return;
        }

        McpOperationStatAttributes mcpStatsAttributes = mcpStatsAttributesOpt.get();

        /*
         * Create and/or update MBean
         */
        McpOperationStatistics mcpStats = getOrCreateMcpOperationStat(mcpStatsAttributes, appName);

        //Monitor bundle when updating statistics will do synchronization
        mcpStats.incrementOperationCountBy(1);
        mcpStats.addOperationTimeStat(duration.toNanos());

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
        if (!McpMonitorAppStateListener.isMcpEnabled()) {
            return;
        }

        Optional<McpSessionStatAttributes> mcpStatsAttributesOpt = builder.build();
        if (mcpStatsAttributesOpt.isEmpty()) {
            return;
        }

        McpSessionStatAttributes mcpStatsAttributes = mcpStatsAttributesOpt.get();

        /*
         * Create and/or update MBean
         */
        McpSessionStatistics mcpStats = getOrCreateMcpSessionStat(mcpStatsAttributes, appName);
        if (mcpStats == null) {
            return;
        }

        //Monitor bundle when updating statistics will do synchronization
        mcpStats.incrementSessionCountBy(1);
        mcpStats.addSessionDurationStat(duration.toNanos());

        metricsManagerService.run(metricsManager -> {
            metricsManager.updateMcpSessionDurationMetrics(mcpStatsAttributes, duration);
            return null;
        });
    }

    private McpOperationStatistics getOrCreateMcpOperationStat(McpOperationStatAttributes statAttri, String appName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "initializeMcpOperationStat", statAttri);
        }

        String statId = generateOperationStatId(statAttri);
        McpOperationStatistics mcpStats = mcpOperationStatsCollection.get(statId);
        if (mcpStats != null) {
            return mcpStats;
        }

        synchronized (this) {
            /*
             * Check again it was added, thread that was blocking may have been adding it
             */
            mcpStats = mcpOperationStatsCollection.get(statId);
            if (mcpStats != null) {
                return mcpStats;
            }

            McpOperationStatistics mcpMetricStats = new McpOperationStatistics(statAttri);
            // Use attribute-based put method
            mcpOperationStatsCollection.put(statId, statAttri.toAttributeMap(), mcpMetricStats);

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

    private McpSessionStatistics getOrCreateMcpSessionStat(McpSessionStatAttributes statAttri, String appName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "initializeMcpSessionStat", statAttri);
        }

        String statId = generateSessionStatId(statAttri);
        McpSessionStatistics mcpStats = mcpSessionStatsCollection.get(statId);
        if (mcpStats != null) {
            return mcpStats;
        }

        synchronized (this) {
            /*
             * Check again it was added, thread that was blocking may have been adding it
             */
            mcpStats = mcpSessionStatsCollection.get(statId);
            if (mcpStats != null) {
                return mcpStats;
            }

            McpSessionStatistics mcpMetricStats = new McpSessionStatistics(statAttri);
            // Use attribute-based put method
            mcpSessionStatsCollection.put(statId, statAttri.toAttributeMap(), mcpMetricStats);

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
            metricsManagerService.call(metricsManager -> metricsManager.removeMetricsForApp(appName));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Removed " + removedCount + " statistics for application: " + appName);
            }
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
    public void recordOperationEnd(McpOperationMetrics metrics) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Recording operation end for method: " + metrics.getMethodName());
            Tr.debug(this, tc, "MCP status=" + metrics.getStatus() + ", errorType=" + metrics.getErrorType());
        }

        McpOperationStatAttributes.Builder builder = McpOperationStatAttributes.builder();
        builder.withMcpMethodName(metrics.getMethodName());

        if (metrics.getToolName() != null) {
            builder.withGenAiToolName(metrics.getToolName());
        }

        if (metrics.getTransport() != null) {
            try {
                if (metrics.getTransport().getMcpRequest() != null) {
                    String jsonrpcVersion = metrics.getTransport().getMcpRequest().jsonrpc();
                    if (jsonrpcVersion != null) {
                        builder.withJsonrpcProtocolVersion(jsonrpcVersion);
                    }
                }

                if (metrics.getTransport().getProtocolVersion() != null) {
                    builder.withMcpProtocolVersion(metrics.getTransport().getProtocolVersion().getVersion());
                }

                if (metrics.getTransport().getReq() != null) {
                    String protocol = metrics.getTransport().getReq().getProtocol();
                    if (protocol != null) {
                        String[] protocolParts = protocol.split("/");
                        if (protocolParts.length >= 2) {
                            builder.withNetworkProtocolName(protocolParts[0]);
                            builder.withNetworkProtocolVersion(protocolParts[1]);
                        }
                    }
                }

                builder.withNetworkTransport("tcp");
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Error extracting transport information: " + e.getMessage());
                }
            }
        }

        long elapsedNanos = System.nanoTime() - metrics.getStartTimeNanos();
        Duration duration = Duration.ofNanos(elapsedNanos);

        if (metrics.getStatus() != null) {
            builder.withRpcResponseStatusCode(metrics.getStatus());
        }

        if (metrics.getErrorType() != null) {
            builder.withErrorType(metrics.getErrorType());
        }

        updateMcpOperationStatDuration(builder, duration, metrics.getAppName());
    }

    @Override
    public void recordSessionEnd(McpSessionMetrics metrics) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "MCP errorType=" + metrics.getErrorType());
        }

        McpSessionStatAttributes.Builder builder = McpSessionStatAttributes.builder();

        if (metrics.getTransport() != null) {
            try {
                if (metrics.getTransport().getMcpRequest() != null) {
                    String jsonrpcVersion = metrics.getTransport().getMcpRequest().jsonrpc();
                    if (jsonrpcVersion != null) {
                        builder.withJsonrpcProtocolVersion(jsonrpcVersion);
                    }
                }

                if (metrics.getTransport().getProtocolVersion() != null) {
                    builder.withMcpProtocolVersion(metrics.getTransport().getProtocolVersion().getVersion());
                }

                String protocol = metrics.getProtocol();
                if (protocol != null) {
                    String[] protocolParts = protocol.split("/");
                    if (protocolParts.length >= 2) {
                        builder.withNetworkProtocolName(protocolParts[0]);
                        builder.withNetworkProtocolVersion(protocolParts[1]);
                    }
                }

                builder.withNetworkTransport("tcp");
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Error extracting transport information: " + e.getMessage());
                }
            }
        }

        long elapsedNanos = metrics.getDurationNanos();
        Duration duration = Duration.ofNanos(elapsedNanos);

        if (metrics.getErrorType() != null) {
            builder.withErrorType(metrics.getErrorType());
        }

        updateMcpSessionStatDuration(builder, duration, metrics.getAppName());
    }

}
/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.monitor.computed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

public class ComputedMappingTable {

    public static final int COMPUTED_METRIC_NAME = 0;
    public static final int COMPUTED_METRIC_DISPLAYNAME = 1;
    public static final int COMPUTED_METRIC_DESCRIPTION = 2;
    public static final int COMPUTED_METRIC_TYPE = 3;
    public static final int COMPUTED_METRIC_UNIT = 4;
    public static final int MONITOR_METRIC_REGISTRY_SCOPE = 5;
    public static final int MONITOR_METRIC_DURATION = 6;
    public static final int MONITOR_METRIC_DURATION_UNIT = 7;
    public static final int MONITOR_METRIC_TOTAL_COUNT = 8;
    public static final int MONITOR_METRIC_TOTAL_UNIT = 9;
    
    public static final String GAUGE = MetricType.GAUGE.toString().toUpperCase();

    private static ComputedMappingTable singleton = null;

    private Map<String, String[][]> mappingTable = new HashMap<String, String[][]>();

    public static ComputedMappingTable getInstance() {
        if (singleton == null)
            singleton = new ComputedMappingTable();
        return singleton;
    }

    // All new computed metrics are vendor scoped metric types.
    private ComputedMappingTable() {
        // Base Metrics
        String[][] cpuTable = new String[][]{
            {"cpu.processCpuUtilization", "Process CPU Utilization", "cpu.processCpuUtilization.description", GAUGE,
                MetricUnits.PERCENT, "base", "cpu.processCpuTime", MetricUnits.NANOSECONDS,"cpu.availableProcessors", MetricUnits.NONE}};
        mappingTable.put("cpuStats", cpuTable);

        String[][] memoryTable = new String[][]{
            {"memory.heapUtilization", "Heap Utilization", "memory.heapUtilization.description", GAUGE, 
                MetricUnits.PERCENT, "base", "memory.usedHeap", MetricUnits.BYTES, "memory.maxHeap", MetricUnits.BYTES}};
        mappingTable.put("memoryStats", memoryTable);

        String[][] gcTable = new String[][] { 
            { "gc.time.per.cycle", "Total Garbage Collection Time Per Garbage Collection Cycle", "gc.time.per.cycle.description", GAUGE, MetricUnits.SECONDS, 
                "base", "gc.time", MetricUnits.MILLISECONDS, "gc.total", MetricUnits.NONE}};
        mappingTable.put("gcStats", gcTable);

        // Vendor Metrics
        String[][] servletTable = new String[][]{
            {"servlet.request.elapsedTime.per.request", "Total Elapsed Time per Request", "servlet.request.elapsedTime.per.request.description",
                GAUGE, MetricUnits.SECONDS, "vendor", "servlet.responseTime.total", MetricUnits.NANOSECONDS, "servlet.request.total", MetricUnits.NONE}};
        mappingTable.put("ServletStats", servletTable);

        String[][] connectionPoolTable = new String[][]{
            {"connectionpool.inUseTime.per.usedConnection", "Total Connection Pool In Use Time per Used Connection", "connectionpool.inUseTime.per.usedConnection.description",
                GAUGE, MetricUnits.SECONDS, "vendor", "connectionpool.inUseTime.total", MetricUnits.MILLISECONDS, "connectionpool.usedConnections.total", MetricUnits.NONE},
            {"connectionpool.waitTime.per.queuedRequest", "Total Connection Pool Wait Time per Queued Request", "connectionpool.waitTime.per.queuedRequest.description",
                GAUGE, MetricUnits.SECONDS, "vendor", "connectionpool.waitTime.total", MetricUnits.MILLISECONDS, "connectionpool.queuedRequests.total", MetricUnits.NONE}};
        mappingTable.put("ConnectionPoolStats", connectionPoolTable);

        String[][] restTable = new String[][]{
            {"REST.request.elapsedTime.per.request", "Total Elapsed Time per Request", "REST.request.elapsedTime.per.request.description",
                GAUGE, MetricUnits.SECONDS, "vendor", "REST.request.elapsedTime", MetricUnits.NANOSECONDS, "REST.request.total", MetricUnits.NONE}};
        mappingTable.put("REST_Stats", restTable);

        // Add more computed metrics HERE...
    }

    public List<String> getMetricGroupsList(String metricScope) {
        List<String> metricGroupList = new ArrayList<>();

        for (Map.Entry<String, String[][]> entry : mappingTable.entrySet()) {
            String[][] data = entry.getValue();
            for (String[] metricData : data) {
                if (metricData[ComputedMappingTable.MONITOR_METRIC_REGISTRY_SCOPE].equals(metricScope)) {
                    metricGroupList.add(entry.getKey());
                }
            }
        }
        return metricGroupList;
    }

    public String[][] getData(String metricGroup) {
        String group;
        if (metricGroup.contains(",")) {
            // vendor metric (MBean ObjectName)
            group = getType(metricGroup);
        } else {
            // base metric
            group = metricGroup;
        }

        for (String k : mappingTable.keySet()) {
            if (group.contains(k))
                return mappingTable.get(k);
        }
        return null;
    }

    private String getType(String objectName) {
        for (String subString : objectName.split(",")) {
            subString = subString.trim();
            if (subString.contains("type=")) {
                return subString.split("=")[1];
            }
        }
        return "notype";
    }

}

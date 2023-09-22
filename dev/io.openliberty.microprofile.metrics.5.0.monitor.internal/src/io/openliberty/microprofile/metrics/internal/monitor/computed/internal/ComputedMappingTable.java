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
package io.openliberty.microprofile.metrics.internal.monitor.computed.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.metrics.MetricUnits;

import io.openliberty.microprofile.metrics50.helper.Constants;

public class ComputedMappingTable {

    public static final int COMPUTED_METRIC_NAME = 0;
    public static final int COMPUTED_METRIC_DESCRIPTION = 1;
    public static final int COMPUTED_METRIC_TYPE = 2;
    public static final int COMPUTED_METRIC_UNIT = 3;
    public static final int MONITOR_METRIC_REGISTRY_SCOPE = 4;
    public static final int MONITOR_METRIC_DURATION = 5;
    public static final int MONITOR_METRIC_TOTAL_COUNT = 6;

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
            {"cpu.processCpuUtilization", "cpu.processCpuUtilization.description", Constants.GAUGE,
                MetricUnits.PERCENT, "base", "cpu.processCpuTime", "cpu.availableProcessors"}};
        mappingTable.put("cpuStats", cpuTable);

        String[][] memoryTable = new String[][]{
            {"memory.heapUtilization", "memory.heapUtilization.description", Constants.GAUGE, 
                MetricUnits.PERCENT, "base", "memory.usedHeap", "memory.maxHeap"}};
        mappingTable.put("memoryStats", memoryTable);

        // String[][] gcTable = new String[][] { { "gc.time.per.cycle",
        // "gc.time.per.cycle.description",
        // Constants.GAUGE, MetricUnits.SECONDS, "base", "gc.time", "gc.total" } };
        // mappingTable.put("gcStats", gcTable);

        // Vendor Metrics
        String[][] servletTable = new String[][]{
            {"servlet.request.elapsedTime.per.request", "servlet.request.elapsedTime.per.request.description",
                   Constants.GAUGE, MetricUnits.SECONDS, "vendor", "servlet.responseTime.total", "servlet.request.total"}};
        mappingTable.put("ServletStats", servletTable);

        String[][] connectionPoolTable = new String[][]{
            {"connectionpool.inUseTime.per.usedConnection", "connectionpool.inUseTime.per.usedConnection.description",
                Constants.GAUGE, MetricUnits.SECONDS, "vendor", "connectionpool.inUseTime.total", "connectionpool.usedConnections.total"},
            {"connectionpool.waitTime.per.queuedRequest",  "connectionpool.waitTime.per.queuedRequest.description",
                Constants.GAUGE, MetricUnits.SECONDS, "vendor", "connectionpool.waitTime.total", "connectionpool.queuedRequests.total"}};
        mappingTable.put("ConnectionPoolStats", connectionPoolTable);

        String[][] restTable = new String[][]{
            {"REST.request.elapsedTime.per.request", "REST.request.elapsedTime.per.request.description",
                Constants.GAUGE, MetricUnits.SECONDS, "vendor", "REST.request.seconds.sum", "REST.request.seconds.count"}};
        mappingTable.put("RESTStats", restTable);

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

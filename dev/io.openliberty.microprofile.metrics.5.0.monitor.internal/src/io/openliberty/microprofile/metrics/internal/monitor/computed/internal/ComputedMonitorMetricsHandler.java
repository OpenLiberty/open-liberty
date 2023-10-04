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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.CpuInfo;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.microprofile.metrics50.helper.Constants;

public class ComputedMonitorMetricsHandler {

    private static final TraceComponent tc = Tr.register(ComputedMonitorMetricsHandler.class);

    public static final long COMPUTED_METRICS_TIMER_INTERVAL = 15000; // 15000 milliseconds = 15 seconds

    public static final long EWMA_MOVING_WINDOW_INTERVAL = 5; // 5 minute moving window

    public static Map<MetricID, Set<ComputedMonitorMetrics>> computationMetricsMap;

    public static Map<MetricID, Object> finalComputedMetricsMap;

    private Timer computeMetricsTimer = new Timer(true);

    private ComputedMappingTable mappingTable;

    protected SharedMetricRegistries sharedMetricRegistry;

    // Initialize data structures to hold the computed metrics.
    public ComputedMonitorMetricsHandler(SharedMetricRegistries sharedMetricRegistry) {
        // Retrieve each registered metric from the Registry.
        this.sharedMetricRegistry = sharedMetricRegistry;

        // Get mapping table for computed metrics.
        this.mappingTable = ComputedMappingTable.getInstance();

        // Stores the computed metricID with the required metrics needed for calculation.
        computationMetricsMap = new ConcurrentHashMap<MetricID, Set<ComputedMonitorMetrics>>();

        // Stores the final computed values for the computed metrics.
        finalComputedMetricsMap = new ConcurrentHashMap<MetricID, Object>();

        // Create the Computed Base scoped metrics.
        createComputedBaseMetrics();

        // Start the Metric computation Timer Task.
        ComputeMetricsTimerTask cmtt = new ComputeMetricsTimerTask();
        computeMetricsTimer.schedule(cmtt, 0, COMPUTED_METRICS_TIMER_INTERVAL);
    }

    // Timer task to calculate the new metrics every time interval.
    public class ComputeMetricsTimerTask extends TimerTask {

        public ComputeMetricsTimerTask() {
            super();
        }

        @Override
        public void run() {
            // Once the timer pops, calculate the metric values for the computed vendor metrics.
            calculateMetricValue();
        }
    }

    public void createComputedBaseMetrics() {
        // Get the Base scoped Metric Registry.
        MetricRegistry metricRegistry = getMetricRegistry(MetricRegistry.BASE_SCOPE);

        /*
         * metricRegistry is null due to failed initialization of the MP Metrics runtime.
         * Fail silently.
         */
        if (metricRegistry == null) {
            if (tc.isDebugEnabled() || tc.isAnyTracingEnabled()) {
                Tr.debug(tc,
                        "MetricRegistry obtained from SharedMetricRegistries was null. No metrics will be registered.");
            }
            return;
        }

        // Get all the base metrics from the Metrics Registry.
        SortedSet<MetricID> baseMetricsIDSet = metricRegistry.getMetricIDs();

        // Get the Base scoped metrics needed to create the computed base metrics.
        List<String> baseMetricGroupList = mappingTable
                .getMetricGroupsList("base");

        // Iterate over the list of Base metric groups needed for computation.
        for (String baseMetricGroup : baseMetricGroupList) {
            // Get the data for the required base metrics for creation and computation.
            String[][] data = mappingTable.getData(baseMetricGroup);

            // Loop through each base metric data.
            for (String[] metricData : data) {
                String metricDurationName = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION];
                String metricTotalCountName = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_COUNT];

                for (MetricID mid : baseMetricsIDSet) {
                    ComputedMonitorMetrics cmm = null;
                    Tag[] metricTagNames = null;

                    // Create a new set for every new computed metric.
                    Set<ComputedMonitorMetrics> computedMonitorMetricsSet = new HashSet<ComputedMonitorMetrics>();

                    // Check if the base metric name from the registry equals the base metric name duration needed for computation.
                    String baseMetricName = mid.getName();
                    if (baseMetricName.equals(metricDurationName)) {
                        metricTagNames = mid.getTagsAsArray();

                        // Add the duration metrics to the computation set, no appName for base metrics.
                        cmm = new ComputedMonitorMetrics(
                                MetricRegistry.BASE_SCOPE, mid, Constants.DURATION, null);
                        computedMonitorMetricsSet.add(cmm);

                        // Add the total count metric to the computation set, no appName for base metrics.
                        MetricID totalCountMetricID = new MetricID(
                                metricTotalCountName, metricTagNames);
                        cmm = new ComputedMonitorMetrics(
                                MetricRegistry.BASE_SCOPE, totalCountMetricID, Constants.TOTAL, null);
                        computedMonitorMetricsSet.add(cmm);

                        // Once the existing metrics needed for calculation are cached, register new computed metric in the Metric Registry.
                        MetricID computedMetricID = registerNewComputedMetricWithExistingMetricTag(
                                metricData, metricTagNames);

                        // Populating map with newly registered computed metric and corresponding metrics set needed for computation.
                        computationMetricsMap.put(computedMetricID,
                                computedMonitorMetricsSet);

                        if (TraceComponent.isAnyTracingEnabled()
                                && tc.isDebugEnabled()) {
                            Tr.debug(tc,
                                    "Created computed metric for base computation : "
                                            + computedMetricID.toString());
                        }
                    }
                }
            }
        }
    }

    public void createComputedMetrics(String objectName, Set<MetricID> monitorMetricsIDSet) {
        String[][] data = mappingTable.getData(objectName);

        for (String[] metricData : data) {
            Tag[] computedMetricTagNames = null;
            // Create a new set for every new computed metric.
            Set<ComputedMonitorMetrics> computedMonitorMetricsSet = new HashSet<ComputedMonitorMetrics>();

            for (MetricID mID : monitorMetricsIDSet) {
                ComputedMonitorMetrics cmm = null;
                String metricDurationName = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION];
                String metricTotalCountName = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_COUNT];
                String metricName = mID.getName();

                if (metricDurationName.equals(metricName)) {
                    // cache the metrics tag for the new computed metric name.
                    computedMetricTagNames = mID.getTagsAsArray();
                    cmm = new ComputedMonitorMetrics(
                            MetricRegistry.VENDOR_SCOPE, mID,
                            Constants.DURATION, null); // no appName

                } else if (metricTotalCountName.equals(metricName)) {
                    cmm = new ComputedMonitorMetrics(
                            MetricRegistry.VENDOR_SCOPE, mID, Constants.TOTAL, null); // no appName
                }

                if (cmm != null) {
                    computedMonitorMetricsSet.add(cmm);
                }
            }

            // Once the existing metrics needed for calculation are cached, register new computed metric.
            MetricID computedMetricID = registerNewComputedMetricWithExistingMetricTag(
                    metricData, computedMetricTagNames);

            // Populating map with newly registered computed metric and corresponding metrics set needed for computation.
            computationMetricsMap.put(computedMetricID,
                    computedMonitorMetricsSet);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                        "Created computed metric for vendor computation :  "
                                + computedMetricID.toString());
            }
        }
    }

    public void createRESTComputedMetrics(String objectName, MetricID restMetricID, String appName) {
        String[][] data = mappingTable.getData(objectName);

        // Loop through each base REST metric data.
        for (String[] metricData : data) {
            ComputedMonitorMetrics cmm = null;
            Tag[] metricTagNames = null;

            // Create a new set for every new computed metric.
            Set<ComputedMonitorMetrics> computedMonitorMetricsSet = new HashSet<ComputedMonitorMetrics>();

            metricTagNames = restMetricID.getTagsAsArray();

            // Add the duration metrics to the computation set.
            cmm = new ComputedMonitorMetrics(MetricRegistry.BASE_SCOPE,
                    restMetricID, Constants.DURATION, appName);
            computedMonitorMetricsSet.add(cmm);

            // Add the total count metric to the computation set, using the previous MetricID, 
            // since REST is a Timer, it has the same metricID.
            cmm = new ComputedMonitorMetrics(MetricRegistry.BASE_SCOPE,
                    restMetricID, Constants.TOTAL, appName);
            computedMonitorMetricsSet.add(cmm);

            // Once the existing metrics needed for calculation are cached, register new
            // computed metric in the Metric Registry.
            MetricID computedMetricID = registerNewComputedMetricWithExistingMetricTag(
                    metricData, metricTagNames);

            // Populating map with newly registered computed metric and corresponding
            // metrics set needed for computation
            computationMetricsMap.put(computedMetricID, computedMonitorMetricsSet);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                        "Created computed metric for base REST computation : " + computedMetricID.toString());
            }
        }
    }

    public MetricID registerNewComputedMetricWithExistingMetricTag(String[] metricData, Tag[] existingMetricTags) {
        MetricRegistry metricRegistry = getMetricRegistry(MetricRegistry.VENDOR_SCOPE);
        String computedMetricName = metricData[ComputedMappingTable.COMPUTED_METRIC_NAME];
        String metricType = metricData[ComputedMappingTable.COMPUTED_METRIC_TYPE];

        MetricID computedMetricID = new MetricID(computedMetricName, existingMetricTags);

        // Resolve the resource bundle metric description.
        String description = Tr.formatMessage(tc, metricData[ComputedMappingTable.COMPUTED_METRIC_DESCRIPTION]);
        String unit = metricData[ComputedMappingTable.COMPUTED_METRIC_UNIT];

        Metadata metadata = Metadata.builder().withName(computedMetricName).withDescription(description).withUnit(unit).build();

        if (Constants.GAUGE.equalsIgnoreCase(metricType)) {
            ComputedMonitorGauge<Number> cmg = new ComputedMonitorGauge<Number>(this, computedMetricID);
            if (existingMetricTags == null) {
                metricRegistry.gauge(metadata, cmg, x -> (x.getValue().doubleValue()));
            } else {
                metricRegistry.gauge(metadata, cmg, x -> (x.getValue().doubleValue()), existingMetricTags);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Registered " + computedMetricID.toString() + " in the Metric Registry.");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to register " + computedMetricName + " because of invalid type " + metricType);
            }
        }

        return computedMetricID;
    }

    public void unregisterAllComputedMetrics() {
        MetricRegistry baseRegistry = getMetricRegistry(MetricRegistry.VENDOR_SCOPE);

        for (MetricID metricIDToRemove : computationMetricsMap.keySet()) {
            boolean rc = baseRegistry.remove(metricIDToRemove);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                        "Unregistered computed metric : " + metricIDToRemove.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
            }
        }

        // Clear all the computed base metric maps
        computationMetricsMap.clear();
        finalComputedMetricsMap.clear();
    }

    public void unregister(Set<MetricID> monitorMetricsIDSet) {
        MetricID computedMetricID = null;
        for (MetricID mID : monitorMetricsIDSet) {
            computedMetricID = getComputedMetricIDToRemove(mID);
            if (computedMetricID != null) {
                // Remove the corresponding computed metricID.
                removeComputedMetrics(computedMetricID);
            }
        }
    }

    public void unregisterComputedRESTMetricsByAppName(String appName) {
        for (Entry<MetricID, Set<ComputedMonitorMetrics>> entry : computationMetricsMap.entrySet()) {
            MetricID computedMetricID = entry.getKey();
            for (ComputedMonitorMetrics cmm : entry.getValue()) {
                String restAppName = cmm.getAppName();
                if (restAppName != null) {
                    if (restAppName.equals(appName)) {
                        removeComputedMetrics(computedMetricID);
                        break; // Removed the computed REST metric for the corresponding app, no need to continue the loop.
                    }
                }
            }
        }
    }

    public void removeComputedMetrics(MetricID computedMetricID) {
        // Remove the corresponding computed metricID.
        computationMetricsMap.remove(computedMetricID);
        finalComputedMetricsMap.remove(computedMetricID);

        MetricRegistry vendorRegistry = getMetricRegistry(MetricRegistry.VENDOR_SCOPE);
        boolean rc = vendorRegistry.remove(computedMetricID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,
                    "Unregistered computed vendor metric : " + computedMetricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
        }
    }

    public MetricID getComputedMetricIDToRemove(MetricID monitorMetricID) {
        for (Map.Entry<MetricID, Set<ComputedMonitorMetrics>> entry : computationMetricsMap.entrySet()) {
            MetricID computedMetricID = entry.getKey();
            Set<ComputedMonitorMetrics> monitorMetricSet = entry.getValue();
            for (ComputedMonitorMetrics cmm : monitorMetricSet) {
                if (cmm.getMonitorMetricID().equals(monitorMetricID)) {
                    // Return the matched computed MetricID for removal
                    return computedMetricID;
                }
            }
        }
        return null;
    }

    public Double getComputedValue(MetricID metricId) {
        Double computedValue = 0.0;
        Object computedObject = finalComputedMetricsMap.get(metricId);

        if (computedObject == null) {
            // Should only be hit during the first initialization of the metric
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "computedObject is null - computing metric values...");
            }
            calculateMetricValue();
            // Re-get the computedObject
            computedObject = finalComputedMetricsMap.get(metricId);
        }

        if (computedObject != null) {
            if (computedObject instanceof Double) {
                // Should only be for memory.heapUtilization and cpu.processCpuUtilization
                computedValue = ((Double) computedObject).doubleValue();
            } else if (computedObject instanceof EWMA) {
                computedValue = ((EWMA) computedObject).getAveragedValue();
            }
        }
        return computedValue;
    }

    public void calculateMetricValue() {
        MetricRegistry mr;
        Double metricVal = null, diffDuration = null, diffTotalCount = null;

        for (Map.Entry<MetricID, Set<ComputedMonitorMetrics>> entry : computationMetricsMap.entrySet()) {
            MetricID computedMetricID = entry.getKey();
            Set<ComputedMonitorMetrics> monitorMetrics = entry.getValue();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calculating metric value for computed metric : " + computedMetricID.toString());
            }

            String computedMetricName = computedMetricID.getName();
            if (computedMetricName.equals("memory.heapUtilization")) {
                // Do not need to use the EWMA for the heap utilization calculation
                // memory utilization = usedHeap / maxHeap;
                calculateHeapUsage(computedMetricID, monitorMetrics);
            } else if (computedMetricName.equals("cpu.processCpuUtilization")) {
                // Do not need to use the EWMA for the cpu utilization calculation
                // Can get it directly from com.ibm.ws.kernel.service.util.CpuInfo.getJavaCpuUsage()
                calculateProcessCpuUsage(computedMetricID);
            } else if (computedMetricName.equals("gc.time.per.cycle")) {
                // Need to retrieve the gc.time and gc.total from the GarbageCollectionMXBean directly,
                // instead of the mpMetrics-5.x API, since there is a known bug, where the gc.time from
                // the mpMetrics-5.x API returns as a Counter, which drops the decimal in the returned float value,
                // making the value not useful for computation.
                calculateEWMAValueForGC(computedMetricID, monitorMetrics);
            } else {
                for (ComputedMonitorMetrics cmm : monitorMetrics) {
                    mr = getMetricRegistry(cmm.getMonitorMetricScope());
                    // Get the metric value.
                    metricVal = getMetricValue(mr, cmm);
                    if (metricVal != null) {
                        if (cmm.getComputationType().equals(Constants.DURATION)) {
                            diffDuration = cmm.getDifference(metricVal.doubleValue());
                        } else if (cmm.getComputationType().equals(Constants.TOTAL)) {
                            diffTotalCount = cmm.getDifference(metricVal.doubleValue());
                        }
                    }
                }
                if (diffDuration != null && diffTotalCount != null) {
                    // Only compute the EWMA if we are able to retrieve the metrics for both Duration and Total count.
                    calculateEWMAValue(computedMetricID, diffDuration, diffTotalCount);
                }
            }
        }
    }
    
    private void calculateEWMAValueForGC(MetricID computedMetricID, Set<ComputedMonitorMetrics> monitorMetrics) {
        double currValue = 0.0, diffDuration = 0.0, diffTotalCount = 0.0;
        
        // Get the collection of Garbage Collection MXBeans
        List<GarbageCollectorMXBean> gcMXBeansList = ManagementFactory.getGarbageCollectorMXBeans();
        
        for (GarbageCollectorMXBean gcMXBean : gcMXBeansList) {
            for (ComputedMonitorMetrics cmm : monitorMetrics) {
                String gcName = gcMXBean.getName();
                String gcMetricsName = cmm.getMonitorMetricID().getTags().get("name");
                if (gcName.equals(gcMetricsName)) {
                    if (cmm.getComputationType().equals(Constants.DURATION)) {
                        currValue = gcMXBean.getCollectionTime() * Constants.MILLISECONDCONVERSION;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "GarbageCollectionTime for + " + computedMetricID + " = " + currValue);
                        }
                        diffDuration = currValue < 0 ? -1.0 : cmm.getDifference(currValue);
                    } else if (cmm.getComputationType().equals(Constants.TOTAL)) {
                        currValue = gcMXBean.getCollectionCount();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "GarbageCollectionCount for + " + computedMetricID + " = " + currValue);
                        }
                        diffTotalCount = currValue < 0 ? -1.0 : cmm.getDifference(currValue);
                    }
                }
            }
            calculateEWMAValue(computedMetricID, diffDuration, diffTotalCount);
        }
    }


    public void calculateHeapUsage(MetricID computedMetricID, Set<ComputedMonitorMetrics> monitorMetrics) {
        double usedHeap = 0.0, maxHeap = 0.0, heapUsage = 0.0;
        MetricRegistry mr = getMetricRegistry(MetricRegistry.BASE_SCOPE);
        for (ComputedMonitorMetrics cmm : monitorMetrics) {
            MetricID metricId = cmm.getMonitorMetricID();
            Gauge<?> currentGauge = mr.getGauge(metricId);
            double currentValue = currentGauge.getValue().doubleValue();
            if (metricId.getName().equals("memory.usedHeap")) {
                usedHeap = currentValue;
            } else if (metricId.getName().equals("memory.maxHeap")) {
                maxHeap = currentValue;
            }
        }

        // If the maxHeap is undefined and is equal to -1, then the newly computed heap usage should be -1, as well.
        heapUsage = (maxHeap == -1.0) ? -1.0 : (usedHeap / maxHeap);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Computed Heap Usage, usedHeap = " + usedHeap + " maxHeap = " + maxHeap + " heapUsage = " + heapUsage);
        }
        finalComputedMetricsMap.put(computedMetricID, usedHeap / maxHeap);
    }

    public void calculateProcessCpuUsage(MetricID computedMetricID) {
        double processCpuUsageInPercent = 0.0, processCpuUsage = 0.0;

        // Retrieve the Cpu usage from CpuInfo from the kernel.
        processCpuUsageInPercent = CpuInfo.getJavaCpuUsage();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ProcessCpu Usage from CpuInfo.getJavaCpuUsage() in percentage, processCpuUsage = " + processCpuUsageInPercent);
        }

        // MP Metrics specification expects the Percent Unit value to be between 0 and 1.
        processCpuUsage = processCpuUsageInPercent / 100.0;

        finalComputedMetricsMap.put(computedMetricID, processCpuUsage);
    }

    public Double getMetricValue(MetricRegistry mr, ComputedMonitorMetrics cmm) {
        Number metricNum = null;
        Double currentValue = null;

        Metric metricValue = mr.getMetric(cmm.getMonitorMetricID());
        if (metricValue instanceof Gauge) {
            Gauge<?> currentGauge = (Gauge<?>) metricValue;
            metricNum = currentGauge.getValue();
        } else if (metricValue instanceof Counter) {
            Counter currentCount = (Counter) metricValue;
            metricNum = currentCount.getCount();
        } else if (metricValue instanceof org.eclipse.microprofile.metrics.Timer) {
            org.eclipse.microprofile.metrics.Timer currentTimer = (org.eclipse.microprofile.metrics.Timer) metricValue;
            if (cmm.getComputationType().equals(Constants.DURATION)) {
                Duration currentDur = currentTimer.getElapsedTime();
                if (currentDur != null)  {
                    metricNum = currentDur.getNano();
                    currentValue = (metricNum.doubleValue()) * Constants.NANOSECONDCONVERSION; // to seconds.
                }
            } else {
                // Get Total Counter Value for Timer
                metricNum = currentTimer.getCount();
            }
        }
        
        if (metricNum != null && currentValue == null) {
            // If the metricValue is present and the currentValue is not already set.
            currentValue = metricNum.doubleValue();
        }

        return currentValue;
    }

    public MetricRegistry getMetricRegistry(String scope) {
        MetricRegistry metricRegistry = sharedMetricRegistry.getOrCreate(scope);
        return metricRegistry;
    }

    public void calculateEWMAValue(MetricID computedMetricID, double duration, double totalCount) {
        double computedVal = 0.0;

        // Calculate the new computed metric.
        // If the duration or the totalCount is a negative value, set the computedValue to be -1.0
        // Should only happen when calculating the gc.time.per.cycle.
        computedVal = ((duration < 0.0) || (totalCount < 0.0)) ? -1.0 : (duration / totalCount);

        // Only the computed metricIDs that require EWMA will be passed into this method.
        EWMA ewmaObj = (EWMA) finalComputedMetricsMap.get(computedMetricID);

        if (ewmaObj == null) {
            // Initialization of the EWMA object, for the first time.
            double alpha = calculateAlpha(EWMA_MOVING_WINDOW_INTERVAL); // 5 min moving window
            ewmaObj = new EWMA(alpha);

            // EWMA[0] will be equal directly to the initially calculated value.
            double initialValue = (duration == 0.0 || totalCount == 0.0) ? 0.0 : computedVal;
            ewmaObj.updateNewValue(initialValue);
        } else {
            if ((duration == 0.0 || totalCount == 0.0)) {
                // If nothing changed during the current sampling period, get the previously calculated EWMA value and feed it into it again.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Idling - There were no new data in the sampling period, getting the previously calculated EWMA value to feed into it again.");
                }
                ewmaObj.updateNewValue(ewmaObj.getAveragedValue());
            } else {
                ewmaObj.updateNewValue(computedVal);
            }
        }
        finalComputedMetricsMap.put(computedMetricID, ewmaObj);
    }

    public double calculateAlpha(double movingWindowInMins) {
        double alpha, movingWindowInSecs, numOfDataSamplesInMovingWindow;

        // Get the moving window duration in seconds.
        movingWindowInSecs = movingWindowInMins * Constants.MINUTECONVERSION;

        // Calculate the number of data samples in the moving window, during the sampling period.
        numOfDataSamplesInMovingWindow = movingWindowInSecs / (COMPUTED_METRICS_TIMER_INTERVAL * Constants.MILLISECONDCONVERSION); // Data is retrieved every 15 seconds.

        alpha = 2 / (numOfDataSamplesInMovingWindow + 1);

        return alpha;
    }
}

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.CpuInfo;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;


public class ComputedMonitorMetricsHandler {

    private static final TraceComponent tc = Tr.register(ComputedMonitorMetricsHandler.class);

    public static final long COMPUTED_METRICS_TIMER_INTERVAL = 15000; // 15000 milliseconds = 15 seconds

    public static final long EWMA_MOVING_WINDOW_INTERVAL = 5; // 5 minute moving window
    
    public final static String DURATION = "DURATION";
    
    public final static String TOTAL = "TOTAL";
    
    public static final String GAUGE = MetricType.GAUGE.toString().toUpperCase();
    
    // Conversion factors
    public final static double MILLISECOND_CONVERSION = 0.001;
   
    public final static double NANOSECOND_CONVERSION = 0.000000001;
    
    public final static double MINUTE_CONVERSION = 60;

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
        MetricRegistry metricRegistry = getMetricRegistry(MetricRegistry.Type.BASE.getName());

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
        List<String> baseMetricGroupList = mappingTable.getMetricGroupsList("base");

        // Iterate over the list of Base metric groups needed for computation.
        for (String baseMetricGroup : baseMetricGroupList) {
            // Get the data for the required base metrics for creation and computation.
            String[][] data = mappingTable.getData(baseMetricGroup);

            // Loop through each base metric data.
            for (String[] metricData : data) {
                String metricDurationName = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION];
                String metricTotalCountName = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_COUNT];
                String metricDurationUnit = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION_UNIT];
                String metricTotalUnit = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_UNIT];

                for (MetricID mid : baseMetricsIDSet) {
                    ComputedMonitorMetrics cmm = null;
                    Tag[] metricTagNames = null;

                    // Create a new set for every new computed metric.
                    Set<ComputedMonitorMetrics> computedMonitorMetricsSet = new HashSet<ComputedMonitorMetrics>();

                    // Check if the base metric name from the registry equals the base metric name duration needed for computation.
                    String baseMetricName = mid.getName();
                    if (baseMetricName.equals(metricDurationName)) {
                        metricTagNames = getComputedMetricsTags(mid);

                        // Add the duration metrics to the computation set, no appName for base metrics.
                        MetricID durationMetricID = new MetricID(metricDurationName, metricTagNames);
                        cmm = new ComputedMonitorMetrics(MetricRegistry.Type.BASE.getName(), durationMetricID, DURATION, metricDurationUnit);
                        computedMonitorMetricsSet.add(cmm);

                        // Add the total count metric to the computation set, no appName for base metrics.
                        MetricID totalCountMetricID = new MetricID(metricTotalCountName, metricTagNames);
                        cmm = new ComputedMonitorMetrics(MetricRegistry.Type.BASE.getName(), totalCountMetricID, TOTAL, metricTotalUnit);
                        computedMonitorMetricsSet.add(cmm);

                        // Once the existing metrics needed for calculation are cached, register new computed metric in the Metric Registry.
                        MetricID computedMetricID = registerNewComputedMetricWithExistingMetricTag(metricData, metricTagNames);

                        // Populating map with newly registered computed metric and corresponding metrics set needed for computation.
                        computationMetricsMap.put(computedMetricID, computedMonitorMetricsSet);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Created computed metric for base computation : " + computedMetricID.toString());
                        }
                    }
                }
            }
        }
    }

    public void createComputedMetrics(String objectName, Set<MetricID> monitorMetricsIDSet, String appName, String mpAppNameConfigValue) {
        String[][] data = mappingTable.getData(objectName);

        for (String[] metricData : data) {
            Tag[] computedMetricTagNames = null;
            // Create a new set for every new computed metric.
            Set<ComputedMonitorMetrics> computedMonitorMetricsSet = new HashSet<ComputedMonitorMetrics>();

            for (MetricID mID : monitorMetricsIDSet) {
                ComputedMonitorMetrics cmm = null;
                String metricDurationName = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION];
                String metricTotalCountName = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_COUNT];
                String metricDurationUnit = metricData[ComputedMappingTable.MONITOR_METRIC_DURATION_UNIT];
                String metricTotalUnit = metricData[ComputedMappingTable.MONITOR_METRIC_TOTAL_UNIT];
                String metricName = mID.getName();

                if (objectName.contains("REST_Stats")) {
                    // Cache the metrics tag for the new computed metric name.
                    computedMetricTagNames = getComputedMetricsTags(mID);
                    
                    // Add the duration metrics to the computation set.
                    cmm = new ComputedMonitorMetrics(MetricRegistry.Type.BASE.getName(), mID, DURATION, metricDurationUnit, appName, mpAppNameConfigValue);
                    computedMonitorMetricsSet.add(cmm);

                    // Add the total count metric to the computation set, using the previous MetricID, 
                    // since REST is a SimpleTimer, it has the same metricID.
                    cmm = new ComputedMonitorMetrics(MetricRegistry.Type.BASE.getName(), mID, TOTAL, metricTotalUnit, appName, mpAppNameConfigValue);
                    computedMonitorMetricsSet.add(cmm);
                }
                else {
                    if (metricDurationName.equals(metricName)) {
                        // cache the metrics tag for the new computed metric name.
                        computedMetricTagNames = getComputedMetricsTags(mID);
                        cmm = new ComputedMonitorMetrics(MetricRegistry.Type.VENDOR.getName(), mID, DURATION, metricDurationUnit);

                    } else if (metricTotalCountName.equals(metricName)) {
                        cmm = new ComputedMonitorMetrics(MetricRegistry.Type.VENDOR.getName(), mID, TOTAL, metricTotalUnit);
                    }
                    
                    if (cmm != null) {
                        computedMonitorMetricsSet.add(cmm);
                    }
                }
            }

            // Once the existing metrics needed for calculation are cached, register new computed metric.
            MetricID computedMetricID = registerNewComputedMetricWithExistingMetricTag(metricData, computedMetricTagNames);

            // Populating map with newly registered computed metric and corresponding metrics set needed for computation.
            computationMetricsMap.put(computedMetricID, computedMonitorMetricsSet);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                        "Created computed metric for vendor computation :  " + computedMetricID.toString());
            }
        }
    }

    private Tag[] getComputedMetricsTags(MetricID metricId) {
        Map<String, String> metricTagMap = metricId.getTags();
        Tag[] metricsTagsArr = null;
        
        // The _app tag is a reserved tag in mpMetrics, we cannot register or retrieve a metric with
        // a metricID that contains the _app tag. Need to remove it from the metricID 
        // before registering or retrieving a metric.
        if (metricTagMap.containsKey("_app")) {
            Map<String, String> tempTagMaps = new HashMap<>(metricTagMap);
            tempTagMaps.remove("_app");
            
            List<Tag> metricsTagList = new ArrayList<>();
            for (Map.Entry<String, String> entry : tempTagMaps.entrySet()) {
                metricsTagList.add(new Tag(entry.getKey(), entry.getValue()));
            }
            metricsTagsArr = metricsTagList.toArray(new Tag[metricsTagList.size()]);
        }
        else {
            // If no _app tag is present in the metricID, just return tags as-is.
            metricsTagsArr = metricId.getTagsAsArray();
        }
        return metricsTagsArr;
    }

    public MetricID registerNewComputedMetricWithExistingMetricTag(String[] metricData, Tag[] existingMetricTags) {
        MetricRegistry metricRegistry = getMetricRegistry(MetricRegistry.Type.VENDOR.getName());
        String computedMetricName = metricData[ComputedMappingTable.COMPUTED_METRIC_NAME];
        String computedMetricDisplayName = metricData[ComputedMappingTable.COMPUTED_METRIC_DISPLAYNAME];
        MetricType metricType = MetricType.valueOf(metricData[ComputedMappingTable.COMPUTED_METRIC_TYPE]);

        MetricID computedMetricID = new MetricID(computedMetricName, existingMetricTags);

        // Resolve the resource bundle metric description.
        String description = Tr.formatMessage(tc, metricData[ComputedMappingTable.COMPUTED_METRIC_DESCRIPTION]);
        String unit = metricData[ComputedMappingTable.COMPUTED_METRIC_UNIT];

        Metadata metadata = Metadata.builder().withName(computedMetricName).withDisplayName(computedMetricDisplayName).withDescription(description).withUnit(unit).build();

        if (MetricType.GAUGE.equals(metricType)) {
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
        MetricRegistry baseRegistry = getMetricRegistry(MetricRegistry.Type.VENDOR.getName());

        for (MetricID metricIDToRemove : computationMetricsMap.keySet()) {
            boolean rc = baseRegistry.remove(metricIDToRemove);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                        "Unregistered computed metric : " + metricIDToRemove.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
            }
        }

        // Clear all the computed metric maps
        computationMetricsMap.clear();
        finalComputedMetricsMap.clear();
    }

    public void unregister(Set<MetricID> monitorMetricsIDSet, String mpAppName) {
        MetricID computedMetricID = null;
        for (MetricID mID : monitorMetricsIDSet) {
            computedMetricID = getComputedMetricIDToRemove(mID);
            if (computedMetricID != null) {
                // Remove the corresponding computed metricID.
                removeComputedMetrics(computedMetricID, mpAppName);
            }
        }
    }
    
    public void unregisterComputedRESTMetricsByAppName(String appName) {
        for (Entry<MetricID, Set<ComputedMonitorMetrics>> entry : computationMetricsMap.entrySet()) {
            MetricID computedMetricID = entry.getKey();
            for (ComputedMonitorMetrics cmm : entry.getValue()) {
                String restAppName = cmm.getAppName();
                String mpAppNameConfigValue = cmm.getMpAppNameConfigValue();
                if (restAppName != null) {
                    if (restAppName.equals(appName)) {
                        removeComputedMetrics(computedMetricID, mpAppNameConfigValue);
                        break; // Removed the computed REST metric for the corresponding app, no need to continue the loop.
                    }
                }
            }
        }
    }

    public void removeComputedMetrics(MetricID computedMetricID, String mpAppName) {
        // Remove the corresponding computed metricID.
        computationMetricsMap.remove(computedMetricID);
        finalComputedMetricsMap.remove(computedMetricID);
        
        // Check if the mpAppName is set via MpConfig.
        if (mpAppName != null && !mpAppName.isEmpty()) {
            computedMetricID = mergeMPAppTag(computedMetricID, mpAppName);
        }

        MetricRegistry vendorRegistry = getMetricRegistry(MetricRegistry.Type.VENDOR.getName());
        boolean rc = vendorRegistry.remove(computedMetricID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,
                    "Unregistered computed vendor metric : " + computedMetricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
        }
    }

    private MetricID getComputedMetricIDToRemove(MetricID monitorMetricID) {
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
    
    private MetricID mergeMPAppTag(MetricID mid, String appNameValue) {
        Tag appTag = new Tag("_app", appNameValue);
        
        Tag[] tempArr = Arrays.copyOf(mid.getTagsAsArray(), mid.getTagsAsArray().length + 1);
        tempArr[tempArr.length - 1] = appTag;
        
        return new MetricID(mid.getName(), tempArr);
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

        for (Map.Entry<MetricID, Set<ComputedMonitorMetrics>> entry : computationMetricsMap.entrySet()) {
            Double metricVal = null, diffDuration = null, diffTotalCount = null;
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
            } else {
                for (ComputedMonitorMetrics cmm : monitorMetrics) {
                    mr = getMetricRegistry(cmm.getMonitorMetricScope());
                    // Get the metric value.
                    metricVal = getMetricValue(mr, cmm);
                    if (metricVal != null) {
                        if (cmm.getComputationType().equals(DURATION)) {
                            diffDuration = cmm.getDifference(metricVal);
                        } else if (cmm.getComputationType().equals(TOTAL)) {
                            diffTotalCount = cmm.getDifference(metricVal);
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

    public void calculateHeapUsage(MetricID computedMetricID, Set<ComputedMonitorMetrics> monitorMetrics) {
        double usedHeap = 0.0, maxHeap = 0.0, heapUsage = 0.0;
        MetricRegistry mr = getMetricRegistry(MetricRegistry.Type.BASE.getName());
        for (ComputedMonitorMetrics cmm : monitorMetrics) {
            MetricID metricId = cmm.getMonitorMetricID();
            Gauge<?> currentGauge = mr.getGauge(metricId);
            double currentValue = ((Number) currentGauge.getValue()).doubleValue();
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
            metricNum = (Number) currentGauge.getValue();
        } else if (metricValue instanceof Counter) {
            Counter currentCount = (Counter) metricValue;
            metricNum = currentCount.getCount();
        } else if (metricValue instanceof org.eclipse.microprofile.metrics.SimpleTimer) {
            org.eclipse.microprofile.metrics.SimpleTimer currentTimer = (org.eclipse.microprofile.metrics.SimpleTimer) metricValue;
            if (cmm.getComputationType().equals(DURATION)) {
                Duration currentDur = currentTimer.getElapsedTime();
                if (currentDur != null)  {
                    // Get total elapsed time in nanoseconds from Duration API.
                    double tempDurNanos = (double) currentDur.toNanos();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "REST Timer ElapsedTime Duration in nanoseconds = " + tempDurNanos);
                    }
                    currentValue = tempDurNanos * NANOSECOND_CONVERSION; // convert to secs
                }
            } else {
                // Get Total Counter Value for Timer
                metricNum = currentTimer.getCount();
            }
        }
        
        if (metricNum != null && currentValue == null) {
            // If the metricValue is present and the currentValue is not already set. Should not be the case for REST_stats.
            if (cmm.getComputationType().equals(DURATION)) {
                // Check if the retrieved metric needed to be converted to seconds.
                final double unitConversionFactor = getUnitConversionFactor(cmm.getMetricUnit());
                currentValue = metricNum.doubleValue() * unitConversionFactor;
            }
            else {
                currentValue = metricNum.doubleValue();
            }
        }
        
        return currentValue;
    }

    private double getUnitConversionFactor(String metricUnit) {
        double convFactor = 1.0;
        if (metricUnit.equals(MetricUnits.NANOSECONDS)) {
            convFactor = NANOSECOND_CONVERSION;
        } 
        else if (metricUnit.equals(MetricUnits.MILLISECONDS)) {
            convFactor = MILLISECOND_CONVERSION;
        }
        return convFactor;
    }

    public MetricRegistry getMetricRegistry(String scope) {
        MetricRegistry metricRegistry = sharedMetricRegistry.getOrCreate(scope);
        return metricRegistry;
    }

    public void calculateEWMAValue(MetricID computedMetricID, double duration, double totalCount) {
        double computedVal = 0.0;

        // Calculate the new computed metric.
        // If the duration or the totalCount is a negative value, set the computedValue to be -1.0
        // Should only happen when calculating the gc.time.per.cycle, if the gc.time is unknown to
        // the JVM. The rest of the metrics needed for computation are monotonically increasing,
        // and will never result in a negative value.
        computedVal = ((duration < 0.0) || (totalCount < 0.0)) ? -1.0 : (duration / totalCount);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The computed value is " + computedVal);
        }
        
        String computedMetricName = computedMetricID.getName();
        if (computedMetricName.equals("gc.time.per.cycle") && computedVal == -1.0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The GC Time was unknown to the JVM, hence updating map for GCTime computed metric to -1.");
            }
            finalComputedMetricsMap.put(computedMetricID, computedVal);
        }
        else {
            // Only the computed metricIDs that require EWMA will be passed into this method.
            EWMA ewmaObj = (EWMA) finalComputedMetricsMap.get(computedMetricID);
            if (ewmaObj == null) {
                // Initialization of the EWMA object, for the first time.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "First initialization of the computed metric, creating EWMA object.");
                }
                double alpha = calculateAlpha(EWMA_MOVING_WINDOW_INTERVAL); // 5 min moving window
                ewmaObj = new EWMA(alpha);

                // EWMA[0] will be equal directly to the initially calculated value.
                double initialValue = (duration == 0.0 || totalCount == 0.0) ? 0.0 : computedVal;
                ewmaObj.updateNewValue(initialValue);
            } else {
                if (duration == 0.0 || totalCount == 0.0 || computedVal < 0.0) {
                    // If nothing changed during the current sampling period, or if the computed value is negative, get the previously calculated EWMA value and feed it into it again.
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
    }

    private double calculateAlpha(double movingWindowInMins) {
        double alpha, movingWindowInSecs, numOfDataSamplesInMovingWindow;

        // Get the moving window duration in seconds.
        movingWindowInSecs = movingWindowInMins * MINUTE_CONVERSION;

        // Calculate the number of data samples in the moving window, during the sampling period.
        numOfDataSamplesInMovingWindow = movingWindowInSecs / (COMPUTED_METRICS_TIMER_INTERVAL * MILLISECOND_CONVERSION); // Data is retrieved every 15 seconds.

        alpha = 2 / (numOfDataSamplesInMovingWindow + 1);

        return alpha;
    }
}

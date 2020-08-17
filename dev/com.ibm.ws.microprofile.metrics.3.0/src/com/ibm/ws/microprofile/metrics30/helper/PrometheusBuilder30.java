/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30.helper;

import java.util.Map;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.helper.Tag;
import com.ibm.ws.microprofile.metrics23.helper.PrometheusBuilder23;

/**
 *
 */
public class PrometheusBuilder30 extends PrometheusBuilder23 {

    private static final TraceComponent tc = Tr.register(PrometheusBuilder30.class);

    private static final String GAUGE_STRING = "gauge";
    private static final String COUNTER_STRING = "counter";

    /**
     * If there exists global tags then we will combine these tags with the provided
     * MetricID's tags. The returned combination of tags will be represented as a String in
     * 'key="value",key2="value2",...'. This is exactly the same as MetricID's getTagAsString()
     *
     * @param mid the MetricID
     * @return String of combined tags 'key="value",key2="value2",...' format
     */
    protected static String resolveTagsAsStringWithGlobalTags(MetricID mid) {
        org.eclipse.microprofile.metrics.Tag[] globalTags = Util30.getCachedGlobalTags();

        /*
         * If there exists global tags, then we will need to copy
         *
         */
        if (globalTags != null && globalTags.length != 0) {
            org.eclipse.microprofile.metrics.Tag[] applicationTags = mid.getTagsAsArray();
            org.eclipse.microprofile.metrics.Tag[] combinedTags = new org.eclipse.microprofile.metrics.Tag[globalTags.length + applicationTags.length];

            /*
             * Copying Global Tags first ensures that any application tag
             * with matching key will override the global tag when
             * it is copied into the array.
             *
             * This is because the MetricID will read through the array
             * and take the "newest" value.
             *
             * This is the behaviour in mpMetrics-2.x and below
             */
            System.arraycopy(globalTags, 0, combinedTags, 0, globalTags.length);
            System.arraycopy(applicationTags, 0, combinedTags, globalTags.length, applicationTags.length);

            MetricID temp = new MetricID("temporary", combinedTags);

            return temp.getTagsAsString();
        } else {
            return mid.getTagsAsString();
        }
    }

    public static void buildTimer30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        buildMetered30(builder, name, description, currentMetricMap);
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {

            getPromValueLine(builder, lineName, ((Timer) currentMetricMap.get(mid)).getElapsedTime().toNanos() * conversionFactor, resolveTagsAsStringWithGlobalTags(mid));
        }

        // Build Histogram
        buildSampling30(builder, name, description, currentMetricMap, conversionFactor, Constants.APPENDEDSECONDS);
    }

    public static void buildSimpleTimer30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        double conversionFactor = Constants.NANOSECONDCONVERSION;

        buildCounting30(builder, name, description, currentMetricMap);

        String lineName = name + "_elapsedTime_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((SimpleTimer) currentMetricMap.get(mid)).getElapsedTime().toNanos() * conversionFactor, resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_maxTimeDuration_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {
            Number value = (((SimpleTimer) currentMetricMap.get(mid)).getMaxTimeDuration() != null) ? ((SimpleTimer) currentMetricMap.get(mid)).getMaxTimeDuration().toNanos()
                                                                                                      * conversionFactor : Double.NaN;
            getPromValueLine(builder, lineName, value, resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_minTimeDuration_" + MetricUnits.SECONDS.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {
            Number value = (((SimpleTimer) currentMetricMap.get(mid)).getMinTimeDuration() != null) ? ((SimpleTimer) currentMetricMap.get(mid)).getMinTimeDuration().toNanos()
                                                                                                      * conversionFactor : Double.NaN;
            getPromValueLine(builder, lineName, value, resolveTagsAsStringWithGlobalTags(mid));
        }

    }

    public static void buildGauge30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor,
                                    String appendUnit) {
        getPromTypeLine(builder, name, GAUGE_STRING, appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);

        for (MetricID mid : currentMetricMap.keySet()) {
            // Skip non number values
            Number gaugeValNumber = null;
            Object gaugeValue = null;
            try {
                gaugeValue = ((Gauge) currentMetricMap.get(mid)).getValue();
            } catch (IllegalStateException e) {
                // The forwarding gauge is likely unloaded. A warning has already been emitted
                return;
            }
            if (!Number.class.isInstance(gaugeValue)) {
                if (!improperGaugeSet.contains(mid)) {
                    Tr.event(tc, "Skipping Prometheus output for Gauge: " + mid.toString() + " of type " + ((Gauge) currentMetricMap.get(mid)).getValue().getClass());
                    improperGaugeSet.add(mid);
                }
                return;
            }
            gaugeValNumber = (Number) gaugeValue;
            if (!(Double.isNaN(conversionFactor))) {
                gaugeValNumber = gaugeValNumber.doubleValue() * conversionFactor;
            }

            getPromValueLine(builder, name, gaugeValNumber, resolveTagsAsStringWithGlobalTags(mid), appendUnit);
        }

    }

    public static void buildCounter30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        /*
         * As per the microprofile metric specification for prometheus output
         * if the metric name already ends with "_total" do nothing.
         */
        String lineName = appendSuffixIfNeeded(getPrometheusMetricName(name), "total");

        getPromTypeLine(builder, lineName, COUNTER_STRING);
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((Counter) currentMetricMap.get(mid)).getCount(), resolveTagsAsStringWithGlobalTags(mid));
        }

    }

    public static void buildConcurrentGauge30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        String lineName = name + "_current";

        getPromTypeLine(builder, lineName, GAUGE_STRING);
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getCount(), resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_min";

        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getMin(), resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_max";

        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getMax(), resolveTagsAsStringWithGlobalTags(mid));
        }

    }

    public static void buildHistogram30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor,
                                        String appendUnit) {
        buildSampling30(builder, name, description, currentMetricMap, conversionFactor, appendUnit);
    }

    public static void buildMeter30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        buildCounting30(builder, name, description, currentMetricMap);
        buildMetered30(builder, name, description, currentMetricMap);
    }

    protected static void buildSampling30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor,
                                          String appendUnit) {

        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, GAUGE_STRING, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double meanVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMean() * conversionFactor : sampling.getSnapshot().getMean();
            getPromValueLine(builder, lineName, meanVal, resolveTagsAsStringWithGlobalTags(mid), appendUnit);
        }

        lineName = name + "_max";
        getPromTypeLine(builder, lineName, GAUGE_STRING, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double maxVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMax() * conversionFactor : sampling.getSnapshot().getMax();
            getPromValueLine(builder, lineName, maxVal, resolveTagsAsStringWithGlobalTags(mid), appendUnit);
        }

        lineName = name + "_min";
        getPromTypeLine(builder, lineName, GAUGE_STRING, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double minVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMin() * conversionFactor : sampling.getSnapshot().getMin();
            getPromValueLine(builder, lineName, minVal, resolveTagsAsStringWithGlobalTags(mid), appendUnit);
        }

        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, GAUGE_STRING, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double stdDevVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getStdDev() * conversionFactor : sampling.getSnapshot().getStdDev();
            getPromValueLine(builder, lineName, stdDevVal, resolveTagsAsStringWithGlobalTags(mid), appendUnit);
        }

        getPromTypeLine(builder, name, "summary", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            if (Counting.class.isInstance(sampling)) {
                getPromValueLine(builder, name, ((Counting) sampling).getCount(), resolveTagsAsStringWithGlobalTags(mid), appendUnit == null ? "_count" : appendUnit + "_count");
            }
            double medianVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMedian() * conversionFactor : sampling.getSnapshot().getMedian();
            double percentile75th = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().get75thPercentile()
                                                                          * conversionFactor : sampling.getSnapshot().get75thPercentile();
            double percentile95th = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().get95thPercentile()
                                                                          * conversionFactor : sampling.getSnapshot().get95thPercentile();
            double percentile98th = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().get98thPercentile()
                                                                          * conversionFactor : sampling.getSnapshot().get98thPercentile();
            double percentile99th = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().get99thPercentile()
                                                                          * conversionFactor : sampling.getSnapshot().get99thPercentile();
            double percentile999th = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().get999thPercentile()
                                                                           * conversionFactor : sampling.getSnapshot().get999thPercentile();
            getPromValueLine(builder, name, medianVal, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.5"), appendUnit);
            getPromValueLine(builder, name, percentile75th, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.75"), appendUnit);
            getPromValueLine(builder, name, percentile95th, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.95"), appendUnit);
            getPromValueLine(builder, name, percentile98th, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.98"), appendUnit);
            getPromValueLine(builder, name, percentile99th, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.99"), appendUnit);
            getPromValueLine(builder, name, percentile999th, resolveTagsAsStringWithGlobalTags(mid), new Tag(QUANTILE, "0.999"), appendUnit);
        }

    }

    protected static void buildCounting30(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        String lineName = name + "_total";
        getPromTypeLine(builder, lineName, COUNTER_STRING);
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((Counting) currentMetricMap.get(mid)).getCount(), resolveTagsAsStringWithGlobalTags(mid));
        }

    }

    protected static void buildMetered30(StringBuilder builder, String name, String description, Map<MetricID, Metric> map) {
        String lineName = name + "_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getMeanRate(), resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_one_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getOneMinuteRate(), resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_five_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getFiveMinuteRate(), resolveTagsAsStringWithGlobalTags(mid));
        }

        lineName = name + "_fifteen_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, GAUGE_STRING);
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getFifteenMinuteRate(), resolveTagsAsStringWithGlobalTags(mid));
        }

    }
}

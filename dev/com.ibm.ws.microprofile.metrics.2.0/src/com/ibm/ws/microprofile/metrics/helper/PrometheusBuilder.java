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
package com.ibm.ws.microprofile.metrics.helper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.metrics.Constants;

/**
 *
 */
public class PrometheusBuilder {

    private static final TraceComponent tc = Tr.register(PrometheusBuilder.class);

    private static final String QUANTILE = "quantile";
    private static Set<MetricID> improperGaugeSet = new HashSet<MetricID>();

    @FFDCIgnore({ IllegalStateException.class })

    public static void buildGauge(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor,
                                  String appendUnit) {
        getPromTypeLine(builder, name, "gauge", appendUnit);
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

            getPromValueLine(builder, name, gaugeValNumber, mid.getTagsAsString(), appendUnit);
        }

    }

    /**
     * @param builder
     * @param metricNamePrometheus
     * @param description
     * @param currentMetricMap
     */
    public static void buildCounter(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        /*
         * As per the microprofile metric specification for prometheus output
         * if the metric name already ends with "_total" do nothing.
         */
        String lineName = appendSuffixIfNeeded(getPrometheusMetricName(name), "total");

        getPromTypeLine(builder, lineName, "counter");
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((Counter) currentMetricMap.get(mid)).getCount(), mid.getTagsAsString());
        }

    }

    /**
     * @param builder
     * @param metricNamePrometheus
     * @param description
     * @param currentMetricMap
     */
    public static void buildConcurrentGauge(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        String lineName = name + "_current";

        getPromTypeLine(builder, lineName, "gauge");
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getCount(), mid.getTagsAsString());
        }

        lineName = name + "_min";

        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getMin(), mid.getTagsAsString());
        }

        lineName = name + "_max";

        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((ConcurrentGauge) currentMetricMap.get(mid)).getMax(), mid.getTagsAsString());
        }

    }

    public static void buildTimer(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        buildMetered(builder, name, description, currentMetricMap);
        double conversionFactor = Constants.NANOSECONDCONVERSION;
        // Build Histogram
        buildSampling(builder, name, description, currentMetricMap, conversionFactor, Constants.APPENDEDSECONDS);
    }

    public static void buildHistogram(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor,
                                      String appendUnit) {
        buildSampling(builder, name, description, currentMetricMap, conversionFactor, appendUnit);
    }

    public static void buildMeter(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        buildCounting(builder, name, description, currentMetricMap);
        buildMetered(builder, name, description, currentMetricMap);
    }

    private static void buildSampling(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap, Double conversionFactor, String appendUnit) {

        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double meanVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMean() * conversionFactor : sampling.getSnapshot().getMean();
            getPromValueLine(builder, lineName, meanVal, mid.getTagsAsString(), appendUnit);
        }

        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double maxVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMax() * conversionFactor : sampling.getSnapshot().getMax();
            getPromValueLine(builder, lineName, maxVal, mid.getTagsAsString(), appendUnit);
        }

        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double minVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getMin() * conversionFactor : sampling.getSnapshot().getMin();
            getPromValueLine(builder, lineName, minVal, mid.getTagsAsString(), appendUnit);
        }

        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            double stdDevVal = (!(Double.isNaN(conversionFactor))) ? sampling.getSnapshot().getStdDev() * conversionFactor : sampling.getSnapshot().getStdDev();
            getPromValueLine(builder, lineName, stdDevVal, mid.getTagsAsString(), appendUnit);
        }

        getPromTypeLine(builder, name, "summary", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        for (MetricID mid : currentMetricMap.keySet()) {
            Sampling sampling = (Sampling) currentMetricMap.get(mid);
            if (Counting.class.isInstance(sampling)) {
                getPromValueLine(builder, name, ((Counting) sampling).getCount(), mid.getTagsAsString(), appendUnit == null ? "_count" : appendUnit + "_count");
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
            getPromValueLine(builder, name, medianVal, mid.getTagsAsString(), new Tag(QUANTILE, "0.5"), appendUnit);
            getPromValueLine(builder, name, percentile75th, mid.getTagsAsString(), new Tag(QUANTILE, "0.75"), appendUnit);
            getPromValueLine(builder, name, percentile95th, mid.getTagsAsString(), new Tag(QUANTILE, "0.95"), appendUnit);
            getPromValueLine(builder, name, percentile98th, mid.getTagsAsString(), new Tag(QUANTILE, "0.98"), appendUnit);
            getPromValueLine(builder, name, percentile99th, mid.getTagsAsString(), new Tag(QUANTILE, "0.99"), appendUnit);
            getPromValueLine(builder, name, percentile999th, mid.getTagsAsString(), new Tag(QUANTILE, "0.999"), appendUnit);
        }

    }

    protected static void buildCounting(StringBuilder builder, String name, String description, Map<MetricID, Metric> currentMetricMap) {
        String lineName = name + "_total";
        getPromTypeLine(builder, lineName, "counter");
        getPromHelpLine(builder, lineName, description);
        for (MetricID mid : currentMetricMap.keySet()) {
            getPromValueLine(builder, lineName, ((Counting) currentMetricMap.get(mid)).getCount(), mid.getTagsAsString());
        }

    }

    protected static void buildMetered(StringBuilder builder, String name, String description, Map<MetricID, Metric> map) {
        String lineName = name + "_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getMeanRate(), mid.getTagsAsString());
        }

        lineName = name + "_one_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getOneMinuteRate(), mid.getTagsAsString());
        }

        lineName = name + "_five_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getFiveMinuteRate(), mid.getTagsAsString());
        }

        lineName = name + "_fifteen_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        for (MetricID mid : map.keySet()) {
            getPromValueLine(builder, lineName, ((Metered) map.get(mid)).getFifteenMinuteRate(), mid.getTagsAsString());
        }

    }

    protected static void getPromValueLine(StringBuilder builder, String name, Number value, String tags) {
        getPromValueLine(builder, name, value, tags, null);
    }

    protected static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, Tag quantile, String appendUnit) {

        if (tags == null || tags.isEmpty()) {
            tags = quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        } else {
            tags = tags + "," + quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        }
        getPromValueLine(builder, name, value, tags, appendUnit);
    }

    protected static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, String appendUnit) {

        String metricName = getPrometheusMetricName(name);

        builder.append(metricName);

        if (appendUnit != null) {
            builder.append(appendUnit);
        }

        if (tags != null && tags.length() > 0) {
            builder.append("{").append(tags).append("}");
        }

        builder.append(" ").append(value).append('\n');
    }

    protected static void getPromHelpLine(StringBuilder builder, String name, String description) {
        getPromHelpLine(builder, name, description, null);
    }

    protected static void getPromHelpLine(StringBuilder builder, String name, String description, String appendUnit) {
        String metricName = getPrometheusMetricName(name);
        if (description != null && !description.isEmpty()) {
            builder.append("# HELP ").append(metricName);

            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(" ").append(description).append("\n");
        }
    }

    protected static void getPromTypeLine(StringBuilder builder, String name, String type) {
        getPromTypeLine(builder, name, type, null);
    }

    protected static void getPromTypeLine(StringBuilder builder, String name, String type, String appendUnit) {

        String metricName = getPrometheusMetricName(name);
        builder.append("# TYPE ").append(metricName);
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        builder.append(" ").append(type).append("\n");
    }

    /*
     * Create the Prometheus metric name by sanitizing some characters
     */
    protected static String getPrometheusMetricName(String name) {
        String out = name;

        //Change other special characters to underscore
        out = out.replaceAll("[-+_.!?@#$%^&*`'\\s]+", "_");

        //PR 214- first char shouldn't be 0-9
        out = out.replaceAll("^[0-9]*(.*?)", "$1");

        //non-ascii characters to "" -- does this affect other languages?
        out = out.replaceAll("[^A-Za-z0-9_]", "");

        return out;
    }

    protected static String appendSuffixIfNeeded(String metricName, String suffix) {
        return (!metricName.endsWith("_" + suffix)) ? metricName + "_" + suffix : metricName;
    }

}

/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.helper;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.Timer;

/**
 *
 */
public class PrometheusBuilder {
    private static final String QUANTILE = "quantile";

    public static void buildGauge(StringBuilder builder, String name, Gauge<?> gauge, String description, Double conversionFactor, String tags, String appendUnit) {
        double gaugeVal = 0;
        Number gaugeValNumber = (Number) gauge.getValue();
        if (!(Double.isNaN(conversionFactor))) {
            gaugeVal = gaugeValNumber.doubleValue() * conversionFactor;
            gaugeValNumber = gaugeVal;
        }
        getPromTypeLine(builder, name, "gauge", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        getPromValueLine(builder, name, gaugeValNumber, tags, appendUnit);
    }

    public static void buildCounter(StringBuilder builder, String name, Counter counter, String description, String tags) {
        getPromTypeLine(builder, name, "counter");
        getPromHelpLine(builder, name, description);
        getPromValueLine(builder, name, counter.getCount(), tags);
    }

    public static void buildTimer(StringBuilder builder, String name, Timer timer, String description, String tags) {
        buildMetered(builder, name, timer, description, tags);
        // Build Histogram
        buildSampling(builder, name, timer, description, tags);
    }

    public static void buildHistogram(StringBuilder builder, String name, Histogram histogram, String description, Double conversionFactor, String tags,
                                      String appendUnit) {
        buildCounting(builder, name, histogram, description, tags);
        // Build Histogram
        buildSampling(builder, name, histogram, description, conversionFactor, tags, appendUnit);
    }

    public static void buildMeter(StringBuilder builder, String name, Meter meter, String description, String tags) {
        buildMetered(builder, name, meter, description, tags);
    }

    /**
     * Builds the Prometheus summary information
     *
     * @param builder
     * @param name
     * @param sampling
     */
    private static void buildSampling(StringBuilder builder, String name, Sampling sampling, String description, String tags) {
        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, sampling.getSnapshot().getMean());
        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, sampling.getSnapshot().getMax());
        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, sampling.getSnapshot().getMin());
        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, sampling.getSnapshot().getStdDev());

        getPromTypeLine(builder, name, "summary");

        getPromValueLine(builder, name, sampling.getSnapshot().getMedian(), tags, new Tag(QUANTILE, "0.5"), null);
        getPromValueLine(builder, name, sampling.getSnapshot().get75thPercentile(), tags, new Tag(QUANTILE, "0.75"), null);
        getPromValueLine(builder, name, sampling.getSnapshot().get95thPercentile(), tags, new Tag(QUANTILE, "0.95"), null);
        getPromValueLine(builder, name, sampling.getSnapshot().get98thPercentile(), tags, new Tag(QUANTILE, "0.98"), null);
        getPromValueLine(builder, name, sampling.getSnapshot().get99thPercentile(), tags, new Tag(QUANTILE, "0.99"), null);
        getPromValueLine(builder, name, sampling.getSnapshot().get999thPercentile(), tags, new Tag(QUANTILE, "0.999"), null);
    }

    private static void buildSampling(StringBuilder builder, String name, Sampling sampling, String description, Double conversionFactor, String tags,
                                      String appendUnit) {

        double meanVal = sampling.getSnapshot().getMean();
        double maxVal = sampling.getSnapshot().getMax();
        double minVal = sampling.getSnapshot().getMin();
        double stdDevVal = sampling.getSnapshot().getStdDev();
        double medianVal = sampling.getSnapshot().getMedian();
        double percentile75th = sampling.getSnapshot().get75thPercentile();
        double percentile95th = sampling.getSnapshot().get95thPercentile();
        double percentile98th = sampling.getSnapshot().get98thPercentile();
        double percentile99th = sampling.getSnapshot().get99thPercentile();
        double percentile999th = sampling.getSnapshot().get999thPercentile();

        if (!(Double.isNaN(conversionFactor))) {
            meanVal = sampling.getSnapshot().getMean() * conversionFactor;
            maxVal = sampling.getSnapshot().getMax() * conversionFactor;
            minVal = sampling.getSnapshot().getMin() * conversionFactor;
            stdDevVal = sampling.getSnapshot().getStdDev() * conversionFactor;
            medianVal = sampling.getSnapshot().getMedian() * conversionFactor;
            percentile75th = sampling.getSnapshot().get75thPercentile() * conversionFactor;
            percentile95th = sampling.getSnapshot().get95thPercentile() * conversionFactor;
            percentile98th = sampling.getSnapshot().get98thPercentile() * conversionFactor;
            percentile99th = sampling.getSnapshot().get99thPercentile() * conversionFactor;
            percentile999th = sampling.getSnapshot().get999thPercentile() * conversionFactor;
        }

        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, meanVal, tags, appendUnit);
        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, maxVal, tags, appendUnit);
        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, minVal, tags, appendUnit);
        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, stdDevVal, tags, appendUnit);

        getPromTypeLine(builder, name, "summary");

        getPromValueLine(builder, name, medianVal, tags, new Tag(QUANTILE, "0.5"), appendUnit);
        getPromValueLine(builder, name, percentile75th, tags, new Tag(QUANTILE, "0.75"), appendUnit);
        getPromValueLine(builder, name, percentile95th, tags, new Tag(QUANTILE, "0.95"), appendUnit);
        getPromValueLine(builder, name, percentile98th, tags, new Tag(QUANTILE, "0.98"), appendUnit);
        getPromValueLine(builder, name, percentile99th, tags, new Tag(QUANTILE, "0.99"), appendUnit);
        getPromValueLine(builder, name, percentile999th, tags, new Tag(QUANTILE, "0.999"), appendUnit);
    }

    private static void buildCounting(StringBuilder builder, String name, Counting counting, String description, String tags) {
        String lineName = name + "_count";
        getPromTypeLine(builder, lineName, "counter");
        getPromHelpLine(builder, name, description);
        getPromValueLine(builder, lineName, counting.getCount(), tags);
    }

    /**
     *
     *
     * @param builder
     * @param name
     * @param metered
     */
    private static void buildMetered(StringBuilder builder, String name, Metered metered, String description, String tags) {
        buildCounting(builder, name, metered, description, tags);

        String lineName = name + "_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getMeanRate(), tags);

        lineName = name + "_one_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getOneMinuteRate(), tags);

        lineName = name + "_five_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFiveMinuteRate(), tags);

        lineName = name + "_fifteen_min_rate_" + MetricUnits.PER_SECOND.toString();
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFifteenMinuteRate(), tags);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value) {
        getPromValueLine(builder, name, value, null, null);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags) {
        getPromValueLine(builder, name, value, tags, null);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, Tag quantile, String appendUnit) {

        if (tags == null || tags.isEmpty()) {
            tags = quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        } else {
            tags = tags + "," + quantile.getKey() + "=\"" + quantile.getValue() + "\"";
        }
        getPromValueLine(builder, name, value, tags, appendUnit);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, String appendUnit) {

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

    private static void getPromHelpLine(StringBuilder builder, String name, String description) {
        getPromHelpLine(builder, name, description, null);
    }

    private static void getPromHelpLine(StringBuilder builder, String name, String description, String appendUnit) {
        String metricName = getPrometheusMetricName(name);
        if (description != null) {
            builder.append("# HELP ").append(metricName);

            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(" ").append(description).append("\n");
        }
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type) {
        getPromTypeLine(builder, name, type, null);
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type, String appendUnit) {

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
    private static String getPrometheusMetricName(String name) {

        String out = name.replace('-', '_').replace('.', '_').replace(' ', '_');

        // convert camelCase to snake_case
        out = out.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();

        out = out.replace("__", "_");
        out = out.replace(":_", ":");

        return out;
    }
}

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

/**
 *
 */
public class Util {
    private static final TraceComponent tc = Tr.register(Util.class);

    public static SharedMetricRegistries SHARED_METRIC_REGISTRIES;

    public static Map<MetricID, Metric> getMetricsAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);

        Map<MetricID, Metric> metricMap = registry.getMetrics();

        Map<MetricID, Metric> returnMap = new HashMap<MetricID, Metric>();

        Set<MetricID> potentialMatches = new HashSet<MetricID>();
        for (MetricID tempmid : metricMap.keySet()) {
            if (tempmid.getName().equals(metricName)) {
                potentialMatches.add(tempmid);
            }
        }

        if (metricMap.isEmpty()) {
            throw new EmptyRegistryException();
        } else if (potentialMatches.size() == 0) {
            throw new NoSuchMetricException();
        } else {
            for (MetricID tmid : potentialMatches) {
                returnMap.put(tmid, metricMap.get(tmid));
            }
        }
        return returnMap;
    }

    public static Map<MetricID, Metric> getMetricsAsMap(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        Map<MetricID, Metric> metricMap = registry.getMetrics();
        if (metricMap.isEmpty()) {
            throw new EmptyRegistryException();
        }
        return metricMap;
    }

    public static Map<String, Metadata> getMetricsMetadataAsMap(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metadata> metricMetadataMap = registry.getMetadata();
        if (metricMetadataMap.isEmpty()) {
            throw new EmptyRegistryException();
        }
        return metricMetadataMap;
    }

    public static Map<String, Metadata> getMetricsMetadataAsMap(String registryName,
                                                                String metricName) throws NoSuchRegistryException, EmptyRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metadata> metricMetadataMap = registry.getMetadata();
        Map<String, Metadata> returnMap = new HashMap<String, Metadata>();
        if (metricMetadataMap.isEmpty()) {
            throw new EmptyRegistryException();
        } else if (!(metricMetadataMap.containsKey(metricName))) {
            throw new NoSuchMetricException();
        } else {
            returnMap.put(metricName, metricMetadataMap.get(metricName));
        }
        return returnMap;
    }

    protected static MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        if (!Constants.REGISTRY_NAMES_LIST.contains(registryName)) {
            Tr.event(tc, Constants.REGISTRY_NAMES_LIST.toString());
            throw new NoSuchRegistryException();
        }
        return SHARED_METRIC_REGISTRIES.getOrCreate(registryName);
    }

    public static Map<String, Number> getTimerNumbers(Timer timer, String tags, double conversionFactor) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, timer.getCount());
        results.put(Constants.MEAN_RATE + tags, timer.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE + tags, timer.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE + tags, timer.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE + tags, timer.getFifteenMinuteRate());

        results.put(Constants.MAX + tags, timer.getSnapshot().getMax() / conversionFactor);
        results.put(Constants.MEAN + tags, timer.getSnapshot().getMean() / conversionFactor);
        results.put(Constants.MIN + tags, timer.getSnapshot().getMin() / conversionFactor);

        results.put(Constants.STD_DEV + tags, timer.getSnapshot().getStdDev() / conversionFactor);

        results.put(Constants.MEDIAN + tags, timer.getSnapshot().getMedian() / conversionFactor);
        results.put(Constants.PERCENTILE_75TH + tags, timer.getSnapshot().get75thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_95TH + tags, timer.getSnapshot().get95thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_98TH + tags, timer.getSnapshot().get98thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_99TH + tags, timer.getSnapshot().get99thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_999TH + tags, timer.getSnapshot().get999thPercentile() / conversionFactor);

        return results;
    }

    public static Map<String, Number> getHistogramNumbers(Histogram histogram, String tags) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, histogram.getCount());

        results.put(Constants.MAX + tags, histogram.getSnapshot().getMax());
        results.put(Constants.MEAN + tags, histogram.getSnapshot().getMean());
        results.put(Constants.MIN + tags, histogram.getSnapshot().getMin());

        results.put(Constants.STD_DEV + tags, histogram.getSnapshot().getStdDev());

        results.put(Constants.MEDIAN + tags, histogram.getSnapshot().getMedian());
        results.put(Constants.PERCENTILE_75TH + tags, histogram.getSnapshot().get75thPercentile());
        results.put(Constants.PERCENTILE_95TH + tags, histogram.getSnapshot().get95thPercentile());
        results.put(Constants.PERCENTILE_98TH + tags, histogram.getSnapshot().get98thPercentile());
        results.put(Constants.PERCENTILE_99TH + tags, histogram.getSnapshot().get99thPercentile());
        results.put(Constants.PERCENTILE_999TH + tags, histogram.getSnapshot().get999thPercentile());

        return results;
    }

    public static Map<String, Number> getMeterNumbers(Meter meter, String tags) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, meter.getCount());
        results.put(Constants.MEAN_RATE + tags, meter.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE + tags, meter.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE + tags, meter.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE + tags, meter.getFifteenMinuteRate());

        return results;
    }

    public static Map<String, Number> getConcurrentGaugeNumbers(ConcurrentGauge concurrentgauge, String tags) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.CURRENT + tags, concurrentgauge.getCount());
        results.put(Constants.MIN + tags, concurrentgauge.getMin());
        results.put(Constants.MAX + tags, concurrentgauge.getMax());
        return results;
    }
}

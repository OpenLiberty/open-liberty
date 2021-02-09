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
import java.util.Map;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

/**
 *
 */
public class Util {

    public static SharedMetricRegistries SHARED_METRIC_REGISTRIES;

    public static Map<String, Metric> getMetricsAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metric> metricMap = registry.getMetrics();
        Map<String, Metric> returnMap = new HashMap<String, Metric>();
        if (metricMap.isEmpty()) {
            throw new EmptyRegistryException();
        } else if (!(metricMap.containsKey(metricName))) {
            throw new NoSuchMetricException();
        } else {
            returnMap.put(metricName, metricMap.get(metricName));
        }
        return returnMap;
    }

    public static Map<String, Metric> getMetricsAsMap(String registryName) throws NoSuchRegistryException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metric> metricMap = registry.getMetrics();
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

    private static MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        if (!Constants.REGISTRY_NAMES_LIST.contains(registryName)) {
            throw new NoSuchRegistryException();
        }
        return SHARED_METRIC_REGISTRIES.getOrCreate(registryName);
    }

    public static Map<String, Number> getTimerNumbers(Timer timer, double conversionFactor) {
        Map<String, Number> results = new HashMap<String, Number>();

        // These do not need to be converted due to having their own unique unit/no unit
        results.put(Constants.COUNT, timer.getCount());
        results.put(Constants.MEAN_RATE, timer.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE, timer.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE, timer.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE, timer.getFifteenMinuteRate());

        results.put(Constants.MAX, (timer.getSnapshot().getMax()) / conversionFactor);
        results.put(Constants.MEAN, timer.getSnapshot().getMean() / conversionFactor);
        results.put(Constants.MIN, timer.getSnapshot().getMin() / conversionFactor);

        results.put(Constants.STD_DEV, timer.getSnapshot().getStdDev() / conversionFactor);

        results.put(Constants.MEDIAN, timer.getSnapshot().getMedian() / conversionFactor);
        results.put(Constants.PERCENTILE_75TH, timer.getSnapshot().get75thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_95TH, timer.getSnapshot().get95thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_98TH, timer.getSnapshot().get98thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_99TH, timer.getSnapshot().get99thPercentile() / conversionFactor);
        results.put(Constants.PERCENTILE_999TH, timer.getSnapshot().get999thPercentile() / conversionFactor);

        return results;
    }

    public static Map<String, Number> getHistogramNumbers(Histogram histogram) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT, histogram.getCount());

        results.put(Constants.MAX, histogram.getSnapshot().getMax());
        results.put(Constants.MEAN, histogram.getSnapshot().getMean());
        results.put(Constants.MIN, histogram.getSnapshot().getMin());

        results.put(Constants.STD_DEV, histogram.getSnapshot().getStdDev());

        results.put(Constants.MEDIAN, histogram.getSnapshot().getMedian());
        results.put(Constants.PERCENTILE_75TH, histogram.getSnapshot().get75thPercentile());
        results.put(Constants.PERCENTILE_95TH, histogram.getSnapshot().get95thPercentile());
        results.put(Constants.PERCENTILE_98TH, histogram.getSnapshot().get98thPercentile());
        results.put(Constants.PERCENTILE_99TH, histogram.getSnapshot().get99thPercentile());
        results.put(Constants.PERCENTILE_999TH, histogram.getSnapshot().get999thPercentile());

        return results;
    }

    public static Map<String, Number> getMeterNumbers(Meter meter) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT, meter.getCount());
        results.put(Constants.MEAN_RATE, meter.getMeanRate());
        results.put(Constants.ONE_MINUTE_RATE, meter.getOneMinuteRate());
        results.put(Constants.FIVE_MINUTE_RATE, meter.getFiveMinuteRate());
        results.put(Constants.FIFTEEN_MINUTE_RATE, meter.getFifteenMinuteRate());

        return results;
    }
}

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics30.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;
import com.ibm.ws.microprofile.metrics23.helper.Util23;
import com.ibm.ws.microprofile.metrics30.impl.MetricRegistry30Impl;

/**
 *
 */
public class Util30 extends Util23 {
    private static final TraceComponent tc = Tr.register(Util30.class);

    public static Map<MetricID, Metric> getMetricsAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, EmptyRegistryException {
        MetricRegistry registry = getRegistry(registryName);

        SortedSet<MetricID> metricIDSet = registry.getMetricIDs();

        Map<MetricID, Metric> returnMap = new HashMap<MetricID, Metric>();

        Set<MetricID> potentialMatches = new HashSet<MetricID>();

        //for each metricID... want to check if name equals..
        for (MetricID tempmid : metricIDSet) {
            if (tempmid.getName().equals(metricName)) {
                potentialMatches.add(tempmid);
            }
        }

        if (metricIDSet.isEmpty()) {
            throw new EmptyRegistryException();
        } else if (potentialMatches.size() == 0) {
            throw new NoSuchMetricException();
        } else {
            for (MetricID tmid : potentialMatches) {
                returnMap.put(tmid, registry.getMetric(tmid));
            }
        }
        return returnMap;
    }

    /**
     * This static utility function will call the MetricRegistry's getCachedGlobalTag() method to retrieve the server level global tags
     *
     * @return Tag[] An array of Tag that represents the server level global tags. This can be null if none has been defined/resolved.
     */
    public static Tag[] getCachedGlobalTags() {
        return MetricRegistry30Impl.getCachedGlobalTags();
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
        Map<String, Metadata> metricMetadataMap = new HashMap<String, Metadata>();//registry.getMetadata();
        for (String name : registry.getNames()) {
            metricMetadataMap.put(name, registry.getMetadata(name));
        }
        if (metricMetadataMap.isEmpty()) {
            throw new EmptyRegistryException();
        }
        return metricMetadataMap;
    }

    public static Map<String, Metadata> getMetricsMetadataAsMap(String registryName,
                                                                String metricName) throws NoSuchRegistryException, EmptyRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metadata> metricMetadataMap = new HashMap<String, Metadata>();
        for (String name : registry.getNames()) {
            metricMetadataMap.put(name, registry.getMetadata(name));
        }

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

    public static Map<String, Number> getTimerNumbers(Timer timer, String tags, double conversionFactor) {
        Map<String, Number> results = new HashMap<String, Number>();
        results.put(Constants.COUNT + tags, timer.getCount());
        results.put("elapsedTime" + tags, timer.getElapsedTime().toNanos());
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

    /*
     * To facilitate changes into the MicroProfile Specification in 3.0 we need to return a value that is an
     * Object because the maxTimeDuration or minTimeDuration could be a null object.
     */
    public static Map<String, Object> getSimpleTimerNumbersAsObjects(SimpleTimer simpleTimer, String tags, double conversionFactor) {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put(Constants.COUNT + tags, simpleTimer.getCount());
        results.put("elapsedTime" + tags, simpleTimer.getElapsedTime().toNanos());

        Number value = (simpleTimer.getMaxTimeDuration() != null) ? simpleTimer.getMaxTimeDuration().toNanos() * conversionFactor : null;
        results.put("maxTimeDuration" + tags, value);
        value = (simpleTimer.getMinTimeDuration() != null) ? simpleTimer.getMinTimeDuration().toNanos() * conversionFactor : null;
        results.put("minTimeDuration" + tags, value);
        return results;
    }

}

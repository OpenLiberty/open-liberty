/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.helper;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Snapshot;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.metrics30.internal.micrometer.PercentileHistogramBuckets;
import io.openliberty.microprofile.metrics30.setup.config.DefaultBucketConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.HistogramBucketConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.HistogramBucketMaxConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.HistogramBucketMinConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.MetricsConfigurationManager;
import io.openliberty.microprofile.metrics30.setup.config.TimerBucketConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.TimerBucketMaxConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.TimerBucketMinConfiguration;

public class BucketManager {
    private final Map<Double, BucketValue> buckets = new TreeMap<>();
    private final Map<String, Map<Double, BucketValue>> allBuckets = new TreeMap<>();
    private final BucketValue infiniteObject;
    private static boolean issuedBetaMessage = false;
    private static boolean issuedBetaWarning = false;

    private static final TraceComponent tc = Tr.register(BucketManager.class);

    public BucketManager(Metadata metadata) {
        String metricName = metadata.getName();
        infiniteObject = new BucketValue(0, metadata.getUnit());

        double[] bucketArr = null;

        Optional<String> defaultHistogramOption = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.percentiles-histogram.enabled", String.class);

        if (defaultHistogramOption.isPresent()) {
            DefaultBucketConfiguration defaultBucketConfig = MetricsConfigurationManager.getInstance().getDefaultBucketConfiguration(metricName);

            if (defaultBucketConfig != null && defaultBucketConfig.isEnabled()) {
                if (metadata.getType().equals("histogram")) {
                    NavigableSet<Double> defaultBucketsMicrometer = new TreeSet<>();

                    HistogramBucketMaxConfiguration defaultBucketMaxConfig = MetricsConfigurationManager.getInstance().getDefaultHistogramMaxBucketConfiguration(metricName);
                    HistogramBucketMinConfiguration defaultBucketMinConfig = MetricsConfigurationManager.getInstance().getDefaultHistogramMinBucketConfiguration(metricName);

                    double minHistogramValue = defaultBucketMinConfig != null ? defaultBucketMinConfig.getValue() : 0.00;
                    double maxHistogramValue = defaultBucketMaxConfig != null ? defaultBucketMaxConfig.getValue() : Double.MAX_VALUE;

                    defaultBucketsMicrometer.addAll(PercentileHistogramBuckets.getDefaultBuckets(minHistogramValue, maxHistogramValue, false));

                    Iterator<Double> itr = defaultBucketsMicrometer.iterator();

                    while (itr.hasNext()) {
                        buckets.put(itr.next(), new BucketValue(0, metadata.getUnit()));

                    }
                } else if (metadata.getType().equals("timer")) {
                    NavigableSet<Double> defaultBucketsMicrometer = new TreeSet<>();

                    TimerBucketMaxConfiguration defaultTimerMaxConfig = MetricsConfigurationManager.getInstance().getDefaultTimerMaxBucketConfiguration(metricName);
                    TimerBucketMinConfiguration defaultTimerMinConfig = MetricsConfigurationManager.getInstance().getDefaultTimerMinBucketConfiguration(metricName);

                    double minTimerValue = defaultTimerMinConfig != null ? defaultTimerMinConfig.getValue().toMillis() / 1000.0 : 0.001;
                    double maxTimerValue = defaultTimerMaxConfig != null ? defaultTimerMaxConfig.getValue().toMillis() / 1000.0 : 30;

                    defaultBucketsMicrometer.addAll(PercentileHistogramBuckets.getDefaultBuckets(minTimerValue, maxTimerValue, true));

                    Iterator<Double> itr = defaultBucketsMicrometer.iterator();

                    while (itr.hasNext()) {
                        buckets.put(itr.next(), new BucketValue(0, metadata.getUnit()));

                    }
                }

            }

        }

        Optional<String> histogramOptionalData = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.histogram.buckets", String.class);
        Optional<String> timerOptionalData = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.timer.buckets", String.class);

        if (histogramOptionalData.isPresent() || timerOptionalData.isPresent()) {
            HistogramBucketConfiguration bucketsConfig = MetricsConfigurationManager.getInstance().getHistogramBucketConfiguration(metricName);

            TimerBucketConfiguration timerBucketsConfig = MetricsConfigurationManager.getInstance().getTimerBucketConfiguration(metricName);

            if (metadata.getType().equals("histogram")) {
                if (bucketsConfig != null && bucketsConfig.getValues() != null
                    && bucketsConfig.getValues().length > 0) {
                    double[] vals = Stream.of(bucketsConfig.getValues()).mapToDouble(Double::doubleValue).toArray();

                    bucketArr = vals;

                    for (Double value : bucketArr) {
                        buckets.put(value, new BucketValue(0, metadata.getUnit()));
                    }
                    buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

                }
            }

            if (metadata.getType().equals("timer")) {
                if (timerBucketsConfig != null && timerBucketsConfig.getValues() != null
                    && timerBucketsConfig.getValues().length > 0) {
                    Duration[] vals = timerBucketsConfig.getValues();

                    for (Duration value : vals) {
                        try {
                            buckets.put(value.toMillis() / 1000.0, new BucketValue(0, metadata.getUnit()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    buckets.put(Double.POSITIVE_INFINITY, infiniteObject);
                }
            }

        }

        if (metadata != null && buckets != null && !buckets.isEmpty() && betaFenceCheck()) {
            allBuckets.put(metricName, buckets);
        }

    }

    private boolean betaFenceCheck() throws UnsupportedOperationException {
        // Not running beta edition, throw exception
        if (!ProductInfo.getBetaEdition()) {
            if (!issuedBetaWarning) {
                Tr.warning(tc, "This method is beta and is not available.");
                issuedBetaWarning = true;
            }
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = true;
            }
        }

        return issuedBetaMessage;
    }

    public void updateTimer(long value) {
        for (Map.Entry<String, Map<Double, BucketValue>> entry : allBuckets.entrySet()) {

            Map<Double, BucketValue> innerMap = entry.getValue();
            for (Map.Entry<Double, BucketValue> innerEntry : innerMap.entrySet()) {

                if (innerEntry.getKey() >= (value) / 1000000000.00)
                    innerEntry.getValue().increment();
            }
        }
    }

    public void updateHistogram(long value) {
        for (Map.Entry<String, Map<Double, BucketValue>> entry : allBuckets.entrySet()) {

            Map<Double, BucketValue> innerMap = entry.getValue();
            for (Map.Entry<Double, BucketValue> innerEntry : innerMap.entrySet()) {

                if (innerEntry.getKey() >= (value))
                    innerEntry.getValue().increment();
            }
        }
    }

    public static class BucketValue {
        private long value;
        private final String unit;

        public BucketValue(long value, String unit) {
            this.value = value;
            this.unit = unit;

        }

        public double getValue() {
            return value;
        }

        public String getUnit() {
            return unit;
        }

        public void increment() {
            value++;
        }
    }

    public static class Bucket {
        private final double value;
        private final BucketValue bucketValue;

        public Bucket(double value, BucketValue newBucket) {
            this.value = value;
            this.bucketValue = newBucket;
        }

    }

    public Map<String, Map<Double, BucketValue>> getBuckets() {
        return allBuckets;
    }

    public Snapshot getBucketsSnap() {
        return (Snapshot) buckets;
    }

}
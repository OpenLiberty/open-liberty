/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.helper;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Snapshot;

import com.ibm.ws.microprofile.metrics.Constants;

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
    private final Map<Double, BucketValue> buckets = new LinkedHashMap<>();
    private final Map<String, Map<Double, BucketValue>> allBuckets = new LinkedHashMap<>();
    private final BucketValue infiniteObject;

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

                    System.out.println(defaultTimerMaxConfig);
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

        Optional<String> input3 = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.histogram.buckets", String.class);
        Optional<String> input4 = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.timer.buckets", String.class);

        if (input3.isPresent() || input4.isPresent()) {
            HistogramBucketConfiguration bucketsConfig = MetricsConfigurationManager.getInstance().getHistogramBucketConfiguration(metricName);

            TimerBucketConfiguration timerBucketsConfig = MetricsConfigurationManager.getInstance().getTimerBucketConfiguration(metricName);

            if (bucketsConfig != null && bucketsConfig.getValues() != null
                && bucketsConfig.getValues().length > 0) {
                double[] vals = Stream.of(bucketsConfig.getValues()).mapToDouble(Double::doubleValue).toArray();
                System.out.println("Bucket(histogram) values: " + vals);

                bucketArr = vals;

                for (Double value : bucketArr) {
                    System.out.println("bucketValue: " + value + " -- " + metadata.getUnit());

                    buckets.put(value, new BucketValue(0, metadata.getUnit()));

                }
                buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

            } else if (timerBucketsConfig != null && timerBucketsConfig.getValues() != null
                       && timerBucketsConfig.getValues().length > 0) {

                Duration[] vals = timerBucketsConfig.getValues(); //DURATION

                System.out.println("Bucket(timer) values: " + vals);

                for (Duration value : vals) {
                    System.out.println("timerValue:" + value.toMillis());

                    try {
                        buckets.put(value.toMillis() / 1000.0, new BucketValue(0, metadata.getUnit()));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        System.out.println(e.getStackTrace());
                    }

                }
                buckets.put(Double.POSITIVE_INFINITY, infiniteObject);
            }

        } else
            System.out.println("CONFIG DOES NOT EXIST1111!!");

        if (metadata != null && buckets != null) {
            allBuckets.put(metricName, buckets);
            System.out.println("Added buckets for metricName: " + metricName);
            System.out.println(buckets);
        }

    }

    public void update(long value) {

        for (Map.Entry<String, Map<Double, BucketValue>> entry : allBuckets.entrySet()) {
            String outerKey = entry.getKey();
            Map<Double, BucketValue> innerMap = entry.getValue();
            for (Map.Entry<Double, BucketValue> innerEntry : innerMap.entrySet()) {

                Entry<String, Double> test = resolveConversionFactorXappendUnitEntry(innerEntry.getValue().getUnit());
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

    /**
     * Calculates the unit String suffix and conversion factor used for later calculations
     *
     * @param unit String that encompasses the unit needed to calculate appropriate conversion factor and value to append
     * @return Map.Entry<String, Double> that contains the unit string suffix and conversion factor
     */
    protected Map.Entry<String, Double> resolveConversionFactorXappendUnitEntry(String unit) {

        if (unit == null || unit.trim().isEmpty() || unit.equals(MetricUnits.NONE)) {
            return new AbstractMap.SimpleEntry<String, Double>(null, Double.NaN);

        } else if (unit.equals(MetricUnits.NANOSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.NANOSECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MICROSECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MICROSECONDCONVERSION);
        } else if (unit.equals(MetricUnits.SECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.SECONDCONVERSION);

        } else if (unit.equals(MetricUnits.MINUTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MINUTECONVERSION);

        } else if (unit.equals(MetricUnits.HOURS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.HOURCONVERSION);

        } else if (unit.equals(MetricUnits.DAYS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.DAYCONVERSION);

        } else if (unit.equals(MetricUnits.PERCENT)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDPERCENT, Double.NaN);

        } else if (unit.equals(MetricUnits.BYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.BYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KILOBYTECONVERSION);

        } else if (unit.equals(MetricUnits.MEGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.GIGABYTES)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIGABYTECONVERSION);

        } else if (unit.equals(MetricUnits.KILOBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KILOBITCONVERSION);

        } else if (unit.equals(MetricUnits.MEGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEGABITCONVERSION);
        } else if (unit.equals(MetricUnits.GIGABITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIGABITCONVERSION);

        } else if (unit.equals(MetricUnits.KIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.KIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MEBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.MEBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.GIBIBITS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDBYTES, Constants.GIBIBITCONVERSION);
        } else if (unit.equals(MetricUnits.MILLISECONDS)) {
            return new AbstractMap.SimpleEntry<String, Double>(Constants.APPENDEDSECONDS, Constants.MILLISECONDCONVERSION);
        } else {
            return new AbstractMap.SimpleEntry<String, Double>("_" + unit, Double.NaN);
        }
    }
}
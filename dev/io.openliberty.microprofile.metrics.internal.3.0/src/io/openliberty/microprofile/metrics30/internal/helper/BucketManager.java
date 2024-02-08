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
    private Map<Double, BucketValue> buckets;
    private final Map<String, Map<Double, BucketValue>> allBuckets = new LinkedHashMap<>();
    private final BucketValue infiniteObject;

    public BucketManager(Metadata metadata) {
        String metricName = metadata.getName();
        infiniteObject = new BucketValue(0, metadata.getUnit());

        double[] bucketArr = null;

        Optional<String> defaultHistogramOption = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.percentiles-histogram.enabled", String.class);

        if (defaultHistogramOption.isPresent()) {

            buckets = new LinkedHashMap<>();
            DefaultBucketConfiguration defaultBucketConfig = MetricsConfigurationManager.getInstance().getDefaultBucketConfiguration(metricName);

            if (defaultBucketConfig != null && defaultBucketConfig.isEnabled()) {
                if (metadata.getType().equals("histogram")) {
                    NavigableSet<Double> defaultBucketsMicrometer = new TreeSet<>();
                    System.out.println("IN HERE---1");
                    HistogramBucketMaxConfiguration defaultBucketMaxConfig = MetricsConfigurationManager.getInstance().getDefaultHistogramMaxBucketConfiguration(metricName);
                    HistogramBucketMinConfiguration defaultBucketMinConfig = MetricsConfigurationManager.getInstance().getDefaultHistogramMinBucketConfiguration(metricName);

                    System.out.println("IN HERE---2 -- " + defaultBucketMinConfig.getValue());
                    double minHistogramValue = defaultBucketMinConfig != null ? defaultBucketMinConfig.getValue() : 0.00;
                    double maxHistogramValue = defaultBucketMaxConfig != null ? defaultBucketMaxConfig.getValue() : Double.MAX_VALUE;

//                    if (defaultBucketMinConfig != null && defaultBucketMinConfig.getValue() != null
//                        && defaultBucketMinConfig.getValue() != Double.NaN)
                    System.out.println("IN HERE---3 -- " + minHistogramValue);
                    defaultBucketsMicrometer.addAll(PercentileHistogramBuckets.getDefaultBuckets(minHistogramValue, maxHistogramValue, false));

                    System.out.println("IN HERE---4");
                    Iterator<Double> itr = defaultBucketsMicrometer.iterator();

                    while (itr.hasNext()) {
                        buckets.put(itr.next(), new BucketValue(0, metadata.getUnit()));

                    }
                } else if (metadata.getType().equals("timer")) {
                    NavigableSet<Double> defaultBucketsMicrometer = new TreeSet<>();

                    TimerBucketMaxConfiguration defaultTimerMaxConfig = MetricsConfigurationManager.getInstance().getDefaultTimerMaxBucketConfiguration(metricName);
                    TimerBucketMinConfiguration defaultTimerMinConfig = MetricsConfigurationManager.getInstance().getDefaultTimerMinBucketConfiguration(metricName);

                    double minTimerValue = defaultTimerMinConfig != null ? defaultTimerMinConfig.getValue().getSeconds() : 0.001;
                    double maxTimerValue = defaultTimerMaxConfig != null ? defaultTimerMaxConfig.getValue().getSeconds() : 30;
//                        if (defaultBucketMinConfig != null && defaultBucketMinConfig.getValue() != null
//                            && defaultBucketMinConfig.getValue() != Double.NaN)
                    defaultBucketsMicrometer.addAll(PercentileHistogramBuckets.getDefaultBuckets(minTimerValue, maxTimerValue, true));

                    Iterator<Double> itr = defaultBucketsMicrometer.iterator();

                    while (itr.hasNext()) {
                        buckets.put(itr.next(), new BucketValue(0, metadata.getUnit()));

                    }
                }

            }

        }

        Optional<String> input3 = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.histogram.buckets", String.class);

        if (input3.isPresent()) {

            // for (HistogramBucketConfiguration test : HistogramBucketConfiguration.parse(input3.get())) {
            //  buckets = new LinkedHashMap<>();
            HistogramBucketConfiguration bucketsConfig = MetricsConfigurationManager.getInstance().getHistogramBucketConfiguration(metricName);

            TimerBucketConfiguration timerBucketsConfig = MetricsConfigurationManager.getInstance().getTimerBucketConfiguration(metricName);

            if (bucketsConfig != null && bucketsConfig.getValues() != null
                && bucketsConfig.getValues().length > 0) {
                double[] vals = Stream.of(bucketsConfig.getValues()).mapToDouble(Double::doubleValue).toArray();
                // System.out.println("Bucket values: " + vals);
                bucketArr = vals;
                System.out.println(bucketsConfig.getValues());

                for (Double value : bucketArr) {
                    System.out.println("VALUE: " + value + " -- " + metadata.getUnit());

                    buckets.put(value, new BucketValue(0, metadata.getUnit()));

                }
                buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

                System.out.println("Name(histogram): " + metadata);

            } else if (timerBucketsConfig != null && timerBucketsConfig.getValues() != null
                       && timerBucketsConfig.getValues().length > 0) {

                Duration[] vals = timerBucketsConfig.getValues(); //DURATION

                System.out.println("Bucket values: " + vals);
                //bucketArr = vals;

                for (Duration value : vals) {

//                    String unit = metadata.getUnit();
//                    System.out.println("TIMER UNIT: " + unit);
                    // if (unit == "null" || unit == "none")
                    //unit = MetricUnits.MILLISECONDS;
                    buckets.put((double) value.getSeconds(), new BucketValue(0, metadata.getUnit()));

                }
                buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

                System.out.println("Name(timer): " + metadata);

            }
//                else if (bucketsConfig != null && percentilesConfig.getValues() == null
//                                && bucketsConfig.isDisabled()) {
//                            //do nothing - percentiles were disabled
//                        }

            //}
        } else
            System.out.println("CONFIG DOES NOT EXIST1111!!");

        if (metadata != null && buckets != null)
            allBuckets.put(metricName, buckets);
        //  MetricPercentileConfiguration percentilesConfig = MetricsConfigurationManager.getInstance().getPercentilesConfiguration("myHisto.histogram");

        //BucketsConfig individual call for hardcoded metrics
        //HistogramBucketConfiguration bucketsConfig = MetricsConfigurationManager.getInstance().getHistogramBucketConfiguration("myHistoo.buckets");

        // DefaultBucketConfiguration defaultBucketConfig = MetricsConfigurationManager.getInstance().getDefaultBucketConfiguration("myHisto.histogram");

        //System.out.println("Percentiles Config: " + percentilesConfig.getValues());

//        for (String key : allBuckets.keySet()) {
//
//            Map<Double, BucketValue> innerMap = allBuckets.get(key);
//
//            for (Double innerKey : innerMap.keySet()) {
//                System.out.println("Inner key: " + innerKey + ", Value: " + innerMap.get(innerKey));
//            }
//        }

        // buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

    }

//    public BucketValue createObject(double value) {
//        BucketValue object = new BucketValue(0, "");
//        buckets.put(value, object);
//        return object;
//    }

    public void update(long value) {
//        for (Entry<String, Map<Double, BucketValue>> entry : allBuckets.entrySet()) {
//
////            if (entry.getKey() >= (double) value / 1000000000) {
////                entry.getValue().increment();
////            }
//        }

        for (Map.Entry<String, Map<Double, BucketValue>> entry : allBuckets.entrySet()) {
            String outerKey = entry.getKey();
            Map<Double, BucketValue> innerMap = entry.getValue();
            for (Map.Entry<Double, BucketValue> innerEntry : innerMap.entrySet()) {
                //   System.out.println("Unit: -- " + resolveConversionFactorXappendUnitEntry(innerEntry.getValue().getUnit()) + " -- Key: " + innerEntry.getKey() + " -- Value:"
                //                    + innerEntry.getValue().getValue());

                Entry<String, Double> test = resolveConversionFactorXappendUnitEntry(innerEntry.getValue().getUnit());
                //System.out.println("Conversion info: " + test.getKey() + " -- " + test.getValue());
                //if (innerEntry.getKey() >= (double) value / 1000000000)
                //System.out.println("updating with vlaue: " + innerEntry.getKey() + " -- " + value + " -- " + test.getValue());
                //if (innerEntry.getKey() >= (value * (test.getValue())))
                if (innerEntry.getKey() >= (value))
                    innerEntry.getValue().increment();
            }
        }
        //infiniteObject.increment();
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

    public static void main(String[] args) {
        BucketManager manager = new BucketManager(null);

        manager.update(7783548);
        // manager.update((long) 0.76);

        System.out.println("--------------Updated .45 & .74 ---------------");
        for (String key : manager.getBuckets().keySet()) {

            Map<Double, BucketValue> innerMap = manager.getBuckets().get(key);

            for (Double innerKey : innerMap.keySet()) {
                System.out.println("Inner key: " + innerKey + ", Value: " + innerMap.get(innerKey));
            }
        }

        manager.update((long) 0.95);
        manager.update((long) 0.988);

//        for (Double key : manager.getBuckets().keySet()) {
//            System.out.println("Bucket " + key + ": " + manager.getBuckets().get(key).getValue());
//        }
        //System.out.println("Bucket inf: " + manager.infiniteObject.getValue());

//        System.out.println("--------------Updated buckets 100 & 200---------------");
//        System.out.println("Bucket 1: " + manager.bucket1.getValue());
//        System.out.println("Bucket 2: " + manager.bucket2.getValue());
//        System.out.println("Bucket 3: " + manager.bucket3.getValue());
//        System.out.println("Bucket 4: " + manager.bucket4.getValue());
//        System.out.println("Bucket inf: " + manager.infiniteObject.getValue());

    }
}
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.Snapshot;

import io.smallrye.metrics.setup.config.DefaultBucketConfiguration;
import io.smallrye.metrics.setup.config.HistogramBucketConfiguration;
import io.smallrye.metrics.setup.config.MetricPercentileConfiguration;
import io.smallrye.metrics.setup.config.MetricsConfigurationManager;

//import io.openliberty.microprofile.metrics30.setup.config.DefaultBucketConfiguration;
//import io.openliberty.microprofile.metrics30.setup.config.HistogramBucketConfiguration;
//import io.openliberty.microprofile.metrics30.setup.config.MetricPercentileConfiguration;
//import io.openliberty.microprofile.metrics30.setup.config.MetricsConfigurationManager;

public class BucketManager {
    private final Map<Double, BucketValue> buckets;
    private final BucketValue infiniteObject;
//    private final BucketValue bucket1;
//    private final BucketValue bucket2;
//    private final BucketValue bucket3;
//    private final BucketValue bucket4;
//    private final BucketValue bucket5;
//    private final BucketValue bucket6;

    public BucketManager(Double[] bucketArr) {
        buckets = new LinkedHashMap<>();
        infiniteObject = new BucketValue(0);
//        bucket1 = new BucketValue(0);
//        buckets.put(0.0005, bucket1);
//        bucket2 = new BucketValue(0);
//        buckets.put(0.75, bucket2);
//        bucket3 = new BucketValue(0);
//        buckets.put(0.95, bucket3);
//        bucket4 = new BucketValue(0);
//        buckets.put(0.98, bucket4);
//        bucket5 = new BucketValue(0);
//        buckets.put(0.99, bucket5);
//        bucket6 = new BucketValue(0);
//        buckets.put(0.999, bucket6);

        Double[] defaultBuckets = new Double[] { 0.0005, 0.75, 0.95, 0.98, 0.999 };

        //Figure out how to get all the default buckets set here, whether we're allowed to use the micrometer algo to create the default 69 and 250+ buckets or not.
        //Maybe have a separate array for the defaults and a separate array for the configured buckets? Do we separate the output or merge them
        if (bucketArr == null || bucketArr.length == 0) {
            bucketArr = defaultBuckets;

        }

        for (Double value : bucketArr) {
            buckets.put(value, new BucketValue(0));
        }
        buckets.put(Double.POSITIVE_INFINITY, infiniteObject);

        MetricPercentileConfiguration percentilesConfig = MetricsConfigurationManager.getInstance().getPercentilesConfiguration("Name");

        HistogramBucketConfiguration bucketsConfig = MetricsConfigurationManager.getInstance().getHistogramBucketConfiguration("Name");

        DefaultBucketConfiguration defaultBucketConfig = MetricsConfigurationManager.getInstance().getDefaultBucketConfiguration("Name");

        System.out.println("Percentiles Config: " + percentilesConfig.getValues());
        System.out.println("Buckets Config: " + bucketsConfig.getValues());

    }

    public BucketValue createObject(double value) {
        BucketValue object = new BucketValue(0);
        buckets.put(value, object);
        return object;
    }

    public void update(long value) {
        for (Map.Entry<Double, BucketValue> entry : buckets.entrySet()) {
            if (entry.getKey() >= (double) value / 1000000000) {
                entry.getValue().increment();
            }
        }
        //infiniteObject.increment();
    }

    public static class BucketValue {
        private long value;

        public BucketValue(long value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public void increment() {
            value++;
        }
    }

    public Map<Double, BucketValue> getBuckets2() {
        return buckets;
    }

    public Map<Double, BucketValue> getBuckets6() {
        return buckets;
    }

    public Map<Double, BucketValue> getBuckets() {
        return buckets;
    }

    public Snapshot getBucketsSnap() {
        return (Snapshot) buckets;
    }

    public static void main(String[] args) {
        BucketManager manager = new BucketManager(null);

        //BucketValue bucket1 = manager.createObject(225);
//        BucketValue bucket2 = manager.createObject(50);
//        BucketValue bucket3 = manager.createObject(75);
//        BucketValue bucket4 = manager.createObject(100);

        System.out.println(manager.getBuckets2());

        manager.update(7783548);
        // manager.update((long) 0.76);

        System.out.println("--------------Updated .45 & .74 ---------------");
        for (Double key : manager.getBuckets().keySet()) {
            System.out.println("Bucket " + key + ": " + manager.getBuckets().get(key).getValue());
        }

        for (Double key : manager.getBuckets2().keySet()) {
            System.out.println(key + " -- " + manager.getBuckets().get(key).getValue());
        }
        manager.update((long) 0.95);
        manager.update((long) 0.988);

        for (Double key : manager.getBuckets().keySet()) {
            System.out.println("Bucket " + key + ": " + manager.getBuckets().get(key).getValue());
        }
        //System.out.println("Bucket inf: " + manager.infiniteObject.getValue());

//        System.out.println("--------------Updated buckets 100 & 200---------------");
//        System.out.println("Bucket 1: " + manager.bucket1.getValue());
//        System.out.println("Bucket 2: " + manager.bucket2.getValue());
//        System.out.println("Bucket 3: " + manager.bucket3.getValue());
//        System.out.println("Bucket 4: " + manager.bucket4.getValue());
//        System.out.println("Bucket inf: " + manager.infiniteObject.getValue());

    }
}
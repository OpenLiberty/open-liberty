/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.function.Supplier;

public class ThrottleState {

    public final long[] buckets;
    private int currentBucket = 0;
    private long lastBucketTime;

    private long runningTotal = 0;
    private long weightedRunningTotal = 0;

    private final double weightedBuckets = 5; //The most recent 5 buckets will be weighed higher during garbage collection
    private final double weightMultiplier = 2; //Weight the most recent 5 buckets twice as high as other buckets.

    private final int windowIntervals = 20;

    private final double bucketDurationMs;

    private long lastAccessTime;

    private final Supplier<Integer> maxMessagesSupplier;

    public ThrottleState(int throttleWindowDuration, Supplier<Integer> maxMessagesSupplier) {
        this.maxMessagesSupplier = maxMessagesSupplier;
        this.bucketDurationMs = throttleWindowDuration / windowIntervals;
        this.buckets = new long[windowIntervals];
        this.lastBucketTime = System.currentTimeMillis();
        this.lastAccessTime = System.currentTimeMillis();
    }

    /*
     * Ensure buckets are appropriately rotated, increment the runningTotal variables, and determine if logs should be throttled.
     */
    public synchronized boolean increment() {
        rotateBuckets();
        buckets[currentBucket]++;
        runningTotal++;
        lastAccessTime = System.currentTimeMillis();

        double weight = getBucketWeight(0);
        weightedRunningTotal += weight;

        return runningTotal > maxMessagesSupplier.get();
    }

    /*
     * Determine if bucket should be rotated and update the runningTotal variables accordingly.
     */
    private void rotateBuckets() {
        long now = System.currentTimeMillis();
        int elapsed = (int) ((now - lastBucketTime) / bucketDurationMs);

        if (elapsed > 0) {
            for (int i = 1; i <= Math.min(elapsed, buckets.length); i++) {
                int idx = (currentBucket + i) % buckets.length;

                double weight = getBucketWeight(i);
                weightedRunningTotal -= buckets[idx] * weight;

                runningTotal -= buckets[idx];
                buckets[idx] = 0;
            }
            currentBucket = (currentBucket + elapsed) % buckets.length;
            lastBucketTime += elapsed * bucketDurationMs;
        }
    }

    /*
     * Give the most recent 5 buckets a higher weight for the runningTotal to be used when running garbage collection
     */
    public double getBucketWeight(int offset) {
        return (offset < weightedBuckets) ? weightMultiplier : 1.0;
    }

    public synchronized long getRunningTotal() {
        return runningTotal;
    }

    public synchronized long getWeightedRunningTotal() {
        return weightedRunningTotal;
    }

    public synchronized long getLastAccessTime() {
        return lastAccessTime;
    }

}
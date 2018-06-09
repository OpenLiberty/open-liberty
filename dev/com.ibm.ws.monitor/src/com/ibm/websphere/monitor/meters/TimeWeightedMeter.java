/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.monitor.meters;

import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A meter that contains a exponentially decaying reservoir
 * that can be used to compute time weighed average for all the
 * data point being added to the reservoir. It is used to track
 * time averages like servlet response time or connection pool's
 * wait time and in use time.
 */
@Trivial
public class TimeWeightedMeter {
    private final LongAdderAdapter count = LongAdderProxy.create();
    private final Histogram histogram;
    private long currentTime;

    public TimeWeightedMeter() {
        this.histogram = new HistogramImpl();
        this.currentTime = 0;
    }

    /**
     * Mark the occurrence of an event.
     */
    public void mark() {
        mark(1);
    }

    /**
     * Mark the occurrence of a given number of events.
     *
     * @param n the number of events
     */
    public void mark(long n) {
        count.add(n);
    }

    public long getCount() {
        return histogram.getCount();
    }

    /**
     *
     * @return returns the time weighted average of all the data points in the reservoir
     */
    public double getMean() {
        // TODO Auto-generated method stub
        return getSnapshot().getMean();
    }

    /**
     *
     * @return returns the most recent entry in the reservoir
     */
    public double getCurrent() {
        // TODO Auto-generated method stub
        return currentTime;
    }

    /**
     * Add new data to the reservoir with value and time unit.
     *
     * @param duration
     * @param unit
     */
    public void update(long duration, TimeUnit unit) {
        update(unit.toNanos(duration));
    }

    /**
     * @return returns the snapshot of the current data in the reservoir
     */
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

    private void update(long duration) {
        if (duration >= 0) {
            currentTime = duration;
            histogram.update(duration);
            mark(duration);
        }
    }

}

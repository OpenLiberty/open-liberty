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
public class TimeWeightedMeter extends com.ibm.websphere.monitor.jmx.TimeWeightedMeter implements TimeWeightedMXBean {
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

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.TimeWeightedMXBean#getMean()
     */
    @Override
    public double getMean() {
        // TODO Auto-generated method stub
        return getSnapshot().getMean();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.TimeWeightedMXBean#getCurrent()
     */
    @Override
    public double getCurrent() {
        // TODO Auto-generated method stub
        return currentTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.TimeWeightedMXBean#update()
     */
    @Override
    public void update(long duration, TimeUnit unit) {
        update(unit.toNanos(duration));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.TimeWeightedMXBean#getSnapshot()
     */
    @Override
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

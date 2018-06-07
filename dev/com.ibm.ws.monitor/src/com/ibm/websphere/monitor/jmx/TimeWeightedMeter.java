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
package com.ibm.websphere.monitor.jmx;

import java.util.concurrent.TimeUnit;

import com.ibm.websphere.monitor.meters.Snapshot;

/**
 * A meter that contains a exponentially decaying reservoir
 * that can be used to compute time weighed average for all the
 * data point being added to the reservoir.
 *
 * @ibm-api
 */
public class TimeWeightedMeter extends Meter {
    long count;
    long mean;
    long currentTime;
    Snapshot snapshot;

    /**
     * returns the number of objects in the reservoir
     *
     * @return
     */
    public long getCount() {
        return count;
    }

    /**
     *
     * @return returns the time weighted average of all the data points in the reservoir
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return returns the snapshot of the current data in the reservoir
     */
    public Snapshot getSnapshot() {
        // TODO Auto-generated method stub
        return snapshot;
    }

    /**
     *
     * @return returns the most recent added data point
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
        // TODO Auto-generated method stub

    }
}

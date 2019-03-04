/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.impl;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.ConcurrentGauge;

public class ConcurrentGaugeImpl implements ConcurrentGauge {
    private final LongAdderAdapter count;
    private final AtomicLong curr_Max;
    private final AtomicLong curr_Min;
    private final AtomicLong max;
    private final AtomicLong min;
    private final AtomicLong currentMinute;

    public ConcurrentGaugeImpl() {
        this.count = LongAdderProxy.create();
        this.curr_Max = new AtomicLong(0);
        this.curr_Min = new AtomicLong(0);
        this.max = new AtomicLong(0);
        this.min = new AtomicLong(0);
        this.currentMinute = new AtomicLong(getCurrentMinute());
    }

    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public long getMax() {
        return max.get();
    }

    @Override
    public long getMin() {
        return min.get();
    }

    @Override
    public synchronized void inc() {
        checkIfNewMinute();
        count.increment();
        updateWatermark();
    }

    @Override
    public synchronized void dec() {
        checkIfNewMinute();
        count.decrement();
        updateWatermark();
    }

    private synchronized void checkIfNewMinute() {
        int newMinute = getCurrentMinute();
        if (newMinute > currentMinute.get()) {
            currentMinute.set(newMinute);

            max.set(curr_Max.get());
            min.set(curr_Min.get());

            curr_Max.set(count.sum());
            curr_Min.set(count.sum());
        }

    }

    private void updateWatermark() {
        long currCount = count.sum();
        if (currCount > curr_Max.get()) {
            curr_Max.set(count.sum());
        }
        if (currCount < curr_Min.get()) {
            curr_Min.set(count.sum());
        }
    }

    private int getCurrentMinute() {
        return LocalTime.now().getMinute();
    }

}

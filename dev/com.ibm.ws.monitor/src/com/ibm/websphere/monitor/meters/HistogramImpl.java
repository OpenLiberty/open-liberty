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

/**
 *
 */
public class HistogramImpl implements Histogram {
    private final ExpDecayingReservoir reservoir;
    private final LongAdderAdapter count;
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.Histogram#update(int)
     */

    /**
     * Creates a new {@link HistogramImpl} with default setting.
     */
    public HistogramImpl() {
        this.reservoir = new ExpDecayingReservoir();
        this.count = LongAdderProxy.create();
    }

    /**
     * Create a new {@link HistogramImpl} with custom size and lapha value
     *
     * @param size the number of items in the reservoir
     * @param alpha the alpha constants used for control the rate of decaying.
     *            Higher alpha value usually results faster convergence to the instant value,
     *            and hence the faster decaying rate.
     */
    public HistogramImpl(int size, double alpha) {
        this.reservoir = new ExpDecayingReservoir(size, alpha);
        this.count = LongAdderProxy.create();
    }

    @Override
    public void update(int value) {
        // TODO Auto-generated method stub
        update((long) value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.Histogram#update(long)
     */
    @Override
    public void update(long value) {
        // TODO Auto-generated method stub
        count.increment();
        reservoir.update(value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.Histogram#getCount()
     */
    @Override
    public long getCount() {
        // TODO Auto-generated method stub
        return count.sum();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.monitor.meters.Histogram#getSnapshot()
     */
    @Override
    public Snapshot getSnapshot() {
        // TODO Auto-generated method stub
        return reservoir.getSnapshot();
    }

}

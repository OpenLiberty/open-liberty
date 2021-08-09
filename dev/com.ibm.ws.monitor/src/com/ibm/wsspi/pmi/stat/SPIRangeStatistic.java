/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.stat;

import com.ibm.websphere.pmi.stat.WSRangeStatistic;

/**
 * WebSphere interface to instrument a Range statistic.
 * 
 * @ibm-spi
 */

public interface SPIRangeStatistic extends SPIStatistic, WSRangeStatistic {
    /** Updates high water mark and low water mark based on the input value */
    public void setWaterMark(long currentValue);

    /** Updates high water mark and low water mark based on the input value */
    public void setWaterMark(long lastSampleTime, long currentValue);

    /** Set the Range statistic with the following values */
    public void set(long lowWaterMark, long highWaterMark, long current, double integral,
                    long startTime, long lastSampleTime);

    /** Set the current value. The water marks will be updated automatically. */
    public void set(long currentValue);

    /** Set the current value. The water marks will be updated automatically. */
    public void set(long lastSampleTime, long val);

    /** Increment the current value by 1. The water marks will be updated automatically. */
    public void increment();

    /** Increment the current value by incVal. The water marks will be updated automatically. */
    public void increment(long incVal);

    /** Increment the current value by incVal. The water marks will be updated automatically. */
    public void increment(long lastSampleTime, long incVal);

    /*
     * public void incrementWithoutSync(long lastSampleTime, long val);
     * public void decrementWithoutSync(long lastSampleTime, long val);
     */

    /** Decrement the current value by 1. The water marks will be updated automatically. */
    public void decrement();

    /** Decrement the current value by incVal. The water marks will be updated automatically. */
    public void decrement(long decVal);

    /** Decrement the current value by incVal. The water marks will be updated automatically. */
    public void decrement(long lastSampleTime, long incVal);

    /** Set the current value. The water marks are not updated. */
    public void setLastValue(long val);

    /** Updates the intergal value. Typically, this method shouldn't be called from the application. */
    public long updateIntegral();

    /** Updates the intergal value. Typically, this method shouldn't be called from the application. */
    public long updateIntegral(long lastSampleTime);
}
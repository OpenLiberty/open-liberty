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

import com.ibm.websphere.pmi.stat.WSCountStatistic;

/**
 * WebSphere interface to instrument a Count statistic.
 * 
 * @ibm-spi
 */

public interface SPICountStatistic extends SPIStatistic, WSCountStatistic {
    /** Increment the Count statistic by 1 */
    public void increment();

    /** Increment the Count statistic by incVal */
    public void increment(long incVal);

    /** Increment the Count statistic by incVal */
    public void increment(long lastSampleTime, long incVal);

    /** Decrement the Count statistic by 1 */
    public void decrement();

    /** Decrement the Count statistic by decVal */
    public void decrement(long decVal);

    /** Decrement the Count statistic by decVal */
    public void decrement(long lastSampleTime, long incVal);

    /** Set the Count statistic with the following values */
    public void set(long count, long startTime, long lastSampleTime);

    /** Set the count to the following value */
    public void setCount(long value);
}

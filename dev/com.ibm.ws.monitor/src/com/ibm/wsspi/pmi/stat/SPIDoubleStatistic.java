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

import com.ibm.websphere.pmi.stat.WSDoubleStatistic;

/**
 * WebSphere interface to instrument a Double statistic.
 * 
 * @ibm-spi
 */

public interface SPIDoubleStatistic extends SPIStatistic, WSDoubleStatistic {
    /** Set the Double statistic with following values */
    public void set(double count, long startTime, long lastSampleTime);

    /** Set the double value */
    public void setDouble(double value);

    /** Increment the statistic by 1 */
    public void increment();

    /** Increment the statistic by the input value */
    public void increment(double val);

    /** Decrement the statistic by 1 */
    public void decrement();

    /** Decrement the statistic by the input value */
    public void decrement(double val);
}

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

import com.ibm.websphere.pmi.stat.WSAverageStatistic;

/**
 * WebSphere interface to instrument an Average statistic.
 * 
 * @ibm-spi
 */

public interface SPIAverageStatistic extends SPIStatistic, WSAverageStatistic {
    /** Add a measurement value to the Average statistic. */
    public void add(long val);

    /** Add a measurement value to the Average statistic. */
    public void add(long lastSampleTime, long val);

    /** Set the Average statistic with the following values. */
    public void set(long count, long min, long max, long total, double sumOfSquares,
                    long startTime, long lastSampleTime);
}

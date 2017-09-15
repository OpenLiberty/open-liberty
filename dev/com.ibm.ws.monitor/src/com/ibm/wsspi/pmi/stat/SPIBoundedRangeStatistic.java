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

import com.ibm.websphere.pmi.stat.WSBoundedRangeStatistic;

/**
 * WebSphere interface to instrument BoundedRange statistic.
 * 
 * @ibm-spi
 */

public interface SPIBoundedRangeStatistic extends SPIBoundaryStatistic, SPIRangeStatistic,
                                                  WSBoundedRangeStatistic {
    /** Set the Bounded Range statistic with the following values. */
    public void set(long lowerBound, long upperBound,
                    long lowWaterMark, long highWaterMark,
                    long current, double integral,
                    long startTime, long lastSampleTime);
}

/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.pmi.stat;

import com.ibm.websphere.pmi.stat.WSBoundaryStatistic;

/**
 * WebSphere interface to instrument a Boundary statistic.
 * 
 * @ibm-spi
 */
public interface SPIBoundaryStatistic extends SPIStatistic, WSBoundaryStatistic {
    /** Set the Boundary statistic with the following values */
    public void set(long lowerBound, long upperBound, long startTime, long lastSampleTime);

    /** Sets the low bound */
    public void setLowerBound(long lowerBound);

    /** Sets the upper bound */
    public void setUpperBound(long upperBound);
}

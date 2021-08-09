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

package com.ibm.websphere.pmi.stat;

/**
 * WebSphere Range statistic interface.
 * 
 * @ibm-api
 */

public interface WSRangeStatistic extends WSStatistic {
    /** Returns the highest value this attribute held since the beginning of the measurement. */
    public long getHighWaterMark();

    /** Returns the lowest value this attribute held since the beginning of the measurement. */
    public long getLowWaterMark();

    /** Returns the current value of this attribute. */
    public long getCurrent();

    /** Return the integral value of this attribute. */
    public double getIntegral();

    /** Returns the time-weighted mean value of this attribute. */
    public double getMean();
}

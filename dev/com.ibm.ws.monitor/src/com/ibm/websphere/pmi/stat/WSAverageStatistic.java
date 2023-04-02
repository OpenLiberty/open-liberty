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

package com.ibm.websphere.pmi.stat;

/**
 * WebSphere Average statistic to represent a simple average.
 * 
 * @ibm-api
 */

public interface WSAverageStatistic extends WSStatistic {
    /** Returns the number of samples involved in this statistic. */
    public long getCount();

    /** Returns the sum of the values of all the samples. */
    public long getTotal();

    /** Returns the mean or average (getTotal() divided by getCount()). */
    public double getMean();

    /** Returns the minimum value of all the samples. */
    public long getMin();

    /** Returns the maximum value of all the samples. */
    public long getMax();

    /** Returns the sum-of-squares of the values of all the samples. */
    public double getSumOfSquares();
}

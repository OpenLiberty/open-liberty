/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.management.j2ee.statistics;

/**
 * Specifies standard timing measurements for a given operation.
 */
public interface TimeStatistic extends Statistic {

    /*
     * Returns the number of times the operation was invoked since the beginning of
     * this measurement.
     */
    public long getCount();

    /*
     * Returns the maximum amount of time taken to complete one invocation of
     * this operation since the beginning of this measurement.
     */
    public long getMaxTime();

    /*
     * Returns the minimum amount of time taken to complete one invocation of this
     * operation since the beginning of this measurement.
     */
    public long getMinTime();

    /*
     * Returns the sum total of time taken to complete every invocation of this
     * operation since the beginning of this measurement. Dividing totalTime by
     * count will give you the average execution time for this operation.
     */
    public long getTotalTime();

}

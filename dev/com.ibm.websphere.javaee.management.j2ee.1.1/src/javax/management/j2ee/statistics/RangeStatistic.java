/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package javax.management.j2ee.statistics;

/**
 * Specifies standard measurements of the lowest and highest values an attribute has
 * held as well as its current value.
 */
public interface RangeStatistic extends Statistic {

    /*
     * Returns the highest value this attribute has held since the beginning of the
     * measurement.
     */
    public long getHighWaterMark();

    /*
     * Returns the lowest value this attribute has held since the beginning of the
     * measurement.
     */
    public long getLowWaterMark();

    /*
     * Returns the current value of this attribute.
     */
    public long getCurrent();

}

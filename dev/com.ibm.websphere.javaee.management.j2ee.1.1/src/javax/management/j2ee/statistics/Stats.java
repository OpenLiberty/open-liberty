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
 * The Stats interface and its subinterfaces specify performance data accessors for each
 * of the specific managed object types. The data required by the interfaces is
 * commonly available on all platform implementations. Managed objects that support
 * statistics are permitted to provide support for a subset of the accessors in the Stats
 * interfaces. Managed objects indicate which of the specified accessors return valid
 * data by including only the names of the supported statistics in the statisticNames
 * list. The data provided by a supported statistic must be exactly as specified by the
 * corresponding Stats interface. The Stats interfaces may be extended to provide
 * vendor specific performance statistics. Vendor specific perfomance statistics must
 * implement or extend one of the standard Statistics interfaces.
 */
public interface Stats {

    /*
     * Gets a Statistic by name.
     */
    public Statistic getStatistic(String name);

    /*
     * Returns a list of the names of the attributes from the specific Stats interface
     * that this object supports. Attributes named in the list must correspond to the
     * names of operations in the Stats interface that will return Statistics object of the
     * appropriate type which contains valid data. Each operation in a Stats interface is
     * an accessor which follows the pattern getAttributeName. The AttributeName
     * portion of the operation name is the value that is returned as the name in the
     * StatisticNames list.
     * The value of attributes whose names are not included in the StatisticNames
     * list is undefined and must be considered invalid. For each attribute name in the
     * StatisticNames list that returns a Statistic there must be one Statistic object with
     * the same name in the statistics list.
     */
    public String[] getStatisticNames();

    /*
     * Returns a list of all the Statistic objects supported by this Stats object..
     */
    public Statistic[] getStatistics();
}

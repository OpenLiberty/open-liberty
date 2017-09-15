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
package com.ibm.ws.pmi.stat;

import java.util.ArrayList;

import com.ibm.wsspi.pmi.stat.SPIStatistic;
import com.ibm.wsspi.pmi.stat.SPIStats;

/**
 * Static factory for <code>StatsImpl</code> that creates new instances of all
 * <code>*StatsImpl</code> classes based on the specified parameters.
 */
public class StatsCreator {
    /**
     * Creates the specified <code>StatsImpl</code> instance from the specified
     * parameters.
     * 
     * @param statstype The <code>StatsImpl</code> sub-class to create
     * @param name Name of the Stats
     * @param type Type of the Stats
     * @param time Time created
     * @return the created <code>StatsImpl</code>
     */
    public static StatsImpl create(String statsType, String name, int type, long time) {
        return create(statsType, name, type, 0, time, null, null);
    }

    /**
     * Creates the specified <code>StatsImpl</code> instance from the specified
     * parameters.
     * 
     * @param statstype The <code>StatsImpl</code> sub-class to create
     * @param name Name of the Stats
     * @param type Type of the Stats
     * @param level Stats level
     * @param time Time created
     * @return the created <code>StatsImpl</code>
     */
    public static StatsImpl create(String statsType, String name, int type,
                                   int level, long time) {
        return create(statsType, name, type, level, time, null, null);
    }

    /**
     * /**
     * Creates the specified <code>StatsImpl</code> instance from the specified
     * parameters.
     * 
     * @param statstype The <code>StatsImpl</code> sub-class to create
     * @param name Name of the Stats
     * @param type Type of the Stats
     * @param level Stats level
     * @param time Time Stats was created
     * @param dataMembers ArrayList of SPIStatistics
     * @param subCollections ArrayList of SPIStats
     * @return the created <code>StatsImpl</code>
     */
    public static StatsImpl create(String statsType, String name, int type, int level,
                                   long time, ArrayList dataMembers, ArrayList subCollections) {
        StatsImpl toReturn = null;

        // always return the base stats since there is no more specific stats created in pmi.
        // specific stats are created only when using J2EE interfaces in pmi.j2ee component.
        toReturn = new StatsImpl(statsType, name, type, level, dataMembers, subCollections);
        toReturn.time = time;

        return toReturn;

        /*
         * // Not sure why we'd get a null statsType...
         * if (statsType == null) {
         * System.out.println("got null statsType for stats: " + name);
         * toReturn = new StatsImpl( statsType, name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * 
         * if ( statsType.equals( PmiConstants.BEAN_MODULE ) ) {
         * toReturn = new EJBStatsImpl( name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * else if ( statsType.equals( PmiConstants.J2C_MODULE ) ) {
         * toReturn = new JCAStatsImpl( name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * else if ( statsType.equals( PmiConstants.CONNPOOL_MODULE ) ) {
         * toReturn = new JDBCConnectionStatsImpl( name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * else if ( statsType.equals( PmiConstants.TRAN_MODULE ) ) {
         * toReturn = new JTAStatsImpl( name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * else if ( statsType.equals( PmiConstants.RUNTIME_MODULE ) ) {
         * toReturn = new JVMStatsImpl( name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         * 
         * // default
         * else {
         * toReturn = new StatsImpl(statsType, name, type, level, dataMembers, subCollections );
         * toReturn.time = time;
         * 
         * return toReturn;
         * }
         */
    }

    /**
     * This method adds one Stats object as a subStats of another Stats object
     * 
     * @param parentStats This Stats is the parent
     * @param subStats This Stats is the child
     */
    public static void addSubStatsToParent(SPIStats parentStats, SPIStats subStats) {
        StatsImpl p = (StatsImpl) parentStats;
        StatsImpl s = (StatsImpl) subStats;
        p.add(s);
    }

    /**
     * This method adds one Statistic object as the child of a Stats object
     * 
     * @param parentStats This stats is the parent
     * @param statistic This statistic should be added
     */
    public static void addStatisticsToParent(SPIStats parentStats, SPIStatistic statistic) {
        StatsImpl p = (StatsImpl) parentStats;
        StatisticImpl s = (StatisticImpl) statistic;
        p.add(s);
    }
}

/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.pmi.stat;

import java.util.ArrayList;

import com.ibm.ws.pmi.stat.StatsCreator;
import com.ibm.ws.pmi.stat.StatsImpl;

/**
 * This is a public api wrapper class for com.ibm.ws.pmi.stat
 */
public class SPIStatsCreator {
    public static StatsImpl create(String statsType,
                                    String name,
                                    int type,
                                    long time) {
        return StatsCreator.create(statsType, name, type, time);
    }

    public static StatsImpl create(String statsType,
                                    String name,
                                    int type,
                                    int level,
                                    long time) {
        return StatsCreator.create(statsType, name, type, level, time);
    }

    public static StatsImpl create(String statsType,
                                    String name,
                                    int type,
                                    int level,
                                    long time,
                                    ArrayList dataMembers,
                                    ArrayList subCollections) {
        return StatsCreator.create(statsType, name, type, level, time, dataMembers, subCollections);
    }

    public static void addSubStatsToParent(SPIStats parentStats, SPIStats subStats) {
        StatsCreator.addSubStatsToParent(parentStats, subStats);
    }

    public static void addStatisticsToParent(SPIStats parentStats, SPIStatistic statistic) {
        StatsCreator.addStatisticsToParent(parentStats, statistic);
    }
}

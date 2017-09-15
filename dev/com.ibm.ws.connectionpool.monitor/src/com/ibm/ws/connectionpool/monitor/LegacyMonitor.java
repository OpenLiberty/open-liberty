/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.connectionpool.monitor;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.pmi.stat.BoundedRangeStatisticImpl;
import com.ibm.ws.pmi.stat.CountStatisticImpl;
import com.ibm.ws.pmi.stat.TimeStatisticImpl;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class LegacyMonitor extends StatisticActions {

    public final static int NUM_CREATES = 1;
    public final static int NUM_DESTROYS = 2;
    public static final int NUM_MANAGED_CONNECTIONS = 14;
    public static final int NUM_CONNECTIONS = 15;
    public static final int AVERAGE_WAIT = 13;
    public final static int FREE_POOL_SIZE = 6;
    private StatsInstance si;

    /** Number of ManagedConnections in use */
    private CountStatisticImpl numManagedConnections = null;
    /** Number of Connections in use */
    private CountStatisticImpl numConnectionHandles = null;
    /** Number of Creates */
    private CountStatisticImpl numManagedConnectionsCreated = null;
    /** Number of Destroys */
    private CountStatisticImpl numManagedConnectionsDestroyed = null;
    /** Average wait time */
    private TimeStatisticImpl averageWait = null;

    /** Average free pool size */
    private BoundedRangeStatisticImpl freePoolSize = null;

    public LegacyMonitor(String dsName, StatsGroup grp) {
        // TODO Auto-generated constructor stub
        if (grp != null) {
            try {

                si = StatsFactory.createStatsInstance(dsName, grp, null, this);

            } catch (StatsFactoryException e) {
                // TODO Auto-generated catch block
                FFDCFilter.processException(e, getClass().getName(), "ConnectionPool LegacyMonitor");
            }
        }
    }

    @Override
    public void statisticCreated(SPIStatistic s) {
        if (s.getId() == NUM_CREATES) {
            numManagedConnectionsCreated = (CountStatisticImpl) s;
        } else if (s.getId() == NUM_DESTROYS) {
            numManagedConnectionsDestroyed = (CountStatisticImpl) s;
        } else if (s.getId() == NUM_MANAGED_CONNECTIONS) {
            numManagedConnections = (CountStatisticImpl) s;
        } else if (s.getId() == NUM_CONNECTIONS) {
            numConnectionHandles = (CountStatisticImpl) s;
        } else if (s.getId() == AVERAGE_WAIT) {
            averageWait = (TimeStatisticImpl) s;
        } else if (s.getId() == FREE_POOL_SIZE) {
            freePoolSize = (BoundedRangeStatisticImpl) s;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateStatisticOnRequest(int dataId) {
        if (si != null) {
            ConnectionPoolStats temp = ConnectionPoolMonitor.getConnectionPoolOB((si.getName()));
            if (dataId == NUM_CREATES) {
                numManagedConnectionsCreated.setCount(temp.getCreateCount());
            }
            if (dataId == NUM_DESTROYS) {
                numManagedConnectionsDestroyed.setCount(temp.getDestroyCount());
            }
            if (dataId == NUM_MANAGED_CONNECTIONS) {
                numManagedConnections.setCount(temp.getManagedConnectionCount());
            }
            if (dataId == NUM_CONNECTIONS) {
                numConnectionHandles.setCount(temp.getConnectionHandleCount());
            }
            if (dataId == AVERAGE_WAIT) {
                averageWait.set(((long) temp.getWaitTime()), (long) temp.getWaitTime(), (long) temp.getWaitTime(), 0, 0, 0, 0);
            }
            if (dataId == FREE_POOL_SIZE) {
                freePoolSize.set(temp.getFreeConnectionCount());
            }
        }
    }

    public void removeSInstance() {
        try {
            StatsFactory.removeStatsInstance(this.si);
        } catch (StatsFactoryException e) {
            // TODO Auto-generated catch block
            e.getMessage();
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.monitors;

import java.lang.reflect.Method;
import java.util.Map;

import com.ibm.websphere.monitor.annotation.Args;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.monitors.helper.ThreadPoolStats;
import com.ibm.ws.monitors.helper.ThreadPoolStatsHelper;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * Monitor Class for WebContainer.
 */
@Monitor(group = "ThreadPool")
public class ThreadPoolMonitor extends StatisticActions {

    @PublishedMetric
    public MeterCollection<ThreadPoolStats> threadPoolCountByName = new MeterCollection<ThreadPoolStats>("ThreadPool", this);

    private Object ob_ref;

    private static final TraceComponent tc = Tr.register(ThreadPoolMonitor.class);

    private static final String DEFAULT_POOL_NAME = "Default Executor";

    //For Legacy PMI
    //Define stats ID here.
    public static final int ACTIVE_THREADS = 3;
    public static final int POOL_SIZE = 4;
    //-----------------------

    private static final String template = "/com/ibm/ws/monitors/threadPoolModule.xml";
    private SPIBoundedRangeStatistic activeThreads;
    private SPIBoundedRangeStatistic poolSize;
    private static ThreadPoolStatsHelper _tpHelper = null;

    public ThreadPoolMonitor() {
        try {
            StatsGroup grp = StatsFactory.createStatsGroup("ThreadPool", template, null, this);
            StatsFactory.createStatsInstance(DEFAULT_POOL_NAME, grp, null, this);
        } catch (StatsFactoryException e) {
            //If PMI Is disabled, we get this.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Thread Pool Module is not registered with PMI");
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), ".ThreadPoolMonitor");
        }
    }

    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.threading.internal.ExecutorServiceImpl", method = "execute")
    public void atFieldGet(@This Object esi) {
        //We will be checking this only once.
        //Once we get ThreadPoolExecutorImpl for DEFAULT_POOL_NAME, we set ob_ref and won't execute any code here.
        if (ob_ref == null) {
            //This is first time we are here.
            //This only support single ThreadPool, Default Executor
            String poolName = DEFAULT_POOL_NAME; //TODO
            try {
                //Check if this object is for Default Executor ThreadPool?
                //If not, return. We only support Default Executor at this moment.
                Method tMethod = esi.getClass().getMethod("getPoolName", null);
                String tempName = null;
                if (tMethod != null) {
                    tempName = (String) tMethod.invoke(esi, null);
                }

                ob_ref = esi;
                if (!!!poolName.equalsIgnoreCase(tempName)) {
                    poolName = tempName;
                }
                ThreadPoolStats tpStats = threadPoolCountByName.get(poolName);
                if (tpStats == null) {
                    initThreadPoolStat(poolName);
                }

            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getSimpleName(), "Unable to query Thread Pool Exec.");
            }
        }
    }

    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.threading.internal.ExecutorServiceImpl", method = "createExecutor")
    public void atCreateExecutorEntry(@Args Object[] myargs) {
        Map<String, Object> componentConfig = (Map<String, Object>) myargs[0];
        String name = (String) componentConfig.get("name");
        if (name != null && !!!name.isEmpty()) {
            threadPoolCountByName.remove(name);
            this.ob_ref = null;
        }

    }

    /**
     * @param poolName
     */
    private synchronized void initThreadPoolStat(String _poolName) {
        if (threadPoolCountByName.get(_poolName) != null) {
            return;
        }
        if (_tpHelper == null) {
            _tpHelper = new ThreadPoolStatsHelper(_poolName, ob_ref);
        }
        ThreadPoolStats tpStats = new ThreadPoolStats(_poolName, ob_ref);
        threadPoolCountByName.put(_poolName, tpStats);
    }

    /** {@inheritDoc} */
    @Override
    public void statisticCreated(SPIStatistic s) {
        if (s.getId() == ACTIVE_THREADS) {
            activeThreads = (SPIBoundedRangeStatistic) s;
        } else if (s.getId() == POOL_SIZE) {
            poolSize = (SPIBoundedRangeStatistic) s;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invlid stats found " + s);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateStatisticOnRequest(int dataId) {
        if (_tpHelper == null) {
            return;
        }

        if (dataId == ACTIVE_THREADS) {
            activeThreads.set(_tpHelper.getActiveThreads());
        }
        if (dataId == POOL_SIZE) {
            poolSize.set(_tpHelper.getPoolSize());
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitors;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.monitors.helper.JvmMonitorHelper;
import com.ibm.ws.monitors.helper.JvmStats;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIDoubleStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

@Monitor(group = "JVM")
public class JVM extends StatisticActions {

    //Define stats ID here.
    public static final int HeapSize = 1;
    public static final int FreeMemory = 2;
    public static final int UsedMemory = 3;
    public static final int UpTime = 4;
    public static final int ProcessCPU = 5;
    public static final int GCCount = 6;
    public static final int GCTime = 7;
    //-----------------------

    private static final String template = "/com/ibm/ws/monitors/JVMMonitorStats.xml";

    private SPICountStatistic usedMemory;
    private SPICountStatistic freeMemory;
    private SPIBoundedRangeStatistic heap;
    private SPICountStatistic upTime;
    private SPIDoubleStatistic processCPU;
    private SPICountStatistic gcCount;
    private SPICountStatistic gcTime;

    private final TraceComponent tc = Tr.register(JVM.class);

    //For New Monitoring:
    @SuppressWarnings("unused")
    @PublishedMetric
    private final JvmStats PerformanceData;
    private static JvmMonitorHelper jHelper = new JvmMonitorHelper();

    public JVM() {
        //For Legacy PMI
        try {
            StatsFactory.createStatsInstance("JVM", template, null, this);
        } catch (StatsFactoryException e) {
            //If PMI Is disabled, we get this.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JVM Module is not registered with PMI");
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), ".JVM");
        }
        //For New Monitoring :
        PerformanceData = new JvmStats(jHelper);
    }

    @Override
    public void statisticCreated(SPIStatistic s) {
        if (s.getId() == JVM.HeapSize) {
            heap = (SPIBoundedRangeStatistic) s;
            heap.setLowerBound(jHelper.getInitHeapMemorySettings());
            heap.setUpperBound(jHelper.getMaxHeapMemorySettings());
        } else if (s.getId() == JVM.UsedMemory) {
            usedMemory = (SPICountStatistic) s;
        } else if (s.getId() == JVM.FreeMemory) {
            freeMemory = (SPICountStatistic) s;
        } else if (s.getId() == JVM.UpTime) {
            upTime = (SPICountStatistic) s;
        } else if (s.getId() == JVM.ProcessCPU) {
            processCPU = (SPIDoubleStatistic) s;
        } else if (s.getId() == JVM.GCCount) {
            gcCount = (SPICountStatistic) s;
        } else if (s.getId() == JVM.GCTime) {
            gcTime = (SPICountStatistic) s;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invlid stats found " + s);
            }
        }
    }

    @Override
    public void updateStatisticOnRequest(int dataId) {
        //MEMORY
        if (dataId == JVM.HeapSize) {
            heap.set(jHelper.getCommitedHeapMemoryUsage());
        }
        if (dataId == JVM.UsedMemory) {
            usedMemory.setCount(jHelper.getUsedHeapMemoryUsage());
        }
        if (dataId == JVM.FreeMemory) {
            if (heap != null && usedMemory != null)
                freeMemory.setCount(jHelper.getCommitedHeapMemoryUsage() - jHelper.getUsedHeapMemoryUsage());
        }

        //GC DATA
        if (dataId == JVM.GCCount) {
            gcCount.setCount(jHelper.getGCCollectionCount());
        }
        if (dataId == JVM.GCTime) {
            gcTime.setCount(jHelper.getGCCollectionTime());
        }

        //UPTIME
        if (dataId == JVM.UpTime) {
            upTime.setCount(jHelper.getUptime());
        }

        //Process CPU
        if (dataId == JVM.ProcessCPU) {
            processCPU.setDouble(jHelper.getCPU());
        }
    }
}

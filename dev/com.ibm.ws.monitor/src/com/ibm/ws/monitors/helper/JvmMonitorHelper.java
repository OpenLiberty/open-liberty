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
package com.ibm.ws.monitors.helper;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 *
 */
public class JvmMonitorHelper {

    private final MemoryMXBean mmx;
    private final List<GarbageCollectorMXBean> gmx;
    private final GarbageCollectorMXBean firstGCMBean;
    private final RuntimeMXBean rmx;

    /**
     *
     */
    public JvmMonitorHelper() {
        mmx = ManagementFactory.getMemoryMXBean();
        gmx = ManagementFactory.getGarbageCollectorMXBeans();
        firstGCMBean = gmx.get(0);
        rmx = ManagementFactory.getRuntimeMXBean();
    }

    /**
     * Method : getCommitedHeapMemoryUsage().
     *
     * @return the memory, which is committed to use for this JVM.
     *         Always query and give latest value.
     */
    public long getCommitedHeapMemoryUsage() {
        MemoryUsage mu = mmx.getHeapMemoryUsage();
        return mu.getCommitted();
    }

    /**
     * Method : getInitHeapMemorySettings().
     *
     * @return the memory, which initially asked by this JVM.
     */
    public long getInitHeapMemorySettings() {
        MemoryUsage mu = mmx.getHeapMemoryUsage();
        return mu.getInit();
    }

    /**
     * Method : getMaxHeapMemorySettings().
     *
     * @return max memory, that can be used by this JVM.
     */
    public long getMaxHeapMemorySettings() {
        MemoryUsage mu = mmx.getHeapMemoryUsage();
        return mu.getMax();
    }

    /**
     * Method : getUsedHeapMemoryUsage.
     *
     * @return amount of memory used in bytes.
     */
    public long getUsedHeapMemoryUsage() {
        MemoryUsage mu = mmx.getHeapMemoryUsage();
        return mu.getUsed();
    }

    /**
     * Method getGCCollectionCount
     *
     * @return The total number of collections that have occurred.
     */
    public long getGCCollectionCount() {
        return firstGCMBean.getCollectionCount();
    }

    /**
     * Method : getGCCollectionTime
     *
     * @return The approximate accumulated collection elapsed time in milliseconds.
     */
    public long getGCCollectionTime() {
        return firstGCMBean.getCollectionTime();
    }

    /**
     * Method : getUptime()
     *
     * @return Returns the uptime of the Java virtual machine in milliseconds.
     */
    public long getUptime() {
        return rmx.getUptime();

    }

    /**
     *
     * Method : getCPU
     *
     * @return Percentage CPU usage for JVM Process
     */
    public double getCPU() {
        return CpuInfo.getJavaCpuUsage();
    }
}

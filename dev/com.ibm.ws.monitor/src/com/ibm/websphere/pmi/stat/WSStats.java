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

package com.ibm.websphere.pmi.stat;

import com.ibm.websphere.pmi.PmiModuleConfig;

/**
 * WebSphere Performance Monitoring Infrastructure (PMI) Stats interface. This interface is similar to <code>javax.management.j2ee.statistics.Stats</code> interface.
 * There are two ways to access stats:
 * <ol>
 * <li>via MBean stats attribute
 * <li>via WebSphere Perf MBean
 * </ol>
 * When using the MBean stats attribute an object of type <code>javax.management.j2ee.statistics.Stats</code> will be returned.
 * If the MBean has a specific stats interface defined in the J2EE specification then an object of that specific type will be returned.
 * For example, if the target MBean type is JVM then the return type will be <code>javax.management.j2ee.statistics.JVMStats</code>.
 * <p>
 * When using the Perf MBean an object of type <code>com.ibm.websphere.pmi.stat.WSStats</code> will be returned.
 * The Perf MBean doesn't provide support for specific stats interface like JVMStats. The following example
 * explains how to get the individual statistic from WSStat interface.
 * 
 * <pre>
 * ObjectName perfMBean; // get Perf MBean (MBean type=Perf)
 * String query = "WebSphere:type=perf,node=" + nodeName + ",process=" + serverName + ",*";
 * ObjectName queryName = new ObjectName(query);
 * Set s = adminClient.queryNames(queryName, null);
 * if (!s.isEmpty())
 * perfMBean = (ObjectName)s.iterator().next();
 * 
 * ObjectName jvmMBean; // get JVM MBean in the same way as above with type JVM(MBean type=JVM)
 * 
 * // invoke getStatsObject on perfMBean
 * signature = new String[] {"javax.management.ObjectName","java.lang.Boolean"};
 * params = new Object[] {jvmMBean, new Boolean(false)};
 * WSStats jvmStats = (WSStats) ac.invoke(perfMBean, "getStatsObject", params, signature);
 * 
 * // get JVM Heap size.
 * // {@link com.ibm.websphere.pmi.stat.WSJVMStats} interface defines all the statistics that are available from JVM
 * WSRangeStatistic jvmHeapStatistic = (WSRangeStatistic) jvmStats.getStatistic (WSJVMStats.HeapSize);
 * long heapSize = jvmHeapStatistic.getCurrent();
 * 
 * // print all statistics
 * System.out.println (jvmStats.toString());
 * </pre>
 * 
 * WebSphere Performance Monitoring Infrastructure (PMI) maintains the Stats from various components in a tree structure. Refer to the Perf MBean documentation for details about
 * Perf MBean API.
 * 
 * @ibm-api
 */

public interface WSStats {
    /**
     * Returns the Stats name (eg., JVM Runtime, Thread Pools)
     */
    public String getName();

    /**
     * Returns the Stats type (eg., jvmRuntimeModule, threadPoolModule). This type is used to bind the text information like name, description and unit to the Stats.
     */
    public String getStatsType();

    /**
     * Returns the time when the client request came to the server
     */
    public long getTime();

    /**
     * Set textual information. If the text information like name, description, and unit are null then this
     * method can be used to bind the text information to the Stats. The text information
     * will be set by default.
     * 
     * @see com.ibm.websphere.pmi.stat.WSStatsHelper
     * 
     */
    public void setConfig(PmiModuleConfig config);

    /**
     * Get Statistic by ID. The IDs are defined in <code>com.ibm.websphere.pmi.stat.WS*Stats</code> interface.
     */
    public WSStatistic getStatistic(int id);

    /**
     * Get Statistic by Name.
     */
    public WSStatistic getStatistic(String statisticName);

    /**
     * Get Statistic names
     */
    public String[] getStatisticNames();

    /**
     * Get all statistics
     */
    public WSStatistic[] getStatistics();

    /**
     * Get the sub-module stats by the name
     */
    public WSStats getStats(String name);

    /**
     * Get all the sub-module stats
     */
    public WSStats[] getSubStats();

    /**
     * 
     * Returns the number of statistics available in this Stats object (this number doesn't include the sub-module stats).
     */

    public int numStatistics();

    /**
     * Returns all the statistics
     * 
     * @deprecated As of 6.0, replaced by {@link #getStatistics()}
     */
    @Deprecated
    public WSStatistic[] listStatistics();

    /**
     * Returns all the sub-module statistics
     * 
     * @deprecated As of 6.0, replaced by {@link #getSubStats()}
     */
    @Deprecated
    public WSStats[] listSubStats();

    /**
     * Returns all the statistic names
     * 
     * @deprecated As of 6.0, replaced by {@link #getStatisticNames()}
     */
    @Deprecated
    public String[] listStatisticNames();

    /**
     * Update the Stats object
     * 
     * @param newStats the new value of the Stats
     * @param keepOld indicates if the the statistics/subStats that are not in newStats should be removed
     * @param recursiveUpdate recursively update the sub-module stats when it is true
     */
    public void update(WSStats newStats, boolean keepOld, boolean recursiveUpdate);

    /**
     * Reset the statistic (the statistic is reset only in the client side and not in the server side).
     */
    public void resetOnClient(boolean recursive);

    /**
     * 
     * Returns the Stats in XML format
     */
    public String toXML();

    /**
     * 
     * Returns the Stats in String format
     */
    public String toString();

    /**
     * Returns the PMI collection type
     * 
     * @deprecated No replacement
     */
    @Deprecated
    public int getType();

    /**
     * Returns the PMI instrumentation level
     * 
     * @deprecated No replacement
     */
    @Deprecated
    public int getLevel();

    public void mSetConfig(PmiModuleConfig config);

}
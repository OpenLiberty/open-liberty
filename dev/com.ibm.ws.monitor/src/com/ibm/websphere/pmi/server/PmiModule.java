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

package com.ibm.websphere.pmi.server;

import java.util.ArrayList;
import javax.management.ObjectName;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.pmi.stat.StatDescriptor;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.ws.pmi.server.ModuleItem;

import com.ibm.ws.pmi.stat.*;

public interface PmiModule extends java.io.Serializable {
    public static final long serialVersionUID = -906336887444153079L;

    /**
     * Return the moduleID
     */
    public String getModuleID();

    public ObjectName getMBeanName();

    public MBeanStatDescriptor getMBeanStatDescriptor();

    public StatDescriptor get60_StatDescriptor();

    public StatDescriptor getMSD_StatDescriptor();

    /**
     * Return an array of MBeanStatDescriptor (may be null) which can be used to
     * query a subset of PMI data in the MBean/Module. It provides a finer granularity
     * than MBean
     */
    public MBeanStatDescriptor[] listStatMembers();

    public void setMBeanName(ObjectName mbeanName);

    public void setMBeanName(ObjectName mbeanName, StatDescriptor msd);

    public void init(Object[] params);

    /**
     * Return the name of this module instance
     */
    public String getName();

    /**
     * Return an array of String for the path in module tree
     */
    public String[] getPath();

    /**
     * Return MODULE_INSTANCE or MODULE_AGGREGATE
     */
    public int getType();

    /**
     * Return the default level
     */
    public int getDefaultLevel();

    /**
     * Return the current instrumentation level
     */
    public int getInstrumentationLevel();

    /**
     * Create(enable)/remove(disable) data based on the new level
     */
    public void setInstrumentationLevel(int newLevel);

    /**
     * enable an individual data
     */
    public void enableData(int dataId);

    /**
     * disable an individual data
     */
    public void disableData(int dataId);

    /**
     * Return a list of SpdData based on the data ids.
     */
    public SpdData[] listData();

    public SpdData[] listData(int[] ids);

    public SpdData listData(int id);

    /**
     * Return all the enabled SpdData.getStatistic
     */
    public ArrayList listStatistics();

    public StatsImpl getStats(ArrayList dataMembers, ArrayList subStats);

    // generic methods to update data
    public void updateData(int dataId, int opType, double value);

    public void updateData(String dataName, int opType, double value);

    public void updateData(SpdData mydata, int opType, double value);

    /**
     * unregister PMI module and set all the counters to be null.
     * It should be called by all the components that stop running
     */
    public void unregister();

    public boolean isCustomModule();

    public PmiModuleConfig getModuleConfig();

    public StatsImpl getJSR77Stats(ModuleItem item);

    public void cleanup();

    public boolean setFineGrainedInstrumentation(int[] enabled, int[] enableWithSync);

    public int[] getEnabled();

    public int[] getEnabledSync();

    public String getWCCMStatsType();

    public boolean isJ2EEStatisticProvider();

    public void setInstrumentationBySet(String set);

    public void setSyncEnabled(boolean sync);

    public void updateStatistics();
}
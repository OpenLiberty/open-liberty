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

package com.ibm.wsspi.pmi.factory;

import javax.management.ObjectName;

import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * StatsInstance represents a single instance of the Stats template.
 * The instance will have all the statistics defined in the template.
 * The Stats template XML file is defined using the stats.dtd at com/ibm/websphere/pmi/xml
 * 
 * @ibm-spi
 */

public interface StatsInstance {
    /**
     * Returns the name of the instance
     * 
     * @return instance name
     */
    public String getName();

    /**
     * Return the MBean name associated with this StatsInstance.
     * Return null if no MBean is associated.
     * 
     * @return MBean ObjectName
     */
    public ObjectName getMBean();

    /**
     * Associate a managed object MBean with this StatsInstance.
     * This is required to access the statistics by calling getStats() on the managed object MBean.
     * 
     * @param mBeanName managed object ObjectName
     */
    public void setMBean(ObjectName mBeanName);

    /**
     * Return the current instrumentation/monitoring level for this StatsInstance.
     * The instrumentation level is set via Administrative Console, WSAdmin, PerfMBean and PMI API.
     * The default instrumentaion level is LEVEL_NONE when the instance is created.
     * The various levels are defined in com.ibm.websphere.pmi.PmiConstants
     * 
     * @return instrumentation level
     */
    public int getInstrumentationLevel();

    /**
     * Returns a statistic by ID. The ID is defined in the Stats template.
     * 
     * @param id Statistic ID
     * @return Statistic
     */
    public SPIStatistic getStatistic(int id);

    /**
     * Returns the MBeanStatDescriptor for this StatsInstance.
     * If an MBean is associated with the StatsInstance then the ObjectName will be returned as part of the MBeanStatDescriptor.
     * 
     * @deprecated No replacement.
     * @return MBeanStatDescriptor of the StatsInstance
     */
    public MBeanStatDescriptor getMBeanStatDescriptor();

    /*
     * ~~~~~~~~~~~~~~ commented ~~~~~~~~~~~~~~
     * /--
     * Add a StatisticCreatedListener to the StatsInstance
     * 
     * @param scl StatisticCreatedListener
     * -/
     * public void addStatisticCreatedListener (StatisticCreatedListener scl);
     * 
     * /--
     * Remove a StatisticCreatedListener from the StatsInstance
     * 
     * @param scl StatisticCreatedListener
     * -/
     * public void removeStatisticCreatedListener (StatisticCreatedListener scl);
     * ~~~~~~~~~~~~~~ commented ~~~~~~~~~~~~~~
     */
}

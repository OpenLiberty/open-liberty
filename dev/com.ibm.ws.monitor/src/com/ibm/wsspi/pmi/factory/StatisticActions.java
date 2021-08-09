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
package com.ibm.wsspi.pmi.factory;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * This class is used to propagate action events from PMI service to the runtime
 * component.
 * 
 * @ibm-spi
 */
public class StatisticActions {
    private StatisticActionListener legacyListener = null;

    /**
     * This is the default constructor.
     */
    public StatisticActions() {}

    /**
     * This is the default constructor.
     * 
     * @param legacy This is the StatisticActionListener object that will be wrapped by this class.
     */
    public StatisticActions(StatisticActionListener legacy) {
        legacyListener = legacy;
    }

    /**
     * This method is called to indicate that a statistic is created in the Stats instance.
     * The runtime component should use this message to cache the reference to the statistic.
     * This eliminates querying the individual statistic from the StatsInstance object.
     * 
     * @param s statistic created in the StatsInstance
     */
    public void statisticCreated(SPIStatistic s) {
        if (legacyListener != null)
            legacyListener.statisticCreated(s);
    }

    /**
     * This method is called to indicate that a client or monitoring application is
     * requesting the statistic. This message is applicable only to the "updateOnRequest" statistic.
     * 
     * @param dataId data ID of the statistic
     */
    public void updateStatisticOnRequest(int dataId) {
        //System.out.println("Hi This is for UpdataStatistic");
        if (legacyListener != null)
            legacyListener.updateStatisticOnRequest(dataId);
    }

    /**
     * This method is called whenever the PMI framework has either enabled or disabled
     * statistics. The arrays provided as parameters identify which statistics are enabled
     * and which are disabled.
     * 
     * @param enabled Array of enabled statistic data IDs
     * @param disabled Array of disabled statistic data IDs
     */
    public void enableStatusChanged(int[] enabled, int[] disabled) {}

    public Bundle getCurrentBundle() {
        return FrameworkUtil.getBundle(getClass());
    }

}

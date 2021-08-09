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

import com.ibm.wsspi.pmi.stat.*;

/**
 * This interface is used to propagate action events from PMI service to the runtime component.
 * 
 * @deprecated As of 6.1, replaced with {@link com.ibm.wsspi.pmi.factory.StatisticActions}
 * 
 * @ibm-api
 **/

public interface StatisticActionListener {
    /**
     * This method is called to indicate that a statistic is created in the Stats instance.
     * The runtime component should use this message to cache the reference to the statistic.
     * This eliminates querying the individual statistic from the StatsInstance object.
     * 
     * @param s statistic created in the StatsInstance
     */
    public void statisticCreated(SPIStatistic s);

    /**
     * This method is called to indicate that a client or monitoring application is
     * requesting the statistic. This message is applicable only to the "updateOnRequest" statistic.
     * 
     * @param dataId data ID of the statistic
     */
    public void updateStatisticOnRequest(int dataId);
}

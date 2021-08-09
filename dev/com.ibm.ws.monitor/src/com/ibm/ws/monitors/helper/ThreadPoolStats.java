/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.monitors.helper;

import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.ThreadPoolMXBean;

/**
 * 
 */
public class ThreadPoolStats extends Meter implements ThreadPoolMXBean {

    private final ThreadPoolStatsHelper _tpHelper;

    public ThreadPoolStats(String poolName, Object tpExecImpl) {
        _tpHelper = new ThreadPoolStatsHelper(poolName, tpExecImpl);
    }

    /**
     * @return the poolName
     */
    public String getPoolName() {
        return _tpHelper.getPoolName();
    }

    /**
     * @return the poolSize
     */
    public int getPoolSize() {
        return _tpHelper.getPoolSize();
    }

    /**
     * @return the activeThreads
     */
    public int getActiveThreads() {
        return _tpHelper.getActiveThreads();
    }
}

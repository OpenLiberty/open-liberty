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

import com.ibm.websphere.monitor.meters.JvmMXBean;
import com.ibm.websphere.monitor.meters.Meter;

/**
 *
 */

public class JvmStats extends Meter implements JvmMXBean {

    private static JvmMonitorHelper _jHelper;

    /**
     * @param jHelper
     * 
     */
    public JvmStats(JvmMonitorHelper jHelper) {
        if (_jHelper == null) {
            //FFDC Here
        }
        _jHelper = jHelper;
    }

    /** {@inheritDoc} */
    @Override
    public long getFreeMemory() {
        return (_jHelper.getCommitedHeapMemoryUsage() - _jHelper.getUsedHeapMemoryUsage());
    }

    /** {@inheritDoc} */
    @Override
    public long getGcCount() {
        // TODO Auto-generated method stub
        return _jHelper.getGCCollectionCount();
    }

    /** {@inheritDoc} */
    @Override
    public long getGcTime() {
        // TODO Auto-generated method stub
        return _jHelper.getGCCollectionTime();
    }

    /** {@inheritDoc} */
    @Override
    public long getHeap() {
        return _jHelper.getCommitedHeapMemoryUsage();
    }

    /** {@inheritDoc} */
    @Override
    public double getProcessCPU() {
        return _jHelper.getCPU();
    }

    /** {@inheritDoc} */
    @Override
    public long getUpTime() {
        // TODO Auto-generated method stub
        return _jHelper.getUptime();
    }

    /** {@inheritDoc} */
    @Override
    public long getUsedMemory() {
        // TODO Auto-generated method stub
        return _jHelper.getUsedHeapMemoryUsage();
    }

}

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
package com.ibm.ws.session.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.session.monitor.SessionStatsMXBean;

/**
 *
 */
public class SessionStats extends Meter implements SessionStatsMXBean {
    private final Gauge activeCount;
    private final Gauge liveCount;
    private final Counter createCount;
    private final Counter invalidatedCountbyTimeout;
    private final Counter invalidatedCount;

    /**
     * @return the createCount
     */
    public SessionStats() {
        liveCount = new Gauge();
        activeCount = new Gauge();
        createCount = new Counter();
        invalidatedCountbyTimeout = new Counter();
        invalidatedCount = new Counter();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.monitor.sessionStatsMXBean#getActiveCount()
     */

    @Override
    public long getActiveCount() {
        // TODO Auto-generated method stub        
        return this.activeCount.getCurrentValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.monitor.sessionStatsMXBean#getLiveCount()
     */

    @Override
    public long getLiveCount() {
        // TODO Auto-generated method stub
        return this.liveCount.getCurrentValue();
    }

    public void activeCountInc() {
        this.activeCount.incrementCurrentValue(1);
    }

    public void liveCountInc() {
        this.liveCount.incrementCurrentValue(1);
    }

    public void activeCountDec() {
        this.activeCount.decrementCurrentValue(1);
    }

    public void liveCountDec() {
        this.liveCount.decrementCurrentValue(1);
    }

    /**
     * @return the createCount
     */
    @Override
    public long getCreateCount() {
        return this.createCount.getCurrentValue();
    }

    public void incCreateCount() {
        this.createCount.incrementBy(1);
    }

    /**
     * @return the destroyedCount
     */
    @Override
    public long getInvalidatedCountbyTimeout() {
        return invalidatedCountbyTimeout.getCurrentValue();
    }

    public void setInvalidatedCountbyTimeout() {
        this.invalidatedCountbyTimeout.incrementBy(1);
    }

    /**
     * @return the invalidatedCount
     */
    @Override
    public long getInvalidatedCount() {
        return invalidatedCount.getCurrentValue();
    }

    /**
     * 
     */
    public void setInvalidatedCount() {
        this.invalidatedCount.incrementBy(1);
    }
}

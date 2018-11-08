/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.connectionpool.monitor;

import com.ibm.websphere.connectionpool.monitor.ConnectionPoolStatsMXBean;
import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 * This class is the actual class where we declare counters using different Meter Objects like Counter,Gauge and handles the increments and decrements of
 * the declared counter.In future if we need add a new counter this is the place where we need to declare and add the getter ,setter methods.
 */
public class ConnectionPoolStats extends Meter implements ConnectionPoolStatsMXBean {

    private final Counter createCount, destroyCount;
    private final Gauge poolSize, freeConnectionCount;
    private final Gauge managedConnectionCount, connectionHandleCount;
    private final StatisticsMeter waitTime, inUseTime;

    public ConnectionPoolStats() {
        createCount = new Counter();
        destroyCount = new Counter();
        poolSize = new Gauge();
        managedConnectionCount = new Gauge();
        connectionHandleCount = new Gauge();
        waitTime = new StatisticsMeter();
        freeConnectionCount = new Gauge();
        inUseTime = new StatisticsMeter();
    }

    /**
     * @param createCount the createCount to set
     */
    public void incCreateCount() {
        this.createCount.incrementBy(1);
    }

    /**
     * @param poolSize the poolSize to set
     */
    public void incPoolSize() {
        this.poolSize.incrementCurrentValue(1);
    }

    /**
     * @param destroyCount the destroyCount to set
     */
    public void incDestroyCount() {
        this.destroyCount.incrementBy(1);
    }

    /**
     * @param managedConnectionCount the managedConnectionCount to set
     */
    public void incManagedConnectionCount() {
        this.managedConnectionCount.incrementCurrentValue(1);
    }

    public void decManagedConnectionCount() {
        this.managedConnectionCount.decrementCurrentValue(1);
    }

    public void incConnectionHandleCount() {
        this.connectionHandleCount.incrementCurrentValue(1);
    }

    public void decConnectionHandleCount() {
        this.connectionHandleCount.decrementCurrentValue(1);
    }

    public void updateWaitTime(long elapsed) {
        this.waitTime.addDataPoint(elapsed);
    }

    public void updateInUseTime(long elapsed) {
        this.inUseTime.addDataPoint(elapsed);
    }

    public void incFreeConnectionCount() {
        this.freeConnectionCount.incrementCurrentValue(1);
    }

    public void decFreeConnectionCount() {
        this.freeConnectionCount.decrementCurrentValue(1);
    }

    /** {@inheritDoc} */
    @Override
    public long getCreateCount() {
        // TODO Auto-generated method stub
        return this.createCount.getCurrentValue();
    }

    /** {@inheritDoc} */
    @Override
    public long getDestroyCount() {
        // TODO Auto-generated method stub
        return this.destroyCount.getCurrentValue();
    }

    /**
     * @return the managedConnectionCount
     */
    @Override
    public long getManagedConnectionCount() {
        return this.managedConnectionCount.getCurrentValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.connectionpool.monitor.ConnectionPoolStatsMXBean#getConnectionHandleCount()
     */
    @Override
    public long getConnectionHandleCount() {
        // TODO Auto-generated method stub
        return this.connectionHandleCount.getCurrentValue();
    }

    @Override
    public double getWaitTime() {
        // TODO Auto-generated method stub
        return this.waitTime.getMean();
    }

    @Override
    public double getInUseTime() {
        return this.inUseTime.getMean();
    }

    @Override
    public StatisticsMeter getWaitTimeDetails() {
        return this.waitTime;
    }

    @Override
    public StatisticsMeter getInUseTimeDetails() {
        return this.inUseTime;
    }

    @Override
    public long getFreeConnectionCount() {
        // TODO Auto-generated method stub
        return this.freeConnectionCount.getCurrentValue();
    }

    @Override
    public String toString() {
        return "ConnectionPoolStats [createCount=" + createCount + ", destroyCount=" + destroyCount + ", poolSize=" + poolSize + ", freeConnectionCount=" + freeConnectionCount
               + ", managedConnectionCount=" + managedConnectionCount + ", connectionHandleCount=" + connectionHandleCount + ", waitTime=" + waitTime + "]";
    }

}

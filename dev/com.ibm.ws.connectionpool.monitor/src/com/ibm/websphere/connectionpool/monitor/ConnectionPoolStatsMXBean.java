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
package com.ibm.websphere.connectionpool.monitor;

import com.ibm.websphere.monitor.jmx.StatisticsMeter;

/**
 * Management interface for the MBean "WebSphere:type=ConnectionPoolStats".
 * The Liberty profile makes this MBean available in its platform MBean server when the monitor-1.0 feature is
 * enabled to allow monitoring of the connection pool.
 *
 * @ibm-api
 */
public interface ConnectionPoolStatsMXBean {

    /**
     * The total number of managed connections created since pool creation.
     */
    public long getCreateCount();

    /**
     * The total number of managed connections destroyed since pool creation.
     */
    public long getDestroyCount();

    /**
     * The number of connections that are in use, including multiple connections shared from a single managed connection.
     */
    public long getConnectionHandleCount();

    /**
     * The total number of managed connections in the free, shared, and unshared pools.
     */
    public long getManagedConnectionCount();

    /**
     * The average waiting time in milliseconds until a connection is granted if a connection is not currently available.
     */
    public double getWaitTime();

    /**
     * Retrieves StatisticMeter object of WaitTime detail, which provides statistical details on the connection wait time.
     *
     * @return wait time details
     */
    public StatisticsMeter getWaitTimeDetails();

    /**
     * The number of managed connections in the free pool.
     */
    public long getFreeConnectionCount();

    /**
     * The average time in milliseconds a connection is in use.
     */
    public double getInUseTime();

    /**
     * Retrieves StatisticMeter object of InUseTime detail, which provides statistical details on the connection in use time.
     *
     * @return in use time details
     */
    public StatisticsMeter getInUseTimeDetails();
}

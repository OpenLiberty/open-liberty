/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.j2c.poolmanager;

/**
 * This interface encapsulates properties relevant to the
 * connection pooling. These properties are:
 * <ul>
 * <li> <code> ReapTime </code> - interval, in <b>seconds</b>, between runs of the garbage collector.
 * The garbage collector discards all connections that have been unused for
 * <code> UnusedTimeout </code>. To disable garbage collector, the <code> ReapTime </code>
 * should be set to <code> DEFAULT_REAP_TIME </code>. Alternate way to disable garbage
 * collector is to set <code> UnusedTimeout </code> to <code> DEFAULT_UNUSED_TIMEOUT </code>.
 *
 * <li> <code> UnusedTimeout </code> - interval, in miliseconds, after which the
 * unused connection is discarded. Setting this value to
 * <code> DEFAULT_UNUSED_TIMEOUT </code> disables garbage collector.
 *
 * <li> <code>ConnectionTimeout</code> - interval, in miliseconds, after which a requestor waiting for
 * connection times out and <code>ResourceAllocationException</code> is thrown. The wait may be
 * necessary if the maximum value of connections has been reached <code> (MaxConnections) </code>.
 * This value has no meaning if <code> MaxConnections </code> has not been set.
 * If <code> ConnectionTimeout </code> is set to <code> DEFAULT_CONNECTION_TIMEOUT </code>
 * the <code>ResourceAllocationException</code> is thrown immediately after PoolManager determines
 * that maximum connections are used.
 * If <code> ConnectionTimeout </code> is set to <code> DISABLE_CONNECTION_TIMEOUT </code>
 * the PoolManager waits until connection can be allocated (the number of connections falls below <code> MaxConnections </code>).
 *
 * <li> <code> MaxConnections </code> - the maximum number of ManagedConnections that can be
 * created by a particular <code>ManagedConnectionFactory</code>. Once this number is reached, no new connections are
 * created and the requester waits or the <code>ResourceAllocationException</code> is thrown.
 * If <code> MaxConnections </code> is set to <code>DEFAULT_MAX_CONNECTIONS</code> the number
 * of connections can grow indefinitely.
 *
 * <li> <code> MinConnections </code> - the minimum number of ManagedConnections that should be maintained. If this
 * number is reached, the garbage collector will not discard the ManagedConnection, however no attempt
 * is made to bring the number of connections to this number i.e. the actual number of connections
 * can be lower than <code> MinConnections </code>.
 *
 * </ul>
 *
 */

public interface ConnectionPoolProperties extends java.io.Serializable {

    static final long serialVersionUID = 6727028063700156173L;

    public static final int DEFAULT_CONNECTION_TIMEOUT = 180;
    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    public static final int DEFAULT_MIN_CONNECTIONS = 1;
    public static final int DEFAULT_UNUSED_TIMEOUT = 1800;
    public static final int DEFAULT_REAP_TIME = 180;
    public static final int DEFAULT_AGED_TIMEOUT = 0;
    public static final String DEFAULT_PURGE_POLICY = "EntirePool";
    public static final int DEFAULT_NUMBER_OF_SHARED_POOL_PARTITIONS = 0;
    public static final int DEFAULT_NUMBER_OF_FREE_POOL_PARTITIONS = 0;
    public static final int DEFAULT_FREE_POOL_DISTRIBUTION_TABLE_SIZE = 0;
    public static final int DEFAULT_DIAGNOSTIC_MODE = 0;
    public static final int DIAGNOSTIC_MODE_MIN_PERF_IMPACT = 1;
    public static final int DIAGNOSTIC_MODE_FULL = 2;
    public static final int DIAGNOSTIC_MODE_SELFTEST = 3;
    public static final int DEFAULT_HOLD_TIME_LIMIT = 10;
    public static final int DEFAULT_numConnectionsPerThreadLocal = 0;
    public static final int DEFAULT_MAX_CONNECTIONS_IN_RESERVE_POOL = 0;
    public static final int DEFAULT_NUMBER_OF_RESERVE_POOLS = 0;
    public static final int MAX_NUMBER_RESERVE_POOLS = 16;

    public int getConnectionTimeout();

    public int getMaxConnections();

    public int getMinConnections();

    public String getPurgePolicy();

    public int getReapTime();

    public int getAgedTimeout();

    public int getUnusedTimeout();

    public int getNumberOfSharedPoolPartitions();

    public int getNumberOfFreePoolPartitions();

    public int getFreePoolDistributionTableSize();

    public int getDiagnosticMode();

    public int getHoldTimeLimit();

    /**
     * This method sets the connection timeout. The connection timeout determines
     * the time, in milliseconds, after which a waiting requestor times out and
     * <code>ResourceAllocationException</code> is thrown.
     *
     * @param max int
     */
    public void setConnectionTimeout(int max);

    /**
     * This method sets the maximum number of used connections allowed.
     * Once this limit is reached, no new connections are created. If the
     * waiting time exceedes ConnectionTimeout, <code>ResourceAllocationException</code> exception
     * is thrown.
     *
     * @param max int
     */
    public void setMaxConnections(int max);

    /**
     * This method sets the minimum number of Connections available for reuse.
     * Once this limit is reached, the connections that exceeded the unused time
     * limit are not removed.
     *
     * @param max int
     */
    public void setMinConnections(int max);

    /**
     * This method sets the purge policy of the pool to which the ManagedConnection belongs.
     *
     * @param string Either "EntirePool" or "FailingConnectionOnly"
     */
    public void setPurgePolicy(String aPurgePolicy);

    /**
     * This method sets the time interval, in seconds at which the garbage collector
     * thread is run. Setting this value to the default or disabled (DEFAULT_REAP_TIME
     * DISABLE_REAP_TIME) turns off the connection manager collection, even if other
     * control values are set.
     *
     * @param max int Interval, in seconds, between garbage collector thread execution.
     */
    public void setReapTime(int max);

    /**
     * This method sets the maximum time (in milliseconds) that the unused connection
     * is kept. After this time, the connection is removed from the conneciton manager
     * vector and destroyed.
     *
     * @param max int The time that unused connection is kept in the connection manager vector.
     */

    public void setUnusedTimeout(int max);

    /**
     * This method sets the maximum time (in seconds) that the connection
     * is kept. After this time, the connection is removed from the conneciton manager
     * vector and destroyed.
     *
     * @param max int The time that unused connection is kept in the connection manager vector.
     */

    public void setAgedTimeout(int max);

    /**
     * This method sets the number of shared pool partitions (buckets).
     *
     * @param int number of partitions desired
     */
    public void setNumberOfSharedPoolPartitions(int numberOfPartitions);

    /**
     * This method sets the number of free pool partitions (buckets).
     *
     * @param int number of partitions desired
     */
    public void setNumberOfFreePoolPartitions(int numberOfPartitions);

    /**
     * This method sets hash table (distribution table) size for the free pool.
     *
     * @param int size of hash table
     */
    public void setFreePoolDistributionTableSize(int tableSize);

    /**
     * Sets the current setting of the diagnosticMode for this ConnectionFactory
     * Currently Defined levels are as follows:
     * <ul>
     * <li>0 - information that can be gathered with no performance impact (default).
     * <li>1 - information that can be gathered with minimum to moderate performance impact.
     * <li>2 - information that can be gather which has a moderate to significant performance impact.
     * </ul>
     */
    public void setDiagnosticMode(int diagLevel);

    /**
     * @param holdTimeLimit Indicates the amount of time a connection is held by an application
     *            before it is flagged in trace and diagnostic reports.
     */
    public void setHoldTimeLimit(int holdTimeLimit);

    /**
     * Thread local storage is used for connections if value is set greater then 0.
     */
    public void setnumConnectionsPerThreadLocal(int value);

    /**
     * This method return a value greater then 0 if thread local storage is used for connections.
     */
    public int getnumConnectionsPerThreadLocal();

    public Integer getMaxNumberOfMCsAllowableInThread();

    public void setMaxNumberOfMCsAllowableInThread(Integer maxNumberOfMCsAllowableInThread);

    public Boolean getThrowExceptionOnMCThreadCheck();

    public void setThrowExceptionOnMCThreadCheck(Boolean throwExceptionOnMCThreadCheck);

    /**
     *
     * @param value
     */
    public void setdefaultPretestOptimizationOverride(Boolean value);

    /**
     *
     * @return
     */
    public Boolean getdefaultPretestOptimizationOverride();

    /**
     * Gets the maximum number of connections that are allowed in a reserve pool.
     *
     * @return the maximum number of connections
     */
    public Integer getMaxConnectionsInReservePool();

    /**
     * Set the maximum number of connections allowed in a reserve pool.
     *
     * @param maxConnectionsInReservePool the maximum number of connections
     */
    public void setMaxConnectionsInReservePool(Integer maxConnectionsInReservePool);

    /**
     * Gets the number of reserve pools created.
     *
     * @return the number of reserve pools
     */
    public Integer getNumberOfReservePools();

    /**
     * Sets the number of reserve pools to create.
     *
     * @param numberOfReservePools the number of reserve pools
     */
    public void setNumberOfReservePools(Integer numberOfReservePools);
}
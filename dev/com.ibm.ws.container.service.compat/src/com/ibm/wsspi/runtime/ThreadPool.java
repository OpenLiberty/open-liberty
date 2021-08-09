/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.runtime;

/**
 * <p>
 * A <code>ThreadPool</code> provides a mechanism by which <code>Runnable</code> instances can be executed asynchronously on one of the threads managed by the pool.
 * </p>
 *
 * <p>
 * All ThreadPool implementations have three main configurable parameters that influence their behavior:
 * </p>
 *
 * <ul>
 *
 * <li><b>Keep alive time:</b> Once a pooled thread has been inactive for in excess of the keep alive period it will be allowed to die unless doing so would cause the pool to drop
 * below its minimum pool size.</li>
 *
 * <li><b>Minimum pool size:</b> Determines the minimum number of threads in the pool at any given time. Note that an implementation may choose to lazily create threads as work is
 * submitted to the pool so the minimum pool size may not be satisfied at instantiation. However, once the number of threads in the pool has exceeded the minimum pool size the
 * implementation must not allow the number of threads to fall below the configured minimum.</li>
 *
 * <li><b>Maximum pool size:</b> Determines the maximum number of threads in the pool at any given time. An implementation must guarantee not to pool more threads than the
 * configured maximum. Note that in addition to the pool of threads some implementations may choose to use additional storage to allow requests to the pool to be enqueued until a
 * thread is available to service the request. While this will allow a pool to accept greater than maximum pool size concurrent requests only maximum pool size of those requests
 * will ever be run concurrently.</li>
 *
 * </ul>
 *
 * see com.ibm.wsspi.runtime.ThreadPoolRepository
 */
public interface ThreadPool {
    /**
     * <p>
     * Specifies that a dispatch should be rejected and <code>DISPATCH_REJECTED_POOL_AT_CAPACITY</code> returned if the pool is at capacity.
     * </p>
     *
     * <p>
     * Note: A ThreadPool implementation <b>must</b> support this mode of execution.
     * </p>
     *
     * @see #execute(Runnable, int)
     * @see #DISPATCH_REJECTED_POOL_AT_CAPACITY
     */
    public static final int REJECT_WHEN_AT_CAPACITY = 0;

    /**
     * <p>
     * Specifies that, if the pool is at capacity, a dispatch should block until capacity is available to process the dispatch.
     * </p>
     *
     * <p>
     * Note: A ThreadPool implementation <b>may</b> support this mode of execution. If it does not an attempt to dispatch work to the pool using this mode will result in an
     * </code>UnsupportedOperationException</code> being thrown.
     * </p>
     *
     * @see #execute(Runnable, int)
     */
    public static final int WAIT_WHEN_AT_CAPACITY = 1;

    /**
     * <p>
     * Specifies that the pool's capacity should be expanded if a dispatch is made when it is already at capacity. In the event of the pool's capacity having reached its expansion
     * limit the dispatch will be rejected and <code>DISPATCH_REJECTED_POOL_AT_CAPACITY</code> returned.
     * </p>
     *
     * <p>
     * Note that this execute type is only supported by ThreadPool implementations that used storage in addition to the pooled threads to process dispatches. The described
     * expansion
     * refers to this additional storage rather than the pool's maximum size which must never exceed its configured value. If this mode is not supported an attempt to utilize it
     * will
     * result in an <code>UnsupportedOperationException</code> being thrown.
     * </p>
     *
     * @see #execute(Runnable, int)
     * @see #DISPATCH_REJECTED_POOL_AT_CAPACITY
     */
    public static final int EXPAND_WHEN_AT_CAPACITY_REJECT_AT_LIMIT = 2;

    /**
     * <p>
     * Specifies that the pool's capacity should be expanded if a dispatch is made when it is already at capacity. In the event of the capacity having already reached its expansion
     * limit, the dispatch will block until capacity is available.
     * </p>
     *
     * <p>
     * Note that this execute type is only supported by ThreadPool implementations that used storage in addition to the pooled threads to process dispatches. The described
     * expansion
     * refers to this additional storage rather than the pool's maximum size which must never exceed its configured value. If this mode is not supported an attempt to utilize it
     * will
     * result in an <code>UnsupportedOperationException</code> being thrown.
     * </p>
     *
     * @see #execute(Runnable, int)
     */
    public static final int EXPAND_WHEN_AT_CAPACITY_WAIT_AT_LIMIT = 3;

    /**
     * <p>
     * Returned by execute when a request has been successfully dispatched to the pool.
     * </p>
     *
     * @see #execute(Runnable)
     * @see #execute(Runnable, int)
     */
    public static final int DISPATCH_SUCCESSFUL = 0;

    /**
     * <p>
     * Returned by execute when an attempt to dispatch work to the pool has been rejected because it has been shutdown.
     * </p>
     *
     * @see #execute(Runnable)
     * @see #execute(Runnable, int)
     */
    public static final int DISPATCH_REJECTED_POOL_SHUTDOWN = 1;

    /**
     * <p>
     * Returned by execute when an attempt to dispatch work to the pool has been rejected because it is already at capacity.
     * </p>
     *
     * @see #execute(Runnable)
     * @see #execute(Runnable, int)
     */
    public static final int DISPATCH_REJECTED_POOL_AT_CAPACITY = 2;

    /**
     * <p>
     * Arrange for the given command to be executed by a thread in this pool. The method normally returns when the command has been handed off for (possibly later) execution.
     * </p>
     *
     * <p>
     * This method is equivalent to <code>execute(command, ThreadPool.REJECT_WHEN_AT_CAPACITY)</code> and <b>must</b> be supported by all ThreadPool implementations.
     * </p>
     *
     * @return A value indicating whether or not the attempt to dispatch work
     *         to the pool was successful.
     *
     * @see #DISPATCH_SUCCESSFUL
     * @see #DISPATCH_REJECTED_POOL_SHUTDOWN
     * @see #DISPATCH_REJECTED_POOL_AT_CAPACITY
     * @see #REJECT_WHEN_AT_CAPACITY
     * @see #execute(Runnable, int)
     */
    public int execute(Runnable command);

    /**
     * <p>
     * Arrange for the given command to be executed by a thread in this pool using the given mode which determines the call's behavior when the pool is at capacity.
     * </p>
     *
     * @param command
     *            The work to be dispatched.
     *
     * @param mode
     *            Specifies how the pool will behave when work is
     *            dispatched to it and it is at capacity.
     *
     * @return A value indicating whether or not the attempt to dispatch work
     *         to the pool was successful.
     *
     * @throws UnsupportedOperationException
     *             Thrown if the given execution mode
     *             is not supported by the ThreadPool implementation
     *
     * @see #WAIT_WHEN_AT_CAPACITY
     * @see #REJECT_WHEN_AT_CAPACITY
     * @see #EXPAND_WHEN_AT_CAPACITY_REJECT_AT_LIMIT
     * @see #EXPAND_WHEN_AT_CAPACITY_WAIT_AT_LIMIT
     * @see #DISPATCH_SUCCESSFUL
     * @see #DISPATCH_REJECTED_POOL_SHUTDOWN
     * @see #DISPATCH_REJECTED_POOL_AT_CAPACITY
     */
    public int execute(Runnable command, int mode) throws UnsupportedOperationException;

    /**
     * <p>
     * Sets the minimum number of threads to be used by the pool. If the supplied value is lower than the current value excess threads will be terminated when they next become
     * idle.
     * If the supplied value is larger than the current value new threads may be created.
     * </p>
     *
     * <p>
     * A ThreadPool implementation that does not support dynamic configuration of the minimum poolsize may throw an <code>UnsupportedOperationException</code>.
     * </p>
     *
     * @param minimumPoolSize
     *            The new minimum size of the pool
     *
     * @throws UnsupportedOperationException
     *             Thrown if the implementation does not
     *             support dynamic configuration of the pool's minimum size.
     */
    public void setMinimumPoolSize(int minimumPoolSize) throws UnsupportedOperationException;

    /**
     * <p>
     * Sets the maximum number of threads to be used by the pool. If the supplied value is lower than the current value excess threads will be terminated when they next become
     * idle.
     * If the supplied value is larger than the current value new threads may be created.
     * </p>
     *
     * <p>
     * A ThreadPool implementation that does not support dynamic configuration of the maximum poolsize may throw an <code>UnsupportedOperationException</code>.
     * </p>
     *
     * @param maximumPoolSize
     *            The new maximum size of the pool
     *
     * @throws UnsupportedOperationException
     *             Thrown if the implementation does not
     *             support dynamic configuration of the pool's maximum size.
     */
    public void setMaximumPoolSize(int maximumPoolSize);

    /**
     * <p>
     * Gets the maximum number of threads that can be pooled at any given time.
     *
     * @return The maximum number of threads that can be pooled
     *         </p>
     */
    public int getMaximumPoolSize();

    /**
     * <p>
     * Sets the time (in milliseconds) for which an unused thread will be kept alive. Should this time be exceeded the thread will be discarded until the pool reaches its minimum
     * size.
     * </p>
     *
     * @param keepAliveTime
     *            The new keep-alive time (in milliseconds) for the pool
     *
     * @throws UnsupportedOperationException
     *             Thrown if the implementation does not
     *             support dynamic configuration of the pool's keep-alive time.
     */
    public void setKeepAliveTime(long keepAliveTime);

    /**
     * <p>
     * Instructs the pool to shutdown. Previously submitted work will be processed as normal but no new work will be accepted by the pool. An attempt to dispatch work to a pool
     * that
     * has been shutdown will result in the request being rejected and <code>DISPATCH_REJECTED_POOL_SHUTDOWN</code> being returned.
     * </p>
     *
     * <p>
     * To ensure that the resources which relate to this <code>ThreadPool</code> are released in a timely manner this method must be called when the <code>ThreadPool</code> is no
     * longer required.
     * </p>
     *
     * @see #DISPATCH_REJECTED_POOL_SHUTDOWN
     */
    public void shutdown();

    /**
     * <p>
     * Allows the user of a <code>ThreadPool</code> instance to inform the pool (by invoking the method with <code>true</code>) that the current thread is waiting for an unknown
     * and
     * potentially prolonged period and may become unresponsive. Once that period has elapsed and the thread is no longer waiting the method should be invoked with
     * <code>false</code>
     * .
     * </p>
     *
     * @param threadIsWaiting
     *            Indicates whether or not the current thread is waiting.
     */
    public void setThreadWaiting(boolean threadIsWaiting);

    /**
     * <p>
     * A marker interface to allow the identification of a thread that belongs to a <code>ThreadPool</code> instance.
     * </p>
     *
     * <p>
     * Note: all threads created by a <code>ThreadPool</code> implementation <b>must</b> implement this interface.
     * </p>
     *
     * @see ThreadPool
     */
    public interface WorkerThread {

    }
}

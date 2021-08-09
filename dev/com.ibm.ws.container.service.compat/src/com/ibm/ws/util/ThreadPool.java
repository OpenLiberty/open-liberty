/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

//Alex import static com.ibm.ffdc.Manager.Ffdc;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TooManyListenersException;

import com.ibm.ejs.ras.RasHelper;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.jtc.adapter.PlatformAdapterAccessor;
import com.ibm.ws.exception.WsException;

public class ThreadPool {

    private static TraceComponent tc = Tr.register(ThreadPool.class, "Runtime", "com.ibm.ws.runtime.runtime");

    private static ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = ThreadContextAccessor.getThreadContextAccessor(); // D527355.3

    /**
     * The maximum pool size; used if not otherwise specified. Default
     * value is choosen conservatively at a value of 10
     **/
    public static final int DEFAULT_MAXIMUMPOOLSIZE = 10; // d211700

    /**
     * The minimum pool size; used if not otherwise specified. Default
     * value is 0.
     **/
    public static final int DEFAULT_MINIMUMPOOLSIZE = 0; // d211700

    /**
     * The maximum time to keep worker threads alive waiting for new
     * tasks; used if not otherwise specified. Default value is five
     * seconds (5000 milliseconds).
     **/
    public static final long DEFAULT_KEEPALIVETIME = 5 * 1000;

    // begin D186668

    /**
     * Specifies that a dispatch should block if the request queue
     * is already full.
     * 
     * @see #execute(Runnable, int)
     * 
     * 
     */
    public static final int WAIT_WHEN_QUEUE_IS_FULL = 0;

    /**
     * Specifies that a dispatch should throw an exception if
     * the request queue is already full.
     * 
     * @see #execute(Runnable, int)
     * 
     * 
     */
    public static final int ERROR_WHEN_QUEUE_IS_FULL = 1;

    // end D186668

    /**
     * Specifies that the request queue should be expanded if
     * a dispatch is made when it is full. Should the queue
     * have already reached its expansion limit an exception
     * is thrown.
     * 
     * @see #execute(Runnable, int)
     * @see #setQueueExpansionLimit(int)
     * 
     * 
     */
    public static final int EXPAND_WHEN_QUEUE_IS_FULL_ERROR_AT_LIMIT = 2;

    /**
     * Specifies that the request queue should be expanded if
     * a dispatch is made when it is full. Should the queue
     * have already reached its expansion limit the dispatch
     * will block.
     * 
     * @see #execute(Runnable, int)
     * @see #setQueueExpansionLimit(int)
     * 
     * 
     */
    public static final int EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT = 3;

    /**
     * The maximum number of threads allowed in pool.
     * 
     * @deprecated Use getMaximumPoolSize()
     */

    @Deprecated
    protected int maximumPoolSize_ = DEFAULT_MAXIMUMPOOLSIZE;

    /**
     * The minumum number of threads to maintain in pool.
     * 
     * @deprecated Use getMinimumPoolSize()
     */
    @Deprecated
    protected int minimumPoolSize_ = DEFAULT_MINIMUMPOOLSIZE;

    /**
     * The current pool size.
     * 
     * @deprecated Use getPoolSize()
     */
    @Deprecated
    protected int poolSize_ = 0;

    /**
     * @deprecated This will be private
     */

    @Deprecated
    protected int activeThreads = 0;

    /**
     * The maximum time for an idle thread to wait for new task.
     * 
     * @deprecated Use getKeepAliveTime()
     */
    @Deprecated
    protected long keepAliveTime_ = DEFAULT_KEEPALIVETIME;

    /**
     * @deprecated This will become private in a future release.
     */

    @Deprecated
    protected BoundedBuffer requestBuffer;

    /**
     * Shutdown flag - latches true when a shutdown method is called
     * in order to disable queuing/handoffs of new tasks.
     * 
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    protected boolean shutdown_ = false;

    /**
     * The set of active threads, declared as a map from workers to
     * their threads. This is needed by the interruptAll method. It
     * may also be useful in subclasses that need to perform other
     * thread management chores.
     * 
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    protected final Map threads_;

    /**
     * Unique thread identifier within this ThreadPool instance.
     * 
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    protected int threadid = 0;

    /**
     * Should we queue thread requests, rather then creating new threads.
     * 
     * @deprecated Use isGrowWasNeeded() and setGrowAsNeeded()
     */
    @Deprecated
    protected boolean growasneeded = false;

    /**
     * The context class loader to be set on new threads.
     */
    private ClassLoader contextClassLoader;

    /**
     * Controls the clearing of any java.lang.ThreadLocal
     * objects after each dispatch.
     * 
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    private boolean clearJavaLangThreadLocals = false; // LIDB2255-58

    /**
     * Cached thread priority.
     * 
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    protected int threadpriority = Thread.NORM_PRIORITY;

    private static ThreadPoolListener[] ZERO_TP_LISTENERS = new ThreadPoolListener[0];

    /**
     * @deprecated This will be private in a future release.
     */
    @Deprecated
    protected ThreadPoolListener[] threadPoolListeners;

    /**
     * @deprecated This will be private in a future release.
     */

    @Deprecated
    protected String name;

    // begin LIDB3275

    private MonitorPlugin monitorPlugin = null;

    private int requestBufferExpansionLimit_;

    private int requestBufferInitialCapacity_;

    private boolean lastThreadCheckDidntComplete = false; // D222794

    // PM13174 - Tracks whether the expansion of the thread pool has been logged
    private boolean poolGrowthLogged = false;

    private final boolean isZOS = false; // @LIDB3275.1A

    /**
     * specifies whether the threads in the pool are to be decorated via
     * setupThreadStub
     * 
     * @since 6.0.2 (263391/331761)
     */
    protected boolean _isDecoratedZOS = false; // 331761A

    /**
     * Reflection is used to resolve the xMemCRBridgeImpl.setupThreadStub method
     * This method is called by DecoratedZOSThread instances to decorate the
     * threads
     * so that requests processed by the threads on zOS can zWLM from the CR to
     * the SR.
     * 
     * Marked volatile to ensure correct initialisation (without it the contents
     * of the Method
     * object may not be seen by a thread that sees the reference as non-null)
     */
    protected static volatile java.lang.reflect.Method xMemSetupThread = null; // 331761A

    /**
     * Specifies whether the full buffer message has been displayed for this
     * thread
     * pool.
     */
    private boolean bufferLimitReached = false; // F743-24122

    /**
     * Provides a plug point for monitoring activity in
     * the thread pool.
     * 
     * @since 5.0.3 (LIDB3275)
     */

    public interface MonitorPlugin {

        /**
         * Called during a check of all active threads. The implementation
         * should return true if the thread is considered to be hung (that is,
         * active for too long).
         * 
         * @param threadName
         *            - the thread's name..
         * @param threadNumber
         *            - the thread's id.
         * @param timeActiveInMillis
         *            - the approximate time (in milliseconds) that
         *            the thread has been active.
         * @return true if the thread is hung; false if it is not.
         */
        boolean checkThread(Thread thread, String threadId, long timeActiveInMillis); // d212112

        /**
         * Called when a thread that was previously marked as
         * hung has now completed work.
         * 
         * @param threadName
         *            - the thread's name
         * @param threadNumber
         *            - the thread's id
         * @param long timeActiveInMillis the approximate time (in milliseconds)
         *        that the thread has been active.
         */
        void clearThread(Thread thread, String threadId, long timeActiveInMillis); // d212112
    }

    // end LIDB3275

    /**
     * DecoratedCRThread is a specialization of WorkerThread. A DecoratedCRThread
     * is
     * called back by xMem code to process tasks after the xMem component has
     * decorated
     * the thread so that CR to SR PCs can be processed on the thread.
     */

    public interface DecoratedCRThread extends WorkerThread { // 331761A

        /**
         * This method is called from the xMem component once the thread has been
         * decorated.
         * It is functionally equivalent to the run method on the Worker class.
         */
        void processTasks();
    }

    // begin 189357.2

    /**
     * Defines an interface for Worker Threads.
     * 
     */

    public interface WorkerThread extends com.ibm.wsspi.runtime.ThreadPool.WorkerThread {
        /**
         * Disables hang detection for the work currently dispatched
         * on the thread. If detection is enabled, it will be
         * restored on any subsequent dispatches.
         */
        void disableHangDetectionForCurrentDispatch();
    }

    // end 189357.2

    // begin @LIDB3275.1A
    /**
     * Defines an interface for z/OS Worker Threads.
     * 
     */

    public interface WorkerZOSThread {
        /**
         * Method called from CommonBridge.threadStarted on ZOSWorker thread
         */
        void threadStarted();

        /**
         * Method called from CommonBridge.threadStarted on ZOSWorker thread
         */
        void threadReturned();
    }

    // end @LIDB3275.1A

    // begin LIDB2255

    /**
     * Defines the interface required on each thread in order to support
     * WasThreadLocal requirements.
     * 
     * <p>
     * Note: this is <u>not</u> an SPI.
     */

    public interface WasThreadLocalSupport {

        /**
         * Gets the WasThreadLocal instance for the current thread
         * associated with the given key.
         * 
         * @param key
         * @return The WasThreadLocal instance (if it exists) or null.
         * @see com.ibm.websphere.spi.util.WasThreadLocal
         */
        Object get(Object key);

        /**
         * Sets the WasThreadLocal instance for the current thread
         * associated with the given key.
         * 
         * @param key
         * @param value
         */
        void set(Object key, Object value);
    }

    // end LIDB2255

    /**
     * Create a thread cache, whose threads are to be children of the group.
     * 
     * @param group
     *            The thread group to which this thread cache is bound.
     * @param nstart
     *            Number of thread to create in advance.
     */
    public ThreadPool(int minSize, int maxSize, ThreadPoolListener[] tpls) {
        this(null, minSize, maxSize, tpls);
    }

    /**
     * Create a thread cache, after creating a new thread group.
     * 
     * @param name
     *            The name of the thread group to create.
     */
    public ThreadPool(String name, int minSize, int maxSize) {
        this(name, minSize, maxSize, null);
    }

    public ThreadPool(String name, int minPoolSize, int maxPoolSize, ThreadPoolListener[] tpls) {
        // Alex PlatformHelper platFormHelper = null; // @LIDB3275.1A
        this.name = name;
        maximumPoolSize_ = maxPoolSize;
        minimumPoolSize_ = minPoolSize;

        requestBufferExpansionLimit_ = maxPoolSize * 10;

        threads_ = new HashMap();

        // begin LIDB2255-58, D201932
        String clearThreadLocals = (String) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
        {
            return System.getProperty("com.ibm.websphere.threadpool.clearThreadLocal");
        }
        });

        if ((clearThreadLocals != null) && Boolean.valueOf(clearThreadLocals).booleanValue()) {
            setClearThreadLocals(true);
        }
        // end LIDB2255-58, D201932

        requestBufferInitialCapacity_ = maximumPoolSize_;

        requestBuffer = new BoundedBuffer(requestBufferInitialCapacity_);

        if (tpls == null) {
            threadPoolListeners = ZERO_TP_LISTENERS;
        } else {
            threadPoolListeners = tpls.clone();
        }

        for (int i = 0; i < threadPoolListeners.length; ++i) {
            threadPoolListeners[i].threadPoolCreated(this);
        }

        // Alex platFormHelper = PlatformHelperFactory.getPlatformHelper(); //
        // @LIDB3275.1A
        // Alex isZOS = platFormHelper.isZOS(); // @LIDB3275.1A
    }

    public String getName() {
        return name;
    }

    /**
     * Controls the pool's behavior with respect to thread local.
     * <p>
     * <b>WARNING:</b> Altering this attribute when work has already been dispatched to the thread pool will result in undefined behavior.
     * 
     * @param b
     *            when true, thread locals will be cleared after
     *            every dispath. When false, thread locals will not
     *            be cleared.
     * @since LIDB1855-58
     * @deprecated This capability will go away in a future
     *             release (the behavior will be to never clear thread
     *             locals).
     */
    @Deprecated
    public void setClearThreadLocals(boolean b) {
        this.clearJavaLangThreadLocals = b;
        if (b) {
            Tr.warning(tc, "WSVR0623W", this.name);
        }
    }

    /**
     * Get the maximum number of threads that may be used
     * to execute concurrent work. Note that this number may
     * still be exceeded if the pool is growable.
     * 
     * @see #setGrowAsNeeded(boolean)
     * 
     * 
     */
    public synchronized int getMaximumPoolSize() {
        return maximumPoolSize_;
    }

    /**
     * Set the maximum number of threads that may be used
     * to execute concurrent work. Note that this number may
     * still be exceeded if the pool is growable.
     * 
     * @exception IllegalArgumentException
     *                if newMaximum is
     *                less or equal to zero. It is not considered an error to
     *                set the maximum to be less than than the minimum. However,
     *                in this case there are no guarantees about behavior.
     * 
     * 
     */

    public synchronized void setMaximumPoolSize(int newMaximum) {
        if (newMaximum <= 0)
            throw new IllegalArgumentException();

        maximumPoolSize_ = newMaximum;
    }

    /**
     * Gets the minimum number of threads that may be used
     * to execute concurrent work.
     * 
     * <p>
     * Note: the pool does not necessarily create the minimum number of threads (they may be created on demand). However, the pool of threads will not shrink below this minimum
     * value.
     * 
     * 
     */

    public synchronized int getMinimumPoolSize() {
        return minimumPoolSize_;
    }

    /**
     * Set the minimum number of threadsthat may be used
     * to execute concurrent work.
     * 
     * @exception IllegalArgumentException
     *                if newMinimum is less
     *                than zero. It is not considered an error to set the minimum
     *                to be greater than the maximum. However, in this case there
     *                are no guarantees about behavior.)
     * 
     * 
     */

    public synchronized void setMinimumPoolSize(int newMinimum) {
        if (newMinimum < 0)
            throw new IllegalArgumentException();

        minimumPoolSize_ = newMinimum;
    }

    /**
     * Gets the current number of active threads in the pool.
     * 
     * <p>
     * Note that this number is somewhat volatile.
     * 
     * 
     */

    public synchronized int getPoolSize() {
        return poolSize_;
    }

    /**
     * Get the number of milliseconds to keep idle threads while waiting
     * for new work. A negative value means to wait forever. A zero
     * value means not to wait at all.
     * 
     * 
     */
    public synchronized long getKeepAliveTime() {
        return keepAliveTime_;
    }

    /**
     * Set the number of milliseconds to keep idle threads alive
     * while waiting for new work. A negative value means to wait
     * forever. A zero value means not to wait at all.
     * 
     * 
     */
    public synchronized void setKeepAliveTime(long msecs) {
        keepAliveTime_ = msecs;
    }

    // begin D186668

    /**
     * Sets the size of the request buffer. If work has been
     * dispatched on this thread pool, the behavior of this
     * method and all subsequent calls to this pool are
     * undefined.
     * 
     * As a result of the request buffer size being set the
     * expansion limit of the buffer is set to be 10
     * times greater than the newly configured buffer size.
     */
    public synchronized void setRequestBufferSize(int size) {

        if (size <= 0) {
            throw new IllegalArgumentException("request buffer size must be greater than zero");
        }

        if (this.requestBuffer.size() != 0) {
            throw new IllegalStateException("cannot resize non-empty ThreadPool request buffer");
        }

        this.requestBuffer = new BoundedBuffer(size);
        requestBufferInitialCapacity_ = size;
        requestBufferExpansionLimit_ = requestBufferInitialCapacity_ * 10;
    }

    public synchronized int getRequestBufferSize() {
        return this.requestBuffer.capacity();
    }

    // end D186668

    /**
     * Create and start a thread to handle a new command. Call only
     * when holding lock.
     * 
     * @deprecated This will be private in a future release.
     */

    @Deprecated
    protected void addThread(Runnable command) {
        Worker worker;
        if (_isDecoratedZOS) // 331761A
            worker = new DecoratedZOSWorker(command, threadid++); // 331761A
        else
            // 331761A
            worker = new Worker(command, threadid++); // LIDB1855-58

        // D527355.3 - If the current thread is an application thread, then the
        // creation of a new thread will copy the application class loader as the
        // context class loader. If the new thread is long-lived in this pool, the
        // application class loader will be leaked.
        if (contextClassLoader != null && THREAD_CONTEXT_ACCESSOR.getContextClassLoader(worker) != contextClassLoader) {
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(worker, contextClassLoader);
        }

        Thread.interrupted(); // PK27301
        threads_.put(worker, worker);
        ++poolSize_;
        ++activeThreads;

        // PK77809 Tell the buffer that we have created a new thread waiting for
        // work
        if (command == null) {
            requestBuffer.incrementWaitingThreads();
        }

        // must fire before it is started
        fireThreadCreated(poolSize_);
        try {
            worker.start();
        } catch (OutOfMemoryError error) {
            // 394200 - If an OutOfMemoryError is thrown because too many threads
            // have already been created, undo everything we've done.
            // Alex Ffdc.log(error, this, ThreadPool.class.getName(), "626"); //
            // D477704.2
            threads_.remove(worker);
            --poolSize_;
            --activeThreads;
            fireThreadDestroyed(poolSize_);
            throw error;
        }
    }

    // begin @LIDB3275.1A
    /**
     * This method is called from CommonBridge.createApplicationThreads on z/OS
     * only.
     * If it is called on any other platform then it will throw an
     * UnsupportedOperationException
     */
    /*
     * public void addZOSThread(int internalWorkThread, int useWLM, long
     * stackSize, ZThreadUtilities utils) throws UnsupportedOperationException {
     * ZOSWorker worker = new ZOSWorker(internalWorkThread, useWLM, stackSize,
     * utils);
     * Thread.interrupted(); // PK27301
     * threads_.put(worker, worker);
     * ++poolSize_;
     * // must fire before it is started
     * fireThreadCreated(poolSize_);
     * worker.start();
     * }
     */

    // end @LIDB3275.1A

    /**
     * Create and start up to numberOfThreads threads in the pool.
     * Return the number created. This may be less than the number
     * requested if creating more would exceed maximum pool size bound.
     * 
     * @deprecated This method will go away in a future release. All
     *             thread creation should be done lazily.
     */
    @Deprecated
    public int createThreads(int numberOfThreads) {

        int ncreated = 0;

        for (int i = 0; i < numberOfThreads; ++i) {
            synchronized (this) {
                if (poolSize_ < maximumPoolSize_) {
                    addThread(null);
                    ++ncreated;
                } else
                    break;
            }
        }

        return ncreated;
    }

    /**
     * Drains the request queue and interrupts all executing threads.
     */

    public synchronized void shutdownNow() {
        shutdown_ = true; // don't allow new tasks
        minimumPoolSize_ = maximumPoolSize_ = 0; // don't make new threads
        interruptAll(); // interrupt all existing threads
    }

    /**
     * @deprecated
     */

    @Deprecated
    public synchronized void shutdownAfterProcessingCurrentlyQueuedTasks() {
        shutdown_ = true;

        if (poolSize_ == 0) // disable new thread construction when idle
            minimumPoolSize_ = maximumPoolSize_ = 0;
    }

    /**
     * Interrupt all threads in the pool, causing them all to
     * terminate. Assuming that executed tasks do not disable (clear)
     * interruptions, each thread will terminate after processing its
     * current task. Threads will terminate sooner if the executed tasks
     * themselves respond to interrupts.
     * 
     * @deprecated
     */
    @Deprecated
    public synchronized void interruptAll() {
        for (Iterator it = threads_.values().iterator(); it.hasNext();) {
            Thread t = (Thread) (it.next());
            t.interrupt();
        }
    }

    /**
     * Wait for a shutdown pool to fully terminate, or until the timeout
     * has expired. This method may only be called <em>after</em> invoking
     * shutdownNow or
     * shutdownAfterProcessingCurrentlyQueuedTasks.
     * 
     * @param maxWaitTime
     *            the maximum time in milliseconds to wait
     * @return true if the pool has terminated within the max wait period
     * @exception IllegalStateException
     *                if shutdown has not been requested
     * @exception InterruptedException
     *                if the current thread has been interrupted in the course of
     *                waiting
     * 
     * @deprecated
     */
    @Deprecated
    public synchronized boolean awaitTerminationAfterShutdown(long maxWaitTime) throws InterruptedException {
        if (!shutdown_)
            throw new IllegalStateException();

        if (poolSize_ == 0)
            return true;

        long waitTime = maxWaitTime;

        if (waitTime <= 0)
            return false;

        long start = System.currentTimeMillis();

        for (;;) {
            wait(waitTime);

            if (poolSize_ == 0)
                return true;

            waitTime = maxWaitTime - (System.currentTimeMillis() - start);

            if (waitTime <= 0)
                return false;
        }
    }

    /**
     * Wait for a shutdown pool to fully terminate. This method may
     * only be called <em>after</em> invoking shutdownNow or
     * shutdownAfterProcessingCurrentlyQueuedTasks.
     * 
     * @exception IllegalStateException
     *                if shutdown has not been requested
     * @exception InterruptedException
     *                if the current thread has been interrupted in the course of
     *                waiting
     * @deprecated
     */
    @Deprecated
    public synchronized void awaitTerminationAfterShutdown() throws InterruptedException {
        if (!shutdown_)
            throw new IllegalStateException();

        while (poolSize_ > 0)
            wait();
    }

    /**
     * Cleanup method called upon termination of worker thread.
     * 
     * @deprecated This will become private in a future release.
     **/
    @Deprecated
    protected synchronized void workerDone(Worker w, boolean taskDied) {

        threads_.remove(w);

        if (taskDied) {
            --activeThreads;
            --poolSize_;
        }

        if (poolSize_ == 0 && shutdown_) {
            maximumPoolSize_ = minimumPoolSize_ = 0; // disable new threads
            notifyAll(); // notify awaitTerminationAfterShutdown
        }

        fireThreadDestroyed(poolSize_);
    }

    /**
     * Get a task from the handoff queue, or null if shutting down.
     * 
     * @deprecated This will become private in a future release.
     */

    @Deprecated
    protected Runnable getTask(boolean oldThread) throws InterruptedException { // PK77809
        long waitTime;
        Runnable r = null;
        boolean firstTime = true;

        while (true) {
            synchronized (this) {
                // System.out.println("1 " + activeThreads + " : " + poolSize_);
                if (firstTime) {
                    --activeThreads;

                    // PK77809 Let the buffer know we have a waiting thread
                    if (oldThread) {
                        requestBuffer.incrementWaitingThreads();
                    }

                }

                if (poolSize_ > maximumPoolSize_) {
                    // Cause to die if too many threads
                    if (!growasneeded || !firstTime) {
                        // System.out.println("2 die");
                        --poolSize_;

                        // PK77809 Let the buffer know we have a waiting thread
                        requestBuffer.decrementWaitingThreads();

                        return null;
                    }
                }

                // infinite timeout if we are below the minimum size
                waitTime = (shutdown_) ? 0 : (poolSize_ <= minimumPoolSize_) ? -1 : keepAliveTime_;
            }

            // PK27301 start
            try {
                r = (waitTime >= 0) ? (Runnable) (requestBuffer.poll(waitTime)) : (Runnable) (requestBuffer.take());
            } catch (InterruptedException e) {
                ++activeThreads;

                // PK77809 Let the buffer know we have a waiting thread
                synchronized (this) {
                    requestBuffer.decrementWaitingThreads();
                };

                throw e;
            }
            // PK27301 end

            synchronized (this) {
                // System.out.println("3 r=" + r);
                if (r == null) {
                    r = (Runnable) requestBuffer.poll(0);
                }
                if (r != null) {
                    ++activeThreads;
                    break;
                } else if (poolSize_ > minimumPoolSize_) {
                    // discount the current thread
                    // System.out.println("4 die");
                    poolSize_--;

                    // PK77809 Signal the buffer we have removed a thread
                    requestBuffer.decrementWaitingThreads();

                    break;
                }
                // System.out.println("going again");
            }

            firstTime = false;
        }

        return r;
    }

    /**
     * Enable/disable the thread pool to grow as needed.
     * This flag should be turned on only if always getting a thread as fast
     * as possible is critical.
     * 
     * @param onoff
     *            The toggle.
     * 
     */
    public void setGrowAsNeeded(boolean onoff) {
        this.growasneeded = onoff;
    }

    /**
    *
    */
    public boolean isGrowAsNeeded() {
        return growasneeded;
    }

    /**
     * Sets the context class loader to be used when new threads are created. By
     * default, the context class loader of the thread calling <tt>execute</tt> will be used. If this occurs within the context of an application thread,
     * then the application class loader will not be eligible for garbage
     * collection until the thread expires.
     * 
     * @param cl
     *            the context class loader for new threads
     */
    public void setContextClassLoader(ClassLoader cl) {
        contextClassLoader = cl;
    }

    /**
     * Gets the context class loader to be used when new threads are created.
     * 
     * @return the context class loader for new threads
     */
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    /**
     * Set all the cached threads priority.
     * Changing the cached thread priority should be done before the thread
     * cache is initialized, it will <em>not</em> affect already created
     * threads.
     * 
     * @param priority
     *            The new cachewd threads priority.
     * 
     */
    public void setThreadPriority(int priority) {
        threadpriority = priority;
    }

    /**
     * Get the cached thread normal priority.
     * 
     * @return Currently assigned cached thread priority.
     * 
     */
    public int getThreadPriority() {
        return threadpriority;
    }

    /**
     * sets this thread pool to create threads which are decorated via
     * setupThreadStub
     * 
     */
    public void setDecoratedZOS() { // 331761A

        if (xMemSetupThread == null) {
            try {
                final Class xMemCRBridgeClass = Class.forName("com.ibm.ws390.xmem.XMemCRBridgeImpl");
                xMemSetupThread = xMemCRBridgeClass.getMethod("setupThreadStub", new Class[] { java.lang.Object.class });
            } catch (Throwable t) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unexpected exception caught accessing XMemCRBridgeImpl.setupThreadStub", t);
                // Alex Ffdc.log(t, this, "com.ibm.ws.util.ThreadPool.setDecoratedZOS",
                // "893"); // D477704.2
            }
        }
        if (xMemSetupThread != null) {
            _isDecoratedZOS = true;
        }
    }

    // begin D186668

    /**
    *
    */
    public static class ThreadPoolQueueIsFullException extends WsException {
        private static final long serialVersionUID = 5986384706904725945L;

        public ThreadPoolQueueIsFullException() {
            super();
        }

        public ThreadPoolQueueIsFullException(String arg0) {
            super(arg0);
        }

        public ThreadPoolQueueIsFullException(Throwable arg0) {
            super(arg0);
        }

        public ThreadPoolQueueIsFullException(String arg0, Throwable arg1) {
            super(arg0, arg1);
        }

    }

    // begin D189357.2

    private int daemonId = -1; // D189357.2

    /**
     * Dispatch work on a daemon thread. This thread is not accounted
     * for in the pool. There are no corresponding
     * ThreadPoolListener events. There is no MonitorPlugin support.
     * 
     * 
     */

    public void executeOnDaemon(Runnable command) {

        int id = 0;
        final Runnable commandToRun = command;

        synchronized (this) {
            this.daemonId++;
            id = this.daemonId;
        }

        final String runId = name + " : DMN" + id; // d185137.2

        Thread t = (Thread) AccessController.doPrivileged(new PrivilegedAction()
        { // d185137.2
            public Object run()
        {
            // return new Thread(commandToRun); // d185137.2
            Thread temp = new Thread(commandToRun, runId); // d185137.2
            temp.setDaemon(true);
            return temp;
        }
        });
        t.start();
    }

    // end D189357.2

    /**
     * Arrange for the given command to be executed by a thread in this
     * pool. The method normally returns when the command has been
     * handed off for (possibly later) execution.
     * 
     * This is equivalent to <code>execute(command, ThreadPool.WAIT_WHEN_QUEUE_IS_FULL);</code>
     * 
     * @see #execute(Runnable, int)
     * 
     * 
     */

    public void execute(Runnable command) throws InterruptedException, IllegalStateException {
        try {
            execute(command, WAIT_WHEN_QUEUE_IS_FULL, 0);
        } catch (ThreadPoolQueueIsFullException e) {
            // we should not get here.
            // Alex Ffdc.log(e, this, ThreadPool.class.getName(), "564"); // D477704.2
        }
    }

    /**
     * Arrange for the given command to be executed by a thread in this
     * pool. The call's behavior when the pool and its internal request
     * queue are at full capacity is determined by the blockingMode.
     * 
     * 
     * 
     * @param command
     *            - the work to be dispatched.
     * 
     * @param blockingMode
     *            - specifies whether this call will block
     *            until capacity becomes available(WAIT_WHEN_QUEUE_IS_FULL) or
     *            throws an exception (ERROR_WHEN_QUEUE_IS_FULL).
     * 
     * @see #WAIT_WHEN_QUEUE_IS_FULL
     * @see #ERROR_WHEN_QUEUE_IS_FULL
     */

    public void execute(Runnable command, int blockingMode) throws InterruptedException, ThreadPoolQueueIsFullException, IllegalStateException { // D186668
        execute(command, blockingMode, 0);
    }

    /**
     * Arrange for the given command to be executed by a thread in this
     * pool. If the pools internal request buffer is full, the call
     * will block for at most timeoutInMillis milliseconds.
     * 
     * @param command
     *            - the work to be dispatched.
     * @param timeoutInMillis
     *            - the amount of time to block and
     *            wait if the request buffer is full.
     * @return the command if it was, indeed queued to the pool or
     *         null if the request timed out.
     * 
     * 
     */

    public Runnable execute(Runnable command, long timeoutInMillis) throws InterruptedException, IllegalStateException {
        try {
            return execute(command, WAIT_WHEN_QUEUE_IS_FULL, timeoutInMillis);
        } catch (ThreadPoolQueueIsFullException e) {
            // we should not get here.
            // Alex Ffdc.log(e, this, ThreadPool.class.getName(), "855"); // D477704.2
            return null;
        }
    }

    private Runnable execute(Runnable command, int blockingMode, long timeoutInMillis) throws InterruptedException, ThreadPoolQueueIsFullException, IllegalStateException {

        // Before we synchronize check that the pool hasn't been shutdown.
        // This prevents the performance optimization below (220640)
        // allowing work to be enqueued after the pool's been shutdown.
        // Note that as we're doing the check outside a synchronized block
        // it isn't thread safe so we have to check again within the sync
        // block below.
        if (shutdown_) {
            throw new IllegalStateException();
        }

        // D220640 : Before we synchronize, offer up the command to the
        // request buffer. If it works and we know we will not have
        // to do any pool maintenance, we can get out of here without
        // having to acquire a lock.
        //
        // Implementation note: For now, "no pool maintenance" is
        // equivalent to a non-oscillating pool (min=max, non-growable)
        // where we have already added the minimum number of threads.

        boolean offered = requestBuffer.offer(command, 0);

        if (offered && (minimumPoolSize_ == maximumPoolSize_) && (poolSize_ == minimumPoolSize_) && !growasneeded) {
            return command;
        }

        boolean yield = false;

        for (;;) {

            if (yield) {
                Thread.yield();
            }

            // PK47789 - Move the call to expand() into the same critical
            // section as the call to offer() to avoid the scenario where
            // many threads fail to offer and then all of them attempt to
            // expand the buffer resulting in an overly large request
            // buffer. In the referenced APAR, the request buffer became
            // so large that attempts to expand the buffer were resulting
            // in OutOfMemoryErrors.
            synchronized (this) {
                if (!offered) {

                    offered = requestBuffer.offer(command, 0);

                    if (offered && (minimumPoolSize_ == maximumPoolSize_) && (poolSize_ == minimumPoolSize_) && !growasneeded) {
                        return command;
                    }
                }

                if (shutdown_) {
                    throw new IllegalStateException();
                }

                // offer moved above
                // Only yield if we are growing unbounded
                if (growasneeded && !offered && !yield) {
                    yield = true;
                    continue;
                }

                int size = poolSize_;
                if (offered && requestBuffer.excessWaitingThreads() >= 0) { // PK77809
                    // Try to give to existing thread, check if there are threads for the
                    // request
                    return command;
                }

                // If cannot handoff and still under maximum, create new thread
                if (size < maximumPoolSize_) {
                    // Since we have already required the user of the ThreadPool to do the
                    // doPriv(..) when they called getThreadPool(...), it would be
                    // redundant of
                    // the runtime to require this on this method....

                    // PM16525 We need to use java.security.AccessController.doPriv
                    // instead
                    // of com.ibm.ws...doPriv to avoid leaking classloaders. When no
                    // SecurityManager exists (i.e. Java 2 security is disabled), the WAS
                    // doPriv will simply call the run() method of the PrivilegedAction to
                    // avoid the performance overhead of delegating to the JDK doPriv.
                    // However, this approach will end up "copying" ProtectionDomains that
                    // are held by the originating thread's accessControlContext to the
                    // new
                    // thread that may contain references to CompoundClassLoaders -- this
                    // happens because customer code (that was loaded by the CCL) was on
                    // the
                    // stack when the new thread was created.
                    // Using the JDK doPriv directly avoids this leak.
                    AccessController.doPrivileged(new PrivilegedAction()
                    { // d185137
                        public Object run()
                    {
                        addThread(null);
                        return null; // nothing to return
                    }
                    });
                }

                if (size >= maximumPoolSize_ && growasneeded) {
                    // Since we have already required the user of the ThreadPool to do the
                    // doPriv(..) when they called getThreadPool(...), it would be
                    // redundant of
                    // the runtime to require this on this method....

                    // PM16525 We need to use java.security.AccessController.doPriv
                    // instead
                    // of com.ibm.ws...doPriv to avoid leaking classloaders. When no
                    // SecurityManager exists (i.e. Java 2 security is disabled), the WAS
                    // doPriv will simply call the run() method of the PrivilegedAction to
                    // avoid the performance overhead of delegating to the JDK doPriv.
                    // However, this approach will end up "copying" ProtectionDomains that
                    // are held by the originating thread's accessControlContext to the
                    // new
                    // thread that may contain references to CompoundClassLoaders -- this
                    // happens because customer code (that was loaded by the CCL) was on
                    // the
                    // stack when the new thread was created.
                    // Using the JDK doPriv directly avoids this leak.
                    AccessController.doPrivileged(new PrivilegedAction()
                    { // d185137
                        public Object run()
                    {
                        addThread(null);
                        return null; // nothing to return
                    }
                    });
                    // PM13147: Log the fact that the thread pool has grown beyond its
                    // maximum size
                    if (!poolGrowthLogged) {
                        Tr.info(tc, "WSVR0630I", new Object[] { name, Integer.valueOf(poolSize_) });
                        poolGrowthLogged = true;
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "WSVR0630I", new Object[] { name, Integer.valueOf(poolSize_) });
                        }
                    }
                }

                if (offered) {
                    return command;
                }

                if (blockingMode == EXPAND_WHEN_QUEUE_IS_FULL_ERROR_AT_LIMIT || blockingMode == EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT) {
                    final int currentCapacity = requestBuffer.capacity();

                    if (currentCapacity != requestBufferExpansionLimit_) {
                        // There is space available to expand the buffer.
                        // The buffer's expanded by half its current
                        // capacity or, if this would make it too large, to
                        // its configured expansion limit.
                        final int additionalCapacity = Math.min(currentCapacity / 2, requestBufferExpansionLimit_ - currentCapacity);

                        Tr.uncondEvent(tc, "Exanding buffer of ThreadPool " + name + " by " + additionalCapacity);

                        requestBuffer.expand(additionalCapacity);

                        // Having expanded the buffer we re-dispatch the given command
                        continue;
                    }
                }
            }

            // F743-24122 - Output an info message the first time the request buffer
            // has been filled,
            // writing it only to trace thereafter
            if (!bufferLimitReached) {
                Tr.info(tc, "WSVR0629I", name);
                bufferLimitReached = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "WSVR0629I", name);
            }

            // begin D186668
            // The thread pool is at capacity and the queue is full.
            // Whether the request is queued (with the implied blocking
            // behavior) or we thrown an exception depends on the
            // requested mode:

            if (blockingMode == WAIT_WHEN_QUEUE_IS_FULL) {
                if (timeoutInMillis > 0) {
                    return (Runnable) requestBuffer.put(command, timeoutInMillis, requestBufferInitialCapacity_);
                } else {
                    requestBuffer.put(command);
                    offered = true;
                }
            } else if (blockingMode == EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT) {
                if (timeoutInMillis > 0) {
                    return (Runnable) requestBuffer.put(command, timeoutInMillis);
                } else {
                    requestBuffer.put(command);
                    offered = true;
                }
            } else {
                throw new ThreadPoolQueueIsFullException();
            }

            // end D186668
        }
    }

    // end D186668

    // F743-11444 - New method
    /**
     * Removes the command from the internal queue if it is present, thus causing
     * it
     * to not be run. A command may not be canceled if it has already been started
     * on a thread.
     * 
     * If the command exists in the internal queue multiple times, the one which
     * would be executed first will be removed.
     * 
     * @param command
     *            - the command to be canceled
     * 
     * @return true if the command was canceled
     */
    public boolean cancel(Runnable command) {
        return requestBuffer.cancel(command);
    }

    public void addThreadPoolListener(ThreadPoolListener tpl) {
        int size = threadPoolListeners.length + 1;
        ThreadPoolListener[] tpls = new ThreadPoolListener[size];
        System.arraycopy(threadPoolListeners, 0, tpls, 0, size - 1);
        tpls[size - 1] = tpl;
        threadPoolListeners = tpls;
    }

    public void removeThreadPoolListener(ThreadPoolListener tpl) {
        int idx = 0;

        while (idx < threadPoolListeners.length && threadPoolListeners[idx] != tpl) {
            ++idx;
        }

        if (idx < threadPoolListeners.length) {
            int size = threadPoolListeners.length - 1;
            ThreadPoolListener[] tpls = new ThreadPoolListener[size];
            System.arraycopy(threadPoolListeners, 0, tpls, 0, idx);
            System.arraycopy(threadPoolListeners, idx + 1, tpls, idx, size - idx - 1);
            threadPoolListeners = tpls;
        }
    }

    protected void fireThreadCreated(int poolSize) {
        for (int i = 0; i < threadPoolListeners.length; ++i)
            threadPoolListeners[i].threadCreated(this, poolSize);
    }

    protected void fireThreadStarted(int activeThreads, int maxThreads) {
        for (int i = 0; i < threadPoolListeners.length; ++i)
            threadPoolListeners[i].threadStarted(this, activeThreads, maxThreads);
    }

    protected void fireThreadReturned(int activeThreads, int maxThreads) {
        for (int i = 0; i < threadPoolListeners.length; ++i)
            threadPoolListeners[i].threadReturned(this, activeThreads, maxThreads);
    }

    protected void fireThreadDestroyed(int poolSize) {
        for (int i = 0; i < threadPoolListeners.length; ++i)
            threadPoolListeners[i].threadDestroyed(this, poolSize);
    }

    public synchronized boolean areRequestsOutstanding() {

        if (poolSize_ == 0) {
            // no pool...no requests
            return false;
        } else {
            // see if any requests are alive
            for (Iterator it = threads_.values().iterator(); it.hasNext();) {
                Thread t = (Thread) (it.next());

                if (t.isAlive()) {
                    return true;
                }
            }
        }

        return false;
    }

    // BEGIN LIDB3275

    /**
     * Registers a MonitorPlugin with this thread pool. Only
     * one plugin is supported in the current implementation.
     * 
     * @throws TooManyListenersException
     *             if a different
     *             MonitorPlugin had already been registered with
     *             this ThreadPool.
     */

    public void setMonitorPlugin(MonitorPlugin plugin) throws TooManyListenersException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setMonitorPlugin", plugin);
        }

        if (this.monitorPlugin != null && !this.monitorPlugin.equals(plugin)) {
            throw new TooManyListenersException("INTERNAL ERROR: ThreadPool.MonitorPlugin already set.");
        }
        this.monitorPlugin = plugin;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setMonitorPlugin", plugin);
        }
    }

    /**
     * Checks all active threads in this thread pool to determine if
     * they are hung. The definition of hung is provided by the
     * MonitorPlugin.
     * 
     * @see MonitorPlugin
     */
    public void checkAllThreads() {

        // if nothing is plugged in, exit early
        if (this.monitorPlugin == null) {
            return;
        }

        long currentTime = 0; // lazily initialize current time

        try {
            for (Iterator i = this.threads_.values().iterator(); i.hasNext();) {

                Worker thread = (Worker) i.next();
                synchronized (thread) { // d204471
                    if (thread.getStartTime() > 0) {
                        if (currentTime == 0) {
                            currentTime = System.currentTimeMillis();
                        }

                        if (!thread.isHung && this.monitorPlugin.checkThread(thread, thread.threadNumber, currentTime - thread.getStartTime())) { // PK25446
                            thread.isHung = true;
                        }
                    }
                }
            }

            this.lastThreadCheckDidntComplete = false; // D222794
        } catch (ConcurrentModificationException e) { // begin D222794
            // NOTE: we can pretty much ignore this exception ... if
            // we occasionally fail on the check, we'll be OK. So
            // we simply keep track with a flag. If two consecutive
            // checks fail, then we'll log to FFDC
            if (this.lastThreadCheckDidntComplete) {
                // Alex Ffdc.log(e, this, this.getClass().getName(), "1181", this); //
                // D477704.2
            }
            this.lastThreadCheckDidntComplete = true;
        } // end D222794
    }

    // END LIDB3275

    /**
     * Sets the upper limit of the request buffer's expansion.
     * This limit is only used when requests are dispatched
     * in a mode that allows the buffer to expand. The initial
     * value for this limit is ten times the pool's maximum
     * size.
     * <p>
     * There are no guarantees in behavior should this limit be set to a value that is less than the buffer's size.
     * 
     * @see #EXPAND_WHEN_QUEUE_IS_FULL_ERROR_AT_LIMIT
     * @see #EXPAND_WHEN_QUEUE_IS_FULL_WAIT_AT_LIMIT
     * 
     * @see #getRequestBufferSize(int)
     * 
     * 
     */
    public void setRequestBufferExpansionLimit(int limit) {
        requestBufferExpansionLimit_ = limit;
    }

    public void setThreadWaiting(boolean isWaiting) {
        try {
            // begin 379236
            Worker worker = (Worker) Thread.currentThread();
            if (isWaiting) {
                worker.disableHungThreadDetection();
                worker.clearThreadLocals();
            } else {
                worker.enableHungThreadDetection();
            }
            // end 379236
        } catch (ClassCastException cce) {
            // Alex Ffdc.log(cce, this, "com.ibm.ws.util.ThreadPool.setThreadWaiting",
            // "1300", this); // D477704.2

            // A non-Worker thread has called setThreadWaiting. Non-Worker threads are
            // not subject to hang detection anyway so we just FFDC the exception and
            // carry on.
        }
    }

    // begin @LIDB3275.1A
    private synchronized void incActive() {
        ++activeThreads;
    }

    private synchronized void decActive() {
        --activeThreads;
    }

    private synchronized void decPoolSize() {
        --poolSize_;
    }

    // End @LIDB3275.1A

    static Object[] ZERO_OBJECTS = new Object[0];

    class Worker extends Thread implements WorkerThread, WasThreadLocalSupport { // D189357.2, LIDB2255

        protected Runnable firstTask_;

        Object[] wsThreadLocals = ZERO_OBJECTS;

        private volatile long startTime = 0; // LIDB3275

        protected volatile boolean isHung = false; // LIDB3275,@LIDB3275.1C

        protected String threadNumber = "-1"; // LIDB3275, d212112,@LIDB3275.1C

        // begin LIDB2255
        //
        // The worker thread maintains an array containing
        // clearable thread local objects. This array will
        // be cleared after every dispatch. Note that for
        // performance reasons, we do not rely on the
        // array size to be the actual size (the array grows
        // monotonically to hold enough data for any given
        // dispatch.
        //
        // See also com.ibm.websphere.spi.WasThreadLocal
        //

        WasThreadLocalData[] activeWasThreadLocals = new WasThreadLocalData[0];

        int numberOfActiveWasThreadLocals = 0;

        // end LIDB2255

        /**
         * d209497 - new method
         * Because the jvm implementation of volatile cannot guarantee atomic
         * access to the volatile long field we must synchronize access to the
         * attribute.
         * Consequently, we rely on the fact that the jvm can perform lightweight
         * synchronization in light contention code paths...
         **/
        protected synchronized long getStartTime() { // d209497,@LIDB3275.1C
            return startTime;
        }

        /**
         * d209497 - new method
         * Because the jvm implementation of volatile cannot guarantee atomic
         * access to the volatile long field we must synchronize access to the
         * attribute.
         * Consequently, we rely on the fact that the jvm can perform lightweight
         * synchronization in light contention code paths...
         **/
        protected synchronized void setStartTime(long timeValue) { // d209497,@LIDB3275.1C
            startTime = timeValue;
        }

        // begin @LIDB3275.1A
        protected Worker(ThreadGroup tg, Runnable r, String n, long ss) {
            super(tg, r, n, ss);
        }

        // end @LIDB3275.1A

        protected Worker(Runnable firstTask, int id) {
            super(name + " : " + id);
            firstTask_ = firstTask;
            setPriority(getThreadPriority());
            setDaemon(true);
        }

        @Override
        public void run() {
            // LIDB3275 NOTE: method simplified and reformatted
            this.threadNumber = RasHelper.getThreadId(); // d212112
            Runnable task = firstTask_;
            boolean oldThread = (task == null) ? false : true; // PK77809
            try {

                firstTask_ = null; // enable GC

                do {

                    if (task != null) {
                        threadStarted(); // 379236
                        try {
                            task.run();
                        } finally {
                            threadReturned(); // 379236
                        }
                    }

                    task = getTask(oldThread); // PK77809
                    // PK77809 once a thread has retrieved a single task, it is "old".
                    // This is to keep refCounting of waitingThreads correct.
                    oldThread = true;
                } while (task != null);

            } catch (InterruptedException ex) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "InterruptedException Caught", new Object[] { Integer.valueOf(activeThreads) }); // PK27301
            } finally {
                // fall through
                Thread.interrupted(); // PK27301
                workerDone(this, (task != null));
            }
        }

        // begin 379236
        public void threadStarted() {
            fireThreadStarted(activeThreads, maximumPoolSize_);
            enableHungThreadDetection();
        }

        public void threadReturned() {
            disableHungThreadDetection();
            fireThreadReturned(activeThreads, maximumPoolSize_);
            clearThreadLocals();
        }

        public void disableHangDetectionForCurrentDispatch() {
            setStartTime(0);
        }

        protected void enableHungThreadDetection() {
            // Note that this is done only when a monitor is plugged into the
            // thread pool so that performance is not adversely affected when
            // monitoring is completely disabled.
            if (monitorPlugin != null) {
                // Denote the time at which work was dispatched on this
                // thread.
                setStartTime(System.currentTimeMillis());
            }
        }

        protected void disableHungThreadDetection() {
            // Note that this is done only when a monitor is plugged into the
            // thread pool so that performance is not adversely affected when
            // monitoring is completely disabled.
            if (monitorPlugin != null) {
                synchronized (this) {
                    if (isHung) {
                        monitorPlugin.clearThread(this, this.threadNumber, System.currentTimeMillis() - getStartTime()); // d209497
                        isHung = false;
                    }

                    setStartTime(0); // d209497
                }
            }
        }

        protected void clearThreadLocals() {
            // begin LIDB2255-58

            // If there are WasThreadLocal objects to be cleared,
            // then do so:
            if (numberOfActiveWasThreadLocals > 0) {
                for (int i = 0; i < numberOfActiveWasThreadLocals; i++) {
                    activeWasThreadLocals[i].key = null;
                    activeWasThreadLocals[i].value = null;
                }
                numberOfActiveWasThreadLocals = 0;
            }

            // If this thread pool is configured to clear java.lang.ThreadLocal's
            // then do so:
            if (clearJavaLangThreadLocals) {
                PlatformAdapterAccessor.getInstance().cleanThreadLocals(this); // D652960
            }
            // end LIDB2255-58
        }

        // end 379236

        // begin LIDB2255

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.ibm.ws.util.ThreadPool.WasThreadLocalSupport#get(java.lang.Object)
         */
        public Object get(Object key) {

            if (key == null) {
                return null;
            }

            // Walk the array to find a matching key. Note that is
            // is linear search time. We are OK with that ... this
            // algorithm is optimized to save space, not time.
            for (int i = 0; i < numberOfActiveWasThreadLocals; i++) {
                if (key.equals(activeWasThreadLocals[i].key)) {
                    return activeWasThreadLocals[i].value;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.ibm.ws.util.ThreadPool.WasThreadLocalSupport#set(java.lang.Object,
         * java.lang.Object)
         */
        public void set(Object key, Object value) {

            if (key == null) {
                return;
            }
            // Walk the array to find a matching key. Note that is
            // is linear search time. We are OK with that ... this
            // algorithm is optimized to save space, not time.

            for (int i = 0; i < numberOfActiveWasThreadLocals; i++) {
                if (key.equals(activeWasThreadLocals[i].key)) {
                    activeWasThreadLocals[i].value = value;
                    return;
                }
            }

            // Nothing was found.

            // Resize the array if we need to:
            if (numberOfActiveWasThreadLocals == activeWasThreadLocals.length) {
                WasThreadLocalData[] newArray = new WasThreadLocalData[numberOfActiveWasThreadLocals + 1];
                System.arraycopy(activeWasThreadLocals, 0, newArray, 0, numberOfActiveWasThreadLocals);
                newArray[numberOfActiveWasThreadLocals] = new WasThreadLocalData();
                activeWasThreadLocals = newArray;
            }

            // Add the key/value pair:
            activeWasThreadLocals[numberOfActiveWasThreadLocals].key = key;
            activeWasThreadLocals[numberOfActiveWasThreadLocals].value = value;
            numberOfActiveWasThreadLocals++;

        }

        // end LIDB2255

        @Override
        // D658409
        public void setContextClassLoader(ClassLoader newClassLoader) {
            if (ThreadContextAccessor.PRINT_STACK_ON_SET_CTX_CLASSLOADER) {
                String msg = "Stack trace - new ContextClassLoader: " + newClassLoader.toString();
                Throwable t = new Throwable(msg);
                t.printStackTrace(System.out);
            }
            super.setContextClassLoader(newClassLoader);
        }
    }

    // begin @LIDB3275.1A
    /**
     * This is a z/OS only worker thread. It can not and should not
     * be created on any other platform that WebSphere runs on.
     */
    /*
     * Alex class ZOSWorker extends Worker implements WorkerZOSThread {
     * 
     * private int internalWorkThread = 0; // Flag to tell C++ code what type of
     * thread
     * 
     * private int useWLM = 0; // Flag to tell C++ code to use WLM Select to get
     * work.
     * 
     * // This in reality a CommonBridge Object that has methods that we need to
     * call. These
     * // methods use to be called from C++ when we attached z/OS Application
     * threads to the
     * // JVM. Since we are already in JAVA when the thread is created now I
     * decided to save
     * // the JNI call/return for these threads by calling the method when the
     * thread first
     * // gets started and when the thread is exiting.
     * private ZThreadUtilities utils = null;
     * 
     * /**
     * This constructor is call from addThread in the ThreadPool outter class.
     */
    /*
     * public ZOSWorker(int internalWorkThread, int useWLM, long stackSize,
     * ZThreadUtilities utils) {
     * // the thread name passed to the constructor can resolve to one of two
     * things:
     * // - "WebSphere WLM Dispatch Thread t=" for worker threads that use WLM
     * queueing
     * // - "WebSphere non-WLM Dispatch Thread t=" for worker threads that use
     * internal queueing
     * //
     * // Note: The TCB number is appended to the name later, after the thread
     * gets launched.
     * super((ThreadGroup) null,
     * (Runnable) null,
     * "WebSphere " +
     * ((useWLM == 1) ? "WLM " : "non-WLM ") +
     * "Dispatch Thread " +
     * "t=",
     * stackSize); //@660363C
     * if (isZOS) {
     * this.internalWorkThread = internalWorkThread;
     * this.useWLM = useWLM;
     * this.utils = utils;
     * setPriority(getThreadPriority());
     * setDaemon(true);
     * } else {
     * throw new
     * UnsupportedOperationException("Method not supported on this platform");
     * }
     * }
     * 
     * /**
     */
    /*
     * public void run() {
     * this.threadNumber = RasHelper.getThreadId();
     * //--------------------------------------------------------------
     * // Once runApplicationThread is called it does not return till
     * // thread exit happens in native C++ code. All of the waiting
     * // and dispatching of work happens in native C++ code for this
     * // class.
     * //--------------------------------------------------------------
     * try {
     * utils.runApplicationThread(internalWorkThread, useWLM);
     * } catch (Throwable t) {
     * Ffdc.log(t, this, this.getClass().getName(), "1567", this); // D477704.2
     * Tr.uncondEvent(tc, "An error occured on a thread in Threadpool: " + name,
     * t);
     * } finally {
     * //--------------------------------------------------------------
     * // Need to do this here because calling workerDone saying the task
     * // died will decrement activeThreads and it shouldn't for z/OS.
     * // The active thread count was decremented when threadReturned was
     * // called.
     * //--------------------------------------------------------------
     * decPoolSize();
     * workerDone(this, false);
     * }
     * }
     * 
     * /**
     * This method is call from CommonBridge.threadStarted. It does a lot of the
     * things
     * that are done in the other platform's thread run method. HAd to do it this
     * way because
     * the z/OS wait and dispatch work loop is not done in Java
     */
    /*
     * public synchronized void threadStarted() {
     * incActive();
     * super.threadStarted(); // 379236
     * }
     * 
     * /**
     * This method is call from CommonBridge.threadReturned. It does a lot of the
     * things
     * that are done in the other platform's thread run method. Had to do it this
     * way because
     * the z/OS wait and dispatch work loop is not done in Java
     */
    /*
     * public void threadReturned() {
     * super.threadReturned(); // 379236
     * decActive();
     * }
     * }
     */
    // end @LIDB3275.1A

    // begin LIDB2255
    static private class WasThreadLocalData {
        Object key = null;

        Object value = null;
    }

    // end LIDB2255

    /**
     * This is a z/OS CR only worker thread. When the thread is run it calls
     * 
     * Added in defect 263391/331761
     */

    class DecoratedZOSWorker extends Worker implements DecoratedCRThread // 331761A
    {
        protected DecoratedZOSWorker(Runnable firstTask, int id) {
            super(firstTask, id);
        }

        /**
         * Calls xMem component to decorate the thread.
         */
        @Override
        public void run() {
            this.threadNumber = RasHelper.getThreadId();

            try {
                xMemSetupThread.invoke(null, new Object[] { this });
            } catch (Throwable t) {
                // Alex Ffdc.log(t, this,
                // "com.ibm.ws.util.ThreadPool.DecoratedZOSWorker.run", "1770", this);
                // // D477704.2
            }
        }

        /**
         * This method is called from the xMem component once the thread has been
         * decorated.
         * It is functionally equivalent to the run method on the Worker class.
         */
        public void processTasks() {
            super.run();
        }

    } // End DecoratedZOSWorker class
}

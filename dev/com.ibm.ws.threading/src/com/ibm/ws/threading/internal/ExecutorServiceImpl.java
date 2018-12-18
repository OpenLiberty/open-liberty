/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.threading.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.CpuInfo;
import com.ibm.ws.threading.ThreadQuiesce;
import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;
import com.ibm.wsspi.threading.WSExecutorService;

/**
 * Component implementation for the threading component.
 */
@Component(name = "com.ibm.ws.threading",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM",
           service = { java.util.concurrent.ExecutorService.class, com.ibm.wsspi.threading.WSExecutorService.class })
public final class ExecutorServiceImpl implements WSExecutorService, ThreadQuiesce {

    /**
     * The target ExecutorService.
     */
    ThreadPoolExecutor threadPool = null;

    /**
     * The controller that (when active) can monitor the throughput of
     * the underlying thread pool and adjust its size in an attempt to
     * maximize throughput.
     */
    ThreadPoolController threadPoolController = null;

    /**
     * The thread pool name.
     */
    String poolName = null;

    /**
     * The most recently provided component config for the executor.
     */
    Map<String, Object> componentConfig = null;

    /**
     * Indicates whether any interceptors are currently being used. This is for performance
     * reasons, to avoid getting an iterator over an empty set for every task that is submitted.
     */
    boolean interceptorsActive = false;

    /**
     * A Set of interceptors that are all given a chance to wrap tasks that are submitted
     * to the executor for execution.
     */
    Set<ExecutorServiceTaskInterceptor> interceptors = new CopyOnWriteArraySet<ExecutorServiceTaskInterceptor>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setInterceptor(ExecutorServiceTaskInterceptor interceptor) {
        interceptors.add(interceptor);
        interceptorsActive = true;
    }

    protected synchronized void unsetInterceptor(ExecutorServiceTaskInterceptor interceptor) {
        interceptors.remove(interceptor);
        if (interceptors.size() == 0) {
            interceptorsActive = false;
        }
    }

    /**
     * The ThreadFactory used by the executor to create new threads.
     */
    ThreadFactory threadFactory = null;

    private Boolean serverStopping = false;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               target = "(com.ibm.ws.threading.defaultExecutorThreadFactory=true)")
    protected void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        createExecutor();
    }

    protected void unsetThreadFactory(ThreadFactory threadFactory) {
        if (this.threadFactory == threadFactory) {
            threadFactory = null;
            createExecutor();
        }
    }

    /**
     * Activate this executor service component.
     */
    @Activate
    protected void activate(Map<String, Object> componentConfig) {
        this.componentConfig = componentConfig;
        createExecutor();
    }

    /**
     * Modify this executor's configuration.
     */
    @Modified
    protected void modified(Map<String, Object> componentConfig) {
        this.componentConfig = componentConfig;
        createExecutor();
    }

    /**
     * Deactivate this executor component.
     */
    @Deactivate
    protected void deactivate(int reason) {
        threadPoolController.deactivate();

        // Shutdown the thread pool and let users finish using it
        softShutdown(threadPool);

        componentConfig = null;
    }

    /**
     * Get a reference to the underlying thread pool.
     */
    @Trivial
    ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    /**
     * Create a thread pool executor with the configured attributes from this
     * component config.
     */
    private synchronized void createExecutor() {
        if (componentConfig == null) {
            // this is a completely normal occurrence and can happen if a ThreadFactory is bound prior to
            // component activation...  the proper thing to do is to do nothing and wait for activation
            return;
        }

        if (threadPoolController != null)
            threadPoolController.deactivate();

        ThreadPoolExecutor oldPool = threadPool;

        poolName = (String) componentConfig.get("name");
        String threadGroupName = poolName + " Thread Group";

        int coreThreads = Integer.parseInt(String.valueOf(componentConfig.get("coreThreads")));
        int maxThreads = Integer.parseInt(String.valueOf(componentConfig.get("maxThreads")));

        if (maxThreads <= 0) {
            maxThreads = Integer.MAX_VALUE;
        }

        if (coreThreads < 0) {
            coreThreads = 2 * CpuInfo.getAvailableProcessors();
        }

        // If coreThreads is greater than maxThreads, automatically lower it and proceed
        coreThreads = Math.min(coreThreads, maxThreads);

        BlockingQueue<Runnable> workQueue = new BoundedBuffer<Runnable>(java.lang.Runnable.class, 1000, 1000);

        RejectedExecutionHandler rejectedExecutionHandler = new ExpandPolicy(workQueue, this);

        threadPool = new ThreadPoolExecutor(coreThreads, maxThreads, 0, TimeUnit.MILLISECONDS, workQueue, threadFactory != null ? threadFactory : new ThreadFactoryImpl(poolName, threadGroupName), rejectedExecutionHandler);

        threadPoolController = new ThreadPoolController(this, threadPool);

        if (oldPool != null) {
            softShutdown(oldPool);
        }
    }

    private class RunnableWrapper implements Runnable {

        private final Runnable wrappedTask;

        RunnableWrapper(Runnable r) {
            this.wrappedTask = r;

            phaser.register();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                this.wrappedTask.run();
            } finally {

                phaser.arriveAndDeregister();
            }
        }

    }

    // Used to keep track of the number of threads that are not finished
    protected final Phaser phaser = new Phaser(1);

    private class CallableWrapper<T> implements Callable<T> {
        private final Callable<T> callable;

        CallableWrapper(Callable<T> c) {
            this.callable = c;
            phaser.register();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public T call() throws Exception {
            try {
                return this.callable.call();
            } finally {
                phaser.arriveAndDeregister();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        threadPoolController.resumeIfPaused();
        return threadPool.awaitTermination(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        threadPoolController.resumeIfPaused();
        return threadPool.invokeAll(interceptorsActive ? wrap(tasks) : tasks);
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        threadPoolController.resumeIfPaused();
        return threadPool.invokeAll(interceptorsActive ? wrap(tasks) : tasks, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        threadPoolController.resumeIfPaused();
        return threadPool.invokeAny(interceptorsActive ? wrap(tasks) : tasks);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        threadPoolController.resumeIfPaused();
        return threadPool.invokeAny(interceptorsActive ? wrap(tasks) : tasks, timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdown() {
        return threadPool.isShutdown();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTerminated() {
        return threadPool.isTerminated();
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        threadPoolController.resumeIfPaused();
        return threadPool.submit(createWrappedCallable(task));
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> submit(Runnable task) {
        threadPoolController.resumeIfPaused();
        return threadPool.submit(createWrappedRunnable(task));
    }

    /** {@inheritDoc} */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        threadPoolController.resumeIfPaused();
        return threadPool.submit(createWrappedRunnable(task), result);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(Runnable command) {
        threadPoolController.resumeIfPaused();
        threadPool.execute(createWrappedRunnable(command));
    }

    /** {@inheritDoc} */
    @Override
    public void executeGlobal(Runnable command) {
        threadPoolController.resumeIfPaused();
        threadPool.execute(createWrappedRunnable(command));
    }

    /**
     * For internal use only. Invoker is responsible for ensuring that the interceptors are applied
     * to the underlying task that the proxy eventually delegates to. This allows the proxy Runnable
     * to be offered directly to the BlockingQueue, which can then identify information about it,
     * such as whether it ought to be expedited instead of inserted at the tail of the queue.
     *
     * @param proxy
     */
    void executeWithoutInterceptors(Runnable proxy) {
        threadPoolController.resumeIfPaused();
        threadPool.execute(proxy);
    }

    @Trivial
    public int getPoolSize() {
        return threadPool.getPoolSize();
    }

    @Trivial
    public int getActiveCount() {
        return threadPool.getActiveCount();
    }

    @Trivial
    public String getPoolName() {
        return poolName;
    }

    /**
     * Shutdown a thread pool while still allowing current users to submit new work to it. The standard
     * ThreadPoolExecutor.shutdown() method causes any new work to get rejected while the pool is shutting
     * down. A soft shutdown, on the other hand, will still allow new work to get submitted while the pool
     * is shutting down. The thread pool will stay alive until no more references to it exist and all work
     * in the queue has been processed.
     *
     * @param threadPool the ThreadPoolExecutor to shutdown
     */
    private void softShutdown(final ThreadPoolExecutor oldThreadPool) {
        // setting keepAlive and coreThreads to 0 ensures that idle threads go away immediately; the
        // thread pool will be eligible for garbage collection when all threads are gone and no other
        // code has a reference to it
        oldThreadPool.setKeepAliveTime(0, TimeUnit.SECONDS);
        oldThreadPool.setCorePoolSize(0);
    }

    /**
     * A handler for rejected tasks that throws a {@code RejectedExecutionException}.
     */
    public static class ExpandPolicy implements RejectedExecutionHandler {

        public BoundedBuffer<Runnable> workQueue;
        public WSExecutorService exService;

        /**
         * Creates an {@code ExpandPolicy}.
         */
        public ExpandPolicy(BlockingQueue<Runnable> workQueue2, WSExecutorService exService) {
            this.workQueue = (BoundedBuffer<Runnable>) workQueue2;
            this.exService = exService;
        }

        /**
         * Expand the work queue
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always.
         */
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (e.isShutdown()) {
                throw new RejectedExecutionException("Task " + r.toString() +
                                                     " rejected from " +
                                                     e.toString());
            } else {
                if (r instanceof QueueItem && ((QueueItem) r).isExpedited())
                    workQueue.expandExpedited(1000);
                else
                    workQueue.expand(1000);

                //Resubmit rejected task
                exService.execute(r);
            }
        }
    }

    private Runnable createWrappedRunnable(Runnable in) {
        Runnable r = interceptorsActive ? wrap(in) : in;
        if (serverStopping)
            return r;

        return new RunnableWrapper(r);
    }

    Runnable wrap(Runnable r) {
        Iterator<ExecutorServiceTaskInterceptor> i = interceptors.iterator();
        while (i.hasNext()) {
            r = i.next().wrap(r);
        }

        return r;
    }

    private <T> Callable<T> createWrappedCallable(Callable<T> in) {

        Callable<T> c = interceptorsActive ? wrap(in) : in;
        if (serverStopping)
            return c;
        return new CallableWrapper<T>(c);
    }

    <T> Callable<T> wrap(Callable<T> c) {
        Iterator<ExecutorServiceTaskInterceptor> i = interceptors.iterator();
        while (i.hasNext()) {
            c = i.next().wrap(c);
        }

        return c;
    }

    // This is private, so handling both interceptors and wrapping in this method for simplicity
    private <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> wrappedTasks = new ArrayList<Callable<T>>();
        Iterator<? extends Callable<T>> i = tasks.iterator();
        while (i.hasNext()) {
            Callable<T> c = wrap(i.next());
            if (serverStopping)
                wrappedTasks.add(c);
            else
                wrappedTasks.add(new CallableWrapper<T>(c));
        }
        return wrappedTasks;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.threading.ThreadQuiesce#quiesceThreads()
     */
    @Override
    @FFDCIgnore(TimeoutException.class)
    public boolean quiesceThreads() {
        this.serverStopping = true;

        try {
            // Wait 30 seconds for all pre-quiesce work to complete
            phaser.arriveAndDeregister();
            phaser.awaitAdvanceInterruptibly(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //FFDC and fail quiesce notification
            return false;
        } catch (TimeoutException e) {
            // If we time out, quiesce has failed. This is normal, so no FFDC.
            return false;
        }

        return true;
    }

    @Override
    public int getActiveThreads() {
        int count = phaser.getUnarrivedParties();
        if (this.serverStopping)
            return count;

        return count - 1;
    }

    @Override
    public boolean quiesceStarted() {
        return this.serverStopping;
    }
}

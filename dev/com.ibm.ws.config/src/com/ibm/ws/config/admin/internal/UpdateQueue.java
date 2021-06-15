/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 *
 */
class UpdateQueue<T> {
    private static final TraceComponent tc = Tr.register(UpdateQueue.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private static class Queue<T> implements Runnable {
        private final LinkedList<Runnable> runOrder = new LinkedList<Runnable>();
        private boolean hasProcessor = false;

        public synchronized boolean enqueue(T key, Runnable runnable) {
            runOrder.add(runnable);
            if (hasProcessor) {
                return false;
            } else {
                hasProcessor = true;
                return true;
            }
        }

        public synchronized Runnable dequeue() {
            Runnable runner = runOrder.poll();
            if (runner == null) {
                hasProcessor = false;
            }
            return runner;
        }

        @FFDCIgnore(Throwable.class)
        @Override
        public void run() {
            Runnable runnable = null;
            while ((runnable = dequeue()) != null) {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    // ignore
                }
            }
        }

    }

    private final ScheduledThreadPoolExecutor threadPool;
    private final Queue<T>[] queues;

    public UpdateQueue() {
        // Customize the thread factory used by the ConfigAdmin service:
        // Give configuration threads a meaningful name, and
        // create the threads as daemon threads so they don't
        // prevent the server from shutting down.

        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                    @Override
                    public Thread run() {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        t.setName("Config-" + t.getName());
                        return t;
                    }
                });
            }
        };

        // this.thPool = Executors.newCachedThreadPool(threadFactory);

        // START: D84795: Do not throw an exception from the rejected execution handler.
        //                Instead, display logging information and continue processing.

        // Replace the default rejected execution handler -- an instance of
        // ThreadpoolExecutor$AbortPolicy -- with a custom rejected execution handler.
        // AbortPolicy always handled rejected execution by throwing a
        // RejectedExecutionException.  Instead, display information about the rejected
        // runnable then proceed.  Do not throw an exception.
        //
        // This is a very limited way to handle the problem, but better handling requires
        // more information about the particular failures which are occurring.
        //
        // If failures are occurring because the thread pool is terminating (or terminated),
        // that argues for a check before adding runnables to the pool.
        //
        // If failures are occurring because the thread pool is becoming saturated,
        // that argues for adjusting the thread pool size, or for getting better
        // information about how so many tasks are being generated.

        RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                String executorText;

                if (e.isTerminating()) {
                    executorText = "Terminating executor [ " + e + " ] rejected [ " + r + " ]";

                } else if (e.isTerminated()) {
                    executorText = "Terminated executor [ " + e + " ] rejected [ " + r + " ]";

                } else {
                    long coreSize = e.getCorePoolSize();
                    long maxSize = e.getMaximumPoolSize();
                    long active = e.getActiveCount();
                    long completed = e.getCompletedTaskCount();

                    executorText = "Running executor [ " + e + " ] rejected [ " + r + " ]" +
                                   ": State: Core Size [ " + coreSize + " ] Max Size [ " + maxSize + " ]" +
                                   " Active Count [ " + active + " ] Completed Count [ " + completed + " ]";
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, executorText);
                }
            }
        };

        /**
         * subclass the ThreadPoolExecutor to provide some debug info.
         * Pass in the new RejectedExecutionHandler defined above.
         */
        int maxSize = CpuInfo.getAvailableProcessors().get();
        threadPool = new ScheduledThreadPoolExecutor(0, threadFactory, rejectionHandler);
        threadPool.setMaximumPoolSize(maxSize);
        threadPool.setKeepAliveTime(60, TimeUnit.SECONDS);
        // STOP: D84795

        @SuppressWarnings("unchecked")
        Queue<T>[] newQueues = new Queue[maxSize];
        queues = newQueues;
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new Queue<T>();
        }
    }

    public Future<?> add(T key, Runnable runnable) {
        Queue<T> queue = getQueue(key);
        FutureTask<?> future = new FutureTask<Void>(runnable, null);
        boolean startQueueProcessor = queue.enqueue(key, future);
        // start a processing task if there isn't one already
        if (startQueueProcessor) {
            threadPool.execute(queue);
        }
        return future;
    }

    Future<?> addScheduled(Runnable command) {
        return threadPool.schedule(command, 30, TimeUnit.SECONDS);
    }

    /**
     * Wait for all futures to finish within a specified time.
     *
     * @return true if all futures finished within a specified time, false otherwise.
     */
    @FFDCIgnore({ InterruptedException.class, ExecutionException.class, TimeoutException.class })
    public static boolean waitForAll(Collection<Future<?>> futureList, long timeout, TimeUnit timeUnit) {
        long timeoutNanos = timeUnit.toNanos(timeout);
        for (Future<?> future : futureList) {
            if (future == null || future.isDone()) {
                continue;
            }
            if (timeoutNanos <= 0) {
                return false;
            }
            long startTime = System.nanoTime();
            try {
                future.get(timeoutNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            } catch (TimeoutException e) {
                return false;
            }
            long endTime = System.nanoTime();
            timeoutNanos -= (endTime - startTime);
        }
        return true;
    }

    private Queue<T> getQueue(T key) {
        int hash = key.hashCode() & 0x7FFFFFFF;
        return queues[hash % queues.length];
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}

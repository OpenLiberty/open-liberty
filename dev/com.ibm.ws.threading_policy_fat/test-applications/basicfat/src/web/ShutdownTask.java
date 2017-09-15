/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that decrements a CountDownLatch when it runs and then awaits a second latch before shutting down an executor.
 * This allows the invoker to keep the task active and control when it completes.
 */
public class ShutdownTask implements Callable<List<Runnable>> {
    private final long awaitContinueNanos;
    private final CountDownLatch beginLatch;
    private final CountDownLatch continueLatch;
    final LinkedBlockingQueue<Thread> executionThreads = new LinkedBlockingQueue<Thread>();
    private final ExecutorService executor;
    private final boolean shutdownNow;

    public ShutdownTask(ExecutorService executor, boolean shutdownNow, CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.executor = executor;
        this.shutdownNow = shutdownNow;
    }

    @Override
    public List<Runnable> call() throws TimeoutException {
        List<Runnable> canceledQueuedTasks = null;
        System.out.println("> call " + toString());
        executionThreads.add(Thread.currentThread());
        beginLatch.countDown();
        try {
            try {
                if (!continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS))
                    throw new TimeoutException();
            } catch (InterruptedException x) {
                System.out.println("interrupted " + x);
                // Let shutdown/shutdownNow handle the interrupt
                Thread.currentThread().interrupt();
            }
            if (shutdownNow)
                canceledQueuedTasks = executor.shutdownNow();
            else
                executor.shutdown();
            System.out.println("< call " + toString() + " " + canceledQueuedTasks);
            return canceledQueuedTasks;
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } finally {
            executionThreads.remove(Thread.currentThread());
        }
    }
}

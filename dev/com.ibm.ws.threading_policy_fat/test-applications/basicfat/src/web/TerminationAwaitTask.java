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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that awaits termination of an executor.
 */
public class TerminationAwaitTask implements Callable<Boolean> {
    private long awaitContinueNanos;
    private CountDownLatch beginLatch;
    private CountDownLatch continueLatch;
    BlockingQueue<Throwable> errorOnAwait = new LinkedBlockingQueue<Throwable>();
    private final ExecutorService executor;
    private final long timeout;

    public TerminationAwaitTask(ExecutorService executor, long nanos) {
        this.executor = executor;
        this.timeout = nanos;
    }

    public TerminationAwaitTask(ExecutorService executor, long nanos, CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.executor = executor;
        this.timeout = nanos;
    }

    @Override
    public Boolean call() throws InterruptedException, TimeoutException {
        System.out.println("> call " + toString());
        if (beginLatch != null)
            beginLatch.countDown();
        try {
            if (continueLatch != null && !continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS))
                throw new TimeoutException();

            boolean terminated = executor.awaitTermination(timeout, TimeUnit.NANOSECONDS);
            System.out.println("< call " + toString() + " " + terminated);
            return terminated;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            errorOnAwait.add(x);
            throw x;
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            errorOnAwait.add(x);
            throw x;
        }
    }
}

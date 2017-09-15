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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Task that decrements a CountDownLatch when it runs and then awaits a second latch before completing.
 * This allows the invoker to keep the task active and control when it completes.
 */
public class CountDownTask implements Callable<Boolean> {
    private final long awaitContinueNanos;
    private final CountDownLatch beginLatch;
    private final CountDownLatch continueLatch;
    final LinkedBlockingQueue<Thread> executionThreads = new LinkedBlockingQueue<Thread>();

    public CountDownTask(CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
    }

    @Override
    public Boolean call() throws InterruptedException {
        System.out.println("> call " + toString());
        executionThreads.add(Thread.currentThread());
        beginLatch.countDown();
        try {
            boolean awaited = continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS);
            System.out.println("< call " + toString() + " " + awaited);
            return awaited;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } finally {
            executionThreads.remove(Thread.currentThread());
        }
    }
}

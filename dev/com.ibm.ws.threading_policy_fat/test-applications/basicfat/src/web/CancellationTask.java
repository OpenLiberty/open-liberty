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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task that decrements a CountDownLatch when it runs and then awaits a second latch before canceling a Future.
 * This allows the invoker to keep the task active and control when the cancellation happens, for example,
 * to encourage multiple threads to all attempt cancel at the same time.
 */
public class CancellationTask implements Callable<Boolean> {
    private final long awaitContinueNanos;
    private final CountDownLatch beginLatch;
    private final CountDownLatch continueLatch;
    private final Future<?> future;
    private final boolean interruptIfRunning;

    public CancellationTask(Future<?> future, boolean interruptIfRunning, CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.future = future;
        this.interruptIfRunning = interruptIfRunning;
    }

    @Override
    public Boolean call() throws InterruptedException, TimeoutException {
        System.out.println("> call " + toString());
        beginLatch.countDown();
        try {
            if (!continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS))
                throw new TimeoutException();
            boolean canceled = future.cancel(interruptIfRunning);
            System.out.println("< call " + toString() + " " + canceled);
            return canceled;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }
}

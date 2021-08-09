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

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyTaskCallback;

/**
 * Task that submits another task.
 * This is useful when we are expecting the task submission to be blocked for a period of time,
 * such as when the queue is full and a maxWaitForEnqueue is configured or the queue full action is
 * one of the caller runs options.
 *
 * @param <T>
 */
public class SubmitterTask<T> implements Callable<Future<T>> {
    private long awaitContinueNanos;
    private CountDownLatch beginLatch;
    private CountDownLatch continueLatch;
    private final PolicyExecutor executor;
    private final Callable<?> callable;
    private final PolicyTaskCallback callback;

    public SubmitterTask(PolicyExecutor executor, Callable<T> callable) {
        this.executor = executor;
        this.callable = callable;
        this.callback = null;
    }

    public SubmitterTask(PolicyExecutor executor, Callable<T> callable, CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.executor = executor;
        this.callable = callable;
        this.callback = null;
    }

    public SubmitterTask(PolicyExecutor executor, Callable<T> callable, PolicyTaskCallback callback) {
        this.executor = executor;
        this.callable = callable;
        this.callback = callback;
    }

    @Override
    public Future<T> call() throws InterruptedException, TimeoutException {
        System.out.println("> call " + toString());
        if (beginLatch != null)
            beginLatch.countDown();
        try {
            if (continueLatch != null && !continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS))
                throw new TimeoutException();
            @SuppressWarnings("unchecked")
            Future<T> future = (Future<T>) (callback == null ? executor.submit(callable) : executor.submit(callable, callback));
            System.out.println("< call " + toString() + " " + future);
            return future;
        } catch (RuntimeException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }
}

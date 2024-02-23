/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.ScheduledCustomExecutorTask;

/**
 * A task that directs its execution to the specified PolicyExecutor
 * when submitted to the Liberty scheduled executor.
 */
public class ScheduledRunnableTask implements Runnable, ScheduledCustomExecutorTask {
    private final CountDownLatch beginLatch;
    private final CountDownLatch continueLatch;
    private final PolicyExecutor executor;

    /**
     * Counts down the beginLatch every time the run method is invoked.
     * After counting down to 0, then awaits the continueLatch.
     *
     * @param executor
     * @param beginLatch
     * @param continueLatch
     */
    public ScheduledRunnableTask(PolicyExecutor executor, CountDownLatch beginLatch, CountDownLatch continueLatch) {
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.executor = executor;
    }

    @Override
    public PolicyExecutor getExecutor() {
        return executor;
    }

    @Override
    public Exception resubmitFailed(Exception failure) {
        return failure;
    }

    @Override
    public void run() {
        System.out.println("> run " + toString());
        beginLatch.countDown();
        try {
            long remaining = beginLatch.getCount();
            if (remaining == 0) {
                boolean awaited = continueLatch.await(PolicyExecutorServlet.TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
                System.out.println("< run " + toString() + " " + awaited);
                if (!awaited)
                    throw new CompletionException(new TimeoutException("Task timed out"));
            } else {
                System.out.println("< run " + toString() + " with " + remaining + " remaining");
            }
        } catch (InterruptedException x) {
            System.out.println("< run " + toString() + " " + x);
            throw new CompletionException(x);
        }
    }
}

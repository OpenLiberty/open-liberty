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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.PolicyExecutor;

/**
 * Task that decrements a CountDownLatch when it runs and then awaits a second latch.
 * When the second latch is invoked the requested config change is called on the supplied executor
 */
public class ConfigChangeTask implements Callable<Boolean> {
    private final ExecutorService executor;
    private final long awaitContinueNanos;
    private final CountDownLatch beginLatch;
    private final CountDownLatch continueLatch;
    private final String methodName;
    private final String value;

    public ConfigChangeTask(ExecutorService executor, CountDownLatch beginLatch, CountDownLatch continueLatch, long awaitContinueNanos, String methodName, String value) {
        this.awaitContinueNanos = awaitContinueNanos;
        this.beginLatch = beginLatch;
        this.continueLatch = continueLatch;
        this.methodName = methodName;
        this.value = value;
        this.executor = executor;
    }

    @Override
    public Boolean call() throws InterruptedException {
        System.out.println("> call " + toString());
        beginLatch.countDown();
        try {
            boolean awaited = continueLatch.await(awaitContinueNanos, TimeUnit.NANOSECONDS);
            System.out.println("< call " + toString() + " " + awaited + " to call " + methodName + " with value " + value);
            if (methodName.equals("maxConcurrency")) {
                ((PolicyExecutor) executor).maxConcurrency(Integer.parseInt(value));
            } else if (methodName.equals("maxQueueSize")) {
                ((PolicyExecutor) executor).maxQueueSize(Integer.parseInt(value));
            } else if (methodName.equals("maxWaitForEnqueue")) {
                ((PolicyExecutor) executor).maxWaitForEnqueue(Long.parseLong(value));
            }
            return awaited;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }
}

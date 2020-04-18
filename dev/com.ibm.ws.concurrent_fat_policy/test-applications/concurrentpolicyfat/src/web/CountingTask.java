/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.ManagedTaskListener;

class CountingTask implements Callable<Integer>, ManagedTask {
    final CountDownLatch beginLatch;
    final CountDownLatch continueLatch;
    final AtomicInteger counter;
    final Map<String, String> execProps = new TreeMap<String, String>();
    final ManagedTaskListener listener;
    volatile long threadId;

    CountingTask(AtomicInteger counter, ManagedTaskListener listener, CountDownLatch beginLatch, CountDownLatch continueLatch) {
        this.beginLatch = beginLatch == null ? new CountDownLatch(0) : beginLatch;
        this.continueLatch = continueLatch == null ? new CountDownLatch(0) : continueLatch;
        this.counter = counter == null ? new AtomicInteger() : counter;
        this.listener = listener;
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("> call " + toString());
        threadId = Thread.currentThread().getId();
        beginLatch.countDown();
        try {
            if (!continueLatch.await(ConcurrentPolicyFATServlet.TIMEOUT_NS * 2, TimeUnit.NANOSECONDS)) {
                System.out.println("< call " + toString() + " -1 (timed out)");
                return -1;
            }
            int count = counter.incrementAndGet();
            System.out.println("< call " + toString() + " " + count);
            return count;
        } catch (InterruptedException x) {
            System.out.println("< call " + toString() + " " + x);
            throw x;
        }
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return listener;
    }
}
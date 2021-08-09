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
package fat.concurrent.spec.app;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A simple task that takes some time to run.
 * Try to avoid using this if possible because it will slow down tests.
 */
class SlowTask implements Callable<Long>, Runnable {
    static final TraceComponent tc = Tr.register(SlowTask.class);

    final long delay;
    final AtomicReference<LinkedBlockingQueue<Future<?>>> cancelationQueueRef = new AtomicReference<LinkedBlockingQueue<Future<?>>>();
    final AtomicBoolean interruptIfCanceled = new AtomicBoolean();
    final AtomicInteger interruptionCount;
    final AtomicInteger numStarted;

    SlowTask() {
        numStarted = new AtomicInteger();
        interruptionCount = new AtomicInteger();
        delay = EEConcurrencyTestServlet.TIMEOUT * 5;
    }

    SlowTask(AtomicInteger numStarted, AtomicInteger interruptionCount, long delay) {
        this.numStarted = numStarted;
        this.interruptionCount = interruptionCount;
        this.delay = delay;
    }

    @Override
    public void run() {
        int num = numStarted.incrementAndGet();
        Tr.entry(this, tc, "run " + num + " for " + delay + " ms");
        try {
            LinkedBlockingQueue<Future<?>> cancelationQueue = cancelationQueueRef.get();
            if (cancelationQueue != null)
                cancelationQueue.poll(EEConcurrencyTestServlet.TIMEOUT, TimeUnit.MILLISECONDS).cancel(interruptIfCanceled.get());

            Thread.sleep(delay);
        } catch (InterruptedException x) {
            int count = interruptionCount.incrementAndGet();
            Tr.exit(this, tc, "run interrupted; interruption count=" + count);
            throw new RuntimeException(x);
        }
        Tr.exit(this, tc, "run");
    }

    @Override
    public Long call() throws InterruptedException {
        int num = numStarted.incrementAndGet();
        Tr.entry(this, tc, "call " + num + " for " + delay + " ms");
        try {
            LinkedBlockingQueue<Future<?>> cancelationQueue = cancelationQueueRef.get();
            if (cancelationQueue != null)
                cancelationQueue.poll(EEConcurrencyTestServlet.TIMEOUT, TimeUnit.MILLISECONDS).cancel(interruptIfCanceled.get());

            Thread.sleep(delay);
        } catch (InterruptedException x) {
            int count = interruptionCount.incrementAndGet();
            Tr.exit(this, tc, "call interrupted; interruption count=" + count);
            throw x;
        }
        Tr.exit(this, tc, "call");
        return delay;
    };
}
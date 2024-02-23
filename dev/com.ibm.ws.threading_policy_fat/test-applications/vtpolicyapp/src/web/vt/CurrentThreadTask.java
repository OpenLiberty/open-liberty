/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package web.vt;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tasks that returns the thread on which it runs.
 */
public class CurrentThreadTask implements Callable<Thread> {
    CountDownLatch taskCompletionBlocker;
    CountDownLatch[] taskStartCountdowns;

    CurrentThreadTask(CountDownLatch taskCompletionBlocker, CountDownLatch... taskStartCountdowns) {
        this.taskCompletionBlocker = taskCompletionBlocker;
        this.taskStartCountdowns = taskStartCountdowns;
    }

    @Override
    public Thread call() throws TimeoutException, InterruptedException {
        Thread thread = Thread.currentThread();
        System.out.println("> CurrentThreadTask@ " + Long.toHexString(hashCode()) + " running on " + thread + " #" + thread.getId());

        if (taskStartCountdowns != null)
            for (CountDownLatch latch : taskStartCountdowns)
                latch.countDown();

        if (taskCompletionBlocker == null || taskCompletionBlocker.await(PolicyVirtualThreadServlet.TIMEOUT_NS, TimeUnit.NANOSECONDS)) {
            System.out.println("< CurrentThreadTask@ " + Long.toHexString(hashCode()) + ": thread #" + thread.getId());
            return thread;
        } else {
            System.out.println("< CurrentThreadTask@ " + Long.toHexString(hashCode()) + ": timed out");
            throw new TimeoutException("Timed out out waiting for " + taskCompletionBlocker);
        }
    }
}

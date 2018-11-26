/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class TestFunction implements Callable<String> {

    private final AtomicInteger executions = new AtomicInteger(0);
    private long callLength = 100;
    private int exception = 0;
    private final String context;
    private final CountDownLatch wait;
    private final CountDownLatch notify;

    public TestFunction(Duration callLength, int exception, CountDownLatch wait, CountDownLatch notify, String context) {
        this.callLength = callLength.toMillis();
        this.exception = exception;
        this.wait = wait;
        this.notify = notify;
        this.context = context;
    }

    public TestFunction(int exception, String context) {
        this(Duration.ofMillis(100), exception, null, null, context);
    }

    public TestFunction(Duration callLength, String context) {
        this(callLength, 0, null, null, context);
    }

    public TestFunction(Duration callLength, CountDownLatch wait, CountDownLatch notify, String context) {
        this(callLength, 0, wait, notify, context);
    }

    /** {@inheritDoc} */
    @Override
    public String call() throws Exception {
        int execution = executions.incrementAndGet();
        if (exception == -1 || execution <= exception) {
            System.out.println(System.currentTimeMillis() + " Test " + context + ": " + execution + "/" + exception + " - exception");
            throw new TestException();
        } else {
            System.out.println(System.currentTimeMillis() + " Test " + context + ": " + execution + "/" + exception + " - execute");
        }
        if (this.wait != null) {
            System.out.println(System.currentTimeMillis() + " Test " + context + ": " + execution + "/" + exception + " - waiting on latch");
            wait.await(callLength, TimeUnit.MILLISECONDS);
        } else {
            System.out.println(System.currentTimeMillis() + " Test " + context + ": " + execution + "/" + exception + " - sleeping");
            Thread.sleep(callLength);
        }
        System.out.println(System.currentTimeMillis() + " Test " + context + ": " + execution + "/" + exception + " - complete");
        if (notify != null) {
            notify.countDown();
        }
        return context;
    }

    public int getExecutions() {
        return executions.get();
    }

}

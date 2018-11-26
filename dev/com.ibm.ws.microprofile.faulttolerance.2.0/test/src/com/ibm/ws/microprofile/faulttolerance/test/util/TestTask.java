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

import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;

/**
 *
 */
public class TestTask implements Callable<String> {

    private final Executor<String> executor;
    private final TestFunction testFunction;
    private final String id;

    public TestTask(Executor<String> executor, Duration callLength, CountDownLatch wait, CountDownLatch notify, String id) {
        this.executor = executor;
        this.testFunction = new TestFunction(callLength, 0, wait, notify, id);
        this.id = id;
    }

    public TestTask(Executor<String> executor, Duration callLength, String id) {
        this.executor = executor;
        this.testFunction = new TestFunction(callLength, id);
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public String call() {
        FTExecutionContext context = this.executor.newExecutionContext(id, null, id);
        try {
            String execution = this.executor.execute(testFunction, context);
            return execution;
        } finally {
            context.close();
        }
    }

}

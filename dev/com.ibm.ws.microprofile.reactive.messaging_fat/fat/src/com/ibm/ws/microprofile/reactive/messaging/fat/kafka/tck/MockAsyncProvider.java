/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;
import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMContext;

/**
 * Mock implementation of {@link RMAsyncProvider} for running tests outside of liberty
 * <p>
 * It does no context propagation and uses a fixed size thread pool for concurrency.
 */
public class MockAsyncProvider implements RMAsyncProvider, AutoCloseable {

    public ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

    @Override
    public RMContext captureContext() {
        return RMContext.NOOP;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executor;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return executor;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Stop waiting and shutdown now
        }
        executor.shutdownNow();
    }

}

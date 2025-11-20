/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ToolStatusImpl implements ToolStatus {

    private final ConcurrentMap<String, CountDownLatch> startLatches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CountDownLatch> endLatches = new ConcurrentHashMap<>();

    // Retrieves an existing start latch or creates a new one if not present.
    private CountDownLatch getOrCreateStartLatch(String latchName) {
        synchronized (this) {
            return startLatches.computeIfAbsent(latchName, l -> new CountDownLatch(1));
        }
    }

    // Retrieves an existing end latch or creates a new one if not present.
    // Synchronized ensures only one thread creates a latch at a time for a given name.
    private CountDownLatch getOrCreateEndLatch(String latchName) {
        synchronized (this) {
            return endLatches.computeIfAbsent(latchName, l -> new CountDownLatch(1));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void signalStarted(String latchName) {
        // Countdown allows any waiting thread to proceed (tool start confirmed)
        CountDownLatch latch = getOrCreateStartLatch(latchName);
        latch.countDown();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitStarted(String latchName) {
        // Waits up to 10 seconds for the latch to count down (tool start)
        CountDownLatch latch = getOrCreateStartLatch(latchName);
        try {
            boolean toolStarted = latch.await(10, TimeUnit.SECONDS);
            if (!toolStarted) {
                throw new AssertionError("Timeout: Tool '" + latchName + "' did not start within 10 seconds.");
            }
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted: waiting for tool '" + latchName + "'");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void signalShouldEnd(String latchName) {
        // Counts down the latch, unblocking any waiting threads (tool allowed to end)
        getOrCreateEndLatch(latchName).countDown();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitShouldEnd(String latchName) {
        // Waits up to 10 seconds for the latch to be released (tool finish)
        try {
            boolean ok = getOrCreateEndLatch(latchName).await(10, TimeUnit.SECONDS);
            if (!ok) {
                throw new AssertionError("Timeout: Tool '" + latchName + "' was not signalled to end within 10 seconds.");
            }
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted: waiting end signal '" + latchName + "'");
        }
    }
}

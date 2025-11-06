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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ToolStatus {

    Map<String, CountDownLatch> latches = new HashMap<>();

    private CountDownLatch getOrCreateCountDownLatch(String latchName) {
        // ensures only one thread at a time can safely get or add a latch to the map of latches
        synchronized (this) {
            return latches.computeIfAbsent(latchName, l -> new CountDownLatch(1));
        }
    }

    public void setRunning(String latchName) {
        CountDownLatch latch = getOrCreateCountDownLatch(latchName);
        latch.countDown();
    }

    public void awaitRunning(String latchName) throws InterruptedException {
        CountDownLatch latch = getOrCreateCountDownLatch(latchName);
        boolean toolStarted = latch.await(10, TimeUnit.SECONDS);
        if (!toolStarted) {
            throw new RuntimeException("Tool did not start");
        }
    }
}

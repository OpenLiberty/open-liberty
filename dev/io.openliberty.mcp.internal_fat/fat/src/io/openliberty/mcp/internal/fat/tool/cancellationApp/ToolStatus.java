/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.cancellationApp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ToolStatus {

    private CountDownLatch latch;

    public void setRunning() {
        resetLatchForNewTest();
        latch.countDown();
    }

    public void awaitRunning() throws InterruptedException {
        resetLatchForNewTest();
        boolean toolStarted = latch.await(10, TimeUnit.SECONDS);
        if (!toolStarted) {
            throw new RuntimeException("Tool did not start");
        }
    }

    private void resetLatchForNewTest() {
        if (latch == null || latch.getCount() == 0)
            latch = new CountDownLatch(1);
    }
}

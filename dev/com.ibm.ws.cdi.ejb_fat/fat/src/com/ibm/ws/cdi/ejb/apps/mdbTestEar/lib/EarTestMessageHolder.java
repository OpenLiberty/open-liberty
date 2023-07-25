/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.mdbTestEar.lib;

import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EarTestMessageHolder {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MIN_WAIT = Duration.ofMillis(10);

    private final List<String> messages = new ArrayList<>();

    /**
     * Add a message to the holder
     *
     * @param message
     */
    public void addMessage(String message) {
        synchronized (messages) {
            messages.add(message);
            messages.notifyAll();
        }
    }

    /**
     * Assert that a particular message is added within the timeout
     *
     * @param expectedMessage the message to wait for
     * @throws InterruptedException if interrupted
     */
    public void assertMessageArrives(String expectedMessage) throws InterruptedException {
        long startTime = System.nanoTime();
        synchronized (messages) {
            while (!messages.contains(expectedMessage)) {
                Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
                Duration remaining = TIMEOUT.minus(elapsed);
                if (remaining.minus(MIN_WAIT).isNegative()) {
                    System.out.println("Giving up waiting");
                    fail("Timed out waiting for message: " + expectedMessage + ", messages received:" + messages);
                }
                System.out.println("Still waiting for " + remaining);
                messages.wait(remaining.toMillis());
            }
        }
    }
}

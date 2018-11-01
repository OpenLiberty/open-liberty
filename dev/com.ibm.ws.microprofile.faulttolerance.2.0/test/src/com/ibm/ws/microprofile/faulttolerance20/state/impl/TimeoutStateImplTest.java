/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.TimeoutPolicyImpl;

public class TimeoutStateImplTest {

    private ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean timeoutFlag = new AtomicBoolean(false);

    @Before
    public void setup() {
        scheduledExecutorService = new ScheduledThreadPoolExecutor(10);
    }

    @After
    public void teardown() throws InterruptedException {
        scheduledExecutorService.shutdownNow();
        scheduledExecutorService.awaitTermination(10, SECONDS);
    }

    @Test
    public void testNoTimeout() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy);
        state.start(this::setTimeoutFlag);
        state.stop();

        assertFalse(state.isTimedOut());
        assertFalse(timeoutFlag.get());

        // Wait until the timeout should have fired
        Thread.sleep(150L);

        assertFalse(state.isTimedOut());
        assertFalse(timeoutFlag.get());
    }

    @Test
    public void testTimeout() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy);
        state.start(this::setTimeoutFlag);

        assertFalse(timeoutFlag.get());

        // Wait until the timeout should have fired
        Thread.sleep(150L);

        state.stop();

        assertTrue(state.isTimedOut());
        assertTrue(timeoutFlag.get());
    }

    private void setTimeoutFlag() {
        timeoutFlag.set(true);
    }

}

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

import static com.ibm.ws.microprofile.faulttolerance20.state.impl.MockScheduledTaskMatcher.cancelledTask;
import static com.ibm.ws.microprofile.faulttolerance20.state.impl.MockScheduledTaskMatcher.taskWithDelay;
import static java.time.Duration.ofMillis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.TimeoutPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;

@SuppressWarnings("restriction") // Unit test accesses non-exported *PolicyImpl classes
public class TimeoutStateImplTest {

    private MockScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean timeoutFlag = new AtomicBoolean(false);
    private static MetricRecorder dummyMetrics = DummyMetricRecorder.get();

    @Before
    public void setup() {
        scheduledExecutorService = new MockScheduledExecutorService();
    }

    @Test
    public void testNoTimeout() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy, dummyMetrics);
        state.start();
        state.setTimeoutCallback(this::setTimeoutFlag);

        // Check the timeout task has been scheduled
        assertThat(scheduledExecutorService.getTasks(), contains(taskWithDelay(ofMillis(100))));

        state.stop();

        // Check the timeout task has been cancelled
        assertThat(scheduledExecutorService.getTasks(), contains(cancelledTask()));

        assertFalse(state.isTimedOut());
        assertFalse(timeoutFlag.get());
    }

    @Test
    public void testTimeout() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy, dummyMetrics);
        state.start();
        state.setTimeoutCallback(this::setTimeoutFlag);

        // Check the timeout task has been scheduled
        assertThat(scheduledExecutorService.getTasks(), contains(taskWithDelay(ofMillis(100))));

        assertFalse(timeoutFlag.get());

        // Run the timeout
        scheduledExecutorService.getTasks().get(0).run();

        state.stop();

        assertTrue(state.isTimedOut());
        assertTrue(timeoutFlag.get());
    }

    @Test
    public void testTimeoutZero() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ZERO);

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy, dummyMetrics);
        state.start();
        state.setTimeoutCallback(this::setTimeoutFlag);

        // Assert that no timeout test was scheduled
        assertThat(scheduledExecutorService.getTasks(), is(empty()));

        assertFalse(timeoutFlag.get());

        state.stop();

        assertFalse(state.isTimedOut());
        assertFalse(timeoutFlag.get());
    }

    /**
     * Test setting the timeoutCallback before calling start()
     */
    @Test
    public void testCallbackSetEarly() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy, dummyMetrics);
        state.setTimeoutCallback(this::setTimeoutFlag);
        state.start();

        // Check the timeout task has been scheduled
        assertThat(scheduledExecutorService.getTasks(), contains(taskWithDelay(ofMillis(100))));

        assertFalse(timeoutFlag.get());

        // Call the timeout
        scheduledExecutorService.getTasks().get(0).run();

        state.stop();

        assertTrue(state.isTimedOut());
        assertTrue(timeoutFlag.get());
    }

    /**
     * Test setting the timeoutCallback after calling stop()
     */
    @Test
    public void testCallbackSetLate() throws InterruptedException {
        TimeoutPolicyImpl policy = new TimeoutPolicyImpl();
        policy.setTimeout(Duration.ofMillis(100L));

        TimeoutStateImpl state = new TimeoutStateImpl(scheduledExecutorService, policy, dummyMetrics);
        state.start();

        // Check the timeout task has been scheduled
        assertThat(scheduledExecutorService.getTasks(), contains(taskWithDelay(ofMillis(100))));

        assertFalse(timeoutFlag.get());

        // Call the timeout
        scheduledExecutorService.getTasks().get(0).run();

        state.stop();

        assertTrue(state.isTimedOut());
        assertFalse(timeoutFlag.get());

        state.setTimeoutCallback(this::setTimeoutFlag);

        assertTrue(state.isTimedOut());
        assertTrue(timeoutFlag.get());
    }

    private void setTimeoutFlag() {
        timeoutFlag.set(true);
    }

}

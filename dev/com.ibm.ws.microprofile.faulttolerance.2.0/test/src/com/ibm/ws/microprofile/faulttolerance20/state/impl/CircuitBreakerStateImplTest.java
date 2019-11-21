/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.CircuitBreakerPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;

@SuppressWarnings("restriction") // Unit test accesses non-exported *PolicyImpl classes
public class CircuitBreakerStateImplTest {

    private static MetricRecorder dummyMetrics = DummyMetricRecorder.get();

    @Test
    public void testCircuitBreaker() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(1);

        // Summary:
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // Run 100% failures, results in circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 1 failure, results in circuit open
        runFailures(state, 1);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 1 success, results in circuit closed
        runSuccesses(state, 1);

        // Test circuit really closed by running another success
        runSuccesses(state, 1);

        // Run 3 failures -> 75% failure rate -> circuit open
        runFailures(state, 3);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 1 success, results in circuit closed
        runSuccesses(state, 1);

        // Run 2 successes, 2 failures -> 50% failure rate -> circuit remains closed
        runSuccesses(state, 2);
        runFailures(state, 2);

        // Assert circuit closed by running another success
        runSuccesses(state, 1);
    }

    @Test
    public void testFailOn() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(1);
        policy.setFailOn(TestExceptionA.class);

        // Summary:
        // Only TestExceptionA counts as a failure
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // 4x TestExceptionA -> 100% failure rate -> circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4, TestExceptionA.class);
        assertFalse(state.requestPermissionToExecute());

        // Wait >150ms -> circuit half-open
        Thread.sleep(150);
        // Run 1 success, results in circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionB -> 0% failure rate -> circuit closed
        runFailures(state, 4, TestExceptionB.class);
        // assert circuit closed
        runSuccesses(state, 1);
    }

    @Test
    public void testSkipOn() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(1);
        policy.setFailOn(TestExceptionA.class);
        policy.setSkipOn(TestExceptionAextended.class);

        // Summary:
        // TestExceptionA         counts as a failure
        // TestExceptionAextended counts as a pass due to skipOn despite being instance of failOn
        // TestExceptionB         counts as a pass since not instance of failOn
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // 4x TestExceptionA -> 100% failure rate -> circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4, TestExceptionA.class);
        assertFalse(state.requestPermissionToExecute());

        // Wait >150ms -> circuit half-open
        Thread.sleep(150);
        // Run 1 success, results in circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionAextended -> 0% failure rate -> circuit closed
        runFailures(state, 4, TestExceptionAextended.class);
        // assert circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionB -> 0% failure rate -> circuit closed
        // Since TestExceptionB (or any of its superclasses) isn't mentioned in either failOn nor skipOn
        runFailures(state, 4, TestExceptionB.class);
        // assert circuit closed
        runSuccesses(state, 1);
    }

    @Test
    public void testMultipleExceptions() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(1);
        policy.setFailOn(TestExceptionA.class, TestExceptionBextended.class);
        policy.setSkipOn(TestExceptionB.class, TestExceptionAextended.class);

        // Summary:
        // Only TestExceptionA    counts as a failure
        // TestExceptionAextended counts as a pass due to skipOn despite being instance of failOn
        // TestExceptionB         counts as a pass due to skipOn
        // TestExceptionBextended counts as a pass due to instance of skipOn despite being failOn
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // 4x TestExceptionA -> 100% failure rate -> circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4, TestExceptionA.class);
        assertFalse(state.requestPermissionToExecute());

        // Wait >150ms -> circuit half-open
        Thread.sleep(150);
        // Run 1 success, results in circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionAextended -> 0% failure rate -> circuit closed
        runFailures(state, 4, TestExceptionAextended.class);
        // assert circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionB -> 0% failure rate -> circuit closed
        runFailures(state, 4, TestExceptionB.class);
        // assert circuit closed
        runSuccesses(state, 1);

        // 4x TestExceptionBextended -> 0% failure rate -> circuit closed
        runFailures(state, 4, TestExceptionBextended.class);
        // assert circuit closed
        runSuccesses(state, 1);
    }

    @Test
    public void testSuccessThreshold() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(3);

        // Summary:
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // Run 100% failures, results in circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 1 failure, results in circuit open
        runFailures(state, 1);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 3 successes, results in circuit closed
        runSuccesses(state, 3);

        // Test circuit really closed by running another success
        runSuccesses(state, 1);

        // Run 3 failures -> 75% failure rate -> circuit open
        runFailures(state, 3);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // Run 1 success, then 1 failure -> results in circuit open
        runSuccesses(state, 1);
        runFailures(state, 1);
        assertFalse(state.requestPermissionToExecute());
    }

    @Test
    public void testHalfOpenLimit() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(3);

        // Summary:
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 3 successful requests

        // Run 100% failures, results in circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // While half open, we should be able to start only three executions
        assertTrue(state.requestPermissionToExecute());
        assertTrue(state.requestPermissionToExecute());
        assertTrue(state.requestPermissionToExecute());
        assertFalse(state.requestPermissionToExecute());

        // As one finishes, we should be able to start one more
        state.recordResult(MethodResult.success(null));
        assertTrue(state.requestPermissionToExecute());
        assertFalse(state.requestPermissionToExecute());

        // As one finishes, we should be able to start one more
        state.recordResult(MethodResult.success(null));
        assertTrue(state.requestPermissionToExecute());
        assertFalse(state.requestPermissionToExecute());

        // After three complete successfully, circuit should be closed and we can run as many as we want
        state.recordResult(MethodResult.success(null));
        assertTrue(state.requestPermissionToExecute());
        assertTrue(state.requestPermissionToExecute());
        assertTrue(state.requestPermissionToExecute());
        assertTrue(state.requestPermissionToExecute());
    }

    @Test
    public void testHalfOpenStuck() throws InterruptedException {
        CircuitBreakerPolicyImpl policy = new CircuitBreakerPolicyImpl();
        policy.setDelay(Duration.ofMillis(100));
        policy.setFailureRatio(0.75);
        policy.setRequestVolumeThreshold(4);
        policy.setSuccessThreshold(1);

        // Summary:
        // circuit opens if 75% of last 4 requests failed
        // circuit half-closes after 100ms
        // circuit fully closes after 1 successful request

        // Run 100% failures, results in circuit open
        CircuitBreakerStateImpl state = new CircuitBreakerStateImpl(policy, dummyMetrics);
        runFailures(state, 4);
        assertFalse(state.requestPermissionToExecute());

        // Wait >100ms, results in circuit half-open
        Thread.sleep(150);
        // While half open, we should be able to start only one execution...
        assertTrue(state.requestPermissionToExecute());
        assertFalse(state.requestPermissionToExecute());

        // ...unless we wait for the delay again
        Thread.sleep(150);
        // then we should get one more
        assertTrue(state.requestPermissionToExecute());
        assertFalse(state.requestPermissionToExecute());
    }

    private void runFailures(CircuitBreakerState state, int times) {
        runFailures(state, times, RuntimeException.class);
    }

    private void runFailures(CircuitBreakerState state, int times, Class<? extends Exception> exClazz) {
        try {
            for (int i = 0; i < times; i++) {
                assertTrue("No permission for attempt " + i, state.requestPermissionToExecute());
                state.recordResult(MethodResult.failure(exClazz.newInstance()));
            }
        } catch (IllegalAccessException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void runSuccesses(CircuitBreakerState state, int times) {
        for (int i = 0; i < times; i++) {
            assertTrue("No permission for attempt " + i, state.requestPermissionToExecute());
            state.recordResult(MethodResult.success(null));
        }
    }

    // Needs to be package-private to allow runFailures() to reflectively construct it
    static class TestExceptionA extends Exception {
    }

    static class TestExceptionB extends Exception {
    }

    static class TestExceptionAextended extends TestExceptionA {
    }

    static class TestExceptionBextended extends TestExceptionB {
    }

}

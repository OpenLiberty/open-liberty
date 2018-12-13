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

import static java.time.temporal.ChronoUnit.YEARS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.RetryPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState.RetryResult;

public class RetryStateImplTest {

    @Test
    public void testMaxRetries() {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();

        // Should retry three times
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
    }

    @Test
    public void testNoRetryOnSuccess() {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();

        // Should not retry because execution was successful
        assertFalse(retryState.recordResult(MethodResult.success(null)).shouldRetry());
    }

    @Test
    public void testRetryOn() {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setRetryOn(TestExceptionA.class, TestExceptionB.class);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionAsubclass())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionB())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionC())).shouldRetry());
    }

    @Test
    public void testAbortOn() {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setAbortOn(TestExceptionA.class, TestExceptionB.class);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionAsubclass())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionB())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionC())).shouldRetry());
    }

    @Test
    public void testRetryOnAndAbortOn() {

        // ---------------------------
        // Expect to retry on ExceptionA, abort on ExceptionAsubclass
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setRetryOn(TestExceptionA.class, TestExceptionB.class);
        retryPolicy.setAbortOn(TestExceptionAsubclass.class);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionAsubclass())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionB())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionC())).shouldRetry());

        // ---------------------------
        // Expect to abort on both ExceptionA and ExceptionAsubclass since setAbortOn takes priority
        retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setRetryOn(TestExceptionAsubclass.class, TestExceptionB.class);
        retryPolicy.setAbortOn(TestExceptionA.class);

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionAsubclass())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionB())).shouldRetry());

        retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionC())).shouldRetry());
    }

    @Test
    public void testMaxDuration() throws InterruptedException {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setMaxDuration(Duration.ofMillis(200));

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        Thread.sleep(300);
        assertFalse(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
    }

    @Test
    public void testDelay() throws InterruptedException {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(3);
        retryPolicy.setDelay(Duration.ofMillis(200));
        retryPolicy.setJitter(Duration.ofMillis(0));

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();
        RetryResult result = retryState.recordResult(MethodResult.failure(new TestExceptionA()));
        assertTrue(result.shouldRetry());
        assertEquals(200, TimeUnit.MILLISECONDS.convert(result.getDelay(), result.getDelayUnit()));

        result = retryState.recordResult(MethodResult.failure(new TestExceptionA()));
        assertTrue(result.shouldRetry());
        assertEquals(200, TimeUnit.MILLISECONDS.convert(result.getDelay(), result.getDelayUnit()));
    }

    @Test
    public void testDelayJitter() throws InterruptedException {
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(10);
        retryPolicy.setDelay(Duration.ofMillis(200));
        retryPolicy.setJitter(Duration.ofMillis(50));

        Set<Long> delayTimes = new HashSet<>();

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();

        // Test 10 iterations
        for (int i = 0; i < 10; i++) {
            RetryResult result = retryState.recordResult(MethodResult.failure(new TestExceptionA()));
            assertTrue(result.shouldRetry());

            long delayMillis = TimeUnit.MILLISECONDS.convert(result.getDelay(), result.getDelayUnit());
            // Assert value within range
            assertThat(delayMillis, greaterThanOrEqualTo(150L));
            assertThat(delayMillis, lessThanOrEqualTo(250L));
            delayTimes.add(delayMillis);
        }

        // Assert that getDelay didn't always return the same result
        assertThat(delayTimes.size(), greaterThan(1));
    }

    @Test
    public void testDelayStream() {
        doDelayStreamTest(Duration.ZERO, Duration.ZERO);
        doDelayStreamTest(Duration.ofMillis(500), Duration.ZERO);
        doDelayStreamTest(Duration.ZERO, Duration.ofMinutes(5));
        doDelayStreamTest(Duration.ofSeconds(5), Duration.ofMillis(100));
    }

    private void doDelayStreamTest(Duration delay, Duration jitter) {
        PrimitiveIterator.OfLong stream = RetryStateImpl.createDelayStream(delay, jitter);
        HashSet<Long> allValues = new HashSet<>();

        long lowerBound = delay.minus(jitter).toNanos();
        long upperBound = delay.plus(jitter).toNanos();

        for (int i = 0; i < 100; i++) {
            long value = stream.nextLong();
            allValues.add(value);
            assertThat(value, greaterThanOrEqualTo(lowerBound));
            assertThat(value, lessThanOrEqualTo(upperBound));
        }

        if (jitter.isZero()) {
            assertThat(allValues, hasSize(1));
        } else {
            // out of 100 random values, we should get more than 10 different results *easily*
            assertThat(allValues, hasSize(greaterThan(10)));
        }
    }

    @Test
    public void testDelayStreamHuge() {
        // Test a delay stream which would return values larger than MAX_LONG
        // just iterate through the stream to check it doesn't throw exceptions and doesn't just return one value
        Duration delay5y = YEARS.getDuration().multipliedBy(5);
        Duration jitter9000y = YEARS.getDuration().multipliedBy(9000);
        PrimitiveIterator.OfLong hugeStream = RetryStateImpl.createDelayStream(delay5y, jitter9000y);
        HashSet<Long> values = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            values.add(hugeStream.nextLong());
        }
        // Assert we get at least 10 unique values
        assertThat(values, hasSize(greaterThan(10)));
    }

    @Test
    public void testMaxRetriesForever() {
        // Test a policy with maxRetries = -1 -> no retry count limit
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxRetries(-1);
        retryPolicy.setJitter(Duration.ZERO);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();

        // Should retry forever
        for (int i = 0; i < 100; i++) {
            assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
        }
    }

    @Test
    public void testMaxDurationForever() throws InterruptedException {
        // Test a policy with maxDuration = 0 -> no retry time limit
        RetryPolicyImpl retryPolicy = new RetryPolicyImpl();
        retryPolicy.setMaxDuration(Duration.ZERO);

        RetryStateImpl retryState = new RetryStateImpl(retryPolicy);
        retryState.start();

        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());

        // Unfortunately, the default value is 3 minutes, we're not going to wait longer than that in a unit test
        // Just check that we're not treating zero as "never retry"
        Thread.sleep(300);
        assertTrue(retryState.recordResult(MethodResult.failure(new TestExceptionA())).shouldRetry());
    }

    private class TestExceptionA extends Exception {}

    private class TestExceptionB extends Exception {}

    private class TestExceptionC extends Exception {}

    private class TestExceptionAsubclass extends TestExceptionA {}

}

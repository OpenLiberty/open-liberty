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

import static com.ibm.ws.microprofile.faulttolerance20.state.impl.DurationUtils.asClampedNanos;

import java.time.Duration;
import java.util.PrimitiveIterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;

public class RetryStateImpl implements RetryState {

    private final RetryPolicy policy;
    private final PrimitiveIterator.OfLong delayStream;
    private int attempts = 0;
    private long startNanos;

    public RetryStateImpl(RetryPolicy policy) {
        this.policy = policy;
        delayStream = createDelayStream(policy.getDelay(), policy.getJitter());
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        startNanos = System.nanoTime();
    }

    /** {@inheritDoc} */
    @Override
    public RetryResult recordResult(MethodResult<?> methodResult) {
        RetryResultImpl result = new RetryResultImpl();

        attempts++;
        long duration = System.nanoTime() - startNanos;

        if (methodResult.isFailure()) {
            // Failure case
            if (abortOn(methodResult.getFailure())) {
                result.shouldRetry = false;
            } else if (retryOn(methodResult.getFailure())) {
                result.shouldRetry = true;
            } else {
                result.shouldRetry = false;
            }
        } else {
            // Successful case
            result.shouldRetry = false;
        }

        // If we want to retry based on the methodResult, check if there's some other reason we shouldn't
        if (result.shouldRetry) {
            int maxAttempts = policy.getMaxRetries() + 1;
            if (maxAttempts != 0 && attempts >= maxAttempts) {
                result.shouldRetry = false;
            } else if (overMaxDuration(duration)) {
                result.shouldRetry = false;
            }
        }

        if (result.shouldRetry) {
            populateDelay(result);
        }

        return result;
    }

    private boolean overMaxDuration(long duration) {
        return !policy.getMaxDuration().isZero() // Zero -> no duration limit
               && Duration.ofNanos(duration).compareTo(policy.getMaxDuration()) >= 0; // duration >= policy.getMaxDuration()
    }

    private boolean abortOn(Throwable failure) {
        for (Class<? extends Throwable> abortClazz : policy.getAbortOn()) {
            if (abortClazz.isInstance(failure)) {
                return true;
            }
        }

        return false;
    }

    private boolean retryOn(Throwable failure) {
        for (Class<? extends Throwable> retryClazz : policy.getRetryOn()) {
            if (retryClazz.isInstance(failure)) {
                return true;
            }
        }
        return false;
    }

    private void populateDelay(RetryResultImpl result) {
        result.delay = delayStream.nextLong();
        if (result.delay < 0) {
            result.delay = 0;
        }
        result.delayUnit = TimeUnit.NANOSECONDS;
    }

    /**
     * Returns a stream of times in nanoseconds, randomly distributed between delay-jitter and delay+jitter
     * <p>
     * protected only to allow unit testing
     *
     * @param delay the delay duration
     * @param jitter the jitter duration
     * @return stream of times in nanoseconds within delay +/- jitter
     */
    protected static PrimitiveIterator.OfLong createDelayStream(Duration delay, Duration jitter) {
        if (jitter.isZero()) {
            // No jitter, return an infinite stream of the delay duration
            long delayNanos = asClampedNanos(delay);
            return LongStream.generate(() -> delayNanos).iterator();
        } else {
            // Using jitter, compute the upper and lower bounds and return a stream of randomly distributed values
            long lowerLimitNanos = asClampedNanos(delay.minus(jitter));
            long upperLimitNanos = asClampedNanos(delay.plus(jitter));
            return ThreadLocalRandom.current().longs(lowerLimitNanos, upperLimitNanos).iterator();
        }
    }

    public static class RetryResultImpl implements RetryResult {
        public boolean shouldRetry;
        public long delay;
        public TimeUnit delayUnit;

        @Override
        public boolean shouldRetry() {
            return shouldRetry;
        }

        @Override
        public long getDelay() {
            return delay;
        }

        @Override
        public TimeUnit getDelayUnit() {
            return delayUnit;
        }
    }

}

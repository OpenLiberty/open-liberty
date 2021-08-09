/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.NO_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.WITH_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance20.state.impl.DurationUtils.asClampedNanos;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.PrimitiveIterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;

public class RetryStateImpl implements RetryState {

    private final RetryPolicy policy;
    private final PrimitiveIterator.OfLong delayStream;
    private int attempts = 0;
    private long startNanos;

    private final MetricRecorder metricRecorder;

    public RetryStateImpl(RetryPolicy policy, MetricRecorder metricRecorder) {
        this.policy = policy;
        delayStream = createDelayStream(policy.getDelay(), policy.getJitter());
        this.metricRecorder = metricRecorder;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        startNanos = System.nanoTime();
    }

    /** {@inheritDoc} */
    @Override
    public RetryResult recordResult(MethodResult<?> methodResult) {
        RetryResultCategory retryResultCategory = null;

        attempts++;
        long duration = System.nanoTime() - startNanos;

        if (methodResult.isFailure()) {
            // Failure case
            if (abortOn(methodResult.getFailure())) {
                retryResultCategory = RetryResultCategory.EXCEPTION_IN_ABORT_ON;
            } else if (retryOn(methodResult.getFailure())) {
                retryResultCategory = RetryResultCategory.EXCEPTION_IN_RETRY_ON;
            } else {
                retryResultCategory = RetryResultCategory.EXCEPTION_NOT_IN_RETRY_ON;
            }
        } else {
            // Successful case
            retryResultCategory = RetryResultCategory.NO_EXCEPTION;
        }

        // Capture whether this result was considered a retry-able failure by the Retry
        boolean resultWasRetryableFailure = shouldRetry(retryResultCategory);

        // If we want to retry based on the methodResult, check if there's some other reason we shouldn't
        if (resultWasRetryableFailure) {
            int maxAttempts = policy.getMaxRetries() + 1;
            if (maxAttempts != 0 && attempts >= maxAttempts) {
                retryResultCategory = RetryResultCategory.MAX_RETRIES_REACHED;
            } else if (overMaxDuration(duration)) {
                retryResultCategory = RetryResultCategory.MAX_DURATION_REACHED;
            }
        }

        if (shouldRetry(retryResultCategory)) {
            metricRecorder.incrementRetriesCount();
        } else {
            // Finished execution, record metrics
            metricRecorder.incrementRetryCalls(retryResultCategory, attempts > 1 ? WITH_RETRIES : NO_RETRIES);
        }

        return createResult(retryResultCategory);
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

    private RetryResultImpl createResult(RetryResultCategory retryResultCategory) {
        long delay = 0;

        if (shouldRetry(retryResultCategory)) {
            delay = delayStream.nextLong();
            if (delay < 0) {
                delay = 0;
            }
        }

        return new RetryResultImpl(retryResultCategory, delay, NANOSECONDS);
    }

    private static boolean shouldRetry(RetryResultCategory category) {
        return category == RetryResultCategory.EXCEPTION_IN_RETRY_ON;
    }

    /**
     * Returns a stream of times in nanoseconds, randomly distributed between delay-jitter and delay+jitter
     * <p>
     * protected only to allow unit testing
     *
     * @param delay  the delay duration
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

    private static class RetryResultImpl implements RetryResult {
        private final RetryResultCategory retryResultCategory;
        private final long delay;
        private final TimeUnit delayUnit;

        private RetryResultImpl(RetryResultCategory reason, long delay, TimeUnit delayUnit) {
            this.retryResultCategory = reason;
            this.delay = delay;
            this.delayUnit = delayUnit;
        }

        @Override
        public boolean shouldRetry() {
            return RetryStateImpl.shouldRetry(retryResultCategory);
        }

        @Override
        public long getDelay() {
            return delay;
        }

        @Override
        public TimeUnit getDelayUnit() {
            return delayUnit;
        }

        @Override
        public RetryResultCategory getCategory() {
            return retryResultCategory;
        }

        @Override
        public String toString() {
            return retryResultCategory.toString();
        }
    }

}

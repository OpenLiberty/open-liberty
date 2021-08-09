/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics23.impl;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.SimpleTimer;

import com.ibm.ws.microprofile.metrics.impl.Clock;
import com.ibm.ws.microprofile.metrics.impl.CounterImpl;

/**
 * A timer metric which aggregates timing durations and provides duration statistics, plus
 * throughput statistics via {@link Meter}.
 */
public class SimpleTimerImpl implements SimpleTimer {
    /**
     * A timing context.
     *
     * @see SimpleTimerImpl#time()
     */
    public static class Context implements SimpleTimer.Context {
        private final SimpleTimerImpl simpleTimer;
        private final Clock clock;
        private final long startTime;

        private Context(SimpleTimerImpl simpleTimer, Clock clock) {
            this.simpleTimer = simpleTimer;
            this.clock = clock;
            this.startTime = clock.getTick();
        }

        /**
         * Updates the timer with the difference between current and start time. Call to this method will
         * not reset the start time. Multiple calls result in multiple updates.
         *
         * @return the elapsed time in nanoseconds
         */
        @Override
        public long stop() {
            final long elapsed = clock.getTick() - startTime;
            simpleTimer.update(Duration.ofNanos(elapsed));
            return elapsed;
        }

        /** Equivalent to calling {@link #stop()}. */
        @Override
        public void close() {
            stop();
        }
    }

    private final Counter count;
    private Duration elapsedTime;
    private final Clock clock;

    public SimpleTimerImpl() {
        this(Clock.defaultClock());
    }

    public SimpleTimerImpl(Clock clock) {
        this.clock = clock;
        this.count = new CounterImpl();
        this.elapsedTime = Duration.ZERO;
    }

    /**
     * Adds a recorded duration.
     *
     * @param duration the length of the duration
     * @param unit     the scale unit of {@code duration}
     */
    @Override
    public void update(Duration duration) {
        synchronized (this) {
            elapsedTime = elapsedTime.plus(duration);
        }

        if (duration.toNanos() >= 0) {
            count.inc();
        }
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception {
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public void time(Runnable event) {
        final long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public Context time() {
        return new Context(this, clock);
    }

    @Override
    public long getCount() {
        return count.getCount();
    }

    @Override
    public Duration getElapsedTime() {
        return elapsedTime;
    }
}

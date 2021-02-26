/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.impl;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.SimpleTimer;

import com.ibm.ws.microprofile.metrics.impl.Clock;
import com.ibm.ws.microprofile.metrics.impl.CounterImpl;

/**
 * A timer metric which aggregates timing durations and provides duration statistics, plus
 * throughput statistics via {@link Meter}.
 */
public class SimpleTimer30Impl implements SimpleTimer {

    // maximum count achieved in previous minute
    private final AtomicLong max_previousMinute;
    // minimum count achieved in previous minute
    private final AtomicLong min_previousMinute;

    // maximum count achieved in this minute
    private final AtomicLong max_thisMinute;
    // minimum count achieved in this minute
    private final AtomicLong min_thisMinute;

    // current timestamp rounded down to the last whole minute
    private final AtomicLong thisMinute;

    /**
     * A timing context.
     *
     * @see SimpleTimer30Impl#time()
     */
    public static class Context implements SimpleTimer.Context {
        private final SimpleTimer30Impl simpleTimer;
        private final Clock clock;
        private final long startTime;

        private Context(SimpleTimer30Impl simpleTimer, Clock clock) {
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

    public SimpleTimer30Impl() {
        this(Clock.defaultClock());
    }

    public SimpleTimer30Impl(Clock clock) {
        this.clock = clock;
        this.count = new CounterImpl();
        this.elapsedTime = Duration.ZERO;

        max_previousMinute = new AtomicLong(0);
        min_previousMinute = new AtomicLong(0);
        max_thisMinute = new AtomicLong(0);
        min_thisMinute = new AtomicLong(0);
        thisMinute = new AtomicLong(getCurrentMinuteFromSystem());
    }

    /**
     * Adds a recorded duration.
     *
     * @param duration the length of the duration
     * @param unit     the scale unit of {@code duration}
     */
    @Override
    public void update(Duration duration) {
        maybeStartNewMinute();
        Long duration_nanos = duration.toNanos();
        if (duration_nanos >= 0) {
            synchronized (this) {
                count.inc();
                elapsedTime = elapsedTime.plus(duration);

                if (duration_nanos > max_thisMinute.get())
                    max_thisMinute.set(duration_nanos);

                if (duration_nanos < min_thisMinute.get() || min_thisMinute.get() == 0L)
                    min_thisMinute.set(duration_nanos);
            }
        }
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception {
        maybeStartNewMinute();
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public void time(Runnable event) {
        maybeStartNewMinute();
        final long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));

        }
    }

    @Override
    public Context time() {
        maybeStartNewMinute();
        return new Context(this, clock);
    }

    @Override
    public long getCount() {
        maybeStartNewMinute();
        return count.getCount();
    }

    @Override
    public Duration getElapsedTime() {
        maybeStartNewMinute();
        return elapsedTime;
    }

    /** {@inheritDoc} */
    @Override
    public Duration getMaxTimeDuration() {
        maybeStartNewMinute();
        return (max_previousMinute.get() == 0) ? null : Duration.ofNanos(max_previousMinute.get());
    }

    /** {@inheritDoc} */
    @Override
    public Duration getMinTimeDuration() {
        maybeStartNewMinute();
        return (min_previousMinute.get() == 0) ? null : Duration.ofNanos(min_previousMinute.get());
    }

    /*
     * If a new minute has started, move the data for 'this' minute to 'previous' minute and
     * reset the min/max values to 0
     */
    private void maybeStartNewMinute() {
        long newMinute = getCurrentMinuteFromSystem();
        if (newMinute > thisMinute.get()) {
            synchronized (this) {
                if (newMinute > thisMinute.get()) {
                    thisMinute.set(newMinute);

                    max_previousMinute.set(max_thisMinute.get());
                    min_previousMinute.set(min_thisMinute.get());

                    //If new minute - reset to 0
                    max_thisMinute.set(0L);
                    min_thisMinute.set(0L);
                }
            }
        }
    }

    // Get the current system time in minutes, truncating. This number will increase by 1 every complete minute.
    private long getCurrentMinuteFromSystem() {
        return System.currentTimeMillis() / 60000;
    }
}

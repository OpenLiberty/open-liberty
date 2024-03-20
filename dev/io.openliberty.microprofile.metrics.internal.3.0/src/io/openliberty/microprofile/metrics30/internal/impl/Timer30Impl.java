/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
*******************************************************************************
* Copyright 2010-2013 Coda Hale and Yammer, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.impl;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.ws.microprofile.metrics.impl.Clock;
import com.ibm.ws.microprofile.metrics.impl.ExponentiallyDecayingReservoir;
import com.ibm.ws.microprofile.metrics.impl.HistogramImpl;
import com.ibm.ws.microprofile.metrics.impl.MeterImpl;
import com.ibm.ws.microprofile.metrics.impl.Reservoir;
import com.ibm.ws.microprofile.metrics.impl.TimerImpl;

import io.openliberty.microprofile.metrics30.internal.helper.BucketManager;
import io.openliberty.microprofile.metrics30.internal.helper.BucketManager.BucketValue;
import io.openliberty.microprofile.metrics30.setup.config.MetricPercentileConfiguration;
import io.openliberty.microprofile.metrics30.setup.config.MetricsConfigurationManager;

/**
 * A timer metric which aggregates timing durations and provides duration statistics, plus
 * throughput statistics via {@link Meter}.
 */
public class Timer30Impl implements Timer {
    /**
     * A timing context.
     *
     * @see TimerImpl#time()
     */
    public static class Context implements Timer.Context {
        protected final Timer30Impl timer;
        protected final Clock clock;
        protected final long startTime;

        public Context(Timer30Impl timer, Clock clock) {
            this.timer = timer;
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
            timer.update(Duration.ofNanos(elapsed));
            return elapsed;
        }

        /** Equivalent to calling {@link #stop()}. */
        @Override
        public void close() {
            stop();
        }
    }

    protected Duration elapsedTime;
    protected final Meter meter;
    protected final Histogram histogram;
    protected final BucketManager manager;
    protected final Clock clock;
    private final double[] percentiles;

    /**
     * Creates a new {@link TimerImpl} using an {@link ExponentiallyDecayingReservoir} and the default
     * {@link Clock}.
     *
     * @param metadata
     */
    public Timer30Impl(Metadata metadata) {
        this(new ExponentiallyDecayingReservoir(), metadata);
    }

    /**
     * Creates a new {@link TimerImpl} that uses the given {@link Reservoir}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should use
     * @param metadata
     */
    public Timer30Impl(Reservoir reservoir, Metadata metadata) {
        this(reservoir, Clock.defaultClock(), metadata);
    }

    /**
     * Creates a new {@link TimerImpl} that uses the given {@link Reservoir} and {@link Clock}.
     *
     * @param reservoir the {@link Reservoir} implementation the timer should use
     * @param clock     the {@link Clock} implementation the timer should use
     */
    protected Timer30Impl(Reservoir reservoir, Clock clock, Metadata metadata) {
        this.meter = new MeterImpl(clock);
        this.clock = clock;
        this.histogram = new HistogramImpl(reservoir);
        this.manager = new BucketManager(metadata);
        this.elapsedTime = Duration.ZERO;
        this.percentiles = setConfiguredPercentiles(metadata);

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
            if (duration.toNanos() >= 0) {
                histogram.update(duration.toNanos());
                manager.update(duration.getSeconds());
                System.out.println("Duration(default): " + duration + " -- toNanos: " + duration.toNanos() + " seconds:" + duration.getSeconds());
                meter.mark();
                elapsedTime = elapsedTime.plus(duration);
            }
        }

    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Callable} whose {@link Callable#call()} method implements a process
     *                  whose duration should be timed
     * @param <T>   the type of the value returned by {@code event}
     * @return the value returned by {@code event}
     * @throws Exception if {@code event} throws an {@link Exception}
     */
    @Override
    public <T> T time(Callable<T> event) throws Exception {
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Runnable} whose {@link Runnable#run()} method implements a process
     *                  whose duration should be timed
     */
    @Override
    public void time(Runnable event) {
        final long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    /**
     * Returns a new {@link Context}.
     *
     * @return a new {@link Context}
     * @see Context
     */
    @Override
    public Timer.Context time() {
        return new Context(this, clock);
    }

    @Override
    public long getCount() {
        return histogram.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return histogram.getSnapshot();
    }

    /** {@inheritDoc} */
    @Override
    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public Map<String, Map<Double, BucketValue>> getBuckets() {
        return manager.getBuckets();
    }

    public double[] getConfiguredPercentiles() {
        return percentiles;
    }

    public double[] setConfiguredPercentiles(Metadata metadata) {
        Optional<String> percentileConfiguration = ConfigProvider.getConfig().getOptionalValue("mp.metrics.distribution.percentiles", String.class);
        String metricName = metadata.getName();
        if (percentileConfiguration.isPresent()) {
            MetricPercentileConfiguration percentileConfig = MetricsConfigurationManager.getInstance().getPercentilesConfiguration(metricName);
            System.out.println("MetricName: " + metricName);
            if (percentileConfig != null && percentileConfig.getValues() != null
                && percentileConfig.getValues().length > 0) {
                double[] vals = Stream.of(percentileConfig.getValues()).mapToDouble(Double::doubleValue).toArray();

                for (Double value : vals) {
                    System.out.println("Timer Pecentile Value: " + value + " -- " + metadata.getUnit());

                }
                return vals;
            } else if (percentileConfig == null) {
                return null;
            } else {
                System.out.println("Returning empty double for percentiles");
                return new double[0];
            }
        }
        System.out.println("Returning null for percentiles.");
        return null;

    }

}
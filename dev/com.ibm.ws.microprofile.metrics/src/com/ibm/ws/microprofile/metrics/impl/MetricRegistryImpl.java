/*******************************************************************************
* Copyright (c) 2017 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
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
package com.ibm.ws.microprofile.metrics.impl;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistryImpl extends MetricRegistry {
    /**
     * Concatenates elements to form a dotted name, eliding any null values or empty strings.
     *
     * @param name the first element of the name
     * @param names the remaining elements of the name
     * @return {@code name} and {@code names} concatenated by periods
     */
    public static String name(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        return builder.toString();
    }

    /**
     * Concatenates a class name and elements to form a dotted name, eliding any null values or
     * empty strings.
     *
     * @param klass the first element of the name
     * @param names the remaining elements of the name
     * @return {@code klass} and {@code names} concatenated by periods
     */
    public static String name(Class<?> klass, String... names) {
        return name(klass.getName(), names);
    }

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }

    private final ConcurrentMap<String, Metric> metrics;
    private final ConcurrentMap<String, Metadata> metadata;

    /**
     * Creates a new {@link MetricRegistry}.
     */
    public MetricRegistryImpl() {
        this.metrics = buildMap();

        //initializing metadata in a separate list
        this.metadata = new ConcurrentHashMap<String, Metadata>();
    }

    /**
     * Creates a new {@link ConcurrentMap} implementation for use inside the registry. Override this
     * to create a {@link MetricRegistry} with space- or time-bounded metric lifecycles, for
     * example.
     *
     * @return a new {@link ConcurrentMap}
     */
    protected ConcurrentMap<String, Metric> buildMap() {
        return new ConcurrentHashMap<String, Metric>();
    }

    /**
     * Given a {@link Metric}, registers it under the given name.
     *
     * @param name the name of the metric
     * @param metric the metric
     * @param <T> the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {

        final Metric existing = metrics.putIfAbsent(name, metric);
        this.metadata.putIfAbsent(name, new Metadata(name, MetricType.from(metric.getClass())));
        if (existing == null) {
        } else {
            throw new IllegalArgumentException("A metric named " + name + " already exists");
        }
        return metric;
    }

    @Override
    public <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException {

        final Metric existing = metrics.putIfAbsent(name, metric);
        this.metadata.putIfAbsent(name, metadata);
        if (existing == null) {
        } else {
            throw new IllegalArgumentException("A metric named " + name + " already exists");
        }
        return metric;
    }

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    @Override
    public Counter counter(String name) {
        return this.counter(new Metadata(name, MetricType.COUNTER));
    }

    @Override
    public Counter counter(Metadata metadata) {
        Counter counter = getOrAdd(metadata.getName(), MetricBuilder.COUNTERS);
        this.metadata.put(metadata.getName(), metadata);
        return counter;
    }

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} using the provided MetricSupplier if none is registered.
     *
     * @param name the name of the metric
     * @param supplier a MetricSupplier that can be used to manufacture a counter.
     * @return a new or pre-existing {@link Counter}
     */
    Counter counter(String name, final MetricSupplier<Counter> supplier) {
        return getOrAdd(name, new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric() {
                return supplier.newMetric();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        });
    }

    /**
     * Return the {@link Histogram} registered under this name; or create and register
     * a new {@link Histogram} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Histogram}
     */
    @Override
    public Histogram histogram(String name) {
        return this.histogram(new Metadata(name, MetricType.HISTOGRAM));
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        Histogram histogram = getOrAdd(metadata.getName(), MetricBuilder.HISTOGRAMS);
        this.metadata.put(metadata.getName(), metadata);
        return histogram;
    }

    /**
     * Return the {@link Histogram} registered under this name; or create and register
     * a new {@link Histogram} using the provided MetricSupplier if none is registered.
     *
     * @param name the name of the metric
     * @param supplier a MetricSupplier that can be used to manufacture a histogram
     * @return a new or pre-existing {@link Histogram}
     */
    Histogram histogram(String name, final MetricSupplier<Histogram> supplier) {
        return getOrAdd(name, new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric() {
                return supplier.newMetric();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        });
    }

    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Meter}
     */
    @Override
    public Meter meter(String name) {
        return this.meter(new Metadata(name, MetricType.METERED));
    }

    @Override
    public Meter meter(Metadata metadata) {
        Meter meter = getOrAdd(metadata.getName(), MetricBuilder.METERS);
        this.metadata.put(metadata.getName(), metadata);
        return meter;
    }

    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} using the provided MetricSupplier if none is registered.
     *
     * @param name the name of the metric
     * @param supplier a MetricSupplier that can be used to manufacture a Meter
     * @return a new or pre-existing {@link Meter}
     */
    Meter meter(String name, final MetricSupplier<Meter> supplier) {
        return getOrAdd(name, new MetricBuilder<Meter>() {
            @Override
            public Meter newMetric() {
                return supplier.newMetric();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Meter.class.isInstance(metric);
            }
        });
    }

    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} if none is registered.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Timer}
     */
    @Override
    public Timer timer(String name) {
        return timer(new Metadata(name, MetricType.TIMER));
    }

    @Override
    public Timer timer(Metadata metadata) {
        Timer timer = getOrAdd(metadata.getName(), MetricBuilder.TIMERS);
        this.metadata.put(metadata.getName(), metadata);
        return timer;
    }

    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} using the provided MetricSupplier if none is registered.
     *
     * @param name the name of the metric
     * @param supplier a MetricSupplier that can be used to manufacture a Timer
     * @return a new or pre-existing {@link Timer}
     */
    Timer timer(String name, final MetricSupplier<Timer> supplier) {
        return getOrAdd(name, new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return supplier.newMetric();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        });
    }

    /**
     * Return the {@link Gauge} registered under this name; or create and register
     * a new {@link Gauge} using the provided MetricSupplier if none is registered.
     *
     * @param name the name of the metric
     * @param supplier a MetricSupplier that can be used to manufacture a Gauge
     * @return a new or pre-existing {@link Gauge}
     */
    Gauge gauge(String name, final MetricSupplier<Gauge> supplier) {
        return getOrAdd(name, new MetricBuilder<Gauge>() {
            @Override
            public Gauge newMetric() {
                return supplier.newMetric();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Gauge.class.isInstance(metric);
            }
        });
    }

    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    @Override
    public boolean remove(String name) {
        final Metric metric = metrics.remove(name);
        metadata.remove(name);
        if (metric != null) {
            return true;
        }
        return false;
    }

    /**
     * Removes all metrics which match the given filter.
     *
     * @param filter a filter
     */
    @Override
    public void removeMatching(MetricFilter filter) {
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            if (filter.matches(entry.getKey(), entry.getValue())) {
                remove(entry.getKey());
            }
        }
    }

    /**
     * Returns a set of the names of all the metrics in the registry.
     *
     * @return the names of all the metrics
     */
    @Override
    public SortedSet<String> getNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<String>(metrics.keySet()));
    }

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    @Override
    public SortedMap<String, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the gauges in the registry
     */
    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    @Override
    public SortedMap<String, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the counters in the registry and their names which match the given
     * filter.
     *
     * @param filter the metric filter to match
     * @return all the counters in the registry
     */
    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    /**
     * Returns a map of all the histograms in the registry and their names.
     *
     * @return all the histograms in the registry
     */
    @Override
    public SortedMap<String, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the histograms in the registry and their names which match the given
     * filter.
     *
     * @param filter the metric filter to match
     * @return all the histograms in the registry
     */
    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    /**
     * Returns a map of all the meters in the registry and their names.
     *
     * @return all the meters in the registry
     */
    @Override
    public SortedMap<String, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the meters in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the meters in the registry
     */
    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    /**
     * Returns a map of all the timers in the registry and their names.
     *
     * @return all the timers in the registry
     */
    @Override
    public SortedMap<String, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the timers in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the timers in the registry
     */
    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return register(name, builder.newMetric());
            } catch (IllegalArgumentException e) {
                final Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<String, T> getMetrics(Class<T> klass, MetricFilter filter) {
        final TreeMap<String, T> timers = new TreeMap<String, T>();
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(),
                                                                     entry.getValue())) {
                timers.put(entry.getKey(), (T) entry.getValue());
            }
        }
        return Collections.unmodifiableSortedMap(timers);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Metadata getMetadata(String name) {
        return metadata.get(name);
    }

    interface MetricSupplier<T extends Metric> {
        T newMetric();
    }

    /**
     * A quick and easy way of capturing the notion of default metrics.
     */
    private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric() {
                return new CounterImpl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric() {
                return new HistogramImpl(new ExponentiallyDecayingReservoir());
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };

        MetricBuilder<Meter> METERS = new MetricBuilder<Meter>() {
            @Override
            public Meter newMetric() {
                return new MeterImpl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Meter.class.isInstance(metric);
            }
        };

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return new TimerImpl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        T newMetric();

        boolean isInstance(Metric metric);
    }
}

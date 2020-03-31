/*******************************************************************************
* Copyright (c) 2019, 2020 IBM Corporation and others.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistryImpl extends MetricRegistry {

    protected final ConcurrentMap<String, Metric> metrics;
    protected final ConcurrentMap<String, Metadata> metadata;
    protected final ConcurrentMap<MetricID, Metric> metricsMID;
    protected final ConcurrentMap<String, Metadata> metadataMID;
    protected final ConcurrentHashMap<String, ConcurrentLinkedQueue<MetricID>> applicationMap;
    private final ConfigProviderResolver configResolver;

    private final static boolean usingJava2Security = System.getSecurityManager() != null;

    /**
     * Creates a new {@link MetricRegistry}.
     *
     * @param configResolver
     */
    public MetricRegistryImpl(ConfigProviderResolver configResolver) {
        this.metrics = buildMap();//duped
        this.metricsMID = new ConcurrentHashMap<MetricID, Metric>();

        //initializing metadata in a separate list
        this.metadata = new ConcurrentHashMap<String, Metadata>(); //duped
        this.metadataMID = new ConcurrentHashMap<String, Metadata>();

        this.applicationMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<MetricID>>();

        this.configResolver = configResolver;
    }

    /**
     * Concatenates elements to form a dotted name, eliding any null values or empty strings.
     *
     * @param name  the first element of the name
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

    /**
     * Convert the metric class type into an enum
     * For MP Metrics 1.0, MetricType.from(Class in) does not support lambdas or proxy classes
     *
     * @param in The metric
     * @return the matching Enum
     */
    public static MetricType from(Metric in) {
        if (Gauge.class.isInstance(in))
            return MetricType.GAUGE;
        if (Counter.class.isInstance(in))
            return MetricType.COUNTER;
        if (Histogram.class.isInstance(in))
            return MetricType.HISTOGRAM;
        if (Meter.class.isInstance(in))
            return MetricType.METERED;
        if (Timer.class.isInstance(in))
            return MetricType.TIMER;
        return MetricType.INVALID;
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
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        // For MP Metrics 1.0, MetricType.from(Class in) does not support lambdas or proxy classes

        //return register(new Metadata(name, from(metric)), metric);
        return register(Metadata.builder().withName(name).withType(from(metric)).build(), metric);
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
        return register(metadata, metric, null);
    }

    @Override
    @FFDCIgnore({ NoSuchElementException.class })
    public <T extends Metric> T register(Metadata metadata, T metric, Tag... tags) throws IllegalArgumentException {

        /*
         * Checks if MetaData with the given name already exists or not.
         * If it does, then check if they match.
         * Throw an exception otherwise.
         * RF->f(x)
         */
        if (metadataMID.keySet().contains(metadata.getName())) {
            Metadata existingMetadata = metadataMID.get(metadata.getName());

            if (!metadata.equals(existingMetadata)) {
                throw new IllegalArgumentException("Metadata does not match for existing Metadata for " + metadata.getName());
            }
        }
        //Create Copy of Metadata object so it can't be changed after its registered
        //rf-rm
        MetadataBuilder metadataBuilder = Metadata.builder(metadata);

        ArrayList<Tag> cumulativeTags = (tags == null) ? new ArrayList<Tag>() : new ArrayList<Tag>(Arrays.asList(tags));

        //Append global tags to the metric
        //rf-rm
        Config config = configResolver.getConfig(getThreadContextClassLoader());
        try {
            String[] globaltags = config.getValue("MP_METRICS_TAGS", String.class).split("(?<!\\\\),");
            for (String tag : globaltags) {
                if (!(tag == null || tag.isEmpty() || !tag.contains("="))) {
                    String key = tag.substring(0, tag.indexOf("="));
                    String val = tag.substring(tag.indexOf("=") + 1);
                    if (key.length() == 0 || val.length() == 0) {
                        throw new IllegalArgumentException("Malformed list of Global Tags. Tag names "
                                                           + "must match the following regex [a-zA-Z_][a-zA-Z0-9_]*."
                                                           + " Global Tag values must not be empty."
                                                           + " Global Tag values MUST escape equal signs `=` and commas `,`"
                                                           + "with a backslash `\\` ");
                    }
                    val = val.replace("\\,", ",");
                    val = val.replace("\\=", "=");
                    if (!cumulativeTags.contains(key)) {
                        cumulativeTags.add(new Tag(key, val));
                    }
                }
            }
        } catch (NoSuchElementException e) {
            //Continue if there is no global tags
        }

        MetricID MetricID = new MetricID(metadata.getName(), tags);
        Class<T> metricClass = determineMetricClass(metric);

        //Ensure all metrics with this name are the same type
        validateMetricNameToSingleType(MetricID.getName(), metricClass);

        /*
         * Rest of the method officialy registers the metric
         * Add to MetricID -> Metric Map and Name -> MetaData map
         */

        final Metric existingMetric = metricsMID.putIfAbsent(MetricID, metric);

        if (existingMetric != null) {
            throw new IllegalArgumentException("A metric named " + MetricID.getName() + " with tags " + MetricID.getTagsAsString() + " already exists");
        }

        this.metadataMID.putIfAbsent(metadata.getName(), metadataBuilder.build());

        addNameToApplicationMap(MetricID);
        return metric;
    }

    /**
     * Adds the MetricID to an application map.
     * This map is not a complete list of metrics owned by an application,
     * produced metrics are managed in the MetricsExtension
     *
     * @param name
     */
    protected void addNameToApplicationMap(MetricID metricID) {
        String appName = getApplicationName();
        addNameToApplicationMap(metricID, appName);
    }

    /**
     * Adds the MetricID to an application map given the application name.
     * This map is not a complete list of metrics owned by an application,
     * produced metrics are managed in the MetricsExtension
     *
     * @param metricID metric ID of metric that was added
     * @param appName  applicationName
     */
    public void addNameToApplicationMap(MetricID metricID, String appName) {
        // If it is a base metric, the name will be null
        if (appName == null)
            return;
        ConcurrentLinkedQueue<MetricID> list = applicationMap.get(appName);
        if (list == null) {
            ConcurrentLinkedQueue<MetricID> newList = new ConcurrentLinkedQueue<MetricID>();
            list = applicationMap.putIfAbsent(appName, newList);
            if (list == null)
                list = newList;
        }
        list.add(metricID);
    }

    public void unRegisterApplicationMetrics() {
        unRegisterApplicationMetrics(getApplicationName());
    }

    public void unRegisterApplicationMetrics(String appName) {
        ConcurrentLinkedQueue<MetricID> list = applicationMap.remove(appName);

        if (list != null) {
            for (MetricID metricID : list) {
                remove(metricID);
            }
        }
    }

    private String getApplicationName() {
        com.ibm.ws.runtime.metadata.ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (metaData != null) {
            com.ibm.websphere.csi.J2EEName name = metaData.getJ2EEName();
            if (name != null) {
                return name.getApplication();
            }
        }
        return null;
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
        return this.counter(name, null);
    }

    /** {@inheritDoc} */
    @Override
    public Counter counter(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.COUNTER).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.COUNTER)) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }

        return this.counter(metadata, tags);
    }

    @Override
    public Counter counter(Metadata metadata) {
        return this.counter(metadata, null);
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder.COUNTERS, tags);
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
        return this.histogram(name, null);
    }

    /** {@inheritDoc} */
    @Override
    public Histogram histogram(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.HISTOGRAM).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.HISTOGRAM)) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }

        return this.histogram(metadata, tags);
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return this.histogram(metadata, null);
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder.HISTOGRAMS, tags);
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
        // return this.meter(new Metadata(name, MetricType.METERED));
        return this.meter(name, null);
    }

    /** {@inheritDoc} */
    @Override
    public Meter meter(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.METERED).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.METERED)) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }
        return this.meter(metadata, tags);
    }

    @Override
    public Meter meter(Metadata metadata) {
        return this.meter(metadata, null);
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder.METERS, tags);
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
        return this.timer(name, null);
    }

    /** {@inheritDoc} */
    @Override
    public Timer timer(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.TIMER).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.TIMER)) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }
        return this.timer(metadata, tags);
    }

    @Override
    public Timer timer(Metadata metadata) {
        return this.timer(metadata, null);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder.TIMERS, tags);
    }

    /**
     * Removes all metrics with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    @Override
    public boolean remove(String name) {
        Iterator<Entry<MetricID, Metric>> iterator = metricsMID.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<MetricID, Metric> entry = iterator.next();
            MetricID tempMID = entry.getKey();
            if (tempMID.getName().equals(name)) {
                iterator.remove();
            }
        }
        metadataMID.remove(name);

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(MetricID metricID) {
        final Metric metric = metricsMID.remove(metricID);
        String name = metricID.getName();

        if (metric != null) {
            boolean isLastOne = true;

            for (MetricID mid : metricsMID.keySet()) {
                if (mid.getName().equals(name)) {
                    isLastOne = false;
                    break;
                }
            }
            if (isLastOne) {
                metadataMID.remove(name);
            }
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
        for (Map.Entry<MetricID, Metric> entry : metricsMID.entrySet()) {
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
        return Collections.unmodifiableSortedSet(new TreeSet<String>(metadataMID.keySet()));
    }

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the gauges in the registry
     */
    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    @Override
    public SortedMap<MetricID, Counter> getCounters() {
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
    public SortedMap<MetricID, Counter> getCounters(MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    /**
     * Returns a map of all the histograms in the registry and their names.
     *
     * @return all the histograms in the registry
     */
    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
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
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    /**
     * Returns a map of all the meters in the registry and their names.
     *
     * @return all the meters in the registry
     */
    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the meters in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the meters in the registry
     */
    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    /**
     * Returns a map of all the timers in the registry and their names.
     *
     * @return all the timers in the registry
     */
    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    /**
     * Returns a map of all the timers in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the timers in the registry
     */
    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Metric> T getOrAdd(Metadata metadata, MetricBuilder<T> builder, Tag... tags) {
        /*
         * Check if metric with this name already exists or not.
         * If it does exist, checks if is of the same metric type.
         * Will throw an exception otherwise
         */
        validateMetricNameToSingleType(metadata.getName(), builder);

        MetricID metricID = new MetricID(metadata.getName(), tags);
        final Metric metric = metricsMID.get(metricID);

        //Found an existing metric with matching MetricID
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) { //otherwise register this new metric..
            try {
                return register(metadata, builder.newMetric(), tags);
            } catch (IllegalArgumentException e) {

                //rf-rm
                validateMetricNameToSingleType(metadata.getName(), builder);

                final Metric added = metricsMID.get(metricID);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }
        throw new IllegalArgumentException(metadata.getName() + " is already used for a different type of metric");
    }

    /**
     * Identify if there exists an existing metric with the same metricName, but of different type and throw an exception if so
     *
     * @param name    metric name
     * @param builder MetricBuilder
     */
    private <T extends Metric> void validateMetricNameToSingleType(String name, MetricBuilder<T> builder) {
        for (MetricID mid : metricsMID.keySet()) {
            if (mid.getName().equals(name) && !builder.isInstance(metricsMID.get(mid))) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }
    }

    /**
     * Identify if there exists an existing metric with the same metricName, but of different type and throw an exception if so
     *
     * @param name        metric name
     * @param metricClass Class/Type of the metric
     */
    private <T extends Metric> void validateMetricNameToSingleType(String name, Class<T> metricClass) {

        for (Entry<MetricID, Metric> entrySet : metricsMID.entrySet()) {
            if (entrySet.getKey().getName().equals(name) && !metricClass.isAssignableFrom(entrySet.getValue().getClass())) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> klass, MetricFilter filter) {
        final TreeMap<MetricID, T> timers = new TreeMap<MetricID, T>();
        for (Map.Entry<MetricID, Metric> entry : metricsMID.entrySet()) {
            if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(),
                                                                     entry.getValue())) {
                timers.put(entry.getKey(), (T) entry.getValue());
            }
        }
        return Collections.unmodifiableSortedMap(timers);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsMID);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(metadataMID);
    }

    //do we still need this?!
    public Metadata getMetadata(String name) {
        return metadata.get(name);
    }

    protected <T extends Metric> Class<T> determineMetricClass(T metric) {
        if (Counter.class.isInstance(metric))
            return (Class<T>) Counter.class;
        if (ConcurrentGauge.class.isInstance(metric))
            return (Class<T>) ConcurrentGauge.class;
        if (Histogram.class.isInstance(metric))
            return (Class<T>) Histogram.class;
        if (Meter.class.isInstance(metric))
            return (Class<T>) Meter.class;
        if (Timer.class.isInstance(metric))
            return (Class<T>) Timer.class;
        if (Gauge.class.isInstance(metric))
            return (Class<T>) Gauge.class;
        return null;
    }

    /**
     * A quick and easy way of capturing the notion of default metrics.
     */
    public interface MetricBuilder<T extends Metric> {
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

        MetricBuilder<ConcurrentGauge> CONCURRENT_GAUGE = new MetricBuilder<ConcurrentGauge>() {
            @Override
            public ConcurrentGauge newMetric() {
                return new ConcurrentGaugeImpl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return ConcurrentGauge.class.isInstance(metric);
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

    /** {@inheritDoc} */
    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return Collections.unmodifiableSortedSet(new TreeSet<MetricID>(metricsMID.keySet()));
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name) {
        return this.concurrentGauge(name, null);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.CONCURRENT_GAUGE).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.CONCURRENT_GAUGE)) {
                throw new IllegalArgumentException(name + " is CONCURRENT_GAUGE used for a different type of metric");
            }
        }

        return this.concurrentGauge(metadata, tags);
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return this.concurrentGauge(metadata, null);
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder.CONCURRENT_GAUGE, tags);
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return getConcurrentGauges(MetricFilter.ALL);
    }

    /** {@inheritDoc} */
    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter filter) {
        return getMetrics(ConcurrentGauge.class, filter);
    }

    private ClassLoader getThreadContextClassLoader() {
        if (usingJava2Security) {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                return Thread.currentThread().getContextClassLoader();
            });
        }
        return Thread.currentThread().getContextClassLoader();
    }
}

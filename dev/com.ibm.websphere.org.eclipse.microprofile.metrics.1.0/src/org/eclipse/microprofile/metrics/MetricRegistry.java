/*
 **********************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *               2010-2013 Coda Hale, Yammer.com
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * The registry that stores metrics and their metadata.
 * The MetricRegistry provides methods to register, create and retrieve metrics and their respective metadata.
 * 
 * 
 * @see MetricFilter
 */
public abstract class MetricRegistry {
    
    /**
     * An enumeration representing the scopes of the MetricRegistry
     */
    public enum Type {
        /**
         * The Application (default) scoped MetricRegistry.
         * Any metric registered/accessed via CDI will use this MetricRegistry.
         */
        APPLICATION("application"), 
        
        /**
         * The Base scoped MetricRegistry.
         * This MetricRegistry will contain required metrics specified in the MicroProfile Metrics specification.
         */
        BASE("base"),
        
        /**
         * The Vendor scoped MetricRegistry.
         * This MetricRegistry will contain vendor provided metrics which may vary between different vendors.
         */
        VENDOR("vendor");
    
        private String name;
        
        private Type(String name) {
            this.name = name;
        }
        
        /**
         * Returns the name of the MetricRegistry scope.
         * @return the scope
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Concatenates elements to form a dotted name, eliding any null values or empty strings.
     *
     * @param name     the first element of the name
     * @param names    the remaining elements of the name
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

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }
    
    /**
     * Concatenates a class name and elements to form a dotted name, eliding any null values or
     * empty strings.
     *
     * @param klass    the first element of the name
     * @param names    the remaining elements of the name
     * @return {@code klass} and {@code names} concatenated by periods
     */
    public static String name(Class<?> klass, String... names) {
        return name(klass.getName(), names);
    }

    /**
     * Given a {@link Metric}, registers it under the given name. 
     * A {@link Metadata} object will be registered with the name and type.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public abstract <T extends Metric> T register(String name, T metric) throws IllegalArgumentException;
    
    /**
     * Given a {@link Metric}, registers it under the given name along with the provided {@link Metadata}.
     *
     * @param name      the name of the metric
     * @param metric    the metric
     * @param metadata  the metadata
     * @param <T>       the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public abstract <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException;


    /**
     * Return the {@link Counter} registered under this name; or create and register 
     * a new {@link Counter} if none is registered.
     * If a {@link Counter} was created, a {@link Metadata} object will be registered with the name and type.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    public abstract Counter counter(String name);
    
    /**
     * Return the {@link Counter} registered under the {@link Metadata}'s name; or create and register 
     * a new {@link Counter} if none is registered.
     * If a {@link Counter} was created, the provided {@link Metadata} object will be registered.
     * <p>
     * Note: The {@link Metadata} will not be updated if the metric is already registered.
     * </p>
     * 
     * @param metadata the name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    public abstract Counter counter(Metadata metadata);



    /**
     * Return the {@link Histogram} registered under this name; or create and register 
     * a new {@link Histogram} if none is registered.
     * If a {@link Histogram} was created, a {@link Metadata}  object will be registered with the name and type.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Histogram}
     */
    public abstract Histogram histogram(String name);
    
    /**
     * Return the {@link Histogram} registered under the {@link Metadata}'s name; or create and register 
     * a new {@link Histogram} if none is registered.
     * If a {@link Histogram} was created, the provided {@link Metadata} object will be registered.
     * <p>
     * Note: The {@link Metadata} will not be updated if the metric is already registered.
     * </p>
     * 
     * @param metadata the name of the metric
     * @return a new or pre-existing {@link Histogram}
     */
    public abstract Histogram histogram(Metadata metadata);


    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} if none is registered.
     * If a {@link Meter} was created, a {@link Metadata}  object will be registered with the name and type.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Meter}
     */
    public abstract Meter meter(String name);
    
    /**
     * Return the {@link Meter} registered under the {@link Metadata}'s name; or create and register 
     * a new {@link Meter} if none is registered.
     * If a {@link Meter} was created, the provided {@link Metadata} object will be registered.
     * <p>
     * Note: The {@link Metadata} will not be updated if the metric is already registered.
     * </p>
     * 
     * @param metadata the name of the metric
     * @return a new or pre-existing {@link Meter}
     */
    public abstract Meter meter(Metadata metadata);


    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} if none is registered.
     * If a {@link Timer} was created, a {@link Metadata}  object will be registered with the name and type.
     *
     * @param name the name of the metric
     * @return a new or pre-existing {@link Timer}
     */
    public abstract Timer timer(String name);
    
    /**
     * Return the {@link Timer} registered under the {@link Metadata}'s name; or create and register 
     * a new {@link Timer} if none is registered.
     * If a {@link Timer} was created, the provided {@link Metadata} object will be registered.
     * <p>
     * Note: The {@link Metadata} will not be updated if the metric is already registered.
     * </p>
     * 
     * @param metadata the name of the metric
     * @return a new or pre-existing {@link Timer}
     */
    public abstract Timer timer(Metadata metadata);
 


    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    public abstract boolean remove(String name);

    /**
     * Removes all metrics which match the given filter.
     *
     * @param filter a filter
     */
    public abstract void removeMatching(MetricFilter filter);


    /**
     * Returns a set of the names of all the metrics in the registry.
     *
     * @return the names of all the metrics
     */
    public abstract SortedSet<String> getNames();

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    public abstract SortedMap<String, Gauge> getGauges();

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the gauges in the registry
     */
    public abstract SortedMap<String, Gauge> getGauges(MetricFilter filter);

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    public abstract SortedMap<String, Counter> getCounters();

    /**
     * Returns a map of all the counters in the registry and their names which match the given
     * filter.
     *
     * @param filter    the metric filter to match
     * @return all the counters in the registry
     */
    public abstract SortedMap<String, Counter> getCounters(MetricFilter filter);

    /**
     * Returns a map of all the histograms in the registry and their names.
     *
     * @return all the histograms in the registry
     */
    public abstract SortedMap<String, Histogram> getHistograms();

    /**
     * Returns a map of all the histograms in the registry and their names which match the given
     * filter.
     *
     * @param filter    the metric filter to match
     * @return all the histograms in the registry
     */
    public abstract SortedMap<String, Histogram> getHistograms(MetricFilter filter);

    /**
     * Returns a map of all the meters in the registry and their names.
     *
     * @return all the meters in the registry
     */
    public abstract SortedMap<String, Meter> getMeters();

    /**
     * Returns a map of all the meters in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the meters in the registry
     */
    public abstract SortedMap<String, Meter> getMeters(MetricFilter filter);

    /**
     * Returns a map of all the timers in the registry and their names.
     *
     * @return all the timers in the registry
     */
    public abstract SortedMap<String, Timer> getTimers();

    /**
     * Returns a map of all the timers in the registry and their names which match the given filter.
     *
     * @param filter    the metric filter to match
     * @return all the timers in the registry
     */
    public abstract SortedMap<String, Timer> getTimers(MetricFilter filter);

    /**
     * Returns a map of all the metrics in the registry and their names.
     *
     * @return all the metrics in the registry
     */
    public abstract Map<String, Metric> getMetrics();

    /**
     * Returns a map of all the metadata in the registry and their names.
     *
     * @return all the metadata in the registry
     */
    public abstract Map<String, Metadata> getMetadata();
    
}

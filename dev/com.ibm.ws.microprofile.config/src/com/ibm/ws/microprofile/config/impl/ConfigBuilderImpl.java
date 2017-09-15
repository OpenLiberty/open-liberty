/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config.interfaces.DefaultConverters;
import com.ibm.ws.microprofile.config.interfaces.DefaultSources;

/**
 *
 */
public abstract class ConfigBuilderImpl implements ConfigBuilder {

    private static final TraceComponent tc = Tr.register(ConfigBuilderImpl.class);

    private final Map<Type, List<Converter<?>>> userConverters = new HashMap<>();
    private final TreeSet<ConfigSource> userSources = new TreeSet<>(ConfigSourceComparator.INSTANCE);
    private ClassLoader classloader;

    private boolean addDefaultSources = false;
    private boolean addDiscoveredSources = false;

    private boolean addDefaultConverters = false;
    private boolean addDiscoveredConverters = false;

    private final ScheduledExecutorService executor;

    /**
     * Constructor
     *
     * @param classLoader
     * @param executor
     */
    public ConfigBuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor) {
        this.classloader = classLoader;
        this.executor = executor;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder addDefaultSources() {
        synchronized (this) {
            this.addDefaultSources = true;
        }
        return this;
    }

    /**
     * @return this (builder pattern)
     */
    @Override
    public ConfigBuilder addDiscoveredSources() {
        synchronized (this) {
            this.addDiscoveredSources = true;
        }
        return this;
    }

    /**
     * @return this (builder pattern)
     */
    public ConfigBuilder addDefaultConverters() {
        synchronized (this) {
            this.addDefaultConverters = true;
        }
        return this;
    }

    /**
     * @return this (builder pattern)
     */
    @Override
    public ConfigBuilder addDiscoveredConverters() {
        synchronized (this) {
            this.addDiscoveredConverters = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder forClassLoader(ClassLoader loader) {
        synchronized (this) {
            if (loader == null) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "null.classloader.CWMCG0002E"));
            }
            this.classloader = loader;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder withSources(ConfigSource... sources) {
        synchronized (this) {
            for (ConfigSource source : sources) {
                addSource(source);
            }
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder withConverters(Converter<?>... converters) {
        synchronized (this) {
            for (Converter<?> con : converters) {
                addConverter(con);
            }
        }
        return this;
    }

    /////////////////////////////////////////////
    // ALL PROTECTED METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
    /////////////////////////////////////////////

    /**
     * Get the sources, default, discovered and user registered sources are
     * included as appropriate.
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return sources as a sorted set
     */
    protected SortedSources getSources() {
        SortedSources sources = new SortedSources(this.userSources);
        if (addDefaultSources) {
            sources.addAll(DefaultSources.getDefaultSources(getClassLoader()));
        }
        if (addDiscoveredSources) {
            sources.addAll(DefaultSources.getDiscoveredSources(getClassLoader()));
        }
        sources = sources.unmodifiable();
        return sources;
    }

    /**
     * Get the converters, default, discovered and user registered converters are
     * included as appropriate.
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return converters as a Map keyed on Type
     */
    protected Map<Type, Converter<?>> getConverters() {
        //the 1:1 map to be returned
        Map<Type, Converter<?>> converters = new HashMap<>();

        //a 1:many map of all converters. The nested converter list is in the order they were added.
        Map<Type, List<Converter<?>>> allConverters = new HashMap<>();

        //add the default converters
        if (addDefaultConverters) {
            addConverters(allConverters, DefaultConverters.getDefaultConverters());
        }
        //add the discovered converters
        if (addDiscoveredConverters) {
            addConverters(allConverters, DefaultConverters.getDiscoveredConverters(getClassLoader()));
        }
        //finally add the programatically added converters
        addConverterLists(allConverters, userConverters);

        //go through all of the converters
        //for each type, find the one with the highest priority
        //if more than one converter has the same priority then the one added last wins
        for (Map.Entry<Type, List<Converter<?>>> entry : allConverters.entrySet()) {
            Type type = entry.getKey();
            List<Converter<?>> typeConverters = entry.getValue();
            Converter<?> highest = null;
            int highestPriority = Integer.MIN_VALUE;
            if (typeConverters.size() == 1) {
                highest = typeConverters.get(0);
            } else {
                for (Converter<?> converter : typeConverters) {
                    int value = ConfigConstants.DEFAULT_CONVERTER_PRIORITY;
                    //get Priority annotations from class only, not from the super-class
                    Priority[] priorities = converter.getClass().getDeclaredAnnotationsByType(Priority.class);
                    if (priorities != null && priorities.length > 0) {
                        Priority priority = priorities[0];
                        value = priority.value();
                    }
                    if (value >= highestPriority) {
                        highest = converter;
                        highestPriority = value;
                    }
                }
            }

            //take this highest priority converter and put it in the final map
            if (highest != null) {
                converters.put(type, highest);
            }
        }

        converters = Collections.unmodifiableMap(converters);
        return converters;
    }

    /**
     * Get the ScheduledExecutorService
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return the executor
     */
    protected ScheduledExecutorService getScheduledExecutorService() {
        return executor;
    }

    protected long getRefreshInterval() {
        long refreshInterval = ConfigConstants.DEFAULT_DYNAMIC_REFRESH_INTERVAL;

        String refreshProp = getRefreshRateSystemProperty();
        if (refreshProp != null && !"".equals(refreshProp)) {
            refreshInterval = Long.parseLong(refreshProp);
        }

        if (refreshInterval > 0 && refreshInterval < ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL) {
            refreshInterval = ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL;
        }
        return refreshInterval;
    }

    /////////////////////////////////////////////
    // ALL PRIVATE METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
    /////////////////////////////////////////////

    private static String getRefreshRateSystemProperty() {
        String prop = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME);
            }
        });
        return prop;
    }

    /**
     * For each converter in the list call addConverter(converters, type, converter)
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @param converters
     * @param additions
     */
    private static void addConverterLists(Map<Type, List<Converter<?>>> converters, Map<Type, List<Converter<?>>> additions) {
        for (Map.Entry<Type, List<Converter<?>>> entry : additions.entrySet()) {
            Type type = entry.getKey();
            List<Converter<?>> converterList = entry.getValue();
            for (Converter<?> converter : converterList) {
                addConverter(converters, type, converter);
            }
        }
    }

    /**
     *
     * Call addConverter(converters, type, converter) for each converter in the additions entrySet
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @param converters
     * @param additions
     */
    private static void addConverters(Map<Type, List<Converter<?>>> converters, Map<Type, Converter<?>> additions) {
        for (Map.Entry<Type, Converter<?>> entry : additions.entrySet()) {
            Type type = entry.getKey();
            Converter<?> converter = entry.getValue();
            addConverter(converters, type, converter);
        }
    }

    /**
     * Call addConverter(converters, type, converter) for each converter in the additions entrySet
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @param converters
     * @param type
     * @param converter
     */
    private static void addConverter(Map<Type, List<Converter<?>>> converters, Type type, Converter<?> converter) {
        List<Converter<?>> converterList = converters.get(type);
        if (converterList == null) {
            converterList = new ArrayList<>();
            converters.put(type, converterList);
        }
        converterList.add(converter);
    }

    /**
     * Call this method only from within a 'synchronized(this) block
     *
     * @param source
     */
    private void addSource(ConfigSource source) {
        this.userSources.add(source);
    }

    /**
     * Call this method only from within a 'synchronized(this) block
     *
     * @param converter
     */
    private void addConverter(Converter<?> converter) {
        Type type = DefaultConverters.getConverterType(converter);
        addConverter(userConverters, type, converter);
    }

    /**
     * Call this method only from within a 'synchronized(this) block
     *
     * @return
     */
    private ClassLoader getClassLoader() {
        return classloader;
    }
}
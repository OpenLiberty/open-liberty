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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.converters.DefaultConverters;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.converters.UserConverter;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config.interfaces.DefaultSources;

/**
 *
 */
public abstract class ConfigBuilderImpl implements ConfigBuilder {

    private static final TraceComponent tc = Tr.register(ConfigBuilderImpl.class);

    private final PriorityConverterMap userConverters = new PriorityConverterMap();
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
                UserConverter<?> userConverter = UserConverter.newInstance(con);
                userConverters.addConverter(userConverter);
            }
        }
        return this;
    }

    public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        synchronized (this) {
            UserConverter<?> userConverter = UserConverter.newInstance(type, priority, converter);
            userConverters.addConverter(userConverter);
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
    protected PriorityConverterMap getConverters() {
        //the map to be returned
        PriorityConverterMap allConverters = new PriorityConverterMap();

        //add the default converters
        if (addDefaultConverters) {
            allConverters.addAll(getDefaultConverters());
        }
        //add the discovered converters
        if (addDiscoveredConverters) {
            allConverters.addAll(DefaultConverters.getDiscoveredConverters(getClassLoader()));
        }
        //finally add the programatically added converters
        allConverters.addAll(userConverters);

        allConverters.setUnmodifiable();

        return allConverters;
    }

    protected PriorityConverterMap getDefaultConverters() {
        return DefaultConverters.getDefaultConverters();
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
     * @return
     */
    protected ClassLoader getClassLoader() {
        return classloader;
    }
}
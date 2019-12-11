/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import java.util.SortedSet;
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
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.config.sources.DefaultSources;

/**
 *
 */
public abstract class AbstractConfigBuilder implements ConfigBuilder {

    private static final TraceComponent tc = Tr.register(AbstractConfigBuilder.class);

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
    public AbstractConfigBuilder(ClassLoader classLoader, ScheduledExecutorService executor) {
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
                addUserConverter(userConverter);
            }
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public WebSphereConfig build() {
        WebSphereConfig config = null;
        synchronized (this) {
            SortedSources sources = getSources();
            PriorityConverterMap converters = getConverters();
            ScheduledExecutorService executor = getScheduledExecutorService();
            long refreshInterval = getRefreshInterval();
            ClassLoader classLoader = getClassLoader();
            ConversionManager conversionManager = getConversionManager(converters, classLoader);

            config = buildConfig(conversionManager, sources, executor, refreshInterval);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDumpEnabled()) {
                Tr.debug(tc, "Config dump: {0}", config.dump());
            }
        }
        return config;
    }

    /////////////////////////////////////////////
    // ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
    /////////////////////////////////////////////

    protected void addUserConverter(UserConverter<?> userConverter) {
        this.userConverters.addConverter(userConverter);
    }

    /**
     * Construct a new WebSphereConfig object
     *
     * @param conversionManager
     * @param sources
     * @param executor
     * @param refreshInterval
     * @return
     */
    protected abstract WebSphereConfig buildConfig(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval);

    /**
     * Get the sources, default, discovered and user registered sources are
     * included as appropriate.
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return sources as a sorted set
     */
    protected SortedSources getSources() {
        SortedSources sources = new SortedSourcesImpl(getUserSources());
        if (addDefaultSourcesFlag()) {
            sources.addAll(DefaultSources.getDefaultSources(getClassLoader()));
        }
        if (addDiscoveredSourcesFlag()) {
            sources.addAll(DefaultSources.getDiscoveredSources(getClassLoader()));
        }
        sources = sources.unmodifiable();
        return sources;
    }

    protected SortedSet<ConfigSource> getUserSources() {
        return this.userSources;
    }

    protected boolean addDefaultSourcesFlag() {
        return this.addDefaultSources;
    }

    protected boolean addDiscoveredSourcesFlag() {
        return this.addDiscoveredSources;
    }

    protected boolean addDefaultConvertersFlag() {
        return this.addDefaultConverters;
    }

    protected boolean addDiscoveredConvertersFlag() {
        return this.addDiscoveredConverters;
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
        if (addDefaultConvertersFlag()) {
            allConverters.addAll(getDefaultConverters());
        }
        //add the discovered converters
        if (addDiscoveredConvertersFlag()) {
            allConverters.addAll(DefaultConverters.getDiscoveredConverters(getClassLoader()));
        }
        //finally add the programatically added converters
        allConverters.addAll(this.userConverters);

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
        return this.executor;
    }

    protected long getRefreshInterval() {
        long refreshInterval = ConfigConstants.DEFAULT_DYNAMIC_REFRESH_INTERVAL;

        String refreshProp = getRefreshRateSystemProperty();
        if ((refreshProp != null) && !"".equals(refreshProp)) {
            refreshInterval = Long.parseLong(refreshProp);
        }

        if ((refreshInterval > 0) && (refreshInterval < ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL)) {
            refreshInterval = ConfigConstants.MINIMUM_DYNAMIC_REFRESH_INTERVAL;
        }
        return refreshInterval;
    }

    private static String getRefreshRateSystemProperty() {
        String prop = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME));
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
        return this.classloader;
    }

    protected ConversionManager getConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        return new ConversionManager(converters, classLoader);
    }

    /////////////////////////////////////////////
    // ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
    /////////////////////////////////////////////
}
/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSourcesImpl;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.config13.impl.Config13BuilderImpl;
import com.ibm.ws.microprofile.config14.converters.Config14DefaultConverters;
import com.ibm.ws.microprofile.config14.sources.Config14DefaultSources;

public class Config14BuilderImpl extends Config13BuilderImpl implements ConfigBuilder {

    /**
     * Constructor
     *
     * @param classLoader           the classloader which scopes this config
     * @param executor              the executor to use for async update threads
     * @param internalConfigSources
     */
    public Config14BuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor, Set<ConfigSource> internalConfigSources) {
        super(classLoader, executor, internalConfigSources);
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

    @Override
    protected SortedSources getSources() {
        SortedSources sources = new SortedSourcesImpl(getUserSources());
        if (addDefaultSourcesFlag()) {
            sources.addAll(Config14DefaultSources.getDefaultSources(getClassLoader(), getScheduledExecutorService()));
        }
        if (addDiscoveredSourcesFlag()) {
            sources.addAll(Config14DefaultSources.getDiscoveredSources(getClassLoader()));
        }
        sources.addAll(getInternalConfigSources());
        sources = sources.unmodifiable();
        return sources;
    }

    @Override
    protected WebSphereConfig buildConfig(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        WebSphereConfig config = new Config14Impl(conversionManager, sources, executor, refreshInterval);
        return config;
    }

    @Override
    protected PriorityConverterMap getDefaultConverters() {
        return Config14DefaultConverters.getDefaultConverters();
    }

    @Override
    protected ConversionManager getConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        return new Config14ConversionManager(converters, classLoader);
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

}

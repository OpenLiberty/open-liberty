/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.impl;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSourcesImpl;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config12.impl.Config12BuilderImpl;
import com.ibm.ws.microprofile.config13.converters.Config13DefaultConverters;
import com.ibm.ws.microprofile.config13.sources.Config13DefaultSources;

public class Config13BuilderImpl extends Config12BuilderImpl implements ConfigBuilder {

    private final Set<ConfigSource> internalConfigSources;

    /**
     * Constructor
     *
     * @param classLoader           the classloader which scopes this config
     * @param executor              the executor to use for async update threads
     * @param internalConfigSources
     */
    public Config13BuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor, Set<ConfigSource> internalConfigSources) {
        super(classLoader, executor);
        this.internalConfigSources = internalConfigSources;
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

    /**
     * Get the sources, default, discovered and user registered sources are
     * included as appropriate.
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return sources as a sorted set
     */
    @Override
    protected SortedSources getSources() {
        SortedSources sources = new SortedSourcesImpl(getUserSources());
        if (addDefaultSourcesFlag()) {
            sources.addAll(Config13DefaultSources.getDefaultSources(getClassLoader()));
        }
        if (addDiscoveredSourcesFlag()) {
            sources.addAll(Config13DefaultSources.getDiscoveredSources(getClassLoader()));
        }
        sources.addAll(getInternalConfigSources());
        sources = sources.unmodifiable();
        return sources;
    }

    protected Set<ConfigSource> getInternalConfigSources() {
        return this.internalConfigSources;
    }

    @Override
    protected PriorityConverterMap getDefaultConverters() {
        return Config13DefaultConverters.getDefaultConverters();
    }

    @Override
    protected ConversionManager getConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        return new Config13ConversionManager(converters, classLoader);
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

}

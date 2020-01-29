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

import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.archaius.cache.ConfigCache;
import com.ibm.ws.microprofile.config.archaius.composite.CompositeConfig;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class ConfigImpl extends AbstractConfig implements WebSphereConfig {

    //the underlying composite config
    private final CompositeConfig composite;
    //a caching layer on top
    private final ConfigCache cache;

    /**
     * The sources passed in should have already been wrapped up as an unmodifiable copy
     *
     * @param sources
     * @param converters
     * @param executor
     */
    public ConfigImpl(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        super(conversionManager, sources);
        this.composite = new CompositeConfig(conversionManager, sources, executor, refreshInterval);
        //a config cache does the job of applying the type conversions and then caching those converted values
        this.cache = new ConfigCache(this.composite);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        super.close();
        this.composite.close();
    }

    /** {@inheritDoc} */
    @Override
    public SourcedValue getSourcedValue(String propertyName, Type propertyType) {
        return this.cache.getSourcedValue(propertyName, propertyType);
    }

    /** {@inheritDoc} */
    @Override
    protected Set<String> getKeySet() {
        //TODO cache the keys
        return this.composite.getKeySet();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String dump() {
        return this.composite.dump();
    }
}

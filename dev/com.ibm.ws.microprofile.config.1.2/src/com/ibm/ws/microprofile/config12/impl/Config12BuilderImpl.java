/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;

import com.ibm.ws.microprofile.config.archaius.impl.ArchaiusConfigBuilderImpl;
import com.ibm.ws.microprofile.config.archaius.impl.ConversionDecoder;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config12.converters.Config12DefaultConverters;

public class Config12BuilderImpl extends ArchaiusConfigBuilderImpl implements ConfigBuilder {

    /**
     * Constructor
     *
     * @param classLoader the classloader which scopes this config
     * @param executor the executor to use for async update threads
     */
    public Config12BuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor) {
        super(classLoader, executor);
    }

    @Override
    protected PriorityConverterMap getDefaultConverters() {
        return Config12DefaultConverters.getDefaultConverters();
    }

    @Override
    protected ConversionDecoder getConversionDecoder(PriorityConverterMap converters, ClassLoader classLoader) {
        return new Config12ConversionManager(converters, classLoader);
    }
}
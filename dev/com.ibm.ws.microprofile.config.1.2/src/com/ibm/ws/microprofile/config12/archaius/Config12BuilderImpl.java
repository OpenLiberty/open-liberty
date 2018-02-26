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
package com.ibm.ws.microprofile.config12.archaius;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config.archaius.ConfigBuilderImpl;
import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.converters.UserConverter;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config12.converters.Config12DefaultConverters;
import com.ibm.ws.microprofile.config12.impl.Config12ConversionManager;

public class Config12BuilderImpl extends ConfigBuilderImpl implements ConfigBuilder {

    /**
     * Constructor
     *
     * @param classLoader the classloader which scopes this config
     * @param executor the executor to use for async update threads
     */
    public Config12BuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor) {
        super(classLoader, executor);
    }

    //new api method for 1.2
    @Override
    public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        synchronized (this) {
            UserConverter<?> userConverter = UserConverter.newInstance(type, priority, converter);
            addUserConverter(userConverter);
        }
        return this;
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

    @Override
    protected PriorityConverterMap getDefaultConverters() {
        return Config12DefaultConverters.getDefaultConverters();
    }

    @Override
    protected ConversionManager getConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        return new Config12ConversionManager(converters, classLoader);
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

}
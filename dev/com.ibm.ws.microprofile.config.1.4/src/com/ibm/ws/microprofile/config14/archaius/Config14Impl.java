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
package com.ibm.ws.microprofile.config14.archaius;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigSnapshot;

import com.ibm.ws.microprofile.config.archaius.ConfigImpl;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;
import com.ibm.ws.microprofile.config14.impl.ConfigAccessorImpl;
import com.ibm.ws.microprofile.config14.impl.ConfigSnapshotImpl;

public class Config14Impl extends ConfigImpl implements WebSphereConfig, Config {

    /**
     * The sources passed in should have already been wrapped up as an unmodifiable copy
     *
     * @param sources
     * @param converters
     * @param executor
     */
    public Config14Impl(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        super(conversionManager, sources, executor, refreshInterval);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigAccessor<String> access(String propertyName) {
        ConfigAccessor<String> accessor = new ConfigAccessorImpl<>(this, propertyName, String.class);
        return accessor;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigSnapshot snapshotFor(ConfigAccessor<?>... configValues) {
        return new ConfigSnapshotImpl(configValues);
    }
}

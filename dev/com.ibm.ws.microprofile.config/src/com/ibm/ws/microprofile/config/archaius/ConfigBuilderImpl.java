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
package com.ibm.ws.microprofile.config.archaius;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;

import com.ibm.ws.microprofile.config.converters.PriorityConverterMap;
import com.ibm.ws.microprofile.config.impl.AbstractConfigBuilder;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;

public class ConfigBuilderImpl extends AbstractConfigBuilder implements ConfigBuilder {

    /**
     * Constructor
     *
     * @param classLoader
     * @param executor
     */
    public ConfigBuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor) {
        super(classLoader, executor);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigImpl build() {
        ConfigImpl config = null;
        synchronized (this) {
            SortedSources sources = getSources();
            PriorityConverterMap converters = getConverters();
            ScheduledExecutorService executor = getScheduledExecutorService();
            long refreshInterval = getRefreshInterval();

            config = build(sources, converters, executor, refreshInterval);
        }
        return config;
    }

    //If Java 2 security is turned on then somewhere in the constructor for ArchaiusConfigImpl (probably one of the archaius super-classes)
    //it needs to have permission for "accessClassInPackage.com.ibm.oti.shared"
    //I don't know why and I don't much like putting a doPriv around a constructor with 3rd party code involved
    //The exception was...
    //
    //   java.lang.SecurityException: Exception creating permissions: class com.ibm.oti.shared.SharedClassPermission: Access denied ("java.lang.RuntimePermission" "accessClassInPackage.com.ibm.oti.shared")
    //
    //https://www.ibm.com/support/knowledgecenter/SSYKE2_6.0.0/com.ibm.java.api.60.doc/com.ibm.oti.shared/com/ibm/oti/shared/SharedClassPermission.html
    private ConfigImpl build(SortedSources sources, PriorityConverterMap converters, ScheduledExecutorService executor, long refreshInterval) {
        ConfigImpl config = AccessController.doPrivileged(new PrivilegedAction<ConfigImpl>() {
            @Override
            public ConfigImpl run() {
                return new ConfigImpl(getConversionManager(converters, getClassLoader()), sources, executor, refreshInterval);
            }
        });
        return config;
    }

    protected ConversionManager getConversionManager(PriorityConverterMap converters, ClassLoader classLoader) {
        return new ConversionManager(converters, classLoader);
    }
}
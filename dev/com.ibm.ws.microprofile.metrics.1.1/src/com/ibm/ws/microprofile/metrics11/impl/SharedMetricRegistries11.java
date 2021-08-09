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
package com.ibm.ws.microprofile.metrics11.impl;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

/**
 * A map of shared, named metric registries.
 */
@Component(service = SharedMetricRegistries.class, immediate = true)
public class SharedMetricRegistries11 extends SharedMetricRegistries {

    private ConfigProviderResolver configResolver;

    @Override
    public MetricRegistry getOrCreate(String name) {
        final MetricRegistry existing = SharedMetricRegistries.REGISTRIES.get(name);
        if (existing == null) {
            final MetricRegistry created = new MetricRegistry11Impl(configResolver);
            final MetricRegistry raced = add(name, created);
            if (raced == null) {
                return created;
            }
            return raced;
        }
        return existing;
    }

    @Reference(service = ConfigProviderResolver.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setConfigProvider(ConfigProviderResolver configResolver) {
        this.configResolver = configResolver;
    }

}

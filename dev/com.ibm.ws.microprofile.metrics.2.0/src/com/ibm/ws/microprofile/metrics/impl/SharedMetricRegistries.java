/*******************************************************************************
* Copyright (c) 2017, 2020 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
*******************************************************************************
* Copyright 2010-2013 Coda Hale and Yammer, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.ibm.ws.microprofile.metrics.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * A map of shared, named metric registries.
 */
@Component(service = SharedMetricRegistries.class, immediate = true)
public class SharedMetricRegistries {
    protected ConfigProviderResolver configResolver;

    protected static final ConcurrentMap<String, MetricRegistry> REGISTRIES = new ConcurrentHashMap<String, MetricRegistry>();

    public static void clear() {
        REGISTRIES.clear();
    }

    public static Set<String> names() {
        return REGISTRIES.keySet();
    }

    public static void remove(String key) {
        REGISTRIES.remove(key);
    }

    public MetricRegistry add(String name, MetricRegistry registry) {
        return REGISTRIES.putIfAbsent(name, registry);
    }

    public MetricRegistry getOrCreate(String name) {
        final MetricRegistry existing = SharedMetricRegistries.REGISTRIES.get(name);
        if (existing == null) {
            final MetricRegistry created = createNewMetricRegsitry(configResolver);
            final MetricRegistry raced = add(name, created);
            if (raced == null) {
                return created;
            }
            return raced;
        }
        return existing;
    }

    public void associateMetricIDToApplication(MetricID metricID, String appName, MetricRegistry registry) {
        if (MetricRegistryImpl.class.isInstance(registry)) {
            MetricRegistryImpl metricRegistryImpl = (MetricRegistryImpl) registry;
            metricRegistryImpl.addNameToApplicationMap(metricID, appName);
        }

    }

    @Reference(service = ConfigProviderResolver.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setConfigProvider(ConfigProviderResolver configResolver) {
        this.configResolver = configResolver;
    }

    protected MetricRegistry createNewMetricRegsitry(ConfigProviderResolver configResolver) {
        return new MetricRegistryImpl(configResolver);
    }

}

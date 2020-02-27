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
package com.ibm.ws.microprofile.metrics.cdi23.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@ApplicationScoped
public class MetricRegistryFactory {

    public static SharedMetricRegistries SHARED_METRIC_REGISTRIES;

    @Produces
    @ApplicationScoped
    public static MetricRegistry getDefaultRegistry() {
        return SHARED_METRIC_REGISTRIES.getOrCreate(MetricRegistry.Type.APPLICATION.getName());
    }

    @Produces
    @ApplicationScoped
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    public static MetricRegistry getApplicationRegistry() {
        return SHARED_METRIC_REGISTRIES.getOrCreate(MetricRegistry.Type.APPLICATION.getName());
    }

    @Produces
    @ApplicationScoped
    @RegistryType(type = MetricRegistry.Type.BASE)
    public static MetricRegistry getBaseRegistry() {
        return SHARED_METRIC_REGISTRIES.getOrCreate(MetricRegistry.Type.BASE.getName());
    }

    @Produces
    @ApplicationScoped
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    public static MetricRegistry getVendorRegistry() {
        return SHARED_METRIC_REGISTRIES.getOrCreate(MetricRegistry.Type.VENDOR.getName());
    }

}

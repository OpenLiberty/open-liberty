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
package com.ibm.ws.microprofile.metrics23.impl;

import java.util.SortedMap;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.ws.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistry23Impl extends MetricRegistryImpl {

    /**
     * @param configResolver
     */
    public MetricRegistry23Impl(ConfigProviderResolver configResolver) {
        super(configResolver);
    }

    @Override
    public SimpleTimer simpleTimer(String name) {
        return this.simpleTimer(name, null);
    }

    @Override
    public SimpleTimer simpleTimer(String name, Tag... tags) {
        Metadata metadata = Metadata.builder().withName(name).withType(MetricType.SIMPLE_TIMER).build();

        if (metadataMID.keySet().contains(name)) {
            metadata = metadataMID.get(name);

            if (!metadata.getTypeRaw().equals(MetricType.SIMPLE_TIMER)) {
                throw new IllegalArgumentException(name + " is already used for a different type of metric");
            }
        }
        return this.simpleTimer(metadata, tags);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return this.simpleTimer(metadata, null);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return getOrAdd(metadata, MetricBuilder23.SIMPLE_TIMER, tags);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return getSimpleTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter filter) {
        return getMetrics(SimpleTimer.class, filter);
    }

    @Override
    protected <T extends Metric> Class<T> determineMetricClass(T metric) {
        if (SimpleTimer.class.isInstance(metric))
            return (Class<T>) SimpleTimer.class;
        else {
            return super.determineMetricClass(metric);
        }
    }

    protected interface MetricBuilder23<T extends Metric> extends MetricBuilder<Metric> {
        MetricBuilder<SimpleTimer> SIMPLE_TIMER = new MetricBuilder<SimpleTimer>() {
            @Override
            public SimpleTimer newMetric() {
                return new SimpleTimerImpl();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return SimpleTimer.class.isInstance(metric);
            }
        };
    }
}

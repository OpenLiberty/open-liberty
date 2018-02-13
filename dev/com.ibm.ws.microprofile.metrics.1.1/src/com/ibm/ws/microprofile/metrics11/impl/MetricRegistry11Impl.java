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

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

import com.ibm.ws.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * A registry of metric instances.
 */
@Vetoed
public class MetricRegistry11Impl extends MetricRegistryImpl {

    /**
     * Creates a new {@link MetricRegistry}.
     */
    public MetricRegistry11Impl() {
        super();
    }

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        // For MP Metrics 1.0, MetricType.from(Class in) does not support lambdas or proxy classes
        return register(new Metadata(name, from(metric)), metric);
    }

    @Override
    @Deprecated
    public <T extends Metric> T register(String name, T metric, Metadata metadata) throws IllegalArgumentException {
        return register(metadata, metric);
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
        //Create Copy of Metadata object so it can't be changed after its registered
        Metadata metadataCopy = new Metadata(metadata.getName(), metadata.getDisplayName(), metadata.getDescription(), metadata.getTypeRaw(), metadata.getUnit());
        for (String tag : metadata.getTags().keySet()) {
            metadataCopy.getTags().put(tag, metadata.getTags().get(tag));
        }
        metadataCopy.setReusable(metadata.isReusable());
        final Metric existing = metrics.putIfAbsent(metadata.getName(), metric);
        this.metadata.putIfAbsent(metadata.getName(), metadataCopy);
        if (existing == null) {
        } else {
            throw new IllegalArgumentException("A metric named " + metadata.getName() + " already exists");
        }
        addNameToApplicationMap(metadata.getName());
        return metric;
    }

}

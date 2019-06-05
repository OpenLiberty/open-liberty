/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.impl;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.microprofile.config.impl.AbstractConfigBuilder;
import com.ibm.ws.microprofile.config12.impl.Config12ProviderResolverImpl;

public class Config13ProviderResolverImpl extends Config12ProviderResolverImpl {

    private final HashSet<ConfigSource> internalConfigSources = new HashSet<>();

    /** {@inheritDoc} */
    @Override
    protected AbstractConfigBuilder newBuilder(ClassLoader classLoader) {
        return new Config13BuilderImpl(classLoader, getScheduledExecutorService(), getInternalConfigSources());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setConfigSource(ConfigSource configSource) {
        synchronized (internalConfigSources) {
            internalConfigSources.add(configSource);
        }
    }

    protected void unsetConfigSource(ConfigSource configSource) {
        synchronized (internalConfigSources) {
            internalConfigSources.remove(configSource);
        }
    }

    protected Set<ConfigSource> getInternalConfigSources() {
        Set<ConfigSource> sources = new HashSet<>();
        synchronized (internalConfigSources) {
            sources.addAll(internalConfigSources);
        }
        return sources;
    }
}

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
package com.ibm.ws.microprofile.config13.archaius;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigBuilder;

import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config12.archaius.Config12BuilderImpl;
import com.ibm.ws.microprofile.config13.sources.Config13DefaultSources;

public class Config13BuilderImpl extends Config12BuilderImpl implements ConfigBuilder {

    /**
     * Constructor
     *
     * @param classLoader the classloader which scopes this config
     * @param executor the executor to use for async update threads
     */
    public Config13BuilderImpl(ClassLoader classLoader, ScheduledExecutorService executor) {
        super(classLoader, executor);
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

    /**
     * Get the sources, default, discovered and user registered sources are
     * included as appropriate.
     *
     * Call this method only from within a 'synchronized(this) block
     *
     * @return sources as a sorted set
     */
    @Override
    protected SortedSources getSources() {
        SortedSources sources = new SortedSources(getUserSources());
        if (addDefaultSourcesFlag()) {
            sources.addAll(Config13DefaultSources.getDefaultSources(getClassLoader()));
        }
        if (addDiscoveredSourcesFlag()) {
            sources.addAll(Config13DefaultSources.getDiscoveredSources(getClassLoader()));
        }
        sources = sources.unmodifiable();
        return sources;
    }

/////////////////////////////////////////////
// ALL NON-PUBLIC METHODS MUST ONLY BE CALLED FROM WITHIN A 'synchronized(this)' BLOCK
/////////////////////////////////////////////

}

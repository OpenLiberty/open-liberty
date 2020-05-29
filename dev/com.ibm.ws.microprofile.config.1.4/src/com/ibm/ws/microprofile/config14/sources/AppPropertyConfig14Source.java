/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.sources;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.config.serverxml.AppPropertyConfigSource;
import com.ibm.ws.microprofile.config14.impl.TimedCache;

/**
 *
 */
public class AppPropertyConfig14Source extends AppPropertyConfigSource {

    private static final String CACHE_KEY = "properties";

    private final ScheduledExecutorService executor;
    private final TimedCache<String, Map<String, String>> cache;

    public AppPropertyConfig14Source(ScheduledExecutorService executor) {
        this.executor = executor;
        this.cache = new TimedCache<>(executor, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<String, String> getProperties() {
        return cache.get(CACHE_KEY, (k -> super.getProperties()));
    }

    public void close() {
        cache.close();
    }

}

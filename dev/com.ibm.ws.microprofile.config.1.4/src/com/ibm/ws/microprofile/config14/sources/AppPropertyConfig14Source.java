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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.ws.microprofile.config13.sources.AppPropertyConfigSource;

/**
 *
 */
public class AppPropertyConfig14Source extends AppPropertyConfigSource {

    private final AtomicReference<Map<String, String>> cachedValues = new AtomicReference<>();
    private final AtomicLong lastUpdated = new AtomicLong();
    private static final long CACHE_TIME = Duration.of(500, ChronoUnit.MILLIS).toNanos();

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> result = null;

        if (System.nanoTime() - lastUpdated.get() < CACHE_TIME) {
            result = cachedValues.get();
        }

        if (result == null) {
            result = super.getProperties();
            cachedValues.set(result);
            lastUpdated.set(System.nanoTime());
        }

        return result;
    }

}

/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.customSources.test;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class HashMapConfigSource extends AbstractConfigSource implements ConfigSource {

    private final ConcurrentMap<String, String> properties;

    public HashMapConfigSource(ConcurrentMap<String, String> properties, int ordinal, String id) {
        super(ordinal, id);
        this.properties = properties;
    }

    @Override
    public ConcurrentMap<String, String> getProperties() {
        return properties;
    }

    public static HashMapConfigSource newInstance(ConcurrentMap<String, String> properties, String id) {
        int ordinal = 100;
        HashMapConfigSource source = new HashMapConfigSource(properties, ordinal, id);
        return source;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}

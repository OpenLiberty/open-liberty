/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class CustomSource implements ConfigSource {

    public ConcurrentMap<String, String> props;
    public int ordinal = 700;
    public String id = "mySource";

    public CustomSource() {
        props = new ConcurrentHashMap<String, String>();
    }

    public CustomSource put(String key, String value) {
        props.put(key, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}
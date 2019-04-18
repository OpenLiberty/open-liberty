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
package com.ibm.ws.microprofile.config;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public abstract class AbstractConfigSource extends HashMap<String, String> implements ConfigSource {

    private final int ordinal;
    private final String name;

    public AbstractConfigSource(int ordinal, String id) {
        this.ordinal = ordinal;
        this.name = id;
    }

    public AbstractConfigSource(Map<String, String> properties, int ordinal, String id) {
        this(ordinal, id);
        putAll(properties);
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getProperties() {
        return this;
    }
}

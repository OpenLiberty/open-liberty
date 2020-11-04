/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class DiscoveredConfigSource extends HashMap<String, String> implements ConfigSource {

    public DiscoveredConfigSource() {
        super();
        put("PARENT_KEY", "parent");
        put("DISCOVERED_KEY", "DISCOVERED_VALUE");
        put("DOG_KEY", "Bob");
        put("NULL_KEY", null);
        put("PIZZA_KEY", "cheese"); //this resolve resolves to a null value as the size portion is missing
        put("PIZZA_GOOD_KEY", "ham;9"); //this is the right pizza key
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>(this);
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "DiscoveredConfigSource";
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

}

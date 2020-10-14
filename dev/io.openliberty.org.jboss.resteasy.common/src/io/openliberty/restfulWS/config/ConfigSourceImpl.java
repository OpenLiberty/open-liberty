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
package io.openliberty.restfulWS.config;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class ConfigSourceImpl implements ConfigSource {
    static final boolean java2SecurityEnabled = System.getSecurityManager() != null;
    private final Config config;
    
    ConfigSourceImpl(Config config) {
        this.config = config;
    }
    @Override
    public String getName() {
        return "Internal ConfigSource for RESTEasy implementation";
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        for (String prop : config.getPropertyNames()) {
            map.put(prop, config.getValue(prop, String.class));
        }
        return map;
    }

    @Override
    public String getValue(String key) {
        return config.getValue(key, String.class);
    }
}
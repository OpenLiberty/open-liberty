/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.options;

import java.util.Map;

import io.openliberty.transport.config.options.EndpointOption;
import io.openliberty.transport.config.options.EndpointOption.ConfigType;

/**
 * Enumeration of HTTP header configuration options.
 * Each constant in this enum represents a specific HTTP endpoint header configuration option
 * with its key, default value, and value type.
 */
public enum HeaderOption implements EndpointOption{

    ADD("add", null, String[].class, ConfigType.HEADERS),
    SET("set", null, String[].class, ConfigType.HEADERS),
    SET_IF_MISSING("setIfMissing", null, String[].class, ConfigType.HEADERS),
    REMOVE("remove", null, String[].class, ConfigType.HEADERS);

    private final String key;
    private final Object defaultValue;
    private final Class<?> valueType;
    private final ConfigType configType;
    
    HeaderOption(String key, Object defaultValue, Class<?> valueType, ConfigType configType) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        this.configType = configType;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Class<?> getValueType() {
        return  valueType;
    }

    @Override
    public ConfigType getConfigType() {
        return configType;
    }
}

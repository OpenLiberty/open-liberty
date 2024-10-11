/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.options;

import java.util.Map;

import io.openliberty.http.options.EndpointOption.ConfigType;
import io.openliberty.http.utils.HttpConfigUtils;

/**
 * Enumeration of HTTP compression configuration options.
 * Each constant in this enum represents a specific HTTP endpoint compression configuration option
 * with its key, default value, and value type.
 */
public enum CompressionOption implements EndpointOption {

    TYPES("types", new String[]{"text/*", "application/javascript"}, String[].class, ConfigType.COMPRESSION),
    SERVER_PREFERRED_ALGORITHM("serverPreferredAlgorithm", "none", String.class, ConfigType.COMPRESSION);

    private final String key;
    private final Object defaultValue;
    private final Class<?> valueType;
    private final ConfigType configType;

    CompressionOption(String key, Object defaultValue, Class<?> valueType, ConfigType configType) {
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T parse(Map<String, Object> config) {
        return(T) HttpConfigUtils.getOptionValue(config, this);
    }
}

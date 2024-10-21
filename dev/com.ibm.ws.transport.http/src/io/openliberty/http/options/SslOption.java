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
 * Enumeration of SSL configuration options.
 * Each constant in this enum respresents a specific HTTP endpoint SSL configuration option
 * with its key, default value, and value type.
 */
public enum SslOption implements EndpointOption{

    SESSION_TIMEOUT("sessionTimeout", 86400, Integer.class, ConfigType.SSL),
    SSL_SESSION_TIMEOUT("sslSessionTimeout", 86400000, Integer.class, ConfigType.SSL),
    SUPPRESS_HANDSHAKE_ERRORS("suppressHandshakeErrors", true, Boolean.class, ConfigType.SSL),
    SUPPRESS_HANDSHAKE_ERRORS_COUNT("suppressHandshakeErrorsCount", 100, Integer.class, ConfigType.SSL);
    

    private final String key;
    private final Object defaultValue;
    private final Class<?> valueType;
    private final ConfigType configType;

    SslOption(String key, Object defaultValue, Class<?> valueType, ConfigType configType) {
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

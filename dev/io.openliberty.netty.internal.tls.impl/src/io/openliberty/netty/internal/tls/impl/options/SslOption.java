/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tls.impl.options;

import java.util.Map;

import io.openliberty.transport.config.options.EndpointOption;

/**
 * Enumeration of SSL configuration options.
 * Each constant in this enum respresents a specific endpoint SSL configuration option
 * with its key, default value, and value type.
 */
public enum SslOption implements EndpointOption{

    SESSION_TIMEOUT("sessionTimeout", 86400, Integer.class, ConfigType.SSL),
    SSL_SESSION_TIMEOUT("sslSessionTimeout", 86400000, Integer.class, ConfigType.SSL),
    SUPPRESS_HANDSHAKE_ERRORS("suppressHandshakeErrors", true, Boolean.class, ConfigType.SSL),
    SUPPRESS_HANDSHAKE_ERRORS_COUNT("suppressHandshakeErrorsCount", 100, Long.class, ConfigType.SSL),
    ENFORCE_CIPHER_ORDER("com.ibm.ws.ssl.enforceCipherOrder", false, Boolean.class, ConfigType.SSL);
    

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
}

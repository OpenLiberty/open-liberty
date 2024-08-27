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
 * Enumeration of HTTP configuration options.
 * Each constant in this enum respresents a specific HTTP endpoint configuration option
 * with its key, default value, and value type.
 */
public enum HttpOption implements EndpointOption {

    KEEP_ALIVE_ENABLED("keepAliveEnabled", false, Boolean.class, ConfigType.HTTP),
    MAX_KEEP_ALIVE_REQUESTS("maxKeepAliveRequests", -1, Integer.class, ConfigType.HTTP),
    PERSIST_TIMEOUT("persistTimeout", "30s", String.class, ConfigType.HTTP),
    READ_TIMEOUT("readTimeout", "60s", String.class, ConfigType.HTTP),
    WRITE_TIMEOUT("writeTimeout", "60s", String.class, ConfigType.HTTP),
    REMOVE_SERVER_HEADER("removeServerHeader", false, Boolean.class, ConfigType.HTTP),  
    NO_CACHE_COOKIES_CONTROL("NoCacheCookiesControl", true, Boolean.class, ConfigType.HTTP),
    AUTO_DECOMPRESSION("AutoDecompression", true, Boolean.class, ConfigType.HTTP),
    LIMIT_NUM_HEADERS("limitNumHeaders", 500, Integer.class, ConfigType.HTTP),
    LIMIT_FIELD_SIZE("limitFieldSize", 32768, Integer.class, ConfigType.HTTP),
    DO_NOT_ALLOW_DUPLICATE_SET_COOKIES("DoNotAllowDuplicateSetCookies", "false", String.class, ConfigType.HTTP),
    MESSAGE_SIZE_LIMIT("MessageSizeLimit", -1L, Long.class, ConfigType.HTTP),
    INCOMING_BODY_BUFFER_SIZE("incomingBodyBufferSize", 32768, Integer.class, ConfigType.HTTP),
    THROW_IOE_FOR_INBOUND_CONNECTIONS("ThrowIOEForInboundConnections", null, Boolean.class, ConfigType.HTTP),
    DECOMPRESSION_RATIO_LIMIT("decompressionRatioLimit", 200, Integer.class, ConfigType.HTTP),
    DECOMPRESSION_TOLERANCE("decompressionTolerance", 3, Integer.class, ConfigType.HTTP),
    HTTP2_CONNECTION_IDLE_TIMEOUT("http2ConnectionIdleTimeout", "0", String.class, ConfigType.HTTP2),
    MAX_CONCURRENT_STREAMS("maxConcurrentStreams", 100, Integer.class, ConfigType.HTTP2),
    MAX_FRAME_SIZE("maxFrameSize", 57344, Integer.class, ConfigType.HTTP2),
    SETTINGS_INITIAL_WINDOW_SIZE("settingsInitialWindowSize", 65535, Integer.class, ConfigType.HTTP2),
    CONNECTION_WINDOW_SIZE("connectionWindowSize", 65535, Integer.class, ConfigType.HTTP2),
    LIMIT_WINDOW_UPDATE_FRAMES("limitWindowUpdateFrames", false, Boolean.class, ConfigType.HTTP2),
    MAX_RESET_FRAMES("maxResetFrames", 100, Integer.class, ConfigType.HTTP2),
    RESET_FRAMES_WINDOW("resetFramesWindow", "30s", String.class, ConfigType.HTTP2),
    MAX_STREAMS_REFUSED("maxStreamsRefused", 100, Integer.class, ConfigType.HTTP2);
    
    
    private final String key;
    private final Object defaultValue;
    private final Class<?> valueType;
    private final ConfigType configType;

    HttpOption(String key, Object defaultValue, Class<?> valueType, ConfigType configType) {
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
    

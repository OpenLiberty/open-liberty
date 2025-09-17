/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 
public enum TcpOption implements EndpointOption{

    ACCEPT_THREAD("acceptThread", false, Boolean.class, ConfigType.TCP),
    ADDRESS_EXCLUDE_LIST("addressExcludeList", "" , String.class, ConfigType.TCP),
    ADDRESS_INCLUDE_LIST("addressIncludeList", "", String.class, ConfigType.TCP),
    HOST_NAME_EXCLUDE_LIST("hostNameExcludeList", "", String.class, ConfigType.TCP),
    HOST_NAME_INCLUDE_LIST("hostNameIncludeList", "", String.class, ConfigType.TCP),
    INACTIVITY_TIMEOUT("inactivityTimeout", 60000, Integer.class, ConfigType.TCP),
    MAX_OPEN_CONNECTIONS("maxOpenConnections", 128000, Integer.class, ConfigType.TCP),
    PORT_OPEN_RETRIES("portOpenRetries", 0, Integer.class, ConfigType.TCP),
    SO_LINGER("soLinger", -1, Integer.class, ConfigType.TCP),
    SO_REUSE_ADDR("soReuseAddr", true, Boolean.class, ConfigType.TCP),
    WAIT_TO_ACCEPT("waitToAccept", false, Boolean.class, ConfigType.TCP);


    private final String key;
    private final Object defaultValue;
    private final Class<?> valueType;
    private final ConfigType configType;

    TcpOption(String key, Object defaultValue, Class<?> valueType, ConfigType configType){
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

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

/**
 * Represents a configuration option for an HTTP endpoint.
 * This interface defines the structure for all types of options
 * (HTTP, HTTP/2, SSL, Compression, etc.) used in endpoint configuration.
 * 
 * @param <T> The type of the option's value
 */
public interface EndpointOption {

    /**
     * Enum representing different types of configurations.
     */
    enum ConfigType{
        HTTP,
        HTTP2,
        SSL,
        TCP,
        COMPRESSION,
        HEADERS,
        REMOTE_IP,
        SAMESITE
    }

    /**
     * Gets the key used to identify this option in configuration maps.
     * 
     * @return The configuration key as a String
     */
    String getKey();

    /**
     * Gets the default value for this option.
     * 
     * @return The default value of type T
     */
    Object getDefaultValue();

    /**
     * Gets the class of the option's value type.
     * 
     * @return The Class object representing the type T
     */
    Class<?> getValueType();

    /**
     * Gets the configuration type this option belongs to.
     * 
     * @return The ConfigType enum value
     */
    ConfigType getConfigType();

    /**
     * Parses the value for this option from the given configuration map.
     * 
     * @param config The configuration map containing all options
     * @return The parsed value of type T
     */
    <T> T parse(Map<String, Object> config);

}

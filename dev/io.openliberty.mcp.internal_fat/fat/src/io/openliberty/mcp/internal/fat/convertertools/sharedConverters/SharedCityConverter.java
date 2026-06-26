/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.convertertools.sharedConverters;

import java.util.logging.Logger;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Shared converter for City objects, accessible from EAR/lib to all WAR modules.
 * Can be overridden by module-specific converters.
 */
@ApplicationScoped
public class SharedCityConverter implements DefaultValueConverter<City> {
    
    private static final Logger LOG = Logger.getLogger(SharedCityConverter.class.getName());

    /**
     * Converts a default value string in the format "Name::Country::Population" to a City object.
     * Example: "London::UK::9000000"
     */
    @Override
    public City convert(String defaultValue) {
        String[] parts = defaultValue.split("::");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid city format. Expected: Name::Country::Population");
        }
        LOG.info("[SharedCityConverter] Converting city using SHARED converter from EAR/lib");
        return new City(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}

// Made with Bob

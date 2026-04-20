/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.convertertools.customConverterModule;

import java.util.logging.Logger;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import io.openliberty.mcp.internal.fat.convertertools.sharedConverters.City;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Module1-specific City converter that OVERRIDES the shared converter.
 * Adds "Module1-" prefix to city names to demonstrate module isolation.
 */
@ApplicationScoped
public class Module1CityConverter implements DefaultValueConverter<City> {
    
    private static final Logger LOG = Logger.getLogger(Module1CityConverter.class.getName());

    /**
     * Converts a default value string in the format "Name::Country::Population" to a City object.
     * Adds "Module1-" prefix to demonstrate this is the module-specific converter.
     * Example: "London::UK::9000000" becomes City("Module1-London", "UK", 9000000)
     */
    @Override
    public City convert(String defaultValue) {
        String[] parts = defaultValue.split("::");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid city format. Expected: Name::Country::Population");
        }
        LOG.info("[Module1CityConverter] Converting city using MODULE1-SPECIFIC converter (overrides shared)");
        return new City("Module1-" + parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}

// Made with Bob

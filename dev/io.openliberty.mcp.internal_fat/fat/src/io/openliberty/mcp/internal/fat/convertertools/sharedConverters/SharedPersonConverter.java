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
 * Shared converter for Person objects, accessible from EAR/lib to all WAR modules.
 * This converter is NOT overridden by any module-specific converter.
 */
@ApplicationScoped
public class SharedPersonConverter implements DefaultValueConverter<Person> {
    
    private static final Logger LOG = Logger.getLogger(SharedPersonConverter.class.getName());

    /**
     * Converts a default value string in the format "Name::Age" to a Person object.
     * Example: "John::30"
     */
    @Override
    public Person convert(String defaultValue) {
        String[] parts = defaultValue.split("::");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid person format. Expected: Name::Age");
        }
        LOG.info("[SharedPersonConverter] Converting person using SHARED converter from EAR/lib");
        return new Person(parts[0], Integer.parseInt(parts[1]));
    }
}

// Made with Bob

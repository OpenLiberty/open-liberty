/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

@ApplicationScoped
public class ConverterRegistry {

    private static ConverterRegistry staticInstance = null;
    private static Map<Type, List<DefaultValueConverter<?>>> converters = new HashMap<>();

    public static ConverterRegistry get() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getConverterRegistry();
    }

    /**
     * For unit testing only
     *
     * @param converterRegistry
     */
    public static void set(ConverterRegistry converterRegistry) {
        staticInstance = converterRegistry;
    }

    public static Optional<DefaultValueConverter<?>> getConverter(Type type) {
        List<DefaultValueConverter<?>> convertersForType = converters.get(type);
        return Optional.ofNullable(convertersForType != null ? convertersForType.get(0) : null);
    }

    public void addConverter(Type type, DefaultValueConverter<?> converter) {
        converters.computeIfAbsent(type, k -> new ArrayList<>()).add(converter);
    }

    public static Collection<List<DefaultValueConverter<?>>> getAllCustomConverters() {
        return converters.values();
    }

}

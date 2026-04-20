/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import io.openliberty.mcp.internal.requests.BuiltinDefaultValueConverters;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.spi.CreationalContext;

public class ConverterRegistry {

    private static final int DEFAULT_CONVERTER_PRIORITY = 0;

    private Map<Type, List<DefaultValueConverter<?>>> convertersMap;
    private CreationalContext<?> context;
    private final ConverterRegistry globalRegistry;

    /**
     * Constructor for module registries (receives global reference)
     */
    public ConverterRegistry(ConverterRegistry globalRegistry) {
        this.globalRegistry = globalRegistry;
        this.convertersMap = new HashMap<>();
    }

    /**
     * Constructor for the global registry (no parent registry)
     * Automatically registers built-in converters for primitive types.
     * Built-in converters are ONLY in the global registry - modules access them via fallback.
     */
    public ConverterRegistry() {
        this.globalRegistry = null;
        this.convertersMap = new HashMap<>();
        
        // Register built-in converters ONLY in global registry (not CDI beans, framework-provided)
        for (Map.Entry<Type, DefaultValueConverter<?>> entry : BuiltinDefaultValueConverters.CONVERTERS.entrySet()) {
            convertersMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
    }

    public void registerConverters(Map<Type, List<DefaultValueConverter<?>>> newConverters, CreationalContext<?> context) {
        this.context = context;
        
        // Merge new converters into existing map (preserving built-in converters)
        for (Map.Entry<Type, List<DefaultValueConverter<?>>> entry : newConverters.entrySet()) {
            this.convertersMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
        
        sortConverters();
    }

    /**
     * Sort the registered converters for a type by priority (highest first, descending)
     */
    private void sortConverters() {
        for (List<DefaultValueConverter<?>> convertersList : convertersMap.values()) {
            convertersList.sort(Comparator.<DefaultValueConverter<?>> comparingInt(converter -> getPriority(converter)).reversed());
        }
    }

    private int getPriority(Object converter) {
        Priority priority = converter.getClass().getAnnotation(Priority.class);
        return priority != null ? priority.value() : DEFAULT_CONVERTER_PRIORITY;
    }

    public Optional<DefaultValueConverter<?>> getConverter(Type type) {
        // Check local converters first
        Optional<DefaultValueConverter<?>> converter = getConverterLocally(type);

        // Fallback to global if not found and we have a global registry
        if (converter.isEmpty() && globalRegistry != null) {
            converter = globalRegistry.getConverter(type);
        }

        return converter;
    }

    /**
     * Search for converter in this registry's local converters only (no fallback)
     */
    private Optional<DefaultValueConverter<?>> getConverterLocally(Type type) {
        List<DefaultValueConverter<?>> convertersForType = convertersMap.get(type);
        return Optional.ofNullable(convertersForType != null ? convertersForType.get(0) : null);
    }

    public void addConverter(Type type, DefaultValueConverter<?> converter) {
        convertersMap.computeIfAbsent(type, k -> new ArrayList<>()).add(converter);
    }

    @PreDestroy
    public void cleanup() {
        // Destroy any @Dependent beans
        context.release();
    }

}

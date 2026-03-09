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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.DefaultValueConverter;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;

@ApplicationScoped
public class ConverterRegistry {

    private static final int DEFAULT_CONVERTER_PRIORITY = 0;

    private Map<Type, List<DefaultValueConverter<?>>> convertersMap;
    private CreationalContext<?> context;

    public void registerConverters(Map<Type, List<DefaultValueConverter<?>>> convertersMap, CreationalContext<?> context) {
        this.convertersMap = convertersMap;
        this.context = context;
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

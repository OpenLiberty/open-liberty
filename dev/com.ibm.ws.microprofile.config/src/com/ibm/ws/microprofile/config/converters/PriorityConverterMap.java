/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.converters;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A Map of PriorityConverters, that only stores the PriorityConverter with the highest priority for each Type
 */
public class PriorityConverterMap {

    private final Map<Type, PriorityConverter> converters = new HashMap<>();
    private boolean unmodifiable = false;

    /**
     * Basic constructor
     */
    public PriorityConverterMap() {
        //no-op
    }

    /**
     * Copy constructor
     */
    public PriorityConverterMap(PriorityConverterMap toCopy) {
        for (PriorityConverter converter : toCopy.getAll()) {
            _addConverter(converter);
        }
    }

    /**
     * Add a converter to the map if:
     * - there is no existing converter for that type
     * - there is an existing converter of equal priority
     * - there is an existing converter of lower priority
     *
     * @param converter the new converter
     * @return the new converter if it was added or the existing converter if it was not
     */
    public PriorityConverter addConverter(PriorityConverter converter) {
        PriorityConverter existing = _addConverter(converter);
        return existing;
    }

    @Trivial
    private PriorityConverter _addConverter(PriorityConverter converter) {
        if (this.unmodifiable) {
            throw new UnsupportedOperationException();
        }

        PriorityConverter existing;
        Type type = converter.getType();
        existing = converters.get(type);
        if (existing == null || existing.getPriority() <= converter.getPriority()) {
            converters.put(type, converter);
            existing = converter;
        }
        return existing;
    }

    /**
     * Add all of the converters from the given map to this one... if they have a higher priority as above
     *
     * @param convertersToAdd the converters to add
     */
    public void addAll(PriorityConverterMap convertersToAdd) {
        for (PriorityConverter converter : convertersToAdd.converters.values()) {
            _addConverter(converter);
        }
    }

    /**
     * Get a converter for the given type
     *
     * @param <T>
     *
     * @param type the type to find a converter for
     * @return the converter for the given type
     */
    public PriorityConverter getConverter(Type type) {
        PriorityConverter converter = converters.get(type);
        return converter;
    }

    public void setUnmodifiable() {
        this.unmodifiable = true;
    }

    /**
     * @param type
     * @return
     */
    public boolean hasType(Type type) {
        return converters.containsKey(type);
    }

    /**
     * @return
     */
    public Collection<PriorityConverter> getAll() {
        return converters.values();
    }

    /**
     * @return
     */
    public Collection<? extends Type> getTypes() {
        return converters.keySet();
    }

    @Override
    public String toString() {
        return "PriorityConverterMap:" + converters;
    }
}
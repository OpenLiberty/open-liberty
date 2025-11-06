/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import java.lang.annotation.Annotation;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A single property extracted from a record
 *
 * @see JsonProperty
 */
public class RecordProperty implements JsonProperty {

    private RecordComponent component;

    public RecordProperty(RecordComponent component) {
        super();
        this.component = component;
    }

    @Override
    public String getElementName() {
        return component.getName();
    }

    @Override
    public boolean isInput() {
        return !JsonProperty.isTransient(component.getAccessor());
    }

    @Override
    public boolean isOutput() {
        return !JsonProperty.isTransient(component.getAccessor());
    }

    @Override
    public String getInputName() {
        String name = JsonProperty.getNameFromAnnotations(component.getAccessor());
        if (name == null) {
            name = getElementName();
        }
        return name;
    }

    @Override
    public String getOutputName() {
        String name = JsonProperty.getNameFromAnnotations(component.getAccessor());
        if (name == null) {
            name = getElementName();
        }
        return name;
    }

    @Override
    public Type getInputType() {
        return component.getGenericType();
    }

    @Override
    public Type getOutputType() {
        return component.getGenericType();
    }

    @Override
    public Annotation[] getInputAnnotations() {
        return component.getAccessor().getAnnotations();
    }

    @Override
    public Annotation[] getOutputAnnotations() {
        return component.getAccessor().getAnnotations();
    }

    public static List<JsonProperty> extractFromRecord(Class<? extends Record> cls) {
        List<JsonProperty> result = new ArrayList<>();
        for (var component : cls.getRecordComponents()) {
            result.add(new RecordProperty(component));
        }
        return result;
    }
}

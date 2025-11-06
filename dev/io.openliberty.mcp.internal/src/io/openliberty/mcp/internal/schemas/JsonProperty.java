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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 * Stores information about the properties extracted from a class or record in the way that JSON-B does it
 */
public interface JsonProperty {

    /**
     * The name of the property, as identified by the field and method names
     * <p>
     * This may be overridden using {@code JsonbProperty} for input or output
     *
     * @return the property name
     * @see #getInputName()
     * @see #getOutputName()
     */
    String getElementName();

    /**
     * Whether this property is used for output
     *
     * @return {@code true} if the property is used for output
     */
    boolean isInput();

    /**
     * Whether this property is used for input
     *
     * @return {@code true} if the property is used for input
     */
    boolean isOutput();

    /**
     * The name used when this property is input
     *
     * @return the input name
     */
    String getInputName();

    /**
     * The name used when this property is output
     *
     * @return the output name
     */
    String getOutputName();

    /**
     * The type of this property when used as input
     *
     * @return the input type
     */
    Type getInputType();

    /**
     * The type of this property when used as output
     *
     * @return the output type
     */
    Type getOutputType();

    /**
     * The annotations which apply when this property is used as input
     *
     * @return the annotations that apply to input
     */
    Annotation[] getInputAnnotations();

    /**
     * The annotations which apply when this property is used as output
     *
     * @return the annotations that apply to output
     */
    Annotation[] getOutputAnnotations();

    /**
     * Extract the name to use from the annotations on a method or field
     *
     * @param element the method or field
     * @return the name defined in the annotations, or {@code null} there are no relevant annotations or they do not define a name
     */
    public static String getNameFromAnnotations(AnnotatedElement element) {
        if (element == null) {
            return null;
        }

        JsonbProperty ann = element.getAnnotation(JsonbProperty.class);
        if (ann == null || ann.value().isBlank()) {
            return null;
        }

        return ann.value();
    }

    /**
     * Extract the JsonProperties for a class or record using reflection
     *
     * @param cls the class to examine
     * @return the properties of the class
     */
    public static List<JsonProperty> extract(Class<?> cls) {
        if (cls.isRecord()) {
            return RecordProperty.extractFromRecord(cls.asSubclass(Record.class));
        } else {
            return ClassProperty.extractFromClass(cls);
        }

    }

    /**
     * Returns whether a given annotated element is marked transient
     *
     * @param element the element
     * @return {@code true} if the element is transient, otherwise {@code false}
     */
    public static boolean isTransient(AnnotatedElement element) {
        return element.getAnnotation(JsonbTransient.class) != null;
    }
}
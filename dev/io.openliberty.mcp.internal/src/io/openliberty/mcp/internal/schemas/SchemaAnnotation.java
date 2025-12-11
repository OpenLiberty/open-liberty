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

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Optional;

import io.openliberty.mcp.annotations.Schema;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * The result of reading a {@link Schema} annotation
 *
 * @param annotation the {@link Schema} annotation itself, if present
 * @param schema the value of {@link Schema#value()}, if set
 * @param description the value of {@link Schema#description()}, if set
 */
public record SchemaAnnotation(Optional<Schema> annotation, Optional<String> schema, Optional<String> description) {

    public static final SchemaAnnotation EMPTY = new SchemaAnnotation(Optional.empty(), Optional.empty(), Optional.empty());

    /**
     * Reads any {@link Schema} annotations in the annotations array
     *
     * @param annotations the annotations array
     * @return the information extracted from any Schema annotations in the array
     */
    public static SchemaAnnotation read(Annotation[] annotations) {
        return Arrays.stream(annotations)
                     .filter(a -> a.annotationType().equals(Schema.class))
                     .map(Schema.class::cast)
                     .findAny()
                     .map(s -> new SchemaAnnotation(Optional.of(s), getValue(s), getDescription(s)))
                     .orElse(EMPTY);
    }

    /**
     * Reads the {@link Schema} from the annotated element (if present)
     *
     * @param element the annotated element
     * @return the information extracted from any Schema annotations in the array
     */
    public static SchemaAnnotation read(AnnotatedElement element) {
        Schema annotation = element.getAnnotation(Schema.class);
        if (annotation != null) {
            return new SchemaAnnotation(Optional.of(annotation), getValue(annotation), getDescription(annotation));
        } else {
            return EMPTY;
        }
    }

    private static Optional<String> getDescription(Schema anno) {
        String description = anno.description();
        return description.equals(Schema.UNSET) ? Optional.empty() : Optional.of(description);
    }

    private static Optional<String> getValue(Schema anno) {
        String value = anno.value();
        return value.equals(Schema.UNSET) ? Optional.empty() : Optional.of(value);
    }

    public static SchemaAnnotation ofDescription(String description) {
        return new SchemaAnnotation(Optional.empty(), Optional.empty(), Optional.ofNullable(description));
    }

    /**
     * Converts {@link #schema()} to a JsonSchema object, if present
     *
     * @return the JsonSchema
     */
    public Optional<JsonObjectBuilder> asJsonSchema() {
        return schema.map(s -> Json.createObjectBuilder(parse(s)));
    }

    private JsonObject parse(String json) {
        try (var reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
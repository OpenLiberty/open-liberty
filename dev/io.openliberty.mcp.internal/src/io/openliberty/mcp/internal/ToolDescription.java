/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;

public class ToolDescription {

    private final String name;
    private final String title;
    private final String description;
    private final InputSchemaObject inputSchema;
    private final AnnotationsDescription annotations;

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public InputSchemaObject getInputSchema() {
        return inputSchema;
    }

    public AnnotationsDescription getAnnotations() {
        return annotations;
    }

    public ToolDescription(ToolMetadata toolMetadata) {
        this.name = toolMetadata.name();
        this.title = toolMetadata.title();
        this.description = toolMetadata.description();

        Tool.Annotations ann = toolMetadata.annotation().annotations();
        if (isDefaultAnnotation(ann)) {
            this.annotations = null;
        } else {
            this.annotations = new AnnotationsDescription(
                                                          ann.readOnlyHint() == false ? null : ann.readOnlyHint(),
                                                          ann.destructiveHint() == true ? null : ann.destructiveHint(),
                                                          ann.idempotentHint() == false ? null : ann.idempotentHint(),
                                                          ann.openWorldHint() == true ? null : ann.openWorldHint(),
                                                          ann.title().isEmpty() ? null : ann.title());
        }
        Map<String, ArgumentMetadata> argumentMap = toolMetadata.arguments();
        Map<String, InputSchemaPrimitive> primitiveInputSchemaMap = new HashMap<>();
        LinkedList<String> requiredParameterList = new LinkedList<>();

        for (String argumentName : argumentMap.keySet()) {
            ArgumentMetadata argumentMetadata = argumentMap.get(argumentName);
            primitiveInputSchemaMap.put(argumentName, buildPrimitiveInputSchema(argumentMetadata));

            if (argumentMetadata.required()) {
                requiredParameterList.add(argumentName);
            }
        }

        inputSchema = new InputSchemaObject("object", primitiveInputSchemaMap, requiredParameterList);
    }

    /*
     * Helper Method for default Annotation
     */

    private boolean isDefaultAnnotation(Tool.Annotations ann) {
        return ann.readOnlyHint() == false
               && ann.destructiveHint() == true
               && ann.idempotentHint() == false
               && ann.openWorldHint() == true
               && ann.title().isEmpty();

    }

    private InputSchemaPrimitive buildPrimitiveInputSchema(ArgumentMetadata argumentMetadata) {

        Type type = argumentMetadata.type();
        String argumentDescription = argumentMetadata.description();
        if (argumentDescription.equals("")) {
            argumentDescription = null;
        }

        InputSchemaPrimitive tempSchemaPrimitive;

        boolean isJsonNumber = type.equals(long.class) || type.equals(double.class) || type.equals(byte.class) || type.equals(float.class) || type.equals(short.class)
                               || type.equals(Long.class) || type.equals(Double.class) || type.equals(Byte.class) || type.equals(Float.class) || type.equals(Short.class);

        if (isJsonNumber)
            tempSchemaPrimitive = new InputSchemaPrimitive("number", argumentDescription);
        else if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class))
            tempSchemaPrimitive = new InputSchemaPrimitive("string", argumentDescription);
        else if (type.equals(int.class) || type.equals(Integer.class))
            tempSchemaPrimitive = new InputSchemaPrimitive("integer", argumentDescription);
        else if (type.equals(boolean.class) || type.equals(Boolean.class))
            tempSchemaPrimitive = new InputSchemaPrimitive("boolean", argumentDescription);
        else
            tempSchemaPrimitive = new InputSchemaPrimitive(type.getTypeName(), argumentDescription);
        //else if (type.equals(Object.class)) {
        // TODO Implement this using the InputSchema record
        //}
        return tempSchemaPrimitive;
    }

    public record InputSchema(List<InputSchemaObject> objs, List<InputSchemaPrimitive> primitives) {}

    public record InputSchemaObject(String type, Map<String, InputSchemaPrimitive> properties, List<String> required) {}

    public record InputSchemaPrimitive(String type, String description) {}

    public record AnnotationsDescription(
                                         Boolean readOnlyHint,
                                         Boolean destructiveHint,
                                         Boolean idempotentHint,
                                         Boolean openWorldHint,
                                         String title) {}
}
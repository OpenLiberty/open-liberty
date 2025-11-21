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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.exceptions.GenericArgumentException;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.json.JsonObject;

public record ToolMetadata(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method,
                           Map<String, ArgumentMetadata> arguments,
                           List<SpecialArgumentMetadata> specialArguments,
                           String name, String title, String description,
                           List<Class<? extends Throwable>> businessExceptions,
                           boolean returnsCompletionStage,
                           JsonObject inputSchema,
                           JsonObject outputSchema) {

    public static final String MISSING_TOOL_ARG_NAME = "<<<MISSING TOOL_ARG NAME>>>";

    public record ArgumentMetadata(Type type, int index, String description, boolean required, boolean isDuplicate) {}

    public record SpecialArgumentMetadata(SpecialArgumentType.Resolution typeResolution, int index) {}

    public ToolMetadata {
        arguments = ((arguments == null) ? Collections.emptyMap() : arguments);
        specialArguments = ((specialArguments == null) ? Collections.emptyList() : specialArguments);
    }

    public static ToolMetadata createFrom(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method) {
        String name = annotation.name().equals(Tool.ELEMENT_NAME) ? method.getJavaMember().getName() : annotation.name();
        String title = annotation.title().isEmpty() ? null : annotation.title();
        String description = annotation.description().isEmpty() ? null : annotation.description();

        Type returnType = method.getJavaMember().getGenericReturnType();
        Class<?> returnTypeClass = method.getJavaMember().getReturnType();

        WrapBusinessError wrapAnnotation = method.getAnnotation(WrapBusinessError.class);
        List<Class<? extends Throwable>> businessExceptions = (wrapAnnotation != null) ? List.of(wrapAnnotation.value()) : Collections.emptyList();
        boolean returnsCompletionStage = CompletionStage.class.isAssignableFrom(returnTypeClass);
        SchemaRegistry sr = SchemaRegistry.get();

        JsonObject inputSchema = sr.getToolInputSchema(method);

        boolean hasContentListReturn = (returnType instanceof ParameterizedType pt && ((Class<?>) pt.getRawType()).isAssignableFrom(List.class)
                                        && (pt.getActualTypeArguments()[0] instanceof Class<?>) && ((Class<?>) pt.getActualTypeArguments()[0]).isAssignableFrom(Content.class));
        boolean hasOutputSchema = (!returnTypeClass.isAssignableFrom(ToolResponse.class) && !hasContentListReturn && !returnTypeClass.isAssignableFrom(Content.class)
                                   && !returnTypeClass.isAssignableFrom(String.class) && annotation.structuredContent());

        if (!hasOutputSchema && returnTypeClass.isAssignableFrom(ToolResponse.class) && annotation.structuredContent()
            && method.isAnnotationPresent(Schema.class)
            && method.getAnnotation(Schema.class).value() != Schema.UNSET) {

            hasOutputSchema = true;

        }
        JsonObject outputSchema = hasOutputSchema ? sr.getToolOutputSchema(method) : null;

        outputSchema = (outputSchema == null || outputSchema.isEmpty()) ? null : outputSchema;

        return new ToolMetadata(annotation,
                                bean,
                                method,
                                getArgumentMap(method),
                                getSpecialArgumentList(method),
                                name,
                                title,
                                description,
                                businessExceptions,
                                returnsCompletionStage,
                                inputSchema,
                                outputSchema);

    }

    public static Map<String, ArgumentMetadata> getArgumentMap(AnnotatedMethod<?> method) {
        Map<String, ArgumentMetadata> result = new HashMap<>();
        ArrayList<String> genericParams = new ArrayList<>();
        for (AnnotatedParameter<?> param : method.getParameters()) {

            if (TypeUtility.hasGenericParams(param.getBaseType())) {
                genericParams.add(param.getJavaParameter().getName());
            } else {
                ToolArg argAnnotation = param.getAnnotation(ToolArg.class);

                if (argAnnotation == null) {
                    continue;
                }
                String argName = resolveArgumentName(param, argAnnotation);
                boolean isDuplicateArg = result.containsKey(argName);

                result.put(argName, new ArgumentMetadata(param.getBaseType(),
                                                         param.getPosition(),
                                                         argAnnotation.description(),
                                                         argAnnotation.required(),
                                                         isDuplicateArg));
            }

        }
        if (!genericParams.isEmpty()) {
            throw new GenericArgumentException(genericParams);
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    private static String resolveArgumentName(AnnotatedParameter<?> param, ToolArg argAnnotation) {
        String argAnnotationName = argAnnotation.name();

        if (!argAnnotationName.equals(ToolArg.ELEMENT_NAME)) {
            return argAnnotationName;
        }

        if (param.getJavaParameter().isNamePresent()) {
            // needs java compiler -parameter flag to work
            return param.getJavaParameter().getName();
        }

        return MISSING_TOOL_ARG_NAME;
    }

    private static List<SpecialArgumentMetadata> getSpecialArgumentList(AnnotatedMethod<?> method) {
        List<SpecialArgumentMetadata> result = new ArrayList<>();
        for (AnnotatedParameter<?> p : method.getParameters()) {
            ToolArg pInfo = p.getAnnotation(ToolArg.class);
            if (pInfo == null) {
                SpecialArgumentMetadata pData = new SpecialArgumentMetadata(SpecialArgumentType.fromClass(p.getBaseType()), p.getPosition());
                result.add(pData);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Used for error reporting cases, such as locating Duplicate Tools and ToolArgs
     */
    public String getToolQualifiedName() {
        return bean.getBeanClass() + "." + method.getJavaMember().getName();
    }

    /**
     * Used for error reporting cases, such as Generic args
     */
    public static String getToolQualifiedName(Bean<?> bean, AnnotatedMethod<?> method) {
        return bean.getBeanClass() + "." + method.getJavaMember().getName();
    }
}
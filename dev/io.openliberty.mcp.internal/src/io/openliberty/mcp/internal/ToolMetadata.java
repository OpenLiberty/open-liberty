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
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.exceptions.GenericArgumentException;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.security.SecurityRequirement;
import io.openliberty.mcp.internal.tools.AsyncBeanMethodHandler;
import io.openliberty.mcp.internal.tools.BeanMethodHandler.MethodMetadata;
import io.openliberty.mcp.internal.tools.SyncBeanMethodHandler;
import io.openliberty.mcp.internal.tools.ToolManager.ToolAnnotations;
import io.openliberty.mcp.internal.tools.ToolManager.ToolArguments;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;

/**
 * Metadata about a tool
 *
 * @param name the tool name
 * @param title the tool title, may be {@code null}
 * @param description the tool description, may be {@code null}
 * @param arguments a map of argument name to argument metadata
 * @param annotations the MCP annotations applied to the tool
 * @param returnsCompletionStage whether the tool is asynchronous
 * @param inputSchema the input schema for the tool, usually an object containing all of the named parameters
 * @param outputSchema the output schema, {@code null} if the tool does not return structured content
 * @param handler the handler, {@code null} if this is an async tool
 * @param asyncHandler the async handler, {@code null} if this is not an async tool
 * @param methodMetadata data about the method if this tool was discovered by annotating a method with {@link Tool}. Should not be used in most circumstances.
 * @param securityRequirement stores authorization requirements needed to authorize access to a MCP tool
 */
public record ToolMetadata(String name,
                           String title,
                           String description,
                           Map<String, ArgumentMetadata> arguments,
                           ToolAnnotations annotations,
                           boolean returnsCompletionStage,
                           JsonObject inputSchema,
                           JsonObject outputSchema,
                           Function<ToolArguments, ToolResponse> handler,
                           Function<ToolArguments, CompletionStage<ToolResponse>> asyncHandler,
                           Optional<MethodMetadata> methodMetadata,
                           SecurityRequirement securityRequirement) {

    public static final String MISSING_TOOL_ARG_NAME = "<<<MISSING TOOL_ARG NAME>>>";

    public record ArgumentMetadata(Type type, int index, String description, boolean required, boolean isDuplicate) {}

    public record SpecialArgumentMetadata(SpecialArgumentType.Resolution typeResolution, int index) {}

    public ToolMetadata {
        arguments = ((arguments == null) ? Collections.emptyMap() : arguments);

        if (handler == null && asyncHandler == null) {
            throw new IllegalArgumentException("Either handler or asyncHandler must be set");
        } else if (handler != null && asyncHandler != null) {
            throw new IllegalArgumentException("Only one of handler and async handler may be set");
        }
    }

    /**
     * Create the tool metadata for a method annotated with {@link Tool}
     *
     * @param annotation the {@code Tool} annotation
     * @param bean the bean containing the method
     * @param method the annotated method
     * @param bm the bean manager to use to obtain an instance of the bean
     * @param jsonb the jsonb to use to serialize structured content
     * @return the created tool metadata
     */
    public static ToolMetadata createFrom(Tool annotation, Bean<?> bean, AnnotatedMethod<?> method, BeanManager bm, Jsonb jsonb) {
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

        ToolAnnotations annotations = readAnnotations(annotation.annotations());

        Map<String, ArgumentMetadata> argumentMap = getArgumentMap(method);

        MethodMetadata methodMetadata = new MethodMetadata(name,
                                                           bean,
                                                           method.getJavaMember(),
                                                           hasOutputSchema,
                                                           businessExceptions,
                                                           getSpecialArgumentList(method),
                                                           getArgNameArray(method, argumentMap));

        SyncBeanMethodHandler handler = null;
        AsyncBeanMethodHandler asyncHandler = null;
        if (returnsCompletionStage) {
            asyncHandler = new AsyncBeanMethodHandler(jsonb, bm, methodMetadata);
        } else {
            handler = new SyncBeanMethodHandler(jsonb, bm, methodMetadata);
        }

        return new ToolMetadata(name,
                                title,
                                description,
                                getArgumentMap(method),
                                annotations,
                                returnsCompletionStage,
                                inputSchema,
                                outputSchema,
                                handler,
                                asyncHandler,
                                Optional.of(methodMetadata),
                                SecurityRequirement.createFrom(method));

    }

    private static String[] getArgNameArray(AnnotatedMethod<?> method, Map<String, ArgumentMetadata> argumentMap) {
        String[] nameArray = new String[method.getJavaMember().getParameterCount()];
        for (var entry : argumentMap.entrySet()) {
            nameArray[entry.getValue().index] = entry.getKey();
        }
        return nameArray;
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

    public static ToolAnnotations readAnnotations(Tool.Annotations annotations) {
        return new ToolAnnotations(annotations.title(),
                                   annotations.readOnlyHint(),
                                   annotations.destructiveHint(),
                                   annotations.idempotentHint(),
                                   annotations.openWorldHint());
    }

    /**
     * Used for error reporting cases, such as locating Duplicate Tools and ToolArgs
     */
    public String getToolQualifiedName() {
        return methodMetadata.map(m -> m.bean().getBeanClass().toString() + "." + m.method().getName())
                             .orElse("User-defined tool: " + name);
    }

    /**
     * Used for error reporting cases, such as Generic args
     */
    public static String getToolQualifiedName(Bean<?> bean, AnnotatedMethod<?> method) {
        return bean.getBeanClass() + "." + method.getJavaMember().getName();
    }
}

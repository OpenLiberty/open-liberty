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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.mcpjava.server.MetaField;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.internal.ToolRegistry.ToolArgumentImpl;
import io.openliberty.mcp.internal.ToolValidation.ToolValidationError;
import io.openliberty.mcp.internal.requests.DefaultValueResolver;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.security.SecurityRequirement;
import io.openliberty.mcp.internal.tools.AsyncBeanMethodHandler;
import io.openliberty.mcp.internal.tools.BeanMethodHandler.MethodMetadata;
import io.openliberty.mcp.internal.tools.SyncBeanMethodHandler;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolManager.ToolAnnotations;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolManager.ToolArguments;
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
 * @param arguments the list of tool argument metadata
 * @param annotations the MCP annotations applied to the tool, if supplied
 * @param returnsCompletionStage whether the tool is asynchronous
 * @param inputSchema the input schema for the tool, usually an object containing all of the named parameters
 * @param outputSchema the output schema, {@code null} if the tool does not return structured content
 * @param handler the handler, {@code null} if this is an async tool
 * @param asyncHandler the async handler, {@code null} if this is not an async tool
 * @param methodMetadata data about the method if this tool was discovered by annotating a method with {@link Tool}. Should not be used in most circumstances.
 * @param securityRequirement stores authorization requirements needed to authorize access to a MCP tool
 * @param createdAt when the tool was created
 */
public record ToolMetadata(String name,
                           String title,
                           String description,
                           List<ToolArgument> arguments,
                           Optional<ToolAnnotations> annotations,
                           boolean returnsCompletionStage,
                           JsonObject inputSchema,
                           JsonObject outputSchema,
                           Function<ToolArguments, ToolResponse> handler,
                           Function<ToolArguments, CompletionStage<ToolResponse>> asyncHandler,
                           Optional<MethodMetadata> methodMetadata,
                           SecurityRequirement securityRequirement,
                           Instant createdAt,
                           Map<String, Object> metadata,
                           List<ToolValidationError> validationErrors) implements ToolManager.ToolInfo {

    public static final String MISSING_TOOL_ARG_NAME = "<<<MISSING TOOL_ARG NAME>>>";

    public record ToolMethodArgument(AnnotatedParameter<?> parameter, ToolArgument argument) {}

    public record SpecialArgumentMetadata(SpecialArgumentType type, int index) {}

    public ToolMetadata {
        arguments = ((arguments == null) ? Collections.emptyList() : arguments);

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
        List<ToolValidationError> validationErrors = new ArrayList<>();
        String name = annotation.name().equals(Tool.ELEMENT_NAME) ? method.getJavaMember().getName() : annotation.name();
        String title = annotation.title().isEmpty() ? null : annotation.title();
        String description = annotation.description().isEmpty() ? null : annotation.description();

        Type returnType = method.getJavaMember().getGenericReturnType();
        Class<?> returnTypeClass = method.getJavaMember().getReturnType();

        Type unwrappedOutputType = unwrapOutputType(returnType);
        Class<?> unwrappedOutputClass = getRawClass(unwrappedOutputType);

        Map<TypeVariable<?>, Type> genericMap = Collections.emptyMap();
        if (bean != null) {
            Type javaType = bean.getBeanClass();
            genericMap = TypeUtility.generateGenericMap(javaType);
        }
        List<ToolMethodArgument> methodArguments = getArguments(bean, method, genericMap, validationErrors);

        WrapBusinessError wrapAnnotation = method.getAnnotation(WrapBusinessError.class);
        List<Class<? extends Throwable>> businessExceptions = (wrapAnnotation != null) ? List.of(wrapAnnotation.value()) : Collections.emptyList();
        boolean returnsCompletionStage = CompletionStage.class.isAssignableFrom(returnTypeClass);
        SchemaRegistry sr = SchemaRegistry.get();

        JsonObject inputSchema = sr.getToolInputSchema(methodArguments);

        boolean hasContentListReturn = unwrappedOutputType instanceof ParameterizedType pt
                                       && (List.class.isAssignableFrom((Class<?>) pt.getRawType())
                                           && pt.getActualTypeArguments()[0] instanceof Class<?>)
                                       && ContentBlock.class.isAssignableFrom((Class<?>) pt.getActualTypeArguments()[0]);

        Class<?> outputSchemaFromClass = annotation.outputSchemaFrom();
        boolean hasOutputSchemaFrom = outputSchemaFromClass != Void.class;

        boolean hasOutputSchema = annotation.structuredContent()
                                  && !hasContentListReturn
                                  && !ToolResponse.class.isAssignableFrom(unwrappedOutputClass)
                                  && !ContentBlock.class.isAssignableFrom(unwrappedOutputClass)
                                  && !String.class.isAssignableFrom(unwrappedOutputClass);

        if (!hasOutputSchema && hasOutputSchemaFrom && annotation.structuredContent()) {
            hasOutputSchema = true;
        }

        if (!hasOutputSchema && ToolResponse.class.isAssignableFrom(unwrappedOutputClass) && annotation.structuredContent()
            && method.isAnnotationPresent(Schema.class)
            && method.getAnnotation(Schema.class).value() != Schema.UNSET) {

            hasOutputSchema = true;
        }
        if (TypeUtility.hasGenericParams(unwrappedOutputType)) {
            unwrappedOutputType = TypeUtility.createResolvedType(unwrappedOutputType, genericMap);
        }
        Type effectiveOutputType = hasOutputSchemaFrom ? outputSchemaFromClass : unwrappedOutputType;
        JsonObject outputSchema = hasOutputSchema ? sr.getToolOutputSchema(method, effectiveOutputType) : null;

        outputSchema = (outputSchema == null || outputSchema.isEmpty()) ? null : outputSchema;

        if (outputSchema != null && !(method.isAnnotationPresent(Schema.class) && method.getAnnotation(Schema.class).value() != Schema.UNSET)
            && !checkConcreteType(outputSchema).equals("object")) {
            validationErrors.add(new ToolValidationError("CWMCM0025E.unsupported.output", effectiveOutputType, ToolMetadata.getToolQualifiedName(bean, method)));
        }

        Optional<ToolAnnotations> annotations = readAnnotations(annotation.annotations());

        Map<String, Object> metaMap = getMetaMapFromAnnotations(bean, method, jsonb, validationErrors);

        MethodMetadata methodMetadata = new MethodMetadata(name,
                                                           bean,
                                                           method.getJavaMember(),
                                                           hasOutputSchema,
                                                           businessExceptions,
                                                           getSpecialArgumentList(method),
                                                           getArgNameArray(method, methodArguments),
                                                           genericMap);

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
                                methodArguments.stream().map(ToolMethodArgument::argument).collect(toUnmodifiableList()),
                                annotations,
                                returnsCompletionStage,
                                inputSchema,
                                outputSchema,
                                handler,
                                asyncHandler,
                                Optional.of(methodMetadata),
                                SecurityRequirement.createFrom(method),
                                Instant.now(),
                                metaMap,
                                validationErrors.isEmpty() ? Collections.emptyList() : validationErrors);
    }

    /**
     *
     * @param outputSchema the JSON schema generated to validate (will not work with custom schemas based JSON)
     * @return type of object
     */
    public static String checkConcreteType(JsonObject outputSchema) {
        if (outputSchema.containsKey("type")) {
            return outputSchema.getString("type");
        } else {
            if (outputSchema.containsKey("$ref")) {
                String[] path = outputSchema.getString("$ref").split("/");
                JsonObject curJsonObj = outputSchema.getJsonObject("$defs").getJsonObject(path[path.length - 1]);
                return curJsonObj.getString("type");
            }
        }
        return "";
    }

    private static String[] getArgNameArray(AnnotatedMethod<?> method, List<ToolMethodArgument> toolMethodArgs) {
        String[] nameArray = new String[method.getJavaMember().getParameterCount()];
        for (ToolMethodArgument arg : toolMethodArgs) {
            nameArray[arg.parameter().getPosition()] = arg.argument().name();
        }
        return nameArray;
    }

    public static List<ToolMethodArgument> getArguments(Bean<?> bean, AnnotatedMethod<?> method, Map<TypeVariable<?>, Type> genericMap,
                                                        List<ToolValidationError> validationErrors) {
        List<ToolMethodArgument> result = new ArrayList<>();
        for (AnnotatedParameter<?> param : method.getParameters()) {
            if (TypeUtility.hasGenericParams(param.getBaseType())) {
                Type baseType = param.getBaseType();
                boolean unresolvedGenericParam = hasUnresolvableTypeVariables(baseType, genericMap);
                if (unresolvedGenericParam) {
                    validationErrors.add(new ToolValidationError("CWMCM0018E.generic.arguments",
                                                                 ToolMetadata.getToolQualifiedName(bean, method),
                                                                 resolveArgumentName(param, param.getAnnotation(ToolArg.class))));
                } else {
                    Type argType = TypeUtility.createResolvedType(param.getBaseType(), genericMap);
                    addArgumentMetadata(param, argType, result);
                }
            } else {
                addArgumentMetadata(param, param.getBaseType(), result);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    /**
     * Tool Arguments are NOT required if:
     *
     * ToolArg.required is set to false
     * ToolArg.defaultValue is set, or
     * the argument return type is optional
     */
    private static boolean isArgumentRequired(ToolArg argAnnotation, Type argumentType) {
        if (argAnnotation != null && !argAnnotation.defaultValue().isEmpty()) {
            return false;
        }
        if (DefaultValueResolver.isOptionalType(argumentType)) {
            return false;
        }
        return argAnnotation != null ? argAnnotation.required() : true;
    }

    private static void addArgumentMetadata(AnnotatedParameter<?> param, Type argumentType, List<ToolMethodArgument> result) {

        if (SpecialArgumentType.fromClass(argumentType).isPresent()) {
            // This parameter is a special argument
            return;
        }

        ToolArg argAnnotation = param.getAnnotation(ToolArg.class);
        String argName = resolveArgumentName(param, argAnnotation);
        String description = argAnnotation != null ? argAnnotation.description() : "";
        boolean required = isArgumentRequired(argAnnotation, param.getBaseType());
        String rawDefault = argAnnotation != null ? argAnnotation.defaultValue() : "";
        String defaultValue = (rawDefault == null || rawDefault.isEmpty()) ? null : rawDefault;

        result.add(new ToolMethodArgument(param,
                                          new ToolArgumentImpl(argName,
                                                               description,
                                                               required,
                                                               argumentType,
                                                               defaultValue)));
    }

    private static boolean hasUnresolvableTypeVariables(Type baseType, Map<TypeVariable<?>, Type> genericMap) {

        List<Type> genericTypes;
        if (baseType instanceof ParameterizedType pt) {
            genericTypes = List.of(pt.getActualTypeArguments());

        } else if (baseType instanceof GenericArrayType gat) {
            genericTypes = List.of(gat.getGenericComponentType());

        } else if (baseType instanceof TypeVariable<?> tv) {
            genericTypes = List.of(tv);

        } else if (baseType instanceof Class clazz && clazz.isArray()) {
            Type elementType = (clazz).getComponentType();
            genericTypes = List.of(elementType);

        } else {
            genericTypes = List.of();
        }

        for (Type genericType : genericTypes) {
            if (genericType instanceof TypeVariable<?> tv) {
                if (genericMap.get(tv) == null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return hasUnresolvableTypeVariables(genericType, genericMap);
            }
        }
        return false;

    }

    private static String resolveArgumentName(AnnotatedParameter<?> param, ToolArg argAnnotation) {
        if (argAnnotation != null) {
            String argAnnotationName = argAnnotation.name();

            if (!argAnnotationName.equals(ToolArg.ELEMENT_NAME)) {
                return argAnnotationName;
            }
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
            SpecialArgumentType.fromClass(p.getBaseType()).ifPresent(type -> {
                result.add(new SpecialArgumentMetadata(type, p.getPosition()));
            });
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<ToolAnnotations> readAnnotations(Tool.Annotations annotations) {

        if (isDefaultAnnotation(annotations)) {
            return Optional.empty();
        } else {
            return Optional.of(new ToolAnnotations(annotations.title(),
                                                   annotations.readOnlyHint(),
                                                   annotations.destructiveHint(),
                                                   annotations.idempotentHint(),
                                                   annotations.openWorldHint()));
        }
    }

    private static boolean isDefaultAnnotation(Tool.Annotations ann) {
        return ann.readOnlyHint() == false
               && ann.destructiveHint() == true
               && ann.idempotentHint() == false
               && ann.openWorldHint() == true
               && ann.title().isEmpty();
    }

    /**
     * Used for error reporting cases, such as locating Duplicate Tools and ToolArgs
     */
    public String getToolQualifiedName() {
        return methodMetadata.map(m -> m.bean().getBeanClass().getName() + "." + m.method().getName())
                             .orElse("User-defined tool: " + name);
    }

    /**
     * Used for error reporting cases, such as Generic args
     */
    public static String getToolQualifiedName(Bean<?> bean, AnnotatedMethod<?> method) {
        return bean.getBeanClass().getName() + "." + method.getJavaMember().getName();
    }

    private static Map<String, Object> getMetaMapFromAnnotations(Bean<?> bean, AnnotatedMethod<?> method, Jsonb jsonb, List<ToolValidationError> validationErrors) {
        Set<MetaField> metaFields = method.getAnnotations(MetaField.class);
        if (metaFields.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        for (MetaField metaField : method.getAnnotations(MetaField.class)) {
            String prefix = metaField.prefix();
            if (!prefix.isEmpty()) {
                if (!prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }
                if (!ToolValidation.isValidMetaPrefix(prefix)) {
                    validationErrors.add(new ToolValidationError("CWMCM0040E.invalid.metadata.prefix", prefix, getToolQualifiedName(bean, method)));
                    continue;
                }
            }
            String name = metaField.name();
            if (!ToolValidation.isValidMetaName(name)) {
                validationErrors.add(new ToolValidationError("CWMCM0041E.invalid.metadata.name", name, getToolQualifiedName(bean, method)));
                continue;
            }
            String key = prefix.isEmpty() ? name : prefix + name;
            Object value = getValue(metaField, key, jsonb, bean, method, validationErrors);
            result.put(key, value);
        }

        return result;
    }

    private static Object getValue(MetaField metaField, String key, Jsonb jsonb, Bean<?> bean, AnnotatedMethod<?> method, List<ToolValidationError> validationErrors) {
        try {
            return switch (metaField.type()) {
                case BOOLEAN -> Boolean.valueOf(metaField.value());
                case INT -> Integer.valueOf(metaField.value());
                case STRING -> metaField.value();
                case JSON -> jsonb.fromJson(metaField.value(), Object.class);
            };
        } catch (Exception e) {
            validationErrors.add(new ToolValidationError("CWMCM0042E.metadata.value.conversion.error", key, getToolQualifiedName(bean, method), metaField.type(), e.toString()));
            return null;
        }
    }

    /**
     * Unwraps the type inside CompletionStage if applicable.
     *
     * @param type the type to be unwrapped
     * @return the inner type or the original type if {@code returnType} is not a {@code CompletionStage}
     */
    public static Type unwrapOutputType(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> rawClass &&
                CompletionStage.class.isAssignableFrom(rawClass)) {
                return pt.getActualTypeArguments()[0];
            }
        }
        return type;
    }

    /**
     * Convert a parameterized type to its raw type.
     * <p>
     * If {@code type} is a class, return it.
     * If {@code type} is a parameterized type, return the raw type.
     * Otherwise, return {@code Object}
     *
     * @param type the type to extract the raw type from
     * @return the raw type
     */
    public static Class<?> getRawClass(Type type) {
        if (type instanceof Class clazz) {
            return clazz;
        } else if (type instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        } else {
            return Object.class;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMethod() {
        return methodMetadata.isPresent();
    }

}

/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 *
 */
public class McpToolCallParams {

    private String name;
    private ToolMetadata metadata;
    private static final TraceComponent tc = Tr.register(McpToolCallParams.class);
    private static Map<Class<?>, Object> TYPE_DEFAULTS_MAP = Map.of(
                                                                    boolean.class, false,
                                                                    char.class, '\0',
                                                                    byte.class, (byte) 0,
                                                                    short.class, (short) 0,
                                                                    int.class, 0,
                                                                    long.class, 0L,
                                                                    float.class, 0f,
                                                                    double.class, 0d);

    /**
     * @return the metadata
     */
    public ToolMetadata getMetadata() {
        return metadata;
    }

    private JsonObject arguments;

    @JsonbProperty("_meta")
    private JsonObject meta;

    private Map<String, Object> parsedArguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        ToolRegistry tools = ToolRegistry.get();
        metadata = tools.getTool(name);
    }

    public void setArguments(JsonObject arguments) {
        this.arguments = arguments;
    }

    public Map<String, Object> getArguments(Jsonb jsonb) {
        if (this.arguments == null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of(Tr.formatMessage(tc, "jsonrpc.missing.params")));
        }
        if (parsedArguments == null) {
            parsedArguments = parseArguments(arguments, jsonb);
        }
        return parsedArguments;
    }

<<<<<<< HEAD
    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    private Map<String, Object> parseArguments(JsonObject requestArguments, Jsonb jsonb) {
||||||| parent of 33f4e591db2 (Fix merge conflicts)
<<<<<<< HEAD
    private Map<String, Object> parseArguments(JsonObject arguments, Jsonb jsonb) {
=======
    private Map<String, Object> parseArguments(JsonObject requestArguments, Jsonb jsonb) {
>>>>>>> 33f4e591db2 (Fix merge conflicts)
        Map<String, ArgumentMetadata> metadatas = metadata.arguments();
        Map<String, Object> result = new HashMap<>();
<<<<<<< HEAD

        for (var argEntry : metadatas.entrySet()) {
            String argName = argEntry.getKey();
            ArgumentMetadata argMetadata = argEntry.getValue();
            JsonValue argValue = requestArguments.get(argName);
            if (argValue != null) {
                String argValueJson = jsonb.toJson(argValue);
                result.put(argName, jsonb.fromJson(argValueJson, argMetadata.type()));
            } else if (!argMetadata.required()) {
                if (!argMetadata.defaultValue().isEmpty()) {
                    result.put(argName, convertDefaultValueToArgType(metadata, argMetadata));
                } else {
                    result.put(argName, emptyToolArgValue(argMetadata.type())); //blank result for no value provided for optional argument
                }
            } else {
                List<String> data = generateArgumentMismatchData(requestArguments.keySet(), metadatas.keySet());
||||||| parent of 33f4e591db2 (Fix merge conflicts)
||||||| parent of e0cbf5803b0 (test: unit tests for tool arg default value)
    private Object[] parseArguments(JsonObject requestArguments, Jsonb jsonb) {
        JsonObject requestArgumentsObject = requestArguments.asJsonObject();
        Map<String, ArgumentMetadata> toolArgs = metadata.arguments();
        List<SpecialArgumentMetadata> specialArguments = metadata.specialArguments();
        Object[] results = new Object[toolArgs.size() + specialArguments.size()];
        for (var argName : toolArgs.keySet()) {
            ArgumentMetadata argMetadata = toolArgs.get(argName);
            if (requestArgumentsObject.containsKey(argName)) {
                JsonValue argValue = requestArgumentsObject.get(argName);
                String argValueJson = jsonb.toJson(argValue);
                results[argMetadata.index()] = jsonb.fromJson(argValueJson, argMetadata.type());
            } else if (!argMetadata.required()) {
                String defaultValue = argMetadata.defaultValue();
                if (!defaultValue.isEmpty()) {
                    results[argMetadata.index()] = convert(defaultValue, argMetadata.type());
                } else {
                    results[argMetadata.index()] = nullToolArgValue((Class<?>) argMetadata.type()); //blank result for no value provided for optional argument
                }
=======
    private Object[] parseArguments(JsonObject requestArguments, Jsonb jsonb) {
        requestArguments = requestArguments.asJsonObject();
        Map<String, ArgumentMetadata> toolArgs = metadata.arguments();
        List<SpecialArgumentMetadata> specialArguments = metadata.specialArguments();
        Object[] results = new Object[toolArgs.size() + specialArguments.size()];
        for (var argName : toolArgs.keySet()) {
            ArgumentMetadata argMetadata = toolArgs.get(argName);
            if (requestArguments.containsKey(argName)) {
                JsonValue argValue = requestArguments.get(argName);
                String argValueJson = jsonb.toJson(argValue);
                results[argMetadata.index()] = jsonb.fromJson(argValueJson, argMetadata.type());
            } else if (!argMetadata.required()) {
                String defaultValue = argMetadata.defaultValue();
                if (!defaultValue.isEmpty()) {
                    results[argMetadata.index()] = convert(argName);
                } else {
                    results[argMetadata.index()] = nullToolArgValue((Class<?>) argMetadata.type()); //blank result for no value provided for optional argument
                }
>>>>>>> e0cbf5803b0 (test: unit tests for tool arg default value)

<<<<<<< HEAD
        HashSet<String> argsProcessed = new HashSet<>();
        for (var entry : arguments.entrySet()) {
            String argName = entry.getKey();
            JsonValue argValue = entry.getValue();
            ArgumentMetadata argMetadata = metadatas.get(argName);
            if (argMetadata != null) {
                String json = jsonb.toJson(argValue);
                result.put(argName, jsonb.fromJson(json, argMetadata.type()));
||||||| parent of e0cbf5803b0 (test: unit tests for tool arg default value)
            } else { // if a request doesn't contain a required toolArg, throw an exception
                List<String> data = generateArgumentMismatchData(requestArgumentsObject.keySet(), toolArgs.keySet());
=======

        for (var argName : metadatas.keySet()) {
            ArgumentMetadata argMetadata = metadatas.get(argName);
            if (requestArguments.containsKey(argName)) {
                JsonValue argValue = requestArguments.get(argName);
                String argValueJson = jsonb.toJson(argValue);
                result.put(argName, jsonb.fromJson(argValueJson, argMetadata.type()));
            } else if (!argMetadata.required()) {
                String defaultValue = argMetadata.defaultValue();
                if (!defaultValue.isEmpty()) {
                    result.put(argName, convert(argName));
                } else {
                    result.put(argName, nullToolArgValue((Class<?>) argMetadata.type())); //blank result for no value provided for optional argument
                }
            } else {
                List<String> data = generateArgumentMismatchData(requestArguments.keySet(), metadatas.keySet());
>>>>>>> 33f4e591db2 (Fix merge conflicts)
                throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, data);
            }

        }
        return result;
    }

    /**
     * The null value to use for different types. Null for objects or 0 for primitives.
     *
     * @param type the type to get the null value for
     * @return the null value for the class inputted as a parameter
     */
    public static Object emptyToolArgValue(Type type) {
        if (type instanceof Class clazz && clazz.isPrimitive())
            return TYPE_DEFAULTS_MAP.get(clazz);
        return null;
    }

    /**
     * Converts a tool argument's default value, specified in {@link ToolArg#defaultValue()}, from a string to a Java object matching the tool argument's type.
     *
     * @param toolMetadata the metadata for the tool containing the tool argument
     * @param argMetadata the metadata for the tool argument, which includes the default value and type
     * @return the default value as a Java object matching the type of the tool argument
     * @throws IllegalArgumentException if the default value cannot be converted to the target type or there is no converter for the target type.
     */
    @SuppressWarnings("unchecked")
    public static Object convertDefaultValueToArgType(ToolMetadata toolMetadata, ArgumentMetadata argMetadata) {
        String defaultValue = argMetadata.defaultValue();
        Type type = TypeUtility.box(argMetadata.type());
        try {
            DefaultValueConverter<?> converter = BuiltinDefaultValueConverters.CONVERTERS.get(type);
            if (converter != null) {
                return converter.convert(defaultValue);
            }
            if (type instanceof Class clazz) {
                if (clazz.isEnum()) {
                    return Enum.valueOf(clazz.asSubclass(Enum.class), defaultValue);
                }
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0020E.defaultvalue.conversion.error", toolMetadata.name(), argMetadata.name(), argMetadata.type(),
                                                                defaultValue));
        }
<<<<<<< HEAD

        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0017E.missing.toolarg.defaultvalue.converter", toolMetadata.name(), argMetadata.name(), argMetadata.type()));
    }

    public List<String> generateArgumentMismatchData(Set<String> processed, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(processed);
        Set<String> extra = new HashSet<>(processed);
        extra.removeAll(expected);
        ArrayList<String> data = new ArrayList<>();
        if (!extra.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.extra.arguments", extra));
        }
        if (!missing.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.missing.arguments", missing));
        }
        return !data.isEmpty() ? data : null;
    }

}
||||||| parent of 33f4e591db2 (Fix merge conflicts)
<<<<<<< HEAD
        throw new IllegalArgumentException(
                                           "Unable to convert the default value for argument type [" + type
                                           + "] - provide a custom converter implementation");
    }

    public static final Map<Type, DefaultValueConverter<?>> CONVERTERS = Map.of(
                                                                                Boolean.class, new BuiltinDefaultValueConverters.BooleanConverter(),
                                                                                Byte.class, new BuiltinDefaultValueConverters.ByteConverter(),
                                                                                Short.class, new BuiltinDefaultValueConverters.ShortConverter(),
                                                                                Integer.class, new BuiltinDefaultValueConverters.IntegerConverter(),
                                                                                Long.class, new BuiltinDefaultValueConverters.LongConverter(),
                                                                                Float.class, new BuiltinDefaultValueConverters.FloatConverter(),
                                                                                Double.class, new BuiltinDefaultValueConverters.DoubleConverter(),
                                                                                Character.class, new BuiltinDefaultValueConverters.CharacterConverter());

    /**
     * Converts primitive types to their wrapper classes
     *
     * @param type the type to be boxed
     * @return the boxed wrapper type if {@code type} is a primitive, otherwise it returns {@code type}
     */
    public static Type box(Type type) {
        if (type instanceof Class clazz) {
            if (!clazz.isPrimitive()) {
                return type;
            } else if (clazz.equals(Boolean.TYPE)) {
                return Boolean.class;
            } else if (clazz.equals(Character.TYPE)) {
                return Character.class;
            } else if (clazz.equals(Byte.TYPE)) {
                return Byte.class;
            } else if (clazz.equals(Short.TYPE)) {
                return Short.class;
            } else if (clazz.equals(Integer.TYPE)) {
                return Integer.class;
            } else if (clazz.equals(Long.TYPE)) {
                return Long.class;
            } else if (clazz.equals(Float.TYPE)) {
                return Float.class;
            } else if (clazz.equals(Double.TYPE)) {
                return Double.class;
            }
        }
        return type;
    }

    public List<String> generateArgumentMismatchData(Set<String> processed, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(processed);
        Set<String> extra = new HashSet<>(processed);
        extra.removeAll(expected);
        ArrayList<String> data = new ArrayList<>();
        if (!extra.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.extra.arguments", extra));
        }
        if (!missing.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.missing.arguments", missing));
        }
        return !data.isEmpty() ? data : null;
    }

||||||| parent of e0cbf5803b0 (test: unit tests for tool arg default value)
        throw new IllegalArgumentException(
                                           "Unable to convert the default value for argument type [" + type
                                           + "] - provide a custom converter implementation");
    }

    public static final Map<Type, DefaultValueConverter<?>> CONVERTERS = Map.of(
                                                                                Boolean.class, new BuiltinDefaultValueConverters.BooleanConverter(),
                                                                                Byte.class, new BuiltinDefaultValueConverters.ByteConverter(),
                                                                                Short.class, new BuiltinDefaultValueConverters.ShortConverter(),
                                                                                Integer.class, new BuiltinDefaultValueConverters.IntegerConverter(),
                                                                                Long.class, new BuiltinDefaultValueConverters.LongConverter(),
                                                                                Float.class, new BuiltinDefaultValueConverters.FloatConverter(),
                                                                                Double.class, new BuiltinDefaultValueConverters.DoubleConverter(),
                                                                                Character.class, new BuiltinDefaultValueConverters.CharacterConverter());

    /**
     * Converts primitive types to their wrapper classes
     *
     * @param type the type to be boxed
     * @return the boxed wrapper type if {@code type} is a primitive, otherwise it returns {@code type}
     */
    public static Type box(Type type) {
        if (type instanceof Class clazz) {
            if (!clazz.isPrimitive()) {
                return type;
            } else if (clazz.equals(Boolean.TYPE)) {
                return Boolean.class;
            } else if (clazz.equals(Character.TYPE)) {
                return Character.class;
            } else if (clazz.equals(Byte.TYPE)) {
                return Byte.class;
            } else if (clazz.equals(Short.TYPE)) {
                return Short.class;
            } else if (clazz.equals(Integer.TYPE)) {
                return Integer.class;
            } else if (clazz.equals(Long.TYPE)) {
                return Long.class;
            } else if (clazz.equals(Float.TYPE)) {
                return Float.class;
            } else if (clazz.equals(Double.TYPE)) {
                return Double.class;
            }
        }
        return type;
    }

    public List<String> generateArgumentMismatchData(Set<String> processed, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(processed);
        Set<String> extra = new HashSet<>(processed);
        extra.removeAll(expected);
        ArrayList<String> data = new ArrayList<>();
        if (!extra.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.extra.arguments", extra));
        }
        if (!missing.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.missing.arguments", missing));
        }
        return !data.isEmpty() ? data : null;
    }

    public Bean<?> getBean() {
        return metadata.bean();
    }
=======
        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0019E.missing.toolarg.defaultvalue.converter", metadata.name(), argName, type));
    }
>>>>>>> e0cbf5803b0 (test: unit tests for tool arg default value)
}
=======
        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0019E.missing.toolarg.defaultvalue.converter", metadata.name(), argName, type));
    }

    public List<String> generateArgumentMismatchData(Set<String> processed, Set<String> expected) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(processed);
        Set<String> extra = new HashSet<>(processed);
        extra.removeAll(expected);
        ArrayList<String> data = new ArrayList<>();
        if (!extra.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.extra.arguments", extra));
        }
        if (!missing.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.missing.arguments", missing));
        }
        return !data.isEmpty() ? data : null;
    }

}
>>>>>>> 33f4e591db2 (Fix merge conflicts)

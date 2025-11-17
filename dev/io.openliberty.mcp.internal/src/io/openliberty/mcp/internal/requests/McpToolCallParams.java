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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
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

    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    private Map<String, Object> parseArguments(JsonObject arguments, Jsonb jsonb) {
        Map<String, ArgumentMetadata> metadatas = metadata.arguments();
        Map<String, Object> result = new HashMap<>();

        HashSet<String> argsProcessed = new HashSet<>();
        for (var entry : arguments.entrySet()) {
            String argName = entry.getKey();
            JsonValue argValue = entry.getValue();
            ArgumentMetadata argMetadata = metadatas.get(argName);
            if (argMetadata != null) {
                String json = jsonb.toJson(argValue);
                result.put(argName, jsonb.fromJson(json, argMetadata.type()));
            }

        }
        validateProcessedArgs(argsProcessed, metadatas.keySet());

        return result;
    }

    /**
     * The null value to use for different types. Null for objects or 0 for primitives.
     *
     * @param type the type to get the null value for
     * @return the null value for the class inputted as a parameter
     */
    public static Object nullToolArgValue(Class<?> type) {
        if (!type.isPrimitive())
            return null;
        if (type == boolean.class)
            return false;
        if (type == char.class)
            return '\0';
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0f;
        if (type == double.class)
            return 0d;
        return null;
    }

    /**
     * Converts a the value stored in a string to a Java object of type {@code type}
     *
     * @param value the string to be converted into a Java object
     * @param type the type of Java object you want the string to be converted to
     * @return the value as a Java object of type {@code type}
     */
    public static Object convert(String value, Type type) {
        if (String.class.equals(type)) {
            return value;
        }
        type = box(type);
        DefaultValueConverter<?> converter = CONVERTERS.get(type);
        if (converter != null) {
            return converter.convert(value);
        }
        if (type instanceof Class clazz) {
            if (clazz.isEnum()) {
                for (Object constant : clazz.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase(value)) {
                        return constant;
                    }
                }
            }
        }
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

}

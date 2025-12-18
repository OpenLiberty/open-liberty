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
import java.util.stream.Collectors;

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

    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    private Map<String, Object> parseArguments(JsonObject requestArguments, Jsonb jsonb) {
        Map<String, ArgumentMetadata> metadatas = metadata.arguments();
        Map<String, Object> result = new HashMap<>();

        boolean hasMissingArgs = false;
        int requestArgumentsProcessed = 0;

        for (var argEntry : metadatas.entrySet()) {
            String argName = argEntry.getKey();
            ArgumentMetadata argMetadata = argEntry.getValue();
            JsonValue argValue = requestArguments.get(argName);
            if (argValue != null) {
                String argValueJson = jsonb.toJson(argValue);
                result.put(argName, jsonb.fromJson(argValueJson, argMetadata.type()));
                requestArgumentsProcessed++;
            } else if (!argMetadata.required()) {
                if (!argMetadata.defaultValue().isEmpty()) {
                    result.put(argName, convertDefaultValueToArgType(metadata, argMetadata));
                } else {
                    result.put(argName, emptyToolArgValue(argMetadata.type())); //blank result for no value provided for optional argument
                }
            } else {
                // Required argument was not provided in the request
                hasMissingArgs = true;
                break;
            }
        }

        if (hasMissingArgs || requestArgumentsProcessed != requestArguments.size()) {
            Set<String> requiredArgs = metadatas.values().stream()
                                                .filter(arg -> arg.required())
                                                .map(arg -> arg.name())
                                                .collect(Collectors.toSet());
            List<String> data = generateArgumentMismatchData(requestArguments.keySet(), metadatas.keySet(), requiredArgs);
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, data);
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
        DefaultValueConverter<?> converter = BuiltinDefaultValueConverters.CONVERTERS.get(type);

        if (converter != null) {
            try {
                return converter.convert(defaultValue);
            } catch (Exception e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0020E.defaultvalue.conversion.error", toolMetadata.name(), argMetadata.name(), argMetadata.type(),
                                                                    defaultValue, e),
                                                   e);
            }
        }

        if (type instanceof Class clazz) {
            if (clazz.isEnum()) {
                return Enum.valueOf(clazz.asSubclass(Enum.class), defaultValue);
            }
        }

        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWMCM0017E.missing.toolarg.defaultvalue.converter", toolMetadata.name(), argMetadata.name(), argMetadata.type()));
    }

    private List<String> generateArgumentMismatchData(Set<String> receivedArguments, Set<String> allowedArguments, Set<String> requiredArguments) {
        Set<String> missingArguments = new HashSet<>(requiredArguments);
        missingArguments.removeAll(receivedArguments);
        Set<String> extraArguments = new HashSet<>(receivedArguments);
        extraArguments.removeAll(allowedArguments);
        ArrayList<String> data = new ArrayList<>();
        if (!extraArguments.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.extra.arguments", extraArguments));
        }
        if (!missingArguments.isEmpty()) {
            data.add(Tr.formatMessage(tc, "jsonrpc.missing.arguments", missingArguments));
        }
        return !data.isEmpty() ? data : null;
    }

}
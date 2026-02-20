/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
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
        if (parsedArguments != null) {
            return parsedArguments;
        }

        JsonObject safeArguments = (this.arguments != null) ? this.arguments : JsonValue.EMPTY_JSON_OBJECT;
        parsedArguments = parseArguments(safeArguments, jsonb);
        return parsedArguments;
    }

    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    @FFDCIgnore(NumberFormatException.class)
    private Map<String, Object> parseArguments(JsonObject requestArguments, Jsonb jsonb) {

        List<ToolArgument> metadatas = metadata.arguments();
        Map<String, Object> result = new HashMap<>();

        boolean hasMissingArgs = false;
        int requestArgumentsProcessed = 0;

        for (ToolArgument argMetadata : metadatas) {
            String argName = argMetadata.name();
            JsonValue argValue = requestArguments.get(argName);
            if (argValue != null) {
                String argValueJson = jsonb.toJson(argValue);
                try {
                    result.put(argName, jsonb.fromJson(argValueJson, argMetadata.type()));
                    requestArgumentsProcessed++;
                } catch (JsonbException | NumberFormatException e) {
                    throw new ToolCallException(
                                                Tr.formatMessage(tc, "argument.conversion.failed",
                                                                 argName, argMetadata.type().getTypeName(), argValueJson),
                                                e);
                }
            } else if (!argMetadata.required()) {
                //Argument is optional and not provided, resolve the default value
                result.put(argName, DefaultValueResolver.resolveDefaultValue(argMetadata));
            } else {
                // Required argument was not provided in the request
                hasMissingArgs = true;
                break;
            }
        }

        if (hasMissingArgs || requestArgumentsProcessed != requestArguments.size()) {
            Set<String> requiredArgs = metadatas.stream()
                                                .filter(arg -> arg.required())
                                                .map(arg -> arg.name())
                                                .collect(Collectors.toSet());
            Set<String> allowedArgs = metadatas.stream()
                                               .map(arg -> arg.name())
                                               .collect(Collectors.toSet());
            String data = generateArgumentMismatchMessage(requestArguments.keySet(), allowedArgs, requiredArgs);

            throw new ToolCallException(data);
        }
        return result;
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
    public static Object convertDefaultValueToArgType(ToolMetadata toolMetadata, ToolArgument argMetadata) {
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

    /**
     * Builds user-facing error message describing invalid tool arguments.
     *
     * <p>The message reports:
     * <ul>
     * <li>Arguments that were provided but are not supported</li>
     * <li>Required arguments that were not provided</li>
     * </ul>
     *
     * @param receivedArguments arguments supplied in the request
     * @param allowedArguments arguments supported by the tool
     * @param requiredArguments arguments required by the tool
     * @return a combined error message for extra and/or missing arguments
     */
    private String generateArgumentMismatchMessage(Set<String> receivedArguments, Set<String> allowedArguments, Set<String> requiredArguments) {

        Set<String> missingArguments = new HashSet<>(requiredArguments);
        missingArguments.removeAll(receivedArguments);

        Set<String> extraArguments = new HashSet<>(receivedArguments);
        extraArguments.removeAll(allowedArguments);

        List<String> messages = new ArrayList<>();
        if (!extraArguments.isEmpty()) {
            messages.add(Tr.formatMessage(tc, "extra.arguments", extraArguments));
        }

        if (!missingArguments.isEmpty()) {
            messages.add(Tr.formatMessage(tc, "missing.arguments", missingArguments));
        }
        return String.join(" ", messages);
    }

}

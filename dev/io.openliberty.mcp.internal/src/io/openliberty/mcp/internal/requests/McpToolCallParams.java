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

import io.openliberty.mcp.internal.ConverterRegistry;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
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

    public Map<String, Object> getArguments(Jsonb jsonb, ConverterRegistry converterRegistry) {
        if (parsedArguments != null) {
            return parsedArguments;
        }

        JsonObject safeArguments = (this.arguments != null) ? this.arguments : JsonValue.EMPTY_JSON_OBJECT;
        parsedArguments = parseArguments(safeArguments, jsonb, converterRegistry);
        return parsedArguments;
    }

    public JsonObject getMeta() {
        return meta;
    }

    public void setMeta(JsonObject meta) {
        this.meta = meta;
    }

    @FFDCIgnore(NumberFormatException.class)
    private Map<String, Object> parseArguments(JsonObject requestArguments, Jsonb jsonb, ConverterRegistry converterRegistry) {

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
                result.put(argName, DefaultValueResolver.resolveDefaultValue(name, argMetadata, converterRegistry));
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

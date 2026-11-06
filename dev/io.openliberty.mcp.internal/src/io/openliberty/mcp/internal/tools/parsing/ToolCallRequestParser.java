/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools.parsing;

import java.util.ArrayList;
import java.util.Collections;
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
import io.openliberty.mcp.internal.McpInternal;
import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.DefaultValueResolver;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.parsing.CommonParser;
import io.openliberty.mcp.internal.security.Authorizer;
import io.openliberty.mcp.internal.tools.ToolCallRequest;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;

@ApplicationScoped
public class ToolCallRequestParser {

    private static final TraceComponent tc = Tr.register(ToolCallRequestParser.class);

    private final ToolRegistry toolRegistry;
    private final ConverterRegistry converterRegistry;
    private final Jsonb jsonb;

    // No-args constructor for CDI
    ToolCallRequestParser() {
        this.toolRegistry = null;
        this.converterRegistry = null;
        this.jsonb = null;
    }

    @Inject
    public ToolCallRequestParser(ToolRegistry toolRegistry, ConverterRegistry converterRegistry, @McpInternal Jsonb jsonb) {
        this.toolRegistry = toolRegistry;
        this.converterRegistry = converterRegistry;
        this.jsonb = jsonb;
    }

    /**
     * Reads the information needed for metrics and error reporting without parsing the whole message.
     */
    public PartialToolCallRequest preParse(McpOperationRequest opRequest) {
        String toolName = opRequest.args().getString("name", null);
        ToolMetadata toolMetadata;
        if (toolName != null) {
            toolMetadata = toolRegistry.getTool(toolName);
        } else {
            toolMetadata = null;
        }

        return new PartialToolCallRequest(toolName, toolMetadata);
    }

    public ToolCallRequest parse(McpOperationRequest opRequest, PartialToolCallRequest partialToolCallRequest, McpTransport transport) {
        ToolCallRequestTO requestTO = CommonParser.parseParamsTransferObject(jsonb, opRequest.args(), ToolCallRequestTO.class);

        // Check tool exists
        ToolMetadata tool = partialToolCallRequest.tool();
        if (tool == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Attempt to call non-existant tool: " + partialToolCallRequest.toolName());
            }
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of("Method " + requestTO.name() + " not found"));
        }

        // Check user authorized for tool
        Authorizer.requireAuthorized(transport, tool);

        // Parse tool arguments
        JsonObject safeArguments = (requestTO.arguments() != null) ? requestTO.arguments() : JsonValue.EMPTY_JSON_OBJECT;
        Map<String, Object> parsedArguments = parseArguments(tool, safeArguments);

        Map<String, Object> safeMetadata = requestTO.metadata() != null ? requestTO.metadata() : Collections.emptyMap();

        return new ToolCallRequest(opRequest, tool, parsedArguments, safeMetadata);
    }

    @FFDCIgnore(NumberFormatException.class)
    private Map<String, Object> parseArguments(ToolMetadata tool, JsonObject requestArguments) {

        List<ToolArgument> arguments = tool.arguments();
        Map<String, Object> result = new HashMap<>();

        boolean hasMissingArgs = false;
        int requestArgumentsProcessed = 0;

        for (ToolArgument argument : arguments) {
            String argName = argument.name();
            JsonValue argValue = requestArguments.get(argName);
            if (argValue != null) {
                try {
                    result.put(argName, jsonb.fromJson(argValue.toString(), argument.type()));
                    requestArgumentsProcessed++;
                } catch (JsonbException | NumberFormatException e) {
                    throw new ToolCallException(
                                                Tr.formatMessage(tc, "argument.conversion.failed",
                                                                 argName, argument.type().getTypeName(), argValue.toString()),
                                                e);
                }
            } else if (!argument.required()) {
                //Argument is optional and not provided, resolve the default value
                result.put(argName, DefaultValueResolver.resolveDefaultValue(tool.name(), argument, converterRegistry));
            } else {
                // Required argument was not provided in the request
                hasMissingArgs = true;
                break;
            }
        }

        if (hasMissingArgs || requestArgumentsProcessed != requestArguments.size()) {
            Set<String> requiredArgs = arguments.stream()
                                                .filter(arg -> arg.required())
                                                .map(arg -> arg.name())
                                                .collect(Collectors.toSet());
            Set<String> allowedArgs = arguments.stream()
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

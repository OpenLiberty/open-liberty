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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

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
    private Object[] parsedArguments;

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

    public Method getMethod() {
        return metadata.method().getJavaMember();
    }

    public Object[] getArguments(Jsonb jsonb) {
        if (this.arguments == null) {
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, List.of(Tr.formatMessage(tc, "jsonrpc.missing.params")));
        }
        if (parsedArguments == null) {
            parsedArguments = parseArguments(arguments, jsonb);
        }
        return parsedArguments;
    }

    private Object[] parseArguments(JsonObject arguments2, Jsonb jsonb) {
        JsonObject argsObject = arguments2.asJsonObject();
        Map<String, ArgumentMetadata> arguments = metadata.arguments();
        List<SpecialArgumentMetadata> specialArguments = metadata.specialArguments();
        Object[] results = new Object[arguments.size() + specialArguments.size()];

        HashSet<String> argsProcessed = new HashSet<>();
        for (var entry : argsObject.entrySet()) {
            String argName = entry.getKey();
            JsonValue argValue = entry.getValue();
            ArgumentMetadata argMetadata = metadata.arguments().get(argName);
            if (argMetadata != null) {
                String json = jsonb.toJson(argValue);
                results[argMetadata.index()] = jsonb.fromJson(json, argMetadata.type());
            }
            argsProcessed.add(argName);
        }

        validateProcessedArgs(argsProcessed, arguments.keySet());

        return results;
    }

    private void validateProcessedArgs(Set<String> processedArgs, Set<String> expectedArgs) {
        if (!processedArgs.equals(expectedArgs)) {
            List<String> data = generateArgumentMismatchData(processedArgs, expectedArgs);
            throw new JSONRPCException(JSONRPCErrorCode.INVALID_PARAMS, data);
        }
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
}

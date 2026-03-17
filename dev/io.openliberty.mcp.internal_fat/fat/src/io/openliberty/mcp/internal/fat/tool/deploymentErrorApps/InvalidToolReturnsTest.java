/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.deploymentErrorApps;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.meta.Meta;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvalidToolReturnsTest {

    @Tool(name = "testArrayResponse", title = "Array of ints",
          description = "A tool to return an array of ints", structuredContent = true)
    public int[] testArrayResponse() {
        return new int[] { 1, 2, 3, 4, 5 };
    }

    @Tool(name = "testListStringResponse", title = "String List",
          description = "A tool to return a list of strings", structuredContent = true)
    public List<String> testListStringResponse() {
        return List.of("red", "blue", "yellow");
    }

    @Tool(name = "asyncListObjectTool", title = "Async asyncListObjectTool", description = "A tool to return a list of string asynchronously", structuredContent = true)
    public CompletionStage<List<String>> asyncListObjectTool() {
        return null;
    }

    // This test is to ensure return types with custom schemas can be returned without an erro even thought it could be invalid
    @Tool(name = "addPersonToListToolResponseSchemaBasedReturnType", title = "adds person to people list", description = "adds person to people list", structuredContent = true)
    public @Schema(value = "{ \"$defs\": { \"Address\": { \"type\": \"object\", \"properties\": { \"number\": { \"type\": \"integer\" }, \"street\": { \"description\": \"A street object to represent complex streets\", \"type\": \"object\", \"properties\": { \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ] }, \"postcode\": { \"type\": \"string\" } }, \"required\": [ \"number\", \"street\", \"postcode\" ] }, \"Person\": { \"description\": \"A person object contains address, company objects\", \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"company\": { \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/$defs/Address\" }, \"name\": { \"type\": \"string\" }, \"shareholders\": { \"description\": \"A list of shareholder (person object)\", \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" } }, \"shareholderRegistry\": { \"type\": \"object\", \"properties\": { \"value\": { \"$ref\": \"#/$defs/person\" }, \"key\": { \"type\": \"integer\" } }, \"required\": [] } }, \"required\": [ \"name\", \"address\", \"shareholders\" ] }, \"fullname\": { \"type\": \"string\" } }, \"required\": [ \"fullname\", \"address\", \"company\" ] } }, \"type\": \"array\", \"items\": { \"$ref\": \"#/$defs/Person\" }, \"description\": \"Returns list of person object\" }",
                   description = "Returns list of person object") ToolResponse addPersonToListToolResponseSchemaBasedReturnType(@ToolArg(name = "employeeList",
                                                                                                                                         description = "List of people") List<String> employeeNameList,
                                                                                                                                @ToolArg(name = "person",
                                                                                                                                         description = "Person object") Optional<String> personName,
                                                                                                                                Meta meta) {
        return null;
    }

}

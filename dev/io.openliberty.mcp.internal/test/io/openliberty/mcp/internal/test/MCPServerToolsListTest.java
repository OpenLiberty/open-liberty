/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.ToolDescription;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class MCPServerToolsListTest {

    Jsonb jsonb;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setup() throws Exception {
        jsonb = JsonbBuilder.create();
    }

    private List<ToolDescription> generateResponse(Tool numberTestTool, Map<String, ArgumentMetadata> arguments) {
        return List.of(new ToolDescription(ToolMetadataTestUtility.createFrom(numberTestTool, arguments, Collections.emptyList())));
    }

    @Test
    public void testJSONNumberFromPrimitives() throws Exception {
        Tool numberTestTool = Literals.tool("parseAllPrimitiveNumbers", "parse All PrimitiveNumbers", "Checks if primitives arguments are handled by the Json Serialiser");
        Map<String, ArgumentMetadata> arguments = Map.of("var1", new ArgumentMetadata(long.class, 0, "long -> number", true, false),
                                                         "var2", new ArgumentMetadata(double.class, 1, "double -> number", true, false),
                                                         "var3", new ArgumentMetadata(byte.class, 1, "byte -> number", true, false),
                                                         "var4", new ArgumentMetadata(float.class, 1, "float -> number", true, false),
                                                         "var5", new ArgumentMetadata(short.class, 1, "short -> number", true, false));

        String responseString = jsonb.toJson(generateResponse(numberTestTool, arguments));
        String expectedString = """
                        [
                              {
                                  "name": "parseAllPrimitiveNumbers",
                                  "title": "parse All PrimitiveNumbers",
                                  "description": "Checks if primitives arguments are handled by the Json Serialiser",
                                  "inputSchema": {
                                      "type": "object",
                                      "properties": {
                                          "var1": {
                                              "description": "long -> number",
                                              "type": "number"
                                          },
                                          "var2": {
                                              "description": "double -> number",
                                              "type": "number"
                                          },
                                          "var3": {
                                              "description": "byte -> number",
                                              "type": "number"
                                          },
                                          "var4": {
                                              "description": "float -> number",
                                              "type": "number"
                                          },
                                          "var5": {
                                              "description": "short -> number",
                                              "type": "number"
                                          }
                                      },
                                      "required": [
                                          "var1",
                                          "var2",
                                          "var3",
                                          "var4",
                                          "var5"
                                      ]
                                  }
                              }
                          ]

                                                                  """;
        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    @Test
    public void testJSONNumberFromWrapperPrimitives() throws Exception {
        Tool numberTestTool = Literals.tool("parseAllWrapperNumbers", "parse All Wrapper Numbers", "Checks if wrapper type primitive arguments are handled by the Json Serialiser");
        Map<String, ArgumentMetadata> arguments = Map.of("var1", new ArgumentMetadata(Long.class, 0, "Long -> number", true, false),
                                                         "var2", new ArgumentMetadata(Double.class, 1, "Double -> number", true, false),
                                                         "var3", new ArgumentMetadata(Byte.class, 1, "Byte -> number", true, false),
                                                         "var4", new ArgumentMetadata(Float.class, 1, "Float -> number", true, false),
                                                         "var5", new ArgumentMetadata(Short.class, 1, "Short -> number", true, false));
        String responseString = jsonb.toJson(generateResponse(numberTestTool, arguments));
        String expectedString = """
                        [
                             {
                                 "name": "parseAllWrapperNumbers",
                                 "title": "parse All Wrapper Numbers",
                                 "description": "Checks if wrapper type primitive arguments are handled by the Json Serialiser",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "var1": {
                                             "description": "Long -> number",
                                             "type": "number"
                                         },
                                         "var2": {
                                             "description": "Double -> number",
                                             "type": "number"
                                         },
                                         "var3": {
                                             "description": "Byte -> number",
                                             "type": "number"
                                         },
                                         "var4": {
                                             "description": "Float -> number",
                                             "type": "number"
                                         },
                                         "var5": {
                                             "description": "Short -> number",
                                             "type": "number"
                                         }
                                     },
                                     "required": [
                                         "var1",
                                         "var2",
                                         "var3",
                                         "var4",
                                         "var5"
                                     ]
                                 }
                             }
                         ]

                                                                 """;
        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    @Test
    public void testJSONString() throws Exception {
        Tool stringTestTool = Literals.tool("parseStrings", "parseStrings", "Checks string types are handled by the Json Serialiser");
        Map<String, ArgumentMetadata> arguments = Map.of("var1", new ArgumentMetadata(String.class, 0, "String -> string", true, false),
                                                         "var2", new ArgumentMetadata(Character.class, 1, "Character -> string", true, false),
                                                         "var3", new ArgumentMetadata(char.class, 1, "char -> string", true, false));
        String responseString = jsonb.toJson(generateResponse(stringTestTool, arguments));
        String expectedString = """
                        [
                             {
                                 "name": "parseStrings",
                                 "title": "parseStrings",
                                 "description": "Checks string types are handled by the Json Serialiser",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "var1": {
                                             "description": "String -> string",
                                             "type": "string"
                                         },
                                         "var2": {
                                             "description": "Character -> string",
                                             "type": "string"
                                         },
                                         "var3": {
                                             "description": "char -> string",
                                             "type": "string"
                                         }
                                     },
                                     "required": [
                                         "var1",
                                         "var2",
                                         "var3"
                                     ]
                                 }
                             }
                         ]

                                                                 """;
        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    @Test
    public void testJSONInteger() throws Exception {
        Tool intTestTool = Literals.tool("parseInts", "parseInts", "Checks int types are handled by the Json Serialiser");
        Map<String, ArgumentMetadata> arguments = Map.of("var1", new ArgumentMetadata(int.class, 0, "int -> int", true, false),
                                                         "var2", new ArgumentMetadata(Integer.class, 1, "Integer -> int", true, false),
                                                         "var3", new ArgumentMetadata(Integer.class, 1, "Integer -> int", true, false));
        String responseString = jsonb.toJson(generateResponse(intTestTool, arguments));
        String expectedString = """
                        [
                             {
                                 "name": "parseInts",
                                 "title": "parseInts",
                                 "description": "Checks int types are handled by the Json Serialiser",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "var1": {
                                             "description": "int -> int",
                                             "type": "integer"
                                         },
                                         "var2": {
                                             "description": "Integer -> int",
                                             "type": "integer"
                                         },
                                         "var3": {
                                             "description": "Integer -> int",
                                             "type": "integer"
                                         }
                                     },
                                     "required": [
                                         "var1",
                                         "var2",
                                         "var3"
                                     ]
                                 }
                             }
                         ]

                                                                 """;
        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    @Test
    public void testJSONBoolean() throws Exception {
        Tool booleanTestTool = Literals.tool("parseBooleans", "parseBooleans", "Checks boolean types are handled by the Json Serialiser");
        Map<String, ArgumentMetadata> arguments = Map.of("var1", new ArgumentMetadata(boolean.class, 0, "boolean -> boolean", true, false),
                                                         "var2", new ArgumentMetadata(Boolean.class, 1, "Boolean -> boolean", true, false),
                                                         "var3", new ArgumentMetadata(Boolean.class, 1, "Boolean -> boolean", true, false));
        String responseString = jsonb.toJson(generateResponse(booleanTestTool, arguments));
        String expectedString = """
                        [
                             {
                                 "name": "parseBooleans",
                                 "title": "parseBooleans",
                                 "description": "Checks boolean types are handled by the Json Serialiser",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "var1": {
                                             "description": "boolean -> boolean",
                                             "type": "boolean"
                                         },
                                         "var2": {
                                             "description": "Boolean -> boolean",
                                             "type": "boolean"
                                         },
                                         "var3": {
                                             "description": "Boolean -> boolean",
                                             "type": "boolean"
                                         }
                                     },
                                     "required": [
                                         "var1",
                                         "var2",
                                         "var3"
                                     ]
                                 }
                             }
                         ]

                                                                 """;
        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    @Test
    public void testJSONSerialization() throws Exception {

        //Weather Tool
        Tool weatherTool = Literals.tool("get_weather", "Weather Information Provider", "Get current weather information for a location");
        Map<String, ArgumentMetadata> arguments = Map.of("location", new ArgumentMetadata(String.class, 0, "City in a country", true, false),
                                                         "temperature", new ArgumentMetadata(double.class, 1, "in degrees Celsius", true, false),
                                                         "humidity", new ArgumentMetadata(int.class, 2, "Relative Humidity", true, false));
        // Addition Tool
        Tool additionTool = Literals.tool("addition_calculator", "The Calculator Addition Tool", "Can add two floating point numbers");
        Map<String, ArgumentMetadata> arguments2 = Map.of("number1", new ArgumentMetadata(double.class, 0, "operand 1", true, false),
                                                          "number2", new ArgumentMetadata(double.class, 1, "operand 2", true, false));
        // Subtraction Tool
        Tool subtractionTool = Literals.tool("subtraction_calculator", "The Calculator Subtraction Tool", "Can subtract two integers");
        Map<String, ArgumentMetadata> arguments3 = Map.of("number1", new ArgumentMetadata(int.class, 0, "operand 1", true, false),
                                                          "number2", new ArgumentMetadata(int.class, 1, "operand 2", true, false));
        // True or False Tool
        Tool booleanTool = Literals.tool("and_operator", "Boolean And Operator", "Does a Boolean And Operation on two boolean variables");
        Map<String, ArgumentMetadata> arguments4 = Map.of("var1", new ArgumentMetadata(boolean.class, 0, "operand 1", true, false),
                                                          "var2", new ArgumentMetadata(boolean.class, 1, "operand 2", true, false));

        LinkedList<ToolDescription> toolDescriptions = new LinkedList<>();
        toolDescriptions.add(new ToolDescription(ToolMetadataTestUtility.createFrom(weatherTool, arguments, Collections.emptyList())));
        toolDescriptions.add(new ToolDescription(ToolMetadataTestUtility.createFrom(additionTool, arguments2, Collections.emptyList())));
        toolDescriptions.add(new ToolDescription(ToolMetadataTestUtility.createFrom(subtractionTool, arguments3, Collections.emptyList())));
        toolDescriptions.add(new ToolDescription(ToolMetadataTestUtility.createFrom(booleanTool, arguments4, Collections.emptyList())));

        String responseString = jsonb.toJson(toolDescriptions);
        String expectedString = """
                        [
                            {
                                "description": "Get current weather information for a location",
                                "inputSchema": {
                                    "properties": {
                                        "temperature": {
                                            "description": "in degrees Celsius",
                                            "type": "number"
                                        },
                                        "humidity": {
                                            "description": "Relative Humidity",
                                            "type": "integer"
                                        },
                                        "location": {
                                            "description": "City in a country",
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "temperature",
                                        "humidity",
                                        "location"
                                    ],
                                    "type": "object"
                                },
                                "name": "get_weather",
                                "title": "Weather Information Provider"
                            },
                            {
                                "description": "Does a Boolean And Operation on two boolean variables",
                                "inputSchema": {
                                    "properties": {
                                        "var2": {
                                            "description": "operand 2",
                                            "type": "boolean"
                                        },
                                        "var1": {
                                            "description": "operand 1",
                                            "type": "boolean"
                                        }
                                    },
                                    "required": [
                                        "var2",
                                        "var1"
                                    ],
                                    "type": "object"
                                },
                                "name": "and_operator",
                                "title": "Boolean And Operator"
                            },
                            {
                                "description": "Can subtract two integers",
                                "inputSchema": {
                                    "properties": {
                                        "number1": {
                                            "description": "operand 1",
                                            "type": "integer"
                                        },
                                        "number2": {
                                            "description": "operand 2",
                                            "type": "integer"
                                        }
                                    },
                                    "required": [
                                        "number1",
                                        "number2"
                                    ],
                                    "type": "object"
                                },
                                "name": "subtraction_calculator",
                                "title": "The Calculator Subtraction Tool"
                            },
                            {
                                "description": "Can add two floating point numbers",
                                "inputSchema": {
                                    "properties": {
                                        "number2": {
                                            "description": "operand 2",
                                            "type": "number"
                                        },
                                        "number1": {
                                            "description": "operand 1",
                                            "type": "number"
                                        }
                                    },
                                    "required": [
                                        "number1",
                                        "number2"
                                    ],
                                    "type": "object"
                                },
                                "name": "addition_calculator",
                                "title": "The Calculator Addition Tool"
                            }
                        ]
                        """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }
}

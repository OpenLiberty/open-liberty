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

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.internal.ToolDescription;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.testutils.TestUtils;
import io.openliberty.mcp.tools.ToolResponse;
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
        SchemaRegistry.set(new SchemaRegistry());
        jsonb = JsonbBuilder.create();
    }

    private List<ToolDescription> generateResponse(ToolMetadata tm) {
        return List.of(new ToolDescription(tm));
    }

    @Tool(name = "parseAllPrimitiveNumbers", title = "parse All PrimitiveNumbers", description = "Checks if primitives arguments are handled by the Json Serialiser")
    public void parseAllPrimitiveNumbers(@ToolArg(name = "var1", description = "long -> integer") long var1,
                                         @ToolArg(name = "var2", description = "double -> number") double var2,
                                         @ToolArg(name = "var3", description = "byte -> integer") byte var3,
                                         @ToolArg(name = "var4", description = "float -> number") float var4,
                                         @ToolArg(name = "var5", description = "short -> integer") short var5) {}

    @Test
    public void testJSONNumberFromPrimitives() throws Exception {
        ToolMetadata tm = TestUtils.findTool(MCPServerToolsListTest.class, "parseAllPrimitiveNumbers");
        String responseString = jsonb.toJson(generateResponse(tm));
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
                                              "description": "long -> integer",
                                              "type": "integer"
                                          },
                                          "var2": {
                                              "description": "double -> number",
                                              "type": "number"
                                          },
                                          "var3": {
                                              "description": "byte -> integer",
                                              "type": "integer"
                                          },
                                          "var4": {
                                              "description": "float -> number",
                                              "type": "number"
                                          },
                                          "var5": {
                                              "description": "short -> integer",
                                              "type": "integer"
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

    @Tool(name = "parseAllWrapperNumbers", title = "parse All Wrapper Numbers", description = "Checks if wrapper type primitive arguments are handled by the Json Serialiser")
    public void parseAllWrapperNumbers(@ToolArg(name = "var1", description = "Long -> integer") Long var1,
                                       @ToolArg(name = "var2", description = "Double -> number") Double var2,
                                       @ToolArg(name = "var3", description = "Byte -> integer") Byte var3,
                                       @ToolArg(name = "var4", description = "Float -> number") Float var4,
                                       @ToolArg(name = "var5", description = "Short -> integer") Short var5) {}

    @Test
    public void testJSONNumberFromWrapperPrimitives() throws Exception {
        ToolMetadata tm = TestUtils.findTool(MCPServerToolsListTest.class, "parseAllWrapperNumbers");
        String responseString = jsonb.toJson(generateResponse(tm));
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
                                             "description": "Long -> integer",
                                             "type": "integer"
                                         },
                                         "var2": {
                                             "description": "Double -> number",
                                             "type": "number"
                                         },
                                         "var3": {
                                             "description": "Byte -> integer",
                                             "type": "integer"
                                         },
                                         "var4": {
                                             "description": "Float -> number",
                                             "type": "number"
                                         },
                                         "var5": {
                                             "description": "Short -> integer",
                                             "type": "integer"
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

    @Tool(name = "parseStrings", title = "parseStrings", description = "Checks string types are handled by the Json Serialiser")
    public void parseStrings(@ToolArg(name = "var1", description = "String -> string") String var1,
                             @ToolArg(name = "var2", description = "Character -> string") Character var2,
                             @ToolArg(name = "var3", description = "char -> string") char var3) {}

    @Test
    public void testJSONString() throws Exception {
        ToolMetadata tm = TestUtils.findTool(MCPServerToolsListTest.class, "parseStrings");
        String responseString = jsonb.toJson(generateResponse(tm));
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

    @Tool(name = "parseInts", title = "parseInts", description = "Checks int types are handled by the Json Serialiser")
    public void parseInts(@ToolArg(name = "var1", description = "int -> integer") int var1,
                          @ToolArg(name = "var2", description = "Integer -> integer") Integer var2,
                          @ToolArg(name = "var3", description = "Integer -> integer") Integer var3) {}

    @Test
    public void testJSONInteger() throws Exception {
        ToolMetadata tm = TestUtils.findTool(MCPServerToolsListTest.class, "parseInts");
        String responseString = jsonb.toJson(generateResponse(tm));
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
                                             "description": "int -> integer",
                                             "type": "integer"
                                         },
                                         "var2": {
                                             "description": "Integer -> integer",
                                             "type": "integer"
                                         },
                                         "var3": {
                                             "description": "Integer -> integer",
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

    @Tool(name = "parseBooleans", title = "parseBooleans", description = "Checks boolean types are handled by the Json Serialiser")
    public void parseBooleans(@ToolArg(name = "var1", description = "boolean -> boolean") boolean var1,
                              @ToolArg(name = "var2", description = "Boolean -> boolean") Boolean var2,
                              @ToolArg(name = "var3", description = "Boolean -> boolean") Boolean var3) {}

    @Test
    public void testJSONBoolean() throws Exception {
        ToolMetadata tm = TestUtils.findTool(MCPServerToolsListTest.class, "parseBooleans");
        String responseString = jsonb.toJson(generateResponse(tm));
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

    public record Weather(double temp, int humidity, String location, String weatherDescription) {};

    @Tool(name = "get_weather", title = "Weather Information Provider", description = "Get current weather information for a location", structuredContent = true)
    public Weather get_weather(@ToolArg(name = "location", description = "City in a country") String location,
                               @ToolArg(name = "temperature", description = "in degrees Celsius") double temperature,
                               @ToolArg(name = "humidity", description = "Relative Humidity") int humidity) {
        return null;
    }

    @Tool(name = "addition_calculator", title = "The Calculator Addition Tool", description = "Can add two floating point numbers", structuredContent = true)
    public double addition_calculator(@ToolArg(name = "number1", description = "operand 1") double number1,
                                      @ToolArg(name = "number2", description = "operand 2") double number2) {
        return 0.0;
    }

    @Tool(name = "subtraction_calculator", title = "The Calculator Subtraction Tool", description = "Can subtract two integers", structuredContent = false)
    public double subtraction_calculator(@ToolArg(name = "number1", description = "operand 1") int number1,
                                         @ToolArg(name = "number2", description = "operand 2") int number2) {
        return 0.0;
    }

    @Tool(name = "and_operator", title = "Boolean And Operator", description = "Does a Boolean And Operation on two boolean variables", structuredContent = true)
    public boolean and_operator(@ToolArg(name = "var1", description = "operand 1") boolean number1,
                                @ToolArg(name = "var2", description = "operand 2") boolean number2) {
        return false;
    }

    @Test
    public void testJSONSerialization() throws Exception {

        LinkedList<ToolDescription> toolDescriptions = new LinkedList<>();
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "get_weather")));
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "addition_calculator")));
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "subtraction_calculator")));
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "and_operator")));

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
                                "outputSchema": {
                                    "type": "object",
                                    "properties": {
                                        "temp": {
                                            "type": "number"
                                        },
                                        "humidity": {
                                            "type": "integer"
                                        },
                                        "location": {
                                            "type": "string"
                                        },
                                        "weatherDescription": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "temp",
                                        "humidity",
                                        "location",
                                        "weatherDescription"
                                    ]
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
                                "outputSchema": {
                                    "type": "boolean"
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
                                "outputSchema": {
                                    "type": "number"
                                },
                                "title": "The Calculator Addition Tool"
                            }
                        ]
                        """;

        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Tool(name = "toolResponseTest", title = "Test Tool Response", description = "tests Tool Response")
    public ToolResponse toolResponseTest(@ToolArg(name = "name", description = "name") String name) {
        return null;
        //comment
    }

    @Tool(name = "contentTest", title = "Test Content Response", description = "tests Content Response")
    public List<Content> contentTest(@ToolArg(name = "name", description = "name") String name) {
        return null;
        //comment
    }

    @Tool(name = "stringTest", title = "Test String Response", description = "tests String Response")
    public String stringTest(@ToolArg(name = "name", description = "name") String name) {
        return null;
        //comment
    }

    @Test
    public void testContentResponse() {
        LinkedList<ToolDescription> toolDescriptions = new LinkedList<>();
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "toolResponseTest")));
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "contentTest")));
        toolDescriptions.add(new ToolDescription(TestUtils.findTool(MCPServerToolsListTest.class, "stringTest")));

        String expectedString = """
                        [
                             {
                                 "name": "toolResponseTest",
                                 "title": "Test Tool Response",
                                 "description": "tests Tool Response",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "name": {
                                             "description": "name",
                                             "type": "string"
                                         }
                                     },
                                     "required": [
                                         "name"
                                     ]
                                 }
                             },
                             {
                                 "name": "contentTest",
                                 "title": "Test Content Response",
                                 "description": "tests Content Response",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "name": {
                                             "description": "name",
                                             "type": "string"
                                         }
                                     },
                                     "required": [
                                         "name"
                                     ]
                                 }
                             },
                             {
                                 "name": "stringTest",
                                 "title": "Test String Response",
                                 "description": "tests String Response",
                                 "inputSchema": {
                                     "type": "object",
                                     "properties": {
                                         "name": {
                                             "description": "name",
                                             "type": "string"
                                         }
                                     },
                                     "required": [
                                         "name"
                                     ]
                                 }
                             }
                         ]

                                                                 """;
        String responseString = jsonb.toJson(toolDescriptions);
        JSONAssert.assertEquals(expectedString, responseString, JSONCompareMode.NON_EXTENSIBLE);

    }
}

/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.exceptions.UnsupportedTypeException;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.testutils.TestUtils;
import io.openliberty.mcp.internal.typeimpl.ParameterizedTypeImpl;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class ToolMetadataTest {
    Jsonb jsonb;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setup() throws Exception {
        SchemaRegistry.set(new SchemaRegistry());
        jsonb = JsonbBuilder.create();
    }

    @Tool(name = "addGenericToGenericArray", title = "adds generic to generic Array", description = "adds person to Generic Array, returns nothing")
    public @Schema(description = "Returns list of  object") <T> List<T> addGenericToGenericArray(@ToolArg(name = "generic list 1",
                                                                                                          description = "List of generics 1") T[] list1,
                                                                                                 @ToolArg(name = "generic list 2",
                                                                                                          description = "List of generics 1 ") List<List<T>>[] list2,
                                                                                                 @ToolArg(name = "generic", description = "Generic object") T item,
                                                                                                 @ToolArg(name = "concrete", description = "Concrete object") List<String> item2) {
        return null;
    }

    @Test
    public void testGenericParams() {
        Method tm = TestUtils.findActualMethod(ToolMetadataTest.class, "addGenericToGenericArray");
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[0].getType()));
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[1].getType()));
        assertTrue(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[2].getType()));
        assertFalse(TypeUtility.hasGenericParams(tm.getAnnotatedParameterTypes()[3].getType()));

    }

    @Tool(structuredContent = true)
    public CompletionStage<List<String>> asyncToolWithOutputSchema() {
        return null;
    }

    @Test(expected = UnsupportedTypeException.class)
    public void testAsyncToolWithOutputSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "asyncToolWithOutputSchema");

    }

    @Tool(structuredContent = true)
    public String toolReturnsString() {
        return "hello";
    }

    @Test
    public void testToolReturnsStringHasNoSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "toolReturnsString");
        assertNull(metadata.outputSchema());
    }

    @Tool(structuredContent = true)
    public Content toolReturnsContent() {
        return new TextContent("hello");
    }

    @Test
    public void testToolReturnsContentHasNoSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "toolReturnsContent");
        assertNull(metadata.outputSchema());
    }

    @Tool(structuredContent = true)
    public List<Content> toolReturnsListOfContent() {
        return List.of(new TextContent("hi"), new TextContent("bye"));
    }

    @Test
    public void testToolReturnsListOfContentHasNoSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "toolReturnsListOfContent");
        assertNull(metadata.outputSchema());
    }

    public record City(String name, String country) {}

    @Tool(structuredContent = false)
    public City cityToolNoSchema() {
        return new City("Paris", "France");
    }

    @Test
    public void testStructuredObjectWithStructuredContentFalse() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "cityToolNoSchema");
        assertNull(metadata.outputSchema());
    }

    @Tool(structuredContent = true)
    @Schema("""
                    {
                      "type": "object",
                      "properties": {
                        "message": { "type": "string" }
                      }
                    }
                    """)
    public ToolResponse toolResponseWithValidSchema() {
        return ToolResponse.structuredSuccess(Map.of("message", "hi"));
    }

    @Test
    public void testToolResponseWithValidSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "toolResponseWithValidSchema");
        assertNotNull(metadata.outputSchema());
    }

    @Tool(structuredContent = true)
    public ToolResponse toolResponseWithoutSchema() {
        return ToolResponse.success("hello");
    }

    @Test
    public void testToolResponseWithoutSchemaHasNoOutputSchema() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "toolResponseWithoutSchema");
        assertNull(metadata.outputSchema());
    }

    @Test
    public void testUnwrapOutputTypeRemovesCompletionStage() throws Exception {
        Method method = ToolMetadataTest.class.getMethod("asyncToolWithOutputSchema");
        Type returnType = method.getGenericReturnType();

        Type unwrapped = ToolMetadata.unwrapOutputType(returnType);

        assertTrue("Expected List<String>", unwrapped instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) unwrapped;
        assertEquals(List.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    public void testGetRawClassFromParameterizedType() {
        Type listOfString = new ParameterizedTypeImpl(List.class, String.class);
        Class<?> raw = ToolMetadata.getRawClass(listOfString);
        assertEquals(List.class, raw);
    }

    @Test
    public void testCheckConcreteTypeSingleLevelFromRef() {
        String jsonString = """
                            {
                            "$defs": {
                                "Address": {
                                    "properties": {
                                        "number": {
                                            "type": "integer"
                                        },
                                        "street": {
                                            "description": "A street object to represent complex streets",
                                            "properties": {
                                                "streetName": {
                                                    "type": "string"
                                                },
                                                "roadType": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "streetName"
                                            ],
                                            "type": "object"
                                        },
                                        "postcode": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "number",
                                        "street",
                                        "postcode"
                                    ],
                                    "type": "object"
                                },
                                "Person": {
                                    "description": "A person object contains address, company objects",
                                    "properties": {
                                        "address": {
                                            "$ref": "#/$defs/Address"
                                        },
                                        "company": {
                                            "properties": {
                                                "address": {
                                                    "$ref": "#/$defs/Address"
                                                },
                                                "name": {
                                                    "type": "string"
                                                },
                                                "employees": {
                                                    "description": "A list of employees (person object)",
                                                    "items": {
                                                        "$ref": "#/$defs/Person"
                                                    },
                                                    "type": "array"
                                                },
                                                "employeeRegistry": {
                                                    "properties": {
                                                        "value": {
                                                            "$ref": "#/$defs/Person"
                                                        },
                                                        "key": {
                                                            "type": "integer"
                                                        }
                                                    },
                                                    "required": [],
                                                    "type": "object"
                                                }
                                            },
                                            "required": [
                                                "name",
                                                "address",
                                                "employees",
                                                "employeeRegistry"
                                            ],
                                            "type": "object"
                                        },
                                        "fullname": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "fullname",
                                        "address",
                                        "company"
                                    ],
                                    "type": "objectA"
                                }
                            },
                            "$ref": "#/$defs/Person"
                        }
                                        """;
        assertEquals("objectA", ToolMetadata.checkConcreteType(jsonb.fromJson(jsonString, JsonObject.class)));
    }

    @Test
    public void testCheckConcreteTypeSingleLevelFromType() {
        String jsonString = """
                        {
                                "properties": {
                                    "number": {
                                        "type": "integer"
                                    },
                                    "street": {
                                        "description": "A street object to represent complex streets",
                                        "properties": {
                                            "streetName": {
                                                "type": "string"
                                            },
                                            "roadType": {
                                                "type": "string"
                                            }
                                        },
                                        "required": [
                                            "streetName"
                                        ],
                                        "type": "object"
                                    },
                                    "postcode": {
                                        "type": "string"
                                    }
                                },
                                "required": [
                                    "number",
                                    "street",
                                    "postcode"
                                ],
                                "type": "objectB"
                            }
                                    """;
        assertEquals("objectB", ToolMetadata.checkConcreteType(jsonb.fromJson(jsonString, JsonObject.class)));
    }

    @Test
    public void testCheckConcreteTypeArrayFromType() {
        String jsonString = """
                        {
                                    "type": "arrayA",
                                    "items": {
                                        "$ref": "#/$defs/Address"
                                    },
                                    "$defs": {
                                        "Address": {
                                            "properties": {
                                                "number": {
                                                    "type": "integer"
                                                },
                                                "street": {
                                                    "description": "A street object to represent complex streets",
                                                    "properties": {
                                                        "streetName": {
                                                            "type": "string"
                                                        },
                                                        "roadType": {
                                                            "type": "string"
                                                        }
                                                    },
                                                    "required": [
                                                        "streetName"
                                                    ],
                                                    "type": "object"
                                                },
                                                "postcode": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "number",
                                                "street",
                                                "postcode"
                                            ],
                                            "type": "object"
                                        }
                                    }
                                }
                                    """;
        assertEquals("arrayA", ToolMetadata.checkConcreteType(jsonb.fromJson(jsonString, JsonObject.class)));
    }

    @Tool(structuredContent = true)
    @Schema("""
                    {
                    "$ref": "#externalURL"
                    }
                    """)
    public City customSchemaType() {
        return null;
    }

    @Test
    public void testToolResponseWithCustomSchemaType() {
        ToolMetadata metadata = TestUtils.findTool(ToolMetadataTest.class, "customSchemaType");
        assertNotNull(metadata.outputSchema());
    }

}

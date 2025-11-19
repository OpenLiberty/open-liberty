/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.schema;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.internal.schemas.SchemaDirection;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.testutils.MockAnnotatedMethod;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class SchemaTest {
    private static SchemaRegistry registry;

    @Schema(description = "A person object contains address, company objects")
    public static record Person(@JsonbProperty("fullname") String name, Address address, Company company) {};

    public static record Address(int number, @Schema(description = "A street object to represent complex streets") Street street, String postcode,
                                 @JsonbTransient String directions) {};

    @Schema("{\"properties\": {  \"streetName\": { \"type\": \"string\" }, \"roadType\": { \"type\": \"string\" } }, \"required\": [ \"streetName\" ], \"type\": \"object\"}")
    public static record Street(String streetname, String roadtype) {}

    public static record Company(String name, Address address, @Schema(description = "A list of employees (person object)") List<Person> employees,
                                 @Schema(value = "{\"properties\": {\"key\":{ \"type\": \"integer\" }, \"value\":{ \"$ref\": \"#/$defs/Person\" }},\"required\": [ ], \"type\": \"object\"}") Map<String, Person> employeeRegistry) {};

    public static record PartialPerson(String name, Optional<Address> address, PartialCompany partialCompany) {}

    public static record PartialCompany(Optional<String> name, Optional<Address> address,
                                        @Schema(description = "A list of employees (person object)") Optional<List<PartialPerson>> employees,
                                        Optional<Map<String, Optional<PartialPerson>>> employeeRegistry) {}

    public static class SoftwareCompanyEntry {
        SoftwareCompanyEntry.person person;
        String companyName;

        public SoftwareCompanyEntry(SoftwareCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTest.Person person, int softwareid) {}

        /**
         * @return the person
         */
        public SoftwareCompanyEntry.person getPerson() {
            return person;
        }

        /**
         * @param person the person to set
         */
        public void setPerson(SoftwareCompanyEntry.person person) {
            this.person = person;
        }

        /**
         * @return the companyName
         */
        public String getCompanyName() {
            return companyName;
        }

        /**
         * @param companyName the companyName to set
         */
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

    };

    public static class ConstructionCompanyEntry {
        ConstructionCompanyEntry.person person;
        String companyName;

        public ConstructionCompanyEntry(ConstructionCompanyEntry.person person, String companyName) {}

        public static record person(SchemaTest.Person person, String constructionid) {}

        /**
         * @return the person
         */
        public ConstructionCompanyEntry.person getPerson() {
            return person;
        }

        /**
         * @param person the person to set
         */
        public void setPerson(ConstructionCompanyEntry.person person) {
            this.person = person;
        }

        /**
         * @return the companyName
         */
        public String getCompanyName() {
            return companyName;
        }

        /**
         * @param companyName the companyName to set
         */
        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

    };

    public static class PortfolioEntry {
        SoftwareCompanyEntry sce;
        ConstructionCompanyEntry cce;

        public PortfolioEntry(SoftwareCompanyEntry sce, ConstructionCompanyEntry cce) {}

        /**
         * @return the sce
         */
        public SoftwareCompanyEntry getSce() {
            return sce;
        }

        /**
         * @param sce the sce to set
         */
        public void setSce(SoftwareCompanyEntry sce) {
            this.sce = sce;
        }

        /**
         * @return the cce
         */
        public ConstructionCompanyEntry getCce() {
            return cce;
        }

        /**
         * @param cce the cce to set
         */
        public void setCce(ConstructionCompanyEntry cce) {
            this.cce = cce;
        }

    };

    @Tool(name = "checkPerson", title = "checks if person is in employee list", description = "Returns boolean")
    public boolean checkPerson(@ToolArg(name = "person", description = "Person object") Person person, @ToolArg(name = "company", description = "Company object") Company company) {
        return true;
    }

    @Tool(name = "addPersonToList", title = "adds person to employee list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<Person> addPersonToList(@ToolArg(name = "employeeList",
                                                                                                        description = "List of people") List<Person> employeeList,
                                                                                               @ToolArg(name = "person", description = "Person object") Person person) {
        employeeList.add(person);
        return employeeList;
        //comment
    }

    @BeforeClass
    public static void setup() {
        registry = new SchemaRegistry();
    }

    @Test
    public void testPersonSchema() {
        String response = registry.getSchema(Person.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
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
                                    "type": "object"
                                }
                            },
                            "$ref": "#/$defs/Person"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public record Widget(String name, int flangeCount) {};

    @Tool(description = "Updates a widget in the database")
    public Widget updateWidget(@ToolArg(name = "id", description = "The ID of the widget to update") long id,
                               @ToolArg(name = "widget", description = "The new widget data") Widget widget) {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testToolInputSchema() throws NoSuchMethodException, SecurityException {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "updateWidget");

        String toolInputSchema = registry.getToolInputSchema(toolMethod).toString();
        String expectedSchema = """
                        {
                        "type" : "object",
                        "properties" : {
                            "id" : {
                                "type": "integer",
                                "description": "The ID of the widget to update"
                            },
                            "widget" : {
                                "type": "object",
                                "description": "The new widget data",
                                "properties": {
                                    "name": {
                                        "type" : "string"
                                    },
                                    "flangeCount": {
                                        "type" : "integer"
                                    }
                                },
                                "required": [
                                    "name",
                                    "flangeCount"
                                ]
                            }
                        },
                        "required": [
                            "widget",
                            "id"
                        ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    @Test
    public void testToolOutputSchema() throws NoSuchMethodException, SecurityException {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "updateWidget");

        String toolInputSchema = registry.getToolOutputSchema(toolMethod).toString();
        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type" : "string"
                                },
                                "flangeCount": {
                                    "type" : "integer"
                                }
                            },
                            "required": [
                                "name",
                                "flangeCount"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    public record CompositeWidget(String name, int flangeCount, List<CompositeWidget> subwidgets) {}

    @Tool(description = "combine two widgets to make a new widget")
    public CompositeWidget combineWidgets(@ToolArg(name = "widgetA", description = "the first widget") CompositeWidget widgetA,
                                          @ToolArg(name = "widgetB", description = "the second widget") CompositeWidget widgetB) {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testToolInputRecursive() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "combineWidgets");
        String toolInputSchema = registry.getToolInputSchema(toolMethod).toString();

        String expectedSchema = """
                        {
                            "$defs": {
                                "CompositeWidget": {
                                    "type" : "object",
                                    "properties": {
                                        "name": {
                                            "type": "string"
                                        },
                                        "flangeCount": {
                                            "type": "integer"
                                        },
                                        "subwidgets": {
                                            "type": "array",
                                            "items": {
                                                "$ref": "#/$defs/CompositeWidget"
                                            }
                                        }
                                    },
                                    "required" : [
                                        "name",
                                        "flangeCount",
                                        "subwidgets"
                                    ]
                                }
                            },
                            "type": "object",
                            "properties": {
                                "widgetA": {
                                    "$ref": "#/$defs/CompositeWidget",
                                    "description": "the first widget"
                                },
                                "widgetB": {
                                    "$ref": "#/$defs/CompositeWidget",
                                    "description": "the second widget"
                                }
                            },
                            "required": [
                                "widgetB",
                                "widgetA"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);
    }

    @Test
    public void testToolOutputRecursive() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "combineWidgets");
        String toolInputSchema = registry.getToolOutputSchema(toolMethod).toString();
        String expectedSchema = """
                        {
                            "$defs": {
                                "CompositeWidget": {
                                    "type" : "object",
                                    "properties": {
                                        "name": {
                                            "type": "string"
                                        },
                                        "flangeCount": {
                                            "type": "integer"
                                        },
                                        "subwidgets": {
                                            "type": "array",
                                            "items": {
                                                "$ref": "#/$defs/CompositeWidget"
                                            }
                                        }
                                    },
                                    "required" : [
                                        "name",
                                        "flangeCount",
                                        "subwidgets"
                                    ]
                                }
                            },
                            "$ref": "#/$defs/CompositeWidget",
                        }
                        """;
        JSONAssert.assertEquals(expectedSchema, toolInputSchema, true);

    }

    @Test
    public void testPersonCheckToolSchema() throws NoSuchMethodException, SecurityException {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "checkPerson");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
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
                                            "$ref": "#/$defs/Company"
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
                                    "type": "object"
                                },
                                "Company": {
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
                                }
                            },
                            "properties": {
                                "person": {
                                    "$ref": "#/$defs/Person",
                                    "description": "Person object"
                                },
                                "company": {
                                    "$ref": "#/$defs/Company",
                                    "description": "Company object"
                                }
                            },
                            "required": [
                                "person",
                                "company"
                            ],
                            "type": "object"
                        }
                                                    """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddressSchema() {
        String response = registry.getSchema(Address.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
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
                            "type": "object"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testStreetSchema() {
        String response = registry.getSchema(Street.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                        {
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
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCompanySchema() {
        String response = registry.getSchema(Company.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
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
                                            "$ref": "#/$defs/Company"
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
                                    "type": "object"
                                },
                                "Company": {
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
                                }
                            },
                            "$ref": "#/$defs/Company"
                        }
                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testPortfolioEntryDuplicateNameSchema() {
        String response = registry.getSchema(PortfolioEntry.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
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
                                    "type": "object"
                                }
                            },
                            "properties": {
                                "sce": {
                                    "properties": {
                                        "person": {
                                            "properties": {
                                                "softwareid": {
                                                    "type": "integer"
                                                },
                                                "person": {
                                                    "$ref": "#/$defs/Person"
                                                }
                                            },
                                            "required": [
                                                "person",
                                                "softwareid"
                                            ],
                                            "type": "object"
                                        },
                                        "companyName": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "person",
                                        "companyName"
                                    ],
                                    "type": "object"
                                },
                                "cce": {
                                    "properties": {
                                        "person": {
                                            "properties": {
                                                "person": {
                                                    "$ref": "#/$defs/Person"
                                                },
                                                "constructionid": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "person",
                                                "constructionid"
                                            ],
                                            "type": "object"
                                        },
                                        "companyName": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "person",
                                        "companyName"
                                    ],
                                    "type": "object"
                                }
                            },
                            "required": [
                                "sce",
                                "cce"
                            ],
                            "type": "object"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testPersonAddtoListToolInputSchema() throws NoSuchMethodException, SecurityException {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addPersonToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
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
                                                        "type": "object"
                                                    }
                                                },
                                                "properties": {
                                                    "employeeList": {
                                                        "description": "List of people",
                                                        "items": {
                                                            "$ref": "#/$defs/Person"
                                                        },
                                                        "type": "array"
                                                    },
                                                    "person": {
                                                        "$ref": "#/$defs/Person",
                                                        "description": "Person object"
                                                    }
                                                },
                                                "required": [
                                                    "employeeList",
                                                    "person"
                                                ],
                                                "type": "object"
                                            }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testPersonAddtoListToolOutputSchema() throws NoSuchMethodException, SecurityException {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addPersonToList");
        String response = registry.getToolOutputSchema(toolMethod).toString();
        String expectedResponseString = """
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
                                    "type": "object"
                                }
                            },
                            "description": "Returns list of person object",
                            "items": {
                                "$ref": "#/$defs/Person"
                            },
                            "type": "array"
                        }
                                                    """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public enum TestEnum {
        VALUE1,
        @JsonbProperty("altValue2")
        VALUE2
    }

    public record EnumHolder(TestEnum testEnum) {}

    @Test
    public void testEnumObject() {
        String schema = registry.getSchema(EnumHolder.class, SchemaDirection.INPUT).toString();

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "testEnum": {
                                    "type": "string",
                                    "enum": [
                                        "VALUE1",
                                        "altValue2"
                                    ]
                                }
                            },
                            "required": ["testEnum"]
                        }""";

        JSONAssert.assertEquals(expectedSchema, schema, true);
    }

    public record EnumMapHolder(Map<TestEnum, String> map) {};

    @Test
    public void testEnumKeyedMap() {
        String schema = registry.getSchema(EnumMapHolder.class, SchemaDirection.INPUT).toString();

        String expectedSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "map": {
                                    "type": "object",
                                    "additionalProperties": {
                                        "type": "string"
                                    },
                                    "propertyNames": {
                                        "type": "string",
                                        "enum": [
                                            "VALUE1",
                                            "altValue2"
                                        ]
                                    }
                                }
                            },
                            "required": ["map"]
                        }""";
        JSONAssert.assertEquals(expectedSchema, schema, true);
    }

    @Test
    public void testOptionalPartialPersonSchema() {
        String response = registry.getSchema(PartialPerson.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
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
                                "PartialPerson": {
                                    "type": "object",
                                    "properties": {
                                        "name": {"type": "string"},
                                        "address": {"$ref": "#/$defs/Address"},
                                        "partialCompany": {
                                            "type": "object",
                                            "properties": {
                                                "name": {"type": "string"},
                                                "address": {"$ref": "#/$defs/Address"},
                                                "employees": {
                                                    "type": "array",
                                                    "description": "A list of employees (person object)",
                                                    "items": {"$ref": "#/$defs/PartialPerson"}
                                                },
                                                "employeeRegistry": {
                                                    "type": "object",
                                                    "additionalProperties": {"$ref": "#/$defs/PartialPerson"}
                                                }
                                            },
                                            "required": []
                                        }
                                    },
                                    "required": [
                                        "name",
                                        "partialCompany"
                                    ]
                                }
                            },
                            "$ref": "#/$defs/PartialPerson"
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public static class BoxMap<K, V, T> {
        K key;
        V value;
        T type;

        /**
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * @param key the key to set
         */
        public void setKey(K key) {
            this.key = key;
        }

        /**
         * @return the value
         */
        public V getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(V value) {
            this.value = value;
        }

        /**
         * @return the type
         */
        public T getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(T type) {
            this.type = type;
        }

    }

    public static class Container {
        BoxMap<String, String, Integer> bm;

        /**
         * @return the bm
         */
        public BoxMap<String, String, Integer> getBm() {
            return bm;
        }

        /**
         * @param bm the bm to set
         */
        public void setBm(BoxMap<String, String, Integer> bm) {
            this.bm = bm;
        }

    }

    @Test
    public void testConcreteParamterizedGenericClass() {
        String response = registry.getSchema(Container.class, SchemaDirection.INPUT).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "bm": {
                                    "type": "object",
                                    "properties": {
                                        "type": {
                                            "type": "integer"
                                        },
                                        "value": {
                                            "type": "string"
                                        },
                                        "key": {
                                            "type": "string"
                                        }
                                    },
                                    "required": [
                                        "type",
                                        "value",
                                        "key"
                                    ]
                                }
                            },
                            "required": [
                                "bm"
                            ]
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T> List<T> addGenericToList(@ToolArg(name = "generic list",
                                                                                                        description = "List of generics") List<T> list,
                                                                                               @ToolArg(name = "generic", description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addGenericToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "type": "object"
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericSingleBoundToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T extends Number> List<T> addGenericSingleBoundToList(@ToolArg(name = "generic list",
                                                                                                                                  description = "List of generics") List<T> list,
                                                                                                                         @ToolArg(name = "generic",
                                                                                                                                  description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericSingleBoundToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addGenericSingleBoundToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "type": "number"
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public static interface NumberRestrictor {
        public Number getMax();

        public Number getMin();

        public void setMax(Number number);

        public void setMin(Number number);
    }

    @Tool(name = "addGenericMultipleBoundsToList", title = "adds generic to generic list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") <T extends Number & NumberRestrictor> List<T> addGenericMultipleBoundsToList(@ToolArg(name = "generic list",
                                                                                                                                                        description = "List of generics") List<T> list,
                                                                                                                                               @ToolArg(name = "generic",
                                                                                                                                                        description = "Generic object") T item) {
        list.add(item);
        return list;
        //comment
    }

    @Test
    public void testGenericMultipleBoundsToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addGenericMultipleBoundsToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                            {
                            "type": "object",
                            "properties": {
                                "generic list": {
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/$defs/T"
                                    },
                                    "description": "List of generics"
                                },
                                "generic": {
                                    "$ref": "#/$defs/T",
                                    "description": "Generic object"
                                }
                            },
                            "required": [
                                "generic list",
                                "generic"
                            ],
                            "$defs": {
                                "T": {
                                    "allOf": [
                                        {
                                            "type": "number"
                                        },
                                        {
                                            "type": "object",
                                            "properties": {
                                                "min": {
                                                    "type": "number"
                                                },
                                                "max": {
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "min",
                                                "max"
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                            """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<?> addWildcardToList(@ToolArg(name = "wildcard list",
                                                                                                     description = "List of wildcard") List<?> list,
                                                                                            @ToolArg(name = "number", description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testWildcardToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addWildcardToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                        {
                            "type": "object",
                            "properties": {
                                "number": {
                                    "type": "number",
                                    "description": "number"
                                },
                                "wildcard list": {
                                    "type": "array",
                                    "items": {
                                        "type": "object"
                                    },
                                    "description": "List of wildcard"
                                }
                            },
                            "required": [
                                "number",
                                "wildcard list"
                            ]
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardExtendBoundToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<? extends Number> addWildcardExtendBoundToList(@ToolArg(name = "wildcard list",
                                                                                                                               description = "List of wildcard") List<? extends NumberRestrictor> list,
                                                                                                                      @ToolArg(name = "number",
                                                                                                                               description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testGenericExtendBoundToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addWildcardExtendBoundToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "number": {
                                                "type": "number",
                                                "description": "number"
                                            },
                                            "wildcard list": {
                                                "type": "array",
                                                "items": {
                                                    "type": "object",
                                                    "properties": {
                                                        "min": {
                                                            "type": "number"
                                                        },
                                                        "max": {
                                                            "type": "number"
                                                        }
                                                    },
                                                    "required": [
                                                        "min",
                                                        "max"
                                                    ]
                                                },
                                                "description": "List of wildcard"
                                            }
                                        },
                                        "required": [
                                            "number",
                                            "wildcard list"
                                        ]
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addWildcardSuperBoundsToList", title = "adds wildcard to wildcard list", description = "adds person to employee list, returns nothing")
    public @Schema(description = "Returns list of person object") List<? super Integer> addWildcardSuperBoundsToList(@ToolArg(name = "wildcard list",
                                                                                                                              description = "List of wildcard") List<? super NumberRestrictor> list,
                                                                                                                     @ToolArg(name = "number",
                                                                                                                              description = "number") Number number) {
        return null;
        //comment
    }

    @Test
    public void testWildcardSuperBoundsToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addWildcardSuperBoundsToList");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "number": {
                                                "type": "number",
                                                "description": "number"
                                            },
                                            "wildcard list": {
                                                "type": "array",
                                                "items": {
                                                    "type": "object"
                                                },
                                                "description": "List of wildcard"
                                            }
                                        },
                                        "required": [
                                            "number",
                                            "wildcard list"
                                        ]
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "addGenericToGenericArray", title = "adds generic to generic Array", description = "adds person to Generic Array, returns nothing")
    public @Schema(description = "Returns list of person object") <T> List<T> addGenericToGenericArray(@ToolArg(name = "generic list 1",
                                                                                                                description = "List of generics 1") T[] list1,
                                                                                                       @ToolArg(name = "generic list 2",
                                                                                                                description = "List of generics 1 ") List<T>[] list2,
                                                                                                       @ToolArg(name = "generic", description = "Generic object") T item) {
        return null;
        //comment
    }

    @Test
    public void testGenericArrayToolArg() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "addGenericToGenericArray");
        String response = registry.getToolInputSchema(toolMethod).toString();
        String expectedResponseString = """
                                    {
                                        "type": "object",
                                        "properties": {
                                            "generic list 2": {
                                                "type": "array",
                                                "items": {
                                                    "type": "array",
                                                    "items": {
                                                        "$ref": "#/$defs/T"
                                                    }
                                                },
                                                "description": "List of generics 1 "
                                            },
                                            "generic": {
                                                "$ref": "#/$defs/T",
                                                "description": "Generic object"
                                            },
                                            "generic list 1": {
                                                "type": "array",
                                                "items": {
                                                    "$ref": "#/$defs/T"
                                                },
                                                "description": "List of generics 1"
                                            }
                                        },
                                        "required": [
                                            "generic list 2",
                                            "generic",
                                            "generic list 1"
                                        ],
                                        "$defs": {
                                            "T": {
                                                "type": "object"
                                            }
                                        }
                                    }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Tool(name = "primitiveArrayTest", title = "Test Content Response", description = "tests Content Response")
    public int[] primitiveArrayTest(@ToolArg(name = "name", description = "name") String name) {
        return null;
        //comment
    }

    @Test
    public void testPrimitiveArray() {
        MockAnnotatedMethod<Object> toolMethod = TestUtils.findMethod(SchemaTest.class, "primitiveArrayTest");
        String response = registry.getToolOutputSchema(toolMethod).toString();
        String expectedResponseString = """
                                    {"type":"array","items":{"type":"integer"}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

}

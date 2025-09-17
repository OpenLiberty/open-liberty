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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.math.BigDecimal;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpRequestId;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Unit tests for the overidden .equals method for {@link McpRequestId}
 */
public class McpRequestIdTest {
    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        jsonb = JsonbBuilder.create();
    }

    @Test
    public void testRequestIdStringsAreEqual() {
        McpRequestId reqId1 = new McpRequestId("Dog");
        McpRequestId reqId2 = new McpRequestId("Dog");
        assertTrue(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdStringsAreNotEqual() {
        McpRequestId reqId1 = new McpRequestId("Dog");
        McpRequestId reqId2 = new McpRequestId("Cat");
        assertFalse(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdNumbersAreEqual() {
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(5.0f);
        McpRequestId req1 = new McpRequestId(num1);
        McpRequestId req2 = new McpRequestId(num2);
        assertTrue(req1.equals(req2));
    }

    @Test
    public void testRequestIdNumbersAreNotEqual() {
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(7);
        McpRequestId req1 = new McpRequestId(num1);
        McpRequestId req2 = new McpRequestId(num2);
        assertFalse(req1.equals(req2));
    }

    @Test
    public void testRequestIdStringDoesNotEqualNumber() {
        McpRequestId reqIdInt = new McpRequestId(new BigDecimal(1));
        McpRequestId reqIdString = new McpRequestId("1");
        assertFalse(reqIdString.equals(reqIdInt));
    }

    @Test
    public void testRequestIdNumberSerialization() {
        McpRequestId id = new McpRequestId(new BigDecimal(2));
        JsonObject params = Json.createObjectBuilder().build(); //empty params object
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);
        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                        {"getRequestMethod":"TOOLS_CALL","id":2,"jsonrpc":"2.0","method":"tools/call","params":{}}
                        """;
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testRequestIdStringSerialization() {
        McpRequestId id = new McpRequestId("2");
        JsonObject params = Json.createObjectBuilder().build(); //empty params object
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);
        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                        {"getRequestMethod":"TOOLS_CALL","id":"2","jsonrpc":"2.0","method":"tools/call","params":{}}
                        """;
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testNullRequestIdSerialization() {
        McpRequestId id = new McpRequestId("");
        JsonObject params = Json.createObjectBuilder().build(); //empty params object
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);
        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                        {"getRequestMethod":"TOOLS_CALL","id":null,"jsonrpc":"2.0","method":"tools/call","params":{}}
                        """;
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testRequestIdStringDeserialization() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {}
                          }
                        }
                        """);
        McpRequest actualRequest = jsonb.fromJson(reader, McpRequest.class);
        assertThat(actualRequest.id().getStrVal(), equalTo("2"));
    }

    @Test
    public void testRequestIdNumberDeserialization() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {}
                          }
                        }
                        """);
        McpRequest actualRequest = jsonb.fromJson(reader, McpRequest.class);
        assertThat(actualRequest.id().getNumVal(), equalTo(new BigDecimal(2)));
    }

    @Test
    public void testRequestIdStringToString() {
        McpRequestId reqId = new McpRequestId("Dog");
        assertThat(reqId.toString(), equalTo("Dog"));
    }

    @Test
    public void testRequestIdNumberToString() {
        BigDecimal num = new BigDecimal(5);
        McpRequestId reqId = new McpRequestId(num);
        assertThat(reqId.toString(), equalTo("5"));

    }

}

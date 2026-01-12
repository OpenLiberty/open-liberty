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
import io.openliberty.mcp.internal.requests.McpRequestIdDeserializer;
import io.openliberty.mcp.internal.requests.McpRequestIdSerializer;
import io.openliberty.mcp.request.RequestId;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Unit tests for the overidden .equals method for {@link RequestId}
 */
public class McpRequestIdTest {
    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        JsonbConfig jsonbConfig = new JsonbConfig().withSerializers(new McpRequestIdSerializer())
                                                   .withDeserializers(new McpRequestIdDeserializer());

        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    @Test
    public void testRequestIdStringsAreEqual() {
        RequestId reqId1 = new RequestId("Dog");
        RequestId reqId2 = new RequestId("Dog");
        assertTrue(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdStringsAreNotEqual() {
        RequestId reqId1 = new RequestId("Dog");
        RequestId reqId2 = new RequestId("Cat");
        assertFalse(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdNumbersAreEqual() {
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(5.0f);
        RequestId req1 = new RequestId(num1);
        RequestId req2 = new RequestId(num2);
        assertTrue(req1.equals(req2));
    }

    @Test
    public void testRequestIdNumbersAreNotEqual() {
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(7);
        RequestId req1 = new RequestId(num1);
        RequestId req2 = new RequestId(num2);
        assertFalse(req1.equals(req2));
    }

    @Test
    public void testRequestIdStringDoesNotEqualNumber() {
        RequestId reqIdInt = new RequestId(new BigDecimal(1));
        RequestId reqIdString = new RequestId("1");
        assertFalse(reqIdString.equals(reqIdInt));
    }

    @Test
    public void testRequestIdNumberSerialization() {
        RequestId id = new RequestId(new BigDecimal(2));
        JsonObject params = Json.createObjectBuilder().build();
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);

        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                          {"id":2,"jsonrpc":"2.0","method":"tools/call","params":{}}
                        """;

        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testRequestIdStringSerialization() {
        RequestId id = new RequestId("2");
        JsonObject params = Json.createObjectBuilder().build();
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);

        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                          {"id":"2","jsonrpc":"2.0","method":"tools/call","params":{}}
                        """;

        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testNullRequestIdSerialization() {
        RequestId id = new RequestId("");
        JsonObject params = Json.createObjectBuilder().build();
        McpRequest req = new McpRequest("2.0", id, "tools/call", params);

        String actualJson = jsonb.toJson(req);
        String expectedJson = """
                            {"id":null ,"jsonrpc":"2.0","method":"tools/call","params":{}}
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
                        """);
        McpRequest actualRequest = McpRequest.createValidMCPRequest(reader);
        assertThat(actualRequest.id().value(), equalTo("2"));
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
                        """);
        McpRequest actualRequest = McpRequest.createValidMCPRequest(reader);
        assertThat(actualRequest.id().value(), equalTo(new BigDecimal(2)));
    }

    @Test
    public void testRequestIdStringToString() {
        RequestId reqId = new RequestId("Dog");
        assertThat(reqId.toString(), equalTo("Dog"));
    }

    @Test
    public void testRequestIdNumberToString() {
        BigDecimal num = new BigDecimal(5);
        RequestId reqId = new RequestId(num);
        assertThat(reqId.toString(), equalTo("5"));

    }

}

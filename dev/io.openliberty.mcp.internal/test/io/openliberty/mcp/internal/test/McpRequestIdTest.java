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
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.requests.parsing.JsonRpcRequestTO;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.responses.McpErrorResponse;
import io.openliberty.mcp.internal.responses.McpResultResponse;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;

/**
 * Unit tests for the overidden .equals method for {@link RequestId}
 */
public class McpRequestIdTest {
    private static Jsonb jsonb;
    private static Method parseAndValidateId;

    @BeforeClass
    public static void setup() throws NoSuchMethodException, SecurityException {
        jsonb = TestUtils.createJsonb();
        parseAndValidateId = McpOperationRequestParser.class.getDeclaredMethod("parseAndValidateId", JsonValue.class, List.class);
        parseAndValidateId.setAccessible(true);
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
        McpResultResponse response = new McpResultResponse(id, "data");

        String actualJson = jsonb.toJson(response);
        String expectedJson = """
                          {"id":2,"jsonrpc":"2.0", "result": "data"}
                        """;

        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testRequestIdStringSerialization() {
        RequestId id = new RequestId("2");
        McpResultResponse response = new McpResultResponse(id, "data");

        String actualJson = jsonb.toJson(response);
        String expectedJson = """
                          {"id":"2","jsonrpc":"2.0", "result": "data"}
                        """;

        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    public void testNullRequestIdSerialization() {
        RequestId id = RequestId.UNPARSABLE;
        McpErrorResponse response = new McpErrorResponse(id, new McpErrorResponse.Error(1, "test", "test"));

        String actualJson = jsonb.toJson(response);
        String expectedJson = """
                          {"id":null, "jsonrpc":"2.0", "error": {}}
                        """;

        JSONAssert.assertEquals(expectedJson, actualJson, LENIENT);
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
        JsonRpcRequestTO actualRequest = jsonb.fromJson(reader, JsonRpcRequestTO.class);
        assertThat(parseRequestId(actualRequest.id()).value(), equalTo("2"));
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
        JsonRpcRequestTO actualRequest = jsonb.fromJson(reader, JsonRpcRequestTO.class);
        assertThat(parseRequestId(actualRequest.id()).value(), equalTo(new BigDecimal(2)));
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

    private RequestId parseRequestId(JsonValue value) {
        List<String> errors = new ArrayList<>();
        RequestId result;
        try {
            result = (RequestId) parseAndValidateId.invoke(null, value, errors);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("Parse errors: " + String.join(", ", errors));
        }
        return result;
    }

}

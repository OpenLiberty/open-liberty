/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Helper class to handle authorization errors and expected responses
 */
public class AuthHelper {

    // User Names
    private static final String ADMIN = "BobTheAdmin";

    private static final String TESTER = "testuser";
    private static final String NO_ROLE_USER = "noRoleUser";
    private static final String NON_EXISTING_USER = "JOHN_DOE";
    // Passwords
    private static final String VALID_PASSWORD = "testpassword";
    private static final String WRONG_PASSWORD = "drowssaptset";

    static enum Scenario {
        NO_AUTHENTICATION,
        ADMIN_PASS_LOGIN, ADMIN_FAIL_LOGIN,
        TESTUSER_PASS_LOGIN, TESTUSER_FAIL_LOGIN,
        UNKNOWN_USER, UNKNOWN_ROLE
    };

    static enum ExpectedTestResult {
        PASS, FAIL
    };

    // used to control how far down the call stack we want to go
    // to retrieve the name of the original calling test method
    private static final int STACK_NESTING_LEVEL = 4;

    /**
     *
     * Authentication list
     *
     * Accepts and tests an authorization test scenario along with the expected result.
     *
     * The scenarios tested for each tool method are in the Scenario enum:
     *
     * 1) No credentials supplied (no authorization)
     * 2) Admin authenticated
     * 3) Admin not authenticated
     * 4) Tester authenticated
     * 5) Tester not authenticated
     * 6) Wrong user password
     * 7) Logging in with a valid user that has no role
     *
     * For each scenario a different test result is expected.
     * The expected test result is TestResult.PASSed in as a parameter so we know how to handle an
     *
     *
     * @param scenario
     * @param expectedTestResult
     * @param client
     * @throws Exception
     */
    static void test(Scenario scenario, ExpectedTestResult expectedTestResult, McpClient client) throws Exception {

        if (scenario.equals(Scenario.NO_AUTHENTICATION)) {
            switch (expectedTestResult) {
                case PASS:
                    positiveResponseExpectedForMCPTool(client);
                    break;
                case FAIL:
                    negativeResponseExpectedForMCPTool(client);
                    break;
                default:
                    throw new Exception("Unsupported ExpectedTestResult value");
            }
            return;
        }

        String user;
        String password;

        switch (scenario) {
            case ADMIN_PASS_LOGIN:
                user = ADMIN;
                password = VALID_PASSWORD;
                break;
            case ADMIN_FAIL_LOGIN:
                user = ADMIN;
                password = WRONG_PASSWORD;
                break;
            case TESTUSER_PASS_LOGIN:
                user = TESTER;
                password = VALID_PASSWORD;
                break;
            case TESTUSER_FAIL_LOGIN:
                user = TESTER;
                password = WRONG_PASSWORD;
                break;
            case UNKNOWN_USER:
                user = NON_EXISTING_USER;
                password = WRONG_PASSWORD;
                break;
            case UNKNOWN_ROLE:
                user = NO_ROLE_USER;
                password = VALID_PASSWORD;
                break;

            default:
                throw new Exception("Unsupported Authorization test Scenario");
        }

        switch (expectedTestResult) {
            case PASS:
                positiveResponseExpectedForMCPTool(client, user, password);
                break;
            case FAIL:
                negativeResponseExpectedForMCPTool(client, user, password);
                break;
            default:
                throw new Exception("Unsupported ExpectedTestResult value");
        }
    }

    /*
     * Given a JSON request and authentication, then this MCP call is expected to fail authorization or authentication
     */
    private static void negativeResponseExpectedForMCPTool(McpClient client, String user, String password) throws Exception {
        String request = AuthHelper.buildMCPCallRequest();
        String response = client.callMCPwithBasicAuth_AuthorisationErrorExpected(request, user, password);
        assertNull(response);
    }

    /*
     * Given a JSON request and no authentication, then this MCP call is expected to fail authorization
     */
    private static void negativeResponseExpectedForMCPTool(McpClient client) throws Exception {
        String request = AuthHelper.buildMCPCallRequest();
        String response = client.callMCPAuthorisationErrorExpected(request);
        assertNull(response);
    }

    /**
     * This asserts the expected output from a call to the echo tool without authorization
     *
     * @param client TODO
     * @throws Exception
     */
    private static void positiveResponseExpectedForMCPTool(McpClient client) throws Exception {
        String request = AuthHelper.buildMCPCallRequest();
        String response = client.callMCP(request);
        compareJSONWithExpectedEchoResponse(response);
    }

    /**
     * This asserts the expected output from a call to the echo tool in order to check that authorization has worked
     *
     * @param client TODO
     * @throws Exception
     */
    private static void positiveResponseExpectedForMCPTool(McpClient client, String user, String password) throws Exception {
        String request = AuthHelper.buildMCPCallRequest();
        String response = client.callMCPwithBasicAuth(request, user, password);
        compareJSONWithExpectedEchoResponse(response);
    }

    /**
     * @param response
     * @throws JSONException
     */
    private static void compareJSONWithExpectedEchoResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);
        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    /**
     * Converts a FAT Test test method name to the Tool method equivalent
     * for e.g.
     * RolesToolsTestDenyAll.testEchoDenyAll() would be converted to echoDenyAll,
     * so that it can be called by MCPClient.callMCP(..)
     *
     * @param methodName the name of the method to be transformed
     * @return a transformed method name that will match the name of the calling test
     */
    private static String transfromCurrentMethodName(String methodName) {
        methodName = methodName.substring(4); // Remove first 4 characters ("test")
        return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1); // make charAt(0) lowercase
    }

    /**
     * Builds a JSON RPC request meant for an echo style method
     * This is to check authorization and authentication only
     *
     * @return the MCP request for the test that called it
     */
    private static String buildMCPCallRequest() {
        // finds:
        // - the name of the method that called this method and then
        // - the calling method of that method, up until the nesting level

        String methodName = Thread.currentThread().getStackTrace()[STACK_NESTING_LEVEL].getMethodName();
        return String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "%s",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """, transfromCurrentMethodName(methodName));
    }
}

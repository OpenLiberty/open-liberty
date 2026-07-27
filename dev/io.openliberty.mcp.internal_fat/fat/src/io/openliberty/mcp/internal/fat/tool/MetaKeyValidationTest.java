/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.metaKeyApp.MetaKeyTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Verify _meta key validation is enforced
 * Application code calls {@code putMetadata} / {@code setMetadata}
 * on a {@code ToolResponse.Builder}; the tests confirm that
 * valid keys are accepted and invalid / reserved keys are rejected with an
 * {@link IllegalArgumentException} before the response is built.
 */
@RunWith(FATRunner.class)
public class MetaKeyValidationTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/metaKeyTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "metaKeyTest.war")
                                   .addPackage(MetaKeyTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    // putMetadata — valid keys accepted
    @Test
    public void testPutValidReverseDnsKey() throws Exception {
        String response = callPutValidMetaKey("com.example/myKey", "hello");
        assertToolResponseText(response, "OK");
    }

    @Test
    public void testPutValidNameOnlyKey() throws Exception {
        String response = callPutValidMetaKey("plainName", "world");
        assertToolResponseText(response, "OK");
    }

    @Test
    public void testPutValidEmptyName() throws Exception {
        // Empty name segment is allowed by spec
        String response = callPutValidMetaKey("com.example/", "value");
        assertToolResponseText(response, "OK");
    }

    @Test
    public void testPutValidKeyWithHyphenAndUnderscore() throws Exception {
        String response = callPutValidMetaKey("com.example/my-key_v1.0", "value");
        assertToolResponseText(response, "OK");
    }

    // putMetadata — reserved / invalid keys rejected
    @Test
    public void testPutReservedMcpPrefixIsRejected() throws Exception {
        // Second label is "mcp" — reserved
        String response = callPutInvalidMetaKey("io.mcp/key");
        assertRejected(response);
    }

    @Test
    public void testPutReservedModelContextProtocolPrefixIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("io.modelcontextprotocol/key");
        assertRejected(response);
    }

    @Test
    public void testPutReservedPrefixCaseInsensitiveIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("dev.MCP/key");
        assertRejected(response);
    }

    @Test
    public void testPutNameStartingWithHyphenIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("-badName");
        assertRejected(response);
    }

    @Test
    public void testPutNameEndingWithDotIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("badName.");
        assertRejected(response);
    }

    @Test
    public void testPutLabelStartingWithDigitIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("1com.example/key");
        assertRejected(response);
    }

    @Test
    public void testPutEmptyPrefixIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("/key");
        assertRejected(response);
    }

    @Test
    public void testPutEmptyLabelFromConsecutiveDotsIsRejected() throws Exception {
        String response = callPutInvalidMetaKey("com..example/key");
        assertRejected(response);
    }

    // setMetadata — valid map accepted
    @Test
    public void testSetValidMetadataMap() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "setValidMetadata",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertToolResponseText(response, "OK");
    }

    // setMetadata — reserved key in map rejected

    @Test
    public void testSetReservedKeyInMapIsRejected() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "setReservedMetadata",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertRejected(response);
    }

    // Non-reserved prefix that contains "mcp" in a non-second label position
    @Test
    public void testNonReservedPrefixWithMcpAsThirdLabelIsAccepted() throws Exception {
        // com.example.mcp/ — second label is "example", NOT reserved (spec example)
        String response = callPutValidMetaKey("com.example.mcp/key", "value");
        assertToolResponseText(response, "OK");
    }

    private String callPutValidMetaKey(String key, String value) throws Exception {
        String request = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "putValidMetaKey",
                            "arguments": {
                              "key": "%s",
                              "value": "%s"
                            }
                          }
                        }
                        """, key, value);
        return client.callMCP(request);
    }

    private String callPutInvalidMetaKey(String key) throws Exception {
        String request = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "putInvalidMetaKey",
                            "arguments": {
                              "key": "%s"
                            }
                          }
                        }
                        """, key);
        return client.callMCP(request);
    }

    /** Asserts the first content block text equals {@code expected}. */
    private static void assertToolResponseText(String response, String expected) throws Exception {
        JSONObject json = new JSONObject(response);
        String text = json.getJSONObject("result")
                          .getJSONArray("content")
                          .getJSONObject(0)
                          .getString("text");
        JSONAssert.assertEquals(
                                String.format("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"%s\"}]}}", expected),
                                response, JSONCompareMode.LENIENT);
    }

    /** Asserts the response text starts with "REJECTED:", confirming the exception was thrown. */
    private static void assertRejected(String response) throws Exception {
        JSONObject json = new JSONObject(response);
        String text = json.getJSONObject("result")
                          .getJSONArray("content")
                          .getJSONObject(0)
                          .getString("text");
        assertTrue("Expected text to start with 'REJECTED:' but was: " + text,
                   text.startsWith("REJECTED:"));
    }
}

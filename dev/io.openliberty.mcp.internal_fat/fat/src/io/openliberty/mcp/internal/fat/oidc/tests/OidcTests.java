/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.oidc.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.oidc.tools.RolesAllowedTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.McpDetailedAuthResponse;
import io.openliberty.mcp.internal.fat.utils.McpClient.StateMode;

@RunWith(FATRunner.class)
public class OidcTests extends FATServletClient {

    // Credentials used by the Keycloak test users
    private static final String TEST_ADMIN_USERNAME = KeycloakContainer.getTestAdminUsername();
    private static final String TEST_USER_USERNAME = KeycloakContainer.getTestUserUsername();
    private static final String TEST_PASSWORD = KeycloakContainer.getTestPassword();

    // Note: We don't use @Rule for McpClient because it runs McpClient.before() which sends the MCP initialize request without an authorization header.
    // This would cause a 401 as the server in this test uses OIDC which requires a token each request
    @Server("mcp-server-oidc")
    public static LibertyServer server;

    // Provides an isolated Keycloak instance for this test class
    @ClassRule
    public static KeycloakContainer keycloakContainer = new KeycloakContainer();

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oidcTests.war")
                                   .addPackage(RolesAllowedTools.class.getPackage())
                                   .addAsWebInfResource(new File("publish/servers/mcp-server-oidc/resources/WEB-INF/web.xml"), "web.xml");
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        keycloakContainer.setupRealm();
        server.startServer();
        keycloakContainer.updateServerConfig(server);

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
        // Wait for LTPA configuration to be ready
        server.waitForLTPAConfigReady();
        server.waitForDefaultHTTPEndpointSSLStart();
    }

    @AfterClass
    public static void teardown() throws Exception {
        // CWWKZ0014W: oidcAuthFlowTests.war is declared in server.xml but is only deployed by
        // OidcAuthorizationFlowTests — it will not be present during OidcTests' server lifecycle.
        server.stopServer("CWWKZ0014W");
    }

    @Test
    public void testProctectedToolCallWithoutAccessTokenFailsWith401Unauthorized() throws Exception {
        testUnauthroizedToolCallAssertion("adminTool");
        testUnauthroizedToolCallAssertion("userTool");
    }

    private void testUnauthroizedToolCallAssertion(String toolName) throws Exception {
        McpClient client = new McpClient(server, "/oidcTests", StateMode.STATELESS);
        String request = String.format("""
                          {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "%s",
                            "arguments": {}
                          }
                        }
                        """, toolName);

        McpDetailedAuthResponse mcpResponse = client.callMCP401AuthErrorExpected(request);
        assertEquals(401, mcpResponse.statusCode());

        String wwwAuthenticate = mcpResponse.wwwAuthenticate();
        assertNotNull(wwwAuthenticate);
        assertTrue(wwwAuthenticate.contains("Bearer realm=\"oauth\""));
        assertTrue(wwwAuthenticate.contains("error=\"invalid_token\""));
    }

    @Test
    public void testUserRestrictedToolCallWithUserAccessTokenSucceeds() throws Exception {
        String accessToken = getAccessToken(TEST_USER_USERNAME, TEST_PASSWORD);
        assertNotNull("Access token should not be null", accessToken);

        McpClient client = new McpClient(server, "/oidcTests", StateMode.STATELESS, accessToken);

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "userTool",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCPWithBearerToken(request);
        String expectedResponseString = """
                        {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}], "isError": false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testUserRestrictedToolCallWithAdminAccessTokenSucceeds() throws Exception {
        String accessToken = getAccessToken(TEST_ADMIN_USERNAME, TEST_PASSWORD);
        assertNotNull("Access token should not be null", accessToken);

        McpClient client = new McpClient(server, "/oidcTests", StateMode.STATELESS, accessToken);

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "userTool",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCPWithBearerToken(request);
        String expectedResponseString = """
                        {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}], "isError": false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithAdminAccessTokenSucceeds() throws Exception {
        String accessToken = getAccessToken(TEST_ADMIN_USERNAME, TEST_PASSWORD);
        assertNotNull("Access token should not be null", accessToken);

        McpClient client = new McpClient(server, "/oidcTests", StateMode.STATELESS, accessToken);

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "adminTool",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCPWithBearerToken(request);
        String expectedResponseString = """
                        {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}], "isError": false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAdminRestrictedToolCallWithBasicUserTokenFails() throws Exception {
        String accessToken = getAccessToken(TEST_USER_USERNAME, TEST_PASSWORD);
        assertNotNull("Access token should not be null", accessToken);

        McpClient client = new McpClient(server, "/oidcTests", StateMode.STATELESS, accessToken);

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "adminTool",
                            "arguments": {}
                          }
                        }
                        """;

        client.callMCPWithBearerTokenAuthorisationErrorExpected(request);
    }

    /**
     * Obtains an access token from Keycloak for the given user via the ROPC grant.
     */
    private String getAccessToken(String username, String password) throws Exception {
        return keycloakContainer.obtainAccessToken(username, password);
    }
}

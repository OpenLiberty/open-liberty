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
import org.json.JSONObject;
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

/**
 * Tests the end-to-end MCP OAuth 2.0 authorization discovery flow as described
 * by the MCP Authorization specification (RFC 9728).
 *
 * <p>Each scenario is exercised twice — once over plain HTTP and once over HTTPS.
 * Both paths share the same scenario helper methods; the only difference is the
 * {@link McpClient} instance passed in:
 * <ul>
 * <li>{@link #httpClient} — plain HTTP client with a Keycloak-trusting discovery client attached</li>
 * <li>{@link #httpsClient} — HTTPS client built with a combined Liberty + Keycloak
 * {@link javax.net.ssl.SSLContext}</li>
 * </ul>
 *
 * <p>The full flow under test is:
 * <ol>
 * <li>Attempt to connect and call a protected tool without an access token</li>
 * <li>Receive 401 with a {@code WWW-Authenticate} header containing a {@code resource_metadata} URL</li>
 * <li>Fetch the Protected Resource Metadata from the discovered URL</li>
 * <li>Read the {@code authorization_servers} field to find the Authorization Server</li>
 * <li>Fetch the Authorization Server Metadata ({@code /.well-known/openid-configuration})</li>
 * <li>Complete the OAuth 2.0 ROPC login against the discovered {@code token_endpoint}</li>
 * <li>Call the protected tool with the obtained access token and verify the response</li>
 * </ol>
 *
 * <p>This class deploys its own war ({@code oidcAuthFlowTests.war}) against the
 * {@code mcp-server-oidc} Liberty server. The {@code @ClassRule} Keycloak container is
 * started before the first test in this class and stopped after the last.
 */
@RunWith(FATRunner.class)
public class AuthorizationFlowTests extends FATServletClient {

    private static final String CONTEXT_ROOT = "/oidcAuthFlowTests";

    private static final String AS_METADATA_SUFFIX = "/.well-known/openid-configuration";

    // Credentials used by the Keycloak test users
    private static final String TEST_ADMIN_USERNAME = KeycloakContainer.getTestAdminUsername();
    private static final String TEST_USER_USERNAME = KeycloakContainer.getTestUserUsername();
    private static final String TEST_PASSWORD = KeycloakContainer.getTestPassword();

    // Provides an isolated Keycloak instance for this test class
    @ClassRule
    public static KeycloakContainer keycloakContainer = new KeycloakContainer();

    @Server("mcp-server-oidc")
    public static LibertyServer server;

    /** Plain HTTP client for MCP calls; carries a Keycloak-trusting discovery client. */
    private static McpClient httpClient;

    /**
     * HTTPS client — trusts both the Liberty and Keycloak TLS certificates.
     * Built in {@link #setup()} after the server starts and {@code key.p12} exists on disk.
     */
    private static McpClient httpsClient;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oidcAuthFlowTests.war")
                                   .addPackage(RolesAllowedTools.class.getPackage())
                                   .addAsWebInfResource(
                                                        new File("publish/servers/mcp-server-oidc/resources/WEB-INF/web.xml"),
                                                        "web.xml");
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);

        keycloakContainer.setupRealm();

        // Write the live Keycloak coordinates into server.xml BEFORE starting the server
        // so Liberty picks up the correct issuer, JWK endpoint, and trust store on first boot.
        keycloakContainer.updateServerConfig(server);

        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
        // Wait for LTPA configuration to be ready
        server.waitForLTPAConfigReady();
        server.waitForDefaultHTTPEndpointSSLStart();

        // Plain HTTP client for MCP calls.
        // The Keycloak discovery client is attached so that fetchJson/fetchAccessToken can
        // follow HTTPS discovery URLs (Keycloak AS metadata, token endpoint) even though
        // the MCP calls themselves go over plain HTTP.
        httpClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS)
                        .withDiscoveryClient(keycloakContainer.getHttpClient());

        // HTTPS client — built now that key.p12 exists on disk.
        // This SSLContext trusts both Liberty's auto-generated cert and the Keycloak self-signed cert,
        // so it handles every URL in the discovery chain over TLS.
        httpsClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS,
                                    HttpsRequestHelper.buildCombinedSslContext(keycloakContainer, server));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0014W");
    }

    // HTTP tests

    @Test
    public void testUnauthenticatedRequestReturns401WithResourceMetadataInWwwAuthenticate() throws Exception {
        assertUnauthenticated401WithResourceMetadata(httpClient, "adminTool");
    }

    @Test
    public void testProtectedResourceMetadataIsDiscoverableFromWwwAuthenticateHeader() throws Exception {
        assertProtectedResourceMetadataDiscoverable(httpClient, "adminTool");
    }

    @Test
    public void testAuthorizationServerMetadataIsDiscoverableFromProtectedResourceMetadata() throws Exception {
        assertAuthorizationServerMetadataDiscoverable(httpClient, "adminTool");
    }

    @Test
    public void testFullOAuthDiscoveryFlowAllowsAdminToolCall() throws Exception {
        assertFullFlowAllowsToolCall(httpClient, "adminTool", TEST_ADMIN_USERNAME,
                                     """
                                     {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}],"isError":false}}
                                     """);
    }

    @Test
    public void testFullOAuthDiscoveryFlowAllowsUserToolCall() throws Exception {
        assertFullFlowAllowsToolCall(httpClient, "userTool", TEST_USER_USERNAME,
                                     """
                                     {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}],"isError":false}}
                                     """);
    }

    @Test
    public void testFullOAuthDiscoveryFlowDeniesUserAccessToAdminTool() throws Exception {
        assertFullFlowDeniesUserAccessToAdminTool(httpClient);
    }

    // HTTPS tests

    @Test
    public void testUnauthenticatedRequestReturns401WithResourceMetadataInWwwAuthenticate_Https() throws Exception {
        assertUnauthenticated401WithResourceMetadata(httpsClient, "adminTool");
    }

    @Test
    public void testProtectedResourceMetadataIsDiscoverableFromWwwAuthenticateHeader_Https() throws Exception {
        assertProtectedResourceMetadataDiscoverable(httpsClient, "adminTool");
    }

    @Test
    public void testAuthorizationServerMetadataIsDiscoverableFromProtectedResourceMetadata_Https() throws Exception {
        assertAuthorizationServerMetadataDiscoverable(httpsClient, "adminTool");
    }

    @Test
    public void testFullOAuthDiscoveryFlowAllowsAdminToolCall_Https() throws Exception {
        assertFullFlowAllowsToolCall(httpsClient, "adminTool", TEST_ADMIN_USERNAME,
                                     """
                                     {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}],"isError":false}}
                                     """);
    }

    @Test
    public void testFullOAuthDiscoveryFlowAllowsUserToolCall_Https() throws Exception {
        assertFullFlowAllowsToolCall(httpsClient, "userTool", TEST_USER_USERNAME,
                                     """
                                     {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}],"isError":false}}
                                     """);
    }

    @Test
    public void testFullOAuthDiscoveryFlowDeniesUserAccessToAdminTool_Https() throws Exception {
        assertFullFlowDeniesUserAccessToAdminTool(httpsClient);
    }

    // -------------------------------------------------------------------------
    // Shared scenario helpers
    // -------------------------------------------------------------------------

    /**
     * An unauthenticated tool call must return 401 with a
     * {@code WWW-Authenticate} header that contains a {@code resource_metadata} URL
     * (RFC 9728 §3).
     */
    private static void assertUnauthenticated401WithResourceMetadata(McpClient client, String toolName) throws Exception {
        McpDetailedAuthResponse response = client.callMCP401AuthErrorExpected(toolCallRequest(toolName));

        assertEquals("Expected HTTP 401 for unauthenticated call", 401, response.statusCode());

        String wwwAuthenticate = response.wwwAuthenticate();
        assertNotNull("WWW-Authenticate header must be present on 401 response", wwwAuthenticate);
        assertTrue("WWW-Authenticate must use Bearer scheme", wwwAuthenticate.contains("Bearer"));
        assertTrue("WWW-Authenticate must include realm", wwwAuthenticate.contains("realm="));
        assertTrue("WWW-Authenticate must include resource_metadata URL (RFC 9728)",
                   wwwAuthenticate.contains("resource_metadata="));
    }

    /**
     * Read the {@code resource_metadata} URL from the 401 response,
     * fetch the Protected Resource Metadata document, and validate its structure.
     */
    private static void assertProtectedResourceMetadataDiscoverable(McpClient client, String toolName) throws Exception {
        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(toolCallRequest(toolName));
        assertEquals(401, authResponse.statusCode());

        String resourceMetadataUrl = extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject metadata = client.fetchJson(resourceMetadataUrl);
        assertNotNull("Protected Resource Metadata must contain 'resource' field",
                      metadata.optString("resource", null));
        assertTrue("Protected Resource Metadata must contain a non-empty 'authorization_servers' array",
                   metadata.has("authorization_servers") && metadata.getJSONArray("authorization_servers").length() > 0);
    }

    /**
     * Follow the discovery chain from the 401 response all the way to
     * the Authorization Server Metadata document.
     */
    private static void assertAuthorizationServerMetadataDiscoverable(McpClient client, String toolName) throws Exception {
        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(toolCallRequest(toolName));
        String resourceMetadataUrl = extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = client.fetchJson(resourceMetadataUrl);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);
        assertNotNull("authorization_servers[0] must not be null", authorizationServerUrl);

        JSONObject asMetadata = client.fetchJson(authorizationServerUrl + AS_METADATA_SUFFIX);
        assertNotNull("AS metadata must contain 'issuer'", asMetadata.optString("issuer", null));
        assertNotNull("AS metadata must contain 'token_endpoint'", asMetadata.optString("token_endpoint", null));
        assertNotNull("AS metadata must contain 'jwks_uri'", asMetadata.optString("jwks_uri", null));
    }

    /**
     * Full end-to-end OAuth 2.0 discovery flow:
     * unauthenticated call → discover endpoints → obtain token → call tool → verify response.
     */
    private void assertFullFlowAllowsToolCall(McpClient unauthClient, String toolName,
                                              String username, String expectedResponse) throws Exception {
        String tokenEndpoint = discoverTokenEndpoint(unauthClient, toolName);
        String accessToken = unauthClient.fetchAccessToken(tokenEndpoint, KeycloakContainer.PUBLIC_CLIENT_ID,
                                                           username, TEST_PASSWORD);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS,
                                                      accessToken, unauthClient.getSslContext());
        String response = authenticatedClient.callMCPWithBearerToken(toolCallRequest(toolName));

        JSONAssert.assertEquals(expectedResponse, response, true);
    }

    /**
     * A token obtained for a regular user must not grant access to an admin-only tool (403 Forbidden).
     */
    private void assertFullFlowDeniesUserAccessToAdminTool(McpClient unauthClient) throws Exception {
        String tokenEndpoint = discoverTokenEndpoint(unauthClient, "adminTool");
        String accessToken = unauthClient.fetchAccessToken(tokenEndpoint, KeycloakContainer.PUBLIC_CLIENT_ID,
                                                           TEST_USER_USERNAME, TEST_PASSWORD);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS,
                                                      accessToken, unauthClient.getSslContext());
        authenticatedClient.callMCPWithBearerTokenAuthorisationErrorExpected(toolCallRequest("adminTool"));
    }

    // Private helpers

    /**
     * Runs the full discovery flow and returns the {@code token_endpoint} URL.
     */
    private static String discoverTokenEndpoint(McpClient client, String toolName) throws Exception {
        McpDetailedAuthResponse authChallenge = client.callMCP401AuthErrorExpected(toolCallRequest(toolName));
        assertEquals("Unauthenticated request must return 401", 401, authChallenge.statusCode());

        String resourceMetadataUrl = extractResourceMetadataUrl(authChallenge.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = client.fetchJson(resourceMetadataUrl);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);

        JSONObject asMetadata = client.fetchJson(authorizationServerUrl + AS_METADATA_SUFFIX);
        String tokenEndpoint = asMetadata.getString("token_endpoint");
        assertNotNull("token_endpoint must be present in AS metadata", tokenEndpoint);
        return tokenEndpoint;
    }

    /**
     * Builds a JSON-RPC {@code tools/call} request body for the given tool name.
     */
    private static String toolCallRequest(String toolName) {
        return String.format("""
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
    }

    /**
     * Parses the {@code resource_metadata="<url>"} parameter from a {@code WWW-Authenticate} header.
     */
    private static String extractResourceMetadataUrl(String wwwAuthenticate) {
        if (wwwAuthenticate == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("resource_metadata=\"([^\"]+)\"")
                        .matcher(wwwAuthenticate);
        return matcher.find() ? matcher.group(1) : null;
    }
}

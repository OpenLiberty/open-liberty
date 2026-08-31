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
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

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
 * <p>Each scenario is exercised twice — once over plain HTTP (using {@link McpClient})
 * and once over HTTPS (the {@code _Https} variants).
 *
 * <p>The full flow under test is:
 * <ol>
 * <li>Attempt to connect and call a protected tool without an access token</li>
 * <li>Receive 401 with a {@code WWW-Authenticate} header containing a {@code resource_metadata} URL</li>
 * <li>Fetch the Protected Resource Metadata from the discovered URL</li>
 * <li>Read the {@code authorization_servers} field to find the Authorization Server</li>
 * <li>Fetch the Authorization Server Metadata ({@code /.well-known/openId-configuration})</li>
 * <li>Complete the OAuth 2.0 ROPC login against the discovered {@code token_endpoint}</li>
 * <li>Call the protected tool with the obtained access token and verify the response</li>
 * </ol>
 *
 * <p><b>Helper classes:</b>
 * <ul>
 * <li>{@link HttpRequestHelper} — shared utilities: {@code toolCallRequest},
 * {@code extractResourceMetadataUrl}, {@code fetchJson}, {@code fetchAccessToken}</li>
 * <li>{@link HttpsRequestHelper} — HTTPS utilities: {@code buildLibertyAndKeycloakHttpClient},
 * {@code postMcpHttps}, {@code fetchJsonHttps}, {@code discoverTokenEndpointHttps}</li>
 * </ul>
 *
 * <p>This class deploys its own war ({@code oidcAuthFlowTests.war}) against the
 * {@code mcp-server-oidc} Liberty server. The {@code @ClassRule} Keycloak container is
 * started before the first test in this class and stopped after the last.
 */
@RunWith(FATRunner.class)
public class AuthorizationFlowTests extends FATServletClient {

    private static final String CONTEXT_ROOT = "/oidcAuthFlowTests";
    private static final String MCP_PATH = CONTEXT_ROOT + "/mcp";

    // Credentials used by the Keycloak test users
    private static final String TEST_ADMIN_USERNAME = KeycloakContainer.getTestAdminUsername();
    private static final String TEST_USER_USERNAME = KeycloakContainer.getTestUserUsername();
    private static final String TEST_PASSWORD = KeycloakContainer.getTestPassword();

    // Provides an isolated Keycloak instance for this test class
    @ClassRule
    public static KeycloakContainer keycloakContainer = new KeycloakContainer();

    @Server("mcp-server-oidc")
    public static LibertyServer server;

    /**
     * A {@link HttpClient} that trusts both the Liberty and Keycloak TLS certificates.
     * Built in {@link #setup()} after the server starts and {@code key.p12} exists on disk.
     * Used by all {@code _Https} test variants via {@link HttpsRequestHelper}.
     */
    private static HttpClient libertyAndKeycloakHttpClient;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oidcAuthFlowTests.war")
                                   .addPackage(RolesAllowedTools.class.getPackage())
                                   .addAsWebInfResource(
                                                        new File("publish/servers/mcp-server-oidc/resources/WEB-INF/web.xml"),
                                                        "web.xml");
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);

        // // Creates the realm, clients, users, and groups inside Keycloak.
        keycloakContainer.setupRealm();

        // Write the live Keycloak coordinates into server.xml BEFORE starting the server
        // so Liberty picks up the correct issuer, JWK endpoint, and trust store on first boot.
        keycloakContainer.updateServerConfig(server);

        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
        // Wait for LTPA configuration to be ready
        server.waitForLTPAConfigReady();
        server.waitForDefaultHTTPEndpointSSLStart();

        // Build the combined HTTPS client now that key.p12 exists
        // This client trusts both Liberty's auto-generated cert and the Keycloak self-signed cert
        libertyAndKeycloakHttpClient = HttpsRequestHelper.buildLibertyAndKeycloakHttpClient(keycloakContainer, server);
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0014W");
    }

    // HTTP tests
    /**
     * An unauthenticated tool call must return 401 with a
     * {@code WWW-Authenticate} header that contains a {@code resource_metadata} URL
     * (RFC 9728 §3).
     */
    @Test
    public void testUnauthenticatedRequestReturns401WithResourceMetadataInWwwAuthenticate() throws Exception {
        McpClient client = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);

        McpDetailedAuthResponse response = client.callMCP401AuthErrorExpected(HttpRequestHelper.toolCallRequest("adminTool"));

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
    @Test
    public void testProtectedResourceMetadataIsDiscoverableFromWwwAuthenticateHeader() throws Exception {
        McpClient client = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);

        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(HttpRequestHelper.toolCallRequest("adminTool"));
        assertEquals(401, authResponse.statusCode());

        String resourceMetadataUrl = HttpRequestHelper.extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject metadata = HttpRequestHelper.fetchJson(resourceMetadataUrl, keycloakContainer);
        assertNotNull("Protected Resource Metadata must contain 'resource' field",
                      metadata.optString("resource", null));
        assertTrue("Protected Resource Metadata must contain a non-empty 'authorization_servers' array",
                   metadata.has("authorization_servers") && metadata.getJSONArray("authorization_servers").length() > 0);
    }

    /**
     * Follow the discovery chain from the 401 response all the way to
     * the Authorization Server Metadata document.
     */
    @Test
    public void testAuthorizationServerMetadataIsDiscoverableFromProtectedResourceMetadata() throws Exception {
        McpClient client = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);

        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(HttpRequestHelper.toolCallRequest("adminTool"));
        String resourceMetadataUrl = HttpRequestHelper.extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = HttpRequestHelper.fetchJson(resourceMetadataUrl, keycloakContainer);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);
        assertNotNull("authorization_servers[0] must not be null", authorizationServerUrl);
        System.out.println("[OidcAuthorizationFlowTests] authorization_servers[0] = " + authorizationServerUrl);

        JSONObject asMetadata = HttpRequestHelper.fetchJson(authorizationServerUrl + HttpRequestHelper.AS_METADATA_SUFFIX, keycloakContainer);
        assertNotNull("AS metadata must contain 'issuer'", asMetadata.optString("issuer", null));
        assertNotNull("AS metadata must contain 'token_endpoint'", asMetadata.optString("token_endpoint", null));
        assertNotNull("AS metadata must contain 'jwks_uri'", asMetadata.optString("jwks_uri", null));
    }

    /**
     * Steps 1-7 (full flow, admin) — Complete end-to-end OAuth 2.0 discovery flow over HTTP:
     * unauthenticated call → discover endpoints → obtain admin token → call admin tool.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsAdminToolCall() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("adminTool");

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_ADMIN_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        String response = authenticatedClient.callMCPWithBearerToken(HttpRequestHelper.toolCallRequest("adminTool"));

        JSONAssert.assertEquals("""
                        {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}],"isError":false}}
                        """,
                                response, true);
    }

    /**
     * Steps 1-7 (full flow, user) — Same discovery chain for a regular user calling a
     * user-role tool over HTTP.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsUserToolCall() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("userTool");

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        String response = authenticatedClient.callMCPWithBearerToken(HttpRequestHelper.toolCallRequest("userTool"));

        JSONAssert.assertEquals("""
                        {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}],"isError":false}}
                        """,
                                response, true);
    }

    /**
     * Steps 1-7 (full flow, negative) — A token obtained for a regular user must not
     * grant access to an admin-only tool (403 Forbidden) over HTTP.
     */
    @Test
    public void testFullOAuthDiscoveryFlowDeniesUserAccessToAdminTool() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("adminTool");

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        authenticatedClient.callMCPWithBearerTokenAuthorisationErrorExpected(HttpRequestHelper.toolCallRequest("adminTool"));
    }

    // HTTPS tests
    // All HTTPS helpers delegate to KeycloakHttpsContainer.

    /**
     * HTTPS — unauthenticated tool call must return 401 with a
     * {@code WWW-Authenticate} header containing a {@code resource_metadata} URL.
     */
    @Test
    public void testUnauthenticatedRequestReturns401WithResourceMetadataInWwwAuthenticate_Https() throws Exception {
        HttpResponse<String> response = HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, ("adminTool"), null, 401);

        String wwwAuthenticate = response.headers().firstValue("WWW-Authenticate").orElse(null);
        assertNotNull("WWW-Authenticate header must be present on 401 HTTPS response", wwwAuthenticate);
        assertTrue("WWW-Authenticate must use Bearer scheme", wwwAuthenticate.contains("Bearer"));
        assertTrue("WWW-Authenticate must include realm", wwwAuthenticate.contains("realm="));
        assertTrue("WWW-Authenticate must include resource_metadata URL (RFC 9728)", wwwAuthenticate.contains("resource_metadata="));
    }

    /**
     * HTTPS — fetches the Protected Resource Metadata document over HTTPS
     * and validates its structure.
     */
    @Test
    public void testProtectedResourceMetadataIsDiscoverableFromWwwAuthenticateHeader_Https() throws Exception {
        HttpResponse<String> challengeResponse = HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, HttpRequestHelper.toolCallRequest("adminTool"),
                                                                                 null, 401);

        String resourceMetadataUrl = HttpRequestHelper.extractResourceMetadataUrl(challengeResponse.headers().firstValue("WWW-Authenticate").orElse(null));
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject metadata = HttpsRequestHelper.fetchJsonHttps(resourceMetadataUrl, libertyAndKeycloakHttpClient);
        assertNotNull("Protected Resource Metadata must contain 'resource' field", metadata.optString("resource", null));
        assertTrue("Protected Resource Metadata must contain a non-empty 'authorization_servers' array",
                   metadata.has("authorization_servers") && metadata.getJSONArray("authorization_servers").length() > 0);
    }

    /**
     * HTTPS — follows the full discovery chain from the 401 response to
     * the Authorization Server Metadata document, all over HTTPS.
     */
    @Test
    public void testAuthorizationServerMetadataIsDiscoverableFromProtectedResourceMetadata_Https() throws Exception {
        HttpResponse<String> challengeResponse = HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, HttpRequestHelper.toolCallRequest("adminTool"),
                                                                                 null, 401);

        String resourceMetadataUrl = HttpRequestHelper.extractResourceMetadataUrl(challengeResponse.headers().firstValue("WWW-Authenticate").orElse(null));
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = HttpsRequestHelper.fetchJsonHttps(resourceMetadataUrl, libertyAndKeycloakHttpClient);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);
        assertNotNull("authorization_servers[0] must not be null", authorizationServerUrl);
        System.out.println("[OidcAuthorizationFlowTests] HTTPS: authorization_servers[0] = " + authorizationServerUrl);

        JSONObject asMetadata = HttpsRequestHelper.fetchJsonHttps(authorizationServerUrl + HttpRequestHelper.AS_METADATA_SUFFIX, libertyAndKeycloakHttpClient);
        assertNotNull("AS metadata must contain 'issuer'", asMetadata.optString("issuer", null));
        assertNotNull("AS metadata must contain 'token_endpoint'", asMetadata.optString("token_endpoint", null));
        assertNotNull("AS metadata must contain 'jwks_uri'", asMetadata.optString("jwks_uri", null));
    }

    /**
     * HTTPS — full end-to-end OAuth 2.0 discovery flow over TLS for an admin user.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsAdminToolCall_Https() throws Exception {
        String tokenEndpoint = HttpsRequestHelper.discoverTokenEndpointHttps("adminTool", server, MCP_PATH, libertyAndKeycloakHttpClient);

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_ADMIN_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        HttpResponse<String> response = HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, HttpRequestHelper.toolCallRequest("adminTool"), accessToken,
                                                                        200);

        JSONAssert.assertEquals(
                                """
                                                {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}],"isError":false}}
                                                """,
                                response.body(), true);
    }

    /**
     * HTTPS — full end-to-end OAuth 2.0 discovery flow over TLS for a regular user.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsUserToolCall_Https() throws Exception {
        String tokenEndpoint = HttpsRequestHelper.discoverTokenEndpointHttps("userTool", server, MCP_PATH, libertyAndKeycloakHttpClient);

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        HttpResponse<String> response = HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, HttpRequestHelper.toolCallRequest("userTool"), accessToken,
                                                                        200);

        JSONAssert.assertEquals(
                                """
                                                {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}],"isError":false}}
                                                """,
                                response.body(), true);
    }

    /**
     * HTTPS — a regular-user token must not grant access to an admin-only tool (403 Forbidden).
     */
    @Test
    public void testFullOAuthDiscoveryFlowDeniesUserAccessToAdminTool_Https() throws Exception {
        String tokenEndpoint = HttpsRequestHelper.discoverTokenEndpointHttps("adminTool", server, MCP_PATH, libertyAndKeycloakHttpClient);

        String accessToken = HttpRequestHelper.fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, TEST_PASSWORD, keycloakContainer);
        assertNotNull("Access token must not be null", accessToken);

        HttpsRequestHelper.postMcpHttps(server, MCP_PATH, libertyAndKeycloakHttpClient, HttpRequestHelper.toolCallRequest("adminTool"), accessToken, 403);
    }

    // HTTP helper methods

    /**
     * Runs the full HTTP discovery flow and returns the {@code token_endpoint} URL.
     */
    private String discoverTokenEndpoint(String toolName) throws Exception {
        McpClient unauthClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);
        McpDetailedAuthResponse authChallenge = unauthClient.callMCP401AuthErrorExpected(HttpRequestHelper.toolCallRequest(toolName));
        assertEquals("Unauthenticated request must return 401", 401, authChallenge.statusCode());

        String resourceMetadataUrl = HttpRequestHelper.extractResourceMetadataUrl(authChallenge.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = HttpRequestHelper.fetchJson(resourceMetadataUrl, keycloakContainer);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);

        JSONObject asMetadata = HttpRequestHelper.fetchJson(
                                                            authorizationServerUrl + HttpRequestHelper.AS_METADATA_SUFFIX, keycloakContainer);
        String tokenEndpoint = asMetadata.getString("token_endpoint");
        assertNotNull("token_endpoint must be present in AS metadata", tokenEndpoint);
        return tokenEndpoint;
    }
}

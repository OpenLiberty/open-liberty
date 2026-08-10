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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * by the MCP Authorization specification (RFC 9728):
 *
 * <ol>
 * <li>Attempt to connect and call a protected tool without an access token</li>
 * <li>Receive 401 with a {@code WWW-Authenticate} header containing a {@code resource_metadata} URL</li>
 * <li>Fetch the Protected Resource Metadata from the discovered URL</li>
 * <li>Read the {@code authorization_servers} field to find the Authorization Server</li>
 * <li>Fetch the Authorization Server Metadata ({@code /.well-known/openid-configuration})</li>
 * <li>Complete the OAuth 2.0 ROPC login against the discovered {@code token_endpoint}</li>
 * <li>Create a new {@link McpClient} with the access token and successfully call the protected tool</li>
 * </ol>
 *
 * <p>This class re-uses the {@code mcp-server-oidc} Liberty server and Keycloak container that
 * {@link OidcTests} already spins up. It deploys its own war ({@code oidcAuthFlowTests.war}) so
 * the two test classes can run independently without interfering with each other.
 */
@RunWith(FATRunner.class)
public class OidcAuthorizationFlowTests extends FATServletClient {

    private static final String CONTEXT_ROOT = "/oidcAuthFlowTests";
    private static final String AS_METADATA_SUFFIX = "/.well-known/oauth-authorization-server";

    // Credentials — must match the Keycloak realm configured via KeycloakContainer
    private static final String TEST_ADMIN_USERNAME = "admin@example.com";
    private static final String TEST_USER_USERNAME = "user@example.com";

    // @ClassRule on the shared Keycloak container ensures it is running whether this
    // test class runs standalone or as part of the full suite after OidcTests.
    // Testcontainers is idempotent: starting an already-running container is a no-op.
    @ClassRule
    public static KeycloakContainer keycloakContainer = new KeycloakContainer();

    // Re-use the same server and Keycloak container as OidcTests.
    // OidcTests runs first in the suite; it stops the server in its @AfterClass,
    // so we must start it again here and stop it ourselves when done.
    @Server("mcp-server-oidc")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oidcAuthFlowTests.war")
                                   .addPackage(RolesAllowedTools.class.getPackage())
                                   .addAsWebInfResource(
                                                        new File("publish/servers/mcp-server-oidc/resources/WEB-INF/web.xml"),
                                                        "web.xml");
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        // setupRealm() is idempotent — if OidcTests already ran it this is a no-op;
        // if this class runs standalone it creates the realm, clients, users, and groups.
        keycloakContainer.setupRealm();
        // wipe the oidcAuthFlowTests.war we just deployed above.
        // cleanStart=false: no --clean flag; the OSGi cache from OidcTests' run is still valid.
        server.startServer("OidcAuthorizationFlowTests.log", false, false);
        // Write the live Keycloak coordinates into server.xml now that the server is running
        // so waitForStringInLogUsingMark works correctly against an active log.
        keycloakContainer.updateServerConfig(server);
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        // CWWKZ0014W: oidcTests.war is declared in server.xml but was deployed by OidcTests
        // in its own server lifecycle — it will not be present when this class restarts the server.
        // CWWKO0221E: port-already-in-use can appear in the log if a previous server run left a
        // stale port binding entry; it is not caused by this test class.
        server.stopServer("CWWKZ0014W", "CWWKO0221E");
    }

    /**
     * An unauthenticated tool call must return 401 with a
     * {@code WWW-Authenticate} header that contains a {@code resource_metadata} URL
     * (RFC 9728 §3).
     */
    @Test
    public void testUnauthenticatedRequestReturns401WithResourceMetadataInWwwAuthenticate() throws Exception {
        McpClient client = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);

        McpDetailedAuthResponse response = client.callMCP401AuthErrorExpected(toolCallRequest("adminTool"));

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

        // Steps 1-2: unauthenticated call → 401 + WWW-Authenticate
        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(toolCallRequest("adminTool"));
        assertEquals(401, authResponse.statusCode());

        // Step 3: extract the resource_metadata URL from the WWW-Authenticate header
        String resourceMetadataUrl = extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        // Step 4: fetch and validate the Protected Resource Metadata document
        JSONObject metadata = fetchJson(resourceMetadataUrl);
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

        // Steps 1-3
        McpDetailedAuthResponse authResponse = client.callMCP401AuthErrorExpected(toolCallRequest("adminTool"));
        String resourceMetadataUrl = extractResourceMetadataUrl(authResponse.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        // Step 4: Protected Resource Metadata → authorization server base URL
        JSONObject resourceMetadata = fetchJson(resourceMetadataUrl);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);
        assertNotNull("authorization_servers[0] must not be null", authorizationServerUrl);
        System.out.println("[OidcAuthorizationFlowTests] authorization_servers[0] = " + authorizationServerUrl);
        System.out.println("[OidcAuthorizationFlowTests] AS metadata URL = " + authorizationServerUrl + AS_METADATA_SUFFIX);

        // Steps 5-6: fetch AS metadata and validate required fields
        JSONObject asMetadata = fetchJson(authorizationServerUrl + AS_METADATA_SUFFIX);
        assertNotNull("AS metadata must contain 'issuer'", asMetadata.optString("issuer", null));
        assertNotNull("AS metadata must contain 'token_endpoint'", asMetadata.optString("token_endpoint", null));
        assertNotNull("AS metadata must contain 'jwks_uri'", asMetadata.optString("jwks_uri", null));
    }

    /**
     * Steps 1-7 (full flow, admin) — Complete end-to-end OAuth 2.0 discovery flow:
     * unauthenticated call → discover endpoints → obtain admin token → call admin tool.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsAdminToolCall() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("adminTool");

        // Step 6: obtain admin access token via discovered token endpoint
        String accessToken = fetchAccessToken(tokenEndpoint, TEST_ADMIN_USERNAME, KeycloakContainer.TEST_PASSWORD);
        assertNotNull("Access token must not be null", accessToken);

        // Step 7: call the admin-only tool with the discovered access token
        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        String response = authenticatedClient.callMCPWithBearerToken(toolCallRequest("adminTool"));

        JSONAssert.assertEquals(
                                """
                                                {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you handsome admin!"}],"isError":false}}
                                                """,
                                response, true);
    }

    /**
     * Steps 1-7 (full flow, user) — Same discovery chain for a regular user calling a
     * user-role tool.
     */
    @Test
    public void testFullOAuthDiscoveryFlowAllowsUserToolCall() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("userTool");

        String accessToken = fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, KeycloakContainer.TEST_PASSWORD);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        String response = authenticatedClient.callMCPWithBearerToken(toolCallRequest("userTool"));

        JSONAssert.assertEquals(
                                """
                                                {"id":1,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello you basic user"}],"isError":false}}
                                                """,
                                response, true);
    }

    /**
     * Steps 1-7 (full flow, negative) — A token obtained for a regular user must not
     * grant access to an admin-only tool (403 Forbidden).
     */
    @Test
    public void testFullOAuthDiscoveryFlowDeniesUserAccessToAdminTool() throws Exception {
        String tokenEndpoint = discoverTokenEndpoint("adminTool");

        String accessToken = fetchAccessToken(tokenEndpoint, TEST_USER_USERNAME, KeycloakContainer.TEST_PASSWORD);
        assertNotNull("Access token must not be null", accessToken);

        McpClient authenticatedClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS, accessToken);
        authenticatedClient.callMCPWithBearerTokenAuthorisationErrorExpected(toolCallRequest("adminTool"));
    }

    // Helpers

    /**
     * The discovery flow for the given tool and returns the
     * {@code token_endpoint} URL discovered from the Authorization Server Metadata.
     */
    private String discoverTokenEndpoint(String toolName) throws Exception {
        McpClient unauthClient = new McpClient(server, CONTEXT_ROOT, StateMode.STATELESS);
        McpDetailedAuthResponse authChallenge = unauthClient.callMCP401AuthErrorExpected(toolCallRequest(toolName));
        assertEquals("Unauthenticated request must return 401", 401, authChallenge.statusCode());

        String resourceMetadataUrl = extractResourceMetadataUrl(authChallenge.wwwAuthenticate());
        assertNotNull("resource_metadata URL must be present in WWW-Authenticate header", resourceMetadataUrl);

        JSONObject resourceMetadata = fetchJson(resourceMetadataUrl);
        String authorizationServerUrl = resourceMetadata.getJSONArray("authorization_servers").getString(0);
        System.out.println("[OidcAuthorizationFlowTests] discoverTokenEndpoint: authorization_servers[0] = " + authorizationServerUrl);
        System.out.println("[OidcAuthorizationFlowTests] discoverTokenEndpoint: AS metadata URL = " + authorizationServerUrl + AS_METADATA_SUFFIX);

        JSONObject asMetadata = fetchJson(authorizationServerUrl + AS_METADATA_SUFFIX);
        String tokenEndpoint = asMetadata.getString("token_endpoint");
        assertNotNull("token_endpoint must be present in AS metadata", tokenEndpoint);
        return tokenEndpoint;
    }

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
     * Parses the {@code resource_metadata="<url>"} parameter out of a
     * {@code WWW-Authenticate} header value.
     */
    private static String extractResourceMetadataUrl(String wwwAuthenticate) {
        if (wwwAuthenticate == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("resource_metadata=\"([^\"]+)\"").matcher(wwwAuthenticate);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * GETs a JSON document. Uses the Keycloak-trusting {@link HttpClient} for
     * {@code https://} URLs and a plain client for {@code http://} URLs.
     */
    private static JSONObject fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .header("Accept", "application/json")
                                         .GET()
                                         .build();
        HttpClient client = url.startsWith("https://") ? keycloakContainer.getHttpClient() : HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("[OidcAuthorizationFlowTests] fetchJson(" + url + ") -> HTTP "
                               + response.statusCode() + "\nBody: " + response.body());
        }
        assertEquals("Expected HTTP 200 from " + url, 200, response.statusCode());
        return new JSONObject(response.body());
    }

    /**
     * Performs an OAuth 2.0 ROPC grant against {@code tokenEndpoint} and returns the
     * access token string.
     */
    private static String fetchAccessToken(String tokenEndpoint, String username, String password) throws Exception {
        String formData = String.join("&",
                                      "client_id=" + encode(KeycloakContainer.PUBLIC_CLIENT_ID),
                                      "username=" + encode(username),
                                      "password=" + encode(password),
                                      "grant_type=password");

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(tokenEndpoint))
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(formData))
                                         .build();

        HttpResponse<String> response = keycloakContainer.getHttpClient().send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token request failed. Status: " + response.statusCode()
                                       + "\nBody: " + response.body());
        }

        Matcher matcher = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) {
            throw new RuntimeException("No access_token in token response: " + response.body());
        }
        return matcher.group(1);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

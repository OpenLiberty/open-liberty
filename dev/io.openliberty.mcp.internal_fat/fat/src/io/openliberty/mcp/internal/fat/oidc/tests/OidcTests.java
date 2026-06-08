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
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.OpenidConnectClient;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.oidc.tools.RolesAllowedTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.McpDetailedAuthResponse;
import io.openliberty.mcp.internal.fat.utils.McpClient.StateMode;

@RunWith(FATRunner.class)
public class OidcTests extends FATServletClient {

    // Keycloak
    private static final String KEYCLOAK_REALM = "mcp-realm";
    private static final String TEST_ADMIN_USERNAME = "admin@example.com";
    private static final String TEST_USER_USERNAME = "user@example.com";
    private static final String TEST_PASSWORD = "123";
    private static final String LIBERTY_MCP_SERVER_CLIENT_ID = "liberty-mcp-server-conf-client";
    private static final String TEST_PUBLIC_CLIENT_ID = "mcp-public-client";

    private static String keycloakConfidentialClientUUID = null;
    private static String keycloakConfidentialClientSecret = null;
    private static String keycloakPublicClientUUID = null;

    // Container config
    private static final String KEYCLOAK_TAG = "26.6.2";
    private static final String KEYCLOAK_REGISTRY = "quay.io/keycloak/keycloak:" + KEYCLOAK_TAG;

    // Note: We don't use @Rule for McpClient because it runs McpClient.before() which sends the MCP initialize request without an authorization header.
    // This would cause a 401 as the sever in this test use OIDC which requires a token each request
    @Server("mcp-server-oidc")
    public static LibertyServer server;

    @SuppressWarnings("resource")
    @ClassRule
    public static GenericContainer<?> keycloakContainer = new GenericContainer<>(KEYCLOAK_REGISTRY).withExposedPorts(8080)
                                                                                                   .withEnv("KEYCLOAK_ADMIN", "admin")
                                                                                                   .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                                                                                                   .withCommand("start-dev")
                                                                                                   .withLogConsumer(new SimpleLogConsumer(OidcTests.class, "keycloak-containe"))
                                                                                                   .waitingFor(Wait.forLogMessage(".*Listening on:.*", 1)
                                                                                                                   .withStartupTimeout(Duration.ofMinutes(2)));

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "oidcTests.war")
                                   .addPackage(RolesAllowedTools.class.getPackage())
                                   .addAsWebInfResource(new File("publish/servers/mcp-server-oidc/resources/WEB-INF/web.xml"), "web.xml");
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);

        setupKeycloak();
        updateOidcConfigAttributes();
        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
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

    private String getAccessToken(String username, String password) throws Exception {
        Pattern accessTokenPattern = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

        String tokenEndpoint = "http://localhost:" + keycloakContainer.getMappedPort(8080)
                               + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";

        String formData = String.join("&",
                                      "client_id=" + encode(TEST_PUBLIC_CLIENT_ID),
                                      "username=" + encode(username),
                                      "password=" + encode(password),
                                      "grant_type=password");

        Builder requestBuilder = HttpRequest.newBuilder()
                                            .uri(URI.create(tokenEndpoint))
                                            .header("Content-Type", "application/x-www-form-urlencoded")
                                            .POST(HttpRequest.BodyPublishers.ofString(formData));

        HttpResponse<String> response = HttpClient.newHttpClient()
                                                  .send(requestBuilder.build(), BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get access token. Status: " + response.statusCode()
                                       + "\nBody: " + response.body());
        }

        Matcher matcher = accessTokenPattern.matcher(response.body());
        if (!matcher.find()) {
            throw new RuntimeException("No access_token found in Keycloak response: " + response.body());
        }

        String accessToken = matcher.group(1);
        return accessToken;
    }

    /**
     * URL encodes a string value using UTF-8 encoding.
     *
     * This makes values safe to put inside that form body. Without encoding, special characters could break the request.
     *
     * For example, encode("user@example.com") becomes user%40example.com.
     *
     * @param value the string to be URL encoded
     * @return the URL encoded string
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void setupKeycloak() throws CloneNotSupportedException, Exception {
        // Expose Liberty server port to container
        final int localServerPort = server.getHttpDefaultPort();
        Testcontainers.exposeHostPorts(localServerPort);

        // Create realm
        runCommandInContainer("/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 " +
                              "--realm master --user admin --password admin");

        runCommandInContainer("/opt/keycloak/bin/kcadm.sh create realms -s realm=" + KEYCLOAK_REALM + " -s enabled=true");

        // Create confidential client
        keycloakConfidentialClientUUID = runCommandInContainer(
                                                               "/opt/keycloak/bin/kcadm.sh create clients -r " + KEYCLOAK_REALM + " " +
                                                               "-s clientId=" + LIBERTY_MCP_SERVER_CLIENT_ID + " " +
                                                               "-s name=" + LIBERTY_MCP_SERVER_CLIENT_ID + " " +
                                                               "-s enabled=true " +
                                                               "-s publicClient=false " +
                                                               "-s clientAuthenticatorType=client-secret " +
                                                               "-s standardFlowEnabled=true " +
                                                               "-s directAccessGrantsEnabled=true " +
                                                               "-s serviceAccountsEnabled=true " +
                                                               "-s 'redirectUris=[\"*\"]' " +
                                                               "-s 'webOrigins=[\"*\"]' " +
                                                               "-i").trim();

        // Get confidential client secret
        String secretResponse = runCommandInContainer(
                                                      "/opt/keycloak/bin/kcadm.sh get clients/" + keycloakConfidentialClientUUID + "/client-secret -r " + KEYCLOAK_REALM)
                                                                                                                                                                         .trim();
        keycloakConfidentialClientSecret = extractClientSecret(secretResponse);

        // Create public client
        keycloakPublicClientUUID = runCommandInContainer(
                                                         "/opt/keycloak/bin/kcadm.sh create clients -r " + KEYCLOAK_REALM + " " +
                                                         "-s clientId=" + TEST_PUBLIC_CLIENT_ID + " " +
                                                         "-s name=" + TEST_PUBLIC_CLIENT_ID + " " +
                                                         "-s enabled=true " +
                                                         "-s publicClient=true " +
                                                         "-s standardFlowEnabled=true " +
                                                         "-s directAccessGrantsEnabled=true " +
                                                         "-s authorizationServicesEnabled=false " +
                                                         "-s 'redirectUris=[\"*\"]' " +
                                                         "-s 'webOrigins=[\"*\"]' " +
                                                         "-i").trim();

        // Create normal user
        String regularUserId = runCommandInContainer(
                                                     "/opt/keycloak/bin/kcadm.sh create users -r " + KEYCLOAK_REALM + " " +
                                                     "-s username=" + TEST_USER_USERNAME + " " +
                                                     "-s email=" + TEST_USER_USERNAME + " " +
                                                     "-s firstName=Test " +
                                                     "-s lastName=Admin " +
                                                     "-s enabled=true " +
                                                     "-s emailVerified=true " +
                                                     "-s 'requiredActions=[]' " +
                                                     "-i").trim();

        setNonTemporaryPassword(regularUserId);

        // Create admin user
        String adminUserId = runCommandInContainer(
                                                   "/opt/keycloak/bin/kcadm.sh create users -r " + KEYCLOAK_REALM + " " +
                                                   "-s username=" + TEST_ADMIN_USERNAME + " " +
                                                   "-s email=" + TEST_ADMIN_USERNAME + " " +
                                                   "-s firstName=Test " +
                                                   "-s lastName=User " +
                                                   "-s enabled=true " +
                                                   "-s emailVerified=true " +
                                                   "-s 'requiredActions=[]' " +
                                                   "-i").trim();

        setNonTemporaryPassword(adminUserId);

        // Create groups
        String userGroupId = runCommandInContainer(
                                                   "/opt/keycloak/bin/kcadm.sh create groups -r " + KEYCLOAK_REALM + " " +
                                                   "-s name=mcp-user " +
                                                   "-i").trim();

        String adminGroupId = runCommandInContainer(
                                                    "/opt/keycloak/bin/kcadm.sh create groups -r " + KEYCLOAK_REALM + " " +
                                                    "-s name=mcp-admin " +
                                                    "-i").trim();

        // Add regular user to mcp-user group
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh update users/" + regularUserId + "/groups/" + userGroupId + " " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s realm=" + KEYCLOAK_REALM + " " +
                              "-s userId=" + regularUserId + " " +
                              "-s groupId=" + userGroupId + " " +
                              "-n");

        // Add admin user to mcp-user group
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh update users/" + adminUserId + "/groups/" + userGroupId + " " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s realm=" + KEYCLOAK_REALM + " " +
                              "-s userId=" + adminUserId + " " +
                              "-s groupId=" + userGroupId + " " +
                              "-n");

        // Add admin user to mcp-admin group
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh update users/" + adminUserId + "/groups/" + adminGroupId + " " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s realm=" + KEYCLOAK_REALM + " " +
                              "-s userId=" + adminUserId + " " +
                              "-s groupId=" + adminGroupId + " " +
                              "-n");

        // Add group mapper to public client
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh create clients/" + keycloakPublicClientUUID + "/protocol-mappers/models " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s name=groups " +
                              "-s protocol=openid-connect " +
                              "-s protocolMapper=oidc-group-membership-mapper " +
                              "-s 'config.\"claim.name\"=groups' " +
                              "-s 'config.\"full.path\"=false' " +
                              "-s 'config.\"id.token.claim\"=true' " +
                              "-s 'config.\"access.token.claim\"=true' " +
                              "-s 'config.\"userinfo.token.claim\"=true' " +
                              "-s 'config.\"introspection.token.claim\"=true'");

        // Add audience mapper to public client
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh create clients/" + keycloakPublicClientUUID + "/protocol-mappers/models " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s name=liberty-mcp-server-audience " +
                              "-s protocol=openid-connect " +
                              "-s protocolMapper=oidc-audience-mapper " +
                              "-s 'config.\"included.client.audience\"=" + LIBERTY_MCP_SERVER_CLIENT_ID + "' " +
                              "-s 'config.\"id.token.claim\"=false' " +
                              "-s 'config.\"access.token.claim\"=true' " +
                              "-s 'config.\"introspection.token.claim\"=true'");

    }

    private static void updateOidcConfigAttributes() throws CloneNotSupportedException, Exception {
        // Update the Liberty server configuration with dynamic Keycloak values
        String keycloakBaseUrl = "http://localhost:" + keycloakContainer.getMappedPort(8080);
        ServerConfiguration config = server.getServerConfiguration().clone();
        OpenidConnectClient openidConnectClient = config.getOpenidConnectClients().get(0);

        // Update with dynamically created Keycloak client credentials
        openidConnectClient.setClientId(LIBERTY_MCP_SERVER_CLIENT_ID);
        openidConnectClient.setClientSecret(keycloakConfidentialClientSecret);
        openidConnectClient.setJwkEndpointUrl(keycloakBaseUrl + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/certs");
        openidConnectClient.setIssuerIdentifier(keycloakBaseUrl + "/realms/" + KEYCLOAK_REALM);
        openidConnectClient.setAudiences(LIBERTY_MCP_SERVER_CLIENT_ID);

        updateConfigDynamically(server, config);
    }

    protected static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        server.updateServerConfiguration(config);
        // Wait for server config update to complete before continuing
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
    }

    private static void setNonTemporaryPassword(String userId) throws Exception {
        // Set the user's non temporary password
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh update users/" + userId + "/reset-password " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s type=password " +
                              "-s value=" + TEST_PASSWORD + " " +
                              "-s temporary=false " +
                              "-n");

        // Marks the user’s email as verified and clears any required actions on the account, avoid "Account is not fully set up" errors.
        runCommandInContainer(
                              "/opt/keycloak/bin/kcadm.sh update users/" + userId + " " +
                              "-r " + KEYCLOAK_REALM + " " +
                              "-s enabled=true " +
                              "-s emailVerified=true " +
                              "-s 'requiredActions=[]'");
    }

    private static String extractClientSecret(String response) {
        Pattern clientSecretPattern = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = clientSecretPattern.matcher(response);
        if (!matcher.find()) {
            throw new RuntimeException("Could not extract Keycloak client secret from: " + response);
        }
        return matcher.group(1);
    }

    private static String runCommandInContainer(String command) throws IOException, InterruptedException {
        Container.ExecResult result = keycloakContainer.execInContainer("sh", "-c", command);
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Command failed: " + command +
                                       "\nStdout: " + result.getStdout() +
                                       "\nStderr: " + result.getStderr());
        }
        return result.getStdout();
    }

}

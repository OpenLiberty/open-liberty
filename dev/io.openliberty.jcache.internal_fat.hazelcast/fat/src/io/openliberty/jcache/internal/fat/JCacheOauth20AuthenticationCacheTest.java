/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat;

import static io.openliberty.jcache.internal.fat.docker.KeycloakContainer.TEST_REALM;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.config.OAuth2Login;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.docker.KeycloakContainer;

/**
 * Test OAuth2 with distributed authentication cache.
 */
@SkipIfSysProp("skip.tests=true")
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JCacheOauth20AuthenticationCacheTest extends BaseTestCase {
    private static final Class<?> CLASS = JCacheOauth20AuthenticationCacheTest.class;

    @Server("io.openliberty.jcache.internal.fat.oauth20.auth.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.oauth20.auth.cache.2")
    public static LibertyServer server2;

    private static KeycloakContainer keycloak;

    private static final String OAUTH2LOGIN_ID = "keycloakLogin";
    private static final String KEYCLOAK_CLIENT_ID = "oauth_client";
    private static final String TEST_USER = "testuser";
    private static final String TEST_USER_PASS = "testuserpassword";

    private String keycloakClientId = null;
    private String keycloakClientSecret = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeShouldNotSkipTests();

        /*
         * Start the Keycloak IDP.
         */
        keycloak = new KeycloakContainer();
        keycloak.start();

        /*
         * Get the TLS certificate for the Keycloak server and copy to the Liberty servers.
         * The IDP requires HTTPS for some operations, so we need to trust the Keycloak server.
         */
        keycloak.createTrustFromKeycloak("trustPassword", server1, server2);

        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(server1.getServerRoot() + "/apps/helloworld.war"));
            JakartaEE9Action.transformApp(Paths.get(server2.getServerRoot() + "/apps/helloworld.war"));
        } else if (JakartaEE10Action.isActive()) {
            JakartaEE10Action.transformApp(Paths.get(server1.getServerRoot() + "/apps/helloworld.war"));
            JakartaEE10Action.transformApp(Paths.get(server2.getServerRoot() + "/apps/helloworld.war"));
        }
    }

    @AfterClass
    public static void afterClass() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    @Before
    public void before() throws Exception {
        String groupName = UUID.randomUUID().toString();

        /*
         * Update and start server 1.
         */
        startServer1(server1, groupName, null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * Update and start server 2.
         */
        startServer2(server2, groupName);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);

        /*
         * Register server1 as an OAuth20 client in Keycloak.
         */
        keycloakClientId = keycloak.getKeycloakAdmin().registerOAuth20Client(server1, KEYCLOAK_CLIENT_ID, TEST_REALM, OAUTH2LOGIN_ID);
        keycloak.getKeycloakAdmin().createUser(TEST_REALM, TEST_USER, TEST_USER_PASS);
        keycloakClientSecret = keycloak.getKeycloakAdmin().getClientSecret(TEST_REALM, KEYCLOAK_CLIENT_ID);

        /*
         * Update the OAuth2 configuration for each server.
         */
        ServerConfiguration config = server1.getServerConfiguration().clone();
        OAuth2Login oauth2Login = config.getOAuth2Logins().get(0);
        oauth2Login.setClientSecret(keycloakClientSecret);
        oauth2Login.setAuthorizationEndpoint(keycloak.getKeycloakAdmin().getOAuth20AuthorizationEndpoint(TEST_REALM));
        oauth2Login.setTokenEndpoint(keycloak.getKeycloakAdmin().getOAuth20TokenEndpoint(TEST_REALM));
        oauth2Login.setUserApi(keycloak.getKeycloakAdmin().getOAuth20UserApiEndpoint(TEST_REALM));
        updateConfigDynamically(server1, config);

        config = server2.getServerConfiguration().clone();
        oauth2Login = config.getOAuth2Logins().get(0);
        oauth2Login.setClientSecret(keycloakClientSecret);
        oauth2Login.setAuthorizationEndpoint(keycloak.getKeycloakAdmin().getOAuth20AuthorizationEndpoint(TEST_REALM));
        oauth2Login.setTokenEndpoint(keycloak.getKeycloakAdmin().getOAuth20TokenEndpoint(TEST_REALM));
        oauth2Login.setUserApi(keycloak.getKeycloakAdmin().getOAuth20UserApiEndpoint(TEST_REALM));
        updateConfigDynamically(server2, config);
    }

    @After
    public void after() throws Exception {
        try {
            keycloak.getKeycloakAdmin().deleteUser(TEST_REALM, TEST_USER);
        } catch (Exception e) {
            // Ignore.
        }
        if (keycloakClientId != null) {
            try {
                keycloak.getKeycloakAdmin().deleteClient(TEST_REALM, keycloakClientId);
                keycloakClientId = null;
            } catch (Exception e) {
                // Ignore.
            }
        }

        try {
            /*
             * We should not have cleared the authentication cache's JCache.
             */
            assertAuthCacheJCacheNotCleared(server1);
            assertAuthCacheJCacheNotCleared(server2);
        } finally {
            /*
             * Stop the servers in the reverse order they were started.
             */
            try {
                stopServer(server2);
            } finally {
                stopServer(server1);
            }
        }
    }

    /**
     * Test caching of OAuth2 credentials/tokens in the distributed JCache authentication cache. This
     * test will use two Liberty servers that share the distributed JCache authentication cache.
     *
     * <pre>
     * 1. We will do a login to server1 via the IDP. Notice there will be no checks
     *    for whether we hit the in-memory or JCache authentication cache b/c the
     *    authentication is performed against the IDP.
     * 2. Use the LTPA token to authenticate with server1. This will result in hit
     *    to the JCache authentication cache.
     * 3. Use the LTPA token to authenticate with server2. This will result in hit
     *    to the JCache authentication cache.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    public void authcache_oauth2_sociallogin() throws Exception {
        final String sp1Resource = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/helloworld/rest/helloworld";
        final String sp2Resource = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort() + "/helloworld/rest/helloworld";

        WebClient webClient = null;
        try {
            webClient = TestHelpers.getWebClient(true);

            /*
             * 1. We will do a login to server1 via the IDP. Notice there will be no checks
             * for whether we hit the in-memory or JCache authentication cache b/c the
             * authentication is performed against the IDP.
             */
            Page page = buildGetRequest(webClient, sp1Resource);
            page = performKeycloakLogin(webClient, page);
            String response = page.getWebResponse().getContentAsString();
            String ltpaToken = webClient.getCookieManager().getCookie("LtpaToken2").getValue();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsOauth2Credentials(response);

            /*
             * 2. Use the LTPA token to authenticate with server1. This will result in a hit
             * to the JCache authentication cache.
             */
            page = buildGetRequest(webClient, sp1Resource);
            response = page.getWebResponse().getContentAsString();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsOauth2Credentials(response);
            assertJCacheLtpaAuthCacheHit(true, server1, ltpaToken);

            /*
             * 3. Use the LTPA token to authenticate with server2. This will result in hit
             * to the JCache authentication cache.
             */
            page = buildGetRequest(webClient, sp2Resource);
            response = page.getWebResponse().getContentAsString();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsOauth2Credentials(response);
            assertJCacheLtpaAuthCacheHit(true, server2, ltpaToken);

        } finally {
            TestHelpers.destroyWebClient(webClient);
        }
    }

    /**
     * Build and execute a GET request for the specified URL.
     *
     * @param webClient The {@link WebClient} to make the request on.
     * @param url       The endpoint for the resource.
     * @return The response page.
     * @throws Exception If there was an unforeseen error building or executing the GET request.
     */
    private Page buildGetRequest(WebClient webClient, String url) throws Exception {
        final String methodName = "buildGetRequest";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + url);

        WebRequest request = new WebRequest(URI.create(url).toURL(), HttpMethod.GET);
        Page resultPage = webClient.getPage(request);

        /*
         * Print some helpful debug.
         */
        WebResponse response = resultPage.getWebResponse();
        printWebResponse(methodName, response, webClient);

        Log.info(CLASS, methodName, "EXITING " + resultPage);
        return resultPage;
    }

    /**
     * Perform a login to Keycloak.
     *
     * @param webClient The {@link WebClient} to execute the request on.
     * @param startPage The IDP login page.
     * @return The response page.
     * @throws Exception If there was an unforeseen error executing the login.
     */
    private Page performKeycloakLogin(WebClient webClient, Page startPage) throws Exception {
        final String methodName = "performKeycloakLogin";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + startPage);

        HtmlForm form = ((HtmlPage) startPage).getForms().get(0);
        webClient.getOptions().setJavaScriptEnabled(false);

        /*
         * Fill in the login form and submit the login request
         */
        HtmlElement button = form.getInputByName("login");

        HtmlTextInput textField = form.getInputByName("username");
        Log.info(CLASS, methodName, "username field is: " + textField);
        textField.setValueAttribute(TEST_USER);
        HtmlPasswordInput textField2 = form.getInputByName("password");
        Log.info(CLASS, methodName, "password field is: " + textField2);
        textField2.setValueAttribute(TEST_USER_PASS);
        Log.info(CLASS, methodName, "Setting: " + textField + " to: " + TEST_USER);
        Log.info(CLASS, methodName, "Setting: " + textField2 + " to: " + TEST_USER_PASS);

        Log.info(CLASS, methodName, "\'Clicking the " + button + " button\'");

        Page resultPage = button.click();

        /*
         * Print some helpful debug.
         */
        printWebResponse(methodName, resultPage.getWebResponse(), webClient);

        /*
         * make sure the page is processed before continuing
         */
        TestHelpers.waitBeforeContinuing(webClient);

        Log.info(CLASS, methodName, "EXITING " + resultPage);
        return resultPage;
    }

    /**
     * Assert whether the response contains the subject with the OAuth2 credentials.
     *
     * @param response The response to check.
     */
    private static void assertResponseContainsOauth2Credentials(String response) {
        assertTrue("Did not find the WSPrincipal in the subject.", response.contains("WSPrincipal:" + TEST_USER));
        assertTrue("Did not find the UserProfile credential in the subject.", response.contains("Private Credential: com.ibm.websphere.security.social.UserProfile"));
        assertTrue("Did not find the JWT in the hashtable credential in the subject.", response.matches("(?s).*Private Credential:.*issuedJwt=.*"));
    }
}

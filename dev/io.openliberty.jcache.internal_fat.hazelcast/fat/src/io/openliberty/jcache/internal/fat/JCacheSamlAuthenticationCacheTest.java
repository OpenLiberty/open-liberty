/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.docker.KeycloakContainer;

/**
 * Test SAML with distributed authentication cache.
 */
@SkipIfSysProp("skip.tests=true")
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JCacheSamlAuthenticationCacheTest extends BaseTestCase {
    private static final Class<?> CLASS = JCacheSamlAuthenticationCacheTest.class;

    @Server("io.openliberty.jcache.internal.fat.saml.auth.cache.1")
    public static LibertyServer server1;

    @Server("io.openliberty.jcache.internal.fat.saml.auth.cache.2")
    public static LibertyServer server2;

    private static KeycloakContainer keycloak;

    private static final String DEFAULT_LIBERTY_SAML_SP = "defaultSP";
    private static final String TEST_USER = "testuser";
    private static final String TEST_USER_PASS = "testuserpassword";

    private String keycloakClientId1 = null;
    private String keycloakClientId2 = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeShouldNotSkipTests();

        /*
         * Start the Keycloak IDP.
         */
        keycloak = new KeycloakContainer();
        keycloak.start();

        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server1.getServerRoot() + "/apps/samlclient.war"));
            JakartaEEAction.transformApp(Paths.get(server2.getServerRoot() + "/apps/samlclient.war"));
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
         * Download the SAML descriptors from the IDP and install them in
         * the servers before they start.
         */
        keycloak.downloadSamlDescriptor(TEST_REALM, server1, server2);

        /*
         * Start server 1.
         */
        startServer1(server1, groupName, null, null);
        waitForDefaultHttpsEndpoint(server1);
        waitForCreatedOrExistingJCache(server1, AUTH_CACHE_NAME);

        /*
         * Start server 2.
         */
        startServer2(server2, groupName);
        waitForDefaultHttpsEndpoint(server2);
        waitForCreatedOrExistingJCache(server2, AUTH_CACHE_NAME);

        /*
         * Register the each server as a SAML client.
         */
        keycloakClientId1 = keycloak.getKeycloakAdmin().registerSamlClient(server1, DEFAULT_LIBERTY_SAML_SP, TEST_REALM);
        keycloakClientId2 = keycloak.getKeycloakAdmin().registerSamlClient(server2, DEFAULT_LIBERTY_SAML_SP, TEST_REALM);
        keycloak.getKeycloakAdmin().createUser(TEST_REALM, TEST_USER, TEST_USER_PASS);
    }

    @After
    public void after() throws Exception {
        try {
            keycloak.getKeycloakAdmin().deleteUser(TEST_REALM, TEST_USER);
        } catch (Exception e) {
            // Ignore.
        }
        if (keycloakClientId1 != null) {
            try {
                keycloak.getKeycloakAdmin().deleteClient(TEST_REALM, keycloakClientId1);
                keycloakClientId1 = null;
            } catch (Exception e) {
                // Ignore.
            }
        }
        if (keycloakClientId2 != null) {
            try {
                keycloak.getKeycloakAdmin().deleteClient(TEST_REALM, keycloakClientId2);
                keycloakClientId2 = null;
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
                stopServer(server2, "CWWKS5207W");
            } finally {
                stopServer(server1, "CWWKS5207W");
            }
        }
    }

    /**
     * Test caching of SAML credentials/tokens in the distributed JCache authentication cache. This
     * test will use two Liberty servers that share the distributed JCache authentication cache.
     *
     * <pre>
     * 1. Use an SP initiated request to access the protected resource on server 1.
     *    Neither the in-memory nor the JCache authentication caches should have an entry
     *    as this is the first call.
     * 2. Request the resource from server 1 again. We should be able to directly access it
     *    without an authentication challenge. This time the JCache authentication cache
     *    should have a hit.
     * 3. Request the resource from server 2 using the same conversation (and cookies/tokens)
     *    as the request we made to server 1. Again we should be able to directly access
     *    the resource without an authentication challenge as we should have a JCache authentication
     *    cache hit.
     * </pre>
     *
     * @throws Exception if the test fails for some unforeseen reason.
     */
    @Test
    public void authcache_saml_sp() throws Exception {
        final String sp1Resource = "https://" + server1.getHostname() + ":" + server1.getHttpDefaultSecurePort() + "/samlclient/fat/sp1/SimpleServlet";
        final String sp2Resource = "https://" + server2.getHostname() + ":" + server2.getHttpDefaultSecurePort() + "/samlclient/fat/sp1/SimpleServlet";

        WebClient webClient = null;
        try {
            webClient = TestHelpers.getWebClient(true);

            /*
             * 1. Use an SP initiated request to access the protected resource on server 1.
             * Neither the in-memory nor the JCache authentication caches should have an entry
             * as this is the first call.
             */
            Page page = buildPostSpInitiatedRequest(webClient, sp1Resource);
            page = performIdpLogin(webClient, page);
            page = invokeAcsWithSamlResponse(webClient, page);
            String response = page.getWebResponse().getContentAsString();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsSamlCredentials(response);
            String samlCacheKey = extractCacheKeyFromResponse(response);
            assertJCacheSamlAuthCacheHit(false, server1, samlCacheKey);
            assertInMemorySamlAuthCacheHit(false, server1, samlCacheKey);

            /*
             * 2. Request the resource from server 1 again. We should be able to directly access it
             * without an authentication challenge. This time the JCache authentication cache
             * should have a hit.
             */
            resetMarksInLogs(server1);
            page = invokeAppSameConversation(webClient, sp1Resource);
            response = page.getWebResponse().getContentAsString();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsSamlCredentials(response);
            assertJCacheSamlAuthCacheHit(true, server1, samlCacheKey);

            /*
             * 3. Request the resource from server 2 using the same conversation (and cookies/tokens)
             * as the request we made to server 1. Again we should be able to directly access
             * the resource without an authentication challenge as we should have a JCache authentication
             * cache hit.
             */
            page = invokeAppSameConversation(webClient, sp2Resource);
            response = page.getWebResponse().getContentAsString();
            assertResponseContainsCustomCredentials(response);
            assertResponseContainsSamlCredentials(response);
            assertJCacheSamlAuthCacheHit(true, server2, samlCacheKey);

        } finally {
            TestHelpers.destroyWebClient(webClient);
        }
    }

    /**
     * Extract the SAML cache key from the SimpleServlet response.
     *
     * @param response The SimpleServlet response that contains the subject's private credentials.
     * @return The SAML cache key.
     */
    private String extractCacheKeyFromResponse(String response) {
        Pattern p = Pattern.compile("(?s).*Private Credential:.*com.ibm.wsspi.security.cred.cacheKey=(.+),.*com.ibm.ws.saml.spcookie.*");
        Matcher m = p.matcher(response);
        if (m.matches()) {
            return m.group(1);
        }
        assertNotNull("Expected to find SAML cache key in SimpleServlet response.");
        return null; // Not reachable in JUnit.
    }

    /**
     * Invoke the application endpoint on the {@link WebClient}'s same conversation.
     *
     * @param webClient The {@link WebClient} to use.
     * @param app       The application endpoint.
     * @return The response page.
     * @throws Exception If there was an unforeseen error fulfilling the request.
     */
    private Page invokeAppSameConversation(WebClient webClient, String app) throws Exception {
        final String methodName = "invokeAppSameConversation";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + app);

        URL url = AutomationTools.getNewUrl(app);
        WebRequest request = new WebRequest(url, HttpMethod.POST);

        Page resultPage = webClient.getPage(request);

        /*
         * make sure the page is processed before continuing
         */
        TestHelpers.waitBeforeContinuing(webClient);

        Log.info(CLASS, methodName, "EXITING " + resultPage);
        return resultPage;
    }

    /**
     * Assert whether the response contains the subject with the SAML credentials.
     *
     * @param response The response to check.
     */
    private static void assertResponseContainsSamlCredentials(String response) {
        assertTrue("Did not find the WSPrincipal in the subject.", response.contains("WSPrincipal:testuser@liberty.org"));
        assertTrue("Did not find the Saml20Token credential in the subject.", response.contains("Private Credential: Saml20Token"));
        assertTrue("Did not find the cacheKey / spcookie credential in the subject.",
                   response.matches("(?s).*Private Credential:.*com.ibm.wsspi.security.cred.cacheKey" +
                                    ".*com.ibm.ws.saml.spcookie.session.not.on.or.after.*"));
        assertTrue("Did not find the SamlAssertion in the response.", response.contains("SamlAssertion"));
    }

    /**
     * Invoke the Assertion Consumer Service (ACS) URL with the SAML response.
     *
     * <p/>
     * Based on the method:
     * {@link SAMLCommonTestHelpers#invokeACSWithSAMLResponse(String, WebClient, Object, com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings, List)}.
     *
     * @param webClient The {@link WebClient} to use to invoked the ACS endpoint.
     * @param startPage The page containing the SAML response to use to invoke the ACS endpoint.
     * @return The response page. This should be the SP resource that we originally requested.
     * @throws Exception If there was an unforeseen error fulfilling the requests.
     */
    private Page invokeAcsWithSamlResponse(WebClient webClient, Page startPage) throws Exception {
        final String methodName = "invokeAcsWithSamlResponse";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + startPage);

        /*
         * We get a "continue page" submit that to complete the "login" process
         */
        List<HtmlForm> forms = ((HtmlPage) startPage).getForms();
        if (forms == null || forms.isEmpty()) {
            throw new Exception("Response did not contain any forms but was expected to. Full response was: " + AutomationTools.getResponseText(startPage));
        }
        HtmlForm form = forms.get(0);
        WebRequest request = form.getWebRequest(null);

        /*
         * Clear LTPA cookie sent by IdP we'll end up with multiple ltpa cookies on the next response and it will be
         * hard to know which is the one we want - clear this one webClient.getCookieManager().clearCookies();
         */

        Page resultPage = webClient.getPage(request);

        /*
         * Print some helpful debug.
         */
        WebResponse response = resultPage.getWebResponse();
        printWebResponse(methodName, response, webClient);

        /*
         * make sure the page is processed before continuing
         */
        TestHelpers.waitBeforeContinuing(webClient);

        Log.info(CLASS, methodName, "EXITING " + resultPage);
        return resultPage;
    }

    /**
     * Build and execute a POST request for an SP initiated request for a resource.
     *
     * </p>
     * Based on the method
     * {@link SAMLCommonTestHelpers#buildPostSolicitedSPInitiatedRequest(String, WebClient, com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings, List)}
     *
     * @param webClient The {@link WebClient} to make the request on.
     * @param url       The endpoint for the SP resource.
     * @return The response page.
     * @throws Exception If there was an unforeseen error building or executing the POST request.
     */
    private Page buildPostSpInitiatedRequest(WebClient webClient, String url) throws Exception {
        final String methodName = "buildPostSpInitiatedRequest";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + url);

        WebRequest request = new WebRequest(URI.create(url).toURL(), HttpMethod.POST);
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
     * Perform a login to the IDP.
     *
     * <p/>
     * Base on the method
     * {@link SAMLCommonTestHelpers#performIDPLogin(String, WebClient, HtmlPage, com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings, List)}
     *
     * @param webClient The {@link WebClient} to execute the request on.
     * @param startPage The IDP login page.
     * @return The response page.
     * @throws Exception If there was an unforeseen error executing the login.
     */
    private Page performIdpLogin(WebClient webClient, Page startPage) throws Exception {
        final String methodName = "performIdpLogin";
        Log.info(CLASS, methodName, "ENTERING " + webClient + ", " + startPage);

        /*
         * try to work around a bug in htmlunit where it returns a null list of forms
         * even though there are forms
         */
        HtmlForm form = getForm0WithDebug((HtmlPage) startPage, methodName);
        webClient.getOptions().setJavaScriptEnabled(false);

        /*
         * Fill in the login form and submit the login request
         */
        HtmlElement button = null;

        if (AutomationTools.getResponseText(startPage).contains(SAMLConstants.SAML_REQUEST)) {
            button = form.getButtonByName("redirectform");
        } else {

            button = form.getInputByName("login");

            HtmlTextInput textField = form.getInputByName("username");
            Log.info(CLASS, methodName, "username field is: " + textField);
            textField.setValueAttribute(TEST_USER);
            HtmlPasswordInput textField2 = form.getInputByName("password");
            Log.info(CLASS, methodName, "password field is: " + textField2);
            textField2.setValueAttribute(TEST_USER_PASS);
            Log.info(CLASS, methodName, "Setting: " + textField + " to: " + TEST_USER);
            Log.info(CLASS, methodName, "Setting: " + textField2 + " to: " + TEST_USER_PASS);
        }

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
     * I've been seeing an issue where HTMLUnit is not handling some of the log pages correctly.
     * When we look for the title in the response, it comes back null even though we can see the title when we print the entire
     * response.
     * And, we're also seeing an issue where it can't get the login form from the response
     *
     * *********** after more debugging, I think the issue is a timing problem - the javascript hasn't completed... and
     * the page isn't fully formed - add "waits" after invoking each frame request
     * - leaving this getForm0WithDebug method just in case
     *
     * @param startPage
     * @param testcase
     * @return
     * @throws Exception
     */
    private HtmlForm getForm0WithDebug(HtmlPage startPage, String testcase) throws Exception {
        String thisMethod = "getForm0WithDebug";
        Log.info(CLASS, thisMethod, "ENTERING " + startPage + ", " + testcase);

        HtmlForm form = null;
        try {
            HtmlForm tempForm = startPage.getForms().get(0);
            form = tempForm;
        } catch (Exception e) {
            try {
                Log.error(CLASS, testcase, e, "Exception occurred in " + thisMethod);
                Log.info(CLASS, testcase, "Searching for \"<form\" in the text version of the page");
                Log.info(CLASS, testcase, "Found \"<form\" in the page - if getForms() fails, it is an issue with HTMLUnit");
                Log.info(CLASS, testcase, "Will try again to get the form");
                HtmlForm tempForm = startPage.getForms().get(0);
                form = tempForm;
            } catch (Exception e2) {
                Log.error(CLASS, testcase, e2, "Exception occurred in " + thisMethod);
                List<HtmlForm> allForms = startPage.getForms();
                if (allForms.size() == 0) {
                    fail("Can NOT find any forms on the login page");
                }
                HtmlForm tempForm = allForms.get(0);
                form = tempForm;
            }

        }
        Log.info(CLASS, thisMethod, "EXITING: " + form);
        return form;
    }
}

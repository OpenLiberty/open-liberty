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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.refresh.tests;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerTraceExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonLogoutAndRefreshTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests various flows using refresh.  Make sure that we refresh tokens when we should and do NOT refresh tokens when we should not.
 * Some of the tests are similar to the logout tests, but, we do have refresh specified in the config and we'll just make sure
 * that we behave properly.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class BasicRefreshTests extends CommonLogoutAndRefreshTests {

    protected static Class<?> thisClass = BasicRefreshTests.class;

    @Server("jakartasec-3.0_fat.refresh.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.refresh.rp")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    protected static final boolean TokenAutoRefresh = true;
    protected static final boolean DoNotTokenAutoRefresh = false;

    protected static final boolean ProvderAllowsRefresh = true;
    protected static final boolean ProvderDoesNotAllowsRefresh = false;

    protected static final boolean TokenWasRefreshedSecondTime = true;
    protected static final boolean TokenWasNotRefreshedSecondTime = false;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.JWT_TOKEN_FORMAT);

    static Map<String, String> appMap = new HashMap<String, String>();

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps(); // run this after starting the RP so we have the rp port to update the openIdConfig.properties file within the apps

        baseAppName = "BasicRefreshServlet";
    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        swh.defaultDropinApp(rpServer, "PostLogoutServlet.war", "oidc.client.postLogout.*", "oidc.client.base.utils");

        // deploy the apps that are created at test time from common source
        // Notes about the providers:
        // OP0 - no id or access token lifetime specified
        // OP0_noRefresh - no id or access token lifetime specified, issueRefreshToken=false
        // OP1 - id token short lifetime specified
        // OP1_noRefresh - id token short lifetime specified, issueRefreshToken=false
        // OP2 - access token short lifetime specified
        // OP2_noRefresh - access token short lifetime specified, issueRefreshToken=false
        // OP3 - id and access token short lifetime specified
        // OP3_noRefresh - id and access token short lifetime specified, issueRefreshToken=false

        // Loop through all combinations of tokenAutoRefresh (true/false), providerAllowsRefresh (op providing a refresh token - true/false), notifyProvider (true/false),
        //                                                                        idTokenExpiry (true/false), accessTokenExpiry (true/false)
        // the provider configs used for cases where both expiry settings are false is OP3* (both tokens are short lived)

        // Create unique test apps that use openIdConfig.properties to set the specified values in the @OpenIdAuthenticationMechanismDefinition and @LogoutDefinition of the app.
        // The names that would uniquely identify each test app would be too long, we'll give them generic names and just log the map from names like
        //  TokenAutoRefreshfalseRefreshAllowedfalseNotifyProviderfalseIdTokenExpiryfalseAccessTokenExpirytrueRefreshServlet.war = App_1

        int i = 0;
        for (final boolean tokenAutoRefresh : new boolean[] { DoNotTokenAutoRefresh, TokenAutoRefresh }) {
            for (final boolean providerAllowsRefresh : new boolean[] { false, true }) {
                for (final boolean notifyProvider : new boolean[] { DoNotNotifyProvider, NotifyProvider }) {
                    for (final boolean idTokenExpiry : new boolean[] { IDTokenDoNotHonorExpiry, IDTokenHonorExpiry }) {
                        for (final boolean accessTokenExpiry : new boolean[] { AccessTokenDoNotHonorExpiry, AccessTokenHonorExpiry }) {

                            String appName = buildAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);

                            // Now set the provider that the apps will use (differences are token lifetime settings and whether the provider includes refresh_tokens)
                            String provider = determineProvider(providerAllowsRefresh, idTokenExpiry, accessTokenExpiry);
                            String shortAppName = "App_" + Integer.toString(i);
                            i++;
                            appMap.put(appName, shortAppName);

                            swh.deployConfigurableTestApps(rpServer, shortAppName + ".war", "BasicRefreshServlet.war",
                                                           buildUpdatedConfigMap(shortAppName, provider, tokenAutoRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry, null),
                                                           "oidc.client.basicRefresh.servlets",
                                                           "oidc.client.base.*");
                        }
                    }
                }
            }
        }

        // create a couple extra apps that set the redirectURI
        swh.deployConfigurableTestApps(rpServer, "GoodRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue.war",
                                       "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue",
                                                             "OP3_noRefresh", TokenAutoRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry,
                                                             goodRedirectUri),
                                       "oidc.client.basicRefresh.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "BadRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue.war",
                                       "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("BadRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue",
                                                             "OP3_noRefresh", TokenAutoRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry,
                                                             badRedirectUri),
                                       "oidc.client.basicRefresh.servlets",
                                       "oidc.client.base.*");

        Log.info(thisClass, "deployMyApps", "App Mapping");
        for (Entry<String, String> a : appMap.entrySet()) {
            Log.info(thisClass, "deployMyApps", "Short App Name: " + a.getValue() + " maps to an app that would/could be named: " + a.getKey());
        }

    }

    /**
     * Build the long application name bases on the various config attribute settings that we're looping through
     *
     * @param tokenAutoRefresh - tokenAutoRefresh true/false
     * @param providerAllowsRefresh - does the OP issue a refresh token true/false
     * @param notifyProvider - should the rp notify the provider if tokens are expired true/false
     * @param idTokenExpiry - should the rp honor an expired id_token
     * @param accessTokenExpiry - should the rp honor an expired access_token
     * @return - the generated test application long name
     * @throws Exception
     */
    public static String buildAppName(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                                      boolean accessTokenExpiry) throws Exception {

        String appName = "TokenAutoRefresh" + Boolean.toString(tokenAutoRefresh);
        appName = appName + "RefreshAllowed" + Boolean.toString(providerAllowsRefresh);
        appName = appName + "NotifyProvider" + Boolean.toString(notifyProvider);
        appName = appName + "IdTokenExpiry" + Boolean.toString(idTokenExpiry);
        appName = appName + "AccessTokenExpiry" + Boolean.toString(accessTokenExpiry);
        appName = appName + "RefreshServlet";
        return appName;
    }

    /**
     * Determine which provider the test app should be using based on the config attributes passed in
     *
     * @param tokenAutoRefresh - tokenAutoRefresh true/false
     * @param idTokenLifetimeShort - should the id_token have a short or long lifetime
     * @param accessTokenLifetimeShort - shoudl the access_token have a short or long lifetime
     * @return - the OP that the app should use.
     * @throws Exception
     */
    public static String determineProvider(boolean tokenAutoRefresh, boolean idTokenLifetimeShort, boolean accessTokenLifetimeShort) throws Exception {

        String provider = null;
        if (idTokenLifetimeShort) {
            if (accessTokenLifetimeShort) {
                provider = "OP3";
            } else {
                provider = "OP1";
            }
        } else {
            if (accessTokenLifetimeShort) {
                provider = "OP2";
            } else {
//                provider = "OP0";
                provider = "OP3"; // it's more interesting to have the tokens expired with the annotation have expiry=false
            }
        }
        if (!tokenAutoRefresh) {
            provider = provider + "_noRefresh";
        }
        Log.info(thisClass, "determineProvider", provider);
        return provider;
    }

    /**
     * Build a hashmap of config settings that the EL expressions in the test app annotation will use
     *
     * @param app - the app that we'll be updating
     * @param provider - the OP/provider that the annotation will need to reference
     * @param tokenAutoRefresh - value to set for the topenAutoRefresh annotation attribute
     * @param notifyProvider - value to set for the notifyProovider annotation attribute
     * @param idTokenExpires - value to set for the idTokenExpiry annotation attribute
     * @param accessTokenExpires - value to set for the accessTokenExpir annotation attribute
     * @param redirectUri - value to set for the redirectURI annotation attribute - the value unique to the redirectURI within @LogoutDefinition
     * @return - a map containing all of the config values to include in openIdConfig.properties within the test app
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, boolean tokenAutoRefresh, boolean notifyProvider, boolean idTokenExpires,
                                                            boolean accessTokenExpires, String redirectUri) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

        // set the config value to either enable/disable notification to the provider
        if (tokenAutoRefresh) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getTokenAutoRefreshExpressionTrue());
        } else {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getTokenAutoRefreshExpressionFalse());
        }

        // set the config value to either enable/disable notification to the provider
        if (notifyProvider) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getNotifyProviderExpressionTrue());
        } else {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getNotifyProviderExpressionFalse());
        }

        // set the config attribute to check or not check id_token expiration
        if (idTokenExpires) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getIdentityTokenExpiryExpressionTrue());
        } else {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getIdentityTokenExpiryExpressionFalse());
        }

        // set the config attribute to check or not check access_token expiration
        if (accessTokenExpires) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getAccessTokenExpiryExpressionTrue());
        } else {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getAccessTokenExpiryExpressionFalse());
        }
        if (redirectUri != null) {
            if (redirectUri.equals(goodRedirectUri)) {
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIGood_Logout(rpHttpsBase, null));
            } else {
                if (redirectUri.equals(badRedirectUri)) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIBad_Logout(opHttpsBase));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIEmpty_Logout());
                }
            }
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to return the mapped app name which is much shorter, but not very descriptive.
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param providerAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @return - returns the short app name of the app that has the parm values specified in the openIdConfig.properties file
     * @throws Exception
     */
    public String getShortAppName(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                                  boolean accessTokenExpiry) throws Exception {

        String appName = buildAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);
        return appMap.get(appName);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericGoodRefreshTest method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the on the
     * app again. Validate that 2 different sets of tokens are used for each access - also make sure that there is NO re-authn message in the rp server log. The lack
     * of this message and the validation that the tokens are different show that we've refreshed.
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param providerAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @throws Exception
     */
    public void genericGoodRefreshTest(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                                       boolean accessTokenExpiry, boolean refreshedSecondTime) throws Exception {

        String appName = getShortAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);
        String provider = determineProvider(providerAllowsRefresh, idTokenExpiry, accessTokenExpiry);

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        Page response1 = runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(20);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        Page response2 = invokeAppGetToAppWithRefreshedToken(webClient, url); // get to app not because either id or access token is good, but because the token was refreshed.

        if (tokensAreDifferent(response1, response2, providerAllowsRefresh, TokenWasRefreshed)) {
            Log.info(thisClass, _testName, "Test tokens were refreshed");
        } else {
            fail("Test tokens were NOT refreshed");
        }
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerTraceExpectation(null, rpServer, Constants.STRING_DOES_NOT_MATCH, "Redirect to the OpenID Connect Provider Authentication endpoint for re-authentication", "The request got redirected to the OP to re-authenticate when it should not have."));
        validationUtils.validateResult(response2, expectations);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(20);
        Page response3 = null;
        response3 = invokeAppGetToAppWithRefreshedToken(webClient, url); // get to app not because either id or access token is good, but because the token was refreshed, or we re-using a refreshed token
        if (refreshedSecondTime) {
            if (tokensAreDifferent(response1, response3, providerAllowsRefresh, TokenWasRefreshed)
                && tokensAreDifferent(response2, response3, providerAllowsRefresh, TokenWasRefreshed)) {
                Log.info(thisClass, _testName, "Test tokens were refreshed");
            } else {
                fail("Test tokens were NOT refreshed");
            }
        } else {

            if (tokensAreTheSameIdTokenNull(response2, response3, providerAllowsRefresh)) {
                Log.info(thisClass, _testName, "Test tokens are the same");
            } else {
                fail("Test tokens were different");
            }

        }

        validationUtils.validateResult(response3, expectations);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericReAuthn method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the on the
     * app again. Validate that 2 different sets of tokens are used for each access - also check for a re-authn message in the rp server log to show that the new access
     * was obtained via a re-authn request.
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param providerAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @throws Exception
     */
    public void genericReAuthn(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                               boolean accessTokenExpiry) throws Exception {

        String appName = getShortAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);
        String provider = determineProvider(providerAllowsRefresh, idTokenExpiry, accessTokenExpiry);

        genericReAuthn(rpServer, appName, provider, providerAllowsRefresh, DoNotPromptLogin);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericaGoodLogout method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the logout panel
     * instead of landing on the app.
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param providerAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @throws Exception
     */
    public void genericGoodLogoutTest(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                                      boolean accessTokenExpiry) throws Exception {

        String appName = getShortAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);
        String provider = determineProvider(providerAllowsRefresh, idTokenExpiry, accessTokenExpiry);

        genericGoodLogoutTest(appName, provider);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericGoodEndToEndReAccessTest method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the on the
     * app again. Validate that the same tokens are used for each access - also make sure that there is NO re-authn message in the rp server log to show that we're using
     * the same token
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param providerAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @throws Exception
     */
    public void genericGoodEndToEndReAccessTest(boolean tokenAutoRefresh, boolean providerAllowsRefresh, boolean notifyProvider, boolean idTokenExpiry,
                                                boolean accessTokenExpiry) throws Exception {

        String appName = getShortAppName(tokenAutoRefresh, providerAllowsRefresh, notifyProvider, idTokenExpiry, accessTokenExpiry);
        String provider = determineProvider(providerAllowsRefresh, idTokenExpiry, accessTokenExpiry);

        genericGoodEndToEndReAccessTest(appName, provider, providerAllowsRefresh);

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * All Test will access a protected app and then wait for short lived tokens to expire.
     */

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and both the id and access tokens have expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and the id token has expired. The notifyProvider and the expiry settings should not matter in this case.
     * On the third request, we'll re-use the refreshed token since we no longer have an exp from the id_token - the access_token is NOT expired - test uses OP1
     * which has a long access_token lifetime.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, TokenWasNotRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and the access token has expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and both token have expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and both tokens have expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and the id token has expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, TokenWasNotRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and the id token has expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to use a refreshed token to access the protected app the second time. tokenAutoRefresh is true, the OP includes refresh tokens
     * and both tokens have expired. The notifyProvider and the expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodRefreshTest(TokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, TokenWasRefreshedSecondTime);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is true, but the OP does not include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The tokens have expired and without the refresh token, we'll now logout. The expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is true, but the OP does not include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The id token has expired and without the refresh token, we'll now logout. The expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericGoodLogoutTest(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is true, but the OP does not include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The access token has expired and without the refresh token, we'll now logout. The expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is true, but the OP does not include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The tokens have expired and without the refresh token, we'll now logout. The expiry settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodLogoutTest(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is true, but the OP does not
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The tokens have expired and without the refresh token and notifyProvider is false, we'll now re-authenticate. The expiry
     * settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is true, but the OP does not
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The id token has expired and without the refresh token and notifyProvider is false, we'll now re-authenticate. The expiry
     * settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericReAuthn(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is true, but the OP does not
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The access token has expired and without the refresh token and notifyProvider is false, we'll now re-authenticate. The expiry
     * settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is true, but the OP does not
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The both tokens have expired and without the refresh token and notifyProvider is false, we'll now re-authenticate. The expiry
     * settings should not matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericReAuthn(TokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is false, but the OP does include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The tokens have expired and without the refresh token, we'll now logout. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is false, but the OP does include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The id token has expired and without the refresh token, we'll now logout. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to land on the logout page. tokenAutoRefresh is false, but the OP does include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. The access token has expired and without the refresh token, we'll now logout. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will re-use the original token to access the protected app the second time. tokenAutoRefresh is false, but the OP does
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is true. Both tokens have expired, but, we're not honoring the expiry - we'll re-use the original token.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotTokenAutoRefresh, ProvderAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is false, but the OP does
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. Both tokens have expired, we'll now re-authenticate. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is false, but the OP does
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The id token has expired, we'll now re-authenticate. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    /**
     * This test will expect to use a re-authenticated token to access the protected app the second time. tokenAutoRefresh is false, but the OP does
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. The access token has expired, we'll now re-authenticate. The expiry settings will matter in this case.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    /**
     * This test will re-use the original token to access the protected app the second time. tokenAutoRefresh is false, but the OP does
     * include refresh tokens. - this means that we can't refresh.
     * notifyProvider is false. Both tokens have expired, but, we're not honoring the expiry - we'll re-use the original token.
     *
     * @throws Exception
     */
    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshTrue_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotTokenAutoRefresh, ProvderAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericGoodLogoutTest(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderTrue_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryTrue_accessTokenExpiryFalse() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryTrue() throws Exception {

        genericReAuthn(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_tokenAutoRefreshFalse_providerAllowsRefreshFalse_notifyProviderFalse_idTokenExpiryFalse_accessTokenExpiryFalse() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotTokenAutoRefresh, ProvderDoesNotAllowsRefresh, DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry);

    }

    @Test
    public void BasicRefreshTests_GoodRedirectUri_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_ExpiryFalse_TokensExpiredTrue() throws Exception {

        genericGoodRedirectWithoutLogoutTest("GoodRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue", "OP3", null);

    }

    @Test
    public void BasicRefreshTests_BadRedirectUri_tokenAutoRefreshTrue_providerAllowsRefreshFalse_notifyProviderFalse_ExpiryFalse_TokensExpiredTrue() throws Exception {

        genericGoodSplashPage("BadRedirectUriTokenAutoRefreshTrueRefreshAllowedFalseNotifyProviderFalseTokenExpiryFalseTokensExpiredTrue", "OP3");

    }

}

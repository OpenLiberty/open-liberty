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
package io.openliberty.security.jakartasec.fat.logout.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonLogoutAndRefreshTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

/**
 * Tests various logout flows.  Make sure that we logout when we should and do NOT logout when we should not.
 * Some of the tests are similar to the refresh tests, but, we we'll go into a little bit more detail with regard
 * to expiry and actual expired tokens than the refresh tests will.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class BasicLogoutTests extends CommonLogoutAndRefreshTests {

    protected static Class<?> thisClass = BasicLogoutTests.class;

    @Server("jakartasec-3.0_fat.logout.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.logout.rp")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    protected static final boolean IDTokenShortLifetime = true;
    protected static final boolean IDTokenLongLifetime = false;
    protected static final boolean AccessTokenShortLifetime = true;
    protected static final boolean AccessTokenLongLifetime = false;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.OPAQUE_TOKEN_FORMAT);

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

        baseAppName = "BasicLogoutServlet";

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are created at test time from common source
        // Notes about the providers:
        // OP0 - no id or access token lifetime specified
        // OP1 - id token short lifetime specified
        // OP2 - access token short lifetime specified
        // OP3 - id and access token short lifetime specified

        // Loop through all combinations of notifyProvider (true/false), idTokenExpiry (true/false), accessTokenExpiry (true/false),
        //  idTokenLifetime (Short/Long), and accessTokenLifetime (Short/Long)
        // Create unique test apps that use openIdConfig.properties to set the specified values in the @LogoutDefinition of the app.
        // The names that would uniquely identify each name would be too long, we'll give them generic names and just log the map from names like
        //  NotifyTrueIdTokenExpiryTrueIdTokenShortLifetimeAccessTokenExpiryTrueAccessTokenShortLifetimeRedirectURIDefault.war = App_1

        int i = 0;
        for (final boolean notifyProvider : new boolean[] { false, true }) {
            for (final boolean idTokenExpiry : new boolean[] { false, true }) {
                for (final boolean accessTokenExpiry : new boolean[] { false, true }) {
                    for (final boolean idTokenLifetimeShort : new boolean[] { false, true }) {
                        for (final boolean accessTokenLifetimeShort : new boolean[] { false, true }) {
                            String appName = buildAppName(notifyProvider, idTokenExpiry, accessTokenExpiry, idTokenLifetimeShort, accessTokenLifetimeShort);

                            // Now set the provider that the apps will use (the provider determines how long the tokens will live)
                            String provider = determineProvider(idTokenLifetimeShort, accessTokenLifetimeShort);
                            String shortAppName = "App_" + Integer.toString(i);
                            i++;
                            appMap.put(appName, shortAppName);

                            swh.deployConfigurableTestApps(rpServer, shortAppName + ".war", "BasicLogoutServlet.war",
                                                           buildUpdatedConfigMap(shortAppName, provider, notifyProvider, idTokenExpiry, accessTokenExpiry, null, PromptType.LOGIN),
                                                           "oidc.client.basicLogout.servlets",
                                                           "oidc.client.base.*");
                        }
                    }
                }
            }
        }

        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderTrueLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderTrueLogoutServlet", "OP3", NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry,
                                                             goodRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "BadRedirectNotifyProviderTrueLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("BadRedirectNotifyProviderTrueLogoutServlet", "OP3", NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry,
                                                             badRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "EmptyRedirectNotifyProviderTrueLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("EmptyRedirectNotifyProviderTrueLogoutServlet", "OP3", NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry,
                                                             emptyRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderFalsePromptLoginLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderFalsePromptLoginLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             goodRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderFalsePromptNoneLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderFalsePromptNoneLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             goodRedirectUri, PromptType.NONE),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderFalsePromptConsentLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderFalsePromptConsentLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             goodRedirectUri, PromptType.CONSENT),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderFalsePromptSelectAccountLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderFalsePromptSelectAccountLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             goodRedirectUri, PromptType.SELECT_ACCOUNT),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "GoodRedirectNotifyProviderFalsePromptEmptyLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("GoodRedirectNotifyProviderFalsePromptEmptyLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             goodRedirectUri, null),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "BadRedirectNotifyProviderFalseLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("BadRedirectNotifyProviderFalseLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry,
                                                             badRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "EmptyRedirectNotifyProviderFalseLogoutServlet.war", "BasicLogoutServlet.war",
                                       buildUpdatedConfigMap("EmptyRedirectNotifyProviderFalseLogoutServlet", "OP3", DoNotNotifyProvider, IDTokenHonorExpiry,
                                                             AccessTokenHonorExpiry,
                                                             emptyRedirectUri, PromptType.LOGIN),
                                       "oidc.client.basicLogout.servlets",
                                       "oidc.client.base.*");

//        Log.info(thisClass, "deployMyApps", "App Mapping");
//        for (Entry<String, String> a : appMap.entrySet()) {
//            Log.info(thisClass, "deployMyApps", "Short App Name: " + a.getValue() + " maps to an app that would/could be named: " + a.getKey());
//        }

    }

    /**
     * Build the long application name bases on the various config attribute settings that we're looping through
     *
     * @param notifyProvider - should the rp notify the provider if tokens are expired true/false
     * @param idTokenExpiry - should the rp honor an expired id_token
     * @param accessTokenExpiry - should the rp honor an expired access_token
     * @param idTokenLifetimeShort - should the id_token be expired by the time we make the second request
     * @param accessTokenLifetimeShort - should the access_token be expired by the time we make the second request
     * @return - the generated test application long name
     * @throws Exception
     */
    public static String buildAppName(boolean notifyProvider, boolean idTokenExpiry, boolean accessTokenExpiry, boolean idTokenLifetimeShort,
                                      boolean accessTokenLifetimeShort) throws Exception {
        String appName = "NotifyProvider" + Boolean.toString(notifyProvider);
        appName = appName + "IdTokenExpiry" + Boolean.toString(idTokenExpiry);
        if (idTokenLifetimeShort) {
            appName = appName + "IdTokenShortLifetime";
        } else {
            appName = appName + "IdTokenLongLifetime";
        }
        appName = appName + "AccessTokenExpiry" + Boolean.toString(accessTokenExpiry);
        if (accessTokenLifetimeShort) {
            appName = appName + "AccessTokenShortLifetime";
        } else {
            appName = appName + "AccessTokenLongLifetime";
        }
        appName = appName + "LogoutServlet";
        return appName;
    }

    /**
     * Determine which provider the test app should be using based on the config attributes passed in
     *
     * @param idTokenLifetimeShort - should the id_token have a short or long lifetime
     * @param accessTokenLifetimeShort - shoudl the access_token have a short or long lifetime
     * @return - the OP that the app should use.
     * @throws Exception
     */
    public static String determineProvider(boolean idTokenLifetimeShort, boolean accessTokenLifetimeShort) throws Exception {

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
                provider = "OP0";
            }
        }
        return provider;
    }

    /**
     * Build a hashmap of config settings that the EL expressions in the test app annotation will use
     *
     * @param app - the app that we'll be updating
     * @param provider - the OP/provider that the annotation will need to reference
     * @param notifyProvider - value to set for the notifyProovider annotation attribute
     * @param idTokenExpires - value to set for the idTokenExpiry annotation attribute
     * @param accessTokenExpires - value to set for the accessTokenExpir annotation attribute
     * @param redirectUri - value to set for the redirectURI annotation attribute - the value unique to the redirectURI within @LogoutDefinition
     * @param prompt - value to set for the prompt annotation attribute
     * @return - a map containing all of the config values to include in openIdConfig.properties within the test app
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String app, String provider, boolean notifyProvider, boolean idTokenExpires,
                                                            boolean accessTokenExpires, String redirectUri, PromptType prompt) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

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
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIGood_Logout(opHttpsBase, provider));
            } else {
                if (redirectUri.equals(badRedirectUri)) {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIBad_Logout(opHttpsBase));
                } else {
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectURIEmpty_Logout());
                }
            }
        }
        if (prompt == null) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionEmpty());
        } else {
            switch (prompt) {
                case NONE:
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionNone());
                    break;
                case CONSENT:
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionConsent());
                    break;
                case LOGIN:
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionLogin());
                    break;
                case SELECT_ACCOUNT:
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionSelectAccount());
                    break;
                default:
                    testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getPromptExpressionEmpty());
                    break;
            }
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, app, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to return the mapped app name which is much shorter, but not very descriptive.
     *
     * @param notifyProvider - Flag indicating if the @LogoutDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the access_token should be checked
     * @param idTokenLifetimeShort - Flag indicating if the actaul id_token will be expired when we attempt to access the app the second time
     * @param accessTokenLifetimeShort - Flag indicating if the actaul access_token will be expired when we attempt to access the app the second time
     * @return - returns the short app name of the app that has the parm values specified in the openIdConfig.properties file
     * @throws Exception
     */
    public String getShortAppName(boolean notifyProvider, boolean idTokenExpiry, boolean accessTokenExpiry, boolean idTokenLifetimeShort,
                                  boolean accessTokenLifetimeShort) throws Exception {

        String appName = buildAppName(notifyProvider, idTokenExpiry, accessTokenExpiry, idTokenLifetimeShort, accessTokenLifetimeShort);
        String shortAppName = appMap.get(appName);
        Log.info(thisClass, _testName, "Using app: " + shortAppName + " which maps to an app with settings: " + appName);
        return shortAppName;

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericaGoodLogout method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the logout panel
     * instead of landing on the app.
     *
     * @param notifyProvider - Flag indicating if the @LogoutDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the access_token should be checked
     * @param idTokenLifetimeShort - Flag indicating if the actaul id_token will be expired when we attempt to access the app the second time
     * @param accessTokenLifetimeShort - Flag indicating if the actaul access_token will be expired when we attempt to access the app the second time
     * @throws Exception
     */
    public void genericGoodLogoutTest(boolean notifyProvider, boolean idTokenExpiry,
                                      boolean accessTokenExpiry, boolean idTokenLifetimeShort,
                                      boolean accessTokenLifetimeShort) throws Exception {

        String appName = getShortAppName(notifyProvider, idTokenExpiry, accessTokenExpiry, idTokenLifetimeShort, accessTokenLifetimeShort);
        String provider = determineProvider(idTokenLifetimeShort, accessTokenLifetimeShort);
        genericGoodLogoutTest(appName, provider);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericGoodEndToEndReAccessTest method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the on the
     * app again. Validate that the same tokens are used for each access - also make sure that there is NO re-authn message in the rp server log to show that we're using
     * the same token
     *
     * @param notifyProvider - Flag indicating if the @LogoutDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @LogoutDefinition specifies that expiration of the access_token should be checked
     * @param idTokenLifetimeShort - Flag indicating if the actaul id_token will be expired when we attempt to access the app the second time
     * @param accessTokenLifetimeShort - Flag indicating if the actaul access_token will be expired when we attempt to access the app the second time
     * @throws Exception
     */
    public void genericGoodEndToEndReAccessTest(boolean notifyProvider, boolean idTokenExpiry,
                                                boolean accessTokenExpiry, boolean idTokenLifetimeShort,
                                                boolean accessTokenLifetimeShort) throws Exception {

        String appName = getShortAppName(notifyProvider, idTokenExpiry, accessTokenExpiry, idTokenLifetimeShort, accessTokenLifetimeShort);
        String provider = determineProvider(idTokenLifetimeShort, accessTokenLifetimeShort);
        genericGoodEndToEndReAccessTest(appName, provider, false);

    }

    /**
     * Build the long app name based on the different test settings passed in and use that to obtain the mapped app name which is much shorter, but not very descriptive.
     * Determine the OP to use for this test based on the different test settings passed in.
     * Then invoke the genericGoodEndToEndReAccessTest method to invoke the app, sleep and try to invoke the app again. On the second request, expect to land on the on the
     * app again. Validate that 2 different sets of tokens are used for each access - also check for a re-authn message in the rp server log to show that the new access
     * was obtained via a re-authn request.
     *
     * @param tokenAutoRefresh - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the token should be auto-refreshed
     * @param provderAllowsRefresh - Flag indicating if the OP provider creates refresh_tokens
     * @param notifyProvider - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that the provider should be notified
     * @param idTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the id_token should be checked
     * @param accessTokenExpiry - Flag indicating if the @OpenIdAuthenticationMechanismDefinition specifies that expiration of the access_token should be checked
     * @throws Exception
     */
    public void genericReAuthn(boolean notifyProvider, boolean idTokenExpiry,
                               boolean accessTokenExpiry, boolean idTokenLifetimeShort,
                               boolean accessTokenLifetimeShort) throws Exception {

        String appName = getShortAppName(notifyProvider, idTokenExpiry, accessTokenExpiry, idTokenLifetimeShort, accessTokenLifetimeShort);
        String provider = determineProvider(idTokenLifetimeShort, accessTokenLifetimeShort);
        genericReAuthn(rpServer, appName, provider, false);

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Expect to land on the logout page since both tokens are expired and we'll honor both expirations and notifyProvider is true
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the logout page since the id token is expired and we'll honor both expirations and notifyProvider is true
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the logout page since the access token is expired and we'll honor both expirations and notifyProvider is true
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the logout page since the id token is expired and we'll honor both expirations and notifyProvider is true
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the logout page since the access token is expired and we'll honor both expirations and notifyProvider is true
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we'll honor expired tokens, but, neither token is expired
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since the we'll honor expired id token, but, it's not expired - the access token is expired, but we don't honor it
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we'll honor expired id token, but, it's not expired - the access token is not expired, but we don't honor it
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the logout page since the access token is expired, and we will honor it
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we'll honor expired acsess token, but, it's not expired - the id token is expired, but we don't honor it
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since the we'll honor expired acsess token, but, it's not expired - we'll honor expired id token, but it's not expired
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we won't honor either expired tokens
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the logout page since the access token is expired, and we will honor it
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericGoodLogoutTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we don't honor the id_token, we do honor the access token expiry, but neither of the tokens is expired
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since the we don't honor the expiry of either token
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since the we don't honor the expiry of either token
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderTrue_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(NotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have expired tokens and we do honor them. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have an expired id token and we do honor both. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but both tokens are expired and we honor the id token. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have an expired id token and we do honor the id token. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_shortIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have an expired access token and we do honor the access token. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, both tokens are still good
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, the access token is expired, but we don't honor that, the id_token is not expired
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, both tokens are still good
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryTrue_longIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have an expired access token and we do honor the access token. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, the id token is expired, but we don't honor that, the access token is still good.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, both tokens are expired, but we don't honor either.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, the id token is expired, but we don't honor that, the access token is still valid.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_shortIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenShortLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, but we have an expired access token and we do honor the access token. We'll re-authenticate
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryTrue_shortAccessTokenLifetime() throws Exception {

        genericReAuthn(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, neither token is expired, the access token is still valid.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryTrue_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, the access token is expired, but we don't honor it, the access token is still valid.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryFalse_shortAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenShortLifetime);

    }

    /**
     * Expect to land on the app again since notifyProvider is false, neither token is expired, the access token is still valid.
     * We'll use the original token
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_notifyProviderFalse_idTokenExpiryFalse_longIdTokenLifetime_accessTokenExpiryFalse_longAccessTokenLifetime() throws Exception {

        genericGoodEndToEndReAccessTest(DoNotNotifyProvider, IDTokenDoNotHonorExpiry, AccessTokenDoNotHonorExpiry, IDTokenLongLifetime, AccessTokenLongLifetime);

    }

    /**
     * Show that when we have end_session from discovery and notify is true, we'll use end_session and perform the logout
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierTrue() throws Exception {

        genericGoodLogoutTest("GoodRedirectNotifyProviderTrueLogoutServlet", "OP3");

    }

    /**
     * Show that when we have end_session from discovery and notify is true, we'll use end_session and perform the logout
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_badRedirectUri_NotifyProvierTrue() throws Exception {

        genericGoodLogoutTest("BadRedirectNotifyProviderTrueLogoutServlet", "OP3");

    }

    /**
     * Show that when we have end_session from discovery and notify is true, we'll use end_session and perform the logout
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_EmptyRedirectUri_NotifyProvierTrue() throws Exception {

        genericGoodLogoutTest("EmptyRedirectNotifyProviderTrueLogoutServlet", "OP3");

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to the valid end_session value
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierFalse_PromptLogin() throws Exception {

        genericGoodLogoutTest("GoodRedirectNotifyProviderFalsePromptLoginLogoutServlet", "OP3");

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to the valid end_session value
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception", "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException" })
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierFalse_PromptNone() throws Exception {

        String provider = "OP3";

        String appName = "GoodRedirectNotifyProviderFalsePromptNoneLogoutServlet";
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        actions.invokeUrlWithBasicAuth(_testName, webClient, url, Constants.TESTUSER, Constants.TESTUSERPWD);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(sleepTimeInSeconds);

        invokeAppReturnLogoutPage(webClient, url);

        // even though we validated that we landed on the logout successful page,
        // make sure that we need to log in again.
        // since login page cannot be presented with prompt=none,
        // just check that invoking the app returns the login_required error
        Page response = invokeApp(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER + ".*\\[login_required]", "Did not receive an error message stating that the callback url includes the [login_required] error param."));
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to the valid end_session value
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierFalse_PromptConsent() throws Exception {

        genericGoodLogoutTest("GoodRedirectNotifyProviderFalsePromptConsentLogoutServlet", "OP3");

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to the valid end_session value
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierFalse_PromptSelectAccount() throws Exception {

        genericGoodLogoutTest("GoodRedirectNotifyProviderFalsePromptSelectAccountLogoutServlet", "OP3");

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to the valid end_session value
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_goodRedirectUri_NotifyProvierFalse_PromptEmpty() throws Exception {

        genericGoodLogoutTest("GoodRedirectNotifyProviderFalsePromptEmptyLogoutServlet", "OP3");

    }

    /**
     * Show that when notify is false and we have the logout redirectURI, we'll use that redirectURI
     * This test has that redirect set to a bad value - just the Liberty splash page
     *
     * @throws Exception
     */
    @Test
    public void BasicLogoutTests_badRedirectUri_NotifyProvierFalse() throws Exception {

        genericGoodSplashPage("BadRedirectNotifyProviderFalseLogoutServlet", "OP3");

    }

    @Test
    public void BasicLogoutTests_emptyRedirectUri_NotifyProvierFalse() throws Exception {

        genericReAuthn(rpServer, "EmptyRedirectNotifyProviderFalseLogoutServlet", "OP3", false);

    }

}

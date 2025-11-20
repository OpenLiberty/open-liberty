/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.tests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Test with apps that injects OpenIdContext and others that do not.  The @OpenIdAuthenticationMechanismDefinition in the apps vary the settings of
 * redirectToOriginalResourceExpression and useSessionExpression.  The apps don't have any roles defined, but will call SecurityContext.authenticate.
 * The test cases will pass in a flag to indicate how newAuthentication should be used/passed.  It'll be omitted, set to true or false.
 * The test cases will validate that we apps record the proper request/openIdContext/WSSubject content and the proper calls to the app and app callback.
 *
 */

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class AuthenticationTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = AuthenticationTests.class;

    protected static ShrinkWrapHelpers swh = null;

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.authentication.jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.authentication.opaque.rp")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    // create repeats for opaque and jwt tokens - in lite mode, only run with jwt tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.JWT_TOKEN_FORMAT);

    private static final String app = "AuthenticationApp";

    public interface ExpressionValues {

    }

    // Attribute states can be true, false or not set (empty) - not set/empty will mean something different for different attributes
    public static enum BooleanPlusValues implements ExpressionValues {
        EMPTY, TRUE, FALSE
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

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

        deployMyApps();

        // Allow error messages that are issued because the app tries to log information that may not be set
        rpServer.addIgnoredErrors(Arrays.asList(MessageConstants.SESN0066E_RSP_COMMITTED_COOKIE_CANNOT_BE_SET, MessageConstants.SRVE8114W_CANNOT_SET_SESSION_COOKIE));

        // Wait to ensure LTPA configuration has completed before starting tests
        opServer.waitForStringInLog("CWWKS4105I");
        rpServer.waitForStringInLog("CWWKS4105I");
        // rspValues used to validate the app output will be initialized before each test - any unique values (other than the
        //  app need to be updated by the test case - the app is updated by the invokeApp* methods)
    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectFalseUseSessionFalseServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectFalseUseSessionFalseServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.FALSE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectFalseUseSessionTrueServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectFalseUseSessionTrueServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.TRUE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectFalseUseSessionEmptyServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectFalseUseSessionEmptyServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectTrueUseSessionFalseServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectTrueUseSessionFalseServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.FALSE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectTrueUseSessionTrueServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectTrueUseSessionTrueServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.TRUE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectTrueUseSessionEmptyServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectTrueUseSessionEmptyServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectEmptyUseSessionFalseServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectEmptyUseSessionFalseServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectEmptyUseSessionTrueServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectEmptyUseSessionTrueServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectEmptyUseSessionEmptyServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectEmptyUseSessionEmptyServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppInjectRedirectTrueUseSessionFalseExpiredTokenServlet.war", "AuthAppInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppInjectRedirectTrueUseSessionFalseExpiredTokenServlet", "OP4", BooleanPlusValues.TRUE,
                                                             BooleanPlusValues.FALSE, true),
                                       "oidc.authAppInjection.servlets",
                                       "oidc.client.base.*");

        // Apps that do not inject OpenIdContext
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectFalseUseSessionFalseServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectFalseUseSessionFalseServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.FALSE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectFalseUseSessionTrueServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectFalseUseSessionTrueServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.TRUE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectFalseUseSessionEmptyServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectFalseUseSessionEmptyServlet", "OP1", BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectTrueUseSessionFalseServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectTrueUseSessionFalseServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.FALSE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectTrueUseSessionTrueServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectTrueUseSessionTrueServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.TRUE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectTrueUseSessionEmptyServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectTrueUseSessionEmptyServlet", "OP1", BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectEmptyUseSessionFalseServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectEmptyUseSessionFalseServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectEmptyUseSessionTrueServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectEmptyUseSessionTrueServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "AuthAppNoInjectRedirectEmptyUseSessionEmptyServlet.war", "AuthAppNoInjectServlet.war",
                                       buildUpdatedConfigMap("AuthAppNoInjectRedirectEmptyUseSessionEmptyServlet", "OP1", BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY),
                                       "oidc.authAppNoInjection.servlets",
                                       "oidc.client.simple.servlets",
                                       "oidc.client.base.*");

    }

    /**
     * Build the map used to create openIdConfig.properties
     *
     * @param appName - the appName that will be invoked and redirected to
     * @param provider - the provider that the test uses (most use OP1)
     * @param redirectToOriginalResource - flag indicating if redirectToOriginalResource should be set to true, false, or not set
     * @param useSession - flag indicating if useSession should be set to true, false, or not set
     * @return - return a map of openIdConfig.properties values to be set
     * @throws Exception
     */
    public static Map<String, Object> buildUpdatedConfigMap(String appName, String provider, BooleanPlusValues redirectToOriginalResource,
                                                            BooleanPlusValues useSession) throws Exception {

        return buildUpdatedConfigMap(appName, provider, redirectToOriginalResource, useSession, false);
    }

    public static Map<String, Object> buildUpdatedConfigMap(String appName, String provider, BooleanPlusValues redirectToOriginalResource,
                                                            BooleanPlusValues useSession, boolean expiredToken) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

        switch (redirectToOriginalResource) {
            case EMPTY:
                break;
            case TRUE:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectToOriginalResourceExpressionTrue());
                break;
            case FALSE:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getRedirectToOriginalResourceExpressionFalse());
                break;
        }

        switch (useSession) {
            case EMPTY:
                break;
            case TRUE:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUseSessionExpressionTrue());
                break;
            case FALSE:
                testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUseSessionExpressionFalse());
                break;
        }

        // for tests with short lived tokens
        if (expiredToken) {
            // tokenAutoRefresh is false by default - don't override it
            // notifyProvider is false by default - don't override it
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getAccessTokenExpiryExpressionTrue());
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getIdentityTokenExpiryExpressionTrue());
        }

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, appName, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    /**
     * The test and callback servlets track how many times they're called/invoked so that the tests can determine if the proper behavior is happening in the server.
     * Since the same apps may be used by multiple tests (and in some cases, the counter is defined in the base app) we need to reset the counter at the beginning
     * of each test and between the first and second invocation of the test app in each test case.
     *
     * @param appRoot - the app whose counters should be rest
     * @throws Exception
     */
    public void resetAuthAppFlags(String appRoot) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        // update the call count in the app
        String appUrl = rpHttpsBase + "/" + appRoot + "/AuthenticationApp";
        actions.invokeUrlWithParameters(_testName, webClient, appUrl, HttpMethod.PUT, null);

        // update the call count in the callback
        String cbUrl = rpHttpsBase + "/" + appRoot + "/Callback";
        actions.invokeUrlWithParameters(_testName, webClient, cbUrl, HttpMethod.PUT, null);

    }

    /**
     * Build the name of the test app that the test will use - the name is bases on he parms passed in
     *
     * @param usesInjection - does the app inject openIdContext (the first part of the app name is based on this)
     * @param redirect - is redirectToOriginalResource true/false/empty (meaning not set)
     * @param useSession - is useSession true/false/empty (meaning not set)
     * @param newAuth - is newAuthentication true/false/empty (meaning not set)
     * @return - returns the built app name
     * @throws Exception
     */
    public String buildAppName(boolean usesInjection, BooleanPlusValues redirect, BooleanPlusValues useSession, boolean expiredToken) throws Exception {

        String appRoot = "AuthApp";
        if (usesInjection) {
            appRoot = appRoot + "Inject";
        } else {
            appRoot = appRoot + "NoInject";
            rspValues.setBaseApp(ServletMessageConstants.ALT_BASE_SERVLET);
        }
        switch (redirect) {
            case EMPTY:
                appRoot = appRoot + "RedirectEmpty";
                break;
            case TRUE:
                appRoot = appRoot + "RedirectTrue";
                break;
            case FALSE:
                appRoot = appRoot + "RedirectFalse";
                break;
        }

        switch (useSession) {
            case EMPTY:
                appRoot = appRoot + "UseSessionEmpty";
                break;
            case TRUE:
                appRoot = appRoot + "UseSessionTrue";
                break;
            case FALSE:
                appRoot = appRoot + "UseSessionFalse";
                break;
        }

        if (expiredToken) {
            appRoot = appRoot + "ExpiredToken";
        }
        appRoot = appRoot + "Servlet";
        Log.info(thisClass, _testName, "AppRoot is: " + appRoot);

        return appRoot;
    }

    /**
     * Invoke the app the first time and validate the response
     *
     * @param webClient - the webClient to use for both attempts to access the protected app
     * @param appRoot - the app to invoke
     * @param requestCount - the number of times we expect to land on the test app
     * @param callbackCount - the number of times we expect to land on the callback (number was set based on the value of the redirect flag)
     * @return - returns the page output
     * @throws Exception
     */
    public Page invokeAppFirstTime(WebClient webClient, String appRoot, int requestCount, int callbackCount) throws Exception {

        // reset the counters in the app and callback - these counters will tell us how many times each was invoked
        resetAuthAppFlags(appRoot); // reset the call counters (counters for number of invocations of the app and the apps callback)

        rspValues.printRspValues();

        Page response = runGoodEndToEndTest(webClient, appRoot, app);

        Expectations extraExpectations1 = new Expectations();
        extraExpectations1 = setServletMessageExpectations(extraExpectations1, requestCount);
        extraExpectations1 = setCallbackMessageExpectations(extraExpectations1, callbackCount);

        validationUtils.validateResult(response, extraExpectations1);

        return response;
    }

    /**
     *
     * All of the tests follow the same flow. Invoke a test app and validate what happens, then invoke the same app again using a different user and validate what happens. Finally,
     * compare the responses for the 2 attempts.
     * The tests vary based on
     * 1) the app that they use - one inject openIdContext and the other does not
     * 2) does the annotation in the app specify redirectToOriginalResource (true/false/default)
     * 3) does the annotation in the app specify useSession (true/false/default)
     * 4) does the securityContext.authenticate call specify newAuthentication (true/false/not specified)
     *
     * We'll invoke the test app and validate that the proper info is logged/returned in the servlet output. We'll also check the log and validate that we landed on the test app
     * and callback the correct number of times
     * Reset the app/callback counters and reset the mark in the server logs and then
     * Invoke the test app again - if we're not specifing newAuth=true, expect to land on the app again without having to log in.
     * if we do specify newAuth=true, expect a failure in the app if we have use_session=true since user1 will try to access the session created by testuser
     *
     * Finally, if we're able to get to the app a second time, compare the response from the first and second invocations.
     *
     */

    public Page genericAuthTest(boolean usesInjection, BooleanPlusValues redirect, BooleanPlusValues useSession, BooleanPlusValues newAuth) throws Exception {
        return genericAuthTest(usesInjection, redirect, useSession, newAuth, false);
    }

    public Page genericAuthTest(boolean usesInjection, BooleanPlusValues redirect, BooleanPlusValues useSession, BooleanPlusValues newAuth, boolean expiredToken) throws Exception {

        rspValues.setRPServer(rpServer);
        // the initial invocation of the apps should hit the app, then the callback and then the app again
        int requestCount1 = 2;
        int callbackCount1 = 1;

        int requestCount2 = 1;
        int callbackCount2 = 0;

        String appRoot = buildAppName(usesInjection, redirect, useSession, expiredToken);

        // setup newAuth parm and adjust counters for the second app invocation based on setting newAuth to true
        if (newAuth != BooleanPlusValues.EMPTY) {
            List<NameValuePair> parms = new ArrayList<NameValuePair>();
            if (newAuth == BooleanPlusValues.TRUE) {
                Log.info(thisClass, _testName, "Saving parm useNewAuth to true");
                parms.add(new NameValuePair("useNewAuth", "true"));
                requestCount2 = 2;
                callbackCount2 = 1;
            } else {
                Log.info(thisClass, _testName, "Saving parm useNewAuth to false");
                parms.add(new NameValuePair("useNewAuth", "false"));
            }
            rspValues.setParms(parms);
        }

        // If we're redirecting automatically, we won't invoke the callback, so set the counts for that to 0
        if (redirect == BooleanPlusValues.TRUE) {
            callbackCount1 = 0;
            callbackCount2 = 0;
        }

        // set a flag to tell some of the expectation builds to skip/alter some checks
        rspValues.setUseAuthApp(true);

        WebClient webClient = getAndSaveWebClient();
        // invoke the app for the first time and validate the response
        Page response1 = invokeAppFirstTime(webClient, appRoot, requestCount1, callbackCount1);

        // set the mark in the logs so that we find information for the second request (not the first)
        rpServer.setMarkToEndOfLog(); // reset the marks in the log so we are only looking at the output from the second call to the app

        // reset the counters in the test apps and callback
        resetAuthAppFlags(appRoot); // reset the call counters (counters for number of invocations of the app and the apps callback)
        // invoke the app again, but use the context from the first call (we shouldn't need to re-authenticate, but, we're specifically invoking authenticate and will validate the proper behavior base on the annotation setting and the flag passed to authenticate

        String url = rpHttpsBase + "/" + appRoot + "/AuthenticationApp";

        Page response2 = null;
        // special case where we're testing with a token that expires between the first and second call
        if (expiredToken) {
            // sleep long enough to let the token expire
            actions.testLogAndSleep(25);
            // token should have expired - expect a login prompt
            response2 = invokeAppReturnLoginPage(webClient, url);
        } else {

            boolean extraChecks = true;
            // if we're going to pass newAuthentication = true, we'll have to handle different behavior
            // if we don't, we'll get to the app without having to log in again
            if (newAuth == BooleanPlusValues.TRUE) {
                rspValues.setSubject("user1");
                // if we're using the same session, the app will hit an UnauthorizedSessionRequestException exception because the session was created by testuser, but now user1 is trying to use it
                if (useSession != BooleanPlusValues.FALSE & (!(!usesInjection && redirect == BooleanPlusValues.TRUE))) {
                    extraChecks = false;
                    response2 = invokeAppReturnLoginPage(webClient, url);
                    response2 = actions.doFormLogin(response2, "user1", "user1pwd");
                    // confirm protected resource was accessed
                    validationUtils.validateResult(response2, setUnauthorizedSessionRequestExpectations());
                } else {
                    // expect to login again and get different user info - no issues with conflicting sessions since we're not using session
                    response2 = runGoodEndToEndTest(webClient, appRoot, app, "user1", "user1pwd");
                }
            } else {
                // invoke the app and expect to land on it without having to log in
                response2 = invokeApp(webClient, url, getProcessLoginExpectations(app));
            }

            // Extra checks for the second app invocation as well as comparison of results from the first and second call
            // Skipped if we expect an UnauthorizedSessionRequestException exception
            if (extraChecks) {
                Expectations extraExpectations2 = new Expectations();
                extraExpectations2 = setServletMessageExpectations(extraExpectations2, requestCount2);
                extraExpectations2 = setCallbackMessageExpectations(extraExpectations2, callbackCount2);

                validationUtils.validateResult(response2, extraExpectations2);

                compareOpenIdContext(response1, response2, usesInjection);
            }
        }
        return response2;
    }

    /**
     * Compare the OpenIdContext reference between the first and second app invocations. The ref should be the same, but the content should be different. (the content is validated
     * as part of the normal test flow)
     *
     * @param response1 - the response from the first call to the test app
     * @param response2 - the response from the first call to the test app
     * @param usesInjection - flag indicating if injection is used - if it is NOT, there should be no openidContext
     * @throws Exception
     */
    public void compareOpenIdContext(Page response1, Page response2, boolean usesInjection) throws Exception {

        String thisMethod = "compareOpenIdContext";

        if (usesInjection) {
            String context1 = AutomationTools.getTokenFromResponse(response1,
                                                                   ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                              + ServletMessageConstants.OPENID_CONTEXT);
            String context2 = AutomationTools.getTokenFromResponse(response2,
                                                                   ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                              + ServletMessageConstants.OPENID_CONTEXT);
            Log.info(thisClass, thisMethod, context1);
            Log.info(thisClass, thisMethod, context2);

            if (context1.equals(context2)) {
                // context content is check as part of the call to runGoodEndToEndTest
                Log.info(thisClass, thisMethod, "openIdContext's are the same - this is expected - content should be different, but the same context should be used.");
            } else {
                fail("openIdContext's are the NOT same - this is NOT expected");
            }

        }

    }

    /**
     * Create expectations for failures when user1 tries to use a session created by testuser. The test app will return a status code of 500 as it hits an exception thrown by the
     * runtime.
     * We'll check the status code, messages in the response as well as messages in the log.
     *
     * @return - UnauthorizedSessionRequestException expectations
     * @throws Exception
     */
    public Expectations setUnauthorizedSessionRequestExpectations() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(Constants.INTERNAL_SERVER_ERROR_STATUS));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, "Internal Server Error", "Did not receive the Internal ServerError message."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.SESN0008E_SESSION_OWNED_BY_OTHER_USER, "Did not receive the UnauthorizedSessionRequestException message."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SESN0008E_SESSION_OWNED_BY_OTHER_USER
                                                                           + ".*user1.*testuser", "Did not find a message stating that user1 attempted to access a session owned by testuser"));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SRVE0777E_SECURITY_CHECK, "Did not find a message stating that the application encountered a security check error"));

        return expectations;
    }

    /**
     * Create expectations for checking that we landed on the test app the correct number of times.
     *
     * @param expectations - expectations to add to
     * @param requestCount - the number of times that we're expecting to land on the test app
     * @return - updated expectations
     * @throws Exception
     */
    public Expectations setServletMessageExpectations(Expectations expectations, int requestCount) throws Exception {

        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.APP_REQUEST_COUNT
                                                                                           + Integer.toString(requestCount), "Did not find message stating that we landed on the app "
                                                                                                                             + Integer.toString(requestCount)
                                                                                                                             + " time(s)"));

        return expectations;
    }

    /**
     * Create expectations for checking that we landed on the callback the correct number of times.
     *
     * @param expectations - expectations to add to
     * @param requestCount - the number of times that we're expecting to land on the callback
     * @return - updated expectations
     * @throws Exception
     */
    public Expectations setCallbackMessageExpectations(Expectations expectations, int callbackCount) throws Exception {

        if (callbackCount == 0) {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, Constants.STRING_DOES_NOT_CONTAIN, ServletMessageConstants.CALLBACK_REQUEST_COUNT, "Found a call to the callback and should not have."));
        } else {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, ServletMessageConstants.CALLBACK_REQUEST_COUNT
                                                                               + Integer.toString(callbackCount), "Did not find message stating that we landed on the callback "
                                                                                                                  + Integer.toString(callbackCount) + " time(s)"));
        }

        return expectations;
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/
    /**
     * Test with app that injects OpenIdContext.
     */

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times. That last time should result in an
     * UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it.
     *
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times. That last time should result in an
     * UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times and on the callback 1 time.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 2 time and on the callback 1 time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (and defaults to false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it (useSession defaults to true).
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    /**
     * Test with app that injects OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we get the login page since the token passed has expired (expiry checking is true for access and id
     * tokens)
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthFalse_ExpiredToken() throws Exception {

        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/OP4");
        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, true);

    }

    //-----------------------------------------------------------------------/
    /**
     * Test with apps that DO NOT inject OpenIdContext.
     */
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and that we DO NOT use the callback because redirect
     * is set to true. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 2 times and on the callback one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it (useSession defaults to true).
     * OpenIdContext is not available.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set to false. On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it (useSession defaults to true).
     * OpenIdContext is not available.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (default is false). On the second request to the test app, make sure that we have to log in and that we land on the app 2 times and on the callback one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (default is false). On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running
     * the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it (useSession defaults to true).
     * OpenIdContext is not available.
     *
     */
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is set not set (default is false). On the second request to the test app, make sure that we do not have to log in and that we only land on the app one time.
     * OpenIdContext is not available.
     *
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    /**
     * Test with app that DOES NOT inject OpenIdContext.
     * Invoke the test app that uses injection of openIdContext - make sure that on the first call, we land on the app 2 times and use the callback one time because redirect
     * is not set (default is false). On the second request to the test app, make sure that we have to log in and that we land on the app 1 time and on the callback 1 time. Running
     * the callback
     * should result in an UnauthorizedSessionRequestException exception since the session was created by testuser and user1 is trying to access it (useSession defaults to true).
     * OpenIdContext is not available.
     *
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

}

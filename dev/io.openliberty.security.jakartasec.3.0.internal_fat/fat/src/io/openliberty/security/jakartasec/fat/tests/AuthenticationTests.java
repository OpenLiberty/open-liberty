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

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.JWT_TOKEN_FORMAT);
//  public static RepeatTests repeat = createTokenTypeRepeats();

    private static final String app = "AuthenticationApp";

    public interface ExpressionValues {

    }

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

    public static Map<String, Object> buildUpdatedConfigMap(String appName, String provider, BooleanPlusValues redirectToOriginalResource,
                                                            BooleanPlusValues useSession) throws Exception {

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

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, appName, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    public void resetAuthAppFlags(String appRoot) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        // update the call count in the app
        String appUrl = rpHttpsBase + "/" + appRoot + "/AuthenticationApp";
        actions.invokeUrlWithParameters(_testName, webClient, appUrl, HttpMethod.PUT, null);

        // update the call count in the callback
        String cbUrl = rpHttpsBase + "/" + appRoot + "/Callback";
        actions.invokeUrlWithParameters(_testName, webClient, cbUrl, HttpMethod.PUT, null);

    }

    public String buildAppName(boolean usesInjection, BooleanPlusValues redirect, BooleanPlusValues useSession, BooleanPlusValues newAuth) throws Exception {

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
     * All of the tests follow the same flow. Invoke a test app and validate what happens, then invoke the same app again using a different user and validate what happens. Finally,
     * compare the responses for the 2 attempts.
     * The test vary in
     * 1) the app that they use - one inject openIdContext and the other does not
     * 2) does the annotation in the app specify redirectToOriginalResource (true/false/default)
     * 3) does the annotation in the app specify useSession (true/false/default)
     * 4) does the securityContext.authenticate call specify newAuthentication (true/false/not specified)
     *
     * Test with app that injects OpenIdContext.
     * Invoke the test app and see:
     ** We should land on the test app
     ** Process a login
     ** Print Basically unset request, openIdContext and WSSubject values
     ** See the callback invoked and have it print the appropriate values for the request, openIdContext and WSSubject
     ** See that we land on the test app again and that it logs the appropriate values for the request, openIdContext and WSSubject
     * Test case validates the proper response values including a count of having landed on the app 2 times and the callback once
     * Invoke the test app again and we should see:
     ** We should land on the test app
     ** Process a login
     ** Print Basically unset request, openIdContext and WSSubject values
     ** See the callback invoked and have it print the appropriate values for the request, openIdContext and WSSubject
     ** See that we land on the test app again and that it logs the appropriate values for the request, openIdContext and WSSubject
     ** Test case validates the proper response values including a count of having landed on the app 2 times and the callback once
     *
     */

    public Page genericAuthTest(boolean usesInjection, BooleanPlusValues redirect, BooleanPlusValues useSession, BooleanPlusValues newAuth) throws Exception {

        rspValues.setRPServer(rpServer);
        // the initial invocation of the apps should hit the app, then the callback and then the app again
        int requestCount1 = 2;
        int callbackCount1 = 1;

        int requestCount2 = 1;
        int callbackCount2 = 0;

        String appRoot = buildAppName(usesInjection, redirect, useSession, newAuth);

        // setup newAuth parm and adjust
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

        if (redirect == BooleanPlusValues.TRUE) {
            callbackCount1 = 0;
            callbackCount2 = 0;
        }

        rspValues.setUseAuthApp(true);

        WebClient webClient = getAndSaveWebClient();
        Page response1 = invokeAppFirstTime(webClient, appRoot, requestCount1, callbackCount1);

        rpServer.setMarkToEndOfLog(); // reset the marks in the log so we are only looking at the output from the second call to the app

        resetAuthAppFlags(appRoot); // reset the call counters (counters for number of invocations of the app and the apps callback)
        // invoke the app again, but use the context from the first call (we shouldn't need to re-authenticate, but, we're specifically invoking authenticate and will validate the proper behavior base on the annotation setting and the flag passed to authenticate

        Page response2 = null;
        boolean extraChecks = true;
        // if we're going to pass newAuthentication = true, we'll have to handle different behavior
        // if we don't, we'll get to the app without having to log in again
        if (newAuth == BooleanPlusValues.TRUE) {
            rspValues.setSubject("user1");
            // if we're using the same session, the app will hit an UnauthorizedSessionRequestException exception because the session was created by testuser, but now user1 is trying to use it
            if (useSession != BooleanPlusValues.FALSE & (!(!usesInjection && redirect == BooleanPlusValues.TRUE))) {
                extraChecks = false;
                String url = rpHttpsBase + "/" + appRoot + "/" + app;
                Page response = invokeAppReturnLoginPage(webClient, url);
                Expectations currentExpectations = new Expectations();
                currentExpectations.addExpectation(new ResponseStatusExpectation(Constants.INTERNAL_SERVER_ERROR_STATUS));
                currentExpectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, "Internal Server Error", "Did not receive the Internal ServerError message."));
                currentExpectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.SESN0008E_SESSION_OWNED_BY_OTHER_USER, "Did not receive the UnauthorizedSessionRequestException message."));
                currentExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SESN0008E_SESSION_OWNED_BY_OTHER_USER
                                                                                          + ".*user1.*testuser", "Did not find a message stating that user1 attempted to access a session owned by testuser"));
                currentExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.SRVE0777E_SECURITY_CHECK, "Did not find a message stating that the application encountered a security check error"));

                response = actions.doFormLogin(response, "user1", "user1pwd");
                // confirm protected resource was accessed
                validationUtils.validateResult(response, currentExpectations);

            } else {
                // expect to login again and get different user info - no issues with conflicting sessions since we're not using session
                response2 = runGoodEndToEndTest(webClient, appRoot, app, "user1", "user1pwd");
            }
        } else {
            // invoke the app and expect to land on it without having to log in
            String url = rpHttpsBase + "/" + appRoot + "/AuthenticationApp";
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

    public Expectations setServletMessageExpectations(Expectations expectations, int requestCount) throws Exception {

        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.APP_REQUEST_COUNT
                                                                                           + Integer.toString(requestCount), "Did not find message stating that we landed on the app "
                                                                                                                             + Integer.toString(requestCount)
                                                                                                                             + " time(s)"));

        return expectations;
    }

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
     * Invoke the test app and see:
     ** We should land on the test app
     ** Process a login
     ** Print Basically unset request, openIdContext and WSSubject values
     ** See the callback invoked and have it print the appropriate values for the request, openIdContext and WSSubject
     ** See that we land on the test app again and that it logs the appropriate values for the request, openIdContext and WSSubject
     * Test case validates the proper response values including a count of having landed on the app 2 times and the callback once
     * Invoke the test app again and we should see:
     ** We should land on the test app
     ** Process a login
     ** Print Basically unset request, openIdContext and WSSubject values
     ** See the callback invoked and have it print the appropriate values for the request, openIdContext and WSSubject
     ** See that we land on the test app again and that it logs the appropriate values for the request, openIdContext and WSSubject
     ** Test case validates the proper response values including a count of having landed on the app 2 times and the callback once
     *
     */
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceTrueUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceFalseUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_injectOpenIdContext_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(true, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //-----------------------------------------------------------------------/
    /**
     * Test with apps that injects OpenIdContext.
     */
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceTrueUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceFalseUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

    //---------------------/
    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.FALSE);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionFalse_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionTrue_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE, BooleanPlusValues.TRUE);

    }

    //---------------------/
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthNotSet() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY);

    }

    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthFalse() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.FALSE);

    }

    @ExpectedFFDC({ "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    @Test
    public void AuthenticationTests_noOpenIdContextInjection_redirectToOriginalResourceEmptyUseSessionEmpty_newAuthTrue() throws Exception {

        genericAuthTest(false, BooleanPlusValues.EMPTY, BooleanPlusValues.EMPTY, BooleanPlusValues.TRUE);

    }

}

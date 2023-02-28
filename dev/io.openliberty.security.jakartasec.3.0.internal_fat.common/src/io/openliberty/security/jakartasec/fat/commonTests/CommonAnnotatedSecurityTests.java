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
package io.openliberty.security.jakartasec.fat.commonTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringJoiner;

import org.junit.Before;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.utils.CommonExpectations;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ResponseValues;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletRequestExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

public class CommonAnnotatedSecurityTests extends CommonSecurityFat {

    protected static Class<?> thisClass = CommonAnnotatedSecurityTests.class;

    protected final TestActions actions = new TestActions();
    protected final TestValidationUtils validationUtils = new TestValidationUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    protected CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();

    protected static String opHttpBase = null;
    protected static String opHttpsBase = null;
    protected static String rpHttpBase = null;
    protected static String rpHttpsBase = null;

    protected static final String withOidcClientConfig = "withOidcClientConfig";
    protected static final String useRedirectToOriginalResource = "useRedirectToOriginalResource";
    protected static final String useCallbacks = "useCallbacks";

    protected ResponseValues rspValues;

    public static class skipIfUseRedirectToOriginalResource extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (RepeatTestFilter.getRepeatActionsAsString().contains(useRedirectToOriginalResource)) {
                Log.info(thisClass, "skipIfUseRedirectToOriginalResource",
                         "Test case is using useRedirectToOriginalResource - App annotation of this test does not contain that setting - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfUseRedirectToOriginalResource",
                     "Test case is NOT using useRedirectToOriginalResource - App annotation of this test contains that setting - run test");
            return false;
        }
    }

    public static RepeatTests createRandomTokenTypeRepeats() {

        String accessTokenType = Utils.getRandomSelection(Constants.JWT_TOKEN_FORMAT, Constants.OPAQUE_TOKEN_FORMAT);
        return createTokenTypeRepeat(accessTokenType);
    }

    public static RepeatTests createTokenTypeRepeat(String accessTokenType) {

        Log.info(thisClass, "createRepeats", "Will be running tests using a/an " + accessTokenType + " access_token");

        RepeatTests rTests = addRepeat(null, new SecurityTestRepeatAction(accessTokenType));

        return rTests;

    }

    protected static RepeatTests createTokenTypeRepeats() {
        return createTokenTypeRepeats(null);

    }

    /**
     * Creates repeats for a calling test class. The caller passes in a unique string that will be used in the repeat name. This method will create 2 repeats
     * <uniqueString>_jwt and
     * <uniqueString>_opaque
     *
     * @param specialCase
     * @return
     */
    protected static RepeatTests createTokenTypeRepeats(String specialCase) {

        RepeatTests rTests = null;
        return createTokenTypeRepeats(rTests, specialCase);
    }

    protected static RepeatTests createTokenTypeRepeats(RepeatTests rTests, String specialCase) {
        String additionalString = "";
//        List<String> repeatTokenTypes = Arrays.asList(Constants.JWT_TOKEN_FORMAT);
        List<String> repeatTokenTypes = Arrays.asList(Constants.JWT_TOKEN_FORMAT, Constants.OPAQUE_TOKEN_FORMAT);

        if (specialCase != null) {
            additionalString = specialCase + "_";
        }
        for (String tokenType : repeatTokenTypes) {
            rTests = addRepeat(rTests, new SecurityTestRepeatAction(additionalString + tokenType));
        }
        return rTests;
    }

    protected static RepeatTests createMultipleTokenTypeRepeats(String... extraCases) {

        RepeatTests rTests = null;

        for (String aCase : extraCases) {
            rTests = createTokenTypeRepeats(rTests, aCase);
        }

        return rTests;
    }

    public static RepeatTests addRepeat(RepeatTests rTests, SecurityTestRepeatAction currentRepeat) {
        if (rTests == null) {
            return RepeatTests.with(currentRepeat);
        } else {
            return rTests.andWith(currentRepeat);
        }
    }

    public static Class<?> getMyClassName() {

        return thisClass;
    }

    public static void updateTrackers(LibertyServer opServer, LibertyServer rpServer, boolean serversAreReconfigured) throws Exception {

        // track the servers that we start so that they'll be cleaned up at the end of this classes execution, or if the tests fail out
        serverTracker.addServer(opServer);
        serverTracker.addServer(rpServer);
        if (!serversAreReconfigured) {
            // at the moment, none of the tests reconfigure the servers, so skip restoring them at the end of each test case.
            skipRestoreServerTracker.addServer(opServer);
            skipRestoreServerTracker.addServer(rpServer);
        }

    }

    public static void setTokenTypeInBootstrap(LibertyServer server) throws Exception {

        setTokenTypeInBootstrap(server, null, null);
    }

    public static LibertyServer setTokenTypeInBootstrap(LibertyServer opServer, LibertyServer rpJwtServer, LibertyServer rpOpaqueServer) throws Exception {

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.JWT_TOKEN_FORMAT)) {
            bootstrapUtils.writeBootstrapProperty(opServer, "opTokenFormat", "jwt");
            return rpJwtServer;
        } else {
            bootstrapUtils.writeBootstrapProperty(opServer, "opTokenFormat", "opaque");
            return rpOpaqueServer;
        }
        // not testing with mpJwt at the moment
    }

    public Expectations getGotToTheAppExpectations(String app, String url) throws Exception {
        Expectations expectations = getGotToTheAppExpectations(null, app, url);
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                 + "BaseServlet", "Did not land on the unprotected app."));
        return expectations;
    }

    public Expectations getGotToTheAppExpectations(String currentAction, String app, String url) throws Exception {
        Expectations expectations = CommonExpectations.successfullyReachedUrl(currentAction, url);
        expectations.addExpectation(new ResponseFullExpectation(currentAction, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                          + app, "Did not land on the unprotected app."));
        return expectations;

    }

    public void initResponseValues() throws Exception {

        rspValues = new ResponseValues();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/OP1");

    }

    @Override
    @Before
    public void commonBeforeTest() {
        super.commonBeforeTest();
        try {
            initResponseValues();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Page invokeApp(WebClient webClient, String url, Expectations expectations) throws Exception {

        Page response = null;
        if (!(rspValues.getOriginalRequest() != null && rspValues.getOriginalRequest().contains(ServletMessageConstants.UNAUTH_SESSION_REQUEST_EXCEPTION))) {
            rspValues.setOriginalRequest(url);
        }

        // the call to invokeUrlWithParametersAndHeaders mangles the headers, so make a copy
        HashMap<String, String> tempHeaders = null;
        if (rspValues.getHeaders() != null) {
            tempHeaders = new HashMap<String, String>(rspValues.getHeaders());
        }

        List<Cookie> cookies = rspValues.getCookies();
        if (cookies != null && cookies.size() != 0) {
            for (Cookie c : cookies) {
                webClient.addCookie(c.getName() + "=" + c.getValue(), new URL("https://localhost"), null);
            }

        }

        response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, url, rspValues.getParms(), tempHeaders);

        loggingUtils.printAllCookies(webClient);

        validationUtils.validateResult(response, expectations);

        return response;
    }

    /**
     * Invoke the requested app - and ensure that we landed on the login page (login.jsp)
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the login page (that the next step will use to actually login)
     * @throws Exception
     */
    public Page invokeAppReturnLoginPage(WebClient webClient, String url) throws Exception {

        return invokeApp(webClient, url, CommonExpectations.successfullyReachedOidcLoginPage());

    }

    /**
     * Invoke the requested app - and ensure that we landed on the logout page - we'll land on this page when we try to use expired tokens
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the logout page
     * @throws Exception
     */
    public Page invokeAppReturnLogoutPage(WebClient webClient, String url) throws Exception {

        return invokeApp(webClient, url, CommonExpectations.successfullyReachedOidcLogoutPage());

    }

    /**
     * Invoke the requested app - and ensure that we landed on the post logout page - we'll land on this page when we try to use expired tokens
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the logout page
     * @throws Exception
     */
    public Page invokeAppReturnPostLogoutPage(WebClient webClient, String url, Map<String, String> extraParms) throws Exception {

        return invokeApp(webClient, url, CommonExpectations.successfullyReachedPostLogoutPage(extraParms));

    }

    /**
     * Invoke the requested app - and ensure that we landed on the test endSession app (with/without a logout redirect, we won't get past the test endSession)
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the logout page
     * @throws Exception
     */
    public Page invokeAppReturnTestEndSessionPage(WebClient webClient, String url, boolean willRedirect) throws Exception {

        return invokeApp(webClient, url, CommonExpectations.successfullyReachedTestEndSessiontPage(rpHttpsBase, willRedirect));

    }

    /**
     * Invoke the requested app - and ensure that we land on the app without having to login again - we'll land on the app when the tokens included in the request are still valid
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the app output page
     * @throws Exception
     */
    public Page invokeAppGetToApp(WebClient webClient, String url) throws Exception {

        return invokeApp(webClient, url, getProcessLoginExpectations(""));

    }

    public Page invokeAppGetToAppWithRefreshedToken(WebClient webClient, String url) throws Exception {

        return invokeApp(webClient, url, getProcessAppAccessWithRefreshedTokenExpectations(""));

    }

    /**
     * Invoke the requested app - and ensure that we land on the Open Liberty splash page - we use this page as a bad attribute setting in the annotation and should land on it at
     * times
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the Open Liberty splash page
     * @throws Exception
     */
    public Page invokeAppGetToSplashPage(WebClient webClient, String url) throws Exception {
        return invokeApp(webClient, url, getSplashPageExpectations());
    }

    /**
     * Wrapper to invoke the requested app - used in cases where we don't want to do a full login flow - caller should validate the
     * response.
     * Use this instead of just actions.invokeUrl when you'll need the originalRequest set properly in rspValues
     *
     * @param webClient
     * @param url
     * @return
     * @throws Exception
     */
    public Page invokeApp(WebClient webClient, String url) throws Exception {

        rspValues.setOriginalRequest(url);
        Page response = actions.invokeUrl(_testName, webClient, url);

        return response;

    }

    public Page processLogin(Page response, String user, String pw, String app) throws Exception {

        Expectations currentExpectations = getProcessLoginExpectations(app);

        response = actions.doFormLogin(response, user, pw);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, currentExpectations);
//        validateTheSameContext(ServletMessageConstants.CALLBACK, response);
        // when simple servlet is used, we won't have an openidContext
        if (!rspValues.getBaseApp().equals(Constants.DEFAULT_SERVLET)) {
            validateTheSameContext(ServletMessageConstants.SERVLET, response);
        }

        return response;
    }

    public Expectations getGeneralAppExpecations(String app) throws Exception {

        Expectations processLoginexpectations = new Expectations();
        processLoginexpectations.addSuccessCodeForCurrentAction();
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "got here servlet", "Did not land on the servlet."));
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, ServletMessageConstants.SERVLET
                                                                                                                     + "OpenIdContext: null", "The context was null and should not have been"));

        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                             + rspValues.getBaseApp(), "Did not land on the test app."));
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                             + app, "Did not land on the test app."));

        return processLoginexpectations;
    }

    public Expectations getProcessLoginExpectations(String app) throws Exception {

        Expectations processLoginexpectations = getGeneralAppExpecations(app);
        //            // check for the correct values from the callback in the server log
//            OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
//            WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
        // check for the correct values from the servlet in the response
        OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        ServletRequestExpectationHelpers.getServletRequestExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);

        return processLoginexpectations;
    }

    public Expectations getProcessAppAccessWithRefreshedTokenExpectations(String app) throws Exception {

        Expectations processLoginexpectations = getGeneralAppExpecations(app);
        //            // check for the correct values from the callback in the server log
//            OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
//            WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
        // check for the correct values from the servlet in the response
        OpenIdContextExpectationHelpers.getOpenIdContextFromRefreshedTokenExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        ServletRequestExpectationHelpers.getServletRequestExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);

        return processLoginexpectations;
    }

    public Expectations getSplashPageExpectations() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        //expectations.addExpectation(new ResponseTitleExpectation(null, Constants.STRING_CONTAINS, Constants.OPEN_LIBERTY, "Did not land on the Open Liberty page."));
        expectations.addExpectation(new ResponseTitleExpectation(null, Constants.STRING_CONTAINS, "Liberty", "Did not land on the Open Liberty page."));

        return expectations;
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the
     * standard user/password and does not need the webClient
     * afterwards
     *
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(String appRoot, String app) throws Exception {
        WebClient webClient = getAndSaveWebClient();
        return runGoodEndToEndTest(webClient, appRoot, app);
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the
     * standard user/password, but does need the webClient
     * afterwards
     *
     * @param webClient
     *            the webClient to use to process the requests
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app) throws Exception {

        return runGoodEndToEndTest(webClient, appRoot, app, Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to specify the
     * user/password and does need access to the webClient
     * afterwards
     *
     * @param webClient
     *            the webClient to use to process the requests
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @param user
     *            the user to log in as
     * @param pw
     *            the password to use to log in
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app, String user, String pw) throws Exception {

        Log.info(thisClass, _testName, "headers: " + rspValues.getHeaders());

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, user, pw, app);

        return response;
    }

    public static Map<String, Object> buildUpdatedConfigMap(LibertyServer opServer, LibertyServer rpServer, String appName, String configFileName,
                                                            Map<String, Object> overrideConfigSettings) throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();

        if (configFileName != null) {
            String sourceConfigFile = "publish/shared/config/oidcClient/" + configFileName;
            Log.info(thisClass, "buildUpdatedConfigMap", "sourceConfigFile: " + sourceConfigFile);

            File cf = new File(sourceConfigFile);
            InputStream configFile = new FileInputStream(cf);
            if (configFile != null) {
                Log.info(thisClass, "deployConfigurableTestApps", "Loading config from: " + sourceConfigFile);
                Properties config = new Properties();
                config.load(configFile);
                for (Entry<Object, Object> entry : config.entrySet()) {
                    updatedMap.put((String) entry.getKey(), fixConfigValue(opServer, rpServer, appName, entry.getValue()));
                    Log.info(thisClass, "deployConfigurableTestApps", "key: " + entry.getKey() + " updatedValue: " + updatedMap.get(entry.getKey()));
                }
            }
        }
        if (overrideConfigSettings != null) {
            for (Entry<String, Object> entry : overrideConfigSettings.entrySet()) {
                updatedMap.put(entry.getKey(), fixConfigValue(opServer, rpServer, appName, entry.getValue()));
            }
        }

        return updatedMap;
    }

    public static Object fixConfigValue(LibertyServer opServer, LibertyServer rpServer, String appName, Object value) throws Exception {

        Object newValue = null;
        if (value instanceof String) {
            newValue = ((String) value).replace("op_Port_op",
                                                Integer.toString(opServer.getBvtPort())).replace("op_SecurePort_op",
                                                                                                 Integer.toString(opServer.getBvtSecurePort())).replace("rp_Port_rp",
                                                                                                                                                        Integer.toString(rpServer.getBvtPort())).replace("rp_SecurePort_rp",
                                                                                                                                                                                                         Integer.toString(rpServer.getBvtSecurePort())).replace("rp_AppName_rp",
                                                                                                                                                                                                                                                                appName);

        }
        if (value instanceof PromptType) {
            newValue = ((PromptType) value).toString();
        }
        if (value instanceof PromptType[]) {
            StringJoiner joiner = new StringJoiner(",");
            for (PromptType promptType : (PromptType[]) value) {
                joiner.add(promptType.toString());
            }
            newValue = joiner.toString();
        }
        if (value instanceof DisplayType) {
            newValue = ((DisplayType) value).toString();
        }
        if (value instanceof Integer) {
            newValue = ((Integer) value).toString();
        }
        return newValue;
    }

    public void validateNotTheSame(String instance, Page response1, Page response2) throws Exception {

        // TODO
//        String nonce1 = AutomationTools.getTokenFromResponse(response1, OpenIdContextExpectationHelpers.buildNonceString(instance));
//        String iat1 = AutomationTools.getTokenFromResponse(response1, OpenIdContextExpectationHelpers.buildIssuedAtTimeString(instance));
//        String nonce2 = AutomationTools.getTokenFromResponse(response2, OpenIdContextExpectationHelpers.buildNonceString(instance));
//        String iat2 = AutomationTools.getTokenFromResponse(response2, OpenIdContextExpectationHelpers.buildIssuedAtTimeString(instance));
//
//        Log.info(thisClass, "validateNotTheSame", "Nonces are: " + nonce1 + " and " + nonce2);
//        if (nonce1 == null || nonce2 == null) {
//            fail("Nonce value was null and should not have been");
//        }
//
//        if (!nonce1.equals(nonce2)) {
//            fail("The nonce was the same for two different application invocations and should NOT have been");
//        }
//
//        Log.info(thisClass, "validateNotTheSame", "iats are: " + iat1 + " and " + iat2);
//        if (iat1 == null || iat2 == null) {
//            fail("iat (issued at time) value was null and should not have been");
//        }
//
//        if (!iat1.equals(iat2)) {
//            fail("The iat was the same for two different application invocations and should NOT have been");
//        }

    }

    public void validateTheSameContext(String instance, Page response) throws Exception {

        String context = AutomationTools.getTokenFromResponse(response, OpenIdContextExpectationHelpers.buildContextString(instance));

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "Private Credential: "
                                                                                                 + context, "Did not find the same OpenIdContext for both the WSSubject and the injected OpenIdContext."));

        validationUtils.validateResult(response, expectations);
    }

    /**
     * Compare access_tokens, id_tokens and refresh_tokens between 2 responses. Can be used to validate that we've invoked refresh properly, or have had to login again
     *
     * @param response1 - the output from the first request
     * @param response2 - the output from teh second request
     * @return - true if the responses are different, false if they're the same
     * @throws Exception
     */
    public boolean tokensAreDifferent(Page response1, Page response2, boolean provderAllowsRefresh, boolean tokenWasRefreshed) throws Exception {

        boolean differentToken = accessTokensAreDifferent(response1, response2);
        if (tokenWasRefreshed) {
            differentToken = differentToken && idTokensAreDifferent(response1, response2);
        }
        if (provderAllowsRefresh) {
            differentToken = differentToken && refreshTokensAreDifferent(response1, response2);
        }

        return differentToken;
    }

    public boolean tokensAreTheSame(Page response1, Page response2, boolean provderAllowsRefresh) throws Exception {

        boolean tokensAreTheSame = !accessTokensAreDifferent(response1, response2) && !idTokensAreDifferent(response1, response2);
        if (provderAllowsRefresh) {
            return tokensAreTheSame && !refreshTokensAreDifferent(response1, response2);
        }
        return tokensAreTheSame;
    }

    public boolean tokensAreTheSameIdTokenNull(Page response1, Page response2, boolean provderAllowsRefresh) throws Exception {

        boolean tokensAreTheSame = !accessTokensAreDifferent(response1, response2) && idTokensAreDifferent(response1, response2);
        if (provderAllowsRefresh) {
            return tokensAreTheSame && !refreshTokensAreDifferent(response1, response2);
        }
        return tokensAreTheSame;
    }

    public boolean accessTokensAreDifferent(Page response1, Page response2) throws Exception {

        String first_access_token = AutomationTools.getTokenFromResponse(response1,
                                                                         ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                    + ServletMessageConstants.ACCESS_TOKEN);

        String second_access_token = AutomationTools.getTokenFromResponse(response2,
                                                                          ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                     + ServletMessageConstants.ACCESS_TOKEN);

        if (first_access_token.equals(second_access_token)) {
            Log.info(thisClass, _testName, "Both access_tokens were the same");
            return false;
        } else {
            Log.info(thisClass, _testName, "The access_tokens were different");
            return true;
        }

    }

    public boolean idTokensAreDifferent(Page response1, Page response2) throws Exception {

        String first_id_token = AutomationTools.getTokenFromResponse(response1,
                                                                     ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                + ServletMessageConstants.ID_TOKEN);

        String second_id_token = AutomationTools.getTokenFromResponse(response2,
                                                                      ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                 + ServletMessageConstants.ID_TOKEN);
        if (first_id_token == null || first_id_token.equals("null")) {
            return true; // will only have a null first id_token in the case of multiple refreshes
        }
        if (first_id_token.equals(second_id_token)) {
            Log.info(thisClass, _testName, "Both id_tokens were the same");
            return false;
        } else {
            Log.info(thisClass, _testName, "The id_tokens were different");
            return true;
        }

    }

    public boolean refreshTokensAreDifferent(Page response1, Page response2) throws Exception {

        String first_refresh_token = AutomationTools.getTokenFromResponse(response1,
                                                                          ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                     + ServletMessageConstants.REFRESH_TOKEN);

        String second_refresh_token = AutomationTools.getTokenFromResponse(response2,
                                                                           ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT
                                                                                      + ServletMessageConstants.REFRESH_TOKEN);

        if (first_refresh_token.equals(second_refresh_token)) {
            Log.info(thisClass, _testName, "Both refresh_tokens were the same");
            return false;
        } else {
            Log.info(thisClass, _testName, "The refresh_tokens were different");
            return true;
        }

    }

}

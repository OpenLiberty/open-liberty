/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * Test class for Mangling JWT token test tooling
 *
 * @author chrisc
 *
 */
public class MangleJWTTestTools extends JwtCommonTest {

    private static final Class<?> thisClass = MangleJWTTestTools.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static MultiProviderUtils mpUtils = new MultiProviderUtils();
    protected static String targetProvider = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static final String badString = "someBadValue";
    protected static final String someString = "someString";

    /**
     * Create expectations for a good test flow - add items to check in the output from snoop
     *
     * @param testCase
     *            - the test case name
     * @param settings
     *            - the current test settings
     * @return - newly created good expectations
     * @throws Exception
     */
    protected List<validationData> addRSProtectedAppExpectations(String testCase, TestSettings settings) throws Exception {
        return addRSProtectedAppExpectations(null, testCase, settings);
    }

    /**
     * Create/update expectations for a good test flow - add items to check in the output from snoop
     *
     * @param expectations
     *            - expectations to add to if already set
     * @param testCase
     *            - the test case name
     * @param settings
     *            - the current test settings
     * @return - newly created good expectations
     * @throws Exception
     */
    protected List<validationData> addRSProtectedAppExpectations(List<validationData> expectations, String testCase, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the SnoopServlet", null, "SnoopServlet");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, Constants.APP_TITLE);

        return expectations;

    }

    /**
     * Adds expectations for the JWT consumer client using a built JWT. Verifies
     * - We got to the right JWT consumer client URL
     * - The correct JwtConsumer object was created
     * - JWT created by the JwtConsumer is valid and contains the expected information
     *
     * @param expectations
     * @param testCase
     * @param settings
     * @return
     * @throws Exception
     */
    protected List<validationData> addJwtConsumerExpectations(List<validationData> expectations, String testCase, TestSettings settings) throws Exception {
        if (expectations == null) {
            expectations = vData.addSuccessStatusCodesForActions(Constants.CONSUMER_TEST_ACTIONS);
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not get to the JWT consumer client servlet.", null, Constants.JWT_CONSUMER_ENDPOINT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not enter the JWT consumer client servlet.", null, Constants.JWT_CONSUMER_START_SERVLET_OUTPUT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not instantiate JwtConsumerClient with expected ID [" + settings.getJwtId() + "].", null, Constants.JWT_CONSUMER_SUCCESSFUL_CONSUMER_CREATION + settings.getJwtId());
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Failed general validation of JWT.", null, null);

        expectations = addJwtHeaderExpectation(expectations, Constants.HEADER_ALGORITHM, settings.getSignatureAlg());
        //        expectations = addJwtHeaderExpectation(expectations, Constants.HEADER_KEY_ID, Constants.HEADER_DEFAULT_KEY_ID);

        expectations = addJwtClaimExpectation(expectations, Constants.JWT_BUILDER_ISSUER, settings.getClientID());
        expectations = addJwtClaimExpectation(expectations, Constants.JWT_BUILDER_SUBJECT, settings.getAdminUser());
        expectations = addJwtClaimExpectation(expectations, Constants.JWT_BUILDER_AUDIENCE, "[" + settings.getJwtConsumerUrl() + "]");

        expectations = addJwtJsonClaimExpectation(expectations, Constants.JWT_SCOPE, settings.getScope());
        expectations = addJwtJsonClaimExpectation(expectations, Constants.JWT_REALM_NAME, settings.getRealm());

        return expectations;
    }

    protected List<validationData> addJwtHeaderExpectation(List<validationData> expectations, String key, String expectedValue) throws Exception {
        String headerPrefix = Constants.JWT_BUILDER_HEADER + Constants.JWT_CONSUMER_TOKEN_HEADER_JSON;
        return vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find expected key [" + key + "] with value [" + expectedValue + "] in the token header.", null,
                headerPrefix + Constants.JWT_BUILDER_KEY + key + " " + Constants.JWT_BUILDER_VALUE + expectedValue);
    }

    protected List<validationData> addJwtClaimExpectation(List<validationData> expectations, String claim, String expectedValue) throws Exception {
        return vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find expected [" + claim + "] claim value [" + expectedValue + "] in the token claims.", null,
                Constants.JWT_CONSUMER_CLAIM + claim + expectedValue);
    }

    protected List<validationData> addJwtJsonClaimExpectation(List<validationData> expectations, String claim, String expectedValue) throws Exception {
        String claimPrefix = Constants.JWT_CONSUMER_CLAIM + Constants.JWT_BUILDER_JSON + Constants.JWT_BUILDER_GETALLCLAIMS;
        return vData.addExpectation(expectations, Constants.INVOKE_JWT_CONSUMER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find expected [" + claim + "] claim value [" + expectedValue + "] in the token claims.", null,
                claimPrefix + Constants.JWT_BUILDER_KEY + claim + " " + Constants.JWT_BUILDER_VALUE + expectedValue);
    }

    /**
     * Create/update expectations with error messages that should be found in the RS and OP servers
     *
     * @param expectations
     *            - current expectations if already set
     * @param settings
     *            - the current test settings
     * @return - updated/created expectations
     * @throws Exception
     */
    protected List<validationData> addServerSideErrorMessages(List<validationData> expectations, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        }
        expectations = rsTools.setInvalidTokenMsg(expectations, settings, genericTestServer);
        expectations = rsTools.setExpectedErrorMessageForInvalidToken(expectations, settings, genericTestServer, testOPServer);

        return expectations;
    }

    /**
     * update app information in the test settings (app/provider info will vary by test and test instance)
     *
     * @param settings
     *            - current test settings
     * @param rsApp
     *            - the test app that we'll be running
     * @param OAuth_provider
     *            - the OAuth provider name that the tests will be using
     * @param OIDC_provider
     *            - the OIDC provider name that the tests will be using
     * @return - updated testsettings
     * @throws Exception
     */
    protected TestSettings updateTestSettings(TestSettings settings, String rsApp, String OAuth_provider, String OIDC_provider) throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, rsApp);

        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OAUTHCONFIGSAMPLE_APP, OAuth_provider, null);
        } else {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OIDCCONFIGSAMPLE_APP, OIDC_provider, null);
        }
        return updatedTestSettings;
    }

    /**
     * Run a test and expect to get good results - get to the protected app
     * Build a JWT token (containing nothing that will cause us not to get to the app)
     *
     * @param settings
     *            - the settings to use to build the token as well as tell us what app to run
     * @throws Exception
     */
    public WebResponse positiveTest(TestSettings settings) throws Exception {
        String originalJwtToken = null;
        if (settings.getUseJwtConsumer()) {
            originalJwtToken = buildAJWTToken(settings, settings.getScope());
        } else {
            originalJwtToken = buildAJWTToken(settings);
        }
        return positiveTest(settings, originalJwtToken);
    }

    /**
     * Run a test and expect to get good results - get to the protected app
     * Build a JWT token (containing nothing that will cause us not to get to the app)
     *
     * @param settings
     *            - the settings to use to tell us what app to run
     * @param jwtToken
     *            - the token to use on the invocation
     * @throws Exception
     */
    public WebResponse positiveTest(TestSettings settings, String jwtToken) throws Exception {

        settings.printTestSettings();
        WebConversation wc = new WebConversation();

        if (settings.getUseJwtConsumer()) {
            // TODO
            List<validationData> expectations = addJwtConsumerExpectations(null, _testName, settings);
            msgUtils.printOAuthOidcExpectations(expectations);
            return helpers.invokeJwtConsumer(_testName, wc, jwtToken, settings, expectations);
        } else {
            List<validationData> expectations = addRSProtectedAppExpectations(_testName, settings);
            msgUtils.printOAuthOidcExpectations(expectations);
            return helpers.invokeRsProtectedResource(_testName, wc, jwtToken, settings, expectations);
        }

    }

    /**
     * Run a test and expect to get a failure - attempt to invoke the protected app, but get a 401 exception
     * and no messages in the server side log(s)
     *
     * @param settings
     *            - the settings to use to tell us what app to run
     * @param jwtToken
     *            - the token to pass
     * @throws Exception
     */
    public WebResponse negativeTest(TestSettings settings, String jwtToken) throws Exception {
        return negativeTest(settings, jwtToken, null);
    }

    /**
     * Run a test and expect to get a failure - attempt to invoke the protected app, but get a 401 exception
     * and messages in the server side log(s)
     *
     * @param settings
     *            - the settings to use to tell us what app to run
     * @param jwtToken
     *            - the token to pass
     * @param msgs
     *            - the messages that we need to look for in the RS server log (add an expectation for each)
     * @throws Exception
     */
    public WebResponse negativeTest(TestSettings settings) throws Exception {
        String originalJwtToken = buildAJWTToken(settings);
        return negativeTest(settings, originalJwtToken, null);
    }

    public WebResponse negativeTest(TestSettings settings, String[] msgs) throws Exception {
        String originalJwtToken = buildAJWTToken(settings);
        return negativeTest(settings, originalJwtToken, msgs);
    }

    public WebResponse negativeTest(TestSettings settings, String jwtToken, String[] msgs) throws Exception {

        settings.printTestSettings();
        WebConversation wc = new WebConversation();

        List<validationData> expectations = null;
        String errorStep = null;

        if (settings.getUseJwtConsumer()) {
            errorStep = Constants.INVOKE_JWT_CONSUMER;
            expectations = vData.addSuccessStatusCodesForActions(null, errorStep, Constants.CONSUMER_TEST_ACTIONS);
            expectations = vData.addResponseStatusExpectation(expectations, errorStep, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            errorStep = Constants.INVOKE_RS_PROTECTED_RESOURCE;
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        }

        if (msgs != null) {
            for (String msg : msgs) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, errorStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain [" + msg + "] message indicating issue with the JWT token.", msg);
                if (settings.getUseJwtConsumer()) {
                    String updatedMsg = msg.replace("'", "&#39;");
                    expectations = vData.addExpectation(expectations, errorStep, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find expected message [" + updatedMsg + "] in server log.", null, updatedMsg);
                }
            }
        }

        msgUtils.printOAuthOidcExpectations(expectations);

        if (settings.getUseJwtConsumer()) {
            return helpers.invokeJwtConsumer(_testName, wc, jwtToken, settings, expectations);
        } else {
            return helpers.invokeRsProtectedResource(_testName, wc, jwtToken, settings, expectations);
        }

    }

}

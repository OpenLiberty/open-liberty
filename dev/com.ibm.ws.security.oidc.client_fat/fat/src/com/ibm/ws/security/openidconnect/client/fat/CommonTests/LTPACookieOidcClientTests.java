/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.CommonTests;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * <P>
 * This test class tests that the access_token (either form access_token or jwt) are included, found and usable from the LTPA
 * cookie
 * The following conditions need to be met for the function to work properly:
 * <OL>
 * <LI>The config for the RP needs:
 * <UL>
 * <LI>accessTokenInLtpaCookie set to true
 * <LI>ssoCookieName set to the same value as the RS
 * <LI>Uses the same ltpa.keys as the RS
 * </UL>
 * <LI>The config for the RS needs:
 * <UL>
 * <LI>accessTokenInLtpaCookie also set to true
 * <LI>inboundPropagation set to supported
 * <LI>ssoCookieName set to the same value as the RP
 * <LI>Uses the same ltpa.keys as the RS
 * </UL>
 * <LI>The OP config is valid for the RP being used (ie: client, OIDC config name, ... the standard stuff). The test framework
 * will vary the setting of the access_token type so that the OP returns an access_token or jwt (to provide better test coverage
 * as either should work)
 * </OL>
 * <P>
 * The tests will vary the config settings mentioned above as well as any others that could affect behavior. They'll verify
 * appropriate behavior as well as content of output. They will also ensure that understandable/meaningful information is provided
 * in negative scenarios.
 * <P>
 * NOTE: The validationMethod is randomly chosen. Some runs will use introspection, others will use userinfo unless specifically
 * indicated
 **/

public class LTPACookieOidcClientTests extends CommonTest {

    public static Class<?> thisClass = LTPACookieOidcClientTests.class;
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static final String testSpecificOPCookieName = "MyOPCookieName";
    public static final String testSpecificRPCookieName = "MyRPCookieName";
    public static final String testSpecificRSCookieName = "MyRPCookieName";

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // adding rules in the main test class so we have access to all of the flags that are set
    // Some attributes are only ever defined in the generic config, while others are always only in the specific config
    // GenericConfig/SpecificConfig is useful to rule in/out those types of tests

    /*********** NOTE: callSpecificCheck's return TRUE to SKIP a test ***********/

    /**
     * Rult to skip test if:
     * Style of config is OIDC (test runs for oauth2Login, or oidcLogin (generic or provider specific doesn't matter))
     *
     * @author chrisc
     *
     */
    public static class skipIfJWTTokenAsAccessToken extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (testSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
                Log.info(thisClass, "skipIfJWTTokenAsAccessToken", "Token used is JWT as access_token - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfJWTTokenAsAccessToken", "Token used is NOT JWT as access_token - run test");
            return false;
        }
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invokes a protected app on the RP. The RP oidc config specifies that the access_token (either form (access_token or
     * jwt)) should be included in the LTPA token. When the RP returns to the test client, use the same conversation to invoke a
     * protected app on the RS server. We will NOT pass any token in the header when we call the RS. The LTPA token should be
     * found in the LTPA cookie instead.
     * <LI>The config for the RP has accessTokenInLtpaCookie set to true
     * <LI>The config for the RS has accessTokenInLtpaCookie also set to true as well as inboundPropagation set to supported
     * <LI>
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Expects something
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void LTPACookieOidcClientTests_mainPath() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGoodHelloWorldExpectations();

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @Test
    public void LTPACookieOidcClientTests_passTokenInHeaderToo() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        WebResponse response = invokeAppOnRP(wc, testSettings);

        String access_token = validationTools.getTokenFromOutput("access_token=", response);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGoodHelloWorldExpectations();

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2, access_token, Constants.HEADER);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2, access_token, Constants.HEADER);

    }

    @Test
    public void LTPACookieOidcClientTests_badTokenInHeader() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        String bad_token = "some string";

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGoodHelloWorldExpectations();

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2, bad_token, Constants.HEADER);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2, bad_token, Constants.HEADER);

    }

    @Test
    public void LTPACookieOidcClientTests_RPTokenInLTPA_false() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replaceAll("SimpleServlet", "simple/SimpleServlet_RPTokenInLTPA_false"));

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, updatedTestSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        // when we attempt to access the protected app on the RS server, we should get a login page again as there is no token in the LTPA cookie
        List<validationData> expectations2 = setGetLoginPageInstead();
        // issue 3710 // expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find the propagation token missing message", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);
        // issue 3710 // expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get the invalid propagation token message.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @Test
    public void LTPACookieOidcClientTests_RSTokenInLTPA_false() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_RSTokenInLTPA_false");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        // when we attempt to access the protected app on the RS server, we should get a login page again as there is no token in the LTPA cookie
        List<validationData> expectations2 = setGetLoginPageInstead();
        // iss 3710 //expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find the propagation token missing message", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);
        // iss 3710 //  expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get the invalid propagation token message.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @Test
    public void LTPACookieOidcClientTests_RSInboundPropagation_required() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_RSInboundPropagation_required");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        // when we attempt to access the protected app on the RS server, we should get a login page again as there is no token in the LTPA cookie
        List<validationData> expectations2 = vData.addSuccessStatusCodesForActions(Constants.INVOKE_PROTECTED_RESOURCE, Constants.ONLY_PROTECTED_RESOURCE_ACTIONS);
        expectations2 = vData.addResponseStatusExpectation(expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        // issue 3710  expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find the propagation token missing message", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @Test
    public void LTPACookieOidcClientTests_RSInboundPropagation_none() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_RSInboundPropagation_none");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        // when we attempt to access the protected app on the RS server, we should get a login page again as there is no token in the LTPA cookie
        List<validationData> expectations2 = setGetLoginPageInstead();
        // TODO - should there be error messages in the log?

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    /**
     * Shouldn't see a difference in behavior as the RP doesn't support the attribute...
     *
     * @throws Exception
     */
    @Test
    public void LTPACookieOidcClientTests_RPDisableLtpaCookie_true() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replaceAll("SimpleServlet", "simple/SimpleServlet_RPDisableLtpaCookie_true"));

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, updatedTestSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        // when we attempt to access the protected app on the RS server, we should get a login page again as there is no token because there is no LTPA cookie
        List<validationData> expectations2 = setGetLoginPageInstead();
        // issue 3710 expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find the propagation token missing message.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);
        // issue 3710 expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get the invalid propagation token message.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    /**
     * Even though we get an LTPA token with the access_token in it, the RS config says to ignore the LTAP token, so, we get the
     * login page
     *
     * @throws Exception
     */
    @Test
    public void LTPACookieOidcClientTests_RSDisableLtpaCookie_true() throws Exception {

        WebConversation wc = new WebConversation();

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, testSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_RSDisableLtpaCookie_true");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGoodHelloWorldExpectations();

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJWTTokenAsAccessToken.class)
    @Test
    public void LTPACookieOidcClientTests_OPIntrospectTokens_false_introspectEndpoint() throws Exception {

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replaceAll("SimpleServlet", "simple/SimpleServlet_introspectTokens_false_rp"));

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, updatedTestSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_introspectTokens_false_introspectEndpoint_rs");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGetLoginPageInstead();
        expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find the validation failed message.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get the invalid propagation token message.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        testOPServer.addIgnoredServerExceptions("CWWKS1420E");
        
        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfJWTTokenAsAccessToken.class)
    @Test
    public void LTPACookieOidcClientTests_OPIntrospectTokens_false_userInfoEndpoint() throws Exception {

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replaceAll("SimpleServlet", "simple/SimpleServlet_introspectTokens_false_rp"));

        // Invoke the app on the RP - this causes the creation of the access_token and for its inclusion in the LTPA token
        invokeAppOnRP(wc, updatedTestSettings);

        // we've invoked our app on the RP - the tokens were good...
        // Now, we want to use the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.

        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld_introspectTokens_false_userinfoEndpoint_rs");

        // save off the cookie from the conversation
        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);

        List<validationData> expectations2 = setGoodHelloWorldExpectations();

        // invoke app on RS using the same conversation
        invokeAppOnRS(wc, updatedTestSettings, expectations2);

        // invoke app on RS using a new conversation, but, include the RP's LTPA token/cookie
        WebConversation wc2 = new WebConversation();
        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
        invokeAppOnRS(wc2, updatedTestSettings, expectations2);

    }

    /*********************************************************************************************************************************/

    /**
     * Helper method to invoke the app on the RP - when it completes, we should have the access_token in the conversation (in most
     * cases - some configs do exclude that)
     *
     * @param wc
     * @param settings
     * @return
     * @throws Exception
     */
    private WebResponse invokeAppOnRP(WebConversation wc, TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, Constants.LOGIN_USER, settings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, settings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, settings);
        return genericRP(_testName, wc, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

    private void invokeAppOnRS(WebConversation inWC, TestSettings settings, List<validationData> expectations) throws Exception {
        invokeAppOnRS(inWC, settings, expectations, null, null);
    }

    private void invokeAppOnRS(WebConversation inWC, TestSettings settings, List<validationData> expectations, String value, String where) throws Exception {

        // clear the OP's Cookie - it can muddy the water
        inWC.putCookie(testSpecificOPCookieName, null);

        // invoke app on RS using the same conversation
        msgUtils.printAllCookies(inWC);
        helpers.invokeProtectedResource(_testName, inWC, value, where, settings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);

    }

    List<validationData> setGoodHelloWorldExpectations() throws Exception {
        List<validationData> expectations2 = vData.addSuccessStatusCodesForActions(Constants.ONLY_PROTECTED_RESOURCE_ACTIONS);
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_MSG);
        return expectations2;
    }

    List<validationData> setGetLoginPageInstead() throws Exception {
        List<validationData> expectations2 = vData.addSuccessStatusCodesForActions(Constants.ONLY_PROTECTED_RESOURCE_ACTIONS);
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        return expectations2;

    }

    //    //    //chc//chc@Test
    //    public void LTPACookieOidcClientTests_useNewConversation_copyLTPA() throws Exception {
    //
    //        WebConversation wc = new WebConversation();
    //
    //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.setScope("openid profile");
    //        updatedTestSettings.addRequestParms();
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        //        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
    //        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
    //        //        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
    //        //        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
    //        //        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
    //        WebResponse response = genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    //
    //        // we've invoked our app on the RP - the tokens were good...
    //        //Now, we want to Just grab the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.
    //
    //        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");
    //
    //        msgUtils.printAllCookies(wc);
    //        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);
    //
    //        WebConversation wc2 = new WebConversation();
    //        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
    //        helpers.invokeProtectedResource(_testName, wc2, null, null, updatedTestSettings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);
    //    }
    //
    //    ////chc@Test  - chris' hack code
    //    public void LTPACookieOidcClientTests_3() throws Exception {
    //
    //        //        WebConversation wc = new WebConversation();
    //        //
    //        //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        //        updatedTestSettings.setScope("openid profile");
    //        //        updatedTestSettings.setPostLogoutRedirect(null);
    //        //
    //        //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
    //        //        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
    //        //        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
    //        //        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
    //        //        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    //        //
    //        //        // validate the cookie name and that it has value
    //        //        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName));
    //        //        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName));
    //        //        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);
    //
    //        WebConversation wc = new WebConversation();
    //
    //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        updatedTestSettings.setScope("openid profile");
    //        updatedTestSettings.addRequestParms();
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes(null);
    //        //        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
    //        //        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
    //        //        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
    //        //        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
    //        //        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
    //        WebResponse response = genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    //
    //        // we've invoked our app on the RP - the tokens were good...
    //        //Now, we want to Just grab the LTPA token from that response and pass it to the  RS server when we try to invoke helloworld there.
    //
    //        updatedTestSettings.setProtectedResource(genericTestServer.getHttpString() + "/helloworld/rest/helloworld");
    //
    //        msgUtils.printAllCookies(wc);
    //        String ltpaCookie = cookieTools.getCookieValue(wc, testSpecificRPCookieName);
    //
    //        WebConversation wc2 = new WebConversation();
    //        wc2.putCookie(testSpecificRPCookieName, ltpaCookie);
    //
    //        //        String accessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
    //        //        Log.info(thisClass, _testName, "access_token: " + accessToken);
    //        //        helpers.invokeProtectedResource(_testName, wc, accessToken, Constants.HEADER, updatedTestSettings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);
    //
    //        helpers.invokeProtectedResource(_testName, wc2, null, null, updatedTestSettings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);
    //    }
}

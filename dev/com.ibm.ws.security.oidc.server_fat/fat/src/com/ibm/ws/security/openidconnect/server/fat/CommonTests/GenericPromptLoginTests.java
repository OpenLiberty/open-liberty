/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;

public class GenericPromptLoginTests extends CommonTest {

    private static final Class<?> thisClass = GenericPromptLoginTests.class;

    /**
     * Update and return test settings for the calling test - this test class needs to use a different redirect jsp and
     * a different client.
     * 
     * @param settings
     *            - original test settings (where we can pull any needed values from)
     * @param addPromptLogin
     *            - true: add prompt=login to the parm list, false: don't add prompt=login to the parm list
     * @return - an updated copy of settings (so we don't alter the classwide settings)
     * @throws Exception
     */
    public TestSettings updateSettingsForPromptLoginTests(TestSettings settings, Boolean addPromptLogin) throws Exception {

        TestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.setClientRedirect(testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/authorize_redirect.jsp");
        updatedTestSettings.setClientID("client05");
        updatedTestSettings.setClientName("client05");
        updatedTestSettings.setScope("openid scope1 scope2"); // added since prompt only works when scope contains "openid".

        if (addPromptLogin) {
            updatedTestSettings.setLoginPrompt("login");
        }
        return updatedTestSettings;

    }

    /**
     * Create the Expectations for a good BasicAuth test flow.
     * 
     * @param settings
     *            - current test settings
     * @return - good test expecations for basicauth
     * @throws Exception
     */
    public List<validationData> buildGoodBasicAuthExpectations(TestSettings settings) throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        // no login prompt
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Did get the login page and should NOT have", null, Constants.LOGIN_PROMPT);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.ACCESS_TOKEN_KEY);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        //		// Response should not have an ltpa token
        //		expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");
        // make sure we have the correct user in the output
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, null, "Remote user was NOT: " + settings.getAdminUser(), null, settings.getAdminUser());

        return expectations;
    }

    /**
     * Create the Expectations for a good form login test flow - where we've already logged in.
     * It looks the same as a basic auth flow
     * 
     * @param settings
     *            - current test settings
     * @return - good test expecations for a form login where we've already logged in
     * @throws Exception
     */
    public List<validationData> buildGoodFormLoginAgainExpectations(TestSettings settings) throws Exception {

        return buildGoodBasicAuthExpectations(settings);
    }

    /**
     * Create the Expectations for a good Form Login test flow.
     * 
     * @param settings
     *            - current test settings
     * @return - good test expecations for form login
     * @throws Exception
     */
    public List<validationData> buildGoodFormLoginExpectations(TestSettings settings) throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        // check for login prompt
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the login page", null, Constants.LOGIN_PROMPT);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.ACCESS_TOKEN_KEY);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        //		// Response should not have an ltpa token
        //		expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");
        // make sure we have the correct user in the output
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, null, "Remote user was NOT: " + settings.getAdminUser(), null, settings.getAdminUser());

        return expectations;
    }

    /** Form Login Tests **/
    /*
     * OP Server form login flow:
     * invoke authorize endpoint
     * get login prompt - omitted on second pass if prompt=login is NOT specified
     * fill in login prompt page - omitted on second pass if prompt=login is NOT specified
     * get auth code
     * invoke token endpoint with auth code
     * get access token and if appropriate id_token
     * use access token to invoke protected app
     */

    /**
     * Invoke a protected app twice using our OP server form login flow
     * without prompt=login
     * Expect to get to the protected app the second time without having to log in.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);
        List<validationData> expectations2 = buildGoodFormLoginAgainExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        // since default of NO prompt, ...
        // Invoke normal flow without client.jsp  (auth endpoint called, login bypassed, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_AGAIN_ACTIONS, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server form login flow
     * with prompt=login
     * Expect to get to the protected app the second time having to log in.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_sameUser_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        // same expectations as the first call
        // since setting prompt, ... it should behave the same as the first invocation
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server form login flow
     * specifying prompt=login on the second attempt
     * Expect to get to the protected app the second time having to log in.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPromptOnSecondAttempt_sameUser_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        updatedTestSettings.setLoginPrompt("login");
        // same expectations as the first call
        // since setting prompt, ... it should behave the same as the first invocation
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server form login flow
     * specifying prompt=login on the first attempt only
     * Expect to get to the protected app the second time without having to log in.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPromptOnFirstAttempt_sameUser_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);
        List<validationData> expectations2 = buildGoodFormLoginAgainExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        updatedTestSettings.setLoginPrompt(null);

        // since default of NO prompt, ...
        // Invoke normal flow without client.jsp  (auth endpoint called, login bypassed, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_AGAIN_ACTIONS, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server form login flow
     * with prompt=login and specifying a bad password on the second attempt
     * Expect to get a login failure and be presented with the login page
     * a second time on the second attempt.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_sameUserBadPw_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        updatedTestSettings.setAdminPswd("badpswd");

        // should expect failures due to the bad password
        List<validationData> expectations2 = vData.addSuccessStatusCodes();
        expectations2 = vData.addExpectation(expectations2, Constants.PERFORM_LOGIN, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating the user [testuser] has enter an invalid password.", null, Constants.MSG_INVALID_PWD + ".*testuser");
        expectations2 = vData.addExpectation(expectations2, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_PROMPT);
        expectations2 = vData.addExpectation(expectations2, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_ERROR);

        testOPServer.addIgnoredServerExceptions("CWIML4537E");
        
        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server form login flow
     * with prompt=login and specifying a differnt user/pw on the second attempt
     * Expect to get the login page on the second attempt and that the user
     * will be valid. The output from the protected app will contain this
     * second user, not the user from the first attempt
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_diffUser_formLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        updatedTestSettings.setAdminUser("diffuser");
        updatedTestSettings.setAdminPswd("diffuserpwd");

        // create good expectations again, just specify the different user
        List<validationData> expectations2 = buildGoodFormLoginExpectations(updatedTestSettings);

        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations2);

    }

    // Basic Auth variations
    /*
     * OP Server basic auth flow:
     * invoke authorize endpoint
     * get auth code (login is hidden from view)
     * invoke token endpoint with auth code
     * get access token and if appropriate id_token
     * use access token to invoke protected app
     */

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * without prompt=login
     * Expect to get to the protected app the second time - no visible difference
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_sameUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        // this isn't proving that we're not using the id/pw passed in the header, ... but...
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get to the protected app the second time - no visible difference
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_sameUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        // this isn't proving that we're not using the id/pw passed in the header, ... but...
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * without prompt=login
     * Expect to get to the protected app the second time - no visible difference
     * The bad password is not used as prompt=login is NOT set
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_sameUserBadPw_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setAdminPswd("badpswd");

        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get a login failure as the password is bad on the second
     * under the covers login (and no form to present for a new password)
     * We should also get a 302 status code
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException")
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_sameUserBadPw_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setAdminPswd("badpswd");

        // should expect failures due to the bad password
        List<validationData> expectations2 = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_ENDPOINT);
        expectations2 = vData.addResponseStatusExpectation(expectations2, Constants.INVOKE_AUTH_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, Constants.UNAUTHORIZED_MESSAGE);
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, "Error 401: SRVE0295E: Error reported: 401");
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, Constants.MSG_INVALID_PWD + ".*testuser");
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, "CWWKS1440E");

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWWKS1440E", "SRVE8094W", "SRVE8115W");
        
        // since setting prompt, ... it should be forced to authenticate again, but will fail because of the bad password
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get a login failure as the password is bad on the second
     * under the covers login (and no form to present for a new password)
     * We should also get a 302 status code
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPromptNone_sameUserBadPw_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);
        updatedTestSettings.setLoginPrompt("none");

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setAdminPswd("badpswd");

        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * without prompt=login
     * Expect to get to the protected app the second time - no visible difference
     * The protected app will print the original user
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_diffUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        // update the user but not the expectations - we won't be logging in again, so the user will be the same
        updatedTestSettings.setAdminUser("diffuser");
        updatedTestSettings.setAdminPswd("diffuserpwd");

        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get to the protected app the second time - the only visible difference
     * is in the output of the protected app - where we'll see "diffuser" instead of
     * "testuser".
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_diffUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setAdminUser("diffuser");
        updatedTestSettings.setAdminPswd("diffuserpwd");

        List<validationData> expectations2 = buildGoodBasicAuthExpectations(updatedTestSettings);

        // since setting prompt, ... it should behave the same as the first invocation until we give the wrong password
        // Invoke normal flow without client.jsp  (auth endpoint called, then login, and finally the login page with the error
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations2);

    }

    // Form Login/Basic Auth Mix variations
    /**
     * Invoke a protected app using our OP server basic auth flow
     * and then do it again using our form login flow
     * both without prompt=login
     * Expect to get to the protected app the second time with NO login prompt
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_basicAuth_thenFormLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        // since default of NO prompt, ...
        // leave out the basic auth info - all should still work as it's not needed
        // Invoke normal flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_AGAIN_ACTIONS, expectations);

    }

    /**
     * Invoke a protected app using our OP server basic auth flow
     * and then do it again using our form login flow
     * both with prompt=login
     * Expect to get to the protected app the second time after getting the
     * login prompt on the second attempt
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_basicAuth_thenFormLogin() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        List<validationData> expectations2 = buildGoodFormLoginExpectations(updatedTestSettings);

        // leave out the basic auth info - with it there, we can't tell if we're getting access because of that or because we skipped the check
        // Invoke normal basic auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations2);

    }

    /**
     * Invoke a protected app using our OP server form login flow
     * and then do it again using our basic auth flow
     * both without prompt=login
     * Expect to get to the protected app the second time with NO login prompt
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithoutLoginPrompt_FormLogin_thenBasicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);
        List<validationData> expectations2 = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal form login flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        // since default of NO prompt, ...
        // Invoke normal flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations2);

    }

    /**
     * Invoke a protected app using our OP server form login flow
     * and then do it again using our basic auth flow
     * both with prompt=login
     * Expect to get to the protected app the second time with NO login prompt
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPrompt_FormLogin_thenBasicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodFormLoginExpectations(updatedTestSettings);
        List<validationData> expectations2 = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal form login flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations);

        // since default of NO prompt, ...
        // Invoke normal flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * without prompt=login on the first attempt, and with prompt=login
     * on the second attempt
     * Expect to get to the protected app the second time - no visible difference
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPromptOnSecondAttempt_sameUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setLoginPrompt("login");

        List<validationData> expectations2 = buildGoodFormLoginExpectations(updatedTestSettings);

        // leave out the basic auth info - with it there, we can't tell if we're getting access because of that or because we skipped the check
        // Invoke normal basic auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS, expectations2);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login on the first attempt, and without prompt=login
     * on the second attempt
     * Expect to get to the protected app the second time - no visible difference
     * 
     * @throws Exception
     */
    @Test
    public void testAuthCodeBasicFlowAgainWithLoginPromptOnFirstAttempt_sameUser_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = buildGoodBasicAuthExpectations(updatedTestSettings);

        // Invoke normal flow without client.jsp  (auth endpoint called, then login, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

        updatedTestSettings.setLoginPrompt(null);

        // since default of NO prompt, ...
        // Invoke normal flow without client.jsp  (auth endpoint called, login bypassed, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_AGAIN_ACTIONS, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get a login failure as the password is bad on the second
     * under the covers login (and no form to present for a new password)
     * We should also get a 302 status code
     * 
     * @throws Exception
     */
    /* OAuth expects a 401 status code and an ffdc */
    /* OIDC expects a 302 status code which the server redirects preventing its return to the test - NO ffdc in this case */
    @Test
    @AllowedFFDC(value = { "com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException", "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception" })
    public void testAuthCodeBasicFlowLoginPromptNone_BadPw_basicAuth() throws Exception {

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, false);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_ENDPOINT);
        if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            // redirect is happening in the server, so we can't catch it here - we'll get a 200
            //overrideRedirect() ;
            //expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.REDIRECT_STATUS);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.REDIRECT_STATUS);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, "error=login_required");
        } else {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, Constants.UNAUTHORIZED_MESSAGE);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, "Error 401: SRVE0295E: Error reported: 401");
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, Constants.MSG_INVALID_PWD + ".*testuser");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, "CWWKS1440E");

        updatedTestSettings.setAdminPswd("badpswd");
        updatedTestSettings.setLoginPrompt("none");

        overrideRedirect();

        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        List<endpointSettings> headers = null;
        Log.info(thisClass, _testName, "Building basic auth with user: " + updatedTestSettings.getAdminUser() + " and password: " + updatedTestSettings.getAdminPswd());
        headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(updatedTestSettings.getAdminUser(), updatedTestSettings.getAdminPswd()));
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", updatedTestSettings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "response_type", "code");
        parms = eSettings.addEndpointSettings(parms, "prompt", updatedTestSettings.getLoginPrompt());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", updatedTestSettings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", updatedTestSettings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "autoauthz", "true");
        parms = eSettings.addEndpointSettings(parms, "state", updatedTestSettings.getState());

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWWKS1440E");
        
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getAuthorizeEndpt(), Constants.POSTMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers, expectations);

    }

    /**
     * Invoke a protected app twice using our OP server basic auth flow
     * with prompt=login
     * Expect to get a login failure as the password is bad on the second
     * under the covers login (and no form to present for a new password)
     * We should also get a 302 status code
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException")
    public void testAuthCodeBasicFlowLoginPrompt_BadPw_basicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = updateSettingsForPromptLoginTests(testSettings, true);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_ENDPOINT);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, Constants.UNAUTHORIZED_MESSAGE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail login (due to the bad password)", null, "Error 401: SRVE0295E: Error reported: 401");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, Constants.MSG_INVALID_PWD + ".*testuser");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating the user [testuser] has entered an invalid password.", null, "CWWKS1440E");

        updatedTestSettings.setAdminPswd("badpswd");

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWWKS1440E", "SRVE8115W");
        
        // Invoke normal basis auth flow without client.jsp  (auth endpoint called, then token endpoint, and finally invoke our protected app
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }
}

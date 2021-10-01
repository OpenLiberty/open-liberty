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

package com.ibm.ws.security.openidconnect.client.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect end session logout tests.
 **/

public class LogoutOidcClientTests extends CommonTest {

    public static Class<?> thisClass = LogoutOidcClientTests.class;

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token after a successful login. The client then invokes the
     * <LI>end_session endpoint to logoff, by sending the ID token in the logout
     * <LI>request using the id_token_hint parameter. In this scenario, the
     * <LI>post_logout_redirect_uri parameter is not set in the logout request.
     * <LI>The test verifies that logout is successfull and also verifies that if
     * <LI>the client attempts to access the protected resource after logout, it
     * <LI>is prompted with a login form.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get login page when protected app is invoked again after logout.
     * </OL>
     */

    @Mode(TestMode.LITE)
    @Test
    public void LogoutOidcClientTestMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);
        // make sure we get the login page again (since we logged out at the end of the last step
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token after a successful login. The client then invokes the
     * <LI>end_session endpoint to logoff, by sending the ID token in the logout
     * <LI>request using the id_token_hint parameter. In this scenario, the
     * <LI>post_logout_redirect_uri parameter is also set in the logout request.
     * <LI>The test verifies that logout is successfull and also verifies that the
     * <LI>OP correctly redirects the logout response to the URI specified by
     * <LI>the post_logout_redirect_uri parameter.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The client should be redirected to the URI specified by post_logout_redirect_uri
     * </OL>
     */

    @Mode(TestMode.LITE)
    @Test
    public void LogoutOidcClientTestMainPathWithRedirectUri() throws Exception {

        WebConversation wc = new WebConversation();
        String opHttpPort = testOPServer.getHttpDefaultPort().toString();
        String postLogoutURL = "http://localhost:" + opHttpPort + "/oauth2tai/simpleIdP";

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(postLogoutURL);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "OP did not redirect request to post_logout uri", null, Constants.POSTLOGOUTPAGE);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);
        // make sure we get the login page again (since we logged out at the end of the last step
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);

    }

}

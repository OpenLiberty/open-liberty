/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientCookieVerificationTests extends CommonTest {

    public static Class<?> thisClass = OidcClientCookieVerificationTests.class;

    private String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientCookieVerificationTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(Constants.RP_FLOW);

        testRPServer.addIgnoredServerExceptions("CWWKS1859E", "CWWKS1741W");
    }

    /**
     * Go through the golden path OIDC login flow with nonce enabled. Verify that the nonce cookie is ultimately deleted
     * correctly.
     */
    @Test
    public void test_verifyNonceCookieDeleted() throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        String protectedUrl = updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/nonceEnabled");
        updatedTestSettings.setTestURL(protectedUrl);
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(test_GOOD_LOGIN_ACTIONS);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_HEADER, Constants.STRING_DOES_NOT_CONTAIN, "Should not have seen any Set-Cookie headers in the response but did.", null, "Set-Cookie");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found a WASOidcState cookie but didn't.", null, "WASOidcState[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found a WASOidcNonce cookie but didn't.", null, "WASOidcNonce[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_HEADER, Constants.STRING_MATCHES, "Should have found a Set-Cookie header to clear the WASOidcState cookie but didn't.", null, "WASOidcState[pn][0-9]+=\"\"; Expires=Thu, 01 Dec 1994");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_HEADER, Constants.STRING_MATCHES, "Should have found a Set-Cookie header to clear the WASOidcNonce cookie but didn't.", null, "WASOidcNonce[pn][0-9]+=\"\"; Expires=Thu, 01 Dec 1994");
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);

        genericRP(_testName, webClient, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

        // Verify all cookies that should have been deleted do not appear in the web client anymore
        validationTools.verifyNoUnexpectedCookiesStillPresent(webClient, Arrays.asList("WASOidcCode", "WASOidcState", "WASOidcNonce", "WASReqURLOidc"));
    }

    // TODO - OAuthClientTracker.TRACK_OAUTH_CLIENT_COOKIE_NAME

}

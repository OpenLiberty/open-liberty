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

import java.util.HashMap;
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
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class GenericOidcClientConsentTests extends CommonTest {

    public static Class<?> thisClass = GenericOidcClientConsentTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.GET_RP_CONSENT;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";

    // old message before 152627
    // Stack Dump = org.openid4java.discovery.yadis.YadisException: 
    //    0x704: I/O transport error: peer not authenticated
    //
    // new message after 152627
    // Stack Dump = org.openid4java.discovery.yadis.YadisException: 
    //    0x704: I/O transport error: com.ibm.jsse2.util.j: 
    //       PKIX path building failed: java.security.cert.CertPathBuilderException:
    //       PKIXCertPathBuilderImpl could not build a valid CertPath.; internal cause is:     
    String errMsg0x704 = "CertPathBuilderException";

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is enabled in OP
     * <LI>And in the client, the grantType is set to "implicit" to enable the response_mode=form_post
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientConsentTestGetMainPath__and__testUserInfoWithImplicitFlow() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
        
        // this tests userinfo with implicit flow.
        new GenericOidcClientTests().testUserInfo(wc);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is enabled in OP
     * <LI>And in the client, the grantType is set to "implicit"
     * <LI>but we disable the response_mode=form_post through FAT function processRPConsent
     * <LI>Our current processRPConsent will only test the fragment through "GET" method
     * <LI>In the case, we block the "GET" on the fragment flow, then
     * <LI>then this test case need to be changed to a negative test
     * <LI>This will test the RP client handling the fragment in implicit flow
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    //@Mode(TestMode.LITE)
    //@Test
    public void OidcClientTestGetMainPathWithoutResponseMode() throws Exception {
        testRPServer.reconfigServer("rp_server_nonce_enabled_implicit.xml", _testName, false, null);

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);
        updatedTestSettings.setResponseMode(null); // disable the response_mode through FAT processRPConsent method

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is enabled in OP
     * <LI>And in the client, the grantType is set to "implicit" to enable the response_mode=form_post
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientTestPostMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.addRequestFileParms();
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.POST_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addRequestFileParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_POST_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url,
     * <LI>then specify a valid user id, password, ....
     * <LI>finally, attempt to access the same servlet again
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>Test is showing that we can access the same servlet again without having to log back in.
     * <LI>In this scenario, HTTP is used between RP and OP and httpsRequired is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue, then access the same
     * <LI>servlet again, without requiring another login/authentication
     * </OL>
     */

    @Test
    public void OidcClientTestMainPathAccessAppAgain() throws Exception {

        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.GET_RP_CONSENT, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_AGAIN, Constants.RESPONSE_FULL, Constants.IDToken_STR);

        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_AGAIN_ACTIONS, expectations);

    }

}

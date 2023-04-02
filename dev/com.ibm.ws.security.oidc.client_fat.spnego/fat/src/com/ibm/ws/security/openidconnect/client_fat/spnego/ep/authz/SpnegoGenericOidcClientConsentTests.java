/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client_fat.spnego.ep.authz;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;
import com.ibm.ws.security.spnego.fat.config.InitClass;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SpnegoGenericOidcClientConsentTests extends SpnegoOIDCCommonTest {

    public static Class<?> thisClass = SpnegoGenericOidcClientConsentTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_BAD_LOGIN_ACTIONS = SpnegoOIDCConstants.BAD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = SpnegoOIDCConstants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = SpnegoOIDCConstants.GET_RP_CONSENT;
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

    @Mode(TestMode.LITE)
    @Test
    public void test_SPNEGO_OIDC_Client_GetMainPath_NotContain() throws Exception {
        if (InitClass.isRndHostName) {
            return;
        }
        TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings(), true);
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setNonce(SpnegoOIDCConstants.EXIST_WITH_ANY_VALUE);;

        WebClient webClient = getAndSaveWebClient(true);
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SpnegoOIDCConstants.RESPONSE_FULL, SpnegoOIDCConstants.IDToken_STR);
        expectations = vData.addExpectation(expectations, SpnegoOIDCConstants.GET_SPNEGO_LOGIN_PAGE_METHOD, SpnegoOIDCConstants.RESPONSE_FULL, SpnegoOIDCConstants.STRING_CONTAINS,
                                            "Did Not get the OpenID Connect login page.", null, SpnegoOIDCConstants.FORMLOGIN_SERVLET);
        expectations = vData.addExpectation(expectations, SpnegoOIDCConstants.GET_SPNEGO_LOGIN_PAGE_METHOD, SpnegoOIDCConstants.RESPONSE_FULL, SpnegoOIDCConstants.STRING_CONTAINS,
                                            "Did Not get the OpenID Connect login page.", null, SpnegoOIDCConstants.SPNEGO_NEGOTIATE + getCommonSpnegoToken());
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, webClient, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

//    @Mode(TestMode.LITE)
//    @Test
//    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException")
    public void test_SPNEGO_OIDC_Client_Bad_Token_GetMainPath_NotContain() throws Exception {
        testOPServer.addIgnoredServerException("SRVE8094W");
        testOPServer.addIgnoredServerException("SRVE8115W");
        testOPServer.addIgnoredServerException("CWWKS1440E");
        TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings(), true);
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setNonce(SpnegoOIDCConstants.EXIST_WITH_ANY_VALUE);

        WebClient webClient = getAndSaveWebClient(true);
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SpnegoOIDCConstants.GET_LOGIN_PAGE);
        expectations = vData.addResponseStatusExpectation(expectations, SpnegoOIDCConstants.GET_LOGIN_PAGE, 401);
        expectations = vData.addResponseExpectation(expectations, SpnegoOIDCConstants.GET_LOGIN_PAGE, "Error 401: SRVE0295E: Error reported: 401",
                                                    "Error 401: SRVE0295E: Error reported: 401");

        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, webClient, updatedTestSettings, test_BAD_LOGIN_ACTIONS, expectations);

    }

}

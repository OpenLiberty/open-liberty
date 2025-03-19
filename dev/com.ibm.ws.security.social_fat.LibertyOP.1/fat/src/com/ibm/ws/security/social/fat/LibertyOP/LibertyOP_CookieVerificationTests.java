/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.LibertyOP;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This is the test class that will run tests to verify that cookies are handled
 * correctly in social login flows.
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class LibertyOP_CookieVerificationTests extends SocialCommonTest {

    public static Class<?> thisClass = LibertyOP_CookieVerificationTests.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {
        classOverrideValidationEndpointValue = Constants.USERINFO_ENDPOINT;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        List<String> opStartMsgs = new ArrayList<String>();
        // opStartMsgs.add("CWWKS1600I.*" + SocialConstants.OIDCCONFIGMEDIATOR_APP);
        opStartMsgs.add("CWWKS1631I.*");

        List<String> opExtraApps = new ArrayList<String>();
        opExtraApps.add(SocialConstants.OP_SAMPLE_APP);

        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.op", "op_server_orig.xml",
                SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null,
                SocialConstants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.social",
                "server_LibertyOP_cookieVerificationTests.xml", SocialConstants.GENERIC_SERVER, extraApps,
                SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP);

        socialSettings = updateLibertyOPSettings(socialSettings);
    }

    @Test
    public void test_verifyDefaultCookiesDeleted() throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_DOES_NOT_CONTAIN,
                "Should not have seen any Set-Cookie headers in the response but did.", null, "Set-Cookie");
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE,
                Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES,
                "Should have found a WASOidcState cookie but didn't.", null,
                "WASOidcState[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE,
                Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES,
                "Should have found a WASOidcNonce cookie but didn't.", null,
                "WASOidcNonce[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        String expirationCookieFormat = JakartaEEAction.isEE10OrLaterActive() ? ";(?i) max-age=0"
                : ";(?i) Expires=Thu, 01 Dec 1994";
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN,
                Constants.RESPONSE_HEADER, Constants.STRING_MATCHES,
                "Should have found a Set-Cookie header to clear the WASOidcState cookie but didn't.", null,
                "WASOidcState[pn][0-9]+=\"\"" + expirationCookieFormat);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN,
                Constants.RESPONSE_HEADER, Constants.STRING_MATCHES,
                "Should have found a Set-Cookie header to clear the WASOidcNonce cookie but didn't.", null,
                "WASOidcNonce[pn][0-9]+=\"\"" + expirationCookieFormat);

        genericSocial(_testName, webClient, invoke_social_login_actions, socialSettings, expectations);

        List<String> allowedCookies = new ArrayList<>();
        allowedCookies.add("JSESSIONID");
        allowedCookies.add("LtpaToken2");
        allowedCookies.add("WAS_[np][0-9]+");
        allowedCookies.add("WASOidcSession");

        // Verify all cookies that should have been deleted do not appear in the web
        // client anymore
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, allowedCookies);
    }

}

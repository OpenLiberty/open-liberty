/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.CommonTests.GenericCookieNameTests;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCCookieNameTest extends GenericCookieNameTests {

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    private static final Class<?> thisClass = OIDCCookieNameTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for 
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OIDC_OP);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp(testServerName, "server_cookieName.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        goodActions = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;

    }

    @Test
    public void testSpecifiedCookieName_LogoutAndCheckForCookie() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setPostLogoutRedirect(null);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Invoke client.jsp and login
        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);

        cookieTools.validateCookie(wc, testSpecificCookieName);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

        // Invoke end session logout
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericOP(_testName, wc, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, expectations, null, validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY));

        cookieTools.validateCookie(wc, testSpecificCookieName, false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    @Test
    public void testDefaultCookieName_LogoutAndCheckForCookie() throws Exception {

        testOPServer.reconfigServer("server_orig.xml", _testName, Constants.JUNIT_REPORTING, null);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setPostLogoutRedirect(null);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Invoke client.jsp and login
        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);

        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName));
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

        // Invoke end session logout
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericOP(_testName, wc, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, expectations, null, validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY));

        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

}

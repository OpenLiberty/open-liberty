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

package com.ibm.ws.security.openidconnect.server.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test class for grantType. Tests grantType handling within the OP.
 * These tests are run for both web and implicit clients.
 * They run with both oauth and oidc. The extending class will set the test
 * setting appropriatly for the flow/provider being tested.
 * The extending class specifies the action/flow.
 * 
 * @author chrisc
 * 
 */
public class GenericCookieNameTests extends CommonTest {

    private static final Class<?> thisClass = GenericCookieNameTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    protected static String targetProvider = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String testServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat";
    protected static String testSpecificCookieName = "MyCookieName";
    protected static String cookieActions = "invoke_cookie_jsp";

    @Test
    public void testSpecifiedCookieName() throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, testSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);

        cookieTools.validateCookie(wc, testSpecificCookieName);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    @Test
    public void testDefaultCookieName() throws Exception {

        testOPServer.reconfigServer("server_orig.xml", _testName, Constants.JUNIT_REPORTING, null);
        List<validationData> expectations = vData.addSuccessStatusCodes();

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, testSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);

        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, testServerName));
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    @Mode(TestMode.LITE)
    @Test
    public void testGetSpecifiedCookieName() throws Exception {

        String cookieNameJsp = testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/printCookieName.jsp";
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, cookieActions, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the correct cookie name", null, testSpecificCookieName);

        WebConversation wc = new WebConversation();
        genericInvokeEndpoint(_testName, wc, null, cookieNameJsp, Constants.GETMETHOD, cookieActions, null, null, expectations, testSettings);

    }

    @Mode(TestMode.LITE)
    @Test
    public void testGetDefaultCookieName() throws Exception {

        testOPServer.reconfigServer("server_orig.xml", _testName, Constants.JUNIT_REPORTING, null);

        String cookieNameJsp = testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/printCookieName.jsp";
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, cookieActions, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the correct cookie name", null, cookieTools.buildDefaultCookieName(testOPServer, testServerName));

        WebConversation wc = new WebConversation();
        genericInvokeEndpoint(_testName, wc, null, cookieNameJsp, Constants.GETMETHOD, cookieActions, null, null, expectations, testSettings);

    }
}
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
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class JaxRSClientGenericTests extends CommonTest {

    public static Class<?> thisClass = JaxRSClientGenericTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;
    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";

    String errMsg0x704 = "CertPathBuilderException";

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url - the test servlet then invokes an app on the RS server -
     * passing in the access_token. Access should be granted to the second app...
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
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
    public void OidcClientTestGetMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = validationTools.addDefaultRSOAuthExpectations(null, _testName, test_FinalAction, updatedTestSettings);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

}

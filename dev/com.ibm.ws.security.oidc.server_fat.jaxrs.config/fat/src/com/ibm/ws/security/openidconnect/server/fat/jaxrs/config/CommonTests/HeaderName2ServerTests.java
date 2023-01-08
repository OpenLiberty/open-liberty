/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;

public class HeaderName2ServerTests extends CommonTest {

    private static final Class<?> thisClass = HeaderName2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static CommonTools commonTools = new CommonTools();
    protected static String realmName = Constants.BASIC_REALM;
    protected static int OIDC_ERROR_1 = 1;

    @Before
    public void beforeTest() {
        createCommonPropagationToken();
    }

    /**
     * set the correct realm name for the current invocation
     *
     * @param settings
     *            - test settings
     * @throws Exception
     */
    public static void setRealmForValidationType(TestSettings settings) throws Exception {
        realmName = Constants.BASIC_REALM;
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY) && eSettings.getProviderType().equals(Constants.OIDC_OP) && genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            realmName = "http://" + targetISSEndpoint;
        }

    }

    List<validationData> setGoodHeaderNameExpectations(TestSettings settings) throws Exception {
        TestSettings updatedTestSettings = settings.copyTestSettings();
        // app looks for "Authorization" in the header - we won't find that in the header (as it uses a different name)
        // so, trick the tools to look for info in the output that will actually be there.
        updatedTestSettings.setWhere("");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, realmName);

        msgUtils.printTestSettings(updatedTestSettings);
        msgUtils.printOAuthOidcExpectations(expectations, settings);

        return expectations;
    }

    /***************************************************** Tests *****************************************************/

    /**
     * Set a headerName in the RS server config. Put the propagation token into the same attribute in the header of the
     * request to the RS server
     * The server should find the token and grant access to the app
     *
     * @throws Exception
     */
    @Test
    public void HeaderName2ServerTests_headerName_setInCfg_testUsesSameName() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_headerName_jwt");
        updatedTestSettings.setHeaderName("jwt");

        List<validationData> expectations = setGoodHeaderNameExpectations(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Set a headerName in the RS server config. Put the propagation token into the default "Authorization" attribute in the
     * header of the
     * request to the RS server
     * The server should NOT find the token and should NOT grant access to the app - we should get a msg indicating that there was
     * no token
     *
     * @throws Exception
     */
    @Test
    public void HeaderName2ServerTests_headerName_setInCfg_testUsesDefaultName() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_headerName_jwt");
        TestSettings updatedTestSettings2 = updatedTestSettings.copyTestSettings();
        updatedTestSettings2.setWhere("");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        // iss 3710 //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Set a headerName in the RS server config. Put the propagation token into an attribute of the same name/different case in
     * the header of the
     * request to the RS server
     * The server should find the token and grant access to the app (HttpServletRequest getHeaderName will locate the value as it
     * is case insensitive)
     *
     * @throws Exception
     */
    @Test
    public void HeaderName2ServerTests_headerName_setInCfg_testUsesDiffCase() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_headerName_jwt");
        updatedTestSettings.setHeaderName("JWT");

        List<validationData> expectations = setGoodHeaderNameExpectations(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Set a headerName to "" in the RS server config. Put the propagation token into the default "Authorization" in the header of
     * the
     * request to the RS server
     * The server should find the token and grant access to the app
     *
     * @throws Exception
     */
    @Test
    public void HeaderName2ServerTests_headerName_emptyStringInCfg_testUsesDefaultName() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_headerName_empty");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, realmName);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Do NOT set a headerName in the RS server config. Put the propagation token into some attribute in the header of the
     * request to the RS server
     * The server should NOT find the token and should NOT grant access to the app
     *
     * @throws Exception
     */
    @Test
    public void HeaderName2ServerTests_headerName_notInCfg_testUsesNonDefaultName() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld");
        updatedTestSettings.setHeaderName("jwt");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        //iss 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

}

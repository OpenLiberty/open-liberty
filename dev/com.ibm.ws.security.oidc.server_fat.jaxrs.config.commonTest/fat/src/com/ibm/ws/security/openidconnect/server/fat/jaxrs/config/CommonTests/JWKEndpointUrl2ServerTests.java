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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.ExpectedFFDC;

public class JWKEndpointUrl2ServerTests extends CommonTest {

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

    @Before
    public void beforeTest() {
        createCommonPropagationToken();
    }

    //
    //    /**
    //     * set the correct realm name for the current invocation
    //     * @param settings - test settings
    //     * @throws Exception
    //     */
    //    public static void setRealmForValidationType(TestSettings settings) throws Exception {
    //    	realmName = Constants.BASIC_REALM ;
    //        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY) && eSettings.getProviderType().equals(Constants.OIDC_OP) && genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
    //        	realmName = "http://"+ targetISSEndpoint ;
    //        }
    //
    //    }
    //
    List<validationData> setGoodJWKEndpointUrlExpectations(TestSettings settings) throws Exception {
        TestSettings updatedTestSettings = settings.copyTestSettings();
        // app looks for "Authorization" in the header - we won't find that in the header (as it uses a different name)
        // so, trick the tools to look for info in the output that will actually be there.

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, realmName);

        msgUtils.printTestSettings(updatedTestSettings);
        msgUtils.printOAuthOidcExpectations(expectations, settings);

        return expectations;
    }

    /***************************************************** Tests *****************************************************/

    /*
     * testing with a valid JWK in the token and a valid value in the jwkEndpointUrl is
     * tested almost all of the other JWT tests - I'm not going to duplicate that here!
     * So, the first test can be commented out
     */
    //    @Test
    public void JWKEndpointUrl2ServerTests_configured() throws Exception {

        List<validationData> expectations = setGoodJWKEndpointUrlExpectations(testSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, testSettings, expectations);
    }

    /**
     * Invoke an app protected by an openidConnect client config that does NOT specify jwkEndpointUrl
     * expect a 401 status code
     *
     * @throws Exception
     */
    @Test
    public void JWKEndpointUrl2ServerTests_notConfigured() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_jwk_notConfigured");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Invoke an app protected by an openidConnect client config that specifies jwkEndpointUrl set to "" instead of
     * the correct jwkEndpointUrl
     * expect a 401 status code
     *
     * @throws Exception
     */
    @Test
    public void JWKEndpointUrl2ServerTests_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_jwk_empty");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);

    }

    /**
     * Invoke an app protected by an openidConnect client config that specifies jwkEndpointUrl set to some string instead of
     * the correct jwkEndpointUrl
     * expect a 401 status code
     *
     * @throws Exception
     */
    @Test
    public void JWKEndpointUrl2ServerTests_string() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_jwk_string");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);

    }

    /**
     * Invoke an app protected by an openidConnect client config that specifies jwkEndpointUrl set to some non-existent url
     * instead of the correct jwkEndpointUrl. Expects a 401 status code.
     *
     * @throws Exception
     */
    @Test
    public void JWKEndpointUrl2ServerTests_badUrl() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_jwk_badUrl");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that a JWK was not returned from the JWK URL.", MessageConstants.CWWKS6049E_JWK_NOT_RETURNED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);

    }

    /**
     * Invoke an app protected by an openidConnect client config that specifies jwkEndpointUrl set to the authorization
     * endpoint instead of the jwk endpoint
     * expect a 401 status code
     * expect a com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException ffdc since we're using the authorization
     * endpoint
     * as the jwkEndpointUrl - the parms that are passed to it are NOT what would normally be passed the jwk endpoint
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    public void JWKEndpointUrl2ServerTests_someOtherValidUrl() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_jwk_someOtherUrl");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the request does not have a propagation token.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);

    }

    // http vs https  - Adam tested elsewhere
}

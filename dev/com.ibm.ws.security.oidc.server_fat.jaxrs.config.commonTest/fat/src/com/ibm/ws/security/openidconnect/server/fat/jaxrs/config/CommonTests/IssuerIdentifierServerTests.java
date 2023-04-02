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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;

public class IssuerIdentifierServerTests extends CommonTest {

    private static final Class<?> thisClass = IssuerIdentifierServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String targetISSHttpsEndpoint = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static final String oldAccessToken = "eNVMlACDRk7RKEi8AYp45Y2uogVACpERnHZfYDq6";
    protected static CommonTools commonTools = new CommonTools();
    protected static String http_realm = null;
    protected static String https_realm = null;

    protected final String OAUTH_OP_USERINFO_ERROR = "Will not run this test since pure OAuth providers cannot use the userinfo endpoint";

    public static void setRealmForValidationType(TestSettings settings) throws Exception {

        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY) && genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            http_realm = "http://" + targetISSEndpoint;
            https_realm = "https://" + targetISSHttpsEndpoint;
        } else {
            http_realm = Constants.BASIC_REALM;
            https_realm = Constants.BASIC_REALM;
        }
    }

    /***************************************************** Tests *****************************************************/

    /**
     * There is only one issuer in the openid client config and it matches what will be in the issued token
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_one_issuer_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_one_entry");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, https_realm);

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /*******************************/
    /**
     * There is one issuer in the openid client config and it does not matches what will be in the issued token
     * Expected results:
     * - Expect a 401 and an error message indicating that the issuer did NOT match
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_one_issuer_not_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_one_bad_entry");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (updatedTestSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the JWT token is not good.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1724E_UNABLE_TO_AUTHENTICATE_ISSUERBAD);
        }

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * There are multiple issuers in the openid client config and it matches what will be in the issued token
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_multiple_issuer_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_multiple_entries");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, https_realm);

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * There are multiple issuers in the openid client config, but none of them match what will be in the issued token
     * Expected results:
     * - Expect a 401 and an error message indicating that the issuer did NOT match
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_multiple_issuer_not_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_multiple_bad_entries");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (updatedTestSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the JWT token is not good.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1724E_UNABLE_TO_AUTHENTICATE_ISSUERBAD);
        }

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * There is only one issuer in the openid client config and it is a substring of what will be in the issued token
     * Expected results:
     * - Expect a 401 and an error message indicating that the issuer did NOT match
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_substring_not_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_substring");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (updatedTestSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the JWT token is not good.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1724E_UNABLE_TO_AUTHENTICATE_ISSUERBAD);
        }

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * There is only one issuer in the openid client config and it is more that what will be in the issued token
     * Expected results:
     * - Expect a 401 and an error message indicating that the issuer did NOT match
     *
     * @throws Exception
     */
    @Test
    public void IssuerIdentifierServerTests_extended_string_not_match() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_extended_string");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (updatedTestSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that the JWT token is not good.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying that we have an invalid issuer.", MessageConstants.CWWKS1724E_UNABLE_TO_AUTHENTICATE_ISSUERBAD);
        }

        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

}

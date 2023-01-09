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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MultiProviderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;

public class SignatureAttributes2ServerTests extends CommonTest {

    private static final Class<?> thisClass = SignatureAttributes2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static MultiProviderUtils mpUtils = new MultiProviderUtils();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String targetISSHttpsEndpoint = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static final String oldAccessToken = "eNVMlACDRk7RKEi8AYp45Y2uogVACpERnHZfYDq6";
    protected static String defaultISSAccessId = "user:http://" + targetISSEndpoint + "/" + Constants.OIDC_USERNAME;
    protected static String defaultISSAccessIdWithHttps = null;
    protected static String accessIdWithHttpsProvider = null;

    protected static String http_realm = "";
    protected static String https_realm = "";

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /***************************************************** Tests *****************************************************/

    /********************************************** signature attribute tests **********************************************/
    /*
     * The other tests that have been written already make sure that the signatures match (otherwise they wouldn't work)
     * We'll focus more on mis-matches between the client/server
     * We test with HS256/HS256 and RS256/RS256 in most other tests - do other combo's here
     */

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * Expects: With either an access_token or JWT we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256() throws Exception {
        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * Expects: With an access_token we get access to the protected app
     * With a jWT we get a missing key exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverHS256_clientRS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm NONE on the RS/Client
     * Expects: With either an access_token or JWT we get access to the protected app (the client doesn't require a signature)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientNone() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverHS256_clientNONE", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * Expects: With an access_token we get access to the protected app
     * With a JWT we get a signature mismatch exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientHS256() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverRS256_clientHS256", "OAuthConfigSample_RS256", "OidcConfigSample_RS256");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * Expects: With either an access_token or JWT we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverRS256_clientRS256", "OAuthConfigSample_RS256", "OidcConfigSample_RS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm NONE on the RS/Client
     * Expects: With either an access_token or JWT we get access to the protected app (the client doesn't require a signature)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientNone() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverRS256_clientNONE", "OAuthConfigSample_RS256", "OidcConfigSample_RS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm NONE on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * Expects: With an access_token we get access to the protected app
     * With a JWT we get a signature mismatch exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerNone_ClientHS256() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverNONE_clientHS256", "OAuthConfigSample_NONE", "OidcConfigSample_NONE");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1778E_SIG_MISSING_FROM_JWT);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm NONE on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * Expects: With an access_token we get access to the protected app
     * With a JWT we geta missing key exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerNone_ClientRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverNONE_clientRS256", "OAuthConfigSample_NONE", "OidcConfigSample_NONE");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm NONE on the OP/Server
     * Signature Algorithm NONE on the RS/Client
     * Expects: With either an access_token or JWT we get access to the protected app (the client doesn't require a signature)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerNone_ClientNone() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_serverNONE_clientNONE", "OAuthConfigSample_NONE", "OidcConfigSample_NONE");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /********************************************** Key and Trust Store variations ****************************************/

    /**************
     * NOTE:
     * The following tests use server configurations with key/trust stores that have multiple certificates - so, our testing
     * is demonstrating that we'll get the correct one - we do NOT have a specific test focused on dooing that (we needed
     * to use the keystores this we to test other function and it seems like overkill to add additional tests to target
     * function we're already using/testing)
     */

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * RS/Client has specified a bad value for the SSLRef attribute
     * Expects: With either an access_token or JWT we get access to the protected app (the client clientSecret/sharedKey is used
     * not a cert)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256_Client_Specific_BadSSLRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadSSLRef_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * RS/Client has specified a bad value for the trustRef attribute
     * Expects: With either an access_token or JWT we get access to the protected app (the client clientSecret/sharedKey is used
     * not a cert)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256_Client_Specific_BadTrustRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadTrustRef_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * RS/Client has specified a bad value for the trustAliasName attribute
     * Expects: With either an access_token or JWT we get access to the protected app (the client clientSecret/sharedKey is used
     * not a cert)
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256_Client_Specific_BadTrustAlias() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadTrustAlias_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * RS/Client has specified a bad value for the clientSecret attribute
     * Expects: With an access_token we get access to the protected app
     * With a JWT, we get a signature validation exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256_Client_Specific_BadClientSecret() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_clientBadClientSecret_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                expectations = addMiscExpectations(null, MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message saying the client ID or secret was incorrect.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            } else {
                expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
            }
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1776E_SIGNATURE_VALIDATION);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm HS256 on the OP/Server
     * Signature Algorithm HS256 on the RS/Client
     * RS/Client has specified a bad value for the sharedKey attribute
     * Expects: With an access_token we get access to the protected app
     * With a JWT, we get a signature validation exception
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerHS256_ClientHS256_Client_Specific_BadSharedKey() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_clientBadSharedKey_serverHS256_clientHS256", "OAuthConfigSample_HS256", "OidcConfigSample_HS256");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1776E_SIGNATURE_VALIDATION);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * RS/Client has specified a bad value for the SSLRef attribute
     * Expects: With an access_token we get access to the protected app
     * With a JWT/x509, we get a missing key exception
     * With a JWT/JWK, we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Client_Specific_BadSSLRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadSSLRef_serverRS256_clientRS256", "OAuthConfigSample_RS256_AltCert", "OidcConfigSample_RS256_AltCert");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
                expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
            } else {
                expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
            }
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * RS/Client has specified a bad value for the trustRef attribute
     * Expects: With an access_token we get access to the protected app
     * With a JWT/x509, we get a missing key exception
     * With a JWT/JWK, we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Client_Specific_BadTrustRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadTrustRef_serverRS256_clientRS256", "OAuthConfigSample_RS256_AltCert", "OidcConfigSample_RS256_AltCert");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
                expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
            } else {
                expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
            }
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * RS/Client has specified a bad value for the trustAliasName attribute
     * Expects: With an access_token we get access to the protected app
     * With a JWT/x509, we get a missing key exception
     * With a JWT/JWK, we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Client_Specific_BadTrustAlias() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBadTrustAlias_serverRS256_clientRS256", "OAuthConfigSample_RS256_AltCert", "OidcConfigSample_RS256_AltCert");

        List<validationData> expectations = null;
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
                expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
            } else {
                expectations = addSigMismatchExpectations(null, _testName, updatedTestSettings, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
            }
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * OP/Server has a cert specified that is not correct for the server's config
     * Expects: With a JWT/JWK, we get access to the protected app
     * With an acces_token or JWT/JWK, we will get a failure creating the
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.lang.InvalidKeyException", "java.lang.RuntimeException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException", "java.lang.ClassCastException" })
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Server_Specific_BadCertType() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_serverBad_serverRS256_clientRS256", "OAuthConfigSample_RS256_ServerKeys", "OidcConfigSample_RS256_ServerKeys");

        // unlike the other tests, this should fail for all setups EXCEPT for JWT/JWK
        List<validationData> expectations = null;
        String[] actions;
        if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
            actions = goodActions;
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            actions = Constants.BASIC_AUTHENTICATION_ACTIONS;
            expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.PERFORM_LOGIN, Constants.BAD_REQUEST_STATUS);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.PERFORM_LOGIN, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message saying the OIDC provider couldn't create the token because of a missing signing key.", MessageConstants.CWWKS1456E_JWT_CANNOT_GENERATE_JWT + ".*" + MessageConstants.CWWKS1455E_JWT_BAD_SIGNING_KEY);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.PERFORM_LOGIN, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was an issue due to the signature", MessageConstants.CWOAU0045E_ENDPOINT_CANNOT_WRITE_RESPONSE);

        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm NONE on the OP/Server
     * Signature Algorithm NONE on the RS/Client
     * RS/Client has specified a bad value for the SSLRef attribute
     * Expects: With either an access_token or a JWT we get access to the protected app (there will be no checking done by the
     * RS/client)
     * With a JWT/x509, we get a missing key exception
     * With a JWT/JWK, we get access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void SignatureAttributes2ServerTests_ServerNone_ClientNone_Client_Specific_BadSSLRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_clientBad_serverNONE_clientNONE", "OAuthConfigSample_NONE", "OidcConfigSample_NONE");

        List<validationData> expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * OP/Server has a bad value specified for the keyStoreRef
     * Expects: With a JWT/JWK, we get access to the protected app
     * With an acces_token or JWT/JWK, we will get a failure creating the
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException", "com.ibm.ws.security.jwt.internal.JwtTokenException" })
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Server_Specific_BadKeyStoreRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_serverBadKeyStoreRef_serverRS256_clientRS256", "OAuthConfigSample_RS256_BadKeyStoreRef", "OidcConfigSample_RS256_BadKeyStoreRef");

        // unlike the other tests, this should fail for all setups EXCEPT for JWT/JWK
        List<validationData> expectations = null;
        String[] actions;
        if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
            actions = goodActions;
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            actions = Constants.BASIC_AUTHENTICATION_ACTIONS;
            expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.PERFORM_LOGIN, Constants.BAD_REQUEST_STATUS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was an issue due to the signature", MessageConstants.CWWKS1455E_JWT_BAD_SIGNING_KEY);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.PERFORM_LOGIN, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message saying the OIDC provider couldn't create the token because of a missing signing key.", MessageConstants.CWWKS1456E_JWT_CANNOT_GENERATE_JWT + ".*" + MessageConstants.CWWKS1455E_JWT_BAD_SIGNING_KEY);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * OP/Server has a bad value specified for the trstStoreRef
     * Expects: With either an access_token or JWT we get access to the protected app (the trust store will not be used creating
     * the token)
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException" })
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Server_Specific_BadTrustStoreRef() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_serverBadTrustStoreRef_serverRS256_clientRS256", "OAuthConfigSample_RS256_BadTrustStoreRef", "OidcConfigSample_RS256_BadTrustStoreRef");

        // unlike the other tests, this should fail for all setups EXCEPT for JWT/JWK
        List<validationData> expectations = null;

        expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);
    }

    /**
     * Test with:
     * Signature Algorithm RS256 on the OP/Server
     * Signature Algorithm RS256 on the RS/Client
     * OP/Server has a bad value specified for the keyAliasName
     * Expects: With a JWT/JWK, we get access to the protected app
     * With an acces_token or JWT/JWK, we will get a failure creating the
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException", "com.ibm.ws.security.jwt.internal.JwtTokenException" })
    @Test
    public void SignatureAttributes2ServerTests_ServerRS256_ClientRS256_Server_Specific_BadKeyAliasName() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "helloworld_keyTrust_serverBadKeyAliasName_serverRS256_clientRS256", "OAuthConfigSample_RS256_BadKeyAliasName", "OidcConfigSample_RS256_BadKeyAliasName");

        // unlike the other tests, this should fail for all setups EXCEPT for JWT/JWK
        List<validationData> expectations = null;
        String[] actions;
        if (updatedTestSettings.getRsCertType().equals(Constants.JWK_CERT)) {
            actions = goodActions;
            expectations = addRSProtectedAppExpectations(null, _testName, updatedTestSettings);
        } else {
            actions = Constants.BASIC_AUTHENTICATION_ACTIONS;
            expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.PERFORM_LOGIN, Constants.BAD_REQUEST_STATUS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was an issue due to the signature", MessageConstants.CWWKS1455E_JWT_BAD_SIGNING_KEY);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.PERFORM_LOGIN, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message saying the OIDC provider couldn't create the token because of a missing signing key.", MessageConstants.CWWKS1456E_JWT_CANNOT_GENERATE_JWT + ".*" + MessageConstants.CWWKS1455E_JWT_BAD_SIGNING_KEY);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);
    }

    /**************************************************** Helpers ****************************************************/

    public static void setRealmForValidationType(TestSettings settings) throws Exception {

        if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            http_realm = Constants.BASIC_REALM;
            https_realm = Constants.BASIC_REALM;
        } else {
            http_realm = "http://" + targetISSEndpoint;
            https_realm = "https://" + targetISSHttpsEndpoint;
        }
        defaultISSAccessId = "user:http://" + targetISSEndpoint + "/" + Constants.OIDC_USERNAME;
        defaultISSAccessIdWithHttps = "user:https://" + targetISSHttpsEndpoint + "/" + Constants.OIDC_USERNAME;
        accessIdWithHttpsProvider = "user:https://localhost:" + testOPServer.getHttpDefaultSecurePort().toString() + "/" + Constants.OIDC_ROOT +
                "/endpoint/" + "HttpsRequiredOidcConfigSample" + "/" + Constants.OIDC_USERNAME;

    }

    protected TestSettings updateTestSettings(TestSettings settings, String rsApp, String OAuth_provider, String OIDC_provider) throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, rsApp);

        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OAUTHCONFIGSAMPLE_APP, OAuth_provider, null);
        } else {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OIDCCONFIGSAMPLE_APP, OIDC_provider, null);
        }
        return updatedTestSettings;
    }

    protected List<validationData> addRSProtectedAppExpectations(List<validationData> expectations, String testCase, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, "Accessed Hello World!");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found UnAuthenticated in the App output", null, Constants.HELLOWORLD_UNAUTHENTICATED);

        return expectations;

    }

    protected List<validationData> addSigMismatchExpectations(List<validationData> expectations, String testCase, TestSettings settings, String specificFailure) throws Exception {

        if (expectations == null) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        }
        //		expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, genericTestServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was a mismatch in the signature algorithms", MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH) ;
        //		expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, genericTestServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated", MessageConstants.CWWKS1706E_INVALID_JOSE_COMPACT_SERIALIZATION) ;
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was an issue due to the signature", specificFailure);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found UnAuthenticated in the App output", null, Constants.HELLOWORLD_UNAUTHENTICATED);

        return expectations;

    }

    protected List<validationData> addMiscExpectations(List<validationData> expectations, String specificFailure) throws Exception {

        if (expectations == null) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        }
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there was an issue validating the token", specificFailure);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found UnAuthenticated in the App output", null, Constants.HELLOWORLD_UNAUTHENTICATED);

        return expectations;

    }
}

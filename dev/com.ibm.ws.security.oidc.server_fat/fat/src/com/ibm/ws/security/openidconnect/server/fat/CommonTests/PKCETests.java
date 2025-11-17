/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.server.fat.CommonTests;

import java.security.MessageDigest;
import java.util.List;

// import java.util.Base64;
// import java.util.Base64.Encoder;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MiscStringUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Test class for PKCE. Tests PKCE (code_challenge, code_challenge_method, code_verifier) handling within the OP.
 * PKCE is only supported with the authorization_code flow.
 * These tests are run with just oauth and with oidc.
 * The extending classes will setup the correct correct configuration.
 *
 * Test case names follow the format:
 * PKCETests_<proofKeyFOrCodeExchange_value>_<clientType>_<codeChallenge_setting>_<codeChallengeMethod_setting><_other>
 * PKCETests_<proofKeyFOrCodeExchange_value>_<codeChallenge_setting>_<codeChallengeMethod_setting><_other>
 *
 * @author chrisc
 *
 */
public class PKCETests extends CommonTest {

    private static final Class<?> thisClass = PKCETests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static String targetProvider = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static final String Plain = "plain";
    protected static final String S256 = "S256";
    protected static final String someString = "ThisIsSomeStringThatWeUseToTestWithThatHasMoreThan43Chars";

    /**
     * proofKeyForCodeExchange not specified and no challenge or challenge_method is tested by the hundreds of tests
     * in the other oidc FAT projects - not bothering with any of those tests here
     * proofKeyForCodeExchange NOT specified tests - the default value is false - which means that the
     * code_challenge and code_challenge_method are NOT required
     *
     */
    /**
     * Config:
     * proofKeyForCodeExchange: not specified
     * publicClient: not specified
     * PKCE is NOT required and we're using a private client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotSpecified_privateClient_challengePlain_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_defaults", codeVerifier, codeVerifier, Plain);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: not specified
     * publicClient: not specified
     * PKCE is NOT required and we're using a private client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotSpecified_privateClient_challengeS256_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_defaults", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: not specified
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotSpecified_publicClient_challengePlain_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_omitted_publicClient_true", codeVerifier, codeVerifier, Plain);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: not specified
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotSpecified_publicClient_challengeS256_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_omitted_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * proofKeyForCodeExchange set to true - this means that the PKCE is required
     * Tests that validate various combinations of public/private clients, code_challenge and code_challenge_method values
     *
     */
    /**
     * ----- public Client variations - with code_challenge omitted
     */

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test does NOT pass a code_challenge to the authorization endpoint.
     * Test does NOT pass code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengeOmitted_methodOmitted() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", null, null, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test does NOT pass a code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengeOmitted_methodPlain() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", null, Plain, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test does NOT pass a code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengeOmitted_methodS256() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", null, S256, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test does NOT pass a code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengeOmitted_methodOther() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", null, "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     * ----- public Client variations - with code_challenge plain
     */
    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test does NOT pass code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * The method is omitted, but the default method will be plain
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCERequired_challengePlain_methodOmitted() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, null);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void PKCETests_PKCERequired_challengePlain_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengePlain_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, S256, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengePlain_methodOther() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge that is too short to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengePlainLessthan43_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(42);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge that is too long to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengePlainGreaterThan128_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(129);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * ----- public Client variations - with code_challenge S256
     */
    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test does NOT pass code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * The method is omitted, so, we'll use the default of "plain" and that won't match the challenge that is passed
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengeS256_methodOmitted() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), null, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengeS256_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), Plain, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void PKCETests_PKCERequired_challengeS256_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCERequired_challengeS256_methodOther() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_true_publicClient_true", urlEncodedS256CodeVerifier(codeVerifier), "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge that is too short to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengeS256LessThan43_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(42);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     *
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge that is too long to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCERequired_challengeS256GreaterThan128_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(129);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * proofKeyForCodeExchange set to false - this means that the PKCE is NOT required, but allowed
     * Tests that validate various combinations of public/private clients, code_challenge and code_challenge_method values
     *
     */
    /**
     * ----- public Client variations - with code_challenge omitted
     */

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test does not pass a code_challenge to the authorization endpoint.
     * Test does not pass a code_challenge_method to the authorization endpoint.
     * Test does not pass a code_verifier to the token endpoint
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     * Server doesn't require PKCE and neither key is passed, so, we will not require or use PKCE
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotRequired_challengeOmitted_methodOmitted() throws Exception {

        positiveTestFlow("client_proofKeyForCodeExchange_false_publicClient_true", null, null, null);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test does not pass a code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     * Since the method is specified, we'll require the challenge and enforce PKCE
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeOmitted_methodPlain() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_false_publicClient_true", null, Plain, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test does not pass a code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     * Since the method is specified, we'll require the challenge and enforce PKCE
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeOmitted_methodS256() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_false_publicClient_true", null, S256, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test does not pass a code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeOmitted_methodOther() throws Exception {

        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_false_publicClient_true", null, "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     * ----- public Client variations - with code_challenge plain
     */
    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test does not pass a code_challenge_method to the authorization endpoint.
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     * The default method of plain will be used
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotRequired_challengePlain_methodOmitted() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, codeVerifier, null);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotRequired_challengePlain_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengePlain_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, codeVerifier, S256, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCENotRequired_challengePlain_methodOther() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge that is shorter than the minimum length to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengePlainLessThan43_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(42);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, codeVerifier, Plain, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes a "plain" code_challenge that is longer than the minimum length to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengePlainGreaterThan128_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(129);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, codeVerifier, Plain, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * ----- public Client variations - with code_challenge S256
     */
    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test omits the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeS256_methodOmitted() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), null, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeS256_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), Plain, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_PKCENotRequired_challengeS256_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes an invalid value as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeS256_methodOther() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_authorizationEndpointFailure("client_proofKeyForCodeExchange_false_publicClient_true", urlEncodedS256CodeVerifier(codeVerifier), "otherMethod", MessageConstants.CWOAU0079E_INVALID_CODE_CHALLENGE_METHOD);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge that is shorter than the minimum length to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeS256LessThan43_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(42);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test passes an "S256" code_challenge that is longer than the maximum length to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * Authorization will fail
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_PKCENotRequired_challengeS256GreaterThan128_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(129);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0080E_INVALID_CODE_VERIFIER_LENGTH);

    }

    /**
     * Test mis-match between the authorization and token endpoints in what is used for code_challenge
     */

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     * Test passes the code_challenage as the code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the mismatch
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_codeVerifierInvalidOnTokenEndpoint_badFormat_codeChallengeInsteadOfCodeVerifier() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", urlEncodedS256CodeVerifier(codeVerifier), urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     * Test passes some random string as the code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the mismatch
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_codeVerifierInvalidOnTokenEndpoint_invalidString_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", someString, codeVerifier, Plain, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     * Test passes some random string as the code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the mismatch
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_codeVerifierInvalidOnTokenEndpoint_invalidString_MethodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_true_publicClient_true", someString, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     * Test does not pass a code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the missing code_verifer
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_codeVerifierMissingOnTokenEndpoint_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidRequest("client_proofKeyForCodeExchange_true_publicClient_true", null, codeVerifier, Plain, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     * Test does not pass a code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the missing code_verifer
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void PKCETests_codeVerifierMissingOnTokenEndpoint_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidRequest("client_proofKeyForCodeExchange_true_publicClient_true", null, urlEncodedS256CodeVerifier(codeVerifier), S256, MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: false
     * publicClient: true
     * PKCE is NOT required and we're using a public client.
     * Test does not pass a code_challenge to the authorization endpoint.
     * Test does not pass a code_challenge_method to the authorization endpoint.
     * Test passes a code_verifier to the token endpoint
     *
     * Authorization code is created
     * Token endpoint will fail due to the mismatch with the code_verifer
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_codeVerifierOnTokenEndpoint_afterNoCodeChallengeOrMethodOnAuthorization() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_tokenEndpointFailure_invalidGrant("client_proofKeyForCodeExchange_false_publicClient_true", codeVerifier, null, null, MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test with invalid characters in the plain challenge string
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     * We don't/can't really check for special characters - creator needs to follow documented recommendations
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_codeVerifierConstansInvalidCharacter_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(25) + "?#!" + createCodeVerifier(25);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test with invalid characters in the S256 challenge string
     *
     * Authorization code is created and token endpoint will exchange for an access_token.
     * access_token can be used to access the protected application
     * We don't/can't really check for special characters - creator needs to follow documented recommendations
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_codeVerifierConstansInvalidCharacter_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(25) + "?#!" + createCodeVerifier(25);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Test the lifetime of the code_challenge (should be the same as he auth code)
     */
    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * the token_endpoint fails to create an access_token as the auth_code has expired
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Test
    public void PKCETests_codeChallengeUsedPastLifetime_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_expiredCodeChallenge(codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * the token_endpoint fails to create an access_token as the auth_code has expired
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Test
    public void PKCETests_codeChallengeUsedPastLifetime_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_expiredCodeChallenge(codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Test that the code_verifier can only be used once (really showing that the auth_code is no longer in the cache once used)
     */
    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * the token_endpoint fails to create an access_token as the auth_code was already used and removed from the cache
     *
     * @throws Exception
     */

    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Test
    public void PKCETests_reuseCodeVerifier_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_reuseCodeVerifier(codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * the token_endpoint fails to create an access_token as the auth_code was already used and removed from the cache
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Test
    public void PKCETests_reuseCodeVerifier_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        negativeTestFlow_reuseCodeVerifier(codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Show that the code_verifier (and its associated code_challenge can be used on multiple requests to the authorization
     * endpoint - they're really just random strings)
     *
     * @throws Exception
     */
    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint and token_endpoint can reuse the same code_verifier/code_challenge, so,
     * we'll be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_reuseCodeVerifierAndChallenge_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint and token_endpoint can reuse the same code_verifier/code_challenge, so,
     * we'll be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_reuseCodeVerifierAndChallenge_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);
        positiveTestFlow("client_proofKeyForCodeExchange_true_publicClient_true", codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint and token_endpoint can reuse the same code_verifier/code_challenge, so,
     * we'll be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_useSameCodeVerifierAndChallengeOnMultipleRequests_methodPlain() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow_reuseCodeVerifierAndChallenge(codeVerifier, codeVerifier, Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint and token_endpoint can reuse the same code_verifier/code_challenge, so,
     * we'll be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void PKCETests_useSameCodeVerifierAndChallengeOnMultipleRequests_methodS256() throws Exception {

        String codeVerifier = createCodeVerifier(50);
        positiveTestFlow_reuseCodeVerifierAndChallenge(codeVerifier, urlEncodedS256CodeVerifier(codeVerifier), S256);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes a "plain" code_challenge to the authorization endpoint.
     * Test passes "plain" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint is invoked with a unique code_challenge (twice). The token_endpoint is
     * invoked using the wrong code_verifier
     * The token_endpoint will fail the verification of the code_challenge
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_createMultipleAuthCodes_useWrongCodeVerifierOnTokenEndpoint_methodPlain() throws Exception {

        negativeTestFlow_useWrongCodeVerifier(Plain);

    }

    /**
     * Config:
     * proofKeyForCodeExchange: true
     * publicClient: true
     * PKCE is required and we're using a public client.
     * Test passes an "S256" code_challenge to the authorization endpoint.
     * Test passes "S256" as the code_challenge_method to the authorization endpoint.
     *
     * the authorization_endpoint is invoked with a unique code_challenge (twice). The token_endpoint is
     * invoked using the wrong code_verifier
     * The token_endpoint will fail the verification of the code_challenge
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.InvalidGrantException" })
    @Test
    public void PKCETests_createMultipleAuthCodes_useWrongCodeVerifierOnTokenEndpoint_methodS256() throws Exception {

        negativeTestFlow_useWrongCodeVerifier(S256);

    }

    /****************************************** Helper Methods *************************************/

    /**
     * Create a code_verifier of the specified length
     *
     * @param size
     * @return - the random string of the specified length
     * @throws Exception
     */
    public String createCodeVerifier(int size) throws Exception {

        return MiscStringUtils.getRandom(size);
    }

    /**
     * Create a url encoded SHA-256 ASCII string
     *
     * @param verifier
     *            - the specified verifer
     * @return - the url-encoded SHA-256 ASCII "code_challenge"
     * @throws Exception
     */
    public String urlEncodedS256CodeVerifier(String verifier) throws Exception {

        return urlEncodeVerifier(createS256CodeVerifierDigest(verifier));
    }

    /**
     * Url encode the specified verifier
     *
     * @param verifierArray
     *            - the verifier to url encode
     * @return - the url encoded verifer
     * @throws Exception
     */
    public String urlEncodeVerifier(byte[] verifierArray) throws Exception {

        Log.info(thisClass, _testName, "Digest value: " + verifierArray);
        Log.info(thisClass, _testName, "Digest value: " + verifierArray.toString());
        return Base64.encodeBase64URLSafeString(verifierArray);

    }

    /**
     * SHA-256 ASCII converted verifier
     *
     * @param verifier
     *            - the original verifier
     * @return - the updated verifier
     * @throws Exception
     */
    public byte[] createS256CodeVerifierDigest(String verifier) throws Exception {

        Log.info(thisClass, _testName, "Verifier value: " + verifier);
        byte[] bytes = verifier.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256);
        //        md.update(bytes, 0, bytes.length);
        md.update(bytes);
        byte[] digest = md.digest();

        return digest;
    }

    /**
     * '
     * Create good expectations (to cover the authorizatio and token endpoints as well as helloworld)
     *
     * @param settings
     *            - current test settings
     * @return - test expectations for a good flow
     * @throws Exception
     */
    private List<validationData> setGoodAuthExpectations(TestSettings settings) throws Exception {
        return authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, Constants.WEB_CLIENT_FLOW);
    }

    /**
     * Create updated test settings - update values that vary for the PKCE tests
     *
     * @param clientId
     *            - test client to use
     * @param providerExtension
     *            - extension to add to base test targetProvider
     * @param verifierString
     *            - the code_verifier (will be passed to the token endpoint by the tests)
     * @param challengeString
     *            - the code_challenge (will be passed to the authorization endpoint by the tests)
     * @param challengeMethod
     *            - the code_challenge_method (will be passed to the authorization endpoint by the tests)
     * @return - a copy of updated testSettings
     * @throws Exception
     */
    public TestSettings updateSettingsForChallengeTest(String clientId, String providerExtension, String verifierString, String challengeString, String challengeMethod) throws Exception {

        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, clientId, null, targetProvider, targetProvider + providerExtension, null);
        updatedTestSettings.setCodeVerifier(verifierString);
        updatedTestSettings.setCodeChallenge(challengeString);
        updatedTestSettings.setCodeChallengeMethod(challengeMethod);
        return updatedTestSettings;
    }

    /**
     * Create expectations for a failure with the authorization endpoint where we get an "invalid_request" error
     *
     * @param msg
     *            - the specific msg to look for in the header of the response (we get the failure with in the redirect)
     * @return - expectations for an "invalid_request" failure
     * @throws Exception
     */
    public List<validationData> setAuthorizationEndpointInvalidRequest(String msg) throws Exception {

        String failingAction = Constants.INVOKE_AUTH_ENDPOINT;
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.REDIRECT_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Did not receive invalid_request " + Constants.ERROR_CODE_INVALID_REQUEST + ".", null, Constants.ERROR_CODE_INVALID_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Did not receive " + msg + " in the Header.", null, msg);
        // nothing will show up in the messages.log
        //        validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did NOT find a message in the log indicating that the request was invalid.", msg);

        return expectations;
    }

    /**
     * Create expectations for a failure with the token endpoint where we get an "invalid_request" error
     *
     * @param msg
     *            - the specific msg to look for in the full response - status code will be 400
     * @return - expectations for an "invalid_request" failure
     * @throws Exception
     */
    public List<validationData> setTokenEndpointInvalidRequest(String msg) throws Exception {

        String failingAction = Constants.INVOKE_TOKEN_ENDPOINT;
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive Bad Request in the message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive message " + Constants.ERROR_CODE_INVALID_REQUEST + ".", null, Constants.ERROR_CODE_INVALID_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive message " + msg + ".", null, msg);
        // nothing will show up in the messages.log
        //        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + msg + " error message.", msg);

        return expectations;
    }

    /**
     * Create expectations for a failure with the token endpoint where we get an "invalid_grant" error
     *
     * @param msg
     *            - the specific msg to look for in the full response - status code will be 400
     * @return - expectations for an "invalid_grant" failure
     * @throws Exception
     */
    public List<validationData> setTokenEndpointInvalidGrant(String msg) throws Exception {

        String failingAction = Constants.INVOKE_TOKEN_ENDPOINT;
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive Bad Request in the message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive message " + Constants.ERROR_CODE_INVALID_GRANT + ".", null, Constants.ERROR_CODE_INVALID_GRANT);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive message " + msg + ".", null, msg);
        // nothing will show up in the messages.log
        //        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + msg + " error message.", msg);

        return expectations;
    }

    /**
     * Create expectations for a failure with the token endpoint where we get an unauthorized error because the token is not in
     * the cache - status code will be 401
     *
     * @return - expectations for an unauthorized failure due to the token not being in the cache
     * @throws Exception
     */
    public List<validationData> setTokenEndpointMissingFromCache() throws Exception {

        String failingAction = Constants.INVOKE_TOKEN_ENDPOINT;
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive Unauthorized in the message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive a " + MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE + " message.", null, "\"error\":\"" + Constants.ERROR_CODE_INVALID_GRANT + "\"");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a " + MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE + " error message.", MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        return expectations;
    }

    /**
     * The common steps of a positive test - update the test settings with the specific test case values, create good
     * expectations,
     * invoke the authorization endpoint then the token endpoint and finally invoke the helloworld app using the token that was
     * created
     *
     * @param client
     *            - the oauth client to use
     * @param verifierString
     *            - the code_verifier
     * @param challengeString
     *            - the code_challenge
     * @param challengeMethod
     *            - the code_challenge_method
     * @throws Exception
     */
    public void positiveTestFlow(String client, String verifierString, String challengeString, String challengeMethod) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest(client, "PKCE", verifierString, challengeString, challengeMethod);

        // expect good results
        List<validationData> expectations = setGoodAuthExpectations(updatedTestSettings);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * The common steps of a negative test with an invalid_request failure from the authorization endpoint - update the test
     * settings
     * with the specific test case values, create invalid_request expectations, tell the test framework not to allow the redirect
     * when
     * a 302 status code is returned, invoke the authorization endpoint
     *
     * @param client
     *            - the oauth client to use
     * @param challengeString
     *            - the code_challenge
     * @param challengeMethod
     *            - the code_challenge_method
     * @param msg
     *            - the exact error message that we should get
     * @throws Exception
     */
    public void negativeTestFlow_authorizationEndpointFailure(String client, String challengeString, String challengeMethod, String msg) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest(client, "PKCE", null, challengeString, challengeMethod);

        // expect negative results
        List<validationData> expectations = setAuthorizationEndpointInvalidRequest(msg);

        overrideRedirect();
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * The common steps of a negative test with an invalid_grant failure from the token endpoint - update the test settings
     * with the specific test case values, create invalid_grant expectations, invoke the authorization endpoint and then the
     * token endpoint
     *
     * @param client
     *            - the oauth client to use
     * @param verifierString
     *            - the code_verifier
     * @param challengeString
     *            - the code_challenge
     * @param challengeMethod
     *            - the code_challenge_method
     * @param msg
     *            - the exact error message that we should get
     * @throws Exception
     */
    public void negativeTestFlow_tokenEndpointFailure_invalidGrant(String client, String verifierString, String challengeString, String challengeMethod, String msg) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest(client, "PKCE", verifierString, challengeString, challengeMethod);

        // expect negative results
        List<validationData> expectations = setTokenEndpointInvalidGrant(msg);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * The common steps of a negative test with an invalid_request failure from the token endpoint - update the test settings
     * with the specific test case values, create invalid_request expectations, invoke the authorization endpoint and then the
     * token endpoint
     *
     * @param client
     *            - the oauth client to use
     * @param verifierString
     *            - the code_verifier
     * @param challengeString
     *            - the code_challenge
     * @param challengeMethod
     *            - the code_challenge_method
     * @param msg
     *            - the exact error message that we should get
     * @throws Exception
     */
    public void negativeTestFlow_tokenEndpointFailure_invalidRequest(String client, String verifierString, String challengeString, String challengeMethod, String msg) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest(client, "PKCE", verifierString, challengeString, challengeMethod);

        // expect negative results
        List<validationData> expectations = setTokenEndpointInvalidRequest(msg);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Test that we can not use the same auth_code/code_challenge more than once
     *
     * @param verifierString
     *            - code_verifier
     * @param challengeString
     *            - code_challenge
     * @param challengeMethod
     *            - challenge_method
     * @throws Exception
     */
    public void negativeTestFlow_reuseCodeVerifier(String verifierString, String challengeString, String challengeMethod) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest("client_proofKeyForCodeExchange_true_publicClient_true", "PKCE", verifierString, challengeString, challengeMethod);

        // expect good results
        List<validationData> authExpectations = setGoodAuthExpectations(updatedTestSettings);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations);

        // exchange the auth_code for an access_token (code_verifier and code_challenge will be used in the process)
        List<validationData> goodTokenExpectations = vData.addSuccessStatusCodesForActions(Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS);
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, goodTokenExpectations, response, null);

        // try the exchange again - show that it fails
        List<validationData> badTokenExpectations = setTokenEndpointMissingFromCache();

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, badTokenExpectations, response, null);

    }

    /**
     * Method uses the same code_challenge to create multiple auth_codes (all use the same challenge). Then, it shows that each
     * can be verified
     * by the token_endpoint (as the correct/same verifier string is passed each time)
     *
     * @param verifierString
     *            - verifier string
     * @param challengeString
     *            - verifier string, or calculated challenge
     * @param challengeMethod
     *            - the challenge method - plain or S256
     * @throws Exception
     */
    public void positiveTestFlow_reuseCodeVerifierAndChallenge(String verifierString, String challengeString, String challengeMethod) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest("client_proofKeyForCodeExchange_true_publicClient_true", "PKCE", verifierString, challengeString, challengeMethod);

        // expect good results
        List<validationData> authExpectations = setGoodAuthExpectations(updatedTestSettings);

        // Create the conversation object which will maintain state for us
        WebConversation wc1 = new WebConversation();
        WebResponse response1 = genericOP(_testName, wc1, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations);
        // Use the same challenge
        WebConversation wc2 = new WebConversation();
        WebResponse response2 = genericOP(_testName, wc2, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations);
        // Use the same challenge - again
        WebConversation wc3 = new WebConversation();
        WebResponse response3 = genericOP(_testName, wc3, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations);

        // exchange the auth_code for an access_token (code_verifier and code_challenge will be used in the process)
        List<validationData> goodTokenExpectations = vData.addSuccessStatusCodesForActions(Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS);
        genericOP(_testName, wc2, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, goodTokenExpectations, response2, null);

        genericOP(_testName, wc1, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, goodTokenExpectations, response1, null);

        genericOP(_testName, wc3, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, goodTokenExpectations, response3, null);

    }

    /**
     * Method creates 2 code_verifiers and then creates auth_codes from challenages based on those verifiers.
     * It uses the wrong verifier when it invokes the token endpoint and expects a mismatch failure
     *
     * @param verifierString
     *            - verifier string
     * @param challengeString
     *            - verifier string, or calculated challenge
     * @param challengeMethod
     *            - the challenge method - plain or S256
     * @throws Exception
     */
    public void negativeTestFlow_useWrongCodeVerifier(String challengeMethod) throws Exception {

        String verifierString1 = createCodeVerifier(50);
        String challengeString1 = verifierString1;
        if (challengeMethod.equals(S256)) {
            challengeString1 = urlEncodedS256CodeVerifier(verifierString1);
        }
        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings1 = updateSettingsForChallengeTest("client_proofKeyForCodeExchange_true_publicClient_true", "PKCE", verifierString1, challengeString1, challengeMethod);

        // expect good results
        List<validationData> authExpectations1 = setGoodAuthExpectations(updatedTestSettings1);
        // Create the conversation object which will maintain state for us
        WebConversation wc1 = new WebConversation();
        // create the auth_code
        WebResponse response1 = genericOP(_testName, wc1, updatedTestSettings1, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations1);
        // now we have 1 auth_code where we used challengeString1

        // create a new verifier and challenge
        String verifierString2 = createCodeVerifier(50);
        String challengeString2 = verifierString2;
        if (challengeMethod.equals(S256)) {
            challengeString2 = urlEncodedS256CodeVerifier(verifierString2);
        }
        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings2 = updateSettingsForChallengeTest("client_proofKeyForCodeExchange_true_publicClient_true", "PKCE", verifierString2, challengeString2, challengeMethod);

        // expect good results
        List<validationData> authExpectations2 = setGoodAuthExpectations(updatedTestSettings2);
        // Create the conversation object which will maintain state for us
        WebConversation wc2 = new WebConversation();
        // create the auth_code
        WebResponse response2 = genericOP(_testName, wc2, updatedTestSettings2, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations2);
        // now we have another auth_code where we used challengeString2

        // Now, try to use  code_verifier1 with code_challenge2 and vice-versa
        // expect negative results
        List<validationData> tokenExpectations = setTokenEndpointInvalidGrant(MessageConstants.CWOAU0081E_CODE_VERIFIER_CHALLENGE_MISMATCH);

        // pass updatedTestSettings2 (has the second code_verifier) to the token endpoint call that will have the first code_challenge
        genericOP(_testName, wc1, updatedTestSettings2, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, tokenExpectations, response1, null);

        genericOP(_testName, wc2, updatedTestSettings1, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, tokenExpectations, response2, null);

    }

    /**
     * Method creates an auth_code using a code_challenge with a short lifetime. Try to use it after sleeping
     *
     * @param verifierString
     *            - verifier string
     * @param challengeString
     *            - verifier string, or calculated challenge
     * @param challengeMethod
     *            - the challenge method - plain or S256
     * @throws Exception
     */
    public void negativeTestFlow_expiredCodeChallenge(String verifierString, String challengeString, String challengeMethod) throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = updateSettingsForChallengeTest("client_lifetime", "PKCELifetime", verifierString, challengeString, challengeMethod);

        updatedTestSettings.setRSProtectedResource(updatedTestSettings.getRSProtectedResource().replace("snoop", "snooping"));

        // expect good results
        List<validationData> authExpectations = setGoodAuthExpectations(updatedTestSettings);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS_WITH_BASIC_AUTH, authExpectations);

        // sleep beyond the lifetime of the code_challenge (actually the lifetime of the auth_code)
        helpers.testSleep(10);

        // expect negative results
        List<validationData> tokenExpectations = setTokenEndpointMissingFromCache();

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_TOKEN_NOJSP_ONLY_ACTIONS, tokenExpectations, response, null);

    }
}

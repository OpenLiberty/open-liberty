/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.SSO.clientTests.PKCEAndPrivateKeyJwt;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.SSO.clientTests.commonTools.PKCEPrivateKeyJwtCommonTooling;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run Private Key Jwt client tests - These are tests that use either an openidConnectClient or
 * Social oidcLogin and an OP to provide authorization functionality. The tests stub out the token endpoint which will just log
 * the request used to invoke it. The tests will then search for the different request parameters recorded in the log. They will
 * validate that the proper parms were passed and that the content of those parms is what was expected (based on the config used
 * by that test). This includes parsing the private key jwt and making sure that all of the required claims are included and
 * are correct.
 * PKCE and Private Key Jwt function are independent - just testing to prove that.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class PKCEAndPrivateKeyJwtClientTests extends PKCEPrivateKeyJwtCommonTooling {

    public static Class<?> thisClass = PKCEAndPrivateKeyJwtClientTests.class;

    /**
     * Set expectations for this test flow - in both positive and negative flows using the test token endpoint, the flow will
     * terminate at the return from the token endpoint. This will result in a 401 status and a common failure message.
     *
     * @return - common expectations for this test class
     * @throws Exception
     */

    public List<validationData> setPKCEPrivateKeyJwtCommonExpectations(String challengeMethod) throws Exception {

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = addPKCECommonExpectations(expectations, challengeMethod);

        return expectations;
    }

    /**
     * Process a positive test case flow - expected behavior based on the authMethod
     *
     * @param updatedTestSettings
     *            - the test setting values to use for this test instance
     * @param authMethod
     *            - the auth method used by the client
     * @throws Exception
     */
    public void positiveTestWithPrivateKey(TestSettings updatedTestSettings, String challenge) throws Exception {

        WebClient webClient = getAndSaveWebClientWithLongerTimeout(true);

        List<validationData> expectations = setPKCEPrivateKeyJwtCommonExpectations(challenge);

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        validateTokenRequest(updatedTestSettings, AuthMethod.PRIVATE_KEY_JWT);
    }

    /******************************* Tests *********************************/
    /**
     * Test with the client config using the signature algorithm of RS256, proofKey is set to false in the OP,
     * pkceCodeChallengeMethod is set to S256 and and a private key jwtis configured with RS256.
     * OP:
     * proofKeyForCodeExchange="false" or not set (using the default)
     * Client:
     * pkceCodeChallengeMethod="S256"
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PKCEAndPKCEAndPrivateKeyJwtClientTests_accessTokenUsesRS256_proofKeyFalse_S256_privateKeyJwtUsesRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("proofKeyFalse_RS256", Constants.SIGALG_RS256, "proofKeyFalse_RS256_S256_RS256");

        positiveTestWithPrivateKey(updatedTestSettings, S256);

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void PKCEAndPKCEAndPrivateKeyJwtClientTests_accessTokenUsesRS256_proofKeyFalse_S256_privateKeyJwtUsesES384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("proofKeyFalse_RS256", Constants.SIGALG_ES384, "proofKeyFalse_RS256_S256_ES384");

        positiveTestWithPrivateKey(updatedTestSettings, S256);

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PKCEAndPKCEAndPrivateKeyJwtClientTests_accessTokenUsesRS256_proofKeyFalse_plain_privateKeyJwtUsesRS384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("proofKeyFalse_RS256", Constants.SIGALG_ES512, "proofKeyFalse_RS256_Plain_ES512");

        positiveTestWithPrivateKey(updatedTestSettings, PLAIN);

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PKCEAndPKCEAndPrivateKeyJwtClientTests_accessTokenUsesRS256_proofKeyTrue_S256_privateKeyJwtUsesRS512() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("proofKeyTrue_RS256", Constants.SIGALG_RS512, "proofKeyTrue_RS256_S256_RS512");

        positiveTestWithPrivateKey(updatedTestSettings, S256);

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PKCEAndPKCEAndPrivateKeyJwtClientTests_accessTokenUsesRS256_proofKeyTruee_plain_privateKeyJwtUsesES256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("proofKeyTrue_RS256", Constants.SIGALG_ES256, "proofKeyTrue_RS256_Plain_ES256");

        positiveTestWithPrivateKey(updatedTestSettings, PLAIN);

    }

}

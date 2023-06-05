/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.SSO.clientTests.PKCE;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.SSO.clientTests.commonTools.PKCEPrivateKeyJwtCommonTooling;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run PKCE client tests - end to end tests using and OIDC client and an OP with
 * proofKeyForCodeExchange set to true or not set (uses the default value of false)
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class PKCEClientTests extends PKCEPrivateKeyJwtCommonTooling {

    public static Class<?> thisClass = PKCEClientTests.class;

    protected static boolean firstFFDCInstance = true;

    /**
     * Process a positive test case flow when a challenge should be included
     *
     * @param app
     *            - the app to invoke
     * @param challengeMethod
     *            - the type of challenge (S256, plain, disabled)
     * @throws Exception
     */
    public void positiveTestWithChallenge(String app, String challengeMethod) throws Exception {

        WebClient webClient = getAndSaveWebClientWithLongerTimeout(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the test app.", null, updatedTestSettings.getTestURL());
        expectations = addPKCECommonExpectations(expectations, challengeMethod);

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * Process a positive test case flow when a challenge should not be included
     *
     * @param app
     *            - the app to invoke
     * @throws Exception
     */
    public void positiveTestWithoutChallenge(String app) throws Exception {

        WebClient webClient = getAndSaveWebClientWithLongerTimeout(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the test app.", null, updatedTestSettings.getTestURL());
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Should NOT have found \"code_challenge\' in the WASReqURL cookie but did.", null, "code_challenge");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Should NOT have found \"code_challenge_method\' in the WASReqURL cookie but did.", null, "code_challenge_method");

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    /**
     * Process a negative test case flow when a challenge should not be included - this tests when the server requires the
     * challenge and the client is configured to not include it.
     *
     * @param app
     *            - the app to invoke
     * @throws Exception
     */
    public void negativeTestWithoutChallenge(String app) throws Exception {

        WebClient webClient = getAndSaveWebClientWithLongerTimeout(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        if (updatedTestSettings.getFlowType().equals(Constants.RP_FLOW)) {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.FORBIDDEN_STATUS);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Response message should have contained the " + Constants.FORBIDDEN + " message.", null, Constants.FORBIDDEN);
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain a message stating that the code_challenge was missing.", MessageConstants.CWWKS1557E_REDIRECT_URI_CONTAINED_ERROR + ".*" + MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".*code_challenge.*");
        } else {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.UNAUTHORIZED_STATUS);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Response message should have contained the " + Constants.UNAUTHORIZED_MESSAGE + " message.", null, Constants.UNAUTHORIZED_MESSAGE);
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain a message stating that the code_challenge was missing.", com.ibm.ws.security.fat.common.social.MessageConstants.CWWKS5495E_INVALID_SOCIAL_REQUEST + ".*" + MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".*code_challenge.*");
        }
        if (firstFFDCInstance) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain a message stating that the code_challenge was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".*code_challenge.*");
        }
        firstFFDCInstance = false;

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

    /******************************* Tests *********************************/

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to S256.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_S256() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_RS256_S256", S256);

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_RS256_Plain", PLAIN);

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to disabled.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_disabled() throws Exception {

        positiveTestWithoutChallenge("proofKeyFalse_RS256_disabled");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client does not set
     * pkceCodeChallengeMethod.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_na() throws Exception {

        positiveTestWithoutChallenge("proofKeyFalse_RS256_na");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to S256.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_S256() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_HS256_S256", S256);

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_HS256_Plain", PLAIN);

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to disabled.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_disabled() throws Exception {

        positiveTestWithoutChallenge("proofKeyFalse_HS256_disabled");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client does not set
     * pkceCodeChallengeMethod.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_na() throws Exception {

        positiveTestWithoutChallenge("proofKeyFalse_HS256_na");

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to S256.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_RS256_S256() throws Exception {

        positiveTestWithChallenge("proofKeyTrue_RS256_S256", S256);

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_RS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyTrue_RS256_Plain", PLAIN);

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to disabled.
     * Both client and server are using RS256 as the signature algorithm.
     * We should fail to access the protected app - the caller should receive a 403 status and
     * the OP should log a message stating that the code_challenge was missing.
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_RS256_disabled() throws Exception {

        negativeTestWithoutChallenge("proofKeyTrue_RS256_disabled");

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client does not set
     * pkceCodeChallengeMethod.
     * Both client and server are using RS256 as the signature algorithm.
     * We should fail to access the protected app - the caller should receive a 403 status and
     * the OP should log a message stating that the code_challenge was missing.
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_RS256_na() throws Exception {

        negativeTestWithoutChallenge("proofKeyTrue_RS256_na");

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to S256.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyTrue_HS256_S256() throws Exception {

        positiveTestWithChallenge("proofKeyTrue_HS256_S256", S256);

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyTrue_HS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyTrue_HS256_Plain", PLAIN);

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to disabled.
     * Both client and server are using RS256 as the signature algorithm.
     * We should fail to access the protected app - the caller should receive a 403 status and
     * the OP should log a message stating that the code_challenge was missing.
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_HS256_disabled() throws Exception {

        negativeTestWithoutChallenge("proofKeyTrue_HS256_disabled");

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client does not set
     * pkceCodeChallengeMethod.
     * Both client and server are using RS256 as the signature algorithm.
     * We should fail to access the protected app - the caller should receive a 403 status and
     * the OP should log a message stating that the code_challenge was missing.
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Mode(TestMode.LITE)
    @Test
    public void PKCEClientTests_proofKeyTrue_HS256_na() throws Exception {

        negativeTestWithoutChallenge("proofKeyTrue_HS256_na");

    }

}

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
package com.ibm.ws.security.SSO.clientTests.PKCE;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
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
public class PKCEClientTests extends CommonTest {

    public static Class<?> thisClass = PKCEClientTests.class;
    public static TestServer clientServer = null;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static boolean firstFFDCInstance = true;

    /**
     * Process a positive test case flow when a challenge should be included
     *
     * @param app
     *            - the app to invoke
     * @param challenge
     *            - the type of challenge (S256 or plain)
     * @throws Exception
     */
    public void positiveTestWithChallenge(String app, String challenge) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the test app.", null, updatedTestSettings.getTestURL());
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found code_challenge in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge.*");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found code_challenge_method=S256 in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge_method=" + challenge + ".*");

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

        WebClient webClient = getAndSaveWebClient(true);

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

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        if (updatedTestSettings.getFlowType().equals(Constants.RP_FLOW)) {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.FORBIDDEN_STATUS);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Response message should have contained the " + Constants.FORBIDDEN + " message.", null, Constants.FORBIDDEN);
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
    //    //    @BeforeClass
    //    //    public static void setUp() throws Exception {
    //    //
    //    //        firstFFDCInstance = true;
    //    //
    //    //        useLdap = false;
    //    //        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
    //    //
    //    //        List<String> startMsgs = new ArrayList<String>();
    //    //        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
    //    //
    //    //        List<String> opStartMsgs = new ArrayList<String>();
    //    //        opStartMsgs.add("CWWKS1631I.*");
    //    //
    //    //        List<String> opExtraApps = new ArrayList<String>();
    //    //        opExtraApps.add(SocialConstants.OP_SAMPLE_APP);
    //    //
    //    //        String[] propagationTokenTypes = rsTools.chooseTokenSettings(SocialConstants.OIDC_OP);
    //    //        String tokenType = propagationTokenTypes[0];
    //    //        String certType = propagationTokenTypes[1];
    //    //        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);
    //    //
    //    //        socialSettings = new SocialTestSettings();
    //    //        testSettings = socialSettings;
    //    //
    //    //        // Start the OIDC OP server
    //    //        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.op.pkce", "op_server_PKCE.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true, tokenType, certType);
    //    //        //Start the OIDC Social server and setup default values
    //    //        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.social.pkce", "server_LibertyOP_PKCE.xml", SocialConstants.GENERIC_SERVER, null, SocialConstants.DO_NOT_USE_DERBY, startMsgs);
    //    //
    //    //        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP);
    //    //
    //    //        setGenericVSSpeicificProviderFlags(GenericConfig, "server_LibertyOP_basicTests_oidc_usingSocialConfig");
    //    //
    //    //        socialSettings = updateLibertyOPSettings(socialSettings);
    //    //        socialSettings.setUserName("testuser");
    //    //
    //    //    }
    //    //
    //    /**
    //     * Process a positive test case flow when a challenge should be included
    //     *
    //     * @param app
    //     *            - the app to invoke
    //     * @param challenge
    //     *            - the type of challenge (S256 or plain)
    //     * @throws Exception
    //     */
    //    public abstract void positiveTestWithChallenge(String app, String challenge) throws Exception;
    //
    //    //        throw new Exception("positiveTestWithChallenge has not been overriden");
    //    //        WebClient webClient = getAndSaveWebClient(true);
    //    //
    //    //        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
    //    //        updatedSocialTestSettings.setProtectedResource(genericTestServer.getHttpsString() + "/formlogin/simple/" + app);
    //    //
    //    //        List<validationData> expectations = vData.addSuccessStatusCodes();
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not land on the test app.", null, updatedSocialTestSettings.getProtectedResource());
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_COOKIE, SocialConstants.STRING_MATCHES, "Should have found code_challenge in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge.*");
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_COOKIE, SocialConstants.STRING_MATCHES, "Should have found code_challenge_method=S256 in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge_method=" + challenge + ".*");
    //    //
    //    //        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    //
    //    //    }
    //
    //    /**
    //     * Process a positive test case flow when a challenge should not be included
    //     *
    //     * @param app
    //     *            - the app to invoke
    //     * @throws Exception
    //     */
    //    public abstract void positiveTestWithoutChallenge(String app) throws Exception;
    //
    //    //        throw new Exception("positiveTestWithoutChallenge has not been overriden");
    //
    //    //        WebClient webClient = getAndSaveWebClient(true);
    //    //
    //    //        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
    //    //        updatedSocialTestSettings.setProtectedResource(genericTestServer.getHttpsString() + "/formlogin/simple/" + app);
    //    //
    //    //        List<validationData> expectations = vData.addSuccessStatusCodes();
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not land on the test app.", null, updatedSocialTestSettings.getProtectedResource());
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_COOKIE, SocialConstants.STRING_DOES_NOT_CONTAIN, "Should NOT have found \"code_challenge\' in the WASReqURL cookie but did.", null, "code_challenge");
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_COOKIE, SocialConstants.STRING_DOES_NOT_CONTAIN, "Should NOT have found \"code_challenge_method\' in the WASReqURL cookie but did.", null, "code_challenge_method");
    //    //
    //    //        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    //
    //    //    }
    //
    //    /**
    //     * Process a negative test case flow when a challenge should not be included - this tests when the server requires the
    //     * challenge and the client is configured to not include it.
    //     *
    //     * @param app
    //     *            - the app to invoke
    //     * @throws Exception
    //     */
    //    public abstract void negativeTestWithoutChallenge(String app) throws Exception;
    //
    //    //        throw new Exception("negativeTestWithoutChallenge has not been overriden");
    //
    //    //        WebClient webClient = getAndSaveWebClient(true);
    //    //
    //    //        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
    //    //        updatedSocialTestSettings.setProtectedResource(genericTestServer.getHttpsString() + "/formlogin/simple/" + app);
    //    //
    //    //        List<validationData> expectations = vData.addSuccessStatusCodes(null, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN);
    //    //        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
    //    //        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Response message should have contained the " + SocialConstants.UNAUTHORIZED_MESSAGE + " message.", null, SocialConstants.UNAUTHORIZED_MESSAGE);
    //    //        if (firstFFDCInstance) {
    //    //            expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain a message stating that the code_challenge was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".*code_challenge.*");
    //    //        }
    //    //        firstFFDCInstance = false;
    //    //        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain a message stating that the code_challenge was missing.", MessageConstants.CWWKS5495E_INVALID_SOCIAL_REQUEST + ".*" + MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".*code_challenge.*");
    //    //
    //    //        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    //
    //    //    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to S256.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_S256() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_RS256_S256", "S256");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_RS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_RS256_Plain", "plain");

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

        positiveTestWithChallenge("proofKeyFalse_HS256_S256", "S256");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_HS256_Plain", "plain");

    }

    /**
     * Test with proofKeyForCodeExchange not set in the OP, so it uses the default value of false. The client does not set
     * pkceCodeChallengeMethod.
     * Both client and server are using RS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyFalse_HS256_na() throws Exception {

        positiveTestWithChallenge("proofKeyFalse_RS256_Plain", "plain");

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

        positiveTestWithChallenge("proofKeyTrue_RS256_S256", "S256");

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

        positiveTestWithChallenge("proofKeyTrue_RS256_Plain", "plain");

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

        positiveTestWithChallenge("proofKeyTrue_HS256_S256", "S256");

    }

    /**
     * Test with proofKeyForCodeExchange is set to true in the OP. The client sets
     * pkceCodeChallengeMethod to plain.
     * Both client and server are using HS256 as the signature algorithm.
     * We should successfully access the protected application.
     */
    @Test
    public void PKCEClientTests_proofKeyTrue_HS256_plain() throws Exception {

        positiveTestWithChallenge("proofKeyTrue_HS256_Plain", "plain");

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

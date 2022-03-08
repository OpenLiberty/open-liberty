/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.backchannelLogout.fat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.base64url.Base64;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**********************************************************************************/
/****** TODO: We may need to add an OP for use with some of the sid tests ******/
/****** The validation should ensure that the sid is valid ******/
/****** We may also need to use update to use an OP for tests ******/
/**********************************************************************************/

/**
 * This test class contains tests that validate the proper behavior of the back channel logout endpoint's logout token validation.
 * The tests in this class will test with logout tokens that contain both valid and invalid content.
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class LogoutTokenValidationTests extends BackChannelLogoutCommonTests {

    public static Class<?> thisClass = LogoutTokenValidationTests.class;

    // Repeat tests using the OIDC and Social endpoints
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC))
            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(SocialConstants.SOCIAL));

    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_orig.xml", Constants.GENERIC_SERVER, apps, Constants.DO_NOT_USE_DERBY,
                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        }
        testSettings.setFlowType(Constants.RP_FLOW);

    }

    /**
     * This method contains the common steps that a test case validating behavior when a required claim is missing.
     *
     * @param claimToOmit
     *            specifies the claim that should be removed from the builder before the logout token is built
     * @throws Exception
     */
    public void genericMissingRequiredClaimTest(String claimToOmit) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParm_removingOne(claimToOmit);

        List<validationData> expectations = setMissingBackChannelLogoutRequestClaim(claimToOmit);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This method contains the common steps that a test case validating behavior when a claim contains an invalid value.
     *
     * @param claim
     *            the claim to update
     * @param value
     *            the value to update for a claim
     * @param errorMsg
     *            an additional error message to validate (Tests using this method always expects the messages CWWKS1541E and
     *            CWWKS1543E - this is an additional message to check in the messages.log)
     * @throws Exception
     */
    public void genericInvalidClaimTest(String claim, Object value, String errorMsg) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParm_addOrOverrideOne(claim, value);

        List<validationData> expectations = setInvalidBackChannelLogoutRequest(errorMsg);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This method contains the common steps that a test case validating behavior when for extra claims, or claims with updated
     * values (that are valid)
     *
     * @param claim
     *            the claim to add or update
     * @param value
     *            the value to set for the claim
     * @throws Exception
     */
    public void genericAddedUpdatedClaimsTest(String claim, Object value) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParm_addOrOverrideOne(claim, value);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This method contains the common steps that a test case validating behavior when a logout token is signed with a signature
     * algorithm other than HS256. This method can test both positive and negative cases (negative cases will expect CWWKS1777E in
     * messages.log). We'll determine if the test should expect a good status code if the signature algorithm in the client config
     * matches what the logout token is signed with. The client id's in the config use the signature algorithm in their name and
     * this method will use that to determine if it should expect a 200 status, or 400 and error messages.
     *
     * @param client
     *            the client we should be testing with
     * @param alg
     *            the signature algorithm to sign the logout token with
     * @throws Exception
     */
    public void genericSignatureAlgTest(String client, String alg) throws Exception {
        genericSignatureAlgTest(client, alg, null);
    }

    /**
     * See the comment of the same named method without "failureMsg" - this method let's us override the default error message
     * (CWWKS1777E) searched for.
     *
     * @param client
     *            the client we should be testing with
     * @param alg
     *            the signature algorithm to sign the logout token with
     * @param failureMsg
     *            the error message we should search for instead of CWWKS1777E.
     * @throws Exception
     */
    public void genericSignatureAlgTest(String client, String alg, String failureMsg) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = null;
        if (alg.startsWith("HS")) {
            b = setHSASignedBuilderForLogoutToken(alg);
        } else {
            b = setNonHSASignedBuilderForLogoutToken(alg);
        }

        List<endpointSettings> parms = createParmFromBuilder(b, false);

        // here's a bit of a hack - using the same common test code for both positive and negative tests.
        // building the token and invoking the endpoint is the same in both cases - the only difference
        // is the expectations - so, if the alg used to build the token is not what the client uses, build
        // negative expectations, otherwise build positve.  The client names in the config contain the alg name
        // so, that's how we can tell
        List<validationData> expectations = null;
        if (client.contains(alg) && (failureMsg == null)) {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);
        } else {
            if (failureMsg != null) {
                expectations = setInvalidBackChannelLogoutRequest(failureMsg);
            } else {
                expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            }
        }

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This method contains the common steps that a test case validating behavior when a logout token is encrypted. This method
     * can test both positive and negative cases (negative cases will expect CWWKS6056E in messages.log). We'll determine if the
     * test should expect a good status code if the key in the client config matches what the logout token is encrypted with. The
     * client id's in the config use the algorithm in their name, the token builder finds the key based on the algorithm and this
     * method will use that to determine if it should expect a 200 status, or 400 and error messages.
     *
     * @param client
     *            the client we should be testing with
     * @param alg
     *            the signature algorithm to sign the logout token with
     * @throws Exception
     */
    public void genericEncryptionTest(String client, String alg) throws Exception {

        genericEncryptionTest(client, alg, null);
    }

    /**
     * See the comment of the same named method without "failureMsg" - this method let's us override the default error message
     * (CWWKS6056E) searched for.
     *
     * @param client
     *            the client we should be testing with
     * @param alg
     *            the signature algorithm to sign the logout token with
     * @param failureMsg
     *            the error message we should search for instead of CWWKS6056E.
     * @throws Exception
     */
    public void genericEncryptionTest(String client, String alg, String failureMsg) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = setNonHSAEncryptedBuilderForLogoutToken(alg);

        List<endpointSettings> parms = createParmFromBuilder(b, true);

        // here's a bit of a hack - using the same common test code for both positive and negative tests.
        // building the token and invoking the endpoint is the same in both cases - the only difference
        // is the expectations - so, if the alg used to build the token is not what the client uses, build
        // negative expectations, otherwise build positve.  The client names in the config contain the alg name
        // so, that's how we can tell
        List<validationData> expectations = null;
        if (client.contains(alg) && (failureMsg == null)) {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);
        } else {
            if (failureMsg != null) {
                expectations = setInvalidBackChannelLogoutRequest(failureMsg);
            } else {
                expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
            }
        }

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that a logout token with valid content successfully validates through the product runtime.
     * A valid logout token is a logout token that contains all required claims. THose claims need to be valid when checked
     * against the RP/client configuration.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_validTokenContent() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This test makes sure that the call to the backchannelLogout endpoint fails when a logout token is omiitted.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_logoutToken() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = null;

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1542E_BACK_CHANNEL_MISSING_LOGOUT_TOKEN);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);
    }

    /**
     * This tests makes sure that the call to the backchannelLogout endpoint fails when a string is passed as the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_logoutToken_is_just_a_String() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, "justAString");

        List<validationData> expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * This tests that a backchannelLogout is NOT performed when backchannelLogoutSupported="false" is specified in the config.
     * This setting should prevent the logout from being done
     *
     * @throws Exception
     */
    // TODO runtime does not currently check this - update test when it does
    @Test
    public void LogoutTokenValidationTests_backchannelLogout_not_supported() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientBackChannelLogoutNotSupported");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);
        // TOD add proper expectations

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout fails when we omit the required iss claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_iss() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_ISSUER);

    }

    /**
     * Test that the backchannelLogout fails when we specify the iss claim that is invalid for the oidc client config.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iss() throws Exception {

        genericInvalidClaimTest(Constants.PAYLOAD_ISSUER, "someBadValue", MessageConstants.CWWKS1751E_INVALID_ISSUER);

    }

    /**
     * Test that the backchannelLogout fails when we omit the required aud claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_aud() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_AUDIENCE);

    }

    /**
     * Test that the backchannelLogout fails when we specify the aud claim that is invalid for the oidc client config.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_aud() throws Exception {

        genericInvalidClaimTest(Constants.PAYLOAD_AUDIENCE, "someBadValue", MessageConstants.CWWKS1754E_ID_TOKEN_AUD_MISMATCH);

    }

    /**
     * Test that the backchannelLogout fails when we omit the required iat claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_iat() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);

    }

    /**
     * Test that the backchannelLogout fails when we specify an iat that is way out in the future
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat() throws Exception {

        // set an invalid iat (future date - 2/21/2035)
        genericInvalidClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 2055696852, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    /**
     * Test that the backchannelLogout fails when we specify an iat that is just in the future (just beyond the clockskew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_future_beyond_clockSkew() throws Exception {

        long clockSkew = 5;
        long tomorrow = System.currentTimeMillis() / 1000 + minutesToSeconds(clockSkew + 5);

        genericInvalidClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, tomorrow, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    /**
     * Test that the backchannelLogout fails when we specify an iat that is in the future (tomorrow)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_tomorrow() throws Exception {

        long tomorrow = System.currentTimeMillis() / 1000 + hoursToSeconds(24);

        genericInvalidClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, tomorrow, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    /**
     * tool to convert minutes to seconds
     *
     * @param minutes
     *            minutes to convert
     * @return minutes converted to seconds
     * @throws Exception
     */
    public long minutesToSeconds(long minutes) throws Exception {
        return minutes * 60;
    }

    /**
     * tool to convert hours to seconds
     *
     * @param hours
     *            hours to convert
     * @return hours converted to seconds
     * @throws Exception
     */
    public long hoursToSeconds(long hours) throws Exception {
        return hours * 60 * 60;
    }

    /**
     * Test shows that even though the token is "old", the runtime doens't check its value
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_yesterday_without_exp() throws Exception {

        long yesterday = System.currentTimeMillis() / 1000 - hoursToSeconds(24);

        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, yesterday);

    }

    /**
     * Test shows that even though the token is "old", the runtime doens't check its value - as long as
     * the exp (+clockskew) is still after now
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_yesterday_with_exp() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        long yesterday = System.currentTimeMillis() / 1000 - hoursToSeconds(24);
        long soon = System.currentTimeMillis() / 1000 + minutesToSeconds(2);

        JWTTokenBuilder b = setDefaultLogoutToken();

        b.setClaim(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, yesterday);
        b.setClaim(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, soon);
        String logoutToken = b.build();

        Log.info(thisClass, _testName, "claims: " + b.getJsonClaims());
        Log.info(thisClass, _testName, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test shows that even though the token is very, very "old", the runtime doens't check its value
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_1971_without_exp() throws Exception {

        // 02/21/1971
        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 36010452);

    }

    /**
     * Test shows that even though the token is very, very "old", the runtime doens't check its value - as long as
     * the exp (+clockskew) is still after now
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_1971_with_exp() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        // 02/21/1971
        long soon = System.currentTimeMillis() / 1000 + hoursToSeconds(2);

        JWTTokenBuilder b = setDefaultLogoutToken();

        b.setClaim(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 36010452);
        b.setClaim(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, soon);
        String logoutToken = b.build();

        Log.info(thisClass, _testName, "claims: " + b.getJsonClaims());
        Log.info(thisClass, _testName, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the logout token is valid when it contains an exp that is not earlier than now (+clockskew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_optional_exp_notExpired() throws Exception {

        long stillValid = System.currentTimeMillis() / 1000 + hoursToSeconds(2);
        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, stillValid);

    }

    /**
     * Test that the backchannelLogout fails when we specify an exp that is prior to the current time (just beyond the clockskew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_optional_exp_expired() throws Exception {

        long expired = System.currentTimeMillis() / 1000 - hoursToSeconds(1); // time is outside clockskew
        genericInvalidClaimTest(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, expired, "InvalidJwtException");
    }

    /**
     * Test that the backchannelLogout fails when we omit the required jti claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_jti() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_JWTID);

    }

    /**
     * Test that a logout token can not be used multiple times - try to reuse the same token (same jti value) and show that the
     * backchannelLogout request fails.
     *
     * @throws Exception
     */
    // TODO - jti is not currently checked by the runtime @Test
    public void LogoutTokenValidationTests_replay_required_jti() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

        // reuse the same token (same jti) and expect it to fail
        List<validationData> replayExpectations = setInvalidBackChannelLogoutRequest();
        //TODO look for the correct message        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the token contained a dis-allowed nonce claim.", MessageConstants.CWWKS1549E_BACK_CHANNEL_LOGOUT_NONCE_CLAIM);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, replayExpectations, testSettings);

    }

    /**
     * Test that the backchannelLogout fails when we omit the required events claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_events() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_EVENTS);

    }

    /**
     * Test that the backchannelLogout fails when we omit the "http://schemas.openid.net/event/backchannel-logout" value from the
     * events claim
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_events() throws Exception {

        JSONObject events = new JSONObject();
        events.put("claim1", new JSONObject());
        events.put("claim2", "claim2_value");
        events.put("claim3", "claim3_value");
        events.put("claim4", "claim4_value");
        events.put("claim5", "claim5_value");
        genericInvalidClaimTest(Constants.PAYLOAD_EVENTS, events, MessageConstants.CWWKS1548E_EVENTS_CLAIM_MISSING_REQUIRED_MEMBER);

    }

    /**
     * Test that the backchannelLogout fails when the events claim is not json
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_events_not_json() throws Exception {

        genericInvalidClaimTest(Constants.PAYLOAD_EVENTS, "JustAString", MessageConstants.CWWKS1547E_EVENTS_CLAIM_NOT_JSON);

    }

    /**
     * Test that the backchannelLogout success when the events claim contains extra values
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_events_json_contains_extra_values() throws Exception {

        JSONObject events = new JSONObject();
        events.put("http://schemas.openid.net/event/backchannel-logout", new JSONObject());
        events.put("claim1", new JSONObject());
        events.put("claim2", "claim2_value");
        events.put("claim3", "claim3_value");
        events.put("claim4", "claim4_value");
        events.put("claim5", "claim5_value");

        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_EVENTS, events);

    }

    /**
     * Test that the backchannelLogout request succeeds when sub is included in the logout token
     *
     * @throws Exception
     */
    // test really is the same as LogoutTokenValidationTests_validTokenContent
    @Test
    public void LogoutTokenValidationTests_include_optional_sub_omit_sid() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout request succeeds when sid is included in the logout token instead of sub
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_include_optional_sid_omit_sub() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParm_removingOne_addOrOverrideOne(Constants.PAYLOAD_SUBJECT, Constants.PAYLOAD_SESSION_ID, "xx");

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout fails when we omit both the sub and sid claims from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_optional_sub_and_sid() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParm_removingOne(Constants.PAYLOAD_SUBJECT); // sid is not added by default by test tooling

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the sub and sid claims are both missing.", MessageConstants.CWWKS1546E_BACK_CHANNEL_LOGOUT_MISSING_SUB_AND_SID_CLAIMS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the logout token is valid when it contains bot sub and sid claims (that are valid)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_include_optional_sub_and_sid() throws Exception {

        // our default test logout token contains sub already, so, just add sid
        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_SESSION_ID, "xx");

    }

    /**
     * Test that the backchannelLogout fails when the sub claim content is invalid and the sid is omitted
     *
     * @throws Exception
     */
    // TODO runtime doesn't check at this time @Test
    public void LogoutTokenValidationTests_invalid_sub_omit_sid() throws Exception {

        genericInvalidClaimTest(Constants.PAYLOAD_SUBJECT, "someBadValue", "");

    }

    /**
     * Test that the backchannelLogout fails when the sid claim content is invalid and the sub is omitted
     *
     * @throws Exception
     */
    // TODO runtime doesn't check at this time @Test
    public void LogoutTokenValidationTests_invalid_sid_omit_sub() throws Exception {

    }

    /**
     * Test that the backchannelLogout fails when the logout token does not have a sid, but the client configure requires a sid.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_omit_sid_when_config_requires_it() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientRequiresSid");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm(); // default claims do not include sid

        List<validationData> expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS1551E_BACK_CHANNEL_LOGOUT_MISSING_SID);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout does not perform a logout when the request contains a logout token and that logout token
     * does not have a sid.
     * (The fact that the client doesn;t support backchannelLogouts should take priority over the client requiring a sid)
     * TODO: may need a check or allow a warning during server startup for this config
     *
     * @throws Exception
     */
    // TODO runtime isn't checking the backchannel supported attribute - test will need to be updated when it does
    @Test
    public void LogoutTokenValidationTests_backchannelLogout_not_supported_sid_required() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientBackChannelLogoutNotSupportedRequiresSid");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm(); // default claims do not include sid

        List<validationData> expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS1551E_BACK_CHANNEL_LOGOUT_MISSING_SID);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout fails when we specify the nonce claim that is NOT allowed in a lgoutout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_include_prohibited_nonce() throws Exception {

        genericInvalidClaimTest(Constants.PAYLOAD_NONCE, "xx", MessageConstants.CWWKS1549E_BACK_CHANNEL_LOGOUT_NONCE_CLAIM);

    }

    /**
     * Test that the backchannelLogout succeeds when extra, but not prohibited claims are included in the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_include_ignored_claims() throws Exception {

        JSONObject acrValue = new JSONObject();
        JSONArray testJsonArray = new JSONArray();
        testJsonArray.add("xx");
        testJsonArray.add("yy");

        acrValue.put("essential", true);
        acrValue.put("values", testJsonArray);

        Map<String, Object> claimMap = new HashMap<String, Object>() {
            {

                // TODO - once the runtime validation is updated, re-enable the next line and remove the last check in this method
                //put(Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, 2056387597);
                put("email", "joe@something.com");
                put(Constants.PAYLOAD_AUTHZ_TIME_IN_SECS, 478550797);
                put(Constants.PAYLOAD_AUTHORIZED_PARTY, "noOne");
                put(Constants.PAYLOAD_AT_HASH, new String(Base64.encode("myDogHasFleas".getBytes())));
                put(Constants.PAYLOAD_CLASS_REFERENCE, acrValue);
                put(Constants.PAYLOAD_METHODS_REFERENCE, testJsonArray);
                put(Constants.PAYLOAD_GROUP, testJsonArray);
                put(Constants.PAYLOAD_GROUPS, testJsonArray);
                put(Constants.PAYLOAD_USER_PRINCIPAL_NAME, "someFakeUser");
                put(Constants.PAYLOAD_TOKEN_TYPE, Constants.TOKEN_TYPE_BEARER);

            }
        };

        for (Map.Entry<String, Object> entry : claimMap.entrySet()) {

            genericAddedUpdatedClaimsTest(entry.getKey(), entry.getValue());
        }

        // TODO - once the runtime validation is updated, remove the next line and restore the line in the map above
        genericInvalidClaimTest(Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, 2056387597, "InvalidJwtException");

    }

    /*****************************************
     * Signature tests
     *****************************************/
    /**
     * The RP config specifies HS256 and the token is signed using HS256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS256() throws Exception {
        genericSignatureAlgTest("clientSignHS256", Constants.SIGALG_HS256);
    }

    /**
     * The rp config specifies HS256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS256", Constants.SIGALG_HS512);
    }

    /**
     * The RP config specifies HS384 and the token is signed using HS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS384() throws Exception {
        genericSignatureAlgTest("clientSignHS384", Constants.SIGALG_HS384);
    }

    /**
     * The rp config specifies HS384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS384", Constants.SIGALG_HS256);
    }

    /**
     * The RP config specifies HS512 and the token is signed using HS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS512() throws Exception {
        genericSignatureAlgTest("clientSignHS512", Constants.SIGALG_HS512);
    }

    /**
     * The rp config specifies HS512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS512", Constants.SIGALG_HS384);
    }

    /**
     * The RP config specifies RS256 and the token is signed using RS256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS256() throws Exception {
        genericSignatureAlgTest("clientSignRS256", Constants.SIGALG_RS256);
    }

    /**
     * The rp config specifies RS256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS256", Constants.SIGALG_RS512);
    }

    /**
     * The RP config specifies RS384 and the token is signed using RS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS384() throws Exception {
        genericSignatureAlgTest("clientSignRS384", Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies RS384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS384", Constants.SIGALG_ES512);
    }

    /**
     * The RP config specifies RS512 and the token is signed using RS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS512() throws Exception {
        genericSignatureAlgTest("clientSignRS512", Constants.SIGALG_RS512);
    }

    /**
     * The rp config specifies RS512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS512", Constants.SIGALG_RS256);
    }

    /**
     * The RP config specifies ES256 and the token is signed using ES256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES256() throws Exception {
        genericSignatureAlgTest("clientSignES256", Constants.SIGALG_ES256);
    }

    /**
     * The rp config specifies ES256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES256", Constants.SIGALG_ES384);
    }

    /**
     * The RP config specifies ES384 and the token is signed using ES384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES384() throws Exception {
        genericSignatureAlgTest("clientSignES384", Constants.SIGALG_ES384);
    }

    /**
     * The rp config specifies ES384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES384", Constants.SIGALG_ES256);
    }

    /**
     * The RP config specifies ES512 and the token is signed using ES512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES512() throws Exception {
        genericSignatureAlgTest("clientSignES512", Constants.SIGALG_ES512);
    }

    /**
     * The rp config specifies ES512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES512", Constants.SIGALG_RS384);
    }

    /*****************************************
     * Encryption tests
     *****************************************/

    /**
     * The RP config specifies RS256 and the token is encrypted using RS256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS256() throws Exception {
        genericEncryptionTest("clientSignEncryptRS256", Constants.SIGALG_RS256);
    }

    /**
     * The rp config specifies RS256, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS256_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS256", Constants.SIGALG_RS384);
    }

    /**
     * The RP config specifies RS384 and the token is encrypted using RS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS384() throws Exception {
        genericEncryptionTest("clientSignEncryptRS384", Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies RS384, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS384_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS384", Constants.SIGALG_RS512);
    }

    /**
     * The RP config specifies RS512 and the token is encrypted using RS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS512() throws Exception {
        genericEncryptionTest("clientSignEncryptRS512", Constants.SIGALG_RS512);
    }

    /**
     * The rp config specifies RS512, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS512_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS512", Constants.SIGALG_RS256);
    }

    /**
     * The RP config specifies ES256 and the token is encrypted using ES256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES256() throws Exception {
        genericEncryptionTest("clientSignEncryptES256", Constants.SIGALG_ES256);
    }

    /**
     * The rp config specifies ES256, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES256_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES256", Constants.SIGALG_ES384);
    }

    /**
     * The RP config specifies ES384 and the token is encrypted using ES384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES384() throws Exception {
        genericEncryptionTest("clientSignEncryptES384", Constants.SIGALG_ES384);
    }

    /**
     * The rp config specifies ES384, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES384_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES384", Constants.SIGALG_ES256);
    }

    /**
     * The RP config specifies ES512 and the token is encrypted using ES512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES512() throws Exception {
        genericEncryptionTest("clientSignEncryptES512", Constants.SIGALG_ES512);
    }

    /**
     * The rp config specifies ES512, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES512_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES512", Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies that the token be signed and NOT encrypted - the token is both signed and encrypted - expect the
     * request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_token_encrypted_clientExpects_signed() throws Exception {
        genericEncryptionTest("clientSignRS256", Constants.SIGALG_RS256, MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS);

    }

    /**
     * The rp config specifies that the token be signed and encrypted - the token is both only signed and NOT encrypted - expect
     * the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_token_signed_clientExpects_encrypted() throws Exception {

        genericSignatureAlgTest("clientSignEncryptRS256", Constants.SIGALG_RS256, MessageConstants.CWWKS1537E_JWE_IS_NOT_VALID);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify A192GCM as the content encryption key in the encrypted logout
     * token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_contentEncryptionAlg_A192GCM() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientSignEncryptRS256");

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = setNonHSAEncryptedBuilderForLogoutToken(Constants.ENCRYPT_RS256, JwtConstants.CONTENT_ENCRYPT_ALG_192, JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);

        List<endpointSettings> parms = createParmFromBuilder(b, true);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify RSA-OAEP-256 as the key mgmt key alg in the encrypted logout
     * token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_keyMgmtKeyAlg_256() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientSignEncryptRS256");

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = setNonHSAEncryptedBuilderForLogoutToken(Constants.ENCRYPT_RS256, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256);

        List<endpointSettings> parms = createParmFromBuilder(b, true);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify notJose as the type in the encrypted logout token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_type_notJose() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientSignEncryptRS256");

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = setNonHSAEncryptedBuilderForLogoutToken(Constants.ENCRYPT_RS256);

        List<endpointSettings> parms = createParmFromBuilder(b, true, "notJose", "jwt");

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test that the backchannelLogout fails when we specify not_jwt as the content_type in the encrypted logout token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_contentTyp_notJwt() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("clientSignEncryptRS256");

        WebClient webClient = getAndSaveWebClient(true);

        JWTTokenBuilder b = setNonHSAEncryptedBuilderForLogoutToken(Constants.ENCRYPT_RS256);

        List<endpointSettings> parms = createParmFromBuilder(b, true, "JOSE", "not_jwt");

        List<validationData> expectations = setInvalidBackChannelLogoutRequest(MessageConstants.CWWKS6057E_CTY_NOT_JWT_FOR_NESTED_JWS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

}

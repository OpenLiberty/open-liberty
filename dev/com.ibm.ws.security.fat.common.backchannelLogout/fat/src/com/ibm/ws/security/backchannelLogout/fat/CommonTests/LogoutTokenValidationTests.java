/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.backchannelLogout.fat.CommonTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.base64url.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServerWrapper;

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
    public static final String defaultClient = "clientSignHS256";

    @BeforeClass
    public static void setUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_logoutTokenValidation.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_logoutTokenValidation.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_logoutTokenValidation.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            testSettings.setFlowType("Social_Flow");

        }

    }

    /**
     * This method contains the common steps that a test case validating behavior when a required claim is missing.
     *
     * @param claimToOmit
     *            specifies the claim that should be removed from the builder before the logout token is built
     * @throws Exception
     */
    public void genericMissingRequiredClaimTest(String claimToOmit) throws Exception {

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.unsetClaim(claimToOmit);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setMissingBCLRequestClaimExpectations(claimToOmit);

        invokeBcl(logutOutEndpoint, parms, expectations);

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

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.setClaim(claim, value);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setInvalidBCLRequestExpectations(errorMsg);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * This method contains the common steps that a test case validating behavior when the iat or exp claim contains an invalid value.
     * 
     * @param iat
     *            the iat claim value
     * @param exp
     *            the exp claim value
     * @throws Exception
     */
    public void genericInvalidIatOrExpClaimTest(long iat, long exp) throws Exception {

    	JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.setClaim(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, iat);
        builder.setClaim(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, exp);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

        invokeBcl(logutOutEndpoint, parms, expectations);

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

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.setClaim(claim, value);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Make sure that the signature algorithm config variable is set to an algorithm that the Liberty OP supports.
     * Also make sure that the keyManagmentKeyAlias variable is not set since the Liberty OP does not currently support encryption
     *
     * @param alg
     *            the algorithm variable name root to update
     * @throws Exception
     */
    public void restoreServerVars(String alg) throws Exception {

        Map<String, String> vars = new HashMap<String, String>();

        // Make sure the sig alg is set the "default" (which is a sig alg that the OP understands)
        //(reminder: OP supports a subset of what the RP, RS and Social client do)
        if (!(alg.equals(Constants.SIGALG_HS256) || alg.equals(Constants.SIGALG_RS256))) {
            if (alg.startsWith("HS")) {
                vars.put(alg, Constants.SIGALG_HS256);
            } else {
                vars.put(alg, Constants.SIGALG_RS256);
            }
        }
        // disable encryption
        vars.put(alg + "_keyMgmtAlias", "");

        updateServerSettings(clientServer, vars);
    }

    /**
     * Sets the signature algorithm, and keyManagmentKeyAlais to the values that the test needs for the validation of the
     * logout_token
     *
     * @param alg
     *            the algorithm variable root name and value to set (ie: configVar: ES512 now set to ES512 and ES512_keyMgmtAlias
     *            set to ES512)
     * @throws Exception
     */
    public void setServerVars(String alg) throws Exception {

        Map<String, String> vars = new HashMap<String, String>();

        vars.put(alg, alg);
        vars.put(alg + "_keyMgmtAlias", alg);

        updateServerSettings(clientServer, vars);
    }

    /**
     * This method is called to perform the generic steps of a signature algorithm test (steps are the same, configs vary between
     * tests). It invokes genericSignOrEncryptTest adding parms to the call to indicate this is not encrypting and to use the
     * default failure message for negative tests
     *
     * @param client
     *            the client we should be testing with
     * @param configAlg
     *            the signature algorithm that the client will use when invoking the back channel logout
     * @param tokenAlg
     *            the signature algorithm to sign the logout_token with
     * @throws Exception
     */
    public void genericSignatureAlgTest(String client, String configAlg, String tokenAlg) throws Exception {
        genericSignOrEncryptTest(false, client, configAlg, tokenAlg, null);
    }

    /**
     * This method is called to perform the generic steps of a signature algorithm test (steps are the same, configs vary between
     * tests). It invokes genericSignOrEncryptTest adding parms to the call to indicate this is not encrypting
     *
     * @param client
     *            the client we should be testing with
     * @param configAlg
     *            the signature algorithm that the client will use when invoking the back channel logout
     * @param tokenAlg
     *            the signature algorithm to sign the logout_token with
     * @param failureMsg
     *            the error message we should search for instead of CWWKS1777E.
     * @throws Exception
     */
    public void genericSignatureAlgTest(String client, String configAlg, String tokenAlg, String failureMsg) throws Exception {
        genericSignOrEncryptTest(false, client, configAlg, tokenAlg, failureMsg);
    }

    /**
     * This method is called to perform the generic steps of a encryption test (steps are the same, configs vary between tests).
     * It invokes genericSignOrEncryptTest adding parms to the call to indicate this is encrypting and to use the default failure
     * message for negative tests
     *
     * @param client
     *            the client we should be testing with
     * @param configAlg
     *            the signature algorithm that the client will use when invoking the back channel logout
     * @param tokenAlg
     *            the signature algorithm to sign the logout_token with
     * @throws Exception
     */
    public void genericEncryptionTest(String client, String configAlg, String tokenAlg) throws Exception {

        genericSignOrEncryptTest(true, client, configAlg, tokenAlg, null);
    }

    /**
     * This method is called to perform the generic steps of a encryption test (steps are the same, configs vary between tests).
     * It invokes genericSignOrEncryptTest adding parms to the call to indicate this is encrypting
     *
     * @param client
     *            the client we should be testing with
     * @param configAlg
     *            the signature algorithm that the client will use when invoking the back channel logout
     * @param tokenAlg
     *            the signature algorithm to sign the logout_token with
     * @param failureMsg
     *            the error message we should search for instead of CWWKS1777E.
     * @throws Exception
     */
    public void genericEncryptionTest(String client, String configAlg, String tokenAlg, String failureMsg) throws Exception {
        genericSignOrEncryptTest(true, client, configAlg, tokenAlg, failureMsg);
    }

    /**
     * This method contains the common steps to to test valid signed or signed/encrypted logout_tokens.
     * The Liberty OP does not currently support all of the signature algorithms that our clients do. It also does not encrypt
     * any tokens. So, to create the id_token/access_token, we set the client config appropriately for the OP config. Once
     * we have the id_token, we reconfigure the client to the configuration needed to test the logout_token.
     * Steps:
     * 1) create the back channel uri
     * 2) update the client config to the value that the OP uses (Replace the signature algorithm and truststore alias to the
     * value that the OP uses. Also remove the keyManagementKeyAlias value so the client won't expect a JWE)
     * 3) Access the appropriate protected app and retrieve the id_token - use that id_token to populate a JWTTokenBuilder object
     * 4) update the client config to what is needed to test the logout_token (Set the signature algorithm, truststore alias and
     * keyManagementKeyAlias values)
     * 5) update the JWTTokenBuilder opject with the signing and encrypting values we're trying to test
     * -a) set encrypt and sign values in the builder if testing encryption
     * -b) set HS signing values in the builder if testing sigAlg only and the alg is HS*
     * -c) set RS signing values in the builder if testing sigAlg only and the alg is RS*
     * 6) create the bcl parms from the builder (just the logout_token parm with value used when invoking the bcl endpoint)
     * -a) use the builder to create a JWT or JWE from the JWTTokenBuilder object
     * -b) create an EndpointSettings list of "logout_token":<logout_token>
     * 7) Set expectations
     * -a) if both the config and token algs are the same and caller didn't pass a failure msg, expect success (200 status code)
     * -b) if caller passed in a failureMsg, expect a 400 status and the failureMsg
     * -c) otherwise expect a mismatch failure msg (encrypt: CWWKS6056E, sign: CWWKS1777E)
     * 8) Finally invoke the back channel logout - normal tooling handles invocation, response checking
     *
     *
     * @param encrypt
     *            is test doing encryption (other wise just signing)
     * @param client
     *            the client we're testing with (used to build the url to the protected app as well as the back channel logout
     *            endpoint)
     * @param configAlg
     *            the alg that the client is to be configured with when we make the logout request
     * @param tokenAlg
     *            the alg that will be used to build the logout_token (signing and if needed encrypting)
     * @param failureMsg
     *            for negative tests, the failure message if different than the defaults to look for
     * @throws Exception
     */
    public void genericSignOrEncryptTest(boolean encrypt, String client, String configAlg, String tokenAlg, String failureMsg) throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        // set server config variables back to values that will allow us to "login" for the specified client.
        // The Liberty OP doesn't support all of the signature algorithms that the RP does.  It also does not encrypt tokens, so, we
        // modify the client config to something that allows us to login and once we have the id_token, we reconfig the client
        // to the configuration that we need to have to test the logout_token validation
        restoreServerVars(configAlg);

        // the client configs are originally setup with RS256 (the only signature algs that we support in the OP right now)
        // We'll login using the RP/OP with those "default" algorithms, then reconfig the client before trying to verify the logout_token
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        // update the builder with the sig alg that we want to build the logout_Token with
        setServerVars(configAlg);

        if (encrypt) {
            builder = updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, tokenAlg);
        } else {
            if (tokenAlg.startsWith("HS")) {
                builder = updateLogoutTokenBuilderWithHSASignatureSettings(builder, tokenAlg);
            } else {
                builder = updateLogoutTokenBuilderWithNonHSASignatureSettings(builder, tokenAlg);
            }

        }

        List<endpointSettings> parms = createParmFromBuilder(builder, encrypt);

        List<validationData> expectations = null;
        if (client.contains(tokenAlg) && (failureMsg == null)) {
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);
        } else {
            if (failureMsg != null) {
                expectations = setInvalidBCLRequestExpectations(failureMsg);
            } else {
                if (encrypt) {
                    expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
                } else {
                    expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
                }
            }
        }

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /***************************** Tests *****************************/

    /**
     * Test that a logout token with valid content successfully validates through the product runtime.
     * A valid logout token is a logout token that contains all required claims. THose claims need to be valid when checked
     * against the RP/client configuration.
     *
     * @throws Exception
     */
    // same test as LogoutTokenValidationTests_include_optional_sub_and_sid - @Test
    public void LogoutTokenValidationTests_validTokenContent() throws Exception {

        String client = defaultClient;
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * This test makes sure that the call to the backchannelLogout endpoint fails when a logout token is omiitted.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void LogoutTokenValidationTests_missing_logoutToken() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = null;

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1542E_BACK_CHANNEL_MISSING_LOGOUT_TOKEN);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * This tests makes sure that the call to the backchannelLogout endpoint fails when a string is passed as the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_logoutToken_is_just_a_String() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, "justAString");

        List<validationData> expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    //    /**
    //     * This tests that a backchannelLogout is NOT performed when backchannelLogoutSupported="false" is specified in the config.
    //     * This setting should prevent the logout from being done
    //     *
    //     * @throws Exception
    //     */
    //    // TODO The current RP config is not using the backchannelLogoutSupported attribute - so, bcl requests will always be supported/allowed @Test
    //    public void LogoutTokenValidationTests_backchannelLogout_not_supported() throws Exception {
    //
    //        String client = "clientBackChannelLogoutNotSupported";
    //        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);
    //
    //        String logutOutEndpoint = buildBackchannelLogoutUri(client);
    //
    //        List<endpointSettings> parms = createParmFromBuilder(builder);
    //
    //        // TODO add proper expectations
    //        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);
    //
    //        invokeBcl(logutOutEndpoint, parms, expectations);
    //
    //    }

    /**
     * Test that the backchannelLogout fails when we omit the required iss claim from the logout token.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
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

        long iat = System.currentTimeMillis() / 1000 + hoursToSeconds(24 * 365 * 10);
        long exp = iat + minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout fails when we specify an iat that is just in the future (just beyond the clockskew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_future_beyond_clockSkew() throws Exception {

        long clockSkew = 5;
        long iat = System.currentTimeMillis() / 1000 + minutesToSeconds(clockSkew + 5); // just beyond clockSkew
        long exp = iat + minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout fails when we specify an iat that is in the future (tomorrow)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_tomorrow() throws Exception {

        long iat = System.currentTimeMillis() / 1000 + hoursToSeconds(24); // tomorrow
        long exp = iat + minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

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
    public void LogoutTokenValidationTests_invalid_iat_yesterday() throws Exception {

        long yesterday = System.currentTimeMillis() / 1000 - hoursToSeconds(24);

        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, yesterday);

    }

    /**
     * Test shows that even though the token is very, very "old", the runtime doens't check its value
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_iat_1971() throws Exception {

        // 02/21/1971
        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 36010452);

    }
    
    /**
     * Test that the backchannelLogout fails when we omit the required exp claim from the logout token.
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_missing_required_exp() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS);

    }
    
    /**
     * Test that the backchannelLogout fails when we specify an exp that is before the iat
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_exp_before_iat() throws Exception {

    	long iat = System.currentTimeMillis() / 1000;
        long exp = iat - hoursToSeconds(1);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout fails when we specify an exp that is way in the past
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_exp_1971() throws Exception {

        long exp = 36010452; // 02/21/1971
        long iat = exp - minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout fails when we specify an exp that is from yesterday
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_exp_yesterday() throws Exception {

        long exp = System.currentTimeMillis() / 1000 - hoursToSeconds(24);
        long iat = exp - minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout fails when we specify an exp that is just in the past (just beyond the clockskew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_exp_past_beyond_clockSkew() throws Exception {

        long clockSkew = 5;
        long exp = System.currentTimeMillis() / 1000 - minutesToSeconds(clockSkew);
        long iat = exp - minutesToSeconds(2);

        genericInvalidIatOrExpClaimTest(iat, exp);

    }

    /**
     * Test that the backchannelLogout passes when we specify an exp that is the current time (due to the clowSkew)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_valid_exp_now() throws Exception {

        long exp = System.currentTimeMillis() / 1000;
        long iat = exp - minutesToSeconds(2);

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.setClaim(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, iat);
        builder.setClaim(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, exp);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout passes when we specify an exp that is tomorrow
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_valid_exp_tomorrow() throws Exception {

        long tomorrow = System.currentTimeMillis() / 1000 + hoursToSeconds(24);
        
        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, tomorrow);

    }

    /**
     * Test that the backchannelLogout passes when we specify an exp that is way in the future
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_valid_exp_farInFuture() throws Exception {

        long futureTime = System.currentTimeMillis() / 1000 + hoursToSeconds(24 * 365 * 10);

        genericAddedUpdatedClaimsTest(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, futureTime);

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
     * Test that a logout token can not be used multiple times - try to reuse the same token (same jti value) and show that
     * the backchannelLogout request fails.
     */
    @Test
    public void LogoutTokenValidationTests_replay_required_jti() throws Exception {

        String client = defaultClient;
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

        // reuse the same token (same jti) and expect it to fail
        List<validationData> replayExpectations = setInvalidBCLRequestExpectations();
        replayExpectations = validationTools.addMessageExpectation(clientServer, replayExpectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the token contained a jti claim that has already been used.", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR + ".*" + MessageConstants.CWWKS1551E_LOGOUT_TOKEN_DUP_JTI);

        invokeBcl(logutOutEndpoint, parms, replayExpectations);

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
    @Mode(TestMode.FULL)
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
    @Test
    public void LogoutTokenValidationTests_include_optional_sub_omit_sid() throws Exception {

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        // remove sid
        builder.unsetClaim(Constants.PAYLOAD_SESSION_ID);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout request succeeds when sid is included in the logout token instead of sub
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_include_optional_sid_omit_sub() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        //  sid is in by default now
        builder.unsetClaim(Constants.PAYLOAD_SUBJECT);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout fails when we omit both the sub and sid claims from the logout token.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void LogoutTokenValidationTests_missing_optional_sub_and_sid() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        builder.unsetClaim(Constants.PAYLOAD_SESSION_ID);
        builder.unsetClaim(Constants.PAYLOAD_SUBJECT);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setInvalidBCLRequestExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the sub and sid claims are both missing.", MessageConstants.CWWKS1546E_BACK_CHANNEL_LOGOUT_MISSING_SUB_AND_SID_CLAIMS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the logout token is valid when it contains both sub and sid claims (that are valid)
     * This is the "default"
     *
     * @throws Exception
     */
    // test really is the same as LogoutTokenValidationTests_validTokenContent (that test has been disabled)
    @Test
    public void LogoutTokenValidationTests_include_optional_sub_and_sid() throws Exception {

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout fails when the sub claim content is invalid and the sid is omitted
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_sub_omit_sid() throws Exception {

        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        String badSub = "someBadValue";
        builder.setSubject(badSub);
        builder.unsetClaim(Constants.PAYLOAD_SESSION_ID);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setInvalidBCLRequestExpectations(null);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a recent session for the subject couldn't be found.", MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + badSub + ".*" + "sub");

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout fails when the sid claim content is invalid and the sub is omitted
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_invalid_sid_omit_sub() throws Exception {
        JWTTokenBuilder builder = loginAndReturnIdTokenData(defaultClient);
        String badSid = "someBadValue";
        builder.setClaim(Constants.PAYLOAD_SESSION_ID, badSid);
        builder.unsetClaim(Constants.PAYLOAD_SUBJECT);

        String logutOutEndpoint = buildBackchannelLogoutUri(defaultClient);

        List<endpointSettings> parms = createParmFromBuilder(builder);

        List<validationData> expectations = setInvalidBCLRequestExpectations(null);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a recent session for the sid couldn't be found.", MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + badSid + ".*" + "sid");

        invokeBcl(logutOutEndpoint, parms, expectations);
    }

    //    /**
    //     * Test that the backchannelLogout fails when the logout token does not have a sid, but the client configure requires a sid.
    //     *
    //     * @throws Exception
    //     */
    //    // TODO backchannelLogoutSessionRequired is not supported/used in the RP config, so, we'll always allow sid, but never require it as far as the RP is concerned
    //    //@Test
    //    public void LogoutTokenValidationTests_omit_sid_when_config_requires_it() throws Exception {
    //
    //        String client = "clientRequiresSid";
    //        String logutOutEndpoint = buildBackchannelLogoutUri(client);
    //
    //        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);
    //        builder.unsetClaim(Constants.PAYLOAD_SESSION_ID);
    //
    //        List<endpointSettings> parms = createParmFromBuilder(builder);
    //
    //        List<validationData> expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS1551E_BACK_CHANNEL_LOGOUT_MISSING_SID);
    //
    //        invokeBcl(logutOutEndpoint, parms, expectations);
    //
    //    }

    //    /**
    //     * Test that the backchannelLogout does not perform a logout when the request contains a logout token and that logout token
    //     * does not have a sid.
    //     * (The fact that the client doesn;t support backchannelLogouts should take priority over the client requiring a sid)
    //     * TODO: may need a check or allow a warning during server startup for this config
    //     *
    //     * @throws Exception
    //     */
    //    // TODO backchannelLogoutSupported and backchannelLogoutSessionRequired not currently supported/used by the RP
    //    @Test
    //    public void LogoutTokenValidationTests_backchannelLogout_not_supported_sid_required() throws Exception {
    //
    //        String client = "clientBackChannelLogout_NotSupportedRequiresSid";
    //        String logutOutEndpoint = buildBackchannelLogoutUri(client);
    //
    //        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);
    //        builder.unsetClaim(Constants.PAYLOAD_SESSION_ID);
    //
    //        List<endpointSettings> parms = createParmFromBuilder(builder);
    //
    //        List<validationData> expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS1551E_BACK_CHANNEL_LOGOUT_MISSING_SID);
    //
    //        invokeBcl(logutOutEndpoint, parms, expectations);
    //
    //    }

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

        HashMap<String, Object> claimMap = new HashMap<String, Object>() {
            {

                put(Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, 2056387597);
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
        genericSignatureAlgTest("clientSignHS256", Constants.SIGALG_HS256, Constants.SIGALG_HS256);
    }

    /**
     * The rp config specifies HS256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS256", Constants.SIGALG_HS256, Constants.SIGALG_HS512);
    }

    /**
     * The RP config specifies HS384 and the token is signed using HS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS384() throws Exception {
        genericSignatureAlgTest("clientSignHS384", Constants.SIGALG_HS384, Constants.SIGALG_HS384);
    }

    /**
     * The rp config specifies HS384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS384", Constants.SIGALG_HS384, Constants.SIGALG_HS256);
    }

    /**
     * The RP config specifies HS512 and the token is signed using HS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS512() throws Exception {
        genericSignatureAlgTest("clientSignHS512", Constants.SIGALG_HS512, Constants.SIGALG_HS512);
    }

    /**
     * The rp config specifies HS512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_HS512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignHS512", Constants.SIGALG_HS512, Constants.SIGALG_HS384);
    }

    /**
     * The RP config specifies RS256 and the token is signed using RS256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS256() throws Exception {
        genericSignatureAlgTest("clientSignRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS256);
    }

    /**
     * The rp config specifies RS256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS512);
    }

    /**
     * The RP config specifies RS384 and the token is signed using RS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS384() throws Exception {
        genericSignatureAlgTest("clientSignRS384", Constants.SIGALG_RS384, Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies RS384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS384", Constants.SIGALG_RS384, Constants.SIGALG_ES512);
    }

    /**
     * The RP config specifies RS512 and the token is signed using RS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS512() throws Exception {
        genericSignatureAlgTest("clientSignRS512", Constants.SIGALG_RS512, Constants.SIGALG_RS512);
    }

    /**
     * The rp config specifies RS512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_RS512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignRS512", Constants.SIGALG_RS512, Constants.SIGALG_RS256);
    }

    /**
     * The RP config specifies ES256 and the token is signed using ES256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES256() throws Exception {
        genericSignatureAlgTest("clientSignES256", Constants.SIGALG_ES256, Constants.SIGALG_ES256);
    }

    /**
     * The rp config specifies ES256, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES256_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES256", Constants.SIGALG_ES256, Constants.SIGALG_ES384);
    }

    /**
     * The RP config specifies ES384 and the token is signed using ES384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES384() throws Exception {
        genericSignatureAlgTest("clientSignES384", Constants.SIGALG_ES384, Constants.SIGALG_ES384);
    }

    /**
     * The rp config specifies ES384, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES384_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES384", Constants.SIGALG_ES384, Constants.SIGALG_ES256);
    }

    /**
     * The RP config specifies ES512 and the token is signed using ES512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES512() throws Exception {
        genericSignatureAlgTest("clientSignES512", Constants.SIGALG_ES512, Constants.SIGALG_ES512);
    }

    /**
     * The rp config specifies ES512, the token is signed using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_sign_ES512_mismatch() throws Exception {
        genericSignatureAlgTest("clientSignES512", Constants.SIGALG_ES512, Constants.SIGALG_RS384);
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
        genericEncryptionTest("clientSignEncryptRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS256);
    }

    /**
     * The rp config specifies RS256, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS256_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS384);
    }

    /**
     * The RP config specifies RS384 and the token is encrypted using RS384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS384() throws Exception {
        genericEncryptionTest("clientSignEncryptRS384", Constants.SIGALG_RS384, Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies RS384, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS384_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS384", Constants.SIGALG_RS384, Constants.SIGALG_RS512);
    }

    /**
     * The RP config specifies RS512 and the token is encrypted using RS512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS512() throws Exception {
        genericEncryptionTest("clientSignEncryptRS512", Constants.SIGALG_RS512, Constants.SIGALG_RS512);
    }

    /**
     * The rp config specifies RS512, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_RS512_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptRS512", Constants.SIGALG_RS512, Constants.SIGALG_RS256);
    }

    /**
     * The RP config specifies ES256 and the token is encrypted using ES256 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES256() throws Exception {
        genericEncryptionTest("clientSignEncryptES256", Constants.SIGALG_ES256, Constants.SIGALG_ES256);
    }

    /**
     * The rp config specifies ES256, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES256_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES256", Constants.SIGALG_ES256, Constants.SIGALG_ES384);
    }

    /**
     * The RP config specifies ES384 and the token is encrypted using ES384 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES384() throws Exception {
        genericEncryptionTest("clientSignEncryptES384", Constants.SIGALG_ES384, Constants.SIGALG_ES384);
    }

    /**
     * The rp config specifies ES384, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES384_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES384", Constants.SIGALG_ES384, Constants.SIGALG_ES256);
    }

    /**
     * The RP config specifies ES512 and the token is encrypted using ES512 - expect the request to succeed
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES512() throws Exception {
        genericEncryptionTest("clientSignEncryptES512", Constants.SIGALG_ES512, Constants.SIGALG_ES512);
    }

    /**
     * The rp config specifies ES512, the token is encrypted using another algorithm - expect the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_ES512_mismatch() throws Exception {
        genericEncryptionTest("clientSignEncryptES512", Constants.SIGALG_ES512, Constants.SIGALG_RS384);
    }

    /**
     * The rp config specifies that the token be signed and NOT encrypted - the token is both signed and encrypted - expect the
     * request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_token_encrypted_clientExpects_signed() throws Exception {
        genericEncryptionTest("clientSignRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS256, MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS);

    }

    /**
     * The rp config specifies that the token be signed and encrypted - the token is only signed and NOT encrypted - expect
     * the request to fail
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_token_signed_clientExpects_encrypted() throws Exception {

        genericSignatureAlgTest("clientSignEncryptRS256", Constants.SIGALG_RS256, Constants.SIGALG_RS256, MessageConstants.CWWKS1537E_JWE_IS_NOT_VALID);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify A192GCM as the content encryption key in the encrypted logout
     * token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_contentEncryptionAlg_A192GCM() throws Exception {

        String client = "clientSignEncryptRS256";

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        restoreServerVars(Constants.SIGALG_RS256);

        // the client configs are originally setup with RS256 (the only signature algs that we support in the OP right now) - without encryption (since the OP doesn't support that
        // We'll login using the RP/OP with those "default" settings, then reconfig the client before trying to verify the logout_token
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        setServerVars(Constants.SIGALG_RS256);

        builder = updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, Constants.ENCRYPT_RS256, JwtConstants.CONTENT_ENCRYPT_ALG_192, JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);

        List<endpointSettings> parms = createParmFromBuilder(builder, true);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify RSA-OAEP-256 as the key mgmt key alg in the encrypted logout
     * token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_keyMgmtKeyAlg_256() throws Exception {

        String client = "clientSignEncryptRS256";

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        restoreServerVars(Constants.SIGALG_RS256);

        // the client configs are originally setup with RS256 (the only signature algs that we support in the OP right now) - without encryption (since the OP doesn't support that
        // We'll login using the RP/OP with those "default" settings, then reconfig the client before trying to verify the logout_token
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        setServerVars(Constants.SIGALG_RS256);

        builder = updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, Constants.ENCRYPT_RS256, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256);

        List<endpointSettings> parms = createParmFromBuilder(builder, true);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout succeeds when we specify notJose as the type in the encrypted logout token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_type_notJose() throws Exception {

        String client = "clientSignEncryptRS256";

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        restoreServerVars(Constants.SIGALG_RS256);

        // the client configs are originally setup with RS256 (the only signature algs that we support in the OP right now) - without encryption (since the OP doesn't support that
        // We'll login using the RP/OP with those "default" settings, then reconfig the client before trying to verify the logout_token
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        setServerVars(Constants.SIGALG_RS256);

        builder = updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, Constants.ENCRYPT_RS256);

        List<endpointSettings> parms = createParmFromBuilder(builder, true, "notJose", "jwt");

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

    /**
     * Test that the backchannelLogout fails when we specify not_jwt as the content_type in the encrypted logout token's header
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenValidationTests_encrypt_contentTyp_notJwt() throws Exception {
        String client = "clientSignEncryptRS256";

        String logutOutEndpoint = buildBackchannelLogoutUri(client);

        restoreServerVars(Constants.SIGALG_RS256);

        // the client configs are originally setup with RS256 (the only signature algs that we support in the OP right now) - without encryption (since the OP doesn't support that
        // We'll login using the RP/OP with those "default" settings, then reconfig the client before trying to verify the logout_token
        JWTTokenBuilder builder = loginAndReturnIdTokenData(client);

        setServerVars(Constants.SIGALG_RS256);

        builder = updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, Constants.ENCRYPT_RS256);

        List<endpointSettings> parms = createParmFromBuilder(builder, true, "JOSE", "not_jwt");

        List<validationData> expectations = setInvalidBCLRequestExpectations(MessageConstants.CWWKS6057E_CTY_NOT_JWT_FOR_NESTED_JWS);

        invokeBcl(logutOutEndpoint, parms, expectations);

    }

}

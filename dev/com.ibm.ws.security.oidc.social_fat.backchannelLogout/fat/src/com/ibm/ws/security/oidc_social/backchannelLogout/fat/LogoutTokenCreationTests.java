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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test class contains tests that validate that the logout_token is built properly - based on the client config
 *
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class LogoutTokenCreationTests extends BackChannelLogoutCommonTests {

    public static Class<?> thisClass = LogoutTokenCreationTests.class;

    /**
     * Only testing with an RP (oidc client)
     * The OP is what will generate the logout_token and it will do that based on the config that it has
     *
     * TODO - do we want to repeat this with different client stores?
     *
     */

    @AfterClass
    public static void afterClass() {
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + defaultUseLdap);
        useLdap = defaultUseLdap;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // bypass LDAP setup which isn't needed and wastes a good bit of time.
        useLdap = false;
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
                add("backchannelLogoutTestApp");
            }
        };

        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_validateLogoutTokenCreation.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_validateLogoutTokenCreation.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        testSettings.setFlowType(Constants.RP_FLOW);
        // set up the postlogout redirect to call the test app - it will build a response with the logout_token saved from the invocation of the back channel logout request
        // it'll do this to retrieve the logout_token content
        testSettings.setPostLogoutRedirect(clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");

    }

    public JwtClaims getHeaderAlg(String jwtTokenString) throws Exception, InvalidJwtException {

        Log.info(thisClass, "getHeaderAlg", "Original JWS Token String: " + jwtTokenString);
        JwtClaims jwtClaims = new JwtClaims();

        try {

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();

            JwtContext context = jwtConsumer.process(jwtTokenString);
            List<JsonWebStructure> jsonStructures = context.getJoseObjects();
            if (jsonStructures == null || jsonStructures.isEmpty()) {
                throw new Exception("Invalid JsonWebStructure");
            }
            JsonWebStructure jsonStruct = jsonStructures.get(0);

            jwtClaims.setClaim(Constants.HEADER_ALGORITHM, jsonStruct.getAlgorithmHeaderValue());

            Log.info(thisClass, "getHeaderAlg", "JWT consumer populated succeeded! " + jwtClaims);
        } catch (InvalidJwtException e) {
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            Log.info(thisClass, "getHeaderAlg", "Invalid JWT! " + e);
            throw e;
        }

        // debug if ever needed
        //        Map<String, Object> claimMap = jwtClaims.getClaimsMap();
        //        for (Map.Entry<String, Object> entry : claimMap.entrySet()) {
        //            Log.info(thisClass, "getHeaderAlg", "Key = " + entry.getKey() + ", Value = " + entry.getValue());
        //            Log.info(thisClass, "getHeaderAlg", "value of type: " + entry.getValue().getClass().toString());
        //        }

        return jwtClaims;

    }

    public JwtClaims getClaims(String jwtTokenString) throws Exception, InvalidJwtException {

        JwtClaims jwtClaims = null;

        Log.info(thisClass, "getClaims", "Original JWS Token String: " + jwtTokenString);

        try {

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipAllValidators()
                    .setDisableRequireSignature()
                    .setSkipSignatureVerification()
                    .build();

            jwtClaims = jwtConsumer.process(jwtTokenString).getJwtClaims();
            Log.info(thisClass, "getClaims", "JWT consumer populated succeeded! " + jwtClaims);
        } catch (InvalidJwtException e) {
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            Log.info(thisClass, "getClaims", "Invalid JWT! " + e);
            throw e;
        }

        // debug if ever needed
        //        Map<String, Object> claimMap = jwtClaims.getClaimsMap();
        //        for (Map.Entry<String, Object> entry : claimMap.entrySet()) {
        //            Log.info(thisClass, "getClaims", "Key = " + entry.getKey() + ", Value = " + entry.getValue());
        //            Log.info(thisClass, "getClaims", "value of type: " + entry.getValue().getClass().toString());
        //        }

        return jwtClaims;

    }

    /**
     *
     * @param idTokenData
     * @param logoutToken
     * @param updatedTestSettings
     * @throws Exception
     */
    public void validateLogoutTokenContent(String id_token, String logout_token, boolean sidRequired) throws Exception {

        // validate header
        validateLogoutTokenHeader(id_token, logout_token);
        // validate payload
        validateLogoutTokenClaims(id_token, logout_token, sidRequired);
    }

    public void validateLogoutTokenHeader(String id_token, String logout_token) throws Exception {

        JwtClaims idTokenClaims = getHeaderAlg(id_token);
        JwtClaims logoutTokenClaims = getHeaderAlg(logout_token);

        mustHaveString(Constants.HEADER_ALGORITHM, idTokenClaims, logoutTokenClaims, true);

    }

    public void validateLogoutTokenClaims(String id_token, String logout_token, boolean sidRequired) throws Exception {

        JwtClaims idTokenClaims = getClaims(id_token);
        JwtClaims logoutTokenClaims = getClaims(logout_token);

        mustHaveString(Constants.PAYLOAD_ISSUER, idTokenClaims, logoutTokenClaims, true);

        mustHaveArray(Constants.PAYLOAD_AUDIENCE, idTokenClaims, logoutTokenClaims);
        mustHaveLong(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, idTokenClaims, logoutTokenClaims, true);
        mustHaveString(Constants.PAYLOAD_JWTID, idTokenClaims, logoutTokenClaims, false); // maybe change interface to pass alternately obtained jti value to validate against

        if (sidRequired) {
            mustHaveString(Constants.PAYLOAD_SESSION_ID, idTokenClaims, logoutTokenClaims, true);
        } else {
            // we need to have either sid or sub
            mustHaveOneOrTheOtherString(Constants.PAYLOAD_SUBJECT, Constants.PAYLOAD_SESSION_ID, idTokenClaims, logoutTokenClaims);
        }

        mustHaveEvents(logoutTokenClaims);
        mustNotHave(Constants.PAYLOAD_NONCE, logoutTokenClaims);

    }

    //    protected void mustHaveString(String key, Map<String, Object> idTokenDataClaims, Map<String, Object> logoutClaims, boolean mustBeInIdToken) throws Exception {
    protected void mustHaveString(String key, JwtClaims idTokenClaims, JwtClaims logoutTokenClaims, boolean mustBeInIdToken) throws Exception {

        String logoutTokenValue = logoutTokenClaims.getStringClaimValue(key);
        String idTokenValue = idTokenClaims.getStringClaimValue(key);

        if (logoutTokenValue == null) {
            fail("Key: " + key + " was missing in the logout_token");
        } else {
            // all of the required keys should be in the id_token, so we should be able to compare the value,
            // but check for a null in the idToken - just in case...
            if (idTokenValue == null) {
                if (mustBeInIdToken) {
                    fail("Key: " + key + " was missing in the id_token (nothing to validate the logout_token value against");
                } else {
                    Log.info(thisClass, "mustHaveString", "Key: " + key + " was missing in the id_token, so we can not validate the value in the logout_token - all we can do is make sure that claim exists.");
                }
            } else {
                if (!logoutTokenValue.toString().equals(idTokenValue.toString())) {
                    if (mustBeInIdToken) {
                        fail("Was epecting Key: " + key + " with value: " + idTokenValue + ", but found: " + logoutTokenValue);
                    } else {
                        Log.info(thisClass, "mustHaveString", "The value for the key in the id_token and logout_token did not match - but, for claims like jti, they should not");
                    }
                }
            }
        }

    }

    protected void mustHaveLong(String key, JwtClaims idTokenClaims, JwtClaims logoutTokenClaims, boolean mustBeInIdToken) throws Exception {

        Long logoutTokenValue = null;
        try {
            logoutTokenValue = logoutTokenClaims.getClaimValue(key, Long.class);
        } catch (Exception e) {
            fail("Logout token value for key: " + key + " is not of type Long");
        }
        Long idTokenValue = idTokenClaims.getClaimValue(key, Long.class);

        if (logoutTokenValue == null) {
            fail("Key: " + key + " was missing in the logout_token");
        } else {

            // all of the required keys should be in the id_token, so we should be able to compare the value
            if (idTokenValue == null) {
                if (mustBeInIdToken) {
                    fail("Key: " + key + " was missing in the id_token (nothing to validate the logout_token value against");
                } else {
                    Log.info(thisClass, "mustHaveString", "Key: " + key + " was missing in the id_token, so we can not validate the value in the logout_token - all we can do is make sure that claim exists.");
                }
            } else {
                if (logoutTokenValue < idTokenValue) {
                    fail("Was epecting Key: " + key + " with a value greater than: " + idTokenValue + ", but found: " + logoutTokenValue);
                }
            }
        }

    }

    protected void mustHaveArray(String key, JwtClaims idTokenClaims, JwtClaims logoutTokenClaims) throws Exception {

        Object rawLogoutTokenValues = logoutTokenClaims.getClaimValue(key);
        Object rawIdTokenValues = idTokenClaims.getClaimValue(key);

        if (rawLogoutTokenValues == null) {
            fail("Key: " + key + " was missing in the logout_token");
        } else {
            Log.info(thisClass, "mustHaveArray", "array type: " + rawLogoutTokenValues.getClass());

            // all of the required keys should be in the id_token, so we should be able to compare the value,
            // but check for a null in the idToken - just in case...
            if (rawIdTokenValues == null) {
                fail("Key: " + key + " was missing in the id_token (nothing to validate the logout_token value against");
            } else {
                String[] logoutTokenValues = rawLogoutTokenValues.toString().split(",");
                String[] idTokenValues = rawIdTokenValues.toString().split(",");
                for (String logoutTokenValue : logoutTokenValues) {
                    boolean found = false;
                    for (String idTokenValue : idTokenValues) {
                        if (logoutTokenValue.equals(idTokenValue)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        fail("Member: " + logoutTokenValue + " was not found in the id_token - it is not valid.");
                    }
                }

                for (String idTokenValue : idTokenValues) {
                    boolean found = false;
                    for (String logoutTokenValue : logoutTokenValues) {
                        if (logoutTokenValue.equals(idTokenValue)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        fail("Member: " + idTokenValue + " was not found in the logout_token - it is not valid.");
                    }
                }

            }
        }

    }

    protected void mustHaveOneOrTheOtherString(String oneKey, String otherKey, JwtClaims idTokenClaims, JwtClaims logoutTokenClaims) throws Exception {

        String oneLogoutTokenValue = logoutTokenClaims.getStringClaimValue(oneKey);
        String otherLogoutTokenValue = idTokenClaims.getStringClaimValue(oneKey);

        if (oneLogoutTokenValue == null) {
            if (otherLogoutTokenValue == null) {
                // have to have one or the other or both - having neither is NOT allowed
                fail("Both key: " + oneKey + " and key: " + otherKey + " were missing in the logout_token");
            }
        } else {
            // validate against what's in the id_token
            mustHaveString(oneKey, idTokenClaims, logoutTokenClaims, true);
        }

        // don't have to check for null - if it's null, we don't need to check it and we've already checked for both claims being null
        if (otherLogoutTokenValue != null) {
            // validate against what's in the id_token
            mustHaveString(otherKey, idTokenClaims, logoutTokenClaims, true);
        }

    }

    protected void mustNotHave(String key, JwtClaims logoutClaims) throws Exception {

        if (logoutClaims.hasClaim(key)) {
            fail("Key: " + key + " was included in the logout_token and should NOT have been");
        }
    }

    protected void mustHaveEvents(JwtClaims logoutClaims) throws Exception {

        // {"http://schemas.openid.net/event/backchannel-logout":{}}

        Object value = null;

        Map<String, Object> events = logoutClaims.getClaimValue(Constants.PAYLOAD_EVENTS, Map.class);
        if (events == null) {
            fail("Key: events is missing in the logout_token");
        }

        Log.info(thisClass, "mustHaveEvents", "events:" + events.getClass().toString());

        if (!events.containsKey(logoutEventKey)) {
            fail("Key: events in the logout_token does not contain a Json object containing the key: " + logoutEventKey);
        }
        // check for key being of type JSONObject
        try {
            value = events.get(logoutEventKey);
        } catch (Exception e) {
            fail(logoutEventKey + " is not of type JSONObject");
        }

        if (value == null) {
            // should not be able to have a null content, but, just in case
            fail(logoutEventKey + " value is null and should at least contain an empty Json object");
        } else {
            Log.info(thisClass, "mustHaveEvents", "value: " + value.getClass().toString());
            if (!value.equals(new JSONObject())) {
                Log.info(thisClass, "mustHaveEvents", "*************************************************************************************************************************");
                Log.info(thisClass, "mustHaveEvents", "value for the: " + logoutEventKey + " within events is NOT an empty Json object - the value is: " + value);
                Log.info(thisClass, "mustHaveEvents", "*************************************************************************************************************************");
                //                    fail("value for the: " + logoutEventKey + " within events is NOT an empty Json object - the value is: " + value);
            } else {
                Log.info(thisClass, "mustHaveEvents", "value is an empty Json object - as it should be");
            }
        }
    }

    public void generic_logoutTokenCreationValidation(TestSettings settings, String provider, boolean sidRequired) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        // update the end_session that the test will use (it needs the specific provider)
        settings.setEndSession(settings.getEndSession().replace("OidcConfigSample", provider));

        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes(); // this call will also add the successful status check for logout
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");

        Object response = genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = validationTools.getIDTokenFromOutput(response);
        Log.info(thisClass, _testName, "id token: " + id_token);

        // invoke logout/end_session which will invoke the backchannel uri - which we've configured to use our test app that will print/log the logout_token
        Object logoutResponse = genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, expectations, response, null);
        String logoutToken = getLogoutTokenFromOutput(Constants.LOGOUT_TOKEN, logoutResponse);
        Log.info(thisClass, _testName, "Logout token: " + logoutToken);

        validateLogoutTokenContent(id_token, logoutToken, sidRequired);

    }

    /**
     * Test that the logout_token is does not contain the session id
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_sessionNotRequired() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_sessionNotRequired");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigSample", sidIsNotRequired);
    }

    /**
     * Test that the logout_token is does contain the session id
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void LogoutTokenCreationTests_sessionRequired() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_sessionRequired");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigSample", sidIsRequired);

    }

    /**
     * Test that the logout_token constains a jti claim that does NOT match the jti that is contained in the id_token
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void LogoutTokenCreationTests_jtiEnabledInOP() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_jtiInOP");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigJtiSample", sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper issuer (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultIssuer() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_nonDefaultIssuer");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigIssuerSample", sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_All() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_nonDefaultAudienceAll");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigAudienceSample", sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_one() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_nonDefaultAudienceOne");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigAudienceSample", sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_multiple() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_nonDefaultAudienceMultiple");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigAudienceSample", sidIsNotRequired);

    }

    /**
     * Test that the logout_token DOES NOT contain a nonce claim even though the id_token contains nonce
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void LogoutTokenCreationTests_nonceEnabledInOP() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_nonceInOP");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigNonceSample", sidIsNotRequired);

    }

    /**
     * This test is really a dup of most of the other tests - the other tests mostly use HS256 - only including it here so the
     * test results look complete
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_sign_HS256() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_sessionNotRequired");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigSample", sidIsNotRequired);

    }

    /*
     * TODO - enable tests once these signature algs are supported in the OP
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_HS384() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_HS512() throws Exception {
     *
     * }
     */

    /**
     * Test that the logout_token is signed with RS256 when the OP config specifies RS256
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_sign_RS256() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/client_rs256");

        generic_logoutTokenCreationValidation(updatedTestSettings, "OidcConfigRs256Sample", sidIsNotRequired);

    }

    /*
     * TODO - enable tests once these signature algs are supported in the OP
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_RS384() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_RS512() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_ES256() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_ES384() throws Exception {
     *
     * }
     */

    /*
     * TODO - enable tests once encryption is supported in the OP
     *
     * @Test
     * public void LogoutTokenCreationTests_sign_ES512() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_encrypt_RS256() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_encrypt_RS384() throws Exception {
     *
     * }
     *
     * @Test
     * public void LogoutTokenCreationTests_encrypt_RS512() throws Exception {
     *
     * }
     */
}

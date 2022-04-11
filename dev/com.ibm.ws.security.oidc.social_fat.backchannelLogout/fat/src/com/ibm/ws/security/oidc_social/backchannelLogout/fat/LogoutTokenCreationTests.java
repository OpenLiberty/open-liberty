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

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
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

    }

    /**
     *
     * @param idTokenData
     * @param logoutToken
     * @param updatedTestSettings
     * @throws Exception
     */
    public void validateLogoutTokenContent(JwtTokenForTest idTokenData, JwtTokenForTest logoutTokenData, boolean sidRequired) throws Exception {

        // validate header
        validateLogoutTokenHeader(idTokenData, logoutTokenData);
        // validate payload
        validateLogoutTokenClaims(idTokenData, logoutTokenData, sidRequired);
    }

    public void validateLogoutTokenHeader(JwtTokenForTest idTokenData, JwtTokenForTest logoutTokenData) throws Exception {

        Map<String, Object> idTokenDataClaims = idTokenData.getMapHeader();
        Map<String, Object> logoutClaims = logoutTokenData.getMapHeader();

        mustHaveString(Constants.HEADER_ALGORITHM, idTokenDataClaims, logoutClaims, true);

    }

    public void validateLogoutTokenClaims(JwtTokenForTest idTokenData, JwtTokenForTest logoutTokenData, boolean sidRequired) throws Exception {

        Map<String, Object> idTokenDataClaims = idTokenData.getMapPayload();
        Map<String, Object> logoutClaims = logoutTokenData.getMapPayload();

        mustHaveString(Constants.PAYLOAD_ISSUER, idTokenDataClaims, logoutClaims, true);

        mustHaveArray(Constants.PAYLOAD_AUDIENCE, idTokenDataClaims, logoutClaims);
        mustHaveLong(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, idTokenDataClaims, logoutClaims, true);
        mustHaveString(Constants.PAYLOAD_JWTID, idTokenDataClaims, logoutClaims, false); // maybe change interface to pass alternately obtained jti value to validate against

        if (sidRequired) {
            mustHaveString(Constants.PAYLOAD_SESSION_ID, idTokenDataClaims, logoutClaims, true);
        } else {
            // we need to have either sid or sub
            mustHaveOneOrTheOtherString(Constants.PAYLOAD_SUBJECT, Constants.PAYLOAD_SESSION_ID, idTokenDataClaims, logoutClaims);
        }

        mustHaveEvents(logoutClaims);
        mustNotHave(Constants.PAYLOAD_NONCE, logoutClaims);

    }

    protected void mustHaveString(String key, Map<String, Object> idTokenDataClaims, Map<String, Object> logoutClaims, boolean mustBeInIdToken) throws Exception {

        Object logoutTokenValue = logoutClaims.get(key);
        Object idTokenValue = idTokenDataClaims.get(key);

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
                        fail("Was epecting Key: " + key + " with value: " + idTokenDataClaims.get(key) + ", but found: " + logoutTokenValue);
                    } else {
                        Log.info(thisClass, "mustHaveString", "The value for the key in the id_token and logout_token did not match - but, for claims like jti, they should not");
                    }
                }
            }
        }

    }

    protected void mustHaveLong(String key, Map<String, Object> idTokenDataClaims, Map<String, Object> logoutClaims, boolean mustBeInIdToken) throws Exception {

        Object rawLogoutTokenValue = logoutClaims.get(key);
        Object rawIdTokenValue = idTokenDataClaims.get(key);

        if (rawLogoutTokenValue == null) {
            fail("Key: " + key + " was missing in the logout_token");
        } else {
            Long logoutTokenValue = Long.valueOf((String) rawLogoutTokenValue).longValue();
            // all of the required keys should be in the id_token, so we should be able to compare the value
            if (rawIdTokenValue == null) {
                if (mustBeInIdToken) {
                    fail("Key: " + key + " was missing in the id_token (nothing to validate the logout_token value against");
                } else {
                    Log.info(thisClass, "mustHaveString", "Key: " + key + " was missing in the id_token, so we can not validate the value in the logout_token - all we can do is make sure that claim exists.");
                }
            } else {
                Long idTokenValue = Long.valueOf((String) rawIdTokenValue).longValue();
                if (logoutTokenValue < idTokenValue) {
                    fail("Was epecting Key: " + key + " with a value greater than: " + idTokenValue + ", but found: " + logoutTokenValue);
                }
            }
        }

    }

    protected void mustHaveArray(String key, Map<String, Object> idTokenDataClaims, Map<String, Object> logoutClaims) throws Exception {

        Object rawLogoutTokenValue = logoutClaims.get(key);
        Object rawIdTokenValue = idTokenDataClaims.get(key);

        if (rawLogoutTokenValue == null) {
            fail("Key: " + key + " was missing in the logout_token");
        } else {
            Log.info(thisClass, "mustHaveArray", "array type: " + rawLogoutTokenValue.getClass());

            // all of the required keys should be in the id_token, so we should be able to compare the value,
            // but check for a null in the idToken - just in case...
            if (rawIdTokenValue == null) {
                fail("Key: " + key + " was missing in the id_token (nothing to validate the logout_token value against");
            } else {
                String[] logoutTokenList = rawLogoutTokenValue.toString().split(",");
                String[] idTokenList = rawIdTokenValue.toString().split(",");
                for (String logoutTokenValue : logoutTokenList) {
                    boolean found = false;
                    for (String idTokenValue : idTokenList) {
                        if (logoutTokenValue.equals(idTokenValue)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        fail("Member: " + logoutTokenValue + " was not found in the id_token - it is not valid.");
                    }
                }

                for (String idTokenValue : idTokenList) {
                    boolean found = false;
                    for (String logoutTokenValue : logoutTokenList) {
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

    protected void mustHaveOneOrTheOtherString(String oneKey, String otherKey, Map<String, Object> idTokenDataClaims, Map<String, Object> logoutClaims) throws Exception {

        Object oneLogoutTokenValue = logoutClaims.get(oneKey);
        Object otherLogoutTokenValue = logoutClaims.get(otherKey);

        if (oneLogoutTokenValue == null) {
            if (otherLogoutTokenValue == null) {
                // have to have one or the other or both - having neither is NOT allowed
                fail("Both key: " + oneKey + " and key: " + otherKey + " were missing in the logout_token");
            }
        } else {
            // need to have validate against what was in the id_token
            Object oneIdTokenValue = idTokenDataClaims.get(oneKey);
            if (oneIdTokenValue == null) {
                fail("Key: " + oneKey + " was missing in the id_token (nothing to validate the logout_token value against");
            }
            if (!oneIdTokenValue.toString().equals(oneLogoutTokenValue.toString())) {
                fail("Was epecting Key: " + oneKey + " with value: " + oneIdTokenValue.toString() + ", but found: " + oneLogoutTokenValue.toString());
            }
        }

        // don't have to check for null - if it's null, we don't need to check it and we've already checked for both claims being null
        if (otherLogoutTokenValue != null) {
            // need to have validate against what was in the id_token
            Object otherIdTokenValue = idTokenDataClaims.get(otherKey);
            if (otherIdTokenValue == null) {
                fail("Key: " + otherKey + " was missing in the id_token (nothing to validate the logout_token value against");
            }

            if (!otherIdTokenValue.toString().equals(otherLogoutTokenValue.toString())) {
                fail("Was epecting Key: " + otherKey + " with value: " + otherIdTokenValue.toString() + ", but found: " + otherLogoutTokenValue.toString());
            }

        }

    }

    protected void mustNotHave(String key, Map<String, Object> logoutClaims) throws Exception {

        Object logoutTokenValue = logoutClaims.get(key);
        if (logoutTokenValue != null) {
            fail("Key: " + key + " was included in the logout_token and should NOT have been");
        }
    }

    protected void mustHaveEvents(Map<String, Object> logoutClaims) throws Exception {

        Object rawLogoutTokenValue = logoutClaims.get(Constants.PAYLOAD_EVENTS);
        if (rawLogoutTokenValue == null) {
            fail("Key: events is missing in the logout_token");
        }

        if (rawLogoutTokenValue instanceof JsonObject) {

            Log.info(thisClass, "ugh", "in jsonObject");
            Log.info(thisClass, "mustHaveEvents", "JsonObject: " + rawLogoutTokenValue);
            JSONObject event = JSONObject.parse(rawLogoutTokenValue.toString());
            if (event == null) {
                fail("Key: events is Null in the logout_token");
            }
            if (!event.containsKey(logoutEventKey)) {
                fail("Key: events in the logout_token does not contain a Json object containing the key: " + logoutEventKey);
            }
            Object value = event.get(logoutEventKey);
            if (value == null) {

            } else {
                if (!value.equals(new JSONObject())) {
                    fail("value for the: " + logoutEventKey + " within events is NOT an empty Json object - the value is: " + value);
                } else {
                    Log.info(thisClass, "mustHaveEvents", "value is an empty Json object - as it should be");
                }
            }
        } else {
            fail("Events is not of type JsonObject");
        }
    }

    public void generic_logoutTokenCreationValidation(TestSettings settings, String provider, boolean sidRequired) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        // set up the postlogout redirect to call the test app - it will build a response with the logout_token saved from the invocation of the back channel logout request
        // it'll do this to retrieve the logout_token content
        settings.setPostLogoutRedirect(clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");
        // update the end_session that the test will use (it needs the specific provider)
        settings.setEndSession(settings.getEndSession().replace("OidcConfigSample", provider));

        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes(); // this call will also add the successful status check for logout
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");

        Object response = genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = validationTools.getIDTokenFromOutput(response);
        Log.info(thisClass, _testName, "id token: " + id_token);
        JwtTokenForTest idTokenData = gatherDataFromToken(id_token, settings);

        // invoke logout/end_session which will invoke the backchannel uri - which we've configured to use our test app that will print/log the logout_token
        Object logoutResponse = genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, expectations, response, null);
        String logoutToken = getLogoutTokenFromOutput(Constants.LOGOUT_TOKEN, logoutResponse);
        Log.info(thisClass, _testName, "Logout token: " + logoutToken);

        JwtTokenForTest logoutTokenData = gatherDataFromToken(logoutToken, settings);
        validateLogoutTokenContent(idTokenData, logoutTokenData, sidRequired);

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

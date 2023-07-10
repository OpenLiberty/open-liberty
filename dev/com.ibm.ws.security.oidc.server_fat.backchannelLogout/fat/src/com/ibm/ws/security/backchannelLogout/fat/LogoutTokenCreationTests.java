/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

package com.ibm.ws.security.backchannelLogout.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.backchannelLogout.fat.utils.TokenKeeper;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

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

    @BeforeClass
    public static void setUp() throws Exception {

        // bypass LDAP setup which isn't needed and wastes a good bit of time.
        useLdap = false;
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
                add(Constants.backchannelLogoutApp);
            }
        };

        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_validateLogoutTokenCreation.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_validateLogoutTokenCreation.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        testSettings.setFlowType(Constants.RP_FLOW);
        // set up the postlogout redirect to call the test app - it will build a response with the logout_token saved from the invocation of the back channel logout request
        // it'll do this to retrieve the logout_token content
        //        testSettings.setPostLogoutRedirect(clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");
        testSettings.setPostLogoutRedirect(null);

    }

    @AfterClass
    public static void afterClass() {
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + defaultUseLdap);
        useLdap = defaultUseLdap;
    }

    /**
     * Validate the content of the logout_token - use the content of the id_token from the client login request
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

        JwtClaims idTokenClaims = new TokenKeeper(id_token).getHeaderClaims();
        JwtClaims logoutTokenClaims = new TokenKeeper(logout_token).getHeaderClaims();

        mustHaveString(Constants.HEADER_ALGORITHM, idTokenClaims, logoutTokenClaims, true);

    }

    public void validateLogoutTokenClaims(String id_token, String logout_token, boolean sidRequired) throws Exception {

        JwtClaims idTokenClaims = new TokenKeeper(id_token).getPayloadClaims();
        JwtClaims logoutTokenClaims = new TokenKeeper(logout_token).getPayloadClaims();

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
            // all of the required key/claim should be in the id_token, so we should be able to compare the value,
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

            // all of the required key/claim should be in the id_token, so we should be able to compare the value
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

            // all of the required key/claim should be in the id_token, so we should be able to compare the value,
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

        if (!events.containsKey(Constants.logoutEventKey)) {
            fail("Key: events in the logout_token does not contain a Json object containing the key: " + Constants.logoutEventKey);
        }
        // check for key being of type JSONObject
        try {
            value = events.get(Constants.logoutEventKey);
        } catch (Exception e) {
            fail(Constants.logoutEventKey + " is not of type JSONObject");
        }

        if (value == null) {
            // should not be able to have a null content, but, just in case
            fail(Constants.logoutEventKey + " value is null and should at least contain an empty Json object");
        } else {
            Log.info(thisClass, "mustHaveEvents", "value: " + value.getClass().toString());
            if (!value.equals(new JSONObject())) {
                Log.info(thisClass, "mustHaveEvents", "*************************************************************************************************************************");
                Log.info(thisClass, "mustHaveEvents", "value for the: " + Constants.logoutEventKey + " within events is NOT an empty Json object - the value is: " + value);
                Log.info(thisClass, "mustHaveEvents", "*************************************************************************************************************************");
                fail("value for the: " + Constants.logoutEventKey + " within events is NOT an empty Json object - the value is: " + value);
            } else {
                Log.info(thisClass, "mustHaveEvents", "value is an empty Json object - as it should be");
            }
        }
    }

    /**
     * This method provides the "generic" steps that each of the test cases in this class needs to perform
     * - clear the response string in the test bcl endpoint
     * - Update the test settings based on the provider and client info passed
     * - Login to access a protected app
     * - gather the id_token, parse and save it in JwtTokenForTest format
     * - invoke end_session
     * - gather the logout_token and save it in JwtTokenForTest format
     * - validate the content of the logout_token against what was in the id_token
     *
     * @param provider
     *            the OP Provider that the test case is using - used to build the end_session endpoint
     * @param client
     *            the client that the test case is using - used to build the app name, post redirect uri and search for the
     *            logout_token in the post logout response
     * @param sidRequired
     *            flag inidicating if the sid is required by the config (used when validating the logout_token)
     * @throws Exception
     */
    public void generic_logoutTokenCreationValidation(String provider, String client, boolean sidRequired) throws Exception {

        restoreAppMap(client);
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings settings = testSettings.copyTestSettings();
        // set up the postlogout redirect to call the test app - it will build a response with the logout_token saved from the invocation of the back channel logout request
        // it'll do this to retrieve the logout_token content
        settings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + client);

        // update the end_session that the test will use (it needs the specific provider)
        settings.setEndSession(settings.getEndSession().replace("OidcConfigSample", provider));

        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes(); // this call will also add the successful status check for logout
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, testOPServer.getHttpsString() + "/oidc/end_session_logout.html");

        Object response = genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = validationTools.getIDTokenFromOutput(response);
        Log.info(thisClass, _testName, "id token: " + id_token);

        // invoke logout/end_session which will invoke the backchannel uri - which we've configured to use our test app that will print/log the logout_token
        genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, expectations, response, null);

        String logoutToken = getLogoutToken(client);
        validateLogoutTokenContent(id_token, logoutToken, sidRequired);

    }

    /**
     * Test that the logout_token contains a jti claim that does NOT match the jti that is contained in the id_token
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void LogoutTokenCreationTests_jtiEnabledInOP() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigJtiSample", "client_jtiInOP", Constants.sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper issuer (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultIssuer() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigIssuerSample", "client_nonDefaultIssuer", Constants.sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_All() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigAudienceAllSample", "client_nonDefaultAudienceAll", Constants.sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_one() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigAudienceOneSample", "client_nonDefaultAudienceOne", Constants.sidIsNotRequired);

    }

    /**
     * Test that the logout_token contains the proper audience (not what would be the "default" had the config not over-ridden it)
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_nonDefaultAudience_multiple() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigAudienceMultipleSample", "client_nonDefaultAudienceMultiple", Constants.sidIsNotRequired);

    }

    /**
     * Test that the logout_token DOES NOT contain a nonce claim even though the id_token contains nonce
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void LogoutTokenCreationTests_nonceEnabledInOP() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigNonceSample", "client_nonceInOP", Constants.sidIsNotRequired);

    }

    /**
     * This test is really a dup of most of the other tests - the other tests mostly use HS256 - only including it here so the
     * test results look complete
     *
     * @throws Exception
     */
    @Test
    public void LogoutTokenCreationTests_sign_HS256() throws Exception {

        generic_logoutTokenCreationValidation("OidcConfigHS256Sample", "client_hs256", Constants.sidIsNotRequired);

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

        generic_logoutTokenCreationValidation("OidcConfigRS256Sample", "client_rs256", Constants.sidIsNotRequired);

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

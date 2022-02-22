/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backChannelLogout.fat.CommonTests;

import java.util.List;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.NumericDate;

//
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.topology.utils.ServerFileUtils;

public class BackChannelLogoutCommonTests extends CommonTest {

    protected static Class<?> thisClass = BackChannelLogoutCommonTests.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();
    public static EndpointSettings eSettings = new EndpointSettings();

    public String buildBackChannelLogoutUri(String client) throws Exception {

        String part2 = (Constants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT + Constants.OIDC_BACK_CHANNEL_LOGOUT_ROOT + client).replace("//", "/");
        String uri = testRPServer.getHttpsString() + part2;
        Log.info(thisClass, "_testName", "backChannelLogouturi: " + uri);

        return uri;
    }

    //    protected static String defaultKeyFile = null;
    //
    //    public void setDefaultKeyFile(LibertyServer server, String keyFile) throws Exception {
    //
    //        defaultKeyFile = getKeyFileWithPathForServer(server, keyFile);
    //
    //    }
    //
    //    public String getKeyFileWithPathForServer(LibertyServer server, String keyFile) throws Exception {
    //
    //        return serverFileUtils.getServerFileLoc(server) + "/" + keyFile;
    //    }

    public List<endpointSettings> createDefaultLogoutParms() throws Exception {

        String thisMethod = "createDefaultLogoutParms";

        JWTTokenBuilder b = setDefaultLogoutToken();
        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    public List<endpointSettings> createLogoutParms_removingOne(String claim) throws Exception {

        String thisMethod = "createLogoutParms_removingOne";

        JWTTokenBuilder b = setDefaultLogoutToken();

        Log.info(thisClass, thisMethod, "Removing: " + claim);
        b.unsetClaim(claim);

        String logoutToken = b.buildAsIs();

        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());
        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    public List<endpointSettings> createLogoutParms_addOrOverrideOne(String claim, Object value) throws Exception {

        String thisMethod = "createLogoutParms_removingOne";

        JWTTokenBuilder b = setDefaultLogoutToken();

        Log.info(thisClass, thisMethod, "Adding or updating: " + claim + " with value: " + value.toString());
        b.setClaim(claim, value);

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());
        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    public List<endpointSettings> createLogoutParms_removingOne_addOrOverrideOne(String removeClaim, String addOverrideClaim, Object addOverrideValue) throws Exception {

        String thisMethod = "createLogoutParms_removingOne";

        JWTTokenBuilder b = setDefaultLogoutToken();

        Log.info(thisClass, thisMethod, "Adding or updating: " + addOverrideClaim + " with value: " + addOverrideValue.toString());
        b.setClaim(addOverrideClaim, addOverrideValue);
        b.unsetClaim(addOverrideClaim);

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());
        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    public JWTTokenBuilder setDefaultLogoutToken() throws Exception {

        Log.info(thisClass, "setDefaultLogoutToken", "creating token");
        JWTTokenBuilder b = setBuilderForLogoutToken("client01", "testuser", null, null, null, null, "client01");
        Log.info(thisClass, "setDefaultLogoutToken", "claims: " + b.getJsonClaims());
        return b;
    }

    /*
     */
    public JWTTokenBuilder setBuilderForLogoutToken(String iss, String sub, NumericDate iat, String jti, JSONObject events, String sid, String... aud) throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer(iss); // required
        if (sub != null) { // optional
            builder.setSubject(sub);
        }
        builder.setAudience(aud); // required
        if (iat == null) { // required
            builder.setIssuedAtToNow();
        } else {
            builder.setIssuedAt(iat);
        }
        if (jti == null) { // required
            builder.setGeneratedJwtId(); // will ensure a unique jti for each test
        } else {
            builder.setJwtId(jti); // used to test jti re-use
        }

        if (events == null) { // required
            // build "default"
            events = new JSONObject();
            events.put("http://schemas.openid.net/event/backchannel-logout", new JSONObject());
            builder.setClaim("events", events);
        } else {
            builder.setClaim("events", events);
        }
        if (sid != null) { //optional
            builder.setClaim("sid", sid);
        }

        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        //        builder = builder.setHSAKey("secret");
        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");

        Log.info(thisClass, "setBuilderForLogoutToken", "claims: " + builder.getJsonClaims());
        //        builder.unsetClaim(jti);

        //        builder.setExpirationTimeMinutesIntheFuture(5);
        //        builder.setScope("openid profile");
        //        builder.setRealmName("BasicRealm");
        //        builder.setTokenType("Bearer");
        //        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        //        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");
        //        //  setup for encryption - tests can override the following values
        //        builder = builder.setKeyManagementKeyAlg(JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
        //        builder = builder.setContentEncryptionAlg(JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG);
        return builder;
    }

    // TODO - nonce type - is it string?
    public JWTTokenBuilder addNonceToLogoutToken(JWTTokenBuilder builder, String nonce) throws Exception {

        builder.setClaim("nonce", nonce);
        return builder;
    }

    public List<validationData> setMissingBackChannelLogoutRequestClaim(String claim) throws Exception {

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim [" + claim + "] was missing.", MessageConstants.CWWKS1545E_BACK_CHANNEL_LOGOUT_MISSING_REQUIRED_CLAIM);

        return expectations;
    }

    public List<validationData> setInvalidBackChannelLogoutRequestClaim(String specificMsg) throws Exception {

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim contained an invalid value.", specificMsg);

        return expectations;

    }

    public List<validationData> setInvalidBackChannelLogoutRequest() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
        return expectations;

    }

    //    /**
    //     * Create a new JWTTokenBuilder and initialize it with default test values
    //     *
    //     * @return - an initialized JWTTokenBuilder
    //     * @throws Exception
    //     */
    //    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {
    //
    //        JWTTokenBuilder builder = new JWTTokenBuilder();
    //        builder.setIssuer("client01");
    //        builder.setSubject("testuser");
    //
    //        builder.setIssuedAtToNow();
    //        builder.setExpirationTimeMinutesIntheFuture(5);
    //        builder.setScope("openid profile");
    //        builder.setRealmName("BasicRealm");
    //        builder.setTokenType("Bearer");
    //        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
    //        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");
    //        //  setup for encryption - tests can override the following values
    //        builder = builder.setKeyManagementKeyAlg(JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
    //        builder = builder.setContentEncryptionAlg(JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG);
    //        return builder;
    //    }
    //
    //    /*
    //     * Wrap the call to the builder so that we can log the raw values and the generated token
    //     * for debug purposes and not have to duplicate 3 simple lines of code
    //     */
    //    public String buildToken(JWTTokenBuilder builder, String testName) throws Exception {
    //        Log.info(thisClass, "buildToken", "testing _testName: " + testName);
    //        Log.info(thisClass, testName, "Json claims:" + builder.getJsonClaims());
    //        String jwtToken = builder.build();
    //        Log.info(thisClass, testName, "built jwt:" + jwtToken);
    //        return jwtToken;
    //    }
    //
    //    public void updateBuilderWithRSASettings(JWTTokenBuilder builder) throws Exception {
    //        updateBuilderWithRSASettings(builder, null);
    //    }
    //
    //    public void updateBuilderWithRSASettings(JWTTokenBuilder builder, String overrideKeyFile) throws Exception {
    //        String keyFile = defaultKeyFile;
    //        // if an override wasn't given, use the default key file
    //        if (overrideKeyFile != null) {
    //            keyFile = overrideKeyFile;
    //        }
    //
    //        updateBuilderWithRSASettings(builder, AlgorithmIdentifiers.RSA_USING_SHA256, keyFile);
    //    }
    //
    //    public void updateBuilderWithRSASettings(JWTTokenBuilder builder, String alg, String keyFile) throws Exception {
    //
    //        Log.info(thisClass, "updateBuilderWithRSASettings", "alg: " + alg + " keyFile: " + keyFile);
    //        builder.setAlorithmHeaderValue(alg);
    //        builder.setRSAKey(keyFile);
    //    }
    //
    //    /**
    //     * Builds a JWE Token with an alternate (Json) Payload
    //     * We can't use the Liberty builder as it does NOT provide a way to create a simple Json payload
    //     *
    //     * @param key - the key to be used for encryption
    //     * @return - a built JWE Token with a simple Json payload
    //     * @throws Exception
    //     */
    //    public String buildAlternatePayloadJWEToken(Key key) throws Exception {
    //        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key));
    //        String jwtToken = builder.buildAlternateJWE();
    //        return jwtToken;
    //    }
    //
    //    public String buildAlternatePayloadJWEToken(Key key, List<NameValuePair> extraPayload) throws Exception {
    //        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key), extraPayload);
    //        String jwtToken = builder.buildAlternateJWE();
    //        return jwtToken;
    //    }
    //
    //    /**
    //     * Builds a JWE Token with an alternate (Json) Payload and a JWE header with an alternate type and/or contentType.
    //     * We can't use the Liberty builder as it does NOT provide a way to update the typ and cty JWE header attributes
    //     *
    //     * @param key - the key to be used for encryption
    //     * @param type - the typ value to set in the JWE header
    //     * @param contentType - the cty value to set in the JWE header
    //     * @return - a built JWE Token with a simple Json payload and an alternate JWE header
    //     * @throws Exception
    //     */
    //    public String buildAlternatePayloadJWEToken(Key key, String type, String contentType) throws Exception {
    //        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key));
    //
    //        String jwtToken = builder.buildAlternateJWESetJWEHeaderValues(type, contentType);
    //        return jwtToken;
    //    }
    //
    //    /**
    //     * Builds a JWE Token with a JWE header with an alternate type andor content type
    //     * We can't use the Liberty builder as it does NOT provide a way to update the typ and cty JWE header attributes
    //     *
    //     * @param key - they key to use for encryption
    //     * @param type - the typ value to set in the JWE header
    //     * @param contentType - the cty value to set in the JWE header
    //     * @return - a build JWE Token with an alternate JWE header
    //     * @throws Exception
    //     */
    //    public String buildJWETokenWithAltHeader(Key key, String type, String contentType) throws Exception {
    //        JWTTokenBuilder builder = populateAlternateJWEToken(key);
    //
    //        // calling buildJWE will override the payload contents with JWS
    //        String jwtToken = builder.buildJWE(type, contentType);
    //
    //        //new JwtTokenForTest(jwtToken, JwtKeyTools.getComplexPrivateKeyForSigAlg(jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256));
    //        return jwtToken;
    //    }
    //
    //    /**
    //     * Create a "test" token builder and popluate with some default values
    //     *
    //     * @param key - set the key to be used for encryption
    //     * @return - a built JWE token
    //     * @throws Exception
    //     */
    //    public JWTTokenBuilder populateAlternateJWEToken(Key key) throws Exception {
    //        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
    //        builder.setAudience("client01", "client02");
    //        builder.setIssuer("testIssuer");
    //        builder.setKeyManagementKey(key);
    //        return builder;
    //    }
    //
    //    /**
    //     * Create and add a simple Json payload to the passed in builder
    //     *
    //     * @param builder - the builder to upate
    //     * @return - returns the builder with a simple Json payload (method just creates some random values - currently, no one is looking at them
    //     * @throws Exception
    //     */
    //    public JWTTokenBuilder createAlternateJWEPayload(JWTTokenBuilder builder) throws Exception {
    //        return createAlternateJWEPayload(builder, null);
    //    }
    //
    //    public JWTTokenBuilder createAlternateJWEPayload(JWTTokenBuilder builder, List<NameValuePair> extraPayload) throws Exception {
    //// Json Content (buildJWE method will override payload, buildAlternateJWE* methods will use what's set below)
    //        JSONObject payload = new JSONObject();
    //        payload.put(PayloadConstants.ISSUER, "client01");
    //        NumericDate now = NumericDate.now();
    //        payload.put(PayloadConstants.ISSUED_AT, now.getValue());
    //        payload.put(PayloadConstants.EXPIRATION_TIME, now.getValue() + (2 * 60 * 60));
    //        payload.put(PayloadConstants.SCOPE, "openid profile");
    //        payload.put(PayloadConstants.SUBJECT, "testuser");
    //        payload.put(PayloadConstants.REALM_NAME, "BasicRealm");
    //        payload.put(PayloadConstants.TOKEN_TYPE, "Bearer");
    //        payload.put("key1", "ugh.ibm.com");
    //        payload.put("key2", "my.dog.has.fleas");
    //        payload.put("key3", "testing.to.bump.up.part.count");
    //        payload.put("key4", "hereWe.goAgain");
    //        if (extraPayload != null && !extraPayload.isEmpty()) {
    //            for (NameValuePair claim : extraPayload) {
    //                payload.put(claim.getName(), claim.getValue());
    //            }
    //        }
    //        String payloadString = payload.toString();
    //        builder.setPayload(payloadString);
    //        return builder;
    //
    //    }

}

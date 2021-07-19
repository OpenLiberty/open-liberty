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
package com.ibm.ws.security.fat.common.jwt.utils;

import java.security.Key;
import java.util.List;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.NumericDate;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ServerFileUtils;

public class JwtTokenBuilderUtils {

    protected static Class<?> thisClass = JwtTokenBuilderUtils.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();

    protected static String defaultKeyFile = null;

    public void setDefaultKeyFile(LibertyServer server, String keyFile) throws Exception {

        defaultKeyFile = getKeyFileWithPathForServer(server, keyFile);

    }

    public String getKeyFileWithPathForServer(LibertyServer server, String keyFile) throws Exception {

        return serverFileUtils.getServerFileLoc(server) + "/" + keyFile;
    }

    /**
     * Create a new JWTTokenBuilder and initialize it with default test values
     *
     * @return - an initialized JWTTokenBuilder
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("client01");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeMinutesIntheFuture(5);
        builder.setScope("openid profile");
        builder.setSubject("testuser");
        builder.setRealmName("BasicRealm");
        builder.setTokenType("Bearer");
        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");
        //  setup for encryption - tests can override the following values
        builder = builder.setKeyManagementKeyAlg(JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
        builder = builder.setContentEncryptionAlg(JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG);
        return builder;
    }

    /*
     * Wrap the call to the builder so that we can log the raw values and the generated token
     * for debug purposes and not have to duplicate 3 simple lines of code
     */
    public String buildToken(JWTTokenBuilder builder, String testName) throws Exception {
        Log.info(thisClass, "buildToken", "testing _testName: " + testName);
        Log.info(thisClass, testName, "Json claims:" + builder.getJsonClaims());
        String jwtToken = builder.build();
        Log.info(thisClass, testName, "built jwt:" + jwtToken);
        return jwtToken;
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder) throws Exception {
        updateBuilderWithRSASettings(builder, null);
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder, String overrideKeyFile) throws Exception {
        String keyFile = defaultKeyFile;
        // if an override wasn't given, use the default key file
        if (overrideKeyFile != null) {
            keyFile = overrideKeyFile;
        }

        updateBuilderWithRSASettings(builder, AlgorithmIdentifiers.RSA_USING_SHA256, keyFile);
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder, String alg, String keyFile) throws Exception {

        Log.info(thisClass, "updateBuilderWithRSASettings", "alg: " + alg + " keyFile: " + keyFile);
        builder.setAlorithmHeaderValue(alg);
        builder.setRSAKey(keyFile);
    }

    /**
     * Builds a JWE Token with an alternate (Json) Payload
     * We can't use the Liberty builder as it does NOT provide a way to create a simple Json payload
     *
     * @param key - the key to be used for encryption
     * @return - a built JWE Token with a simple Json payload
     * @throws Exception
     */
    public String buildAlternatePayloadJWEToken(Key key) throws Exception {
        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key));
        String jwtToken = builder.buildAlternateJWE();
        return jwtToken;
    }

    public String buildAlternatePayloadJWEToken(Key key, List<NameValuePair> extraPayload) throws Exception {
        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key), extraPayload);
        String jwtToken = builder.buildAlternateJWE();
        return jwtToken;
    }

    /**
     * Builds a JWE Token with an alternate (Json) Payload and a JWE header with an alternate type and/or contentType.
     * We can't use the Liberty builder as it does NOT provide a way to update the typ and cty JWE header attributes
     *
     * @param key - the key to be used for encryption
     * @param type - the typ value to set in the JWE header
     * @param contentType - the cty value to set in the JWE header
     * @return - a built JWE Token with a simple Json payload and an alternate JWE header
     * @throws Exception
     */
    public String buildAlternatePayloadJWEToken(Key key, String type, String contentType) throws Exception {
        JWTTokenBuilder builder = createAlternateJWEPayload(populateAlternateJWEToken(key));

        String jwtToken = builder.buildAlternateJWESetJWEHeaderValues(type, contentType);
        return jwtToken;
    }

    /**
     * Builds a JWE Token with a JWE header with an alternate type andor content type
     * We can't use the Liberty builder as it does NOT provide a way to update the typ and cty JWE header attributes
     *
     * @param key - they key to use for encryption
     * @param type - the typ value to set in the JWE header
     * @param contentType - the cty value to set in the JWE header
     * @return - a build JWE Token with an alternate JWE header
     * @throws Exception
     */
    public String buildJWETokenWithAltHeader(Key key, String type, String contentType) throws Exception {
        JWTTokenBuilder builder = populateAlternateJWEToken(key);

        // calling buildJWE will override the payload contents with JWS
        String jwtToken = builder.buildJWE(type, contentType);

        //new JwtTokenForTest(jwtToken, JwtKeyTools.getComplexPrivateKeyForSigAlg(jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256));
        return jwtToken;
    }

    /**
     * Create a "test" token builder and popluate with some default values
     *
     * @param key - set the key to be used for encryption
     * @return - a built JWE token
     * @throws Exception
     */
    public JWTTokenBuilder populateAlternateJWEToken(Key key) throws Exception {
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.setAudience("client01", "client02");
        builder.setIssuer("testIssuer");
        builder.setKeyManagementKey(key);
        return builder;
    }

    /**
     * Create and add a simple Json payload to the passed in builder
     *
     * @param builder - the builder to upate
     * @return - returns the builder with a simple Json payload (method just creates some random values - currently, no one is looking at them
     * @throws Exception
     */
    public JWTTokenBuilder createAlternateJWEPayload(JWTTokenBuilder builder) throws Exception {
        return createAlternateJWEPayload(builder, null);
    }

    public JWTTokenBuilder createAlternateJWEPayload(JWTTokenBuilder builder, List<NameValuePair> extraPayload) throws Exception {
// Json Content (buildJWE method will override payload, buildAlternateJWE* methods will use what's set below)
        JSONObject payload = new JSONObject();
        payload.put(PayloadConstants.ISSUER, "client01");
        NumericDate now = NumericDate.now();
        payload.put(PayloadConstants.ISSUED_AT, now.getValue());
        payload.put(PayloadConstants.EXPIRATION_TIME, now.getValue() + (2 * 60 * 60));
        payload.put(PayloadConstants.SCOPE, "openid profile");
        payload.put(PayloadConstants.SUBJECT, "testuser");
        payload.put(PayloadConstants.REALM_NAME, "BasicRealm");
        payload.put(PayloadConstants.TOKEN_TYPE, "Bearer");
        payload.put("key1", "ugh.ibm.com");
        payload.put("key2", "my.dog.has.fleas");
        payload.put("key3", "testing.to.bump.up.part.count");
        payload.put("key4", "hereWe.goAgain");
        if (extraPayload != null && !extraPayload.isEmpty()) {
            for (NameValuePair claim : extraPayload) {
                payload.put(claim.getName(), claim.getValue());
            }
        }
        String payloadString = payload.toString();
        builder.setPayload(payloadString);
        return builder;

    }

}

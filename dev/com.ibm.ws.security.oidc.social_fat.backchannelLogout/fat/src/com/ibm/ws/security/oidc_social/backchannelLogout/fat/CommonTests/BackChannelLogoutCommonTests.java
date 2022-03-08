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
package com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests;

import static org.junit.Assert.fail;

import java.util.List;

import org.jose4j.jws.AlgorithmIdentifiers;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.utils.ServerFileUtils;

/**
 * This class supplies support methods to the back channel logout tests.
 */

public class BackChannelLogoutCommonTests extends CommonTest {

    protected static Class<?> thisClass = BackChannelLogoutCommonTests.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();
    public static EndpointSettings eSettings = new EndpointSettings();
    protected static final String sharedHSSharedKey = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
    protected static final String defaultClient = "clientSignHS256";

    public String testClient = null;
    public static TestServer clientServer = null;

    /**
     * Build the backchannel logout url based on the rp server hostinfo and the client to be "logged out"
     *
     * @param client
     *            client name
     * @return - the logout url
     * @throws Exception
     */
    public String buildBackchannelLogoutUri(String client) throws Exception {

        String contextRoot = null;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            contextRoot = Constants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT;
        } else {
            contextRoot = SocialConstants.DEFAULT_CONTEXT_ROOT;
        }
        String part2 = (contextRoot + Constants.OIDC_BACK_CHANNEL_LOGOUT_ROOT + client).replace("//", "/");
        String uri = clientServer.getHttpsString() + part2;
        Log.info(thisClass, "_testName", "backchannelLogouturi: " + uri);
        testClient = client;

        return uri;
    }

    /**
     * Creates a list of parameters that will be added to the logout request. In our case, the list only contains one pair.
     * { "logout_token", built_logout_token}
     * This method tells the method that actually builds the parms to encrypt the token that it builds.
     *
     * @param builder
     *            the populated builder to use to generate the actual logout token
     * @param encrypt
     *            flag indicating if the token will be encrypted (or just signed)
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder, boolean encrypt) throws Exception {
        return createParmFromBuilder(builder, encrypt, "JOSE", "jwt");
    }

    /**
     * Creates a list of parameters that will be added to the logout request. In our case, the list only contains one pair.
     * This method will build either a signed token, or a signed and encrypted token
     *
     *
     * @param builder
     *            the populated builder to use to generate the actual logout token
     * @param encrypt
     *            flag indicating if the token should be signed and encrypted, or just signed
     * @param type
     *            if the token is to be encrypted, this is the value of the "typ" claim
     * @param contentType
     *            if the token is to be encrypted, this is the value of the "cty" claim
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder, boolean encrypt, String type, String contentType) throws Exception {

        String thisMethod = "createParmFromBuilder";

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        String logoutToken = null;
        if (encrypt) {
            logoutToken = builder.buildJWE(type, contentType);
        } else {
            logoutToken = builder.build();
        }

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        if (logoutToken == null) {
            fail("Test failed to create the logout token");
        }

        return parms;

    }

    /**
     * Creates and populates a JWT Builder using default values. It uses that builder to generate a signed logout token.
     *
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createDefaultLogoutParm() throws Exception {

        String thisMethod = "createDefaultLogoutParm";

        JWTTokenBuilder b = setDefaultLogoutToken();
        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    /**
     * Creates and populates a JWT Builder using default values. It then removes the claim specified and finally uses that builder
     * to generate a signed logout token.
     *
     * @param claim
     *            claim to remove from the logout token
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createLogoutParm_removingOne(String claim) throws Exception {

        String thisMethod = "createLogoutParm_removingOne";

        JWTTokenBuilder b = setDefaultLogoutToken();

        Log.info(thisClass, thisMethod, "Removing: " + claim);
        b.unsetClaim(claim);

        String logoutToken = b.buildAsIs();

        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());
        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    /**
     * Creates and populates a JWT Builder using default values. It then updates or adds the claim with the value specified and
     * finally uses that builder to generate a signed logout token.
     *
     * @param claim
     *            the claim to add or update (if it already exists)
     * @param value
     *            the value to use for the added/updated claim
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createLogoutParm_addOrOverrideOne(String claim, Object value) throws Exception {

        String thisMethod = "createLogoutParm_addOrOverrideOne";

        JWTTokenBuilder b = setDefaultLogoutToken();

        Log.info(thisClass, thisMethod, "Adding or updating: " + claim + " with value: " + value.toString());
        b.setClaim(claim, value);

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());
        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    /**
     * Creates and populates a JWT Builder using default values. It then removes the specified claim and updates or adds the other
     * claim with the value specified and finally uses that builder to generate a signed logout token.
     *
     * @param removeClaim
     *            claim to remove from the logout token
     * @param addOverrideClaim
     *            the claim to add or update (if it already exists)
     * @param addOverrideValue
     *            the value to use for the added/updated claim
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createLogoutParm_removingOne_addOrOverrideOne(String removeClaim, String addOverrideClaim, Object addOverrideValue) throws Exception {

        String thisMethod = "createLogoutParm_removingOne_addOrOverrideOne";

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

    /**
     * Creates a JWTBuilder with the default content for these tests
     *
     * @return - returns a builder populated with the default content
     * @throws Exception
     */
    public JWTTokenBuilder setDefaultLogoutToken() throws Exception {

        JWTTokenBuilder b = setHSASignedBuilderForLogoutToken(AlgorithmIdentifiers.HMAC_SHA256);
        return b;
    }

    //    /**
    //     * Creates a JWTBuilder with the default claim content and signing settings based on the HS alg passed in
    //     *
    //     * @param hsaAlg
    //     *            - The HS algorithm that will be used to sign the logout token
    //     * @return - returns a builder populated with the default claim content and specified HS signing info
    //     * @throws Exception
    //     */
    //    public JWTTokenBuilder setDefaultHSALogoutToken(String hsaAlg) throws Exception {
    //
    //        Log.info(thisClass, "setDefaultLogoutToken", "creating token - signing with " + hsaAlg);
    //        JWTTokenBuilder b = setHSASignedBuilderForLogoutToken(hsaAlg);
    //        Log.info(thisClass, "setDefaultLogoutToken", "claims: " + b.getJsonClaims());
    //        return b;
    //    }

    public JWTTokenBuilder setNonHSASignedBuilderForLogoutToken(String alg) throws Exception {

        JWTTokenBuilder builder = setBuilderClaimsForLogoutToken();

        String fullPathKeyFile = serverFileUtils.getServerFileLoc(clientServer.getServer()) + "/" + alg + "private-key.pem";
        Log.info(thisClass, "setNonHSASignedBuilderForLogoutToken", "Using private key from: " + fullPathKeyFile);
        builder.setAlorithmHeaderValue(alg);
        if (alg.contains("RS")) {
            builder.setRSAKey(fullPathKeyFile);
        } else {
            builder.setECKey(fullPathKeyFile.replace("key", "key-pkcs#8"));
        }

        Log.info(thisClass, "setNonHSASignedBuilderForLogoutToken", "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Creates and populates a JWTBuilder with our test's default claim values and then sets the signature settings for the RS or
     * ES algorithm specified - set the encryption header claims alg and enc to default test values as well as setting signing and
     * encryption algorithms based on the algorithm passed in.
     *
     * @param alg
     *            the alg to use for signing and encrypting
     * @return - return a default populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder setNonHSAEncryptedBuilderForLogoutToken(String alg) throws Exception {
        if (alg.startsWith("ES")) {
            return setNonHSAEncryptedBuilderForLogoutToken(alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES);
        } else {
            return setNonHSAEncryptedBuilderForLogoutToken(alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
        }

    }

    /**
     * Creates and populates a JWTBuilder with our test's default claim values and then sets the signature settings for the RS or
     * ES algorithm specified - set the encryption header claims alg and enc claims based on the values passed in as well as
     * setting signing and
     * encryption algorithms based on the algorithm passed in.
     *
     * @param alg
     *            the alg to use for signing and encrypting
     * @param encryptAlg
     *            the value to set the "alg" header claim to
     * @param keyMgmtKeyAlg
     *            the value to set the "enc" header claim to
     * @return - return a populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder setNonHSAEncryptedBuilderForLogoutToken(String alg, String encryptAlg, String keyMgmtKeyAlg) throws Exception {

        JWTTokenBuilder builder = setNonHSASignedBuilderForLogoutToken(alg);

        builder.setKeyManagementKey(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(clientServer.getServer(), alg)));
        builder = builder.setContentEncryptionAlg(encryptAlg);
        builder = builder.setKeyManagementKeyAlg(keyMgmtKeyAlg);

        Log.info(thisClass, "setNonHSAEncryptedBuilderForLogoutToken", "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Creates and populates a JWTBuilder with our test's default claim values and then sets the signature settings for the HS
     * algorithm specified
     *
     * @param alg
     *            - the HS algorithm to use for signing
     * @return - return a default populated builder with HS signature settings based on alg
     * @throws Exception
     */
    public JWTTokenBuilder setHSASignedBuilderForLogoutToken(String alg) throws Exception {

        JWTTokenBuilder builder = setBuilderClaimsForLogoutToken();

        Log.info(thisClass, "setHSASignedBuilderForLogoutToken", "HS alg: " + alg + " HSAKey: " + sharedHSSharedKey);
        builder = builder.setAlorithmHeaderValue(alg);

        builder = builder.setHSAKey(sharedHSSharedKey); // using the same secret in all of our HS configs
        Log.info(thisClass, "setHSASignedBuilderForLogoutToken", "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Create and popluate a JWTBuilder with our test's default claim values.
     *
     * @return - return a default populated builder
     * @throws Exception
     */
    public JWTTokenBuilder setBuilderClaimsForLogoutToken() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("testIssuer"); // required
        builder.setSubject("testuser"); // optional

        builder.setAudience(testClient); // required

        builder.setIssuedAtToNow(); // required
        builder.setGeneratedJwtId(); // will ensure a unique jti for each test

        JSONObject events = new JSONObject();
        events.put("http://schemas.openid.net/event/backchannel-logout", new JSONObject());
        builder.setClaim("events", events); // required

        return builder;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for a missing
     * claim message.
     *
     * @return standard expectations for an invalid login token + a missing claim error message
     * @throws Exception
     */
    public List<validationData> setMissingBackChannelLogoutRequestClaim(String claim) throws Exception {

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim [" + claim + "] was missing.", MessageConstants.CWWKS1545E_BACK_CHANNEL_LOGOUT_MISSING_REQUIRED_CLAIM);

        return expectations;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for an error
     * that the caller passes in.
     *
     * @return standard expectations for an invalid login token + the caller's error
     * @throws Exception
     */
    public List<validationData> setInvalidBackChannelLogoutRequest(String specificMsg) throws Exception {

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        if (specificMsg != null && !specificMsg.equals("")) {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim contained an invalid value.", specificMsg);
        }

        return expectations;

    }

    /**
     * Create the general expectations for a logout token that contains something invalid.
     * We expect a status code of 400, with a response message of "Bad Request". We'll also receive msgs CWWKS1541E and CWWKS1543E
     * in the RP's message log.
     *
     * @return standard expectations for an invalid login token
     * @throws Exception
     */
    public List<validationData> setInvalidBackChannelLogoutRequest() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
        return expectations;

    }

}

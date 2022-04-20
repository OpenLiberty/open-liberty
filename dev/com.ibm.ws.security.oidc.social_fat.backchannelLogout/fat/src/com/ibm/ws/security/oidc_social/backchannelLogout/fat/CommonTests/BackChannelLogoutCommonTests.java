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
import java.util.Map;
import java.util.Map.Entry;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
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
    protected static boolean defaultUseLdap = useLdap;
    protected static final String logoutEventKey = "http://schemas.openid.net/event/backchannel-logout";
    protected static final boolean sidIsRequired = true;
    protected static final boolean sidIsNotRequired = false;

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

    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder) throws Exception {
        return createParmFromBuilder(builder, false, "JOSE", "jwt");
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
            logoutToken = builder.buildAsIs();
        }

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        if (logoutToken == null) {
            fail("Test failed to create the logout token");
        }

        return parms;

    }

    /**
     * Updates a JWTBuilder with the signature settings for the HS algorithm specified
     *
     * @param builder
     *            the JWTTokenBuilder to update signature settings
     * @param alg
     *            - the HS algorithm to use for signing
     * @return - return an updated builder with HS signature settings based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithHSASignatureSettings(JWTTokenBuilder builder, String alg) throws Exception {

        String thisMethod = "updateLogoutTokenBuilderWithHSASignatureSettings";

        Log.info(thisClass, thisMethod, "HS alg: " + alg + " HSAKey: " + sharedHSSharedKey);
        builder = builder.setAlorithmHeaderValue(alg);

        builder = builder.setHSAKey(sharedHSSharedKey); // using the same secret in all of our HS configs
        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Updates a JWTBuilder with the signature settings for the non HS algorithm specified (RS or ES)
     *
     * @param builder
     *            the JWTTokenBuilder to update signature settings
     * @param alg
     *            - the non-HS algorithm to use for signing
     * @return - return an updated builder with non-HS signature settings based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureSettings(JWTTokenBuilder builder, String alg) throws Exception {

        String thisMethod = "setNonHSASignedBuilderForLogoutToken";

        String fullPathKeyFile = serverFileUtils.getServerFileLoc(clientServer.getServer()) + "/" + alg + "private-key.pem";
        Log.info(thisClass, thisMethod, "Using private key from: " + fullPathKeyFile);
        builder.setAlorithmHeaderValue(alg);
        if (alg.contains("RS")) {
            builder.setRSAKey(fullPathKeyFile);
        } else {
            builder.setECKey(fullPathKeyFile.replace("key", "key-pkcs#8"));
        }

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Calls updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings with the appropriate JWE header values based on the
     * alg (header values are different for RS/ES).
     * updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings will update the JWTBuilder with the signature settings for
     * the RS or
     * ES algorithm specified - sets the encryption header claims alg and enc to default test values as well as setting encryption
     * key based on the algorithm passed in.
     *
     * @param builder
     *            the JWTTokenBuilder to update signature/encryption settings
     * @param alg
     *            the alg to use for signing and encrypting
     * @return - return a default populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(JWTTokenBuilder builder, String alg) throws Exception {
        if (alg.startsWith("ES")) {
            return updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES);
        } else {
            return updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
        }

    }

    /**
     * Update the JWTBuilder with the signature settings for the RS or ES algorithm specified - sets the encryption header claims
     * alg and enc to default test values as well as setting encryption key based on the algorithm passed in.
     *
     * @param builder
     *            the JWTTokenBuilder to update signature/encryption settings
     * @param alg
     *            the alg to use for signing and encrypting
     * @param encryptAlg
     *            the value to set the "alg" header claim to
     * @param keyMgmtKeyAlg
     *            the value to set the "enc" header claim to
     * @return - return a populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(JWTTokenBuilder builder, String alg, String encryptAlg, String keyMgmtKeyAlg) throws Exception {

        String thisMethod = "updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings";

        // update the signature settings
        builder = updateLogoutTokenBuilderWithNonHSASignatureSettings(builder, alg);

        Log.info(thisClass, thisMethod, "alg: " + alg + " encryptAlg: " + encryptAlg + " keyMgmtKeyAlg: " + keyMgmtKeyAlg);

        // get the key
        String rawKey = JwtKeyTools.getComplexPublicKeyForSigAlg(clientServer.getServer(), alg);

        // update the builder's encryption settings
        builder.setKeyManagementKey(JwtKeyTools.getPublicKeyFromPem(rawKey));
        builder = builder.setContentEncryptionAlg(encryptAlg);
        builder = builder.setKeyManagementKeyAlg(keyMgmtKeyAlg);

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Creates and returns a JWTTokenBuilder with the values found in an id_token (caller has parsed the id_token and saved the
     * values in a JwtTokenForTest object)
     *
     * @param idTokenData
     *            the JwtTokenForTest containing the id_token content
     * @return a JWTTokenBuilder object containing the values from the id_token
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderFromIdToken(JwtTokenForTest idTokenData) throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();

        Map<String, Object> idTokenDataHeaders = idTokenData.getMapHeader();
        String alg = getStringValue(idTokenDataHeaders, Constants.HEADER_ALGORITHM);
        if (alg == null) {
            fail("Signature algorithm was missing in the id_token - that shouldn't happen");
        }
        builder = updateLogoutTokenBuilderWithHSASignatureSettings(builder, alg);

        Map<String, Object> idTokenDataClaims = idTokenData.getMapPayload();

        String issuer = getStringValue(idTokenDataClaims, Constants.PAYLOAD_ISSUER); // required
        builder.setIssuer((issuer != null) ? issuer : "IssuerNotInIdToken");

        String subject = getStringValue(idTokenDataClaims, Constants.PAYLOAD_SUBJECT); // optional (our id_token should always have it)
        if (subject != null) {
            builder.setSubject(subject);
        }

        String sessionId = getStringValue(idTokenDataClaims, Constants.PAYLOAD_SESSION_ID); // optional (our id_token should always have it)
        if (subject != null) {
            builder.setClaim(Constants.PAYLOAD_SESSION_ID, sessionId);
        }

        String audience = getStringValue(idTokenDataClaims, Constants.PAYLOAD_AUDIENCE); // required
        builder.setAudience((audience != null) ? audience : "AudienceNotInIdToken");

        builder.setIssuedAtToNow(); // required
        builder.setGeneratedJwtId(); // will ensure a unique jti for each test

        JSONObject events = new JSONObject();
        events.put(logoutEventKey, new JSONObject());
        builder.setClaim("events", events); // required

        return builder;
    }

    /**
     * Invokes a protected client on the RP/Social client and logs in to get access.
     * Grab the id_token from that response.
     * Use the values from that id_token to populate a jwt builder which will later be used to create a logout_token
     *
     * @param client
     *            - the RP client that we'll be using - the app contains this name and our filters direct us to use this client
     * @return a jwt builder populated with values from the generated id_token
     * @throws Exception
     */
    public JWTTokenBuilder loginAndReturnIdTokenData(String client) throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + client);

        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes(); // this call will also add the successful status check for logout
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, clientServer.getHttpString() + "/backchannelLogoutTestApp/logBackChannelLogoutUri");

        Object response = genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = null;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            id_token = validationTools.getIDTokenFromOutput(response);
        } else {
            id_token = validationTools.getTokenFromResponse(response, "ID token:");
        }
        Log.info(thisClass, _testName, "id token: " + id_token);
        JwtTokenForTest idTokenData = gatherDataFromToken(id_token, updatedTestSettings);

        JWTTokenBuilder builder = createBuilderFromIdToken(idTokenData);

        return builder;

    }

    /**
     * Invoke the back channel logout endpoint validating the response
     *
     * @param bclEndpoint
     *            the endpoint to invoke
     * @param parms
     *            the parms to pass (the logout_token parm and value)
     * @param expectations
     *            what to expect (status code and possible messages in the server side log)
     * @throws Exception
     */
    public void invokeBcl(String bclEndpoint, List<endpointSettings> parms, List<validationData> expectations) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        genericInvokeEndpoint(_testName, webClient, null, bclEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    private String getStringValue(Map<String, Object> claims, String key) throws Exception {

        Object objValue = claims.get(key);
        if (objValue != null) {
            Log.info(thisClass, "getStringValue", "value: " + objValue.toString() + " value type: " + objValue.getClass());
            String value = objValue.toString().replace("\"", "");
            return value;
        }
        return null;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for a missing
     * claim message.
     *
     * @return standard expectations for an invalid login token + a missing claim error message
     * @throws Exception
     */
    public List<validationData> setMissingBCLRequestClaimExpectations(String claim) throws Exception {

        List<validationData> expectations = setInvalidBCLRequestExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim [" + claim + "] was missing.", MessageConstants.CWWKS1545E_BACK_CHANNEL_LOGOUT_MISSING_REQUIRED_CLAIM + ".*" + claim);
        return expectations;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for an error
     * that the caller passes in.
     *
     * @return standard expectations for an invalid login token + the caller's error
     * @throws Exception
     */
    public List<validationData> setInvalidBCLRequestExpectations(String specificMsg) throws Exception {

        List<validationData> expectations = setInvalidBCLRequestExpectations();
        if (specificMsg != null && !specificMsg.equals("")) {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain expected message [" + specificMsg + "].", specificMsg);
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
    public List<validationData> setInvalidBCLRequestExpectations() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
        return expectations;

    }

    public String getLogoutTokenFromOutput(String tokenName, Object response) throws Exception {

        String thisMethod = "getIDTokenFromOutput";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        String respReceived = AutomationTools.getResponseText(response);

        if (!respReceived.contains(tokenName)) {
            throw new Exception("logout_token is missing");
        }

        int start = respReceived.indexOf(tokenName);
        String theValue = respReceived.substring(start + tokenName.length(), respReceived.length());
        Log.info(thisClass, thisMethod, tokenName + " " + theValue);
        if (!theValue.isEmpty()) {
            return theValue;
        }

        throw new Exception("logout_token is missing");

    }

    public JwtTokenForTest gatherDataFromToken(String token, TestSettings settings) throws Exception {

        String thisMethod = "gatherDataFromIdToken";

        try {
            validationTools.setExpectedSigAlg(settings);

            String decryptKey = settings.getDecryptKey();

            JwtTokenForTest jwtToken;
            if (JweHelper.isJwe(token) && decryptKey != null) {
                jwtToken = new JwtTokenForTest(token, decryptKey);
            } else {
                jwtToken = new JwtTokenForTest(token);
            }

            jwtToken.printJwtContent();

            return jwtToken;

        } catch (

        Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating id_token in response");
            throw e;
        }
    }

    /**
     * Update/Set config variables for a server and push the updates to the server.
     * Method waits for server to update or indicate that no update in needed
     *
     * @param server
     *            - ref to server that will be updated
     * @param valuesToSet
     *            - a map of the variables and their values to set
     * @throws Exception
     */
    public void updateServerSettings(TestServer server, Map<String, String> valuesToSet) throws Exception {

        String thisMethod = "updateServerSettings";
        ServerConfiguration config = server.getServer().getServerConfiguration();
        ConfigElementList<Variable> configVars = config.getVariables();

        for (Variable variableEntry : configVars) {
            Log.info(thisClass, thisMethod, "Already set configVar: " + variableEntry.getName() + " configVarValue: " + variableEntry.getValue());
        }

        for (Entry<String, String> variableEntry : valuesToSet.entrySet()) {
            updateConfigVariable(configVars, variableEntry.getKey(), variableEntry.getValue());
        }

        server.getServer().updateServerConfiguration(config);
        server.getServer().waitForConfigUpdateInLogUsingMark(null);
        //        helpers.testSleep(5);
    }

    /**
     * Update a servers variable map with the key/value passed in.
     *
     * @param vars
     *            - map of existing variables
     * @param name
     *            - the key to add/update
     * @param value
     *            - the value for the key specified
     */
    protected void updateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {

        Variable var = vars.getBy("name", name);
        if (var == null) {
            Log.info(thisClass, "updateConfigVariable", name + " doesn't appear to exist, so no update is needed.");
        } else {
            var.setValue(value);
        }
    }

}

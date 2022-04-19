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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
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
    protected static final String defaultLogoutPage = "/oidc/end_session_logout.html";
    protected static final String postLogoutJSessionIdApp = "/backchannelLogoutTestApp/backChannelLogoutJSessionId";
    protected static final String simpleLogoutApp = "simpleLogoutTestApp";

    protected static final boolean sidIsRequired = true;
    protected static final boolean sidIsNotRequired = false;
    protected static final boolean doNotNeedToLogin = true;
    protected static final boolean needToLogin = false;
    protected static final boolean successfulOPLogout = true;
    protected static final boolean unsuccessfulOPLogout = false;
    protected static final boolean successfulRPLogout = true;
    protected static final boolean unsuccessfulRPLogout = false;
    protected static final boolean refreshTokenValid = true;
    protected static final boolean refreshTokenInvalid = false;

    public String testClient = null;
    public static TestServer clientServer = null;
    public static TestServer clientServer2 = null;
    protected static String opCookieName = "testOPCookie";
    protected static String clientCookieName = null;
    protected static String client2CookieName = null;
    protected static String logoutMethodTested = Constants.END_SESSION;

    @AfterClass
    public static void afterClass() {
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + defaultUseLdap);
        useLdap = defaultUseLdap;
    }

    /**
     * Build the backchannel logout url based on the rp server hostinfo and the client to be "logged out"
     *
     * @param client
     *            client name
     * @return - the logout url
     * @throws Exception
     */
    public String buildBackchannelLogoutUri(String client) throws Exception {
        return buildBackchannelLogoutUri(clientServer, client);
    }

    public String buildBackchannelLogoutUri(TestServer server, String client) throws Exception {

        String contextRoot = null;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            contextRoot = Constants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT;
        } else {
            contextRoot = SocialConstants.DEFAULT_CONTEXT_ROOT;
        }
        String part2 = (contextRoot + Constants.OIDC_BACK_CHANNEL_LOGOUT_ROOT + client).replace("//", "/");
        String uri = server.getHttpsString() + part2;
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

    public Object accessProtectedApp(String client) throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + client);

        return accessProtectedApp(webClient, updatedTestSettings);

    }

    public Object accessProtectedApp(WebClient webClient, TestSettings settings) throws Exception {
        return accessProtectedApp(webClient, null, settings);
    }

    public Object accessProtectedApp(WebClient webClient, Object previousResponse, TestSettings settings) throws Exception {

        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());

        Object response = genericRP(_testName, webClient, settings, previousResponse, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        return response;
    }

    /**
     * Attempt to access the protected app after we've run logout/end_session.
     * We'll only attempt the first step of invoking the protected app. If the webClient still has cookies, we'll get to the app,
     * otherwise, we'll be prompted to log in.
     *
     * @param webClient
     *            the context to use
     * @param settings
     *            test case settings to use to make the request
     * @param alreadyLoggedIn
     *            flag indicating that if we should or should not have access to protected app without having to log in
     * @throws Exception
     */
    public void accessAppAfterLogout(WebClient webClient, TestSettings settings, boolean alreadyLoggedIn) throws Exception {

        List<validationData> postLogoutExpectations = vData.addSuccessStatusCodes();
        // make sure we landed on the app
        if (alreadyLoggedIn) {
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
        } else {
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getTestURL());
        }

        genericRP(_testName, webClient, settings, Constants.GET_LOGIN_PAGE_ONLY, postLogoutExpectations);

    }

    /**
     * Validate tokens/app access after logout/end_session completes.
     * Flags indicating if the OP and RP steps have completed successfully will be used to determine if app access and refresh
     * tokens should be good/invalid
     *
     * @param webClient
     *            the context to use
     * @param settings
     *            test case settings to use to make the requests
     * @param refresh_token
     *            the refresh_token that we should check
     * @param wasOPLogoutSuccessfull
     *            flag indicating that the OP's logout steps should have completed successfully (or not)
     * @param wasRPLogoutSuccessfull
     *            flag indicating that the RP's logout steps should have compelted successfully (or not)
     * @throws Exception
     */
    public void validateLogoutResult(WebClient webClient, TestSettings settings, String clientCookie, String refresh_token, String previousJSessionCookie, boolean wasOPLogoutSuccessfull, boolean wasRPLogoutSuccessfull) throws Exception {

        validateOPCookies(webClient, wasOPLogoutSuccessfull);
        validateRPCookies(webClient, clientCookie, previousJSessionCookie, wasRPLogoutSuccessfull);

        if (wasOPLogoutSuccessfull && wasRPLogoutSuccessfull) {
            // try to access the protected app using the existing session
            // the logout steps on both the OP and RP succeeded, so, we should have to login again
            accessAppAfterLogout(webClient, settings, false);
            refreshTokens(webClient, settings, refresh_token, refreshTokenInvalid);
        } else {
            // either the OP or RP logout did not complete, so, we should still have access to the app
            accessAppAfterLogout(webClient, settings, true);
            if (wasOPLogoutSuccessfull && logoutMethodTested.equals(Constants.END_SESSION)) {
                refreshTokens(webClient, settings, refresh_token, refreshTokenInvalid);
            } else {
                refreshTokens(webClient, settings, refresh_token, refreshTokenValid);
            }
        }

    }

    /**
     * Invoke the refresh_token endpoint after logging out - check that the request succeeds or fails based on the flag passed in
     * (it should basically fail when the BCL succeeded)
     *
     * @param webClient
     *            the context to use
     * @param settings
     *            test case settings to use to make the requests
     * @param refresh_token
     *            the refresh_token that we should check
     * @param refreshTokenInvalid
     *            flag indicating if the refresh_token is still valid or not
     * @throws Exception
     */
    // TODO - do we need to make sure that the new access_token is valid?  we've already tested that
    public void refreshTokens(WebClient webClient, TestSettings settings, String refresh_token, boolean isRefreshTokenValid) throws Exception {

        List<validationData> refreshLogoutExpectations = null;

        if (isRefreshTokenValid) {
            refreshLogoutExpectations = vData.addSuccessStatusCodes();
            refreshLogoutExpectations = vData.addExpectation(refreshLogoutExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        } else {
            // set expectations for refresh token no longer valid
            refreshLogoutExpectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_REFRESH_ENDPOINT);
            refreshLogoutExpectations = vData.addExpectation(refreshLogoutExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not receive error message trying to refresh token", null, ".*" + Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        }
        invokeGenericForm_refreshToken(_testName, webClient, settings, refresh_token, refreshLogoutExpectations);

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
        //        WebClient webClient = getAndSaveWebClient(true);
        //
        //        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        //        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + client);
        //
        //        // Access a protected app - using a normal RP flow
        //        List<validationData> expectations = vData.addSuccessStatusCodes(); // this call will also add the successful status check for logout
        //        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, clientServer.getHttpString() + "/backchannelLogoutTestApp/backChannelLogoutUri");
        //
        //        Object response = genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        Object response = accessProtectedApp(client);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = null;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            id_token = validationTools.getIDTokenFromOutput(response);
        } else {
            id_token = validationTools.getTokenFromResponse(response, "ID token:");
        }
        Log.info(thisClass, _testName, "id token: " + id_token);
        JwtTokenForTest idTokenData = gatherDataFromToken(id_token, testSettings); // right now none of the tests need a diff sig alg, if they do, we'll need to pass an updated testSettings into this method

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

    public List<validationData> initLogoutExpectations(String logoutPage) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        if (logoutPage.equals(postLogoutJSessionIdApp)) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the post back channel logout test app", null, postLogoutJSessionIdApp);
        } else {
            if (logoutPage.equals(simpleLogoutApp)) {
                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the simple logout test app", null, simpleLogoutApp);
            } else {
                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, testOPServer.getHttpsString() + defaultLogoutPage);
            }
        }

        return expectations;
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
    public List<validationData> setInvalidBCLRequestExpectations(String specificMsg) throws Exception {

        List<validationData> expectations = setInvalidBCLRequestExpectations();
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
    public List<validationData> setInvalidBCLRequestExpectations() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
        return expectations;

    }

    //TODO - remove as response from logout (using post logout redirect is TextPage and we can't get cookies from it)
    public List<validationData> addRPLogoutCookieExpectations(List<validationData> expectations, String clientCookie, String jSessionCookie, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + clientCookie + "\" was found in the response and should not have been.", null, clientCookie);
            if (jSessionCookie == null) {
                fail("addRPLogoutCookieExpectations failure: Could not set up expectation for JSESSIONID check - no JSESSIONID value found/passed in");
            }
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_MATCH, "Cookie JSESSIONID was found in the response with a value that should have been updated.", null, "JSESSIONID.*" + jSessionCookie + ".*");
            // previous JSession should not match current jSession
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + clientCookie + "\" was NOT found in the response and should have been.", null, clientCookie);
        }
        return expectations;
    }

    public List<validationData> addOPLogoutCookieExpectations(List<validationData> expectations, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + opCookieName + "\" was found in the response and should not have been.", null, opCookieName);
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + opCookieName + "\" was NOT found in the response and should have been.", null, opCookieName);
        }
        return expectations;
    }

    public void validateRPCookies(WebClient webClient, String cookieName, String previousJSessionCookie, boolean shouldNotExist) throws Exception {

        String cookieValue = getCookieValue(webClient, cookieName);
        String currentJSessionCookieValue = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);
        if (shouldNotExist) {
            if (cookieValue != null) {
                fail("Cookie \"" + cookieName + "\" was found in the response and should not have been.");
            }
            if (previousJSessionCookie == null) {
                fail("validateRPCookies failure: Could not validate the JSESSIONID cookie - the previous value of the JSESSIONID cookie was not provided.");
            } else {
                if (currentJSessionCookieValue != null && previousJSessionCookie.equals(currentJSessionCookieValue)) {
                    fail("The original JSESSIONID was not removed");
                }
                Log.info(thisClass, "validateRPCookies", "Current JSESSIONID value [" + currentJSessionCookieValue + "] did not match the previous value [" + previousJSessionCookie + "]");
            }
        } else {
            if (cookieValue == null) {
                fail("Cookie \"" + cookieName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, "validateRPCookies", "The cookie [" + cookieName + "] was found as it should have been.");
        }

    }

    public void validateOPCookies(WebClient webClient, boolean shouldNotExist) throws Exception {

        String cookieValue = getCookieValue(webClient, opCookieName);
        if (shouldNotExist) {
            if (cookieValue != null) {
                fail("Cookie \"" + opCookieName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, "validateOPCookies", "The OP cookie [" + opCookieName + "] was NOT found as it should not have been.");
        } else {
            if (cookieValue == null) {
                fail("Cookie \"" + opCookieName + "\" was NOT found in the response and should not have been.");
            }
            Log.info(thisClass, "validateOPCookies", "The OP cookie [" + opCookieName + "] was found as it should have been.");
        }
    }

    public String getCookieValue(WebClient webClient, String cookieName) throws Exception {

        String cookieValue = null;
        CookieManager cm = webClient.getCookieManager();
        if (cm != null) {
            Cookie cookie = cm.getCookie(cookieName);
            if (cookie != null) {
                cookieValue = cookie.getValue();
            }
        }
        Log.info(thisClass, "getCookieValue", "CookieName: " + cookieName + " CookieValue: " + cookieValue);
        return cookieValue;
    }

    public String getRefreshToken(Object response) throws Exception {

        String refreshKey = Constants.REFRESH_TOKEN_KEY;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(SocialConstants.SOCIAL)) {
            refreshKey = "refresh token";
        }

        String refreshToken = validationTools.getTokenFromResponse(response, refreshKey);
        Log.info(thisClass, "getRefreshToken", "Refresh Token: " + refreshToken);
        return refreshToken;

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
        String theValue = respReceived.substring(start + tokenName.length(), respReceived.length() - 1).split(System.getProperty("line.separator"))[0];
        Log.info(thisClass, thisMethod, tokenName + " " + theValue);
        if (!theValue.isEmpty()) {
            return theValue;
        }

        throw new Exception("logout_token is missing");

    }

    public String getLogoutTokenFromMessagesLog(TestServer server, String tokenName) throws Exception {

        String thisMethod = "getLogoutTokenFromMessagesLog";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        String searchResult = server.getServer().waitForStringInLogUsingMark(tokenName, server.getServer().getMatchingLogFile(Constants.MESSAGES_LOG));
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        Log.info(thisClass, thisMethod, searchResult);
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        if (searchResult != null) {
            int start = searchResult.indexOf(tokenName);
            int len = tokenName.length();
            if (start == -1) {
                start = searchResult.indexOf("logout_token: ");
                len = "logout_token: ".length();
            }
            Log.info(thisClass, thisMethod, "start: " + start + " length: " + searchResult.length());
            String theValue = searchResult.substring(start + len, searchResult.length() - 1);
            Log.info(thisClass, thisMethod, tokenName + " " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }
        }
        return searchResult;

        //        String respReceived = AutomationTools.getResponseText(response);
        //
        //        if (!respReceived.contains(tokenName)) {
        //            throw new Exception("logout_token is missing");
        //        }
        //
        //        int start = respReceived.indexOf(tokenName);
        //        String theValue = respReceived.substring(start + tokenName.length(), respReceived.length() - 1);
        //        Log.info(thisClass, thisMethod, tokenName + " " + theValue);
        //        if (!theValue.isEmpty()) {
        //            return theValue;
        //        }
        //
        //        throw new Exception("logout_token is missing");

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
    public static void updateServerSettings(TestServer server, Map<String, String> valuesToSet) throws Exception {

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
    protected static void updateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {

        Variable var = vars.getBy("name", name);
        if (var == null) {
            Log.info(thisClass, "updateConfigVariable", name + " doesn't appear to exist, so no update is needed.");
        } else {
            var.setValue(value);
        }
    }

    public static void updateClientCookieNameAndPort(TestServer server, String cookieName, String cookieNameValue) throws Exception {

        Map<String, String> vars = new HashMap<String, String>();

        vars.put(cookieName, cookieNameValue);
        vars.put("client2Port", Integer.toString(server.getServerHttpsPort()));

        updateServerSettings(server, vars);

    }

}

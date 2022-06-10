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
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.AfterLogoutStates;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.TokenKeeper;

import componenttest.topology.utils.ServerFileUtils;

/**
 * This class supplies support methods to the back channel logout tests.
 */

public class BackChannelLogoutCommonTests extends CommonTest {

    protected static Class<?> thisClass = BackChannelLogoutCommonTests.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();
    public static EndpointSettings eSettings = new EndpointSettings();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static boolean defaultUseLdap = useLdap;
    public String testClient = null;
    public static TestServer clientServer = null;
    public static TestServer clientServer2 = null;
    protected static String logoutMethodTested = Constants.END_SESSION;
    protected static String sessionLogoutEndpoint = null;
    protected static String currentRepeatAction = null;

    //    protected boolean debug = true;
    protected boolean debug = false;

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
        if (currentRepeatAction.contains(Constants.OIDC)) {
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

        Log.info(thisClass, thisMethod, "HS alg: " + alg + " HSAKey: " + Constants.sharedHSSharedKey);
        builder = builder.setAlorithmHeaderValue(alg);

        builder = builder.setHSAKey(Constants.sharedHSSharedKey); // using the same secret in all of our HS configs
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

        String thisMethod = "createBuilderFromIdToken";
        msgUtils.printMethodName(thisMethod);

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
        events.put(Constants.logoutEventKey, new JSONObject());
        builder.setClaim("events", events); // required

        return builder;
    }

    public void restoreAppMap(String client) throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutUri/" + client,
                Constants.PUTMETHOD, "resetBCLLogoutTokenMap", null, null, vData.addSuccessStatusCodes(), testSettings);
    }

    public String getLogoutToken(String client) throws Exception {
        Object logoutResponse = genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutUri/" + client + "_postLogout",
                Constants.GETMETHOD, "getLogoutTokens", null, null, vData.addSuccessStatusCodes(), testSettings);

        String logoutToken = getLogoutTokenFromOutput(client + " - " + Constants.LOGOUT_TOKEN + ": ", logoutResponse);
        Log.info(thisClass, _testName, "Logout token: " + logoutToken);

        return logoutToken;
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

        String thisMethod = "accessProtectedApp";
        msgUtils.printMethodName(thisMethod);

        Object response = null;
        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());

        if (logoutMethodTested.equals(Constants.SAML)) {
            response = genericRP(_testName, webClient, settings, previousResponse, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT_WITH_SAML, expectations);
        } else {
            response = genericRP(_testName, webClient, settings, previousResponse, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        }
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
    public void accessAppAfterLogout(WebClient webClient, TestSettings settings, AfterLogoutStates states) throws Exception {

        String thisMethod = "accessAppAfterLogout";
        msgUtils.printMethodName("Start - " + thisMethod);

        List<validationData> postLogoutExpectations = vData.addSuccessStatusCodes();
        // make sure we landed on the app if any of the cookies exist
        //        if (states.getOpCookieExists() || states.getOpJSessionIdExists() || states.getClientCookieExists() || states.getClientJSessionIdExists()) {
        if (states.getAppSessionAccess()) {
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find the FormLoginServlet output in the response", null, Constants.FORMLOGIN_SERVLET);
        } else {
            states.setClientJSessionIdMatchesPrevious(false); // client JSEssionId will exist (and match the value from the login) before this attempt, it will exist but be a new value after
            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not Redirect to OP", null, "Redirect To OP");
            //            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
            //            postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getTestURL());
        }

        genericRP(_testName, webClient, settings, Constants.GET_LOGIN_PAGE_ONLY, postLogoutExpectations);

        msgUtils.printMethodName("End - " + thisMethod);

    }

    // TODO remove this method if we dod not end up testing with multiple "clients" (rp1 & rp2/social1 & social2)
    public void validateLogoutResult(WebClient webClient, TestSettings settings, TokenKeeper previousTokenKeeper, AfterLogoutStates states) throws Exception {
        // just use the default client cookie names (for what would be RP1/social client1 )
        validateLogoutResult(webClient, settings, Constants.clientCookieName, previousTokenKeeper, states);
    }

    /**
     * Validate tokens/app access after logout/end_session completes.
     * Flags indicating if the OP and RP steps have completed Successfuly will be used to determine if app access and refresh
     * tokens should be good/invalid
     *
     * @param webClient
     *            the context to use
     * @param settings
     *            test case settings to use to make the requests
     * @param refresh_token
     *            the refresh_token that we should check
     * @param wasOPLogoutSuccessful
     *            flag indicating that the OP's logout steps should have completed successfully (or not)
     * @param wasRPLogoutSuccessful
     *            flag indicating that the RP's logout steps should have compelted successfully (or not)
     * @throws Exception
     */
    public void validateLogoutResult(WebClient webClient, TestSettings settings, String clientCookieName, TokenKeeper previousTokenKeeper, AfterLogoutStates states) throws Exception {

        if (debug) {
            return;

        }

        // TODO - remove this and set the appropriate checking for SAML
        if (logoutMethodTested.equals(Constants.SAML)) {
            Log.info(thisClass, "validateLogoutResult", "*************************************");
            Log.info(thisClass, "validateLogoutResult", "* Skipping checks for SAML right now*");
            Log.info(thisClass, "validateLogoutResult", "*************************************");
            return;
        }

        CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
        String thisMethod = "validateLogoutResult";
        msgUtils.printMethodNameBlock(thisMethod);
        loggingUtils.logTestCaseInServerLog(clientServer.getServer(), _testName, "Starting " + thisMethod);

        // clientCookieName is simply used to determine if we're using RP1 or RP2 cookie and jsessionid names are based on which server we're using
        TokenKeeper currentTokenKeeper = new TokenKeeper(webClient);

        validateOPCookies(previousTokenKeeper, currentTokenKeeper, states);
        validateClientCookies(previousTokenKeeper, currentTokenKeeper, clientCookieName, states);

        // show that we can/can't access the protected app using just the webClient
        accessAppAfterLogout(webClient, settings, states);
        // revalidate client's jsessionid token - it may have been replaced
        TokenKeeper afterAccessAttemptTokenKeeper = new TokenKeeper(webClient);
        validateClientJSessionId(afterAccessAttemptTokenKeeper, currentTokenKeeper, clientCookieName, states);

        // show that can/can't access the protected app using the previously created access_token
        validateAccessToken(settings, previousTokenKeeper, states);
        //        // show that can/can't use the previously created refresh_token
        validateRefreshToken(settings, previousTokenKeeper, states);

        loggingUtils.logTestCaseInServerLog(clientServer.getServer(), _testName, "Ending " + thisMethod);

    }

    /**
     * Attempt to use the access_token from the original login - verify that it is valid or invalid based on the state value
     * passed
     *
     * @param settings
     *            test case settings to use to make the requests
     * @param tokenKeeper
     *            the TokenKeeper object containing the access_token to check
     * @param states
     *            the AfterLogoutStates object containing the flag indicating if the calling test expects the access_token to be
     *            valid at this time
     * @throws Exception
     */
    public void validateAccessToken(TestSettings settings, TokenKeeper tokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateAccessToken";
        msgUtils.printMethodName("Start - " + thisMethod);

        String action = "POST_LOGOUT_ACCESS_TOKEN_CHECK";

        List<validationData> accessTokenExpectations = null;

        if (states.getAccessTokenValid()) {
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getProtectedResource());
        } else {
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getProtectedResource());
            // TODO: Social and saml may require different messages
            if (testSettings.getFlowType().equals(SocialConstants.SOCIAL)) {

            } else {
                accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated (using introspection).", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);
                accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the inbound request was invalid.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
            }
        }

        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tokenKeeper.getAccessToken(), Constants.HEADER, settings, accessTokenExpectations, action);

        msgUtils.printMethodName("End - " + thisMethod);

    }

    /**
     * Invoke the refresh_token endpoint after logging out - check that the request succeeds or fails based on the flag passed in
     * (it should fail when the BCL succeeded)
     *
     * @param settings
     *            test case settings to use to make the requests
     * @param tokenKeeper
     *            the TokenKeeper object containing the refresh_token to check
     * @param states
     *            the AfterLogoutStates object containing the flag indicating if the calling test expects the refresh_token to be
     *            valid at this time
     * @throws Exception
     */
    public void validateRefreshToken(TestSettings settings, TokenKeeper tokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateRefreshToken";
        msgUtils.printMethodName("Start - " + thisMethod);

        List<validationData> refreshTokenExpectations = null;

        if (states.getRefreshTokenValid()) {
            refreshTokenExpectations = vData.addSuccessStatusCodes();
            refreshTokenExpectations = vData.addExpectation(refreshTokenExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        } else {
            // set expectations for refresh token no longer valid
            refreshTokenExpectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_REFRESH_ENDPOINT);
            refreshTokenExpectations = vData.addExpectation(refreshTokenExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not receive error message trying to refresh token", null, ".*" + Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        }
        invokeGenericForm_refreshToken(_testName, getAndSaveWebClient(true), settings, tokenKeeper.getRefreshToken(), refreshTokenExpectations);

        msgUtils.printMethodName("End - " + thisMethod);

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
        Object response = accessProtectedApp(client);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = null;
        if (currentRepeatAction.contains(Constants.OIDC)) {
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

        String logoutStep = ((logoutMethodTested.equals(Constants.SAML) ? Constants.PROCESS_LOGOUT_PROPAGATE_YES : Constants.LOGOUT));

        expectations = vData.addExpectation(expectations, logoutStep, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the post back channel logout test app", null, logoutPage);

        //        if (logoutPage.equals(Constants.postLogoutJSessionIdApp)) {
        //            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the post back channel logout test app", null, Constants.postLogoutJSessionIdApp);
        //        } else {
        //            if (logoutPage.equals(Constants.simpleLogoutApp)) {
        //                //                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the simple logout test app", null, simpleLogoutApp);
        //                //                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Logout app did not complete successfully", null, "Logout successful");
        //            } else {
        //                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, testOPServer.getHttpsString() + Constants.defaultLogoutPage);
        //            }
        //        }

        return expectations;
    }

    //    public List<validationData> initLogoutExpectations(String logoutStep, String finalPage) throws Exception {
    //
    //        List<validationData> expectations = vData.addSuccessStatusCodes();
    //            expectations = vData.addExpectation(expectations, logoutStep, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the post back channel logout test app", null, finalPage);
    //        return expectations;
    //    }
    //
    //    public String getFinalPage(boolean usesPostLogout) throws Exception {
    //
    //        if (currentRepeatAction.equals(Constants.END_SESSION)) {
    //            if (usesPostLogout) {
    //
    //            } else {
    //
    //            }
    //        }
    //        return null ;
    //    }
    //
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

    //TODO - remove as response from logout (using post logout redirect is TextPage and we can't get cookies from it)
    // TODO - handle client2?
    public List<validationData> addRPLogoutCookieExpectations(List<validationData> expectations, String clientCookie, String clientJSessionCookie, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + clientCookie + "\" was found in the response and should not have been.", null, clientCookie);
            if (clientJSessionCookie == null) {
                fail("addRPLogoutCookieExpectations failure: Could not set up expectation for JSESSIONID check - no " + Constants.clientJSessionIdName + " value found/passed in");
            }
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_MATCH, "Cookie " + Constants.clientJSessionIdName + " was found in the response with a value that should have been updated.", null, Constants.clientJSessionIdName + ".*" + clientJSessionCookie + ".*");
            // previous JSession should not match current jSession
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + clientCookie + "\" was NOT found in the response and should have been.", null, clientCookie);
        }
        return expectations;
    }

    public List<validationData> addOPLogoutCookieExpectations(List<validationData> expectations, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + Constants.opCookieName + "\" was found in the response and should not have been.", null, Constants.opCookieName);
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + Constants.opCookieName + "\" was NOT found in the response and should have been.", null, Constants.opCookieName);
        }
        return expectations;
    }

    public void validateClientCookies(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientCookieName, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateRPCookies";
        msgUtils.printMethodName("Start - " + thisMethod);

        validateClientCookie(beforeLogoutTokenKeeper, currentTokenKeeper, clientCookieName, states);
        // TODO - doesn't seem to be cleaned up??? validateClientJSessionId(beforeLogoutTokenKeeper, currentTokenKeeper, states);
        validateClientJSessionId(beforeLogoutTokenKeeper, currentTokenKeeper, clientCookieName, states);

        msgUtils.printMethodName("End - " + thisMethod);

    }

    public void validateClientCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientCookieName, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateClientCookie";
        msgUtils.printMethodName("Start - " + thisMethod);

        String beforeLogoutClientCookie = beforeLogoutTokenKeeper.getClientCookie();
        String currentClientCookie = currentTokenKeeper.getClientCookie();

        if (!clientCookieName.equals(Constants.clientCookieName)) {
            beforeLogoutClientCookie = beforeLogoutTokenKeeper.getClient2Cookie();
            currentClientCookie = currentTokenKeeper.getClient2Cookie();
        }

        if (states.getClientCookieExists()) {
            if (currentClientCookie == null) {
                fail("validateClientCookie failure: Cookie \"" + clientCookieName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, "validateClientCookies", "The Client cookie [" + clientCookieName + "] was found as it should have been.");
            if (states.getClientCookieMatchesPrevious()) {
                if (beforeLogoutClientCookie == null) {
                    fail("validateClientCookie failure: Could not validate the Client cookie [" + clientCookieName + "] - the previous value of the Client cookie was not provided.");
                }
                if (!beforeLogoutClientCookie.equals(currentClientCookie)) {
                    fail("validateClientCookie failure: The Client cookie [" + clientCookieName + "] with value [" + currentClientCookie + "] does not match the previous value [" + beforeLogoutClientCookie + "].");
                }
                Log.info(thisClass, "validateClientCookies", "The Client cookie [" + clientCookieName + "] value was valid.");
            }
        } else {
            if (currentClientCookie != null) {
                fail("validateClientCookie failure: Cookie \"" + clientCookieName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, "validateClientCookies", "The Client cookie [" + clientCookieName + "] was NOT found as it should not have been.");
        }
        msgUtils.printMethodName("End - " + thisMethod);

    }

    public void validateClientJSessionId(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientCookieName, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateClientJSessionId";
        msgUtils.printMethodName("Start - " + thisMethod);

        String beforeLogoutClientJSessionId = beforeLogoutTokenKeeper.getClientJSessionId();
        String currentClientJSessionId = currentTokenKeeper.getClientJSessionId();
        String clientJSessionIdName = Constants.clientJSessionIdName;

        if (!clientCookieName.equals(Constants.clientCookieName)) {
            beforeLogoutClientJSessionId = beforeLogoutTokenKeeper.getClient2JSessionId();
            currentClientJSessionId = currentTokenKeeper.getClient2JSessionId();
            clientJSessionIdName = Constants.client2JSessionIdName;
        }

        if (states.getClientJSessionIdExists()) {
            if (currentClientJSessionId == null) {
                fail("validateClientJSessionId failure: JSessionId \"" + clientJSessionIdName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, "validateClientJSessionId", "The Client cookie [" + clientJSessionIdName + "] was found as it should have been.");
            if (states.getClientJSessionIdMatchesPrevious()) {
                if (beforeLogoutClientJSessionId == null) {
                    fail("validateClientJSessionId failure: Could not validate the Client cookie [" + clientJSessionIdName + "] - the previous value of the Client cookie was not provided.");
                }
                if (!beforeLogoutClientJSessionId.equals(currentClientJSessionId)) {
                    fail("validateClientJSessionId failure: The Client cookie [" + clientJSessionIdName + "] with value [" + currentClientJSessionId + "] does not match the previous value [" + beforeLogoutClientJSessionId + "].");
                }
                Log.info(thisClass, "validateClientJSessionId", "The Client cookie [" + clientJSessionIdName + "] value was valid.");
            } else {
                if (beforeLogoutClientJSessionId != null && beforeLogoutClientJSessionId.equals(currentClientJSessionId)) {
                    fail("validateClientJSessionId failure: The Client cookie [" + clientJSessionIdName + "] with value [" + currentClientJSessionId + "] should not match the previous value [" + beforeLogoutClientJSessionId + "], but does.");
                }
                Log.info(thisClass, "validateClientJSessionId", "The Client cookie [" + clientJSessionIdName + "] value was valid.");
            }
        } else {
            if (currentClientJSessionId != null) {
                fail("validateClientJSessionId failure: JSessionId \"" + clientJSessionIdName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, "validateClientJSessionId", "The Client cookie [" + clientJSessionIdName + "] was NOT found as it should not have been.");
        }
        msgUtils.printMethodName("End - " + thisMethod);

    }

    public void validateOPCookies(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateOPCookies";
        msgUtils.printMethodName("Start - " + thisMethod);

        validateOPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states);
        validateOPJSessionId(beforeLogoutTokenKeeper, currentTokenKeeper, states);
        validateSPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states);
        validateIDPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states);

        msgUtils.printMethodName("End - " + thisMethod);
    }

    public void validateOPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateOPCookie";
        msgUtils.printMethodName("Start - " + thisMethod);

        if (states.getOpCookieExists()) {
            if (currentTokenKeeper.getOPCookie() == null) {
                fail("validateOPCookie failure: Cookie \"" + Constants.opCookieName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, "validateOPCookies", "The OP cookie [" + Constants.opCookieName + "] was found as it should have been.");
            if (states.getOpCookieMatchesPrevious()) {
                if (beforeLogoutTokenKeeper.getOPCookie() == null) {
                    fail("validateOPCookie failure: Could not validate the OP cookie [" + Constants.opCookieName + "] - the previous value of the OP cookie was not provided.");
                }
                if (!beforeLogoutTokenKeeper.getOPCookie().equals(currentTokenKeeper.getOPCookie())) {
                    fail("validateOPCookie failure: The OP cookie [" + Constants.opCookieName + "] with value [" + currentTokenKeeper.getOPCookie() + "] does not match the previous value [" + beforeLogoutTokenKeeper.getOPCookie() + "].");
                }
                Log.info(thisClass, "validateOPCookies", "The OP cookie [" + Constants.opCookieName + "] value was valid.");
            } else {
                // TODO - do we need to check to make sure that they don't match?
            }
        } else {
            if (currentTokenKeeper.getOPCookie() != null) {
                fail("validateOPCookie failure: Cookie \"" + Constants.opCookieName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, "validateOPCookies", "The OP cookie [" + Constants.opCookieName + "] was NOT found as it should not have been.");
        }

        msgUtils.printMethodName("End - " + thisMethod);

    }

    public void validateOPJSessionId(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateOPJSessionId";
        msgUtils.printMethodName("Start - " + thisMethod);

        if (states.getOpJSessionIdExists()) {
            if (currentTokenKeeper.getOPJSessionId() == null) {
                fail("validateOPJSessionId failure: Cookie \"" + Constants.opJSessionIdName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, "validateOPJSessionId", "The OP cookie [" + Constants.opJSessionIdName + "] was found as it should have been.");
            if (states.getOpJSessionIdMatchesPrevious()) {
                if (beforeLogoutTokenKeeper.getOPJSessionId() == null) {
                    fail("validateOPJSessionId failure: Could not validate the OP cookie [" + Constants.opJSessionIdName + "] - the previous value of the OP cookie was not provided.");
                }
                if (!beforeLogoutTokenKeeper.getOPJSessionId().equals(currentTokenKeeper.getOPJSessionId())) {
                    fail("validateOPJSessionId failure: The OP cookie [" + Constants.opJSessionIdName + "] with value [" + currentTokenKeeper.getOPJSessionId() + "] does not match the previous value [" + beforeLogoutTokenKeeper.getOPJSessionId() + "].");
                }
                Log.info(thisClass, "validateOPJSessionId", "The OP cookie [" + Constants.opJSessionIdName + "] value was valid.");
            }
        } else {
            if (currentTokenKeeper.getOPJSessionId() != null) {
                Log.info(thisClass, thisMethod, thisMethod, "validateOPJSessionId failure: Cookie \"" + Constants.opJSessionIdName + "\" was found in the response and should not have been.");
                // TODO - check is failing currently fail("validateOPJSessionId failure: Cookie \"" + Constants.opJSessionIdName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, "validateOPJSessionId", "The OP cookie [" + Constants.opJSessionIdName + "] was NOT found as it should not have been.");
        }

        msgUtils.printMethodName("End - " + thisMethod);

    }

    public void validateSPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateSPCookie";
        msgUtils.printMethodName("Start - " + thisMethod);
        // TODO
    }

    public void validateIDPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateIDPCookie";
        msgUtils.printMethodName("Start - " + thisMethod);
        // TODO

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
        String theValue = respReceived.substring(start + tokenName.length(), respReceived.length()).split(System.getProperty("line.separator"))[0];
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

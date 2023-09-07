/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.SSO.clientTests.commonTools;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.NumericDate;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This class contains some common tools used by tests that are verifying PKCE and private key jwt behavior.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class PKCEPrivateKeyJwtCommonTooling extends CommonTest {

    public static Class<?> thisClass = PKCEPrivateKeyJwtCommonTooling.class;
    public static TestServer clientServer = null;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    protected ArrayList<String> used_jtis = new ArrayList<String>();

    protected static final String S256 = "S256";
    protected static final String PLAIN = "plain";
    protected static final String DISABLED = "disabled";
    // for searches that are run after a command completes, and where we don't expect to find a message, set a short timeout
    protected static final int SHORT_NOT_FOUND_LOG_SEARCH_TIMEOUT = 12 * 1000;

    public enum AuthMethod {
        CLIENT_SECRET_BASIC, CLIENT_SECRET_POST, PRIVATE_KEY_JWT
    }

    /**
     * Create a WebClient with a 5 minute read time out (which is longer than the default timeout).
     * The PKCE tests need a longer timeout since the runtime has a call to "SecureRandom" and it
     * can sometimes take longer to return.
     *
     * @param override
     *            - true/false - flag indicating if scripting errors and failing status codes should be ignored
     * @return - a new WebClient that is saved to the list of webClients that'll be cleaned up at test case end
     * @throws Exception
     */
    public WebClient getAndSaveWebClientWithLongerTimeout(boolean override) throws Exception {

        WebClient webClient = getAndSaveWebClient(override);
        webClient.getOptions().setTimeout(5 * 60 * 1000);

        return webClient;
    }

    /**
     * Create a copy of TestSettings with values specific to the calling test case.
     *
     * @param clientId
     *            - the OP client id that the test is using
     * @param sigAlg
     *            - the signature algorithm that the private key jwt uses - this is NOT the signature algorithm that the
     *            "client/server" is using.
     * @param app
     *            - the app that will be called - used to set the test url
     * @return - a copy of TestSettings with current test values
     * @throws Exception
     */
    public TestSettings updateTestCaseSettings(String clientId, String sigAlg, String app) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID(clientId);
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + app);
        updatedTestSettings.setSignatureAlg(sigAlg);
        updatedTestSettings.setIssuer(clientId); // the issuer is the client
        return updatedTestSettings;

    }

    /**
     * Build a string representing what the test token endpoint will log for headers that are passed to it
     *
     * @param key
     *            - the key we're expecting
     * @return - "header: <key>:"
     * @throws Exception
     */
    public String getHeaderString(String key) throws Exception {

        return "header: " + key + ":";
    }

    /**
     * Build a string representing what the test token endpoint will log for parms that are passed to it
     *
     * @param key
     *            - the key we're expecting
     * @return - "parameter: <key>:"
     * @throws Exception
     */
    public String getParmString(String key) throws Exception {

        return "parameter: " + key + ":";
    }

    /**
     * Add PKCE expectation messages to the expectations already built
     *
     * @param expectations
     *            - existing expectations
     * @param challengeMethod
     *            - the challenge method we should look for in the logged message
     * @return - updated expectations including challenge message checks
     * @throws Exception
     */
    public List<validationData> addPKCECommonExpectations(List<validationData> expectations, String challengeMethod) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found code_challenge in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge.*");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found code_challenge_method=" + challengeMethod + " in the WASReqURL cookie but didn't.", null, "WASReqURL" + ".*" + "code_challenge_method=" + challengeMethod + ".*");
        return expectations;
    }

    /**
     * Set expectations for this test flow - in both positive and negative flows using the test token endpoint, the flow will
     * terminate at the return from the token endpoint. This will result in a 401 status and a common failure message.
     *
     * @return - common expectations for this test class
     * @throws Exception
     */
    public List<validationData> setPrivateKeyJwtCommonExpectations() throws Exception {

        return setPrivateKeyJwtCommonExpectations(Constants.UNAUTHORIZED_STATUS);
    }

    /**
     * Set expectations for this private key jwt test flow - in both positive and negative flows using the test token endpoint,
     * the flow will
     * terminate at the return from the token endpoint. This will result in the status code passed in and a common failure
     * message.
     *
     * @return - common expectations for this test class
     * @throws Exception
     */
    public List<validationData> setPrivateKeyJwtCommonExpectations(int status_code) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, status_code);
        if (status_code == 200) {
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Should have gotten to the protected app", "", "Servlet");
        } else {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token endpoint did not return an id_token.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER);
        }
        return expectations;
    }

    /**
     * Validate the content of the request to the token endpoint
     *
     * @param updatedTestSettings
     *            - the test setting values to use for the test instance
     * @param authMethod
     *            - the auth method used by the client
     * @throws Exception
     */
    public void validateTokenRequest(TestSettings updatedTestSettings, AuthMethod authMethod) throws Exception {
        validateTokenRequest(updatedTestSettings, authMethod, null);
    }

    public void validateTokenRequest(TestSettings updatedTestSettings, AuthMethod authMethod, String originHeader) throws Exception {

        // validate the origin header
        String oh = getParmValueFromMessagesLog(clientServer, getHeaderString("Origin"));
        if (originHeader != null) {
            if (oh == null) {
                fail("The Origin header (" + originHeader + ") was missing from the request to the token endpoint.");
            } else {
                if (!originHeader.contentEquals(oh)) {
                    fail("The Origin header value of (" + oh + ") did not match the configured value (" + originHeader + ")");
                }
            }
        } else {
            if (oh != null) {
                fail("Found an Origin header (" + oh + ") and was not expecting it");
            }
        }
        // validate client_assertion_type is correct for the auth method used
        String assertionType = getParmValueFromMessagesLog(clientServer, getParmString(Constants.CLIENT_ASSERTION_TYPE));
        validateClientAssertionType(authMethod, assertionType);

        // we should always have a grant parm
        String grant = getParmValueFromMessagesLog(clientServer, getParmString(Constants.GRANT_TYPE));
        if (grant == null) {
            fail("The grant type was missing from the request to the token endpoint.");
        }

        // we should always have a code parm
        String code = getParmValueFromMessagesLog(clientServer, getParmString("code"));
        if (code == null) {
            fail("The code parm was missing from the request to the token endpoint.");
        }

        // we should always have a redirect_uri
        String redirect_uri = getParmValueFromMessagesLog(clientServer, getParmString("redirect_uri"));
        if (redirect_uri == null) {
            fail("The redirect_uri was missing from the request to the token endpoint.");
        }

        String clientId = null;
        String clientSecret = null;
        // validate the client_assertion is correct for the auth method used
        String token = getParmValueFromMessagesLog(clientServer, getParmString(Constants.CLIENT_ASSERTION));

        switch (authMethod) {
        case CLIENT_SECRET_BASIC:
        case CLIENT_SECRET_POST:
            validateClientAssertion(token, updatedTestSettings, false);
            clientId = getParmValueFromMessagesLog(clientServer, getParmString(Constants.JWT_CLIENT_ID));
            clientSecret = getParmValueFromMessagesLog(clientServer, getParmString(Constants.JWT_CLIENT_SECRET));
            if (clientId == null) {
                fail("The client_id was not found in the request to the token endpoint and it should have been included.");
            }
            if (clientSecret == null) {
                fail("The client_secret was not found in the request to the token endpoint and it should have been included.");
            }
            break;
        case PRIVATE_KEY_JWT:
            validateClientAssertion(token, updatedTestSettings, true);
            // the client_id and client_secret shouldn't be in the request (make sure to increase the allowed timeout msgs)
            clientId = clientServer.getServer().verifyStringNotInLogUsingMark(getParmString(Constants.JWT_CLIENT_ID), 2000);
            clientSecret = clientServer.getServer().verifyStringNotInLogUsingMark(getParmString(Constants.JWT_CLIENT_SECRET), 2000);
            addToAllowableTimeoutCount(2);
            if (clientId != null) {
                fail("The client_id was found in the request to the token endpoint and it should not have been included.");
            }
            if (clientSecret != null) {
                fail("The client_secret was found in the request to the token endpoint and it should not have been included.");
            }
            break;
        }

    }

    /**
     * Locates and returns the requested parm value from the output of the test token endpoint in the messages log.
     *
     * @param server
     *            - the reference to the server running the token endpoint
     * @param keyName
     *            - the keyName to return the value for
     * @return - the value recorded in the messages.log for the requested parm/keyName
     * @throws Exception
     */
    public String getParmValueFromMessagesLog(TestServer server, String keyName) throws Exception {

        String thisMethod = "getParmValueFromMessagesLog";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + keyName);

        String searchResult = server.getServer().waitForStringInLogUsingMark(keyName, SHORT_NOT_FOUND_LOG_SEARCH_TIMEOUT, server.getServer().getMatchingLogFile(Constants.MESSAGES_LOG));
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        Log.info(thisClass, thisMethod, searchResult);
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        if (searchResult != null) {
            int start = searchResult.indexOf(keyName);
            int len = keyName.length();
            if (start == -1) {
                throw new Exception("Did not truely find " + keyName + " even through we found a line with it.");
            }
            Log.info(thisClass, thisMethod, "start: " + start + " length: " + searchResult.length());
            String theValue = searchResult.substring(start + len, searchResult.length());
            Log.info(thisClass, thisMethod, keyName + " " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }
        }
        return searchResult;

    }

    /**
     * Validates the assertion type based on the authMethod that the client (testcase) is using.
     *
     * @param authMethod
     *            - the authMethod configured in the client used by the testcase
     * @param assertionType
     *            - the client_assertion_type found in the tokenEndpoint parms
     * @throws Exception
     */
    public void validateClientAssertionType(AuthMethod authMethod, String assertionType) throws Exception {

        switch (authMethod) {
        case CLIENT_SECRET_BASIC:
        case CLIENT_SECRET_POST:
            if (assertionType != null) {
                fail("client_assertion_type was found in the request to the token endpoint and we're not expecting it to be there.");
            }
            break;
        case PRIVATE_KEY_JWT:
            if (!assertionType.equals(Constants.PRIVATE_KEY_JWT_ASSERTION_TYPE)) {
                fail("Test was expecting an assertion type of: " + Constants.PRIVATE_KEY_JWT_ASSERTION_TYPE + ", but found: " + assertionType);
            }
            break;
        }
    }

    /**
     * Return a string claim from the map of claims built from the private key jwt
     *
     * @param claims
     *            - the map of claims from the private key jwt
     * @param key
     *            - the key to return the value for
     * @return - the value of the key
     * @throws Exception
     */
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
     * Validate the content of the private key jwt header
     *
     * @param jwtToken
     *            - the parsed private key jwt
     * @param settings
     *            - the test case settings to use to validate the header content
     * @throws Exception
     */
    public void validateClientAssertionHeader(JwtTokenForTest jwtToken, TestSettings settings) throws Exception {

        // make sure that there is an alg claim and that it is set properly
        Map<String, Object> headerMap = jwtToken.getMapHeader();
        String alg = getStringValue(headerMap, Constants.HEADER_ALGORITHM);
        if (alg == null) {
            fail("Signature algorithm was missing in the private key - it is required");
        }
        if (!alg.equals(settings.getSignatureAlg())) {
            fail("Signature algorithm was not what was expected.  Expected: " + settings.getSignatureAlg() + ", but found: " + alg);
        }

        // make sure that there is a type claim and that is set properly
        String typ = getStringValue(headerMap, Constants.HEADER_TYPE);
        if (typ == null) {
            fail("The typ claim should always be set in the header - it was not found - it is required");
        }
        if (!typ.equals(Constants.HEADER_JWT_TYPE)) {
            fail(Constants.HEADER_TYPE + " claim was not what was expected.  Expected: " + Constants.HEADER_JWT_TYPE + ", but found: " + typ);
        }

    }

    /**
     * Validate the content of the private key payload
     *
     * @param jwtToken
     *            - the parsed private key jwt
     * @param settings
     *            - the test case settings to use to validate the payload content
     * @throws Exception
     */
    public void validateClientAssertionPayload(JwtTokenForTest jwtToken, TestSettings settings) throws Exception {

        Map<String, Object> payloadMap = jwtToken.getMapPayload();
        String iss = getStringValue(payloadMap, Constants.PAYLOAD_ISSUER);
        if (iss == null) {
            fail("Issuer was missing in the private key - it is required");
        }
        if (!iss.equals(settings.getIssuer())) {
            fail("Issuer was not what was expected.  Expected: " + settings.getIssuer() + ", but found: " + iss);
        }

        String sub = getStringValue(payloadMap, Constants.PAYLOAD_SUBJECT);
        if (sub == null) {
            fail("Subject was missing in the private key - it is required");
        }
        if (!sub.equals(settings.getClientID())) {
            fail("Subject was not what was expected.  Expected: " + settings.getClientID() + ", but found: " + sub);
        }

        String aud = getStringValue(payloadMap, Constants.PAYLOAD_AUDIENCE);
        if (aud == null) {
            fail("Audience was missing in the private key - it is required");
        }
        if (!aud.equals(settings.getTokenEndpt())) {
            fail("Audience was not what was expected.  Expected: " + settings.getTokenEndpt() + ", but found: " + aud);
        }

        Long iat = Long.parseLong((String) payloadMap.get(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS));
        if (iat == 0) {
            fail("Issued At Time is missing in the private key - it is optional, but the Liberty clients always include it.");
        } else {
            Long now = NumericDate.now().getValue();
            if (iat > now) {
                fail("Issued At Time was not what was expected.  Expected a value prior to \"now\": " + now + ", but found: " + iat);
            }
        }

        Long exp = Long.parseLong((String) payloadMap.get(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS));
        if (exp == 0) {
            Log.info(thisClass, _testName,
                    "Expiration Time is missing in the private key - the Liberty OP should have included it, but, it is an optional value.");
        } else {
            if (exp < (iat + 295) && exp > (iat + 305)) {
                fail("Expiration Time was not what was expected.  Expected a value 5 minutes after \"iat\" (plus/minus 5 seconds): " + iat + " + 300, but found: " + exp);
            }
        }

        String jti = getStringValue(payloadMap, Constants.PAYLOAD_JWTID);
        if (jti == null) {
            fail("jti was missing in the payload of the private key jwt - it is required");
        }
        if (!used_jtis.isEmpty()) {
            if (used_jtis.contains(jti)) {
                fail("jti in the payload of the private key jwt has already been used by this client");
            }
        }
        Log.info(thisClass, _testName, "jti, " + jti + " is unique.");
        used_jtis.add(jti);

        // The jti should be unique on each request - normally, we would try to re-use a
        // token or jti and the OP would then fail the request, but, the Liberty OP does
        // not currently support the private key jwt, so, ... we can't use the OP to
        // validate

    }

    /**
     * Validate the content of the client_assertion/private key jwt
     *
     * @param token
     *            - the token found in the client_assertion claim logged by the test token endpoint
     * @param settings
     *            - the test case settings to use to validate the token content
     * @throws Exception
     */
    public void validateClientAssertion(String token, TestSettings settings, boolean shouldHaveToken) throws Exception {

        if (shouldHaveToken) {
            if (token != null) {

                JwtTokenForTest jwtToken = new JwtTokenForTest(token);

                jwtToken.printJwtContent();

                validateClientAssertionHeader(jwtToken, settings);
                validateClientAssertionPayload(jwtToken, settings);
            } else {
                fail("Client Assertion token was missing.");
            }
        } else {
            if (token != null) {
                fail("Should not have had a Client Assertion token, but did find one.");
            }
        }

    }

}

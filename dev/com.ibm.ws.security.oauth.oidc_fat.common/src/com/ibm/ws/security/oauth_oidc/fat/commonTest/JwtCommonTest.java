/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;

public class JwtCommonTest extends CommonTest {

    private static final Class<?> thisClass = JwtCommonTest.class;
    protected String twentyFourHourSeconds = "86400000"; // use milliseconds
    protected String NO_OVERRIDE = "nothingToOverride";

    /***
     * build expectations for standard good results
     * 1) make sure we get an access_token
     * 2) make sure we DON'T get an id_token
     * 3) were we able to get to the protected resource
     *
     * @return expectations
     */
    protected List<validationData> buildGoodExpectations(boolean bTestScope) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The access_token missing in the response.", null, Constants.ACCESS_TOKEN_KEY);
        if (!bTestScope) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The JWT scope missing in the response.", null, "\"scope\":\"openid profile\""); // this could be \"scope\":\"profile openid\"
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_ENDPOINT, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, "Token validate response found an id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        return expectations;
    }

    /***
     * build bad request expectation
     * A bad request status contains different information than other status codes
     * We can generically create these expectation though
     *
     * @return expectations
     */
    List<validationData> buildBadRequestExpectation() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_JWT_ENDPOINT);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_JWT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_JWT_ENDPOINT, Constants.RESPONSE_MESSAGE, null, "Did not receive the expected " + Constants.BAD_REQUEST_STATUS + " status code", null, "Bad Request");
        return expectations;
    }

    /***
     * return a string containing the jwt url based on flow type and user's request
     */
    protected String buildJWTUrl(String jspName) throws Exception {
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            return testOPServer.getHttpsString() + "/" + Constants.OAUTHCLIENT_APP + "/" + jspName;
        } else {
            return testOPServer.getHttpsString() + "/" + Constants.OIDCCLIENT_APP + "/" + jspName;
        }
    }

    /**
     * Setup the parms that are to be passed into the token endpoint
     *
     * @param testSettings
     *            - Test case settings
     * @param JWTToken
     *            - the generated jwt token
     * @throws Exception
     */
    protected List<endpointSettings> setScopeParams(TestSettings testSettings, String JWTToken) throws Exception {
        return setScopeParams(testSettings, JWTToken, "openid profile", null);
    }

    /**
     * Setup the parms that are to be passed into the token endpoint
     *
     * @param testSettings
     *            - Test case settings
     * @param JWTToken
     *            - the generated jwt token
     * @throws Exception
     */
    protected List<endpointSettings> setScopeParams(TestSettings testSettings, String JWTToken, String Scopes) throws Exception {
        return setScopeParams(testSettings, JWTToken, Scopes, null);
    }

    /**
     * Setup the parms that are to be passed into the token endpoint
     *
     * @param testSettings
     *            - Test case settings
     * @param JWTToken
     *            - the generated jwt token
     * @return - List of endpointSettings (parms to be omitted from the request)
     * @throws Exception
     */
    protected List<endpointSettings> setScopeParams(TestSettings testSettings, String JWTToken, String Scopes, String[] skipParms) throws Exception {

        // Set up parameters for Jwt.jsp to get new Jwt and access token
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "testCaseName", _testName);
        if (!validationTools.isInList(skipParms, Constants.JWT_SCOPE) && Scopes != null) {
            parms = eSettings.addEndpointSettings(parms, Constants.JWT_SCOPE, Scopes);
        }
        if (!validationTools.isInList(skipParms, Constants.JWT_BEARER_TOKEN)) {
            parms = eSettings.addEndpointSettings(parms, Constants.JWT_BEARER_TOKEN, JWTToken);
        }
        if (!validationTools.isInList(skipParms, Constants.JWT_CLIENT_ID)) {
            parms = eSettings.addEndpointSettings(parms, Constants.JWT_CLIENT_ID, testSettings.getClientID());
        }
        if (!validationTools.isInList(skipParms, Constants.JWT_CLIENT_SECRET)) {
            parms = eSettings.addEndpointSettings(parms, Constants.JWT_CLIENT_SECRET, testSettings.getClientSecret());
        }
        if (!validationTools.isInList(skipParms, Constants.JWT_TOKEN_ENDPOINT)) {
            parms = eSettings.addEndpointSettings(parms, "token_endpoint", testSettings.getTokenEndpt());
        }
        return parms;
    }

    /**
     * Setup the parms that are to be passed into the token endpoint
     *
     * @param testSettings
     *            - Test case settings
     * @param JWTToken
     *            - the generated jwt token
     * @throws Exception
     */
    protected List<endpointSettings> setJWTParms(TestSettings testSettings, String JWTToken) throws Exception {
        return setScopeParams(testSettings, JWTToken, null);
    }

    /**
     * Setup the parms that are to be passed into the token endpoint
     *
     * @param testSettings
     *            - Test case settings
     * @param JWTToken
     *            - the generated jwt token
     * @return - List of endpointSettings (parms to be omitted from the request)
     * @throws Exception
     */
    protected List<endpointSettings> setJWTParms(TestSettings testSettings, String JWTToken, String[] skipParms) throws Exception {
        return setScopeParams(testSettings, JWTToken, null, skipParms);
    }

    /**
     * Build a new JWT Token - taking default values - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @return - return a jwt token string
     */
    protected String buildAJWTToken(TestSettings testSettings) {
        return buildAJWTToken(testSettings, NO_OVERRIDE, NO_OVERRIDE);
    }

    protected String reBuildAJWTToken(TestSettings testSettings, String token) {
        return reBuildAJWTToken(testSettings, NO_OVERRIDE, NO_OVERRIDE, token);
    }

    /**
     * Build a new JWT Token - taking default values or overriding just one - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @param parmToOverride
     *            - the key/attribute to override
     * @param overrideValue
     *            - the value to use instead of the default
     * @return - return a jwt token string
     */
    protected String buildAJWTToken(TestSettings testSettings, String parmToOverride, String overrideValue) {
        String scopes = "openid profile";
        if (testSettings.getUseJwtConsumer()) {
            scopes = testSettings.getScope();
        }
        return buildAJWTToken(testSettings, parmToOverride, overrideValue, scopes);
    }

    protected String reBuildAJWTToken(TestSettings testSettings, String parmToOverride, String overrideValue, String token) {
        String scopes = "openid profile";
        if (testSettings.getUseJwtConsumer()) {
            scopes = testSettings.getScope();
        }
        return reBuildAJWTToken(testSettings, parmToOverride, overrideValue, scopes, token);
    }

    /**
     * Build a new JWT Token - taking default values - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @return - return a jwt token string
     */
    protected String buildAJWTToken(TestSettings testSettings, String scope) {
        return buildAJWTToken(testSettings, NO_OVERRIDE, NO_OVERRIDE, scope);
    }

    protected String reBuildAJWTToken(TestSettings testSettings, String scope, String token) {
        return reBuildAJWTToken(testSettings, NO_OVERRIDE, NO_OVERRIDE, scope, token);
    }

    /**
     * Build a new JWT Token - taking default values or overriding just one - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @param parmToOverride
     *            - the key/attribute to override
     * @param overrideValue
     *            - the value to use instead of the default
     * @param scopes
     *            - the scopes claim to be set in the payload
     * @return - return a jwt token string
     */
    protected String buildAJWTToken(TestSettings testSettings, String parmToOverride, String overrideValue, String scopes) {

        return buildAJWTToken(testSettings, parmToOverride, overrideValue, scopes, null);
    }

    protected String reBuildAJWTToken(TestSettings testSettings, String parmToOverride, String overrideValue, String scopes, String token) {

        return buildAJWTToken(testSettings, parmToOverride, overrideValue, scopes, token);
    }

    protected String buildAJWTToken(TestSettings testSettings, String parmToOverride, String overrideValue, String scopes, String inToken) {
        String token = null;
        JWTToken jsonToken = null;
        Map<String, String> currentValues = null;
        if (inToken != null) {
            currentValues = parseToken(inToken);
        }

        try {
            if (parmToOverride.equals(Constants.PAYLOAD_ISSUER)) {
                jsonToken = new JWTToken(null, Constants.HEADER_DEFAULT_KEY_ID, testSettings);
            } else {
                jsonToken = new JWTToken("autofat.ibm.com", Constants.HEADER_DEFAULT_KEY_ID, testSettings);
            }

            if (scopes != null) {
                jsonToken.setPayloadProp("scope", scopes);
            }

            jsonToken.buildAJWTToken(_testName, testSettings, parmToOverride, overrideValue, currentValues);

            token = jsonToken.getJWTTokenString();

        } catch (Exception e) {
            Log.error(thisClass, _testName, e, "Exception occurred");
            token = null;
        }

        Log.info(thisClass, testName.toString(), "JWT Token: " + token);
        return token;
    }

    /**
     * Build a new JWT Token - taking default values - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @return - return a jwt token string
     */
    protected String buildARS256JWTToken(TestSettings testSettings, boolean bDefaultClientKey) {
        return buildARS256JWTToken(testSettings, NO_OVERRIDE, NO_OVERRIDE, bDefaultClientKey);
    }

    /**
     * Build a new JWT Token - taking default values or overriding just one - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @param parmToOverride
     *            - the key/attribute to override
     * @param overrideValue
     *            - the value to use instead of the default
     * @return - return a jwt token string
     */
    protected String buildARS256JWTToken(TestSettings testSettings, String parmToOverride, String overrideValue, boolean bDefaultClientKey) {
        return buildARS256JWTToken(testSettings, parmToOverride, overrideValue, "openid profile", bDefaultClientKey);
    }

    /**
     * Build a new JWT Token - taking default values or overriding just one - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @param parmToOverride
     *            - the key/attribute to override
     * @param overrideValue
     *            - the value to use instead of the default
     * @param scopes
     *            - the scopes claim to be set in the payload
     * @return - return a jwt token string
     */
    protected String buildARS256JWTToken(TestSettings testSettings,
            String parmToOverride,
            String overrideValue,
            String scopes,
            boolean bDefaultClientKey) {
        String jksFile = "./securitykeys/jwtConfigClientDefault.jks";
        //String storePass = "LibertyClient";
        String keyPass = "LibertyClient";
        String alias = "configClientDefault";
        if (!bDefaultClientKey) {
            jksFile = "./securitykeys/jwtConfigClientDefault2.jks";
            //storePass = "LibertyClient2";
            keyPass = "LibertyClient2";
            alias = "configClientDefault2";
        }
        String token = null;
        JWTToken jsonToken = null;
        try {
            //public JWTToken(String issuerCompany, String keyId,
            //		String strKeyStorePathName, String keyPassword, String keyAlias)
            if (parmToOverride.equals(Constants.PAYLOAD_ISSUER)) {
                jsonToken = new JWTToken("issuser.ibm.com", "idRS256", jksFile, keyPass, alias);
            } else {
                jsonToken = new JWTToken("autofat.ibm.com", "keyidrs256", jksFile, keyPass, alias);
            }

            if (scopes != null) {
                jsonToken.setPayloadProp("scope", scopes);
            }

            jsonToken.buildAJWTToken(_testName, testSettings, parmToOverride, overrideValue, null);

            token = jsonToken.getJWTTokenString();

        } catch (Exception e) {
            Log.error(thisClass, _testName, e, "Exception occurred");
            token = null;
        }

        Log.info(thisClass, testName.toString(), "JWT Token: " + token);
        return token;
    }

    /**
     * Build a new JWT Token - taking default values or overriding just one - this
     * routine is used to test both good and bad value for individual attributes.
     *
     * @param testSettings
     *            - test case settings
     * @param parmToOverride
     *            - the key/attribute to override
     * @param overrideValue
     *            - the value to use instead of the default
     * @param scopes
     *            - the scopes claim to be set in the payload
     * @return - return a jwt token string
     */
    protected String buildARS256JWTTokenForPropagation(TestSettings testSettings,
            String parmToOverride,
            String overrideValue,
            //    		                        String scopes,
            String jksFile,
            String keyPass,
            String alias,
            String issuer,
            String keyId) {
        //    	String jksFile = "./securitykeys/jwtConfigClientDefault.jks";
        //String storePass = "LibertyClient";
        //    	String keyPass = "LibertyClient";
        //    	String alias = "configClientDefault";
        //    	if(!bDefaultClientKey){
        //        	jksFile = "./securitykeys/jwtConfigClientDefault2.jks";
        //storePass = "LibertyClient2";
        //        	keyPass = "LibertyClient2";
        //        	alias = "configClientDefault2";
        //    	}
        String token = null;
        JWTToken jsonToken = null;
        try {
            //public JWTToken(String issuerCompany, String keyId,
            //		String strKeyStorePathName, String keyPassword, String keyAlias)
            if (parmToOverride.equals(Constants.PAYLOAD_ISSUER)) {
                jsonToken = new JWTToken("issuser.ibm.com", "idRS256", jksFile, keyPass, alias);
            } else {
                jsonToken = new JWTToken("autofat.ibm.com", "keyidrs256", jksFile, keyPass, alias);
            }

            //            if( scopes != null){
            //                jsonToken.setPayloadProp("scope", scopes);
            //            }

            jsonToken.buildAJWTToken(_testName, testSettings, parmToOverride, overrideValue, null);

            token = jsonToken.getJWTTokenString();

        } catch (Exception e) {
            Log.error(thisClass, _testName, e, "Exception occurred");
            token = null;
        }

        Log.info(thisClass, testName.toString(), "JWT Token: " + token);
        return token;
    }

    public Map<String, String> parseToken(String token) {

        String[] tokenParts = token.split("\\.");
        Map<String, String> currentValues = new HashMap<String, String>();

        String jwtHeaderSegment = tokenParts[0];
        JsonParser parser = new JsonParser();
        JsonObject header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment)).getAsJsonObject();
        Log.info(thisClass, "parseToken", "Header: " + header.toString());
        String algHeader = header.get("alg").getAsString();
        currentValues.put("alg", algHeader);
        Log.info(thisClass, "parseToken", "algHeader: " + algHeader);
        String kidHeader = header.get("kid").getAsString();
        currentValues.put("kid", kidHeader);
        Log.info(thisClass, "parseToken", "kidHeader: " + kidHeader);

        String jwtPayloadSegment = tokenParts[1];
        JsonObject payload = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtPayloadSegment)).getAsJsonObject();
        Log.info(thisClass, "parseToken", "Payload: " + payload.toString());
        String rawIssPayload = getMyValue(payload, "iss");
        String issPayload = null;
        if (rawIssPayload != null) {
            issPayload = rawIssPayload.replace("\"", "");
        }
        currentValues.put("iss", issPayload);
        Log.info(thisClass, "parseToken", "issPayload: " + issPayload);
        String subPayload = getMyValue(payload, "sub");
        currentValues.put("sub", subPayload);
        Log.info(thisClass, "parseToken", "subPayload: " + subPayload);
        String jtiPayload = getMyValue(payload, "jti");
        currentValues.put("jti", jtiPayload);
        Log.info(thisClass, "parseToken", "jtiPayload: " + jtiPayload);

        String scopePayload = getMyValue(payload, "scope");
        currentValues.put("scope", scopePayload);
        Log.info(thisClass, "parseToken", "scopePayload: " + scopePayload);
        String iatPayload = getMyValue(payload, "iat");
        currentValues.put("iat", iatPayload);
        Log.info(thisClass, "parseToken", "iatPayload: " + iatPayload);
        String expPayload = getMyValue(payload, "exp");
        currentValues.put("exp", expPayload);
        Log.info(thisClass, "parseToken", "expPayload: " + expPayload);
        String audPayload = getMyValue(payload, "aud");
        currentValues.put("aud", audPayload);
        Log.info(thisClass, "parseToken", "audPayload: " + audPayload);
        String nbfPayload = getMyValue(payload, "nbf");
        currentValues.put("nbf", nbfPayload);
        Log.info(thisClass, "parseToken", "nbfPayload: " + nbfPayload);

        return currentValues;

    }

    private String getMyValue(JsonObject part, String entryName) {

        JsonElement value = part.get(entryName);
        if (value != null) {
            return trimAll(value.toString(), '"');
        }
        return null;
    }

    public static String trimFront(String text, char character) {
        String normalizedText;
        int index;

        if (text == null) {
            return text;
        }

        normalizedText = text.trim();
        index = 0;

        while (normalizedText.charAt(index) == character) {
            index++;
        }
        return normalizedText.substring(index).trim();
    }

    public static String trimEnd(String text, char character) {
        String normalizedText;
        int index;

        if (text == null) {
            return text;
        }

        normalizedText = text.trim();
        index = normalizedText.length() - 1;

        while (normalizedText.charAt(index) == character) {
            if (--index < 0) {
                return "";
            }
        }
        return normalizedText.substring(0, index + 1).trim();
    }

    public static String trimAll(String text, char character) {
        String normalizedText = trimFront(text, character);

        return trimEnd(normalizedText, character);
    }

    protected String generateRandomString(int count, Boolean useNumbers) throws Exception {

        String charsToUse;
        if (useNumbers) {
            charsToUse = "0123456789";
        } else {
            charsToUse = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789+@";
        }
        final SecureRandom RANDOM = new SecureRandom();
        String returnString = "";
        for (int i = 0; i < 8; i++) {
            int index = RANDOM.nextInt(charsToUse.length());
            returnString += charsToUse.substring(index, index + 1);
        }

        return returnString;
    }
}

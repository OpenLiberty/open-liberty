/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.joda.time.Instant;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings.StoreType;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.OAuthOidcExpectations;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.ibm.ws.security.openidconnect.jwk.BigIntegerUtil;

import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.SystemClock;

public class CommonValidationTools {

    private final String BOOLEAN_CAST_OFFSET = "com.ibm.ws.security.oauth-oidc_fat.commonTest.boolean.offset";
    public static final String JSON_TOKEN_DELIMITER = ".";
    public static final String HEADER_DELIMITER = "|";
    private final Class<?> thisClass = CommonValidationTools.class;
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public ValidationData vData = new ValidationData();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    String[] generatesExceptionList = { Integer.toString(Constants.BAD_REQUEST_STATUS) };
    String[] validLogLocations = { Constants.CONSOLE_LOG, Constants.MESSAGES_LOG, Constants.TRACE_LOG, };

    /**
     * Validates the results of a step in the test process
     *
     * @param response
     *            - the response output from the latest step in the test process
     * @param currentAction
     *            - the latest test step/action performed
     * @param expectations
     *            - an array of validationData - these include the string to
     *            search for, how to do the search, where to search and for
     *            which test stop/action do expect the string
     * @throws exception
     */
    public void validateResult(Object response, String currentAction,
            List<validationData> expectations, TestSettings settings)
            throws Exception {

        String thisMethod = "validateResult";
        msgUtils.printMethodName(thisMethod, "Start of");

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);

        try {
            // we get passed in the response from the form submissions as well
            // as a list of string pairs - these pairs contain strings
            // that we search for in the response as well as the
            // corresponding error message that will be
            // issued in a failed assertion
            if (response == null) {
                Log.info(thisClass, thisMethod, "Response is null");
                Log.info(thisClass, thisMethod, "Will validate any msgs in server side logs");
                for (validationData expected : expectations) {
                    if (isInList(validLogLocations, expected.getWhere())) {
                        validateWithServerLog(expected);
                    }
                    if (expected.getWhere().equals(Constants.EXCEPTION_MESSAGE)) {
                        validatedExceptionHandled(expected);
                    }
                }
                return;
            }
            if (expectations == null) {
                Log.info(thisClass, thisMethod, "expectations are null");
                return;
            }
            for (validationData expected : expectations) {

                if (currentAction.equals(expected.getAction())) {
                    if ((expected.getWhere().equals(Constants.RESPONSE_FULL)
                            || expected.getWhere().equals(Constants.RESPONSE_TITLE)
                            || expected.getWhere().equals(Constants.RESPONSE_STATUS)
                            || expected.getWhere().equals(Constants.RESPONSE_URL)
                            || expected.getWhere().equals(Constants.RESPONSE_HEADER)
                            || expected.getWhere().equals(Constants.RESPONSE_MESSAGE))) {
                        validateWithResponse(response, expected);
                    } else {
                        if (isInList(validLogLocations, expected.getWhere())) {
                            validateWithServerLog(expected);
                        } else {
                            if (expected.getWhere().equals(Constants.RESPONSE_TOKEN)) {
                                validateReponseLTPAToken(response, expected);
                            } else {
                                if (expected.getWhere().equals(Constants.RESPONSE_ID_TOKEN)) {
                                    validateIdToken(response, expected, settings);
                                } else {
                                    if (expected.getWhere().equals(Constants.RESPONSE_JWT_TOKEN)) {
                                        validateJWTToken(response, expected, settings);
                                    } else {

                                        if (expected.getWhere().equals(Constants.RESPONSE_GENERAL)) {
                                            validateGeneralResponse(response, expected, settings);
                                        } else {
                                            if (expected.getWhere().equals(Constants.JSON_OBJECT)) {
                                                validateJSONData(response, expected);
                                            } else {
                                                if (expected.getWhere().equals(Constants.JSON_OBJECT_COUNT)) {
                                                    validateJSONCount(response, expected);
                                                } else {
                                                    if (expected.getWhere().equals(Constants.EXCEPTION_MESSAGE)) {
                                                        validatedExceptionHandled(expected);
                                                        Log.info(thisClass, thisMethod, "Exception validated separately");
                                                    } else {
                                                        if (expected.getWhere().equals(Constants.RESPONSE_TOKEN_LENGTH)) {
                                                            validateDataLengthInResponse(response, expected);
                                                        } else {
                                                            if (expected.getWhere().equals(Constants.RESPONSE_KEY_SIZE)) {
                                                                validateDataLengthInResponse(response, expected, Constants.RESPONSE_KEY_SIZE);
                                                            } else {
                                                                Log.info(thisClass, thisMethod, "Unknown validation type: " + expected.getWhere());
                                                                throw new Exception("Unknown validation type");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            msgUtils.printMethodName(thisMethod, "End of");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    /**
     * Validates that this exception has been appropriately handled. If the
     * exception has not been handled, an exception is thrown.
     *
     * @param expected
     * @throws Exception
     */
    private void validatedExceptionHandled(validationData expected) throws Exception {
        String thisMethod = "validatedExceptionHandled";
        Log.info(thisClass, thisMethod, "Validating that exception was handled: " + expected.getValidationValue());
        if (!expected.isExceptionHandled()) {
            throw new Exception("An exception was expected but was not marked as having been handled already for step [" + expected.getAction() + "]: " + expected.getPrintMsg() + ".");
        }
        // Set the value back to false in case these exceptions must be used and validated again
        expected.setIsExceptionHandled(false);
    }

    /***
     * invoke the correct server instance to validate content in it's log
     */
    public void validateWithServerLog(validationData expected) throws Exception {

        if (expected.getServerRef() != null) {
            Log.info(thisClass, "validateWithServerLog", "passed in Server type is: " + expected.getServerRef().getServerType() + " passed in Log name is: " + expected.getWhere());
            expected.getServerRef().waitForValueInServerLog(expected);
        } else {
            throw new Exception("Server reference passed to validateWithServerLog is null");
        }

        //        if (expected.getWhere().contains("op")) {
        //            testOPServer.validateWithServerLog(expected);
        //        } else {
        //            if (expected.getWhere().contains("rp")) {
        //                testRPServer.validateWithServerLog(expected);
        //            } else {
        //                if (expected.getWhere().contains("generic")) {
        //                    genericTestServer.validateWithServerLog(expected);
        //                } else {
        //                    throw new Exception("Unrecognized server specified");
        //                }
        //            }
        //
        //        }

    }

    /**
     * Searches for a message string in the response
     *
     * @param reponse
     *            - the response output to search through
     * @param expected
     *            - a validationMsg type to search (contains the string to
     *            search for)
     * @throws exception
     */
    public void validateWithResponse(Object response,
            validationData expected) throws Exception {

        String thisMethod = "validateWithResponse";
        msgUtils.printMethodName(thisMethod, "Start of");

        String responseContent = null;

        try {
            if (expected.getWhere().equals(Constants.RESPONSE_FULL)) {
                responseContent = AutomationTools.getResponseText(response);
            } else {
                if (expected.getWhere().equals(Constants.RESPONSE_TITLE)) {
                    responseContent = AutomationTools.getResponseTitle(response);
                } else {
                    if (expected.getWhere().equals(Constants.RESPONSE_MESSAGE)) {
                        responseContent = AutomationTools.getResponseMessage(response);
                    } else {
                        if (expected.getWhere().equals(Constants.RESPONSE_URL)) {
                            responseContent = AutomationTools.getResponseUrl(response);
                        } else {
                            if (expected.getWhere().equals(Constants.RESPONSE_HEADER)) {
                                String[] hs = AutomationTools.getResponseHeaderNames(response);
                                StringBuilder sb = new StringBuilder();
                                for (String h : hs) {
                                    sb.append(h + ": " + AutomationTools.getResponseHeaderField(response, h));
                                    sb.append(HEADER_DELIMITER);
                                }
                                responseContent = sb.toString();
                            } else {
                                if (expected.getWhere().equals(Constants.RESPONSE_STATUS)) {
                                    // if we have a status code that results in an exception, that is handled differently
                                    //                                    if (isInList(generatesExceptionList, expected.validationValue)) {
                                    //                                        return;
                                    //                                    } else {
                                    responseContent = Integer.toString(AutomationTools.getResponseStatusCode(response));
                                    //                                    }
                                } else {
                                    Log.info(thisClass, thisMethod, "No valid Response area specified - assuming ALL");
                                    responseContent = AutomationTools.getResponseText(response);
                                }
                            }
                        }
                    }
                }
            }

            Log.info(thisClass, thisMethod, "checkType is: " + expected.getCheckType());
            Log.info(thisClass, thisMethod, "Checking for: " + expected.getValidationValue());

            String fullResponseContent = AutomationTools.getFullResponseContentForFailureMessage(response, expected.getWhere());

            if (expected.getCheckType().equals(Constants.STRING_CONTAINS)) {
                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                        + " Was expecting [" + expected.getValidationValue() + "]"
                        + " but received [" + responseContent + "]." + fullResponseContent,
                        responseContent.contains(expected.getValidationValue()));
            } else {
                if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_CONTAIN)) {
                    msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                            + " Was not expecting [" + expected.getValidationValue() + "]"
                            + " but received [" + responseContent + "]." + fullResponseContent,
                            !responseContent.contains(expected.getValidationValue()));
                } else {
                    if (expected.getCheckType().equals(Constants.STRING_MATCHES) || expected.getCheckType().equals(Constants.STRING_EQUALS)) {
                        Pattern pattern = Pattern.compile(expected.getValidationValue());
                        Matcher m = pattern.matcher(responseContent);

                        msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                                + " Was expecting [" + expected.getValidationValue() + "]"
                                + " but received [" + responseContent + "]." + fullResponseContent,
                                m.find());
                    } else {
                        if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_MATCH)) {
                            Pattern pattern = Pattern.compile(expected.getValidationValue());
                            Matcher m = pattern.matcher(responseContent);

                            msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                                    + " Was not expecting [" + expected.getValidationValue() + "]"
                                    + " but received [" + responseContent + "]." + fullResponseContent,
                                    !(m.find()));
                        } else {
                            throw new Exception("String comparison type unknown - test case coded incorrectly");
                        }
                    }
                }
            }
            Log.info(thisClass, thisMethod, "Checked Value: " + expected.getValidationValue());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    public void validateJSONData(Object response, validationData expected) throws Exception {

        Boolean checkValue = false;
        String thisMethod = "validateJSONData";
        msgUtils.printMethodName(thisMethod, "Start of");

        try {
            String respReceived = AutomationTools.getResponseText(response);
            Log.info(thisClass, thisMethod, "Response with JSON data for " + expected.getValidationKey() + " : \n" + respReceived);

            // Now validity check the response JSON object
            JSONObject tokenInfo = JSONObject.parse(respReceived);
            Log.info(thisClass, thisMethod, "Checking that [" + expected.getValidationKey() + "]->[" + expected.getValidationValue() + "] is [checkType: " + expected.getCheckType() + "] within " + tokenInfo);

            String tokenValue = "";
            String key1 = null;
            String key2 = null;
            if (expected.getValidationKey().contains(":")) {
                String[] keys = expected.getValidationKey().split(":");
                key1 = keys[0];
                key2 = keys[1];
            } else {
                key1 = expected.getValidationKey();
            }

            if (expected.getCheckType().equals(Constants.STRING_NULL)) {
                msgUtils.assertTrueAndLog(thisMethod, "Token validation response contained a value for: " + key1 + " and it should not have", tokenInfo.get(key1) == null);
            } else {
                msgUtils.assertTrueAndLog(thisMethod, "Token validation response did not contain a value for: " + key1 + " and it should have", tokenInfo.get(key1) != null);
            }

            // Special detection to handle json property values that are JSON
            // Array or booleans
            if (tokenInfo.get(key1) instanceof JSONArray) {
                tokenValue = ((JSONArray) tokenInfo.get(key1)).serialize();
            } else if (tokenInfo.get(key1) instanceof Boolean) {
                tokenValue = getStringFromBoolean((Boolean) tokenInfo.get(key1));
            } else if (tokenInfo.get(key1) instanceof JSONObject) {
                JSONObject searchTarget = (JSONObject) tokenInfo.get(key1);
                if (key2 != null) {
                    tokenValue = (String) searchTarget.get(key2);
                } else {
                    tokenValue = "";
                }
            } else if (tokenInfo.get(key1) instanceof Long) {
                tokenValue = ((Long) tokenInfo.get(key1)).toString();
            } else {
                tokenValue = (String) tokenInfo.get(key1);
                if (tokenValue == null) {
                    tokenValue = "";
                }
            }
            Log.info(thisClass, thisMethod, "Value from the JSON object to check: [" + tokenValue + "]");

            // If the value is a true boolean, ignore the string matching
            // strategies
            if (isBooleanFromString(tokenValue)) {
                // Do special comparison because we need to ensure this is a
                // true boolean value as opposed to a string "true" or "false".
                checkValue = String.valueOf(getBooleanFromString(tokenValue)).equalsIgnoreCase(expected.getValidationValue());
            } else {
                if (expected.getCheckType().equals(Constants.STRING_NULL)) {
                    checkValue = tokenInfo.get(key1) == null;
                } else {
                    if (expected.getCheckType().equals(Constants.STRING_CONTAINS)) {
                        checkValue = tokenValue.contains(expected.getValidationValue());
                    } else {
                        if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_CONTAIN)) {
                            checkValue = !(tokenValue.contains(expected.getValidationValue()));
                        } else {
                            if (expected.getCheckType().equals(Constants.STRING_MATCHES)) {
                                checkValue = tokenValue.matches(expected.getValidationValue());
                            } else {
                                if (expected.getCheckType().equals(Constants.STRING_EQUALS)) {
                                    checkValue = tokenValue.equals(expected.getValidationValue());
                                } else {
                                    if (expected.getCheckType().equals(Constants.LIST_MATCHES)) {
                                        Log.info(thisClass, thisMethod, "Actual response: " + tokenValue);
                                        Log.info(thisClass, thisMethod, "Expected response: " + expected.getValidationValue());

                                        Set<String> actuals = new HashSet<String>();
                                        //String[] actualEntries = tokenValue.replace("^\\s*\\[", "").replace("\\]\\s*$", "").split(" ");
                                        String[] actualEntries = tokenValue.replaceAll("^\\s*\\[", "").replaceAll("\\]\\s*$", "").replaceAll("\"", "").replaceAll(",", " ").replaceAll("  ", " ").split(" ");
                                        actuals.addAll(Arrays.asList(actualEntries));
                                        Log.info(thisClass, thisMethod, "Response with JSON data actuals is:" + actuals);

                                        Set<String> expecteds = new HashSet<String>();
                                        String[] expectedEntries = expected.getValidationValue().split(" ");
                                        expecteds.addAll(Arrays.asList(expectedEntries));

                                        checkValue = false;

                                        msgUtils.assertTrueAndLog(thisMethod, "Response does not contain the same number of entries for " + expected.getValidationKey()
                                                + " as expected.", actuals.size() == expecteds.size());
                                        msgUtils.assertTrueAndLog(thisMethod, "Response value does not contain the same entries as expected for " + expected.getValidationValue(),
                                                actuals.containsAll(expecteds));

                                        checkValue = true;

                                    } else {
                                        if (expected.getCheckType().equals(Constants.JSON_LIST_MATCHES)) {
                                            Log.info(thisClass, thisMethod, "Actual response: " + tokenValue);
                                            Log.info(thisClass, thisMethod, "Expected response: " + expected.getValidationValue());

                                            // convert the expected list into an actual list (caller can only pass a string for this value)
                                            //  tests assumes that the separator is a space
                                            String[] exp = expected.getValidationValue().split(" ");

                                            JSONArray entryObjects = JSONArray.parse(tokenValue);

                                            checkValue = false;
                                            for (int i = 0; i < exp.length; i++) {
                                                boolean found = false;
                                                for (int j = 0; j < entryObjects.size(); j++) {
                                                    Log.info(thisClass, thisMethod, "Checking expected: " + exp[i] + ", actual: " + entryObjects.get(j).toString());

                                                    if (exp[i].equals(entryObjects.get(j).toString())) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                msgUtils.assertTrueAndLog(thisMethod, "Expected value (" + exp[i] + ") NOT found in Actual results (" + entryObjects.toString() + ")", found);
                                            }
                                            for (int i = 0; i < entryObjects.size(); i++) {
                                                boolean found = false;
                                                for (int j = 0; j < exp.length; j++) {
                                                    Log.info(thisClass, thisMethod, "Checking actual: " + entryObjects.get(i).toString() + ", expected: " + exp[j]);
                                                    if (entryObjects.get(i).toString().equals(exp[j])) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                msgUtils.assertTrueAndLog(thisMethod, "Actual value (" + entryObjects.get(i).toString() + ") NOT found in Expected value (" + expected.getValidationValue() + ")", found);
                                            }
                                            checkValue = true;
                                        } else {
                                            Log.info(thisClass, thisMethod, "String checkType, " + expected.getCheckType() + ", unrecognized - will fail the validation check");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            msgUtils.assertTrueAndLog(thisMethod,
                    "Token validation response did not contain the correct value for " + expected.getValidationKey() + ".\nWas expecting: ["
                            + expected.getValidationValue()
                            + "] but received: [" + tokenValue.toString() + "].",
                    checkValue);

            // Check other fields...
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    private int countTopLevelJSONObjects(String gluedJSON) {
        if (gluedJSON.length() > 0 && gluedJSON.charAt(0) == '[') {
            return 1;
        }

        int jsonObjectsFound = 0;
        int level = 0;

        for (int i = 0; i < gluedJSON.length(); i++) {
            if (gluedJSON.charAt(i) == '{') {
                if (level == 0) {
                    jsonObjectsFound++;
                }
                level++;
            } else if (gluedJSON.charAt(i) == '}') {
                level--;
            }
        }
        return jsonObjectsFound;
    }

    public void validateJSONCount(Object response, validationData expected) throws Exception {

        Boolean checkValue = false;
        String thisMethod = "validateJSONCount";
        msgUtils.printMethodName(thisMethod, "Start of");

        try {
            String respReceived = AutomationTools.getResponseText(response);
            Log.info(thisClass, thisMethod, "Expected number of JSON objects in result is " + expected.getValidationValue() + " in response : \n" + respReceived);

            Log.info(thisClass, thisMethod, "Number of JSON objects in result is  " + countTopLevelJSONObjects(respReceived) + "\n");
            checkValue = String.valueOf(countTopLevelJSONObjects(respReceived)).equals(expected.getValidationValue());

            msgUtils.assertTrueAndLog(thisMethod, "JSON response did not contain the expected number of JSON objects " + expected.getValidationValue() + "\nWas expecting: "
                    + expected.getValidationValue() + " but received:" + String.valueOf(countTopLevelJSONObjects(respReceived)), checkValue);

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
    }

    public void validateReponseLTPAToken(Object response, validationData expected) throws Exception {

        String thisMethod = "validateReponseLTPAToken";
        msgUtils.printMethodName(thisMethod, "Start of");

        try {

            boolean hasLtpaToken = false;
            String[] cookies = AutomationTools.getResponseCookieNames(response);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.equals(Constants.LTPA_TOKEN)) {
                        Log.info(thisClass, thisMethod, "Cookie content: " + cookie);
                        hasLtpaToken = true;
                        break;
                    }
                }
            }
            msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(),
                    Boolean.valueOf(expected.getValidationValue()) == hasLtpaToken);

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
    }

    /**
     * this routine does a generic validation of the id_token (required
     * attributes, non-null values, ...
     *
     * @param response
     *            - the received response that needs to be parsed and verified
     * @param expected
     *            - what values to validate
     * @throws Exception
     */
    public void validateIdToken(Object response, validationData expected, TestSettings settings) throws Exception {

        // this routine is called when the validation type is RESPONSE_ID_TOKEN
        // it can be used to verify the content of the token, or individual segments
        String thisMethod = "validateIdToken";
        String id_token = null;
        String id_token_jws = null;
        String[] jws_token_parts = null;

        try {
            String testSigAlg = setExpectedSigAlg(settings);
            id_token = getIDToken(settings, response);

            if (!IDTokenExistanceCheck(expected, id_token)) {
                return;
            }

            // should only get this far if we're needing to validate the contents of the id token
            if (!id_token.equals(Constants.NOT_FOUND)) {
                
                String decryptKey = settings.getDecryptKey();
                
                JwtTokenForTest jwtToken;
                if (JweHelper.isJwe(id_token) && decryptKey != null) {
                    jwtToken = new JwtTokenForTest(id_token, decryptKey);
                } else {
                    jwtToken = new JwtTokenForTest(id_token);
                }

                id_token_jws = jwtToken.getJwsString();
                jws_token_parts = id_token_jws.split("\\.");

                String decodedToken = ApacheJsonUtils.fromBase64StringToJsonString(jws_token_parts[1]);
                Log.info(thisClass, thisMethod, "Decoded payload Token : " + decodedToken);
                JSONObject tokenInfo = JSONObject.parse(decodedToken);

                // no keys passed indicates that we want to do general id_token validation (check format, required parms, ...)
                if (expected.getValidationKey() == null) {
                    // validate general token content
                    genericIDTokenValidation(settings, testSigAlg, id_token_jws, jws_token_parts, tokenInfo);

                } else {
                    // validate specific key's value
                    specificKeysTokenValidation(expected, tokenInfo);
                }
                // validate Header
                validateJWTTokenHeader(JSONObject.parse(ApacheJsonUtils.fromBase64StringToJsonString(jws_token_parts[0])), settings, null);

            } else {
                Log.info(thisClass, thisMethod, "id_token was not found - it should have been there");
                fail("id_token was not found in the result. Test expected it to be there");

            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating id_token in response");
            throw e;
        }
    }

    /**
     * this routine does a generic validation of the JWT token (required
     * attributes, non-null values, ...
     *
     * @param response
     *            - the received response that needs to be parsed and verified
     * @param expected
     *            - what values to validate
     * @throws Exception
     */
    public void validateJWTToken(Object response, validationData expected, TestSettings settings) throws Exception {
        String thisMethod = "validateJWTToken";
        msgUtils.printMethodName(thisMethod, "Start of wrapper for");

        String jwt_token = getTokenForType(settings, response);
        validateJWTToken(jwt_token, expected, settings);
    }

    public void validateJWTToken(String jwt_token, validationData expected, TestSettings settings) throws Exception {

        // this routine is called when the validation type is RESPONSE_JWT_TOKEN
        // it can be used to verify the content of the token, or individual segments
        String thisMethod = "validateJWTToken";
        msgUtils.printMethodName(thisMethod, "Start of");
        String[] jwt_token_parts;

        try {
            String testSigAlg = setExpectedSigAlg(settings);

            // should only get this far if we're needing to validate the contents of the id token
            if (!jwt_token.equals(Constants.NOT_FOUND)) {
                jwt_token_parts = jwt_token.split("\\.");

                String decodedHeader = ApacheJsonUtils.fromBase64StringToJsonString(jwt_token_parts[0]);
                Log.info(thisClass, thisMethod, "Decoded header of Token : " + decodedHeader);
                JSONObject headerInfo = JSONObject.parse(decodedHeader);
                String decodedPayload = ApacheJsonUtils.fromBase64StringToJsonString(jwt_token_parts[1]);
                Log.info(thisClass, thisMethod, "Decoded payload of Token : " + decodedPayload);
                JSONObject payloadInfo = JSONObject.parse(decodedPayload);

                // no keys passed indicates that we want to do general id_token validation (check format, required parms, ...)
                if (expected.getValidationKey() == null) {
                    // validate general token content
                    validateJWTTokenHeader(headerInfo, settings, testSigAlg);
                    genericJWTTokenValidation(settings, testSigAlg, jwt_token, jwt_token_parts, payloadInfo);

                } else {
                    // validate specific key's value
                    specificKeysTokenValidation(expected, payloadInfo);
                }

            } else {
                Log.info(thisClass, thisMethod, "JWT token was not found when it should have been.");
                fail("id_token/jwt token was not found in the result. Test expected it to be there");

            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating id_token in response");
            throw e;
        }
    }

    /**
     * this routine does a generic validation of the id_token (required
     * attributes, non-null values, ...
     *
     * @param response
     *            - the received response that needs to be parsed and verified
     * @param expected
     *            - what values to validate
     * @throws Exception
     */
    public void validateGeneralResponse(Object response, validationData expected, TestSettings settings) throws Exception {

        // this routine is called when the validation type is RESPONSE_ID_TOKEN
        // it can be used to verify the content of the token, or individual segments
        String thisMethod = "validateGeneralResponse";
        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(Constants.ACCESS_TOKEN_KEY);
        requiredKeys.add(Constants.TOKEN_TYPE_KEY);
        if (expected.getValidationValue() != null) {
            requiredKeys.add(Constants.ID_TOKEN_KEY);
        }
        String theLine = null;

        try {
            String lineSplitString;
            String keySplitString;
            // get the id_token from the response - location depends on flow type
            if (settings.getFlowType() == Constants.IMPLICIT_FLOW) {
                theLine = getTokenLineFromUrl(response);
                lineSplitString = "&";
                keySplitString = "=";

            } else {
                if (settings.getFlowType() == Constants.RP_FLOW) {
                    theLine = getResponseFromOutput(response);
                    lineSplitString = ", ";
                    keySplitString = "=";
                } else {
                    theLine = getTokenLineFromResponse(response);
                    Log.info(thisClass, thisMethod, "theLine: " + theLine);
                    lineSplitString = ",";
                    keySplitString = ":";
                }
            }

            // break line up into individual elements
            String[] theLineParts = theLine.split(lineSplitString);

            if (expected.getValidationKey() != "scope") {
                // ensure that we have values for required keys
                for (String key : requiredKeys) {
                    Log.info(thisClass, thisMethod, "Making sure required key: " + key + " exists");
                    boolean found = false;
                    for (String e : theLineParts) {
                        Log.info(thisClass, thisMethod, "entry: " + e);
                        String part1 = e.split(keySplitString)[0].replaceAll("^\"|\"$", "");
                        String part2 = e.split(keySplitString)[1].replaceAll("^\"|\"$", "");
                        if (part1.equals(key) || part1.equals("\"" + key + "\"")) {
                            Log.info(thisClass, thisMethod, key + " value: " + part2);
                            found = true;
                        }
                    }
                    msgUtils.assertTrueAndLog(thisMethod, "Required key: " + key + " was not found", found);
                }

                // check the actual value of the keys
                for (String e : theLineParts) {
                    Log.info(thisClass, thisMethod, "validating entry: " + e);
                    String key = e.split(keySplitString)[0].replaceAll("^\"|\"$", "");
                    String value = e.split(keySplitString)[1].replaceAll("^\"|\"$", "");
                    //Log.info(thisClass, thisMethod, "key: " + key);
                    //Log.info(thisClass, thisMethod, "value: " + value);
                    if (key.equals(Constants.ACCESS_TOKEN_KEY)) {
                        msgUtils.assertTrueAndLog(thisMethod, "Access Token is null", value != null);
                    }
                    if (key.equals(Constants.TOKEN_TYPE_KEY)) {
                        msgUtils.assertTrueAndLog(thisMethod, "Token Type is null", value != null);
                        msgUtils.assertTrueAndLog(thisMethod, "Token Type value expected: Bearer but received" + value, value.equals("Bearer"));
                    }
                    // make sure it has value if it exists - we've already done the check for should it be there or not
                    if (key.equals(Constants.ID_TOKEN_KEY)) {
                        msgUtils.assertTrueAndLog(thisMethod, "ID Token is null", value != null);
                    }
                    if (key.equals(Constants.REFRESH_TOKEN_KEY)) {
                        // when should it exist or not exist?
                        msgUtils.assertTrueAndLog(thisMethod, "Refresh Token is null", value != null);
                    }
                    if (key.equals(Constants.EXPIRES_IN_KEY)) {
                        msgUtils.assertTrueAndLog(thisMethod, "Expires in is null", value != null);
                        Long expectedExpires = setAccessTimeout(settings);
                        Long actualExpires = Long.valueOf(value).longValue();
                        if ((actualExpires <= expectedExpires) && (actualExpires > expectedExpires - 60L)) {
                            Log.info(thisClass, thisMethod, "expires in was within 60sec of expected time");
                        } else {
                            fail("Expires in value expected: " + expectedExpires + " but received: " + actualExpires + " Test expects it within 60sec");
                        }
                    }
                    if (key.equals(Constants.STATE_KEY)) {
                        msgUtils.assertTrueAndLog(thisMethod, "State is null", value != null);
                        if (Constants.EXIST_WITH_ANY_VALUE.equals(settings.getState())) {
                            Log.info(thisClass, thisMethod, "Skipping state check at callers request");
                        } else {
                            msgUtils.assertTrueAndLog(thisMethod, "State value expected: " + settings.getState() + " but received: " + value, value.equals(settings.getState()));
                        }
                    }
                    // when coding the test where request and server scopes do not match in the future, set the expected response value into
                    // TestSettings, add the expectation, then set the value in TestSettings to the value that the request should pass in (before calling
                    // genericRP or genericOP...
                    if (key.equals(Constants.SCOPE_KEY)) {
                        Log.info(thisClass, thisMethod, "Found scope in resonse");
                    }
                }
            } else {
                for (String e : theLineParts) {
                    String key = e.split(keySplitString)[0].replaceAll("^\"|\"$", "");
                    String value = e.split(keySplitString)[1].replaceAll("^\"|\"$", "");
                    msgUtils.assertTrueAndLog(thisMethod, "Scope is null", value != null);
                    if (key.equals(Constants.SCOPE_KEY)) {
                        Log.info(thisClass, thisMethod, "validating scope entry: " + e);
                        String[] actualScopes = value.replaceAll("%20", " ").replaceAll("\\+", " ").split(" ");
                        Log.info(thisClass, thisMethod, "Acutal Scopes" + Arrays.toString(actualScopes));
                        String[] expectedScopes = expected.getValidationValue().split(" ");
                        Log.info(thisClass, thisMethod, "Expected Scopes" + Arrays.toString(expectedScopes));
                        for (String expectedScope : expectedScopes) {
                            msgUtils.assertTrueAndLog(thisMethod, "Scope value expected: " + expectedScope + " to be in scope, but scope contained: " + value, isInList(actualScopes, expectedScope));
                        }
                        for (String actualScope : actualScopes) {
                            msgUtils.assertTrueAndLog(thisMethod, "Extra scope: " + actualScope + " found in actual scope: " + value, isInList(expectedScopes, actualScope));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
    }

    public void validateException(List<validationData> expectations, String action, Exception e) throws Exception {

        String thisMethod = "validateException";

        msgUtils.printMethodName(thisMethod, "Start of");

        //        try {
        // we get passed in the response from the form submissions as well
        // as a list of string pairs - these pairs contain strings
        // that we search for in the response as well as the
        // corresponding error message that will be
        // issued in a failed assertion
        if (e == null || e.getMessage() == null) {
            Log.info(thisClass, thisMethod, "Exception is null");
            throw e;
        }
        Log.info(thisClass, thisMethod, "exception content: " + e.getMessage());
        if (expectations == null) {
            Log.info(thisClass, thisMethod, "expectations is null");
            throw e;
        }
        Boolean found = false;
        for (validationData expected : expectations) {

            if (action.equals(expected.getAction())) {
                if (expected.where.equals(Constants.EXCEPTION_MESSAGE)) {
                    // we can't return the exception as the return type needs to be response, so we need a way of doing an
                    // "or" check against the exception because of different jdk (we got exception x OR y)  So, for now,
                    // doing a hack and putting one value we want to check in the "key" and the other in the "value" of the
                    // expectation (the key is never used in the exception handling - it'll work, but it's not pretty)
                    if (expected.getValidationKey() == null) {
                        msgUtils.assertTrueAndLog(thisMethod, expected.printMsg + " received: " + e,
                                ((e.getMessage().contains(expected.validationValue)) || (e.toString().contains(expected.validationValue))));
                        found = true;
                    } else {
                        msgUtils.assertTrueAndLog(thisMethod, expected.printMsg + " received: " + e,
                                ((e.getMessage().contains(expected.validationValue)) || (e.toString().contains(expected.validationValue)) ||
                                        (e.getMessage().contains(expected.validationKey)) || (e.toString().contains(expected.validationKey))));
                        found = true;
                    }
                    expected.setIsExceptionHandled(true);
                }
            }
        }
        msgUtils.printMethodName(thisMethod, "End of");
        if (found) {
            return;
        } else {
            Log.info(thisClass, thisMethod, "Action that hit the exception did NOT have an expectation defined to validate an exception");
            throw e;
        }

        //        } catch (Exception n) {
        //            e.printStackTrace();
        //            Log.error(thisClass, thisMethod, n, "Error validating response");
        //            throw n;
        //        }

    }

    public String getExpectedStatusForAction(List<validationData> expectations, String action) {

        for (validationData expectation : expectations) {
            if (expectation.action.equals(action) && (expectation.where.equals(Constants.RESPONSE_STATUS))) {
                return expectation.validationValue;
            }
        }
        // if nothnig found, return 200 - or should we throw an exception?
        return "200";
    }

    public Boolean expectException(List<validationData> expectations, String action) {

        String sCode = getExpectedStatusForAction(expectations, action);

        if (isInList(generatesExceptionList, sCode)) {
            return true;
        } else {
            return false;
        }

    }

    public String getTokenLineFromResponse(Object response) throws Exception {

        String thisMethod = "getTokenLineFromResponse";
        msgUtils.printMethodName(thisMethod);

        try {
            String respReceived = AutomationTools.getResponseText(response);

            String tokenLine = null;
            if (respReceived.indexOf(Constants.RECV_FROM_TOKEN_ENDPOINT) != -1) {
                tokenLine = respReceived.substring(
                        respReceived.indexOf(Constants.RECV_FROM_TOKEN_ENDPOINT) + Constants.RECV_FROM_TOKEN_ENDPOINT.length(),
                        respReceived.indexOf("}"));
            } else {
                // using the endpoint directly and the tokens will be directly in the response
                String partialResp = respReceived.trim();
                return partialResp.substring(1, partialResp.length() - 1);
            }
            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);
            return tokenLine;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }
    }

    public String getTokenFromResponse(Object response, String searchKey) throws Exception {

        String thisMethod = "getTokenFromResponse";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + searchKey);

        try {

            String tokenLine = getTokenLineFromResponse(response);
            if (tokenLine != null) {

                String[] entries = tokenLine.split(",");
                for (String e : entries) {
                    String part1 = e.split(":")[0];
                    String part2 = e.split(":")[1];
                    String part1Strip = removeQuote(part1);
                    String part2Strip = removeQuote(part2);
                    if (part1Strip.equals(searchKey)) {
                        Log.info(thisClass, thisMethod, searchKey + " value: " + part2Strip);
                        printJWTToken(part2Strip);
                        return part2Strip;
                    }
                }
            } else {
                String respReceived = AutomationTools.getResponseText(response);
                Log.info(thisClass, thisMethod, "Response with JSON \n" + respReceived);
                // Now validity check the response JSON object
                JSONObject tokenInfo = JSONObject.parse(respReceived);
                Log.info(thisClass, thisMethod, "JSON Data" + tokenInfo);
                printJWTToken(tokenInfo.get(searchKey).toString());
                return tokenInfo.get(searchKey).toString();
            }
            return Constants.NOT_FOUND;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }
    }

    public String getTokenLineFromUrl(Object response) throws Exception {

        String thisMethod = "getTokenlineFromUrl";
        msgUtils.printMethodName(thisMethod);

        try {
            String respReceived = AutomationTools.getResponseUrl(response);

            String tokenLine = respReceived.substring(respReceived.indexOf("#") + 1, respReceived.length());
            Log.info(thisClass, thisMethod, "Token line: " + tokenLine);
            return tokenLine;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }
    }

    public String getTokenFromUrl(Object response, String searchKey) throws Exception {

        String thisMethod = "getTokenFromUrl";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + searchKey);

        try {
            String tokenLine = getTokenLineFromUrl(response);

            String[] entries = tokenLine.split("&");
            for (String e : entries) {
                Log.info(thisClass, thisMethod, "entry: " + e);
                String part1 = e.split("=")[0];
                String part2 = e.split("=")[1];
                if (part1.equals(searchKey)) {
                    Log.info(thisClass, thisMethod, searchKey + " value: " + part2);
                    return part2;
                }
            }
            return Constants.NOT_FOUND;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }
    }

    public String getResponseFromOutput(Object response) throws Exception {

        String thisMethod = "getResponseFromOutput";
        msgUtils.printMethodName(thisMethod);
        String theString = "Public Credential: {";
        Log.info(thisClass, thisMethod, " Searching for:  " + theString);

        try {
            String respReceived = AutomationTools.getResponseText(response);
            int end = respReceived.indexOf("}");
            if (respReceived.contains("userinfo_string")) {
                end = respReceived.indexOf("}", end + 1); // need 2nd } , not 1st.
            }
            String theValue = respReceived.substring(
                    respReceived.indexOf(theString) + theString.length(), end);
            Log.info(thisClass, thisMethod, "The Line: " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }

            return Constants.NOT_FOUND;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }

    }

    public String getIDTokenFromOutput(Object response) throws Exception {
        return getTokenFromOutput("id_token=", response);
    }

    public String getTokenFromOutput(String tokenName, Object response) throws Exception {

        String thisMethod = "getIDTokenFromOutput";
        msgUtils.printMethodName(thisMethod);
        //        String tokenName = "id_token=";
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        try {
            String respReceived = AutomationTools.getResponseText(response);

            if (!respReceived.contains(tokenName)) {
                return Constants.NOT_FOUND;
            }

            //String partOne = respReceived.substring(respReceived.indexOf(tokenName) + tokenName.length(), respReceived.indexOf("}"));
            int start = respReceived.indexOf(tokenName);
            String partOne = respReceived.substring(start + tokenName.length(),
                    respReceived.indexOf("}", start));
            String theValue = null;
            if (partOne.indexOf(",") != -1) {
                theValue = partOne.substring(0, partOne.indexOf(","));
            } else {
                theValue = partOne;
            }
            Log.info(thisClass, thisMethod, tokenName + " " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }

            return Constants.NOT_FOUND;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining token from  response");
            throw e;
        }
    }

    public String getValueFromResponseFull(Object response, String prefix) throws Exception {

        String thisMethod = "getValueFromResponseFull";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, " Searching for the value of:  " + prefix);

        try {
            String respReceived = AutomationTools.getResponseText(response);

            int startOfValue = respReceived.indexOf(prefix) + prefix.length();
            String theValue = respReceived.substring(startOfValue, respReceived.indexOf(".", startOfValue));
            Log.info(thisClass, thisMethod, "The Value: " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }

            return Constants.NOT_FOUND;

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error obtaining value from  response");
            throw e;
        }

    }

    /**
     * Determines if specified string is in the list of strings
     *
     * @param theList
     *            - the list to search in
     * @param searchString
     *            - the string to search for in the list
     * @return true/false - true - in list, false - not in list
     */
    public Boolean isInList(String[] theList, String searchString) {
        if (theList == null) {
            return false;
        }
        for (String entry : theList) {
            if (entry.equals(searchString)) {
                return true;
            }
        }
        return false;
    }

    public List<validationData> addDefaultIDTokenExpectations(List<validationData> expectations, String testcase, String providerType, String testStep, TestSettings settings) throws Exception {

        try {
            if (providerType == Constants.OAUTH_OP) {
                // oauth will not contain id_token
                expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                        "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
            } else {
                // even if we're testing with OIDC, if openid is NOT in the scope, we WON'T get an id_token (check for NO id_token)
                if (settings.getScope().contains("openid")) {
                    // validate general as well as specific information in the id_token
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                            "The general content of the id_token was incorrect", null, null);
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES,
                            "client id (aud) was not correct in the id_token", Constants.IDTOK_AUDIENCE_KEY, settings.getClientID());
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES,
                            "userid id (sub) was not correct in the id_token", Constants.IDTOK_SUBJECT_KEY, settings.getAdminUser());
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                            "unique security name (uniqueSecurityName) was not correct in the id_token", Constants.IDTOK_UNIQ_SEC_NAME_KEY,
                            settings.getAdminUser());
                    String realmName = settings.getRealm();
                    if (realmName != null) {
                        expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES,
                                "realm name (realmName) was not correct in the id_token", Constants.IDTOK_REALM_KEY, realmName);
                    } else {
                        expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES,
                                "realm name (realmName (default)) was not correct in the id_token", Constants.IDTOK_REALM_KEY, Constants.BASIC_REALM);
                    }
                    if (settings.getNonce() != null && !settings.getNonce().isEmpty()) {
                        expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES,
                                "nonce (nonce) was not correct in the id_token", Constants.IDTOK_NONCE_KEY, settings.getNonce());
                    }
                } else { // (check for NO id_token)
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                            "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
                }

            }
        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
        return expectations;
    }

    private JsonToken getIDTokenAsJsonToken(String id_token) {

        String[] pieces = id_token.split(Pattern.quote(JSON_TOKEN_DELIMITER));
        String jwtPayloadString = pieces[1];
        JsonParser parser = new JsonParser();

        JsonObject payload = parser.parse(StringUtils.newStringUtf8(Base64.decodeBase64(jwtPayloadString)))
                .getAsJsonObject();
        return new JsonToken(payload, new SystemClock());
    }

    public String getIssueIdentifierFromIDToken(String id_token) {

        JsonToken jsonToken = getIDTokenAsJsonToken(id_token);
        return jsonToken.getIssuer();
    }

    public String getSubjectIdentifierFromIDToken(String id_token) {

        JsonToken jsonToken = getIDTokenAsJsonToken(id_token);
        JsonObject jsonObject = jsonToken.getPayloadAsJsonObject();
        return jsonObject.get(Constants.IDTOK_SUBJECT_KEY).getAsString();

    }

    /**
     * Validates that the specified tokenStringRegex appears in the specified output location.
     *
     * @param vData
     * @param expectations
     * @param action
     * @param where
     * @param tokenStringRegex
     * @return
     * @throws Exception
     */
    public List<validationData> addIdTokenStringValidation(ValidationData vData, List<validationData> expectations, String action, String where, String tokenStringRegex) throws Exception {
        // Private Credential: IDToken:{"iss"="http://localhost:8998/oidc/endpoint/OidcConfigSample", "sub"="testuser", "aud"="client01", "exp"=1391718480, "iat"=1391714880, "at_hash"="LHRswznUtoj3HYJYLG7CwA"}
        expectations = vData.addExpectation(expectations, action, where, Constants.STRING_MATCHES, "Did not find expected ID token string in output.", null, tokenStringRegex);
        return expectations;
    }

    /**
     * Get the id_token out of the response - how and where it gets it from
     * depends on the test that we're currently running.
     *
     * @param settings
     * @param response
     * @return
     * @throws Exception
     */
    public String getIDToken(TestSettings settings, Object response) throws Exception {

        String thisMethod = "getIDToken";
        String id_token = null;
        try {
            // get the id_token from the response - location depends on flow type
            if (settings.getFlowType() == Constants.IMPLICIT_FLOW) {
                id_token = getTokenFromUrl(response, Constants.ID_TOKEN_KEY);
            } else {
                if (settings.getFlowType() == Constants.RP_FLOW) {
                    id_token = getIDTokenFromOutput(response);
                } else {
                    id_token = getTokenFromResponse(response, Constants.ID_TOKEN_KEY);
                }
            }
            Log.info(thisClass, thisMethod, "id_token : " + id_token);
            return id_token;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating obtaining id_token from response");
            throw e;
        }
    }

    public Boolean IDTokenExistanceCheck(validationData expected, String id_token) throws Exception {

        String thisMethod = "IDTokenExistanceCheck";
        // Check to validate that id_token did or did not exist as appropriate
        if (expected.getValidationKey() == Constants.ID_TOKEN_KEY) {
            if (id_token.equals(expected.getValidationValue())) {
                // the only time we'll pass in id_token as the key is when we're trying to
                // confirm that the token is NOT included...
                // use msg utility just to log that the check was successful
                msgUtils.assertTrueAndLog(thisMethod, "ID Token existed and should not have", true);
                return false; // all is well - check passes
            } else {
                fail("id_token did NOT match the expected id_token value");
            }
        }
        return true;
    }

    /**
     * Validate the general content of the id_token (the number of parts is
     * correct, the signature algorithm is what the test expects, all required
     * parms exist, timestamps are within the correct range, the signature is
     * correct, ...)
     *
     * @param settings
     * @param testSigAlg
     * @param id_token
     * @param id_token_parts
     * @param tokenInfo
     * @throws Exception
     */
    public void genericIDTokenValidation(TestSettings settings, String testSigAlg, String id_token, String[] id_token_parts, JSONObject tokenInfo) throws Exception {

        String thisMethod = "genericIDTokenValidation";
        msgUtils.printMethodName(thisMethod, "Start of");

        try {
            Set<?> keys = tokenInfo.keySet();
            for (Object aKey : keys) {
                Log.info(thisClass, thisMethod, "Found " + aKey + ": " + tokenInfo.get(aKey));
            }
            // ensure that we have the correct number of parts
            msgUtils.assertTrueAndLog(thisMethod, "ID Token does not have the correct number of parts", validateTokenNumParts(id_token_parts, testSigAlg));

            // now, validate the individual parts (sigAlg.PayLoad.Signature)
            // ensure that the algorithm is correct
            msgUtils.assertTrueAndLog(thisMethod, "ID Token signature algorithm does not match expected ", validateTokenSignatureAlgorithm(id_token_parts, testSigAlg));

            // validate keys and key values in part2 of the id_token
            validateIDTokenPayload(tokenInfo, settings, testSigAlg);

            // validate the signature
            msgUtils.assertTrueAndLog(thisMethod, "The ID Token is not signed properly", validateTokenSignature(id_token, settings, testSigAlg));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error during generic validation of the ID Token");
            throw e;
        }
    }

    /**
     * Validate the general content of the JWT token (the number of parts is
     * correct, the signature algorithm is what the test expects, all required
     * parms exist, timestamps are within the correct range, the signature is
     * correct, ...)
     *
     * @param settings
     * @param testSigAlg
     * @param jwt_token
     * @param jwt_token_parts
     * @param tokenInfo
     * @throws Exception
     */
    public void genericJWTTokenValidation(TestSettings settings, String testSigAlg, String jwt_token, String[] jwt_token_parts, JSONObject tokenInfo) throws Exception {

        String thisMethod = "genericJWTTokenValidation";
        msgUtils.printMethodName(thisMethod, "Start of");

        try {
            Set<?> keys = tokenInfo.keySet();
            for (Object aKey : keys) {
                Log.info(thisClass, thisMethod, "Found " + aKey + ": " + tokenInfo.get(aKey));
            }
            // ensure that we have the correct number of parts
            msgUtils.assertTrueAndLog(thisMethod, "JWT Token does not have the correct number of parts", validateTokenNumParts(jwt_token_parts, testSigAlg));

            // now, validate the individual parts (sigAlg.PayLoad.Signature)
            // ensure that the algorithm is correct
            msgUtils.assertTrueAndLog(thisMethod, "JWT Token signature algorithm does not match expected ", validateTokenSignatureAlgorithm(jwt_token_parts, testSigAlg));

            // validate keys and key values in part2 of the JWT token
            validateJWTTokenPayload(tokenInfo, settings, testSigAlg);

            // validate the signature
            msgUtils.assertTrueAndLog(thisMethod, "The JWT Token is not signed properly", validateTokenSignature(jwt_token, settings, testSigAlg));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error during generic validation of the ID Token");
            throw e;
        }
    }

    /**
     * Validate the values for the keys listed in the passed in expectations
     *
     * @param expected
     * @param tokenInfo
     * @throws Exception
     */
    public void specificKeysTokenValidation(validationData expected, JSONObject tokenInfo) throws Exception {

        String thisMethod = "specificKeysTokenValidation";
        msgUtils.printMethodName(thisMethod, "Start of");

        // validate specific key's value
        String expectKeysValue = expected.getValidationValue();
        Log.info(thisClass, thisMethod, "expectKeysValue: " + expectKeysValue);
        Log.info(thisClass, thisMethod, "expectKey: " + expected.getValidationKey());

        Log.info(thisClass, thisMethod, "passed tokenInfo: " + tokenInfo);
        String actualKeysValue = null;
        Object rawKeysValue = tokenInfo.get(expected.getValidationKey());
        if (rawKeysValue != null) {
            actualKeysValue = tokenInfo.get(expected.getValidationKey()).toString();
        }
        // if the expectedKeysValue is null/empty, then we just want to make sure key/value pair exists, otherwise, we need to validate content
        if (expectKeysValue != null && !expectKeysValue.isEmpty()) {

            msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + " (actual value was null)", Boolean.valueOf(actualKeysValue != null));
            // check value
            Log.info(thisClass, thisMethod, "Validating key: " + expected.getValidationKey() + " Expecting: " + expectKeysValue + " Received: " + actualKeysValue);
            if (expected.getCheckType().equals(Constants.STRING_CONTAINS)) {
                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(actualKeysValue.contains(expectKeysValue)));
            } else {
                if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_CONTAIN)) {
                    msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(!actualKeysValue.contains(expectKeysValue)));
                } else {
                    if (expected.getCheckType().equals(Constants.STRING_MATCHES)) {
                        msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(actualKeysValue.matches(expectKeysValue)));
                    } else {
                        // we don't care what the value is (it may be a random number generated on the server) - we just need to make sure it has a value
                        msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(actualKeysValue != null));
                    }
                }
            }
        } else {
            Log.info(thisClass, thisMethod, "Checking if  " + expected.getValidationKey() + " exists");
            if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_CONTAIN) || expected.getCheckType().equals(Constants.STRING_NOT_NULL)) {
                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(actualKeysValue != null));
            } else {
                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg(), Boolean.valueOf(actualKeysValue == null));
            }
        }
    }

    public Boolean validateTokenNumParts(String[] token_parts, String testSigAlg) throws Exception {

        Integer numParts = 0;
        String thisMethod = "validateTokenNumParts";
        msgUtils.printMethodName(thisMethod, "Start of");

        // should only get this far if we're needing to validate the contents of the id token
        if (testSigAlg.equals(Constants.SIGALG_NONE)) {
            numParts = 2;
        } else {
            numParts = 3;
        }

        for (String s : token_parts) {
            Log.info(thisClass, thisMethod, "token part : " + s);

            Log.info(thisClass, thisMethod, "Decoded tokenPart : " + ApacheJsonUtils.fromBase64StringToJsonString(s));
        }
        if (token_parts.length != numParts) {
            Log.info(thisClass, thisMethod, "Token does not have " + numParts + " parts");
            return false;
        }
        return true;

    }

    public Boolean validateTokenSignatureAlgorithm(String[] token_parts, String testSigAlg) throws Exception {

        String thisMethod = "validateTokenSignatureAlgorithm";
        msgUtils.printMethodName(thisMethod, "Start of");

        String respSigAlg = ApacheJsonUtils.fromBase64StringToJsonString(token_parts[0]);
        JSONObject tokenInfo = JSONObject.parse(respSigAlg);
        String actualAlg = tokenInfo.get(Constants.HEADER_ALGORITHM).toString();
        //        String expectString = "{\"alg\":\"" + testSigAlg + "\"}";
        if (actualAlg.equals(testSigAlg)) {
            return true;
        } else {
            Log.info(thisClass, thisMethod, "Token signature algorithm: " + actualAlg + " does not match expected: " + testSigAlg);
            return false;
        }

    }

    public void validateIDTokenPayload(JSONObject tokenInfo, TestSettings settings, String testSigAlg) throws Exception {

        String thisMethod = "validateIDTokenPayload";
        msgUtils.printMethodName(thisMethod, "Start of");

        ArrayList<String> shouldntExistKeys = new ArrayList<String>();

        // make sure we have all required keys
        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(Constants.IDTOK_SUBJECT_KEY);
        requiredKeys.add(Constants.IDTOK_ISSUER_KEY);
        requiredKeys.add(Constants.IDTOK_AUDIENCE_KEY);
        requiredKeys.add(Constants.IDTOK_EXPIRE_KEY);
        requiredKeys.add(Constants.IDTOK_ISSUETIME_KEY);
        if (!testSigAlg.equals(Constants.SIGALG_NONE)) {
            requiredKeys.add(Constants.IDTOK_AT_HASH_KEY);
        } else {
            // make sure at_hash does NOT exist if the response is not signed
            shouldntExistKeys.add(Constants.IDTOK_AT_HASH_KEY);
            //     msgUtils.assertTrueAndLog(thisMethod, "Key: " + Constants.IDTOK_AT_HASH_KEY + " should NOT exist", Boolean.valueOf(tokenInfo.get(Constants.IDTOK_AT_HASH_KEY) == null));
        }
        if (settings.getNonce() != null) {
            requiredKeys.add(Constants.IDTOK_NONCE_KEY);
        } else {
            shouldntExistKeys.add(Constants.IDTOK_NONCE_KEY);
        }

        for (String key : requiredKeys) {
            msgUtils.assertTrueAndLog(thisMethod, "Expected key: " + key + " does not exist", (tokenInfo.get(key) != null));
            String tokenValue = tokenInfo.get(key).toString();
            Log.info(thisClass, thisMethod, "Making sure required key: " + key + " exists");
            // a null value implies the key wasn't there
            msgUtils.assertTrueAndLog(thisMethod, "Expected key: " + key + " does not exist", Boolean.valueOf(tokenValue != null));
        }

        if (shouldntExistKeys != null) {
            for (String key : shouldntExistKeys) {
                Object tokenValue = tokenInfo.get(key);
                Log.info(thisClass, thisMethod, "Making sure key: " + key + " does NOT exist");
                // a null value implies the key wasn't there
                msgUtils.assertTrueAndLog(thisMethod, "Key: " + key + " does exist and should NOT - it has a value of: " + tokenValue, Boolean.valueOf(tokenValue == null));
            }
        }

        // now, let's validate the time  (exp and ait have just been validated as not null,
        // so, we should have a value now.
        validateTokenTimeStamps(settings, tokenInfo);

    }

    public void validateJWTTokenPayload(JSONObject tokenInfo, TestSettings settings, String testSigAlg) throws Exception {

        String thisMethod = "validateJWTTokenPayload";
        msgUtils.printMethodName(thisMethod, "Start of");

        ArrayList<String> shouldntExistKeys = new ArrayList<String>(Arrays.asList("name", "given_name", "family_name", "middle_name", "nickname",
                "preferred_username", "profile", "picture", "website", "email", "email_verified", "gender", "birthdate", "zoneinfo", "locale",
                "phone_number", "phone_number_verified", "address", "updated_at", "nonce", "auth_time", "at_hash", "c_hash", "acr", "amr",
                "sub_jwk", "cnf", Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS));

        // make sure we have all required keys
        List<String> requiredKeys = new ArrayList<String>();
        if (settings.getRequiredJwtKeys() != null) {
            requiredKeys = settings.getRequiredJwtKeys();
        } else {
            requiredKeys = settings.getDefaultRequiredJwtKeys();
        }

        // TODO - as support is added for these claims, add checks
        if (settings.getJti()) {
            requiredKeys.add(Constants.PAYLOAD_JWTID);
        } else {
            shouldntExistKeys.add(Constants.PAYLOAD_JWTID);
        }
        if (settings.getResourceIds() != null) {
            requiredKeys.add(Constants.PAYLOAD_AUDIENCE);
        } else {
            shouldntExistKeys.add(Constants.PAYLOAD_AUDIENCE);
        }
        //        if (check for azp) {
        //        	requiredKeys.add(Constants.PAYLOAD_AUTHORIZED_PARTY) ;
        //        } else {
        //        	shouldntExistKeys.add(Constants.PAYLOAD_AUTHORIZED_PARTY) ;
        //        }
        if (settings.getGroupIds() != null) {
            requiredKeys.add(Constants.PAYLOAD_GROUP);
        } else {
            shouldntExistKeys.add(Constants.PAYLOAD_GROUP);
        }

        for (String reqKey : requiredKeys) {
            if (shouldntExistKeys.contains(reqKey)) {
                Log.info(thisClass, thisMethod, "Key [" + reqKey + "] noted as being required, so removing from \"shouldn't be found\" list.");
                shouldntExistKeys.remove(reqKey);
            }
        }
        requiredKeyCheck(requiredKeys, tokenInfo);
        shouldntExistKeyCheck(shouldntExistKeys, tokenInfo);

        if (settings.getValidateJWTTimeStamps()) {
            // now, let's validate the time (exp and ait have just been
            // validated as
            // not null,
            // so, we should have a value now.
            validateTokenTimeStamps(settings, tokenInfo);
        }

    }

    public void validateJWTTokenHeader(JSONObject tokenInfo, TestSettings settings, String testSigAlg) throws Exception {

        String thisMethod = "validateJWTTokenHeader";
        msgUtils.printMethodName(thisMethod, "Start of");

        ArrayList<String> shouldntExistKeys = new ArrayList<String>();

        // make sure we have all required keys
        ArrayList<String> requiredKeys = new ArrayList<String>();

        //        if (Constants.JWK_CERT.equals(settings.getRsCertType()) || (settings.getUseJwtConsumer() && !Constants.X509_CERT.equals(settings.getRsCertType()))) {
        String algo = (String) tokenInfo.get(Constants.HEADER_ALGORITHM);
        if (algo != null) {
            algo = algo.toString().toUpperCase();
        }
        boolean kidCheck = false;
        if (algo != null && algo.startsWith("RS")) {
            // kid should always be present for RS256, RS384, RS(anything) algorithm, but not hs256 or none
            kidCheck = true;
            requiredKeys.add(Constants.HEADER_KEY_ID);
        }
        Log.info(thisClass, thisMethod, "algorithm is: " + algo + " kid check added:" + kidCheck);
        //        }
        requiredKeys.add(Constants.HEADER_ALGORITHM);

        requiredKeyCheck(requiredKeys, tokenInfo);
        shouldntExistKeyCheck(shouldntExistKeys, tokenInfo);

    }

    public void requiredKeyCheck(List<String> requiredKeys, JSONObject tokenInfo) throws Exception {

        String thisMethod = "requiredKeyCheck";
        for (String key : requiredKeys) {
            msgUtils.assertTrueAndLog(thisMethod, "Expected key: " + key + " does not exist", (tokenInfo.get(key) != null));
            String tokenValue = tokenInfo.get(key).toString();
            Log.info(thisClass, thisMethod, "Making sure required key: " + key + " exists");
            // a null value implies the key wasn't there
            msgUtils.assertTrueAndLog(thisMethod, "Expected key: " + key + " does not exist", Boolean.valueOf(tokenValue != null));
        }

    }

    public void shouldntExistKeyCheck(ArrayList<String> shouldntExistKeys, JSONObject tokenInfo) throws Exception {

        String thisMethod = "shouldntExistKeyCheck";
        if (shouldntExistKeys != null) {
            for (String key : shouldntExistKeys) {
                Object tokenValue = tokenInfo.get(key);
                Log.info(thisClass, thisMethod, "Making sure key: " + key + " does NOT exist");
                // a null value implies the key wasn't there
                msgUtils.assertTrueAndLog(thisMethod, "Key: " + key + " does exist and should NOT - it has a value of: " + tokenValue, Boolean.valueOf(tokenValue == null));
            }
        }

    }

    public void noOtherKeyCheck(ArrayList<String> allowedKeys, JSONObject tokenInfo) throws Exception {

        String thisMethod = "noOtherKeyCheck";
        msgUtils.printMethodName(thisMethod);
        @SuppressWarnings("unchecked")
        Set<String> allKeys = tokenInfo.keySet();
        int typCount = 0;
        for (String key : allKeys) {
            if (key.equals("typ")) { //20171219 typ key req'd for mp-jwt, allow only one.
                typCount++;
                if (typCount == 1) {
                    continue;
                }
            }
            Log.info(thisClass, thisMethod, "Found key: " + key);
            msgUtils.assertTrueAndLog(thisMethod, "Found key, <" + key + "> in the token and it is NOT expected", allowedKeys.contains(key));
        }

    }

    boolean validateTokenSignature(String id_token, TestSettings testSettings, String sigAlg) {
        boolean result = false;
        String thisMethod = "validateTokenSignature";
        try {
            if (sigAlg == Constants.SIGALG_NONE) {
                // return true - signature doesn't need to be checked
                return true;
            } else {
                if (sigAlg == Constants.SIGALG_HS256) {
                    if (testSettings.getClientSecret() == null || testSettings.getClientSecret().isEmpty()) {
                        //todo - what can we do without a client secret?
                        return true;
                    }
                    //	        FatJsonTokenVerifier tokenVerifier = new FatJsonTokenVerifier("client01", (Object)secret.getBytes(), Constants.SIGALG_HS256, idToken);
                    String clientSecret = testSettings.getClientSecret();
                    Log.info(thisClass, thisMethod, "clientID " + testSettings.getClientID() + " componentID " + testSettings.getComponentID() + " store type " + testSettings.getStoreType() + " isHashed " + testSettings.isHash());
                    if (testSettings.isHash()) {
                        clientSecret = processHashPassword(testSettings, clientSecret);
                    }
                    // Log.info(thisClass, thisMethod, "clientSecret " + clientSecret);
                    JWTTokenVerifier tokenVerifier = new JWTTokenVerifier(testSettings.getClientID(), clientSecret.getBytes("UTF-8"), sigAlg, id_token);
                    // verify with current time
                    // We can call verifyAndDeserialize(SystemClock) to check with different time
                    tokenVerifier.verifyAndDeserialize();
                    result = tokenVerifier.isSigned();
                } else {
                    if (isInList(Constants.ALL_TEST_SIGALGS, sigAlg)) {
                        // return true - we're not ready to validate RS256 quite yet
                        return true;
                    } else {
                        // unknown type - should never hit this as the alg was already checked, but, ...
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Exception occurred");
            result = false;
        }

        Log.info(thisClass, thisMethod, "Signature validation status is: " + result);
        return result;
    }

    public void validateTokenTimeStamps(TestSettings settings, JSONObject tokenInfo) throws Exception {

        String thisMethod = "validateTokenTimeStamps";

        // now, let's validate the time  (exp and ait have just been validated as not null,
        // so, we should have a value now.
        Long iat = (Long) tokenInfo.get(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        Long exp = (Long) tokenInfo.get(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        Long nowconverted = new Instant().getMillis() / 1000;
        Log.info(thisClass, thisMethod, "Time Now : " + nowconverted.toString());

        Long expTimeout = setAccessTimeout(settings);

        Log.info(thisClass, thisMethod, "Checking issued at time is NOT more than expTimeout time before exp");
        msgUtils.assertTrueAndLog(thisMethod, " ait (" + iat + ") is not " + expTimeout + " seconds before exp (" + exp + ")", Boolean.valueOf(exp == (iat + expTimeout)));

        Log.info(thisClass, thisMethod, "Checking issued at time is prior to current time");
        msgUtils.assertTrueAndLog(thisMethod, " ait (" + iat + ") is NOT prior to current time (" + nowconverted + ")", Boolean.valueOf(iat <= nowconverted));

        Log.info(thisClass, thisMethod, "Checking issued at time is NOT too far in the past");
        msgUtils.assertTrueAndLog(thisMethod, " ait (" + iat + ") is too far in the past (" + nowconverted + ")", Boolean.valueOf((nowconverted - 60) <= iat));

        // We're not supporting nbf at this time
        //    	Long nbf = (Long) tokenInfo.get(Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS);
        //    	if (nbf != null) {
        //    		Log.info(thisClass, thisMethod, "Checking NBF is set to the same value as ait");
        //    		Long differ = 0L ;
        //    		if (nbf <= iat) {
        //    			differ = iat - nbf ;
        //    		} else {
        //        		msgUtils.assertTrueAndLog(thisMethod, " nbf (" + nbf + ") is NOT earlier than or equal to ait (" + iat + ")", false );
        //    		}
        //    		msgUtils.assertTrueAndLog(thisMethod, " nbf (" + nbf + ") is NOT within approprate range earlier than ait (" + iat + ")", Boolean.valueOf(differ <= 120));
        //    	}

    }

    /**
     * Set the expected signature algorithm type - it is either set in the test
     * settings, or default to HS256
     *
     * @param settings
     * @return
     * @throws Exception
     */
    public String setExpectedSigAlg(TestSettings settings) throws Exception {

        String testSigAlg = settings == null ? null : settings.getSignatureAlg();
        if (testSigAlg == null) {
            testSigAlg = Constants.SIGALG_HS256;
        }
        return testSigAlg;

    }

    /***
         *
         */
    public Long setAccessTimeout(TestSettings settings) throws Exception {
        return setAccessTimeout(settings, 0L);
    }

    public Long setAccessTimeout(TestSettings settings, Long adjustment) throws Exception {

        String expTimeoutString = settings.getAccessTimeout();
        Long expTimeout = 7200L - adjustment;
        if (expTimeoutString != null) {
            expTimeout = Long.valueOf(expTimeoutString).longValue();
        }

        return expTimeout;
    }

    public List<validationData> addDefaultGeneralResponseExpectations(List<validationData> expectations, String testcase, String providerType, String testStep,
            TestSettings settings) throws Exception {

        try {
            if (providerType == Constants.OAUTH_OP) {

                // oauth will not contain id_token
                expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS,
                        "Token validate response found the id_token in the response and should not have", null, null);
            } else {
                // if openid is not in the scope, we should make sure that id_token in not in the general response
                if (settings.getScope().contains("openid")) {
                    // validate general as well as specific information in the response
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS,
                            "The general content of the response was incorrect", null, Constants.OIDC_OP);
                } else {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS,
                            "Token validate response found the id_token in the response and should not have (openid was missing in scope)", null, null);
                }
            }
            expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, "The scope content of the response was incorrect", "scope", settings.getScope());

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
        return expectations;
    }

    public List<validationData> addJaxrsSnoopResponseTokenExpectations(List<validationData> expectations, String testcase, String providerType, String requestType, String token, String testStep, TestSettings settings) throws Exception {
        return addJaxrsSnoopResponseTokenExpectations(expectations, testcase, providerType, requestType, token, testStep, settings.getWhere(), settings.getRSProtectedResource(), settings.getRsTokenType());
    }

    public List<validationData> addJaxrsSnoopResponseAccessTokenExpectations(List<validationData> expectations, String testcase, String providerType, String requestType, String token, String testStep, String where, String resource) throws Exception {
        return addJaxrsSnoopResponseTokenExpectations(expectations, testcase, providerType, requestType, token, testStep, where, resource, Constants.ACCESS_TOKEN_KEY);
    }

    // TODO - when we add tests for the attribute that allows us to "name" the jwt token (and maybe the access_token), we will probably need to enhance this method further
    /**
     * This method adds the appropriate expectation to validate the propagation
     * token used to access the app
     *
     * @param expectations
     *            - expectation to update
     * @param testcase
     *            - the test case name
     * @param providerType
     *            - provider type (OIDC/OAuth)
     * @param requestType
     *            - GET/POST
     * @param token
     *            - token value to compare against
     * @param testStep
     *            - step/action in the test flow
     * @param where
     *            - where the token was put on the request (PARM/HEADER)
     * @param resource
     *            - test application
     * @param tokenType
     *            - access_token/JWT token
     * @return
     * @throws Exception
     */
    public List<validationData> addJaxrsSnoopResponseTokenExpectations(List<validationData> expectations, String testcase, String providerType, String requestType, String token, String testStep, String where, String resource, String tokenType) throws Exception {

        expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, resource);

        // the access_token will be found on 1 line in the snoop output
        // the id_token, will be split with <br> in the output - making it hard to validate
        // so, for id_token, just search for the first 45 characters
        String tokenToMatch = null;
        int tokenLen = token.length();
        if (tokenType.equals(Constants.ACCESS_TOKEN_KEY) || tokenLen <= 45) {
            tokenToMatch = token;
        } else {
            tokenToMatch = token.substring(0, 44);
        }

        if (requestType != null) {
            if (requestType == Constants.GETMETHOD) {
                if (where.equals(Constants.PARM)) {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The correct propagation token was NOT found in the Snoop output", null, "access_token=" + tokenToMatch);
                } else {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The correct propagation token was NOT found in the Snoop output", null, "Bearer " + tokenToMatch);
                }
            } else {
                if (where.equals(Constants.PARM)) {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The correct propagation token was NOT found in the Snoop output", null, "access_token</td><td>" + tokenToMatch);
                } else {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The correct propagation token was NOT found in the Snoop output", null, "Bearer " + tokenToMatch);
                }
            }
        }
        return expectations;
    }

    /**
     * Since boolean values detected get converted to string, there needs to be
     * a mechanism to differentiate a string value of true / false from a
     * boolean value. This method adds a unique offset to the boolean converted
     * value that we understand and use it to deduce a boolean type, even if
     * translated into a string.
     *
     * @param value
     *            of boolean
     * @return String encoded boolean value with offset value appended
     */
    private String getStringFromBoolean(boolean value) {
        return (new StringBuffer())
                .append(String.valueOf(value))
                .append(BOOLEAN_CAST_OFFSET)
                .toString();
    }

    /**
     * Determine from a string value if the origin is from a true boolean type
     * value.
     *
     * @param value
     *            of string encoded boolean value with offset value appended
     * @return true or false if string value was extracted from true boolean
     *         type
     */
    private boolean isBooleanFromString(String value) {
        return value.endsWith(BOOLEAN_CAST_OFFSET);
    }

    /**
     * Return boolean value from specially string encoded boolean value. This is
     * to be used in conjunction with getStringFromBoolean(..)
     *
     * @param value
     *            of specially string encoded boolean value
     * @return boolean value extracted from specially encoded string boolean
     *         value
     */
    private boolean getBooleanFromString(String value) {
        return value.toLowerCase().startsWith("true");

    }

    public List<validationData> addRequestParmsExpectations(List<validationData> expectations, String testcase, String testStep, TestSettings settings) throws Exception {

        if (settings.getWhere().equals(Constants.PARM)) {
            Map<String, String> reqParms = settings.getRequestParms();
            if (reqParms != null) {
                for (String key : reqParms.keySet()) {
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find parm passed through to servlet", null, "Param: " + key + " with value: " + reqParms.get(key));
                }
            }
        }

        return expectations;
    }

    public List<validationData> addRequestFileParmsExpectations(List<validationData> expectations, String testcase, String testStep, TestSettings settings) throws Exception {

        Map<String, String> reqParms = settings.getRequestFileParms();
        if (reqParms != null) {
            for (String key : reqParms.keySet()) {
                String line;
                String theValue = "";
                String theFile = reqParms.get(key);
                File f = new File(theFile);
                if (f.exists() && !f.isDirectory()) {
                    InputStream fis = new FileInputStream(theFile);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
                    Boolean firstOneDone = false;
                    while ((line = br.readLine()) != null) {
                        if (firstOneDone) {
                            theValue = theValue + '\n';
                            firstOneDone = true;
                        }
                        theValue = theValue + line;
                    }
                    // Done with the file
                    br.close();
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find parm passed through to servlet", null, "Param: " + key + " with value: " + theValue);
                } else {
                    // should never get here - if we do it's a test (code/setup bug)
                    expectations = vData.addExpectation(expectations, testStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find parm passed through to servlet", null, "Param: " + key + " with value: missing file");
                }
            }
        }
        return expectations;
    }

    /**
     * Validates the results of a step in the test process
     *
     * @param connection
     *            - the httpURLConnection with result
     * @param currentAction
     *            - the latest test step/action performed
     * @param expectations
     *            - an array of validationData - these include the string to
     *            search for, how to do the search, where to search and for
     *            which test stop/action do expect the string
     * @throws exception
     */
    public void validateHttpUrlConnResult(HttpURLConnection connection, String currentAction,
            List<validationData> expectations, TestSettings settings, String response)
            throws Exception {

        String thisMethod = "validateHttpUrlConnResult";
        msgUtils.printMethodName(thisMethod, "Start of");

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);

        try {
            // we get passed in the response from the form submissions as well
            // as a list of string pairs - these pairs contain strings
            // that we search for in the response as well as the
            // corresponding error message that will be
            // issued in a failed assertion

            if (expectations == null) {
                Log.info(thisClass, thisMethod, "expectations is null");
                return;
            }
            for (validationData expected : expectations) {

                if (currentAction.equals(expected.getAction())) {
                    if ((expected.getWhere().equals(Constants.RESPONSE_STATUS) ||
                            expected.getWhere().equals(Constants.RESPONSE_URL) ||
                            expected.getWhere().equals(Constants.RESPONSE_HEADER) ||
                            expected.getWhere().equals(Constants.RESPONSE_FULL) || expected.getWhere().equals(Constants.RESPONSE_MESSAGE))) {
                        validateWithHttpUrlConnection(connection, expected, response);
                    } else {
                        if (isInList(validLogLocations, expected.getWhere())) {
                            validateWithServerLog(expected);

                        } else {
                            if (expected.getWhere().equals(Constants.EXCEPTION_MESSAGE)) {
                                Log.info(thisClass, thisMethod, "Excpetion validated separately");
                            } else {
                                Log.info(thisClass, thisMethod, "Unknown validation type: " + expected.getWhere());
                                throw new Exception("Unknown validation type");
                            }
                        }
                    }
                }

            }
            msgUtils.printMethodName(thisMethod, "End of");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    /**
     * Searches for a message string in the response
     *
     * @param connection
     *            - the HttpURLConnection from which we get the response
     * @param expected
     *            - a validationMsg type to search (contains the string to
     *            search for)
     * @throws exception
     */
    public void validateWithHttpUrlConnection(HttpURLConnection connection, validationData expected, String response) throws Exception {

        String thisMethod = "validateWithHttpUrlConnection";
        msgUtils.printMethodName(thisMethod, "Start of");

        String responseContent = null;

        try {
            if (expected.getWhere().equals(Constants.RESPONSE_MESSAGE)) {
                responseContent = connection.getResponseMessage();
                Log.info(thisClass, thisMethod, "HttpURLValidation response message :" + responseContent);
            } else {
                if (expected.getWhere().equals(Constants.RESPONSE_URL)) {
                    responseContent = connection.getURL().toString();
                    Log.info(thisClass, thisMethod, "HttpURLValidation URL :" + responseContent);
                } else {
                    if (expected.getWhere().equals(Constants.RESPONSE_HEADER)) {
                        StringBuilder sb = new StringBuilder();
                        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                            sb.append(header.getKey() + ": " + header.getValue().toString().replaceAll("^\\[|\\]$", ""));
                        }
                        responseContent = sb.toString();
                        Log.info(thisClass, thisMethod, "HttpURLValidation responseHeader :" + responseContent);

                    } else {
                        if (expected.getWhere().equals(Constants.RESPONSE_STATUS)) {
                            responseContent = Integer.toString(connection.getResponseCode());
                            Log.info(thisClass, thisMethod, "HttpURLValidation response status :" + responseContent);
                        } else {
                            if (expected.getWhere().equals(Constants.RESPONSE_FULL)) {
                                if (response != null) {
                                    responseContent = response;
                                } else {
                                    responseContent = "None";
                                }
                                Log.info(thisClass, thisMethod, "HttpURLValidation response full :" + responseContent);
                            } else {
                                Log.info(thisClass, thisMethod, "No valid response area specified - assuming NONE");
                                responseContent = "No valid response area specified";
                            }
                        }
                    }
                }
            }

            Log.info(thisClass, thisMethod, "checkType is: " + expected.getCheckType());
            Log.info(thisClass, thisMethod, "Checking for: " + expected.getValidationValue() + " in: \n" + responseContent);

            if (expected.getCheckType().equals(Constants.STRING_CONTAINS)) {
                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + "\n Was expecting " + expected.getValidationValue() + " But, received: " + responseContent,
                        responseContent.contains(expected.getValidationValue()));
            } else {
                if (expected.getCheckType().equals(Constants.STRING_DOES_NOT_CONTAIN)) {
                    msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + "\n Was expecting " + expected.getValidationValue() + " But, received: " + responseContent,
                            !responseContent.contains(expected.getValidationValue()));
                } else {
                    if (expected.getCheckType().equals(Constants.STRING_MATCHES)) {
                        msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + " Was expecting " + expected.getValidationValue() + " But, received: " + responseContent,
                                responseContent.matches(expected.getValidationValue()));
                    } else {
                        throw new Exception("String comparison type unknown - test case coded incorrectly");
                    }
                }
            }
            Log.info(thisClass, thisMethod, "Checked Value: " + expected.getValidationValue());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    public void validateDataLengthInResponse(Object response, validationData expected) throws Exception {
        validateDataLengthInResponse(response, expected, Constants.RESPONSE_TOKEN_LENGTH);
    }

    public void validateDataLengthInResponse(Object response, validationData expected, String dataType) throws Exception {

        String thisMethod = "validateDataLengthInResponse";

        msgUtils.printMethodName(thisMethod, "Start of");
        String key = expected.getValidationKey();
        String value = null;
        if (key.contains(" ")) {
            value = getValueFromResponseFull(response, expected.getValidationKey());
        } else {
            value = getTokenFromResponse(response, expected.getValidationKey());
        }

        if (dataType == Constants.RESPONSE_KEY_SIZE) {
            if (value != null) {
                BigInteger decKey = BigIntegerUtil.decode(value);
                msgUtils.assertTrueAndLog(thisMethod, "The length (" + decKey.bitLength() + ") of the specified key: " + expected.getValidationKey() + " is not " + expected.getValidationValue(), decKey.bitLength() == Integer.parseInt(expected.getValidationValue()));
            }
        } else {
            if (value.length() != 30) {
                msgUtils.assertTrueAndLog(thisMethod, "The length (" + value.length() + ") of the specified key: " + expected.getValidationKey() + " is not " + expected.getValidationValue(), value.length() == Integer.parseInt(expected.getValidationValue()));
            }
        }

        msgUtils.printMethodName(thisMethod, "End of");
    }

    public String removeQuote(String str) {

        if (str == null || str == "") {
            return null;
        }

        char char1 = str.charAt(0);
        char char2 = str.charAt(str.length() - 1);
        if (char1 == '"' && char2 == '"') {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }

    public List<validationData> addDefaultRSOAuthExpectations(List<validationData> expectations, String testcase, String finalAction, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes(null);
        }
        expectations = addIdTokenStringValidation(vData, expectations, finalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = addRequestParmsExpectations(expectations, testcase, finalAction, settings);
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the access_token printed in the app output", null, "access_token=");
        } else {
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the JWT Token printed in the app output", null, Constants.JWT_STR_START + ".*\"iss\":");
        }
        if (settings.getRequestParms() != null) {
            String testApp = settings.getRequestParms().get("targetApp"); // if we're trying to get the app name for verification, we should have a test app set...
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not go to the correct app", null, "Param: targetApp with value: " + testApp);
        }

        return expectations;
    }

    public List<validationData> add401Expectations(String action) throws Exception {
        return add401Expectations(action, Constants.UNAUTHORIZED_STATUS);
    }

    public List<validationData> add401Expectations(String action, int status) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, action);
        //        expectations = vData.addResponseStatusExpectation(expectations, action, status) ;
        expectations = vData.addExpectation(expectations, action, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Should have received an unauthorized exception", null, Constants.UNAUTHORIZED_EXCEPTION);
        return expectations;
    }

    public List<validationData> add401Responses(String action) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, action);
        expectations = vData.addResponseStatusExpectation(expectations, action, Constants.UNAUTHORIZED_STATUS);
        return expectations;
    }

    public List<validationData> addMessageExpectation(TestServer theServer, List<validationData> expected, String step, String log, String checkType, String failureMessage, String logMessage) throws Exception {
        // Set the actual expectation to find the message in the server log
        expected = vData.addExpectation(expected, step, theServer, log, checkType, failureMessage, null, logMessage);

        theServer.addIgnoredServerException(logMessage);

        return expected;
    }

    public String getTokenForType(TestSettings settings, Object response) throws Exception {

        String thisMethod = "getTokenForType";
        msgUtils.printMethodName(thisMethod, "Start of");
        String flowType = settings != null ? settings.getFlowType() : null;

        String token = null;
        String tokenType = settings != null ? settings.getRsTokenType() : Constants.ACCESS_TOKEN_KEY;
        Log.info(thisClass, thisMethod, "tokenType: " + tokenType + " flowType: " + flowType);

        if (tokenType.equals(Constants.ACCESS_TOKEN_KEY)) {
            return getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
        } else {
            if (tokenType.equals(Constants.ID_TOKEN_KEY)) {
                token = getIDToken(settings, response);
            } else {
                if (tokenType.equals(Constants.BUILT_JWT_TOKEN)) {
                    token = getValueFromTestAppOutput(response, Constants.BUILT_JWT_TOKEN);
                } else {
                    if (tokenType.equals(Constants.SOCIAL_JWT_TOKEN)) {
                        token = getJwtFromPageResponse(response);
                    } else {
                        if (flowType != null && flowType.equals(Constants.IMPLICIT_FLOW)) {
                            token = getTokenFromUrl(response, Constants.ACCESS_TOKEN_KEY);
                        } else {
                            token = getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
                        }
                    }
                }
            }
            printJWTToken(token);
            return token;
        }

    }

    public void printJWTToken(String token) throws Exception {

        Log.info(thisClass, "printJWTToken", "Raw JWT token: " + token);
        String[] parts = token.split("\\.");
        for (String part : parts) {
            Log.info(thisClass, "printJWTToken", "Raw Part : " + part);
            String decodedToken = ApacheJsonUtils.fromBase64StringToJsonString(part);
            Log.info(thisClass, "printJWTToken", "Decoded payload Token Part : " + decodedToken);
        }
    }

    public String getValueFromTestAppOutput(Object response, String requested) throws Exception {

        String thisMethod = "getValueFromTestAppOutput";
        String valueFound = null;

        String respReceived = AutomationTools.getResponseText(response);
        //        Log.info(thisClass, thisMethod, "Text: " + respReceived);
        String[] responseLines = respReceived.split(System.getProperty("line.separator"));
        for (String line : responseLines) {
            if (line.contains(requested)) {
                valueFound = line.trim().substring(line.indexOf(requested) + requested.length());
                Log.info(thisClass, thisMethod, "Found: " + requested + " set to: " + valueFound);
            }
        }
        return valueFound;
    }

    public String getJwtFromPageResponse(Object thePage) throws Exception {

        String thisMethod = "getJwtFromPageResponse";
        msgUtils.printMethodName(thisMethod);
        String valueFound = null;
        String jwtTag = "issuedJwt=";
        //        String jwtToken = validationTools.getValueFromTestAppOutput(response, Constants.BUILT_JWT_TOKEN);
        String respReceived = AutomationTools.getResponseText(thePage);
        //        Log.info(thisClass, thisMethod, "Text: " + respReceived);
        String[] responseLines = respReceived.split(System.getProperty("line.separator"));
        for (String line : responseLines) {
            if (line.contains(jwtTag)) {
                line = line.trim();
                valueFound = line.substring(line.indexOf(jwtTag) + jwtTag.length());
                int ender = line.indexOf(",", line.indexOf(jwtTag) + jwtTag.length());
                valueFound = line.substring(line.indexOf(jwtTag) + jwtTag.length(), ender);
                Log.info(thisClass, thisMethod, "Found: " + jwtTag + " set to: " + valueFound);
            }
        }
        return valueFound;
    }

    public Expectations getDefault404Expectations(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(testAction, HttpServletResponse.SC_NOT_FOUND));
        expectations.addExpectation(new ResponseFullExpectation(testAction, Constants.STRING_MATCHES, MessageConstants.CWOAU0073E_FRONT_END_ERROR + ".+",
                "Did not get public facing error message saying authentication failed."));
        return expectations;
    }

    @Deprecated
    public List<validationData> getDefault404VDataExpectations(String testAction) throws Exception {
        Expectations expectations = getDefault404Expectations(testAction);
        return ValidationDataToExpectationConverter.convertExpectations(expectations);
    }

    @Deprecated
    public List<validationData> getDefault404VDataExpectationsWithOtherwiseSuccessfulStatusCodes(String testAction) throws Exception {
        OAuthOidcExpectations expectations = new OAuthOidcExpectations();
        expectations.addSuccessStatusCodes(testAction);
        expectations.addExpectations(getDefault404Expectations(testAction));
        return ValidationDataToExpectationConverter.convertExpectations(expectations);
    }

    /*
     * Pull hash information from the database to compare to hash the clientSecret for token comparison
     */
    private String processHashPassword(TestSettings testSettings, String clientSecret) throws Exception {
        String thisMethod = "processHashPassword";
        assertNotNull("The client ID is null, token cannot be validated", testSettings.getClientID());
        String salt = null;
        String algorithm = null;

        // Pull hash information from the database/custom store
        assertNotNull("The http string isn't set, can't pull salt and algorithm from the database",
                testSettings.getHttpString());
        assertNotNull("The http port isn't set, can't pull salt and algorithm from the database",
                testSettings.getHttpPort());
        if (testSettings.getStoreType() == StoreType.DATABASE) {
            salt = DerbyUtils.checkSalt(testSettings.getHttpString(), testSettings.getHttpPort(),
                    testSettings.getClientID(), testSettings.getComponentID());
            algorithm = DerbyUtils.checkAlgorithm(testSettings.getHttpString(), testSettings.getHttpPort(),
                    testSettings.getClientID(), testSettings.getComponentID());
        } else if (testSettings.getStoreType() == StoreType.CUSTOM
                || testSettings.getStoreType() == StoreType.CUSTOMBELL) {
            salt = MongoDBUtils.checkSalt(testSettings.getHttpString(), testSettings.getHttpPort(),
                    testSettings.getClientID(), testSettings.getComponentID());
            algorithm = MongoDBUtils.checkAlgorithm(testSettings.getHttpString(), testSettings.getHttpPort(),
                    testSettings.getClientID(), testSettings.getComponentID());
        } else {
            fail("The store type is not set to database or customstore, cannot retrieve hash information for verifying the token. Store type provided is "
                    + testSettings.getStoreType());
        }
        assertNotSame("Client not found in database, check componentID is correct: " + testSettings.getComponentID(),
                "null_client", salt);
        assertNotNull("Should have retrieved a salt for client " + testSettings.getClientID(), salt);
        assertNotSame("Salt not found for client " + testSettings.getClientID(), "null_salt", salt);

        assertNotNull("Should have retrieved an algorithm for client " + testSettings.getClientID(), algorithm);
        assertNotSame("Algorithm not found for client " + testSettings.getClientID(), "null_algorithm", algorithm);

        // Log.info(thisClass, thisMethod, "salt retrieved: " + salt + " algorithm retrieved:" + algorithm);
        return HashSecretUtils.hashSecret(clientSecret, testSettings.getClientID(), false, salt, algorithm, 0, 0);

    }

    public boolean foundOneErrorInException(Exception e, String... msgString) throws Exception {

        if (msgString.length > 0) {
            for (String msg : msgString) {
                if ((e.getMessage().contains(msg)) || (e.toString().contains(msg))) {
                    return true;
                }
            }
        }
        return false;
    }
}

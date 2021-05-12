/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class RSCommonTestTools {

    private final Class<?> thisClass = RSCommonTestTools.class;
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    String[] generatesExceptionList = { Integer.toString(Constants.BAD_REQUEST_STATUS) };

    /**
     * Adds an expectation for the condition where we've passed an invalid token. The error message varies depending on the token
     * type.
     *
     * @param expectations
     *            - expectations to update
     * @param settings
     *            - current test settings
     * @param theServer
     *            - the server whose log should contain the message
     * @return
     * @throws Exception
     */
    // an access token will return a not active message, JWT will return a parser error
    public List<validationData> setInvalidTokenMsg(List<validationData> expectations, TestSettings settings, TestServer theServer) throws Exception {
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was expired", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        } else {
            expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was not a valid JSON object", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
        }
        return expectations;
    }

    public List<validationData> setNotActiveTokenMsg(List<validationData> expectations, TestSettings settings, TestServer theServer) throws Exception {

        // with an access token, we'll get the not active message, with JWT we're getting invalid json
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was expired", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        } else {
            //			expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was not a valid JSON object", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was not a valid JSON object", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
            //			expectations = validationTools.addMessageExpectation(theServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was not a valid JSON object", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID) ;
        }
        return expectations;
    }

    /**
     * With an access_token we'll get error messages in both the RS and OP, with JWT, none of these messages appear (the OP is not
     * involved in processing the token)
     *
     * @param expectations
     *            - current expectations to update
     * @param settings
     *            - test settings to get values from
     * @param opServer
     *            - the opServer object
     * @param genericServer
     *            - the RS server object
     * @return
     * @throws Exception
     */
    public List<validationData> setExpectedErrorMessageForInvalidToken(List<validationData> expectations, TestSettings settings, TestServer genericServer, TestServer opServer) throws Exception {
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            if (genericServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                expectations = validationTools.addMessageExpectation(opServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was expired.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);

            } else if (genericServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
                expectations = validationTools.addMessageExpectation(opServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be recognized.", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
                expectations = validationTools.addMessageExpectation(genericServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was expired", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
            }
        }
        return expectations;
    }

    /**
     * Create/add an expectation, only if this is a flow using access_token. The expectation is NOT needed for JWT flows.
     * Using this method removes the need for an "if (access_token)" check in every test case
     *
     * @param theServer
     *            - the server that will get the error (used to add the error to the list of expected/allowed errors for a server
     *            (so the framework doesn't balk)
     * @param expectations
     *            - the expectations to update
     * @param settings
     *            TODO
     * @param step
     *            - the action/step in the process where this should be checked
     * @param log
     *            - the log that we need to look in
     * @param checkType
     *            - the type of check (contains/matches/...)
     * @param errMsg
     *            - the error message to issue of the condition is NOT met
     * @param checkForThis
     *            - what to look for in the log
     * @return - returns updated expectations
     * @throws Exception
     */
    public List<validationData> setExpectationForAccessTokenOnly(TestServer theServer, List<validationData> expectations, TestSettings settings, String step, String log, String checkType, String errMsg, String checkForThis) throws Exception {
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = validationTools.addMessageExpectation(theServer, expectations, step, log, checkType, errMsg, checkForThis);
        }
        return expectations;

    }

    /**
     * Create/add an expectation, only if this is a flow using access_token. The expectation is NOT needed for JWT flows.
     * Using this method removes the need for an "if (access_token)" check in every test case
     *
     * @param theServer
     *            - the server that will get the error (used to add the error to the list of expected/allowed errors for a server
     *            (so the framework doesn't balk)
     * @param expectations
     *            - the expectations to update
     * @param settings
     *            TODO
     * @param step
     *            - the action/step in the process where this should be checked
     * @param log
     *            - the log that we need to look in
     * @param checkType
     *            - the type of check (contains/matches/...)
     * @param errMsg
     *            - the error message to issue of the condition is NOT met
     * @param checkForThis
     *            - what to look for in the log
     * @return - returns updated expectations
     * @throws Exception
     */
    public List<validationData> setExpectationForJWTTokenOnly(TestServer theServer, List<validationData> expectations, TestSettings settings, String step, String log, String checkType, String errMsg, String checkForThis) throws Exception {
        if (!settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = validationTools.addMessageExpectation(theServer, expectations, step, log, checkType, errMsg, checkForThis);
        }
        return expectations;

    }

    public String[] chooseTokenSettings(String providerType) throws Exception {

        // Default is to NOT allow MP JWT's
        return chooseTokenSettings(providerType, false);
    }

    public String[] chooseTokenSettings(String providerType, Boolean allow_MP_JWT) throws Exception {

        String thisMethod = "chooseTokenSettings";
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;
        msgUtils.printMethodName(thisMethod);

        if (Constants.ISAM_OP.equals(providerType)) {
            throw new Exception("ISAM server not supported yet");
            // when ISAM is available, ...
            // 1) randomly choose a working server (here, or maybe in the test server like we do with other things
            //      (the issue is that we need info on what the server is before we start any of our Liberty server, so earlier is better)
            //      If NO ISAM is available, fail, or switch to using an OP and the id_token
            //			tokenType = Utils.getRandomSelection(Constants.JWT_TOKEN, Constants.ACCESS_TOKEN_KEY, Constants.JWT_TOKEN, Constants.ACCESS_TOKEN_KEY) ;
            //			if (tokenType.equals(Constants.JWT_TOKEN)) {
            //				certType = Utils.getRandomSelection(Constants.JWK_CERT, Constants.X509_CERT, Constants.JWK_CERT, Constants.X509_CERT) ;
            //			}
        } else {
            // pick the token type
            if (Constants.OIDC_OP.equals(providerType)) {
                tokenType = Utils.getEnvVar("tokenType");
                if (tokenType == null || tokenType.equals("")) {
                    String jdkVersion = System.getProperty("java.version");
                    if (jdkVersion.compareTo("1.7") < 0) {
                        Log.info(thisClass, thisMethod, "Using JDK 1.6 or earlier; JWT requires JDK 1.7 or later, so forcing token type to " + Constants.ACCESS_TOKEN_KEY);
                        tokenType = Constants.ACCESS_TOKEN_KEY;
                    } else {
                        if (allow_MP_JWT) {
                            tokenType = Utils.getRandomSelection(Constants.JWT_TOKEN, Constants.MP_JWT_TOKEN, Constants.ACCESS_TOKEN_KEY);
                        } else {
                            tokenType = Utils.getRandomSelection(Constants.JWT_TOKEN, Constants.ACCESS_TOKEN_KEY);
                        }
                    }
                } else {
                    Log.info(thisClass, thisMethod, "Using caller override value of: " + tokenType);
                }
                certType = chooseCertType(tokenType);
            } else {
                // type is OAuth - we're using the id_token for testing at the moment and OAuth does not create one.
                // take defaults of access_token with x509
            }
        }

        Log.info(thisClass, thisMethod, "**************************************************************************************");
        Log.info(thisClass, thisMethod, "****** providerType:  " + providerType);
        Log.info(thisClass, thisMethod, "****** tokenType:  " + tokenType);
        Log.info(thisClass, thisMethod, "****** certType:  " + certType);
        Log.info(thisClass, thisMethod, "**************************************************************************************");

        return new String[] { tokenType, certType };
    }

    public String chooseCertType(String tokenType) throws Exception {
        String thisMethod = "chooseCertType";
        msgUtils.printMethodName(thisMethod);

        if (tokenType.equals(Constants.JWT_TOKEN) || tokenType.equals(Constants.MP_JWT_TOKEN)) {
            String certType = Utils.getEnvVar("certType");
            if (certType == null || certType.equals("")) {
                certType = Utils.getRandomSelection(Constants.JWK_CERT, Constants.X509_CERT);
            } else {
                Log.info(thisClass, thisMethod, "Using caller override value of: " + certType);
            }
            return certType;
        } else {
            return Constants.X509_CERT;
        }
    }

    public TestSettings updateRSProtectedResource(TestSettings settings, String newResourceTarget) {
        String method = "updateRSProtectedResource";
        TestSettings updatedSettings = settings.copyTestSettings();

        String rsProtectedResource = updatedSettings.getRSProtectedResource();
        rsProtectedResource = rsProtectedResource.replaceAll("/[^/]+?$", "/" + newResourceTarget);
        Log.info(thisClass, method, "Updated RS protected resource: " + rsProtectedResource);

        updatedSettings.setRSProtectedResource(rsProtectedResource);

        return updatedSettings;
    }

}
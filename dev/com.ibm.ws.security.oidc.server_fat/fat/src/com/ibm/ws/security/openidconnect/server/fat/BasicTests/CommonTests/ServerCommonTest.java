/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.List;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class ServerCommonTest extends CommonTest {

    public static final String NO_SCOPE = "";

    protected List<endpointSettings> createTokenEndpointBasicAuthenticationHeader(String clientID, String clientSecret) throws Exception {
        String basicAuth = cttools.buildBasicAuthCred(clientID, clientSecret);
        List<endpointSettings> headers = eSettings.addEndpointSettings(null, "Authorization", basicAuth);
        headers = eSettings.addEndpointSettings(headers, "Content-type", "application/x-www-form-urlencoded");
        return headers;
    }

    protected List<validationData> expectGoodStatusCodeForAllSteps() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        return expectations;
    }

    protected List<validationData> setPositiveJsonExpectations(List<validationData> expectations) throws Exception {
        return setPositiveJsonExpectationWhenValidRequest(expectations, testSettings);
    }

    protected List<validationData> setPositiveJsonExpectations(List<validationData> expectations, TestSettings updatedSettings) throws Exception {
        return setPositiveJsonExpectationWhenValidRequest(expectations, updatedSettings);
    }

    private List<validationData> setPositiveJsonExpectationWhenValidRequest(List<validationData> expectations, TestSettings settings) throws Exception {
        // Validate header
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the Content-Type: application/json", Constants.RESPONSE_HEADER_CONTENT_TYPE, Constants.RESPONSE_HEADER_CONTENT_JSON);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the Cache-control header no-store", Constants.RESPONSE_HEADER_CACHE_CONTROL, Constants.RESPONSE_CACHE_CONTROL_NO_STORE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the Cache-control header no-store", Constants.RESPONSE_HEADER_PRAGMA, Constants.RESPONSE_PRAGMA_NO_CACHE);
        // Validate response
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the access_token", Constants.ACCESS_TOKEN_KEY, "");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the token_expires value.", Constants.EXPIRES_IN_KEY, "");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the token_type of Bearer.", Constants.TOKEN_TYPE_KEY, "Bearer");

        if (settings.getScope() != NO_SCOPE) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.LIST_MATCHES,
                    "Token endpoint response did not contain the correct scopes", Constants.ACCTOK_SCOPE, settings.getScope());
        } else {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.JSON_OBJECT, Constants.LIST_MATCHES,
                    "Token endpoint response did not contain the correct scopes", Constants.ACCTOK_SCOPE, "");

        }

        return expectations;
    }

    protected List<validationData> setNegativeResponseExpectationForInvalidRequest(List<validationData> expectations, String message, String opLogMessage) throws Exception {

        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Should have failed for error code " + Constants.ERROR_CODE_INVALID_REQUEST, null, Constants.ERROR_CODE_INVALID_REQUEST);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the correct error description.", Constants.ERROR_RESPONSE_DESCRIPTION, message);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_TOKEN_ENDPOINT);
        if (opLogMessage != null) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "Server messages.log should contain a message indicating that the request was not valid.", null, opLogMessage);

        }
        return expectations;
    }

    protected List<validationData> setNegativeResponseExpectationForInvalidClient(List<validationData> expectations, String message, String opLogMessage, boolean isErrorInHeader) throws Exception {

        if (isErrorInHeader) {
            // Header contains error and error_description
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                    "Should have failed for error code " + Constants.ERROR_CODE_INVALID_CLIENT + " in response header.", null, Constants.ERROR_CODE_INVALID_CLIENT);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                    "Token endpoint response did not contain the correct error description in response header.", Constants.ERROR_RESPONSE_DESCRIPTION, message);
        }

        // Full response contains error and error_description
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Should have failed for error code " + Constants.ERROR_CODE_INVALID_CLIENT + " in full response.", null, Constants.ERROR_CODE_INVALID_CLIENT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the correct error description in full response.", Constants.ERROR_RESPONSE_DESCRIPTION, message);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_TOKEN_ENDPOINT);
        if (opLogMessage != null) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "Server messages.log should contain a message indicating that the request was not valid.", null, opLogMessage);

        }
        return expectations;
    }

    protected List<validationData> setNegativeExpectationsForUnauthorized() throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_MESSAGE, null, "Did not receive the expected "
                + Constants.UNAUTHORIZED_STATUS, null, "Unauthorized");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Header did not contain WWW-Authenticate", null,
                Constants.RESPONSE_HEADER_WWWAUTHENTICATE);
        return expectations;
    }

    protected List<validationData> setNegativeExpectationsForBadRequest() throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_MESSAGE, null, "Did not receive the expected "
                + Constants.BAD_REQUEST_STATUS + " status code", null, "Bad Request");
        return expectations;
    }

    protected List<validationData> expectGoodStatusCodesExceptForTokenEndpoint() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
        return expectations;
    }

}

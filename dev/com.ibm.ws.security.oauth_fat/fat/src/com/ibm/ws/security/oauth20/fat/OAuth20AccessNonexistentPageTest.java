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
package com.ibm.ws.security.oauth20.fat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20AccessNonexistentPageTest extends OAuth20TestCommon {

    private static final Class<?> thisClass = OAuth20AccessNonexistentPageTest.class;

    private static final String KEY_RESPONSE_TITLE = "title";
    private static final String KEY_RESPONSE_CODE = "code";
    private static final String KEY_RESPONSE_MESSAGE = "message";
    private static final String KEY_RESPONSE_TEXT = "text";

    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup("com.ibm.ws.security.oauth-2.0.fat");
    }

    /**
     * Tests:
     * - Invokes a path that does not exist within the OAuth context
     * Expects:
     * - Should receive a 404 but with an appropriate public-facing error message.
     */
    @Test
    public void accessOAuthPathThatDoesNotExist() throws Exception {
        Log.info(thisClass, testName.getMethodName(), "Begin");
        try {
            sslSetup();

            HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);

            String nonExistentPath = httpsStart + "/oauth2/";
            Log.info(thisClass, testName.getMethodName(), "Invoking non-existent path: " + nonExistentPath);
            WebRequest request = new GetMethodWebRequest(nonExistentPath);

            WebConversation wc = new WebConversation();
            WebResponse response = wc.getResponse(request);
            verify404Response(response);
        } catch (Exception e) {
            Log.info(thisClass, testName.getMethodName(), "Exception occurred:");
            Log.error(thisClass, testName.getMethodName(), e, "Exception: ");
            throw e;
        }
    }

    private void verify404Response(WebResponse response) throws Exception {
        JsonObject responseContent = buildResponseContent(response);
        verifyResponseTitle(responseContent);
        verifyResponseCode(responseContent);
        verifyResponseMessage(responseContent);
        verifyResponseText(responseContent);
    }

    private void verifyResponseTitle(JsonObject responseContent) throws Exception {
        String expectedTitle = "";
        verifyResponseString(responseContent, KEY_RESPONSE_TITLE, expectedTitle);
    }

    private void verifyResponseCode(JsonObject responseContent) throws Exception {
        int expectedCode = 404;
        Log.info(thisClass, "verifyResponseCode", "Verifying response code matches [" + expectedCode + "]");
        int responseCode = responseContent.getInt(KEY_RESPONSE_CODE);
        if (expectedCode != responseCode) {
            throw new Exception("Response code did not match expected value. Expected [" + expectedCode + "] but received [" + responseCode + "]. Response content was "
                                + responseContent);
        }
    }

    private void verifyResponseMessage(JsonObject responseContent) throws Exception {
        String expectedMessage = "Not Found";
        verifyResponseString(responseContent, KEY_RESPONSE_MESSAGE, expectedMessage);
    }

    private void verifyResponseText(JsonObject responseContent) throws Exception {
        String expectedText = MessageConstants.CWOAU0073E_FRONT_END_ERROR + ".+";
        verifyResponseString(responseContent, KEY_RESPONSE_TEXT, expectedText);
    }

    private void verifyResponseString(JsonObject responseContent, String valueToCheck, String expectedValueRegex) throws Exception {
        Log.info(thisClass, "verifyResponseString", "Verifying " + valueToCheck + " matches [" + expectedValueRegex + "]");
        String responseValue = responseContent.getString(valueToCheck);
        if (responseValue == null) {
            throw new Exception("Response did not contain " + valueToCheck + " but should have. Response content was " + responseContent);
        }
        if (!responseValue.matches(expectedValueRegex)) {
            throw new Exception("Response " + valueToCheck + " did not match expected value. Expected [" + expectedValueRegex + "] but received [" + responseValue
                                + "]. Response content was " + responseContent);
        }
    }

    private JsonObject buildResponseContent(WebResponse response) throws Exception {
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        responseBuilder.add(KEY_RESPONSE_TITLE, response.getTitle());
        responseBuilder.add(KEY_RESPONSE_CODE, response.getResponseCode());
        responseBuilder.add(KEY_RESPONSE_MESSAGE, response.getResponseMessage());
        responseBuilder.add(KEY_RESPONSE_TEXT, response.getText());
        return responseBuilder.build();
    }

}

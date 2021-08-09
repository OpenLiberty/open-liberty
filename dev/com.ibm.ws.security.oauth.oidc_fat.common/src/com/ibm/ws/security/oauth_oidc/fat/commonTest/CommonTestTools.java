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
import com.ibm.ws.security.oauth20.util.Base64;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class CommonTestTools {

    private final Class<?> thisClass = CommonTestTools.class;
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    String[] generatesExceptionList = { Integer.toString(Constants.BAD_REQUEST_STATUS) };
    public static EndpointSettings eSettings = new EndpointSettings();

    public static String buildAppHttpsUrl(String clientHttps, String app, String servlet) {

        return clientHttps + "/" + app + "/" + servlet;

    }

    public String buildAppHttpUrl(String clientHttp, String app, String servlet) {

        return clientHttp + "/" + app + "/" + servlet;

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

        if (validationTools.isInList(generatesExceptionList, sCode)) {
            return true;
        } else {
            return false;
        }

    }

    public String buildBasicAuthCred(String user, String pwd) throws Exception {

        String thisMethod = "buildBasicAuthCred";

        try {
            String clientCreds = user + ":" + pwd;
            return "Basic " + new String(Base64.encode(clientCreds.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error generating basic auth credential  from  response");
            throw e;
        }
    }

    public WebRequest addBasicAuthCredToHeader(WebRequest request, TestSettings settings) throws Exception {

        String thisMethod = "addBasicAuthCredToHeader";

        try {
            String basicAuth = buildBasicAuthCred(settings.getAdminUser(), settings.getAdminPswd());
            List<endpointSettings> headers = eSettings.addEndpointSettings(null, "Authorization", basicAuth);
            for (endpointSettings header : headers) {
                Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                request.setHeaderField(header.key, header.value);
            }
            return request;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error adding basic auth credential  to header of request");
            throw e;
        }
    }

    public String buildBearerTokenCred(String accessToken) throws Exception {

        String thisMethod = "buildBearerTokenCred";

        try {
            return "Bearer " + accessToken;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error generating bearer token credential from access token.");
            throw e;
        }
    }

    public WebRequest addBearerTokenCredToHeader(WebRequest request, String accessToken) throws Exception {

        String thisMethod = "addBasicAuthCredToHeader";

        try {
            String bearerTokenAuth = buildBearerTokenCred(accessToken);
            List<endpointSettings> headers = eSettings.addEndpointSettings(null, "Authorization", bearerTokenAuth);
            for (endpointSettings header : headers) {
                Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                request.setHeaderField(header.key, header.value);
            }
            return request;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error adding bearer token auth credential  to header of request");
            throw e;
        }
    }

    /**
     * Submit a page (the default number of times) and then print the response
     *
     * @param form
     *            - the form to submit
     * @param submitButtom
     *            - the button to use to perform the submit
     * @param printMsg
     *            - The message to print
     * @throws exception
     */
    public WebResponse submitAndPrint(WebForm form, String printMsg) throws Exception {

        try {
            return submitAndPrint(form, Constants.SUBMIT_NUM_TIMES, null, printMsg);

        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Submit a page and then print the response
     *
     * @param form
     *            - the form to submit
     * @param submitNum
     *            - the number of times the form should be submitted (there is
     *            something with httpUnit and some providers that require the
     *            form to be submitted more than once)
     * @param submitButtom
     *            - the button to use to perform the submit
     * @param printMsg
     *            - The message to print
     * @throws exception
     */
    public WebResponse submitAndPrint(WebForm form, int submitNum, String submitButton, String printMsg)
            throws Exception {

        String thisMethod = "submitAndPrint";
        msgUtils.printMethodName(thisMethod);

        try {
            WebResponse newResponse = null;
            Log.info(thisClass, thisMethod, printMsg);
            WebForm newForm = form;
            SubmitButton theButton = null;

            if (submitButton == null) {
                Log.info(thisClass, thisMethod, "Will submit with no button specified");
            } else {
                Log.info(thisClass, thisMethod, "Will submit with the "
                        + submitButton + " button specified");
                theButton = form.getSubmitButtonWithID(submitButton);
                if (theButton == null) {
                    Log.info(thisClass, thisMethod, "submit button not found");
                    theButton = null; // really make sure it's null
                } else {
                    Log.info(thisClass, thisMethod, "submit button was found");
                }
            }

            for (int i = 0; i < submitNum; i++) {
                Log.info(thisClass, thisMethod, "Submit " + i);
                newResponse = newForm.submit(theButton);
                if (newResponse.getText().contains("Form Login Error Page") || newResponse.getResponseCode() == 500) {
                    Log.info(thisClass, thisMethod, "Landed on the Error page - need to check if we're supposed to be here");
                    msgUtils.printResponseParts(newResponse, thisMethod, "Response from SubmitButton: ");
                    break;
                }
                // if we try to grab the "next" form on a page that doesn't
                // have one, the code gods will not be happy, so, stop
                // before we go too far!
                if (i < submitNum - 1) {
                    msgUtils.printResponseParts(newResponse, thisMethod, "Response from SubmitButton: ");
                    newForm = newResponse.getForms()[0];
                }
            }
            //	}
            msgUtils.printResponseParts(newResponse, thisMethod, "Response from SubmitButton: ");
            return newResponse;
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Failed submitting the form");
            throw e;
        }
    }

    /**
     * Click a buttom to submit a page and then print the response
     *
     * @param wc
     *            - the webconversation
     * @param form
     *            - the form to submit
     * @param button
     *            - the button to be "clicked"
     * @throws exception
     */
    public WebResponse clickAndPrint(WebConversation wc, WebForm form, String button) throws Exception {

        String thisMethod = "clickAndPrint";
        msgUtils.printMethodName(thisMethod);

        try {
            WebResponse newResponse = null;
            Log.info(thisClass, thisMethod, "Will click on button: " + button);
            //SubmitButton theButton = form.getSubmitButtonWithID(button);
            SubmitButton theButton = form.getSubmitButton(button);
            if (theButton == null) {
                // see what buttons are available
                for (SubmitButton s : form.getSubmitButtons()) {
                    Log.info(thisClass, thisMethod, "Button ID found: "
                            + s.getID());
                }
                throw new Exception("Button Not Found");
            } else {
                theButton.click();
                Log.info(thisClass, thisMethod, "Get response from conversation");
                newResponse = wc.getCurrentPage();
            }
            msgUtils.printAllCookies(wc);
            msgUtils.printResponseParts(newResponse, thisMethod, "Response from SubmitButton: ");
            return newResponse;

        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Status Message is missing");
            throw e;
        }

    }

    public String assembleRegExPrincipalName(String userName) {
        return "(?s)\\A.*?\\bPrincipal: WSPrincipal:" + userName + ".*\\z";
    }

    public String extractLtpaCookie(WebConversation wc, String cookieName) {
        return retrieveCookie(wc, cookieName);
    }

    public String retrieveCookie(WebConversation wc, String cookieName) {

        String[] cookieNames = wc.getCookieNames();

        for (int i = 0; i < cookieNames.length; i++) {
            if (cookieName.equals(cookieNames[i])) {
                return wc.getCookieValue(cookieNames[i]);
            }
        }
        return null;
    }

    public boolean ltpaCookieExists(String ltpaToken) {

        if (ltpaToken != null && ltpaToken.length() > 1) {
            return true;
        }

        return false;

    }

}
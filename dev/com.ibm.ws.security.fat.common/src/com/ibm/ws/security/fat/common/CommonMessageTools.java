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
package com.ibm.ws.security.fat.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ValidationDataToExpectationConverter;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;

/**
 * Common messaging/logging tools for openidconnect
 */

public class CommonMessageTools {

    private final Class<?> thisClass = CommonMessageTools.class;

    private boolean clearBlankLinesFromResponseFull = true;

    public void setClearBlankLinesFromResponseFull(Boolean trueFalse) {
        clearBlankLinesFromResponseFull = trueFalse;
        //msgUtils.setClearBlankLinesFromResponseFull(true);
    }

    /**
     * Logs the state of the test assertion and then invokes the JUnit assertTrue method to record the test "status"
     * with JUnit.
     *
     * @param caller - Routine that is requesting the check be performed
     * @param msg - Message that will be recorded if the test assertion fails
     * @param testAssertion - State of the test assertion
     */
    public void assertTrueAndLog(String caller, String msg, Boolean testAssertion) {
        assertAndLog(caller, msg, testAssertion, true);
    }

    /**
     * Logs the state of the test assertion and then invokes the JUnit assertTrue or assertFalse methods, depending on
     * the value of expectedResult, to record the test "status" with JUnit.
     *
     * @param caller - Routine that is requesting the check be performed
     * @param msg - Message that will be recorded if the test assertion fails
     * @param testAssertion - State of the test assertion
     * @param expectedResult - Expected result of the test assertion
     * @return
     */

    public boolean assertAndLog(String caller, String msg, boolean testAssertion, boolean expectedResult) {
        Log.info(thisClass, caller, "Test assertion is: " + testAssertion);
        if (expectedResult) {
            if (!testAssertion) {
                Log.info(thisClass, caller, msg);
            }
            assertTrue(msg, testAssertion);
            return true;
        }
        if (testAssertion) {
            Log.info(thisClass, caller, msg);
        }
        assertFalse(msg, testAssertion);
        return false;
    }

    public void printClassName(String theClass) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, "printClassName", "################################################ "
                                              + theClass + " ################################################:");
        System.out.println("################################################ Starting test class: "
                           + theClass + " ################################################:");
    }

    public void printMethodName(String strMethod) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "***************************** "
                                       + strMethod);
        //System.out.println("***************************** " + strMethod);
    }

    public void printMethodName(String strMethod, String task) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "***************************** " + task
                                       + " " + strMethod + " *****************************");
        //System.out.println("***************************** " + task + " "
        //	+ strMethod + " *****************************");
    }

    public void printRequestParts(WebRequest request, String theMethod, String msg) throws Exception {
        printRequestParts(null, request, theMethod, msg);
    }

    public void printRequestParts(WebConversation wc, WebRequest request, String theMethod, String msg) throws Exception {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Request Parts @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (request == null) {
            Log.info(thisClass, theMethod, "The request is null - nothing to print");

            return;
        }
        try {
            Log.info(thisClass, theMethod, msg);
            Map<String, String> requestHeaders = (Map<String, String>) request.getHeaders();
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                Log.info(thisClass, theMethod, "Request header: " + entry.getKey() + ", set to: " + entry.getValue());
            }
            String[] requestParms = request.getRequestParameterNames();
            for (String req : requestParms) {
                Log.info(thisClass, theMethod, "Request parameter: " + req + ", set to: " + request.getParameter(req));
            }
            if (wc != null) {
                printAllCookies(wc);
            }
            Log.info(thisClass, theMethod, "Request Method: " + request.getMethod());
            Log.info(thisClass, theMethod, "Request Query String: " + request.getQueryString());

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, theMethod, e, "Error printing request parms (log error and go on)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Request Parts @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

    }

    public void printRequestParts(com.gargoylesoftware.htmlunit.WebRequest request, String theMethod, String msg) throws Exception {
        printRequestParts(null, request, theMethod, msg);
    }

    public void printRequestParts(WebClient webClient, com.gargoylesoftware.htmlunit.WebRequest request, String theMethod, String msg) throws Exception {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Request Parts @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (request == null) {
            Log.info(thisClass, theMethod, "The request is null - nothing to print");

            return;
        }
        try {
            if (webClient != null) {
                printAllCookies(webClient);
            }

            Log.info(thisClass, theMethod, msg);

            Log.info(thisClass, theMethod, "Request URL: " + request.getUrl());

            Map<String, String> requestHeaders = request.getAdditionalHeaders();
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                Log.info(thisClass, theMethod, "Request header: " + entry.getKey() + ", set to: " + entry.getValue());
            }
            List<NameValuePair> requestParms = request.getRequestParameters();
            for (NameValuePair req : requestParms) {
                Log.info(thisClass, theMethod, "Request parameter: " + req.getName() + ", set to: " + req.getValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, theMethod, e, "Error printing request parms (log error and go on)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Request Parts @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

    }

    public void printResponseParts(Object response, String theMethod, String msg) throws Exception {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (response == null) {
            Log.info(thisClass, theMethod, "The response is null - nothing to print");
            return;
        }
        Log.info(thisClass, "getResponseStatusCode", response.getClass().getSimpleName());

        try {
            Log.info(thisClass, theMethod, msg);
            Log.info(thisClass, theMethod, "Response (StatusCode): " + AutomationTools.getResponseStatusCode(response));
            if (AutomationTools.getResponseIsHtml(response)) {
                Log.info(thisClass, theMethod, "Response (Title): " + AutomationTools.getResponseTitle(response));
            }
            Log.info(thisClass, theMethod, "Response (Url): " + AutomationTools.getResponseUrl(response));
            String[] hs = AutomationTools.getResponseHeaderNames(response);
            if (hs != null) {
                for (String h : hs) {
                    Log.info(thisClass, theMethod, "Response (Header): Name: " + h + " Value: " + AutomationTools.getResponseHeaderField(response, h));
                }
            }
            Log.info(thisClass, theMethod, "Response (Message): " + AutomationTools.getResponseMessage(response));
            Log.info(thisClass, theMethod, "Response (Full): " + stripBlankLines(AutomationTools.getResponseText(response)));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, theMethod, e, "Error printing response (log error and go on)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

    }

    private String stripBlankLines(String text) throws Exception {

        String thisMethod = "stripBlankLines";
        //        printMethodName(thisMethod);

        //        return Arrays.toString(text.split("[\\r\\n]+"));
        if (text == null || !clearBlankLinesFromResponseFull) {
            return text;
        }

        String[] lines = text.split("[\\r\\n]+"); // should create array entries for non-blank lines
        StringBuilder strBuilder = new StringBuilder();
        for (String l : lines) {
            if (l != null && l.trim().length() > 0) {
                strBuilder.append(l + "\r\n");
            }
        }

        return strBuilder.toString();

    }

    /**
     * Use {@link CommonMessageTools#printExpectations(Expectations)}.
     */
    @Deprecated
    public void printExpectations(List<validationData> expectations) throws Exception {
        printExpectations(ValidationDataToExpectationConverter.convertValidationDataList(expectations), null);
    }
    
    public void printExpectations(Expectations expectations) throws Exception {
        printExpectations(expectations, null);
    }

    public void printExpectations(Expectations expectations, String[] actions) throws Exception {

        String thisMethod = "printExpectations";
        if (actions != null) {
            Log.info(thisClass, thisMethod, "Actions: " + Arrays.toString(actions));
        }
        if (expectations == null) {
            Log.info(thisClass, thisMethod,
                     "Expectations are null - we should never have a test case that has null expectations - that would mean we're NOT validating any results - that's bad, very bad");
            throw new Exception("NO expectations were specified - every test MUST validate it's results!!!");
        }
        for (Expectation expected : expectations.getExpectations()) {
            Log.info(thisClass, "printExpectations", "Expectations for test: ");
            if (Constants.RESPONSE_STATUS.equals(expected.getSearchLocation()) && expected.getValidationValue().equals("200")) {
                Log.info(thisClass, "printExpectations", "  Action: " + expected.getAction() + " (expect 200 response)");
            } else {
                if (actions != null && !Arrays.asList(actions).contains(expected.getAction())) {
                    Log.info(thisClass, thisMethod, "*************************************************************");
                    Log.info(thisClass, thisMethod, "* This expectation will never be processed as it's action   *");
                    Log.info(thisClass, thisMethod, "* is NOT in the list of actions to be performed             *");
                    Log.info(thisClass, thisMethod, "*************************************************************");
                    Log.info(thisClass, thisMethod, "  ***Action: " + expected.getAction());
                } else {
                    Log.info(thisClass, thisMethod, "  Action: " + expected.getAction());
                }
                Log.info(thisClass, thisMethod, "  Validate against: " + expected.getSearchLocation());
                Log.info(thisClass, thisMethod, "  How to perform check: " + expected.getCheckType());
                Log.info(thisClass, thisMethod, "  Key to validate: " + expected.getValidationKey());
                Log.info(thisClass, thisMethod, "  Value to validate: " + expected.getValidationValue());
                Log.info(thisClass, thisMethod, "  Print message: " + expected.getFailureMsg());
            }
        }
    }

    public void printAllCookies(WebConversation wc) {

        printMethodName("printAllCookies");

        String[] cookieNames = wc.getCookieNames();
        for (int i = 0; i < cookieNames.length; i++) {
            Log.info(thisClass, "printAllCookies", "Cookie: " + cookieNames[i]
                                                   + " Value: " + wc.getCookieValue(cookieNames[i]));
        }
        return;
    }

    public void printAllCookies(WebClient webClient) {

        printMethodName("printAllCookies");

        CookieManager cookieManager = webClient.getCookieManager();
        Set<Cookie> cookies = cookieManager.getCookies();
        for (Cookie cookie : cookies) {
            Log.info(thisClass, "printAllCookies", "Cookie: " + cookie.getName()
                                                   + " Value: " + cookie.getValue()
                                                   + " isSecure: " + cookie.isSecure()
                                                   + " Path: " + cookie.getPath()
                                                   + " Expires: " + cookie.getExpires()
                                                   + " Domain: " + cookie.getDomain());
        }
        return;
    }

    public void printAllParams(WebForm form) {

        String thisMethod = "printAllParams";
        if (form != null) {
            String[] ss = form.getParameterNames();
            for (String s : ss) {
                Log.info(thisClass, thisMethod, "Parm: " + s);
                Log.info(thisClass, thisMethod, "Value: " + form.getParameterValue(s));
            }
        }

    }

    public void printMarker(String strMethod, String marker) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "     ************************ " + marker + " ***********************");

    }

    public void printFormParts(WebForm form, String theMethod, String msg) throws Exception {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Form Parms @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (form == null) {
            Log.info(thisClass, theMethod, "The form is null - nothing to print");

            return;
        }
        try {
            Log.info(thisClass, theMethod, msg);
            String[] formParms = form.getParameterNames();
            for (String parm : formParms) {
                Log.info(thisClass, theMethod, "Form parameter: " + parm + ", set to: " + form.getParameterValue(parm));
            }
            SubmitButton[] formSubmits = form.getSubmitButtons();
            for (SubmitButton button : formSubmits) {
                Log.info(thisClass, theMethod, "Submit Button Name: " + button.getName());
            }
            Log.info(thisClass, theMethod, "Request is: " + form.getRequest());

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, theMethod, e, "Error printing form parms (log error and go on)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Form Parms @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

    }

    public void printFormParts(HtmlForm form, String theMethod, String msg) throws Exception {

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Form Parms @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (form == null) {
            Log.info(thisClass, theMethod, "The form is null - nothing to print");

            return;
        }
        try {
            Log.info(thisClass, theMethod, msg);
            String page = form.asText();
//            String[] formParms = form.getParameterNames();
//            for (String parm : formParms) {
//                Log.info(thisClass, theMethod, "Form parameter: " + parm + ", set to: " + form.getParameterValue(parm));
//            }
            Log.info(thisClass, theMethod, "Form: " + page);

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, theMethod, e, "Error printing form parms (log error and go on)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Form Parms @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

    }

}

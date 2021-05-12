/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

//import org.jdom.Document;
//import org.jdom.Element;
//import org.jdom.input.SAXBuilder;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class CommonTestHelpers extends TestHelpers {

    private final static Class<?> thisClass = CommonTestHelpers.class;
    public static CommonTestTools cttools = new CommonTestTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static FillFormTools formTools = new FillFormTools();
    public static EndpointSettings eSettings = new EndpointSettings();
    protected static List<TestServer> serverRefList = new ArrayList<TestServer>();
    protected static boolean overrideSetServerMark = false;

    public CommonTestHelpers() {
    }

    public void addToServerRefList(TestServer server) {
        serverRefList.add(server);
    }

    public void removeFromServerRefList(TestServer server) {

        serverRefList.remove(server);
    }

    public void setOverrideSetServerMark(boolean setMark) {
        overrideSetServerMark = setMark;
    }

    public WebResponse invokeFirstClient(String testcase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeFirstClient";
        msgUtils.printMethodName(thisMethod);

        WebRequest request = null;
        WebResponse response = null;

        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: " + settings.getFirstClientURL());
            request = new GetMethodWebRequest(settings.getFirstClientURL());

            // Invoke the client
            response = wc.getResponse(request);

            msgUtils.printResponseParts(response, thisMethod, "Response from firstClientURL: ");

            validationTools.validateResult(response, Constants.INVOKE_OAUTH_CLIENT, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.INVOKE_OAUTH_CLIENT, e);

        }
        return response;

    }

    public Object invokeFirstClient(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeFirstClient - webClient";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "firstClientUrl: " + settings.getFirstClientURL());
            URL url = AutomationTools.getNewUrl(settings.getFirstClientURL());
            com.gargoylesoftware.htmlunit.WebRequest requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);

            // Invoke the client
            thePage = webClient.getPage(requestSettings);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from firstClientURL: ");

            validationTools.validateResult(thePage, Constants.INVOKE_OAUTH_CLIENT, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.INVOKE_OAUTH_CLIENT, e);

        }
        return thePage;

    }

    public WebResponse submitToAuthServer(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations, String submit_type) throws Exception {

        String thisMethod = "submitToAuthServer";
        msgUtils.printMethodName(thisMethod);

        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            WebForm form = formTools.fillClientForm(testcase, response.getForms()[0], settings);

            Log.info(thisClass, thisMethod, "new prop:" + settings.getTokenEndpt());

            // Submit the request
            // specify the null, 0,0 to work around a bug in httpunit where it
            // does a double submit under the covers
            response = form.submit(null, 0, 0);
            msgUtils.printAllCookies(wc);
            msgUtils.printResponseParts(response, thisMethod, "Response1 from Authorization server: ");
            msgUtils.printAllCookies(wc);

            if (response.getTitle().contains("Code Grant")) {
                Log.info(thisClass, thisMethod, "Need to submit the form a second time");
                WebForm form2 = response.getForms()[0];
                //msgUtils.printFormParts(form2, thisMethod, "debug form 2");

                if (submit_type.equals(Constants.SUBMIT_TO_AUTH_SERVER)) {
                    // specify the null, 0,0 to work around a bug in httpunit
                    // where it does a double submit under the covers
                    response = form2.submit(null, 0, 0);
                } else {
                    WebRequest request = null;
                    request = form2.getRequest();
                    Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
                    cttools.addBasicAuthCredToHeader(request, settings);

                    // specify the null, 0,0 to work around a bug in httpunit
                    // where it does a double submit under the covers
                    // response = form2.submit(null, 0, 0);
                    response = wc.getResponse(request);
                }
            }
            msgUtils.printResponseParts(response, thisMethod, "Response2 from Authorization server: ");
            msgUtils.printAllCookies(wc);
            validationTools.validateResult(response, Constants.SUBMIT_TO_AUTH_SERVER, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.SUBMIT_TO_AUTH_SERVER, e);
        }
        return response;

    }

    public Object submitToAuthServer(String testcase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations, String submit_type) throws Exception {

        String thisMethod = "submitToAuthServer - webClient";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            final HtmlForm form = formTools.fillClientForm(testcase, ((HtmlPage) startPage).getFormByName("authform"), settings);

            msgUtils.printAllCookies(webClient);
            Log.info(thisClass, thisMethod, "new prop:" + settings.getTokenEndpt());

            webClient.getCookieManager().setCookiesEnabled(true);
            HtmlButton button1 = form.getButtonByName("processAzn");
            thePage = button1.click();

            msgUtils.printResponseParts(thePage, thisMethod, "Response1 from Authorization server: ");
            msgUtils.printAllCookies(webClient);

            if (AutomationTools.getResponseTitle(thePage).contains("Code Grant")) {

                Log.info(thisClass, thisMethod, "Need to submit the form a second time");
                webClient.getCookieManager().setCookiesEnabled(true);

                HtmlForm form2 = ((HtmlPage) thePage).getFormByName("authform");
                HtmlButton button2 = form2.getButtonByName("processAzn");
                thePage = button2.click();

                msgUtils.printAllCookies(webClient);
                msgUtils.printResponseParts(thePage, thisMethod, "Response2 from Authorization server: ");

            }
            validationTools.validateResult(thePage, Constants.SUBMIT_TO_AUTH_SERVER, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.SUBMIT_TO_AUTH_SERVER, e);
        }
        return thePage;

    }

    public WebResponse submitToAuthServerForToken(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "submitToAuthServerForToken";
        msgUtils.printMethodName(thisMethod);

        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            WebForm form = formTools.fillClientForm2(response.getForms()[0], settings);

            // Log.info(thisClass, thisMethod, "Submit: " +
            // settings.getTokenEndpt());

            // Submit the request
            // specify the null, 0,0 to work around a bug in httpunit where it
            // does a double submit under the covers
            response = form.submit(null, 0, 0);
            msgUtils.printResponseParts(response, thisMethod, "Response from Authorization server (submit): ");

            validationTools.validateResult(response, Constants.SUBMIT_TO_AUTH_SERVER_FOR_TOKEN, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.SUBMIT_TO_AUTH_SERVER_FOR_TOKEN, e);
        }
        return response;

    }

    public WebResponse invokeAuthServer(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations, String invoke_type) throws Exception {

        String thisMethod = "invokeAuthServer";
        msgUtils.printMethodName(thisMethod);

        WebRequest request = null;
        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            // Start the OAuth request by invoking the client
            Log.info(thisClass, thisMethod, "authorization Endpoint: "
                    + settings.getAuthorizeEndpt());
            request = new GetMethodWebRequest(settings.getAuthorizeEndpt());
            Log.info(thisClass, thisMethod, "Request before filling is: " + request);

            request = formTools.fillAuthorizationForm(request, settings);
            Log.info(thisClass, thisMethod, "Request after filling is: " + request);

            if (invoke_type.equals(Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH)) {
                cttools.addBasicAuthCredToHeader(request, settings);
            }

            Log.info(thisClass, thisMethod, "Request after filling is: " + request);
            // Invoke the client
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response from Auth server: ");

            validationTools.validateResult(response, Constants.INVOKE_AUTH_SERVER, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.INVOKE_AUTH_SERVER, e);
        }
        return response;

    }

    /*
     * public WebResponse invokeRefreshJsp(String testcase, WebConversation wc,
     * WebResponse response, TestSettings settings, List<validationData>
     * expectations) throws Exception {
     *
     * String thisMethod = "invokeRefreshJsp" ; WebRequest request = null;
     *
     * try { String tokenValue = validationTools.getTokenFromResponse(response,
     * Constants.ACCESS_TOKEN_KEY) ;
     *
     *
     * Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);
     *
     * // Now, invoke refresh.jsp request = new
     * GetMethodWebRequest(settings.getRefreshTokUrl());
     *
     * // Invoke the client response = wc.getResponse(request);
     * Log.info(thisClass, thisMethod, "Response from refresh jsp: " +
     * response.getText());
     *
     * WebForm form2 = response.getForms()[0];
     *
     * // Set form values form2.setParameter("client_id",
     * settings.getClientID()); form2.setParameter("client_secret",
     * settings.getClientSecret()); form2.setParameter("refresh_token",
     * tokenValue) ; form2.setParameter("token_endpoint",
     * settings.getTokenEndpt()); form2.setParameter("scope",
     * settings.getScope());
     *
     * // specify the null, 0,0 to work around a bug in httpunit where it does a
     * double submit under the covers response = form2.submit(null, 0, 0);
     * Log.info(thisClass, thisMethod, "Response from token endpt: " +
     * response.getText());
     *
     * // Check if we received access token
     * assertTrue("Did not receive access token with refresh token",
     * respReceived.contains(recvAccessToken));
     *
     * // If we received access token from a refresh token, test has // passed.
     * Log.info(thisClass, thisMethod, "Test Passed!"); return response ; }
     * catch (Exception e) { Log.error(thisClass, testcase, e,
     * "Exception occurred in " + thisMethod ); System.err.println("Exception: "
     * + e); throw e; }
     *
     * }
     */

    public WebResponse performLogin(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performLogin";
        msgUtils.printMethodName(thisMethod);

        WebRequest request = null;
        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            assertTrue("Did not receive a prompt to login, user not found or DB/Custom store not loaded: " + settings.getClientID(), response.getForms().length > 0);

            // Should receive prompt for resource owner to login
            WebForm form3 = response.getForms()[0];
            Log.info(thisClass, thisMethod, "Form for resource owner login: ", form3);

            form3 = formTools.fillOPLoginForm(form3, settings);

            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response from login: ");

            validationTools.validateResult(response, Constants.PERFORM_LOGIN, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.PERFORM_LOGIN, e);
        }
        return response;

    }

    public Object performLogin(String testcase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performLogin";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {
            setMarkToEndOfAllServersLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing Form Login...");

            final HtmlForm form = ((HtmlPage) startPage).getForms().get(0);

            // Fill in the login form and submit the login request

            HtmlElement button = null;
            HtmlSubmitInput loginButton = null;

            try {
                button = form.getButtonByName("submitButton");
            } catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
                Log.info(thisClass, thisMethod, "App login page doesn't have a submit button");
                loginButton = form.getInputByValue("Login"); // must be on the apps login page - get that button instead
            } catch (Exception e) {
                // we should never get a different exception, so, rethrow and let the test case fail
                throw e;
            }

            final HtmlTextInput textField = form.getInputByName("j_username");
            Log.info(thisClass, thisMethod, "username field is: " + textField);
            textField.setValueAttribute(settings.getAdminUser());
            final HtmlPasswordInput textField2 = form.getInputByName("j_password");
            Log.info(thisClass, thisMethod, "password field is: " + textField2);
            textField2.setValueAttribute(settings.getAdminPswd());
            Log.info(thisClass, thisMethod, "Setting: " + textField + " to: " + settings.getAdminUser());
            Log.info(thisClass, thisMethod, "Setting: " + textField2 + " to: " + settings.getAdminPswd());

            msgUtils.printFormParts(form, thisMethod, "Parms for FormLogin: ");

            if (button == null) {
                Log.info(thisClass, thisMethod, "\'Pressing the Login button\'");
                thePage = loginButton.click();
            } else {
                Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");
                thePage = button.click();
            }
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.validateResult(thePage, Constants.PERFORM_LOGIN, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.PERFORM_LOGIN, e);
        }

        return (thePage);
    }

    public WebResponse performISAMLogin(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performISAMLogin";
        msgUtils.printMethodName(thisMethod);

        WebRequest request = null;
        try {
            // set the mark to the end of all logs to ensure that any
            // checking for
            // messages is done only for this step of the testing
            setMarkToEndOfAllServersLogs();

            // Should receive prompt for resource owner to login
            WebForm form3 = response.getForms()[0];
            Log.info(thisClass, thisMethod, "Form for resource owner login: ", form3);

            form3 = formTools.fillOPLoginForm(form3, settings);

            request = form3.getRequest();
            Log.info(thisClass, thisMethod, "Target URL of request: " + request.getURL());
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response from login: ");

            validationTools.validateResult(response, Constants.PERFORM_ISAM_LOGIN, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.PERFORM_ISAM_LOGIN, e);
        }
        return response;

    }

    public WebResponse invokeProtectedResource(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeProtectedResource";
        msgUtils.printMethodName(thisMethod);

        String tokenValue = null;
        if (response != null) {
            // Extract access token from response
            tokenValue = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

        }

        return invokeProtectedResource(testcase, wc, tokenValue, settings, expectations);

    }

    public WebResponse invokeProtectedResource(String testcase, WebConversation wc, String accessToken, TestSettings settings, List<validationData> expectations) throws Exception {
        return invokeProtectedResource(testcase, wc, accessToken, Constants.PARM, settings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);
    }

    public WebResponse invokeProtectedResource(String testcase, WebConversation wc, String accessToken, String where, TestSettings settings, List<validationData> expectations, String currentAction) throws Exception {

        String thisMethod = "invokeProtectedResource";
        msgUtils.printMethodName(thisMethod);

        WebResponse response = null;
        WebRequest request = null;

        try {
            setMarkToEndOfAllServersLogs();

            // Invoke protected resource
            request = new GetMethodWebRequest(settings.getProtectedResource());

            if (accessToken != null && where != null) {
                if (where.equals(Constants.PARM)) {
                    request.setParameter(Constants.ACCESS_TOKEN_KEY, accessToken);
                    // throw new
                    // Exception("A valid access token was not passed into invokeProtectedResource")
                    // ;
                } else {
                    request.setHeaderField(Constants.AUTHORIZATION, Constants.BEARER + " " + accessToken);
                }
            }

            msgUtils.printRequestParts(request, thisMethod, "Request for " + currentAction);
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response from protected app: ");

        } catch (Exception e) {

            validationTools.validateException(expectations, currentAction, e);

        }
        validationTools.validateResult(response, currentAction, expectations, settings);
        return response;

    }

    public Object invokeProtectedResource(String testcase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeProtectedResource";
        msgUtils.printMethodName(thisMethod);

        String tokenValue = null;
        if (startPage != null) {
            // Extract access token from response
            tokenValue = validationTools.getTokenFromResponse(startPage, Constants.ACCESS_TOKEN_KEY);
            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);

        }

        return invokeProtectedResource(testcase, webClient, tokenValue, settings, expectations);

    }

    public Object invokeProtectedResource(String testcase, WebClient webClient, String accessToken, TestSettings settings, List<validationData> expectations) throws Exception {
        return invokeProtectedResource(testcase, webClient, accessToken, Constants.PARM, settings, expectations, Constants.INVOKE_PROTECTED_RESOURCE);
    }

    public Object invokeProtectedResource(String testcase, WebClient webClient, String accessToken, String where, TestSettings settings, List<validationData> expectations, String currentAction) throws Exception {

        String thisMethod = "invokeProtectedResource";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {
            setMarkToEndOfAllServersLogs();

            // Invoke protected resource
            URL url = AutomationTools.getNewUrl(settings.getProtectedResource());
            com.gargoylesoftware.htmlunit.WebRequest request = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);
            request.setRequestParameters(new ArrayList());

            if (accessToken != null && where != null) {
                if (where.equals(Constants.PARM)) {
                    setRequestParameterIfSet(request, Constants.ACCESS_TOKEN_KEY, accessToken);
                    // throw new
                    // Exception("A valid access token was not passed into invokeProtectedResource")
                    // ;
                } else {
                    request.setAdditionalHeader(Constants.AUTHORIZATION, Constants.BEARER + " " + accessToken);
                }
            }

            msgUtils.printRequestParts(request, thisMethod, "Request for " + currentAction);
            thePage = webClient.getPage(request);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from protected app: ");

        } catch (Exception e) {

            validationTools.validateException(expectations, currentAction, e);

        }
        validationTools.validateResult(thePage, currentAction, expectations, settings);
        return thePage;

    }

    /**
     * Invoke an RS Protected resource using the access_token in the reqponse
     *
     * @param testcase
     * @param wc
     * @param response
     * @param settings
     * @param expectations
     * @return
     * @throws Exception
     */
    public WebResponse invokeRsProtectedResource(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {
        return invokeRsProtectedResourceWithConversation(testcase, new WebConversation(), response, settings, expectations);
    }

    public WebResponse invokeRsProtectedResourceWithConversation(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeRsProtectedResource";
        msgUtils.printMethodName(thisMethod);

        String tokenValue = null;
        if (response != null) {
            // Extract token from response
            tokenValue = validationTools.getTokenForType(settings, response);
            Log.info(thisClass, thisMethod, "Token Value: " + tokenValue);
        }

        return invokeRsProtectedResource(testcase, wc, tokenValue, settings, expectations);

    }

    public WebResponse invokeRsProtectedResource(String testcase, WebConversation wc, String tokenValue, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeRsProtectedResource";
        msgUtils.printMethodName(thisMethod);

        String where = settings.getWhere();

        HashMap<String, String[]> parms = null;
        HashMap<String, String[]> headers = null;
        String getOrPost = Constants.POSTMETHOD;
        if (where != null && where.equals(Constants.PARM)) {
            parms = new HashMap<String, String[]>();
            // add tokenKey to settings - have front end test set to
            // access_token, id_token, jwt, ...
            // test case can over-ride when we're testing a user defined
            // key/attr value
            parms.put(settings.getRsTokenKey(), new String[] { tokenValue });
            // parms.put(Constants.ACCESS_TOKEN_KEY, new String[]{tokenValue}) ;
            getOrPost = Constants.POSTMETHOD;
            Log.info(thisClass, thisMethod, "Setting parm in POST request");
        } else if (where != null) {
            headers = new HashMap<String, String[]>();
            headers.put(settings.getHeaderName(), new String[] { Constants.BEARER + " " + tokenValue });
            getOrPost = Utils.getRandomSelection(Constants.GETMETHOD, Constants.POSTMETHOD);
            Log.info(thisClass, thisMethod, "Setting header in " + getOrPost + " request");
        }
        WebConversation new_wc = null;
        if (wc == null) {
            new_wc = new WebConversation();
        } else {
            new_wc = wc;
        }
        // TestSettings tempSettings = settings.copyTestSettings() ;
        // tempSettings.setProtectedResource(settings.getRSProtectedResource());
        // return invokeProtectedResource(testcase, new_wc, tokenValue,
        // tempSettings.getWhere(), tempSettings, expectations,
        // Constants.INVOKE_RS_PROTECTED_RESOURCE);
        return invokeRSProtectedResource(testcase, new_wc, getOrPost, headers, parms, settings, expectations);
    }

    /**
     * Inovke an RS protected resource using the access_token provided in the
     * invocation of this method
     *
     * @param testcase
     * @param wc
     * @param tokenValue
     * @param settings
     * @param expectations
     * @return
     * @throws Exception
     */
    public WebResponse invokeRsProtectedResourceWithToken(String testcase, WebConversation wc, String tokenValue, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeRsProtectedResourceWithToken";
        msgUtils.printMethodName(thisMethod);

        TestSettings tempSettings = settings.copyTestSettings();
        tempSettings.setProtectedResource(settings.getRSProtectedResource());
        return invokeProtectedResource(testcase, wc, tokenValue, tempSettings.getWhere(), tempSettings, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE);

    }

    public WebResponse invokeRSProtectedResource(String testcase, String getOrPost, HashMap<String, String[]> headers, HashMap<String, String[]> parms, TestSettings settings, List<validationData> expectations) throws Exception {
        return invokeRSProtectedResource(testcase, new WebConversation(), getOrPost, headers, parms, settings, expectations);
    }

    /**
     * Invoke an RS protected resource using the headers/parms passed and
     * invoking either GET or POST as caller specifies
     *
     * @param testcase
     * @param wc
     * @param getOrPost
     * @param headers
     *            TODO
     * @param parms
     * @param settings
     * @param expectations
     * @return
     * @throws Exception
     */
    public WebResponse invokeRSProtectedResource(String testcase, WebConversation wc, String getOrPost, HashMap<String, String[]> headers, HashMap<String, String[]> parms, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeRSProtectedResource";
        msgUtils.printMethodName(thisMethod);

        WebResponse response = null;
        WebRequest request = null;

        try {
            setMarkToEndOfAllServersLogs();

            TestSettings tempSettings = settings.copyTestSettings();
            tempSettings.setProtectedResource(settings.getRSProtectedResource());

            // Invoke protected resource
            if (getOrPost.equals(Constants.GETMETHOD)) {
                request = new GetMethodWebRequest(tempSettings.getProtectedResource());
            } else {
                request = new PostMethodWebRequest(tempSettings.getProtectedResource());
            }

            if (headers != null) {
                for (Map.Entry<String, String[]> entry : headers.entrySet()) {
                    String[] values = entry.getValue();
                    for (String value : values) {
                        if (value != null && !value.equals("null")) {
                            Log.info(thisClass, thisMethod, "Add Header: " + entry.getKey() + " with value: " + value);
                            request.setHeaderField(entry.getKey(), value);
                        }
                    }
                }
            }
            if (parms != null) {
                for (Map.Entry<String, String[]> entry : parms.entrySet()) {
                    String[] values = entry.getValue();
                    for (String value : values) {
                        if (value != null && !value.equals("null")) {
                            Log.info(thisClass, thisMethod, "Add Parm: " + entry.getKey() + " with value: " + value);
                            request.setParameter(entry.getKey(), value);
                        }
                    }
                }
            }

            msgUtils.printRequestParts(wc, request, thisMethod, "Request for " + thisMethod);
            response = wc.getResponse(request);
            msgUtils.printAllCookies(wc);
            msgUtils.printResponseParts(response, thisMethod, "Response from protected app: ");

        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e);
            msgUtils.printAllCookies(wc);
            if (response != null) {
                msgUtils.printResponseParts(response, thisMethod, "Response from Failed invokation of protected app: ");
            }
            validationTools.validateException(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, e);

        }
        validationTools.validateResult(response, Constants.INVOKE_RS_PROTECTED_RESOURCE, expectations, settings);
        return response;

    }

    public WebResponse invokeJwtConsumer(String testcase, WebConversation wc, String tokenValue, TestSettings settings, List<validationData> expectations) throws Exception {
        String thisMethod = "invokeJwtConsumer";
        msgUtils.printMethodName(thisMethod);

        WebConversation newWC = (wc == null) ? new WebConversation() : wc;
        WebResponse response = null;

        try {
            setMarkToEndOfAllServersLogs();

            // Build the request object
            WebRequest request = new PostMethodWebRequest(settings.getJwtConsumerUrl());
            String configId = settings.getJwtId();
            if (configId != null) {
                request.setParameter(Constants.JWT_CONSUMER_PARAM_CLIENT_ID, configId);
            }
            if (tokenValue != null) {
                request.setParameter(Constants.JWT_CONSUMER_PARAM_JWT, tokenValue);
            }

            // Execute the request
            msgUtils.printRequestParts(newWC, request, thisMethod, "Request for " + thisMethod);
            response = newWC.getResponse(request);
            msgUtils.printAllCookies(newWC);
            msgUtils.printResponseParts(response, thisMethod, "Response from consumer app: ");

        } catch (Exception e) {
            msgUtils.printAllCookies(newWC);
            if (response != null) {
                msgUtils.printResponseParts(response, thisMethod, "Response from failed invocation of consumer app: ");
            }
            validationTools.validateException(expectations, Constants.INVOKE_JWT_CONSUMER, e);

        }
        validationTools.validateResult(response, Constants.INVOKE_JWT_CONSUMER, expectations, settings);
        return response;
    }

    public WebResponse getLoginPage(String testcase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {
        return rpLoginPage(testcase, wc, settings, expectations, Constants.GETMETHOD);
    }

    public WebResponse postLoginPage(String testcase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {
        return rpLoginPage(testcase, wc, settings, expectations, Constants.POSTMETHOD);
    }

    public WebResponse rpLoginPage(String testcase, WebConversation wc, TestSettings settings, List<validationData> expectations, String invokeType) throws Exception {

        String thisStep = null;
        String thisMethod = "rpLoginPage";
        msgUtils.printMethodName(thisMethod);
        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();
        WebResponse response = null;

        try {
            // Invoke
            WebRequest request = null;
            if (invokeType.equals(Constants.GETMETHOD)) {
                Log.info(thisClass, thisMethod, "GET request");
                request = new GetMethodWebRequest(settings.getTestURL());
                thisStep = Constants.GET_LOGIN_PAGE;

            } else {
                Log.info(thisClass, thisMethod, "POST request");
                request = new PostMethodWebRequest(settings.getTestURL(), true);
                // File binaryFile = new
                // File("c:/chrisc/screen_savers/party_photo.bmp") ;
                // File textFile = new File("c:/temp/testFile") ;
                // request.selectFile("binaryFile", binaryFile) ;
                // request.selectFile("textFile", textFile) ;
                thisStep = Constants.POST_LOGIN_PAGE;
            }

            if (request.isFileParameter("textFile")) {
                Log.info(thisClass, thisMethod, "We see the textFile in the request");
            }
            if (request.isFileParameter("TestingParm1")) {
                Log.info(thisClass, thisMethod, "TestingParm1 is NOT a file");
            }
            Map<String, String> reqParms = settings.getRequestParms();
            if (reqParms != null) {
                for (String key : reqParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqParms.get(key));
                    request.setParameter(key, reqParms.get(key));
                }
            }
            Map<String, String> reqFileParms = settings.getRequestFileParms();
            if (reqFileParms != null) {
                for (String key : reqFileParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqFileParms.get(key));
                    request.setParameter(key, reqParms.get(key));
                    request.selectFile(key, new File(reqFileParms.get(key)));
                }
            }

            Log.info(thisClass, testcase, "Request: " + request);

            // Check the response
            response = wc.getResponse(request);

            msgUtils.printResponseParts(response, thisMethod, "Response from getLoginPage: ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, thisStep, e);
        }
        validationTools.validateResult(response, thisStep, expectations, settings);

        return response;
    }

    public Object getLoginPage(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations) throws Exception {
        return rpLoginPage(testcase, webClient, settings, expectations, Constants.GETMETHOD);
    }

    public Object postLoginPage(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations) throws Exception {
        return rpLoginPage(testcase, webClient, settings, expectations, Constants.POSTMETHOD);
    }

    public Object rpLoginPage(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations, String invokeType) throws Exception {

        String thisStep = null;
        String thisMethod = "rpLoginPage";
        msgUtils.printMethodName(thisMethod);
        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();

        Object thePage = null;

        try {
            com.gargoylesoftware.htmlunit.WebRequest requestSettings = null;

            // Invoke
            URL url = AutomationTools.getNewUrl(settings.getTestURL());
            if (invokeType.equals(Constants.GETMETHOD)) {
                Log.info(thisClass, thisMethod, "GET request");
                requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);
                thisStep = Constants.GET_LOGIN_PAGE;

            } else {
                Log.info(thisClass, thisMethod, "POST request");
                requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.POST);
                thisStep = Constants.POST_LOGIN_PAGE;
            }

            requestSettings.setRequestParameters(new ArrayList());
            Map<String, String> reqParms = settings.getRequestParms();
            if (reqParms != null) {
                for (String key : reqParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqParms.get(key));
                    setRequestParameterIfSet(requestSettings, key, reqParms.get(key));
                }
            }
            Map<String, String> reqFileParms = settings.getRequestFileParms();
            if (reqFileParms != null) {
                for (String key : reqFileParms.keySet()) {
                    Log.info(thisClass, thisMethod, "Key: " + key + " Value: " + reqFileParms.get(key));
                    setRequestParameterIfSet(requestSettings, key, reqParms.get(key));
                }
            }

            // Check the response
            Log.info(thisClass, thisMethod, "Outgoing request url: " + requestSettings.getUrl().toString());
            thePage = webClient.getPage(requestSettings);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisStep + ": ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, thisStep, e);
        }
        validationTools.validateResult(thePage, thisStep, expectations, settings);

        return thePage;
    }

    /**
     * Handle filling in and submitting the OpenId Form (specify the OpenID
     * Provider URL
     *
     * @param response
     *            - the response from the previous step (contains the form to
     *            continue on with)
     * @param settings
     *            - the test settings to use for this invocation
     * @param statusMap
     *            - the hashmap of the status codes expected for each step in
     *            the test process
     * @param expectations
     *            - the array of validationMsgs that this test expects
     * @throws Exception
     */
    public WebResponse processOpenIdForm(WebResponse response, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processOpenIdForm";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();

        Integer numSubmissions = Constants.SUBMIT_NUM_TIMES;

        try {
            /* get the login form out of the response and fill it in */
            WebForm loginForm = formTools.fillProviderUrl(response.getForms()[0], settings);

            /* Yahoo requires an additional submit */
            if ((settings.getProviderType() != null)
                    && (settings.getProviderType().contains(Constants.YAHOO_TYPE))) {
                numSubmissions = 2;
            }

            response = cttools.submitAndPrint(loginForm, numSubmissions, null, "Response from call to OpenID submission: ");

            Log.info(thisClass, thisMethod, "finished filling in the provider info");

        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.SPECIFY_PROVIDER, e);
        }
        validationTools.validateResult(response, Constants.SPECIFY_PROVIDER, expectations, settings);
        return response;

    }

    /**
     * Handle filling in and submitting the Login Form
     *
     * @param wc
     *            - the webconversation
     * @param response
     *            - the response from the previous step (contains the form to
     *            continue on with)
     * @param settings
     *            - the test settings to use for this invocation
     * @param statusMap
     *            - the hashmap of the status codes expected for each step in
     *            the test process
     * @param testActions
     *            - the test actions that this test will perform
     * @param expectations
     *            - the array of validationMsgs that this test expects
     * @throws Exception
     */
    public WebResponse processProviderLoginForm(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        WebRequest request = null;
        String thisMethod = "processProviderLoginForm";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();

        try {

            // get and fill in the form returned from the OpenID Provider
            WebForm providerLoginForm = response.getForms()[0];
            formTools.fillRPLoginForm(providerLoginForm, settings);
            Log.info(thisClass, thisMethod, "set user: " + providerLoginForm.getParameterValue(settings.getUserParm()));
            Log.info(thisClass, thisMethod, "set pass: " + providerLoginForm.getParameterValue(settings.getPassParm()));

            if (settings.getLoginButton() != null && !settings.getLoginButton().isEmpty()) {
                response = cttools.clickAndPrint(wc, providerLoginForm, settings.getLoginButton());
            } else {
                Log.info(thisClass, thisMethod, "Provider type is: " + settings.getProviderType());

                request = providerLoginForm.getRequest();
                Log.info(thisClass, thisMethod, "Request is: " + request.toString());
                Log.info(thisClass, thisMethod, "Request URL: " + request.getURL().toString());
                Log.info(thisClass, thisMethod, "Request method: " + request.getMethod());
                String[] parms = request.getRequestParameterNames();
                for (String s : parms) {
                    Log.info(thisClass, thisMethod, "Request parm: " + s + " " + request.getParameter(s));
                }

                response = wc.getResponse(request);
                msgUtils.printAllCookies(wc);
                msgUtils.printResponseParts(response, thisMethod, "Response from providerLoginForm: ");
            }

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.LOGIN_USER, e);
        }
        validationTools.validateResult(response, Constants.LOGIN_USER, expectations, settings);
        return response;
    }

    public static Object processProviderLoginForm(String testcase, WebClient webClient, HtmlPage startPage, String currentAction, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processProviderLoginForm";
        msgUtils.printMethodName(thisMethod + "|" + currentAction);
        Object thePage = null;
        try {
            if (startPage.getForms().size() > 0) {
                final HtmlForm form = startPage.getForms().get(0);
                webClient.getOptions().setJavaScriptEnabled(false);

                Log.info(thisClass, thisMethod + "|" + currentAction, "currentAction: " + currentAction);
                Log.info(thisClass, thisMethod + "|" + currentAction, "Looking for Button name: " + settings.getLoginButton());
                HtmlElement button = form.getButtonByName(settings.getLoginButton());

                final HtmlTextInput textField = form.getInputByName(settings.getUserParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "username field is: " + textField);
                textField.setValueAttribute(settings.getUserName());
                final HtmlPasswordInput textField2 = form.getInputByName(settings.getPassParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "password field is: " + textField2);
                textField2.setValueAttribute(settings.getUserPassword());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField + " to: " + settings.getUserName());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField2 + " to: " + settings.getUserPassword());
                //            }
                Log.info(thisClass, thisMethod + "|" + currentAction, "\'Pressing the " + button + " button\'");

                thePage = button.click();

                if (AutomationTools.getResponseStatusCode(thePage) == Constants.BAD_GATEWAY) {
                    // try again - LinkedIn throws a 502 periodically
                    thePage = button.click();
                }
            } else {
                thePage = startPage;
            }
            msgUtils.printResponseParts(thePage, thisMethod + "|" + currentAction, "Response from " + thisMethod + "|" + currentAction + ": ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod + "|" + currentAction);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, currentAction, e);
        }

        msgUtils.printAllCookies(webClient);
        validationTools.validateResult(thePage, currentAction, expectations, settings);

        return thePage;

    }

    //
    // This processRPConsent method is compatible but not 100% equivalent
    // testing method.
    // But due to the httpunit can not handle the javascript properly,
    // we need to use these method to test,
    // because it's better than nothing.
    //
    // The method here is trying to translate
    // /com.ibm.ws.security.oauth-2.0/resources/scripts/oauthForm.js
    // /com.ibm.ws.security.oauth-2.0/resources/scripts/oauthForm.js
    // and make a COMPATIBLE but not exactly the same request through "GET"
    //
    // *****************************************************************************
    // * Keep in mind that whenever
    // * the product changes the ways they handle the Consent form,
    // * we need to come back and
    // * review this piece of code to make sure it is still compatible.
    // *****************************************************************************
    //
    public WebResponse processRPConsent(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {
        // With TFIM, we get an acceptance panal after a successful login
        // With a failed login, we're getting a status code 200, an a failed
        // authentication page.

        WebRequest request = null;
        String thisMethod = "processRPConsent";
        msgUtils.printMethodName(thisMethod);

        String responseFullText = response.getText();
        Log.info(thisClass, thisMethod, responseFullText);
        HashMap<String, Object> oauthFormData = parseOauthFormData(responseFullText);
        Log.info(thisClass, thisMethod, oauthFormData.toString());

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();
        try {

            Log.info(thisClass, thisMethod, "get consent");
            /*
             * for Nonce testing - we may want to wait before confirming so
             * check for a confirmSleep value and take a nap if set
             */
            if (settings.getConfirmSleep() != null) {
                Log.info(thisClass, thisMethod, "Sleeping "
                        + settings.getConfirmSleep()
                        + " milliseconds before confirming");
                Thread.sleep(settings.getConfirmSleep());
            }
            WebForm form4 = response.getForms()[0];

            URL url = response.getURL();
            String authorizeUrl = url.getProtocol() + "://" + url.getHost() +
                    ":" + url.getPort() + // we alwyas have a port
                    url.getPath();
            String parameters = "";
            if (responseFullText.indexOf("\"/oauth2/scripts/oauthForm.js\"") >= 0) { // oauth
                parameters = handleOauthConsentForm(form4, response, oauthFormData, settings);
            } else if (responseFullText.indexOf("\"/oidc/scripts/oauthForm.js\"") >= 0) {
                parameters = handleOidcConsentForm(form4, response, oauthFormData, settings);
            } else {
                Log.info(thisClass, thisMethod, "***ERROR: Can not find oidc or oauth oauthForm.js");
            }

            // TestSettings hard coded the state value
            // which needs to be updated when we get it
            String state = (String) oauthFormData.get("state");
            if (state != null && !state.isEmpty()) {
                settings.setState(state);
            }

            if (!parameters.isEmpty()) {
                authorizeUrl = authorizeUrl + "?" + parameters;
            }
            form4.setAttribute("action", authorizeUrl);
            form4.setAttribute("method", "GET");
            Log.info(thisClass, thisMethod, "action:"
                    + authorizeUrl
                    + " method:GET");

            request = form4.getRequest();
            response = wc.getResponse(request);

            msgUtils.printResponseParts(response, thisMethod, "Response from processRPConsent: ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.GET_RP_CONSENT, e);
        }
        validationTools.validateResult(response, Constants.GET_RP_CONSENT, expectations, settings);

        return response;
    }

    String handleOidcConsentForm(WebForm form4, WebResponse response, HashMap<String, Object> oauthFormData, TestSettings settings) {
        // always check
        // /com.ibm.ws.security.oauth-2.0/resources/scripts/oauthForm.js for any
        // new changes
        String[] props = new String[] { // need to handle scope, redirect_uri and prompt
                // The value are all string at the writing moment
                "consentNonce",
                "client_id",
                "response_type",
                "state"
        };
        String[] extendProps = new String[] {
                // The value are all string at the writing moment
                "nonce",
                "acr_values"
        };
        return handleConsentForm(form4, oauthFormData, settings, props, extendProps, true);
    }

    String handleOauthConsentForm(WebForm form4, WebResponse response, HashMap<String, Object> oauthFormData, TestSettings settings) {
        // always check
        // /com.ibm.ws.security.openidconnect.server-1.0/resources/scripts/oauthForm.js
        // for any new changes
        String[] props = new String[] { // need to handle scope, redirect_uri and prompt
                "consentNonce",
                "client_id",
                "response_type",
                "state"
        };
        String[] extendProps = new String[] {};
        return handleConsentForm(form4, oauthFormData, settings, props, extendProps, false);
    }

    @SuppressWarnings("unchecked")
    String handleConsentForm(WebForm form4, HashMap<String, Object> oauthFormData, TestSettings settings, // for future
            String[] props, String[] extendProps, boolean bOidc) {
        String parameters = "";
        // need to handle scope, redirect_uri and prompt
        for (String prop : props) {
            String value = (String) oauthFormData.get(prop);
            // form4.setParameter(prop, value);
            if (value != null) {
                if (!parameters.isEmpty()) {
                    parameters = parameters + "&";
                }
                parameters = parameters + prop + "=" + value;
            }
        }

        HashMap<String, Object> extendData = (HashMap<String, Object>) oauthFormData.get("extendedProperties");
        for (String extProp : extendProps) {
            String value = (String) extendData.get(extProp);
            // form4.setParameter(extProp, value);
            if (value != null) {
                if (!parameters.isEmpty()) {
                    parameters = parameters + "&";
                }
                parameters = parameters + extProp + "=" + value;
            }
        }
        if (bOidc) { // Let's handle response_mode
            String responseMode = settings.getResponseMode();
            if (responseMode != null) {
                String value = (String) extendData.get(responseMode);
                // form4.setParameter(extProp, value);
                if (value != null) {
                    if (!parameters.isEmpty()) {
                        parameters = parameters + "&";
                    }
                    parameters = parameters + responseMode + "=" + value;
                }
            }
        }

        // handle scope
        String[] scopes = (String[]) oauthFormData.get("scope");
        String scope = "";
        for (String scope1 : scopes) {
            if (!scope.isEmpty()) {
                scope = scope + " " + scope1;
            } else {
                scope = scope1;
            }
        }
        // form4.setParameter("scope", scope);
        parameters = parameters + "&scope=" + scope;

        // handle prompt
        String prompt = (String) oauthFormData.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            prompt = settings.getPrompt();
        }
        // form4.setParameter( "prompt", prompt);
        parameters = parameters + "&prompt=" + prompt;

        String redirectUri = replaceCtrl((String) oauthFormData.get("redirect_uri"));
        parameters = parameters + "&redirect_uri=" + redirectUri;

        return parameters;
    }

    String replaceCtrl(String string) {
        String str = "\\/";
        int index = string.indexOf(str);
        while (index >= 0) {
            string = string.substring(0, index) + "/" + string.substring(index + str.length());
            index = string.indexOf(str);
        }
        return string;
    }

    HashMap<String, Object> parseOauthFormData(String responseFullText) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        String oauthFormDataKey = "oauthFormData=";
        int iData = responseFullText.indexOf(oauthFormDataKey);
        if (iData > -1) {
            String dataString = responseFullText.substring(iData + oauthFormDataKey.length());
            StringTokenizer st = new StringTokenizer(dataString, "{}[],;\":", true);
            result = parse(st);
        }
        return result;
    }

    static final String brace1 = "{";
    static final String brace2 = "}";
    static final String str1 = "\"";
    static final String str2 = "\"";
    static final String square1 = "[";
    static final String square2 = "]";
    static final String endMap = ";";
    static final String colon = ":";
    static final String more = ",";

    HashMap<String, Object> parse(StringTokenizer st) {
        String token = st.nextToken();
        if (token.endsWith(brace1)) {
            return parseMap(st);
        } else {
            return new HashMap<String, Object>();
        }
    }

    private HashMap<String, Object> parseMap(StringTokenizer st) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        String token = st.nextToken();
        while (true) {
            if (token.equals(str1)) {
                parseEntry(result, st);
            } else if (token.equals(brace2)) {
                break;
            } else if (token.endsWith(endMap)) {
                break;
            } else if (token.endsWith(more)) {
            } else {
                Log.info(thisClass, "parseMap", "***ERR unexpect token:" + token);
                break;
            }
            token = st.nextToken();
        }
        return result;
    }

    void parseEntry(HashMap<String, Object> result, StringTokenizer st) {
        String key = getString(st);
        String ta = st.nextToken(); // this ought to be the ":"
        if (ta.equals(colon)) {
            String tb = st.nextToken();
            if (tb.equals(brace1)) {
                HashMap<String, Object> value = parseMap(st);
                result.put(key, value);
            } else if (tb.equals(square1)) {
                Object[] strArray = parseStrArray(st);
                result.put(key, strArray);
            } else if (tb.equals(str1)) {
                result.put(key, getString(st));
            } else {
                Log.info(thisClass, "parseEntry", "***ERR expect a value '{[\"' but is" + ta);
            }
        } else {
            Log.info(thisClass, "parseEntry", "***ERR expect a ':' but is" + ta);
        }
    }

    Object[] parseStrArray(StringTokenizer st) {
        // This is not necessary a String array. But so far, yes
        Object[] result = new Object[0];
        int iCnt = 0;
        String token = st.nextToken();
        while (true) {
            if (token.equals(str1)) {
                int iLen = iCnt;
                iCnt++;
                String value = getString(st);
                Object[] oldResult = result;
                result = new String[iCnt];
                System.arraycopy(oldResult, 0, result, 0, iLen);
                result[iLen] = value;
            } else if (token.equals(square2)) {
                break;
            } else if (token.endsWith(more)) {
                // do nothing
            } else {
                Log.info(thisClass, "parseStrArreay", "***ERR get" + token);
                break;
            }
            token = st.nextToken();
        }
        return result;
    }

    String getString(StringTokenizer st) {
        String result = "";
        String ta = st.nextToken();
        while (!ta.equals(str2)) {
            result = result + ta;
            ta = st.nextToken();
        }
        Log.info(thisClass, "getString", "return '" + result + "'");
        return result;
    }

    public WebResponse processRPRequestAgain(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        WebRequest request = null;
        String thisMethod = "processRPRequestAgain";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();

        try {

            Log.info(thisClass, testcase, "testURL2: " + settings.getTestURL2());
            request = new GetMethodWebRequest(settings.getTestURL2());
            Log.info(thisClass, testcase, "Request: " + request);

            // Check the response
            response = wc.getResponse(request);

            msgUtils.printResponseParts(response, thisMethod, "Response from processRPRequestAgain: ");
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.LOGIN_AGAIN, e);

        }
        validationTools.validateResult(response, Constants.LOGIN_AGAIN, expectations, settings);
        return response;
    }

    public WebResponse processEndSession(String testcase, WebConversation wc, String id_token, TestSettings settings, List<validationData> expectations) throws Exception {

        WebRequest request = null;
        String thisMethod = "processEndSession";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();
        WebResponse response = null;

        try {

            // String id_token = validationTools.getIDToken(settings, response)
            // ;
            Log.info(thisClass, testcase, "endSession: " + settings.getEndSession());
            request = new GetMethodWebRequest(settings.getEndSession());
            Log.info(thisClass, testcase, "Request: " + request);

            if (id_token != null) {
                request.setParameter("id_token_hint", id_token);
                Log.info(thisClass, testcase, "id_token_hint: " + id_token);
            } else {
                Log.info(thisClass, testcase, "id_token_hint will NOT be set");
            }
            if (settings.getPostLogoutRedirect() != null) {
                request.setParameter("post_logout_redirect_uri", settings.getPostLogoutRedirect());
                Log.info(thisClass, testcase, "post_logout_redirect_uri: " + settings.getPostLogoutRedirect());
            }
            // Check the response
            response = wc.getResponse(request);

            msgUtils.printResponseParts(response, thisMethod, "Response from " + thisMethod + ": ");

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.LOGOUT, e);

        }
        validationTools.validateResult(response, Constants.LOGOUT, expectations, settings);
        // logout just just do logout and validate expectations, extra
        // validation should not be done here
        // msgUtils.assertTrueAndLog(thisMethod,
        // "LTPA cookie still exists after logout",
        // !cttools.ltpaCookieExists(cttools.extractLtpaCookie(wc,
        // Constants.LTPA_TOKEN)) );
        return response;
    }

    public Object processLogout(String testcase, WebClient webClient, String id_token, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processLogout";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();
        Object thePage = null;

        try {

            setMarkToEndOfAllServersLogs();

            // Invoke protected resource
            URL url = AutomationTools.getNewUrl(settings.getEndSession());
            com.gargoylesoftware.htmlunit.WebRequest request = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.POST);
            request.setRequestParameters(new ArrayList());

            if (id_token != null) {
                setRequestParameterIfSet(request, "id_token_hint", id_token);
                Log.info(thisClass, testcase, "id_token_hint: " + id_token);
            } else {
                Log.info(thisClass, testcase, "id_token_hint will NOT be set");
            }
            if (settings.getPostLogoutRedirect() != null) {
                setRequestParameterIfSet(request, "post_logout_redirect_uri", settings.getPostLogoutRedirect());
                Log.info(thisClass, testcase, "post_logout_redirect_uri: " + settings.getPostLogoutRedirect());
            }

            msgUtils.printRequestParts(webClient, request, thisMethod, "Request for " + Constants.LOGOUT);
            thePage = webClient.getPage(request);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from protected app: ");

        } catch (Exception e) {

            validationTools.validateException(expectations, Constants.LOGOUT, e);

        }
        validationTools.validateResult(thePage, Constants.LOGOUT, expectations, settings);
        return thePage;

    }

    public void setMarkToEndOfAllServersLogs() {

        if (overrideSetServerMark) {
            Log.info(thisClass, "setMarkToEndOfAllServersLogs", "Skipping setMarkToEndOfAllServersLogs");
            setOverrideSetServerMark(false);
            return;
        }
        msgUtils.printMethodName("setMarkToEndOfAllServersLogs");

        for (TestServer server : serverRefList) {
            if (server != null) {
                Log.info(thisClass, "setMarkToEndOfAllServersLogs", server.getServerType());
                server.setMarkToEndOfLogs();
            }
        }
    }

//    public void updateServerXml(String serverXml, String attribute, String attributeValue) throws Exception {
//
//        String thisMethod = "updateServerXml";
//        msgUtils.printMethodName(thisMethod);
//
//        SAXBuilder builder = new SAXBuilder();
//        File xmlFile = new File(serverXml);
//        Document doc = builder.build(xmlFile);
//        Element rootNode = doc.getRootElement();
//        Element parent = rootNode.getChild("httpEndpoint");
//        Log.info(thisClass, thisMethod, "current endpoint httpPort is: " + parent.getAttributeValue("httpPort"));
//        Log.info(thisClass, thisMethod, "current endpoint httpsPort is: " + parent.getAttributeValue("httpsPort"));
//
//    }

    public TestSettings fixProviderInUrls(TestSettings settings, String oldProvider, String newProvider) throws Exception {

        settings.setFirstClientURL(safeReplace(settings.getFirstClientURL(), oldProvider, newProvider));
        settings.setTokenEndpt(safeReplace(settings.getTokenEndpt(), oldProvider, newProvider));
        settings.setAuthorizeEndpt(safeReplace(settings.getAuthorizeEndpt(), oldProvider, newProvider));
        settings.setRegistrationEndpt(safeReplace(settings.getRegistrationEndpt(), oldProvider, newProvider));
        settings.setIntrospectionEndpt(safeReplace(settings.getIntrospectionEndpt(), oldProvider, newProvider));

        return settings;
    }

    public String safeReplace(String source, String oldValue, String newValue) throws Exception {

        if (source == null) {
            return source;
        } else {
            return source.replace(oldValue, newValue);
        }
    }

    public void overrideRedirect() {

        // OverrideRedirect = true ;
        HttpUnitOptions.setAutoRedirect(false);

    }

    public WebResponse performIDPLogin(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performIDPLogin";
        msgUtils.printMethodName(thisMethod);

        try {
            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Calling IdP Login...");

            WebForm form = response.getForms()[0];

            // Fill in the form login and submit the login request

            form.setParameter("j_username", settings.getAdminUser());
            form.setParameter("j_password", settings.getAdminPswd());
            // form.setParameter( "j_username", "user1" );
            // form.setParameter( "j_password", "security" );

            msgUtils.printAllCookies(wc);
            msgUtils.printAllParams(form);

            // submit form login with user ID and password
            response = form.submit(null, 0, 0);

            msgUtils.printAllCookies(wc);
            msgUtils.printResponseParts(response, thisMethod, "Response from login: ");

            validationTools.validateResult(response, Constants.PERFORM_IDP_LOGIN, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.PERFORM_IDP_LOGIN, e);
        }

        return (response);

    }

    public Object performIDPLogin(String testcase, WebClient webClient, HtmlPage startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performIDPLogin - webClient";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkToEndOfAllServersLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Calling IdP Login...");
            webClient.getCookieManager().setCookiesEnabled(true);

            msgUtils.printAllCookies(webClient);

            final HtmlForm form = startPage.getForms().get(0);
            //            webClient.getOptions().setJavaScriptEnabled(false);

            Log.info(thisClass, thisMethod, "Java script enabled: " + webClient.getOptions().isJavaScriptEnabled());
            Log.info(thisClass, thisMethod, "Redirect enabled: " + webClient.getOptions().isRedirectEnabled());

            // Fill in the login form and submit the login request
            HtmlElement button = null;

            if (AutomationTools.getResponseText(startPage).contains(Constants.SAML_REQUEST)) {
                button = form.getButtonByName("redirectform");
            } else {
                button = form.getButtonByName("_eventId_proceed");

                final HtmlTextInput textField = form.getInputByName("j_username");
                Log.info(thisClass, thisMethod, "username field is: " + textField);
                textField.setValueAttribute(settings.getAdminUser());
                final HtmlPasswordInput textField2 = form.getInputByName("j_password");
                Log.info(thisClass, thisMethod, "password field is: " + textField2);
                textField2.setValueAttribute(settings.getAdminPswd());
                Log.info(thisClass, thisMethod, "Setting: " + textField + " to: " + settings.getAdminUser());
                Log.info(thisClass, thisMethod, "Setting: " + textField2 + " to: " + settings.getAdminPswd());

                msgUtils.printFormParts(form, thisMethod, "Parms for IDPClientJSP: ");
            }

            Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");

            thePage = button.click();

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            if (!webClient.getOptions().isRedirectEnabled()) {
                msgUtils.printAllCookies(webClient);
                msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod);

                HtmlForm nextForm = ((HtmlPage) thePage).getForms().get(0);
                HtmlElement nextButton = nextForm.getInputByValue("Continue");
                thePage = nextButton.click();
                waitBeforeContinuing(webClient);
            }

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod);
            validationTools.validateResult(thePage, Constants.PERFORM_IDP_LOGIN, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.PERFORM_IDP_LOGIN, e);
        }
        return (thePage);

    }

    public WebResponse invokeACS(String testcase, WebConversation wc, WebResponse response, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeACS";
        msgUtils.printMethodName(thisMethod);

        try {
            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Invoking ACS...");

            WebForm form = response.getForms()[0];

            msgUtils.printAllCookies(wc);
            msgUtils.printAllParams(form);

            // submit form login with user ID and password
            response = form.submit(null, 0, 0);

            msgUtils.printResponseParts(response, thisMethod, "Response from ACS: ");

            validationTools.validateResult(response, Constants.INVOKE_ACS, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.INVOKE_ACS, e);
        }

        return (response);

    }

    public Object invokeACS(String testcase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeACSWithSAMLResponse";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {

            setMarkToEndOfAllServersLogs();

            //            webClient.getOptions().setJavaScriptEnabled(false);
            HtmlForm form = ((HtmlPage) startPage).getForms().get(0);
            com.gargoylesoftware.htmlunit.WebRequest webRequest = form.getWebRequest(null);

            msgUtils.printAllCookies(webClient);
            Log.info(thisClass, thisMethod, "WebClient isJavaScriptEnabled: " + webClient.getOptions().isJavaScriptEnabled());

            thePage = webClient.getPage(webRequest);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + " Continue");

            validationTools.validateResult(thePage, Constants.INVOKE_ACS, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.INVOKE_ACS, e);
        }

        return thePage;

    }

    public WebRequest setNonNullParameter(WebRequest request, String key, String value) throws Exception {

        if (value != null) {
            Log.info(thisClass, "setNonNullParameter", "We have key: " + key + " with value: " + value);
            request.setParameter(key, value);
        }
        return request;

    }

    public WebRequest setNonZeroLongParameter(WebRequest request, String key, Long value) throws Exception {

        //        if (value != 0L && value != -1L) {
        if (value != -1L) {
            Log.info(thisClass, "setNonNullParameter", "We have key: " + key + " with value: " + value.toString());
            request.setParameter(key, value.toString());
        }
        return request;

    }

    public HashMap<String, String[]> setupParmsMap(String tokenValue) throws Exception {
        return setupParmsMap(new String[] { tokenValue });
    }

    public HashMap<String, String[]> setupParmsMap(String[] tokenValue) throws Exception {
        HashMap<String, String[]> theMap = new HashMap<String, String[]>();
        theMap.put(Constants.ACCESS_TOKEN_KEY, tokenValue);
        return theMap;
    }

    public HashMap<String, String[]> setupHeadersMap(TestSettings settings, String tokenValue) throws Exception {
        return setupHeadersMap(settings, new String[] { tokenValue });
    }

    public HashMap<String, String[]> setupHeadersMap(TestSettings settings, String[] tokenValue) throws Exception {
        HashMap<String, String[]> theMap = new HashMap<String, String[]>();
        String[] builtValue = new String[tokenValue.length];
        for (int i = 0; i < tokenValue.length; i++) {
            builtValue[i] = Constants.BEARER + " " + tokenValue[i];
        }
        theMap.put(settings.getHeaderName(), builtValue);
        return theMap;
    }

    public Object processPutRequest(String testcase, WebClient webClient, String token, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processLogout";
        msgUtils.printMethodName(thisMethod);

        // set the mark to the end of all logs to ensure that any checking for
        // messages is done only for this step of the testing
        setMarkToEndOfAllServersLogs();
        Object thePage = null;

        try {

            setMarkToEndOfAllServersLogs();

            // Invoke protected resource
            URL url = AutomationTools.getNewUrl(settings.getTokenEndpt());
            com.gargoylesoftware.htmlunit.WebRequest request = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.PUT);
            request.setRequestParameters(new ArrayList());

            if (token != null) {
                setRequestParameterIfSet(request, "tokenToSave", token);
                Log.info(thisClass, testcase, "tokenToSave: " + token);
            } else {
                Log.info(thisClass, testcase, "tokenToSave will NOT be set");
                throw new Exception("We need to save off a token for the tokenEndpoint to return to the RP - it's kinda the point of the test");
            }

            msgUtils.printRequestParts(webClient, request, thisMethod, "Request for " + Constants.PUTMETHOD);
            thePage = webClient.getPage(request);

            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from protected app: ");

        } catch (Exception e) {

            validationTools.validateException(expectations, Constants.PUTMETHOD, e);

        }
        validationTools.validateResult(thePage, Constants.PUTMETHOD, expectations, settings);
        return thePage;

    }

}

/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.actions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.exceptions.TestActionException;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.web.WebFormUtils;
import com.meterware.httpunit.Base64;

public class TestActions {

    public static final String ACTION_INVOKE_PROTECTED_RESOURCE = "invokeProtectedResource";
    public static final String ACTION_SUBMIT_LOGIN_CREDENTIALS = "submitLoginCredentials";
    public static final String ACTION_INSTALL_APP = "installApp";

    protected CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
    WebFormUtils webFormUtils = new WebFormUtils();

    /**
     * Invokes the specified URL and returns the Page object that represents the response.
     */
    public Page invokeUrl(String currentTest, String url) throws Exception {
        return invokeUrl(currentTest, createWebClient(), url);
    }

    /**
     * Invokes the specified URL using the provided WebClient object and returns the Page object that represents the response.
     */
    public Page invokeUrl(String currentTest, WebClient wc, String url) throws Exception {
        String thisMethod = "invokeUrl";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createGetRequest(url);
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Invoke the specified URL, adding a basic auth header first
     */
    public Page invokeUrlWithBasicAuth(String currentTest, String url, String user, String password) throws Exception {
        return invokeUrlWithBasicAuth(currentTest, createWebClient(), url, user, password);
    }

    /**
     * Invoke the specified URL, adding a basic auth header first
     */
    public Page invokeUrlWithBasicAuth(String currentTest, WebClient wc, String url, String user, String password) throws Exception {
        String thisMethod = "invokeUrlWithBasicAuth";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createGetRequest(url);
            String encodedIdPw = Base64.encode(user + ":" + password);
            request.setAdditionalHeader("Authorization", "Basic " + encodedIdPw);
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Invoke the specified URL, adding a bearer token header first.
     */
    public Page invokeUrlWithBearerToken(String currentTest, WebClient wc, String url, String token) throws Exception {
        return invokeUrlWithBearerTokenUsingGet(currentTest, wc, url, token);
    }

    public Page invokeUrlWithBearerTokenUsingGet(String currentTest, WebClient wc, String url, String token) throws Exception {
        return invokeUrlWithAuthorizationHeaderToken(currentTest, wc, url, Constants.TOKEN_TYPE_BEARER, token, HttpMethod.GET, null);
    }

    public Page invokeUrlWithBearerTokenUsingGet(String currentTest, String url, String token) throws Exception {
        return invokeUrlWithAuthorizationHeaderToken(currentTest, new WebClient(), url, Constants.TOKEN_TYPE_BEARER, token, HttpMethod.GET, null);
    }

    public Page invokeUrlWithBearerTokenUsingPost(String currentTest, WebClient wc, String url, String token) throws Exception {
        return invokeUrlWithAuthorizationHeaderToken(currentTest, wc, url, Constants.TOKEN_TYPE_BEARER, token, HttpMethod.POST, null);
    }

    public Page invokeUrlWithBearerTokenUsingPost(String currentTest, String url, String token) throws Exception {
        return invokeUrlWithAuthorizationHeaderToken(currentTest, new WebClient(), url, Constants.TOKEN_TYPE_BEARER, token, HttpMethod.POST, null);
    }

    public Page invokeUrlWithAuthorizationHeaderToken(String currentTest, WebClient wc, String url, String tokenPrefix, String token, HttpMethod method, List<NameValuePair> requestParms) throws Exception {
        String thisMethod = "invokeUrlWithBearerToken";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createHttpRequest(url, method);
            if (tokenPrefix != null) {
                request.setAdditionalHeader("Authorization", tokenPrefix + " " + token);
            }
            if (requestParms != null) {
                request.setRequestParameters(requestParms);
            }
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Invokes the specified URL, including the specified cookie in the request, and returns the Page object that represents the
     * response.
     */
    public Page invokeUrlWithCookie(String currentTest, String url, Cookie cookie) throws Exception {
        return invokeUrlWithCookies(currentTest, url,cookie);
    }
    
    /**
     * Invokes the specified URL, including the specified cookies in the request, and returns the Page object that represents the
     * response.
     */
    public Page invokeUrlWithCookies(String currentTest, String url, Cookie... cookies) throws Exception {
        String thisMethod = "invokeUrlWithCookies";
        loggingUtils.printMethodName(thisMethod);
        try {
            if (cookies == null || cookies.length == 0) {
                throw new Exception("Cannot invoke the URL because no cookies were provided.");
            }
            WebRequest request = createGetRequest(url);
            
            String cookieString = "";
            boolean loopStart = true;
            for(Cookie c : cookies){
                if(loopStart){
                    loopStart = false;
                }else{
                    cookieString += "; ";
                }
                    cookieString += c.getName() + "=" + c.getValue();

            }
            request.setAdditionalHeader("Cookie", cookieString);
            return submitRequest(currentTest, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Invokes the specified URL using the provided WebClient object and returns the Page object that represents the response.
     */
    public Page invokeUrlWithParameters(String currentTest, WebClient wc, String url, List<NameValuePair> requestParams) throws Exception {
        return invokeUrlWithParametersUsingGet(currentTest, wc, url, requestParams);
    }

    public Page invokeUrlWithParametersUsingGet(String currentTest, WebClient wc, String url, List<NameValuePair> requestParams) throws Exception {
        return invokeUrlWithParameters(currentTest, wc, url, HttpMethod.GET, requestParams);
    }

    public Page invokeUrlWithParametersUsingGet(String currentTest, String url, List<NameValuePair> requestParams) throws Exception {
        return invokeUrlWithParameters(currentTest, createWebClient(), url, HttpMethod.GET, requestParams);
    }

    public Page invokeUrlWithParametersUsingPost(String currentTest, WebClient wc, String url, List<NameValuePair> requestParams) throws Exception {
        return invokeUrlWithParameters(currentTest, wc, url, HttpMethod.POST, requestParams);
    }

    public Page invokeUrlWithParametersUsingPost(String currentTest, String url, List<NameValuePair> requestParams) throws Exception {
        return invokeUrlWithParameters(currentTest, createWebClient(), url, HttpMethod.POST, requestParams);
    }

    public Page invokeUrlWithParameters(String currentTest, WebClient wc, String url, HttpMethod method, List<NameValuePair> requestParams) throws Exception {
        String thisMethod = "invokeUrlWithParameters";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createHttpRequest(url, method);
            if (requestParams != null) {
                request.setRequestParameters(requestParams);
            }
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Invokes the specified URL using the provided WebClient object and returns the Page object that represents the response.
     */
    public Page invokeUrlWithParametersAndHeaders(String currentTest, WebClient wc, String url, List<NameValuePair> requestParams, Map<String, String> requestHeaders) throws Exception {
        String thisMethod = "invokeUrlWithParameters";
        loggingUtils.printMethodName(thisMethod);
        try {
            WebRequest request = createGetRequest(url);
            if (requestParams != null) {
                request.setRequestParameters(requestParams);
            }
            if (requestHeaders != null) {
                request.setAdditionalHeaders(requestHeaders);
            }
            return submitRequest(currentTest, wc, request);
        } catch (Exception e) {
            throw new Exception("An error occurred invoking the URL [" + url + "]: " + e);
        }
    }

    /**
     * Submits the specified WebRequest and returns the Page object that represents the response.
     */
    public Page submitRequest(String currentTest, WebRequest request) throws Exception {
        return submitRequest(currentTest, createWebClient(), request);
    }

    /**
     * Submits the specified WebRequest using the provided WebClient object and returns the Page object that represents the
     * response.
     */
    public Page submitRequest(String currentTest, WebClient wc, WebRequest request) throws Exception {
        String thisMethod = "submitRequest";
        loggingUtils.printMethodName(thisMethod);

        if (request == null) {
            throw new Exception("Cannot invoke the URL because the provided WebRequest object is null.");
        }
        if (wc == null) {
            wc = createWebClient();
        }
        return submitRequestWithNonNullObjects(currentTest, wc, request);
    }

    private Page submitRequestWithNonNullObjects(String currentTest, WebClient wc, WebRequest request) throws Exception {
        String thisMethod = "submitRequestWithNonNullObjects";
        loggingUtils.printRequestParts(wc, request, currentTest);
        try {
            Page response = wc.getPage(request);
            loggingUtils.printResponseParts(response, currentTest, "Response from URL: ");
            return response;
        } catch (Exception e) {
            throw new TestActionException(thisMethod, "An error occurred while submitting a request to [" + request.getUrl() + "].", e);
        }
    }

    /**
     * Finds, fills out, and submits the standard login form in the provided page using the specified credentials. This method
     * expects the page to be an instance of HtmlPage that contains at least one form. The first form the page contains is
     * expected to have an action value of {@code j_security_check} and inputs for {@code j_username} and {@code j_password}.
     */
    public Page doFormLogin(Page loginPage, String username, String password) throws Exception {
        String thisMethod = "doFormLogin";
        loggingUtils.printMethodName(thisMethod);

        if (loginPage == null) {
            throw new Exception("Cannot perform login because the provided page object is null.");
        }
        if (!(loginPage instanceof HtmlPage)) {
            throw new Exception("Cannot perform login because the provided page object is not a " + HtmlPage.class.getName() + " instance. Page class is: "
                    + loginPage.getClass().getName());
        }
        return doFormLogin((HtmlPage) loginPage, username, password);
    }

    /**
     * Finds, fills out, and submits the standard login form in the provided page using the specified credentials. This method
     * expects the page to contain at least one form. The first form the page contains is expected to have an action value of
     * {@code j_security_check} and inputs for {@code j_username} and {@code j_password}.
     */
    public Page doFormLogin(HtmlPage loginPage, String username, String password) throws Exception {
        String thisMethod = "doFormLogin";
        loggingUtils.printMethodName(thisMethod);
        if (loginPage == null) {
            throw new Exception("Cannot perform login because the provided page object is null.");
        }
        try {
            Page postSubmissionPage = webFormUtils.getAndSubmitLoginForm(loginPage, username, password);
            loggingUtils.printResponseParts(postSubmissionPage, thisMethod, "Response from login form submission:");
            return postSubmissionPage;
        } catch (Exception e) {
            throw new TestActionException(thisMethod, "An error occurred while performing form login.", e);
        }
    }

    public WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setUseInsecureSSL(true);
        return webClient;
    }

    public WebRequest createHttpRequest(String url, HttpMethod method) throws MalformedURLException {
        return new WebRequest(new URL(url), method);
    }

    public WebRequest createGetRequest(String url) throws MalformedURLException {
        return new WebRequest(new URL(url), HttpMethod.GET);
    }

    public WebRequest createPostRequest(String url) throws MalformedURLException {
        return new WebRequest(new URL(url), HttpMethod.POST);
    }

    public WebRequest createPostRequest(String url, String body) throws MalformedURLException {
        URL reqUrl = new URL(url);
        WebRequest request = new WebRequest(reqUrl, HttpMethod.POST);
        request.setRequestBody(body);
        return request;
    }
}
